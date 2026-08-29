package com.skyway.web.websocket;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import com.alibaba.fastjson2.JSON;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.service.IMbCustomerService;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IVpsInstanceService;
import com.skyway.web.service.VpsSshCommandService;
import com.skyway.web.websocket.AccessLogParser.AccessLogEntry;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

/** Streams read-only sing-box destination logs over a dedicated WebSocket. */
@Component
public class AccessLogWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AccessLogWebSocketHandler.class);
    private static final String LOG_PATH = "/var/log/sing-box/access.log";
    private static final int HISTORY_SCAN_LINES = 10_000;
    private static final int HISTORY_LIMIT = 50;
    private static final int LIVE_BUFFER_LIMIT = 1_000;
    private static final String ATTR_SSH = "accessLogSsh";
    private static final String ATTR_FOLLOW_SESSION = "accessLogFollowSession";
    private static final String ATTR_CLOSED = "accessLogClosed";

    @Autowired
    private IVpsInstanceService vpsInstanceService;
    @Autowired
    private IProxyNodeService proxyNodeService;
    @Autowired
    private IMbCustomerService mbCustomerService;
    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "access-log-ws-" + r.hashCode());
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) {
        AtomicBoolean closed = new AtomicBoolean(false);
        wsSession.getAttributes().put(ATTR_CLOSED, closed);
        sendStatus(wsSession, "connecting", "正在连接 VPS 并读取访问日志");
        executor.execute(() -> startMonitor(wsSession, closed));
    }

    private void startMonitor(WebSocketSession wsSession, AtomicBoolean closed) {
        SSHClient ssh = null;
        Session followSession = null;
        try {
            MonitorScope scope = resolveScope(wsSession);
            ssh = vpsSshCommandService.openSshClient(scope.instanceId);
            wsSession.getAttributes().put(ATTR_SSH, ssh);
            ensureReadable(ssh);

            List<AccessLogEntry> liveBuffer = new ArrayList<>();
            AtomicBoolean historyReady = new AtomicBoolean(false);

            followSession = ssh.startSession();
            Command follow = followSession.exec("tail -n 0 -F " + LOG_PATH);
            wsSession.getAttributes().put(ATTR_FOLLOW_SESSION, followSession);
            startFollower(wsSession, follow, scope, closed, historyReady, liveBuffer);

            List<AccessLogEntry> history = readHistory(ssh, scope);
            if (closed.get() || !wsSession.isOpen()) return;
            sendHistory(wsSession, history);

            List<AccessLogEntry> buffered;
            synchronized (liveBuffer) {
                buffered = new ArrayList<>(liveBuffer);
                liveBuffer.clear();
                flushBuffered(wsSession, history, buffered);
                sendStatus(wsSession, "live", "正在实时监控");
                historyReady.set(true);
            }
        } catch (Exception e) {
            if (!closed.get()) {
                log.warn("Access log monitor failed: {}", e.getMessage());
                sendError(wsSession, errorCode(e), friendlyMessage(e));
                closeQuietly(wsSession, CloseStatus.SERVER_ERROR);
            }
            cleanup(wsSession);
        }
    }

    private MonitorScope resolveScope(WebSocketSession wsSession) {
        String scopeValue = (String) wsSession.getAttributes().get(AccessLogWebSocketHandshakeInterceptor.ATTR_SCOPE);
        if ("node".equals(scopeValue)) {
            Long nodeId = (Long) wsSession.getAttributes().get(AccessLogWebSocketHandshakeInterceptor.ATTR_NODE_ID);
            ProxyNode node = proxyNodeService.getById(nodeId);
            if (node == null || node.getInstanceId() == null || node.getPort() == null || node.getNodeType() == null) {
                throw new IllegalArgumentException("代理节点不存在或信息不完整");
            }
            VpsInstance instance = vpsInstanceService.getById(node.getInstanceId());
            if (instance == null) throw new IllegalArgumentException("节点所属 VPS 不存在");
            MonitorScope scope = new MonitorScope(node.getInstanceId(), node.getNodeType() + "-" + node.getPort() + ".json");
            scope.nodesByTag.put(scope.wantedTag, node);
            loadCustomerName(scope, node.getCustomerId());
            return scope;
        }

        Long instanceId = (Long) wsSession.getAttributes().get(AccessLogWebSocketHandshakeInterceptor.ATTR_INSTANCE_ID);
        VpsInstance instance = vpsInstanceService.getById(instanceId);
        if (instance == null) throw new IllegalArgumentException("VPS 不存在");
        MonitorScope scope = new MonitorScope(instanceId, null);
        ProxyNode query = new ProxyNode();
        query.setInstanceId(instanceId);
        List<ProxyNode> nodes = proxyNodeService.selectList(query);
        if (nodes != null) {
            for (ProxyNode node : nodes) {
                if (node != null && node.getNodeType() != null && node.getPort() != null) {
                    scope.nodesByTag.put(node.getNodeType() + "-" + node.getPort() + ".json", node);
                    loadCustomerName(scope, node.getCustomerId());
                }
            }
        }
        return scope;
    }

    private void loadCustomerName(MonitorScope scope, Long customerId) {
        if (customerId == null || scope.customerNames.containsKey(customerId)) return;
        MbCustomer customer = mbCustomerService.getById(customerId);
        scope.customerNames.put(customerId, customer != null ? customer.getUsername() : null);
    }

    private void ensureReadable(SSHClient ssh) throws IOException {
        String commandText = "if [ ! -e " + LOG_PATH + " ]; then printf NOT_FOUND; "
                + "elif [ ! -r " + LOG_PATH + " ]; then printf NOT_READABLE; else printf OK; fi";
        try (Session session = ssh.startSession()) {
            Command command = session.exec(commandText);
            String output = readAll(command.getInputStream()).trim();
            command.join(10, TimeUnit.SECONDS);
            if ("NOT_FOUND".equals(output)) throw new AccessLogException("LOG_NOT_FOUND", "访问日志文件不存在: " + LOG_PATH);
            if ("NOT_READABLE".equals(output)) throw new AccessLogException("LOG_NOT_READABLE", "SSH 用户无权读取访问日志: " + LOG_PATH);
            if (!"OK".equals(output)) throw new IOException("无法确认访问日志状态");
        }
    }

    private List<AccessLogEntry> readHistory(SSHClient ssh, MonitorScope scope) throws IOException {
        Deque<AccessLogEntry> ring = new ArrayDeque<>(HISTORY_LIMIT);
        try (Session session = ssh.startSession()) {
            Command command = session.exec("tail -n " + HISTORY_SCAN_LINES + " " + LOG_PATH);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(command.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    AccessLogEntry entry = parseAndEnrich(line, scope);
                    if (entry == null) continue;
                    if (ring.size() == HISTORY_LIMIT) ring.removeFirst();
                    ring.addLast(entry);
                }
            }
            command.join(30, TimeUnit.SECONDS);
            Integer exit = command.getExitStatus();
            if (exit == null || exit != 0) throw new IOException("读取历史访问日志失败");
        }
        return new ArrayList<>(ring);
    }

    private void startFollower(WebSocketSession wsSession, Command command, MonitorScope scope,
                               AtomicBoolean closed, AtomicBoolean historyReady,
                               List<AccessLogEntry> liveBuffer) {
        executor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(command.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!closed.get() && (line = reader.readLine()) != null) {
                    AccessLogEntry entry = parseAndEnrich(line, scope);
                    if (entry == null) continue;
                    boolean sendNow;
                    synchronized (liveBuffer) {
                        sendNow = historyReady.get();
                        if (!sendNow) {
                            if (liveBuffer.size() == LIVE_BUFFER_LIMIT) liveBuffer.remove(0);
                            liveBuffer.add(entry);
                        }
                    }
                    if (sendNow) sendEntry(wsSession, entry);
                }
                if (!closed.get() && wsSession.isOpen()) {
                    sendError(wsSession, "FOLLOW_ENDED", "访问日志实时跟随已结束，请重连后重试");
                    closeQuietly(wsSession, CloseStatus.SERVER_ERROR);
                }
            } catch (Exception e) {
                if (!closed.get() && wsSession.isOpen()) {
                    sendError(wsSession, "FOLLOW_FAILED", "访问日志实时跟随失败: " + safeMessage(e));
                    closeQuietly(wsSession, CloseStatus.SERVER_ERROR);
                }
            }
        });
    }

    private AccessLogEntry parseAndEnrich(String line, MonitorScope scope) {
        AccessLogEntry entry = AccessLogParser.parse(line, scope.wantedTag);
        if (entry == null) return null;
        ProxyNode node = scope.nodesByTag.get(entry.getInboundTag());
        if (node != null) {
            enrichEntry(entry, node, scope.customerNames.get(node.getCustomerId()));
        }
        return entry;
    }

    static void enrichEntry(AccessLogEntry entry, ProxyNode node, String customerName) {
        entry.setNodeId(node.getId());
        entry.setNodeName(node.getNodeName());
        entry.setNodePort(node.getPort());
        entry.setCustomerId(node.getCustomerId());
        entry.setCustomerName(customerName);
    }

    private void flushBuffered(WebSocketSession wsSession, List<AccessLogEntry> history,
                               List<AccessLogEntry> buffered) {
        for (AccessLogEntry entry : removeHistoryOverlap(history, buffered)) {
            sendEntry(wsSession, entry);
        }
    }

    static List<AccessLogEntry> removeHistoryOverlap(List<AccessLogEntry> history,
                                                     List<AccessLogEntry> buffered) {
        Map<String, Integer> overlapCounts = new HashMap<>();
        for (AccessLogEntry entry : history) {
            overlapCounts.put(entry.getRawLine(), overlapCounts.getOrDefault(entry.getRawLine(), 0) + 1);
        }
        List<AccessLogEntry> result = new ArrayList<>();
        for (AccessLogEntry entry : buffered) {
            Integer count = overlapCounts.get(entry.getRawLine());
            if (count != null && count > 0) {
                if (count == 1) overlapCounts.remove(entry.getRawLine());
                else overlapCounts.put(entry.getRawLine(), count - 1);
            } else {
                result.add(entry);
            }
        }
        return result;
    }

    private void sendStatus(WebSocketSession session, String status, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "access_log_status");
        payload.put("status", status);
        payload.put("message", message);
        sendJson(session, payload);
    }

    private void sendHistory(WebSocketSession session, List<AccessLogEntry> entries) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "access_log_history");
        payload.put("entries", entries);
        sendJson(session, payload);
    }

    private void sendEntry(WebSocketSession session, AccessLogEntry entry) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "access_log_entry");
        payload.put("entry", entry);
        sendJson(session, payload);
    }

    private void sendError(WebSocketSession session, String code, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "access_log_error");
        payload.put("code", code);
        payload.put("message", message);
        sendJson(session, payload);
    }

    private void sendJson(WebSocketSession session, Object payload) {
        if (session == null || !session.isOpen()) return;
        try {
            synchronized (session) {
                if (session.isOpen()) session.sendMessage(new TextMessage(JSON.toJSONString(payload)));
            }
        } catch (IOException e) {
            log.debug("Access log WebSocket send failed: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        cleanup(session);
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanup(session);
    }

    void cleanup(WebSocketSession session) {
        Object closedValue = session.getAttributes().get(ATTR_CLOSED);
        if (closedValue instanceof AtomicBoolean) ((AtomicBoolean) closedValue).set(true);
        Object followSession = session.getAttributes().remove(ATTR_FOLLOW_SESSION);
        if (followSession instanceof Session) {
            try { ((Session) followSession).close(); } catch (IOException ignored) { }
        }
        Object ssh = session.getAttributes().remove(ATTR_SSH);
        if (ssh instanceof SSHClient) {
            try { ((SSHClient) ssh).close(); } catch (IOException ignored) { }
        }
    }

    private static String readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String errorCode(Exception e) {
        return e instanceof AccessLogException ? ((AccessLogException) e).code : "MONITOR_FAILED";
    }

    private static String friendlyMessage(Exception e) {
        if (e instanceof AccessLogException || e instanceof IllegalArgumentException) return safeMessage(e);
        return "访问日志监控启动失败: " + safeMessage(e);
    }

    private static String safeMessage(Throwable e) {
        return e.getMessage() == null || e.getMessage().trim().isEmpty() ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static void closeQuietly(WebSocketSession session, CloseStatus status) {
        if (session == null || !session.isOpen()) return;
        try { session.close(status); } catch (IOException ignored) { }
    }

    private static final class MonitorScope {
        private final Long instanceId;
        private final String wantedTag;
        private final Map<String, ProxyNode> nodesByTag = new HashMap<>();
        private final Map<Long, String> customerNames = new HashMap<>();

        private MonitorScope(Long instanceId, String wantedTag) {
            this.instanceId = instanceId;
            this.wantedTag = wantedTag;
        }
    }

    private static final class AccessLogException extends IOException {
        private static final long serialVersionUID = 1L;
        private final String code;

        private AccessLogException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
