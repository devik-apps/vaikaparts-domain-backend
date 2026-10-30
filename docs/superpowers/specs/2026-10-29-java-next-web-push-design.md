# Java to Next.js Web Push Dispatch

## Intent

Connect Java notification delivery to the existing Next.js `/api/push/dispatch` route so notifications persisted by Java can reach browser subscriptions stored by the current client in Supabase, while preserving the existing Java-native push transport for deployments that still use Java subscriptions.

## Goals and constraints

- Use the existing `WEB_PUSH` delivery queue and its retry behavior.
- Select the Next.js transport explicitly with `NOTIFICATIONS_WEB_PUSH_DISPATCH_ENABLED=true`; keep the Java-native transport as the default for backwards compatibility.
- Authenticate Java requests with a shared `PUSH_DISPATCH_SECRET` of at least 32 characters; configure it separately from VAPID keys.
- Send the notification UUID, recipient Supabase UUID, bounded body, and safe same-origin path expected by the Next.js route.
- Treat non-2xx responses and transport errors as delivery failures, without exposing the secret in logs or exceptions.
- Keep notification persistence independent of dispatch success.

## Approaches

- **Chosen: add a selectable Next.js `WEB_PUSH` channel.** In dispatch mode Java uses the client’s existing Supabase subscriptions and queue retries; the existing Java-native channel remains the default. Only one `WEB_PUSH` channel is registered at a time, avoiding duplicate sends.
- Keep Java’s native VAPID sender as the only channel. It cannot reach the current client’s Supabase-only subscriptions, so it does not meet the request.
- Move client subscriptions into Java. This would replace the current Next.js/Supabase subscription flow and expand the change across repositories, so it is not selected.

## Non-goals

No change to browser subscription UX, Supabase schema, Next.js dispatch route, VAPID key generation, or notification content policy.

## Implementation calls

Use `PUSH_DISPATCH_URL` and `PUSH_DISPATCH_SECRET` in the Java deployment. Enable the path with both `NOTIFICATIONS_WEB_PUSH_ENABLED=true` and `NOTIFICATIONS_WEB_PUSH_DISPATCH_ENABLED=true`. Accept any 2xx response, including Next.js `202` and idempotent duplicate `200`; reject redirects and all other statuses so the queue can retry them.
