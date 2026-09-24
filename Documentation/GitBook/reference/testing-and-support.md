# Testing and support

## Test your integration

The SDK has no base-URL override, and its resource classes are final, so you
can't point it at a mock server or subclass it. Put the SDK behind your own
interface and fake that interface in tests. The model classes are public data
classes, and `JanuaryException` has a public constructor, so fakes can return
realistic results and errors.

```kotlin
import ai.january.partner.ErrorCategory
import ai.january.partner.JanuaryException
import ai.january.partner.JanuaryPartnerUserClient
import ai.january.partner.foods.FoodSearchResults
import ai.january.partner.foods.SearchFoodsRequest

interface FoodSearch {
    suspend fun search(query: String): FoodSearchResults
}

class JanuaryFoodSearch(private val user: JanuaryPartnerUserClient) : FoodSearch {
    override suspend fun search(query: String) = user.foods.search(SearchFoodsRequest(query))
}

class OfflineFoodSearch : FoodSearch {
    override suspend fun search(query: String): FoodSearchResults =
        throw JanuaryException(ErrorCategory.TRANSPORT, "Offline")
}
```

Before shipping, check these on a device or emulator:

1. The token endpoint succeeds, and a failing endpoint surfaces as
   `ErrorCategory.AUTHENTICATION` after the retries you configured.
2. Concurrent requests at startup make one token request.
3. A `token_expired` response fetches a new token and replays once.
4. Leaving a screen cancels its requests.
5. Autocomplete → search → `get` shows every serving.
6. Camera denied and granted, a photo scan, and a barcode lookup.
7. Signing out and in as another account shows only the new account's data
   ([Client lifecycle](../concepts/client-lifecycle.md)), and changing the
   timezone moves entries to the right days.

## Versioning and updates

Released versions are on Maven Central and in the [changelog](changelog.md).
Pin an exact version, read the changelog before you update, and rerun the checks
above.

## Support report

Include the SDK version, Android, AGP, and Java versions, the failing operation,
`JanuaryException.category`, `code`, `requestId`, the HTTP status, and steps to
reproduce. Leave out tokens, API keys, meal images, nutrition records, and
health profiles.
