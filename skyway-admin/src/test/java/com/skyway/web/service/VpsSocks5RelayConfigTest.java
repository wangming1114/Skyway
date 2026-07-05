package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

public class VpsSocks5RelayConfigTest {

    @Test
    public void applySocks5RelayReplacesFirstDirectOutboundAndAddsRouteRule() {
        String original = "{\n"
                + "  \"inbounds\": [{\"tag\":\"VLESS-REALITY-10001.json\",\"type\":\"vless\",\"listen_port\":10001}],\n"
                + "  \"outbounds\": [\n"
                + "    {\"type\":\"direct\"},\n"
                + "    {\"tag\":\"public_key_-Eel8leVgaWJnsYwaxNQPfiyzt6f40uRJtACu6FYnFs\",\"type\":\"direct\"}\n"
                + "  ]\n"
                + "}";
        VpsSshCommandService.Socks5RelayConfig relay =
                new VpsSshCommandService.Socks5RelayConfig("204.1.132.93", 35345, "lVZjQtlJ", "Qat3T6ofak");

        String patched = VpsSshCommandService.applySocks5RelayToSingBoxConfig(original, relay, "SOCKS-AbC123");
        org.junit.jupiter.api.Assertions.assertTrue(patched.contains("\n"));
        org.junit.jupiter.api.Assertions.assertTrue(patched.matches("(?s).*\\n\\s+\"outbounds\".*"));
        JSONObject root = JSON.parseObject(patched);
        JSONArray outbounds = root.getJSONArray("outbounds");
        JSONObject socks = outbounds.getJSONObject(0);

        assertEquals("SOCKS-AbC123", socks.getString("tag"));
        assertEquals("socks", socks.getString("type"));
        assertEquals("204.1.132.93", socks.getString("server"));
        assertEquals(35345, socks.getIntValue("server_port"));
        assertEquals("5", socks.getString("version"));
        assertEquals("lVZjQtlJ", socks.getString("username"));
        assertEquals("Qat3T6ofak", socks.getString("password"));

        JSONObject publicKeyOutbound = outbounds.getJSONObject(1);
        assertEquals("public_key_-Eel8leVgaWJnsYwaxNQPfiyzt6f40uRJtACu6FYnFs", publicKeyOutbound.getString("tag"));
        assertEquals("direct", publicKeyOutbound.getString("type"));

        JSONObject routeRule = root.getJSONObject("route").getJSONArray("rules").getJSONObject(0);
        assertEquals("VLESS-REALITY-10001.json", routeRule.getString("inbound"));
        assertEquals("SOCKS-AbC123", routeRule.getString("outbound"));
    }

    @Test
    public void parseSocks5RelayTextSplitsColonSeparatedInput() {
        VpsSshCommandService.Socks5RelayConfig relay =
                VpsSshCommandService.parseSocks5RelayText("204.1.132.93:35345:lVZjQtlJ:Qat3T6ofak");

        assertNotNull(relay);
        assertEquals("204.1.132.93", relay.getServer());
        assertEquals(35345, relay.getServerPort());
        assertEquals("lVZjQtlJ", relay.getUsername());
        assertEquals("Qat3T6ofak", relay.getPassword());
    }

    @Test
    public void parseSocks5RelayTextRejectsInvalidPort() {
        assertThrows(IllegalArgumentException.class,
                () -> VpsSshCommandService.parseSocks5RelayText("204.1.132.93:70000:user:pass"));
    }

