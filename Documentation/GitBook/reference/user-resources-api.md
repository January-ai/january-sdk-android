# Logs and glucose API

The scoped client applies one identity and timezone to every call on this page
([User identity and timezone](../concepts/user-context.md)).

## Scoped food logs

```kotlin
suspend fun create(
    foods: List<FoodSelection>,
    timestampUtc: String? = null,
    name: String? = null,
): FoodLog
suspend fun list(start: String, end: String): ListFoodLogsResponse
suspend fun get(id: String): FoodLog
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

`timestampUtc` is an ISO 8601 date-time with any offset and defaults to now.
`start` and `end` are inclusive `YYYY-MM-DD` dates in the client's timezone;
`list` spans at most 60 days. Log IDs are UUID strings. `update` sends only the
fields you set; with none set, it throws `ErrorCategory.VALIDATION` before any
request. `create` isn't idempotent ([Food logs](../guides/food-logs.md)).

`FoodLog` has a nullable `id`, `foods`, `timestampUtc` (UTC), and optional
`name` ([Models and enums](models-and-enums.md#food-logs)). `list` returns
`items` and `totalCount`, the number of logs returned. `get` returns one
`FoodLog`. `delete` returns `Unit` (`DeleteFoodLogResponse` is an alias for it).

`getSummary` groups the logs in the inclusive range (at most 366 days) into
`buckets`, one per local calendar day or week, each with `startDate`,
`endDate`, `logsCount`, `daysWithLogs`, and summed `nutrients`. Empty periods
are included with zero counts. `totals` covers the whole range, and
`averagePerLoggedDay` divides the totals by the number of days that have a log.
`nutrients` is sparse: read `logsCount` to tell an empty bucket from one whose
logs had no nutrition data.

The unscoped `january.foodLogs` has matching request-object methods:

```kotlin
suspend fun create(request: CreateFoodLogRequest): FoodLog
suspend fun list(request: ListFoodLogsRequest): ListFoodLogsResponse
suspend fun get(request: GetFoodLogRequest): FoodLog
suspend fun getSummary(request: GetFoodLogSummaryRequest): FoodLogSummary
suspend fun update(request: UpdateFoodLogRequest): FoodLog
suspend fun delete(request: DeleteFoodLogRequest): DeleteFoodLogResponse
```

Each request takes `user: PartnerUserContext`; the other fields match the
scoped signatures.

## Scoped water logs

```kotlin
suspend fun create(amount: WaterAmount, consumedAt: String? = null): WaterLog
suspend fun list(start: String, end: String, unit: VolumeUnit): ListWaterLogsResponse
suspend fun delete(id: String): DeleteWaterLogResponse
```

`WaterAmount(value, unit)` is 1–811.5 `VolumeUnit.FL_OZ`, 0.1–101.4
`VolumeUnit.CUP`, or 30–24,000 `VolumeUnit.ML`. An end user's total is capped at
24 L per UTC day ([Water](../guides/water-and-weight-logs.md#water)).
`consumedAt` is an ISO 8601 date-time with any offset and defaults to now.
`create` isn't idempotent. `WaterLog` has `id`, `amount` (as logged), and
`consumedAt` in UTC. `list` returns `items`, one
`DailyWaterTotal(date, total: Volume)` per local calendar day that has water
logged, oldest first, in the requested `unit`, rounded to one decimal place.
`delete` also succeeds for an unknown or already-deleted log. The unscoped
`january.waterLogs` takes `CreateWaterLogRequest`, `ListWaterLogsRequest`, and
`DeleteWaterLogRequest`, each with `user: PartnerUserContext`.

## Scoped weight logs

```kotlin
suspend fun create(weight: Weight, measuredAt: String? = null): WeightLog
suspend fun list(start: String, end: String): ListWeightLogsResponse
```

`Weight(value, unit)` is 10–1,000 `WeightUnit.POUNDS` or 4.5–453.6
`WeightUnit.KILOGRAMS`, stored and returned in the unit it was sent in.
`measuredAt` is an ISO 8601 date-time with any offset and defaults to now.
`create` isn't idempotent. `WeightLog` has `weight` and `measuredAt` in UTC.
`list` returns `items`, one `DailyWeight(date, weight)` per local calendar day
that has a weight, oldest first; when several were logged on one day, the one
with the latest `measuredAt` is returned. Weight logs have no ID and can't be
updated or deleted. The unscoped `january.weightLogs` takes
`CreateWeightLogRequest` and `ListWeightLogsRequest`, each with
`user: PartnerUserContext`.

For water and weight, `start` and `end` are inclusive `YYYY-MM-DD` dates in the
client's timezone, at most five years back (`date_range_too_large` otherwise).
When more than 100 days in the range have entries, the most recent 100 are
returned.

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
| `cgmData` | `List<CgmReading>? = null`; send with `consumedFoods` |
| `consumedFoods` | `List<ConsumedHistoricalFood>? = null`; send with `cgmData` |
| `endUserId` | `PartnerUserId? = null` |
| `timezone` | `String? = null` |

`user.glucose.predict(request)` replaces the request's `endUserId` and
`timezone` with the scoped context's. With no timezone, the SDK sends `UTC`.
The response has `prediction` (points every 15 minutes, each with `minutes`
after `startTime` and `value` in mg/dL), a nullable `impact`
(`GlucoseImpact`), and `chart` with suggested Y-axis bounds `min` and `max`
([Glucose prediction](../guides/glucose-prediction.md)).
