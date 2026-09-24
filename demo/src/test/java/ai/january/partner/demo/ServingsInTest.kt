package ai.january.partner.demo

import ai.january.partner.ServingId
import ai.january.partner.foods.ServingOption
import org.junit.Assert.assertEquals
import org.junit.Test

class ServingsInTest {
    private fun serving(quantity: Double?, unit: String) = ServingOption(ServingId(1), quantity, unit, 1.0, null, true)

    @Test
    fun amountInTheServingUnitBecomesANumberOfServings() {
        assertEquals(1.0, servingsIn(6.0, serving(6.0, "oz")), 0.0)
        assertEquals(2.0, servingsIn(12.0, serving(6.0, "oz")), 0.0)
        assertEquals(1.5, servingsIn(150.0, serving(100.0, "g")), 0.0)
        assertEquals(0.75, servingsIn(0.75, serving(1.0, "cup")), 0.0)
    }

    @Test
    fun servingWithoutAUsableQuantityCountsTheAmountAsServings() {
        assertEquals(2.0, servingsIn(2.0, serving(null, "serving")), 0.0)
        assertEquals(2.0, servingsIn(2.0, serving(0.0, "serving")), 0.0)
    }
}
