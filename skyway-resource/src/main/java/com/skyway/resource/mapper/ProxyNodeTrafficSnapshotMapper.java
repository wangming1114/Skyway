package com.skyway.resource.mapper;

import com.skyway.resource.domain.ProxyNodeTrafficSnapshot;

/**
 * 代理节点流量快照 数据层
 *
 * @author ruoyi
 */
public interface ProxyNodeTrafficSnapshotMapper {

    ProxyNodeTrafficSnapshot selectByNodeId(Long nodeId);

    int insert(ProxyNodeTrafficSnapshot row);

    int update(ProxyNodeTrafficSnapshot row);

    /**
     * 删除该节点快照（节点删除时重置计数用）
     */
    int deleteByNodeId(Long nodeId);
}
