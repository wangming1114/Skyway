package com.skyway.web.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IVpsInstanceService;

/**
 * VPS 实时网速后台采集器。
 *
 * 列表和详情只读取内存快照，避免每次刷新都触发 SSH。
 */
@Component
public class VpsRealtimeSpeedManager {

    private static final Logger log = LoggerFactory.getLogger(VpsRealtimeSpeedManager.class);
    private static final long RECONCILE_INTERVAL_SECONDS = 10L;
    private static final long PROBE_INTERVAL_MILLIS = 45_000L;
    private static final int ROLLING_OUTPUT_LIMIT = 16_384;

    @Autowired
    private IVpsInstanceService vpsInstanceService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    private final Map<Long, VpsSshCommandService.RealtimeSpeedSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<Long, CollectorWorker> workers = new ConcurrentHashMap<>();
    private final Map<Long, String> signatures = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastProbeAt = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "vps-speed-reconcile");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService workerExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "vps-speed-worker");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean closed = false;

    @PostConstruct
    public void start() {
        scheduler.scheduleWithFixedDelay(this::safeReconcileNow, 2L, RECONCILE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        closed = true;
        for (CollectorWorker worker : new ArrayList<>(workers.values())) {
            worker.stop();
        }
        workers.clear();
        scheduler.shutdownNow();
        workerExecutor.shutdownNow();
    }

    public Map<Long, VpsSshCommandService.RealtimeSpeedSnapshot> snapshotAll() {
        return new HashMap<>(snapshots);
    }

    public VpsSshCommandService.RealtimeSpeedSnapshot snapshot(Long instanceId) {
        if (instanceId == null) {
            return skippedSnapshot("unknown");
        }
        VpsSshCommandService.RealtimeSpeedSnapshot snapshot = snapshots.get(instanceId);
        return snapshot != null ? snapshot : skippedSnapshot("pending");
    }

    public void reconcileNow() {
        if (vpsInstanceService == null) {
            return;
        }
        List<VpsInstance> instances = vpsInstanceService.selectList(new VpsInstance());
        reconcile(instances);
    }

    void reconcile(List<VpsInstance> instances) {
        if (closed) {
            return;
        }
        List<VpsInstance> safeList = instances != null ? instances : Collections.emptyList();
        Set<Long> seen = new HashSet<>();
        for (VpsInstance instance : safeList) {
            if (instance == null || instance.getId() == null) {
                continue;
            }
            Long id = instance.getId();
            seen.add(id);
            if (shouldStartSpeedWorker(instance)) {
                ensureWorker(instance);
            } else {
                stopWorker(id);
                signatures.remove(id);
                snapshots.put(id, skippedSnapshot(instance.getStatus()));
                maybeProbeNonRunning(instance);
            }
        }
        for (Long id : new HashSet<>(workers.keySet())) {
            if (!seen.contains(id)) {
                stopWorker(id);
            }
        }
        for (Long id : new HashSet<>(snapshots.keySet())) {
            if (!seen.contains(id)) {
                snapshots.remove(id);
                signatures.remove(id);
                lastProbeAt.remove(id);
            }
        }
    }

    public static boolean shouldStartSpeedWorker(VpsInstance instance) {
        return instance != null
                && instance.getId() != null
                && VpsSshCommandService.isRealtimeSpeedAllowedStatus(instance.getStatus())
                && StringUtils.isNotEmpty(instance.getIp())
                && instance.getSshPort() != null
                && StringUtils.isNotEmpty(instance.getSshUsername());
    }

    public static VpsSshCommandService.RealtimeSpeedSnapshot skippedSnapshot(String status) {
        VpsSshCommandService.RealtimeSpeedSnapshot snapshot = new VpsSshCommandService.RealtimeSpeedSnapshot();
        snapshot.setSkipped(true);
        if ("pending".equals(status)) {
            snapshot.setMessage("实时网速等待后台采集中");
        } else {
            snapshot.setMessage("实例状态非正常，未监控实时网速");
        }
        return snapshot;
    }

    private void safeReconcileNow() {
        try {
            reconcileNow();
        } catch (Exception e) {
            log.warn("VPS realtime speed reconcile failed: {}", e.getMessage());
        }
    }

    private void startWorker(VpsInstance instance) {
        CollectorWorker worker = new CollectorWorker(instance);
        workers.put(instance.getId(), worker);
        snapshots.put(instance.getId(), skippedSnapshot("pending"));
        workerExecutor.submit(worker);
    }

    private void ensureWorker(VpsInstance instance) {
        Long id = instance.getId();
        String signature = signature(instance);
        CollectorWorker current = workers.get(id);
        if (current == null || !signature.equals(signatures.get(id))) {
            stopWorker(id);
            signatures.put(id, signature);
            startWorker(instance);
        }
    }

    private void stopWorker(Long id) {
        CollectorWorker worker = workers.remove(id);
        if (worker != null) {
            worker.stop();
        }
    }

    private void maybeProbeNonRunning(VpsInstance instance) {
        if (vpsSshCommandService == null || vpsInstanceService == null || StringUtils.isEmpty(instance.getIp())) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastProbeAt.get(instance.getId());
        if (last != null && now - last < PROBE_INTERVAL_MILLIS) {
            return;
        }
        lastProbeAt.put(instance.getId(), now);
        workerExecutor.submit(() -> {
            try {
                String status = vpsSshCommandService.detectInstanceStatus(instance.getId());
                if (status != null && !status.equals(instance.getStatus())) {
                    VpsInstance update = new VpsInstance();
                    update.setId(instance.getId());
                    update.setStatus(status);
                    vpsInstanceService.update(update);
                    instance.setStatus(status);
                    if (shouldStartSpeedWorker(instance)) {
                        ensureWorker(instance);
                    }
                }
            } catch (Exception e) {
                log.debug("probe non-running VPS id={} failed: {}", instance.getId(), e.getMessage());
            }
        });
    }

    private static String signature(VpsInstance instance) {
        String password = instance.getSshPassword() != null ? instance.getSshPassword() : "";
        return String.valueOf(instance.getIp()) + ":" + instance.getSshPort()
                + ":" + instance.getSshUsername() + ":" + password.hashCode();
    }

    private final class CollectorWorker implements Runnable {
        private final VpsInstance instance;
        private volatile boolean stopped = false;
        private SSHClient ssh;
        private Session session;
        private Command command;

        private CollectorWorker(VpsInstance instance) {
            this.instance = instance;
        }

        @Override
        public void run() {
            Long id = instance.getId();
            try {
                ssh = vpsSshCommandService.openSshClient(id);
                vpsSshCommandService.ensureRealtimeSpeedScript(ssh);
                session = ssh.startSession();
                command = vpsSshCommandService.startRealtimeSpeedCommand(session);
                snapshots.put(id, skippedSnapshot("pending"));
                readSpeedOutput(id, command.getInputStream());
            } catch (Exception e) {
                if (!stopped) {
                    VpsSshCommandService.RealtimeSpeedSnapshot snapshot = skippedSnapshot("error");
                    snapshot.setMessage(e.getMessage() != null ? e.getMessage() : "实时网速采集异常");
                    snapshots.put(id, snapshot);
                    markStatusAfterWorkerFailure(id);
                    log.warn("VPS realtime speed worker failed: instanceId={}, error={}", id, e.getMessage());
                }
            } finally {
                closeQuietly();
                workers.remove(id, this);
            }
        }

        private void readSpeedOutput(Long id, InputStream input) throws IOException {
            byte[] buf = new byte[2048];
            StringBuilder rolling = new StringBuilder();
            int n;
            while (!stopped && (n = input.read(buf)) > 0) {
                rolling.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                if (rolling.length() > ROLLING_OUTPUT_LIMIT) {
                    rolling.delete(0, rolling.length() - ROLLING_OUTPUT_LIMIT);
                }
                VpsSshCommandService.RealtimeSpeedSnapshot parsed =
                        VpsSshCommandService.parseRealtimeSpeedOutput(rolling.toString());
                if (!parsed.getPorts().isEmpty()) {
                    parsed.setSkipped(false);
                    parsed.setMessage("采集中");
                    snapshots.put(id, parsed);
                }
            }
        }

        private void stop() {
            stopped = true;
            closeQuietly();
        }

        private void closeQuietly() {
            try {
                if (command != null) command.close();
            } catch (Exception ignored) {}
            try {
                if (session != null) session.close();
            } catch (Exception ignored) {}
            try {
                if (ssh != null) ssh.close();
            } catch (Exception ignored) {}
        }
    }

    private void markStatusAfterWorkerFailure(Long id) {
        if (id == null || vpsSshCommandService == null || vpsInstanceService == null) {
            return;
        }
        try {
            String status = vpsSshCommandService.detectInstanceStatus(id);
            if (status != null && !VpsSshCommandService.isRealtimeSpeedAllowedStatus(status)) {
                VpsInstance update = new VpsInstance();
                update.setId(id);
                update.setStatus(status);
                vpsInstanceService.update(update);
                snapshots.put(id, skippedSnapshot(status));
            }
        } catch (Exception e) {
            log.debug("mark status after realtime worker failure id={} failed: {}", id, e.getMessage());
        }
    }
}
