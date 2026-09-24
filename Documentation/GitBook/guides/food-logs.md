# Food logs

Create a scoped client once so every Food Logs request reuses the same app-owned
user ID and IANA timezone:

```kotlin
val user = client.forUser(
    PartnerUserId(account.id),
    timezone = "America/New_York",
)

val log = user.foodLogs.create(
    foods = listOf(portion.selection),
    timestampUtc = Instant.now().toString(),
    name = "Breakfast",
)

val logs = user.foodLogs.list("2026-08-01", "2026-08-31")
val logId = requireNotNull(log.id) { "January did not return a Food Log ID." }
user.foodLogs.update(logId, name = "Post-workout breakfast")
user.foodLogs.delete(logId)
```

For a weekly or daily overview, ask for a summary instead of paging through
logs:

```kotlin
val summary = user.foodLogs.getSummary(
    "2026-09-01", "2026-09-30",
    groupBy = FoodLogSummaryGrouping.WEEK,
)
summary.buckets.forEach { week ->
    println("${week.startDate}: ${week.logsCount} logs, ${week.nutrients.calories?.value} kcal")
}
val dailyAverage = summary.averagePerLoggedDay.nutrients
```

List boundaries are inclusive calendar dates in the supplied timezone. The host
application owns and persists the identity; the SDK only applies it to requests.
