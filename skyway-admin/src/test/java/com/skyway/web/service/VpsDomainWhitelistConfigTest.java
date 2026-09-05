package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class VpsDomainWhitelistConfigTest {
    private static final String BASE = "{\n"
            + "  \"inbounds\":[{\"tag\":\"node-a\",\"type\":\"vless\",\"listen_port\":10001}],\n"
            + "  \"outbounds\":[{\"type\":\"direct\"},{\"tag\":\"keep\",\"type\":\"direct\"}],\n"
            + "  \"route\":{\"rules\":[{\"inbound\":\"other-node\",\"outbound\":\"keep\"}]}\n"
            + "}";

    @Test
    public void emptyPolicyLeavesAnUnmanagedConfigSemanticallyUnchanged() {
        JSONObject original = parse(BASE);
        JSONObject unrestricted = parse(VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                BASE, Collections.emptyList(), true));

        assertEquals(original, unrestricted);
    }

    @Test
    public void modernPolicySniffsThenAllowsRootAndSubdomainsThenRejectsEverythingElse() {
        JSONObject root = parse(VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                BASE, Arrays.asList("example.com", "openai.com"), true));
        JSONArray rules = root.getJSONObject("route").getJSONArray("rules");

        assertEquals("sniff", rules.getJSONObject(0).getString("action"));
        assertEquals(Arrays.asList("tls", "http", "quic"), rules.getJSONObject(0).getJSONArray("sniffer"));
        assertEquals("route", rules.getJSONObject(1).getString("action"));
        assertTrue(rules.getJSONObject(1).getJSONArray("domain").contains("example.com"));
        assertTrue(rules.getJSONObject(1).getJSONArray("domain_suffix").contains(".example.com"));
        assertEquals("reject", rules.getJSONObject(2).getString("action"));
        assertFalse(rules.getJSONObject(2).containsKey("domain"));
        assertFalse(rules.getJSONObject(2).containsKey("network"));
        assertEquals("other-node", rules.getJSONObject(3).getString("inbound"));
        assertTrue(rules.getJSONObject(1).getString("outbound").startsWith("skyway-domain-egress-"));
    }

    @Test
    public void legacyPolicyUsesInboundSniffAndDedicatedBlockOutbound() {
        JSONObject root = parse(VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                BASE, Collections.singletonList("example.com"), false));
        JSONObject inbound = root.getJSONArray("inbounds").getJSONObject(0);
        JSONArray rules = root.getJSONObject("route").getJSONArray("rules");

        assertTrue(inbound.getBooleanValue("sniff"));
        assertTrue(inbound.getBooleanValue("sniff_override_destination"));
        assertTrue(rules.getJSONObject(1).getString("outbound").startsWith("skyway-domain-block-"));
        assertEquals("block", findOutbound(root, "skyway-domain-block-").getString("type"));
    }

    @Test
    public void repeatedApplyIsIdempotentAndClearRestoresUnrelatedRouting() {
        String once = VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                BASE, Collections.singletonList("example.com"), true);
        String twice = VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                once, Collections.singletonList("example.com"), true);
        assertEquals(parse(once), parse(twice));

        JSONObject cleared = parse(VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                twice, Collections.emptyList(), true));
        JSONArray rules = cleared.getJSONObject("route").getJSONArray("rules");
        assertEquals(1, rules.size());
        assertEquals("other-node", rules.getJSONObject(0).getString("inbound"));
        assertNull(findOutbound(cleared, "skyway-domain-egress-"));
    }

    @Test
    public void clearingLegacyPolicyRemovesManagedInboundSniffAndBlock() {
        String applied = VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                BASE, Collections.singletonList("example.com"), false);
        JSONObject cleared = parse(VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                applied, Collections.emptyList(), false));

        JSONObject inbound = cleared.getJSONArray("inbounds").getJSONObject(0);
        assertFalse(inbound.containsKey("sniff"));
        assertFalse(inbound.containsKey("sniff_override_destination"));
        assertNull(findOutbound(cleared, "skyway-domain-block-"));
    }

    @Test
    public void whitelistWithSocksUsesSocksForAllowedTrafficAndRemovalKeepsPolicy() {
        VpsSshCommandService.Socks5RelayConfig relay =
                new VpsSshCommandService.Socks5RelayConfig("203.0.113.9", 1080, "user", "pass");
        String restricted = VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                BASE, Collections.singletonList("example.com"), true);
        JSONObject withSocks = parse(VpsSshCommandService.applySocks5RelayToSingBoxConfig(
                restricted, relay, "SOCKS-test"));

        JSONObject allowedEgress = findOutbound(withSocks, "skyway-domain-egress-");
        assertEquals("socks", allowedEgress.getString("type"));
        assertEquals("203.0.113.9", allowedEgress.getString("server"));
        assertEquals("reject", withSocks.getJSONObject("route").getJSONArray("rules").getJSONObject(2).getString("action"));

        JSONObject withoutSocks = parse(VpsSshCommandService.removeSocks5RelayFromSingBoxConfig(withSocks.toJSONString()));
        assertEquals("direct", findOutbound(withoutSocks, "skyway-domain-egress-").getString("type"));
        assertEquals("reject", withoutSocks.getJSONObject("route").getJSONArray("rules").getJSONObject(2).getString("action"));
        assertNull(findOutbound(withoutSocks, "SOCKS-"));
    }

    @Test
    public void updatingSocksRelayAlsoUpdatesWhitelistEgressWithoutRemovingRejectRule() {
        VpsSshCommandService.Socks5RelayConfig first =
                new VpsSshCommandService.Socks5RelayConfig("203.0.113.9", 1080, "user", "pass");
        VpsSshCommandService.Socks5RelayConfig second =
                new VpsSshCommandService.Socks5RelayConfig("203.0.113.10", 2080, "next", "secret");
        String withSocks = VpsSshCommandService.applySocks5RelayToSingBoxConfig(BASE, first, "SOCKS-test");
        String restricted = VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                withSocks, Collections.singletonList("example.com"), true);

        JSONObject updated = parse(VpsSshCommandService.upsertSocks5RelayToSingBoxConfig(
                restricted, second, "SOCKS-unused"));

        JSONObject egress = findOutbound(updated, "skyway-domain-egress-");
        assertEquals("203.0.113.10", egress.getString("server"));
        assertEquals(2080, egress.getIntValue("server_port"));
        assertEquals("reject", updated.getJSONObject("route").getJSONArray("rules").getJSONObject(2).getString("action"));
    }

    @Test
    public void clearingWhitelistKeepsSocksCatchAllForwarding() {
        VpsSshCommandService.Socks5RelayConfig relay =
                new VpsSshCommandService.Socks5RelayConfig("203.0.113.9", 1080, "user", "pass");
        String withSocks = VpsSshCommandService.applySocks5RelayToSingBoxConfig(BASE, relay, "SOCKS-test");
        String restricted = VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                withSocks, Collections.singletonList("example.com"), true);
        JSONObject cleared = parse(VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                restricted, Collections.emptyList(), true));

        JSONArray rules = cleared.getJSONObject("route").getJSONArray("rules");
        assertTrue(rules.stream().map(JSONObject.class::cast)
                .anyMatch(rule -> "SOCKS-test".equals(rule.getString("outbound"))));
        assertNull(findOutbound(cleared, "skyway-domain-egress-"));
    }

    @Test
    public void inboundRenameUpdatesAllManagedRuleReferences() {
        String restricted = VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                BASE, Collections.singletonList("example.com"), true);
        JSONObject renamed = parse(VpsSshCommandService.updateSingBoxListenPortAndInboundName(
                restricted, "node-a", "node-b", 10088));

        assertEquals(10088, renamed.getJSONArray("inbounds").getJSONObject(0).getIntValue("listen_port"));
        for (Object item : renamed.getJSONObject("route").getJSONArray("rules")) {
            JSONObject rule = (JSONObject) item;
            if (!"other-node".equals(rule.getString("inbound"))) {
                assertTrue(rule.getJSONArray("inbound").contains("node-b"));
            }
        }
    }

    @Test
    public void applyingOneInboundPreservesManagedPolicyBelongingToAnotherInbound() {
        JSONObject original = parse(BASE);
        JSONObject otherEgress = new JSONObject();
        otherEgress.put("type", "direct");
        otherEgress.put("tag", "skyway-domain-egress-other");
        JSONObject otherBlock = new JSONObject();
        otherBlock.put("type", "block");
        otherBlock.put("tag", "skyway-domain-block-other");
        original.getJSONArray("outbounds").add(otherEgress);
        original.getJSONArray("outbounds").add(otherBlock);
        JSONObject otherRule = new JSONObject();
        otherRule.put("inbound", Collections.singletonList("other-node"));
        otherRule.put("outbound", "skyway-domain-egress-other");
        original.getJSONObject("route").getJSONArray("rules").add(otherRule);

        JSONObject patched = parse(VpsSshCommandService.applyDomainWhitelistToSingBoxConfig(
                original.toJSONString(), Collections.singletonList("example.com"), true));

        assertEquals("direct", findOutbound(patched, "skyway-domain-egress-other").getString("type"));
        assertEquals("block", findOutbound(patched, "skyway-domain-block-other").getString("type"));
        assertTrue(patched.getJSONObject("route").getJSONArray("rules").stream().map(JSONObject.class::cast)
                .anyMatch(rule -> "skyway-domain-egress-other".equals(rule.getString("outbound"))));
    }

    @Test
    public void modernBlacklistRejectsOnlyMatchingDomains() {
        JSONObject root = parse(VpsSshCommandService.applyDomainPolicyToSingBoxConfig(
                BASE, Collections.singletonList("example.com"), "blacklist", true));
        JSONArray rules = root.getJSONObject("route").getJSONArray("rules");

        assertEquals("sniff", rules.getJSONObject(0).getString("action"));
        assertEquals("reject", rules.getJSONObject(1).getString("action"));
        assertTrue(rules.getJSONObject(1).getJSONArray("domain").contains("example.com"));
        assertTrue(rules.getJSONObject(1).getJSONArray("domain_suffix").contains(".example.com"));
        assertEquals("other-node", rules.getJSONObject(2).getString("inbound"));
        assertEquals("direct", findOutbound(root, "skyway-domain-egress-").getString("type"));
        assertNull(findOutbound(root, "skyway-domain-block-"));
    }

    @Test
    public void legacyBlacklistUsesDomainScopedBlockAndModeSwitchIsIdempotent() {
        String black = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(
                BASE, Collections.singletonList("example.com"), "blacklist", false);
        JSONObject legacy = parse(black);
        assertEquals("block", findOutbound(legacy, "skyway-domain-block-").getString("type"));
        assertTrue(legacy.getJSONObject("route").getJSONArray("rules").getJSONObject(0)
                .getJSONArray("domain").contains("example.com"));

        String white = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(
                black, Collections.singletonList("openai.com"), "whitelist", true);
        String blackAgain = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(
                white, Collections.singletonList("example.com"), "blacklist", true);
        String repeated = VpsSshCommandService.applyDomainPolicyToSingBoxConfig(
                blackAgain, Collections.singletonList("example.com"), "blacklist", true);
        assertEquals(parse(blackAgain), parse(repeated));
    }

    private JSONObject parse(String json) {
        return JSON.parseObject(json);
    }

    private JSONObject findOutbound(JSONObject root, String tagPrefix) {
        JSONArray outbounds = root.getJSONArray("outbounds");
        for (int i = 0; i < outbounds.size(); i++) {
            JSONObject outbound = outbounds.getJSONObject(i);
            String tag = outbound.getString("tag");
            if (tag != null && tag.startsWith(tagPrefix)) return outbound;
        }
        return null;
    }
}
