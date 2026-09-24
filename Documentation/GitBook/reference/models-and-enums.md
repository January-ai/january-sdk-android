# Models and enums

## Identifiers and context

`PartnerUserId(String)` rejects a blank value. `FoodId(String)` and
`ServingId(String)` are value classes; `.value` returns the string, and
`FoodId(Long)` and `ServingId(Long)` build one from a number.
`PartnerUserContext` has a required `endUserId` and an optional IANA
`timezone`.

## Foods

* `FoodCategory`: `GENERIC`, `BRANDED`, `RECIPE` (`GENERAL` is a deprecated
  alias of `GENERIC`)
* `AutocompleteFoodCategory`: `GENERIC`, `BRANDED` (`GENERAL` is a deprecated
  alias of `GENERIC`)
* `DietPreference`: `VEGETARIAN`, `VEGAN`, `KETO`, `PALEO`, `PESCATARIAN`,
  `LOW_CARBOHYDRATE`, `HIGH_PROTEIN`, `KOSHER`, `HALAL`
* `DietRestriction`: `GLUTEN`, `LACTOSE`, `YEAST`, `TREE_NUTS`, `PEANUTS`,
  `DAIRY`, `EGGS`, `SULFITES`, `SOY`, `WHEAT`, `SHELLFISH`, `FISH`,
  `MUSHROOMS`, `SESAME`, `MONOSODIUM_GLUTAMATE`, `CAFFEINE`, `FODMAPS`

`NutritionFacts` has optional `NutrientAmount(value, unit)` fields: `calories`,
`protein`, `carbohydrates`, `netCarbohydrates`, `totalFat`, `transFat`,
`saturatedFat`, `fiber`, `totalSugars`, `addedSugars`, `cholesterol`,
`calcium`, `iron`, `potassium`, `sodium`, and `vitaminD`.
`CompleteScanNutritionFacts`, used by food analysis and alternatives, has
`calories`, `protein`, `carbohydrates`, `netCarbohydrates`, `totalFat`,
`saturatedFat`, `fiber`, `totalSugars`, `addedSugars`, and `sodium`.

`FoodPortion` has `foodId`, `serving` (the `ServingOption`), `quantity`,
`nutrition` (scaled `NutritionFacts`), `totalWeightGrams`, `glycemicIndex`,
`glycemicLoad` (scaled), and `selection`.

`FoodSelection(id: String, serving: ServingSelection(id: String, quantity: Double))`
is what `foodLogs.create`, `foodLogs.update`, and `glucose.predict` take.

`ServingSummary(id, quantity, unit, weightGrams)` is the catalog serving on a
detected or alternative food; `quantity` is the size of one serving, and
`weightGrams` is its weight when the catalog knows it.

* `AnalysisEffort`: `NONE`, `XHIGH`

## Food logs

`FoodLog` has `id` (nullable; a log without one can't be fetched, updated, or
deleted), `foods: List<LoggedFood>`, `timestampUtc` (UTC), and optional `name`.

`LoggedFood` has `id` (the food ID), `name`, `brandName`, `imageUrl`,
`glycemicIndex`, `glycemicLoad`, `nutrients` (already scaled to the amount
eaten), `consumedServing`, and `servingDetails`:

* `ConsumedServing(id, quantity)`: the serving and how many of it were eaten.
* `ServingDetails(id, quantity, unit, weightGrams)`: that serving's definition.
  The amount eaten is `consumedServing.quantity × servingDetails.quantity`, in
  `servingDetails.unit`.

* `FoodLogSummaryGrouping`: `DAY`, `WEEK`
* `WeekStart`: `MONDAY`, `SUNDAY`

## Water and weight logs

* `VolumeUnit`: `FL_OZ`, `ML`, `CUP` (a US cup, 8 fl oz)

`WaterAmount(value, unit: VolumeUnit)` is the input to a water log and the
amount stored on a `WaterLog`; `Volume(value, unit)` is a daily total in the
requested unit. Weight logs use `Weight(value, unit: WeightUnit)` from the
`ai.january.partner.glucose` package. A unit this SDK version doesn't know is
reported as `ErrorCategory.DECODING`.

## Glucose

```kotlin
GlucosePredictionProfile(
    age: Double,
    sex: Sex,
    height: Height,
    weight: Weight,
    activityLevel: ActivityLevel? = null,
    healthConditions: List<MedicalCondition>? = null,
)
```

`age` is in whole years; a fractional value throws `JanuaryException` with
`ErrorCategory.VALIDATION`.

* `Sex`: `MALE`, `FEMALE`
* `HeightUnit`: `INCHES`, `CENTIMETERS`
* `WeightUnit`: `POUNDS`, `KILOGRAMS`
* `ActivityLevel`: `SEDENTARY`, `LIGHTLY_ACTIVE`, `MODERATELY_ACTIVE`, `VERY_ACTIVE`
* `MedicalCondition`: `TYPE_2_DIABETES`, `PREDIABETES`
* `GlucoseImpact`: `LOW`, `MEDIUM`, `HIGH`. It's a value class, so a grade
  this SDK version doesn't know keeps its string in `value`.

The older profile constructor that takes raw `height` and `weight` numbers reads
them as inches and pounds. Let users enter feet and inches or centimeters, and
pounds or kilograms, then build typed values.

`GlucosePrediction` has `prediction: List<GlucosePredictionPoint(minutes, value)>`,
`impact: GlucoseImpact?`, and `chart: GlucoseChart(min, max)`
([Glucose prediction](../guides/glucose-prediction.md)).

## Errors

`JanuaryException`, `ErrorCategory`, and the local exceptions are on
[Error handling](error-handling.md).
