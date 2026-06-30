# Customer Node Traffic Rank Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add date-range filtering to the dashboard user traffic rank and rank traffic by customer node instead of by customer.

**Architecture:** The dashboard controller will normalize shortcut or custom date ranges into inclusive `fromTime`/`toTime` bounds. The traffic service and MyBatis mapper will aggregate customer traffic by `customer_id + node_id` and return customer and node display fields. The Vue dashboard will expose day/week/month/year/custom controls and reload the rank table when the range changes.

**Tech Stack:** Spring Boot, JUnit 5, Mockito, MyBatis XML, Vue 3, Element Plus.

---

## Chunk 1: Backend API and Aggregation

### Task 1: Controller Range Normalization

**Files:**
- Modify: `skyway-admin/src/test/java/com/skyway/web/controller/resource/DashboardControllerTest.java`
- Modify: `skyway-admin/src/main/java/com/skyway/web/controller/resource/DashboardController.java`

- [ ] Write failing controller tests for shortcut and custom customer rank ranges.
- [ ] Run `mvn -pl skyway-admin -Dtest=DashboardControllerTest test` and verify the new tests fail.
- [ ] Add `range`, `startTime`, and `endTime` request params to `customerTrafficRank`.
- [ ] Normalize day/week/month/year/custom into inclusive date bounds.
- [ ] Run the controller test and verify it passes.

### Task 2: Service and Mapper Node-Dimension Rank

**Files:**
- Modify: `skyway-resource/src/main/java/com/skyway/resource/service/IProxyNodeTrafficService.java`
- Modify: `skyway-resource/src/main/java/com/skyway/resource/service/impl/ProxyNodeTrafficServiceImpl.java`
- Modify: `skyway-resource/src/main/java/com/skyway/resource/mapper/ProxyNodeTrafficMapper.java`
- Modify: `skyway-resource/src/main/resources/mapper/resource/ProxyNodeTrafficMapper.xml`

- [ ] Change the service contract to accept `fromTime` and `toTime`.
- [ ] Change the mapper method to accept both bounds.
- [ ] Update SQL to group by customer and node, returning `customerId`, `username`, `nodeId`, `nodeName`, `totalRx`, `totalTx`, and `totalTraffic`.
- [ ] Normalize returned map keys in the service implementation.

## Chunk 2: Frontend Dashboard

### Task 3: API Client and Dashboard UI

**Files:**
- Modify: `skyway-ui/src/api/resource/vps.js`
- Modify: `skyway-ui/src/views/index.vue`

- [ ] Update `getDashboardCustomerTrafficRank` to accept a params object.
- [ ] Add rank range state, shortcut buttons, and custom date picker.
- [ ] Load the rank table through the dashboard API using the selected range.
- [ ] Replace the old fixed 30-day customer rank table with node-dimension columns.
- [ ] Run a frontend syntax/build check if available.

## Verification

- [ ] Run `mvn -pl skyway-admin -Dtest=DashboardControllerTest test`.
- [ ] Run a frontend build or lint command if local dependencies are available.
