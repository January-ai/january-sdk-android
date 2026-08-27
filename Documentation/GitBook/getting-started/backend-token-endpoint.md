# Backend token endpoint

Every distributed Android app needs a partner-owned backend endpoint that
returns a short-lived January client token. January's private server-side
token-issuance integration stays outside the app and public SDK.

## Stable app-facing contract

Your endpoint may use any host, path, HTTP method, and application-authentication
scheme. Its successful JSON response must be one of:

```json
{ "token": "ct-…", "expiresIn": 1800 }
```

```json
{ "token": "ct-…", "expires_in": 1800 }
```

The SDK accepts either expiry spelling. The lifetime must be greater than 60
seconds because the SDK refreshes one minute early.

## Server responsibilities

1. Authenticate the signed-in partner user.
2. Determine the stable end-user ID on the server; do not trust an arbitrary ID
   from an unauthenticated request.
3. Complete January's private server-side exchange for a token bound to that user.
4. Return only `token` and its lifetime to the app.
5. Apply normal server controls: TLS, authorization, rate limiting, audit events,
   and secret rotation.

Do not log server-side credentials or returned client tokens. Do not put
token-issuance credentials in Gradle properties, `BuildConfig`, app resources,
remote configuration, or the APK.

## Local proof flow

During onboarding, configure the URL of your token endpoint only in the demo's
untracked `local.properties`; the emulator reaches a service on the host machine
through `10.0.2.2`:

```properties
january.partnerTokenUrl=http://10.0.2.2:8787/january-token
```

The public SDK always calls January production and exposes no environment switch.
