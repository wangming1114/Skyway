package com.skyway.web.task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.skyway.common.utils.StringUtils;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.service.IMbCustomerService;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.web.service.CustomerEmailCodeService;
import com.skyway.web.service.VpsSshCommandService;

/**
 * 节点到期停用与邮件通知定时任务。
 * 查询已到期且状态为正常的代理节点，按列表「停止」流程在服务器上停用并更新库表，
 * 向管理员及客户（若有有效邮箱）发送通知。
 * 在「系统管理 -> 定时任务」中配置，invoke_target = proxyNodeExpireTask.processExpired
 */
@Component("proxyNodeExpireTask")
public class ProxyNodeExpireTask {

    private static final Logger log = LoggerFactory.getLogger(ProxyNodeExpireTask.class);
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private IProxyNodeService proxyNodeService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    @Autowired
    private IMbCustomerService mbCustomerService;

    @Autowired
    private CustomerEmailCodeService customerEmailCodeService;

    @Value("${skyway.expire-notify.admin-email:1942152752@qq.com}")
    private String adminEmail;

    public void processExpired() {
        Date now = new Date();
        List<ProxyNode> expired = proxyNodeService.listExpiredAndNormal(now);
        if (expired == null || expired.isEmpty()) {
            return;
        }
        log.info("proxyNodeExpireTask: found {} expired node(s) to stop", expired.size());

        List<ProxyNode> stopped = new ArrayList<>();
        for (ProxyNode node : expired) {
            if (node.getId() == null) {
                continue;
            }
            if ("1".equals(node.getStatus())) {
                continue;
            }
            try {
                vpsSshCommandService.renameProxyNodeConfig(
                    node.getInstanceId(),
                    node.getNodeName(),
                    true);
            } catch (Exception e) {
                log.warn("proxyNodeExpireTask: renameProxyNodeConfig failed nodeId={}, nodeName={}: {}",
                    node.getId(), node.getNodeName(), e.getMessage());
                continue;
            }
            ProxyNode toUpdate = proxyNodeService.getById(node.getId());
            if (toUpdate != null) {
                toUpdate.setStatus("1");
                toUpdate.setUpdateBy("system");
                proxyNodeService.update(toUpdate);
                stopped.add(toUpdate);
            }
        }

        if (stopped.isEmpty()) {
            return;
        }

        String adminSubject = "服务到期停用通知";
        StringBuilder adminBody = new StringBuilder();
        adminBody.append("部分魔法工具已到期，系统已自动停用。本次共 ").append(stopped.size()).append(" 条，详情请登录后台查看。\n\n如有疑问请联系管理员。");
        if (StringUtils.isNotEmpty(adminEmail)) {
            if (!customerEmailCodeService.sendNotification(adminEmail, adminSubject, adminBody.toString())) {
                log.warn("proxyNodeExpireTask: failed to send admin notification to {}", adminEmail);
            }
        }

        Map<Long, List<ProxyNode>> byCustomer = stopped.stream()
            .filter(n -> n.getCustomerId() != null)
            .collect(Collectors.groupingBy(ProxyNode::getCustomerId));

        for (Map.Entry<Long, List<ProxyNode>> e : byCustomer.entrySet()) {
            Long customerId = e.getKey();
            List<ProxyNode> nodes = e.getValue();
            MbCustomer customer = mbCustomerService.getById(customerId);
            if (customer == null) {
                continue;
            }
            String email = customer.getEmail();
            if (StringUtils.isEmpty(email) || !email.trim().matches("^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
                continue;
            }
            String customerSubject = "服务到期通知";
            StringBuilder customerBody = new StringBuilder();
            customerBody.append("您好，").append(customer.getUsername() != null ? customer.getUsername() : "客户").append("：\n\n");
            customerBody.append("您使用的魔法工具已到期并已自动停用。如有需要请联系管理员。");
            if (!customerEmailCodeService.sendNotification(email.trim(), customerSubject, customerBody.toString())) {
                log.warn("proxyNodeExpireTask: failed to send customer notification to customerId={}, email={}", customerId, email);
            }
        }

        sendExpiringSoonReminders(now);
    }

    /** 发送「7 天内即将到期」提醒邮件（管理员 + 客户），文案隐晦不涉及节点/协议。 */
    private void sendExpiringSoonReminders(Date now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_MONTH, 7);
        Date end = cal.getTime();
        List<ProxyNode> expiringSoon = proxyNodeService.listExpiringWithin(now, end);
        if (expiringSoon == null || expiringSoon.isEmpty()) {
            return;
        }
        log.info("proxyNodeExpireTask: found {} node(s) expiring within 7 days, sending reminders", expiringSoon.size());

        String adminSubject = "即将到期提醒";
        String adminBody = "部分魔法工具将在 7 天内到期，本次共 " + expiringSoon.size() + " 条，请登录后台查看并提醒用户续期。";
        if (StringUtils.isNotEmpty(adminEmail)) {
            if (!customerEmailCodeService.sendNotification(adminEmail, adminSubject, adminBody)) {
                log.warn("proxyNodeExpireTask: failed to send admin expiring-soon notification to {}", adminEmail);
            }
        }

        Map<Long, List<ProxyNode>> byCustomer = expiringSoon.stream()
            .filter(n -> n.getCustomerId() != null)
            .collect(Collectors.groupingBy(ProxyNode::getCustomerId));

        for (Map.Entry<Long, List<ProxyNode>> e : byCustomer.entrySet()) {
            Long customerId = e.getKey();
            MbCustomer customer = mbCustomerService.getById(customerId);
            if (customer == null) continue;
            String email = customer.getEmail();
            if (StringUtils.isEmpty(email) || !email.trim().matches("^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) continue;

            String customerSubject = "即将到期提醒";
            String customerBody = "您好，" + (customer.getUsername() != null ? customer.getUsername() : "客户") + "：\n\n您使用的魔法工具将在 7 天内到期，请及时续期或联系管理员。";
            if (!customerEmailCodeService.sendNotification(email.trim(), customerSubject, customerBody)) {
                log.warn("proxyNodeExpireTask: failed to send customer expiring-soon notification to customerId={}, email={}", customerId, email);
            }
        }
    }
}
