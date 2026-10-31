# PAPI Contact Unlock Fees — Domain Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the authenticated researcher quote and pay the exact 3,000 MGA contact-unlock price plus the PAPI payer fee for MVola, Airtel Money, or Orange Money, with a safe end-to-end sandbox flow.

**Architecture:** Vaika owns the 3,000 MGA base price and contact-release decision. It asks Pecunia for a provider-specific quote before checkout, sends only the 3,000 MGA base amount during PAPI initiation, and stores the actual fee/total returned by Pecunia for later status responses.

**Tech Stack:** Java 21, Spring Boot, JPA/Flyway, Pecunia HTTP client, OpenAPI Generator, JUnit/Mockito/Testcontainers.

**Spec:** `doc/superpowers/specs/2026-10-30-papi-contact-unlock-design.md` plus the Pecunia contract in `../vaikaparts-pecunia/doc/api.yaml`.

## Global Constraints

- Set the default `CONTACT_UNLOCK_PRICE_MGA` to `3000`; production/preproduction deployment configuration must also be checked for overrides.
- Send `amount=3000 MGA` as the PAPI payment amount; never add `payerFee` into that amount.
- Obtain `payerFee` and `totalAmount` from Pecunia's fee/payment responses, never calculate operator fees in Vaika or Next.
- Return fee data only as safe numeric metadata; never return the PAPI API key, `Token`, Orange temporary code, or webhook secret.
- Keep current Vanilla Pay and manual Orange Money behavior intact while PAPI is tested.

## Review Focus

- Quote requests from non-owners/non-researchers must not expose offer data or fee configuration.
- Every supported provider must receive the same 3,000 MGA base while returning its own exact PAPI quote.
- `payerFee=0` is valid; negative, absent, wrong-method, wrong-amount, wrong-currency, or inconsistent total values are rejected.
- An initiation timeout preserves the stable unlock reference and its payment terms; retry/poll must not charge a different method or amount.
- Contacts remain hidden until validated PAPI `SUCCESS`, regardless of quoted fee or local payment metadata.

---

### Task 1: Align the configured base price and consume Pecunia fee quotes

**Files:**
- Modify: `src/main/resources/application.yml`, `application-prod.yml`, `application-preprod.yml`, and any checked-in environment example containing `CONTACT_UNLOCK_PRICE_MGA`.
- Modify: `src/main/java/com/devikapps/vaikaparts/client/PecuniaClient.java` and its nested response models.
- Test: `src/test/java/com/devikapps/vaikaparts/client/PecuniaClientTest.java`.

**Interfaces:**
- Consumes Pecunia `GET /v1/payments/papi/fees?paymentMethod=<method>&amount=3000`.
- Produces a typed quote containing `paymentMethod`, `amount`, `payerFee`, `totalAmount`, and `currency`.

- [x] Add client tests for URI/query behavior, `X-Api-Key` auth, exact quote parsing, zero fees, upstream errors, and malformed/inconsistent responses.
- [x] Run focused client tests and confirm the quote cases fail before implementation.
- [x] Change the default unlock price from `5000` to `3000`; no checked-in profile or environment example overrides this default.
- [x] Implement `PecuniaClient.quotePapiFees(paymentMethod, amount)` and validate the returned method, base amount, currency, nonnegative fee, and total arithmetic.
- [x] Run the focused client tests and confirm they pass.
- [x] Commit these changes on the domain feature branch.

### Task 2: Expose an owner-scoped contact-unlock fee quote

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/ContactUnlockFeeQuoteResponse.java`.
- Modify: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/exchange/ContactUnlockController.java` and `service/ContactUnlockService.java`.
- Modify: `doc/api.yaml`.
- Test: `src/test/java/com/devikapps/vaikaparts/service/ContactUnlockServiceTest.java` and `endpoint/rest/controller/ContactUnlockControllerIT.java`.

**Interfaces:**
- Produces `GET /v1/offers/{offerId}/contact-unlocks/fees?payment_method=MVOLA|AIRTEL_MONEY|ORANGE_MONEY`.
- Returns `payment_method`, `amount` (base 3,000), `payer_fee`, `total_amount`, and `currency=MGA`; it creates no unlock and no payment.

