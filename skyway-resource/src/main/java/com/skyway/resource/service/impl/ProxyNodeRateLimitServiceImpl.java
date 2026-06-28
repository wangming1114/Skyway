package com.skyway.resource.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.skyway.resource.domain.ProxyNodeRateLimit;
import com.skyway.resource.mapper.ProxyNodeRateLimitMapper;
import com.skyway.resource.service.IProxyNodeRateLimitService;

/**
 * 代理节点端口限速 服务实现
 */
@Service
public class ProxyNodeRateLimitServiceImpl implements IProxyNodeRateLimitService {

    @Autowired
    private ProxyNodeRateLimitMapper proxyNodeRateLimitMapper;

    @Override
    public ProxyNodeRateLimit getById(Long id) {
        return proxyNodeRateLimitMapper.selectById(id);
    }

    @Override
    public ProxyNodeRateLimit getActiveByNodeId(Long proxyNodeId) {
        return proxyNodeRateLimitMapper.selectActiveByNodeId(proxyNodeId);
    }

    @Override
    public List<ProxyNodeRateLimit> listActiveByNodeIds(List<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return proxyNodeRateLimitMapper.selectActiveByNodeIds(nodeIds);
    }

    @Override
    public List<ProxyNodeRateLimit> listActiveByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return Collections.emptyList();
        }
        return proxyNodeRateLimitMapper.selectActiveByInstanceId(instanceId);
    }

    @Override
    public List<ProxyNodeRateLimit> listExpiredActive(Date now) {
        return proxyNodeRateLimitMapper.selectExpiredActive(now != null ? now : new Date());
    }

    @Override
    public int saveActive(ProxyNodeRateLimit row) {
        row.setStatus(ProxyNodeRateLimit.STATUS_ACTIVE);
        if (row.getId() == null) {
            return proxyNodeRateLimitMapper.insert(row);
        }
        return proxyNodeRateLimitMapper.update(row);
    }

    @Override
    public int markRemoved(Long id, String lastApplyResult, String updateBy) {
        return proxyNodeRateLimitMapper.markStatus(id, ProxyNodeRateLimit.STATUS_REMOVED, lastApplyResult, updateBy);
    }

    @Override
    public int markExpired(Long id, String lastApplyResult, String updateBy) {
        return proxyNodeRateLimitMapper.markStatus(id, ProxyNodeRateLimit.STATUS_EXPIRED, lastApplyResult, updateBy);
    }
}
