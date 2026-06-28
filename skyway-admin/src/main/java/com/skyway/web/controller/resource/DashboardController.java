package com.skyway.web.controller.resource;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.service.IMbCustomerService;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IProxyNodeRateLimitService;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeTrafficService;
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
    private IProxyNodeRateLimitService proxyNodeRateLimitService;
    @Autowired
    private IProxyNodeTrafficService proxyNodeTrafficService;
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
        int expiringNodes = countExpiringNodes();
        int expiredNodes = countExpiredNodes();
        int limitedNodes = proxyNodeRateLimitService.countActive();
        long vpsTrafficTotal = proxyNodeTrafficService.getVpsTrafficTotal();
        long customerTrafficTotal = proxyNodeTrafficService.getCustomerTrafficTotal();

        Map<String, Object> summary = new HashMap<>(16);
        summary.put("totalVps", totalVps);
        summary.put("runningVps", runningVps);
        summary.put("stoppedVps", stoppedVps);
        summary.put("abnormalVps", abnormalVps);
        summary.put("totalNodes", totalNodes);
        summary.put("normalNodes", normalNodes);
        summary.put("disabledNodes", disabledNodes);
        summary.put("totalCustomers", totalCustomers);
        summary.put("expiringNodes", expiringNodes);
        summary.put("expiredNodes", expiredNodes);
        summary.put("limitedNodes", limitedNodes);
        summary.put("vpsTrafficTotal", vpsTrafficTotal);
        summary.put("customerTrafficTotal", customerTrafficTotal);
        return success(summary);
    }

    /**
     * VPS 维度分日流量趋势，days 支持 1/7/15/30 等首页粒度。
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/vpsTrafficTrend")
    public AjaxResult vpsTrafficTrend(@RequestParam(defaultValue = "7") Integer days) {
        return success(proxyNodeTrafficService.getDailyTrafficByInstance(normalizeDays(days)));
    }

    /**
     * 指定时间范围内客户流量倒序排行。
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/customerTrafficRank")
    public AjaxResult customerTrafficRank(@RequestParam(defaultValue = "30") Integer days) {
        return success(proxyNodeTrafficService.getCustomerTrafficRank(normalizeDays(days)));
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

    private int countExpiredNodes() {
        ProxyNode q = new ProxyNode();
        q.setExpireStatus("expired");
        return proxyNodeService.count(q);
    }

    private int countExpiringNodes() {
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        List<ProxyNode> nodes = proxyNodeService.listExpiringWithin(now, calendar.getTime());
        return nodes != null ? nodes.size() : 0;
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return 7;
        }
        return Math.max(1, Math.min(days, 30));
    }
}
