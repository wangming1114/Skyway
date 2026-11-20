package com.skyway.web.service.ws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.*;
import com.skyway.resource.service.*;
import com.skyway.system.service.CloudflareCredentials;
import com.skyway.system.service.ISysConfigService;
import com.skyway.web.controller.resource.WsNodeController;
import com.skyway.web.service.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;

/**
 * Explicit opt-in. Uses real Cloudflare and ACME with new names and an isolated Skyway database.
 */
@EnabledIfEnvironmentVariable(named = "SKYWAY_WS_LIVE_CONFIG", matches = ".+")
class WsPublicIntegrationTest {
    @TempDir Path state;
    private final List<String> intents = new ArrayList<>();
    private WsStore store;
    private WsNodeService service;
    private WsRemote remote;
    private WsCertificates certificates;
    private CloudflareDns dns;
    private MockMvc subscriptions;
    private String origin;
    private String mihomo;
    private String helper;
    private int echoPort;
    private final String run = "qa-" + UUID.randomUUID().toString().substring(0, 8);

    private void setup() throws Exception {
        Path configFile = Paths.get(System.getenv("SKYWAY_WS_LIVE_CONFIG"));
        JSONObject config =
                JSON.parseObject(
                        new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8));
        CloudflareCredentials credentials = new CloudflareCredentials(state.toString());
        ISysConfigService settings = mock(ISysConfigService.class);
        when(settings.selectConfigByKey("skyway.cloudflare.domain")).thenReturn("skyway666.sbs");
        when(settings.selectConfigByKey("skyway.cloudflare.zoneId"))
                .thenReturn(config.getString("zone"));
        when(settings.selectConfigByKey(CloudflareCredentials.TOKEN))
                .thenReturn(credentials.encrypt(config.getString("token")));
        dns = new CloudflareDns(settings, credentials, "https://api.cloudflare.com/client/v4");
        dns.zone();
        mihomo = Objects.requireNonNull(System.getenv("SKYWAY_WS_LIVE_MIHOMO"));
        String lego = Objects.requireNonNull(System.getenv("SKYWAY_WS_LIVE_LEGO"));
        VpsInstance instance = new VpsInstance();
        instance.setId(7L);
        instance.setIp(config.getString("host"));
        instance.setSshPort(config.getIntValue("port"));
        instance.setSshUsername(config.getString("username"));
        instance.setSshPassword(config.getString("password"));
        origin = instance.getIp();
        IVpsInstanceService instances = mock(IVpsInstanceService.class);
        when(instances.getById(7L)).thenReturn(instance);
        store = WsNodeLifecycleTest.memoryStore();
        VpsSshCommandService ssh = new VpsSshCommandService();
        ReflectionTestUtils.setField(ssh, "vpsInstanceService", instances);
        VpsPortAvailabilityService ports = new VpsPortAvailabilityService();
        ReflectionTestUtils.setField(ports, "vpsSshCommandService", ssh);
        ReflectionTestUtils.setField(ports, "proxyNodeService", store.nodeService());
        remote = new WsRemote(ssh);
        certificates =
                new WsCertificates(
                        remote,
                        dns,
                        state.toString(),
                        lego,
                        "https://acme-v02.api.letsencrypt.org/directory");
        String propagationWait = System.getenv("SKYWAY_WS_ACME_PROPAGATION_WAIT_SECONDS");
        if (propagationWait != null)
            ReflectionTestUtils.setField(
                    certificates, "propagationWaitSeconds", Integer.parseInt(propagationWait));
        service =
                new WsNodeService(
                        store,
                        dns,
                        remote,
                        certificates,
                        instances,
                        ports,
                        new VpsInstanceOperationCoordinator(),
                        new ProxyDomainWhitelistService(),
                        mock(IProxyNodeTrafficService.class),
                        mock(IProxyNodeRateLimitService.class));
        subscriptions = MockMvcBuilders.standaloneSetup(new WsNodeController(service)).build();
    }

    private ProxyNode change(String kind, Long node, JSONObject req) throws Exception {
        String id = WsStore.uuid();
        req.put("operationId", id);
        intents.add(id);
        service.submit(kind, 7, node, req, "qa");
        long until = System.nanoTime() + TimeUnit.MINUTES.toNanos(16);
        String previous = null;
        while (System.nanoTime() < until) {
            WsStore.Operation op = store.operation(id);
            if (!op.stage.equals(previous)) {
                System.out.println("[WS public] " + run + " " + kind + " " + op.stage);
                previous = op.stage;
            }
            if ("FAILED".equals(op.state)) fail(op.stage);
            if ("DONE".equals(op.state)) return store.nodeService().getById(op.node);
            if ("RETRY".equals(op.state)) service.recover();
            Thread.sleep(1000);
        }
        throw new IOException("Live operation timeout");
    }

    private String shell(SSHClient ssh, String command) throws IOException {
        try (Session session = ssh.startSession();
                Session.Command c = session.exec(command)) {
            String output =
                    StreamUtils.copyToString(c.getInputStream(), StandardCharsets.UTF_8).trim();
            c.join(30, TimeUnit.SECONDS);
            if (!Integer.valueOf(0).equals(c.getExitStatus()))
                throw new IOException("VPS fixture command failed");
            return output;
        }
    }

    private String execute(String... command) throws Exception {
        Path log = Files.createTempFile(state, "client-result", ".txt");
        Process p =
                new ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .redirectOutput(log.toFile())
                        .start();
        if (!p.waitFor(65, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("Client timeout");
        }
        if (p.exitValue() != 0) throw new IOException("Client exited " + p.exitValue());
        return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
    }

    private String edge(ProxyNode node) throws Exception {
        long until = System.nanoTime() + TimeUnit.MINUTES.toNanos(8);
        while (System.nanoTime() < until) {
            try {
                String response =
                        execute(
                                "curl",
                                "-fsS",
                                "--max-time",
                                "10",
                                "-H",
                                "Accept: application/dns-json",
                                "https://cloudflare-dns.com/dns-query?type=A&name="
                                        + node.getAddress());
                com.alibaba.fastjson2.JSONArray answers =
                        JSON.parseObject(response).getJSONArray("Answer");
                if (answers != null)
                    for (int i = 0; i < answers.size(); i++) {
                        JSONObject answer = answers.getJSONObject(i);
                        if (answer.getIntValue("type") != 1) continue;
                        String ip = CloudflareDns.origin(answer.getString("data"));
                        if (ip.equals(origin)) continue;
                        String head =
                                execute(
                                        "curl",
                                        "--noproxy",
                                        "*",
                                        "-sSI",
                                        "--max-time",
                                        "10",
                                        "--resolve",
                                        node.getAddress() + ":" + node.getPort() + ":" + ip,
                                        "https://" + node.getAddress() + ":" + node.getPort());
                        if (head.toLowerCase(Locale.ROOT).contains("cf-ray:")) return ip;
                    }
            } catch (IOException unavailable) {
                /* Allow the new edge record to propagate. */
            }
            Thread.sleep(4000);
        }
        throw new IOException("Cloudflare edge did not become reachable");
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private class Client implements AutoCloseable {
        final int port = freePort(), api = freePort();
        final Path directory = Files.createTempDirectory(state, "mihomo-");
        final Path file = directory.resolve("config.yaml");
        final String subscription;
        Process process;

        Client(ProxyNode node, String address) throws Exception {
            subscription = node.getSubscriptionPath();
            refresh(node, address);
            process =
                    new ProcessBuilder(mihomo, "-d", directory.toString(), "-f", file.toString())
                            .redirectErrorStream(true)
                            .redirectOutput(directory.resolve("client.log").toFile())
                            .start();
            try {
                for (int i = 0; i < 60; i++) {
                    try (Socket socket = new Socket("127.0.0.1", port)) {
                        return;
                    } catch (IOException starting) {
                        Thread.sleep(100);
                    }
                }
                throw new IOException("Mihomo startup failed");
            } catch (Exception e) {
                close();
                throw e;
            }
        }

        void refresh(ProxyNode node, String address) throws Exception {
            assertEquals(subscription, node.getSubscriptionPath());
            org.springframework.mock.web.MockHttpServletResponse res =
                    subscriptions.perform(get(subscription)).andReturn().getResponse();
            assertEquals(200, res.getStatus());
            String yaml =
                    "mixed-port: "
                            + port
                            + "\nexternal-controller: 127.0.0.1:"
                            + api
                            + "\nhosts:\n  "
                            + node.getAddress()
                            + ": "
                            + JSON.toJSONString(address)
                            + "\n"
                            + res.getContentAsString();
            Files.write(file, yaml.getBytes(StandardCharsets.UTF_8));
            if (process != null)
                execute(
                        "curl",
                        "-fsS",
                        "-X",
                        "PUT",
                        "-H",
                        "Content-Type: application/json",
                        "-d",
                        JSONObject.of("path", file.toString()).toJSONString(),
                        "http://127.0.0.1:" + api + "/configs?force=true");
        }

        void transfer() throws Exception {
            String address = "http://127.0.0.1:" + echoPort;
            Path body = directory.resolve("download");
            String proxy = "socks5h://127.0.0.1:" + port;
            execute(
                    "curl",
                    "--noproxy",
                    "",
                    "-fsS",
                    "--max-time",
                    "50",
                    "-x",
                    proxy,
                    "-o",
                    body.toString(),
                    address + "/download");
            assertEquals(1048576, Files.size(body));
            execute(
                    "curl",
                    "--noproxy",
                    "",
                    "-fsS",
                    "--max-time",
                    "50",
                    "-x",
                    proxy,
                    "--data-binary",
                    "@" + body,
                    address + "/upload");
        }

        @Override
        public void close() {
            if (process != null) process.destroyForcibly();
        }
    }

    private void checkRecordGone(WsStore.Domain d) throws Exception {
        // remove is idempotent. Verify independently by the exact ID, without issuing another
        // DELETE.
        HttpURLConnection c =
                (HttpURLConnection)
                        new URL(
                                        "https://api.cloudflare.com/client/v4/zones/"
                                                + d.zone
                                                + "/dns_records/"
                                                + d.record)
                                .openConnection();
        c.setRequestProperty("Authorization", "Bearer " + dns.token());
        c.setConnectTimeout(8000);
        c.setReadTimeout(15000);
        try {
            assertEquals(404, c.getResponseCode());
        } finally {
            c.disconnect();
        }
    }

    @Test
    void publicDirectOrangeRenewalRenameAndCleanup() throws Exception {
        setup();
        List<WsStore.Domain> owned = new ArrayList<>();
        ExecutorService transfers = Executors.newFixedThreadPool(2);
        try (SSHClient ssh = remote.connect(7)) {
            remote.run(ssh, "preflight", "443");
            helper = "/tmp/skyway-ws-" + run;
            String probe;
            try (InputStream in = new ClassPathResource("ws/traffic-fixture.py").getInputStream()) {
                probe = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            }
            shell(
                    ssh,
                    "umask 077; mkdir "
                            + helper
                            + " && cd "
                            + helper
                            + " && echo "
                            + WsRemote.encode(probe)
                            + " | base64 -d >echo.py");
            shell(
                    ssh,
                    "cd "
                            + helper
                            + " && (nohup python3 -I "
                            + helper
                            + "/echo.py 0 >port 2>log </dev/null & echo $! >pid)");
            for (int i = 0; i < 20 && echoPort == 0; i++) {
                String result = shell(ssh, "cat " + helper + "/port");
                if (!result.isEmpty()) echoPort = Integer.parseInt(result);
                else Thread.sleep(150);
            }
            assertTrue(echoPort > 0);
            ProxyNode a =
                    change("CREATE", null, JSONObject.of("customerId", 1, "subdomain", run + "-a"));
            ProxyNode b =
                    change("CREATE", null, JSONObject.of("customerId", 1, "subdomain", run + "-b"));
            WsStore.Domain first = store.domains(a.getWsKey()).get(0);
            owned.add(first);
            owned.add(store.domains(b.getWsKey()).get(0));
            assertFalse(first.proxied);
            assertEquals(a.getPort(), b.getPort());
            assertNotEquals(a.getListenPort(), b.getListenPort());
            try (Client left = new Client(a, origin);
                    Client right = new Client(b, origin)) {
                service.rate(a, 10, 8);
                service.rate(b, 20, 16);
                Future<?> one =
                        transfers.submit(
                                () -> {
                                    left.transfer();
                                    return null;
                                });
                Future<?> two =
                        transfers.submit(
                                () -> {
                                    right.transfer();
                                    return null;
                                });
                one.get();
                two.get();
                for (ProxyNode n : Arrays.asList(a, b)) {
                    String port = n.getListenPort().toString();
                    String bytes =
                            shell(
                                    ssh,
                                    "iptables -L NODE_TRAFFIC_IN -xnv | awk '$NF==\"dpt:"
                                            + port
                                            + "\" {print $2}'");
                    assertTrue(Long.parseLong(bytes) >= 1048576);
                    assertEquals(
                            "logged",
                            shell(
                                    ssh,
                                    "grep -F '["
                                            + n.getInboundTag()
                                            + "]' /var/log/sing-box/access.log | grep -q 'connection to' && echo logged"));
                }
                a = change("EDIT", a.getId(), JSONObject.of("cloudflareProxied", true));
                String edgeIp = edge(a);
                left.refresh(a, edgeIp);
                left.transfer();
                String oldCert = remote.run(ssh, "certificate", first.id);
                Instant renew =
                        certificates.ensure(ssh, first, Instant.now().plusSeconds(150 * 86400L));
                assertTrue(renew.isAfter(Instant.now()));
                remote.site(ssh, first, a);
                remote.probe(ssh, first, a);
                assertNotEquals(oldCert, remote.run(ssh, "certificate", first.id));
                left.transfer();
                String uuid = JSON.parseObject(a.getConfigJson()).getString("id");
                a =
                        change(
                                "EDIT",
                                a.getId(),
                                JSONObject.of("subdomain", run + "-new", "port", 8443));
                owned.add(store.domains(a.getWsKey()).get(0));
                checkRecordGone(first);
                assertEquals(uuid, JSON.parseObject(a.getConfigJson()).getString("id"));
                left.refresh(a, edge(a));
                left.transfer();
            }
        } finally {
            transfers.shutdownNow();
            try {
                for (ProxyNode n : store.nodeService().selectList(new ProxyNode())) {
                    // Limits were applied directly in this fixture with no business rate-limit
                    // rows.
                    service.rate(n, 0, 0);
                    service.delete(n, false, "qa");
                }
                for (String id : intents) {
                    WsStore.Domain d = store.allocated(id);
                    if (d != null) {
                        owned.add(d);
                        dns.remove(d);
                        try (SSHClient ssh = remote.connect(7)) {
                            remote.run(ssh, "remove-site", d.id);
                            if (d.key.equals(id))
                                remote.run(ssh, "remove-inbound", "VLESS-WS-TLS-" + id);
                        }
                    }
                }
                for (WsStore.Domain d : owned) if (d.record != null) checkRecordGone(d);
            } finally {
                if (helper != null)
                    try (SSHClient ssh = remote.connect(7)) {
                        shell(
                                ssh,
                                "cd "
                                        + helper
                                        + " && if [ -f pid ] && grep -Fqa '"
                                        + helper
                                        + "/echo.py' /proc/$(cat pid)/cmdline; "
                                        + "then kill $(cat pid) 2>/dev/null || :; fi; cd "
                                        + helper
                                        + " && rm -f echo.py port log pid && rmdir "
                                        + helper);
                    }
                service.close();
            }
        }
        System.out.println(
                "[WS public] "
                        + run
                        + " public verification complete; "
                        + owned.size()
                        + " record IDs returned 404");
    }
}
