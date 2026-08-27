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
}
