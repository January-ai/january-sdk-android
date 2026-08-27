# Authentication

## Recommended: token provider

Implement `JanuaryTokenProvider` around your own authenticated backend. This
complete provider uses platform networking so the endpoint remains app-owned:

```kotlin
import ai.january.partner.JanuaryClientToken
import ai.january.partner.JanuaryTokenProvider
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PartnerBackendTokenProvider(
    endpoint: String,
    private val sessionToken: suspend () -> String,
) : JanuaryTokenProvider {
    private val endpointUrl = URL(endpoint)

    override suspend fun fetchClientToken(): JanuaryClientToken =
        withContext(Dispatchers.IO) {
            val connection = (endpointUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer ${sessionToken()}")
            }

            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw IllegalStateException("Token endpoint returned HTTP $status")
                }
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                JanuaryClientToken.fromJson(json)
            } finally {
                connection.disconnect()
            }
        }
}
```

Create one client in your application dependency graph:

```kotlin
import ai.january.partner.JanuaryPartnerClient

val january = JanuaryPartnerClient.withClientTokenProvider(
    provider = PartnerBackendTokenProvider(
        endpoint = requireNotNull(appConfig.januaryTokenUrl),
        sessionToken = { sessionRepository.requireAccessToken() },
    ),
)
```

There is intentionally no token-endpoint default. Missing endpoint configuration
should fail during application setup, not silently fall back to localhost.

## Fixed short-lived token

If the host application owns refresh and client recreation, pass a current
client token directly:

```kotlin
val january = JanuaryPartnerClient.withClientToken(clientToken)
```

## Development partner key

`JanuaryPartnerClient(developmentApiKey = ...)` exists only for approved local,
non-distributable development. Never use it in a shipped app. Prefer the demo's
token-provider flow even during integration testing.

See [Retries and token lifecycle](../reference/retries-and-lifecycle.md) for
caching, refresh, backoff, and `token_expired` behavior.
