package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.SearchRestaurantMenuItemsResponse
import ai.january.partner.transport.models.SearchRestaurantsResponse

internal interface RestaurantsApi {
    /**
     * GET v1.2/restaurants/{restaurant_id}/menu-items
     * Get menu items by restaurant id
     * Load the selected restaurant menu directly by id, without a text query or location. Ordered by food name and id. Use limit and offset to load additional pages; total_count is the full menu count.
     * Responses:
     *  - 200:
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 404: No restaurant exists with this id.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the transient 5xx codes — never not_implemented.
     *
     * @param restaurantId The id from a restaurant search result.
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @param limit  (optional, default to 100)
     * @param offset  (optional, default to 0)
     * @return [SearchRestaurantMenuItemsResponse]
     */
    @GET("v1.2/restaurants/{restaurant_id}/menu-items")
    suspend fun getRestaurantMenuItems(@Path("restaurant_id") restaurantId: kotlin.String, @Header("x-end-user-id") xEndUserId: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = 100, @Query("offset") offset: kotlin.Int? = 0): Response<SearchRestaurantMenuItemsResponse>

    /**
     * GET v1.2/restaurants/menu-items
     * Search menu items near a location
     * Search dishes across restaurants near (&#x60;latitude&#x60;, &#x60;longitude&#x60;). Returns menu items with their nutrition values. &#x60;radius&#x60; and result distances are in meters, e.g. radius&#x3D;5000 for 5 kilometers.
     * Responses:
     *  - 200:
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the transient 5xx codes — never not_implemented.
     *
     * @param query Dish or restaurant name to search for.
     * @param latitude Latitude of the search location, e.g. 37.7749 (San Francisco).
     * @param longitude Longitude of the search location, e.g. -122.4194 (San Francisco).
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @param radius Search radius in meters around (latitude, longitude), e.g. 5000 &#x3D; 5 kilometers. Default 8000 meters (about 5 miles); maximum 17000 (about 10.5 miles). (optional, default to 8000)
     * @param limit Maximum number of results to return. (optional, default to 10)
     * @return [SearchRestaurantMenuItemsResponse]
     */
    @GET("v1.2/restaurants/menu-items")
    suspend fun searchRestaurantMenuItems(@Query("query") query: kotlin.String, @Query("latitude") latitude: java.math.BigDecimal, @Query("longitude") longitude: java.math.BigDecimal, @Header("x-end-user-id") xEndUserId: kotlin.String? = null, @Query("radius") radius: java.math.BigDecimal? = java.math.BigDecimal("8000"), @Query("limit") limit: java.math.BigDecimal? = java.math.BigDecimal("10")): Response<SearchRestaurantMenuItemsResponse>

    /**
     * GET v1.2/restaurants
     * Search restaurants near a location
     * Search restaurants matching &#x60;query&#x60; around (&#x60;latitude&#x60;, &#x60;longitude&#x60;), ranked by proximity. When the name matches no restaurant, results may be menu items (&#x60;type: \&quot;menu_item\&quot;&#x60;). &#x60;radius&#x60; and result distances are in meters.
     * Responses:
     *  - 200:
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the transient 5xx codes — never not_implemented.
     *
     * @param query Restaurant name to search for.
     * @param latitude Latitude of the search location, e.g. 37.7749 (San Francisco).
     * @param longitude Longitude of the search location, e.g. -122.4194 (San Francisco).
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @param radius Search radius in meters around (latitude, longitude), e.g. 5000 &#x3D; 5 kilometers. Default 8000 meters (about 5 miles); maximum 17000 (about 10.5 miles). (optional, default to 8000)
     * @param limit Maximum number of results to return. (optional, default to 10)
     * @return [SearchRestaurantsResponse]
     */
    @GET("v1.2/restaurants")
    suspend fun searchRestaurants(@Query("query") query: kotlin.String, @Query("latitude") latitude: java.math.BigDecimal, @Query("longitude") longitude: java.math.BigDecimal, @Header("x-end-user-id") xEndUserId: kotlin.String? = null, @Query("radius") radius: java.math.BigDecimal? = java.math.BigDecimal("8000"), @Query("limit") limit: java.math.BigDecimal? = java.math.BigDecimal("10")): Response<SearchRestaurantsResponse>

}
