# VPS Realtime Speed Manager Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace per-list-request SSH sampling with a backend realtime speed manager that keeps long-running SSH collectors and exposes lightweight memory snapshots to the frontend.

**Architecture:** A Spring component periodically reconciles database VPS instances with in-memory collectors. Running VPS instances get one long-running SSH worker that ensures `/root/singbox_speed.sh` exists and continuously parses speed output. Non-running instances are not monitored by speed script and are probed at lower frequency so recovered servers can become running and start collection.

**Tech Stack:** Spring Boot 2.5, Java 8, SSHJ, Vue 3 Composition API, Element Plus.

---

## Chunk 1: Backend Manager

### Task 1: Snapshot Manager

**Files:**
- Create: `skyway-admin/src/main/java/com/skyway/web/service/VpsRealtimeSpeedManager.java`
- Test: `skyway-admin/src/test/java/com/skyway/web/service/VpsRealtimeSpeedManagerTest.java`
- Modify: `skyway-admin/src/main/java/com/skyway/web/service/VpsSshCommandService.java`
- Modify: `skyway-admin/src/main/java/com/skyway/web/controller/resource/VpsInstanceController.java`

- [x] Write failing tests for manager snapshot status and non-running skip behavior.
- [x] Expose SSH helpers needed by background workers.
- [x] Implement manager reconciliation, snapshots, worker lifecycle, and low-frequency status probing.
- [x] Add snapshot endpoints and make old single endpoint read cached snapshot.
- [x] Run backend targeted tests.

## Chunk 2: Frontend Snapshot Polling

### Task 2: Replace Per-Instance SSH Calls

**Files:**
- Modify: `skyway-ui/src/api/resource/vps.js`
- Modify: `skyway-ui/src/views/resource/vps/index.vue`
- Modify: `skyway-ui/src/views/resource/vps/components/ProxyNodePanel.vue`

- [x] Add all-instance speed snapshot API helper.
- [x] Change list polling to one lightweight snapshot request.
- [x] Change detail panel polling to read cached snapshot or single cached instance snapshot.
- [x] Keep two-line list display and automatic units.
- [x] Run frontend build.
