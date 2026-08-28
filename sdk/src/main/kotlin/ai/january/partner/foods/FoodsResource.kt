package ai.january.partner.foods

import ai.january.partner.ErrorCategory
import ai.january.partner.FoodId
import ai.january.partner.JanuaryException
import ai.january.partner.ServingId
import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.models.NutrientAmount
import ai.january.partner.models.NutritionFacts
import ai.january.partner.transport.apis.FoodsApi
import ai.january.partner.transport.models.SuggestFoodAlternativesBody
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException

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
                    xEndUserId = request.endUserId?.value,
                    category = request.category?.toTransport(),
                    limit = BigDecimal.valueOf(request.limit.toLong()),
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
                            nutrients = it.nutrients?.toPublicNutrition(),
                        )
                    },
                )
            },
        )
    }

    public suspend fun get(request: GetFoodRequest): FoodSearchItem = executeApiCall(
        operation = { api.getFood(request.foodId.value, request.endUserId?.value) },
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

        try {
            val response = api.searchFoods(
                query = request.query,
                xEndUserId = request.endUserId?.value,
                category = request.category?.toTransport(),
                limit = BigDecimal.valueOf(request.limit.toLong()),
            )
            if (!response.isSuccessful) {
                throw JanuaryException(
                    category = categoryFor(response.code()),
                    message = "The January API returned HTTP ${response.code()}.",
                    httpStatus = response.code(),
                )
            }
            val body = response.body()
                ?: throw JanuaryException(
                    ErrorCategory.DECODING,
                    "The January API returned an empty response.",
                )
            return body.toPublic()
        } catch (error: CancellationException) {
            throw error
        } catch (error: JanuaryException) {
            throw error
        } catch (error: IOException) {
            throw JanuaryException(
                ErrorCategory.TRANSPORT,
                "The request to the January API failed.",
                cause = error,
            )
        }
    }

    public suspend fun lookupBarcode(request: LookupFoodByBarcodeRequest): FoodSearchResults =
        executeApiCall(
            operation = { api.lookupFoodByBarcode(request.upc, request.endUserId?.value) },
            transform = { it.toPublic() },
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
                xEndUserId = request.endUserId?.value,
            )
        },
        transform = { bridgeModel(it) },
    )

    private fun FoodCategory.toTransport() = when (this) {
        FoodCategory.GENERAL -> ai.january.partner.transport.models.FoodCategory.GENERAL
        FoodCategory.BRANDED -> ai.january.partner.transport.models.FoodCategory.BRANDED
        FoodCategory.RECIPE -> ai.january.partner.transport.models.FoodCategory.RECIPE
    }

    private fun AutocompleteFoodCategory.toTransport() = when (this) {
        AutocompleteFoodCategory.GENERAL ->
            ai.january.partner.transport.models.AutocompleteFoodCategory.GENERAL
        AutocompleteFoodCategory.BRANDED ->
            ai.january.partner.transport.models.AutocompleteFoodCategory.BRANDED
    }

    private fun ai.january.partner.transport.models.FoodSearchResults.toPublic() = FoodSearchResults(
        totalCount = totalCount.toInt(),
        items = items.map { it.toPublic() },
    )

    private fun ai.january.partner.transport.models.FoodSearchItem.toPublic() = FoodSearchItem(
        id = FoodId(id),
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
        servings = servings.map { serving ->
            ServingOption(
                id = ServingId(serving.id),
                quantity = serving.quantity.toDouble(),
                unit = serving.unit,
                scalingFactor = serving.scalingFactor?.toDouble() ?: 1.0,
                weightGrams = serving.weightGrams?.toDouble(),
                isPrimary = serving.isPrimary,
            )
        },
    )

    private fun categoryFor(status: Int): ErrorCategory = when (status) {
        400 -> ErrorCategory.VALIDATION
        401 -> ErrorCategory.AUTHENTICATION
        403 -> ErrorCategory.AUTHORIZATION
        404 -> ErrorCategory.NOT_FOUND
        429 -> ErrorCategory.RATE_LIMITED
        in 500..599 -> ErrorCategory.SERVER
        else -> ErrorCategory.TRANSPORT
    }
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
