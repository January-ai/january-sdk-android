# Water and weight logs

Water intake and body weight are logged per end user, like food logs. These
examples use `user`, the scoped client from
[User identity and timezone](../concepts/user-context.md). The token needs the
`water_logs:read`, `water_logs:write`, `weight_logs:read`, and
`weight_logs:write` scopes ([scope table](https://docs.january.ai/rest-api/authentication#client-token-scopes)).

## Water

Log one amount at a time, in fluid ounces (1–811.5), milliliters (30–24,000),
or US cups (0.1–101.4; 1 cup = 8 fl oz). Keep the returned `id` if the user may
undo the entry:

```kotlin
import ai.january.partner.waterlogs.VolumeUnit
import ai.january.partner.waterlogs.WaterAmount
import java.time.OffsetDateTime

val glass = user.waterLogs.create(WaterAmount(8.0, VolumeUnit.FL_OZ))
user.waterLogs.delete(glass.id)

// Backdated two hours.
user.waterLogs.create(
    WaterAmount(500.0, VolumeUnit.ML),
    consumedAt = OffsetDateTime.now().minusHours(2).toString(),
)
```

`consumedAt` defaults to now and accepts an ISO 8601 date-time with any offset.

An end user's water is capped at 24 L (about 811 fl oz) per day. The cap counts
the UTC calendar day of `consumedAt`, whatever offset you send, while lists
group days in the client's timezone, so near midnight a listed day's total can
differ from what the cap counted. A log that would go over the cap fails with
`ErrorCategory.VALIDATION` and the code `daily_water_limit_exceeded`. Don't
retry it.

Creating a water log isn't idempotent. After a create times out, list the day
before retrying, or the water is recorded twice and counts twice toward the
cap. Deleting is idempotent: deleting an unknown or already-deleted log also
succeeds.

The list returns one total per local calendar day that has water logged, oldest
first, in the unit you ask for:

```kotlin
val days = user.waterLogs.list("2026-09-01", "2026-09-30", VolumeUnit.ML)
days.items.forEach { day -> println("${day.date}: ${day.total.value} ${day.total.unit.value}") }
```

## Weight

Log a measurement in pounds (10–1,000) or kilograms (4.5–453.6). It is stored
and returned in the unit you send. `Weight` and `WeightUnit` are in the
`glucose` package:

```kotlin
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit

val measurement = user.weightLogs.create(Weight(154.5, WeightUnit.POUNDS))
```

`measuredAt` works like `consumedAt`. Every measurement is kept, and the list
shows one weight per local day: the one with the latest `measuredAt`. A later
measurement on the same day replaces what that day shows; one backdated to
earlier that day doesn't.

```kotlin
val days = user.weightLogs.list("2026-09-01", "2026-09-30")
days.items.forEach { day -> println("${day.date}: ${day.weight.value} ${day.weight.unit.value}") }
```

Creating a weight log isn't idempotent either: a retried create records the
measurement again. Weight logs have no ID and can't be updated or deleted; to
change what a day shows, log a newer measurement for that day.

## Ranges

`start` and `end` are inclusive `YYYY-MM-DD` dates in the client's timezone
([Days](../concepts/user-context.md#days)), at most five years back. A range
further back fails with the code `date_range_too_large`. When more than 100
days in the range have entries, only the most recent 100 are returned; to read
older days, move `end` back. Days with nothing logged are absent, and an empty
list is a normal result.

To cover a longer period, such as a year for a chart, request consecutive
ranges of 90 days or fewer and join the results, as the example app does.
