# Client and resources

`JanuaryPartnerClient` is the entry point. Create one per signed-in account
([Client lifecycle](../concepts/client-lifecycle.md)) with one of these
constructors:

| Constructor | Use |
| --- | --- |
| `withClientTokenProvider(provider, tokenRetryPolicy)` | Recommended. The SDK fetches, caches, and refreshes tokens through your provider. |
| `withClientToken(clientToken)` | Fixed token. Your app refreshes it and creates a new client before it expires. |

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

class JanuaryTokenProviderException(
    message: String,
    val retryable: Boolean = false,
    cause: Throwable? = null,
) : Exception
```

`JanuaryClientToken.fromJson(json)` reads `token` and either `expires_in` or
`expiresIn`, and throws when either is missing. The provider must return a token
that isn't blank and has more than 60 seconds left
([Retries and token lifecycle](retries-and-lifecycle.md)).

| Resource | Operations |
| --- | --- |
| `foods` | `autocomplete`, `search`, `get`, `lookupBarcode`, `suggestAlternatives` |
| `restaurants` | `search`, `searchMenuItems`, `getMenuItems` |
| `foodAnalysis` | `analyzePhoto`, `analyzeDescription`, `correct` |
| `foodLogs` | `create`, `list`, `get`, `getSummary`, `update`, `delete` |
| `waterLogs` | `create`, `list`, `delete` |
| `weightLogs` | `create`, `list` |
| `glucose` | `predict` |

All network operations are `suspend` functions.

```kotlin
fun forUser(context: PartnerUserContext): JanuaryPartnerUserClient
fun forUser(
    endUserId: PartnerUserId,
    timezone: String? = null,
): JanuaryPartnerUserClient
```

`forUser` returns a lightweight `JanuaryPartnerUserClient` with the same seven
resources, each applying one `PartnerUserContext` (its `context` property)
([User identity and timezone](../concepts/user-context.md)).

Local helpers: `FoodSearchItem.portion(...)` ([Foods API](foods-api.md#portion-helper))
and `PhotoScanImage.dataUri(...)`. The Compose scanner UI is `JanuaryFoodScanner`
([Restaurants and food analysis API](discovery-and-scanning-api.md#native-scanner)).
