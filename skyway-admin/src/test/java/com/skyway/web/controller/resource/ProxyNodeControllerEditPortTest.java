package com.skyway.web.controller.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.skyway.common.constant.HttpStatus;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.core.domain.entity.SysUser;
import com.skyway.common.core.domain.model.LoginUser;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.ProxyNodeRateLimit;
import com.skyway.resource.service.IProxyNodeRateLimitService;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeTrafficService;
import com.skyway.web.service.VpsSshCommandService;
import com.skyway.web.service.VpsSshCommandService.PortRateLimitRemoteResult;

@ExtendWith(MockitoExtension.class)
public class ProxyNodeControllerEditPortTest {

    @Mock
    private IProxyNodeService proxyNodeService;

    @Mock
    private VpsSshCommandService vpsSshCommandService;

    @Mock
    private IProxyNodeTrafficService proxyNodeTrafficService;

    @Mock
    private IProxyNodeRateLimitService proxyNodeRateLimitService;

    @InjectMocks
    private ProxyNodeController controller;

    @BeforeEach
    public void setUpSecurity() {
        SysUser user = new SysUser();
        user.setUserName("tester");
        LoginUser loginUser = new LoginUser();
        ReflectionTestUtils.setField(loginUser, "user", user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    public void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void editRejectsPortOutsideValidRange() throws Exception {
        ProxyNode existing = existingNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 70000);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        assertEquals("端口范围为 1-65535", result.get(AjaxResult.MSG_TAG));
        verify(vpsSshCommandService, never()).updateProxyNodeConfigPortAndName(any(), any(), any(), any(Boolean.class), any(), any());
        verify(proxyNodeService, never()).update(any(ProxyNode.class));
    }

    @Test
    public void editRejectsPortAlreadyUsedByAnotherNodeOnSameInstance() throws Exception {
        ProxyNode existing = existingNode();
        ProxyNode conflict = existingNode();
        conflict.setId(11L);
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(conflict);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        assertEquals("端口 10088 已被当前 VPS 的其他节点使用", result.get(AjaxResult.MSG_TAG));
        verify(vpsSshCommandService, never()).updateProxyNodeConfigPortAndName(any(), any(), any(), any(Boolean.class), any(), any());
        verify(proxyNodeService, never()).update(any(ProxyNode.class));
    }

    @Test
    public void editPortUpdatesRemoteConfigBeforeSavingNewPortAndNodeName() throws Exception {
        ProxyNode existing = existingNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(null);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(1);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);
        body.put("expireTime", "2026-08-09 10:00:00");
        body.put("url", "vless://old-url-with-old-port");

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<ProxyNode> rowCaptor = ArgumentCaptor.forClass(ProxyNode.class);
        InOrder inOrder = inOrder(vpsSshCommandService, proxyNodeService);
        inOrder.verify(vpsSshCommandService).updateProxyNodeConfigPortAndName(
                20L,
                "VLESS-REALITY-203.0.113.8-10001-5-20260701",
                "VLESS-REALITY-203.0.113.8-10088-5-20260809",
                false,
                10001,
                10088);
        inOrder.verify(proxyNodeService).update(rowCaptor.capture());

        ProxyNode saved = rowCaptor.getValue();
        assertEquals(10088, saved.getPort());
        assertEquals("VLESS-REALITY-203.0.113.8-10088-5-20260809", saved.getNodeName());
        assertEquals(null, saved.getUrl());
        assertEquals("tester", saved.getUpdateBy());
    }

    @Test
    public void editPortMovesTrafficRulesWhenNodeHasNoActiveRateLimit() throws Exception {
        ProxyNode existing = existingNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(null);
        when(proxyNodeRateLimitService.getActiveByNodeId(10L)).thenReturn(null);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(1);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        verify(vpsSshCommandService).removeTrafficRulesForPort(20L, 10001);
        verify(vpsSshCommandService).ensureTrafficRulesForPort(20L, 10088);
        verify(vpsSshCommandService, never()).setPortRateLimit(any(), any(Integer.class), any(Integer.class), any(Integer.class));
        verify(proxyNodeRateLimitService, never()).saveActive(any(ProxyNodeRateLimit.class));
    }

    @Test
    public void editPortReappliesActiveRateLimitOnNewPort() throws Exception {
        ProxyNode existing = existingNode();
        ProxyNodeRateLimit activeLimit = new ProxyNodeRateLimit();
        activeLimit.setId(30L);
        activeLimit.setInstanceId(20L);
        activeLimit.setProxyNodeId(10L);
        activeLimit.setPort(10001);
        activeLimit.setDownloadMbps(50);
        activeLimit.setUploadMbps(20);
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(null);
        when(proxyNodeRateLimitService.getActiveByNodeId(10L)).thenReturn(activeLimit);
        when(vpsSshCommandService.removePortRateLimit(20L, 10001))
                .thenReturn(new PortRateLimitRemoteResult(10001, "old removed"));
        when(vpsSshCommandService.setPortRateLimit(20L, 10088, 50, 20))
                .thenReturn(new PortRateLimitRemoteResult(10088, "new applied"));
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(1);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<ProxyNodeRateLimit> limitCaptor = ArgumentCaptor.forClass(ProxyNodeRateLimit.class);
        verify(vpsSshCommandService).removePortRateLimit(20L, 10001);
        verify(vpsSshCommandService).setPortRateLimit(20L, 10088, 50, 20);
        verify(vpsSshCommandService).removeTrafficRulesForPort(20L, 10001);
        verify(vpsSshCommandService).ensureTrafficRulesForPort(20L, 10088);
        verify(proxyNodeRateLimitService).saveActive(limitCaptor.capture());

        ProxyNodeRateLimit savedLimit = limitCaptor.getValue();
        assertEquals(10088, savedLimit.getPort());
        assertEquals("new applied", savedLimit.getLastApplyResult());
        assertEquals("tester", savedLimit.getUpdateBy());
    }

    private ProxyNode existingNode() {
        ProxyNode node = new ProxyNode();
        node.setId(10L);
        node.setInstanceId(20L);
        node.setCustomerId(5L);
        node.setNodeName("VLESS-REALITY-203.0.113.8-10001-5-20260701");
        node.setNodeType("VLESS-REALITY");
        node.setAddress("203.0.113.8");
        node.setPort(10001);
        node.setUrl("vless://old-url");
        node.setConfigJson("{\"protocol\":\"vless\"}");
        node.setStatus("0");
        node.setCustomId("5");
        return node;
    }
}
