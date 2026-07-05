# VPS Proxy Node Edit Port Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let operators edit a proxy node port from VPS detail and apply the new port to the remote sing-box `listen_port`, local node name, share URL, traffic rules, and rate-limit records.

**Architecture:** Keep the backend authoritative for any edit that has remote side effects. The frontend submits the desired port; `ProxyNodeController` validates and coordinates remote config patching through `VpsSshCommandService`, then updates the database only after remote changes succeed.

**Tech Stack:** Java 8, Spring Boot 2.5, MyBatis, Fastjson2, JUnit 5, Vue 3, Element Plus, Node built-in test runner.

---

## Chunk 1: Backend Port Patch And Node Naming

### Task 1: Add sing-box config port patch helper

**Files:**
- Modify: `skyway-admin/src/test/java/com/skyway/web/service/VpsSocks5RelayConfigTest.java`
- Modify: `skyway-admin/src/main/java/com/skyway/web/service/VpsSshCommandService.java`

- [ ] **Step 1: Write failing tests**
  Add tests that call a new static helper, for example `VpsSshCommandService.updateSingBoxListenPortAndInboundName(...)`, and assert:
  - `inbounds[0].listen_port` changes from `10001` to `10088`.
  - inbound tag changes from `VLESS-REALITY-old.json` to `VLESS-REALITY-new.json`.
  - `route.rules[0].inbound` changes to the new tag.
  - SOCKS relay outbound remains unchanged.

- [ ] **Step 2: Run RED**
  Run: `mvn -pl skyway-admin -Dtest=VpsSocks5RelayConfigTest test`
  Expected: compile failure or test failure because the helper does not exist.

- [ ] **Step 3: Implement minimal helper**
  Add a static JSON helper in `VpsSshCommandService` that parses config JSON, updates inbound `listen_port`, optionally updates `tag`, and updates route rule `inbound` values matching the old tag.

- [ ] **Step 4: Run GREEN**
  Run: `mvn -pl skyway-admin -Dtest=VpsSocks5RelayConfigTest test`
  Expected: tests pass.

### Task 2: Add remote update method

**Files:**
- Modify: `skyway-admin/src/main/java/com/skyway/web/service/VpsSshCommandService.java`

- [ ] **Step 1: Implement remote method using tested helper**
  Add `updateProxyNodeConfigPortAndName(Long instanceId, String oldNodeName, String newNodeName, boolean disabled, Integer oldPort, Integer newPort)`.
  It should read the current config file, patch JSON using the helper, write it back, rename to the new filename, and restart sing-box.

- [ ] **Step 2: Keep existing rename behavior**
  Make expiration-only rename continue using existing behavior or the new method with unchanged port. Avoid changing enable/disable semantics.

## Chunk 2: Controller Validation, Persistence, And Port Rules

### Task 3: Add controller tests for edit flow

**Files:**
- Create: `skyway-admin/src/test/java/com/skyway/web/controller/resource/ProxyNodeControllerEditPortTest.java`
- Modify: `skyway-admin/src/main/java/com/skyway/web/controller/resource/ProxyNodeController.java`

- [ ] **Step 1: Write failing tests**
  Use Mockito to instantiate `ProxyNodeController` with mocked services via reflection. Cover:
  - invalid port returns an error.
  - port already used by another node on the same VPS returns an error.
  - successful port edit calls remote config update before database update and saves node name with new port/date.

- [ ] **Step 2: Run RED**
  Run: `mvn -pl skyway-admin -Dtest=ProxyNodeControllerEditPortTest test`
  Expected: failure because controller ignores `port`.

- [ ] **Step 3: Implement controller changes**
  Parse `port`, validate range, check `proxyNodeService.getByInstanceIdAndPort(existing.instanceId, newPort)`, compute new node name, call remote update on port or expiration change, set `row.port`, `row.nodeName`, and `row.url = null` when generated URL should be refreshed.

- [ ] **Step 4: Run GREEN**
  Run: `mvn -pl skyway-admin -Dtest=ProxyNodeControllerEditPortTest test`
  Expected: tests pass.

### Task 4: Migrate rate-limit and traffic rules on port change

**Files:**
- Modify: `skyway-admin/src/main/java/com/skyway/web/controller/resource/ProxyNodeController.java`
- Modify if needed: `skyway-resource/src/main/java/com/skyway/resource/service/IProxyNodeRateLimitService.java`
- Modify if needed: `skyway-resource/src/main/java/com/skyway/resource/service/impl/ProxyNodeRateLimitServiceImpl.java`
- Modify if needed: `skyway-resource/src/main/resources/mapper/resource/ProxyNodeRateLimitMapper.xml`

- [ ] **Step 1: Write failing test**
  Extend `ProxyNodeControllerEditPortTest` to assert that when an active rate limit exists, the controller updates it to the new port and reapplies remote rate limit; otherwise it removes old traffic rules and ensures new traffic rules.

- [ ] **Step 2: Run RED**
  Run: `mvn -pl skyway-admin -Dtest=ProxyNodeControllerEditPortTest test`
  Expected: failure because migration is missing.

- [ ] **Step 3: Implement migration**
  Use existing `getActiveByNodeId`, `saveActive`, `setPortRateLimit`, `removeTrafficRulesForPort`, and `ensureTrafficRulesForPort` where possible. Keep remote failures as returned edit errors before local DB update.

- [ ] **Step 4: Run GREEN**
  Run: `mvn -pl skyway-admin -Dtest=ProxyNodeControllerEditPortTest test`
  Expected: tests pass.

## Chunk 3: Frontend Editor And Contract Tests

### Task 5: Add static contract tests for port editing UI

**Files:**
- Create: `skyway-ui/src/views/resource/vps/proxy-node-edit-port-contract.test.mjs`
- Modify: `skyway-ui/src/views/resource/vps/components/ProxyNodePanel.vue`
- Modify: `skyway-ui/src/views/resource/vps/proxyNode/index.vue`

- [ ] **Step 1: Write failing tests**
  Use Node `node:test` to read the Vue files and assert:
  - edit form has a `label="端口"` or equivalent edit port field.
  - `editNodeForm` includes `port`.
  - `openNodeEdit` copies `row.port`.
  - `submitNodeEdit` sends `port` in `updateProxyNode`.

- [ ] **Step 2: Run RED**
  Run: `node skyway-ui/src/views/resource/vps/proxy-node-edit-port-contract.test.mjs`
  Expected: failure because port edit is missing.

- [ ] **Step 3: Implement frontend changes**
  Add `port` to both duplicated edit forms, initialize it from the row, include it in the payload, and update local row state before refresh.

- [ ] **Step 4: Run GREEN**
  Run: `node skyway-ui/src/views/resource/vps/proxy-node-edit-port-contract.test.mjs`
  Expected: tests pass.

## Chunk 4: Verification

- [ ] Run backend focused tests:
  `mvn -pl skyway-admin -Dtest=VpsSocks5RelayConfigTest,ProxyNodeControllerEditPortTest test`

- [ ] Run frontend contract test:
  `node skyway-ui/src/views/resource/vps/proxy-node-edit-port-contract.test.mjs`

- [ ] Run frontend production build:
  `cd skyway-ui && npm run build:prod`

- [ ] Review final diff:
  `git diff --stat`

- [ ] Do not claim completion until all required verification output has been read.
