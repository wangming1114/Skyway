package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.skyway.resource.domain.VpsInstance;

public class VpsRealtimeSpeedManagerTest {

    @Test
    public void onlyRunningInstancesWithCompleteSshInfoStartSpeedWorker() {
        VpsInstance running = instance(1L, "running");
        VpsInstance stopped = instance(2L, "stopped");
        VpsInstance missingSsh = instance(3L, "running");
        missingSsh.setSshUsername("");

        assertTrue(VpsRealtimeSpeedManager.shouldStartSpeedWorker(running));
        assertFalse(VpsRealtimeSpeedManager.shouldStartSpeedWorker(stopped));
        assertFalse(VpsRealtimeSpeedManager.shouldStartSpeedWorker(missingSsh));
    }

    @Test
    public void skippedSnapshotMarksInstanceAsUnmonitored() {
        VpsSshCommandService.RealtimeSpeedSnapshot snapshot = VpsRealtimeSpeedManager.skippedSnapshot("stopped");

        assertTrue(snapshot.isSkipped());
        assertTrue(snapshot.getMessage().contains("未监控"));
    }

    private static VpsInstance instance(Long id, String status) {
        VpsInstance instance = new VpsInstance();
        instance.setId(id);
        instance.setIp("127.0.0.1");
        instance.setSshPort(22);
        instance.setSshUsername("root");
        instance.setSshPassword("pass");
        instance.setStatus(status);
        return instance;
    }
}
