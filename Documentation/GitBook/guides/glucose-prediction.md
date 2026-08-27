# Glucose prediction

Use a scoped client and typed measurement units:

```kotlin
val profile = GlucosePredictionProfile(
    age = 35.0,
    sex = Sex.MALE,
    height = Height(70.0, HeightUnit.INCHES),
    weight = Weight(175.0, WeightUnit.POUNDS),
    activityLevel = ActivityLevel.MODERATELY_ACTIVE,
    healthConditions = emptyList(),
)

val prediction = user.glucose.predict(
    PredictGlucoseRequest(
        userProfile = profile,
        foods = listOf(portion.selection),
        startTime = OffsetDateTime.now(),
    ),
)
```

Height accepts inches or centimeters; weight accepts pounds or kilograms. A UI
should display imperial height as feet plus inches, not one raw-inch field.
Predictions are informational and are not diagnosis or treatment guidance.
