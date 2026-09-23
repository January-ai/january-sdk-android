package ai.january.partner.demo

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class UserTimeTest {
    private val instant = Instant.parse("2026-09-23T10:30:00Z")

    @Test fun todayIsTheEndUsersDayNotTheDevices() {
        // One instant, three calendar days: UTC+14, UTC and UTC-11.
        assertEquals(LocalDate.parse("2026-09-24"), userToday(userZone("Pacific/Kiritimati"), instant))
        assertEquals(LocalDate.parse("2026-09-23"), userToday(userZone("Africa/Abidjan"), instant))
        assertEquals(LocalDate.parse("2026-09-22"), userToday(userZone("Pacific/Pago_Pago"), instant))
    }

    @Test fun onlyAMissingOrUnknownZoneFallsBackToTheDevice() {
        assertEquals(ZoneId.of("Asia/Tokyo"), userZone(" Asia/Tokyo "))
        assertEquals(ZoneId.systemDefault(), userZone("Not/A_Zone"))
        assertEquals(ZoneId.systemDefault(), userZone(""))
        assertEquals(ZoneId.systemDefault(), userZone(null))
    }

    @Test fun anEarlierDaysEntryIsNoonOnTheEndUsersClock() {
        val zone = userZone("Pacific/Kiritimati")
        val now = ZonedDateTime.ofInstant(instant, zone)
        assertEquals(OffsetDateTime.parse("2026-09-23T12:00+14:00"), entryTime(LocalDate.parse("2026-09-23"), zone, now))
        assertEquals(OffsetDateTime.parse("2026-09-24T00:30+14:00"), entryTime(LocalDate.parse("2026-09-24"), zone, now))
    }

    @Test fun theLogsSpansFollowTheEndUsersDay() {
        assertEquals(FoodLogDateRange(LocalDate.parse("2026-09-24"), LocalDate.parse("2026-09-24")), FoodLogTimeSpan.TODAY.dateRange("Pacific/Kiritimati", instant))
        assertEquals(FoodLogDateRange(LocalDate.parse("2026-09-22"), LocalDate.parse("2026-09-22")), FoodLogTimeSpan.TODAY.dateRange("Pacific/Pago_Pago", instant))
    }
}
