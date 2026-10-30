# PAPI Contact Unlock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let researchers pay to unlock seller contacts through Pecunia's existing direct PAPI payment API while retaining current providers.

**Architecture:** Vaika reserves the contact unlock, initiates PAPI through `PecuniaClient`, and reconciles terminal state through Pecunia's existing status endpoint when the authenticated researcher polls Vaika. Pecunia owns all PAPI credentials and callback validation; the frontend owns the payment screen because direct PAPI does not return a hosted checkout URL.

**Tech Stack:** Java, Spring Boot, JPA, existing RabbitMQ event/outbox patterns, JUnit/Mockito, Pecunia HTTP API.

**Spec:** `doc/superpowers/specs/2026-10-30-papi-contact-unlock-design.md`

## Global Constraints

- Keep `VANILLA_PAY` and `MANUAL_ORANGE_MONEY` behavior unchanged.
- Vaika computes the MGA amount; never accept an amount from the client.
- Reuse `unlockRequestId` as Pecunia's stable PAPI merchant reference/idempotency key.
- Use the authenticated researcher's saved name and phone; reject initiation if either is invalid.
- Accept Orange's temporary code only for that initiation request; never persist or log it.
- Do not release or expose seller contacts until Pecunia reports a validated PAPI `SUCCESS` status.
- PAPI credentials and webhook secrets remain only in Pecunia; no PAPI secret is sent to the browser.
- Frontend polling is required for payment progress to appear in Vaika; this plan changes Vaika only.

## Review Focus

- Pecunia timeout after it accepted a payment: preserve the reserved unlock and stable reference so status polling can reconcile without another charge.
- Duplicate status reads or a late initiation response: keep terminal state and send at most one seller notification.
- Mismatched Pecunia response: reject mismatched reference, provider, method, amount, currency, type, or payment ID before changing local state.
- Invalid/missing saved payer data or Orange code: reject before calling Pecunia; do not leak the temporary code in logs/errors.
- Existing provider callers and contact authorization: retain current response semantics and never return contact data while pending or failed.

---

### Task 1: Extend the Pecunia client for direct PAPI initiation

**Files:**
- Modify: `src/main/java/com/devikapps/vaikaparts/client/PecuniaClient.java`
- Test: `src/test/java/com/devikapps/vaikaparts/client/PecuniaClientTest.java` (create if absent)

**Interfaces:**
- Consumes: Pecunia `POST /v1/payments/papi` contract.
- Produces: typed PAPI initiation method accepting amount, `PROFILE_UNLOCK`, stable reference, payment method, payer name/phone, and Orange temporary code when applicable; returns Pecunia's PAPI payment identifier/status without assuming a payment URL.

- [ ] Add client tests for request path, auth header, JSON fields, successful response, and non-2xx/invalid response handling.
- [ ] Run the focused client test and confirm it fails because the PAPI method/contract is absent.
- [ ] Implement the smallest typed request/response additions in `PecuniaClient`; keep existing Vanilla Pay calls unchanged.
- [ ] Run `./gradlew --offline -PskipClientPublish -PclientVersion=0.0.0-PLACEHOLDER test --tests com.devikapps.vaikaparts.client.PecuniaClientTest --no-daemon` and confirm it passes.
- [ ] Commit as `feat: add Pecunia PAPI initiation client`.

### Task 2: Add PAPI to the contact-unlock initiation flow

