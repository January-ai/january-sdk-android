# Retries and token lifecycle

A client created with `withClientTokenProvider` manages its token in memory:

* It caches the token and refreshes it 60 seconds before it expires.
* It rejects a token that is blank or has 60 seconds or less left
  (`ErrorCategory.AUTHENTICATION`).
* It combines concurrent refreshes into one provider call.
* It retries the provider with bounded exponential backoff when the provider
  throws `JanuaryTokenProviderException(retryable = true)`. Any other exception
  fails at once.
* On HTTP `401` with `code: "token_expired"`, it refreshes the token and
  replays the request once.

The cache belongs to the client and is shared by all its `forUser` scopes
([Client lifecycle](../concepts/client-lifecycle.md)).

## Backoff

The default policy makes nine attempts in total, with ±20% jitter. Nominal
delays between attempts are 1, 2, 4, 8, 8, 8, 8, and 8 seconds:

```kotlin
import ai.january.partner.JanuaryTokenRetryPolicy
import java.time.Duration

val policy = JanuaryTokenRetryPolicy(
    maximumAttempts = 9,
    initialDelay = Duration.ofSeconds(1),
    multiplier = 2.0,
    maximumDelay = Duration.ofSeconds(8),
    jitterRatio = 0.2,
)
```

If your token endpoint keeps failing with retryable errors, the default policy
spends about 47 seconds in backoff, plus each attempt's own timeout (up to
20 seconds with the sample provider's timeouts), before the call fails. Pass a
smaller policy as `tokenRetryPolicy` to `withClientTokenProvider` for
interactive screens, or `JanuaryTokenRetryPolicy.NONE` when your provider
already retries.

```kotlin
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.JanuaryTokenRetryPolicy

val january = JanuaryPartnerClient.withClientTokenProvider(
    provider = tokenProvider,
    tokenRetryPolicy = JanuaryTokenRetryPolicy(maximumAttempts = 3),
)
```

The policy covers fetching a token only. January API responses such as other
`401`s, `403`, validation errors, rate limits, and server errors surface
immediately ([Error handling](error-handling.md)).
