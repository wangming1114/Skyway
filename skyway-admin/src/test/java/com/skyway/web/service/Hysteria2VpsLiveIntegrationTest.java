package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson2.*;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.core.domain.entity.SysUser;
import com.skyway.common.core.domain.model.LoginUser;
import com.skyway.member.service.IMbCustomerService;
import com.skyway.resource.domain.*;
import com.skyway.resource.mapper.*;
import com.skyway.resource.service.*;
import com.skyway.resource.service.impl.*;
import com.skyway.web.controller.resource.*;
import com.skyway.web.task.*;
import com.skyway.web.websocket.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.*;

/** Opt-in integration test. Credentials come only from the environment; business storage is isolated in memory. */
public class Hysteria2VpsLiveIntegrationTest {
    private static final long INSTANCE = 990100L;
    private static final long CUSTOMER = 999990L;
    private static final int HTTP_PORT = 29090;
    private static final int SOCKS_PORT = 29091;
    private static final int UDP_PORT = 29092;
    private static final String AUDIT = "/root/skyway-live-audit-20261026-hysteria2";

    @Test
    void realServerLifecycleTrafficPoliciesClientsAndLegacyProtocols() throws Exception {
        String host = System.getenv("SKYWAY_LIVE_HOST");
        String password = System.getenv("SKYWAY_LIVE_PASS");
        String client = System.getenv("SKYWAY_LIVE_MIHOMO");
        Assumptions.assumeTrue(host != null && password != null && client != null,
                "live VPS credentials and a Mihomo executable are required");
        try (Fixture f = new Fixture(host, password, client)) {
            f.prepareHelpers();
            Map<Integer, Boolean> existing = f.probeExistingNodes();
            ProxyNode node = f.create("Hysteria2", f.port, null);
            JSONObject original = f.config(node);
            assertFalse(original.getJSONArray("inbounds").getJSONObject(0).getJSONObject("tls").containsKey("server_name"));
            try (Client proxy = f.client(node)) {
                try { proxy.health("allowed.example"); } catch (Exception | AssertionError e) {
                    System.out.println("[LIVE] Hysteria2 packet diagnostics: " + f.run("iptables -L NODE_TRAFFIC_IN -v -n -x; iptables -L NODE_TRAFFIC_OUT -v -n -x; ss -unap | grep " + node.getPort() + " || true"));
                    throw e;
                }
                proxy.udpEcho();
                proxy.upload(128 * 1024);
                f.traffic.collect();
                long[] before = f.trafficData.getTotalByNodeId(node.getId());
                VpsSshCommandService.TrafficPair first = f.remote.readTrafficCounters(INSTANCE).get(node.getPort());
                ExecutorService sampler = Executors.newSingleThreadExecutor();
                try {
                    Future<VpsSshCommandService.RealtimeSpeedSnapshot> speed = sampler.submit(() -> f.remote.readRealtimeSpeed(INSTANCE));
                    proxy.loadForSeconds(6);
                    assertTrue(speed.get(15, TimeUnit.SECONDS).getPorts().containsKey(String.valueOf(node.getPort())));
                } finally { sampler.shutdownNow(); }
                f.traffic.collect();
                long[] after = f.trafficData.getTotalByNodeId(node.getId());
                assertTrue(after[1] > before[1]);
                VpsSshCommandService.TrafficPair second = f.remote.readTrafficCounters(INSTANCE).get(node.getPort());
                assertTrue(second.tx > first.tx && second.rx > first.rx);
                f.remote.ensureTrafficRulesForPort(INSTANCE, node.getPort(), "Hysteria2");
                assertTrue(f.remote.readTrafficCounters(INSTANCE).get(node.getPort()).tx >= second.tx);
                f.pass("Hysteria2 TCP/UDP, upload/download, cumulative counters and realtime sampler");
            }
            f.checkNftCounters(node);
            f.edit(node, map("relayText", f.relayText()));
            try (Client proxy = f.client(node)) { proxy.health("allowed.example"); proxy.udpEcho(); }
            f.policy(node, "whitelist", "allowed.example");
            try (Client proxy = f.client(node)) { proxy.health("allowed.example"); proxy.blocked("blocked.example"); }
            f.policy(node, "blacklist", "blocked.example");
            try (Client proxy = f.client(node)) { proxy.health("allowed.example"); proxy.blocked("blocked.example"); }
            f.policy(node, "whitelist");
            f.edit(node, map("relayEnabled", false));
            try (Client proxy = f.client(node)) { proxy.health("allowed.example"); proxy.udpEcho(); }
            f.pass("SOCKS5 relay, whitelist/blacklist, policy switching and removal");

            f.edit(node, map("status", "1"));
            String stoppedPid = f.run("systemctl show sing-box -p MainPID");
            f.policy(node, "blacklist", "blocked.example");
            f.edit(node, map("relayText", f.relayText()));
            f.policy(node, "whitelist");
            assertEquals(stoppedPid, f.run("systemctl show sing-box -p MainPID"));
            long totalBeforePortEdit = Arrays.stream(f.trafficData.getTotalByNodeId(node.getId())).sum();
            f.edit(node, map("port", f.port + 1, "customerId", CUSTOMER + 1, "expireTime", "2099-12-30 12:00:00"));
            assertEquals(Integer.valueOf(f.port + 1), node.getPort());
            assertTrue(node.getUrl().contains(":" + node.getPort() + "?"));
            assertTrue(node.getNodeName().contains(String.valueOf(CUSTOMER + 1)));
            assertEquals(original.getJSONArray("inbounds").getJSONObject(0).get("tls"),
                    f.config(node).getJSONArray("inbounds").getJSONObject(0).get("tls"));
            assertEquals(totalBeforePortEdit, Arrays.stream(f.trafficData.getTotalByNodeId(node.getId())).sum());
            f.edit(node, map("status", "0"));
            try (Client proxy = f.client(node)) { proxy.health("allowed.example"); proxy.udpEcho(); }
            f.pass("disabled edits avoid restart; port/customer/expiry changes preserve TLS, URL and traffic totals");

            int currentPort = node.getPort();
            AjaxResult occupied = f.editor.edit(map("id", node.getId(), "port", 10000));
            assertFalse(occupied.isSuccess());
            assertEquals(currentPort, f.nodes.get(node.getId()).getPort());
            f.failNextUpdate = true;
            AjaxResult failedSave = f.editor.edit(map("id", node.getId(), "port", f.port + 2));
            assertFalse(failedSave.isSuccess());
            assertEquals(currentPort, f.nodes.get(node.getId()).getPort());
            assertEquals(currentPort, f.config(node).getJSONArray("inbounds").getJSONObject(0).getIntValue("listen_port"));
            f.pass("occupied ports rejected; failed database save restores remote port and config");

            f.checkRateLimits(node);
            f.checkExpiry(node);
            try (Client proxy = f.client(node)) { proxy.health("allowed.example"); }
            ProxyNode ws = f.createWebSocket(f.port + 4);
            try (Client proxy = f.client(ws)) { proxy.health("allowed.example"); proxy.udpEcho(); }
            f.pass("WebSocket creation, share parameters and relay on a minimal Hysteria2 config");
            ProxyNode batch = f.create("Hysteria2", f.port + 5, map("domainPolicy", map("mode", "blacklist", "customDomains", Arrays.asList("blocked.example"))));
            try (Client proxy = f.client(batch)) { proxy.health("allowed.example"); proxy.blocked("blocked.example"); }
            ProxyNode auto = f.create("Hysteria2", 10000, map("autoPort", true));
            assertNotEquals(Integer.valueOf(10000), auto.getPort());
            f.pass("serial batch API and automatic reassignment of an occupied port");

            ProxyNode vless = f.create("VLESS-REALITY", f.port + 6, map("sni", "www.bing.com"));
            try (Client proxy = f.client(vless)) { proxy.health("allowed.example"); proxy.udpEcho(); }
            f.edit(vless, map("relayText", f.relayText()));
            f.policy(vless, "blacklist", "blocked.example");
            try (Client proxy = f.client(vless)) { proxy.health("allowed.example"); proxy.blocked("blocked.example"); }
            f.policy(vless, "whitelist");
            f.edit(vless, map("relayEnabled", false));
            ProxyNode vmess = f.create("VMess-TCP", f.port + 7, null);
            try (Client proxy = f.client(vmess)) { proxy.health("allowed.example"); proxy.udpEcho(); }
            f.pass("VLESS-REALITY SNI/TCP/UDP/relay/policy and VMess-TCP TCP/UDP regression");

            for (ProxyNode created : new ArrayList<>(f.nodes.values())) {
                assertTrue(f.editor.remove(String.valueOf(created.getId())).isSuccess());
            }
            assertTrue(f.nodes.isEmpty());
            assertEquals(existing, f.probeExistingNodes());
            f.verifyOriginalFiles();
            f.pass("all temporary nodes deleted; original node connections and configuration hashes unchanged");
        } finally { SecurityContextHolder.clearContext(); }
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static class Fixture implements AutoCloseable {
        final String host, password, executable;
        final VpsSshCommandService remote = new VpsSshCommandService();
        final Map<Long, ProxyNode> nodes = new ConcurrentHashMap<>();
        final Map<Long, ProxyNodeRateLimit> limits = new ConcurrentHashMap<>();
        final ProxyNodeServiceImpl nodeService = new ProxyNodeServiceImpl();
        final ProxyNodeTrafficServiceImpl trafficData = new ProxyNodeTrafficServiceImpl();
        final ProxyNodeTrafficCollectTask traffic = new ProxyNodeTrafficCollectTask();
        final VpsInstanceController creator = new VpsInstanceController();
        final ProxyNodeController editor = new ProxyNodeController();
        final IProxyNodeRateLimitService rateService = mock(IProxyNodeRateLimitService.class);
        final IVpsInstanceService instances = mock(IVpsInstanceService.class);
        final ProxyDomainWhitelistService policies = new ProxyDomainWhitelistService();
        final VpsPortAvailabilityService ports = new VpsPortAvailabilityService();
        final VpsInstanceOperationCoordinator locks = new VpsInstanceOperationCoordinator();
        final AtomicLong sequence = new AtomicLong(990100);
        final SSHClient ssh = new SSHClient();
        final Path local = Files.createTempDirectory("skyway-vps-live-");
        int port;
        boolean failNextUpdate;
        boolean nftCreated;
        boolean namespaceCreated;
        final boolean remoteClient = "1".equals(System.getenv("SKYWAY_LIVE_REMOTE_CLIENT"));

        Fixture(String host, String password, String executable) throws Exception {
            this.host = host; this.password = password; this.executable = executable;
            ssh.addHostKeyVerifier(new PromiscuousVerifier()); ssh.setConnectTimeout(15000); ssh.setTimeout(60000);
            ssh.connect(host, 22); ssh.authPassword("root", password);
            VpsInstance instance = new VpsInstance(); instance.setId(INSTANCE); instance.setIp(host); instance.setSshPort(22);
            instance.setSshUsername("root"); instance.setSshPassword(password); instance.setStatus("running");
            when(instances.getById(INSTANCE)).thenReturn(instance);
            when(instances.selectList(any())).thenReturn(Arrays.asList(instance));
            ReflectionTestUtils.setField(remote, "vpsInstanceService", instances);
            ProxyNodeMapper mapper = mock(ProxyNodeMapper.class);
            ReflectionTestUtils.setField(nodeService, "proxyNodeMapper", mapper);
            when(mapper.insert(any())).thenAnswer(call -> { ProxyNode n = call.getArgument(0); n.setId(sequence.incrementAndGet()); nodes.put(n.getId(), copy(n)); return 1; });
            when(mapper.selectById(anyLong())).thenAnswer(call -> copy(nodes.get(call.getArgument(0))));
            when(mapper.selectList(any())).thenAnswer(call -> nodes.values().stream().map(Fixture::copy).collect(Collectors.toList()));
            when(mapper.selectByInstanceIdAndPort(anyLong(), anyInt())).thenAnswer(call -> nodes.values().stream()
                    .filter(n -> n.getPort().equals(call.getArgument(1))).findFirst().map(Fixture::copy).orElse(null));
            when(mapper.selectPortsByInstanceId(anyLong())).thenAnswer(call -> nodes.values().stream().map(ProxyNode::getPort).collect(Collectors.toList()));
            when(mapper.update(any())).thenAnswer(call -> { if (failNextUpdate) { failNextUpdate = false; return 0; }
                ProxyNode n = call.getArgument(0); ProxyNode stored = copy(n);
                stored.setDomainPolicyJson(nodes.get(n.getId()).getDomainPolicyJson());
                nodes.put(n.getId(), stored); return 1; });
            when(mapper.updateDomainPolicy(anyLong(), nullable(String.class), anyString())).thenAnswer(call -> {
                nodes.get(call.getArgument(0)).setDomainPolicyJson(call.getArgument(1)); return 1;
            });
            when(mapper.deleteById(anyLong())).thenAnswer(call -> nodes.remove(call.getArgument(0)) == null ? 0 : 1);
            when(mapper.selectExpiredAndNormal(any())).thenAnswer(call -> nodes.values().stream()
                    .filter(n -> "0".equals(n.getStatus()) && n.getExpireTime() != null && n.getExpireTime().before(new Date()))
                    .map(Fixture::copy).collect(Collectors.toList()));
            setupTraffic(); setupRates();
            ReflectionTestUtils.setField(ports, "proxyNodeService", nodeService);
            ReflectionTestUtils.setField(ports, "vpsSshCommandService", remote);
            for (Object controller : Arrays.asList(creator, editor)) {
                ReflectionTestUtils.setField(controller, "proxyNodeService", nodeService);
                ReflectionTestUtils.setField(controller, "vpsSshCommandService", remote);
                ReflectionTestUtils.setField(controller, "proxyNodeTrafficService", trafficData);
                ReflectionTestUtils.setField(controller, "proxyNodeRateLimitService", rateService);
                ReflectionTestUtils.setField(controller, "vpsInstanceOperationCoordinator", locks);
                ReflectionTestUtils.setField(controller, "proxyDomainWhitelistService", policies);
            }
            ReflectionTestUtils.setField(creator, "vpsPortAvailabilityService", ports);
            SysUser user = new SysUser(); user.setUserName("skyway-live-test");
            LoginUser login = new LoginUser(); ReflectionTestUtils.setField(login, "user", user);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null));
            VpsSshCommandService.RemotePortScan scan = remote.scanRemotePorts(INSTANCE);
            assertTrue(scan.isComplete());
            for (int p = 21080; p < 22000; p += 10) {
                boolean free = true;
                for (int offset = 0; offset < 8; offset++) free &= !scan.getUnavailablePorts().contains(p + offset);
                if (free) { port = p; break; }
            }
            assertTrue(port > 0);
        }

