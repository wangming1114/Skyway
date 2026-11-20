package com.skyway.resource.domain;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Public WS endpoint and client formats. JSON quoting is also valid YAML scalar quoting. */
public final class VlessWsTls {
    private static final Pattern LABEL = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");

    private VlessWsTls() {}

    public static String hostname(String prefix, String zone) {
        String label = prefix.trim().toLowerCase(Locale.ROOT);
        String root = zone.trim().toLowerCase(Locale.ROOT);
        if (!LABEL.matcher(label).matches()
                || !root.contains(".")
                || label.length() + root.length() + 1 > 253)
            throw new IllegalArgumentException("请输入有效的一级子域名前缀");
        for (String part : root.split("\\.", -1))
            if (!LABEL.matcher(part).matches())
                throw new IllegalArgumentException("Cloudflare 根域名无效");
        return label + "." + root;
    }

    public static void checkPort(int port, boolean proxied) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("公网端口范围为 1–65535");
        if (proxied && !Arrays.asList(443, 2053, 2083, 2087, 2096, 8443).contains(port))
            throw new IllegalArgumentException("开启橙云时可使用 443、2053、2083、2087、2096 或 8443");
    }

    public static String share(ProxyNode node, JSONObject config) {
        String host = node.getAddress();
        return "vless://"
                + UUID.fromString(config.getString("id"))
                + "@"
                + host
                + ":"
                + node.getPort()
                + "?encryption=none&security=tls&type=ws&host="
                + host
                + "&sni="
                + host
                + "&path="
                + encode(config.getString("path"))
                + "#"
                + encode(node.getNodeName());
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static String mihomo(ProxyNode node) {
        JSONObject config = JSON.parseObject(node.getConfigJson());
        String name = JSON.toJSONString(node.getNodeName());
        String host = JSON.toJSONString(node.getAddress());
        return "mode: rule\nproxies:\n  - name: "
                + name
                + "\n    type: vless\n    server: "
                + host
                + "\n    port: "
                + node.getPort()
                + "\n    uuid: "
                + UUID.fromString(config.getString("id"))
                + "\n    network: ws\n    tls: true\n    skip-cert-verify: false\n    udp: true\n    servername: "
                + host
                + "\n    ws-opts:\n      path: "
                + JSON.toJSONString(config.getString("path"))
                + "\n      headers:\n        Host: "
                + host
                + "\nproxy-groups:\n  - name: Skyway\n"
                + "    type: select\n    proxies:\n      - "
                + name
                + "\nrules:\n  - MATCH,Skyway\n";
    }
}
