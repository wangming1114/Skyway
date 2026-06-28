package com.skyway.web.task;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.skyway.resource.domain.ProxyNodeRateLimit;
import com.skyway.resource.service.IProxyNodeRateLimitService;
import com.skyway.web.service.VpsSshCommandService;
import com.skyway.web.service.VpsSshCommandService.PortRateLimitRemoteResult;

/**
 * 代理节点端口限速到期清理任务。
 * 在「系统管理 -> 定时任务」中配置，invoke_target = proxyNodeRateLimitExpireTask.processExpired
 */
@Component("proxyNodeRateLimitExpireTask")
public class ProxyNodeRateLimitExpireTask {

    private static final Logger log = LoggerFactory.getLogger(ProxyNodeRateLimitExpireTask.class);
    private static final int MAX_RULES_PER_RUN = 20;

    @Autowired
    private IProxyNodeRateLimitService proxyNodeRateLimitService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    public void processExpired() {
        List<ProxyNodeRateLimit> expired = proxyNodeRateLimitService.listExpiredActive(new Date());
        if (expired == null || expired.isEmpty()) {
            return;
        }
        int processCount = Math.min(expired.size(), MAX_RULES_PER_RUN);
        log.info("proxyNodeRateLimitExpireTask: found {} expired rate limit rule(s), processing {} this run",
                expired.size(), processCount);
        for (int i = 0; i < processCount; i++) {
            ProxyNodeRateLimit rule = expired.get(i);
            if (rule.getId() == null || rule.getInstanceId() == null || rule.getPort() == null) {
                continue;
            }
            try {
                PortRateLimitRemoteResult result = vpsSshCommandService.removePortRateLimit(rule.getInstanceId(), rule.getPort());
                proxyNodeRateLimitService.markExpired(rule.getId(), result.getOutput(), "system");
            } catch (Exception e) {
                log.warn("proxyNodeRateLimitExpireTask: remove expired rule failed id={}, instanceId={}, port={}: {}",
                        rule.getId(), rule.getInstanceId(), rule.getPort(), e.getMessage());
            }
        }
    }
}
