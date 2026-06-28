package com.skyway.web.controller.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.service.IMbCustomerService;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IProxyNodeRateLimitService;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeTrafficService;
import com.skyway.resource.service.IVpsInstanceService;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {

    @Mock
    private IVpsInstanceService vpsInstanceService;

    @Mock
    private IProxyNodeService proxyNodeService;

    @Mock
    private IMbCustomerService mbCustomerService;

    @Mock
    private IProxyNodeRateLimitService proxyNodeRateLimitService;

    @Mock
    private IProxyNodeTrafficService proxyNodeTrafficService;

    @InjectMocks
    private DashboardController controller;

    @Test
    public void summaryIncludesNodeLifecycleRateLimitAndTrafficMetrics() {
        when(vpsInstanceService.count(any(VpsInstance.class))).thenReturn(8, 5, 2, 1);
        when(proxyNodeService.count(any(ProxyNode.class))).thenReturn(30, 24, 3, 6);
        when(proxyNodeService.listExpiringWithin(any(), any()))
                .thenReturn(Arrays.asList(new ProxyNode(), new ProxyNode(), new ProxyNode(), new ProxyNode()));
        when(mbCustomerService.count(any(MbCustomer.class))).thenReturn(12);
        when(proxyNodeRateLimitService.countActive()).thenReturn(7);
        when(proxyNodeTrafficService.getVpsTrafficTotal()).thenReturn(123456L);
        when(proxyNodeTrafficService.getCustomerTrafficTotal()).thenReturn(98765L);

        AjaxResult result = controller.summary();
        Map<?, ?> data = (Map<?, ?>) result.get("data");

        assertEquals(8, data.get("totalVps"));
        assertEquals(5, data.get("runningVps"));
        assertEquals(30, data.get("totalNodes"));
        assertEquals(12, data.get("totalCustomers"));
        assertEquals(4, data.get("expiringNodes"));
        assertEquals(6, data.get("expiredNodes"));
        assertEquals(7, data.get("limitedNodes"));
        assertEquals(123456L, data.get("vpsTrafficTotal"));
        assertEquals(98765L, data.get("customerTrafficTotal"));
    }

    @Test
    public void vpsTrafficTrendUsesRequestedDays() {
        Map<String, Object> row = new HashMap<>();
        row.put("instanceId", 1L);
        row.put("statDate", "2026-06-28");
        row.put("totalTraffic", 100L);
        when(proxyNodeTrafficService.getDailyTrafficByInstance(15)).thenReturn(Collections.singletonList(row));

        AjaxResult result = controller.vpsTrafficTrend(15);

        assertEquals(Collections.singletonList(row), result.get("data"));
    }

    @Test
    public void customerTrafficRankUsesRequestedDays() {
        Map<String, Object> row = new HashMap<>();
        row.put("customerId", 8L);
        row.put("username", "alice");
        row.put("totalTraffic", 200L);
        when(proxyNodeTrafficService.getCustomerTrafficRank(30)).thenReturn(Collections.singletonList(row));

        AjaxResult result = controller.customerTrafficRank(30);

        assertEquals(Collections.singletonList(row), result.get("data"));
    }
}
