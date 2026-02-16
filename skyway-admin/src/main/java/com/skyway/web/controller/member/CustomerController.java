package com.skyway.web.controller.member;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.skyway.common.utils.poi.ExcelUtil;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.service.IMbCustomerService;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;

/**
 * 会员中心 - 客户管理（mb_customer）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/member/customer")
public class CustomerController extends BaseController {

    @Autowired
    private IMbCustomerService mbCustomerService;

    @Autowired
    private IProxyNodeService proxyNodeService;

    /**
     * 客户列表（关键词、状态、分页，含 node_bind_count）
     */
    @PreAuthorize("@ss.hasPermi('member:customer:list')")
    @GetMapping("/list")
    public TableDataInfo list(MbCustomer query) {
        startPage();
        List<MbCustomer> list = mbCustomerService.selectList(query);
        return getDataTable(list);
    }

    /**
     * 导出客户列表
     */
    @PreAuthorize("@ss.hasPermi('member:customer:export')")
    @Log(title = "会员客户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MbCustomer query) {
        List<MbCustomer> list = mbCustomerService.selectList(query);
        ExcelUtil<MbCustomer> util = new ExcelUtil<>(MbCustomer.class);
        util.exportExcel(response, list, "客户数据");
    }

    /**
     * 客户详情
     */
    @PreAuthorize("@ss.hasPermi('member:customer:query')")
    @GetMapping("/{customerId}")
    public AjaxResult getInfo(@PathVariable Long customerId) {
        MbCustomer customer = mbCustomerService.getById(customerId);
        if (customer == null) {
            return AjaxResult.error("客户不存在");
        }
        return success(customer);
    }

    /**
     * 客户关联的节点/VPS 列表（按 customer_id 过滤）
     */
    @PreAuthorize("@ss.hasPermi('member:customer:query')")
    @GetMapping("/{customerId}/bindings")
    public AjaxResult bindings(@PathVariable Long customerId) {
        ProxyNode query = new ProxyNode();
        query.setCustomerId(customerId);
        List<ProxyNode> list = proxyNodeService.selectList(query);
        return success(list);
    }

    /**
     * 新增客户
     */
    @PreAuthorize("@ss.hasPermi('member:customer:add')")
    @Log(title = "会员客户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MbCustomer row) {
        if (StringUtils.isEmpty(row.getUsername())) {
            return AjaxResult.error("用户名不能为空");
        }
        if (!mbCustomerService.checkUsernameUnique(row)) {
            return AjaxResult.error("用户名已存在");
        }
        if (StringUtils.isEmpty(row.getEmail())) {
            return AjaxResult.error("邮箱不能为空");
        }
        if (StringUtils.isEmpty(row.getPassword())) {
            return AjaxResult.error("密码不能为空");
        }
        if (row.getStatus() == null) {
            row.setStatus("0");
        }
        row.setCreateBy(getUsername());
        row.setPassword(SecurityUtils.encryptPassword(row.getPassword()));
        return toAjax(mbCustomerService.insert(row));
    }

    /**
     * 修改客户（手机号、微信号、状态等）
     */
    @PreAuthorize("@ss.hasPermi('member:customer:edit')")
    @Log(title = "会员客户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MbCustomer row) {
        if (row.getId() == null) {
            return AjaxResult.error("参数错误：缺少客户 id");
        }
        MbCustomer existing = mbCustomerService.getById(row.getId());
        if (existing == null) {
            return AjaxResult.error("客户不存在");
        }
        if (StringUtils.isNotEmpty(row.getUsername()) && !row.getUsername().equals(existing.getUsername())) {
            if (!mbCustomerService.checkUsernameUnique(row)) {
                return AjaxResult.error("用户名已存在");
            }
        }
        row.setUpdateBy(getUsername());
        return toAjax(mbCustomerService.update(row));
    }

    /**
     * 重置密码
     */
    @PreAuthorize("@ss.hasPermi('member:customer:resetPwd')")
    @Log(title = "会员客户", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody MbCustomer row) {
        if (row.getId() == null || StringUtils.isEmpty(row.getPassword())) {
            return AjaxResult.error("参数错误");
        }
        mbCustomerService.resetPwd(row.getId(), SecurityUtils.encryptPassword(row.getPassword()), getUsername());
        return success("重置成功，新密码已生效");
    }

    /**
     * 启用/禁用
     */
    @PreAuthorize("@ss.hasPermi('member:customer:edit')")
    @Log(title = "会员客户", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody MbCustomer row) {
        if (row.getId() == null || row.getStatus() == null) {
            return AjaxResult.error("参数错误");
        }
        return toAjax(mbCustomerService.updateStatus(row.getId(), row.getStatus(), getUsername()));
    }

    /**
     * 删除客户
     */
    @PreAuthorize("@ss.hasPermi('member:customer:remove')")
    @Log(title = "会员客户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(mbCustomerService.deleteByIds(ids));
    }
}
