# User identity and timezone

A client token is bound to one end user. Your app owns that identity: use the
stable, opaque ID your own system has for the account (at most 64 characters),
never an email address or name. `PartnerUserId` rejects a blank ID but doesn't
check the length. The SDK doesn't persist the ID.

After sign-in, create one scoped client and use it for every resource:
`foods`, `restaurants`, `foodAnalysis`, `foodLogs`, `waterLogs`, `weightLogs`,
and `glucose`.

```kotlin
import ai.january.partner.PartnerUserId
import ai.january.partner.foods.SearchFoodsRequest
import java.time.ZoneId

val user = january.forUser(
    endUserId = PartnerUserId(account.id),
    timezone = ZoneId.systemDefault().id,
)

val foods = user.foods.search(SearchFoodsRequest(query = "banana"))
```

When the account signs out or switches, create a new `JanuaryPartnerClient`,
not just a new scope ([Client lifecycle](client-lifecycle.md)).

With a client token, the token decides the user: the SDK removes
`January-End-User-ID` from January requests. With a development API key, the
food, water, and weight log operations send the scoped ID in that header.

## Timezone

`timezone` is an IANA ID. It sets the calendar days for food-log lists and
summaries and for water and weight lists, and it is sent with glucose
predictions. Without one, the Android SDK uses UTC (the iOS SDK uses the device
timezone), so an 8 p.m. dinner in Los Angeles lands on the next day. Pass the
device timezone, as above.

## Days

Each log stores an instant: `timestampUtc`, `consumedAt`, or `measuredAt`
(`created_at` in the REST API). Lists and summaries sort those instants into
calendar days in the scoped timezone when you read them
([Days and timezones](https://docs.january.ai/rest-api/api-overview#days-and-timezones)). So:

* Build `YYYY-MM-DD` dates in that same timezone.
* To backdate an entry to a local day, send a time on that day with its offset.
* When the device timezone changes (`Intent.ACTION_TIMEZONE_CHANGED`), create a
  new scoped client. The same entries can then fall on a different day.

```kotlin
import ai.january.partner.waterlogs.VolumeUnit
import ai.january.partner.waterlogs.WaterAmount
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

val zone = ZoneId.of(user.context.timezone ?: "UTC")
val today = LocalDate.now(zone)
val lastWeek = user.foodLogs.list(today.minusDays(6).toString(), today.toString())

val yesterday = today.minusDays(1)
    .atTime(LocalTime.now(zone)).atZone(zone).toOffsetDateTime().toString()
user.waterLogs.create(WaterAmount(250.0, VolumeUnit.ML), consumedAt = yesterday)
```

The 24 L daily water cap is the exception: it counts the UTC day
([Water](../guides/water-and-weight-logs.md#water)).
