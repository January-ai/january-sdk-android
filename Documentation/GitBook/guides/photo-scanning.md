# Photo and barcode scanning

## Prepare and scan existing image data

`PhotoScanImage` corrects EXIF orientation, preserves aspect ratio, limits the
longest edge to 1,000 pixels, and JPEG-compresses at quality 70 by default:

```kotlin
import ai.january.partner.photos.PhotoScanImage
import ai.january.partner.photos.ScanFoodPhotoRequest

val imageDataUri = PhotoScanImage.dataUri(originalImageBytes)
val scan = january.foodAnalysis.analyzePhoto(
    ScanFoodPhotoRequest(image = imageDataUri),
)
```

Correct a result using its current name and detections:

```kotlin
import ai.january.partner.photos.CorrectPhotoScanRequest

val corrected = january.foodAnalysis.correct(
    CorrectPhotoScanRequest(
        mealName = scan.mealName.orEmpty(),
        detections = scan.detections.orEmpty(),
        userInput = "Remove the fries",
    ),
)
```

## Native scanner UI

`JanuaryFoodScanner` is a full-screen Compose camera experience with photo and
barcode modes:

```kotlin
JanuaryFoodScanner(
    client = january,
    onResult = { result -> handleScannerResult(result) },
    onCancel = { navigator.popBackStack() },
)
```

The SDK manifest declares `CAMERA`, and the scanner requests the runtime camera
permission. It uses CameraX and ML Kit barcode analysis; it is not a system
photo-picker-only flow. Test permission denied, permanent denial/settings,
camera startup failure, scan failure, and cancellation.

Do not send meal images or inferred nutrition to general analytics or logs.
