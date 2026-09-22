package ai.january.partner.demo

import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingChartDataTest {
    private val today = LocalDate.parse("2026-09-22")

    @Test fun weekAndMonthEndTodayAndFitOneRequest() {
        assertEquals(DateSpan(LocalDate.parse("2026-09-16"), today), chartSpan(ChartRange.WEEK, today))
        assertEquals(DateSpan(LocalDate.parse("2026-08-24"), today), chartSpan(ChartRange.MONTH, today))
        assertEquals(1, requestChunks(chartSpan(ChartRange.WEEK, today)).size)
        assertEquals(1, requestChunks(chartSpan(ChartRange.MONTH, today)).size)
    }

    @Test fun yearStartsOnTheFirstOfTheMonthElevenMonthsBack() {
        assertEquals(DateSpan(LocalDate.parse("2025-10-01"), today), chartSpan(ChartRange.YEAR, today))
    }

    @Test fun yearSplitsIntoConsecutiveChunksOfAtMostNinetyDays() {
        val span = chartSpan(ChartRange.YEAR, today)
        val chunks = requestChunks(span)
        assertTrue(chunks.size in 4..5)
        assertEquals(span.start, chunks.first().start)
        assertEquals(span.end, chunks.last().end)
        chunks.zipWithNext().forEach { (a, b) -> assertEquals(a.end.plusDays(1), b.start) }
        chunks.forEach { assertTrue(it.days in 1..90) }
        assertEquals(span.days, chunks.sumOf { it.days })
    }

    @Test fun chunkingCoversExactMultiplesAndSingleDays() {
        val start = LocalDate.parse("2026-01-01")
        assertEquals(listOf(DateSpan(start, start.plusDays(89)), DateSpan(start.plusDays(90), start.plusDays(179))), requestChunks(DateSpan(start, start.plusDays(179))))
        assertEquals(listOf(DateSpan(start, start)), requestChunks(DateSpan(start, start)))
    }

    @Test fun mergedChunksAreOldestFirstWithOneItemPerDate() {
        val merged = mergeChunks(listOf(listOf("2026-02-01" to 1, "2026-02-03" to 2), listOf("2026-01-01" to 3, "2026-02-03" to 4))) { it.first }
        assertEquals(listOf("2026-01-01", "2026-02-01", "2026-02-03"), merged.map { it.first })
    }

    @Test fun dailyBarsFillDaysWithoutLogsWithZero() {
        val span = chartSpan(ChartRange.WEEK, today)
        val bars = dailyBars(span, mapOf(today to 16.0, today.minusDays(3) to 8.0))
        assertEquals(7, bars.size)
        assertEquals(listOf(0.0, 0.0, 0.0, 8.0, 0.0, 0.0, 16.0), bars.map { it.value })
        assertEquals(30, dailyBars(chartSpan(ChartRange.MONTH, today), emptyMap()).size)
    }

    @Test fun monthlyBarsSumEachCalendarMonth() {
        val span = chartSpan(ChartRange.YEAR, today)
        val totals = mapOf(
            LocalDate.parse("2025-10-01") to 10.0,
            LocalDate.parse("2025-10-31") to 5.0,
            LocalDate.parse("2026-02-28") to 7.5,
            LocalDate.parse("2026-09-22") to 2.0,
            LocalDate.parse("2025-09-30") to 100.0, // before the span
        )
        val bars = monthlyBars(span, totals)
        assertEquals(12, bars.size)
        assertEquals(LocalDate.parse("2025-10-01"), bars.first().date)
        assertEquals(LocalDate.parse("2026-09-01"), bars.last().date)
        assertEquals(15.0, bars[0].value, 0.0)
        assertEquals(7.5, bars[4].value, 0.0)
        assertEquals(2.0, bars[11].value, 0.0)
        assertEquals(24.5, bars.sumOf { it.value }, 0.0)
        assertEquals(bars, waterBars(ChartRange.YEAR, span, totals))
    }

    @Test fun weightsConvertToTheChartUnit() {
        assertEquals(68.0388555, convertWeight(Weight(150.0, WeightUnit.POUNDS), WeightUnit.KILOGRAMS), 1e-7)
        assertEquals(154.3235835, convertWeight(Weight(70.0, WeightUnit.KILOGRAMS), WeightUnit.POUNDS), 1e-6)
        assertEquals(70.0, convertWeight(Weight(70.0, WeightUnit.KILOGRAMS), WeightUnit.KILOGRAMS), 0.0)
        val points = weightPoints(listOf(today to Weight(150.0, WeightUnit.POUNDS), today.minusDays(1) to Weight(70.0, WeightUnit.KILOGRAMS)), WeightUnit.KILOGRAMS)
        assertEquals(listOf(today.minusDays(1), today), points.map { it.date })
        assertEquals("68", formatChartNumber(points[1].value))
    }

    @Test fun axisLabelsAreSparseAndKeepBothEnds() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), axisLabelIndices(7, 7))
        assertEquals(listOf(0, 7, 15, 22, 29), axisLabelIndices(30, 5))
        assertEquals(emptyList<Int>(), axisLabelIndices(0, 5))
    }

    @Test fun summariesDescribeTheRange() {
        val points = listOf(ChartValue(today.minusDays(4), 70.2), ChartValue(today.minusDays(2), 70.0), ChartValue(today, 69.8))
        assertEquals("Weight, last 7 days: 3 entries, from 70.2 kg to 69.8 kg", weightChartSummary(ChartRange.WEEK, points, "kg"))
        assertEquals("Weight, last 30 days: no entries", weightChartSummary(ChartRange.MONTH, emptyList(), "kg"))
        val bars = dailyBars(chartSpan(ChartRange.WEEK, today), mapOf(today to 16.0, today.minusDays(2) to 8.0))
        assertEquals("Water, last 7 days: 2 of 7 days logged, 24 fl oz in total, most 16 fl oz on Sep 22", waterChartSummary(ChartRange.WEEK, bars, "fl oz"))
    }
}
