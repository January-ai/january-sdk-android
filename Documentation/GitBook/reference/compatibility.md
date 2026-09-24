# Compatibility and permissions

## Requirements

| Requirement | Value |
| --- | --- |
| Minimum Android | API 24 |
| Compile SDK | 36 or later |
| Core library desugaring | Required in every app that uses the SDK, whatever its `minSdk`, with `desugar_jdk_libs` 2.1.5 or later ([Installation](../getting-started/installation.md)) |
| Java | 17 source and target |
| Kotlin | 2.1 or later. AGP 9 compiles Kotlin itself; with AGP 8, apply the `org.jetbrains.kotlin.android` plugin. |
| Distribution | Maven Central, `ai.january:january-sdk-android:0.3.1` |

The SDK is verified with Gradle 9.5.1 and Android Gradle Plugin 9.2.1.

## Libraries

The SDK brings in these libraries. Gradle raises your app's versions to at
least these:

| Library | Version |
| --- | --- |
| Jetpack Compose BOM | 2026.06.00 |
| CameraX | 1.6.1 |
| ML Kit barcode scanning | 17.3.0 |
| Activity Compose | 1.12.4 |
| Lifecycle runtime Compose | 2.10.0 |
| Retrofit | 3.0.0 |
| Moshi | 1.15.2 |
| Kotlin standard library and reflection | 2.2.10 |

Coroutines (`kotlinx-coroutines-core` 1.10.2) are a runtime dependency only, so
add `kotlinx-coroutines-android` to your app to call the SDK
([Installation](../getting-started/installation.md)).

## Permissions

The SDK's manifest adds three permissions to your app:

* `INTERNET`, for every January request.
* `CAMERA`, for `JanuaryFoodScanner`, which uses CameraX for photos and ML Kit
  for barcodes. The scanner requests it at runtime and shows its own denied and
  settings states.
* `RECORD_AUDIO`, for `VoiceCaptureSession`. Your app requests it at runtime,
  after the user taps a microphone control. Recognition uses Android's speech
  recognition service; the SDK doesn't send audio or transcripts to January or
  keep an audio file.

If your app doesn't use the scanner or voice capture, remove the permission it
doesn't need in your app's manifest:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.RECORD_AUDIO" tools:node="remove" />
    <uses-permission android:name="android.permission.CAMERA" tools:node="remove" />
</manifest>
```

The SDK is licensed under Apache-2.0.
