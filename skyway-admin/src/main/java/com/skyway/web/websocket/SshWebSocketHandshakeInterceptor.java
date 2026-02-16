package com.skyway.web.websocket;

import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * SSH WebSocket 握手拦截器：校验 JWT 与 VPS 连接权限（list/query 或 *:*:*），并传递 instanceId
 *
 * @author ruoyi
 */
@Component
public class SshWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SshWebSocketHandshakeInterceptor.class);
    private static final String ATTR_INSTANCE_ID = "instanceId";
    /** 具备任一即可通过握手：列表、查询或全量权限 */
    private static final String PERMISSION_VPS_LIST = "resource:vps:list";
    private static final String PERMISSION_VPS_QUERY = "resource:vps:query";

    @Autowired
    private TokenService tokenService;

    /** 是否具备 SSH 连接所需权限（resource:vps:list / resource:vps:query / *:*:* 任一即可） */
    private static boolean hasSshConnectPermission(Set<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) return false;
        return permissions.contains(Constants.ALL_PERMISSION)
                || permissions.contains(PERMISSION_VPS_LIST)
                || permissions.contains(PERMISSION_VPS_QUERY);
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        Map<String, String> params = request.getURI().getQuery() != null
                ? parseQuery(request.getURI().getQuery())
                : java.util.Collections.emptyMap();
        String token = params.get("token");
        String instanceIdStr = params.get("instanceId");

        if (StringUtils.isEmpty(token)) {
            log.warn("SSH WebSocket 握手拒绝 [token为空] instanceId={}", instanceIdStr);
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        LoginUser loginUser = tokenService.getLoginUserByToken(token);
        if (loginUser == null) {
            log.warn("SSH WebSocket 握手拒绝 [token无效或已过期] instanceId={}", instanceIdStr);
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        Set<String> permissions = loginUser.getPermissions();
        if (!hasSshConnectPermission(permissions)) {
            int permCount = permissions == null ? 0 : permissions.size();
            log.warn("SSH WebSocket 握手拒绝 [权限不足] username={}, instanceId={}, 需 resource:vps:list 或 resource:vps:query 或 *:*:*，当前权限数={}",
                    loginUser.getUsername(), instanceIdStr, permCount);
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }
        if (StringUtils.isEmpty(instanceIdStr)) {
            log.warn("SSH WebSocket 握手拒绝 [instanceId缺失] username={}", loginUser.getUsername());
            response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
            return false;
        }
        try {
            Long instanceId = Long.parseLong(instanceIdStr);
            attributes.put(ATTR_INSTANCE_ID, instanceId);
            log.debug("SSH WebSocket 握手通过: username={}, instanceId={}", loginUser.getUsername(), instanceId);
            return true;
        } catch (NumberFormatException e) {
            log.warn("SSH WebSocket 握手拒绝 [instanceId格式错误] instanceId={}, username={}",
                    instanceIdStr, loginUser.getUsername());
            response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new java.util.HashMap<>();
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = pair.substring(0, eq).trim();
                String value = pair.substring(eq + 1).trim();
                try {
                    value = java.net.URLDecoder.decode(value, "UTF-8");
                } catch (Exception ignored) {
                }
                map.put(key, value);
            }
        }
        return map;
    }
}
