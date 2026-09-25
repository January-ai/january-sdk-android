# Restaurants

Search nearby restaurants or their menu items with the same location request.
These examples use `user`, the scoped client from
[User identity and timezone](../concepts/user-context.md).

The SDK doesn't read the device location or declare a location permission.
Your app supplies the coordinates, for example from the fused location provider
after the user grants `ACCESS_COARSE_LOCATION`, or from a place the user picks.

```kotlin
import ai.january.partner.restaurants.SearchRestaurantsRequest

val request = SearchRestaurantsRequest(
    query = "mediterranean",
    latitude = 37.7749,
    longitude = -122.4194,
    radius = 8_000.0,
    limit = 10,
)

val restaurants = user.restaurants.search(request)
val menuItems = user.restaurants.searchMenuItems(request.copy(query = "chicken"))
```

Queries contain 1–256 characters, radius is 1–50,000 meters (8,000 by default),
limit is 1–100, and coordinates must be valid latitude and longitude values.

## Load one restaurant's menu

Use an ID from restaurant search to load that restaurant's menu without
repeating the query or coordinates:

```kotlin
import ai.january.partner.restaurants.GetRestaurantMenuItemsRequest

var offset = 0
val limit = 100

do {
    val page = user.restaurants.getMenuItems(
        GetRestaurantMenuItemsRequest(
            restaurantId = restaurant.id,
            limit = limit,
            offset = offset,
        ),
    )

    consume(page.items)
    offset += page.items.size
} while (page.items.size == limit)
```

The response contains only `items`, with no `totalCount`, so keep paging while
a full page comes back. An empty page ends the menu, including for a restaurant
with no menu on record. An unknown restaurant fails with
`ErrorCategory.NOT_FOUND`. In a discovery UI, fall back to `searchMenuItems`
with the user's original query and location when the first page is empty or
the restaurant is not found.