**Files:**
- Modify: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/ContactUnlockRequest.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/exchange/ContactUnlockController.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/ContactUnlockService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/ContactUnlockPersistenceService.java`
- Test: `src/test/java/com/devikapps/vaikaparts/service/ContactUnlockServiceTest.java`
- Test: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/ContactUnlockControllerIT.java`
- Test: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/ContactUnlockFlowIT.java`

**Interfaces:**
- Consumes: Task 1 typed Pecunia PAPI initiation method; existing authenticated user/profile and unlock reservation patterns.
- Produces: additive PAPI request variant with `MVOLA`, `AIRTEL_MONEY`, or `ORANGE_MONEY`, optional Orange-only temporary code, and a response containing unlock ID/status but no `payment_url`.

- [ ] Add service tests for each supported payment method, stable reference equal to the reserved unlock ID, backend-computed amount, invalid saved payer data, invalid/missing Orange code, and no-Pecunia-call on validation failure.
- [ ] Run `./gradlew --offline -PskipClientPublish -PclientVersion=0.0.0-PLACEHOLDER test --tests com.devikapps.vaikaparts.service.ContactUnlockServiceTest --no-daemon` and confirm the PAPI cases fail.
- [ ] Implement the PAPI branch while preserving the current Vanilla Pay and manual Orange branches.
- [ ] Add controller tests in `ContactUnlockControllerIT` for request validation and additive backward-compatible behavior; ensure the temporary code is not echoed in the response.
- [ ] Run `./gradlew --offline -PskipClientPublish -PclientVersion=0.0.0-PLACEHOLDER test --tests com.devikapps.vaikaparts.service.ContactUnlockServiceTest --tests com.devikapps.vaikaparts.endpoint.rest.controller.ContactUnlockControllerIT --tests com.devikapps.vaikaparts.endpoint.rest.controller.ContactUnlockFlowIT --no-daemon` and confirm it passes.
- [ ] Keep this task's changes together with Tasks 3–4 in one commit because the service and persistence files overlap and PAPI is incomplete without terminal-state reconciliation.

### Task 3: Reconcile PAPI status and release contacts

**Files:**
- Modify: `src/main/java/com/devikapps/vaikaparts/service/ContactUnlockService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/ContactUnlockPersistenceService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/service/ContactReleaseService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/repository/ContactUnlockRepository.java` for owner-scoped status lookup; no schema migration is required.
- Test: `src/test/java/com/devikapps/vaikaparts/service/ContactReleaseServiceIT.java`
- Test: `src/test/java/com/devikapps/vaikaparts/service/ContactUnlockServiceTest.java`

**Interfaces:**
- Consumes: Pecunia `GET /v1/payments/papi/{reference}`, which refreshes the provider status and returns payment ID, transaction/merchant reference, method, type, amount, currency, provider, and `PENDING`/`SUCCESS`/`FAILED`.
- Produces: idempotent local state reconciliation; verified `SUCCESS` releases contact and notifies the seller, verified `FAILED` marks the attempt failed and permits a new attempt.

- [ ] Add status reconciliation tests for pending, valid success, valid failure, repeated status reads, and mismatched reference/provider/method/amount/currency/type/payment ID.
- [ ] Run focused service tests and confirm the PAPI terminal-state cases fail before implementation.
- [ ] On authenticated status lookup, query Pecunia outside the DB transaction, validate the response against the reserved unlock, then persist `FAILED` or use the existing seller notification/release behavior for `SUCCESS`; make repeated reads idempotent and never regress terminal local state.
- [ ] Run `./gradlew --offline -PskipClientPublish -PclientVersion=0.0.0-PLACEHOLDER test --tests com.devikapps.vaikaparts.service.ContactReleaseServiceIT --tests com.devikapps.vaikaparts.service.ContactUnlockServiceTest --no-daemon` and confirm they pass.
- [ ] Include these changes in the combined Tasks 2–4 commit.

### Task 4: Add authenticated unlock status lookup

**Files:**
- Modify: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/exchange/ContactUnlockController.java`.
- Modify: `src/main/java/com/devikapps/vaikaparts/service/ContactUnlockService.java`.
- Test: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/ContactUnlockControllerIT.java` and `src/test/java/com/devikapps/vaikaparts/service/ContactUnlockServiceTest.java`.

**Interfaces:**
- Produces: `GET /v1/offers/{offerId}/contact-unlocks/{unlockRequestId}`, restricted to the authenticated owner, returning only unlock ID/status and safe payment metadata; never seller contact details.

- [ ] Add tests for owner access, another researcher's denial/not-found behavior, and pending/released/failed response fields in the named test files.
- [ ] Run the focused tests and confirm failure before implementation.
- [ ] Implement the smallest read-only endpoint/query using existing security and repository patterns.
- [ ] Run `./gradlew --offline -PskipClientPublish -PclientVersion=0.0.0-PLACEHOLDER test --tests com.devikapps.vaikaparts.endpoint.rest.controller.ContactUnlockControllerIT --no-daemon` and confirm it passes.
- [ ] Include these changes in the combined Tasks 2–4 commit.

### Task 5: Verify compatibility and document rollout prerequisites

**Files:**
- Modify: `doc/` API/deployment documentation and `api.yaml` only if this repository's API contract is maintained there.
- Test: relevant existing contact unlock and contact release test suites.

**Interfaces:**
- Consumes: completed Vaika PAPI API and Pecunia status contract.
- Produces: documented request/status contract and staged enablement instructions; no live PAPI secret configuration in Vaika.

- [ ] Update `doc/api.yaml` for PAPI method fields, Orange temporary code handling, response without payment URL, and authenticated status lookup.
- [ ] Run `./gradlew --offline -PskipClientPublish -PclientVersion=0.0.0-PLACEHOLDER test --tests com.devikapps.vaikaparts.client.PecuniaClientTest --tests com.devikapps.vaikaparts.service.ContactUnlockServiceTest --tests com.devikapps.vaikaparts.endpoint.rest.controller.ContactUnlockControllerIT --tests com.devikapps.vaikaparts.endpoint.rest.controller.ContactUnlockFlowIT --tests com.devikapps.vaikaparts.service.ContactReleaseServiceIT --tests com.devikapps.vaikaparts.repository.ContactUnlockRepositoryIT --tests com.devikapps.vaikaparts.event.ContactReleaseListenerIT --tests com.devikapps.vaikaparts.endpoint.rest.controller.ManualContactUnlockControllerTest --tests com.devikapps.vaikaparts.service.ManualContactUnlockServiceTest --no-daemon` and confirm all related tests pass. The existing `ManualContactUnlockSecurityTest` is excluded because its WebMvc slice fails to provide `UserRepository`.
- [ ] Run `./gradlew --offline -PskipClientPublish -PclientVersion=0.0.0-PLACEHOLDER generateJavaClient generateTypeScriptClient --no-daemon` and confirm the API contract generates both clients.
- [ ] Verify Pecunia's deployed endpoint contract and frontend support before enabling this provider; test MVola, Airtel, Orange, provider timeout/retry, status polling, and failed-payment retry in test mode.
- [ ] Review the final diff and commit only the plan's related code/docs changes.
- [ ] Commit Tasks 2–5 as `feat: add PAPI contact unlock flow`.

## Frontend prerequisite (not implemented by this backend plan)

- **Frontend/SDK:** expose method selection, collect Orange temporary code only when needed, display pending/terminal status by polling Vaika's lookup, and do not expect a hosted payment URL. Regenerate the SDK from the updated API contract if that is the project's normal workflow.
- Pecunia already provides direct initiation and status lookup; its secrets and callback verification remain there. Enable PAPI only after end-to-end test-mode confirmation.
