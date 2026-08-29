package com.skyway.web.websocket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Parses sing-box inbound destination log lines used by the access-log monitor.
 */
public final class AccessLogParser {

    private static final Pattern ACCESS_LINE = Pattern.compile(
            "^(\\S+)\\s+(\\S+)\\s+(\\S+).*?inbound/([^\\[]+)\\[([^\\]]+)\\]:\\s+\\[0\\] inbound connection to (.+)$");
    private static final Pattern PORT = Pattern.compile("^[0-9]{1,5}$");

    private AccessLogParser() {
    }

    public static AccessLogEntry parse(String rawLine, String wantedTag) {
        if (rawLine == null || rawLine.isEmpty()) {
            return null;
        }
        Matcher matcher = ACCESS_LINE.matcher(rawLine);
        if (!matcher.matches()) {
            return null;
        }
        String inboundTag = matcher.group(5);
        if (wantedTag != null && !wantedTag.isEmpty() && !wantedTag.equals(inboundTag)) {
            return null;
        }

        HostAndPort destination = splitHostAndPort(matcher.group(6).trim());
        AccessLogEntry entry = new AccessLogEntry();
        entry.setTimestamp(matcher.group(1) + " " + matcher.group(2) + " " + matcher.group(3));
        entry.setProtocol(matcher.group(4));
        entry.setInboundTag(inboundTag);
        entry.setDestinationHost(destination.host);
        entry.setDestinationPort(destination.port);
        entry.setRawLine(rawLine);
        return entry;
    }

    private static HostAndPort splitHostAndPort(String value) {
        if (value.startsWith("[")) {
            int closing = value.lastIndexOf("]:");
            if (closing > 0) {
                String portText = value.substring(closing + 2);
                if (isPort(portText)) {
                    return new HostAndPort(value.substring(1, closing), Integer.valueOf(portText));
                }
            }
        }
        int colon = value.lastIndexOf(':');
        if (colon > 0) {
            String portText = value.substring(colon + 1);
            if (isPort(portText)) {
                return new HostAndPort(value.substring(0, colon), Integer.valueOf(portText));
            }
        }
        return new HostAndPort(value, null);
    }

    private static boolean isPort(String value) {
        if (!PORT.matcher(value).matches()) {
            return false;
        }
        int port = Integer.parseInt(value);
        return port >= 1 && port <= 65535;
    }

    private static final class HostAndPort {
        private final String host;
        private final Integer port;

        private HostAndPort(String host, Integer port) {
            this.host = host;
            this.port = port;
        }
    }

    public static final class AccessLogEntry {
        private String timestamp;
        private String protocol;
        private String inboundTag;
        private Long nodeId;
        private String nodeName;
        private Integer nodePort;
        private Long customerId;
        private String customerName;
        private String destinationHost;
        private Integer destinationPort;
        private String rawLine;

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public String getInboundTag() { return inboundTag; }
        public void setInboundTag(String inboundTag) { this.inboundTag = inboundTag; }
        public Long getNodeId() { return nodeId; }
        public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
        public Integer getNodePort() { return nodePort; }
        public void setNodePort(Integer nodePort) { this.nodePort = nodePort; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getDestinationHost() { return destinationHost; }
        public void setDestinationHost(String destinationHost) { this.destinationHost = destinationHost; }
        public Integer getDestinationPort() { return destinationPort; }
        public void setDestinationPort(Integer destinationPort) { this.destinationPort = destinationPort; }
        @JSONField(serialize = false)
        public String getRawLine() { return rawLine; }
        public void setRawLine(String rawLine) { this.rawLine = rawLine; }
    }
}
