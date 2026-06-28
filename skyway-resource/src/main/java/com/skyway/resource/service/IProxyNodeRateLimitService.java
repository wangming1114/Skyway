package com.skyway.resource.service;

import java.util.Date;
import java.util.List;
import com.skyway.resource.domain.ProxyNodeRateLimit;

/**
 * 代理节点端口限速 服务层
 */
public interface IProxyNodeRateLimitService {

    ProxyNodeRateLimit getById(Long id);

    ProxyNodeRateLimit getActiveByNodeId(Long proxyNodeId);

    List<ProxyNodeRateLimit> listActiveByNodeIds(List<Long> nodeIds);

    List<ProxyNodeRateLimit> listActiveByInstanceId(Long instanceId);

    List<ProxyNodeRateLimit> listExpiredActive(Date now);

    int countActive();

    int saveActive(ProxyNodeRateLimit row);

    int markRemoved(Long id, String lastApplyResult, String updateBy);

    int markExpired(Long id, String lastApplyResult, String updateBy);
}
