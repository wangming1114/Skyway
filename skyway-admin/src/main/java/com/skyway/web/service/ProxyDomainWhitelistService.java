package com.skyway.web.service;

import java.net.IDN;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.ProxyNodeDomainWhitelist;

/** Resolves built-in domain presets and validates persisted node whitelist snapshots. */
@Service
public class ProxyDomainWhitelistService {
    public static final int PRESET_VERSION = 1;
    public static final int MAX_CUSTOM_DOMAINS = 200;
    public static final int MAX_RESOLVED_DOMAINS = 500;

    private static final Pattern LABEL = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");
    private static final Map<String, Preset> PRESETS;

    static {
        LinkedHashMap<String, Preset> values = new LinkedHashMap<>();
        add(values, "ai", "AI", "ChatGPT、Claude、Gemini、Perplexity 等",
                "openai.com", "chatgpt.com", "oaistatic.com", "oaiusercontent.com", "sora.com",
                "anthropic.com", "claude.ai", "claudeusercontent.com", "perplexity.ai", "pplx.ai",
                "gemini.google.com", "generativelanguage.googleapis.com", "ai.google.dev");
        add(values, "google", "Google", "Google 与 YouTube 常用服务",
                "google.com", "google.com.hk", "googleapis.com", "gstatic.com", "googleusercontent.com",
                "ggpht.com", "youtube.com", "youtu.be", "ytimg.com", "googlevideo.com");
        add(values, "microsoft", "Microsoft", "Microsoft 账号、Office、Outlook 与 Azure",
                "microsoft.com", "microsoftonline.com", "microsoftusercontent.com", "live.com", "office.com",
                "office365.com", "outlook.com", "bing.com", "azure.com", "azureedge.net", "windows.net",
                "msauth.net", "msftauth.net");
        add(values, "social", "社交", "X、Facebook、Instagram、Telegram、Discord、Reddit",
                "x.com", "twitter.com", "t.co", "twimg.com", "facebook.com", "fbcdn.net",
                "instagram.com", "cdninstagram.com", "whatsapp.com", "whatsapp.net", "telegram.org",
                "t.me", "discord.com", "discord.gg", "discordapp.com", "reddit.com", "redd.it");
        add(values, "streaming", "流媒体", "Netflix、Disney+、Hulu、Spotify、TikTok 等",
                "netflix.com", "nflxext.com", "nflximg.com", "nflximg.net", "nflxso.net", "nflxvideo.net",
                "disneyplus.com", "dssott.com", "hulu.com", "huluim.com", "spotify.com", "scdn.co",
                "tiktok.com", "tiktokcdn.com", "tiktokv.com", "byteoversea.com", "primevideo.com",
                "amazonvideo.com", "max.com", "hbomax.com");
        PRESETS = Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Preset> values, String key, String name, String description, String... domains) {
        values.put(key, new Preset(key, name, description, Arrays.asList(domains)));
    }

    public List<Preset> listPresets() {
        return new ArrayList<>(PRESETS.values());
    }

