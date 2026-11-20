package com.skyway.web.service.ws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.web.controller.resource.WsNodeController;
import com.skyway.web.service.VpsSshCommandService;
import com.skyway.web.websocket.AccessLogParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import net.schmizz.sshj.SSHClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;

/** Runs only against the dedicated, disposable Linux fixture with a private test CA. */
@EnabledIfEnvironmentVariable(
        named = "SKYWAY_WS_TEST_CONTAINER",
        matches = "skyway-ws-qa-[a-z0-9-]+")
class WsTransportIntegrationTest {
    @TempDir Path state;
    private final WsNodeService resolver = mock(WsNodeService.class);
    private final MockMvc subscriptions =
            MockMvcBuilders.standaloneSetup(new WsNodeController(resolver)).build();
    private final String container = System.getenv("SKYWAY_WS_TEST_CONTAINER");

    private String execute(String input, String... command) throws IOException {
        List<String> args = new ArrayList<>(Arrays.asList("docker", "exec", "-i", container));
        args.addAll(Arrays.asList(command));
        File output = File.createTempFile("ws-qa-", ".log");
        try {
            Process process =
                    new ProcessBuilder(args)
                            .redirectErrorStream(true)
                            .redirectOutput(output)
                            .start();
            try (OutputStream in = process.getOutputStream()) {
                in.write(input.getBytes(StandardCharsets.UTF_8));
            }
            try {
                if (!process.waitFor(90, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new IOException("容器命令超时");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            String text =
                    new String(
                            java.nio.file.Files.readAllBytes(output.toPath()),
                            StandardCharsets.UTF_8);
            if (process.exitValue() != 0)
                throw new IOException(command[0] + " exited " + process.exitValue() + ": " + text);
            return text;
        } finally {
            java.nio.file.Files.deleteIfExists(output.toPath());
        }
    }

    private String script(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    private WsRemote origin() throws IOException {
        String script = script("ws/vless-ws.sh");
        return new WsRemote(null) {
            @Override
            String run(SSHClient ignored, String action, String... params) throws IOException {
                List<String> args = new ArrayList<>(Arrays.asList("bash", "-s", "--", action));
                Collections.addAll(args, params);
                return execute(script, args.toArray(new String[0]));
            }
        };
    }

    private ProxyNode node(String host, int listen) {
        ProxyNode n = new ProxyNode();
        n.setWsKey(WsStore.uuid());
        n.setNodeName("VLESS-WS-TLS-" + n.getWsKey());
        n.setAddress(host);
        n.setPort(443);
        n.setListenPort(listen);
        n.setStatus("0");
        n.setConfigJson(
                JSONObject.of("id", WsStore.uuid(), "path", "/" + WsStore.uuid()).toJSONString());
        n.setSubscriptionToken((WsStore.uuid() + WsStore.uuid()).replace("-", ""));
        when(resolver.subscription(n.getSubscriptionToken())).thenReturn(n);
        return n;
    }

    private WsStore.Domain domain(String host) {
        WsStore.Domain d = new WsStore.Domain();
        d.id = WsStore.uuid();
        d.host = host;
        return d;
    }

    private WsCertificates authority(WsRemote remote) throws IOException {
        execute(script("ws/acme-dns-fixture.py"), "sh", "-c", "cat >/qa/acme-dns-fixture.py");
        execute(script("ws/acme-fixture.sh"), "bash", "-s");
        Path lego = state.resolve("lego");
        Files.write(lego, script("ws/lego-in-container.sh").getBytes(StandardCharsets.UTF_8));
        assertTrue(lego.toFile().setExecutable(true));
        CloudflareDns dns = mock(CloudflareDns.class);
        when(dns.token()).thenReturn("qa-dns-token");
        when(dns.root()).thenReturn("qa.test");
        return new WsCertificates(
                remote,
                dns,
                state.toString(),
                lego.toString(),
                "https://localhost:22451/acme/local/directory");
    }

    private void client(ProxyNode n, int port) throws Exception {
        String subscription =
                subscriptions
                        .perform(get(n.getSubscriptionPath()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String yaml =
                "log-level: debug\nhosts:\n  "
                        + n.getAddress()
                        + ": 127.0.0.1\nmixed-port: "
                        + port
                        + "\nexternal-controller: 127.0.0.1:"
                        + (port + 20)
                        + "\n"
                        + subscription;
        execute(
                yaml,
                "bash",
                "-ec",
                "cd /qa; cat > client-$1.yaml; mkdir -p data-$1; "
                        + "./bin/mihomo -t -d /qa -f /qa/client-$1.yaml; "
                        + "if [ -f pid-$1 ] && kill -0 $(cat pid-$1) 2>/dev/null && curl -fsS localhost:$2/version >/dev/null; then "
                        + "curl -fsS -X PUT -H 'Content-Type: application/json' -d '{\"path\":\"/qa/client-'$1'.yaml\"}' "
                        + "'http://127.0.0.1:'$2'/configs?force=true'; else "
                        + "./bin/mihomo -d /qa -f /qa/client-$1.yaml </dev/null >client-$1.log 2>&1 & "
                        + "echo $! >pid-$1; sleep .5; kill -0 $(cat pid-$1); fi",
                "qa",
                String.valueOf(port),
                String.valueOf(port + 20));
    }

    @Test
    void twoNodesShare443AndRemainIndividuallyReachableAndMetered() throws Exception {
        WsRemote remote = origin();
        ProxyNode a = node("ws-a.qa.test", 22401), b = node("ws-b.qa.test", 22402);
        WsStore.Domain first = domain(a.getAddress()),
                second = domain(b.getAddress()),
                renamed = domain("ws-renamed.qa.test");
        try {
            WsCertificates certificates = authority(remote);
            // Simulate an existing 233boy Caddy with the admin API disabled and a separate site
            // import.
            execute(
                    "",
                    "bash",
                    "-ec",
                    "mkdir -p /etc/caddy/sites; "
                            + "printf '{\n admin off\n auto_https disable_redirects\n}\nimport sites/*.conf\n' >/etc/caddy/Caddyfile; "
                            + "printf 'http://127.0.0.1:22419 {\n respond existing-site 200\n}\n' >/etc/caddy/sites/legacy.conf; "
                            + "rc-service caddy restart");
            String monitor =
                    (String)
                            ReflectionTestUtils.getField(
                                    VpsSshCommandService.class, "SINGBOX_SPEED_SCRIPT");
            execute(monitor, "bash", "-ec", "cat >/qa/speed.sh");
            execute(
                    script("ws/traffic-fixture.py"),
                    "bash",
                    "-ec",
                    "cat >/qa/http.py; "
                            + "python3 -I /qa/http.py </dev/null >/qa/http.log 2>&1 & echo $! >/qa/http.pid");
            for (ProxyNode n : Arrays.asList(a, b)) {
                WsStore.Domain d = n == a ? first : second;
                remote.run(null, "preflight", "443");
                certificates.ensure(null, d);
                remote.inbound(null, n, null);
                remote.site(null, d, n);
                remote.probe(null, d, n);
                remote.run(null, "counters", n.getListenPort().toString());
            }
            client(a, 22411);
            client(b, 22412);
            execute(
                    "",
                    "curl",
                    "-fsS",
                    "--max-time",
                    "10",
                    "--noproxy",
                    "",
                    "-x",
                    "socks5h://127.0.0.1:22411",
                    "http://127.0.0.1:22420/check");
            remote.run(null, "limit", "22401", "10", "8");
            remote.run(null, "limit", "22402", "20", "16");
            // A VPS reboot drops kernel filters. Collection restores the two persisted limits.
            execute("", "tc", "qdisc", "del", "dev", "lo", "clsact");
            remote.run(null, "counters", "22401");
            remote.run(null, "counters", "22402");
            execute(script("ws/check-traffic.sh"), "bash", "-s");
            String logs = execute("", "cat", "/qa/sing-box.log");
            for (ProxyNode n : Arrays.asList(a, b)) {
                assertTrue(
                        Arrays.stream(logs.split("\n"))
                                .anyMatch(
                                        line ->
                                                AccessLogParser.parse(line, n.getInboundTag())
                                                        != null),
                        "缺少节点访问日志 " + n.getInboundTag());
            }
            String oldCert = remote.run(null, "certificate", first.id);
            certificates.ensure(null, first, Instant.now().plusSeconds(4 * 3600));
            assertNotEquals(oldCert, remote.run(null, "certificate", first.id));
            remote.site(null, first, a);
            remote.probe(
                    null, first, a); // Live leaf fingerprint must match the renewed certificate.
            a.setAddress(renamed.host);
            a.setPort(8443);
            certificates.ensure(null, renamed);
            remote.site(null, renamed, a);
            remote.probe(null, renamed, a);
            remote.run(null, "remove-site", first.id);
            String pid = execute("", "cat", "/qa/pid-22411");
            client(a, 22411);
            assertEquals(pid, execute("", "cat", "/qa/pid-22411"));
            execute(
                    "",
                    "curl",
                    "-fsS",
                    "--noproxy",
                    "",
                    "-x",
                    "socks5h://127.0.0.1:22411",
                    "http://127.0.0.1:22420/check");
            remote.probe(null, second, b);
            assertEquals("existing-site", execute("", "curl", "-fsS", "http://127.0.0.1:22419"));
        } finally {
            for (ProxyNode n : Arrays.asList(a, b)) {
                remote.run(null, "unlimit", n.getListenPort().toString());
                remote.run(null, "uncount", n.getListenPort().toString());
                remote.run(null, "remove-inbound", n.getNodeName());
            }
            for (WsStore.Domain d : Arrays.asList(first, second, renamed))
                remote.run(null, "remove-site", d.id);
            execute(
                    "",
                    "bash",
                    "-c",
                    "for p in /qa/http.pid /qa/dns.pid /qa/authority.pid /qa/pid-2241{1,2}; do [ ! -f \"$p\" ] || kill \"$(cat \"$p\")\" 2>/dev/null || :; done");
        }
    }
}
