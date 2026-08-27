# Authentication and security

Use one authentication mode per client:

1. `withClientTokenProvider` for production applications;
2. `withClientToken` when the app owns the full short-lived-token lifecycle; or
3. `developmentApiKey` for approved, non-distributable development only.

```kotlin
val january = JanuaryPartnerClient.withClientTokenProvider(
    provider = JanuaryTokenProvider {
        JanuaryClientToken.fromJson(partnerBackend.fetchJanuaryToken())
    },
)
```

The partner backend response is `{ "token": "ct-…", "expiresIn": 1800 }`.
`JanuaryClientToken.fromJson` also accepts `expires_in`. The provider owns its
URL, method, app authentication, and headers; the SDK has no token-endpoint
default and never receives the partner secret.

The SDK caches tokens only in memory, refreshes 60 seconds before expiration,
and coalesces concurrent refreshes. Provider failures get nine total attempts
with ±20% jitter and nominal delays of 1, 2, 4, 8, 8, 8, 8, and 8 seconds.

```kotlin
val policy = JanuaryTokenRetryPolicy(
    maximumAttempts = 9,
    initialDelay = Duration.ofSeconds(1),
    multiplier = 2.0,
    maximumDelay = Duration.ofSeconds(8),
    jitterRatio = 0.2,
)
```

Only `401` with `code: "token_expired"` refreshes the token and replays the
January request once. Other authentication failures stop immediately.
Client-token requests omit `x-end-user-id` because the token identifies the user.
