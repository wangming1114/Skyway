package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IVpsInstanceService;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class VpsPortEditLiveIntegrationTest {
    private static final String OLD_NAME = "SKYWAY-LIVE-PORT-39084-old";
    private static final String NEW_NAME = "SKYWAY-LIVE-PORT-39085-new";
    private static final String CONF_DIR = "/etc/sing-box/conf";

    @Test
    public void disabledTestNodePortAndNameAreUpdatedAndRestoredWithoutRestartingService() throws Exception {
        String host = System.getenv("SKYWAY_LIVE_HOST");
        String password = System.getenv("SKYWAY_LIVE_PASS");
        Assumptions.assumeTrue(host != null && password != null, "live VPS credentials not provided");

        VpsInstance instance = new VpsInstance();
        instance.setId(990001L);
        instance.setIp(host);
        instance.setSshPort(22);
        instance.setSshUsername("root");
        instance.setSshPassword(password);
        IVpsInstanceService instances = mock(IVpsInstanceService.class);
        when(instances.getById(990001L)).thenReturn(instance);
        VpsSshCommandService service = new VpsSshCommandService();
        ReflectionTestUtils.setField(service, "vpsInstanceService", instances);

        String oldPath = CONF_DIR + "/" + OLD_NAME + ".json.disabled";
        String newPath = CONF_DIR + "/" + NEW_NAME + ".json.disabled";
        String original = "{\"inbounds\":[{\"type\":\"socks\",\"tag\":\"" + OLD_NAME
                + ".json\",\"listen\":\"127.0.0.1\",\"listen_port\":39084}],"
                + "\"outbounds\":[{\"type\":\"direct\",\"tag\":\"direct\"}]}";

        try (SSHClient ssh = connect(host, password)) {
            String existingHashes = run(ssh, "find " + CONF_DIR
                    + " -maxdepth 1 -type f ! -name 'SKYWAY-LIVE-PORT-*' -exec sha256sum {} \\; | sort");
            String mainPid = run(ssh, "systemctl show sing-box -p MainPID -p ExecMainStartTimestamp");
            run(ssh, "test ! -e " + oldPath + " -a ! -e " + newPath);
            write(ssh, oldPath, original);
            try {
                service.updateProxyNodeConfigPortAndName(
                        990001L, OLD_NAME, NEW_NAME, true, 39084, 39085);
                assertEquals("missing", run(ssh, "test -e " + oldPath + " && echo exists || echo missing").trim());
                JSONObject updated = JSON.parseObject(run(ssh, "cat " + newPath));
                assertEquals(39085, updated.getJSONArray("inbounds").getJSONObject(0).getIntValue("listen_port"));
                assertEquals(NEW_NAME + ".json",
                        updated.getJSONArray("inbounds").getJSONObject(0).getString("tag"));

                service.updateProxyNodeConfigPortAndName(
                        990001L, NEW_NAME, OLD_NAME, true, 39085, 39084);
                assertFalse(run(ssh, "test -e " + newPath + " && echo exists || echo missing").contains("exists"));
                assertEquals(JSON.parseObject(original), JSON.parseObject(run(ssh, "cat " + oldPath)));
            } finally {
                run(ssh, "rm -f -- " + oldPath + " " + newPath + " " + oldPath
                        + ".skyway-port-* " + newPath + ".skyway-port-*");
            }
            assertEquals(mainPid, run(ssh, "systemctl show sing-box -p MainPID -p ExecMainStartTimestamp"));
            assertEquals(existingHashes, run(ssh, "find " + CONF_DIR
                    + " -maxdepth 1 -type f ! -name 'SKYWAY-LIVE-PORT-*' -exec sha256sum {} \\; | sort"));
            assertTrue(run(ssh, "systemctl is-active sing-box").contains("active"));
        }
    }

    private static SSHClient connect(String host, String password) throws Exception {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect(host, 22);
        ssh.authPassword("root", password);
        return ssh;
    }

    private static void write(SSHClient ssh, String path, String content) throws Exception {
        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        run(ssh, "printf '%s' '" + encoded + "' | base64 -d > " + path);
    }

    private static String run(SSHClient ssh, String command) throws Exception {
        try (Session session = ssh.startSession(); Command cmd = session.exec(command)) {
            InputStream input = cmd.getInputStream();
            byte[] buffer = new byte[4096];
            StringBuilder output = new StringBuilder();
            int count;
            while ((count = input.read(buffer)) > 0) {
                output.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
            }
            cmd.join(30, TimeUnit.SECONDS);
            if (cmd.getExitStatus() != null && cmd.getExitStatus() != 0) {
                throw new IllegalStateException("remote command failed: " + command + " (" + cmd.getExitStatus() + ")");
            }
            return output.toString();
        }
    }
}
