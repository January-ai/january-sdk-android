# Models and enums

## Identifiers and context

`PartnerUserId(String)` rejects blank values. `FoodId(Long)` and
`ServingId(Long)` are typed value classes. `PartnerUserContext` contains a
required `endUserId` and optional IANA `timezone`.

## Food enums

* `FoodCategory`: `GENERAL`, `BRANDED`, `RECIPE`
* `AutocompleteFoodCategory`: `GENERAL`, `BRANDED`
* `DietPreference`: `VEGETARIAN`, `VEGAN`, `KETO`, `PALEO`, `PESCATARIAN`,
  `LOW_CARBOHYDRATE`, `HIGH_PROTEIN`, `KOSHER`, `HALAL`
* `DietRestriction`: gluten, lactose, yeast, tree nuts, peanuts, dairy, eggs,
  sulfites, soy, wheat, shellfish, fish, mushrooms, sesame, MSG, caffeine, and
  FODMAP enum cases.

`NutritionFacts` contains optional `NutrientAmount(value, unit)` values for
calories, protein, carbohydrates, net carbohydrates, fats, fiber, sugars,
cholesterol, calcium, iron, potassium, sodium, and vitamin D.

`FoodSelection(id, serving)` uses `ServingSelection(id, quantity)` and is the
input accepted by Food Logs and Glucose.

## Glucose profile

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

* `Sex`: `MALE`, `FEMALE`
* `HeightUnit`: `INCHES`, `CENTIMETERS`
* `WeightUnit`: `POUNDS`, `KILOGRAMS`
* `ActivityLevel`: `SEDENTARY`, `LIGHTLY_ACTIVE`, `MODERATELY_ACTIVE`, `VERY_ACTIVE`
* `MedicalCondition`: `TYPE_2_DIABETES`, `PREDIABETES`

The convenience profile constructor taking raw `height` and `weight` interprets
them as inches and pounds. User interfaces should explicitly support feet plus
inches/centimeters and pounds/kilograms, then create typed values.

## Errors

Network operations throw `JanuaryException(category, message, httpStatus,
cause)`. `ErrorCategory` cases are `VALIDATION`, `AUTHENTICATION`,
`AUTHORIZATION`, `NOT_FOUND`, `RATE_LIMITED`, `TIMEOUT`, `TRANSPORT`, `DECODING`,
and `SERVER`. Local argument APIs may throw `IllegalArgumentException`,
`FoodPortionException`, date parsing exceptions, or `NoBarcodeMatchException`.
Coroutine `CancellationException` propagates unchanged.
