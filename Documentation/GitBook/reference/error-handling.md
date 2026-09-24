# Error handling

SDK calls throw `JanuaryException` with `category`, `message`, `httpStatus`,
`code`, `requestId`, and the underlying cause when available.

```kotlin
try {
    client.foods.search(SearchFoodsRequest("banana"))
} catch (error: JanuaryException) {
    when (error.category) {
        ErrorCategory.VALIDATION -> showInputError(error.message.orEmpty())
        ErrorCategory.RATE_LIMITED -> showRetryState()
        ErrorCategory.AUTHENTICATION,
        ErrorCategory.AUTHORIZATION -> showConnectionError()
        else -> showGenericError()
    }
}
```

`code` carries the API's stable error identifier when one was returned, for
example `daily_water_limit_exceeded` (a water log would take the day past 24 L)
or `date_range_too_large` (a range past an operation's limit, such as a start
more than five years back); both are `ErrorCategory.VALIDATION`. So is
`conflict` (HTTP 409), a request that clashes with what the API holds: it is
permanent, and sending it again unchanged gets the same answer.

Coroutine cancellation is never retried. The SDK handles `token_expired`
internally; do not wrap requests in an unbounded retry loop. Never log tokens,
meal images, nutrition data, or health profiles.
