package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.alibaba.fastjson2.JSON;
import com.skyway.resource.domain.ProxyNode;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.junit.jupiter.api.Test;

public class Hysteria2CreationTest {
    @Test
    void portEditRejectsLiveConflictsBeforeReadingOrReplacingAnyConfiguration() throws Exception {
        VpsSshCommandService service = spy(new VpsSshCommandService());
        SSHClient ssh = mock(SSHClient.class);
        doReturn(ssh).when(service).createSshClient(7L);
        doThrow(new IllegalStateException("occupied unmanaged port")).when(service).assertRemotePortAvailable(ssh, 10000);
        assertThrows(java.io.IOException.class, () -> service.updateProxyNodeConfigPortAndName(
                7L, "node-old", "node-new", false, 21080, 10000));
        verify(ssh, never()).startSession();
    }

    @Test
    void synchronousCreationUsesSbAliasAndPreservesCredentialAndTlsParameters() throws Exception {
        Remote remote = new Remote();
        ProxyNode node = remote.service.addProxyNodeOnInstance(7L, 9L, 32332, null, "Hysteria2");
        assertEquals("Hysteria2", node.getNodeType());
        assertEquals("Hysteria2-203.0.113.8-32332-9-permanent", node.getNodeName());
        assertEquals("test-password", JSON.parseObject(node.getConfigJson()).getString("password"));
        assertEquals("0", node.getStatus());
        assertTrue(remote.commands.stream().anyMatch(command -> command.contains("sb add hy2 32332")));
        assertFalse(remote.commands.stream().anyMatch(command -> command.startsWith("rm -f")
                && command.contains("'/etc/sing-box/conf/Hysteria2-32332.json'")));
        verify(remote.service).assertRemotePortAvailable(any(), eq(32332));
    }

    @Test
    void malformedOutputAndRenameFailureRemoveOnlyAllocatedConfigFiles() throws Exception {
        for (boolean malformed : new boolean[]{true, false}) {
            Remote remote = new Remote();
            remote.malformed = malformed;
            remote.renameFails = !malformed;
            assertThrows(Exception.class, () -> remote.service.addProxyNodeOnInstance(7L, 9L, 32332, null, "Hysteria2"));
            String cleanup = remote.commands.stream().filter(command -> command.startsWith("rm -f")).findFirst().get();
            assertTrue(cleanup.contains("/etc/sing-box/conf/Hysteria2-32332.json"));
            assertFalse(cleanup.contains("*"));
            assertTrue(remote.commands.stream().anyMatch(command -> command.contains("systemctl restart")));
        }
    }

    private static class Remote {
        final VpsSshCommandService service = spy(new VpsSshCommandService());
        final List<String> commands = new CopyOnWriteArrayList<>();
        boolean malformed;
        boolean renameFails;

        Remote() throws Exception {
            SSHClient ssh = mock(SSHClient.class);
            Session session = mock(Session.class);
            doReturn(ssh).when(service).createSshClient(7L);
            doNothing().when(service).assertRemotePortAvailable(ssh, 32332);
            when(ssh.startSession()).thenReturn(session);
            when(session.exec(anyString())).thenAnswer(invocation -> {
                String command = invocation.getArgument(0);
                commands.add(command);
                String output = "";
                if (command.contains("command -v sb")) output = "/usr/bin/sb";
                else if (command.contains("sb add hy2")) output = malformed ? "incomplete output"
                        : "hysteria2://test-password@203.0.113.8:32332?alpn=h3&insecure=1#node\n";
                else if (command.startsWith("mv --")) output = renameFails ? "failed" : "__MOVED__";
                else if (command.startsWith("cat --")) output = "{\"inbounds\":[{\"type\":\"hysteria2\",\"tag\":\"Hysteria2-32332.json\",\"listen_port\":32332}]}";
                else if (command.contains("sing-box check")) output = "__RC__0";
                else if (command.contains("echo __OK__")) output = "__OK__";
                else if (command.contains("systemctl restart")) output = "__RC__0";
                Command cmd = mock(Command.class);
                when(cmd.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
                return cmd;
            });
        }
    }
}
