package ai.january.partner.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class HeightInputTest {
    @Test
    fun totalInchesArePresentedAsFeetAndInches() {
        assertEquals(FeetAndInches(5, 6), feetAndInches(66.0))
        assertEquals(FeetAndInches(6, 0), feetAndInches(71.6))
    }

    @Test
    fun metricPresentationRoundTripsToApiInches() {
        val centimeters = inchesToCentimeters(66.0)
        assertEquals(167.64, centimeters, 0.001)
        assertEquals(66.0, centimetersToInches(centimeters), 0.001)
    }
}
