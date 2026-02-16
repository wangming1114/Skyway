package com.skyway.resource.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.skyway.resource.domain.ProxyNodeTraffic;
import com.skyway.resource.domain.ProxyNodeTrafficSnapshot;
import com.skyway.resource.mapper.ProxyNodeTrafficMapper;
import com.skyway.resource.mapper.ProxyNodeTrafficSnapshotMapper;
import com.skyway.resource.service.IProxyNodeTrafficService;

/**
 * 代理节点流量 服务实现
 *
 * @author ruoyi
 */
@Service
public class ProxyNodeTrafficServiceImpl implements IProxyNodeTrafficService {

    private static final int RECENT_LIMIT = 48;

    @Autowired
    private ProxyNodeTrafficMapper proxyNodeTrafficMapper;

    @Autowired
    private ProxyNodeTrafficSnapshotMapper proxyNodeTrafficSnapshotMapper;

    @Override
    public Map<String, Object> getTrafficByNodeId(Long nodeId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> sum = proxyNodeTrafficMapper.selectSumByNodeId(nodeId);
        long totalRx = 0L;
        long totalTx = 0L;
        if (sum != null && !sum.isEmpty()) {
            totalRx = toLong(sum.get("totalRx"), sum.get("totalrx"));
            totalTx = toLong(sum.get("totalTx"), sum.get("totaltx"));
        }
        result.put("totalRx", totalRx);
        result.put("totalTx", totalTx);
        List<ProxyNodeTraffic> recent = proxyNodeTrafficMapper.selectRecentByNodeId(nodeId, RECENT_LIMIT);
        result.put("recentList", recent != null ? recent : java.util.Collections.emptyList());
        return result;
    }

    /** 兼容 MyBatis SUM 返回 BigDecimal / Long 等 */
    private static long toLong(Object a, Object b) {
        Number n = a instanceof Number ? (Number) a : (b instanceof Number ? (Number) b : null);
        return n != null ? n.longValue() : 0L;
    }

    @Override
    public void recordTrafficSnapshot(Long nodeId, Date statTime, long currentRx, long currentTx) {
        ProxyNodeTrafficSnapshot snap = proxyNodeTrafficSnapshotMapper.selectByNodeId(nodeId);
        long rxDelta;
        long txDelta;
        if (snap == null) {
            // 新节点首次采集仅建立基线，不累计历史值，保证“删除后再添加”从 0 开始
            rxDelta = 0L;
            txDelta = 0L;
        } else {
            long lastRx = snap.getLastRx() != null ? snap.getLastRx() : 0L;
            long lastTx = snap.getLastTx() != null ? snap.getLastTx() : 0L;
            rxDelta = Math.max(0, currentRx - lastRx);
            txDelta = Math.max(0, currentTx - lastTx);
        }
        ProxyNodeTraffic row = new ProxyNodeTraffic();
        row.setNodeId(nodeId);
        row.setStatTime(statTime);
        row.setRxDelta(rxDelta);
        row.setTxDelta(txDelta);
        proxyNodeTrafficMapper.insert(row);
        if (snap != null) {
            snap.setLastRx(currentRx);
            snap.setLastTx(currentTx);
            snap.setUpdatedAt(statTime);
            proxyNodeTrafficSnapshotMapper.update(snap);
        } else {
            ProxyNodeTrafficSnapshot newSnap = new ProxyNodeTrafficSnapshot();
            newSnap.setNodeId(nodeId);
            newSnap.setLastRx(currentRx);
            newSnap.setLastTx(currentTx);
            newSnap.setUpdatedAt(statTime);
            proxyNodeTrafficSnapshotMapper.insert(newSnap);
        }
    }

    @Override
    public long[] getLastSnapshot(Long nodeId) {
        ProxyNodeTrafficSnapshot snap = proxyNodeTrafficSnapshotMapper.selectByNodeId(nodeId);
        if (snap == null) return new long[] { 0L, 0L };
        long rx = snap.getLastRx() != null ? snap.getLastRx() : 0L;
        long tx = snap.getLastTx() != null ? snap.getLastTx() : 0L;
        return new long[] { rx, tx };
    }

    @Override
    public long[] getTotalByNodeId(Long nodeId) {
        Map<String, Object> sum = proxyNodeTrafficMapper.selectSumByNodeId(nodeId);
        long totalRx = 0L;
        long totalTx = 0L;
        if (sum != null && !sum.isEmpty()) {
            totalRx = toLong(sum.get("totalRx"), sum.get("totalrx"));
            totalTx = toLong(sum.get("totalTx"), sum.get("totaltx"));
        }
        return new long[] { totalRx, totalTx };
    }

    @Override
    public void deleteByNodeId(Long nodeId) {
        if (nodeId == null) return;
        proxyNodeTrafficSnapshotMapper.deleteByNodeId(nodeId);
        proxyNodeTrafficMapper.deleteByNodeId(nodeId);
    }

}
