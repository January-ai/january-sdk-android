# Food discovery and servings

Food integration is a sequence, not one interchangeable response type:

```text
autocomplete ── selection ──▶ search ── selected result ──▶ get
                                                           │
                                                           ▼
                                                   serving + quantity
                                                           │
                                                           ▼
                                                      FoodPortion
```

* `autocomplete` returns text-entry suggestions.
* Selecting a suggestion should fill the search box and run `search`.
* `search` returns discovery rows, which may not list every serving.
* Before showing servings, fetch the full food with `get`.
* Build a `FoodPortion` from the full food to validate the serving and scale
  nutrition locally.

```kotlin
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.portion

val results = user.foods.search(SearchFoodsRequest(query = "banana"))
val food = user.foods.get(GetFoodRequest(foodId = results.items.first().id))

// Without a serving ID, portion uses the primary serving (or the first one).
val portion = food.portion(quantity = 1.5)

println(portion.nutrition.calories?.value)
val selection = portion.selection
```

`portion.selection` is what `foodLogs.create` and `glucose.predict` take. Treat
an empty serving list or a `FoodPortionException` as a data error to show the
user ([Portion helper](../reference/foods-api.md#portion-helper)).
