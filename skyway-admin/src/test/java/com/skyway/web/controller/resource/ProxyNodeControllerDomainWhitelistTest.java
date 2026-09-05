package com.skyway.web.controller.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.skyway.common.constant.HttpStatus;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.core.domain.entity.SysUser;
import com.skyway.common.core.domain.model.LoginUser;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.ProxyNodeDomainWhitelist;
import com.skyway.resource.service.IProxyNodeRateLimitService;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeTrafficService;
import com.skyway.web.service.ProxyDomainWhitelistService;
import com.skyway.web.service.VpsInstanceOperationCoordinator;
import com.skyway.web.service.VpsSshCommandService;

@ExtendWith(MockitoExtension.class)
public class ProxyNodeControllerDomainWhitelistTest {
    @Mock private IProxyNodeService proxyNodeService;
    @Mock private VpsSshCommandService vpsSshCommandService;
    @Mock private IProxyNodeTrafficService proxyNodeTrafficService;
    @Mock private IProxyNodeRateLimitService proxyNodeRateLimitService;
    @Spy private ProxyDomainWhitelistService proxyDomainWhitelistService = new ProxyDomainWhitelistService();
    @InjectMocks private ProxyNodeController controller;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(controller, "vpsInstanceOperationCoordinator", new VpsInstanceOperationCoordinator());
        SysUser user = new SysUser();
        user.setUserName("tester");
        LoginUser loginUser = new LoginUser();
        ReflectionTestUtils.setField(loginUser, "user", user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void singleUpdateWritesDatabaseOnlyAfterRemoteApply() throws Exception {
        ProxyNode node = node(1L, 10L);
        when(proxyNodeService.getById(1L)).thenReturn(node);
        when(proxyNodeService.updateDomainPolicy(eq(1L), any(), eq("tester"))).thenReturn(1);

        AjaxResult result = controller.updateDomainWhitelist(1L, policyBody("example.com"));

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        InOrder order = inOrder(vpsSshCommandService, proxyNodeService);
        order.verify(vpsSshCommandService).applyDomainWhitelistToProxyNodeConfig(eq(node), any(ProxyNodeDomainWhitelist.class));
        order.verify(proxyNodeService).updateDomainPolicy(eq(1L), any(), eq("tester"));
    }

    @Test
    public void singleUpdateDoesNotWriteDatabaseWhenRemoteValidationFails() throws Exception {
        ProxyNode node = node(1L, 10L);
        when(proxyNodeService.getById(1L)).thenReturn(node);
        doThrow(new IOException("check failed")).when(vpsSshCommandService)
                .applyDomainWhitelistToProxyNodeConfig(eq(node), any(ProxyNodeDomainWhitelist.class));

        AjaxResult result = controller.updateDomainWhitelist(1L, policyBody("example.com"));

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        verify(proxyNodeService, never()).updateDomainPolicy(any(), any(), any());
    }

    @Test
    public void singleUpdateRestoresPreviousRemotePolicyWhenDatabaseSaveFails() throws Exception {
        ProxyNode node = node(1L, 10L);
        node.setDomainPolicyJson(null);
        when(proxyNodeService.getById(1L)).thenReturn(node);
        when(proxyNodeService.updateDomainPolicy(eq(1L), any(), eq("tester"))).thenReturn(0);

        AjaxResult result = controller.updateDomainWhitelist(1L, policyBody("example.com"));

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<ProxyNodeDomainWhitelist> policies = ArgumentCaptor.forClass(ProxyNodeDomainWhitelist.class);
        verify(vpsSshCommandService, times(2)).applyDomainWhitelistToProxyNodeConfig(eq(node), policies.capture());
        assertEquals("example.com", policies.getAllValues().get(0).getDomains().get(0));
        assertEquals(null, policies.getAllValues().get(1));
    }

    @Test
    public void unifiedEndpointStoresBlacklistAndHidesLegacyWhitelistField() throws Exception {
        ProxyNode node = node(1L, 10L);
        when(proxyNodeService.getById(1L)).thenReturn(node);
        when(proxyNodeService.updateDomainPolicy(eq(1L), any(), eq("tester"))).thenReturn(1);
        Map<String, Object> policy = policyBody("example.com");
        policy.put("mode", "blacklist");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("domainPolicy", policy);

        AjaxResult result = controller.updateDomainPolicy(1L, body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        ProxyNode returned = (ProxyNode) result.get(AjaxResult.DATA_TAG);
        assertEquals("blacklist", returned.getDomainPolicy().getMode());
        assertEquals(null, returned.getDomainWhitelist());
        verify(vpsSshCommandService).applyDomainWhitelistToProxyNodeConfig(eq(node),
                org.mockito.ArgumentMatchers.argThat(value -> "blacklist".equals(value.getMode())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void batchAllowsPartialSuccessAcrossVpsButKeepsEachVpsAtomic() throws Exception {
        ProxyNode first = node(1L, 10L);
        ProxyNode second = node(2L, 10L);
        ProxyNode third = node(3L, 20L);
        when(proxyNodeService.getById(1L)).thenReturn(first);
        when(proxyNodeService.getById(2L)).thenReturn(second);
        when(proxyNodeService.getById(3L)).thenReturn(third);
        when(proxyNodeService.updateDomainPolicy(any(), any(), eq("tester"))).thenReturn(1);
        doThrow(new IOException("vps unavailable")).when(vpsSshCommandService)
                .applyDomainWhitelistsToProxyNodeConfigs(anyMap());
        org.mockito.Mockito.doNothing().when(vpsSshCommandService)
                .applyDomainWhitelistsToProxyNodeConfigs(org.mockito.ArgumentMatchers.argThat(map -> map.containsKey(first)));

        Map<String, Object> body = policyBody("example.com");
        body.put("nodeIds", Arrays.asList(1L, 2L, 3L));
        AjaxResult result = controller.batchUpdateDomainWhitelist(body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        Map<String, Object> data = (Map<String, Object>) result.get(AjaxResult.DATA_TAG);
        assertEquals(2, data.get("successCount"));
        assertEquals(1, data.get("failedCount"));
        verify(proxyNodeService).updateDomainPolicy(eq(1L), any(), eq("tester"));
        verify(proxyNodeService).updateDomainPolicy(eq(2L), any(), eq("tester"));
        verify(proxyNodeService, never()).updateDomainPolicy(eq(3L), any(), eq("tester"));
    }

    private ProxyNode node(Long id, Long instanceId) {
        ProxyNode node = new ProxyNode();
        node.setId(id);
        node.setInstanceId(instanceId);
        node.setNodeName("node-" + id);
        node.setPort(10000 + id.intValue());
        node.setStatus("0");
        return node;
    }

    private Map<String, Object> policyBody(String domain) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("presetKeys", Collections.emptyList());
        body.put("customDomains", Collections.singletonList(domain));
        return body;
    }
}
