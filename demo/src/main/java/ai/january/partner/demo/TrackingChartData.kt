package ai.january.partner.demo

import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** The spans the Tracking charts can show. Every span ends today, whatever day the day picker is on. */
enum class ChartRange(val label: String, val spoken: String, val testTagSuffix: String) {
    WEEK("Week", "last 7 days", "week"),
    MONTH("Month", "last 30 days", "month"),
    YEAR("Year", "last 12 months", "year"),
}

/** An inclusive run of calendar days. */
data class DateSpan(val start: LocalDate, val end: LocalDate) {
    val days: Long get() = ChronoUnit.DAYS.between(start, end) + 1
}

/** One bar or one point: a day, or for monthly bars the first day of the month. */
data class ChartValue(val date: LocalDate, val value: Double)

/** The list endpoints return at most 100 days each, so longer spans are fetched in chunks this size. */
const val CHART_CHUNK_DAYS = 90

const val KILOGRAMS_PER_POUND = 0.45359237

/**
 * Week is the last 7 days including today, Month the last 30, and Year the 12 calendar months
 * ending with this one, so it always fills 12 monthly bars.
 */
fun chartSpan(range: ChartRange, today: LocalDate): DateSpan = when (range) {
    ChartRange.WEEK -> DateSpan(today.minusDays(6), today)
    ChartRange.MONTH -> DateSpan(today.minusDays(29), today)
    ChartRange.YEAR -> DateSpan(today.withDayOfMonth(1).minusMonths(11), today)
}

/** Splits [span] into consecutive, oldest-first chunks of at most [maxDays] days. */
fun requestChunks(span: DateSpan, maxDays: Int = CHART_CHUNK_DAYS): List<DateSpan> {
    require(maxDays > 0)
    val chunks = mutableListOf<DateSpan>()
    var start = span.start
    while (!start.isAfter(span.end)) {
        val end = minOf(start.plusDays(maxDays - 1L), span.end)
        chunks += DateSpan(start, end)
        start = end.plusDays(1)
    }
    return chunks
}

/** Joins the chunks' items oldest first, keeping one item per date. */
fun <T> mergeChunks(chunks: List<List<T>>, date: (T) -> String): List<T> =
    chunks.flatten().associateBy(date).toSortedMap().values.toList()

/** One bar per day of [span]; days without a total are zero. */
fun dailyBars(span: DateSpan, totals: Map<LocalDate, Double>): List<ChartValue> =
    (0 until span.days).map { offset -> span.start.plusDays(offset).let { ChartValue(it, totals[it] ?: 0.0) } }

/** One bar per calendar month of [span], each the sum of that month's daily totals. */
fun monthlyBars(span: DateSpan, totals: Map<LocalDate, Double>): List<ChartValue> {
    val bars = mutableListOf<ChartValue>()
    var month = span.start.withDayOfMonth(1)
    while (!month.isAfter(span.end)) {
        val next = month.plusMonths(1)
        val sum = totals.filterKeys { !it.isBefore(month) && it.isBefore(next) && !it.isBefore(span.start) && !it.isAfter(span.end) }.values.sum()
        bars += ChartValue(month, sum)
        month = next
    }
    return bars
}

fun waterBars(range: ChartRange, span: DateSpan, totals: Map<LocalDate, Double>): List<ChartValue> =
    if (range == ChartRange.YEAR) monthlyBars(span, totals) else dailyBars(span, totals)

fun convertWeight(weight: Weight, to: WeightUnit): Double = when {
    weight.unit == to -> weight.value
    to == WeightUnit.KILOGRAMS -> weight.value * KILOGRAMS_PER_POUND
    else -> weight.value / KILOGRAMS_PER_POUND
}

/**
 * A day's weight in the unit the card is set to: exactly as logged when the units match, otherwise
 * converted and shown to one decimal place, like the chart.
 */
fun weightText(weight: Weight, unit: WeightUnit): String =
    if (weight.unit == unit) formatLogNumber(weight.value) else formatChartNumber(convertWeight(weight, unit))

/** Each day's weight in [unit], oldest first, whatever unit it was logged in. */
fun weightPoints(items: List<Pair<LocalDate, Weight>>, unit: WeightUnit): List<ChartValue> =
    items.sortedBy { it.first }.map { (date, weight) -> ChartValue(date, convertWeight(weight, unit)) }

/** Up to [maxLabels] evenly spaced indices into [count] items, always including the first and last. */
fun axisLabelIndices(count: Int, maxLabels: Int): List<Int> = when {
    count <= 0 -> emptyList()
    count <= maxLabels -> (0 until count).toList()
    maxLabels <= 1 -> listOf(count - 1)
    else -> (0 until maxLabels).map { (it * (count - 1).toDouble() / (maxLabels - 1)).roundToInt() }.distinct()
}

/** One decimal place, without a trailing ".0". */
fun formatChartNumber(value: Double): String = formatLogNumber((value * 10).roundToInt() / 10.0)

private val dayLabel = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val weekdayLabel = DateTimeFormatter.ofPattern("EEE", Locale.US)
private val monthLabel = DateTimeFormatter.ofPattern("MMM", Locale.US)

fun axisLabel(range: ChartRange, date: LocalDate): String = when (range) {
    ChartRange.WEEK -> date.format(weekdayLabel)
    ChartRange.MONTH -> date.format(dayLabel)
    ChartRange.YEAR -> date.format(monthLabel)
}

/** For example "Weight, last 7 days: 3 entries, from 70.2 kg to 69.8 kg". */
fun weightChartSummary(range: ChartRange, points: List<ChartValue>, unit: String): String {
    val prefix = "Weight, ${range.spoken}: "
    return when (points.size) {
        0 -> prefix + "no entries"
        1 -> prefix + "1 entry, ${formatChartNumber(points[0].value)} $unit"
        else -> prefix + "${points.size} entries, from ${formatChartNumber(points.first().value)} $unit to ${formatChartNumber(points.last().value)} $unit"
    }
}

/** For example "Water, last 7 days: 5 of 7 days logged, 40 fl oz in total, most 12 fl oz on Sep 20". */
fun waterChartSummary(range: ChartRange, bars: List<ChartValue>, unit: String): String {
    val prefix = "Water, ${range.spoken}: "
    val logged = bars.filter { it.value > 0 }
    if (logged.isEmpty()) return prefix + "nothing logged"
    val period = if (range == ChartRange.YEAR) "months" else "days"
    val most = logged.maxBy { it.value }
    val occurred = if (range == ChartRange.YEAR) "in ${most.date.format(DateTimeFormatter.ofPattern("MMMM", Locale.US))}" else "on ${most.date.format(dayLabel)}"
    return prefix + "${logged.size} of ${bars.size} $period logged, ${formatChartNumber(bars.sumOf { it.value })} $unit in total, most ${formatChartNumber(most.value)} $unit $occurred"
}
