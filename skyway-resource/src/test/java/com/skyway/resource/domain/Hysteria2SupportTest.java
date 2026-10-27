package com.skyway.resource.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.net.URI;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.mapper.ProxyNodeMapper;
import com.skyway.resource.service.impl.ProxyNodeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class Hysteria2SupportTest {
    private static final String PIN = String.join("", java.util.Collections.nCopies(32, "AB"));

    @Test
    void parsesColoredSbOutputWithoutSniAndRoundTripsEncodedCredentialsAndIpv6() throws Exception {
        String output = "密码 (password) = unused\n\u001B[4;41mhy2://a%40b%3Ac%2B%20d%23%26@[2001:db8::1]:32332"
                + "?alpn=h3&insecure=1&allowInsecure=1&pinSHA256=" + PIN + "#test\u001B[0m\n";
        ProxyNode node = Hysteria2Support.parseSbOutput(output, 32332);
        assertNotNull(node);
        JSONObject config = JSON.parseObject(node.getConfigJson());
        assertEquals("a@b:c+ d#&", config.getString("password"));
        assertEquals("udp", config.getString("network"));
        assertEquals(PIN, config.getString("pinSHA256"));
        assertTrue(config.getBooleanValue("insecure"));
        assertFalse(config.containsKey("sni"));
        assertEquals("2001:db8::1", node.getAddress());
        node.setNodeName("测试 节点+#");
        String url = Hysteria2Support.buildShareUrl(node, config);
        assertTrue(url.contains("[2001:db8::1]:32332"));
        assertTrue(url.endsWith("#%E6%B5%8B%E8%AF%95%20%E8%8A%82%E7%82%B9%2B%23"));
        assertEquals(config, JSON.parseObject(Hysteria2Support.parseSbOutput(url, 32332).getConfigJson()));
        assertFalse(new URI(url).getRawQuery().contains("sni="));
    }

    @Test
    void rejectsMalformedCredentialsPortsAndCertificatePins() {
        assertNull(Hysteria2Support.parseSbOutput("hysteria2://@example.com:32332", 32332));
        assertNull(Hysteria2Support.parseSbOutput("hysteria2://pw@example.com:32331", 32332));
        assertNull(Hysteria2Support.parseSbOutput("hysteria2://pw@example.com:32332?pinSHA256=bad", 32332));
        assertNull(Hysteria2Support.parseSbOutput("hysteria2://%XX@example.com:32332", 32332));
    }

    @Test
    void insertAndPortRenameUpdateRebuildShareUriFromStoredParameters() {
        ProxyNode node = Hysteria2Support.parseSbOutput("hysteria2://pw@example.com:32332?alpn=h3&insecure=1&pinSHA256=" + PIN, 32332);
        ProxyNodeMapper mapper = mock(ProxyNodeMapper.class);
        when(mapper.insert(any())).thenReturn(1);
        when(mapper.update(any())).thenReturn(1);
        ProxyNodeServiceImpl service = new ProxyNodeServiceImpl();
        ReflectionTestUtils.setField(service, "proxyNodeMapper", mapper);
        node.setNodeName("customer-permanent");
        service.insert(node);
        assertTrue(node.getUrl().endsWith("#customer-permanent"));
        node.setPort(32333);
        node.setUrl(null);
        service.update(node);
        assertTrue(node.getUrl().contains(":32333?"));
        assertTrue(node.getUrl().contains("pinSHA256=" + PIN));
        verify(mapper).update(node);
    }

    @Test
    void protocolCapabilitiesShareCommandAndTransportAcrossEntrypoints() {
        assertEquals("sb add hy2 32332", ProxyNodeProtocol.createCommand("Hysteria2", 32332));
        assertEquals("udp", ProxyNodeProtocol.transport("Hysteria2"));
        assertTrue(ProxyNodeProtocol.supportsRelay("Hysteria2"));
        assertFalse(ProxyNodeProtocol.supportsRelay("VMess-TCP"));
        assertThrows(IllegalArgumentException.class, () -> ProxyNodeProtocol.createCommand("Hysteria2", 0));
    }
}
