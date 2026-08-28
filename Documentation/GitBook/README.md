# January SDK for Android

Build food discovery, meal scanning, food logging, and glucose-prediction
experiences in native Android apps with Kotlin coroutines and typed models.

{% hint style="warning" %}
**Controlled preview:** no Android artifact is published yet, and the source
repository is private. January must grant your GitHub account access. The coordinate
`ai.january:january-sdk-android:0.1.0` is not available from Maven Central. Follow the
verified [source checkout installation](getting-started/installation.md); do not
use a Maven-only snippet until January announces a published release.
{% endhint %}

## Start here

1. [Install the controlled-preview source build](getting-started/installation.md).
2. Build an authenticated [backend token endpoint](getting-started/backend-token-endpoint.md).
3. Implement `JanuaryTokenProvider` in the app.
4. Run the [first food search](getting-started/quick-start.md).
5. Follow the [food hydration and serving flow](concepts/food-lifecycle.md).

## Security model

Public SDK authentication uses client tokens only. The Android app calls your
authenticated backend for a short-lived token and sends that token directly to
January. The SDK never knows your token endpoint URL and never accepts a public
base-URL override.

```text
Android app ── authenticated request ──▶ Partner backend
                                            │
                                            │ private token issuance
                                            ▼
                                       January token exchange
                                            │
Android app ◀──── { token, expiresIn } ─────┘
     │
     └──── Authorization: Bearer ct-… ──▶ January Partner API
```

## Requirements

* Android API 26+ with compile SDK 36
* Gradle 9.5.1 and Android Gradle Plugin 9.2.1 for the current source checkout
* Java 17
* Kotlin coroutines
* A GitHub account authorized for the private SDK repository

The [example app](getting-started/example-app.md) demonstrates all public
resources and the native camera scanner.
