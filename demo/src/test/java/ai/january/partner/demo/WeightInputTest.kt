package ai.january.partner.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightInputTest {
    @Test
    fun metricPresentationRoundTripsToApiPounds() {
        val kilograms = poundsToKilograms(150.0)
        assertEquals(68.0388555, kilograms, 0.001)
        assertEquals(150.0, kilogramsToPounds(kilograms), 0.001)
    }
}
