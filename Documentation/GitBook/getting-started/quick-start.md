# Quick start

Create a provider-backed client inside your application's dependency graph:

```kotlin
val january = JanuaryPartnerClient.withClientTokenProvider {
    partnerBackend.createJanuaryToken()
}
```

Call suspend APIs from an app-owned coroutine such as `viewModelScope`:

```kotlin
val results = january.foods.search(
    SearchFoodsRequest(
        query = "greek yogurt",
        category = FoodCategory.BRANDED,
        limit = 10,
    ),
)

results.items.forEach { food -> println(food.name) }
```

Search queries contain 1–256 characters and limits are 1–40. Catch
`JanuaryException` to handle SDK failures.
