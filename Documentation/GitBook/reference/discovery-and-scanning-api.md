# Restaurants and scanning API

## Restaurants

```kotlin
suspend fun search(request: SearchRestaurantsRequest): SearchRestaurantsResponse
suspend fun searchMenuItems(
    request: SearchRestaurantsRequest,
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

## Photo scanning

```kotlin
suspend fun scan(request: ScanFoodPhotoRequest): FoodScan
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
fun JanuaryMealScanner(
    client: JanuaryPartnerClient,
    modifier: Modifier = Modifier,
    endUserId: PartnerUserId? = null,
    configuration: JanuaryMealScannerConfiguration = JanuaryMealScannerConfiguration(),
    onResult: (JanuaryMealScannerResult) -> Unit,
    onCancel: () -> Unit,
)
```

Configuration defaults to photo and barcode modes, photo initially, maximum
dimension 1,000, and JPEG quality 70. Results are `Meal(image, analysis)` or
`Barcode(value, food)`. `JanuaryMealScannerController` exposes
`analyzePhoto(ByteArray)` and `lookupBarcode(String)` for host-owned UIs;
unmatched barcodes throw `NoBarcodeMatchException`.
