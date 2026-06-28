package com.skyway.web.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.skyway.resource.domain.ProxyNodeRateLimit;
import com.skyway.resource.service.IProxyNodeRateLimitService;
import com.skyway.web.service.VpsSshCommandService;
import com.skyway.web.service.VpsSshCommandService.PortRateLimitRemoteResult;

@ExtendWith(MockitoExtension.class)
public class ProxyNodeRateLimitExpireTaskTest {

    @Mock
    private IProxyNodeRateLimitService proxyNodeRateLimitService;

    @Mock
    private VpsSshCommandService vpsSshCommandService;

    @InjectMocks
    private ProxyNodeRateLimitExpireTask task;

    @Test
    public void processExpiredRemovesRemoteRuleBeforeMarkingExpired() throws Exception {
        ProxyNodeRateLimit rule = new ProxyNodeRateLimit();
        rule.setId(10L);
        rule.setInstanceId(20L);
        rule.setPort(111);

        when(proxyNodeRateLimitService.listExpiredActive(any(Date.class))).thenReturn(Collections.singletonList(rule));
        when(vpsSshCommandService.removePortRateLimit(20L, 111))
                .thenReturn(new PortRateLimitRemoteResult(111, "remote removed"));

        task.processExpired();

        verify(vpsSshCommandService).removePortRateLimit(20L, 111);
        verify(proxyNodeRateLimitService).markExpired(10L, "remote removed", "system");
    }

    @Test
    public void processExpiredKeepsRuleActiveWhenRemoteRemovalFails() throws Exception {
        ProxyNodeRateLimit rule = new ProxyNodeRateLimit();
        rule.setId(11L);
        rule.setInstanceId(21L);
        rule.setPort(222);

        when(proxyNodeRateLimitService.listExpiredActive(any(Date.class))).thenReturn(Collections.singletonList(rule));
        when(vpsSshCommandService.removePortRateLimit(21L, 222)).thenThrow(new RuntimeException("ssh failed"));

        task.processExpired();

        verify(vpsSshCommandService).removePortRateLimit(21L, 222);
        verify(proxyNodeRateLimitService, never()).markExpired(eq(11L), any(), eq("system"));
    }

    @Test
    public void processExpiredLimitsRulesPerRun() throws Exception {
        List<ProxyNodeRateLimit> rules = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            ProxyNodeRateLimit rule = new ProxyNodeRateLimit();
            rule.setId((long) i + 1);
            rule.setInstanceId(30L);
            rule.setPort(1000 + i);
            rules.add(rule);
            if (i < 20) {
                when(vpsSshCommandService.removePortRateLimit(30L, 1000 + i))
                        .thenReturn(new PortRateLimitRemoteResult(1000 + i, "removed"));
            }
        }
        when(proxyNodeRateLimitService.listExpiredActive(any(Date.class))).thenReturn(rules);

        task.processExpired();

        verify(vpsSshCommandService, times(20)).removePortRateLimit(eq(30L), any(Integer.class));
        verify(proxyNodeRateLimitService, times(20)).markExpired(any(Long.class), eq("removed"), eq("system"));
        verify(vpsSshCommandService, never()).removePortRateLimit(30L, 1024);
    }
}
