# Client and resources

`JanuaryPartnerClient` is the public entry point. Choose one authentication
constructor:

| Authentication | API |
| --- | --- |
| App-managed refresh | `withClientTokenProvider(provider, tokenRetryPolicy)` |
| Fixed short-lived token | `withClientToken(clientToken)` |

```kotlin
JanuaryPartnerClient.withClientToken(clientToken: String): JanuaryPartnerClient

JanuaryPartnerClient.withClientTokenProvider(
    provider: JanuaryTokenProvider,
    tokenRetryPolicy: JanuaryTokenRetryPolicy = JanuaryTokenRetryPolicy(),
): JanuaryPartnerClient

fun interface JanuaryTokenProvider {
    suspend fun fetchClientToken(): JanuaryClientToken
}

data class JanuaryClientToken(
    val token: String,
    val expiresIn: Long,
)
```

`JanuaryClientToken.fromJson(json)` accepts `expiresIn` and `expires_in`.
Provider mode requires a nonblank token whose lifetime is greater than the
60-second refresh leeway.

The public client always targets January production. There is no public base URL
or token endpoint URL. The generated OpenAPI transport is internal.

| Resource | Public operations |
| --- | --- |
| `foods` | `autocomplete`, `search`, `get`, `lookupBarcode`, `suggestAlternatives` |
| `restaurants` | `search`, `searchMenuItems`, `getMenuItems` |
| `foodAnalysis` | `analyzePhoto`, `analyzeDescription`, `correct` |
| `foodLogs` | `create`, `list`, `update`, `delete` |
| `glucose` | `predict` |

`forUser(PartnerUserId, timezone)` returns a lightweight
`JanuaryPartnerUserClient`. Its `foods`, `restaurants`, `foodAnalysis`,
`foodLogs`, and `glucose` wrappers apply one `PartnerUserContext`. Set the user
once, then use the scoped client for every operation. All network operations are
`suspend` functions.

Local food utilities include `FoodSearchItem.portion(...)` and
`PhotoScanImage.dataUri(...)`. Compose scanner UI is exposed through
`JanuaryFoodScanner`.

```kotlin
fun forUser(context: PartnerUserContext): JanuaryPartnerUserClient
fun forUser(
    endUserId: PartnerUserId,
    timezone: String? = null,
): JanuaryPartnerUserClient
```

`JanuaryTokenRetryPolicy` defaults are `maximumAttempts = 9`, one-second initial
delay, multiplier 2, eight-second cap, and jitter ratio 0.2. See the dedicated
[lifecycle reference](retries-and-lifecycle.md).
