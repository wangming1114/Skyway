package com.skyway.web.controller.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    public void customerTrafficRankUsesShortcutRangeBounds() {
        Map<String, Object> row = new HashMap<>();
        row.put("customerId", 8L);
        row.put("username", "alice");
        row.put("nodeId", 18L);
        row.put("nodeName", "alice-node-1");
        row.put("totalTraffic", 200L);
        when(proxyNodeTrafficService.getCustomerTrafficRank(any(Date.class), any(Date.class))).thenReturn(Collections.singletonList(row));

        AjaxResult result = controller.customerTrafficRank("week", null, null);

        assertEquals(Collections.singletonList(row), result.get("data"));
        ArgumentCaptor<Date> fromCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> toCaptor = ArgumentCaptor.forClass(Date.class);
        verify(proxyNodeTrafficService).getCustomerTrafficRank(fromCaptor.capture(), toCaptor.capture());
        assertEquals(6L, (toCaptor.getValue().getTime() - fromCaptor.getValue().getTime()) / (24L * 60L * 60L * 1000L));
    }

    @Test
    public void vpsTrafficRankUsesShortcutRangeBounds() {
        Map<String, Object> row = new HashMap<>();
        row.put("instanceId", 3L);
        row.put("instanceName", "vps-3");
        row.put("totalTraffic", 300L);
        when(proxyNodeTrafficService.getVpsTrafficRank(any(Date.class), any(Date.class))).thenReturn(Collections.singletonList(row));

        AjaxResult result = controller.vpsTrafficRank("week", null, null);

        assertEquals(Collections.singletonList(row), result.get("data"));
        ArgumentCaptor<Date> fromCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> toCaptor = ArgumentCaptor.forClass(Date.class);
        verify(proxyNodeTrafficService).getVpsTrafficRank(fromCaptor.capture(), toCaptor.capture());
        assertEquals(6L, (toCaptor.getValue().getTime() - fromCaptor.getValue().getTime()) / (24L * 60L * 60L * 1000L));
    }

    @Test
    public void vpsTrafficRankUsesCustomDateRangeBounds() {
        when(proxyNodeTrafficService.getVpsTrafficRank(any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());

        controller.vpsTrafficRank("custom", "2026-06-01", "2026-06-30");

        ArgumentCaptor<Date> fromCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> toCaptor = ArgumentCaptor.forClass(Date.class);
        verify(proxyNodeTrafficService).getVpsTrafficRank(fromCaptor.capture(), toCaptor.capture());
        Calendar from = Calendar.getInstance();
        from.setTime(fromCaptor.getValue());
        Calendar to = Calendar.getInstance();
        to.setTime(toCaptor.getValue());
        assertEquals(0, from.get(Calendar.HOUR_OF_DAY));
        assertEquals(23, to.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void customerTrafficRankDefaultsToDayRange() {
        when(proxyNodeTrafficService.getCustomerTrafficRank(any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());

        controller.customerTrafficRank(null, null, null);

        ArgumentCaptor<Date> fromCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> toCaptor = ArgumentCaptor.forClass(Date.class);
        verify(proxyNodeTrafficService).getCustomerTrafficRank(fromCaptor.capture(), toCaptor.capture());
        assertEquals(0L, (toCaptor.getValue().getTime() - fromCaptor.getValue().getTime()) / (24L * 60L * 60L * 1000L));
    }

    @Test
    public void customerTrafficRankUsesCustomDateRangeBounds() {
        when(proxyNodeTrafficService.getCustomerTrafficRank(any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());

        controller.customerTrafficRank("custom", "2026-06-01", "2026-06-30");

        ArgumentCaptor<Date> fromCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> toCaptor = ArgumentCaptor.forClass(Date.class);
        verify(proxyNodeTrafficService).getCustomerTrafficRank(fromCaptor.capture(), toCaptor.capture());
        Calendar from = Calendar.getInstance();
        from.setTime(fromCaptor.getValue());
        Calendar to = Calendar.getInstance();
        to.setTime(toCaptor.getValue());
        assertEquals(0, from.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, from.get(Calendar.MINUTE));
        assertEquals(0, from.get(Calendar.SECOND));
        assertEquals(23, to.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, to.get(Calendar.MINUTE));
        assertEquals(59, to.get(Calendar.SECOND));
    }

    @Test
    public void customerTrafficRankNormalizesReversedCustomDateRange() {
        when(proxyNodeTrafficService.getCustomerTrafficRank(any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());

        controller.customerTrafficRank("custom", "2026-06-30", "2026-06-01");

        ArgumentCaptor<Date> fromCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> toCaptor = ArgumentCaptor.forClass(Date.class);
        verify(proxyNodeTrafficService).getCustomerTrafficRank(fromCaptor.capture(), toCaptor.capture());
        Calendar from = Calendar.getInstance();
        from.setTime(fromCaptor.getValue());
        Calendar to = Calendar.getInstance();
        to.setTime(toCaptor.getValue());
        assertEquals(Calendar.JUNE, from.get(Calendar.MONTH));
        assertEquals(1, from.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, from.get(Calendar.HOUR_OF_DAY));
        assertEquals(Calendar.JUNE, to.get(Calendar.MONTH));
        assertEquals(30, to.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, to.get(Calendar.HOUR_OF_DAY));
    }
}
