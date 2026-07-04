package com.skyway.resource.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.skyway.resource.mapper.ProxyNodeTrafficMapper;
import com.skyway.resource.mapper.ProxyNodeTrafficSnapshotMapper;

@ExtendWith(MockitoExtension.class)
public class ProxyNodeTrafficServiceImplTest {

    @Mock
    private ProxyNodeTrafficMapper proxyNodeTrafficMapper;

    @Mock
    private ProxyNodeTrafficSnapshotMapper proxyNodeTrafficSnapshotMapper;

    @InjectMocks
    private ProxyNodeTrafficServiceImpl service;

    @Test
    public void customerTrafficRankNormalizesVpsDisplayFields() {
        Map<String, Object> row = new HashMap<>();
        row.put("customerid", 8L);
        row.put("username", "alice");
        row.put("nodeid", 18L);
        row.put("nodename", "alice-node-1");
        row.put("port", 10007);
        row.put("instanceid", 3L);
        row.put("instancename", "tokyo-vps");
        row.put("instanceip", "203.0.113.8");
        row.put("totalrx", 1024L);
        row.put("totaltx", 2048L);
        when(proxyNodeTrafficMapper.selectCustomerTrafficRank(any(Date.class), any(Date.class))).thenReturn(Collections.singletonList(row));

        List<Map<String, Object>> rows = service.getCustomerTrafficRank(new Date(), new Date());
        Map<String, Object> data = rows.get(0);

        assertEquals(3L, data.get("instanceId"));
        assertEquals("tokyo-vps", data.get("instanceName"));
        assertEquals("203.0.113.8", data.get("instanceIp"));
        assertEquals(10007L, data.get("port"));
        assertEquals(3072L, data.get("totalTraffic"));
    }
}
