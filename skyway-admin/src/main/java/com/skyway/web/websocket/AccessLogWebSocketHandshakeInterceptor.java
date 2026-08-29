package com.skyway.web.websocket;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.skyway.common.constant.Constants;
import com.skyway.common.core.domain.model.LoginUser;
import com.skyway.common.utils.StringUtils;
import com.skyway.framework.web.service.TokenService;

/** Authenticates and validates the read-only access-log WebSocket request. */
@Component
public class AccessLogWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_SCOPE = "accessLogScope";
    public static final String ATTR_INSTANCE_ID = "accessLogInstanceId";
    public static final String ATTR_NODE_ID = "accessLogNodeId";
    private static final Logger log = LoggerFactory.getLogger(AccessLogWebSocketHandshakeInterceptor.class);
    private static final String PERMISSION_VPS_LIST = "resource:vps:list";
    private static final String PERMISSION_VPS_QUERY = "resource:vps:query";

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Map<String, String> params = parseQuery(request.getURI().getQuery());
        LoginUser loginUser = tokenService.getLoginUserByToken(params.get("token"));
        if (loginUser == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        if (!hasViewPermission(loginUser.getPermissions())) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        String scope = params.get("scope");
        if ("vps".equals(scope)) {
            Long instanceId = parsePositiveLong(params.get("instanceId"));
            if (instanceId == null) {
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }
            attributes.put(ATTR_SCOPE, scope);
            attributes.put(ATTR_INSTANCE_ID, instanceId);
            return true;
        }
        if ("node".equals(scope)) {
            Long nodeId = parsePositiveLong(params.get("nodeId"));
            if (nodeId == null) {
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }
            attributes.put(ATTR_SCOPE, scope);
            attributes.put(ATTR_NODE_ID, nodeId);
            return true;
        }
        log.warn("Access log WebSocket rejected: invalid scope={}", scope);
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    static boolean hasViewPermission(Set<String> permissions) {
        return !CollectionUtils.isEmpty(permissions)
                && (permissions.contains(Constants.ALL_PERMISSION)
                || permissions.contains(PERMISSION_VPS_LIST)
                || permissions.contains(PERMISSION_VPS_QUERY));
    }

    private static Long parsePositiveLong(String value) {
        if (StringUtils.isEmpty(value)) return null;
        try {
            long result = Long.parseLong(value);
            return result > 0 ? result : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String key = pair.substring(0, eq).trim();
            String value = pair.substring(eq + 1).trim();
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (Exception ignored) {
            }
            result.put(key, value);
        }
        return result;
    }
}
