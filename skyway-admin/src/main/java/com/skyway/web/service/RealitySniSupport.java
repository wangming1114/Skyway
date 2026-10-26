package com.skyway.web.service;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.skyway.resource.domain.ProxyNode;

/** Validation and JSON transformations shared by HTTP and WebSocket node operations. */
public final class RealitySniSupport {
    private static final Pattern DOMAIN = Pattern.compile(
            "(?i)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+");

    private RealitySniSupport() {}

    /** A missing parameter preserves legacy and batch creation behavior. */
    public static String requestedSni(Map<String, ?> body, String nodeType) {
        if (body == null || !body.containsKey("sni")) return null;
        if (!"VLESS-REALITY".equals(nodeType)) {
            throw new IllegalArgumentException("当前仅 VLESS-REALITY 支持 SNI 设置");
        }
        return validate(body.get("sni"));
    }

    public static String validate(Object raw) {
        String sni = raw instanceof String ? ((String) raw).trim() : "";
        if (sni.length() > 253 || !DOMAIN.matcher(sni).matches() || sni.matches("[0-9.]+")) {
            throw new IllegalArgumentException("请输入有效的 SNI 域名，不含协议、路径或端口");
        }
        return sni;
    }

    public static String readSni(ProxyNode node) {
        try {
            JSONObject config = JSON.parseObject(node.getConfigJson());
            String sni = config != null ? config.getString("sni") : null;
            if (sni != null && !sni.trim().isEmpty()) return sni.trim();
        } catch (RuntimeException ignored) {}
        try {
            String query = new URI(node.getUrl()).getRawQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    String[] pair = part.split("=", 2);
                    if (pair.length == 2 && "sni".equals(pair[0])) return URLDecoder.decode(pair[1], "UTF-8").trim();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static String withSni(String configJson, String sni) {
        JSONObject config = configJson == null || configJson.trim().isEmpty()
                ? new JSONObject() : JSON.parseObject(configJson);
        if (config == null) throw new IllegalArgumentException("节点配置无效");
        config.put("sni", validate(sni));
        return config.toJSONString();
    }

    public static String patchInbound(String original, ProxyNode node, String sni) {
        sni = validate(sni);
        JSONObject root = JSON.parseObject(original);
        JSONArray inbounds = root != null ? root.getJSONArray("inbounds") : null;
        String credential = null;
        if (node.getConfigJson() != null) {
            JSONObject config = JSON.parseObject(node.getConfigJson());
            credential = config != null ? config.getString("id") : null;
        }
        JSONObject matched = null;
        if (inbounds != null) {
            for (int i = 0; i < inbounds.size(); i++) {
                JSONObject inbound = inbounds.getJSONObject(i);
                if (inbound == null || !"vless".equals(inbound.getString("type"))
                        || !node.getPort().equals(inbound.getInteger("listen_port"))) continue;
                JSONObject tls = inbound.getJSONObject("tls");
                JSONObject reality = tls != null ? tls.getJSONObject("reality") : null;
                if (tls == null || !tls.getBooleanValue("enabled") || reality == null
                        || !reality.getBooleanValue("enabled")) continue;
                if (credential != null && !credential.isEmpty() && !hasUser(inbound, credential)) continue;
                if (matched != null) throw new IllegalArgumentException("匹配到多个 VLESS-REALITY 入站");
                matched = inbound;
            }
        }
        if (matched == null) throw new IllegalArgumentException("未找到匹配的 VLESS-REALITY 入站");
        JSONObject tls = matched.getJSONObject("tls");
        JSONObject handshake = tls.getJSONObject("reality").getJSONObject("handshake");
        if (handshake == null) throw new IllegalArgumentException("REALITY 配置缺少 handshake");
        tls.put("server_name", sni);
        handshake.put("server", sni);
        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }

    private static boolean hasUser(JSONObject inbound, String credential) {
        JSONArray users = inbound.getJSONArray("users");
        if (users != null) {
            for (int i = 0; i < users.size(); i++) {
                if (credential.equals(users.getJSONObject(i).getString("uuid"))) return true;
            }
        }
        return false;
    }
}
