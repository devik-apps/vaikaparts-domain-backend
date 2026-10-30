# PAPI Direct Payments for Contact Unlocks

## Intent

Let a researcher unlock seller contact details using PAPI's direct mobile-money API, while keeping the current Vanilla Pay and manually reviewed Orange Money paths available during rollout.

## Selected approach

Vaika Domain Backend calls Pecunia's existing `POST /v1/payments/papi` endpoint. Pecunia remains the only service that holds PAPI credentials and calls PAPI. This reuses the direct API already implemented in Pecunia; it does not use PAPI Hosted Checkout.

The PAPI direct API does not return a hosted payment URL. Vaika's frontend must show its own payment screen, select `MVOLA`, `AIRTEL_MONEY`, or `ORANGE_MONEY`, and show the pending/terminal result. Vaika uses the authenticated researcher's saved name and phone as the payer; Orange Money additionally requires a temporary code from the client, which must not be persisted or logged.

## Goals

- Add PAPI as a contact-unlock provider without removing `VANILLA_PAY` or `MANUAL_ORANGE_MONEY`.
- Reuse `unlockRequestId` as PAPI's stable merchant `reference` and idempotency key; compute the amount in Vaika, never trust a client amount.
- Support PAPI's integer MGA amount floor (300 MGA), required payer name/phone, and Orange-only temporary code.
- Reconcile terminal `SUCCESS` and `FAILED` outcomes from Pecunia to Vaika through Vaika's authenticated status lookup. A successful outcome releases contacts; a failed outcome frees the unlock for a new attempt.
- Preserve the current seller-contact authorization and notification behavior.

## Cross-service status contract

Pecunia persists the PAPI merchant reference and exposes `GET /v1/payments/papi/{reference}`, refreshing pending payment status from PAPI. Vaika's authenticated unlock-status endpoint reads that response, validates its reference, provider, type, amount, currency, method, and payment identity against the reserved unlock, and applies `SUCCESS`/`FAILED` locally. A successful response releases contacts through the existing notification path; a failed response marks the attempt failed. Repeated status reads and late initiation responses must not regress terminal local state.

## API and configuration

Extend Vaika's contact-unlock request to identify PAPI's payment method and accept the Orange temporary code only when needed. The backend supplies `PROFILE_UNLOCK`, the configured price, and payer identity from the authenticated researcher's profile. Responses for PAPI report the unlock ID and status; they do not contain `payment_url`. Add an authenticated status lookup so the frontend can display the transition while polling Pecunia, without exposing seller contacts before `RELEASED`.

Vaika uses its existing `PECUNIA_BASE_URL` and `PECUNIA_API_KEY` to call Pecunia. PAPI credentials, signature secret, notification URL, test mode, and merchant beneficiary configuration remain in Pecunia's environment. No PAPI secret is sent to the browser.

## Non-goals

- Implement PAPI Hosted Checkout, cards, or a PAPI client directly in Vaika.
- Remove or silently redirect the existing Vanilla Pay/manual Orange paths.
- Store the Orange temporary code or expose seller contact details before verified success.
- Deploy or enable live PAPI credentials as part of this code change.

## Rollout

Configure and test Pecunia in test mode. Then deploy Vaika's additive PAPI path and frontend status polling. Exercise MVola, Airtel Money, Orange Money, timeout/retry, repeated status reads, and failure/retry cases before enabling PAPI for users. Keep existing providers available until production PAPI confirmations are observed end to end.
