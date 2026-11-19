# Demand Update and Seller Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the owning Researcher or any Manager update demand fields and append photos, then notify affected Sellers of changes to published demands through every enabled channel except SMS.

**Architecture:** Add a multipart `PATCH /v1/demands/{demandId}` handled by `DemandController` and `DemandService`. Enforce role and ownership in the transactional service, preserve omitted fields/photos, and enqueue a separate `DemandUpdatedRequested` event in the transactional outbox. Its consumer uses `NotificationService` with a new `DEMAND_UPDATED` type and stable per-recipient request IDs.

**Tech Stack:** Java 21, Spring Boot, Spring MVC multipart binding, Spring Data JPA, PostgreSQL/Flyway, RabbitMQ outbox, JUnit 5, Mockito, Testcontainers.

**Spec:** Requirements approved in the conversation; no separate design document.

## Global Constraints

- Only authenticated users with `RESEARCHER` or `MANAGER` user type may update a demand.
- A Researcher may update only a demand linked to that Researcher; a Manager may update any existing demand.
- The request cannot change the demand ID, owner, lifecycle status, or timestamps directly.
- Omitted fields retain their stored values; uploaded photos append to existing photos; the total is at most five.
- Only a real change to a `PUBLISHED` demand enqueues seller notifications.
- Notifications use in-app, email, and Web Push according to existing channel settings and preferences; SMS is always excluded for `DEMAND_UPDATED`.
- The update and outbox event must commit or roll back together; retrying an event must not create duplicate notifications.
- Do not modify or stage pre-existing untracked files in this repository.

## Review Focus

- A non-owner Researcher must not infer another Researcher’s demand exists; return the existing not-found behavior.
- A Manager can update a demand without being its owner; Sellers and other roles are denied.
- Multipart fields omitted by the caller must not erase stored values or photos.
- Invalid/blank supplied values and a photo total above five must fail before persistence.
- A `DEMAND_UPDATED` notification must never enter the SMS channel, including when SMS preference is enabled; failed/retried delivery must not duplicate notification records.

---

### Task 1: Securely update demand fields and append photos

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/exchange/UpdateDemandRequest.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/exchange/DemandController.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/DemandService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/repository/DemandRepository.java` only if a lock-aware update query is needed.
- Modify: `doc/api.yaml`
- Test: `src/test/java/com/devikapps/vaikaparts/service/DemandServiceTest.java`
- Test: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/DemandControllerIT.java`

**Interfaces:**
- Produces: `PATCH /v1/demands/{demandId}` consuming `multipart/form-data`, returning the updated `Demand`.
- Request properties are optional: `description`, `part.name`, `part.carBrand`, `part.carModel`, `part.carYear`, `part.partCategory`, and `part.images`.
- The service reads the authenticated `JUser` through `UserService`; it compares `userType` and authenticated user ID with `JDemand.researcher.id` before applying changes.

- [ ] **Step 1: Add failing service tests** for owner Researcher partial update/photo append, Manager update of a non-owned demand, non-owner Researcher not-found, disallowed role, blank supplied values, and total photos above five.
- [ ] **Step 2: Run the focused tests and confirm they fail** because the update operation/authorization is absent.
- [ ] **Step 3: Implement the optional multipart request model, service authorization, validation, photo upload/append and controller route.** Preserve all omitted values and keep status/owner/timestamps outside the request mapping. Reject empty/no-op requests and photo totals above five before uploading.
- [ ] **Step 4: Add controller integration coverage** for multipart binding, response content, unauthenticated access and forbidden roles; document the route in OpenAPI.
- [ ] **Step 5: Run focused Domain service/controller tests and commit** as `feat(domain): allow owners and managers to update demands`.

### Task 2: Notify Sellers about published demand changes, excluding SMS

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/event/model/DemandUpdatedRequested.java`
- Create: `src/main/java/com/devikapps/vaikaparts/service/event/DemandUpdatedRequestedService.java`
- Create: `src/main/resources/db/migration/V1_0_<next>__Add_demand_updated_notification_type.sql`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/DemandService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/repository/OfferRepository.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/model/classifier/NotificationType.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/notification/NotificationService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/notification/NotificationMessageResolver.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/notification/EmailNotificationChannel.java`
- Modify: `doc/api.yaml`
- Test: `src/test/java/com/devikapps/vaikaparts/service/DemandServiceTest.java`
- Test: `src/test/java/com/devikapps/vaikaparts/service/event/DemandUpdatedRequestedServiceTest.java`
- Test: `src/test/java/com/devikapps/vaikaparts/service/NotificationServiceTest.java`
- Test: `src/test/java/com/devikapps/vaikaparts/service/NotificationMessageResolverTest.java`

**Interfaces:**
- Consumes: successful update result from Task 1.
- Produces: outbox event `DemandUpdatedRequested(id, demandId)`; its consumer bean name follows the `EventDispatcher` convention: `demandUpdatedRequestedService`.
- Recipient set: active Sellers matching the demand’s current part category, union Sellers with an existing offer for the demand, deduplicated by Seller ID.
- Notification request: `NotificationType.DEMAND_UPDATED`, resource = demand ID, click action = `VIEW_DEMAND`; request ID is stable for the event/Seller pair.

- [ ] **Step 1: Add failing tests** for event enqueue only on real content changes to a published demand, category-matched and existing-offer Seller recipient selection, duplicate-recipient elimination, localized notification messages/titles/email subjects, persistent external delivery, and SMS exclusion even with SMS preference enabled.
- [ ] **Step 2: Run those tests and confirm the expected failures** for the missing event/type and SMS eligibility rule.
- [ ] **Step 3: Add the Flyway enum migration and `DEMAND_UPDATED` notification type.** Update all exhaustive notification-type switches, localization, email subject, OpenAPI enum, recipient validation, resource resolution, and persistent delivery selection.
- [ ] **Step 4: Enqueue the event in the same transaction as a changed published demand; implement its consumer.** Reuse the existing category filter, query offer Seller IDs, send through `NotificationService`, and rely on stable request IDs for retry idempotency. Explicitly exclude SMS for this notification type.
- [ ] **Step 5: Run focused notification/outbox tests and commit** as `feat(notifications): notify sellers when demands change`.

### Task 3: Verify combined behavior and prepare the PR

**Files:** no additional files unless a failing integration test exposes a required fix.

- [ ] **Step 1: Run formatting and the full backend test suite**; ensure Flyway applies the new enum migration and multipart behavior passes in integration tests.
- [ ] **Step 2: Review the complete diff** for role bypass, field clearing, photo-count bypass, SMS dispatch, duplicate notifications, and accidental unrelated files.
- [ ] **Step 3: Confirm only this plan and feature changes are committed**, then push the feature branch and create a PR targeting `preproduction`.

## Execution Notes

- Use the existing checkout and create a dedicated branch; do not create a worktree.
- Create one commit per implementation task as specified above. The plan document may be included in Task 1’s commit.
- Keep all pre-existing untracked files untouched and out of commits.
