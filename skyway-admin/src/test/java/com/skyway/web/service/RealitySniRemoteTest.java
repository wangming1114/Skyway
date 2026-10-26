package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.skyway.resource.domain.ProxyNode;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.junit.jupiter.api.Test;

public class RealitySniRemoteTest {
    @Test
    void activeNodeValidatesReplacesAndRestartsThenCanRestoreExactSnapshot() throws Exception {
        Remote remote = new Remote();
        ProxyNode node = RealitySniSupportTest.node();
        String snapshot = remote.service.applyRealitySniToProxyNodeConfig(node, "www.bing.com");
        assertEquals(RealitySniSupportTest.CONFIG, snapshot);
        assertEquals(JSON.parseObject(RealitySniSupport.patchInbound(snapshot, node, "www.bing.com")), JSON.parseObject(remote.current));
        assertEquals(1, remote.restarts);
        assertEquals(1, remote.checks);
        remote.service.restoreRealitySniConfig(node, snapshot);
        assertEquals(snapshot, remote.current);
        assertEquals(2, remote.restarts);
    }

    @Test
    void unchangedSniDoesNotWriteValidateOrRestart() throws Exception {
        Remote remote = new Remote();
        ProxyNode node = RealitySniSupportTest.node();
        remote.current = RealitySniSupport.patchInbound(remote.current, node, "www.bing.com");
        assertNull(remote.service.applyRealitySniToProxyNodeConfig(node, "www.bing.com"));
        assertTrue(remote.writes.isEmpty());
        assertEquals(0, remote.checks);
        assertEquals(0, remote.restarts);
    }

    @Test
    void disabledNodeUpdatesDisabledFileWithoutRestarting() throws Exception {
        Remote remote = new Remote();
        remote.disabled = true;
        ProxyNode node = RealitySniSupportTest.node();
        node.setStatus("1");
        remote.service.applyRealitySniToProxyNodeConfig(node, "www.bing.com");
        assertTrue(remote.commands.stream().anyMatch(c -> c.contains("node.json.disabled")));
        assertEquals(1, remote.checks);
        assertEquals(0, remote.restarts);
    }

    @Test
    void invalidCandidateNeverReplacesOrRestarts() throws Exception {
        Remote remote = new Remote();
        remote.rejectCandidate = true;
        assertThrows(IOException.class, () -> remote.service.applyRealitySniToProxyNodeConfig(RealitySniSupportTest.node(), "www.bing.com"));
        assertEquals(RealitySniSupportTest.CONFIG, remote.current);
        assertEquals(0, remote.restarts);
        assertFalse(remote.commands.stream().anyMatch(c -> c.contains("mv --")));
    }

    @Test
    void restartFailureRestoresBothOriginalValuesAndRestartsOldConfig() throws Exception {
        Remote remote = new Remote();
        remote.failFirstRestart = true;
        assertThrows(IOException.class, () -> remote.service.applyRealitySniToProxyNodeConfig(RealitySniSupportTest.node(), "www.bing.com"));
        assertEquals(RealitySniSupportTest.CONFIG, remote.current);
        assertEquals(2, remote.restarts);
        assertEquals(2, remote.checks);
        assertEquals(RealitySniSupportTest.CONFIG, remote.writes.get(1));
    }

    @Test
    void replacementFailureAlsoRestoresOriginalFile() throws Exception {
        Remote remote = new Remote();
        remote.failFirstReplacement = true;
        assertThrows(IOException.class, () -> remote.service.applyRealitySniToProxyNodeConfig(RealitySniSupportTest.node(), "www.bing.com"));
        assertEquals(RealitySniSupportTest.CONFIG, remote.current);
        assertEquals(1, remote.restarts);
    }

    private static class Remote {
        final VpsSshCommandService service = spy(new VpsSshCommandService());
        final List<String> commands = new ArrayList<>();
        final List<String> writes = new ArrayList<>();
        String current = RealitySniSupportTest.CONFIG;
        boolean disabled;
        boolean rejectCandidate;
        boolean failFirstRestart;
        boolean failFirstReplacement;
        int restarts;
        int checks;
        int replacements;

        Remote() throws Exception {
            SSHClient ssh = mock(SSHClient.class);
            Session session = mock(Session.class);
            doReturn(ssh).when(service).createSshClient(7L);
            when(ssh.startSession()).thenReturn(session);
            when(session.exec(anyString())).thenAnswer(invocation -> {
                String command = invocation.getArgument(0);
                commands.add(command);
                String output = "";
                if (command.contains("__CONF__")) {
                    output = "__CONF__/etc/sing-box/conf/node.json" + (disabled ? ".disabled" : "");
                } else if (command.startsWith("cat --")) {
                    output = current;
                } else if (command.contains("base64 -d")) {
                    Matcher matcher = Pattern.compile("[A-Za-z0-9+/]{40,}={0,2}").matcher(command);
                    assertTrue(matcher.find());
                    writes.add(new String(Base64.getDecoder().decode(matcher.group()), StandardCharsets.UTF_8));
                } else if (command.contains("sing-box check")) {
                    checks++;
                    output = rejectCandidate ? "invalid candidate\n__RC__1" : "__RC__0";
                } else if (command.contains("mv --")) {
                    replacements++;
                    if (failFirstReplacement && replacements == 1) {
                        output = "failed";
                    } else {
                        current = writes.get(writes.size() - 1);
                        output = "__OK__";
                    }
                } else if (command.contains("systemctl restart")) {
                    restarts++;
                    output = failFirstRestart && restarts == 1 ? "restart failed\n__RC__1" : "__RC__0";
                }
                Command cmd = mock(Command.class);
                when(cmd.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
                return cmd;
            });
        }
    }
}
