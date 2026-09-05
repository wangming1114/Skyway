package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.ProxyNodeDomainWhitelist;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IVpsInstanceService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

/**
 * Destructive-looking operations are restricted to one node created by this test and are
 * skipped unless credentials are explicitly supplied. No existing node is edited or deleted.
 */
public class ProxyNodeDomainPolicyLiveIntegrationTest {
    private static final long INSTANCE_ID = 990002L;
    private static final long CUSTOMER_ID = 999991L;
    private static final String CONF_DIR = "/etc/sing-box/conf";

    @Test
    public void fullTemporaryNodeLifecycleAndMutuallyExclusiveDomainPolicy() throws Exception {
        String host = System.getenv("SKYWAY_LIVE_HOST");
        String password = System.getenv("SKYWAY_LIVE_PASS");
        Assumptions.assumeTrue(host != null && !host.trim().isEmpty()
                && password != null && !password.isEmpty(), "live VPS credentials not provided");

        VpsInstance instance = new VpsInstance();
        instance.setId(INSTANCE_ID);
        instance.setIp(host);
        instance.setSshPort(22);
        instance.setSshUsername("root");
        instance.setSshPassword(password);
        IVpsInstanceService instances = mock(IVpsInstanceService.class);
        when(instances.getById(INSTANCE_ID)).thenReturn(instance);
        VpsSshCommandService service = new VpsSshCommandService();
        ReflectionTestUtils.setField(service, "vpsInstanceService", instances);

        ProxyNode node = null;
        String originalHashes;
        String originalTrafficRules;
        String originalServiceState;
        String oldGeneratedPath = null;
        String nodePath = null;
        String batchPath = null;
        String editedPath = null;
        Integer testPort = null;
        try (SSHClient ssh = connect(host, password)) {
            originalHashes = run(ssh, "find " + CONF_DIR
                    + " -maxdepth 1 -type f -exec sha256sum {} \\; | sort");
            originalTrafficRules = trafficRules(ssh);
            originalServiceState = run(ssh, "systemctl is-active sing-box; systemctl show sing-box -p MainPID -p ExecMainStartTimestamp");
            int port = chooseFreePort(service, ssh);
            testPort = port;
            oldGeneratedPath = CONF_DIR + "/VLESS-REALITY-" + port + ".json";
            run(ssh, "test ! -e " + shellQuote(oldGeneratedPath));

            // Creation is the only operation that invokes the server's sb menu.
            node = service.addProxyNodeOnInstance(INSTANCE_ID, CUSTOMER_ID, port, "2099-12-31", "VLESS-REALITY");
            assertNotNull(node);
            assertEquals("0", node.getStatus());
            assertEquals(port, node.getPort());
            nodePath = CONF_DIR + "/" + node.getNodeName() + ".json";
            run(ssh, "test -s " + shellQuote(nodePath));
            JSONObject originalNodeConfig = JSON.parseObject(run(ssh, "cat -- " + shellQuote(nodePath)));
            String inboundTag = originalNodeConfig.getJSONArray("inbounds").getJSONObject(0).getString("tag");

            // Disable first: all policy edits then avoid reloading existing active nodes.
            service.renameProxyNodeConfig(INSTANCE_ID, node.getNodeName(), true);
            node.setStatus("1");
            String disabledPath = nodePath + ".disabled";
            run(ssh, "test -s " + shellQuote(disabledPath));

            ProxyNodeDomainWhitelist whitelist = policy("whitelist", Arrays.asList("Example.COM", "example.com"));
            service.applyDomainWhitelistToProxyNodeConfig(node, whitelist);
            JSONObject whiteConfig = JSON.parseObject(run(ssh, "cat -- " + shellQuote(disabledPath)));
            assertWhitelist(whiteConfig, inboundTag, "example.com");

            // Re-applying the same state must be byte-for-byte stable.
            String whiteHash = run(ssh, "sha256sum " + shellQuote(disabledPath));
            service.applyDomainWhitelistToProxyNodeConfig(node, whitelist);
            assertEquals(whiteHash, run(ssh, "sha256sum " + shellQuote(disabledPath)));

            // Batch update is isolated to a second synthetic disabled test node.
            ProxyNode batchNode = new ProxyNode();
            batchNode.setId(990003L);
            batchNode.setInstanceId(INSTANCE_ID);
            batchNode.setNodeName("SKYWAY-LIVE-POLICY-" + port + "-BATCH");
            batchNode.setPort(port + 1);
            batchNode.setStatus("1");
            batchPath = CONF_DIR + "/" + batchNode.getNodeName() + ".json.disabled";
            run(ssh, "test ! -e " + shellQuote(batchPath));
            write(ssh, batchPath, syntheticConfig(batchNode.getNodeName() + ".json", port + 1));
            Map<ProxyNode, ProxyNodeDomainWhitelist> batch = new LinkedHashMap<>();
            batch.put(node, whitelist);
            batch.put(batchNode, policy("blacklist", Collections.singletonList("batch-blocked.example")));
            service.applyDomainWhitelistsToProxyNodeConfigs(batch);
            assertBlacklist(JSON.parseObject(run(ssh, "cat -- " + shellQuote(batchPath))),
                    batchNode.getNodeName() + ".json", "batch-blocked.example");
            service.applyDomainWhitelistToProxyNodeConfig(batchNode, policy("whitelist", Collections.emptyList()));
            assertEquals(JSON.parseObject(syntheticConfig(batchNode.getNodeName() + ".json", port + 1)),
                    JSON.parseObject(run(ssh, "cat -- " + shellQuote(batchPath))));
            run(ssh, "rm -f -- " + shellQuote(batchPath));
            batchPath = null;

            // Failure while preparing a later candidate must leave the earlier candidate untouched.
            String beforeFailedBatch = run(ssh, "sha256sum " + shellQuote(disabledPath));
            ProxyNode missing = new ProxyNode();
            missing.setId(990004L);
            missing.setInstanceId(INSTANCE_ID);
            missing.setNodeName("SKYWAY-LIVE-POLICY-MISSING-" + port);
            missing.setPort(port + 2);
            missing.setStatus("1");
            Map<ProxyNode, ProxyNodeDomainWhitelist> invalidBatch = new LinkedHashMap<>();
            invalidBatch.put(node, policy("blacklist", Collections.singletonList("must-not-apply.example")));
            invalidBatch.put(missing, policy("blacklist", Collections.singletonList("missing.example")));
            boolean batchFailed = false;
            try {
                service.applyDomainWhitelistsToProxyNodeConfigs(invalidBatch);
            } catch (Exception expected) {
                batchFailed = true;
            }
            assertTrue(batchFailed, "batch containing a missing node must fail");
            assertEquals(beforeFailedBatch, run(ssh, "sha256sum " + shellQuote(disabledPath)));

            ProxyNodeDomainWhitelist blacklist = policy("blacklist", Collections.singletonList("blocked.example.com"));
            service.applyDomainWhitelistToProxyNodeConfig(node, blacklist);
            JSONObject blackConfig = JSON.parseObject(run(ssh, "cat -- " + shellQuote(disabledPath)));
            assertBlacklist(blackConfig, inboundTag, "blocked.example.com");

            // A SOCKS relay must coexist with blacklist rules and be removable independently.
            VpsSshCommandService.Socks5RelayConfig relay =
                    new VpsSshCommandService.Socks5RelayConfig("192.0.2.10", 1080, "live-user", "live-pass");
            service.applySocks5RelayToProxyNodeConfig(node, relay);
            JSONObject blackWithSocks = JSON.parseObject(run(ssh, "cat -- " + shellQuote(disabledPath)));
            assertBlacklist(blackWithSocks, inboundTag, "blocked.example.com");
            assertTrue(hasOutboundType(blackWithSocks, "socks"));
            service.removeSocks5RelayFromProxyNodeConfig(node);
            JSONObject blackWithoutSocks = JSON.parseObject(run(ssh, "cat -- " + shellQuote(disabledPath)));
            assertBlacklist(blackWithoutSocks, inboundTag, "blocked.example.com");
            assertFalse(hasOutboundType(blackWithoutSocks, "socks"));

            // Switch back, clear to unrestricted, and verify the original node JSON is restored.
            service.applyDomainWhitelistToProxyNodeConfig(node, policy("whitelist", Collections.emptyList()));
            JSONObject cleared = JSON.parseObject(run(ssh, "cat -- " + shellQuote(disabledPath)));
            assertEquals(originalNodeConfig, cleared);

            // Existing port/name edit behavior remains reversible for a disabled temporary node.
            String editedName = node.getNodeName() + "-EDITED";
            editedPath = CONF_DIR + "/" + editedName + ".json.disabled";
            service.updateProxyNodeConfigPortAndName(
                    INSTANCE_ID, node.getNodeName(), editedName, true, port, port + 2);
            JSONObject edited = JSON.parseObject(run(ssh, "cat -- " + shellQuote(editedPath)));
            assertEquals(port + 2, edited.getJSONArray("inbounds").getJSONObject(0).getIntValue("listen_port"));
            service.updateProxyNodeConfigPortAndName(
                    INSTANCE_ID, editedName, node.getNodeName(), true, port + 2, port);
            JSONObject restoredAfterEdit = JSON.parseObject(originalNodeConfig.toJSONString());
            restoredAfterEdit.getJSONArray("inbounds").getJSONObject(0).put("tag", node.getNodeName() + ".json");
            assertEquals(restoredAfterEdit, JSON.parseObject(run(ssh, "cat -- " + shellQuote(disabledPath))));
            inboundTag = node.getNodeName() + ".json";
            editedPath = null;

            // Traffic counters add/remove only rules for the temporary port.
            service.ensureTrafficRulesForPort(INSTANCE_ID, port);
            assertTrue(run(ssh, "iptables-save | grep -F -- '--dport " + port + " -j ACCEPT'").contains(String.valueOf(port)));
            service.removeTrafficRulesForPort(INSTANCE_ID, port);
            assertFalse(runAllowFailure(ssh, "iptables-save | grep -F -- '--dport " + port + " -j ACCEPT'").contains(String.valueOf(port)));

            // Exercise enable/disable lifecycle on the temporary node only.
            service.renameProxyNodeConfig(INSTANCE_ID, node.getNodeName(), false);
            node.setStatus("0");
            run(ssh, "test -s " + shellQuote(nodePath));
            service.applyDomainWhitelistToProxyNodeConfig(node, blacklist);
            assertBlacklist(JSON.parseObject(run(ssh, "cat -- " + shellQuote(nodePath))),
                    inboundTag, "blocked.example.com");
            service.applyDomainWhitelistToProxyNodeConfig(node, policy("whitelist", Collections.emptyList()));
            assertEquals(restoredAfterEdit, JSON.parseObject(run(ssh, "cat -- " + shellQuote(nodePath))));
            service.renameProxyNodeConfig(INSTANCE_ID, node.getNodeName(), true);
            node.setStatus("1");
            run(ssh, "test -s " + shellQuote(disabledPath));
        } finally {
            if (testPort != null) {
                try { service.removeTrafficRulesForPort(INSTANCE_ID, testPort); } catch (Exception ignored) {}
                try { service.removeTrafficRulesForPort(INSTANCE_ID, testPort + 2); } catch (Exception ignored) {}
            }
            if (node != null) {
                try {
                    // Disabled deletion removes only the temporary file and does not restart sb.
                    if (!"1".equals(node.getStatus())) {
                        service.renameProxyNodeConfig(INSTANCE_ID, node.getNodeName(), true);
                    }
                    node.setStatus("1");
                    service.removeProxyNodeFromServer(node);
                } catch (Exception ignored) {
                    // Fallback is constrained to names discovered/generated by this test.
                    try (SSHClient cleanup = connect(host, password)) {
                        if (nodePath != null) run(cleanup, "rm -f -- " + shellQuote(nodePath)
                                + " " + shellQuote(nodePath + ".disabled"));
                        if (editedPath != null) run(cleanup, "rm -f -- " + shellQuote(editedPath));
                        if (oldGeneratedPath != null) run(cleanup, "rm -f -- " + shellQuote(oldGeneratedPath));
                    } catch (Exception ignoredAgain) {}
                }
            } else if (oldGeneratedPath != null) {
                // Covers sb succeeding before its output can be parsed/renamed by the service.
                try (SSHClient cleanup = connect(host, password)) {
                    run(cleanup, "rm -f -- " + shellQuote(oldGeneratedPath));
                } catch (Exception ignored) {}
            }
            if (batchPath != null) {
                try (SSHClient cleanup = connect(host, password)) {
                    run(cleanup, "rm -f -- " + shellQuote(batchPath));
                } catch (Exception ignored) {}
            }
        }

        try (SSHClient verify = connect(host, password)) {
            assertEquals(originalHashes, run(verify, "find " + CONF_DIR
                    + " -maxdepth 1 -type f -exec sha256sum {} \\; | sort"));
            assertEquals(originalTrafficRules, trafficRules(verify));
            assertEquals("active", run(verify, "systemctl is-active sing-box").trim());
            // Service identity is reported for audit; creation/relay operations may legitimately reload it.
            assertTrue(originalServiceState.contains("MainPID="));
        }
    }

