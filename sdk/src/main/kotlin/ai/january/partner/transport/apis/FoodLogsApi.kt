package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.CreateFoodLogBody
import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.FoodLog
import ai.january.partner.transport.models.ListFoodLogsResponse
import ai.january.partner.transport.models.UpdateFoodLogBody

internal interface FoodLogsApi {
    /**
     * POST v1.2/food-logs
     * Log foods for a user
     * **API key or client token.**  Creates a food log from food + serving ids (from search or food-analysis results). The response echoes the log hydrated with full nutrition — save its &#x60;id&#x60; to update or delete the log, or fetch it again with GET /v1.2/food-logs/{log_id}. Not idempotent: verify with the list or get endpoint before retrying a timed-out create.  Callable with a client token carrying the &#x60;food_logs:write&#x60; scope.
     * Responses:
     *  - 201: The created log, hydrated with resolved food and nutrition; its id is in the Location header.
     *  - 400: A field is missing or malformed; the message names it.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description, and `end_user_id_mismatch` means the `January-End-User-ID` header disagrees with the end user the token is bound to — omit it, or send exactly that id.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param createFoodLogBody
     * @param januaryEndUserID Your stable ID for the end user whose food logs this request reads or writes. Opaque to January — use the same ID your system already uses for them.  | Credential | Header | Result | | --- | --- | --- | | API key (&#x60;sk-…&#x60;) | absent | &#x60;400 end_user_id_required&#x60; | | API key (&#x60;sk-…&#x60;) | present | the request acts on that end user | | Client token (&#x60;ct-…&#x60;) | absent | filled in from the token | | Client token (&#x60;ct-…&#x60;) | the end user the token is bound to | accepted | | Client token (&#x60;ct-…&#x60;) | any other end user | &#x60;403 end_user_id_mismatch&#x60; | (optional)
     * @return [FoodLog]
     */
    @POST("v1.2/food-logs")
    suspend fun createFoodLog(@Body createFoodLogBody: CreateFoodLogBody, @Header("January-End-User-ID") januaryEndUserID: kotlin.String? = null): Response<FoodLog>

    /**
     * DELETE v1.2/food-logs/{log_id}
     * Delete a food log
     * **API key or client token.**  Idempotent: deleting an unknown or already-deleted log answers the same 204, so it is safe to retry.  Callable with a client token carrying the &#x60;food_logs:write&#x60; scope.
     * Responses:
     *  - 204: The log was deleted; empty body. Idempotent — deleting an already-deleted or unknown log also answers 204.
     *  - 400: log_id is not a UUID.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description, and `end_user_id_mismatch` means the `January-End-User-ID` header disagrees with the end user the token is bound to — omit it, or send exactly that id.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param logId The log id returned when the log was created.
     * @param januaryEndUserID Your stable ID for the end user whose food logs this request reads or writes. Opaque to January — use the same ID your system already uses for them.  | Credential | Header | Result | | --- | --- | --- | | API key (&#x60;sk-…&#x60;) | absent | &#x60;400 end_user_id_required&#x60; | | API key (&#x60;sk-…&#x60;) | present | the request acts on that end user | | Client token (&#x60;ct-…&#x60;) | absent | filled in from the token | | Client token (&#x60;ct-…&#x60;) | the end user the token is bound to | accepted | | Client token (&#x60;ct-…&#x60;) | any other end user | &#x60;403 end_user_id_mismatch&#x60; | (optional)
     * @return [Unit]
     */
    @DELETE("v1.2/food-logs/{log_id}")
    suspend fun deleteFoodLog(@Path("log_id") logId: java.util.UUID, @Header("January-End-User-ID") januaryEndUserID: kotlin.String? = null): Response<Unit>

    /**
     * GET v1.2/food-logs/{log_id}
     * Get a food log
     * **API key or client token.**  Fetches one food log by the id returned when it was created.  Callable with a client token carrying the &#x60;food_logs:read&#x60; scope.
     * Responses:
     *  - 200: The requested food log, hydrated with resolved food and nutrition.
     *  - 400: log_id is not a UUID.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description, and `end_user_id_mismatch` means the `January-End-User-ID` header disagrees with the end user the token is bound to — omit it, or send exactly that id.
     *  - 404: No log with this id for this user.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param logId The log id returned when the log was created.
     * @param januaryEndUserID Your stable ID for the end user whose food logs this request reads or writes. Opaque to January — use the same ID your system already uses for them.  | Credential | Header | Result | | --- | --- | --- | | API key (&#x60;sk-…&#x60;) | absent | &#x60;400 end_user_id_required&#x60; | | API key (&#x60;sk-…&#x60;) | present | the request acts on that end user | | Client token (&#x60;ct-…&#x60;) | absent | filled in from the token | | Client token (&#x60;ct-…&#x60;) | the end user the token is bound to | accepted | | Client token (&#x60;ct-…&#x60;) | any other end user | &#x60;403 end_user_id_mismatch&#x60; | (optional)
     * @return [FoodLog]
     */
    @GET("v1.2/food-logs/{log_id}")
    suspend fun getFoodLog(@Path("log_id") logId: java.util.UUID, @Header("January-End-User-ID") januaryEndUserID: kotlin.String? = null): Response<FoodLog>

