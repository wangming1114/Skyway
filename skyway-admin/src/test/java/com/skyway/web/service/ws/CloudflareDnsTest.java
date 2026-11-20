package com.skyway.web.service.ws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.system.domain.SysConfig;
import com.skyway.system.service.CloudflareCredentials;
import com.skyway.system.service.ISysConfigService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.StreamUtils;

class CloudflareDnsTest {
    private static final String ZONE = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String RECORD = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private HttpServer server;
    private CloudflareDns client;
    private WsStore.Domain domain;
    private JSONObject record;
    private boolean loseCreateResponse;
    private final AtomicInteger creates = new AtomicInteger();
    private final AtomicInteger deletes = new AtomicInteger();
    @TempDir Path state;

    @BeforeEach
    void start() throws IOException {
        CloudflareCredentials secrets = new CloudflareCredentials(state.toString());
        ISysConfigService config = mock(ISysConfigService.class);
        when(config.selectConfigByKey(CloudflareCredentials.TOKEN))
                .thenReturn(secrets.encrypt("example-test-credential"));
        when(config.selectConfigByKey("skyway.cloudflare.zoneId")).thenReturn(ZONE);
        when(config.selectConfigByKey("skyway.cloudflare.domain")).thenReturn("example.test");
        domain = new WsStore.Domain();
        domain.id = WsStore.uuid();
        domain.zone = ZONE;
        domain.host = "node.example.test";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        client =
                new CloudflareDns(
                        config, secrets, "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange x) throws IOException {
        String path = x.getRequestURI().getPath();
        int status = 200;
        Object result = null;
        switch (x.getRequestMethod()) {
            case "GET":
                if (path.endsWith(RECORD)) {
                    result = record;
                    if (record == null) status = 404;
                } else if (path.endsWith("dns_records"))
                    result = record == null ? new JSONArray() : JSONArray.of(record);
                else if (path.endsWith(ZONE)) result = JSONObject.of("name", "example.test");
                else status = 404;
                break;
            case "POST":
            case "PUT":
                record =
                        JSON.parseObject(
                                StreamUtils.copyToString(
                                        x.getRequestBody(), StandardCharsets.UTF_8));
                record.put("id", RECORD);
                if ("POST".equals(x.getRequestMethod())) {
                    creates.incrementAndGet();
                    if (loseCreateResponse) {
                        loseCreateResponse = false;
                        status = 503;
                    }
                }
                result = record;
                break;
            case "DELETE":
                deletes.incrementAndGet();
                record = null;
                result = JSONObject.of("id", RECORD);
                break;
            default:
                status = 405;
        }
        if (!"Bearer example-test-credential"
                .equals(x.getRequestHeaders().getFirst("Authorization"))) status = 403;
        byte[] body = JSON.toJSONBytes(JSONObject.of("success", status == 200, "result", result));
        x.sendResponseHeaders(status, body.length);
        x.getResponseBody().write(body);
        x.close();
    }

    @Test
    void retryRecoversPostThenUpdatesTheSameRecordAndConfirmsItsDeletion() throws Exception {
        assertEquals(ZONE, client.zone());
        loseCreateResponse = true;
        assertThrows(IOException.class, () -> client.apply(domain, "192.0.2.3", false));
        assertEquals(RECORD, client.apply(domain, "192.0.2.3", false));
        assertEquals(1, creates.get());
        assertFalse(record.getBooleanValue("proxied"));
        domain.record = RECORD;
        assertEquals(RECORD, client.apply(domain, "2001:db8::6", true));
        assertEquals("AAAA", record.getString("type"));
        assertTrue(record.getBooleanValue("proxied"));
        client.remove(domain);
        client.remove(domain);
        assertNull(record);
        assertEquals(1, deletes.get());
    }

    @Test
    void conflictAndCleanupProtectAnUnrelatedRecord() throws IOException {
        record = JSONObject.of("id", RECORD, "name", domain.host, "comment", "manual");
        assertThrows(CloudflareDns.Conflict.class, () -> client.apply(domain, "192.0.2.3", false));
        client.remove(domain);
        domain.record = RECORD;
        assertThrows(CloudflareDns.Conflict.class, () -> client.remove(domain));
        assertEquals(0, deletes.get());
    }

    @Test
    void rotatedTokenIsMaskedAndKeysSurviveProcessRestart() {
        CloudflareCredentials keys = new CloudflareCredentials(state.toString());
        String first = keys.encrypt("first-example-token");
        String next = keys.encrypt("second-example-token");
        assertNotEquals(next, keys.encrypt("second-example-token"));
        assertEquals(
                "second-example-token", new CloudflareCredentials(state.toString()).decrypt(next));
        SysConfig config = new SysConfig();
        config.setConfigKey(CloudflareCredentials.TOKEN);
        config.setConfigValue(first);
        assertEquals(
                CloudflareCredentials.MASK, CloudflareCredentials.mask(config).getConfigValue());
        assertEquals(CloudflareCredentials.MASK, CloudflareCredentials.display("other", next));
    }
}
