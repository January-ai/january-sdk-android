# January Partner SDK for Android

Controlled-preview Kotlin SDK for January food discovery, restaurants, meal
scanning, food logs, and glucose prediction.

> **Distribution status:** `ai.january:partner-sdk:0.1.0` is not published to
> Maven Central, and the source repository is private. January must grant your
> GitHub account access before you can use the pinned composite-build workflow in the
> [installation guide](Documentation/GitBook/getting-started/installation.md).

## Documentation

The [Android SDK GitBook](Documentation/GitBook/README.md) covers installation,
backend token exchange, a complete token provider, first request, every resource,
permissions, retries, errors, testing, and troubleshooting.

## Evaluate the repository

```bash
git clone https://github.com/January-ai/partner-sdk-android.git
cd partner-sdk-android
git checkout a6c2dc225cb2908541e028ba9edcc588aaa151f2
./gradlew :sdk:testDebugUnitTest :sdk:assembleRelease
```

The repository contains:

* `sdk` — the Android library;
* `demo` — the Jetpack Compose example app;
* `Documentation/GitBook` — integration documentation.

## Authentication rule

Never ship a long-lived January partner key in an APK. A production app obtains
a short-lived client token from its own authenticated backend and supplies a
`JanuaryTokenProvider`. Start with the [backend token endpoint](Documentation/GitBook/getting-started/backend-token-endpoint.md).
