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
        // The API reads a selection's quantity as a count of servings: 200 g of the "100 g"
        // serving is 2 servings, not 200.
        assertEquals(2.0, portion.selection.serving.quantity, 0.0)
    }

    // The primary serving is "6 oz" and the food's nutrients are for that serving.
    private val yogurt = FoodSearchItem(
        id = FoodId(70_376_084),
        name = "greek yogurt",
        brandName = null,
        calories = 100.0,
        protein = 17.0,
        carbohydrates = 6.0,
        netCarbohydrates = null,
        totalFat = 0.7,
        saturatedFat = null,
        fiber = null,
        totalSugars = null,
        addedSugars = null,
        sodium = null,
        potassium = null,
        cholesterol = null,
        glycemicIndex = null,
        glycemicLoad = null,
        photoUrl = null,
        servings = listOf(
            ServingOption(ServingId(34_157_706), 6.0, "oz", 1.0, 170.0, true),
            ServingOption(ServingId(34_157_707), 1.0, "cup", 1.3353, 227.0, false),
        ),
        nutrients = NutritionFacts(
            calories = NutrientAmount(100.0, "cal"),
            protein = NutrientAmount(17.0, "g"),
            carbohydrates = NutrientAmount(6.0, "g"),
        ),
    )

    @Test
    public fun defaultPortionOfSixOunceServingSendsOneServing() {
        val portion = yogurt.portion()

        assertEquals(6.0, portion.quantity, 0.0)
        assertEquals("70376084", portion.selection.id)
        assertEquals("34157706", portion.selection.serving.id)
        assertEquals(1.0, portion.selection.serving.quantity, 0.0)
    }

    @Test
    public fun twelveOuncesOfSixOunceServingSendsTwoServings() {
        assertEquals(2.0, yogurt.portion(quantity = 12.0).selection.serving.quantity, 0.0)
    }

    @Test
    public fun oneCupServingSendsTheQuantityUnchanged() {
        val cup = ServingId(34_157_707)

        assertEquals(1.0, yogurt.portion(cup).selection.serving.quantity, 0.0)
        assertEquals(1.5, yogurt.portion(cup, 1.5).selection.serving.quantity, 0.0)
        assertEquals(1.0, banana.portion().selection.serving.quantity, 0.0)
        assertEquals(2.5, banana.portion(quantity = 2.5).selection.serving.quantity, 0.0)
    }

    @Test
    public fun hundredGramServingWith150SendsOneAndAHalfServings() {
        val portion = banana.portion(ServingId(2), 150.0)

        assertEquals("2", portion.selection.serving.id)
        assertEquals(1.5, portion.selection.serving.quantity, 0.0)
    }

    @Test
    public fun nutritionStillScalesByTheAmountInTheServingUnit() {
        val oneServing = yogurt.portion()
        assertEquals(100.0, oneServing.nutrition.calories!!.value, 0.001)
        assertEquals(17.0, oneServing.nutrition.protein!!.value, 0.001)
        assertEquals(170.0, oneServing.totalWeightGrams!!, 0.001)

        val twelveOunces = yogurt.portion(quantity = 12.0)
        assertEquals(12.0, twelveOunces.quantity, 0.0)
        assertEquals(200.0, twelveOunces.nutrition.calories!!.value, 0.001)
        assertEquals(12.0, twelveOunces.nutrition.carbohydrates!!.value, 0.001)
        assertEquals(340.0, twelveOunces.totalWeightGrams!!, 0.001)

        val cup = yogurt.portion(ServingId(34_157_707), 1.5)
        assertEquals(200.295, cup.nutrition.calories!!.value, 0.001)
        assertEquals(340.5, cup.totalWeightGrams!!, 0.001)

        val grams = banana.portion(ServingId(2), 150.0)
        assertEquals(150.0, grams.quantity, 0.0)
        assertEquals(133.5, grams.nutrition.calories!!.value, 0.001)
        assertEquals(150.0, grams.totalWeightGrams!!, 0.001)
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