        void setupTraffic() {
            ProxyNodeTrafficMapper totals = mock(ProxyNodeTrafficMapper.class);
            ProxyNodeTrafficSnapshotMapper snapshots = mock(ProxyNodeTrafficSnapshotMapper.class);
            Map<Long, ProxyNodeTrafficSnapshot> last = new ConcurrentHashMap<>();
            Map<Long, long[]> sums = new ConcurrentHashMap<>();
            when(snapshots.selectByNodeId(anyLong())).thenAnswer(call -> last.get(call.getArgument(0)));
            when(snapshots.insert(any())).thenAnswer(call -> { ProxyNodeTrafficSnapshot s = call.getArgument(0); last.put(s.getNodeId(), s); return 1; });
            when(snapshots.update(any())).thenAnswer(call -> { ProxyNodeTrafficSnapshot s = call.getArgument(0); last.put(s.getNodeId(), s); return 1; });
            when(totals.insert(any())).thenAnswer(call -> { ProxyNodeTraffic t = call.getArgument(0); long[] sum = sums.computeIfAbsent(t.getNodeId(), id -> new long[2]);
                sum[0] += t.getRxDelta(); sum[1] += t.getTxDelta(); return 1; });
            when(totals.selectSumByNodeId(anyLong())).thenAnswer(call -> { long[] sum = sums.getOrDefault(call.getArgument(0), new long[2]); return map("totalRx", sum[0], "totalTx", sum[1]); });
            ReflectionTestUtils.setField(trafficData, "proxyNodeTrafficMapper", totals);
            ReflectionTestUtils.setField(trafficData, "proxyNodeTrafficSnapshotMapper", snapshots);
            ReflectionTestUtils.setField(traffic, "proxyNodeService", nodeService);
            ReflectionTestUtils.setField(traffic, "proxyNodeTrafficService", trafficData);
            ReflectionTestUtils.setField(traffic, "vpsSshCommandService", remote);
        }

