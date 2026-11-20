package com.skyway.web.service.ws;

import com.alibaba.fastjson2.*;
import com.skyway.system.service.CloudflareCredentials;
import com.skyway.system.service.ISysConfigService;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudflareDns {
    private final ISysConfigService config;
    private final CloudflareCredentials secrets;
    private final String endpoint;

    public CloudflareDns(
            ISysConfigService config,
            CloudflareCredentials secrets,
            @Value("${skyway.ws.cloudflare-api:https://api.cloudflare.com/client/v4}")
                    String endpoint) {
        this.config = config;
        this.secrets = secrets;
        this.endpoint = endpoint;
    }

    public String root() {
        return config.selectConfigByKey("skyway.cloudflare.domain")
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
    }

    public String token() {
        return secrets.decrypt(config.selectConfigByKey(CloudflareCredentials.TOKEN));
    }

    public String zone() throws IOException {
        String id = config.selectConfigByKey("skyway.cloudflare.zoneId").trim();
        if (!id.matches("[a-fA-F0-9]{32}"))
            throw new IllegalArgumentException("请在参数设置中填写有效的 Cloudflare Zone ID");
        JSONObject zone = request("GET", "/zones/" + id, null).getJSONObject("result");
        if (!root().equalsIgnoreCase(zone.getString("name")))
            throw new IllegalArgumentException("Zone ID 与配置的根域名不匹配");
        return id;
    }

    public static String origin(String value) {
        try {
            if (value == null
                    || !(value.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")
                            || value.contains(":") && value.matches("[a-fA-F0-9:.]+")))
                throw new IllegalArgumentException();
            InetAddress ip = InetAddress.getByName(value);
            if (ip.isAnyLocalAddress()
                    || ip.isLoopbackAddress()
                    || ip.isSiteLocalAddress()
                    || ip.isLinkLocalAddress()
                    || ip.isMulticastAddress()
                    || (ip.getAddress().length == 16 && (ip.getAddress()[0] & 0xfe) == 0xfc))
                throw new IllegalArgumentException();
            return ip.getHostAddress();
        } catch (Exception ex) {
            throw new IllegalArgumentException("VPS 需要有效的公网 IPv4 或 IPv6 地址");
        }
    }

    public static class Conflict extends IllegalArgumentException {
        public Conflict() {
            super("域名已有其他 DNS 记录，请更换子域名");
        }
    }

    private String records(WsStore.Domain d) {
        return "/zones/" + d.zone + "/dns_records";
    }

    private String marker(WsStore.Domain d) {
        return "Skyway node " + d.id;
    }

    private JSONObject owned(WsStore.Domain domain, boolean creating) throws IOException {
        JSONArray list =
                request("GET", records(domain) + "?name=" + domain.host + "&per_page=100", null)
                        .getJSONArray("result");
        JSONObject owned = null;
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            if (!domain.host.equalsIgnoreCase(item.getString("name"))) continue;
            if (marker(domain).equals(item.getString("comment"))
                    && (domain.record == null || domain.record.equals(item.getString("id")))) {
                if (owned != null) throw new IOException("发现多个相同归属的记录，请检查 Cloudflare");
                owned = item;
            } else if (creating || item.getString("id").equals(domain.record)) {
                throw new Conflict();
            }
        }
        return owned;
    }

    public String apply(WsStore.Domain domain, String ip, boolean proxied) throws IOException {
        JSONObject data = owned(domain, true);
        String type = ip.contains(":") ? "AAAA" : "A";
        if (data != null && matches(data, ip, proxied, type)) return data.getString("id");
        JSONObject body = new JSONObject();
        body.put("name", domain.host);
        body.put("content", ip);
        body.put("type", type);
        body.put("ttl", 1);
        body.put("proxied", proxied);
        body.put("comment", marker(domain));
        if (data == null && domain.record != null)
            throw new IOException("托管记录已在 Cloudflare 外部更改，请检查记录 ID");
        String path = records(domain) + (data == null ? "" : "/" + data.getString("id"));
        JSONObject result =
                request(data == null ? "POST" : "PUT", path, body).getJSONObject("result");
        if (!domain.host.equalsIgnoreCase(result.getString("name"))
                || !matches(result, ip, proxied, type)
                || !marker(domain).equals(result.getString("comment"))
                || result.getString("id") == null)
            throw new IOException("Cloudflare 尚未确认 DNS 记录内容");
        return result.getString("id");
    }

    private boolean matches(JSONObject data, String ip, boolean proxied, String type)
            throws IOException {
        return type.equals(data.getString("type"))
                && Boolean.valueOf(proxied).equals(data.getBoolean("proxied"))
                && Arrays.equals(
                        InetAddress.getByName(ip).getAddress(),
                        InetAddress.getByName(data.getString("content")).getAddress());
    }

    public void remove(WsStore.Domain domain) throws IOException {
        String record = domain.record;
        if (record == null) {
            // POST may have succeeded before the worker could persist the response.
            JSONObject item = owned(domain, false);
            if (item == null) return;
            record = item.getString("id");
        }
        String path = records(domain) + "/" + record;
        JSONObject current = byId(path);
        if (current == null) return;
        if (!record.equals(current.getString("id"))
                || !marker(domain).equals(current.getString("comment"))
                || !domain.host.equalsIgnoreCase(current.getString("name"))) throw new Conflict();
        try {
            request("DELETE", path, null);
        } catch (HttpFailure error) {
            if (error.status != 404) throw error;
        }
        if (byId(path) != null) throw new IOException("Cloudflare 尚未确认删除 DNS");
    }

    private JSONObject byId(String path) throws IOException {
        try {
            return request("GET", path, null).getJSONObject("result");
        } catch (HttpFailure failure) {
            if (failure.status == 404) return null;
            throw failure;
        }
    }

    private static class HttpFailure extends IOException {
        final int status;

        HttpFailure(int status) {
            super("Cloudflare DNS API HTTP " + status);
            this.status = status;
        }
    }

    private JSONObject request(String method, String path, JSONObject body) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(endpoint + path).openConnection();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + token());
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new HttpFailure(status);
            try (InputStream in = connection.getInputStream()) {
                JSONObject result = JSON.parseObject(in, JSONObject.class);
                if (result == null || !result.getBooleanValue("success"))
                    throw new IOException("Cloudflare DNS API 操作失败");
                return result;
            }
        } finally {
            connection.disconnect();
        }
    }
}
