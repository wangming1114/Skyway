package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.skyway.web.service.VpsSshCommandService.RemotePortScan;

public class VpsRemotePortScanParserTest {

    @Test
    public void parsesSocketsConfigsDockerAndEphemeralRange() throws Exception {
        String output = "SOCKET_HEX=2710\n"
                + "SOCKET_HEX=2AF8\n"
                + "CONFIG_PORT=12000\n"
                + "DOCKER_PORT=13000\n"
                + "EPHEMERAL=32768-60999\n"
                + "SOCKETS_OK\nCONFIG_OK\nDOCKER_OK\nEPHEMERAL_OK\n";

        RemotePortScan scan = VpsSshCommandService.parseRemotePortScan(output);

        assertTrue(scan.isComplete());
        assertTrue(scan.getUnavailablePorts().contains(10000));
        assertTrue(scan.getUnavailablePorts().contains(11000));
        assertTrue(scan.getUnavailablePorts().contains(12000));
        assertTrue(scan.getUnavailablePorts().contains(13000));
        assertEquals(Integer.valueOf(32768), scan.getEphemeralStart());
        assertEquals(Integer.valueOf(60999), scan.getEphemeralEnd());
        assertEquals("sing-box 配置", scan.describeSources(12000));
        assertEquals("Docker 映射", scan.describeSources(13000));
    }

    @Test
    public void missingMarkerMakesScanIncomplete() throws Exception {
        RemotePortScan scan = VpsSshCommandService.parseRemotePortScan(
                "SOCKETS_OK\nCONFIG_OK\nEPHEMERAL=32768-60999\nEPHEMERAL_OK\n");

        assertFalse(scan.isComplete());
        assertEquals(1, scan.getMissingSources().size());
        assertEquals("Docker 映射", scan.getMissingSources().get(0));
    }

    @Test
    public void invalidPortsAndInvalidRangeAreNotAccepted() throws Exception {
        RemotePortScan scan = VpsSshCommandService.parseRemotePortScan(
                "SOCKET_HEX=NOT_HEX\nCONFIG_PORT=70000\nDOCKER_PORT=0\n"
                        + "EPHEMERAL=60999-32768\nSOCKETS_OK\nCONFIG_OK\nDOCKER_OK\nEPHEMERAL_OK\n");

        assertTrue(scan.getUnavailablePorts().isEmpty());
        assertFalse(scan.isComplete());
        assertTrue(scan.getMissingSources().contains("动态端口范围"));
    }
}