        void setupRates() {
            when(rateService.getActiveByNodeId(anyLong())).thenAnswer(call -> limits.get(call.getArgument(0)));
            when(rateService.saveActive(any())).thenAnswer(call -> { ProxyNodeRateLimit r = call.getArgument(0); if (r.getId() == null) r.setId(sequence.incrementAndGet()); limits.put(r.getProxyNodeId(), r); return 1; });
            when(rateService.listExpiredActive(any())).thenAnswer(call -> limits.values().stream()
                    .filter(r -> r.getExpireTime() != null && r.getExpireTime().before(new Date())).collect(Collectors.toList()));
            when(rateService.markRemoved(anyLong(), anyString(), anyString())).thenAnswer(call -> { limits.values().removeIf(r -> r.getId().equals(call.getArgument(0))); return 1; });
            when(rateService.markExpired(anyLong(), anyString(), anyString())).thenAnswer(call -> { limits.values().removeIf(r -> r.getId().equals(call.getArgument(0))); return 1; });
        }

        ProxyNode create(String type, int requestedPort, Map<String, Object> extra) throws Exception {
            Map<String, Object> body = map("customerId", CUSTOMER, "port", requestedPort, "nodeType", type);
            if (extra != null) body.putAll(extra);
            AjaxResult result = creator.addProxyNode(INSTANCE, body);
            assertTrue(result.isSuccess(), String.valueOf(result.get("msg")));
            return (ProxyNode) result.get("data");
        }

