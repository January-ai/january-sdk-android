# Error handling

Network operations throw `JanuaryException` with `category`, `message`,
`httpStatus`, `cause`, and, when the API returned them, its stable error `code`
and `requestId`. Branch on `code` where it matters, then on `category`:

```kotlin
import ai.january.partner.ErrorCategory
import ai.january.partner.JanuaryException
import ai.january.partner.foods.SearchFoodsRequest
import android.util.Log

try {
    user.foods.search(SearchFoodsRequest("banana"))
} catch (error: JanuaryException) {
    when {
        error.code == "request_limit_exceeded" || error.code == "credit_limit_exceeded" ->
            showQuotaReached() // Don't retry before the allowance resets.
        error.category == ErrorCategory.RATE_LIMITED -> showRetryLater()
        error.category == ErrorCategory.TRANSPORT ||
            error.category == ErrorCategory.TIMEOUT -> showOffline()
        error.category == ErrorCategory.VALIDATION -> showInputError(error.message.orEmpty())
        error.category == ErrorCategory.AUTHORIZATION -> {
            // A missing scope or disabled client tokens: an integration bug.
            Log.e("January", "code=${error.code} requestId=${error.requestId}")
            showGenericError()
        }
        else -> showGenericError()
    }
}
```

| Category | Cause |
| --- | --- |
| `VALIDATION` | HTTP 400, 409, or 422, or a check the SDK makes before sending. Fix the request; sending it again unchanged gets the same answer. |
| `AUTHENTICATION` | HTTP 401 that a token refresh didn't fix, or the token provider failed. Its `cause` holds the provider's exception, so an offline device can show up here when a token is due. |
| `AUTHORIZATION` | HTTP 403, for example `scope_insufficient` when the token lacks a scope. |
| `NOT_FOUND` | HTTP 404, for example an unknown barcode, restaurant, or food log. |
| `RATE_LIMITED` | HTTP 429. `rate_limited` clears after a short wait; `request_limit_exceeded` and `credit_limit_exceeded` last until the allowance resets. The SDK doesn't expose `Retry-After`. |
| `TIMEOUT` | The request timed out, or HTTP 504. |
| `TRANSPORT` | The network failed, or the API returned an unexpected status. |
| `DECODING` | The response couldn't be read, for example a unit this SDK version doesn't know. |
| `SERVER` | Other HTTP 5xx. Retry later with backoff. |

`code` carries the API's error identifier, such as `daily_water_limit_exceeded`
or `date_range_too_large` (both `VALIDATION`). The
[REST error table](https://docs.january.ai/rest-api/api-overview#errors) lists every code.
Handle a code you don't recognize by its category.

The SDK refreshes and replays a request once on `401 token_expired`, and
retries a failing token provider as [Retries and token lifecycle](retries-and-lifecycle.md)
describes. It retries nothing else. Don't wrap calls in an unbounded retry
loop.

## Errors thrown before a request

These come from local checks and aren't `JanuaryException`:

| Exception | When |
| --- | --- |
| `IllegalArgumentException` | A blank `PartnerUserId`, or a food or water log ID that isn't a UUID. |
| `java.time.format.DateTimeParseException` | A malformed `YYYY-MM-DD` date or ISO 8601 date-time, such as a list range or `timestampUtc`. |
| `FoodPortionException` | `portion(...)` got an invalid serving or quantity ([Portion helper](foods-api.md#portion-helper)). |
| `VoiceCaptureException` | `VoiceCaptureSession.startListening()` couldn't start ([Voice capture](../guides/voice-capture.md)). |

## Cancellation

Canceling the coroutine that called the SDK cancels the HTTP request and
rethrows its `CancellationException` unchanged. Rethrow it if you catch
`Exception`, and don't retry it. A `CancellationException` thrown by your token
provider itself is reported as `JanuaryException` with category
`AUTHENTICATION`, because the provider runs inside the HTTP pipeline.
