# Glucose prediction

`user.glucose.predict` estimates the glucose curve a meal will produce, without
a sensor. The token needs the `glucose:read` scope. Predictions are
informational, not diagnosis or treatment guidance. This example uses `user`,
the scoped client from [User identity and timezone](../concepts/user-context.md),
and a `portion` from [Food discovery and servings](../concepts/food-lifecycle.md).

```kotlin
import ai.january.partner.glucose.ActivityLevel
import ai.january.partner.glucose.GlucoseImpact
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.Height
import ai.january.partner.glucose.HeightUnit
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.glucose.Sex
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import java.time.OffsetDateTime

val profile = GlucosePredictionProfile(
    age = 35.0,
    sex = Sex.MALE,
    height = Height(70.0, HeightUnit.INCHES),
    weight = Weight(175.0, WeightUnit.POUNDS),
    activityLevel = ActivityLevel.MODERATELY_ACTIVE,
    healthConditions = emptyList(),
)

val result = user.glucose.predict(
    PredictGlucoseRequest(
        userProfile = profile,
        foods = listOf(portion.selection),
        startTime = OffsetDateTime.now(),
    ),
)

result.prediction.forEach { point -> println("+${point.minutes} min: ${point.value} mg/dL") }
val impactLabel = when (result.impact) {
    GlucoseImpact.LOW -> "Low impact"
    GlucoseImpact.MEDIUM -> "Medium impact"
    GlucoseImpact.HIGH -> "High impact"
    else -> null // No grade.
}
```

`result.prediction` holds a point every 15 minutes: `minutes` after `startTime`
and the predicted `value` in mg/dL. `result.impact` is `GlucoseImpact.LOW`,
`MEDIUM`, or `HIGH`, or null when there's no grade. `result.chart.min` and
`max` are suggested Y-axis bounds in mg/dL (a target range, not the curve's
lowest and highest values); either can be null.

The prediction depends on the meal's local time of day, so the client's
timezone matters. Without one, the SDK sends UTC
([Timezone](../concepts/user-context.md#timezone)).

`age` is in whole years; a fractional age fails with `ErrorCategory.VALIDATION`
before anything is sent. Height takes inches or centimeters, and weight pounds
or kilograms. Show imperial height as feet plus inches, not one field of raw
inches.

To personalize the prediction, pass `cgmData` together with the
`consumedFoods` eaten during that period, covering at least five full days;
less is rejected. Leave both out for a standard prediction.
