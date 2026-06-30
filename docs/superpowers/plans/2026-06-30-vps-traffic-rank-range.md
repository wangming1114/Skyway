# VPS Traffic Rank Range Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add day/week/month/year/custom range filtering to the dashboard VPS traffic rank while preserving the user node traffic rank behavior.

**Architecture:** Add a separate dashboard endpoint for VPS traffic rank using the same range normalization semantics as customer node rank. The traffic service and mapper will aggregate by `instance_id` over inclusive time bounds. The Vue dashboard will maintain separate VPS rank state and controls so user rank state and API calls are untouched.

**Tech Stack:** Spring Boot, JUnit 5, Mockito, MyBatis XML, Vue 3, Element Plus.

---

## Chunk 1: Backend VPS Rank API

### Task 1: Controller Tests and Endpoint

**Files:**
- Modify: `skyway-admin/src/test/java/com/skyway/web/controller/resource/DashboardControllerTest.java`
- Modify: `skyway-admin/src/main/java/com/skyway/web/controller/resource/DashboardController.java`

- [ ] Add failing tests for `vpsTrafficRank` shortcut and custom date ranges.
- [ ] Run `mvn -pl skyway-admin -am -Dtest=DashboardControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` and verify failure.
- [ ] Add `GET /resource/vps/dashboard/vpsTrafficRank`.
- [ ] Reuse range normalization logic without changing `customerTrafficRank`.

### Task 2: Service and Mapper Aggregation

**Files:**
- Modify: `skyway-resource/src/main/java/com/skyway/resource/service/IProxyNodeTrafficService.java`
- Modify: `skyway-resource/src/main/java/com/skyway/resource/service/impl/ProxyNodeTrafficServiceImpl.java`
- Modify: `skyway-resource/src/main/java/com/skyway/resource/mapper/ProxyNodeTrafficMapper.java`
- Modify: `skyway-resource/src/main/resources/mapper/resource/ProxyNodeTrafficMapper.xml`

- [ ] Add `getVpsTrafficRank(Date fromTime, Date toTime)`.
- [ ] Add mapper query grouped by `p.instance_id`.
- [ ] Return `instanceId`, `instanceName`, `instanceIp`, `nodeCount`, `totalRx`, `totalTx`, and `totalTraffic`.

## Chunk 2: Frontend VPS Rank Controls

### Task 3: API and Dashboard UI

**Files:**
- Modify: `skyway-ui/src/api/resource/vps.js`
- Modify: `skyway-ui/src/views/index.vue`

- [ ] Add `getDashboardVpsTrafficRank(params)`.
- [ ] Add VPS rank range/date/loading/list state separate from customer rank state.
- [ ] Add day/week/month/year/custom controls to the VPS rank card.
- [ ] Show selected-range traffic plus existing realtime speed column.
- [ ] Verify user node rank code still calls `getDashboardCustomerTrafficRank`.

## Verification

- [ ] Run backend target tests.
- [ ] Run frontend production build.
- [ ] Run `git diff --check`.
