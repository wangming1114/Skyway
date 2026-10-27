package com.skyway.resource.domain;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSONObject;

/** Lossless conversion between sb output, stored client parameters and share URIs. */
public final class Hysteria2Support {
    private static final Pattern URL = Pattern.compile("(?i)((?:hysteria2|hy2)://[^\\s\\u001b]+)");
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");

    private Hysteria2Support() {}

    public static ProxyNode parseSbOutput(String output, int expectedPort) {
        if (output == null) return null;
        Matcher matcher = URL.matcher(ANSI.matcher(output).replaceAll(""));
        if (!matcher.find()) return null;
        try {
            URI uri = new URI(matcher.group(1));
            String password = decode(uri.getRawUserInfo());
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 443 : uri.getPort();
            if (password.isEmpty() || host == null || port != expectedPort) return null;
            JSONObject params = new JSONObject();
            if (uri.getRawQuery() != null) {
                for (String part : uri.getRawQuery().split("&")) {
                    String[] pair = part.split("=", 2);
                    if (pair.length == 2) params.put(decode(pair[0]), decode(pair[1]));
                }
            }
            JSONObject config = new JSONObject();
            config.put("protocol", "hysteria2");
            config.put("network", "udp");
            config.put("security", "tls");
            config.put("password", password);
            config.put("alpn", params.getString("alpn") == null ? "h3" : params.getString("alpn"));
            String insecure = params.containsKey("insecure") ? params.getString("insecure") : params.getString("allowInsecure");
            config.put("insecure", "1".equals(insecure) || "true".equalsIgnoreCase(insecure));
            String pin = params.getString("pinSHA256");
            if (pin != null && !pin.isEmpty()) {
                pin = pin.replace(":", "").toUpperCase(java.util.Locale.ROOT);
                if (!pin.matches("[A-F0-9]{64}")) return null;
                config.put("pinSHA256", pin);
            }
            ProxyNode node = new ProxyNode();
            node.setNodeType(ProxyNodeProtocol.HYSTERIA2);
            node.setAddress(host.replaceAll("^\\[|\\]$", ""));
            node.setPort(port);
            node.setNodeName("Hysteria2-" + port);
            node.setConfigJson(config.toJSONString());
            node.setUrl(buildShareUrl(node, config));
            return node;
        } catch (Exception e) {
            return null;
        }
    }

    public static String buildShareUrl(ProxyNode node, JSONObject config) {
        String password = config.getString("password");
        if (password == null || password.isEmpty() || node.getAddress() == null || node.getPort() == null) return null;
        String host = node.getAddress().replaceAll("^\\[|\\]$", "");
        if (host.contains(":")) host = "[" + host + "]";
        Object alpn = config.get("alpn");
        String alpnValue = alpn instanceof java.util.Collection
                ? String.join(",", config.getJSONArray("alpn").toJavaList(String.class))
                : (alpn == null ? "h3" : alpn.toString());
        StringBuilder url = new StringBuilder("hysteria2://").append(encode(password))
                .append('@').append(host).append(':').append(node.getPort())
                .append("?alpn=").append(encode(alpnValue));
        boolean insecure = config.containsKey("insecure") ? config.getBooleanValue("insecure") : config.getBooleanValue("allowInsecure");
        url.append("&insecure=").append(insecure ? '1' : '0').append("&allowInsecure=").append(insecure ? '1' : '0');
        String pin = config.getString("pinSHA256");
        if (pin != null && !pin.isEmpty()) url.append("&pinSHA256=").append(encode(pin));
        if (node.getNodeName() != null) url.append('#').append(encode(node.getNodeName()));
        return url.toString();
    }

    private static String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    private static String decode(String value) throws Exception {
        return value == null ? "" : URLDecoder.decode(value.replace("+", "%2B"), "UTF-8");
    }
}
