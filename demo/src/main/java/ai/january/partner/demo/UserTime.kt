package ai.january.partner.demo

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// The API files each log under a calendar day in the end user's timezone (the one the demo sends
// with every request), so every day the demo shows, asks for or logs to is a day on that clock,
// never the device's.

/** The end user's timezone as set in Settings; the device's only when that is not a valid zone ID. */
fun userZone(timezone: String?): ZoneId =
    timezone?.trim()?.takeIf(String::isNotEmpty)?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

/** The end user's calendar day at [instant]. */
fun userToday(zone: ZoneId, instant: Instant = Instant.now()): LocalDate = instant.atZone(zone).toLocalDate()