    private static ProxyNodeDomainWhitelist policy(String mode, List<String> domains) {
        ProxyNodeDomainWhitelist policy = new ProxyNodeDomainWhitelist();
        policy.setMode(mode);
        policy.setDomains(domains);
        return policy;
    }

    private static void assertWhitelist(JSONObject config, String inboundTag, String domain) {
        JSONArray rules = config.getJSONObject("route").getJSONArray("rules");
        assertEquals("sniff", rules.getJSONObject(0).getString("action"));
        assertEquals("route", rules.getJSONObject(1).getString("action"));
        assertTrue(rules.getJSONObject(1).getJSONArray("domain").contains(domain));
        assertTrue(rules.getJSONObject(1).getJSONArray("domain_suffix").contains("." + domain));
        assertEquals("reject", rules.getJSONObject(2).getString("action"));
        assertEquals(inboundTag, rules.getJSONObject(0).getJSONArray("inbound").getString(0));
    }

    private static void assertBlacklist(JSONObject config, String inboundTag, String domain) {
        JSONArray rules = config.getJSONObject("route").getJSONArray("rules");
        assertEquals("sniff", rules.getJSONObject(0).getString("action"));
        assertEquals("reject", rules.getJSONObject(1).getString("action"));
        assertTrue(rules.getJSONObject(1).getJSONArray("domain").contains(domain));
        assertTrue(rules.getJSONObject(1).getJSONArray("domain_suffix").contains("." + domain));
        assertEquals(inboundTag, rules.getJSONObject(1).getJSONArray("inbound").getString(0));
        for (int i = 2; i < rules.size(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            assertFalse("reject".equals(rule.getString("action")) && !rule.containsKey("domain")
                    && !rule.containsKey("domain_suffix"), "blacklist must not add a catch-all reject");
        }
    }

    private static boolean hasOutboundType(JSONObject config, String type) {
        JSONArray outbounds = config.getJSONArray("outbounds");
        for (int i = 0; i < outbounds.size(); i++) {
            if (type.equals(outbounds.getJSONObject(i).getString("type"))) return true;
        }
        return false;
    }

    private static int chooseFreePort(VpsSshCommandService service, SSHClient ssh) throws Exception {
        VpsSshCommandService.RemotePortScan scan = service.scanRemotePorts(ssh);
        assertTrue(scan.isComplete(), "remote port scan incomplete: " + scan.getMissingSources());
        for (int port = 24000; port < 25000; port++) {
            if (!scan.getUnavailablePorts().contains(port)
                    && !scan.getUnavailablePorts().contains(port + 1)
                    && !scan.getUnavailablePorts().contains(port + 2)) return port;
        }
        throw new IllegalStateException("no free test port in 24000-24999");
    }

    private static SSHClient connect(String host, String password) throws Exception {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.setConnectTimeout(15_000);
        ssh.connect(host, 22);
        ssh.authPassword("root", password);
        return ssh;
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static String syntheticConfig(String inboundTag, int port) {
        JSONObject inbound = new JSONObject();
        inbound.put("type", "socks");
        inbound.put("tag", inboundTag);
        inbound.put("listen", "127.0.0.1");
        inbound.put("listen_port", port);
        JSONObject direct = new JSONObject();
        direct.put("type", "direct");
        JSONObject root = new JSONObject();
        root.put("inbounds", Collections.singletonList(inbound));
        root.put("outbounds", Collections.singletonList(direct));
        return root.toJSONString();
    }

    private static void write(SSHClient ssh, String path, String content) throws Exception {
        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        run(ssh, "printf '%s' " + shellQuote(encoded) + " | base64 -d > " + shellQuote(path));
    }

    private static String run(SSHClient ssh, String command) throws Exception {
        try (Session session = ssh.startSession(); Command cmd = session.exec(command)) {
            InputStream input = cmd.getInputStream();
            byte[] buffer = new byte[4096];
            StringBuilder output = new StringBuilder();
            int count;
            while ((count = input.read(buffer)) > 0) output.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
            cmd.join(45, TimeUnit.SECONDS);
            if (cmd.getExitStatus() != null && cmd.getExitStatus() != 0) {
                throw new IllegalStateException("remote command failed: " + command + " (" + cmd.getExitStatus() + ")");
            }
            return output.toString();
        }
    }

    private static String runAllowFailure(SSHClient ssh, String command) throws Exception {
        try (Session session = ssh.startSession(); Command cmd = session.exec(command)) {
            InputStream input = cmd.getInputStream();
            byte[] buffer = new byte[4096];
            StringBuilder output = new StringBuilder();
            int count;
            while ((count = input.read(buffer)) > 0) output.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
            cmd.join(45, TimeUnit.SECONDS);
            return output.toString();
        }
    }

    private static String trafficRules(SSHClient ssh) throws Exception {
        return runAllowFailure(ssh, "iptables-save | grep -E 'NODE_TRAFFIC|^-A (INPUT|OUTPUT)'");
    }
}
