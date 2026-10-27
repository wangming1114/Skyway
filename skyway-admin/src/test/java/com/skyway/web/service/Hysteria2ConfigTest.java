package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collections;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.web.websocket.AccessLogParser;
import org.junit.jupiter.api.Test;

public class Hysteria2ConfigTest {
    private static final String MINIMAL = "{\"inbounds\":[{\"tag\":\"Hysteria2-32332.json\",\"type\":\"hysteria2\","
            + "\"listen\":\"::\",\"listen_port\":32332,\"users\":[{\"password\":\"test-password\"}],"
            + "\"tls\":{\"enabled\":true,\"alpn\":[\"h3\"],\"key_path\":\"/etc/sing-box/bin/tls.key\","
            + "\"certificate_path\":\"/etc/sing-box/bin/tls.cer\"}}]}";
    private static final VpsSshCommandService.Socks5RelayConfig RELAY =
            new VpsSshCommandService.Socks5RelayConfig("example.com", 1080, "user", "password");

    @Test
    void newNodeHasAnIdempotentDirectFallbackEvenWhenAnotherNodeOwnsTheDefaultSocksOutbound() {
        JSONObject input = JSON.parseObject(MINIMAL);
        input.put("outbounds", JSON.parseArray("[{\"type\":\"socks\",\"tag\":\"unrelated-socks\",\"server\":\"other.example\",\"server_port\":1080}]"));
        String initialized = VpsSshCommandService.ensureNodeScopedDirectRouting(input.toJSONString());
        JSONObject root = JSON.parseObject(initialized);
        String directTag = root.getJSONObject("route").getJSONArray("rules").getJSONObject(0).getString("outbound");
        assertTrue(directTag.startsWith("skyway-node-direct-"));
        assertEquals("direct", root.getJSONArray("outbounds").getJSONObject(1).getString("type"));
        assertEquals("unrelated-socks", root.getJSONArray("outbounds").getJSONObject(0).getString("tag"));
        assertEquals(root, JSON.parseObject(VpsSshCommandService.ensureNodeScopedDirectRouting(initialized)));
        String relayed = VpsSshCommandService.upsertSocks5RelayToSingBoxConfig(initialized, RELAY, "SOCKS-test");
        assertEquals(root, JSON.parseObject(VpsSshCommandService.removeSocks5RelayFromSingBoxConfig(relayed)));
        assertEquals(JSON.parseObject(MINIMAL).get("inbounds"), root.get("inbounds"));
    }

    @Test
    void minimalInboundSupportsRelayUpdateAndRemovalWithoutSniOrTlsChanges() {
        String added = VpsSshCommandService.applySocks5RelayToSingBoxConfig(MINIMAL, RELAY, "SOCKS-test");
        assertEquals(JSON.parseObject(MINIMAL).get("inbounds"), JSON.parseObject(added).get("inbounds"));
        String updated = VpsSshCommandService.upsertSocks5RelayToSingBoxConfig(added,
                new VpsSshCommandService.Socks5RelayConfig("other.example", 1081, "u", "p"), "SOCKS-unused");
        assertEquals(1, JSON.parseObject(updated).getJSONArray("outbounds").size());
        assertEquals("other.example", JSON.parseObject(updated).getJSONArray("outbounds").getJSONObject(0).getString("server"));
        assertEquals(JSON.parseObject(MINIMAL), JSON.parseObject(VpsSshCommandService.removeSocks5RelayFromSingBoxConfig(updated)));
    }

    @Test
    void policiesAndRelayCanBeCombinedInEitherOrderAndCleared() {
        for (String mode : Arrays.asList("whitelist", "blacklist")) {
            String policy = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(MINIMAL, Arrays.asList("example.com"), mode, true);
            assertEquals(JSON.parseObject(MINIMAL).get("inbounds"), JSON.parseObject(policy).get("inbounds"));
            String combined = VpsSshCommandService.upsertSocks5RelayToSingBoxConfig(policy, RELAY, "SOCKS-test");
            String removed = VpsSshCommandService.removeSocks5RelayFromSingBoxConfig(combined);
            assertFalse(removed.contains("\"type\":\"socks\""));
            String cleared = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(removed, Collections.emptyList(), mode, true);
            assertEquals(JSON.parseObject(MINIMAL), JSON.parseObject(cleared));
            String relayFirst = VpsSshCommandService.applySocks5RelayToSingBoxConfig(MINIMAL, RELAY, "SOCKS-test");
            String both = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(relayFirst, Arrays.asList("example.com"), mode, true);
            String policyRemoved = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(both, Collections.emptyList(), mode, true);
            assertEquals(JSON.parseObject(relayFirst), JSON.parseObject(policyRemoved));
        }
    }

    @Test
    void portChangePreservesCredentialsTlsAndUpdatesPolicyRouteReferences() {
        String policy = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(MINIMAL, Arrays.asList("example.com"), "whitelist", true);
        JSONObject updated = JSON.parseObject(VpsSshCommandService.updateSingBoxListenPortAndInboundName(
                policy, "Hysteria2-32332.json", "Hysteria2-new.json", 32333));
        JSONObject inbound = updated.getJSONArray("inbounds").getJSONObject(0);
        assertEquals(32333, inbound.getIntValue("listen_port"));
        assertEquals(JSON.parseObject(MINIMAL).getJSONArray("inbounds").getJSONObject(0).get("tls"), inbound.get("tls"));
        assertEquals("test-password", inbound.getJSONArray("users").getJSONObject(0).getString("password"));
        assertTrue(updated.getJSONObject("route").getJSONArray("rules").stream()
                .allMatch(rule -> JSON.toJSONString(((JSONObject) rule).get("inbound")).contains("Hysteria2-32333.json")));
        assertFalse(inbound.getJSONObject("tls").containsKey("server_name"));
    }

    @Test
    void accessLogsAcceptStreamAndPacketDestinationsWithOrWithoutUserNames() {
        for (String user : Arrays.asList("", "[alice] ", "[0] ")) {
            for (String kind : Arrays.asList("connection", "packet connection")) {
                AccessLogParser.AccessLogEntry entry = AccessLogParser.parse(
                        "2026-10-26 12:03:04 +0800 INFO inbound/hysteria2[Hysteria2-32332.json]: "
                                + user + "inbound " + kind + " to [2001:db8::1]:443", "Hysteria2-32332.json");
                assertNotNull(entry);
                assertEquals("hysteria2", entry.getProtocol());
                assertEquals("2001:db8::1", entry.getDestinationHost());
            }
        }
    }
}