    /** Accepts a request map/JSONObject or a deserialized policy and returns an immutable snapshot. */
    public ProxyNodeDomainWhitelist resolve(Object raw) {
        if (raw == null) return null;
        ProxyNodeDomainWhitelist request;
        try {
            request = raw instanceof ProxyNodeDomainWhitelist
                    ? (ProxyNodeDomainWhitelist) raw
                    : JSON.parseObject(JSON.toJSONString(raw), ProxyNodeDomainWhitelist.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("域名白名单格式错误");
        }
        List<String> keys = request.getPresetKeys() != null ? request.getPresetKeys() : Collections.emptyList();
        List<String> custom = request.getCustomDomains() != null ? request.getCustomDomains() : Collections.emptyList();
        if (keys.isEmpty() && custom.isEmpty()) return null;
        if (custom.size() > MAX_CUSTOM_DOMAINS) {
            throw new IllegalArgumentException("自定义域名最多 " + MAX_CUSTOM_DOMAINS + " 个");
        }

        LinkedHashSet<String> normalizedKeys = new LinkedHashSet<>();
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (String rawKey : keys) {
            String key = rawKey != null ? rawKey.trim().toLowerCase() : "";
            Preset preset = PRESETS.get(key);
            if (preset == null) throw new IllegalArgumentException("未知的白名单分组: " + rawKey);
            normalizedKeys.add(key);
            resolved.addAll(preset.getDomains());
        }
        LinkedHashSet<String> normalizedCustom = new LinkedHashSet<>();
        for (String domain : custom) {
            if (StringUtils.isEmpty(domain) || StringUtils.isEmpty(domain.trim())) continue;
            String normalized = normalizeDomain(domain);
            normalizedCustom.add(normalized);
            resolved.add(normalized);
        }
        if (resolved.isEmpty()) return null;
        if (resolved.size() > MAX_RESOLVED_DOMAINS) {
            throw new IllegalArgumentException("白名单展开后最多 " + MAX_RESOLVED_DOMAINS + " 个域名");
        }
        ProxyNodeDomainWhitelist policy = new ProxyNodeDomainWhitelist();
        policy.setPresetVersion(PRESET_VERSION);
        policy.setPresetKeys(new ArrayList<>(normalizedKeys));
        policy.setCustomDomains(new ArrayList<>(normalizedCustom));
        policy.setDomains(new ArrayList<>(resolved));
        return policy;
    }

    public String serialize(ProxyNodeDomainWhitelist policy) {
        return policy == null ? null : JSON.toJSONString(policy);
    }

    public ProxyNodeDomainWhitelist parseStored(String json) {
        if (StringUtils.isEmpty(json)) return null;
        try {
            ProxyNodeDomainWhitelist policy = JSON.parseObject(json, ProxyNodeDomainWhitelist.class);
            return policy != null && policy.getDomains() != null && !policy.getDomains().isEmpty() ? policy : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public void hydrate(ProxyNode node) {
        if (node != null) node.setDomainWhitelist(parseStored(node.getDomainPolicyJson()));
    }

    public void hydrate(List<ProxyNode> nodes) {
        if (nodes != null) for (ProxyNode node : nodes) hydrate(node);
    }

    public String normalizeDomain(String input) {
        String value = input == null ? "" : input.trim().toLowerCase();
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        if (value.startsWith("*.")) value = value.substring(2);
        if (value.startsWith(".") || value.contains("://") || value.contains("/") || value.contains("@")
                || value.contains(":") || value.contains("*") || value.contains("?") || value.contains("#")) {
            throw new IllegalArgumentException("非法域名: " + input);
        }
        try {
            value = IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES).toLowerCase();
        } catch (Exception e) {
            throw new IllegalArgumentException("非法域名: " + input);
        }
        if (value.length() == 0 || value.length() > 253 || isIpLiteral(value)) {
            throw new IllegalArgumentException("非法域名: " + input);
        }
        String[] labels = value.split("\\.", -1);
        if (labels.length < 2) throw new IllegalArgumentException("域名必须包含有效后缀: " + input);
        for (String label : labels) {
            if (!LABEL.matcher(label).matches()) throw new IllegalArgumentException("非法域名: " + input);
        }
        return value;
    }

    private boolean isIpLiteral(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (!part.matches("\\d{1,3}")) return false;
            int n = Integer.parseInt(part);
            if (n < 0 || n > 255) return false;
        }
        return true;
    }

    public static final class Preset {
        private final String key;
        private final String name;
        private final String description;
        private final List<String> domains;

        private Preset(String key, String name, String description, List<String> domains) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.domains = Collections.unmodifiableList(new ArrayList<>(domains));
        }
        public String getKey() { return key; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getDomains() { return domains; }
    }
}
