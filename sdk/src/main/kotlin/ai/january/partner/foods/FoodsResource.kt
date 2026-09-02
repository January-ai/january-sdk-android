package ai.january.partner.foods

import ai.january.partner.ErrorCategory
import ai.january.partner.FoodId
import ai.january.partner.JanuaryException
import ai.january.partner.ServingId
import ai.january.partner.executeApiCall
import ai.january.partner.models.NutrientAmount
import ai.january.partner.models.NutritionFacts
import ai.january.partner.transport.apis.FoodsApi
import ai.january.partner.transport.models.SuggestFoodAlternativesBody

public class FoodsResource internal constructor(private val api: FoodsApi) {
    public suspend fun autocomplete(request: AutocompleteFoodsRequest): AutocompleteFoodsResponse {
        if (request.query.length > 64) {
            throw JanuaryException(
                ErrorCategory.VALIDATION,
                "Food autocomplete query must contain at most 64 characters.",
            )
        }
        if (request.limit !in 1..20) {
            throw JanuaryException(
                ErrorCategory.VALIDATION,
                "Food autocomplete limit must be between 1 and 20.",
            )
        }
        return executeApiCall(
            operation = {
                api.autocompleteFoods(
                    query = request.query,
                    type = request.category?.toTransport(),
                    limit = request.limit,
                )
            },
            transform = { response ->
                AutocompleteFoodsResponse(
                    response.items.map {
                        FoodSuggestion(
                            id = FoodId(it.id),
                            name = it.name,
                            brandName = it.brandName,
                            imageUrl = it.imageUrl,
                            nutrients = it.nutrients.toPublicNutrition(),
                        )
                    },
                )
            },
        )
    }

    public suspend fun get(request: GetFoodRequest): FoodSearchItem = executeApiCall(
        operation = { api.getFood(request.foodId.value) },
        transform = { it.toPublic() },
    )

    public suspend fun search(request: SearchFoodsRequest): FoodSearchResults {
        if (request.query.isBlank() || request.query.length > 256) {
            throw JanuaryException(
                ErrorCategory.VALIDATION,
                "Food search query must contain between 1 and 256 characters.",
            )
        }
        if (request.limit !in 1..40) {
            throw JanuaryException(
                ErrorCategory.VALIDATION,
                "Food search limit must be between 1 and 40.",
            )
        }

        return executeApiCall(
            operation = {
                api.searchFoods(
                    query = request.query,
                    type = request.category?.toTransport(),
                    limit = request.limit,
                )
            },
            transform = { it.toPublic() },
        )
    }

    public suspend fun lookupBarcode(request: LookupFoodByBarcodeRequest): FoodSearchResults =
        executeApiCall(
            operation = { api.lookupFoodByBarcode(request.upc) },
            transform = { FoodSearchResults(1, listOf(it.toPublic())) },
        )

    public suspend fun suggestAlternatives(
        request: SuggestFoodAlternativesRequest,
    ): SuggestFoodAlternativesResponse = executeApiCall(
        operation = {
            api.suggestFoodAlternatives(
                foodId = request.foodId,
                suggestFoodAlternativesBody = SuggestFoodAlternativesBody(
                    dietRestrictions = request.dietRestrictions.map { restriction ->
                        requireNotNull(ai.january.partner.transport.models.DietRestriction.decode(restriction.value))
                    },
                    dietPreferences = request.dietPreferences.map { preference ->
                        requireNotNull(ai.january.partner.transport.models.DietPreference.decode(preference.value))
                    },
                ),
            )
        },
        transform = { response ->
            SuggestFoodAlternativesResponse(response.alternatives.map { food ->
                DetectedFood(
                    id = food.id,
                    name = food.name,
                    brandName = food.brandName,
                    nutrients = food.nutrients.toPublicCompleteNutrition(),
                    servings = food.servings.map { serving ->
                        DetectedServing(serving.id, serving.quantity?.toDouble(), serving.unit)
                    },
                )
            })
        },
    )

    private fun FoodCategory.toTransport() = when (this) {
        FoodCategory.GENERIC -> ai.january.partner.transport.models.FoodCategory.GENERIC
        FoodCategory.GENERAL -> ai.january.partner.transport.models.FoodCategory.GENERIC
        FoodCategory.BRANDED -> ai.january.partner.transport.models.FoodCategory.BRANDED
        FoodCategory.RECIPE -> ai.january.partner.transport.models.FoodCategory.RECIPE
    }

