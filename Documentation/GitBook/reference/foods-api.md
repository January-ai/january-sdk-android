# Foods API

All operations are `suspend` functions and throw as described in
[Error handling](error-handling.md).

## Operations

```kotlin
suspend fun autocomplete(request: AutocompleteFoodsRequest): AutocompleteFoodsResponse
suspend fun search(request: SearchFoodsRequest): FoodSearchResults
suspend fun get(request: GetFoodRequest): FoodSearchItem
suspend fun lookupBarcode(request: LookupFoodByBarcodeRequest): FoodSearchResults
suspend fun suggestAlternatives(
    request: SuggestFoodAlternativesRequest,
): SuggestFoodAlternativesResponse
```

## Requests and defaults

| Request | Fields |
| --- | --- |
| `AutocompleteFoodsRequest` | `query: String`; `category: AutocompleteFoodCategory? = null`; `limit: Int = 8`; `endUserId: PartnerUserId? = null` |
| `SearchFoodsRequest` | `query: String`; `category: FoodCategory? = null`; `limit: Int = 10` (1–50); `endUserId: PartnerUserId? = null`; `offset: Int = 0` |
| `GetFoodRequest` | `foodId: FoodId`; `endUserId: PartnerUserId? = null` |
| `LookupFoodByBarcodeRequest` | `upc: String`; `endUserId: PartnerUserId? = null` |
| `SuggestFoodAlternativesRequest` | `foodId: String` (a `Long` constructor remains); `dietRestrictions: List<DietRestriction> = emptyList()`; `dietPreferences: List<DietPreference> = emptyList()`; `endUserId: PartnerUserId? = null` |

Autocomplete limits are 1–20, and its query may contain at most 64 characters.
Search requires a query of 1–256 characters that isn't blank, a limit from 1–50,
and an offset of 0 or more.

## Responses

`AutocompleteFoodsResponse.items` contains `FoodSuggestion`: `id`, `name`,
optional `brandName`, optional `imageUrl`, and optional `NutritionFacts`.

`FoodSearchResults` contains `items: List<FoodSearchItem>` and `totalCount`, the
number of items in this response (not the total number of matches). Each item
has its ID, name, brand, complete `NutritionFacts` (`nutrients`), flattened
macro fields, glycemic values, photo URL, and `servings`. `lookupBarcode`
returns one item.

`ServingOption` fields are `id: ServingId`, `quantity`, `unit`, `scalingFactor`,
optional `weightGrams`, and `isPrimary`.

Alternatives returns `alternatives: List<AlternativeFood>`. Meal descriptions
go to `foodAnalysis.analyzeDescription`
([Restaurants and food analysis API](discovery-and-scanning-api.md#food-analysis)).

## Portion helper

```kotlin
fun FoodSearchItem.portion(
    servingId: ServingId? = null,
    quantity: Double? = null,
): FoodPortion
```

Without a serving ID, the primary or first serving is selected. Without a
quantity, the serving's quantity is used. Quantity must be finite, positive, and
at most 10,000. Failures throw `FoodPortionException` with a `reason` of type
`FoodPortionError`: `NO_SERVINGS`, `SERVING_NOT_FOUND`, `INVALID_SERVING`, or
`INVALID_QUANTITY`. [Models and enums](models-and-enums.md#foods) lists the
`FoodPortion` fields.
