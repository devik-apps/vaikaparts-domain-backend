# Contact Unlock and Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a researcher pay once to reveal a seller's contact for one offer, while notifying the seller of the researcher's contact after confirmed payment.

**Architecture:** The Domain Backend stores a `PENDING` unlock before invoking Pecunia, then calls Pecunia with only IDs and the configured price. Pecunia publishes `ContactReleaseRequested`; a dedicated synchronous RabbitMQ listener validates the event, commits the unlock and seller notification atomically, and then acknowledges it. The buyer reads current seller contact data through an authenticated endpoint guarded by `RELEASED`.

**Tech Stack:** Java 21, Spring Boot, Spring Security/JWT, Spring Data JPA, Flyway/PostgreSQL, Spring AMQP, JUnit 5, Mockito, Testcontainers.

**Spec:** Cross-service contract in `/home/kyle/projects/devikapps/vaikaparts-pecunia/docs/superpowers/specs/2026-09-30-vanilla-pay-contact-release-design.md` plus the approved Domain Backend requirements in this conversation.

## Global Constraints

- Version 1 accepts only `provider=VANILLA_PAY`, currency `MGA`, and Vanilla Pay `mobile_money`; the frontend supplies only the provider.
- Create `contact_unlocks` before the Pecunia HTTP request. `unlockRequestId` links both services to the offer.
- Keep contact details in `users`; never copy them into `contact_unlocks` or the RabbitMQ event.
- Permit one active unlock per `(buyer_id, offer_id)` while status is `PENDING` or `RELEASED`; a new ID is allowed after `FAILED` or `EXPIRED`.
- Mark `EXPIRED` only after an authoritative terminal expiry/cancellation confirmation; a timer alone cannot prove a Vanilla Pay link will never be paid. Review any late `SUCCESS` before allowing a replacement attempt.
- Pecunia retries with the same `unlockRequestId` must return the original payment and URL. The Domain Backend must recover with `GET /v1/payments/by-unlock-request/{unlockRequestId}` after ambiguous HTTP failures.
- A `SUCCESS` event must commit the unlock and durable seller notification before RabbitMQ ACK. Repeated events must have no new effect.
- The current `EventConsumer` uses `ExecutorService` and catches handler errors, so it must not consume contact-release messages.

## Shared Contract

| Element | Exact value |
| --- | --- |
| Exchange | `vaikaparts.events` (durable direct; configure both services identically) |
| Routing key | `payment.contact-release.requested` |
| Domain queue | `domain.contact-release.requested` (durable, dedicated) |
| Dead-letter queue | `domain.contact-release.requested.dlq` |
| JSON type | `ContactReleaseRequested` |
| Provider | `VANILLA_PAY` |
| Timestamp | UTC ISO-8601 instant, e.g. `2026-09-30T10:15:30Z` |

The exact fixture `src/test/resources/contracts/contact-release-requested-v1.json` must match the file of the same path in Pecunia:

```json
{"@type":"ContactReleaseRequested","id":"event-id","payment_id":"payment-id","unlock_request_id":"unlock-id","buyer_id":"buyer-id","seller_id":"seller-id","provider":"VANILLA_PAY","paid_at":"2026-09-30T10:15:30Z","attempt_nb":0}
```

## Review Focus

- An HTTP timeout after Pecunia creates a payment must recover the existing URL without a second provider initiation; Task 3 tests this.
- Two unlock attempts for the same buyer/offer must not both remain active; Task 1 tests the partial unique index.
- A forged event with different buyer, seller, payment, or provider must not release contacts; Task 4 tests each mismatch.
- A redelivered event or concurrent listener must persist only one notification; Task 4 tests row locking and idempotency.
- A temporary database failure must propagate from the listener and lead to retry/DLQ, not ACK; Task 4 tests this.

---

### Task 1: Add the contact unlock model and constraints

**Files:**
- Create: `src/main/resources/db/migration/V1_0_35__Create_contact_unlocks.sql`
- Create: `src/main/java/com/devikapps/vaikaparts/model/classifier/ContactUnlockStatus.java`
- Create: `src/main/java/com/devikapps/vaikaparts/repository/model/exchange/JContactUnlock.java`
- Create: `src/main/java/com/devikapps/vaikaparts/repository/ContactUnlockRepository.java`
- Test: `src/test/java/com/devikapps/vaikaparts/repository/ContactUnlockRepositoryIT.java`

