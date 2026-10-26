package com.skyway.web.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.mapper.ProxyNodeMapper;
import com.skyway.resource.service.impl.ProxyNodeServiceImpl;
import com.skyway.web.service.*;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class SshWebSocketSniTest {
    @Test
    void websocketCreateAppliesSniBeforeSavingAndReturnsUpdatedLinkAndConfig() throws Exception {
        Fixture f = new Fixture();
        f.run("{\"type\":\"add_proxy_node\",\"reqId\":1,\"customerId\":9,\"port\":20031,\"sni\":\" www.bing.com \"}");
        JSONObject created = f.messages.stream().filter(m -> "node_created".equals(m.getString("type"))).findFirst().get().getJSONObject("node");
        assertTrue(created.getString("url").contains("sni=www.bing.com"));
        assertEquals("www.bing.com", JSON.parseObject(created.getString("configJson")).getString("sni"));
        org.mockito.InOrder order = inOrder(f.remote, f.mapper);
        order.verify(f.remote).applyRealitySniToProxyNodeConfig(any(), eq("www.bing.com"));
        order.verify(f.mapper).insert(any());
    }

    @Test
    void websocketMissingParameterKeepsScriptSni() throws Exception {
        Fixture f = new Fixture();
        f.run("{\"type\":\"add_proxy_node\",\"customerId\":9,\"port\":20031}");
        verify(f.remote, never()).applyRealitySniToProxyNodeConfig(any(), any());
        JSONObject created = f.messages.stream().filter(m -> "node_created".equals(m.getString("type"))).findFirst().get().getJSONObject("node");
        assertTrue(created.getString("url").contains("sni=old.example"));
    }

    @Test
    void websocketRejectsEmptyInvalidAndWrongProtocolSniBeforeSsh() throws Exception {
        for (String parameters : new String[]{"\"sni\":null", "\"sni\":\"\"", "\"sni\":\"https://bing.com\"",
                "\"sni\":\"www.bing.com\",\"nodeType\":\"VMess-TCP\""}) {
            Fixture f = new Fixture();
            f.run("{\"type\":\"add_proxy_node\",\"customerId\":9,\"port\":20031," + parameters + "}");
            assertTrue(f.messages.stream().anyMatch(m -> "exec_error".equals(m.getString("type"))));
            verify(f.handler, never()).createSshClient(anyLong());
            verify(f.mapper, never()).insert(any());
        }
    }

    @Test
    void websocketCleansUpRemoteNodeWhenSniRestartFails() throws Exception {
        Fixture f = new Fixture();
        doThrow(new IOException("restart failed")).when(f.remote).applyRealitySniToProxyNodeConfig(any(), eq("www.bing.com"));
        f.run("{\"type\":\"add_proxy_node\",\"customerId\":9,\"port\":20031,\"sni\":\"www.bing.com\"}");
        verify(f.remote).removeProxyNodeFromServer(any());
        verify(f.mapper, never()).insert(any());
        assertTrue(f.messages.stream().anyMatch(m -> "exec_error".equals(m.getString("type"))));
    }

    private static class Fixture {
        final SshWebSocketHandler handler = spy(new SshWebSocketHandler());
        final VpsSshCommandService remote = mock(VpsSshCommandService.class);
        final ProxyNodeMapper mapper = mock(ProxyNodeMapper.class);
        final WebSocketSession ws = mock(WebSocketSession.class);
        final List<JSONObject> messages = new CopyOnWriteArrayList<>();
        final CountDownLatch done = new CountDownLatch(1);

        Fixture() throws Exception {
            ProxyNodeServiceImpl service = new ProxyNodeServiceImpl();
            ReflectionTestUtils.setField(service, "proxyNodeMapper", mapper);
            when(mapper.insert(any(ProxyNode.class))).thenReturn(1);
            ReflectionTestUtils.setField(handler, "proxyNodeService", service);
            ReflectionTestUtils.setField(handler, "vpsSshCommandService", remote);
            ReflectionTestUtils.setField(handler, "vpsInstanceOperationCoordinator", new VpsInstanceOperationCoordinator());
            ReflectionTestUtils.setField(handler, "vpsPortAvailabilityService", mock(VpsPortAvailabilityService.class));
            ReflectionTestUtils.setField(handler, "proxyDomainWhitelistService", mock(ProxyDomainWhitelistService.class));
            when(ws.isOpen()).thenReturn(true);
            when(ws.getAttributes()).thenReturn(Collections.singletonMap("instanceId", 7L));
            doAnswer(invocation -> {
                JSONObject message = JSON.parseObject(((TextMessage) invocation.getArgument(0)).getPayload());
                messages.add(message);
                if ("node_created".equals(message.getString("type"))
                        || ("exec_end".equals(message.getString("type")) && message.getIntValue("code") != 0)) done.countDown();
                return null;
            }).when(ws).sendMessage(any());
            SSHClient ssh = mock(SSHClient.class);
            doReturn(ssh).when(handler).createSshClient(7L);
            Session session = mock(Session.class);
            when(ssh.startSession()).thenReturn(session);
            when(session.exec(anyString())).thenAnswer(invocation -> {
                String command = invocation.getArgument(0);
                String output = command.contains("command -v sb") ? "/usr/bin/sb"
                        : command.startsWith("mv ") ? "" : "地址 (address) = 203.0.113.8\n用户ID (id) = user-id\n"
                        + "SNI (serverName) = old.example\nvless://user-id@203.0.113.8:20031?sni=old.example#node\n";
                Command cmd = mock(Command.class);
                when(cmd.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
                when(cmd.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                when(cmd.getExitStatus()).thenReturn(0);
                return cmd;
            });
        }

        void run(String json) throws Exception {
            handler.handleTextMessage(ws, new TextMessage(json));
            assertTrue(done.await(5, TimeUnit.SECONDS), "WebSocket operation did not finish");
        }
    }
}
