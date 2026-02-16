package com.skyway.resource.service;

import java.util.List;
import java.util.Map;
import com.skyway.resource.domain.ProxyNodeTraffic;

/**
 * 代理节点流量 服务层
 *
 * @author ruoyi
 */
public interface IProxyNodeTrafficService {

    /**
     * 查询节点流量汇总与近期明细
     *
     * @param nodeId 节点ID
     * @return totalRx, totalTx, recentList
     */
    Map<String, Object> getTrafficByNodeId(Long nodeId);

    /**
     * 根据当前服务器计数器记录本周期增量并更新快照（delta = current - last）
     */
    void recordTrafficSnapshot(Long nodeId, java.util.Date statTime, long currentRx, long currentTx);

    /**
     * 获取快照 lastRx, lastTx（无则 0,0）
     */
    long[] getLastSnapshot(Long nodeId);

    /**
     * 获取节点累计流量（SUM rx_delta, tx_delta）
     */
    long[] getTotalByNodeId(Long nodeId);

    /**
     * 删除该节点所有流量数据与快照（节点删除时调用，便于同端口重新开通时从 0 开始计）
     */
    void deleteByNodeId(Long nodeId);
}
