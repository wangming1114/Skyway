package com.skyway.web.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.skyway.common.exception.ServiceException;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.web.service.VpsSshCommandService.RemotePortScan;

/** Combines persisted and live VPS state when selecting or validating proxy ports. */
@Service
public class VpsPortAvailabilityService {

    public static final int MIN_RECOMMENDED_PORT = 10000;
    public static final int MAX_PORT = 65535;

    @Autowired
    private IProxyNodeService proxyNodeService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    public PortRecommendation recommend(Long instanceId, Set<Integer> excludedPorts) {
        Set<Integer> databaseUnavailable = databasePorts(instanceId);
        databaseUnavailable.addAll(sanitizePorts(excludedPorts));
        try {
            RemotePortScan scan = vpsSshCommandService.scanRemotePorts(instanceId);
            if (!scan.isComplete()) {
                return fallbackRecommendation(databaseUnavailable,
                        "服务器端口扫描不完整（" + String.join("、", scan.getMissingSources()) + "），已退回数据库推荐");
            }
            Set<Integer> unavailable = new LinkedHashSet<>(databaseUnavailable);
            unavailable.addAll(scan.getUnavailablePorts());
            Integer port = selectRecommendedPort(unavailable, scan.getEphemeralStart(), scan.getEphemeralEnd());
            if (port == null) {
                throw new ServiceException("当前 VPS 没有可推荐的端口");
            }
            return new PortRecommendation(port, true, null);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            String detail = e.getMessage() != null && !e.getMessage().trim().isEmpty() ? "：" + e.getMessage().trim() : "";
            return fallbackRecommendation(databaseUnavailable, "SSH 扫描失败，已退回数据库推荐" + detail);
        }
    }

    /**
     * Rechecks an automatically recommended port while the caller holds the VPS operation lock.
     * The requested port is retained when it is still available; otherwise a fresh port is selected.
     */
    public int resolveAutoPortForCreate(Long instanceId, Integer requestedPort) {
        Set<Integer> unavailable = databasePorts(instanceId);
        RemotePortScan scan = completeScanForCreate(instanceId, requestedPort);
        unavailable.addAll(scan.getUnavailablePorts());
        if (isValidPort(requestedPort)
                && !unavailable.contains(requestedPort)
                && !isInRange(requestedPort, scan.getEphemeralStart(), scan.getEphemeralEnd())) {
            return requestedPort;
        }
        Integer replacement = selectRecommendedPort(unavailable, scan.getEphemeralStart(), scan.getEphemeralEnd());
        if (replacement == null) {
            throw new ServiceException("当前 VPS 没有可用于创建节点的端口");
        }
        return replacement;
    }

    public void assertAvailableForCreate(Long instanceId, int port) {
        if (port < 1 || port > MAX_PORT) {
            throw new ServiceException("端口范围为 1-65535");
        }
        ProxyNode existing = proxyNodeService.getByInstanceIdAndPort(instanceId, port);
        if (existing != null) {
            throw new ServiceException("端口 " + port + " 已被当前 VPS 的代理节点使用");
        }
        RemotePortScan scan = completeScanForCreate(instanceId, port);
        if (scan.getUnavailablePorts().contains(port)) {
            String sources = scan.describeSources(port);
            throw new ServiceException("端口 " + port + " 已被占用" + (sources.isEmpty() ? "" : "（" + sources + "）"));
        }
    }

    private RemotePortScan completeScanForCreate(Long instanceId, Integer port) {
        final RemotePortScan scan;
        try {
            scan = vpsSshCommandService.scanRemotePorts(instanceId);
        } catch (Exception e) {
            throw new ServiceException("无法验证端口" + portLabel(port) + "的服务器占用情况："
                    + (e.getMessage() != null ? e.getMessage() : "SSH 扫描失败"));
        }
        if (!scan.isComplete()) {
            throw new ServiceException("无法完整验证端口" + portLabel(port) + "："
                    + String.join("、", scan.getMissingSources()));
        }
        return scan;
    }

    private PortRecommendation fallbackRecommendation(Set<Integer> unavailable, String warning) {
        Integer port = selectRecommendedPort(unavailable, null, null);
        if (port == null) {
            throw new ServiceException("当前 VPS 没有可推荐的端口");
        }
        return new PortRecommendation(port, false, warning);
    }

    private static String portLabel(Integer port) {
        return isValidPort(port) ? " " + port + " " : " ";
    }

    private static boolean isValidPort(Integer port) {
        return port != null && port >= 1 && port <= MAX_PORT;
    }

    private static boolean isInRange(int port, Integer start, Integer end) {
        return start != null && end != null && port >= start && port <= end;
    }

    public Set<Integer> parseExcludedPorts(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> ports = new LinkedHashSet<>();
        String[] parts = text.split(",");
        if (parts.length > 2000) {
            throw new ServiceException("排除端口数量过多");
        }
        for (String part : parts) {
            String value = part != null ? part.trim() : "";
            if (value.isEmpty()) continue;
            try {
                int port = Integer.parseInt(value);
                if (port < 1 || port > MAX_PORT) {
                    throw new NumberFormatException();
                }
                ports.add(port);
            } catch (NumberFormatException e) {
                throw new ServiceException("无效的排除端口: " + value);
            }
        }
        return ports;
    }

    Set<Integer> databasePorts(Long instanceId) {
        List<Integer> ports = proxyNodeService.listUsedPorts(instanceId);
        return sanitizePorts(ports != null ? new LinkedHashSet<>(ports) : Collections.emptySet());
    }

    static Set<Integer> sanitizePorts(Set<Integer> ports) {
        if (ports == null || ports.isEmpty()) return new LinkedHashSet<>();
        return ports.stream()
                .filter(port -> port != null && port >= 1 && port <= MAX_PORT)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Integer selectRecommendedPort(Set<Integer> unavailable, Integer ephemeralStart, Integer ephemeralEnd) {
        Set<Integer> blocked = unavailable != null ? unavailable : Collections.emptySet();
        for (int port = MIN_RECOMMENDED_PORT; port <= MAX_PORT; port++) {
            if (blocked.contains(port)) continue;
            if (ephemeralStart != null && ephemeralEnd != null
                    && port >= ephemeralStart && port <= ephemeralEnd) continue;
            return port;
        }
        return null;
    }

    public static final class PortRecommendation {
        private final int port;
        private final boolean verified;
        private final String warning;

        public PortRecommendation(int port, boolean verified, String warning) {
            this.port = port;
            this.verified = verified;
            this.warning = warning;
        }

        public int getPort() { return port; }
        public boolean isVerified() { return verified; }
        public String getWarning() { return warning; }
    }
}
