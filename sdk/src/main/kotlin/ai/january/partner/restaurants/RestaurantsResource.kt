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
            transform = { bridgeModel(it) },
        )
    }

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