**Interfaces:** `ContactUnlockRepository.findByUnlockRequestIdForUpdate(String)` uses `PESSIMISTIC_WRITE`; `findByBuyerIdAndOfferIdAndStatusIn(...)` locates active attempts.

The migration creates the exact V1 shape:

```sql
CREATE TYPE contact_unlock_status AS ENUM ('PENDING', 'RELEASED', 'FAILED', 'EXPIRED');
CREATE TABLE contact_unlocks (
    unlock_request_id VARCHAR(255) PRIMARY KEY,
    offer_id VARCHAR(255) NOT NULL REFERENCES offers(id),
    buyer_id VARCHAR(255) NOT NULL REFERENCES researchers(id),
    seller_id VARCHAR(255) NOT NULL REFERENCES sellers(id),
    payment_id VARCHAR(255) UNIQUE,
    release_event_id VARCHAR(255) UNIQUE,
    provider VARCHAR(50) NOT NULL,
    status contact_unlock_status NOT NULL,
    payment_url TEXT,
    paid_at TIMESTAMP WITH TIME ZONE,
    released_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX uq_active_contact_unlock ON contact_unlocks (buyer_id, offer_id)
WHERE status IN ('PENDING', 'RELEASED');
```

- [ ] **Step 1: Write failing database tests** for all fields, foreign keys, unique `payment_id`/`release_event_id`, and the partial unique index on `(buyer_id, offer_id) WHERE status IN ('PENDING','RELEASED')`. Verify a new attempt becomes possible after `FAILED` or `EXPIRED`.
- [ ] **Step 2: Run RED:** `./gradlew test --tests '*ContactUnlockRepositoryIT' --no-daemon`.
- [ ] **Step 3: Implement the migration and JPA mapping.** Use the provided `contact_unlock_status` values and `contact_unlocks` columns, `TIMESTAMP WITH TIME ZONE` mapped to `OffsetDateTime`, and `VARCHAR(255)` foreign keys to `offers`, `researchers`, and `sellers`.
- [ ] **Step 4: Run GREEN:** `./gradlew test --tests '*ContactUnlockRepositoryIT' --no-daemon`.
- [ ] **Step 5: Commit:** `git add src/main/resources/db/migration/V1_0_35__Create_contact_unlocks.sql src/main/java/com/devikapps/vaikaparts/model/classifier/ContactUnlockStatus.java src/main/java/com/devikapps/vaikaparts/repository/model/exchange/JContactUnlock.java src/main/java/com/devikapps/vaikaparts/repository/ContactUnlockRepository.java src/test/java/com/devikapps/vaikaparts/repository/ContactUnlockRepositoryIT.java && git commit -m "feat: persist contact unlock attempts"`.

### Task 2: Freeze the cross-service event contract and dedicated queue

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/event/model/ContactReleaseRequested.java`
- Create: `src/main/java/com/devikapps/vaikaparts/config/ContactReleaseRabbitConf.java`
- Create: `src/test/resources/contracts/contact-release-requested-v1.json`
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/com/devikapps/vaikaparts/event/ContactReleaseContractTest.java`

**Interfaces:** `ContactReleaseRequested extends InfraEvent` exposes `id`, `paymentId`, `unlockRequestId`, `buyerId`, `sellerId`, `provider`, `paidAt: Instant`, and inherited `attemptNb`. The dedicated queue uses a listener created in Task 4.

- [ ] **Step 1: Write failing contract tests**: deserialize the exact fixture, assert every field and type, serialize back to the same JSON structure, and assert the dedicated queue binding/routing. Compare fixture bytes against Pecunia's fixture in CI or a cross-repository contract check.
- [ ] **Step 2: Run RED:** `./gradlew test --tests '*ContactReleaseContractTest' --no-daemon`.
- [ ] **Step 3: Define the DTO and durable exchange, queue, binding, DLX, and DLQ.** Use the shared values above and a named listener container factory with automatic ACK after successful method return, bounded retries, and dead-letter routing. Task 4 attaches the listener. Reject malformed or unknown event types to the DLQ. Keep the existing asynchronous `EventConsumer` unchanged and bound only to its own queue.
- [ ] **Step 4: Run GREEN:** `./gradlew test --tests '*ContactReleaseContractTest' --no-daemon`.
- [ ] **Step 5: Commit** the files in this task with `git commit -m "feat: define contact release event contract"`.

