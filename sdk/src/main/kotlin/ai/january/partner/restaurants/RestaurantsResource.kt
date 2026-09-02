package ai.january.partner.restaurants

import ai.january.partner.JanuaryException
import ai.january.partner.ErrorCategory
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
                    BigDecimal.valueOf(request.radius), request.limit,
                )
            },
            transform = { response -> SearchRestaurantsResponse(response.items.size, response.items.map { item ->
                Restaurant(
                    type = RestaurantResultType.RESTAURANT,
                    id = item.id,
                    name = item.name,
                    isChain = item.isChain,
                    distance = item.distanceMeters?.toDouble(),
                    city = item.city,
                    address1 = item.address1,
                    address2 = item.address2,
                )
            }) },
        )
    }

    public suspend fun searchMenuItems(request: SearchRestaurantsRequest): SearchRestaurantMenuItemsResponse {
        validate(request)
        return executeApiCall(
            operation = {
                api.searchRestaurantMenuItems(
                    request.query, BigDecimal.valueOf(request.latitude), BigDecimal.valueOf(request.longitude),
                    BigDecimal.valueOf(request.radius), request.limit,
                )
            },
            transform = ::mapMenu,
        )
    }

    /** Loads one page of the selected restaurant's menu without text search or coordinates. */
    public suspend fun getMenuItems(request: GetRestaurantMenuItemsRequest): GetRestaurantMenuItemsResponse {
        if (!Regex("^[A-Za-z0-9_-]{1,256}$").matches(request.restaurantId) || request.limit !in 1..100 || request.offset < 0) {
            throw JanuaryException(ErrorCategory.VALIDATION, "A restaurant id and valid menu pagination are required.")
        }
        return executeApiCall(
            operation = { api.getRestaurantMenuItems(request.restaurantId, request.limit, request.offset) },
            transform = { response -> GetRestaurantMenuItemsResponse(response.items.map { item ->
                RestaurantMenuEntry(
                    id = item.id,
                    name = item.name,
                    calories = item.nutrients.calories?.value?.toDouble(),
                    protein = item.nutrients.protein?.value?.toDouble(),
                    carbohydrates = item.nutrients.carbohydrates?.value?.toDouble(),
                    netCarbohydrates = item.nutrients.netCarbohydrates?.value?.toDouble(),
                    totalFat = item.nutrients.totalFat?.value?.toDouble(),
                    fiber = item.nutrients.fiber?.value?.toDouble(),
                    totalSugars = item.nutrients.totalSugars?.value?.toDouble(),
                    addedSugars = item.nutrients.addedSugars?.value?.toDouble(),
                    glycemicIndex = item.glycemicIndex?.toDouble(),
                    glycemicLoad = item.glycemicLoad?.toDouble(),
                    servings = item.servings.map(::mapServing),
                )
            }) },
        )
    }

    private fun mapMenu(response: ai.january.partner.transport.models.SearchRestaurantMenuItemsResponse): SearchRestaurantMenuItemsResponse =
        SearchRestaurantMenuItemsResponse(response.items.size, response.items.map { item ->
                    RestaurantMenuItem(
                        type = item.type.value, id = item.id, name = item.name, restaurantName = item.restaurantName,
                        isChain = item.isChain, calories = item.nutrients.calories?.value?.toDouble(),
                        protein = item.nutrients.protein?.value?.toDouble(), carbohydrates = item.nutrients.carbohydrates?.value?.toDouble(),
                        netCarbohydrates = item.nutrients.netCarbohydrates?.value?.toDouble(), totalFat = item.nutrients.totalFat?.value?.toDouble(),
                        fiber = item.nutrients.fiber?.value?.toDouble(), totalSugars = item.nutrients.totalSugars?.value?.toDouble(),
                        addedSugars = item.nutrients.addedSugars?.value?.toDouble(), glycemicIndex = item.glycemicIndex?.toDouble(),
                        glycemicLoad = item.glycemicLoad?.toDouble(), photoUrl = item.imageUrl, distance = item.distanceMeters?.toDouble(),
                        servings = item.servings.map(::mapServing),
                    )
                })

    private fun validate(request: SearchRestaurantsRequest) {
        if (request.query.isBlank() || request.query.length > 256) {
            throw JanuaryException(ErrorCategory.VALIDATION, "Restaurant search query must contain between 1 and 256 characters.")
        }
        if (request.latitude !in -90.0..90.0 || request.longitude !in -180.0..180.0) {
            throw JanuaryException(ErrorCategory.VALIDATION, "Restaurant coordinates are outside the valid range.")
        }
        if (request.radius !in 1.0..50_000.0 || request.limit !in 1..100) {
            throw JanuaryException(ErrorCategory.VALIDATION, "Restaurant radius or limit is outside the valid range.")
        }
    }
}

private fun mapServing(serving: ai.january.partner.transport.models.ServingOption) =
    ai.january.partner.foods.ServingOption(
        serving.id?.let { ai.january.partner.ServingId(it) },
        serving.quantity?.toDouble(),
        serving.unit,
        serving.scalingFactor?.toDouble() ?: 1.0,
        serving.weightGrams?.toDouble(),
        serving.isPrimary,
    )