        ProxyNode createWebSocket(int requestedPort) throws Exception {
            SshWebSocketHandler handler = new SshWebSocketHandler();
            ReflectionTestUtils.setField(handler, "vpsInstanceService", instances);
            ReflectionTestUtils.setField(handler, "vpsSshCommandService", remote);
            ReflectionTestUtils.setField(handler, "proxyNodeService", nodeService);
            ReflectionTestUtils.setField(handler, "proxyDomainWhitelistService", policies);
            ReflectionTestUtils.setField(handler, "vpsPortAvailabilityService", ports);
            ReflectionTestUtils.setField(handler, "vpsInstanceOperationCoordinator", locks);
            WebSocketSession ws = mock(WebSocketSession.class);
            when(ws.isOpen()).thenReturn(true); when(ws.getAttributes()).thenReturn(new ConcurrentHashMap<>(map("instanceId", INSTANCE)));
            CompletableFuture<ProxyNode> created = new CompletableFuture<>();
            doAnswer(call -> { JSONObject message = JSON.parseObject(((TextMessage) call.getArgument(0)).getPayload());
                if ("node_created".equals(message.getString("type"))) created.complete(message.getObject("node", ProxyNode.class));
                if ("exec_error".equals(message.getString("type"))) created.completeExceptionally(new IOException(message.getString("message")));
                return null;
            }).when(ws).sendMessage(any());
            ReflectionTestUtils.invokeMethod(handler, "handleTextMessage", ws, new TextMessage(JSON.toJSONString(
                    map("type", "add_proxy_node", "reqId", 999, "nodeType", "Hysteria2", "port", requestedPort,
                            "customerId", CUSTOMER, "relayText", relayText()))));
            return created.get(90, TimeUnit.SECONDS);
        }

        void edit(ProxyNode node, Map<String, Object> changes) {
            Map<String, Object> request = new LinkedHashMap<>(changes); request.put("id", node.getId());
            AjaxResult result = editor.edit(request);
            assertTrue(result.isSuccess(), String.valueOf(result.get("msg")));
            ProxyNode updated = nodeService.getById(node.getId());
            node.setNodeName(updated.getNodeName()); node.setPort(updated.getPort()); node.setStatus(updated.getStatus());
            node.setCustomerId(updated.getCustomerId()); node.setExpireTime(updated.getExpireTime()); node.setConfigJson(updated.getConfigJson());
            node.setUrl(updated.getUrl()); node.setDomainPolicyJson(updated.getDomainPolicyJson());
        }

        void policy(ProxyNode node, String mode, String... domains) {
            AjaxResult result = editor.updateDomainPolicy(node.getId(), map("domainPolicy", map("mode", mode, "customDomains", Arrays.asList(domains))));
            assertTrue(result.isSuccess(), String.valueOf(result.get("msg")));
            node.setDomainPolicyJson(nodeService.getById(node.getId()).getDomainPolicyJson());
        }

        JSONObject config(ProxyNode node) throws Exception {
            return JSON.parseObject(run("cat -- " + quote("/etc/sing-box/conf/" + node.getNodeName() + ".json" + ("1".equals(node.getStatus()) ? ".disabled" : ""))));
        }

        String relayText() { return "127.0.0.1:" + SOCKS_PORT + ":live-user:live-socks-password"; }