    @Test
    public void upsertSocks5RelayUpdatesExistingSocksOutboundWithoutChangingRouteTag() {
        String original = "{\n"
                + "  \"inbounds\": [{\"tag\":\"VLESS-REALITY-10001.json\",\"type\":\"vless\",\"listen_port\":10001}],\n"
                + "  \"outbounds\": [\n"
                + "    {\"tag\":\"SOCKS-Old123\",\"type\":\"socks\",\"server\":\"1.1.1.1\",\"server_port\":1111,\"version\":\"5\",\"username\":\"old\",\"password\":\"oldpass\"},\n"
                + "    {\"tag\":\"public_key_keep\",\"type\":\"direct\"}\n"
                + "  ],\n"
                + "  \"route\": {\"rules\":[{\"inbound\":\"VLESS-REALITY-10001.json\",\"outbound\":\"SOCKS-Old123\"}]}\n"
                + "}";
        VpsSshCommandService.Socks5RelayConfig relay =
                new VpsSshCommandService.Socks5RelayConfig("157.238.146.152", 36160, "EE6Zfs2f", "SpXh9uqFOc");

        String patched = VpsSshCommandService.upsertSocks5RelayToSingBoxConfig(original, relay, "SOCKS-New999");
        JSONObject root = JSON.parseObject(patched);
        JSONObject socks = root.getJSONArray("outbounds").getJSONObject(0);

        assertEquals("SOCKS-Old123", socks.getString("tag"));
        assertEquals("socks", socks.getString("type"));
        assertEquals("157.238.146.152", socks.getString("server"));
        assertEquals(36160, socks.getIntValue("server_port"));
        assertEquals("EE6Zfs2f", socks.getString("username"));
        assertEquals("SpXh9uqFOc", socks.getString("password"));
        assertEquals("SOCKS-Old123", root.getJSONObject("route").getJSONArray("rules").getJSONObject(0).getString("outbound"));
        assertEquals("public_key_keep", root.getJSONArray("outbounds").getJSONObject(1).getString("tag"));
    }

    @Test
    public void upsertSocks5RelayRealignsLegacyRouteInboundToCurrentInboundTag() {
        String original = "{\n"
                + "  \"inbounds\": [{\"tag\":\"VLESS-REALITY-23.144.68.66-10012-2-permanent.json\",\"type\":\"vless\",\"listen_port\":10012}],\n"
                + "  \"outbounds\": [\n"
                + "    {\"tag\":\"SOCKS-Y2RbpjLN\",\"type\":\"socks\",\"server\":\"198.65.53.99\",\"server_port\":35351,\"version\":\"5\",\"username\":\"old\",\"password\":\"oldpass\"},\n"
                + "    {\"tag\":\"public_key_keep\",\"type\":\"direct\"}\n"
                + "  ],\n"
                + "  \"route\": {\"rules\":[{\"inbound\":\"VLESS-REALITY-10011.json\",\"outbound\":\"SOCKS-Y2RbpjLN\"}]}\n"
                + "}";
        VpsSshCommandService.Socks5RelayConfig relay =
                new VpsSshCommandService.Socks5RelayConfig("157.238.146.152", 36160, "EE6Zfs2f", "SpXh9uqFOc");

        String patched = VpsSshCommandService.upsertSocks5RelayToSingBoxConfig(original, relay, "SOCKS-New999");
        JSONObject root = JSON.parseObject(patched);
        JSONObject routeRule = root.getJSONObject("route").getJSONArray("rules").getJSONObject(0);

        assertEquals("VLESS-REALITY-23.144.68.66-10012-2-permanent.json", routeRule.getString("inbound"));
        assertEquals("SOCKS-Y2RbpjLN", routeRule.getString("outbound"));
        assertEquals("157.238.146.152", root.getJSONArray("outbounds").getJSONObject(0).getString("server"));
    }

    @Test
    public void removeSocks5RelayRestoresDirectOutboundAndRemovesRoute() {
        String original = "{\n"
                + "  \"inbounds\": [{\"tag\":\"VLESS-REALITY-10001.json\",\"type\":\"vless\",\"listen_port\":10001}],\n"
                + "  \"outbounds\": [\n"
                + "    {\"tag\":\"SOCKS-Old123\",\"type\":\"socks\",\"server\":\"1.1.1.1\",\"server_port\":1111,\"version\":\"5\",\"username\":\"old\",\"password\":\"oldpass\"},\n"
                + "    {\"tag\":\"public_key_keep\",\"type\":\"direct\"}\n"
                + "  ],\n"
                + "  \"route\": {\"rules\":[{\"inbound\":\"VLESS-REALITY-10001.json\",\"outbound\":\"SOCKS-Old123\"}]}\n"
                + "}";

        String patched = VpsSshCommandService.removeSocks5RelayFromSingBoxConfig(original);
        JSONObject root = JSON.parseObject(patched);
        JSONArray outbounds = root.getJSONArray("outbounds");
        JSONObject firstOutbound = outbounds.getJSONObject(0);

        assertEquals("direct", firstOutbound.getString("type"));
        assertEquals(null, firstOutbound.getString("tag"));
        assertEquals("public_key_keep", outbounds.getJSONObject(1).getString("tag"));
        assertEquals(null, root.getJSONObject("route"));
        org.junit.jupiter.api.Assertions.assertTrue(patched.matches("(?s).*\\n\\s+\"outbounds\".*"));
    }

