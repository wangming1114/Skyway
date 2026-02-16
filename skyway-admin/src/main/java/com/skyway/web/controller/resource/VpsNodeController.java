package com.skyway.web.controller.resource;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.resource.domain.VpsCategory;
import com.skyway.resource.service.IVpsCategoryService;

/**
 * VPS 节点列表（供下拉与筛选，数据来自 res_category type='2'）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/resource/vps/node")
public class VpsNodeController extends BaseController {

    @Autowired
    private IVpsCategoryService vpsCategoryService;

    /**
     * 获取节点扁平列表（仅 type='2'）
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        List<VpsCategory> list = vpsCategoryService.listNode();
        return success(list);
    }
}
