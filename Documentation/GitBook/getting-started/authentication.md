# Authentication

The SDK authenticates with client tokens that your app fetches from your
[backend token endpoint](backend-token-endpoint.md). You supply the fetching
code as a `JanuaryTokenProvider`; the SDK calls it when it needs a token.

## Recommended: token provider

This provider uses platform networking, so the endpoint and its authentication
stay in your app's code:

```kotlin
import ai.january.partner.JanuaryClientToken
import ai.january.partner.JanuaryTokenProvider
import ai.january.partner.JanuaryTokenProviderException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackendTokenProvider(
    endpoint: String,
    private val endUserId: String,
    private val sessionToken: suspend () -> String,
) : JanuaryTokenProvider {
    private val endpointUrl = URL(endpoint)

    override suspend fun fetchClientToken(): JanuaryClientToken {
        val session = sessionToken() // Read the current session on every call.
        return withContext(Dispatchers.IO) {
            val connection = (endpointUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setFixedLengthStreamingMode(0)
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $session")
                // Only the token relay reads this header. Your production endpoint
                // takes the user from the session and ignores it.
                setRequestProperty("January-End-User-ID", endUserId)
            }
            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw JanuaryTokenProviderException(
                        "Token endpoint returned HTTP $status",
                        retryable = status == 408 || status == 429 || status >= 500,
                    )
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                try {
                    JanuaryClientToken.fromJson(body)
                } catch (error: Exception) {
                    throw JanuaryTokenProviderException("Token endpoint returned an unreadable body", cause = error)
                }
            } catch (error: IOException) {
                // Offline, refused, or timed out.
                throw JanuaryTokenProviderException("Token endpoint unreachable", retryable = true, cause = error)
            } finally {
                connection.disconnect()
            }
        }
    }
}
```

The SDK retries only a `JanuaryTokenProviderException` with `retryable = true`,
with backoff ([Retries and token lifecycle](../reference/retries-and-lifecycle.md)).
Any other exception ends the request with `ErrorCategory.AUTHENTICATION`.

Create one client per signed-in account (for example in a session-scoped DI
component) and reuse it on every screen. When the account changes, discard it
and create a new one: the client caches the account's token, so a new
`forUser` scope alone keeps using the previous account's token
([Client lifecycle](../concepts/client-lifecycle.md)).

```kotlin
import ai.january.partner.JanuaryPartnerClient

val january = JanuaryPartnerClient.withClientTokenProvider(
    provider = BackendTokenProvider(
        endpoint = requireNotNull(appConfig.januaryTokenUrl),
        endUserId = account.id,
        sessionToken = { sessionRepository.requireAccessToken() },
    ),
)
```

The SDK has no default token endpoint, so fail at startup when yours isn't
configured rather than falling back to a local URL. Next, scope the client to
the user ([User identity and timezone](../concepts/user-context.md)).

## Fixed token

`withClientToken` takes one client token. The SDK doesn't refresh it: your app
fetches a new token and creates a new client before the old one expires.

```kotlin
val january = JanuaryPartnerClient.withClientToken(clientToken)
```

## Development API key

For a quick local test with no token endpoint, `JanuaryPartnerClient(developmentApiKey)`
sends your API key directly. The constructor is deprecated and prints a warning.
Load the key from your untracked `local.properties` into debug builds only, as
the [example app](example-app.md) does, and never ship it.

Next: [First request](quick-start.md)
