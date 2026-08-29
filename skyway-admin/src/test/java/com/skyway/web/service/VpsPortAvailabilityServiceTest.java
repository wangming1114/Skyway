package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.skyway.common.exception.ServiceException;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.web.service.VpsPortAvailabilityService.PortRecommendation;
import com.skyway.web.service.VpsSshCommandService.RemotePortScan;

@ExtendWith(MockitoExtension.class)
public class VpsPortAvailabilityServiceTest {

    @Mock
    private IProxyNodeService proxyNodeService;

    @Mock
    private VpsSshCommandService vpsSshCommandService;

    private VpsPortAvailabilityService service;

    @BeforeEach
    public void setUp() {
        service = new VpsPortAvailabilityService();
        ReflectionTestUtils.setField(service, "proxyNodeService", proxyNodeService);
        ReflectionTestUtils.setField(service, "vpsSshCommandService", vpsSshCommandService);
    }

    @Test
    public void verifiedRecommendationCombinesEverySourceAndExclusions() throws Exception {
        when(proxyNodeService.listUsedPorts(7L)).thenReturn(Collections.singletonList(10000));
        when(vpsSshCommandService.scanRemotePorts(7L)).thenReturn(completeScan(
                "SOCKET_HEX=2711\nCONFIG_PORT=10002\nDOCKER_PORT=10003\nEPHEMERAL=10004-10005\n"));

        PortRecommendation result = service.recommend(7L, Collections.singleton(10006));

        assertEquals(10007, result.getPort());
        assertTrue(result.isVerified());
        assertEquals(null, result.getWarning());
    }

    @Test
    public void incompleteScanFallsBackToDatabaseOnlyAndWarns() throws Exception {
        when(proxyNodeService.listUsedPorts(7L)).thenReturn(Collections.<Integer>emptyList());
        when(vpsSshCommandService.scanRemotePorts(7L)).thenReturn(
                VpsSshCommandService.parseRemotePortScan(
                        "SOCKET_HEX=2710\nSOCKETS_OK\nCONFIG_OK\nEPHEMERAL=32768-60999\nEPHEMERAL_OK\n"));

        PortRecommendation result = service.recommend(7L, Collections.<Integer>emptySet());

        assertEquals(10000, result.getPort());
        assertFalse(result.isVerified());
        assertTrue(result.getWarning().contains("已退回数据库推荐"));
    }

    @Test
    public void sshFailureFallsBackToDatabaseAndKeepsBatchExclusions() throws Exception {
        when(proxyNodeService.listUsedPorts(7L)).thenReturn(Collections.singletonList(10000));
        when(vpsSshCommandService.scanRemotePorts(7L)).thenThrow(new IOException("connection refused"));

        PortRecommendation result = service.recommend(7L, Collections.singleton(10001));

        assertEquals(10002, result.getPort());
        assertFalse(result.isVerified());
        assertTrue(result.getWarning().contains("SSH 扫描失败"));
    }

    @Test
    public void automaticCreationRetainsAvailableRecommendationAndReplacesStaleOne() throws Exception {
        when(proxyNodeService.listUsedPorts(7L)).thenReturn(Collections.singletonList(10000));
        when(vpsSshCommandService.scanRemotePorts(7L)).thenReturn(completeScan("SOCKET_HEX=2711\n"));

        assertEquals(10002, service.resolveAutoPortForCreate(7L, 10002));
        assertEquals(10002, service.resolveAutoPortForCreate(7L, 10001));
    }

    @Test
    public void automaticCreationMovesPortOutOfEphemeralRange() throws Exception {
        when(proxyNodeService.listUsedPorts(7L)).thenReturn(Collections.<Integer>emptyList());
        when(vpsSshCommandService.scanRemotePorts(7L)).thenReturn(completeScan("EPHEMERAL=10000-10010\n"));

        assertEquals(10011, service.resolveAutoPortForCreate(7L, 10005));
    }

    @Test
    public void manualCreationRejectsDatabaseAndRemoteOccupancyWithoutChangingPort() throws Exception {
        ProxyNode existing = new ProxyNode();
        when(proxyNodeService.getByInstanceIdAndPort(7L, 10000)).thenReturn(existing);
        ServiceException databaseError = assertThrows(ServiceException.class,
                () -> service.assertAvailableForCreate(7L, 10000));
        assertTrue(databaseError.getMessage().contains("代理节点使用"));

        when(proxyNodeService.getByInstanceIdAndPort(7L, 10001)).thenReturn(null);
        when(vpsSshCommandService.scanRemotePorts(7L)).thenReturn(completeScan("CONFIG_PORT=10001\n"));
        ServiceException remoteError = assertThrows(ServiceException.class,
                () -> service.assertAvailableForCreate(7L, 10001));
        assertTrue(remoteError.getMessage().contains("sing-box 配置"));
    }

    @Test
    public void candidateSelectionSkipsRangeAndReturnsNullWhenExhausted() {
        assertEquals(Integer.valueOf(10003), VpsPortAvailabilityService.selectRecommendedPort(
                new LinkedHashSet<>(Arrays.asList(10000, 10001)), 10002, 10002));

        Set<Integer> all = new LinkedHashSet<>();
        for (int port = VpsPortAvailabilityService.MIN_RECOMMENDED_PORT;
             port <= VpsPortAvailabilityService.MAX_PORT; port++) {
            all.add(port);
        }
        assertEquals(null, VpsPortAvailabilityService.selectRecommendedPort(all, null, null));
    }

    @Test
    public void parsesAndValidatesBatchExclusions() {
        assertEquals(new LinkedHashSet<>(Arrays.asList(10000, 10001)),
                service.parseExcludedPorts("10000, 10001,10000"));
        assertThrows(ServiceException.class, () -> service.parseExcludedPorts("10000,nope"));
        assertThrows(ServiceException.class, () -> service.parseExcludedPorts("70000"));
    }

    private static RemotePortScan completeScan(String body) throws Exception {
        String ephemeral = body.contains("EPHEMERAL=") ? "" : "EPHEMERAL=32768-60999\n";
        return VpsSshCommandService.parseRemotePortScan(body + ephemeral
                + "SOCKETS_OK\nCONFIG_OK\nDOCKER_OK\nEPHEMERAL_OK\n");
    }
}
