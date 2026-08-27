# Photo and barcode scanning

`PhotoScanImage` corrects EXIF orientation, preserves aspect ratio, limits the
longest edge to 1,000 pixels, and JPEG-compresses at quality 70 by default.

```kotlin
val request = ScanFoodPhotoRequest.fromImageData(imageBytes)
val scan = client.photoScanning.scan(request)
```

Correct a result with its current meal name and detections:

```kotlin
val corrected = client.photoScanning.correct(
    CorrectPhotoScanRequest(
        mealName = scan.mealName.orEmpty(),
        detections = scan.detections.orEmpty(),
        userInput = "Remove the fries",
    ),
)
```

For a ready-made Compose experience, use `JanuaryMealScanner`. It supports
photo and barcode modes and returns `JanuaryMealScannerResult.Meal` or
`JanuaryMealScannerResult.Barcode`.
