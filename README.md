# January SDK for Android

Official Kotlin SDK for January food discovery, restaurants, meal scanning,
food logs, and glucose prediction.

## Documentation

The [Android SDK GitBook](https://docs.january.ai/android-sdk) covers installation,
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
`JanuaryTokenProvider`. Start with the [backend token endpoint](https://docs.january.ai/android-sdk/getting-started/backend-token-endpoint).

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

## License

The Apache 2.0 license applies to the source code in this repository. It does not grant rights to nutrition data, food images, or other content returned by the January API, which are subject to the January API Developer Terms.
