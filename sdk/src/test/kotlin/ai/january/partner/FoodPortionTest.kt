package ai.january.partner

import ai.january.partner.foods.FoodPortionError
import ai.january.partner.foods.FoodPortionException
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.ServingOption
import ai.january.partner.foods.portion
import ai.january.partner.models.NutrientAmount
import ai.january.partner.models.NutritionFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

public class FoodPortionTest {
    private val banana = FoodSearchItem(
        id = FoodId(70_381_819),
        name = "banana",
        brandName = null,
        calories = 105.02,
        protein = 1.2862,
        carbohydrates = 26.9512,
        netCarbohydrates = null,
        totalFat = null,
        saturatedFat = null,
        fiber = null,
        totalSugars = null,
        addedSugars = null,
        sodium = null,
        potassium = 422.0,
        cholesterol = null,
        glycemicIndex = 51.0,
        glycemicLoad = 12.0,
        photoUrl = null,
        servings = listOf(
            ServingOption(ServingId(1), 1.0, "medium", 1.0, 118.0, true),
            ServingOption(ServingId(2), 100.0, "g", 0.8474576271, 100.0, false),
        ),
        nutrients = NutritionFacts(
            calories = NutrientAmount(105.02, "cal"),
            protein = NutrientAmount(1.2862, "g"),
            carbohydrates = NutrientAmount(26.9512, "g"),
            potassium = NutrientAmount(422.0, "mg"),
        ),
    )

    @Test
    public fun scalesNutritionAndBuildsWireSelection() {
        val portion = banana.portion(ServingId(2), 200.0)

        assertEquals(178.0, portion.nutrition.calories!!.value, 0.001)
        assertEquals(2.18, portion.nutrition.protein!!.value, 0.001)
        assertEquals(45.68, portion.nutrition.carbohydrates!!.value, 0.001)
        assertEquals(715.254, portion.nutrition.potassium!!.value, 0.001)
        assertEquals("mg", portion.nutrition.potassium!!.unit)
        assertEquals(200.0, portion.totalWeightGrams!!, 0.001)
        assertEquals(51.0, portion.glycemicIndex!!, 0.0)
        assertEquals(20.3389, portion.glycemicLoad!!, 0.001)
        assertEquals("70381819", portion.selection.id)
        assertEquals("2", portion.selection.serving.id)
        assertEquals(200.0, portion.selection.serving.quantity, 0.0)
    }

    @Test
    public fun defaultsToPrimaryAndRejectsUnsafeInput() {
        assertEquals(ServingId(1), banana.portion().serving.id)
        assertEquals(FoodPortionError.INVALID_QUANTITY, failure { banana.portion(quantity = 0.0) }.reason)
        assertEquals(FoodPortionError.INVALID_QUANTITY, failure { banana.portion(quantity = Double.NaN) }.reason)
        assertEquals(FoodPortionError.SERVING_NOT_FOUND, failure { banana.portion(ServingId(99)) }.reason)
    }

    private fun failure(block: () -> Unit): FoodPortionException =
        assertThrows(FoodPortionException::class.java, block)
}
