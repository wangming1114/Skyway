package com.skyway.web.service.ws;

import com.alibaba.fastjson2.*;
import com.skyway.resource.domain.*;
import com.skyway.web.service.VpsSshCommandService;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class WsRemote {
    private final byte[] script;
    private final VpsSshCommandService sshService;

    public WsRemote(VpsSshCommandService sshService) throws IOException {
        this.sshService = sshService;
        try (InputStream in = new ClassPathResource("ws/vless-ws.sh").getInputStream()) {
            script = StreamUtils.copyToByteArray(in);
        }
    }

    SSHClient connect(long instance) throws IOException {
        return sshService.openSshClient(instance);
    }

    public void ensureCounters(ProxyNode node) throws IOException {
        try (SSHClient ssh = connect(node.getInstanceId())) {
            run(ssh, "counters", node.getListenPort().toString());
        }
    }

    String run(SSHClient ssh, String action, String... args) throws IOException {
        StringBuilder command = new StringBuilder("bash -s -- ").append(quote(action));
        for (String arg : args) command.append(' ').append(quote(arg));
        try (Session session = ssh.startSession();
                Session.Command remote = session.exec(command + " 2>&1")) {
            try (OutputStream stdin = remote.getOutputStream()) {
                stdin.write(script);
            }
            remote.join(180, TimeUnit.SECONDS);
            if (remote.getExitStatus() == null) throw new IOException("VPS 执行超时 (" + action + ")");
            String output =
                    StreamUtils.copyToString(remote.getInputStream(), StandardCharsets.UTF_8);
            if (remote.getExitStatus() != 0) {
                // sb output and remote configuration may contain node credentials.
                String detail =
                        "preflight".equals(action) && output.contains("其他服务占用")
                                ? "，公网端口已被其他服务占用"
                                : "";
                throw new IOException(
                        "VPS 执行失败 (" + action + ", exit=" + remote.getExitStatus() + ")" + detail);
            }
            return output;
        }
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    public static JSONObject generated(String output, ProxyNode node) {
        String clean = output.replaceAll("\\x1b\\[[0-9;]*[A-Za-z]", "");
        int start = clean.indexOf('{'), end = clean.lastIndexOf('}');
        if (start < 0 || end < start) throw new IllegalArgumentException("sb gen vws 未返回入站配置");
        JSONObject root = JSON.parseObject(clean.substring(start, end + 1));
        JSONObject inbound = root.getJSONArray("inbounds").getJSONObject(0);
        JSONObject config = JSON.parseObject(node.getConfigJson());
        JSONObject transport = inbound.getJSONObject("transport");
        if (!"vless".equals(inbound.getString("type"))
                || !"ws".equals(transport.getString("type"))
                || !config.getString("id")
                        .equals(inbound.getJSONArray("users").getJSONObject(0).getString("uuid"))
                || !config.getString("path").equals(transport.getString("path")))
            throw new IllegalArgumentException("sb gen vws 返回的 UUID、路径或协议不匹配");
        JSONObject headers = new JSONObject();
        headers.put("Host", "localhost");
        transport.put("headers", headers);
        inbound.put("listen", "127.0.0.1");
        inbound.put("listen_port", node.getListenPort());
        inbound.put("tag", node.getInboundTag());
        return root;
    }

    void inbound(SSHClient ssh, ProxyNode node, ProxyNodeDomainWhitelist policy)
            throws IOException {
        JSONObject config = JSON.parseObject(node.getConfigJson());
        String output =
                run(
                        ssh,
                        "generate",
                        node.getAddress(),
                        config.getString("id"),
                        config.getString("path"));
        String json =
                VpsSshCommandService.ensureNodeScopedDirectRouting(
                        generated(output, node).toJSONString());
        JSONObject relay = config.getJSONObject("relay");
        if (relay != null) {
            VpsSshCommandService.Socks5RelayConfig socks =
                    new VpsSshCommandService.Socks5RelayConfig(
                            relay.getString("server"),
                            relay.getIntValue("serverPort"),
                            relay.getString("username"),
                            relay.getString("password"));
            json =
                    VpsSshCommandService.upsertSocks5RelayToSingBoxConfig(
                            json, socks, "SOCKS-WS-" + node.getWsKey());
        }
        if (policy != null)
            json =
                    VpsSshCommandService.applyDomainPolicyToSingBoxConfig(
                            json, policy.getDomains(), policy.getMode(), true);
        run(
                ssh,
                "inbound",
                node.getNodeName(),
                node.getStatus(),
                encode(JSON.toJSONString(JSON.parseObject(json), JSONWriter.Feature.PrettyFormat)));
    }

    void site(SSHClient ssh, WsStore.Domain d, ProxyNode node) throws IOException {
        run(
                ssh,
                "site",
                d.id,
                d.host,
                node.getPort().toString(),
                node.getListenPort().toString(),
                JSON.parseObject(node.getConfigJson()).getString("path"));
    }

    void probe(SSHClient ssh, WsStore.Domain d, ProxyNode node) throws IOException {
        run(
                ssh,
                "probe",
                d.host,
                node.getPort().toString(),
                JSON.parseObject(node.getConfigJson()).getString("path"),
                "0".equals(node.getStatus()) ? "1" : "0",
                d.id);
    }

    void probePublic(ProxyNode node, String origin, boolean proxied) throws IOException {
        if (!"0".equals(node.getStatus())) return;
        String host = node.getAddress();
        int port = node.getPort();
        String path = JSON.parseObject(node.getConfigJson()).getString("path");
        try (Socket connection = new Socket()) {
            connection.connect(new InetSocketAddress(proxied ? host : origin, port), 8000);
            connection.setSoTimeout(8000);
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket tls = (SSLSocket) factory.createSocket(connection, host, port, true)) {
                SSLParameters parameters = tls.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                parameters.setServerNames(Collections.singletonList(new SNIHostName(host)));
                tls.setSSLParameters(parameters);
                tls.startHandshake();
                String request =
                        "GET "
                                + path
                                + " HTTP/1.1\r\nHost: "
                                + host
                                + ":"
                                + port
                                + "\r\nConnection: Upgrade\r\nUpgrade: websocket\r\nSec-WebSocket-Version: 13"
                                + "\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n";
                tls.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
                tls.getOutputStream().flush();
                BufferedReader response =
                        new BufferedReader(
                                new InputStreamReader(
                                        tls.getInputStream(), StandardCharsets.US_ASCII));
                String status = response.readLine();
                if (status == null || !status.matches("HTTP/1\\.1 101(?: .*)?"))
                    throw new IOException("公网端点未接受 WebSocket 升级");
                boolean accepted = false, edge = false;
                for (String line; (line = response.readLine()) != null && !line.isEmpty(); ) {
                    int separator = line.indexOf(':');
                    if (separator < 1) continue;
                    String key = line.substring(0, separator),
                            value = line.substring(separator + 1).trim();
                    if ("Sec-WebSocket-Accept".equalsIgnoreCase(key))
                        accepted = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=".equals(value);
                    if ("CF-Ray".equalsIgnoreCase(key) && !value.isEmpty()) edge = true;
                }
                if (!accepted || (proxied && !edge)) throw new IOException("缺少预期的 WebSocket 握手响应");
            }
        } catch (IOException error) {
            throw new IOException(
                    proxied
                            ? "公网橙云 WebSocket 握手失败，请检查解析、端口和 HTTPS 回源"
                            : "管理端连接源站公网 TLS 或 WebSocket 失败，请检查公网端口与证书",
                    error);
        }
    }
}
