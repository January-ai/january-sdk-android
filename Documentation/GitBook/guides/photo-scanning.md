# Food analysis

`user.foodAnalysis` turns a meal photo or a typed or spoken description into
foods with portions and nutrition. The native food scanner,
`JanuaryFoodScanner`, wraps photo analysis and barcode lookup in a full-screen
Compose camera UI. These examples use `user`, the scoped client from
[User identity and timezone](../concepts/user-context.md).

An analysis can take tens of seconds, so the SDK waits at least 120 seconds for
the answer. Show progress while it runs, and don't wrap the call in a shorter
`withTimeout`.

## Analyze a photo

`PhotoScanImage.dataUri` corrects EXIF orientation, scales the longest edge down
to 1,000 pixels, and compresses to JPEG quality 70. It works on the calling
thread, so run it on `Dispatchers.Default`:

```kotlin
import ai.january.partner.photos.PhotoScanImage
import ai.january.partner.photos.ScanFoodPhotoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val image = withContext(Dispatchers.Default) { PhotoScanImage.dataUri(photoBytes) }
val scan = user.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest(image))
```

The photo can show a meal or a packaged product: the front of the pack, the
ingredient list, or the Nutrition Facts panel. Nutrition read from a label can
be incomplete, so check it before relying on it. A photo of only a barcode is
rejected; use [`foods.lookupBarcode`](foods.md#barcodes-and-alternatives) instead.

## Analyze a description

```kotlin
import ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest

val scan = user.foodAnalysis.analyzeDescription(
    SearchFoodsByNaturalLanguageRequest("two scrambled eggs and a slice of toast"),
)
```

A description can be typed or come from [voice capture](voice-capture.md). Its
result has no `mealName`.

## Correct a result

Send the complete prior result back, unchanged, with a plain-language
instruction:

```kotlin
import ai.january.partner.photos.CorrectPhotoScanRequest

val corrected = user.foodAnalysis.correct(
    CorrectPhotoScanRequest(analysis = scan, instruction = "Remove the fries"),
)
```

## Log the result

Each detection's `food` has its catalog `id`, the selected `serving`, and the
`quantity` eaten, ready for a [food log](food-logs.md). `detections` is empty
when nothing was recognized.

```kotlin
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection

val foods = scan.detections.mapNotNull { detection ->
    val food = detection.food
    FoodSelection(
        food.id ?: return@mapNotNull null,
        ServingSelection(
            food.serving.id ?: return@mapNotNull null,
            food.quantity ?: return@mapNotNull null,
        ),
    )
}
if (foods.isNotEmpty()) user.foodLogs.create(foods, name = scan.mealName)
```

## Native food scanner

The scanner has photo and barcode modes. Pass it the scoped client:

```kotlin
import ai.january.partner.scanner.JanuaryFoodScanner
import ai.january.partner.scanner.JanuaryFoodScannerResult

JanuaryFoodScanner(
    userClient = user,
    onResult = { result ->
        when (result) {
            is JanuaryFoodScannerResult.Photo -> openReview(result.analysis)
            is JanuaryFoodScannerResult.Barcode -> openServingPicker(result.food) // already a full food
        }
    },
    onCancel = { navController.popBackStack() },
)
```

The scanner asks for the camera permission and shows its own permission,
progress, and error states; an error offers **Try Again** and **Close**, and
**Close** calls `onCancel`. It doesn't close itself after a result: navigate
away in `onResult`.

Test permission denial (including "Don't ask again"), a camera that fails to
start, a failed scan, and cancellation.
