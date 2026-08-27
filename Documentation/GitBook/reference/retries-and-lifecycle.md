# Retries and token lifecycle

The provider-backed client handles credential lifecycle in memory:

* caches a usable token;
* refreshes 60 seconds before expiry;
* coalesces concurrent refreshes into one provider call;
* retries provider exceptions with bounded exponential backoff;
* preserves coroutine cancellation;
* refreshes and replays a January request once only for HTTP `401` with
  `code: "token_expired"`.

The default provider policy makes nine total attempts with ±20% jitter. Nominal
delays are 1, 2, 4, 8, 8, 8, 8, and 8 seconds:

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

Pass it as `tokenRetryPolicy` to `withClientTokenProvider`. Set
`JanuaryTokenRetryPolicy.NONE` when an app-owned provider already applies its
own retry policy.

The retry policy applies to fetching a credential, not to arbitrary January API
requests. Other `401` responses, `403`, validation errors, rate limits, and
server errors are surfaced immediately. Do not wrap the SDK in an unbounded
retry loop.
