package com.skyway.web.controller.resource;

import java.util.List;
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
import com.skyway.common.enums.BusinessType;
import com.skyway.resource.domain.VpsCategory;
import com.skyway.resource.service.IVpsCategoryService;

/**
 * 资源分类与节点（VPS 分类树 + 节点列表）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/resource/vps/category")
public class VpsCategoryController extends BaseController {

    @Autowired
    private IVpsCategoryService vpsCategoryService;

    /**
     * 获取分类树列表（仅 type='1'）
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/list")
    public AjaxResult listCategoryTree() {
        List<VpsCategory> list = vpsCategoryService.listCategoryTree();
        return success(list);
    }

    /**
     * 根据ID获取详情
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(vpsCategoryService.getById(id));
    }

    /**
     * 新增
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:add')")
    @Log(title = "VPS分类/节点", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpsCategory row) {
        return toAjax(vpsCategoryService.insert(row));
    }

    /**
     * 修改
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:edit')")
    @Log(title = "VPS分类/节点", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpsCategory row) {
        return toAjax(vpsCategoryService.update(row));
    }

    /**
     * 删除（存在子分类或实例时禁止）
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:remove')")
    @Log(title = "VPS分类/节点", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(vpsCategoryService.deleteById(id));
    }
}
