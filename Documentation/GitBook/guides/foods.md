# Foods

Use `client.foods` for autocomplete, search, full food hydration, barcode
lookup, natural-language meal parsing, and alternatives.

```kotlin
val suggestions = client.foods.autocomplete(AutocompleteFoodsRequest("ban"))
```

Selecting a suggestion should fill the search field and run `search`. Before
opening a serving picker, fetch the complete food because discovery responses
may not contain every serving:

```kotlin
val results = client.foods.search(SearchFoodsRequest("banana"))
val food = client.foods.getFood(GetFoodRequest(results.items.first().id))
val portion = food.portion(food.servings.first().id, quantity = 1.5)

println(portion.nutrition.calories?.value)
val selection = portion.selection
```

`portion` validates the serving and quantity and scales nutrients locally.
Barcode values use `lookupBarcode`; meal descriptions use
`searchNaturalLanguage`; dietary replacements use `suggestAlternatives`.