        void prepareHelpers() throws Exception {
            String python = "from http.server import BaseHTTPRequestHandler,HTTPServer\nimport socket,threading,urllib.parse\n"
                    + "def udp():\n s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM);s.bind(('127.0.0.1'," + UDP_PORT + "))\n while True:\n  data,addr=s.recvfrom(65535);s.sendto(data,addr)\n"
                    + "threading.Thread(target=udp,daemon=True).start()\nclass Handler(BaseHTTPRequestHandler):\n"
                    + " def do_GET(self):\n  count=int(urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query).get('bytes',['0'])[0])\n  body=b'x'*min(count,16777216) if self.path.startswith('/blob') else b'skyway-live-ok\\n'\n  self.send_response(200);self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)\n"
                    + " def do_POST(self):\n  self.rfile.read(int(self.headers.get('Content-Length','0')));self.do_GET()\n"
                    + " def log_message(self,*args): pass\nHTTPServer(('127.0.0.1'," + HTTP_PORT + "),Handler).serve_forever()\n";
            write(AUDIT + "/probe_server.py", python);
            JSONObject socks = new JSONObject();
            socks.put("inbounds", Arrays.asList(map("tag", "SKYWAY-LIVE-SOCKS", "type", "socks", "listen", "127.0.0.1", "listen_port", SOCKS_PORT,
                    "users", Arrays.asList(map("username", "live-user", "password", "live-socks-password")))));
            write(AUDIT + "/socks.json", socks.toJSONString());
            run("set -e; rm -f " + AUDIT + "/http.py; test -z \"$(ss -lntup | grep -E ':" + HTTP_PORT + " |:" + SOCKS_PORT + " |:" + UDP_PORT + " ')\"; "
                    + "nohup python3 " + AUDIT + "/probe_server.py > " + AUDIT + "/http.log 2>&1 < /dev/null & echo $! > " + AUDIT + "/http.pid; "
                    + "nohup /etc/sing-box/bin/sing-box run -c " + AUDIT + "/socks.json > " + AUDIT + "/socks.log 2>&1 < /dev/null & echo $! > " + AUDIT + "/socks.pid");
            if (remoteClient) setupRemoteClient();
            assertTrue(run("sleep 1; curl -fsS --max-time 3 http://127.0.0.1:" + HTTP_PORT + "/health").contains("skyway-live-ok"));
            pass("localhost HTTP/upload/UDP and authenticated SOCKS5 test services started");
        }

        Map<Integer, Boolean> probeExistingNodes() throws Exception {
            String files = run("find /etc/sing-box/conf -maxdepth 1 -type f -name '*.json' ! -name '*-" + CUSTOMER + "-*' ! -name '*-" + (CUSTOMER + 1) + "-*' -printf '%f\\n'");
            Map<Integer, Boolean> result = new TreeMap<>();
            for (String file : files.trim().split("\\n")) {
                if (!file.startsWith("VLESS-REALITY-")) continue;
                JSONObject root = JSON.parseObject(run("cat -- " + quote("/etc/sing-box/conf/" + file)));
                JSONObject inbound = root.getJSONArray("inbounds").getJSONObject(0);
                String urlText = run("sb url " + quote(file));
                java.util.regex.Matcher match = java.util.regex.Pattern.compile("vless://[^\\s\\u001b]+").matcher(urlText);
                assertTrue(match.find());
                URI uri = new URI(match.group()); Map<String, String> query = new HashMap<>();
                for (String part : uri.getRawQuery().split("&")) { String[] pair = part.split("=", 2); if (pair.length == 2) query.put(pair[0], URLDecoder.decode(pair[1], "UTF-8")); }
                ProxyNode node = new ProxyNode(); node.setNodeType("VLESS-REALITY"); node.setAddress(host); node.setPort(inbound.getInteger("listen_port"));
                node.setConfigJson(JSON.toJSONString(map("id", uri.getUserInfo(), "flow", query.get("flow"), "sni", query.get("sni"), "publicKey", query.get("pbk"), "shortId", query.get("sid"))));
                boolean healthy;
                try (Client proxy = client(node)) { proxy.health("allowed.example"); healthy = true; } catch (Exception | AssertionError failure) { healthy = false; }
                result.put(node.getPort(), healthy);
                System.out.println("[LIVE] Existing VLESS port " + node.getPort() + " reachable=" + healthy);
            }
            assertEquals(5, result.size(), "unexpected original node inventory");
            return result;
        }

        void checkNftCounters(ProxyNode node) throws Exception {
            nftCreated = run("nft list table inet node_traffic >/dev/null 2>&1 && echo yes || echo no").trim().equals("no");
            java.lang.reflect.Method ensure = VpsSshCommandService.class.getDeclaredMethod("ensureTrafficRulesNft", Session.class, int.class, String.class);
            java.lang.reflect.Method read = VpsSshCommandService.class.getDeclaredMethod("readTrafficCountersNft", Session.class, Map.class);
            ensure.setAccessible(true); read.setAccessible(true);
            try (Session session = ssh.startSession()) { ensure.invoke(remote, session, node.getPort(), "udp"); }
            try (Client proxy = client(node)) { proxy.download(128 * 1024); proxy.udpEcho(); }
            Map<Integer, VpsSshCommandService.TrafficPair> counters = new HashMap<>();
            try (Session session = ssh.startSession()) { read.invoke(remote, session, counters); }
            assertTrue(counters.get(node.getPort()).tx > 100000);
            long previous = counters.get(node.getPort()).tx;
            try (Session session = ssh.startSession()) { ensure.invoke(remote, session, node.getPort(), "udp"); }
            counters.clear(); try (Session session = ssh.startSession()) { read.invoke(remote, session, counters); }
            assertTrue(counters.get(node.getPort()).tx >= previous);
            if (nftCreated) { run("nft delete table inet node_traffic"); nftCreated = false; }
            pass("real nft counters increment and survive repeated provisioning");
        }

