package ai.january.partner.restaurants

import ai.january.partner.JanuaryException
import ai.january.partner.ErrorCategory
import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.transport.apis.RestaurantsApi
import java.math.BigDecimal

public class RestaurantsResource internal constructor(private val api: RestaurantsApi) {
    public suspend fun search(request: SearchRestaurantsRequest): SearchRestaurantsResponse {
        validate(request)
        return executeApiCall(
            operation = {
                api.searchRestaurants(
                    request.query, BigDecimal.valueOf(request.latitude), BigDecimal.valueOf(request.longitude),
                    request.endUserId?.value, BigDecimal.valueOf(request.radius), BigDecimal.valueOf(request.limit.toLong()),
                )
            },
            transform = { bridgeModel(it) },
        )
    }

    public suspend fun searchMenuItems(request: SearchRestaurantsRequest): SearchRestaurantMenuItemsResponse {
        validate(request)
        return executeApiCall(
            operation = {
                api.searchRestaurantMenuItems(
                    request.query, BigDecimal.valueOf(request.latitude), BigDecimal.valueOf(request.longitude),
                    request.endUserId?.value, BigDecimal.valueOf(request.radius), BigDecimal.valueOf(request.limit.toLong()),
                )
            },
            transform = ::mapMenu,
        )
    }

    /** Loads one page of the selected restaurant's menu without text search or coordinates. */
    public suspend fun getMenuItems(request: GetRestaurantMenuItemsRequest): SearchRestaurantMenuItemsResponse {
        if (!Regex("^[A-Za-z0-9_-]{1,256}$").matches(request.restaurantId) || request.limit !in 1..100 || request.offset < 0) {
            throw JanuaryException(ErrorCategory.VALIDATION, "A restaurant id and valid menu pagination are required.")
        }
        return executeApiCall(
            operation = { api.getRestaurantMenuItems(request.restaurantId, request.endUserId?.value, request.limit, request.offset) },
            transform = ::mapMenu,
        )
    }

    private fun mapMenu(response: ai.january.partner.transport.models.SearchRestaurantMenuItemsResponse): SearchRestaurantMenuItemsResponse =
        SearchRestaurantMenuItemsResponse(response.totalCount.toInt(), response.items.map { item ->
                    RestaurantMenuItem(
                        type = item.type, id = item.id, name = item.name, restaurantName = item.restaurantName,
                        isChain = item.isChain, calories = item.nutrients?.calories?.value?.toDouble(),
                        protein = item.nutrients?.protein?.value?.toDouble(), carbohydrates = item.nutrients?.carbohydrates?.value?.toDouble(),
                        netCarbohydrates = item.nutrients?.netCarbohydrates?.value?.toDouble(), totalFat = item.nutrients?.totalFat?.value?.toDouble(),
                        fiber = item.nutrients?.fiber?.value?.toDouble(), totalSugars = item.nutrients?.totalSugars?.value?.toDouble(),
                        addedSugars = item.nutrients?.addedSugars?.value?.toDouble(), glycemicIndex = item.glycemicIndex?.toDouble(),
                        glycemicLoad = item.glycemicLoad?.toDouble(), photoUrl = item.imageUrl, distance = item.distance?.toDouble(),
                        servings = item.servings.map { serving -> ai.january.partner.foods.ServingOption(
                            ai.january.partner.ServingId(serving.id), serving.quantity.toDouble(), serving.unit,
                            serving.scalingFactor.toDouble(), serving.weightGrams?.toDouble(), serving.isPrimary,
                        ) },
                    )
                })

    private fun validate(request: SearchRestaurantsRequest) {
        if (request.query.isBlank() || request.query.length > 256) {
            throw JanuaryException(ErrorCategory.VALIDATION, "Restaurant search query must contain between 1 and 256 characters.")
        }
        if (request.latitude !in -90.0..90.0 || request.longitude !in -180.0..180.0) {
            throw JanuaryException(ErrorCategory.VALIDATION, "Restaurant coordinates are outside the valid range.")
        }
        if (request.radius !in 1.0..17_000.0 || request.limit !in 1..100) {
            throw JanuaryException(ErrorCategory.VALIDATION, "Restaurant radius or limit is outside the valid range.")
        }
    }
}

