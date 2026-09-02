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
): SearchRestaurantMenuItemsResponse
```

`SearchRestaurantsRequest` fields:

| Field | Type and default |
| --- | --- |
| `query` | `String`, required, nonblank, at most 256 characters |
| `latitude` | `Double`, required, −90…90 |
| `longitude` | `Double`, required, −180…180 |
| `radius` | `Double = 8000.0`, range 1…17,000 |
| `limit` | `Int = 10`, range 1…100 |
| `endUserId` | `PartnerUserId? = null` |

`SearchRestaurantsResponse` has `totalCount` and `items: List<Restaurant>`.
Restaurant fields are `type`, `id`, `name`, optional chain/distance/city/address
metadata. Menu search returns `RestaurantMenuItem` values with restaurant name,
optional nutrition/distance/photo data, and `servings`.

`GetRestaurantMenuItemsRequest` accepts `restaurantId`, `limit` (default
`100`, range 1–100), `offset` (default `0`), and optional `endUserId`. Advance
the offset by the returned item count until it reaches `totalCount` or a page
is empty. An unknown restaurant returns `404`; a restaurant without a menu
returns an empty response.

## Food analysis

```kotlin
suspend fun analyzePhoto(request: ScanFoodPhotoRequest): FoodScan
suspend fun analyzeDescription(
    request: SearchFoodsByNaturalLanguageRequest,
): SearchFoodsByNaturalLanguageResponse
suspend fun correct(request: CorrectPhotoScanRequest): FoodScan
```

`ScanFoodPhotoRequest` has `image: String` and optional `endUserId`. Use
`ScanFoodPhotoRequest.fromImageData(imageData, endUserId, maxDimension = 1000,
jpegQuality = 70)` or `PhotoScanImage.dataUri(...)` to prepare camera bytes.

`CorrectPhotoScanRequest` requires `mealName`, the current
`List<FoodDetection>`, `userInput`, and optional `endUserId`.

`FoodScan` contains optional `mealName`, `totalNutrients`, `detections`, and
`glucoseImpact`. Each detection contains a `DetectedFood` and optional confidence
score.

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