    @Test
    public void updateListenPortAndInboundNameKeepsRelayRouteAligned() {
        String original = "{\n"
                + "  \"inbounds\": [{\"tag\":\"VLESS-REALITY-old.json\",\"type\":\"vless\",\"listen_port\":10001}],\n"
                + "  \"outbounds\": [\n"
                + "    {\"tag\":\"SOCKS-Old123\",\"type\":\"socks\",\"server\":\"1.1.1.1\",\"server_port\":1111,\"version\":\"5\",\"username\":\"old\",\"password\":\"oldpass\"},\n"
                + "    {\"tag\":\"public_key_keep\",\"type\":\"direct\"}\n"
                + "  ],\n"
                + "  \"route\": {\"rules\":[{\"inbound\":\"VLESS-REALITY-old.json\",\"outbound\":\"SOCKS-Old123\"}]}\n"
                + "}";

        String patched = VpsSshCommandService.updateSingBoxListenPortAndInboundName(
                original,
                "VLESS-REALITY-old.json",
                "VLESS-REALITY-new.json",
                10088);

        JSONObject root = JSON.parseObject(patched);
        JSONObject inbound = root.getJSONArray("inbounds").getJSONObject(0);

        assertEquals("VLESS-REALITY-new.json", inbound.getString("tag"));
        assertEquals(10088, inbound.getIntValue("listen_port"));
        assertEquals("VLESS-REALITY-new.json", root.getJSONObject("route").getJSONArray("rules").getJSONObject(0).getString("inbound"));
        assertEquals("SOCKS-Old123", root.getJSONArray("outbounds").getJSONObject(0).getString("tag"));
        assertEquals("public_key_keep", root.getJSONArray("outbounds").getJSONObject(1).getString("tag"));
        org.junit.jupiter.api.Assertions.assertTrue(patched.matches("(?s).*\\n\\s+\"inbounds\".*"));
    }

    @Test
    public void updateListenPortAndInboundNameRewritesLegacyPortRouteInbound() {
        String original = "{\n"
                + "  \"inbounds\": [{\"tag\":\"VLESS-REALITY-23.144.68.66-10011-2-permanent.json\",\"type\":\"vless\",\"listen_port\":10011}],\n"
                + "  \"outbounds\": [\n"
                + "    {\"tag\":\"SOCKS-Old123\",\"type\":\"socks\",\"server\":\"1.1.1.1\",\"server_port\":1111,\"version\":\"5\",\"username\":\"old\",\"password\":\"oldpass\"},\n"
                + "    {\"tag\":\"public_key_keep\",\"type\":\"direct\"}\n"
                + "  ],\n"
                + "  \"route\": {\"rules\":[{\"inbound\":\"VLESS-REALITY-10011.json\",\"outbound\":\"SOCKS-Old123\"}]}\n"
                + "}";

        String patched = VpsSshCommandService.updateSingBoxListenPortAndInboundName(
                original,
                "VLESS-REALITY-23.144.68.66-10011-2-permanent.json",
                "VLESS-REALITY-23.144.68.66-10012-2-permanent.json",
                10012);

        JSONObject root = JSON.parseObject(patched);
        JSONObject inbound = root.getJSONArray("inbounds").getJSONObject(0);

        assertEquals("VLESS-REALITY-23.144.68.66-10012-2-permanent.json", inbound.getString("tag"));
        assertEquals(10012, inbound.getIntValue("listen_port"));
        assertEquals("VLESS-REALITY-23.144.68.66-10012-2-permanent.json",
                root.getJSONObject("route").getJSONArray("rules").getJSONObject(0).getString("inbound"));
        assertEquals("SOCKS-Old123", root.getJSONArray("outbounds").getJSONObject(0).getString("tag"));
    }
}
