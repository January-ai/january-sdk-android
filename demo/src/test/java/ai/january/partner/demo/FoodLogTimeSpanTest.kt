package ai.january.partner.demo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodLogTimeSpanTest {
    @Test
    fun spansUseSundayWeekAndPreviousCompleteMonth() {
        val reference = LocalDate.of(2026, 8, 25)
        assertEquals(FoodLogDateRange(reference, reference), FoodLogTimeSpan.TODAY.dateRange(reference))
        assertEquals(
            FoodLogDateRange(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 29)),
            FoodLogTimeSpan.CURRENT_WEEK.dateRange(reference),
        )
        assertEquals(
            FoodLogDateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
            FoodLogTimeSpan.LAST_MONTH.dateRange(reference),
        )
    }

    @Test
    fun lastMonthHandlesYearBoundary() {
        assertEquals(
            FoodLogDateRange(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)),
            FoodLogTimeSpan.LAST_MONTH.dateRange(LocalDate.of(2026, 1, 3)),
        )
    }
    @Test
    fun datesUseTheSelectedUserTimezoneAtWeekAndMonthBoundaries() {
        val instant = java.time.Instant.parse("2026-03-01T01:00:00Z")
        assertEquals(
            FoodLogDateRange(LocalDate.of(2026, 2, 22), LocalDate.of(2026, 2, 28)),
            FoodLogTimeSpan.CURRENT_WEEK.dateRange("America/Los_Angeles", instant),
        )
        assertEquals(
            FoodLogDateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 7)),
            FoodLogTimeSpan.CURRENT_WEEK.dateRange("Asia/Tokyo", instant),
        )
        assertEquals(
            FoodLogDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
            FoodLogTimeSpan.LAST_MONTH.dateRange("America/Los_Angeles", instant),
        )
    }
}
