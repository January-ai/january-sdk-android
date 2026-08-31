package ai.january.partner.demo

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class FoodLogTimeSpan(val title: String) {
    TODAY("Today"),
    CURRENT_WEEK("This week"),
    LAST_MONTH("Last month");

    fun dateRange(timezone: String, instant: java.time.Instant = java.time.Instant.now()): FoodLogDateRange {
        val zone = runCatching { java.time.ZoneId.of(timezone) }.getOrDefault(java.time.ZoneId.systemDefault())
        return dateRange(instant.atZone(zone).toLocalDate())
    }

    fun dateRange(referenceDate: LocalDate = LocalDate.now()): FoodLogDateRange = when (this) {
        TODAY -> FoodLogDateRange(referenceDate, referenceDate)
        CURRENT_WEEK -> {
            val start = referenceDate.minusDays((referenceDate.dayOfWeek.value % 7).toLong())
            FoodLogDateRange(start, start.plusDays(6))
        }
        LAST_MONTH -> {
            val currentMonthStart = referenceDate.withDayOfMonth(1)
            val start = currentMonthStart.minusMonths(1)
            FoodLogDateRange(start, currentMonthStart.minusDays(1))
        }
    }
}

data class FoodLogDateRange(val start: LocalDate, val end: LocalDate) {
    val apiStart: String get() = start.toString()
    val apiEnd: String get() = end.toString()

    fun displayText(): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        return if (start == end) start.format(formatter) else "${start.format(formatter)} – ${end.format(formatter)}"
    }
}
