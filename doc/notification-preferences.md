# Notification preferences — stage 1

All users (seller, researcher and manager) have two opt-in preferences, stored in
the `users` table and exposed in user profiles:

```json
{
  "email_notifications_enabled": false,
  "sms_notifications_enabled": false
}
```

Use these same keys in Supabase `user_metadata` (the webhook field is
`raw_user_meta_data`). Send JSON booleans, not strings. Creation defaults both
preferences to false. Updates apply explicit booleans only: absent, null or
invalid values preserve the saved preference. Explicit false disables it.

Metadata synchronization is the modification path in this stage; no separate
preferences PATCH endpoint is introduced. Keep these metadata values current
when updating the profile. Researcher creation before the webhook also imports
the preferences from the authenticated user's metadata.

IN_APP remains always active and has no user-disable switch. These preferences
express user choices only: stage 1 does not implement or activate email/SMS
delivery. Provider availability and usable contact details will be checked when
the channels are integrated.

Migration: `V1_0_28__Add_user_notification_preferences.sql`. Existing users default
to false for both external channels. No previous migration is modified.

## Stage 2 — external channel implementations

`EmailNotificationChannel` uses `Mailer.sendOrThrow`, which reports SMTP failures.
The legacy `Mailer.accept` continues to log and absorb failures for existing callers.
Email subjects are selected by notification type; the message is HTML-escaped.
SMTP connection/read/write timeouts are bounded and SMTP debug logging is disabled.

`SmsNotificationChannel` depends only on `SmsProvider`, with provider-neutral
`SmsMessage` and `SmsReceipt` records. `BefianaSmsClient` implements the immediate
`POST /api/smsko/v1/send/` endpoint, using the API key directly in `Authorization`
(no `Bearer` prefix). Bulk/scheduled delivery is not implemented.

Configuration in `.env.template`:

```dotenv
NOTIFICATIONS_EMAIL_ENABLED=false
NOTIFICATIONS_SMS_ENABLED=false
BEFIANA_SMS_BASE_URL=https://your-provider-host
BEFIANA_SMS_API_KEY=your-api-key
SMS_CONNECT_TIMEOUT=5s
SMS_REQUEST_TIMEOUT=10s
```

Supply the real HTTPS origin as the base URL, without an API path, query string
or embedded credentials. HTTP is permitted only for localhost/127.0.0.1 testing.
Redirects are disabled to prevent forwarding the API key to another host.
The SMS provider bean is created only when SMS is enabled, and then validates its
configuration. Existing SMTP variables remain unchanged.

BEFIANA numbers are converted from national `0321234567`, international
`+261321234567` / `00261321234567`, or already-local `321234567` to `321234567`.
Spaces, hyphens and parentheses are removed. Other formats are rejected before
network access. Messages must contain 1–320 Unicode code points and must not be
blank; overlong messages are rejected, never silently truncated.

HTTP failures have structured reasons (`INVALID_REQUEST`, `AUTHENTICATION`,
`ACCOUNT_UNAVAILABLE`, `RATE_LIMIT`, `PROVIDER_ERROR`). Network failures,
interruptions and invalid success responses have `UNKNOWN_OUTCOME`: the request
may already have been accepted. No application-level retry is performed. API keys,
phone numbers and raw response bodies are not included in SMS exceptions/logs.
The receipt contains BEFIANA's `clientCorrelator` as a generic provider message ID;
it does not establish delivery to the recipient's handset.

## Stage 3 — notification service integration

The EMAIL and SMS channels are now Spring beans **only when their global flag is
true**. The existing IN_APP bean remains enabled. To activate EMAIL, configure the
existing SMTP variables and set `NOTIFICATIONS_EMAIL_ENABLED=true`. For SMS, set
the BEFIANA URL/key and `NOTIFICATIONS_SMS_ENABLED=true`. User opt-in is also
required; enabling a global flag alone does not notify every user externally.

`NotificationService.createAndSendNotification` is transactional. It first saves
IN_APP using the existing channel. If that channel is absent, disabled or fails,
processing throws and no external sends are scheduled. The email/SMS selection
uses the preferences and contact details mapped from the database, not a preference
supplied in the notification event. Missing contacts are skipped, while malformed
contacts are rejected by their channel/provider adapter. This validates format,
not ownership of the email address or phone number.

External channels execute in `afterCommit`: a transaction rollback produces no
email/SMS. Exceptions are isolated per external channel and cannot roll back the
already committed IN_APP notification or stop the other external channel. Direct
non-proxied callers without transaction synchronization send immediately after
IN_APP and emit a warning. Normal Spring-managed calls create/join a transaction.

The callback runs synchronously on the committing thread, with bounded provider
timeouts. It uses the preference snapshot captured during notification creation.
No new RabbitMQ event name, queue or migration is introduced by this integration.
The existing IN_APP WebSocket timing is unchanged.

Filter logs by `[NOTIF-PIPELINE]`, especially `CHANNEL_SKIPPED`,
`EXTERNAL_WAIT_COMMIT`, `CHANNEL_SEND`, `CHANNEL_RETURNED`, `CHANNEL_FAILED`
and `SMS_FAILED`. A successful callback means provider submission, not confirmed
delivery to the user. Existing event-log success is not a per-channel delivery status.

**Remaining reliability limitations:** after-commit callbacks are not a durable
queue. A process crash after commit can lose external sends; replaying an event
can duplicate them. Durable delivery records, deduplication and independent retries
from the original plan remain future work. No application-level retries or real
provider calls are performed by the unit/HTTP-contract tests.
