# VPS Realtime Speed Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add realtime VPS and proxy-node speed display without changing cumulative traffic collection or calculation.

**Architecture:** Backend exposes a read-only speed endpoint per VPS instance. It SSHes into the VPS, ensures `/root/singbox_speed.sh` exists, samples the script output once, parses per-port speeds, and returns per-port plus total speeds. Frontend polls this endpoint with bounded concurrency and merges realtime speeds into existing cumulative traffic display text.

**Tech Stack:** Spring Boot 2.5, SSHJ, Java 8, Vue 3 Composition API, Element Plus.

---

## Chunk 1: Backend Realtime Speed

### Task 1: Parser and Service

**Files:**
- Create: `skyway-admin/src/test/java/com/skyway/web/service/VpsRealtimeSpeedParserTest.java`
- Modify: `skyway-admin/src/main/java/com/skyway/web/service/VpsSshCommandService.java`
- Modify: `skyway-admin/src/main/java/com/skyway/web/controller/resource/VpsInstanceController.java`

- [x] Write failing parser tests for singbox speed table output.
- [x] Run the parser test and verify it fails because parser API is missing.
- [x] Add DTO classes and parser helpers in `VpsSshCommandService`.
- [x] Add SSH script installation and one-shot sampling method.
- [x] Add `GET /resource/vps/instance/{instanceId}/speed`.
- [x] Run backend test and compile check.

## Chunk 2: Frontend Polling and Display

### Task 2: API and VPS List

**Files:**
- Modify: `skyway-ui/src/api/resource/vps.js`
- Modify: `skyway-ui/src/views/resource/vps/index.vue`

- [x] Add `getInstanceSpeed(instanceId)` API helper.
- [x] Add bounded polling cache for the current VPS page.
- [x] Append realtime upload/download text to the existing cumulative traffic column.
- [x] Add automatic speed unit formatting.

### Task 3: Proxy Node Detail Panel

**Files:**
- Modify: `skyway-ui/src/views/resource/vps/components/ProxyNodePanel.vue`

- [x] Poll current instance speed in the detail panel.
- [x] Match proxy nodes by `row.port`.
- [x] Append realtime speed text to list and dialog traffic displays.
- [x] Clear timers on unmount and refresh after instance changes.

## Chunk 3: Verification

- [x] Run targeted Maven test for parser.
- [x] Run admin module compile or package skip-tests check.
- [x] Run frontend build if dependencies are available.
- [x] Review diff to confirm cumulative traffic logic was not modified.
