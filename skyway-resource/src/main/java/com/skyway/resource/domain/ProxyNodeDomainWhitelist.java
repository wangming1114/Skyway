package com.skyway.resource.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Persisted snapshot of a proxy node domain whitelist. */
public class ProxyNodeDomainWhitelist implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer presetVersion;
    private List<String> presetKeys = new ArrayList<>();
    private List<String> customDomains = new ArrayList<>();
    private List<String> domains = new ArrayList<>();

    public Integer getPresetVersion() { return presetVersion; }
    public void setPresetVersion(Integer presetVersion) { this.presetVersion = presetVersion; }
    public List<String> getPresetKeys() { return presetKeys; }
    public void setPresetKeys(List<String> presetKeys) { this.presetKeys = presetKeys; }
    public List<String> getCustomDomains() { return customDomains; }
    public void setCustomDomains(List<String> customDomains) { this.customDomains = customDomains; }
    public List<String> getDomains() { return domains; }
    public void setDomains(List<String> domains) { this.domains = domains; }
}
