# Restaurants

Search nearby restaurants or their menu items with the same location request:

```kotlin
val request = SearchRestaurantsRequest(
    query = "mediterranean",
    latitude = 37.7749,
    longitude = -122.4194,
    radius = 8_000.0,
    limit = 10,
)

val restaurants = client.restaurants.search(request)
val menuItems = client.restaurants.searchMenuItems(request.copy(query = "chicken"))
```

## Load one restaurant's menu

Use an ID returned by restaurant search to load that restaurant's menu without
repeating the query or coordinates:

```kotlin
var offset = 0
val limit = 100

do {
    val page = client.restaurants.getMenuItems(
        GetRestaurantMenuItemsRequest(
            restaurantId = restaurant.id,
            limit = limit,
            offset = offset,
        ),
    )

    consume(page.items)
    offset += page.items.size
} while (page.items.isNotEmpty() && offset < page.totalCount)
```

An unknown restaurant returns `404`. An existing restaurant with no menu
returns an empty `items` list.

Queries contain 1–256 characters, radius is 1–17,000, limit is 1–100, and
coordinates must be valid latitude and longitude values.
