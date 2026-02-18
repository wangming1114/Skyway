package com.skyway.web.controller.resource;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
import com.skyway.common.utils.poi.ExcelUtil;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IVpsInstanceService;
import com.skyway.web.service.VpsSshCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VPS 实例
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/resource/vps/instance")
public class VpsInstanceController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(VpsInstanceController.class);

    @Autowired
    private IVpsInstanceService vpsInstanceService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    @Autowired
    private IProxyNodeService proxyNodeService;

    /**
     * 分页查询VPS实例列表
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/list")
    public TableDataInfo list(VpsInstance instance) {
        startPage();
        List<VpsInstance> list = vpsInstanceService.selectList(instance);
        return getDataTable(list);
    }

    /**
     * 导出VPS实例列表
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:export')")
    @Log(title = "VPS实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VpsInstance instance) {
        List<VpsInstance> list = vpsInstanceService.selectList(instance);
        ExcelUtil<VpsInstance> util = new ExcelUtil<>(VpsInstance.class);
        util.exportExcel(response, list, "VPS实例数据");
    }

    /**
     * 根据ID获取详情（含分类名、节点名）
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(vpsInstanceService.getById(id));
    }

    /**
     * 连接测试并拉取 CPU/内存/磁盘规格（不写库，用于新增/编辑时的「连接测试」按钮）
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:add')")
    @PostMapping("/testConnection")
    public AjaxResult testConnection(@RequestBody java.util.Map<String, Object> body) {
        String ip = body != null && body.get("ip") != null ? body.get("ip").toString().trim() : null;
        Integer sshPort = null;
        if (body != null && body.get("sshPort") != null) {
            Object p = body.get("sshPort");
            if (p instanceof Number) sshPort = ((Number) p).intValue();
            else try { sshPort = Integer.parseInt(p.toString()); } catch (NumberFormatException ignored) {}
        }
        if (sshPort == null) sshPort = 22;
        String sshUsername = body != null && body.get("sshUsername") != null ? body.get("sshUsername").toString().trim() : null;
        String sshPassword = body != null && body.get("sshPassword") != null ? body.get("sshPassword").toString() : "";
        java.util.Map<String, Object> result = vpsSshCommandService.testConnectionAndFetchSpec(ip, sshPort, sshUsername, sshPassword);
        return success(result);
    }

    /**
     * 新增VPS实例
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:add')")
    @Log(title = "VPS实例", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpsInstance row) {
        return toAjax(vpsInstanceService.insert(row));
    }

    /**
     * 修改VPS实例（状态由定时任务同步，不随编辑更新）
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "VPS实例", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpsInstance row) {
        if (row.getId() != null) {
            VpsInstance existing = vpsInstanceService.getById(row.getId());
            if (existing != null) {
                row.setStatus(existing.getStatus());
            }
        }
        return toAjax(vpsInstanceService.update(row));
    }

    /**
     * 删除VPS实例
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:remove')")
    @Log(title = "VPS实例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(vpsInstanceService.deleteById(id));
    }

    /**
     * 在指定实例上添加代理节点（HTTP 同步执行，用于客户详情等无 WebSocket 场景）
     * 请求体：customerId, port, expireTime(可选，yyyy-MM-dd HH:mm:ss)
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:add')")
    @Log(title = "代理节点", businessType = BusinessType.INSERT)
    @PostMapping("/{instanceId}/proxyNode")
    public AjaxResult addProxyNode(@PathVariable Long instanceId, @RequestBody java.util.Map<String, Object> body) {
        Object customerIdObj = body.get("customerId");
        Object portObj = body.get("port");
        String expireTimeStr = body != null && body.get("expireTime") != null ? body.get("expireTime").toString() : null;
        String remark = body != null && body.get("remark") != null ? body.get("remark").toString().trim() : null;
        if (remark != null && remark.isEmpty()) remark = null;
        if (customerIdObj == null) {
            return AjaxResult.error("请选择归属客户");
        }
        Long customerId = null;
        if (customerIdObj instanceof Number) {
            customerId = ((Number) customerIdObj).longValue();
        } else if (customerIdObj instanceof String) {
            try {
                customerId = Long.parseLong((String) customerIdObj);
            } catch (NumberFormatException e) {
                return AjaxResult.error("归属客户无效");
            }
        }
        if (portObj == null) {
            return AjaxResult.error("请输入端口");
        }
        int port = ((Number) portObj).intValue();
        if (port < 1 || port > 65535) {
            return AjaxResult.error("端口范围为 1-65535");
        }
        try {
            ProxyNode node = vpsSshCommandService.addProxyNodeOnInstance(instanceId, customerId, port, expireTimeStr);
            node.setCreateBy(getUsername());
            if (remark != null) node.setRemark(remark);
            proxyNodeService.insert(node);
            try {
                vpsSshCommandService.ensureTrafficRulesForPort(instanceId, node.getPort() != null ? node.getPort() : port);
            } catch (Exception ex) {
                log.warn("ensureTrafficRulesForPort instanceId={}, port={} failed: {}", instanceId, port, ex.getMessage());
            }
            return success(node);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "添加节点失败");
        }
    }
}
