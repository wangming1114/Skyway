package com.skyway.resource.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 代理节点流量快照 res_proxy_node_traffic_snapshot
 *
 * @author ruoyi
 */
public class ProxyNodeTrafficSnapshot {

    private Long nodeId;
    private Long lastRx;
    private Long lastTx;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Long getLastRx() {
        return lastRx;
    }

    public void setLastRx(Long lastRx) {
        this.lastRx = lastRx;
    }

    public Long getLastTx() {
        return lastTx;
    }

    public void setLastTx(Long lastTx) {
        this.lastTx = lastTx;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
