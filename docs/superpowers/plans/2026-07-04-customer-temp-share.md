# Customer Temp Share Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:test-driven-development while implementing. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build admin-generated temporary customer subscription access links protected by an access password and expiration time.

**Architecture:** Store share links in a dedicated member table with a random token, customer id, BCrypt password hash, expiration time, and status. Add authenticated admin APIs for creating, listing, and revoking links, plus anonymous public APIs that validate token/password and return a read-only proxy node list. The admin UI adds link management to customer detail; the public UI renders a standalone password gate and a read-only version of the existing subscription list.

**Tech Stack:** Spring Boot, MyBatis XML, Vue 3, Element Plus, Vite.

---

## Files

- Create: `sql/2026-07-04-customer-temp-share.sql`
- Create: `skyway-member/src/main/java/com/skyway/member/domain/MbCustomerTempShare.java`
- Create: `skyway-member/src/main/java/com/skyway/member/mapper/MbCustomerTempShareMapper.java`
- Create: `skyway-member/src/main/java/com/skyway/member/service/IMbCustomerTempShareService.java`
- Create: `skyway-member/src/main/java/com/skyway/member/service/impl/MbCustomerTempShareServiceImpl.java`
- Create: `skyway-member/src/main/resources/mapper/member/MbCustomerTempShareMapper.xml`
- Create: `skyway-admin/src/main/java/com/skyway/web/domain/member/CustomerTempShareCreateBody.java`
- Create: `skyway-admin/src/main/java/com/skyway/web/domain/member/CustomerTempShareUnlockBody.java`
- Create: `skyway-admin/src/main/java/com/skyway/web/controller/member/CustomerTempShareController.java`
- Create: `skyway-admin/src/test/java/com/skyway/web/controller/member/CustomerTempShareControllerTest.java`
- Create: `skyway-ui/src/api/member/customerTempShare.js`
- Create: `skyway-ui/src/views/member/customer/components/CustomerTempShareDialog.vue`
- Create: `skyway-ui/src/views/share/customer/index.vue`
- Modify: `skyway-ui/src/views/member/customer/detail.vue`
- Modify: `skyway-ui/src/router/index.js`

## Tasks

- [ ] Write failing backend tests for create, wrong password, expired token, and successful unlock.
- [ ] Implement table DDL, domain, mapper, and service methods.
- [ ] Implement authenticated admin APIs and anonymous public APIs.
- [ ] Add admin customer detail dialog for creating, copying, listing, and revoking links.
- [ ] Add public route and read-only subscription page.
- [ ] Run targeted Maven tests and frontend build checks.
