# Java to Next.js Web Push Dispatch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dispatch Java `WEB_PUSH` notifications through the existing Next.js route using a shared secret.

**Architecture:** Add an authenticated Next.js-backed `WEB_PUSH` channel and select it with a configuration flag, leaving the current Java-native channel as the default. Reuse the existing persistent `WEB_PUSH` delivery queue, so transport failures are retried independently from notification persistence.

**Tech Stack:** Java 21, Spring Boot configuration properties, `java.net.http.HttpClient`, Jackson, JUnit 5, Mockito.

**Spec:** `docs/superpowers/specs/2026-10-29-java-next-web-push-design.md`

## Global Constraints

- The Next.js route requires `eventId`, Supabase UUID `userId`, `title` (max 100), `body` (max 300), and same-origin `url` (max 2048).
- `PUSH_DISPATCH_SECRET` must be at least 32 characters and must never be logged or exposed to the browser.
- Both `NOTIFICATIONS_WEB_PUSH_ENABLED=true` and `NOTIFICATIONS_WEB_PUSH_DISPATCH_ENABLED=true` select the Next.js-backed channel; without the dispatch flag, the existing Java-native channel remains active.
- Existing delivery retry behavior handles non-2xx responses; notification persistence must not depend on the remote dispatch succeeding.
- HTTP redirects must not be followed because the request carries a bearer secret.

## Review Focus

- Wrong identity: dispatch must use `recipient.supabaseUserId`, not the Java database ID; test the outgoing body.
- Duplicate retries: use the stable notification ID as `eventId`; test exact payload mapping.
- Missing/short secret or invalid endpoint: enabled dispatch must fail configuration clearly; test startup validation.
- Next.js `401`, `503`, or other non-2xx: fail delivery for the existing retry worker; test status handling without leaking the secret.
- Redirect response: reject it without forwarding authorization; test the no-redirect client policy.

---

### Task 1: Route the queued Web Push channel through Next.js

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/service/notification/push/PushDispatchRequest.java`
- Create: `src/main/java/com/devikapps/vaikaparts/service/notification/push/PushDispatchClient.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/notification/WebPushProperties.java`
- Create: `src/main/java/com/devikapps/vaikaparts/service/notification/NextWebPushNotificationChannel.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/notification/push/WebPushConfiguration.java`
- Modify: `src/main/resources/application.yml`
- Modify: `.env.template`
- Modify: `src/test/java/com/devikapps/vaikaparts/service/notification/push/WebPushConfigurationTest.java`
- Create: `src/test/java/com/devikapps/vaikaparts/service/notification/NextWebPushNotificationChannelTest.java`
- Create: `src/test/java/com/devikapps/vaikaparts/service/notification/push/PushDispatchClientTest.java`
- Modify: `readme.md`

**Interfaces:**
- `PushDispatchRequest.from(Notification, NotificationMessageResolver)` returns the JSON payload with `eventId`, `userId`, `title`, `body`, `url`.
- `PushDispatchClient.dispatch(Notification)` posts that payload to the configured Next.js route.
- `NextWebPushNotificationChannel.send(Notification)` delegates to the client and reports `WEB_PUSH`; configuration ensures it is the only active `WEB_PUSH` channel in dispatch mode.

- [x] **Step 1: Write failing tests** for payload mapping (stable notification ID, Supabase UUID, safe path, 300-character body bound), bearer header and JSON request, successful 2xx response, non-2xx/transport failure, redirect rejection, channel delegation, and transport selection/configuration validation.
- [x] **Step 2: Run focused tests and confirm they fail** because the dispatch client and matching configuration do not yet exist.
- [x] **Step 3: Implement** the request mapping, validated dispatch properties, redirect-disabled timed HTTP client, status handling, new dispatch channel, and mutually exclusive transport wiring. Preserve the existing external delivery queue and its retry semantics.
- [x] **Step 4: Document** `NOTIFICATIONS_WEB_PUSH_ENABLED`, `NOTIFICATIONS_WEB_PUSH_DISPATCH_ENABLED`, `PUSH_DISPATCH_URL`, and `PUSH_DISPATCH_SECRET`; state that the shared secret is generated randomly and must match Next.js.
- [x] **Step 5: Run focused tests, then the full Java test suite.** Focused push and delivery tests pass. The full suite reports 970 tests, 49 failed, 3 skipped; the same 49 failures were present in the 958-test baseline before this change.

Run: `./gradlew test --no-daemon`
Expected: BUILD SUCCESSFUL; focused dispatch/configuration tests and the existing notification delivery tests pass.

- [x] **Step 6: Review the diff and commit** only the plan, design, Java implementation/tests, configuration, and README changes. Final independent review found and verified a bearer-token validation fix before publishing.
