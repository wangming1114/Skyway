package com.skyway.web.controller.resource;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.service.IMbCustomerService;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IVpsInstanceService;

/**
 * 管理端首页仪表盘
 */
@RestController
@RequestMapping("/resource/vps/dashboard")
public class DashboardController extends BaseController {

    @Autowired
    private IVpsInstanceService vpsInstanceService;
    @Autowired
    private IProxyNodeService proxyNodeService;
    @Autowired
    private IMbCustomerService mbCustomerService;

    /**
     * 仪表盘汇总（VPS/节点/客户数量）
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        int totalVps = vpsInstanceService.count(new VpsInstance());
        int runningVps = countVpsByStatus("running");
        int stoppedVps = countVpsByStatus("stopped");
        int abnormalVps = countVpsByStatus("abnormal");

        int totalNodes = proxyNodeService.count(new ProxyNode());
        int normalNodes = countNodeByStatus("0");
        int disabledNodes = countNodeByStatus("1");

        int totalCustomers = mbCustomerService.count(new MbCustomer());

        Map<String, Integer> summary = new HashMap<>(16);
        summary.put("totalVps", totalVps);
        summary.put("runningVps", runningVps);
        summary.put("stoppedVps", stoppedVps);
        summary.put("abnormalVps", abnormalVps);
        summary.put("totalNodes", totalNodes);
        summary.put("normalNodes", normalNodes);
        summary.put("disabledNodes", disabledNodes);
        summary.put("totalCustomers", totalCustomers);
        return success(summary);
    }

    private int countVpsByStatus(String status) {
        VpsInstance q = new VpsInstance();
        q.setStatus(status);
        return vpsInstanceService.count(q);
    }

    private int countNodeByStatus(String status) {
        ProxyNode q = new ProxyNode();
        q.setStatus(status);
        return proxyNodeService.count(q);
    }
}