    private fun AutocompleteFoodCategory.toTransport() = when (this) {
        AutocompleteFoodCategory.GENERIC ->
            ai.january.partner.transport.models.AutocompleteFoodCategory.GENERIC
        AutocompleteFoodCategory.GENERAL ->
            ai.january.partner.transport.models.AutocompleteFoodCategory.GENERIC
        AutocompleteFoodCategory.BRANDED ->
            ai.january.partner.transport.models.AutocompleteFoodCategory.BRANDED
    }

    private fun ai.january.partner.transport.models.FoodSearchResults.toPublic() = FoodSearchResults(
        totalCount = items.size,
        items = items.map { it.toPublic() },
    )

    private fun ai.january.partner.transport.models.FoodSearchItem.toPublic() = FoodSearchItem(
        id = FoodId(id),
        type = when (type) {
            ai.january.partner.transport.models.FoodSearchItem.Type.GENERIC -> FoodCategory.GENERIC
            ai.january.partner.transport.models.FoodSearchItem.Type.BRANDED -> FoodCategory.BRANDED
            ai.january.partner.transport.models.FoodSearchItem.Type.RECIPE -> FoodCategory.RECIPE
            else -> FoodCategory.GENERIC
        },
        name = name,
        brandName = brandName,
        nutrients = nutrients.toPublicNutrition(),
        calories = nutrients.calories?.value?.toDouble(),
        protein = nutrients.protein?.value?.toDouble(),
        carbohydrates = nutrients.carbohydrates?.value?.toDouble(),
        netCarbohydrates = nutrients.netCarbohydrates?.value?.toDouble(),
        totalFat = nutrients.totalFat?.value?.toDouble(),
        saturatedFat = nutrients.saturatedFat?.value?.toDouble(),
        fiber = nutrients.fiber?.value?.toDouble(),
        totalSugars = nutrients.totalSugars?.value?.toDouble(),
        addedSugars = nutrients.addedSugars?.value?.toDouble(),
        sodium = nutrients.sodium?.value?.toDouble(),
        potassium = nutrients.potassium?.value?.toDouble(),
        cholesterol = nutrients.cholesterol?.value?.toDouble(),
        glycemicIndex = glycemicIndex?.toDouble(),
        glycemicLoad = glycemicLoad?.toDouble(),
        photoUrl = imageUrl,
        barcode = barcode,
        servings = servings.map { serving ->
            ServingOption(
                id = serving.id?.let(::ServingId),
                quantity = serving.quantity?.toDouble(),
                unit = serving.unit,
                scalingFactor = serving.scalingFactor?.toDouble() ?: 1.0,
                weightGrams = serving.weightGrams?.toDouble(),
                isPrimary = serving.isPrimary,
            )
        },
    )


}

private fun ai.january.partner.transport.models.NutritionFacts.toPublicNutrition(): NutritionFacts {
    fun ai.january.partner.transport.models.NutrientAmount?.amount(): NutrientAmount? =
        this?.let { NutrientAmount(it.value.toDouble(), it.unit) }
    return NutritionFacts(
        calories = calories.amount(), protein = protein.amount(),
        carbohydrates = carbohydrates.amount(), netCarbohydrates = netCarbohydrates.amount(),
        totalFat = totalFat.amount(), transFat = transFat.amount(),
        saturatedFat = saturatedFat.amount(), fiber = fiber.amount(),
        totalSugars = totalSugars.amount(), addedSugars = addedSugars.amount(),
        cholesterol = cholesterol.amount(), calcium = calcium.amount(), iron = iron.amount(),
        potassium = potassium.amount(), sodium = sodium.amount(), vitaminD = vitaminD.amount(),
    )
}

private fun ai.january.partner.transport.models.NutritionFacts.toPublicCompleteNutrition():
    ai.january.partner.models.CompleteScanNutritionFacts {
    fun ai.january.partner.transport.models.NutrientAmount?.amount(): NutrientAmount? =
        this?.let { NutrientAmount(it.value.toDouble(), it.unit) }
    return ai.january.partner.models.CompleteScanNutritionFacts(
        calories = calories.amount(), protein = protein.amount(),
        carbohydrates = carbohydrates.amount(), netCarbohydrates = netCarbohydrates.amount(),
        totalFat = totalFat.amount(), saturatedFat = saturatedFat.amount(), fiber = fiber.amount(),
        totalSugars = totalSugars.amount(), addedSugars = addedSugars.amount(), sodium = sodium.amount(),
    )
}
