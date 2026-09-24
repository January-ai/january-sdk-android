# Foods

Use `user.foods` to autocomplete, search, fetch a full food, look up barcodes,
and suggest alternatives. To turn a typed or spoken meal description into
foods, use `user.foodAnalysis.analyzeDescription`
([Food analysis](photo-scanning.md#analyze-a-description)).

```kotlin
import ai.january.partner.foods.AutocompleteFoodsRequest

val suggestions = user.foods.autocomplete(AutocompleteFoodsRequest("ban"))
```

## Search and fetch the full food

Selecting a suggestion should fill the search field and run `search`. Search
rows may not list every serving, so fetch the full food with `get` before
opening a serving picker:

```kotlin
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.portion

val results = user.foods.search(SearchFoodsRequest("banana"))
val food = user.foods.get(GetFoodRequest(results.items.first().id))
val portion = food.portion(quantity = 1.5)
```

`portion` validates the serving and quantity and scales nutrients locally
([Food discovery and servings](../concepts/food-lifecycle.md)).

Search returns up to `limit` results (1–50, default 10). To page, pass
`offset`; a page shorter than `limit` is the last one. `totalCount` is the
number of results in this page, not the total number of matches.

```kotlin
val secondPage = user.foods.search(SearchFoodsRequest("banana", limit = 10, offset = 10))
```

## Barcodes and alternatives

```kotlin
import ai.january.partner.foods.DietPreference
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.foods.SuggestFoodAlternativesRequest

val scanned = user.foods.lookupBarcode(LookupFoodByBarcodeRequest("012345678905")).items.first()
val food = user.foods.get(GetFoodRequest(scanned.id))
val alternatives = user.foods.suggestAlternatives(
    SuggestFoodAlternativesRequest(food.id.value, dietPreferences = listOf(DietPreference.VEGAN)), // foodId is a String
)
```

`lookupBarcode` returns one food. Fetch the full food with `get` before showing
servings, as the native scanner does. A barcode with no match fails with
`ErrorCategory.NOT_FOUND`. Barcode coverage is US-only: a code issued outside
the United States (for example GS1 prefixes 73, 64, 54, or 93) isn't in the
database. For those, analyze a photo of the label
([Food analysis](photo-scanning.md#analyze-a-photo)) or use food search.

Each alternative lists the `servings` its nutrition can be read against.
