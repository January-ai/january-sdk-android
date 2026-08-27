# Food discovery and servings

Food integration is a sequence, not one interchangeable response type:

```text
autocomplete ── selection ──▶ search ── selected result ──▶ getFood
                                                           │
                                                           ▼
                                                   serving + quantity
                                                           │
                                                           ▼
                                                      FoodPortion
```

* `autocomplete` returns text-entry suggestions.
* Selecting a suggestion should populate the search box and run `search`.
* `search` returns discovery rows.
* Before showing servings, call `getFood` with the selected food ID.
* Build a `FoodPortion` from the hydrated food to validate the serving and scale
  nutrition locally.

```kotlin
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.portion

val results = january.foods.search(SearchFoodsRequest(query = "banana"))
val selected = results.items.first()
val food = january.foods.getFood(GetFoodRequest(foodId = selected.id))

val serving = food.servings.firstOrNull { it.isPrimary }
    ?: food.servings.first()
val portion = food.portion(servingId = serving.id, quantity = 1.5)

println(portion.nutrition.calories?.value)
val apiSelection = portion.selection
```

`portion.selection` is accepted by Food Logs and Glucose requests. Handle an
empty serving list and `FoodPortionException` as user-visible data errors.
