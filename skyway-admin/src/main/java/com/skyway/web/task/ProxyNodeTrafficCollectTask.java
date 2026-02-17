package com.skyway.web.task;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeTrafficService;
import com.skyway.web.service.VpsSshCommandService;
import com.skyway.web.service.VpsSshCommandService.TrafficPair;

/**
 * 代理节点流量采集，由「系统管理 -> 定时任务」通过 invoke_target=proxyNodeTrafficTask.collect 调用。
 * SSH 读各实例 iptables/nft 计数器，按 node_id 写增量并更新快照。
 */
@Component("proxyNodeTrafficTask")
public class ProxyNodeTrafficCollectTask {

    private static final Logger log = LoggerFactory.getLogger(ProxyNodeTrafficCollectTask.class);

    @Autowired
    private IProxyNodeService proxyNodeService;

    @Autowired
    private IProxyNodeTrafficService proxyNodeTrafficService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    public void collect() {
        List<ProxyNode> all = proxyNodeService.selectList(new ProxyNode());
        if (all == null || all.isEmpty()) {
            return;
        }
        Map<Long, List<ProxyNode>> byInstance = all.stream().filter(n -> n.getInstanceId() != null)
            .collect(Collectors.groupingBy(ProxyNode::getInstanceId));
        Date now = new Date();
        for (Map.Entry<Long, List<ProxyNode>> e : byInstance.entrySet()) {
            Long instanceId = e.getKey();
            List<ProxyNode> nodesOnInstance = e.getValue();
            // 先为本实例所有节点确保流量规则存在（CentOS 等首次无链时先建链再读，避免一直读到空）
            for (ProxyNode node : nodesOnInstance) {
                if (node.getPort() == null || node.getPort() <= 0) continue;
                try {
                    vpsSshCommandService.ensureTrafficRulesForPort(instanceId, node.getPort());
                } catch (Exception ex) {
                    log.warn("ensureTrafficRulesForPort instanceId={} port={} failed: {}", instanceId, node.getPort(), ex.getMessage());
                }
            }
            Map<Integer, TrafficPair> counters = null;
            try {
                counters = vpsSshCommandService.readTrafficCounters(instanceId);
            } catch (Exception ex) {
                log.warn("collect traffic instanceId={} failed: {}", instanceId, ex.getMessage());
            }
            if (counters != null && !counters.isEmpty()) {
                Set<Long> recorded = new HashSet<>();
                for (Map.Entry<Integer, TrafficPair> ce : counters.entrySet()) {
                    Integer port = ce.getKey();
                    TrafficPair pair = ce.getValue();
                    ProxyNode node = proxyNodeService.getByInstanceIdAndPort(instanceId, port);
                    if (node == null) continue;
                    try {
                        proxyNodeTrafficService.recordTrafficSnapshot(node.getId(), now, pair.rx, pair.tx);
                        recorded.add(node.getId());
                    } catch (Exception ex) {
                        log.warn("recordTrafficSnapshot nodeId={} failed: {}", node.getId(), ex.getMessage());
                    }
                }
                for (ProxyNode node : nodesOnInstance) {
                    if (node.getId() == null || recorded.contains(node.getId())) continue;
                    try {
                        proxyNodeTrafficService.recordTrafficSnapshot(node.getId(), now, 0L, 0L);
                    } catch (Exception ex) {
                        log.warn("recordTrafficSnapshot nodeId={} (no counter) failed: {}", node.getId(), ex.getMessage());
                    }
                }
            } else {
                for (ProxyNode node : nodesOnInstance) {
                    if (node.getId() == null) continue;
                    try {
                        proxyNodeTrafficService.recordTrafficSnapshot(node.getId(), now, 0L, 0L);
                    } catch (Exception ex) {
                        log.warn("recordTrafficSnapshot nodeId={} (no counters) failed: {}", node.getId(), ex.getMessage());
                    }
                }
            }
        }
    }
}
