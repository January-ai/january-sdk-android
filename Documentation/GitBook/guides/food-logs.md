# Food logs

These examples use `user`, the scoped client from
[User identity and timezone](../concepts/user-context.md), and a `portion` from
[Food discovery and servings](../concepts/food-lifecycle.md).

```kotlin
import java.time.OffsetDateTime

val log = user.foodLogs.create(
    foods = listOf(portion.selection),
    name = "Breakfast",
)

// Backdated three hours.
val snack = user.foodLogs.create(
    foods = listOf(portion.selection),
    timestampUtc = OffsetDateTime.now().minusHours(3).toString(),
    name = "Snack",
)

val logs = user.foodLogs.list("2026-08-01", "2026-08-31")
val logId = requireNotNull(log.id) { "January did not return a food log ID." }
user.foodLogs.update(logId, name = "Post-workout breakfast")
user.foodLogs.delete(logId)
```

`timestampUtc` defaults to now and accepts an ISO 8601 date-time with any
offset; January returns it in UTC. `list` takes inclusive `YYYY-MM-DD` dates in
the client's timezone, built in that same timezone
([Days](../concepts/user-context.md#days)), and spans at most 60 days.

Creating a log isn't idempotent. After a create times out, list the day before
retrying, or the meal may be logged twice.

`update` sends only the fields you pass and needs at least one. `delete`
returns `Unit`. Each log's `foods` are `LoggedFood` values whose nutrients are
already scaled to the amount eaten ([Models and enums](../reference/models-and-enums.md#food-logs)).

A malformed date or date-time throws `DateTimeParseException`, and a log ID
that isn't a UUID throws `IllegalArgumentException`, before any request is
sent. Neither is a `JanuaryException`
([Error handling](../reference/error-handling.md#errors-thrown-before-a-request)).

## Summaries

For a daily or weekly overview, ask for a summary instead of paging through
logs:

```kotlin
import ai.january.partner.foodlogs.FoodLogSummaryGrouping

val summary = user.foodLogs.getSummary(
    "2026-09-01", "2026-09-30",
    groupBy = FoodLogSummaryGrouping.WEEK,
)
summary.buckets.forEach { week ->
    val calories = week.nutrients.calories
    println("${week.startDate}: ${week.logsCount} logs, ${calories?.value} ${calories?.unit}")
}
val dailyAverage = summary.averagePerLoggedDay.nutrients
```

A summary spans at most 366 days, with a bucket for every day or week in the
range, including empty ones. `nutrients` is sparse, so read `logsCount` to tell
an empty bucket from one whose logs had no nutrition data.
