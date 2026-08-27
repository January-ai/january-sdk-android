# User identity and timezone

January client tokens are bound to one end user. The host application owns that
identity and must keep it stable across sessions. Never use an email address or
other directly identifying value when an opaque internal account ID is
available.

Create a lightweight scoped client for operations that need identity and
timezone:

```kotlin
import ai.january.partner.PartnerUserId

val user = january.forUser(
    endUserId = PartnerUserId(account.stableId),
    timezone = "America/New_York",
)
```

`timezone` is an optional IANA identifier. It controls calendar-date boundaries
for Food Logs and is forwarded to Glucose. Create a new scoped client after
sign-in, sign-out, account switching, or timezone changes. The SDK does not
persist identity.

With client-token authentication, the transport removes `x-end-user-id`; the
token itself identifies the user. The scoped client still supplies the public
request context consistently and supports other approved authentication modes.
