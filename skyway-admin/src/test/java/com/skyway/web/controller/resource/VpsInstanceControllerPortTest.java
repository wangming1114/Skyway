package com.skyway.web.controller.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.web.service.VpsInstanceOperationCoordinator;
import com.skyway.web.service.VpsPortAvailabilityService;
import com.skyway.web.service.VpsPortAvailabilityService.PortRecommendation;
import com.skyway.web.service.VpsSshCommandService;

@ExtendWith(MockitoExtension.class)
public class VpsInstanceControllerPortTest {

    @Mock
    private IProxyNodeService proxyNodeService;

    @Mock
    private VpsSshCommandService vpsSshCommandService;

    @Mock
    private VpsPortAvailabilityService vpsPortAvailabilityService;

    @InjectMocks
    private VpsInstanceController controller;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(controller, "vpsInstanceOperationCoordinator",
                new VpsInstanceOperationCoordinator());
        SysUser user = new SysUser();
        user.setUserName("tester");
        LoginUser loginUser = new LoginUser();
        ReflectionTestUtils.setField(loginUser, "user", user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void recommendationKeepsNumericDataAndAddsVerificationMetadata() {
        Set<Integer> excluded = Collections.singleton(10000);
        when(vpsPortAvailabilityService.parseExcludedPorts("10000")).thenReturn(excluded);
        when(vpsPortAvailabilityService.recommend(7L, excluded)).thenReturn(
                new PortRecommendation(10001, false, "服务器未验证"));

        AjaxResult result = controller.recommendPort(7L, "10000");

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        assertEquals(10001, result.get(AjaxResult.DATA_TAG));
        assertEquals(false, result.get("verified"));
        assertEquals("服务器未验证", result.get("warning"));
    }

    @Test
    public void automaticHttpCreationUsesActualRecheckedPort() throws Exception {
        when(vpsPortAvailabilityService.resolveAutoPortForCreate(7L, 10000)).thenReturn(10005);
        ProxyNode created = new ProxyNode();
        created.setPort(10005);
        when(vpsSshCommandService.addProxyNodeOnInstance(7L, 9L, 10005, null, "VLESS-REALITY"))
                .thenReturn(created);
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", 9L);
        body.put("port", 10000);
        body.put("autoPort", true);

        AjaxResult result = controller.addProxyNode(7L, body);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        assertEquals(created, result.get(AjaxResult.DATA_TAG));
        assertEquals(Integer.valueOf(10005), created.getPort());
        assertEquals("tester", created.getCreateBy());
        verify(vpsPortAvailabilityService, never()).assertAvailableForCreate(7L, 10000);
        verify(proxyNodeService).insert(created);
        verify(vpsSshCommandService).ensureTrafficRulesForPort(7L, 10005);
    }
}
