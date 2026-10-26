package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.ProxyNode;
import org.junit.jupiter.api.Test;

public class RealitySniSupportTest {
    static final String CONFIG = "{\"inbounds\":[{\"type\":\"vless\",\"tag\":\"legacy.json\",\"listen_port\":20031,"
            + "\"users\":[{\"uuid\":\"user-id\",\"flow\":\"xtls-rprx-vision\"}],"
            + "\"tls\":{\"enabled\":true,\"server_name\":\"old.example\",\"reality\":{\"enabled\":true,"
            + "\"handshake\":{\"server\":\"handshake.example\",\"server_port\":443},"
            + "\"private_key\":\"keep-private\",\"short_id\":[\"\"]}}}],"
            + "\"outbounds\":[{\"type\":\"socks\",\"server\":\"relay.example\"},{\"type\":\"direct\",\"tag\":\"public_key_keep\"}],"
            + "\"route\":{\"rules\":[{\"inbound\":\"legacy.json\",\"outbound\":\"keep\"}]}}";

    static ProxyNode node() {
        ProxyNode node = new ProxyNode();
        node.setInstanceId(7L);
        node.setNodeName("node");
        node.setNodeType("VLESS-REALITY");
        node.setPort(20031);
        node.setConfigJson("{\"id\":\"user-id\",\"sni\":\"old.example\",\"publicKey\":\"keep\"}");
        node.setStatus("0");
        return node;
    }

    @Test
    void validatesDomainsAndPreservesMissingParameterCompatibility() {
        assertNull(RealitySniSupport.requestedSni(Collections.emptyMap(), "VMess-TCP"));
        assertEquals("www.bing.com", RealitySniSupport.validate(" www.bing.com "));
        for (Object invalid : new Object[]{null, 123, "", "bing", "127.0.0.1", "https://bing.com", "bing.com:443",
                "bing.com/path", "bad_.com", "-bad.com", "a..com", "a b.com"}) {
            assertThrows(IllegalArgumentException.class, () -> RealitySniSupport.validate(invalid), String.valueOf(invalid));
        }
        Map<String, Object> request = new HashMap<>();
        request.put("sni", null);
        assertThrows(IllegalArgumentException.class, () -> RealitySniSupport.requestedSni(request, "VLESS-REALITY"));
        request.put("sni", "www.bing.com");
        assertThrows(IllegalArgumentException.class, () -> RealitySniSupport.requestedSni(request, "VMess-TCP"));
    }

    @Test
    void patchesOnlyTheTwoSniFields() {
        JSONObject expected = JSON.parseObject(CONFIG);
        JSONObject tls = expected.getJSONArray("inbounds").getJSONObject(0).getJSONObject("tls");
        tls.put("server_name", "www.bing.com");
        tls.getJSONObject("reality").getJSONObject("handshake").put("server", "www.bing.com");
        assertEquals(expected, JSON.parseObject(RealitySniSupport.patchInbound(CONFIG, node(), "www.bing.com")));
        JSONObject metadata = JSON.parseObject(RealitySniSupport.withSni(node().getConfigJson(), "www.bing.com"));
        assertEquals("keep", metadata.getString("publicKey"));
        assertEquals("www.bing.com", metadata.getString("sni"));
    }

    @Test
    void rejectsMissingMismatchedOrAmbiguousRealityInbounds() {
        for (String invalid : new String[]{"{}", CONFIG.replace("20031", "20032"),
                CONFIG.replace("user-id", "another-user"), CONFIG.replace("\"type\":\"vless\"", "\"type\":\"vmess\""),
                CONFIG.replace("\"enabled\":true", "\"enabled\":false"), CONFIG.replace("\"handshake\"", "\"other\"")}) {
            assertThrows(IllegalArgumentException.class, () -> RealitySniSupport.patchInbound(invalid, node(), "www.bing.com"));
        }
        JSONObject root = JSON.parseObject(CONFIG);
        root.getJSONArray("inbounds").add(JSON.parseObject(CONFIG).getJSONArray("inbounds").getJSONObject(0));
        assertThrows(IllegalArgumentException.class, () -> RealitySniSupport.patchInbound(root.toJSONString(), node(), "www.bing.com"));
    }

    @Test
    void readsSavedSniThenUrlWithoutDefaultingExistingNodes() {
        ProxyNode node = node();
        node.setUrl("vless://user-id@example.com:443?sni=www%2Ebing.com#node");
        assertEquals("old.example", RealitySniSupport.readSni(node));
        node.setConfigJson("{}");
        assertEquals("www.bing.com", RealitySniSupport.readSni(node));
        node.setUrl(null);
        assertEquals("", RealitySniSupport.readSni(node));
    }
}
