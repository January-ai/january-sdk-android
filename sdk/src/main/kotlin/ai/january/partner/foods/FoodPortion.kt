package ai.january.partner.foods

import ai.january.partner.FoodId
import ai.january.partner.ServingId
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.NutrientAmount
import ai.january.partner.models.NutritionFacts
import ai.january.partner.models.ServingSelection

/**
 * A validated serving and amount with locally calculated nutrition.
 *
 * [quantity] is an amount in the serving's unit; [selection] sends it to the API as a number of
 * servings.
 */
public class FoodPortion private constructor(
    public val foodId: FoodId,
    public val serving: ServingOption,
    /**
     * The amount eaten in the serving's unit: 6 is one "6 oz" serving, and 150 is one and a half
     * "100 g" servings. It defaults to the serving's own quantity, one serving.
     */
    public val quantity: Double,
    public val nutrition: NutritionFacts,
    public val totalWeightGrams: Double?,
    public val glycemicIndex: Double?,
    public val glycemicLoad: Double?,
) {
    /**
     * The exact selection sent by food-log and glucose-prediction requests. Its quantity is the
     * number of servings eaten, [quantity] divided by the serving's quantity: 12 oz of a "6 oz"
     * serving is sent as 2.
     */
    public val selection: FoodSelection
        get() = FoodSelection(
            foodId.value,
            ServingSelection(serving.id.value, quantity / checkNotNull(serving.quantity)),
        )

    public companion object {
        @JvmStatic
        public fun from(
            food: FoodSearchItem,
            servingId: ServingId? = null,
            quantity: Double? = null,
        ): FoodPortion {
            if (food.servings.isEmpty()) fail(FoodPortionError.NO_SERVINGS)
            val selected = if (servingId == null) {
                food.servings.firstOrNull { it.isPrimary == true } ?: food.servings.first()
            } else {
                food.servings.firstOrNull { it.id == servingId }
                    ?: fail(FoodPortionError.SERVING_NOT_FOUND)
            }
            val servingQuantity = selected.quantity
            if (servingQuantity == null || !servingQuantity.isFinite() || servingQuantity <= 0 ||
                !selected.scalingFactor.isFinite() || selected.scalingFactor <= 0
            ) {
                fail(FoodPortionError.INVALID_SERVING)
            }
            val requested = quantity ?: servingQuantity
            if (!requested.isFinite() || requested <= 0 || requested > 10_000) {
                fail(FoodPortionError.INVALID_QUANTITY)
            }
            val scale = requested * selected.scalingFactor / servingQuantity
            val base = food.nutrients ?: legacyNutrition(food)
            return FoodPortion(
                foodId = food.id,
                serving = selected,
                quantity = requested,
                nutrition = base.scaledBy(scale),
                totalWeightGrams = selected.weightGrams?.times(requested)?.div(servingQuantity),
                glycemicIndex = food.glycemicIndex,
                glycemicLoad = food.glycemicLoad?.times(scale),
            )
        }

        private fun fail(error: FoodPortionError): Nothing = throw FoodPortionException(error)
    }
}

public enum class FoodPortionError {
    NO_SERVINGS,
    SERVING_NOT_FOUND,
    INVALID_SERVING,
    INVALID_QUANTITY,
}

public class FoodPortionException(public val reason: FoodPortionError) :
    IllegalArgumentException("Invalid food portion: ${reason.name.lowercase()}")

/**
 * A portion of this food. [quantity] is an amount in the serving's unit and defaults to one
 * serving; [servingId] defaults to the primary serving.
 */
public fun FoodSearchItem.portion(
    servingId: ServingId? = null,
    quantity: Double? = null,
): FoodPortion = FoodPortion.from(this, servingId, quantity)

private fun NutrientAmount.scaledBy(scale: Double): NutrientAmount = copy(value = value * scale)

private fun NutritionFacts.scaledBy(scale: Double): NutritionFacts = NutritionFacts(
    calories = calories?.scaledBy(scale),
    protein = protein?.scaledBy(scale),
    carbohydrates = carbohydrates?.scaledBy(scale),
    netCarbohydrates = netCarbohydrates?.scaledBy(scale),
    totalFat = totalFat?.scaledBy(scale),
    transFat = transFat?.scaledBy(scale),
    saturatedFat = saturatedFat?.scaledBy(scale),
    fiber = fiber?.scaledBy(scale),
    totalSugars = totalSugars?.scaledBy(scale),
    addedSugars = addedSugars?.scaledBy(scale),
    cholesterol = cholesterol?.scaledBy(scale),
    calcium = calcium?.scaledBy(scale),
    iron = iron?.scaledBy(scale),
    potassium = potassium?.scaledBy(scale),
    sodium = sodium?.scaledBy(scale),
    vitaminD = vitaminD?.scaledBy(scale),
)

private fun legacyNutrition(food: FoodSearchItem): NutritionFacts = NutritionFacts(
    calories = food.calories?.let { NutrientAmount(it, "cal") },
    protein = food.protein?.let { NutrientAmount(it, "g") },
    carbohydrates = food.carbohydrates?.let { NutrientAmount(it, "g") },
    netCarbohydrates = food.netCarbohydrates?.let { NutrientAmount(it, "g") },
    totalFat = food.totalFat?.let { NutrientAmount(it, "g") },
    saturatedFat = food.saturatedFat?.let { NutrientAmount(it, "g") },
    fiber = food.fiber?.let { NutrientAmount(it, "g") },
    totalSugars = food.totalSugars?.let { NutrientAmount(it, "g") },
    addedSugars = food.addedSugars?.let { NutrientAmount(it, "g") },
    cholesterol = food.cholesterol?.let { NutrientAmount(it, "mg") },
    potassium = food.potassium?.let { NutrientAmount(it, "mg") },
    sodium = food.sodium?.let { NutrientAmount(it, "mg") },
)
