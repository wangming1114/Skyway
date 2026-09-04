package com.skyway.web.controller.resource;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeRateLimitService;
import com.skyway.resource.service.IProxyNodeTrafficService;
import com.skyway.web.service.VpsSshCommandService;
import com.skyway.web.service.VpsSshCommandService.PortRateLimitRemoteResult;

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

    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProxyNode proxyNode) {
        startPage();
        List<ProxyNode> list = proxyNodeService.selectList(proxyNode);
        fillRateLimits(list);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        ProxyNode node = proxyNodeService.getById(id);
        if (node != null) {
            node.setRateLimit(proxyNodeRateLimitService.getActiveByNodeId(id));
        }
        return success(node);
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
            if (StringUtils.isNotEmpty(row.getStatus()) && !row.getStatus().equals(existing.getStatus())) {
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
            int rows = proxyNodeService.update(row);
            return rows > 0 ? success(row) : toAjax(rows);
        }

        String newBaseName = buildNodeBaseName(existing.getNodeType(), existing.getAddress(), newPort, row.getCustomerId(), newExpireTime);
        row.setNodeName(newBaseName);

        String username = getUsername();
        row.setUpdateBy(username);
        int rows = proxyNodeService.update(row);
        return rows > 0 ? success(row) : toAjax(rows);
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
