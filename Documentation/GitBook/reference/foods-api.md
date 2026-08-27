# Foods API

All operations are `suspend`, execute against January production, and throw as
described in [Error handling](error-handling.md).

## Operations

```kotlin
suspend fun autocomplete(request: AutocompleteFoodsRequest): AutocompleteFoodsResponse
suspend fun search(request: SearchFoodsRequest): FoodSearchResults
suspend fun getFood(request: GetFoodRequest): FoodSearchItem
suspend fun lookupBarcode(request: LookupFoodByBarcodeRequest): FoodSearchResults
suspend fun searchNaturalLanguage(
    request: SearchFoodsByNaturalLanguageRequest,
): SearchFoodsByNaturalLanguageResponse
suspend fun suggestAlternatives(
    request: SuggestFoodAlternativesRequest,
): SuggestFoodAlternativesResponse
```

## Requests and defaults

| Request | Fields |
| --- | --- |
| `AutocompleteFoodsRequest` | `query: String`; `category: AutocompleteFoodCategory? = null`; `limit: Int = 8`; `endUserId: PartnerUserId? = null` |
| `SearchFoodsRequest` | `query: String`; `category: FoodCategory? = null`; `limit: Int = 10`; `endUserId: PartnerUserId? = null` |
| `GetFoodRequest` | `foodId: FoodId`; `endUserId: PartnerUserId? = null` |
| `LookupFoodByBarcodeRequest` | `upc: String`; `endUserId: PartnerUserId? = null` |
| `SearchFoodsByNaturalLanguageRequest` | `query: String`; `endUserId: PartnerUserId? = null` |
| `SuggestFoodAlternativesRequest` | `foodId: Long`; `dietRestrictions: List<DietRestriction> = emptyList()`; `dietPreferences: List<DietPreference> = emptyList()`; `endUserId: PartnerUserId? = null` |

Autocomplete limits are 1–20 and its query may contain at most 64 characters.
Search requires a nonblank 1–256-character query and a limit from 1–40.

## Responses

`AutocompleteFoodsResponse.items` contains `FoodSuggestion`: `id`, `name`,
optional `brandName`, optional `imageUrl`, and optional `NutritionFacts`.

`FoodSearchResults` contains `totalCount` and `items: List<FoodSearchItem>`.
Each item includes identity/name/brand, complete `NutritionFacts`, flattened
macro conveniences, glycemic values, photo URL, and `servings`.

`ServingOption` fields are `id: ServingId`, `quantity`, `unit`, `scalingFactor`,
optional `weightGrams`, and `isPrimary`.

Natural-language search returns total nutrients plus detected foods and their
servings. Alternatives returns `alternatives: List<FoodAlternative>`.

## Portion helper

```kotlin
fun FoodSearchItem.portion(
    servingId: ServingId? = null,
    quantity: Double? = null,
): FoodPortion
```

Without a serving ID, the primary or first serving is selected. Without a
quantity, the serving's quantity is used. Quantity must be finite, positive, and
at most 10,000. Failures throw `FoodPortionException` with `FoodPortionError`:
`NO_SERVINGS`, `SERVING_NOT_FOUND`, `INVALID_SERVING`, or `INVALID_QUANTITY`.
