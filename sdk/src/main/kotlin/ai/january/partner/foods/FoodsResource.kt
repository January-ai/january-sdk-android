package ai.january.partner.foods

import ai.january.partner.ErrorCategory
import ai.january.partner.FoodId
import ai.january.partner.JanuaryException
import ai.january.partner.ServingId
import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.transport.apis.FoodsApi
import ai.january.partner.transport.apis.PhotoScanningApi
import ai.january.partner.transport.models.SearchFoodsByNaturalLanguageBody
import ai.january.partner.transport.models.SuggestFoodAlternativesBody
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException

public class FoodsResource internal constructor(
    private val api: FoodsApi,
    private val photoScanningApi: PhotoScanningApi,
) {
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

    public suspend fun searchNaturalLanguage(
        request: SearchFoodsByNaturalLanguageRequest,
    ): SearchFoodsByNaturalLanguageResponse = executeApiCall(
        operation = {
            photoScanningApi.searchFoodsByNaturalLanguage(
                SearchFoodsByNaturalLanguageBody(request.query),
                request.endUserId?.value,
            )
        },
        transform = { bridgeModel(it) },
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

    private fun ai.january.partner.transport.models.FoodSearchResults.toPublic() = FoodSearchResults(
        totalCount = totalCount.toInt(),
        items = items.map { item ->
            FoodSearchItem(
                id = FoodId(item.id),
                name = item.name,
                brandName = item.brandName,
                calories = item.nutrients.calories?.value?.toDouble(),
                protein = item.nutrients.protein?.value?.toDouble(),
                carbohydrates = item.nutrients.carbohydrates?.value?.toDouble(),
                netCarbohydrates = item.nutrients.netCarbohydrates?.value?.toDouble(),
                totalFat = item.nutrients.totalFat?.value?.toDouble(),
                saturatedFat = item.nutrients.saturatedFat?.value?.toDouble(),
                fiber = item.nutrients.fiber?.value?.toDouble(),
                totalSugars = item.nutrients.totalSugars?.value?.toDouble(),
                addedSugars = item.nutrients.addedSugars?.value?.toDouble(),
                sodium = item.nutrients.sodium?.value?.toDouble(),
                potassium = item.nutrients.potassium?.value?.toDouble(),
                cholesterol = item.nutrients.cholesterol?.value?.toDouble(),
                glycemicIndex = item.glycemicIndex?.toDouble(),
                glycemicLoad = item.glycemicLoad?.toDouble(),
                photoUrl = item.imageUrl,
                servings = item.servings.map { serving ->
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
