package com.skyway.web.controller.resource;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skyway.common.annotation.Log;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.core.page.TableDataInfo;
import com.skyway.common.enums.BusinessType;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.ProxyNodeRateLimit;
import com.skyway.resource.domain.ProxyNodeDomainWhitelist;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeRateLimitService;
import com.skyway.resource.service.IProxyNodeTrafficService;
import com.skyway.web.service.VpsSshCommandService;
import com.skyway.web.service.VpsSshCommandService.PortRateLimitRemoteResult;
import com.skyway.web.service.ProxyDomainWhitelistService;
import com.skyway.web.service.VpsInstanceOperationCoordinator;

/**
 * 代理节点
 */
@RestController
@RequestMapping("/resource/vps/proxyNode")
public class ProxyNodeController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(ProxyNodeController.class);

    @Autowired
    private IProxyNodeService proxyNodeService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    @Autowired
    private IProxyNodeTrafficService proxyNodeTrafficService;

    @Autowired
    private IProxyNodeRateLimitService proxyNodeRateLimitService;

    @Autowired
    private ProxyDomainWhitelistService proxyDomainWhitelistService;

    @Autowired
    private VpsInstanceOperationCoordinator vpsInstanceOperationCoordinator;

    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProxyNode proxyNode) {
        startPage();
        List<ProxyNode> list = proxyNodeService.selectList(proxyNode);
        proxyDomainWhitelistService.hydrate(list);
        fillRateLimits(list);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        ProxyNode node = proxyNodeService.getById(id);
        if (node != null) {
            proxyDomainWhitelistService.hydrate(node);
            node.setRateLimit(proxyNodeRateLimitService.getActiveByNodeId(id));
        }
        return success(node);
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:query') or @ss.hasPermi('resource:vps:list')")
    @GetMapping("/domainWhitelist/presets")
    public AjaxResult domainWhitelistPresets() {
        return success(proxyDomainWhitelistService.listPresets());
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:query') or @ss.hasPermi('resource:vps:list')")
    @GetMapping("/domainPolicy/presets")
    public AjaxResult domainPolicyPresets() {
        return success(proxyDomainWhitelistService.listPresets());
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "代理节点域名白名单", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/domainWhitelist")
    public AjaxResult updateDomainWhitelist(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Object raw = body != null && body.containsKey("domainWhitelist") ? body.get("domainWhitelist") : body;
        return updateDomainPolicyInternal(id, raw, true);
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "代理节点域名策略", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/domainPolicy")
    public AjaxResult updateDomainPolicy(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return updateDomainPolicyInternal(id, body, false);
    }

    private AjaxResult updateDomainPolicyInternal(Long id, Object raw, boolean legacyWhitelist) {
        ProxyNode node = proxyNodeService.getById(id);
        if (node == null) return AjaxResult.error("节点不存在");
        ProxyNodeDomainWhitelist policy;
        try {
            if (!legacyWhitelist && raw instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> envelope = (Map<String, Object>) raw;
                policy = proxyDomainWhitelistService.resolveRequest(envelope);
            } else {
                policy = legacyWhitelist ? proxyDomainWhitelistService.resolveLegacyWhitelist(raw)
                        : proxyDomainWhitelistService.resolve(raw);
            }
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
        ProxyNodeDomainWhitelist previous = proxyDomainWhitelistService.parseStored(node.getDomainPolicyJson());
        String json = proxyDomainWhitelistService.serialize(policy);
        try (VpsInstanceOperationCoordinator.LockHandle ignored = vpsInstanceOperationCoordinator.lock(node.getInstanceId())) {
            vpsSshCommandService.applyDomainWhitelistToProxyNodeConfig(node, policy);
            try {
                if (proxyNodeService.updateDomainPolicy(id, json, getUsername()) <= 0) {
                    vpsSshCommandService.applyDomainWhitelistToProxyNodeConfig(node, previous);
                    return AjaxResult.error("域名策略保存失败，远端配置已恢复");
                }
            } catch (Exception dbError) {
                try { vpsSshCommandService.applyDomainWhitelistToProxyNodeConfig(node, previous); } catch (Exception ignoredRollback) {}
                throw dbError;
            }
            node.setDomainPolicyJson(json);
            node.setDomainPolicy(policy);
            node.setDomainWhitelist(proxyDomainWhitelistService.isWhitelist(policy) ? policy : null);
            return success(node);
        } catch (Exception e) {
            log.warn("apply domain policy failed: nodeId={}", id, e);
            return AjaxResult.error("域名策略应用失败: " + (e.getMessage() != null ? e.getMessage() : "服务器操作异常"));
        }
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "批量配置代理节点域名白名单", businessType = BusinessType.UPDATE)
    @PutMapping("/domainWhitelist/batch")
    public AjaxResult batchUpdateDomainWhitelist(@RequestBody Map<String, Object> body) {
        return batchUpdateDomainPolicyInternal(body, true);
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "批量配置代理节点域名策略", businessType = BusinessType.UPDATE)
    @PutMapping("/domainPolicy/batch")
    public AjaxResult batchUpdateDomainPolicy(@RequestBody Map<String, Object> body) {
        return batchUpdateDomainPolicyInternal(body, false);
    }

    private AjaxResult batchUpdateDomainPolicyInternal(Map<String, Object> body, boolean legacyWhitelist) {
        List<Long> nodeIds = parseLongList(body != null ? body.get("nodeIds") : null);
        if (nodeIds.isEmpty()) return AjaxResult.error("请选择代理节点");
        if (nodeIds.size() > 100) return AjaxResult.error("单次最多配置 100 个节点");
        ProxyNodeDomainWhitelist policy;
        try {
            if (legacyWhitelist) {
                Object policyBody = body != null && body.containsKey("domainWhitelist")
                        ? body.get("domainWhitelist") : body;
                policy = proxyDomainWhitelistService.resolveLegacyWhitelist(policyBody);
            } else {
                policy = proxyDomainWhitelistService.resolveRequest(body);
            }
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        Map<Long, List<ProxyNode>> groups = new LinkedHashMap<>();
        for (Long nodeId : nodeIds) {
            ProxyNode node = proxyNodeService.getById(nodeId);
            if (node == null) {
                results.add(domainApplyResult(nodeId, false, "节点不存在"));
            } else {
                groups.computeIfAbsent(node.getInstanceId(), ignored -> new ArrayList<>()).add(node);
            }
        }
        String targetJson = proxyDomainWhitelistService.serialize(policy);
        for (Map.Entry<Long, List<ProxyNode>> entry : groups.entrySet()) {
            List<ProxyNode> saved = new ArrayList<>();
            boolean remotelyApplied = false;
            String failure = null;
            try (VpsInstanceOperationCoordinator.LockHandle ignored = vpsInstanceOperationCoordinator.lock(entry.getKey())) {
                Map<ProxyNode, ProxyNodeDomainWhitelist> remoteUpdates = new LinkedHashMap<>();
                for (ProxyNode node : entry.getValue()) remoteUpdates.put(node, policy);
                vpsSshCommandService.applyDomainWhitelistsToProxyNodeConfigs(remoteUpdates);
                remotelyApplied = true;
                for (ProxyNode node : entry.getValue()) {
                    if (proxyNodeService.updateDomainPolicy(node.getId(), targetJson, getUsername()) <= 0) {
                        throw new IllegalStateException("节点 " + node.getId() + " 保存失败");
                    }
                    saved.add(node);
                }
            } catch (Exception e) {
                failure = e.getMessage() != null ? e.getMessage() : "服务器操作异常";
                if (remotelyApplied) {
                    try (VpsInstanceOperationCoordinator.LockHandle ignored = vpsInstanceOperationCoordinator.lock(entry.getKey())) {
                        Map<ProxyNode, ProxyNodeDomainWhitelist> rollbackUpdates = new LinkedHashMap<>();
                        for (ProxyNode node : entry.getValue()) {
                            rollbackUpdates.put(node, proxyDomainWhitelistService.parseStored(node.getDomainPolicyJson()));
                        }
                        vpsSshCommandService.applyDomainWhitelistsToProxyNodeConfigs(rollbackUpdates);
                    } catch (Exception rollbackError) {
                        log.error("rollback batch domain whitelist failed: instanceId={}", entry.getKey(), rollbackError);
                    }
                }
                for (ProxyNode node : saved) {
                    proxyNodeService.updateDomainPolicy(node.getId(), node.getDomainPolicyJson(), getUsername());
                }
            }
            for (ProxyNode node : entry.getValue()) {
                results.add(domainApplyResult(node.getId(), failure == null,
                        failure == null ? "已应用 " + (policy != null ? policy.getDomains().size() : 0) + " 个域名" : failure));
            }
        }
        int successCount = 0;
        for (Map<String, Object> result : results) if (Boolean.TRUE.equals(result.get("success"))) successCount++;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("successCount", successCount);
        summary.put("failedCount", results.size() - successCount);
        summary.put("results", results);
        return success(summary);
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:query')")
    @GetMapping("/{id}/traffic")
    public AjaxResult getTraffic(@PathVariable Long id) {
        return success(proxyNodeTrafficService.getTrafficByNodeId(id));
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/rateLimit/list")
    public AjaxResult listRateLimit(@RequestParam(required = false) Long instanceId) {
        return success(proxyNodeRateLimitService.listActiveByInstanceId(instanceId));
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:query')")
    @GetMapping("/{id}/rateLimit")
    public AjaxResult getRateLimit(@PathVariable Long id) {
        return success(proxyNodeRateLimitService.getActiveByNodeId(id));
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "代理节点端口限速", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/rateLimit")
    public AjaxResult setRateLimit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ProxyNode node = proxyNodeService.getById(id);
        if (node == null) {
            return AjaxResult.error("节点不存在");
        }
        if (node.getInstanceId() == null || node.getPort() == null || node.getPort() <= 0) {
            return AjaxResult.error("节点实例或端口信息不完整");
        }
        Integer downloadMbps = parseInteger(body != null ? body.get("downloadMbps") : null);
        Integer uploadMbps = parseInteger(body != null ? body.get("uploadMbps") : null);
        if (downloadMbps == null || downloadMbps <= 0) {
            return AjaxResult.error("请输入有效的下载限速 Mbps");
        }
        if (uploadMbps == null || uploadMbps <= 0) {
            return AjaxResult.error("请输入有效的上传限速 Mbps");
        }
        Date expireTime = parseExpireTime(body != null ? body.get("expireTime") : null);
        if (expireTime != null && !expireTime.after(new Date())) {
            return AjaxResult.error("限速到期时间必须晚于当前时间");
        }
        try {
            PortRateLimitRemoteResult remoteResult = vpsSshCommandService.setPortRateLimit(
                    node.getInstanceId(), node.getPort(), downloadMbps, uploadMbps);
            ProxyNodeRateLimit existing = proxyNodeRateLimitService.getActiveByNodeId(id);
            ProxyNodeRateLimit row = existing != null ? existing : new ProxyNodeRateLimit();
            row.setInstanceId(node.getInstanceId());
            row.setProxyNodeId(node.getId());
            row.setPort(node.getPort());
            row.setDownloadMbps(downloadMbps);
            row.setUploadMbps(uploadMbps);
            row.setExpireTime(expireTime);
            row.setLastApplyResult(remoteResult.getOutput());
            row.setUpdateBy(getUsername());
            if (row.getId() == null) {
                row.setCreateBy(getUsername());
            }
            proxyNodeRateLimitService.saveActive(row);
            return success(proxyNodeRateLimitService.getActiveByNodeId(id));
        } catch (Exception e) {
            log.warn("set rate limit failed: nodeId={}, port={}", id, node.getPort(), e);
            return AjaxResult.error("限速应用失败: " + (e.getMessage() != null ? e.getMessage() : "服务器操作异常"));
        }
    }

    private void fillRateLimits(List<ProxyNode> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> nodeIds = new ArrayList<>();
        for (ProxyNode node : list) {
            if (node != null && node.getId() != null) {
                nodeIds.add(node.getId());
            }
        }
        if (nodeIds.isEmpty()) {
            return;
        }
        List<ProxyNodeRateLimit> limits = proxyNodeRateLimitService.listActiveByNodeIds(nodeIds);
        Map<Long, ProxyNodeRateLimit> limitMap = new HashMap<>();
        for (ProxyNodeRateLimit limit : limits) {
            if (limit != null && limit.getProxyNodeId() != null && !limitMap.containsKey(limit.getProxyNodeId())) {
                limitMap.put(limit.getProxyNodeId(), limit);
            }
        }
        for (ProxyNode node : list) {
            if (node != null) {
                node.setRateLimit(limitMap.get(node.getId()));
            }
        }
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "代理节点端口限速", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}/rateLimit")
    public AjaxResult removeRateLimit(@PathVariable Long id) {
        ProxyNode node = proxyNodeService.getById(id);
        if (node == null) {
            return AjaxResult.error("节点不存在");
        }
        ProxyNodeRateLimit existing = proxyNodeRateLimitService.getActiveByNodeId(id);
        if (existing == null) {
            return success();
        }
        try {
            PortRateLimitRemoteResult remoteResult = vpsSshCommandService.removePortRateLimit(existing.getInstanceId(), existing.getPort());
            proxyNodeRateLimitService.markRemoved(existing.getId(), remoteResult.getOutput(), getUsername());
            return success();
        } catch (Exception e) {
            log.warn("remove rate limit failed: nodeId={}, port={}", id, existing.getPort(), e);
            return AjaxResult.error("限速移除失败: " + (e.getMessage() != null ? e.getMessage() : "服务器操作异常"));
        }
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:add')")
    @Log(title = "代理节点", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ProxyNode row) {
        if (row.getCustomerId() == null) {
            return AjaxResult.error("请选择归属客户");
        }
        row.setCreateBy(getUsername());
        int rows = proxyNodeService.insert(row);
        if (rows > 0 && row.getInstanceId() != null && row.getPort() != null && row.getPort() > 0) {
            try {
                vpsSshCommandService.ensureTrafficRulesForPort(row.getInstanceId(), row.getPort());
            } catch (Exception e) {
                log.warn("ensureTrafficRulesForPort instanceId={}, port={} failed: {}", row.getInstanceId(), row.getPort(), e.getMessage());
            }
        }
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "代理节点", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> body) {
        Long id = parseLong(body != null ? body.get("id") : null);
        if (id == null) {
            return AjaxResult.error("参数错误：缺少节点id");
        }
        ProxyNode existing = proxyNodeService.getById(id);
        if (existing == null) {
            return AjaxResult.error("节点不存在");
        }

        ProxyNode row = new ProxyNode();
        row.setId(id);
        fillRequiredFromExisting(row, existing);

        boolean hasExpireTime = body != null && body.containsKey("expireTime");
        boolean hasUrl = body != null && body.containsKey("url");
        boolean hasStatus = body != null && body.containsKey("status");
        boolean hasRemark = body != null && body.containsKey("remark");
        boolean hasRelayText = body != null && body.containsKey("relayText");
        boolean hasRelayEnabled = body != null && body.containsKey("relayEnabled");
        boolean hasPort = body != null && body.containsKey("port");
        boolean relayChanged = false;

        Date newExpireTime = hasExpireTime ? parseExpireTime(body.get("expireTime")) : existing.getExpireTime();
        row.setExpireTime(newExpireTime);

        Integer newPort = existing.getPort();
        if (hasPort) {
            newPort = parseInteger(body.get("port"));
            if (newPort == null || newPort < 1 || newPort > 65535) {
                return AjaxResult.error("端口范围为 1-65535");
            }
        }
        boolean portChanged = hasPort && !newPort.equals(existing.getPort());
        if (portChanged) {
            ProxyNode portOwner = proxyNodeService.getByInstanceIdAndPort(existing.getInstanceId(), newPort);
            if (portOwner != null && portOwner.getId() != null && !portOwner.getId().equals(existing.getId())) {
                return AjaxResult.error("端口 " + newPort + " 已被当前 VPS 的其他节点使用");
            }
        }
        row.setPort(newPort);

        if (hasUrl) {
            Object urlObj = body.get("url");
            row.setUrl(urlObj == null ? "" : String.valueOf(urlObj).trim());
        } else {
            row.setUrl(existing.getUrl());
        }

        if (hasStatus) {
            row.setStatus(body.get("status") == null ? null : String.valueOf(body.get("status")).trim());
        } else {
            row.setStatus(existing.getStatus());
        }

        if (hasRemark) {
            Object remarkObj = body.get("remark");
            row.setRemark(remarkObj == null ? "" : String.valueOf(remarkObj).trim());
        } else {
            row.setRemark(existing.getRemark());
        }

        if (hasRelayEnabled && !parseBoolean(body.get("relayEnabled"))) {
            if (!"VLESS-REALITY".equals(existing.getNodeType())) {
                return AjaxResult.error("当前仅 VLESS-REALITY 支持 SOCKS5 中转");
            }
            try {
                vpsSshCommandService.removeSocks5RelayFromProxyNodeConfig(existing);
                relayChanged = true;
            } catch (Exception e) {
                log.warn("remove socks5 relay failed: nodeId={}, instanceId={}", existing.getId(), existing.getInstanceId(), e);
                return AjaxResult.error("中转配置关闭失败: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
            row.setConfigJson(removeRelayConfigJson(existing.getConfigJson()));
            row.setRemark("");
        } else if (hasRelayText) {
            Object relayObj = body.get("relayText");
            String relayText = relayObj == null ? "" : String.valueOf(relayObj).trim();
            if (!"VLESS-REALITY".equals(existing.getNodeType())) {
                return AjaxResult.error("当前仅 VLESS-REALITY 支持 SOCKS5 中转");
            }
            if (StringUtils.isEmpty(relayText)) {
                return AjaxResult.error("请输入 SOCKS5 中转配置");
            }
            VpsSshCommandService.Socks5RelayConfig relay;
            try {
                relay = VpsSshCommandService.parseSocks5RelayText(relayText);
            } catch (IllegalArgumentException e) {
                return AjaxResult.error(e.getMessage());
            }
            try {
                vpsSshCommandService.applySocks5RelayToProxyNodeConfig(existing, relay);
                relayChanged = true;
            } catch (Exception e) {
                log.warn("apply socks5 relay failed: nodeId={}, instanceId={}", existing.getId(), existing.getInstanceId(), e);
                return AjaxResult.error("中转配置更新失败: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
            row.setConfigJson(updateRelayConfigJson(existing.getConfigJson(), relay));
            row.setRemark(relayText);
        }

        if (body != null && body.containsKey("customerId")) {
            Long customerId = parseLong(body.get("customerId"));
            if (customerId != null) {
                row.setCustomerId(customerId);
            }
        }

        boolean statusOnly = hasStatus
                && !hasExpireTime
                && !hasUrl
                && !hasRemark
                && !hasRelayText
                && !hasRelayEnabled
                && !hasPort
                && (body == null || !body.containsKey("customerId"));
        if (statusOnly) {
            row.setNodeName(existing.getNodeName());
            boolean statusChanged = StringUtils.isNotEmpty(row.getStatus()) && !row.getStatus().equals(existing.getStatus());
            if (statusChanged) {
                try {
                    vpsSshCommandService.renameProxyNodeConfig(
                            existing.getInstanceId(), existing.getNodeName(), "1".equals(row.getStatus()));
                } catch (Exception e) {
                    log.warn("rename proxy config failed: instanceId={}, nodeName={}",
                            existing.getInstanceId(), existing.getNodeName(), e);
                    return AjaxResult.error("服务器配置重命名失败: "
                            + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                }
            }
            row.setUpdateBy(getUsername());
            try {
                int rows = proxyNodeService.update(row);
                if (rows > 0) return success(row);
                if (statusChanged) rollbackRemoteStatus(existing);
                return toAjax(rows);
            } catch (RuntimeException e) {
                if (statusChanged) rollbackRemoteStatus(existing);
                throw e;
            }
        }

        String effectiveNodeName = existing.getNodeName();
        String oldBaseName = normalizeNodeBaseName(existing.getNodeName());
        String newBaseName = buildNodeBaseName(existing.getNodeType(), existing.getAddress(), newPort,
                row.getCustomerId(), newExpireTime);
        boolean nameChanged = !oldBaseName.equals(newBaseName);
        if (nameChanged || portChanged) {
            try {
                boolean currentlyDisabled = "1".equals(existing.getStatus());
                if (portChanged) {
                    vpsSshCommandService.updateProxyNodeConfigPortAndName(
                            existing.getInstanceId(), oldBaseName, newBaseName, currentlyDisabled,
                            existing.getPort(), newPort);
                } else {
                    vpsSshCommandService.renameProxyNodeConfig(
                            existing.getInstanceId(), oldBaseName, newBaseName, currentlyDisabled);
                }
            } catch (Exception e) {
                log.warn("update proxy config failed: instanceId={}, oldNodeName={}, newNodeName={}",
                        existing.getInstanceId(), oldBaseName, newBaseName, e);
                if (relayChanged) rollbackRemoteRelay(existing);
                return AjaxResult.error("服务器配置更新失败: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
            effectiveNodeName = nameChanged ? newBaseName : oldBaseName;
            if (portChanged) {
                // The existing share URL still contains the old port and must not be returned as valid.
                row.setUrl(null);
            }
        }
        row.setNodeName(effectiveNodeName);

        String username = getUsername();
        if (portChanged) {
            AjaxResult migrateResult = migratePortRules(existing, newPort, username);
            if (!migrateResult.isSuccess()) {
                rollbackRemoteNodeIdentity(existing, effectiveNodeName, newPort, true);
                if (relayChanged) rollbackRemoteRelay(existing);
                return migrateResult;
            }
        }

        if (StringUtils.isNotEmpty(row.getStatus()) && !row.getStatus().equals(existing.getStatus())) {
            try {
                vpsSshCommandService.renameProxyNodeConfig(
                        existing.getInstanceId(), effectiveNodeName, "1".equals(row.getStatus()));
            } catch (Exception e) {
                log.warn("rename proxy config failed: instanceId={}, nodeName={}",
                        existing.getInstanceId(), effectiveNodeName, e);
                if (portChanged) {
                    migratePortRules(row, existing.getPort(), username);
                    rollbackRemoteNodeIdentity(existing, effectiveNodeName, newPort, true);
                } else if (nameChanged) {
                    rollbackRemoteNodeIdentity(existing, effectiveNodeName, newPort, false);
                }
                if (relayChanged) rollbackRemoteRelay(existing);
                return AjaxResult.error("服务器配置重命名失败: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }

        row.setUpdateBy(username);
        try {
            int rows = proxyNodeService.update(row);
            if (rows > 0) return success(row);
            rollbackEditedNode(existing, row, effectiveNodeName, newPort, nameChanged, portChanged,
                    relayChanged, username);
            return toAjax(rows);
        } catch (RuntimeException e) {
            rollbackEditedNode(existing, row, effectiveNodeName, newPort, nameChanged, portChanged,
                    relayChanged, username);
            throw e;
        }
    }

    private void rollbackEditedNode(ProxyNode existing, ProxyNode row, String effectiveNodeName,
            Integer newPort, boolean nameChanged, boolean portChanged, boolean relayChanged, String username) {
        boolean statusChanged = StringUtils.isNotEmpty(row.getStatus()) && !row.getStatus().equals(existing.getStatus());
        if (statusChanged) {
            try {
                vpsSshCommandService.renameProxyNodeConfig(
                        existing.getInstanceId(), effectiveNodeName, "1".equals(existing.getStatus()));
            } catch (Exception rollbackError) {
                log.error("rollback proxy node status failed: nodeId={}", existing.getId(), rollbackError);
            }
        }
        if (portChanged) {
            ProxyNode changed = new ProxyNode();
            changed.setId(existing.getId());
            changed.setInstanceId(existing.getInstanceId());
            changed.setPort(newPort);
            migratePortRules(changed, existing.getPort(), username);
        }
        if (nameChanged || portChanged) rollbackRemoteNodeIdentity(existing, effectiveNodeName, newPort, portChanged);
        if (relayChanged) rollbackRemoteRelay(existing);
    }

    private void rollbackRemoteStatus(ProxyNode existing) {
        try {
            vpsSshCommandService.renameProxyNodeConfig(
                    existing.getInstanceId(), existing.getNodeName(), "1".equals(existing.getStatus()));
        } catch (Exception rollbackError) {
            log.error("rollback proxy node status failed: nodeId={}", existing.getId(), rollbackError);
        }
    }

    private void rollbackRemoteRelay(ProxyNode existing) {
        try {
            VpsSshCommandService.Socks5RelayConfig previous = parseStoredRelay(existing.getConfigJson());
            if (previous != null) {
                vpsSshCommandService.applySocks5RelayToProxyNodeConfig(existing, previous);
            } else {
                vpsSshCommandService.removeSocks5RelayFromProxyNodeConfig(existing);
            }
        } catch (Exception rollbackError) {
            log.error("rollback proxy relay failed: nodeId={}", existing.getId(), rollbackError);
        }
    }

    private void rollbackRemoteNodeIdentity(ProxyNode existing, String currentNodeName,
            Integer currentPort, boolean portChanged) {
        try {
            boolean disabled = "1".equals(existing.getStatus());
            if (portChanged) {
                vpsSshCommandService.updateProxyNodeConfigPortAndName(
                        existing.getInstanceId(), currentNodeName, existing.getNodeName(), disabled,
                        currentPort, existing.getPort());
            } else {
                vpsSshCommandService.renameProxyNodeConfig(
                        existing.getInstanceId(), currentNodeName, existing.getNodeName(), disabled);
            }
        } catch (Exception rollbackError) {
            log.error("rollback proxy node identity failed: nodeId={}, currentName={}",
                    existing.getId(), currentNodeName, rollbackError);
        }
    }

    private AjaxResult migratePortRules(ProxyNode existing, Integer newPort, String username) {
        if (existing == null || existing.getInstanceId() == null || existing.getPort() == null || newPort == null) {
            return AjaxResult.error("节点实例或端口信息不完整");
        }
        Long instanceId = existing.getInstanceId();
        Integer oldPort = existing.getPort();
        ProxyNodeRateLimit activeLimit = proxyNodeRateLimitService.getActiveByNodeId(existing.getId());
        boolean activeLimitRemoved = false;
        boolean oldTrafficRemoved = false;
        boolean newLimitApplied = false;
        try {
            if (activeLimit != null) {
                vpsSshCommandService.removePortRateLimit(instanceId, oldPort);
                activeLimitRemoved = true;
            }
            vpsSshCommandService.removeTrafficRulesForPort(instanceId, oldPort);
            oldTrafficRemoved = true;
            if (activeLimit != null) {
                PortRateLimitRemoteResult remoteResult = vpsSshCommandService.setPortRateLimit(
                        instanceId, newPort, activeLimit.getDownloadMbps(), activeLimit.getUploadMbps());
                newLimitApplied = true;
                activeLimit.setInstanceId(instanceId);
                activeLimit.setProxyNodeId(existing.getId());
                activeLimit.setPort(newPort);
                activeLimit.setLastApplyResult(remoteResult.getOutput());
                activeLimit.setUpdateBy(username);
                proxyNodeRateLimitService.saveActive(activeLimit);
            }
            vpsSshCommandService.ensureTrafficRulesForPort(instanceId, newPort);
            return success();
        } catch (Exception e) {
            log.warn("migrate proxy node port rules failed: nodeId={}, instanceId={}, oldPort={}, newPort={}",
                    existing.getId(), instanceId, oldPort, newPort, e);
            try { vpsSshCommandService.removeTrafficRulesForPort(instanceId, newPort); } catch (Exception ignored) {}
            if (activeLimit != null && newLimitApplied) {
                try { vpsSshCommandService.removePortRateLimit(instanceId, newPort); } catch (Exception ignored) {}
            }
            if (oldTrafficRemoved) {
                try { vpsSshCommandService.ensureTrafficRulesForPort(instanceId, oldPort); } catch (Exception ignored) {}
            }
            if (activeLimit != null && activeLimitRemoved) {
                try {
                    PortRateLimitRemoteResult rollback = vpsSshCommandService.setPortRateLimit(
                            instanceId, oldPort, activeLimit.getDownloadMbps(), activeLimit.getUploadMbps());
                    activeLimit.setPort(oldPort);
                    activeLimit.setLastApplyResult(rollback.getOutput());
                    activeLimit.setUpdateBy(username);
                    proxyNodeRateLimitService.saveActive(activeLimit);
                } catch (Exception rollbackError) {
                    log.error("rollback proxy node rate limit failed: nodeId={}, port={}",
                            existing.getId(), oldPort, rollbackError);
                }
            }
            return AjaxResult.error("端口规则迁移失败: "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private void fillRequiredFromExisting(ProxyNode row, ProxyNode existing) {
        row.setInstanceId(existing.getInstanceId());
        row.setCustomerId(existing.getCustomerId());
        row.setNodeType(existing.getNodeType());
        row.setAddress(existing.getAddress());
        row.setPort(existing.getPort());
        row.setConfigJson(existing.getConfigJson());
        row.setCustomId(existing.getCustomId());
    }

    private static String updateRelayConfigJson(String configJson, VpsSshCommandService.Socks5RelayConfig relay) {
        com.alibaba.fastjson2.JSONObject config;
        try {
            config = StringUtils.isNotEmpty(configJson) ? com.alibaba.fastjson2.JSON.parseObject(configJson) : new com.alibaba.fastjson2.JSONObject();
        } catch (Exception e) {
            config = new com.alibaba.fastjson2.JSONObject();
        }
        config.put("relay", relay.toConfigJson());
        return config.toJSONString();
    }

    private static String removeRelayConfigJson(String configJson) {
        if (StringUtils.isEmpty(configJson)) {
            return configJson;
        }
        try {
            com.alibaba.fastjson2.JSONObject config = com.alibaba.fastjson2.JSON.parseObject(configJson);
            config.remove("relay");
            return config.toJSONString();
        } catch (Exception e) {
            return configJson;
        }
    }

    private static VpsSshCommandService.Socks5RelayConfig parseStoredRelay(String configJson) {
        if (StringUtils.isEmpty(configJson)) return null;
        try {
            com.alibaba.fastjson2.JSONObject config = com.alibaba.fastjson2.JSON.parseObject(configJson);
            com.alibaba.fastjson2.JSONObject relay = config != null ? config.getJSONObject("relay") : null;
            if (relay == null) return null;
            String server = relay.getString("server");
            Integer port = relay.getInteger("serverPort");
            String username = relay.getString("username");
            String password = relay.getString("password");
            if (StringUtils.isEmpty(server) || port == null) return null;
            return new VpsSshCommandService.Socks5RelayConfig(server, port,
                    username != null ? username : "", password != null ? password : "");
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean parseBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value == null) return false;
        String s = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
    }

    private static Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Long> parseLongList(Object value) {
        if (!(value instanceof Iterable)) return Collections.emptyList();
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Object item : (Iterable<?>) value) {
            Long id = parseLong(item);
            if (id != null) ids.add(id);
        }
        return new ArrayList<>(ids);
    }

    private static Map<String, Object> domainApplyResult(Long nodeId, boolean success, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeId", nodeId);
        result.put("success", success);
        result.put("message", message);
        return result;
    }

    private static Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Date parseExpireTime(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception ignore) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(s);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static String normalizeNodeBaseName(String nodeName) {
        if (nodeName == null) return "";
        String name = nodeName.trim();
        if (name.endsWith(".json.disabled")) return name.substring(0, name.length() - ".json.disabled".length());
        if (name.endsWith(".json")) return name.substring(0, name.length() - ".json".length());
        if (name.endsWith(".disabled")) return name.substring(0, name.length() - ".disabled".length());
        return name;
    }

    private static String buildNodeBaseName(String nodeType, String address, Integer port, Long customerId, Date expireTime) {
        String typePart = StringUtils.isNotEmpty(nodeType) ? nodeType.trim() : "UNKNOWN";
        String addressPart = StringUtils.isNotEmpty(address) ? address.trim() : "unknown";
        String portPart = port != null ? String.valueOf(port) : "0";
        String customerPart = customerId != null ? String.valueOf(customerId) : "0";
        String expiryTag = expireTime == null ? "permanent" : new SimpleDateFormat("yyyyMMdd").format(expireTime);
        return typePart + "-" + addressPart + "-" + portPart + "-" + customerPart + "-" + expiryTag;
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:remove')")
    @Log(title = "代理节点", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String ids) {
        if (StringUtils.isEmpty(ids)) {
            return AjaxResult.error("参数错误");
        }
        String[] parts = ids.split(",");
        for (String part : parts) {
            Long id;
            try {
                id = Long.parseLong(part.trim());
            } catch (NumberFormatException e) {
                return AjaxResult.error("参数错误: 无效的 id " + part);
            }
            ProxyNode node = proxyNodeService.getById(id);
            if (node != null) {
                AjaxResult rateLimitResult = removeActiveRateLimitBeforeDelete(node, getUsername());
                if (!rateLimitResult.isSuccess()) {
                    return rateLimitResult;
                }
                try {
                    vpsSshCommandService.removeProxyNodeFromServer(node);
                } catch (Exception e) {
                    log.warn("remove node config on server failed: nodeId={}, nodeName={}", id, node.getNodeName(), e);
                    return AjaxResult.error("删除失败: " + (e.getMessage() != null ? e.getMessage() : "服务器操作异常"));
                }
                proxyNodeTrafficService.deleteByNodeId(id);
            }
            proxyNodeService.deleteById(id);
        }
        return success();
    }

    private AjaxResult removeActiveRateLimitBeforeDelete(ProxyNode node, String username) {
        if (node == null || node.getId() == null) {
            return success();
        }
        ProxyNodeRateLimit existing = proxyNodeRateLimitService.getActiveByNodeId(node.getId());
        if (existing == null) {
            return success();
        }
        Long instanceId = existing.getInstanceId() != null ? existing.getInstanceId() : node.getInstanceId();
        Integer port = existing.getPort() != null ? existing.getPort() : node.getPort();
        if (instanceId == null || port == null) {
            return AjaxResult.error("删除失败: 限速记录缺少实例或端口信息");
        }
        try {
            PortRateLimitRemoteResult remoteResult = vpsSshCommandService.removePortRateLimit(instanceId, port);
            proxyNodeRateLimitService.markRemoved(existing.getId(), remoteResult.getOutput(), username);
            return success();
        } catch (Exception e) {
            log.warn("remove active rate limit before deleting node failed: nodeId={}, instanceId={}, port={}",
                    node.getId(), instanceId, port, e);
            return AjaxResult.error("删除失败: 限速移除失败: " + (e.getMessage() != null ? e.getMessage() : "服务器操作异常"));
        }
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:remove')")
    @Log(title = "代理节点强制删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}/force")
    public AjaxResult forceRemove(@PathVariable String ids) {
        if (StringUtils.isEmpty(ids)) {
            return AjaxResult.error("参数错误");
        }
        String[] parts = ids.split(",");
        for (String part : parts) {
            Long id;
            try {
                id = Long.parseLong(part.trim());
            } catch (NumberFormatException e) {
                return AjaxResult.error("参数错误: 无效的 id " + part);
            }
            ProxyNode node = proxyNodeService.getById(id);
            if (node != null) {
                markLocalRateLimitsRemoved(id);
                proxyNodeTrafficService.deleteByNodeId(id);
                proxyNodeService.deleteById(id);
            }
        }
        return success();
    }

    private void markLocalRateLimitsRemoved(Long nodeId) {
        List<ProxyNodeRateLimit> limits = proxyNodeRateLimitService.listActiveByNodeIds(Collections.singletonList(nodeId));
        for (ProxyNodeRateLimit limit : limits) {
            if (limit != null && limit.getId() != null) {
                proxyNodeRateLimitService.markRemoved(limit.getId(), "强制删除节点，仅清理本地记录，未连接服务器", getUsername());
            }
        }
    }
}
