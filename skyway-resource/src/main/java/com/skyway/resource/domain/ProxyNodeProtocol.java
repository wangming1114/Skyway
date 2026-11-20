package com.skyway.resource.domain;

/** Protocol capabilities shared by node creation and traffic collection. */
public final class ProxyNodeProtocol {
    public static final String WS_TLS = "VLESS-WS-TLS";
    public static final String HYSTERIA2 = "Hysteria2";

    private ProxyNodeProtocol() {}

    public static boolean isSupported(String type) {
        return "VLESS-REALITY".equals(type) || "VMess-TCP".equals(type) || HYSTERIA2.equals(type) || WS_TLS.equals(type);
    }

    public static boolean supportsRelay(String type) {
        return "VLESS-REALITY".equals(type) || HYSTERIA2.equals(type) || WS_TLS.equals(type);
    }

    public static String transport(String type) {
        return HYSTERIA2.equals(type) ? "udp" : "tcp";
    }

    public static String createCommand(String type, int port) {
        if (!isSupported(type) || port < 1 || port > 65535) throw new IllegalArgumentException("协议或端口无效");
        if (WS_TLS.equals(type)) throw new IllegalArgumentException("WS-TLS 创建需要托管域名");
        if (HYSTERIA2.equals(type)) return "sb add hy2 " + port;
        return "printf '1\\n" + ("VMess-TCP".equals(type) ? "5" : "18") + "\\n" + port + "\\n' | sb";
    }
}
