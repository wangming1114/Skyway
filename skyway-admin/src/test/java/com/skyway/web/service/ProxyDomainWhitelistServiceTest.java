package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.skyway.resource.domain.ProxyNodeDomainWhitelist;

public class ProxyDomainWhitelistServiceTest {
    private final ProxyDomainWhitelistService service = new ProxyDomainWhitelistService();

    @Test
    public void exposesFiveVersionedBuiltInPresets() {
        List<ProxyDomainWhitelistService.Preset> presets = service.listPresets();

        assertEquals(5, presets.size());
        assertEquals(Arrays.asList("ai", "google", "microsoft", "social", "streaming"),
                Arrays.asList(presets.get(0).getKey(), presets.get(1).getKey(), presets.get(2).getKey(),
                        presets.get(3).getKey(), presets.get(4).getKey()));
        assertTrue(presets.get(0).getDomains().contains("openai.com"));
        assertTrue(presets.get(4).getDomains().contains("netflix.com"));
    }

    @Test
    public void resolvesSnapshotAndNormalizesAndDeduplicatesCustomDomains() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("presetKeys", Collections.singletonList("AI"));
        request.put("customDomains", Arrays.asList("*.Example.COM.", "example.com", "例子.测试"));

        ProxyNodeDomainWhitelist policy = service.resolve(request);

        assertEquals(ProxyDomainWhitelistService.PRESET_VERSION, policy.getPresetVersion());
        assertEquals(Collections.singletonList("ai"), policy.getPresetKeys());
        assertEquals(Arrays.asList("example.com", "xn--fsqu00a.xn--0zwm56d"), policy.getCustomDomains());
        assertEquals(1, policy.getDomains().stream().filter("example.com"::equals).count());
        assertTrue(policy.getDomains().contains("openai.com"));
    }

    @Test
    public void emptyPolicyMeansUnrestricted() {
        assertNull(service.resolve(null));
        assertNull(service.resolve(Collections.emptyMap()));
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("presetKeys", Collections.emptyList());
        empty.put("customDomains", Arrays.asList("", "  "));
        assertNull(service.resolve(empty));
    }

    @Test
    public void rejectsUnknownPresetUrlsPortsPathsWildcardsAndIpAddresses() {
        assertInvalid(request(Collections.singletonList("missing"), Collections.emptyList()));
        for (String value : Arrays.asList("https://example.com", "example.com:443", "example.com/path",
                "foo.*.example.com", "192.0.2.1", "localhost")) {
            assertInvalid(request(Collections.emptyList(), Collections.singletonList(value)));
        }
    }

    @Test
    public void enforcesCustomDomainLimit() {
        List<String> domains = new ArrayList<>();
        for (int i = 0; i <= ProxyDomainWhitelistService.MAX_CUSTOM_DOMAINS; i++) {
            domains.add("d" + i + ".example.com");
        }
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.resolve(request(Collections.emptyList(), domains)));
        assertTrue(error.getMessage().contains("最多"));
    }

    @Test
    public void storedJsonKeepsAppliedSnapshotInsteadOfExpandingPresetAgain() {
        ProxyNodeDomainWhitelist policy = service.resolve(
                request(Collections.singletonList("ai"), Collections.singletonList("snapshot.example")));

        ProxyNodeDomainWhitelist restored = service.parseStored(service.serialize(policy));

        assertEquals(policy.getDomains(), restored.getDomains());
        assertEquals(policy.getPresetKeys(), restored.getPresetKeys());
        assertEquals(policy.getPresetVersion(), restored.getPresetVersion());
    }

    @Test
    public void supportsBlacklistAndTreatsLegacyJsonAsWhitelist() {
        Map<String, Object> blacklist = request(Collections.emptyList(), Collections.singletonList("example.com"));
        blacklist.put("mode", "blacklist");
        ProxyNodeDomainWhitelist resolved = service.resolve(blacklist);
        assertEquals("blacklist", resolved.getMode());

        ProxyNodeDomainWhitelist legacy = service.parseStored(
                "{\"presetVersion\":1,\"presetKeys\":[],\"customDomains\":[\"old.example\"],\"domains\":[\"old.example\"]}");
        assertEquals("whitelist", legacy.getMode());
    }

    @Test
    public void rejectsConflictingNewAndLegacyRequestFields() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("domainPolicy", request(Collections.emptyList(), Collections.singletonList("one.example")));
        body.put("domainWhitelist", request(Collections.emptyList(), Collections.singletonList("two.example")));
        assertThrows(IllegalArgumentException.class, () -> service.resolveRequest(body));
    }

    private void assertInvalid(Map<String, Object> request) {
        assertThrows(IllegalArgumentException.class, () -> service.resolve(request));
    }

    private Map<String, Object> request(List<String> presetKeys, List<String> customDomains) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("presetKeys", presetKeys);
        request.put("customDomains", customDomains);
        return request;
    }
}
