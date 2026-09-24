# Restaurants and food analysis API

## Restaurants

```kotlin
suspend fun search(request: SearchRestaurantsRequest): SearchRestaurantsResponse
suspend fun searchMenuItems(
    request: SearchRestaurantsRequest,
): SearchRestaurantMenuItemsResponse
suspend fun getMenuItems(
    request: GetRestaurantMenuItemsRequest,
): GetRestaurantMenuItemsResponse
```

`SearchRestaurantsRequest` fields:

| Field | Type and default |
| --- | --- |
| `query` | `String`, required, 1–256 characters, not blank |
| `latitude` | `Double`, required, −90…90 |
| `longitude` | `Double`, required, −180…180 |
| `radius` | `Double = 8000.0`, in meters, range 1…50,000 |
| `limit` | `Int = 10`, range 1…100 |
| `endUserId` | `PartnerUserId? = null` |

`SearchRestaurantsResponse` has `items: List<Restaurant>` and `totalCount`, the
number of restaurants returned. Restaurant fields are `type`, `id`, `name`, and
optional chain, distance, city, and address metadata. Menu search returns
`RestaurantMenuItem` values with the restaurant name, optional nutrition,
distance, and photo data, and `servings`.

`GetRestaurantMenuItemsRequest` takes `restaurantId`, `limit` (default `100`,
range 1–100), `offset` (default `0`), and optional `endUserId`.
`GetRestaurantMenuItemsResponse` contains only `items: List<RestaurantMenuEntry>`,
with no `totalCount`: advance the offset by the number of items returned while
a full page comes back. An empty page ends the menu, including for a restaurant
with no menu on record. An unknown restaurant fails with
`ErrorCategory.NOT_FOUND` ([Restaurants](../guides/restaurants.md)).

## Food analysis

```kotlin
suspend fun analyzePhoto(request: ScanFoodPhotoRequest): FoodScan
suspend fun analyzeDescription(
    request: SearchFoodsByNaturalLanguageRequest,
): FoodScan
suspend fun correct(request: CorrectPhotoScanRequest): FoodScan
```

`ScanFoodPhotoRequest` has `image: String` (a data URI or an http(s) URL),
optional `endUserId`, and optional `reasoningEffort`. Left out, the API uses its
default, the reasoning-based analyzer (the same as `AnalysisEffort.XHIGH`);
`AnalysisEffort.NONE` selects the standard analyzer. The result shape and cost
are the same either way. Food-analysis requests wait at least 120 seconds for
their answer. To prepare camera bytes, use
`ScanFoodPhotoRequest.fromImageData(imageData, endUserId, maxDimension = 1000,
jpegQuality = 70, reasoningEffort = null)` or `PhotoScanImage.dataUri(...)`;
both work on the calling thread.

`SearchFoodsByNaturalLanguageRequest(query, endUserId = null)` takes the meal
description, at most 512 characters.

`CorrectPhotoScanRequest(analysis, instruction, endUserId = null)` takes the
complete prior `FoodScan`, unchanged, and a plain-language `instruction` (at
most 1,000 characters). The `(mealName, detections, userInput)` constructor is
deprecated.

`FoodScan` contains `totalNutrients`, `detections`, and an optional `mealName`
(null for a description). `detections` is empty when nothing was recognized.
Food analysis doesn't return a glucose impact; use `glucose.predict` for that.
Each `FoodDetection` contains a `DetectedFood` and an optional
`confidenceScore`.

`DetectedFood` has `id`, `name`, `brandName`, `nutrients`, `serving`, and
`quantity`. `serving` is the selected catalog serving (`ServingSummary` with
`id`, `quantity`, `unit`, and `weightGrams`, where `quantity` is the size of one
serving), and `quantity` is how many of that serving were eaten. `nutrients`
are already scaled to `quantity`. The API always returns `id`, `serving.id`,
and `quantity`; the SDK types are nullable only for values you build yourself
([Log the result](../guides/photo-scanning.md#log-the-result)).

Food alternatives (`foods.suggestAlternatives`) return `AlternativeFood` values
with `servings: List<ServingSummary>` to read the nutrition against.

## Native scanner

```kotlin
@Composable
fun JanuaryFoodScanner(
    userClient: JanuaryPartnerUserClient,
    modifier: Modifier = Modifier,
    configuration: JanuaryFoodScannerConfiguration = JanuaryFoodScannerConfiguration(),
    onResult: (JanuaryFoodScannerResult) -> Unit,
    onCancel: () -> Unit,
)

@Composable
fun JanuaryFoodScanner(
    client: JanuaryPartnerClient,
    modifier: Modifier = Modifier,
    endUserId: PartnerUserId? = null,
    configuration: JanuaryFoodScannerConfiguration = JanuaryFoodScannerConfiguration(),
    onResult: (JanuaryFoodScannerResult) -> Unit,
    onCancel: () -> Unit,
)
```

Use the `userClient` overload. `JanuaryFoodScannerConfiguration` defaults to
photo and barcode modes (`enabledModes`), photo first (`initialMode`), a
maximum image dimension of 1,000, and JPEG quality 70. Results are
`JanuaryFoodScannerResult.Photo(image, analysis)` or
`JanuaryFoodScannerResult.Barcode(value, food)`, where `food` is the full food.

`JanuaryFoodScannerController(userClient, configuration)` exposes
`analyzePhoto(ByteArray)` and `lookupBarcode(String)` for your own scanner UI.
`analyzePhoto` prepares the image on the calling thread, so call it from
`Dispatchers.Default`. `lookupBarcode` also fetches the full food; an unmatched
barcode fails with `ErrorCategory.NOT_FOUND`.