        void checkRateLimits(ProxyNode node) throws Exception {
            assertTrue(editor.setRateLimit(node.getId(), map("downloadMbps", 4, "uploadMbps", 4)).isSuccess());
            String qdisc = run("tc qdisc show; tc filter show dev eth0 parent 1:");
            assertTrue(qdisc.contains("htb"));
            try (Client proxy = client(node)) {
                long started = System.nanoTime(); proxy.download(4 * 1024 * 1024);
                double seconds = proxy instanceof RemoteClient ? ((RemoteClient) proxy).lastSeconds : (System.nanoTime() - started) / 1e9;
                double mbps = 4 * 1024 * 1024 * 8 / seconds / 1e6;
                System.out.printf(java.util.Locale.ROOT, "[LIVE] 4 Mbps UDP limit: observed %.2f Mbps over %.2f seconds%n", mbps, seconds);
                assertTrue(mbps < 6.5, "UDP egress limit did not constrain the transfer");
            }
            edit(node, map("port", port + 2));
            assertEquals(node.getPort(), limits.get(node.getId()).getPort());
            assertTrue(run("cat /etc/tc_ports_rules.conf").contains(node.getPort() + ":4:4"));
            try (Client proxy = client(node)) { proxy.health("allowed.example"); }
            limits.get(node.getId()).setExpireTime(new Date(System.currentTimeMillis() - 1000));
            ProxyNodeRateLimitExpireTask task = new ProxyNodeRateLimitExpireTask();
            ReflectionTestUtils.setField(task, "proxyNodeRateLimitService", rateService);
            ReflectionTestUtils.setField(task, "vpsSshCommandService", remote); task.processExpired();
            assertTrue(limits.isEmpty());
            assertFalse(run("cat /etc/tc_ports_rules.conf").contains(node.getPort() + ":"));
            assertFalse(run("tc qdisc show dev eth0").contains("htb"));
            pass("UDP rate limiting, active-limit port migration and automatic expiry removal");
        }

        void checkExpiry(ProxyNode node) throws Exception {
            edit(node, map("expireTime", "2020-01-01 00:00:00"));
            ProxyNodeExpireTask task = new ProxyNodeExpireTask();
            ReflectionTestUtils.setField(task, "proxyNodeService", nodeService);
            ReflectionTestUtils.setField(task, "vpsSshCommandService", remote);
            ReflectionTestUtils.setField(task, "mbCustomerService", mock(IMbCustomerService.class));
            ReflectionTestUtils.setField(task, "customerEmailCodeService", mock(CustomerEmailCodeService.class));
            ReflectionTestUtils.setField(task, "adminEmail", ""); task.processExpired();
            assertEquals("1", nodeService.getById(node.getId()).getStatus());
            node.setStatus("1"); assertNotNull(config(node));
            edit(node, map("expireTime", "2099-12-31 00:00:00", "status", "0"));
            pass("expired nodes stop and can be renewed and enabled without notifications");
        }

