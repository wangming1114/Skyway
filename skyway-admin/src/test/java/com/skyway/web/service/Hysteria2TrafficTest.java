package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.junit.jupiter.api.Test;

public class Hysteria2TrafficTest {
    @Test
    void iptablesUsesUdpCountersAndRemovesBothTransportsOnDeletion() throws Exception {
        Remote remote = new Remote(false);
        remote.service.ensureTrafficRulesForPort(7L, 32332, "Hysteria2");
        assertTrue(remote.commands.stream().anyMatch(c -> c.contains("-p udp --dport 32332")));
        Map<Integer, VpsSshCommandService.TrafficPair> counters = remote.service.readTrafficCounters(7L);
        assertEquals(4096, counters.get(32332).rx);
        assertEquals(8192, counters.get(32332).tx);
        remote.service.removeTrafficRulesForPort(7L, 32332);
        assertTrue(remote.commands.stream().anyMatch(c -> c.contains("iptables -D NODE_TRAFFIC_IN -p udp --dport 32332")));
        assertTrue(remote.commands.stream().anyMatch(c -> c.contains("iptables -D NODE_TRAFFIC_IN -p tcp --dport 32332")));
    }

    @Test
    void nftEnsuresCountersWithoutDeletingOrResettingExistingRules() throws Exception {
        Remote remote = new Remote(true);
        remote.service.ensureTrafficRulesForPort(7L, 32332, "Hysteria2");
        remote.service.ensureTrafficRulesForPort(7L, 32332, "Hysteria2");
        assertTrue(remote.commands.stream().anyMatch(c -> c.contains("udp dport 32332") && c.contains("grep -Eq")));
        assertFalse(remote.commands.stream().anyMatch(c -> c.contains("delete rule") || c.contains("reset")));
        Map<Integer, VpsSshCommandService.TrafficPair> counters = remote.service.readTrafficCounters(7L);
        assertEquals(4096, counters.get(32332).rx);
        assertEquals(8192, counters.get(32332).tx);
        remote.service.removeTrafficRulesForPort(7L, 32332);
        assertTrue(remote.commands.stream().anyMatch(c -> c.contains("nft -a list chain") && c.contains("handle")));
    }

    private static class Remote {
        final VpsSshCommandService service = spy(new VpsSshCommandService());
        final List<String> commands = new ArrayList<>();

        Remote(boolean nft) throws Exception {
            SSHClient ssh = mock(SSHClient.class);
            Session session = mock(Session.class);
            doReturn(ssh).when(service).createSshClient(7L);
            when(ssh.startSession()).thenReturn(session);
            when(session.exec(anyString())).thenAnswer(invocation -> {
                String command = invocation.getArgument(0);
                commands.add(command);
                String output = "";
                if (command.contains("command -v iptables")) output = nft ? "nft" : "iptables";
                else if (command.equals("nft list table inet node_traffic 2>&1")) {
                    output = "udp dport 32332 counter packets 1 bytes 4096 accept\n"
                            + "udp sport 32332 counter bytes 8192 packets 2 accept\n";
                } else if (command.contains("___CHAIN_OUT___")) {
                    output = "1 4096 ACCEPT udp -- * * 0.0.0.0/0 0.0.0.0/0 udp dpt:32332\n___CHAIN_OUT___\n"
                            + "2 8192 ACCEPT udp -- * * 0.0.0.0/0 0.0.0.0/0 udp spt:32332\n";
                }
                Command cmd = mock(Command.class);
                when(cmd.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
                return cmd;
            });
        }
    }
}
