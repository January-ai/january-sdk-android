# Error handling

SDK calls throw `JanuaryException` with `category`, `message`, `httpStatus`, and
the underlying cause when available.

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

Coroutine cancellation is never retried. The SDK handles `token_expired`
internally; do not wrap requests in an unbounded retry loop. Never log tokens,
meal images, nutrition data, or health profiles.
