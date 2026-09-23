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
     * **API key or client token.**  Lightweight food suggestions for a partial name, built for type-ahead (\&quot;ban\&quot; → banana, banana bread, …): generic foods first, then branded, each with its id, name, brand, a thumbnail and calories. Once the user picks one, fetch &#x60;GET /v1.2/foods/{food_id}&#x60; for servings and full nutrition. &#x60;items&#x60; is empty for fewer than 2 letters or digits, no match, or a search-index error (the suggestion service fails open so a typing user is not interrupted); an unreachable service still answers with the standard 502/504.  Callable with a client token carrying the &#x60;foods:read&#x60; scope.
     * Responses:
     *  - 200: Type-ahead suggestions for the partial name, generic before branded; empty when fewer than 2 characters were given or nothing matches.
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, `POST /v1.2/chat/completions`, `POST /v1.2/literature/search`, `POST /v1.2/extractions`, `GET /v1.2/extractions/{job_id}`, `GET /v1.2/patients/{patient_id}/snapshot`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 429: Three different failures share this status, and the `code` is what tells them apart — two of them must not be retried.  - `rate_limited` — a window short enough to wait out: a per-endpoint limit configured for your account, or the rolling 24-hour burst guard over the monthly ceiling. `Retry-After` carries the wait whenever it is known, and is at most a day. - `request_limit_exceeded` — the monthly request allowance on your account is spent. The message names your plan where the plan set that number, and on the free tier, whose allowance the default *is*. A limit agreed with us, or the default standing in for a **paid** tier the catalog states no ceiling for, is given as a number without naming a plan. - `credit_limit_exceeded` — the monthly credit allowance is spent.  The last two reopen at the start of the next calendar month, the same instant as each other, and neither sends `Retry-After`: a wait of up to four weeks is not something to sleep on, and it does not survive a 32-bit timer. The message names the reset instant, and `GET /v1.2/credits` returns it as `resets_at` along with your balance — that endpoint keeps answering when both of these refuse.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param query The characters the user has typed so far. Fewer than 2 letters or digits yield no suggestions.
     * @param type Narrows suggestions to one kind of food. Omitted, generic and branded foods are suggested together, generic first. (optional)
     * @param limit Maximum number of suggestions to return. (optional, default to 8)
     * @return [AutocompleteFoodsResponse]
     */
    @GET("v1.2/foods/autocomplete")
    suspend fun autocompleteFoods(@Query("query") query: kotlin.String, @Query("type") type: AutocompleteFoodCategory? = null, @Query("limit") limit: kotlin.Int? = 8): Response<AutocompleteFoodsResponse>

    /**
     * GET v1.2/foods/{food_id}
     * Get a food
     * **API key or client token.**  One food&#39;s full record — most importantly the **complete list of serving sizes**. Search, barcode, and food-analysis results carry a single default serving; fetch the food here to let an end user pick \&quot;1 cup\&quot; vs \&quot;100 g\&quot; vs \&quot;1 medium\&quot; when logging or predicting. Nutrition is per the default serving, in the shared nutrient vocabulary.  Callable with a client token carrying the &#x60;foods:read&#x60; scope.
     * Responses:
     *  - 200: The food's full record, including the complete list of serving sizes.
     *  - 400: food_id is not a numeric id; the message shows the expected form.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, `POST /v1.2/chat/completions`, `POST /v1.2/literature/search`, `POST /v1.2/extractions`, `GET /v1.2/extractions/{job_id}`, `GET /v1.2/patients/{patient_id}/snapshot`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 404: No food with this id.
     *  - 429: Three different failures share this status, and the `code` is what tells them apart — two of them must not be retried.  - `rate_limited` — a window short enough to wait out: a per-endpoint limit configured for your account, or the rolling 24-hour burst guard over the monthly ceiling. `Retry-After` carries the wait whenever it is known, and is at most a day. - `request_limit_exceeded` — the monthly request allowance on your account is spent. The message names your plan where the plan set that number, and on the free tier, whose allowance the default *is*. A limit agreed with us, or the default standing in for a **paid** tier the catalog states no ceiling for, is given as a number without naming a plan. - `credit_limit_exceeded` — the monthly credit allowance is spent.  The last two reopen at the start of the next calendar month, the same instant as each other, and neither sends `Retry-After`: a wait of up to four weeks is not something to sleep on, and it does not survive a 32-bit timer. The message names the reset instant, and `GET /v1.2/credits` returns it as `resets_at` along with your balance — that endpoint keeps answering when both of these refuse.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param foodId Food id from a search or food-analysis result.
     * @return [FoodSearchItem]
     */
    @GET("v1.2/foods/{food_id}")
    suspend fun getFood(@Path("food_id") foodId: kotlin.String): Response<FoodSearchItem>

    /**
     * GET v1.2/foods/barcode/{barcode}
     * Look up a food by barcode
     * **API key or client token.**  Exact lookup of the food a barcode names — one food, not a list. The &#x60;barcode&#x60; on the returned food is the database&#39;s normalized form and may differ from the digits you scanned in leading zeros, so display it rather than comparing it. For free-text search, use &#x60;GET /v1.2/foods&#x60; instead.  Callable with a client token carrying the &#x60;foods:read&#x60; scope.
     * Responses:
     *  - 200: The single food the barcode identifies.
     *  - 400: The barcode is not a 6 to 14 digit number.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, `POST /v1.2/chat/completions`, `POST /v1.2/literature/search`, `POST /v1.2/extractions`, `GET /v1.2/extractions/{job_id}`, `GET /v1.2/patients/{patient_id}/snapshot`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 404: No food matches this barcode.
     *  - 429: Three different failures share this status, and the `code` is what tells them apart — two of them must not be retried.  - `rate_limited` — a window short enough to wait out: a per-endpoint limit configured for your account, or the rolling 24-hour burst guard over the monthly ceiling. `Retry-After` carries the wait whenever it is known, and is at most a day. - `request_limit_exceeded` — the monthly request allowance on your account is spent. The message names your plan where the plan set that number, and on the free tier, whose allowance the default *is*. A limit agreed with us, or the default standing in for a **paid** tier the catalog states no ceiling for, is given as a number without naming a plan. - `credit_limit_exceeded` — the monthly credit allowance is spent.  The last two reopen at the start of the next calendar month, the same instant as each other, and neither sends `Retry-After`: a wait of up to four weeks is not something to sleep on, and it does not survive a 32-bit timer. The message names the reset instant, and `GET /v1.2/credits` returns it as `resets_at` along with your balance — that endpoint keeps answering when both of these refuse.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param barcode The numeric barcode: 6 to 14 digits (UPC-E, UPC-A, EAN-8, EAN-13 or GTIN-14). The example is a Coca-Cola can.
     * @return [FoodSearchItem]
     */
    @GET("v1.2/foods/barcode/{barcode}")
    suspend fun lookupFoodByBarcode(@Path("barcode") barcode: kotlin.String): Response<FoodSearchItem>

    /**
     * GET v1.2/foods
     * Search foods by name
     * **API key or client token.**  Full-text search over the January food database, returning up to 50 ranked matches per call. Generic foods, branded products and recipes are searched together unless &#x60;type&#x60; narrows it to one; page deeper with &#x60;offset&#x60;. To look up a scanned barcode, use &#x60;GET /v1.2/foods/barcode/{barcode}&#x60; instead.  Callable with a client token carrying the &#x60;foods:read&#x60; scope.
     * Responses:
     *  - 200: Foods matching the query, best match first; empty when nothing matches or `offset` is past the last result.
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, `POST /v1.2/chat/completions`, `POST /v1.2/literature/search`, `POST /v1.2/extractions`, `GET /v1.2/extractions/{job_id}`, `GET /v1.2/patients/{patient_id}/snapshot`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 429: Three different failures share this status, and the `code` is what tells them apart — two of them must not be retried.  - `rate_limited` — a window short enough to wait out: a per-endpoint limit configured for your account, or the rolling 24-hour burst guard over the monthly ceiling. `Retry-After` carries the wait whenever it is known, and is at most a day. - `request_limit_exceeded` — the monthly request allowance on your account is spent. The message names your plan where the plan set that number, and on the free tier, whose allowance the default *is*. A limit agreed with us, or the default standing in for a **paid** tier the catalog states no ceiling for, is given as a number without naming a plan. - `credit_limit_exceeded` — the monthly credit allowance is spent.  The last two reopen at the start of the next calendar month, the same instant as each other, and neither sends `Retry-After`: a wait of up to four weeks is not something to sleep on, and it does not survive a 32-bit timer. The message names the reset instant, and `GET /v1.2/credits` returns it as `resets_at` along with your balance — that endpoint keeps answering when both of these refuse.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param query The food name to search for.
     * @param type Narrows results to one kind of food. Omitted, all three are searched and returned as one ranked list, so a partner who does not care which kind a match is does not have to ask three times. (optional)
     * @param limit Maximum number of results to return in one call. Values above 50 are treated as 50, so page by 50 if you asked for more. (optional, default to 10)
     * @param offset Number of results to skip, for paging: a page shorter than &#x60;limit&#x60; is the last one. (optional, default to 0)
     * @return [FoodSearchResults]
     */
    @GET("v1.2/foods")
    suspend fun searchFoods(@Query("query") query: kotlin.String, @Query("type") type: FoodCategory? = null, @Query("limit") limit: kotlin.Int? = 10, @Query("offset") offset: kotlin.Int? = 0): Response<FoodSearchResults>

    /**
     * POST v1.2/foods/{food_id}/alternatives
     * Suggest healthier alternatives for a food
     * **API key or client token.**  Returns healthier alternatives for a food, honoring the given dietary restrictions and preferences. Omit either array (or send &#x60;[]&#x60;) if it does not apply. An empty &#x60;alternatives&#x60; result is valid — no suitable alternatives were found.  Callable with a client token carrying the &#x60;foods:read&#x60; scope.
     * Responses:
     *  - 200: Healthier alternatives honoring the restrictions and preferences; may be empty.
     *  - 400: A field is missing or a value is not in the allowed vocabulary; the message names it.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, `POST /v1.2/chat/completions`, `POST /v1.2/literature/search`, `POST /v1.2/extractions`, `GET /v1.2/extractions/{job_id}`, `GET /v1.2/patients/{patient_id}/snapshot`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 404: No food with this id.
     *  - 429: Three different failures share this status, and the `code` is what tells them apart — two of them must not be retried.  - `rate_limited` — a window short enough to wait out: a per-endpoint limit configured for your account, or the rolling 24-hour burst guard over the monthly ceiling. `Retry-After` carries the wait whenever it is known, and is at most a day. - `request_limit_exceeded` — the monthly request allowance on your account is spent. The message names your plan where the plan set that number, and on the free tier, whose allowance the default *is*. A limit agreed with us, or the default standing in for a **paid** tier the catalog states no ceiling for, is given as a number without naming a plan. - `credit_limit_exceeded` — the monthly credit allowance is spent.  The last two reopen at the start of the next calendar month, the same instant as each other, and neither sends `Retry-After`: a wait of up to four weeks is not something to sleep on, and it does not survive a 32-bit timer. The message names the reset instant, and `GET /v1.2/credits` returns it as `resets_at` along with your balance — that endpoint keeps answering when both of these refuse.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param foodId Food id from a search or food-analysis result. The example is brown rice.
     * @param suggestFoodAlternativesBody
     * @return [SuggestFoodAlternativesResponse]
     */
    @POST("v1.2/foods/{food_id}/alternatives")
    suspend fun suggestFoodAlternatives(@Path("food_id") foodId: kotlin.String, @Body suggestFoodAlternativesBody: SuggestFoodAlternativesBody): Response<SuggestFoodAlternativesResponse>

}
