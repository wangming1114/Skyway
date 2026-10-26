package com.skyway.web.controller.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
        ReflectionTestUtils.setField(controller, "vpsInstanceOperationCoordinator",
                new com.skyway.web.service.VpsInstanceOperationCoordinator());
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
        assertEquals(saved, result.get(AjaxResult.DATA_TAG));
    }

    @Test
    public void editPortStillUpdatesRemoteConfigWhenStoredNameAlreadyMatchesGeneratedName() throws Exception {
        ProxyNode existing = existingNode();
        existing.setNodeName("VLESS-REALITY-203.0.113.8-10088-5-permanent");
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(null);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(1);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        verify(vpsSshCommandService).updateProxyNodeConfigPortAndName(
                20L,
                "VLESS-REALITY-203.0.113.8-10088-5-permanent",
                "VLESS-REALITY-203.0.113.8-10088-5-permanent",
                false,
                10001,
                10088);
    }

    @Test
    public void editPortWithPrecomputedNameRollsBackRemoteConfigWhenDatabaseUpdateFails() throws Exception {
        ProxyNode existing = existingNode();
        existing.setNodeName("VLESS-REALITY-203.0.113.8-10088-5-permanent");
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(null);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(0);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        InOrder inOrder = inOrder(vpsSshCommandService, proxyNodeService);
        inOrder.verify(vpsSshCommandService).updateProxyNodeConfigPortAndName(
                20L, existing.getNodeName(), existing.getNodeName(), false, 10001, 10088);
        inOrder.verify(proxyNodeService).update(any(ProxyNode.class));
        inOrder.verify(vpsSshCommandService).updateProxyNodeConfigPortAndName(
                20L, existing.getNodeName(), existing.getNodeName(), false, 10088, 10001);
    }

    @Test
    public void editRenamesStaleRemoteConfigBeforeSavingDatabaseNodeName() throws Exception {
        ProxyNode existing = existingNode();
        existing.setNodeName("stale-node-name");
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(1);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("remark", "updated remark");

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<ProxyNode> rowCaptor = ArgumentCaptor.forClass(ProxyNode.class);
        InOrder inOrder = inOrder(vpsSshCommandService, proxyNodeService);
        inOrder.verify(vpsSshCommandService).renameProxyNodeConfig(
                20L, "stale-node-name", "VLESS-REALITY-203.0.113.8-10001-5-permanent", false);
        inOrder.verify(proxyNodeService).update(rowCaptor.capture());
        assertEquals("VLESS-REALITY-203.0.113.8-10001-5-permanent", rowCaptor.getValue().getNodeName());
    }

    @Test
    public void editPortDoesNotUpdateDatabaseWhenRemoteConfigUpdateFails() throws Exception {
        ProxyNode existing = existingNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(null);
        doThrow(new RuntimeException("remote update failed"))
                .when(vpsSshCommandService).updateProxyNodeConfigPortAndName(
                        20L,
                        "VLESS-REALITY-203.0.113.8-10001-5-20260701",
                        "VLESS-REALITY-203.0.113.8-10088-5-permanent",
                        false,
                        10001,
                        10088);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        assertEquals("服务器配置更新失败: remote update failed", result.get(AjaxResult.MSG_TAG));
        verify(proxyNodeService, never()).update(any(ProxyNode.class));
        verify(vpsSshCommandService, never()).removeTrafficRulesForPort(20L, 10001);
        verify(vpsSshCommandService, never()).ensureTrafficRulesForPort(20L, 10088);
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
    public void editPortRollsBackRemoteIdentityWhenTrafficRuleMigrationFails() throws Exception {
        ProxyNode existing = existingNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(null);
        when(proxyNodeRateLimitService.getActiveByNodeId(10L)).thenReturn(null);
        doThrow(new RuntimeException("traffic remove failed"))
                .when(vpsSshCommandService).removeTrafficRulesForPort(20L, 10001);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        assertEquals("端口规则迁移失败: traffic remove failed", result.get(AjaxResult.MSG_TAG));
        verify(vpsSshCommandService).updateProxyNodeConfigPortAndName(
                20L,
                "VLESS-REALITY-203.0.113.8-10088-5-permanent",
                "VLESS-REALITY-203.0.113.8-10001-5-20260701",
                false,
                10088,
                10001);
        verify(proxyNodeService, never()).update(any(ProxyNode.class));
    }

    @Test
    public void editPortRollsBackRemoteConfigAndRulesWhenDatabaseUpdateFails() throws Exception {
        ProxyNode existing = existingNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.getByInstanceIdAndPort(20L, 10088)).thenReturn(null);
        when(proxyNodeRateLimitService.getActiveByNodeId(10L)).thenReturn(null);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(0);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("port", 10088);

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        verify(vpsSshCommandService).removeTrafficRulesForPort(20L, 10001);
        verify(vpsSshCommandService).ensureTrafficRulesForPort(20L, 10088);
        verify(vpsSshCommandService).removeTrafficRulesForPort(20L, 10088);
        verify(vpsSshCommandService).ensureTrafficRulesForPort(20L, 10001);
        verify(vpsSshCommandService).updateProxyNodeConfigPortAndName(
                20L,
                "VLESS-REALITY-203.0.113.8-10088-5-permanent",
                "VLESS-REALITY-203.0.113.8-10001-5-20260701",
                false,
                10088,
                10001);
    }

    @Test
    public void editPortReappliesActiveRateLimitOnNewPort() throws Exception {
        ProxyNode existing = existingNode();
        ProxyNodeRateLimit activeLimit = activeLimit();
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

    @Test
    public void editStatusDisablesRemoteConfigBeforeUpdatingDatabaseWithoutChangingNodeName() throws Exception {
        ProxyNode existing = existingNode();
        existing.setNodeName("stale-node-name");
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(1);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("status", "1");

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<ProxyNode> rowCaptor = ArgumentCaptor.forClass(ProxyNode.class);
        InOrder inOrder = inOrder(vpsSshCommandService, proxyNodeService);
        inOrder.verify(vpsSshCommandService).renameProxyNodeConfig(20L, "stale-node-name", true);
        inOrder.verify(proxyNodeService).update(rowCaptor.capture());
        assertEquals("stale-node-name", rowCaptor.getValue().getNodeName());
        assertEquals("1", rowCaptor.getValue().getStatus());
    }

    @Test
    public void editStatusEnablesRemoteConfigBeforeUpdatingDatabase() throws Exception {
        ProxyNode existing = existingNode();
        existing.setStatus("1");
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(1);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("status", "0");

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        InOrder inOrder = inOrder(vpsSshCommandService, proxyNodeService);
        inOrder.verify(vpsSshCommandService).renameProxyNodeConfig(
                20L, "VLESS-REALITY-203.0.113.8-10001-5-20260701", false);
        inOrder.verify(proxyNodeService).update(any(ProxyNode.class));
    }

    @Test
    public void editStatusDoesNotUpdateDatabaseWhenRemoteConfigRenameFails() throws Exception {
        ProxyNode existing = existingNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        doThrow(new RuntimeException("mv failed"))
                .when(vpsSshCommandService).renameProxyNodeConfig(20L, existing.getNodeName(), true);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("status", "1");

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        assertEquals("服务器配置重命名失败: mv failed", result.get(AjaxResult.MSG_TAG));
        verify(proxyNodeService, never()).update(any(ProxyNode.class));
    }

    @Test
    public void editStatusRollsBackRemoteConfigWhenDatabaseUpdateFails() throws Exception {
        ProxyNode existing = existingNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.update(any(ProxyNode.class))).thenReturn(0);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("status", "1");

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        InOrder inOrder = inOrder(vpsSshCommandService, proxyNodeService);
        inOrder.verify(vpsSshCommandService).renameProxyNodeConfig(20L, existing.getNodeName(), true);
        inOrder.verify(proxyNodeService).update(any(ProxyNode.class));
        inOrder.verify(vpsSshCommandService).renameProxyNodeConfig(20L, existing.getNodeName(), false);
    }

    @Test
    public void removeNodeRemovesActiveRateLimitBeforeDeletingNode() throws Exception {
        ProxyNode existing = existingNode();
        ProxyNodeRateLimit activeLimit = activeLimit();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeRateLimitService.getActiveByNodeId(10L)).thenReturn(activeLimit);
        when(vpsSshCommandService.removePortRateLimit(20L, 10001))
                .thenReturn(new PortRateLimitRemoteResult(10001, "rate removed"));

        AjaxResult result = controller.remove("10");

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        InOrder inOrder = inOrder(vpsSshCommandService, proxyNodeRateLimitService, proxyNodeTrafficService, proxyNodeService);
        inOrder.verify(vpsSshCommandService).removePortRateLimit(20L, 10001);
        inOrder.verify(proxyNodeRateLimitService).markRemoved(30L, "rate removed", "tester");
        inOrder.verify(vpsSshCommandService).removeProxyNodeFromServer(existing);
        inOrder.verify(proxyNodeTrafficService).deleteByNodeId(10L);
        inOrder.verify(proxyNodeService).deleteById(10L);
    }

    @Test
    public void removeNodeStopsWhenActiveRateLimitRemovalFails() throws Exception {
        ProxyNode existing = existingNode();
        ProxyNodeRateLimit activeLimit = activeLimit();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeRateLimitService.getActiveByNodeId(10L)).thenReturn(activeLimit);
        doThrow(new RuntimeException("tc remove failed"))
                .when(vpsSshCommandService).removePortRateLimit(20L, 10001);

        AjaxResult result = controller.remove("10");

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        assertEquals("删除失败: 限速移除失败: tc remove failed", result.get(AjaxResult.MSG_TAG));
        verify(vpsSshCommandService, never()).removeProxyNodeFromServer(any(ProxyNode.class));
        verify(proxyNodeTrafficService, never()).deleteByNodeId(any());
        verify(proxyNodeService, never()).deleteById(any());
        verify(proxyNodeRateLimitService, never()).markRemoved(any(), any(), any());
    }

    @Test
    public void editSniRegeneratesShareUrlAndPreservesRelayMetadata() throws Exception {
        ProxyNode existing = existingSniNode();
        com.skyway.resource.mapper.ProxyNodeMapper mapper = org.mockito.Mockito.mock(com.skyway.resource.mapper.ProxyNodeMapper.class);
        com.skyway.resource.service.impl.ProxyNodeServiceImpl realService = new com.skyway.resource.service.impl.ProxyNodeServiceImpl();
        ReflectionTestUtils.setField(realService, "proxyNodeMapper", mapper);
        ReflectionTestUtils.setField(controller, "proxyNodeService", realService);
        when(mapper.selectById(10L)).thenReturn(existing);
        when(mapper.update(any(ProxyNode.class))).thenReturn(1);
        Map<String, Object> body = sniRequest(" www.bing.com ");
        body.put("url", existing.getUrl());

        AjaxResult result = controller.edit(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        ProxyNode saved = (ProxyNode) result.get(AjaxResult.DATA_TAG);
        org.junit.jupiter.api.Assertions.assertTrue(saved.getUrl().contains("sni=www.bing.com"));
        org.junit.jupiter.api.Assertions.assertFalse(saved.getUrl().contains("old.example"));
        com.alibaba.fastjson2.JSONObject config = com.alibaba.fastjson2.JSON.parseObject(saved.getConfigJson());
        assertEquals("www.bing.com", config.getString("sni"));
        assertEquals("socks", config.getJSONObject("relay").getString("type"));
        verify(vpsSshCommandService).applyRealitySniToProxyNodeConfig(saved, "www.bing.com");
    }

    @Test
    public void invalidSniIsRejectedBeforeAnyRemoteMutation() {
        when(proxyNodeService.getById(10L)).thenReturn(existingSniNode());
        AjaxResult result = controller.edit(sniRequest("https://www.bing.com"));
        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        org.mockito.Mockito.verifyNoInteractions(vpsSshCommandService);
        verify(proxyNodeService, never()).update(any());
    }

    @Test
    public void failedSniUpdateRollsBackPortRelayAndStatusWithoutSaving() throws Exception {
        ProxyNode existing = existingSniNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        doThrow(new java.io.IOException("restart failed"))
                .when(vpsSshCommandService).applyRealitySniToProxyNodeConfig(any(), org.mockito.ArgumentMatchers.eq("www.bing.com"));
        Map<String, Object> body = sniRequest("www.bing.com");
        body.put("port", 10088);
        body.put("relayText", "relay.example:1080:user:password");
        body.put("status", "1");
        AjaxResult result = controller.edit(body);
        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        verify(proxyNodeService, never()).update(any());
        InOrder order = inOrder(vpsSshCommandService);
        order.verify(vpsSshCommandService).applySocks5RelayToProxyNodeConfig(org.mockito.ArgumentMatchers.eq(existing), any());
        order.verify(vpsSshCommandService).updateProxyNodeConfigPortAndName(20L, existing.getNodeName(),
                "VLESS-REALITY-203.0.113.8-10088-5-permanent", false, 10001, 10088);
        order.verify(vpsSshCommandService).renameProxyNodeConfig(20L, "VLESS-REALITY-203.0.113.8-10088-5-permanent", true);
        order.verify(vpsSshCommandService).applyRealitySniToProxyNodeConfig(any(), org.mockito.ArgumentMatchers.eq("www.bing.com"));
        order.verify(vpsSshCommandService).renameProxyNodeConfig(20L, "VLESS-REALITY-203.0.113.8-10088-5-permanent", false);
        order.verify(vpsSshCommandService).updateProxyNodeConfigPortAndName(20L, "VLESS-REALITY-203.0.113.8-10088-5-permanent",
                existing.getNodeName(), false, 10088, 10001);
        order.verify(vpsSshCommandService).applySocks5RelayToProxyNodeConfig(org.mockito.ArgumentMatchers.eq(existing), any());
    }

    @Test
    public void databaseFailureRestoresExactSniSnapshotBeforeIdentityRollback() throws Exception {
        ProxyNode existing = existingSniNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(vpsSshCommandService.applyRealitySniToProxyNodeConfig(any(), org.mockito.ArgumentMatchers.eq("www.bing.com")))
                .thenReturn("remote snapshot with different handshake");
        Map<String, Object> body = sniRequest("www.bing.com");
        body.put("status", "1");
        AjaxResult result = controller.edit(body);
        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        InOrder order = inOrder(vpsSshCommandService, proxyNodeService);
        order.verify(vpsSshCommandService).applyRealitySniToProxyNodeConfig(any(), org.mockito.ArgumentMatchers.eq("www.bing.com"));
        order.verify(proxyNodeService).update(any());
        order.verify(vpsSshCommandService).restoreRealitySniConfig(any(), org.mockito.ArgumentMatchers.eq("remote snapshot with different handshake"));
        order.verify(vpsSshCommandService).renameProxyNodeConfig(20L, existing.getNodeName(), false);
    }

    @Test
    public void databaseExceptionAlsoRestoresSniSnapshot() throws Exception {
        ProxyNode existing = existingSniNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(vpsSshCommandService.applyRealitySniToProxyNodeConfig(any(), org.mockito.ArgumentMatchers.eq("www.bing.com"))).thenReturn("snapshot");
        when(proxyNodeService.update(any())).thenThrow(new IllegalStateException("database failed"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> controller.edit(sniRequest("www.bing.com")));
        verify(vpsSshCommandService).restoreRealitySniConfig(any(), org.mockito.ArgumentMatchers.eq("snapshot"));
    }

    @Test
    public void unchangedSniPreservesManuallyEditedShareUrl() throws Exception {
        ProxyNode existing = existingSniNode();
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.update(any())).thenReturn(1);
        Map<String, Object> body = sniRequest("old.example");
        body.put("url", "custom share URL");
        AjaxResult result = controller.edit(body);
        assertEquals("custom share URL", ((ProxyNode) result.get(AjaxResult.DATA_TAG)).getUrl());
        org.mockito.Mockito.verifyNoInteractions(vpsSshCommandService);
    }

    @Test
    public void unchangedUrlFallbackSniDoesNotRequireSshOrRewriteLegacyMetadata() throws Exception {
        ProxyNode existing = existingSniNode();
        com.alibaba.fastjson2.JSONObject config = com.alibaba.fastjson2.JSON.parseObject(existing.getConfigJson());
        config.remove("sni");
        existing.setConfigJson(config.toJSONString());
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.update(any())).thenReturn(1);
        Map<String, Object> body = sniRequest("old.example");
        body.put("remark", "new remark");
        AjaxResult result = controller.edit(body);
        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        ProxyNode saved = (ProxyNode) result.get(AjaxResult.DATA_TAG);
        assertEquals(existing.getConfigJson(), saved.getConfigJson());
        assertEquals("new remark", saved.getRemark());
        org.mockito.Mockito.verifyNoInteractions(vpsSshCommandService);
    }

    @Test
    public void unchangedSniKeepsStatusOnlyShortcutAndLegacyNodeName() throws Exception {
        ProxyNode existing = existingSniNode();
        existing.setNodeName("legacy-custom-name");
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.update(any())).thenReturn(1);
        Map<String, Object> body = sniRequest("old.example");
        body.put("status", "1");
        AjaxResult result = controller.edit(body);
        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        assertEquals("legacy-custom-name", ((ProxyNode) result.get(AjaxResult.DATA_TAG)).getNodeName());
        verify(vpsSshCommandService).renameProxyNodeConfig(20L, "legacy-custom-name", true);
        verify(vpsSshCommandService, never()).applyRealitySniToProxyNodeConfig(any(), any());
        org.mockito.Mockito.verifyNoMoreInteractions(vpsSshCommandService);
    }

    @Test
    public void vmessEditWithoutSniPreservesConfigAndDoesNotConnectSsh() {
        ProxyNode existing = existingSniNode();
        existing.setNodeType("VMess-TCP");
        existing.setNodeName("VMess-TCP-203.0.113.8-10001-5-permanent");
        existing.setConfigJson("{\"id\":\"keep-id\",\"aid\":0,\"network\":\"tcp\"}");
        when(proxyNodeService.getById(10L)).thenReturn(existing);
        when(proxyNodeService.update(any())).thenReturn(1);
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("url", "custom vmess link");
        AjaxResult result = controller.edit(body);
        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        ProxyNode saved = (ProxyNode) result.get(AjaxResult.DATA_TAG);
        assertEquals("custom vmess link", saved.getUrl());
        assertEquals(existing.getConfigJson(), saved.getConfigJson());
        org.mockito.Mockito.verifyNoInteractions(vpsSshCommandService);
    }

    private Map<String, Object> sniRequest(String sni) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 10L);
        body.put("sni", sni);
        return body;
    }

    private ProxyNode existingSniNode() {
        ProxyNode node = existingNode();
        node.setNodeName("VLESS-REALITY-203.0.113.8-10001-5-permanent");
        node.setConfigJson("{\"id\":\"user-id\",\"sni\":\"old.example\",\"relay\":{\"type\":\"socks\",\"server\":\"old-relay.example\",\"serverPort\":1080,\"username\":\"old\",\"password\":\"old\"}}");
        node.setUrl("vless://user-id@203.0.113.8:10001?sni=old.example#node");
        return node;
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

    private ProxyNodeRateLimit activeLimit() {
        ProxyNodeRateLimit activeLimit = new ProxyNodeRateLimit();
        activeLimit.setId(30L);
        activeLimit.setInstanceId(20L);
        activeLimit.setProxyNodeId(10L);
        activeLimit.setPort(10001);
        activeLimit.setDownloadMbps(50);
        activeLimit.setUploadMbps(20);
        return activeLimit;
    }
}
