package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.FoodCategory
import ai.january.partner.transport.models.FoodSearchResults
import ai.january.partner.transport.models.SearchFoodsByNaturalLanguageResponse
import ai.january.partner.transport.models.SuggestFoodAlternativesBody
import ai.january.partner.transport.models.SuggestFoodAlternativesResponse

internal interface FoodsApi {
    /**
     * GET v1.2/foods/barcode/{upc}
     * Look up a food by barcode
     * Exact lookup of a food by its barcode. For free-text search, use &#x60;GET /v1.2/foods/search&#x60; instead.
     * Responses:
     *  - 200:
     *  - 400: The barcode is not a 6 to 14 digit number.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 404: No food matches this barcode.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *
     * @param upc The numeric barcode: 6 to 14 digits (UPC-E, UPC-A, EAN-8, EAN-13 or GTIN-14). The example is a Coca-Cola can.
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [FoodSearchResults]
     */
    @GET("v1.2/foods/barcode/{upc}")
    suspend fun lookupFoodByBarcode(@Path("upc") upc: kotlin.String, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<FoodSearchResults>

    /**
     * GET v1.2/foods/search
     * Search foods by name
     * Full-text search over the January food database, returning up to 40 ranked matches. To look up a scanned barcode, use &#x60;GET /v1.2/foods/barcode/{upc}&#x60; instead.
     * Responses:
     *  - 200:
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *
     * @param query The food name to search for.
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @param category Narrows results to one food category. (optional, default to FoodCategory.GENERAL)
     * @param limit Maximum number of results to return. (optional, default to 10)
     * @return [FoodSearchResults]
     */
    @GET("v1.2/foods/search")
    suspend fun searchFoods(@Query("query") query: kotlin.String, @Header("x-end-user-id") xEndUserId: kotlin.String? = null, @Query("category") category: FoodCategory? = FoodCategory.GENERAL, @Query("limit") limit: java.math.BigDecimal? = java.math.BigDecimal("10")): Response<FoodSearchResults>

    /**
     * GET v1.2/foods/search/nlp
     * Parse a meal description into foods
     * Natural-language food search: parses free text like \&quot;a bowl of oatmeal with honey\&quot; into detected foods with quantities and nutrition — the text counterpart of meal-scan. For keyword search over the food database, use &#x60;GET /v1.2/foods/search&#x60;.
     * Responses:
     *  - 200:
     *  - 400: The query is missing or too long.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *
     * @param query Natural-language description of what was eaten; parsed into foods with quantities.
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [SearchFoodsByNaturalLanguageResponse]
     */
    @GET("v1.2/foods/search/nlp")
    suspend fun searchFoodsByNaturalLanguage(@Query("query") query: kotlin.String, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<SearchFoodsByNaturalLanguageResponse>

    /**
     * POST v1.2/foods/{food_id}/alternatives
     * Suggest healthier alternatives for a food
     * Returns healthier alternatives for a food, honoring the given dietary restrictions and preferences. Send &#x60;[\&quot;None\&quot;]&#x60; (not an empty array) to opt out of either. An empty &#x60;alternatives&#x60; result is valid — no suitable alternatives were found.
     * Responses:
     *  - 200:
     *  - 400: A field is missing or a value is not in the allowed vocabulary; the message names it.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 404: No food with this id.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *
     * @param foodId Numeric food id from a search, scan, or NLP result. The example is brown rice.
     * @param suggestFoodAlternativesBody
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [SuggestFoodAlternativesResponse]
     */
    @POST("v1.2/foods/{food_id}/alternatives")
    suspend fun suggestFoodAlternatives(@Path("food_id") foodId: kotlin.Long, @Body suggestFoodAlternativesBody: SuggestFoodAlternativesBody, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<SuggestFoodAlternativesResponse>

}
