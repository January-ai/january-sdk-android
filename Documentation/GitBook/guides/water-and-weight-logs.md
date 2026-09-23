# Water and weight logs

Water intake and body weight are logged per end user, like food logs. Create a
scoped client once so every request reuses the same app-owned user ID and IANA
timezone:

```kotlin
val user = client.forUser(
    PartnerUserId(account.id),
    timezone = "America/New_York",
)
```

## Water

Log one amount at a time, in fluid ounces (1–811.5), milliliters (30–24,000),
or cups (0.1–101.4; 1 cup = 8 fl oz). Keep the returned `id` if the user may
undo the entry:

```kotlin
val glass = user.waterLogs.create(WaterAmount(8.0, VolumeUnit.FL_OZ))
user.waterLogs.delete(glass.id)
```

`consumedAt` defaults to now; pass an ISO-8601 offset date-time to backdate an
entry. Its calendar day, in the scoped timezone, is the one the daily cap counts
it against: an end user's total is capped at 24 L (about 811 fl oz) per day, and
a log that would exceed it fails with `ErrorCategory.VALIDATION` and the error
code `daily_water_limit_exceeded`.

Browse totals rather than individual entries. The list returns one total per
local calendar day that has water logged, oldest first, in the unit you ask for:

```kotlin
val days = user.waterLogs.list("2026-09-01", "2026-09-30", VolumeUnit.ML)
days.items.forEach { day -> println("${day.date}: ${day.total.value} ${day.total.unit}") }
```

## Weight

Log a measurement in pounds or kilograms; it is stored and returned in the unit
you send:

```kotlin
val measurement = user.weightLogs.create(Weight(154.5, WeightUnit.POUNDS))
```

Every measurement is kept. Listing shows one weight per day, the latest by
`measuredAt`, so logging again later the same day replaces what that day shows:

```kotlin
val days = user.weightLogs.list("2026-09-01", "2026-09-30")
days.items.forEach { day -> println("${day.date}: ${day.weight.value} ${day.weight.unit.value}") }
```

Creating a weight log is not idempotent: a retried create records the weight
again. Deleting weight logs is not part of the API.

## Ranges

`start` and `end` are inclusive `YYYY-MM-DD` dates in the scoped timezone, at
most five years back; a range further back fails with the error code
`date_range_too_large`. When more than 100 days in the range have entries, the
most recent 100 are returned. Days with nothing logged are absent, and an empty
list is a normal result.
