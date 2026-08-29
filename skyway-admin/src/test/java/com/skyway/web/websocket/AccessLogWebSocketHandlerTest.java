package com.skyway.web.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import com.skyway.web.websocket.AccessLogParser.AccessLogEntry;
import com.skyway.resource.domain.ProxyNode;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

public class AccessLogWebSocketHandlerTest {

    @Test
    public void overlapRemovalPreservesLegitimateDuplicateCounts() {
        List<AccessLogEntry> history = Arrays.asList(entry("same"), entry("same"), entry("old"));
        List<AccessLogEntry> buffered = Arrays.asList(entry("same"), entry("same"), entry("same"), entry("new"));

        List<AccessLogEntry> result = AccessLogWebSocketHandler.removeHistoryOverlap(history, buffered);

        assertEquals(2, result.size());
        assertEquals("same", result.get(0).getRawLine());
        assertEquals("new", result.get(1).getRawLine());
    }

    @Test
    public void cleanupClosesFollowSessionAndSshClient() throws Exception {
        AccessLogWebSocketHandler handler = new AccessLogWebSocketHandler();
        WebSocketSession webSocket = mock(WebSocketSession.class);
        Session followSession = mock(Session.class);
        SSHClient ssh = mock(SSHClient.class);
        AtomicBoolean closed = new AtomicBoolean(false);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("accessLogClosed", closed);
        attributes.put("accessLogFollowSession", followSession);
        attributes.put("accessLogSsh", ssh);
        org.mockito.Mockito.when(webSocket.getAttributes()).thenReturn(attributes);

        handler.cleanup(webSocket);

        assertTrue(closed.get());
        verify(followSession).close();
        verify(ssh).close();
        assertEquals(false, attributes.containsKey("accessLogFollowSession"));
        assertEquals(false, attributes.containsKey("accessLogSsh"));
    }

    @Test
    public void enrichmentIncludesCustomerNameResolvedFromCustomerId() {
        AccessLogEntry entry = entry("raw");
        ProxyNode node = new ProxyNode();
        node.setId(88L);
        node.setNodeName("VLESS-REALITY-154.219.122.185-10003-49-20260822");
        node.setPort(10003);
        node.setCustomerId(49L);

        AccessLogWebSocketHandler.enrichEntry(entry, node, "示例客户");

        assertEquals(49L, entry.getCustomerId());
        assertEquals("示例客户", entry.getCustomerName());
        assertEquals(node.getNodeName(), entry.getNodeName());
    }

    private static AccessLogEntry entry(String rawLine) {
        AccessLogEntry entry = new AccessLogEntry();
        entry.setRawLine(rawLine);
        return entry;
    }
}
