package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class VpsRealtimeSpeedParserTest {

    @Test
    public void parseSingboxSpeedOutputAggregatesPortSpeeds() {
        String output = "=================================================\n"
                + " 🚀 节点端口实时网速 (按总速度倒序 | 空闲显示0.00)\n"
                + "=================================================\n"
                + "端口 (Port) | 上传 (MB/s)   | 下载 (MB/s)\n"
                + "-------------------------------------------------\n"
                + "10007        | 0.25            | 1.50\n"
                + "10006        | 2.00            | 0.75\n";

        VpsSshCommandService.RealtimeSpeedSnapshot snapshot = VpsSshCommandService.parseRealtimeSpeedOutput(output);

        assertEquals(2, snapshot.getPorts().size());
        assertEquals(0.25D, snapshot.getPorts().get("10007").getUpMbps(), 0.0001D);
        assertEquals(1.50D, snapshot.getPorts().get("10007").getDownMbps(), 0.0001D);
        assertEquals(2.25D, snapshot.getTotalUpMbps(), 0.0001D);
        assertEquals(2.25D, snapshot.getTotalDownMbps(), 0.0001D);
    }

    @Test
    public void realtimeSpeedOnlyAllowsRunningStatus() {
        assertEquals(true, VpsSshCommandService.isRealtimeSpeedAllowedStatus("running"));
        assertEquals(false, VpsSshCommandService.isRealtimeSpeedAllowedStatus("stopped"));
        assertEquals(false, VpsSshCommandService.isRealtimeSpeedAllowedStatus("abnormal"));
        assertEquals(false, VpsSshCommandService.isRealtimeSpeedAllowedStatus(null));
    }
}
