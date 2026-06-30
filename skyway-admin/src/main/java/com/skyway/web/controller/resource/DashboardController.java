package com.skyway.web.controller.resource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
     * 指定时间范围内 VPS 流量倒序排行。
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/vpsTrafficRank")
    public AjaxResult vpsTrafficRank(@RequestParam(defaultValue = "day") String range,
                                     @RequestParam(required = false) String startTime,
                                     @RequestParam(required = false) String endTime) {
        Date[] bounds = resolveCustomerTrafficBounds(range, startTime, endTime);
        return success(proxyNodeTrafficService.getVpsTrafficRank(bounds[0], bounds[1]));
    }

    /**
     * 指定时间范围内客户节点流量倒序排行。
     */
    @PreAuthorize("@ss.hasPermi('resource:vps:list')")
    @GetMapping("/customerTrafficRank")
    public AjaxResult customerTrafficRank(@RequestParam(defaultValue = "day") String range,
                                          @RequestParam(required = false) String startTime,
                                          @RequestParam(required = false) String endTime) {
        Date[] bounds = resolveCustomerTrafficBounds(range, startTime, endTime);
        return success(proxyNodeTrafficService.getCustomerTrafficRank(bounds[0], bounds[1]));
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

    private Date[] resolveCustomerTrafficBounds(String range, String startTime, String endTime) {
        if ("custom".equalsIgnoreCase(range) && startTime != null && endTime != null) {
            Date customStart = parseDate(startTime, true);
            Date customEnd = parseDate(endTime, false);
            if (customStart != null && customEnd != null) {
                if (customEnd.before(customStart)) {
                    return new Date[] { parseDate(endTime, true), parseDate(startTime, false) };
                }
                return new Date[] { customStart, customEnd };
            }
        }
        return recentBounds(daysForCustomerTrafficRange(range));
    }

    private int daysForCustomerTrafficRange(String range) {
        if (range == null || range.trim().isEmpty()) {
            return 1;
        }
        if ("day".equalsIgnoreCase(range)) {
            return 1;
        }
        if ("week".equalsIgnoreCase(range)) {
            return 7;
        }
        if ("year".equalsIgnoreCase(range)) {
            return 365;
        }
        return 30;
    }

    private Date[] recentBounds(int days) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        start.add(Calendar.DAY_OF_MONTH, -(Math.max(days, 1) - 1));

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        return new Date[] { start.getTime(), end.getTime() };
    }

    private Date parseDate(String value, boolean startOfDay) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setLenient(false);
            Date date = format.parse(value);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, startOfDay ? 0 : 23);
            calendar.set(Calendar.MINUTE, startOfDay ? 0 : 59);
            calendar.set(Calendar.SECOND, startOfDay ? 0 : 59);
            calendar.set(Calendar.MILLISECOND, startOfDay ? 0 : 999);
            return calendar.getTime();
        } catch (ParseException ignored) {
            return null;
        }
    }
}
