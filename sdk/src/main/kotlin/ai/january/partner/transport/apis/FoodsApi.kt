package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.AutocompleteFoodCategory
import ai.january.partner.transport.models.AutocompleteFoodsResponse
import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.FoodCategory
import ai.january.partner.transport.models.FoodSearchItem
import ai.january.partner.transport.models.FoodSearchResults
import ai.january.partner.transport.models.SuggestFoodAlternativesBody
import ai.january.partner.transport.models.SuggestFoodAlternativesResponse

internal interface FoodsApi {
    /**
     * GET v1.2/foods/autocomplete
     * Autocomplete food names
     * Lightweight food suggestions for a partial name, built for type-ahead (\&quot;ban\&quot; → banana, banana bread, …): generic foods first, then branded, each with its id, name, brand, a thumbnail and calories. Once the user picks one, fetch &#x60;GET /v1.2/foods/{food_id}&#x60; for servings and full nutrition. &#x60;items&#x60; is empty for fewer than 2 letters or digits, no match, or a search-index error (the suggestion service fails open so a typing user is not interrupted); an unreachable service still answers with the standard 502/504.
     * Responses:
     *  - 200:
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the transient 5xx codes — never not_implemented.
     *
     * @param query The characters the user has typed so far. Fewer than 2 letters or digits yield no suggestions.
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @param category Narrows suggestions to one category. Omitted, generic and branded foods are suggested together, generic first. (optional)
     * @param limit Maximum number of suggestions to return. (optional, default to 8)
     * @return [AutocompleteFoodsResponse]
     */
    @GET("v1.2/foods/autocomplete")
    suspend fun autocompleteFoods(@Query("query") query: kotlin.String, @Header("x-end-user-id") xEndUserId: kotlin.String? = null, @Query("category") category: AutocompleteFoodCategory? = null, @Query("limit") limit: java.math.BigDecimal? = java.math.BigDecimal("8")): Response<AutocompleteFoodsResponse>

    /**
     * GET v1.2/foods/{food_id}
     * Get a food
     * One food&#39;s full record — most importantly the **complete list of serving sizes**. Search, barcode, and scan results carry a single default serving; fetch the food here to let an end user pick \&quot;1 cup\&quot; vs \&quot;100 g\&quot; vs \&quot;1 medium\&quot; when logging or predicting. Nutrition is per the default serving, in the shared nutrient vocabulary.
     * Responses:
     *  - 200:
     *  - 400: food_id is not a numeric id; the message shows the expected form.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 404: No food with this id.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the transient 5xx codes — never not_implemented.
     *
     * @param foodId Numeric food id from a search, scan, or detection result.
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [FoodSearchItem]
     */
    @GET("v1.2/foods/{food_id}")
    suspend fun getFood(@Path("food_id") foodId: kotlin.Long, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<FoodSearchItem>

    /**
     * GET v1.2/foods/barcode/{upc}
     * Look up a food by barcode
     * Exact lookup of a food by its barcode. For free-text search, use &#x60;GET /v1.2/foods&#x60; instead.
     * Responses:
     *  - 200:
     *  - 400: The barcode is not a 6 to 14 digit number.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 404: No food matches this barcode.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the transient 5xx codes — never not_implemented.
     *
     * @param upc The numeric barcode: 6 to 14 digits (UPC-E, UPC-A, EAN-8, EAN-13 or GTIN-14). The example is a Coca-Cola can.
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [FoodSearchResults]
     */
    @GET("v1.2/foods/barcode/{upc}")
    suspend fun lookupFoodByBarcode(@Path("upc") upc: kotlin.String, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<FoodSearchResults>

    /**
     * GET v1.2/foods
     * Search foods by name
     * Full-text search over the January food database, returning up to 40 ranked matches. To look up a scanned barcode, use &#x60;GET /v1.2/foods/barcode/{upc}&#x60; instead.
     * Responses:
     *  - 200:
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the transient 5xx codes — never not_implemented.
     *
     * @param query The food name to search for.
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @param category Narrows results to one food category. (optional, default to FoodCategory.GENERAL)
     * @param limit Maximum number of results to return. (optional, default to 10)
     * @return [FoodSearchResults]
     */
    @GET("v1.2/foods")
    suspend fun searchFoods(@Query("query") query: kotlin.String, @Header("x-end-user-id") xEndUserId: kotlin.String? = null, @Query("category") category: FoodCategory? = FoodCategory.GENERAL, @Query("limit") limit: java.math.BigDecimal? = java.math.BigDecimal("10")): Response<FoodSearchResults>

    /**
     * POST v1.2/foods/{food_id}/alternatives
     * Suggest healthier alternatives for a food
     * Returns healthier alternatives for a food, honoring the given dietary restrictions and preferences. Omit either array (or send &#x60;[]&#x60;) if it does not apply. An empty &#x60;alternatives&#x60; result is valid — no suitable alternatives were found.
     * Responses:
     *  - 200:
     *  - 400: A field is missing or a value is not in the allowed vocabulary; the message names it.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 404: No food with this id.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the transient 5xx codes — never not_implemented.
     *
     * @param foodId Numeric food id from a search, scan, or detection result. The example is brown rice.
     * @param suggestFoodAlternativesBody
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [SuggestFoodAlternativesResponse]
     */
    @POST("v1.2/foods/{food_id}/alternatives")
    suspend fun suggestFoodAlternatives(@Path("food_id") foodId: kotlin.Long, @Body suggestFoodAlternativesBody: SuggestFoodAlternativesBody, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<SuggestFoodAlternativesResponse>

}
