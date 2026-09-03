# Compatibility and permissions

| Requirement | Current preview |
| --- | --- |
| Minimum Android | API 26 |
| Compile SDK used by source | 36 |
| Gradle | 9.5.1 |
| Android Gradle Plugin | 9.2.1 |
| Java source/target | 17 |
| Build system | Gradle Kotlin DSL |
| Network API | Kotlin `suspend` functions |
| Distribution | Maven Central (`ai.january:january-sdk-android:0.1.0`) |

The SDK manifest declares `android.permission.INTERNET`,
`android.permission.CAMERA`, and `android.permission.RECORD_AUDIO`. The camera permission is needed by
`JanuaryFoodScanner`, which uses CameraX for photo capture and barcode analysis.
The scanner requests runtime permission and provides denied/settings states.

`RECORD_AUDIO` enables `VoiceCaptureSession`. The host app owns the runtime
permission request and should launch it only after the user taps a microphone
control. Voice recognition uses the Android speech-recognition service and does
not send audio or transcripts to January.

If an application does not expose the scanner or voice capture, review the
merged manifest and its permission disclosure before release. The Apache-2.0 AAR includes
Compose, CameraX, ML Kit barcode scanning, Retrofit, Moshi, and coroutines.
