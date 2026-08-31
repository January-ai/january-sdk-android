package ai.january.partner.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class PredictionChartTest {
    @Test fun targetRangeRemainsVisibleWhenPredictionIsNarrower() {
        assertEquals(PredictionChartDomain(50.0, 210.0), predictionChartDomain(listOf(PredictionPoint(0.0, 90.0), PredictionPoint(60.0, 130.0)), 70.0, 180.0))
    }
    @Test fun pointsOutsideTheDisplayedTwoHoursDoNotDistortScale() {
        assertEquals(PredictionChartDomain(70.0, 150.0), predictionChartDomain(listOf(PredictionPoint(-1.0, -100.0), PredictionPoint(0.0, 90.0), PredictionPoint(60.0, 130.0), PredictionPoint(121.0, 1000.0)), null, null))
    }
    @Test fun FlatPredictionStillHasReadableVerticalRange() {
        assertEquals(PredictionChartDomain(80.0, 120.0), predictionChartDomain(listOf(PredictionPoint(0.0, 100.0), PredictionPoint(120.0, 100.0)), null, null))
    }
}
