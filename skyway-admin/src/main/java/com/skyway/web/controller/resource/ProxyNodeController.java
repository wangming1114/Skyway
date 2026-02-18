package com.skyway.web.controller.resource;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skyway.common.annotation.Log;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.core.page.TableDataInfo;
import com.skyway.common.enums.BusinessType;
import com.skyway.common.utils.SecurityUtils;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeTrafficService;
import com.skyway.web.service.VpsSshCommandService;

/**
 * 代理节点
 *
 * @author ruoyi
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

    /**
     * 查询代理节点列表
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProxyNode proxyNode) {
        startPage();
        List<ProxyNode> list = proxyNodeService.selectList(proxyNode);
        return getDataTable(list);
    }

    /**
     * 根据ID获取详情
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(proxyNodeService.getById(id));
    }

    /**
     * 节点流量统计（总量 + 近期明细），用于详情等
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:query')")
    @GetMapping("/{id}/traffic")
    public AjaxResult getTraffic(@PathVariable Long id) {
        return success(proxyNodeTrafficService.getTrafficByNodeId(id));
    }

    /**
     * 新增代理节点
     */
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

    /**
     * 修改代理节点。若仅状态变更，会先在服务器上重命名配置文件（.json &harr; .json.disabled）再更新库。
     * 不使用 @Validated，因前端可能只传 id+status，在方法内用库数据补全必填项后再 update。
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "代理节点", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProxyNode row) {
        if (row.getId() == null) {
            return AjaxResult.error("参数错误：缺少节点 id");
        }
        ProxyNode existing = proxyNodeService.getById(row.getId());
        if (existing == null) {
            return AjaxResult.error("节点不存在");
        }
        // 前端可能只传 id+status，用库中数据补全必填项避免校验报错（如“端口不能为空”）
        fillRowFromExisting(row, existing);
        if (StringUtils.isNotEmpty(row.getStatus()) && !row.getStatus().equals(existing.getStatus())) {
            try {
                vpsSshCommandService.renameProxyNodeConfig(
                        existing.getInstanceId(),
                        existing.getNodeName(),
                        "1".equals(row.getStatus()));
            } catch (Exception e) {
                log.warn("服务器配置重命名失败: instanceId={}, nodeName={}", existing.getInstanceId(), existing.getNodeName(), e);
                return AjaxResult.error("服务器配置重命名失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }
        row.setUpdateBy(getUsername());
        return toAjax(proxyNodeService.update(row));
    }

    /** 将 existing 中 row 未传的字段填回 row，便于部分更新（如仅改状态）时满足必填项且不误清空其它字段 */
    private void fillRowFromExisting(ProxyNode row, ProxyNode existing) {
        // ProxyNode 必填项：@NotNull instanceId, @NotBlank nodeType, @NotBlank address, @NotNull port
        if (row.getInstanceId() == null) row.setInstanceId(existing.getInstanceId());
        if (row.getCustomerId() == null) row.setCustomerId(existing.getCustomerId());
        if (row.getNodeName() == null) row.setNodeName(existing.getNodeName());
        if (row.getNodeType() == null || row.getNodeType().isEmpty()) row.setNodeType(existing.getNodeType());
        if (row.getAddress() == null || row.getAddress().isEmpty()) row.setAddress(existing.getAddress());
        if (row.getPort() == null) row.setPort(existing.getPort());
        // 其它字段：未传则用原值，避免被置空
        if (row.getUrl() == null) row.setUrl(existing.getUrl());
        if (row.getConfigJson() == null) row.setConfigJson(existing.getConfigJson());
        if (row.getExpireTime() == null) row.setExpireTime(existing.getExpireTime());
        if (row.getCustomId() == null) row.setCustomId(existing.getCustomId());
        if (row.getStatus() == null) row.setStatus(existing.getStatus());
        if (row.getRemark() == null) row.setRemark(existing.getRemark());
    }

    /**
     * 删除代理节点（支持单条或批量，执行流程一致：先在服务器上删除配置文件，再删流量与库）
     */
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
                return AjaxResult.error("参数错误：无效的 id " + part);
            }
            ProxyNode node = proxyNodeService.getById(id);
            if (node != null) {
                try {
                    vpsSshCommandService.removeProxyNodeFromServer(node);
                } catch (Exception e) {
                    log.warn("服务器上删除节点配置失败: nodeId={}, nodeName={}", id, node.getNodeName(), e);
                    return AjaxResult.error("删除失败: " + (e.getMessage() != null ? e.getMessage() : "服务器操作异常"));
                }
                proxyNodeTrafficService.deleteByNodeId(id);
            }
            proxyNodeService.deleteById(id);
        }
        return success();
    }
}
