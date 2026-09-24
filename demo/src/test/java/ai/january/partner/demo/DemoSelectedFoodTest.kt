package ai.january.partner.demo

import ai.january.partner.FoodId
import ai.january.partner.ServingId
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.ServingOption
import ai.january.partner.models.NutrientAmount
import ai.january.partner.models.NutritionFacts
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoSelectedFoodTest {
    private val sixOunces = ServingOption(ServingId(31), 6.0, "oz", 1.0, 170.0, true)
    private val cup = ServingOption(ServingId(32), 1.0, "cup", 1.3353, 227.0, false)
    private val yogurt = FoodSearchItem(
        id = FoodId(103), name = "Fixture greek yogurt", brandName = null, calories = 100.0, protein = 4.0,
        carbohydrates = 20.0, netCarbohydrates = null, totalFat = 2.0, saturatedFat = null, fiber = null,
        totalSugars = null, addedSugars = null, sodium = null, potassium = null, cholesterol = null,
        glycemicIndex = null, glycemicLoad = null, photoUrl = null, servings = listOf(sixOunces, cup),
        nutrients = NutritionFacts(calories = NutrientAmount(100.0, "kcal")),
    )

    @Test
    fun oneSixOunceServingShowsAndSendsOneServing() {
        val selected = DemoSelectedFood(yogurt, sixOunces, 1.0)

        assertEquals(100.0, selected.portion!!.nutrition.calories!!.value, 0.001)
        assertEquals("103", selected.selection.id)
        assertEquals("31", selected.selection.serving.id)
        assertEquals(1.0, selected.selection.serving.quantity, 0.0)
    }

    @Test
    fun quantityCountsServingsOfAnySize() {
        assertEquals(2.0, DemoSelectedFood(yogurt, sixOunces, 2.0).selection.serving.quantity, 0.0)
        assertEquals(200.0, DemoSelectedFood(yogurt, sixOunces, 2.0).portion!!.nutrition.calories!!.value, 0.001)
        assertEquals(1.5, DemoSelectedFood(yogurt, cup, 1.5).selection.serving.quantity, 0.0)
        assertEquals(200.295, DemoSelectedFood(yogurt, cup, 1.5).portion!!.nutrition.calories!!.value, 0.001)
    }

    @Test
    fun servingWithoutAQuantityStillSendsTheCount() {
        val unsized = ServingOption(ServingId(33), null, "container", 1.0, null, false)
        val selected = DemoSelectedFood(yogurt.copy(servings = listOf(unsized)), unsized, 2.0)

        assertEquals(null, selected.portion)
        assertEquals(2.0, selected.selection.serving.quantity, 0.0)
    }
}
