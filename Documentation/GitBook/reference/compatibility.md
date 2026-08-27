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
| Distribution | Pinned composite source build |

The SDK manifest declares `android.permission.INTERNET` and
`android.permission.CAMERA`. The camera permission is needed by
`JanuaryMealScanner`, which uses CameraX for photo capture and barcode analysis.
The scanner requests runtime permission and provides denied/settings states.

If an application does not expose the scanner, review the merged manifest and
its permission disclosure before release. The controlled-preview AAR includes
Compose, CameraX, ML Kit barcode scanning, Retrofit, Moshi, and coroutines.
