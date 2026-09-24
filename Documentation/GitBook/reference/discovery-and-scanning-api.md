# Restaurants and food analysis API

Prefer `client.forUser(...).restaurants` and
`client.forUser(...).foodAnalysis` so the active user is configured once. The
optional request identity fields below remain for direct-call compatibility.

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
| `query` | `String`, required, nonblank, at most 256 characters |
| `latitude` | `Double`, required, −90…90 |
| `longitude` | `Double`, required, −180…180 |
| `radius` | `Double = 8000.0`, in meters, range 1…50,000 |
| `limit` | `Int = 10`, range 1…100 |
| `endUserId` | `PartnerUserId? = null` |

`SearchRestaurantsResponse` has `totalCount` and `items: List<Restaurant>`.
Restaurant fields are `type`, `id`, `name`, optional chain/distance/city/address
metadata. Menu search returns `RestaurantMenuItem` values with restaurant name,
optional nutrition/distance/photo data, and `servings`.

`GetRestaurantMenuItemsRequest` accepts `restaurantId`, `limit` (default
`100`, range 1–100), `offset` (default `0`), and optional `endUserId`.
`GetRestaurantMenuItemsResponse` contains only `items: List<RestaurantMenuEntry>`,
with no `totalCount`: advance the offset by the returned item count while a
full page comes back. An empty page ends the menu, including for a restaurant
with no menu on record. An unknown restaurant returns `404`.

## Food analysis

```kotlin
suspend fun analyzePhoto(request: ScanFoodPhotoRequest): FoodScan
suspend fun analyzeDescription(
    request: SearchFoodsByNaturalLanguageRequest,
): FoodScan
suspend fun correct(request: CorrectPhotoScanRequest): FoodScan
```

`ScanFoodPhotoRequest` has `image: String`, optional `endUserId`, and optional
`reasoningEffort`. Left out, the API uses its default, the reasoning-based
analyzer (the same as `AnalysisEffort.XHIGH`); `AnalysisEffort.NONE` selects the
standard analyzer. The result shape and cost are the same either way. An analysis
can take tens of seconds, so food-analysis requests wait at least 120 seconds for
their answer. Use
`ScanFoodPhotoRequest.fromImageData(imageData, endUserId, maxDimension = 1000,
jpegQuality = 70)` or `PhotoScanImage.dataUri(...)` to prepare camera bytes.

`CorrectPhotoScanRequest` requires `mealName`, the current
`List<FoodDetection>`, `userInput`, and optional `endUserId`.

`FoodScan` contains optional `mealName`, `totalNutrients`, and `detections`.
Each detection contains a `DetectedFood` and an optional confidence score.
`DetectedFood` has `id`, `name`, `brandName`, `nutrients`, `serving`, and
`quantity`: `serving` is the selected catalog serving (`ServingSummary` with
`id`, `quantity`, `unit`, where `quantity` is the size of one serving) and
`quantity` is how many of that serving were eaten, so
`FoodSelection(food.id, ServingSelection(food.serving.id, food.quantity))` logs
the detection as is. `nutrients` are already scaled to `quantity`.

Food alternatives (`foods.suggestAlternatives`) return `AlternativeFood` values
with `servings: List<ServingSummary>` to read the nutrition against.

## Native scanner

```kotlin
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

Configuration defaults to photo and barcode modes, photo initially, maximum
dimension 1,000, and JPEG quality 70. Results are `Photo(image, analysis)` or
`Barcode(value, food)`. `JanuaryFoodScannerController` exposes
`analyzePhoto(ByteArray)` and `lookupBarcode(String)` for host-owned UIs;
unmatched barcodes throw `NoBarcodeMatchException`.
