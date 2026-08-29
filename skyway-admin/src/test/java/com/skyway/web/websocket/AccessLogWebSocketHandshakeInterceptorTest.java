package com.skyway.web.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import com.skyway.common.core.domain.model.LoginUser;
import com.skyway.framework.web.service.TokenService;

@ExtendWith(MockitoExtension.class)
public class AccessLogWebSocketHandshakeInterceptorTest {

    @Mock
    private TokenService tokenService;
    @Mock
    private WebSocketHandler handler;
    @InjectMocks
    private AccessLogWebSocketHandshakeInterceptor interceptor;

    private LoginUser viewer;

    @BeforeEach
    public void setUp() {
        viewer = new LoginUser();
        viewer.setPermissions(Collections.singleton("resource:vps:list"));
    }

    @Test
    public void rejectsInvalidToken() throws Exception {
        MockExchange exchange = exchange("ws://localhost/ws/access-log?scope=vps&instanceId=1&token=bad");

        assertFalse(interceptor.beforeHandshake(exchange.request, exchange.response, handler, new HashMap<>()));
        verify(exchange.response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void rejectsInsufficientPermission() throws Exception {
        LoginUser denied = new LoginUser();
        denied.setPermissions(Collections.singleton("resource:vps:edit"));
        when(tokenService.getLoginUserByToken("good")).thenReturn(denied);
        MockExchange exchange = exchange("ws://localhost/ws/access-log?scope=vps&instanceId=1&token=good");

        assertFalse(interceptor.beforeHandshake(exchange.request, exchange.response, handler, new HashMap<>()));
        verify(exchange.response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    public void acceptsVpsScopeAndStoresTrustedId() throws Exception {
        when(tokenService.getLoginUserByToken("good")).thenReturn(viewer);
        MockExchange exchange = exchange("ws://localhost/ws/access-log?scope=vps&instanceId=42&token=good");
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(interceptor.beforeHandshake(exchange.request, exchange.response, handler, attributes));
        assertEquals("vps", attributes.get(AccessLogWebSocketHandshakeInterceptor.ATTR_SCOPE));
        assertEquals(42L, attributes.get(AccessLogWebSocketHandshakeInterceptor.ATTR_INSTANCE_ID));
    }

    @Test
    public void acceptsNodeScopeAndRejectsMissingNodeId() throws Exception {
        when(tokenService.getLoginUserByToken("good")).thenReturn(viewer);
        MockExchange valid = exchange("ws://localhost/ws/access-log?scope=node&nodeId=7&token=good");
        Map<String, Object> attributes = new HashMap<>();
        assertTrue(interceptor.beforeHandshake(valid.request, valid.response, handler, attributes));
        assertEquals(7L, attributes.get(AccessLogWebSocketHandshakeInterceptor.ATTR_NODE_ID));

        MockExchange invalid = exchange("ws://localhost/ws/access-log?scope=node&token=good");
        assertFalse(interceptor.beforeHandshake(invalid.request, invalid.response, handler, new HashMap<>()));
        verify(invalid.response).setStatusCode(HttpStatus.BAD_REQUEST);
    }

    private static MockExchange exchange(String uri) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        return new MockExchange(request, response);
    }

    private static final class MockExchange {
        private final ServerHttpRequest request;
        private final ServerHttpResponse response;

        private MockExchange(ServerHttpRequest request, ServerHttpResponse response) {
            this.request = request;
            this.response = response;
        }
    }
}
