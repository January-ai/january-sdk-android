# Food, Water and Weight Logs and Glucose API

Prefer `client.forUser(...)` so one identity and timezone are reused across all
SDK resources, including the Food Logs, Water Logs, Weight Logs, and Glucose
operations documented here.

## Scoped Food Logs

```kotlin
suspend fun create(
    foods: List<FoodSelection>,
    timestampUtc: String? = null,
    name: String? = null,
): FoodLog
suspend fun list(start: String, end: String): ListFoodLogsResponse
suspend fun getSummary(
    start: String,
    end: String,
    groupBy: FoodLogSummaryGrouping = FoodLogSummaryGrouping.DAY,
    weekStart: WeekStart = WeekStart.MONDAY,
): FoodLogSummary
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

`getSummary` aggregates the logs in the inclusive range (at most 366 days) into
`buckets`, one per local calendar day or per week, each with `logsCount`,
`daysWithLogs`, and summed `nutrients`. Empty periods are still returned with
zero counts. `totals` covers the whole range and `averagePerLoggedDay` divides
the totals by the number of days that have a log. `nutrients` is sparse: read
`logsCount` to tell an empty bucket from one whose logs had no nutrition data.

The unscoped `client.foodLogs` has corresponding request-object methods:

```kotlin
suspend fun create(request: CreateFoodLogRequest): FoodLog
suspend fun list(request: ListFoodLogsRequest): ListFoodLogsResponse
suspend fun getSummary(request: GetFoodLogSummaryRequest): FoodLogSummary
suspend fun update(request: UpdateFoodLogRequest): FoodLog
suspend fun delete(request: DeleteFoodLogRequest): DeleteFoodLogResponse
```

Each request requires `user: PartnerUserContext`; other fields match the scoped
signatures.

## Scoped Water Logs

```kotlin
suspend fun create(amount: WaterAmount, consumedAt: String? = null): WaterLog
suspend fun list(start: String, end: String, unit: VolumeUnit): ListWaterLogsResponse
suspend fun delete(id: String): DeleteWaterLogResponse
```

`WaterAmount(value, unit)` is 1–811.5 `VolumeUnit.FL_OZ`, 0.125–101.4
`VolumeUnit.CUP`, or 30–24000 `VolumeUnit.ML`; an end user's total is capped at 24 L per day. `consumedAt`
is an ISO-8601 offset date-time and defaults to now. `WaterLog` contains `id`,
`amount` (as logged), and `consumedAt` in UTC. `list` returns `items`, one
`DailyWaterTotal(date, total: Volume)` per local calendar day that has water
logged, oldest first, in the requested `unit`, rounded to one decimal place.
`delete` succeeds for an unknown or already-deleted log too. The unscoped
`client.waterLogs` takes `CreateWaterLogRequest`, `ListWaterLogsRequest`, and
`DeleteWaterLogRequest`, each with `user: PartnerUserContext`.

## Scoped Weight Logs

```kotlin
suspend fun create(weight: Weight, measuredAt: String? = null): WeightLog
suspend fun list(start: String, end: String): ListWeightLogsResponse
```

`Weight(value, unit)` is 10–1000 `WeightUnit.POUNDS` or 4.5–453.6
`WeightUnit.KILOGRAMS`, stored and returned in the unit it was sent in.
`measuredAt` is an ISO-8601 offset date-time and defaults to now. `WeightLog`
contains `weight` and `measuredAt` in UTC. `list` returns `items`, one
`DailyWeight(date, weight)` per local calendar day that has a weight, oldest
first; when several were logged on one day, the latest by `measuredAt` is
returned. The unscoped `client.weightLogs` takes `CreateWeightLogRequest` and
`ListWeightLogsRequest`, each with `user: PartnerUserContext`.

For both, `start` and `end` are inclusive `YYYY-MM-DD` dates in the scoped
timezone and at most five years back (`date_range_too_large` otherwise); when
more than 100 days in the range have entries, the most recent 100 are returned.

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
