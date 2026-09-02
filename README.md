# January SDK for Android

Official Kotlin SDK for January food discovery, restaurants, meal scanning,
food logs, and glucose prediction.

## Documentation

The [Android SDK GitBook](https://docs.january.ai/android-sdk/android-sdk) covers installation,
backend token exchange, a complete token provider, first request, every resource,
permissions, retries, errors, testing, and troubleshooting.

## Install from source

```bash
git clone https://github.com/January-ai/january-sdk-android.git
cd january-sdk-android
./gradlew :sdk:testDebugUnitTest :sdk:assembleRelease
```

The repository contains:

* `sdk` — the Android library;
* `demo` — the Jetpack Compose example app;
* `Documentation/GitBook` — integration documentation.

## Authentication rule

Public SDK authentication uses short-lived client tokens only. An app obtains
its token from its own authenticated backend and supplies a
`JanuaryTokenProvider`. Start with the [backend token endpoint](https://docs.january.ai/android-sdk/android-sdk/getting-started/backend-token-endpoint).

## Set the active user once

Create one lightweight scoped client after authentication and use it across
every resource:

```kotlin
val user = january.forUser(
    endUserId = PartnerUserId(account.stableId),
    timezone = "America/New_York",
)

val foods = user.foods.search(SearchFoodsRequest(query = "banana"))
val logs = user.foodLogs.list(start = "2026-08-01", end = "2026-08-31")
```

The scoped client exposes Foods, Restaurants, Photo Scanning, Food Logs, and
Glucose. Recreate it when the signed-in account changes.

## Menu items by restaurant ID

Use the ID of a `restaurant` search result to load its menu, independently of search text and location.

```kotlin
val page = client.restaurants.getMenuItems(GetRestaurantMenuItemsRequest(restaurantId = restaurant.id, limit = 100, offset = 0))
```

The response contains `items` and `totalCount` (`total_count` on the wire). Request subsequent pages by advancing `offset` by the number of items received, until it reaches the total or a page is empty. An unknown restaurant returns 404; an existing restaurant with no menu returns an empty list.

## Add voice input

`VoiceCaptureSession` wraps Android speech recognition for search, chat, and
form inputs. It publishes listening/processing state, normalized microphone
level, partial text, a final transcript, duration, and stable errors:

```kotlin
val voiceCapture = VoiceCaptureSession(context)
if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
    PackageManager.PERMISSION_GRANTED
) {
    try {
        voiceCapture.startListening()
        // Render voiceCapture.state and voiceCapture.audioLevel.
        voiceCapture.stopListening()
    } catch (error: VoiceCaptureException) {
        // Present error.code through your app's voice-input UI.
    }
}
```

The SDK manifest declares `RECORD_AUDIO`; the host app must request runtime
permission after the user taps its microphone control. See the
[voice capture guide](Documentation/GitBook/guides/voice-capture.md).

## License

The Apache 2.0 license applies to the source code in this repository. It does not grant rights to nutrition data, food images, or other content returned by the January API, which are subject to the January API Developer Terms.