    /**
     * GET v1.2/food-logs
     * List a user&#39;s food logs in a date range
     * **API key or client token.**  Returns the logs between &#x60;start_date&#x60; and &#x60;end_date&#x60; (both inclusive local calendar dates in &#x60;timezone&#x60;; the range spans at most 60 days), ordered by timestamp. An empty list is a valid result.  Callable with a client token carrying the &#x60;food_logs:read&#x60; scope.
     * Responses:
     *  - 200: The logs in the requested date range, ordered by timestamp; an empty array is a valid result.
     *  - 400: A date is missing, malformed, or the range is inverted or exceeds 60 days; or timezone is missing or not a valid IANA name.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description, and `end_user_id_mismatch` means the `January-End-User-ID` header disagrees with the end user the token is bound to — omit it, or send exactly that id.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param startDate First local calendar date in &#x60;timezone&#x60;, inclusive.
     * @param endDate Last local calendar date in &#x60;timezone&#x60;, inclusive. May equal start_date for a single day. The inclusive range may not exceed 60 calendar days.
     * @param timezone IANA timezone that defines the local calendar days this range covers — required, so the upstream groups by the same days the caller means.
     * @param januaryEndUserID Your stable ID for the end user whose food logs this request reads or writes. Opaque to January — use the same ID your system already uses for them.  | Credential | Header | Result | | --- | --- | --- | | API key (&#x60;sk-…&#x60;) | absent | &#x60;400 end_user_id_required&#x60; | | API key (&#x60;sk-…&#x60;) | present | the request acts on that end user | | Client token (&#x60;ct-…&#x60;) | absent | filled in from the token | | Client token (&#x60;ct-…&#x60;) | the end user the token is bound to | accepted | | Client token (&#x60;ct-…&#x60;) | any other end user | &#x60;403 end_user_id_mismatch&#x60; | (optional)
     * @return [ListFoodLogsResponse]
     */
    @GET("v1.2/food-logs")
    suspend fun listFoodLogs(@Query("start_date") startDate: java.time.LocalDate, @Query("end_date") endDate: java.time.LocalDate, @Query("timezone") timezone: kotlin.String, @Header("January-End-User-ID") januaryEndUserID: kotlin.String? = null): Response<ListFoodLogsResponse>

    /**
     * PATCH v1.2/food-logs/{log_id}
     * Update a food log
     * **API key or client token.**  Replaces any subset of the log: &#x60;foods&#x60;, &#x60;eaten_at&#x60;, &#x60;name&#x60;. Omitted fields are left unchanged.  Callable with a client token carrying the &#x60;food_logs:write&#x60; scope.
     * Responses:
     *  - 200: The updated log, hydrated with resolved food and nutrition, reflecting the applied changes.
     *  - 400: A field is malformed; the message names it.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description, and `end_user_id_mismatch` means the `January-End-User-ID` header disagrees with the end user the token is bound to — omit it, or send exactly that id.
     *  - 404: No log with this id for this user.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param logId The log id returned when the log was created.
     * @param updateFoodLogBody
     * @param januaryEndUserID Your stable ID for the end user whose food logs this request reads or writes. Opaque to January — use the same ID your system already uses for them.  | Credential | Header | Result | | --- | --- | --- | | API key (&#x60;sk-…&#x60;) | absent | &#x60;400 end_user_id_required&#x60; | | API key (&#x60;sk-…&#x60;) | present | the request acts on that end user | | Client token (&#x60;ct-…&#x60;) | absent | filled in from the token | | Client token (&#x60;ct-…&#x60;) | the end user the token is bound to | accepted | | Client token (&#x60;ct-…&#x60;) | any other end user | &#x60;403 end_user_id_mismatch&#x60; | (optional)
     * @return [FoodLog]
     */
    @PATCH("v1.2/food-logs/{log_id}")
    suspend fun updateFoodLog(@Path("log_id") logId: java.util.UUID, @Body updateFoodLogBody: UpdateFoodLogBody, @Header("January-End-User-ID") januaryEndUserID: kotlin.String? = null): Response<FoodLog>

}
