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

Queries contain 1–256 characters, radius is 1–17,000, limit is 1–100, and
coordinates must be valid latitude and longitude values.
