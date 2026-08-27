# January Partner SDK for Android

Build food, nutrition, and metabolic experiences in native Android applications
with idiomatic Kotlin models, coroutines, and Jetpack Compose scanner UI.

The SDK covers food and restaurant discovery, meal-photo and barcode scanning,
food logs, and glucose prediction.

{% hint style="warning" %}
Never ship a long-lived January partner key in an Android app. Production apps
obtain short-lived client tokens from their own authenticated backend.
{% endhint %}

## Requirements

* Android API 26 or later
* Java 17 or later
* Kotlin coroutines

## Quick integration path

1. [Install the SDK](getting-started/installation.md).
2. [Implement a token provider](getting-started/authentication.md).
3. Create `JanuaryPartnerClient`.
4. Run a [food search](getting-started/quick-start.md).
5. Explore the [demo app](getting-started/example-app.md).

```kotlin
val january = JanuaryPartnerClient.withClientTokenProvider {
    partnerBackend.createJanuaryToken()
}
val results = january.foods.search(SearchFoodsRequest("greek yogurt"))
```
