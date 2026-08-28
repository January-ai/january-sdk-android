# User identity and timezone

January client tokens are bound to one end user. The host application owns that
identity and must keep it stable across sessions. Never use an email address or
other directly identifying value when an opaque internal account ID is
available.

Create one lightweight scoped client after authentication and reuse it across
Foods, Restaurants, Photo Scanning, Food Logs, and Glucose:

```kotlin
import ai.january.partner.PartnerUserId

val user = january.forUser(
    endUserId = PartnerUserId(account.stableId),
    timezone = "America/New_York",
)

val foods = user.foods.search(SearchFoodsRequest(query = "banana"))
```

`timezone` is an optional IANA identifier. It controls calendar-date boundaries
for Food Logs and is forwarded to Glucose. Create a new scoped client after
sign-in, sign-out, account switching, or timezone changes. The SDK does not
persist identity.

Request models retain optional identity fields for source compatibility. New
integrations should use the scoped client instead of repeating `endUserId` in
individual calls.

With client-token authentication, the transport removes `x-end-user-id`; the
token itself identifies the user. The scoped client still supplies the public
request context consistently and supports other approved authentication modes.