        void setupRemoteClient() throws Exception {
            assertEquals("ready", run("test -x " + AUDIT + "/mihomo-linux && echo ready").trim());
            run("test ! -e /run/netns/skyway-hy2-live; test ! -d /sys/class/net/skywaytest0");
            namespaceCreated = true;
            run("set -e; ip netns add skyway-hy2-live; ip link add skywaytest0 type veth peer name skywaypeer0; "
                    + "ip link set skywaypeer0 netns skyway-hy2-live; ip addr add 192.0.2.1/30 dev skywaytest0; ip link set skywaytest0 up; "
                    + "ip netns exec skyway-hy2-live ip link set lo up; ip netns exec skyway-hy2-live ip addr add 192.0.2.2/30 dev skywaypeer0; "
                    + "ip netns exec skyway-hy2-live ip link set skywaypeer0 up; ip netns exec skyway-hy2-live ip route add default via 192.0.2.1");
            try (InputStream source = getClass().getResourceAsStream("/live_proxy_probe.py")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int n;
                while ((n = source.read(buffer)) != -1) out.write(buffer, 0, n);
                write(AUDIT + "/live_proxy_probe.py", new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
            pass("isolated client namespace created; UDP traverses a veth interface for real tc shaping");
        }

        Client client(ProxyNode node) throws Exception { return remoteClient ? new RemoteClient(this, node) : new Client(executable, local, node); }
        static ProxyNode copy(ProxyNode node) { return node == null ? null : JSON.parseObject(JSON.toJSONString(node), ProxyNode.class); }
        void pass(String message) { System.out.println("[LIVE] PASS " + message); }
        static String quote(String value) { return "'" + value.replace("'", "'\"'\"'") + "'"; }
        void write(String path, String contents) throws Exception { run("printf '%s' " + quote(Base64.getEncoder().encodeToString(contents.getBytes(StandardCharsets.UTF_8))) + " | base64 -d > " + quote(path)); }
        String run(String command) throws Exception {
            try (Session session = ssh.startSession(); Session.Command cmd = session.exec("PATH=/usr/sbin:/sbin:$PATH; " + command + " 2>&1")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
                while ((count = cmd.getInputStream().read(buffer)) != -1) out.write(buffer, 0, count);
                cmd.join(60, TimeUnit.SECONDS);
                String value = new String(out.toByteArray(), StandardCharsets.UTF_8);
                if (cmd.getExitStatus() == null || cmd.getExitStatus() != 0) throw new IOException("live remote operation failed: " + value);
                return value;
            }
        }
        void verifyOriginalFiles() throws Exception {
            assertTrue(run("sha256sum -c " + AUDIT + "/original-config-hashes.txt; sha256sum -c " + AUDIT + "/original-core-hashes.txt").contains("OK"));
            assertEquals("active", run("systemctl is-active sing-box").trim());
        }
        public void close() throws Exception {
            Exception failure = null;
            for (ProxyNode node : new ArrayList<>(nodes.values())) {
                try { remote.removePortRateLimit(INSTANCE, node.getPort()); remote.removeProxyNodeFromServer(node); }
                catch (Exception e) { failure = e; }
            }
            if (nftCreated) { try { run("nft delete table inet node_traffic"); } catch (Exception e) { failure = e; } }
            try { run("for file in " + AUDIT + "/http.pid " + AUDIT + "/socks.pid; do if [ -f \"$file\" ]; then kill $(cat \"$file\") 2>/dev/null || true; rm -f \"$file\"; fi; done"); }
            finally {
                if (namespaceCreated) {
                    run("ip netns delete skyway-hy2-live; ip link delete skywaytest0 2>/dev/null || true");
                }
                verifyOriginalFiles(); ssh.close();
            }
            if (failure != null) throw failure;
        }
    }

    private static class Client implements AutoCloseable {
        final int port;
        final Process process;
        final Path directory;
        Client(String executable, Path root, ProxyNode node) throws Exception {
            directory = Files.createTempDirectory(root, "client-");
            try (ServerSocket reservation = new ServerSocket(0)) { port = reservation.getLocalPort(); }
            Path config = directory.resolve("config.json");
            Files.write(config, configuration(node, port));
            process = new ProcessBuilder(executable, "-d", directory.toString(), "-f", config.toString()).redirectErrorStream(true).redirectOutput(directory.resolve("client.log").toFile()).start();
            boolean ready = false;
            for (int i = 0; i < 50; i++) {
                try (Socket ignored = new Socket("127.0.0.1", port)) { ready = true; break; }
                catch (IOException e) { if (!process.isAlive()) break; Thread.sleep(100); }
            }
            if (!ready) { process.destroyForcibly(); throw new IOException("Mihomo did not start"); }
        }
        static byte[] configuration(ProxyNode node, int mixedPort) {
        JSONObject cfg = JSON.parseObject(node.getConfigJson());
        Map<String, Object> proxy = map("name", "live-node", "server", node.getAddress(), "port", node.getPort(), "udp", true);
        if ("Hysteria2".equals(node.getNodeType())) {
            proxy.putAll(map("type", "hysteria2", "password", cfg.getString("password"), "alpn", Arrays.asList("h3"),
                "skip-cert-verify", cfg.getBooleanValue("insecure"), "fingerprint", cfg.getString("pinSHA256")));
        } else if ("VLESS-REALITY".equals(node.getNodeType())) {
            proxy.putAll(map("type", "vless", "uuid", cfg.getString("id"), "network", "tcp", "tls", true,
                "flow", cfg.getString("flow"), "servername", cfg.getString("sni"), "client-fingerprint", "chrome",
                "reality-opts", map("public-key", cfg.getString("publicKey"), "short-id", cfg.getString("shortId") == null ? "" : cfg.getString("shortId"))));
        } else { proxy.putAll(map("type", "vmess", "uuid", cfg.getString("id"), "alterId", cfg.getIntValue("aid"), "cipher", "auto", "network", "tcp")); }
        return JSON.toJSONBytes(map("mixed-port", mixedPort, "allow-lan", false, "bind-address", "127.0.0.1", "mode", "rule",
                "log-level", "warning", "proxies", Arrays.asList(proxy), "rules", Arrays.asList("MATCH,live-node")));
        }
        Client() { port = 0; process = null; directory = null; }
        void loadForSeconds(int seconds) throws Exception {
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
            while (System.nanoTime() < until) download(512 * 1024);
        }
        byte[] request(String host, String path, byte[] upload) throws Exception {
            try (Socket socket = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", port)))) {
                socket.setSoTimeout(20000); socket.connect(new InetSocketAddress("127.0.0.1", HTTP_PORT), 15000);
                OutputStream out = socket.getOutputStream();
                String headers = (upload == null ? "GET" : "POST") + " " + path + " HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n"
                        + (upload == null ? "" : "Content-Length: " + upload.length + "\r\n") + "\r\n";
                out.write(headers.getBytes(StandardCharsets.US_ASCII)); if (upload != null) out.write(upload); out.flush();
                ByteArrayOutputStream response = new ByteArrayOutputStream(); byte[] buffer = new byte[65536]; int count;
                while ((count = socket.getInputStream().read(buffer)) != -1) response.write(buffer, 0, count);
                return response.toByteArray();
            }
        }
        void health(String host) throws Exception { String reply = new String(request(host, "/health", null), StandardCharsets.US_ASCII);
            assertTrue(reply.contains("skyway-live-ok"), "proxy health request failed: " + reply.substring(0, Math.min(reply.length(), 200))); }
        void blocked(String host) throws Exception { byte[] response; try { response = request(host, "/health", null); } catch (IOException expected) { return; }
            assertFalse(new String(response, StandardCharsets.US_ASCII).contains("skyway-live-ok"), "domain policy failed to block"); }
        void download(int bytes) throws Exception { assertTrue(request("allowed.example", "/blob?bytes=" + bytes, null).length >= bytes); }
        void upload(int bytes) throws Exception { assertTrue(new String(request("allowed.example", "/health", new byte[bytes]), StandardCharsets.US_ASCII).contains("skyway-live-ok")); }
        void udpEcho() throws Exception {
            try (Socket control = new Socket("127.0.0.1", port); DatagramSocket udp = new DatagramSocket()) {
                control.setSoTimeout(15000); udp.setSoTimeout(15000);
                DataInputStream in = new DataInputStream(control.getInputStream()); OutputStream out = control.getOutputStream();
                out.write(new byte[]{5, 1, 0}); out.flush(); assertEquals(5, in.readUnsignedByte()); assertEquals(0, in.readUnsignedByte());
                out.write(new byte[]{5, 3, 0, 1, 0, 0, 0, 0, 0, 0}); out.flush();
                assertEquals(5, in.readUnsignedByte()); assertEquals(0, in.readUnsignedByte()); in.readByte();
                int type = in.readUnsignedByte(); if (type == 1) { byte[] addr = new byte[4]; in.readFully(addr); }
                else if (type == 4) { byte[] addr = new byte[16]; in.readFully(addr); }
                else { byte[] addr = new byte[in.readUnsignedByte()]; in.readFully(addr); }
                int endpoint = in.readUnsignedShort();
                ByteArrayOutputStream packet = new ByteArrayOutputStream(); packet.write(new byte[]{0, 0, 0, 1, 127, 0, 0, 1, (byte)(UDP_PORT >> 8), (byte)UDP_PORT});
                packet.write("skyway-live-udp".getBytes(StandardCharsets.US_ASCII)); byte[] payload = packet.toByteArray();
                udp.send(new DatagramPacket(payload, payload.length, InetAddress.getByName("127.0.0.1"), endpoint));
                byte[] response = new byte[1024]; DatagramPacket received = new DatagramPacket(response, response.length); udp.receive(received);
                assertTrue(new String(response, 0, received.getLength(), StandardCharsets.US_ASCII).endsWith("skyway-live-udp"));
            }
        }
        public void close() throws Exception {
            process.destroy(); if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly();
            Files.deleteIfExists(directory.resolve("config.json"));
        }
    }
    private static class RemoteClient extends Client {
        final Fixture fixture;
        final String remoteDir;
        final int mixedPort;
        double lastSeconds;
        RemoteClient(Fixture fixture, ProxyNode node) throws Exception {
            super(); this.fixture = fixture;
            remoteDir = AUDIT + "/client-" + fixture.sequence.incrementAndGet();
            mixedPort = Integer.parseInt(fixture.run("python3 -c \"import socket;s=socket.socket();s.bind(('127.0.0.1',0));print(s.getsockname()[1]);s.close()\"").trim());
            fixture.run("mkdir -m 700 " + Fixture.quote(remoteDir));
            fixture.write(remoteDir + "/config.json", new String(configuration(node, mixedPort), StandardCharsets.UTF_8));
            fixture.run("nohup ip netns exec skyway-hy2-live " + AUDIT + "/mihomo-linux -d " + Fixture.quote(remoteDir)
                    + " -f " + Fixture.quote(remoteDir + "/config.json") + " > " + Fixture.quote(remoteDir + "/client.log")
                    + " 2>&1 < /dev/null & echo $! > " + Fixture.quote(remoteDir + "/client.pid"));
            fixture.run("sleep 0.3; kill -0 $(cat " + Fixture.quote(remoteDir + "/client.pid") + ")");
        }
        JSONObject probe(String operation, String host, int bytes, int seconds) throws Exception {
            String output = fixture.run("ip netns exec skyway-hy2-live python3 " + AUDIT + "/live_proxy_probe.py "
                    + mixedPort + " " + operation + " --host " + Fixture.quote(host) + " --bytes " + bytes + " --seconds " + seconds);
            JSONObject result = JSON.parseObject(output.trim());
            lastSeconds = result.getDoubleValue("seconds"); return result;
        }
        @Override void health(String host) throws Exception { JSONObject r = probe("http", host, 0, 0); assertTrue(r.getBooleanValue("health"), "namespace proxy health request failed: " + r.toJSONString()); }
        @Override void blocked(String host) throws Exception { assertFalse(probe("http", host, 0, 0).getBooleanValue("health"), "namespace policy failed to block"); }
        @Override void download(int bytes) throws Exception { JSONObject r = probe("http", "allowed.example", bytes, 0); assertTrue(r.getBooleanValue("ok"), r.toJSONString()); assertEquals(bytes, r.getIntValue("bytes")); }
        @Override void upload(int bytes) throws Exception { assertTrue(probe("upload", "allowed.example", bytes, 0).getBooleanValue("health")); }
        @Override void udpEcho() throws Exception { JSONObject r = probe("udp", "allowed.example", 0, 0); assertTrue(r.getBooleanValue("ok"), r.toJSONString()); }
        @Override void loadForSeconds(int seconds) throws Exception { assertTrue(probe("load", "allowed.example", 0, seconds).getBooleanValue("ok")); }
        @Override public void close() throws Exception {
            fixture.run("kill $(cat " + Fixture.quote(remoteDir + "/client.pid") + ") 2>/dev/null || true; cp "
                    + Fixture.quote(remoteDir + "/client.log") + " " + Fixture.quote(remoteDir + ".log") + "; rm -rf -- " + Fixture.quote(remoteDir));
        }
    }

}
