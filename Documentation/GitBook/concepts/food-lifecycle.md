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

// Without a serving ID, portion uses the primary serving (or the first one);
// without a quantity, it's one serving.
val portion = food.portion()

println(portion.nutrition.calories?.value)
val selection = portion.selection

// With a "6 oz" primary serving: 9 oz, one and a half servings.
val larger = food.portion(quantity = 9.0)
```

`portion.selection` is what `foodLogs.create` and `glucose.predict` take. Treat
an empty serving list or a `FoodPortionException` as a data error to show the
user ([Portion helper](../reference/foods-api.md#portion-helper)).

## Quantity and servings

`quantity` is an amount in the serving's unit, not a number of servings. For a
"6 oz" serving (`quantity` 6.0, `unit` "oz"), `quantity = 9.0` means 9 oz, and
leaving `quantity` out means one serving (6 oz). `portion.selection` sends the
number of servings, `quantity` divided by the serving's own quantity, so 9 oz
of a "6 oz" serving is logged as 1.5 servings. Versions before 0.3.2 sent
`quantity` itself as the number of servings, so food logs and glucose
predictions got the wrong amount (a default "6 oz" portion was logged as 6
servings); upgrade to 0.3.2.