### Task 3: Initiate and recover payments through Pecunia

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/config/PecuniaConf.java`
- Create: `src/main/java/com/devikapps/vaikaparts/client/PecuniaClient.java`
- Create: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/ContactUnlockRequest.java`
- Create: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/ContactUnlockResponse.java`
- Create: `src/main/java/com/devikapps/vaikaparts/service/ContactUnlockService.java`
- Create: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/exchange/ContactUnlockController.java`
- Modify: `src/main/resources/application.yml`
- Modify: `doc/api.yaml`
- Test: `src/test/java/com/devikapps/vaikaparts/service/ContactUnlockServiceTest.java`
- Test: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/ContactUnlockControllerIT.java`

**Interfaces:** `POST /v1/offers/{offerId}/contact-unlocks` accepts `{"provider":"VANILLA_PAY"}` and returns `unlock_request_id`, `status`, `payment_url`; `PecuniaClient.initiate(unlockRequestId, buyerId, sellerId, amount, description)` sends `currency=MGA` and `type=PROFILE_UNLOCK`, and `findByUnlockRequestId(String)` recovers payment state. Both use `X-Api-Key` server-side.

- [ ] **Step 1: Write failing tests**: the Supabase subject from `SecContextUtil.getCurrentUserId()` resolves via `UserRepository.findBySupabaseUserId(...)` to a `JResearcher`; the offer must be `PUBLISHED` and its demand must belong to that researcher; seller ID comes from `OfferRepository.findByIdWithRelations`; price comes from `contact-unlock.price-mga`; a `PENDING` row is committed before the HTTP call. Test an existing active attempt returns its payment URL, an HTTP timeout triggers the Pecunia lookup, and a mismatched Pecunia response is rejected. Test `FAILED`/`EXPIRED` permits a fresh request ID. Test an event arriving before the Pecunia HTTP response: the later response must populate the URL without changing `RELEASED` back to `PENDING`.
- [ ] **Step 2: Run RED:** `./gradlew test --tests '*ContactUnlockServiceTest' --tests '*ContactUnlockControllerIT' --no-daemon`.
- [ ] **Step 3: Implement initiation in two transaction boundaries.** Transaction A creates/commits the unlock; then call Pecunia. Transaction B stores `paymentId` and `paymentUrl` only if the returned IDs and status match. On ambiguous failure, call `GET /v1/payments/by-unlock-request/{unlockRequestId}`; leave the row `PENDING` for later recovery if Pecunia cannot be reached. Never hold a DB transaction open across the HTTP call. Enforce price `BigDecimal > 0`, timeout configuration, and secure API-key injection.
- [ ] **Step 4: Run GREEN:** `./gradlew test --tests '*ContactUnlockServiceTest' --tests '*ContactUnlockControllerIT' --no-daemon`.
- [ ] **Step 5: Commit** the files in this task with `git commit -m "feat: initiate contact unlock payments"`.

### Task 4: Process events synchronously and atomically

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/service/ContactReleaseService.java`
- Create: `src/main/java/com/devikapps/vaikaparts/event/consumer/ContactReleaseListener.java`
- Create: `src/main/resources/db/migration/V1_0_36__Add_contact_unlock_notification_type.sql`
- Modify: `src/main/java/com/devikapps/vaikaparts/repository/ContactUnlockRepository.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/model/classifier/NotificationType.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/notification/NotificationService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/notification/NotificationMessageResolver.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/notification/InAppNotificationChannel.java`
- Test: `src/test/java/com/devikapps/vaikaparts/service/ContactReleaseServiceIT.java`
- Test: `src/test/java/com/devikapps/vaikaparts/event/ContactReleaseListenerIT.java`

**Interfaces:** `@Transactional void ContactReleaseService.release(ContactReleaseRequested event)`; the listener returns only after this method commits.

