package com.skyway.web.task;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IVpsInstanceService;
import com.skyway.web.service.VpsSshCommandService;

/**
 * VPS 实例状态同步定时任务：通过 SSH 探测各实例可达性，自动更新 res_instance.status。
 * 在「系统管理 -> 定时任务」中配置，invoke_target = vpsInstanceStatusSyncTask.sync
 */
@Component("vpsInstanceStatusSyncTask")
public class VpsInstanceStatusSyncTask {

    private static final Logger log = LoggerFactory.getLogger(VpsInstanceStatusSyncTask.class);

    @Autowired
    private IVpsInstanceService vpsInstanceService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    public void sync() {
        List<VpsInstance> list = vpsInstanceService.selectList(new VpsInstance());
        if (list == null || list.isEmpty()) {
            return;
        }
        int updated = 0;
        for (VpsInstance inst : list) {
            if (inst.getId() == null || StringUtils.isEmpty(inst.getIp())) {
                continue;
            }
            try {
                String newStatus = vpsSshCommandService.detectInstanceStatus(inst.getId());
                if (newStatus != null && !newStatus.equals(inst.getStatus())) {
                    VpsInstance update = new VpsInstance();
                    update.setId(inst.getId());
                    update.setStatus(newStatus);
                    vpsInstanceService.update(update);
                    updated++;
                    log.debug("VPS instance id={} status updated to {}", inst.getId(), newStatus);
                }
            } catch (Exception e) {
                log.warn("VPS instance id={} status sync failed: {}", inst.getId(), e.getMessage());
            }
        }
        if (updated > 0) {
            log.info("VPS status sync finished, {} instance(s) updated", updated);
        }
    }
}
