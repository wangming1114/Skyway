package com.skyway.resource.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 代理节点流量明细 res_proxy_node_traffic
 *
 * @author ruoyi
 */
public class ProxyNodeTraffic {

    private Long id;
    private Long nodeId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date statTime;
    private Long rxDelta;
    private Long txDelta;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Date getStatTime() {
        return statTime;
    }

    public void setStatTime(Date statTime) {
        this.statTime = statTime;
    }

    public Long getRxDelta() {
        return rxDelta;
    }

    public void setRxDelta(Long rxDelta) {
        this.rxDelta = rxDelta;
    }

    public Long getTxDelta() {
        return txDelta;
    }

    public void setTxDelta(Long txDelta) {
        this.txDelta = txDelta;
    }
}
