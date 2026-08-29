package com.skyway.web.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson2.JSON;
import com.skyway.web.websocket.AccessLogParser.AccessLogEntry;

public class AccessLogParserTest {

    @Test
    public void parsesVlessDomainDestination() {
        AccessLogEntry entry = AccessLogParser.parse(
                "2026-08-29 12:03:04 +0800 INFO inbound/vless[VLESS-REALITY-3000.json]: [0] inbound connection to example.com:443",
                null);

        assertNotNull(entry);
        assertEquals("2026-08-29 12:03:04 +0800", entry.getTimestamp());
        assertEquals("vless", entry.getProtocol());
        assertEquals("VLESS-REALITY-3000.json", entry.getInboundTag());
        assertEquals("example.com", entry.getDestinationHost());
        assertEquals(443, entry.getDestinationPort());
        assertEquals(false, JSON.toJSONString(entry).contains("rawLine"));
    }

    @Test
    public void parsesVmessIpv4Destination() {
        AccessLogEntry entry = AccessLogParser.parse(
                "2026-08-29 12:03:04 UTC INFO inbound/vmess[VMess-TCP-10003.json]: [0] inbound connection to 203.0.113.8:80",
                "VMess-TCP-10003.json");

        assertNotNull(entry);
        assertEquals("vmess", entry.getProtocol());
        assertEquals("203.0.113.8", entry.getDestinationHost());
        assertEquals(80, entry.getDestinationPort());
    }

    @Test
    public void parsesBracketedIpv6Destination() {
        AccessLogEntry entry = AccessLogParser.parse(
                "2026-08-29 12:03:04 +00:00 INFO inbound/vless[VLESS-REALITY-3000.json]: [0] inbound connection to [2001:db8::10]:8443",
                null);

        assertNotNull(entry);
        assertEquals("2001:db8::10", entry.getDestinationHost());
        assertEquals(8443, entry.getDestinationPort());
    }

    @Test
    public void ignoresUnrelatedAndWrongTagLines() {
        assertNull(AccessLogParser.parse("2026-08-29 service started", null));
        assertNull(AccessLogParser.parse(
                "2026-08-29 12:03:04 +0800 INFO inbound/vless[VLESS-REALITY-3001.json]: [0] inbound connection to example.com:443",
                "VLESS-REALITY-3000.json"));
    }
}
