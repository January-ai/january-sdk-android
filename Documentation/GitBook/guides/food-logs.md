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
user.foodLogs.update(log.id, name = "Post-workout breakfast")
user.foodLogs.delete(log.id)
```

List boundaries are inclusive calendar dates in the supplied timezone. The host
application owns and persists the identity; the SDK only applies it to requests.
