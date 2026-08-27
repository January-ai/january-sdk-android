# Food Logs and Glucose API

Prefer `client.forUser(...)` so one identity and timezone are reused.

## Scoped Food Logs

```kotlin
suspend fun create(
    foods: List<FoodSelection>,
    timestampUtc: String? = null,
    name: String? = null,
): FoodLog
suspend fun list(start: String, end: String): ListFoodLogsResponse
suspend fun update(
    id: String,
    foods: List<FoodSelection>? = null,
    timestampUtc: String? = null,
    name: String? = null,
): FoodLog
suspend fun delete(id: String): DeleteFoodLogResponse
```

`timestampUtc` is an ISO-8601 offset date-time. `start` and `end` are ISO dates
(`YYYY-MM-DD`) and are inclusive calendar boundaries in the scoped timezone.
Log IDs must be UUID strings. `FoodLog` contains `id`, `foods`, `timestampUtc`,
and optional `name`; list returns `totalCount` and items; delete returns `status`.

The unscoped `client.foodLogs` has corresponding request-object methods:

```kotlin
suspend fun create(request: CreateFoodLogRequest): FoodLog
suspend fun list(request: ListFoodLogsRequest): ListFoodLogsResponse
suspend fun update(request: UpdateFoodLogRequest): FoodLog
suspend fun delete(request: DeleteFoodLogRequest): DeleteFoodLogResponse
```

Each request requires `user: PartnerUserContext`; other fields match the scoped
signatures.

## Glucose

```kotlin
suspend fun predict(request: PredictGlucoseRequest): GlucosePrediction
```

`PredictGlucoseRequest` fields:

| Field | Type and default |
| --- | --- |
| `userProfile` | `GlucosePredictionProfile`, required |
| `foods` | `List<FoodSelection>`, required |
| `startTime` | `OffsetDateTime`, required |
| `cgmData` | `List<CgmReading>? = null` |
| `consumedFoods` | `List<ConsumedHistoricalFood>? = null` |
| `endUserId` | `PartnerUserId? = null` |
| `timezone` | `String? = null` |

`user.glucose.predict(request)` replaces the request's identity and timezone
with the scoped context. The response contains prediction points (`minutes`,
`value`), a `GlucoseImpact`, and chart `min`/`max`.
