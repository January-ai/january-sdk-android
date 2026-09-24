# Client lifecycle

## One client per signed-in account

Create one `JanuaryPartnerClient` per signed-in account, for example in a
session-scoped DI component, and reuse it on every screen. The client caches
that account's token in memory, and every `forUser` scope of it shares that
cache. Because the SDK removes `January-End-User-ID` from January requests, a
new `forUser` scope alone keeps sending the previous account's token until it
expires. When the account signs out or switches, discard the client and create
a new one.

```kotlin
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.PartnerUserId
import java.time.ZoneId

class JanuarySession(tokenUrl: String, account: Account, sessions: SessionRepository) {
    val january = JanuaryPartnerClient.withClientTokenProvider(
        BackendTokenProvider(
            endpoint = tokenUrl,
            endUserId = account.id,
            sessionToken = { sessions.requireAccessToken() },
        ),
    )
    val user = january.forUser(PartnerUserId(account.id), timezone = ZoneId.systemDefault().id)
}

// Sign-in: create a JanuarySession for the account.
// Sign-out or account switch: drop it, and create a new one for the next account.
```

`BackendTokenProvider` is the provider from
[Authentication](../getting-started/authentication.md). The client has no
`close()`; dropping the reference is enough. Don't create a client per request,
and don't recreate one to refresh a token: the client caches, refreshes, and
deduplicates token requests itself
([Retries and token lifecycle](../reference/retries-and-lifecycle.md)).

## Coroutines

Network calls are main-safe `suspend` functions. Call them from
`viewModelScope`, `lifecycleScope`, or `LaunchedEffect`. Leaving the scope
cancels the HTTP request ([Cancellation](../reference/error-handling.md#cancellation)).

`PhotoScanImage.dataUri` is the exception: it decodes and re-encodes the image
on the calling thread, so run it on `Dispatchers.Default`
([Food analysis](../guides/photo-scanning.md#analyze-a-photo)).
