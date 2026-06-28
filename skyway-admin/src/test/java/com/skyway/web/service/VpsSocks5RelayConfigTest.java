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
}