- [ ] **Step 1: Write failing tests**: lock the row; verify event IDs, buyer, seller, and provider; save payment ID, event ID, paid time, release time, and `RELEASED`; duplicate or concurrent events produce one durable seller notification containing the researcher's current name/phone/email. Verify a notification persistence error rolls back the unlock. Assert a transient persistence error propagates to the listener and triggers Rabbit retry, with final failure in DLQ.
- [ ] **Step 2: Run RED:** `./gradlew test --tests '*ContactReleaseServiceIT' --tests '*ContactReleaseListenerIT' --no-daemon`.
- [ ] **Step 3: Implement the locked transition and synchronous listener.** Add `CONTACT_UNLOCKED` to the Java and PostgreSQL notification enums; extend existing notification routing for seller/offer. Call `NotificationService` inside the locked transaction; its in-app row must persist before `RELEASED` commits. The locked status transition suppresses duplicate notifications. Change `InAppNotificationChannel` so WebSocket delivery occurs after commit; external SMS/email already follow that pattern. Treat missing request, mismatched IDs/provider, or conflicting prior payment/event as permanent invalid messages routed to DLQ. Retry transient database failures.
- [ ] **Step 4: Run GREEN:** `./gradlew test --tests '*ContactReleaseServiceIT' --tests '*ContactReleaseListenerIT' --no-daemon`.
- [ ] **Step 5: Commit** the files in this task with `git commit -m "feat: release contacts after confirmed payment"`.

### Task 5: Authorize buyer access to current seller contact

**Files:**
- Modify: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/exchange/ContactUnlockController.java`
- Modify: `doc/api.yaml`
- Test: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/ContactUnlockControllerIT.java`

**Interfaces:** `GET /v1/offers/{offerId}/contact-unlocks/me` returns the current seller name, phone, and email only to the buyer when the matching unlock is `RELEASED`.

- [ ] **Step 1: Write failing tests**: before release or for another buyer, contact read returns `403/404` without contact data; after release, it returns current values read from `users`, including changes made after payment. Verify the existing public offer response still exposes only `seller_id` and masked name.
- [ ] **Step 2: Run RED:** `./gradlew test --tests '*ContactUnlockControllerIT' --no-daemon`.
- [ ] **Step 3: Implement the JWT-gated read endpoint.** Resolve the Supabase subject from `SecContextUtil.getCurrentUserId()` via `UserRepository.findBySupabaseUserId(...)` to the researcher, require a `RELEASED` unlock for that buyer and offer, then fetch the seller's current name/phone/email through `UserRepository`. Return a dedicated contact DTO; do not reuse the full `Seller` model.
- [ ] **Step 4: Run GREEN:** `./gradlew test --tests '*ContactUnlockControllerIT' --no-daemon`.
- [ ] **Step 5: Commit** the files in this task with `git commit -m "feat: reveal paid seller contact to buyer"`.

### Task 6: Verify cross-service behavior and failure recovery

**Files:**
- Test: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/ContactUnlockFlowIT.java`
- Test: `src/test/java/com/devikapps/vaikaparts/event/ContactReleaseContractTest.java`
- Modify: `doc/api.yaml`
- Modify: `doc/contact-release-plan.md` if implementation decisions change an interface.

**Interfaces:** Keep the Pecunia request/response and event fixture identical in both projects.

- [ ] **Step 1: Add an end-to-end test**: create offer/researcher/seller, initiate and retry the same request, receive `SUCCESS` event, check `RELEASED`, read seller contact as buyer, find exactly one seller notification, and repeat the event. Add 10 and 100 independent unlocks to verify no missing or duplicate release.
- [ ] **Step 2: Run focused verification:** `./gradlew test --tests '*ContactUnlock*' --tests '*ContactRelease*' --no-daemon` (Docker required for `IT`).
- [ ] **Step 3: Run formatting and full suite:** `make format`, `make test`, `make build`; investigate any failure before claiming completion.
- [ ] **Step 4: Commit** only the final test/documentation changes with `git commit -m "test: verify contact release integration"`.

## Deployment Order

Deploy the Domain Backend migration and dedicated queue first, then Pecunia's idempotent API and event publisher, then enable the public contact-unlock route. Configure the same RabbitMQ exchange/vhost in both services, the Domain Backend's Pecunia API key and URL, and `contact-unlock.price-mga`. Verify the fixture in both CI pipelines. Do not enable contact release until the queue binding exists; otherwise a confirmed payment could produce an unroutable event.
