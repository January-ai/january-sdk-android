# January Partner SDK for Android

Build personalized nutrition experiences with the January Partner API. The
Android SDK provides idiomatic Kotlin models and coroutine-first APIs.

## Requirements

- Android API 26 or later
- Java 17 or later
- Kotlin coroutines

## Installation

Ensure Maven Central is available:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Add the SDK to your app module:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.january:partner-sdk:0.1.0")
}
```

## Quick start

```kotlin
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.JanuaryException
import ai.january.partner.PartnerUserId
import ai.january.partner.foods.SearchFoodsRequest

val january = JanuaryPartnerClient(developmentApiKey = apiKey)

val results = january.foods.search(
    SearchFoodsRequest(
        query = "greek yogurt",
        endUserId = PartnerUserId(userId),
    ),
)

results.items.forEach { food ->
    println(food.name)
}
```

Network operations are exposed as `suspend` functions and should be called from
a coroutine owned by your application, such as `viewModelScope`.

## API resources

- `foods` — food search, barcode lookup, natural-language search, and alternatives
- `restaurants` — restaurant and menu search
- `photoScanning` — meal-photo scanning and corrections
- `foodLogs` — create, retrieve, update, and delete food logs
- `glucose` — glucose prediction

## Error handling

SDK requests throw `JanuaryException`, which provides a category, message, and
HTTP status when available.

```kotlin
try {
    val results = january.foods.search(request)
    // Use results
} catch (error: JanuaryException) {
    println("${error.category}: ${error.message}")
}
```

## Authentication

Keep API credentials out of source control and application logs. Do not embed a
long-lived API key in a distributed application.

## Documentation

See the [January Partner API documentation](https://docs.january.ai/nutrition/apis/v1.2/).
