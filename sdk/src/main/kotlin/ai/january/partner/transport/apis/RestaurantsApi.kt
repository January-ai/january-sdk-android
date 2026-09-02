package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.GetRestaurantMenuItemsResponse
import ai.january.partner.transport.models.SearchRestaurantMenuItemsResponse
import ai.january.partner.transport.models.SearchRestaurantsResponse

internal interface RestaurantsApi {
    /**
     * GET v1.2/restaurants/{restaurant_id}/menu-items
     * List a restaurant&#39;s menu items
     * **API key or client token.**  The menu of one restaurant, by the &#x60;id&#x60; a &#x60;GET /v1.2/restaurants&#x60; result carries — a listing, not a search. Items come ordered by name with the nutrition their menu source publishes; each carries one serving, and &#x60;GET /v1.2/foods/{food_id}&#x60; returns the complete list. Page a long menu with &#x60;limit&#x60; and &#x60;offset&#x60;: a page shorter than &#x60;limit&#x60; is the last one. To find dishes across restaurants near a location, use &#x60;GET /v1.2/menu-items&#x60;.  Callable with a client token carrying the &#x60;restaurants:read&#x60; scope.
     * Responses:
     *  - 200: The restaurant's menu items, by name. Empty when the restaurant has no menu on record or `offset` is past its end.
     *  - 400: restaurant_id, limit or offset is malformed; the message names the parameter and the accepted values.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 404: No restaurant with this id.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param restaurantId Restaurant id from a &#x60;GET /v1.2/restaurants&#x60; result.
     * @param limit Maximum number of menu items to return. Raise it, or page with &#x60;offset&#x60;, for a long menu. (optional, default to 100)
     * @param offset Number of menu items to skip, for paging: a page shorter than &#x60;limit&#x60; is the last one. (optional, default to 0)
     * @return [GetRestaurantMenuItemsResponse]
     */
    @GET("v1.2/restaurants/{restaurant_id}/menu-items")
    suspend fun getRestaurantMenuItems(@Path("restaurant_id") restaurantId: kotlin.String, @Query("limit") limit: kotlin.Int? = 100, @Query("offset") offset: kotlin.Int? = 0): Response<GetRestaurantMenuItemsResponse>

    /**
     * GET v1.2/menu-items
     * Search menu items near a location
     * **API key or client token.**  Search dishes across restaurants near (&#x60;latitude&#x60;, &#x60;longitude&#x60;), with the nutrition each menu source publishes. Use &#x60;radius_meters&#x60; to widen or narrow the search, e.g. &#x60;radius_meters&#x3D;5000&#x60; for 5 kilometers; each result reports its own &#x60;distance_meters&#x60;. To find the restaurants themselves, use &#x60;GET /v1.2/restaurants&#x60;.  Callable with a client token carrying the &#x60;restaurants:read&#x60; scope.
     * Responses:
     *  - 200: Dishes near the location matching the query, ranked by proximity, each with the nutrition its source publishes. Empty when nothing matches.
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param query Dish or restaurant name to search for.
     * @param latitude Latitude of the search location, e.g. 37.7749 (San Francisco).
     * @param longitude Longitude of the search location, e.g. -122.4194 (San Francisco).
     * @param radiusMeters Search radius in meters around (latitude, longitude), e.g. 5000 &#x3D; 5 kilometers. A fractional value is accepted and rounded. Default 8000 meters (about 5 miles); maximum 50000 (about 31 miles). (optional, default to 8000)
     * @param limit Maximum number of results to return. (optional, default to 10)
     * @return [SearchRestaurantMenuItemsResponse]
     */
    @GET("v1.2/menu-items")
    suspend fun searchRestaurantMenuItems(@Query("query") query: kotlin.String, @Query("latitude") latitude: java.math.BigDecimal, @Query("longitude") longitude: java.math.BigDecimal, @Query("radius_meters") radiusMeters: java.math.BigDecimal? = java.math.BigDecimal("8000"), @Query("limit") limit: kotlin.Int? = 10): Response<SearchRestaurantMenuItemsResponse>

    /**
     * GET v1.2/restaurants
     * Search restaurants near a location
     * **API key or client token.**  Search restaurants matching &#x60;query&#x60; around (&#x60;latitude&#x60;, &#x60;longitude&#x60;), ranked by proximity. Every result is a restaurant — &#x60;type&#x60; is always &#x60;restaurant&#x60;. To search the dishes those restaurants serve, use &#x60;GET /v1.2/menu-items&#x60;.  Callable with a client token carrying the &#x60;restaurants:read&#x60; scope.
     * Responses:
     *  - 200: Restaurants matching the query near the location, ranked by proximity. Empty when nothing matches.
     *  - 400: A parameter is missing or invalid; the message names the parameter and the accepted values.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param query Restaurant name to search for.
     * @param latitude Latitude of the search location, e.g. 37.7749 (San Francisco).
     * @param longitude Longitude of the search location, e.g. -122.4194 (San Francisco).
     * @param radiusMeters Search radius in meters around (latitude, longitude), e.g. 5000 &#x3D; 5 kilometers. A fractional value is accepted and rounded. Default 8000 meters (about 5 miles); maximum 50000 (about 31 miles). (optional, default to 8000)
     * @param limit Maximum number of results to return. (optional, default to 10)
     * @return [SearchRestaurantsResponse]
     */
    @GET("v1.2/restaurants")
    suspend fun searchRestaurants(@Query("query") query: kotlin.String, @Query("latitude") latitude: java.math.BigDecimal, @Query("longitude") longitude: java.math.BigDecimal, @Query("radius_meters") radiusMeters: java.math.BigDecimal? = java.math.BigDecimal("8000"), @Query("limit") limit: kotlin.Int? = 10): Response<SearchRestaurantsResponse>

}