- [x] Add tests that all three methods return Pecunia's quote and that a non-owner, unsupported method, or unavailable Pecunia response is handled without creating an unlock.
- [x] Implement the endpoint using the same researcher/offer ownership checks as initiation; amount comes from server configuration.
- [x] Add the quote response schema and endpoint to `doc/api.yaml` with explicit meanings for base amount, payer fee, and total debit.
- [x] Run focused service and controller tests.
- [x] Commit these changes on the domain feature branch.

### Task 3: Preserve actual fees through initiation and status polling

**Files:**
- Modify: `src/main/java/com/devikapps/vaikaparts/repository/model/exchange/JContactUnlock.java`, `service/ContactUnlockService.java`, `service/ContactUnlockPersistenceService.java`, and `endpoint/rest/controller/model/ContactUnlockResponse.java`.
- Add migration: `src/main/resources/db/migration/V1_0_46__Persist_contact_unlock_papi_fees.sql` (confirm the next migration number before implementation).
- Modify: `doc/api.yaml`.
- Test: `src/test/java/com/devikapps/vaikaparts/service/ContactUnlockServiceTest.java`, `endpoint/rest/controller/ContactUnlockFlowIT.java`, and relevant repository ITs.

**Interfaces:**
- PAPI initiation remains `provider=PAPI`, a supported `payment_method`, and Orange-only `temporary_code`; the server sends Pecunia the base `amount=3000` and profile payer identity.
- PAPI responses add safe `payer_fee` and `total_amount`; non-PAPI responses leave these fields absent.

- [x] Add tests for fee persistence on initiation, status reads, zero fee, fee/total mismatch rejection, and unchanged Vanilla/manual response behavior.
- [x] Add nullable fee/total columns to `contact_unlocks` so existing rows migrate safely; do not persist `temporary_code`.
- [x] Validate Pecunia's actual payment response against the reserved method and base amount, and verify `totalAmount == amount + payerFee` before saving fee metadata.
- [x] Map stored fee metadata in initiation and status responses; terminal state and release idempotency do not depend on fee fields.
- [x] Update the OpenAPI response schema and generate both clients. Generated outputs remain in `build/`; no package was published.
- [x] Run focused service/controller/flow/repository tests.
- [x] Commit these changes on the domain feature branch.

### Task 4: Verify the domain service with sandbox Pecunia

- [ ] Configure preproduction/domain test environment with Pecunia's sandbox API key; set `CONTACT_UNLOCK_PRICE_MGA=3000` and ensure Pecunia points to its PAPI sandbox shop. Not run: sandbox credentials were not provided.
- [ ] Request quotes for MVola, Airtel, and Orange; confirm each reports base `3000`, the exact `payer_fee`, and matching `total_amount`.
- [ ] Initiate unlocks with each operator; confirm only the profile phone/name and Orange-only temporary code are sent, and the temporary code is not logged or persisted.
- [ ] Exercise pending→success/failure status reads, quote/provider failures, retry after failed payment, and unknown initiation outcome; confirm contacts are only returned after success.
- [ ] Run the focused commands above plus the project contact unlock/release integration suite; record that tests used a sandbox key and test accounts, never a production key.

## Handoff

Pecunia's plan must land and its quote/payment response contract be available before Tasks 1–3. The frontend plan consumes these domain endpoints and generated TypeScript models. PAPI documents sandbox success numbers `0341230001` (MVola), `331230001` (Airtel), and `0321230001` (Orange); acquire any Orange temporary code from the sandbox instructions and do not guess one.

The full Gradle test suite was started but not completed: unrelated authorization, webhook, user, notification, and WebSocket tests failed in this environment; RabbitMQ then repeatedly returned `Connection refused`, so the run was stopped after the targeted suites passed. Deployment-level `CONTACT_UNLOCK_PRICE_MGA` overrides were not inspectable from this repository.
