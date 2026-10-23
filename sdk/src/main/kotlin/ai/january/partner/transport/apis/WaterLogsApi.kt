package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.CreateWaterLogBody
import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.ListWaterLogsResponse
import ai.january.partner.transport.models.WaterLog

internal interface WaterLogsApi {
    /**
     * POST v1.2/water-logs
     * Log water for a user
     * **API key or client token.**  Records one amount of water, in &#x60;fl_oz&#x60;, &#x60;ml&#x60; or &#x60;cup&#x60; (1–811.5 fl_oz, 30–24000 ml, 0.1–101.4 cup). An end user&#39;s total is capped at 24 L (about 811 fl oz) per day: a log that would take the day of its &#x60;created_at&#x60; past it is refused with &#x60;daily_water_limit_exceeded&#x60;. Save the returned &#x60;id&#x60; to delete the log. Not idempotent: verify with the list endpoint before retrying a timed-out create, since a retry records the water twice and counts twice toward the cap.  Callable with a client token carrying the &#x60;water_logs:write&#x60; scope.
     * Responses:
     *  - 201: The logged entry: its id, the amount in the unit it was sent in, and `created_at` in UTC.
     *  - 400: amount is missing, malformed or out of range, or created_at is malformed (`invalid_request`); or the log would take the end user's total for its day past the daily cap (`daily_water_limit_exceeded`).
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, `POST /v1.2/chat/completions`, `POST /v1.2/literature/search`, `POST /v1.2/extractions`, `GET /v1.2/extractions/{job_id}`, `GET /v1.2/patients/{patient_id}/snapshot`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description, and `end_user_id_mismatch` means the `January-End-User-ID` header disagrees with the end user the token is bound to — omit it, or send exactly that id.
     *  - 429: Two failures share this status; use `code` to choose the response.  - `rate_limited` — a rolling abuse window was reached: the partner-wide v1.2 minute cap, a configured short-window limit, or a client-token limit. Back off and retry; `Retry-After` carries the wait when Redis reports it. - `credit_limit_exceeded` — this call costs more credits than you have left for the billing period, or they are spent. Never returned to an Enterprise account, whose usage past the allowance is billed as overage instead.  Credits reset at the end of your billing period; `GET /v1.2/credits` reports that instant as `resets_at` and remains available when either limit refuses other operations.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param createWaterLogBody
     * @param januaryEndUserID Your stable ID for the end user whose logs this request reads or writes. Opaque to January — use the same ID your system already uses for them.  | Credential | Header | Result | | --- | --- | --- | | API key (&#x60;sk-…&#x60;) | absent | &#x60;400 end_user_id_required&#x60; | | API key (&#x60;sk-…&#x60;) | present | the request acts on that end user | | Client token (&#x60;ct-…&#x60;) | absent | filled in from the token | | Client token (&#x60;ct-…&#x60;) | the end user the token is bound to | accepted | | Client token (&#x60;ct-…&#x60;) | any other end user | &#x60;403 end_user_id_mismatch&#x60; | (optional)
     * @return [WaterLog]
     */
    @POST("v1.2/water-logs")
    suspend fun createWaterLog(@Body createWaterLogBody: CreateWaterLogBody, @Header("January-End-User-ID") januaryEndUserID: kotlin.String? = null): Response<WaterLog>

    /**
     * DELETE v1.2/water-logs/{log_id}
     * Delete a water log
     * **API key or client token.**  Idempotent: deleting an unknown or already-deleted log answers the same 204, so it is safe to retry. The amount no longer counts toward its day&#39;s cap.  Callable with a client token carrying the &#x60;water_logs:write&#x60; scope.
     * Responses:
     *  - 204: The log was deleted; empty body. Idempotent — deleting an already-deleted or unknown log also answers 204.
     *  - 400: log_id is not a UUID.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, `POST /v1.2/chat/completions`, `POST /v1.2/literature/search`, `POST /v1.2/extractions`, `GET /v1.2/extractions/{job_id}`, `GET /v1.2/patients/{patient_id}/snapshot`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description, and `end_user_id_mismatch` means the `January-End-User-ID` header disagrees with the end user the token is bound to — omit it, or send exactly that id.
     *  - 429: Two failures share this status; use `code` to choose the response.  - `rate_limited` — a rolling abuse window was reached: the partner-wide v1.2 minute cap, a configured short-window limit, or a client-token limit. Back off and retry; `Retry-After` carries the wait when Redis reports it. - `credit_limit_exceeded` — this call costs more credits than you have left for the billing period, or they are spent. Never returned to an Enterprise account, whose usage past the allowance is billed as overage instead.  Credits reset at the end of your billing period; `GET /v1.2/credits` reports that instant as `resets_at` and remains available when either limit refuses other operations.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param logId The log id returned when the log was created.
     * @param januaryEndUserID Your stable ID for the end user whose logs this request reads or writes. Opaque to January — use the same ID your system already uses for them.  | Credential | Header | Result | | --- | --- | --- | | API key (&#x60;sk-…&#x60;) | absent | &#x60;400 end_user_id_required&#x60; | | API key (&#x60;sk-…&#x60;) | present | the request acts on that end user | | Client token (&#x60;ct-…&#x60;) | absent | filled in from the token | | Client token (&#x60;ct-…&#x60;) | the end user the token is bound to | accepted | | Client token (&#x60;ct-…&#x60;) | any other end user | &#x60;403 end_user_id_mismatch&#x60; | (optional)
     * @return [Unit]
     */
    @DELETE("v1.2/water-logs/{log_id}")
    suspend fun deleteWaterLog(@Path("log_id") logId: java.util.UUID, @Header("January-End-User-ID") januaryEndUserID: kotlin.String? = null): Response<Unit>

    /**
     * GET v1.2/water-logs
     * List a user&#39;s daily water totals in a date range
     * **API key or client token.**  Returns one total per local calendar day between &#x60;start_date&#x60; and &#x60;end_date&#x60; (both inclusive, in &#x60;timezone&#x60;), oldest first, in the &#x60;unit&#x60; you ask for, rounded to one decimal place. Days with nothing logged are absent. At most 100 days are returned — when more match, the most recent 100. &#x60;start_date&#x60; may reach back at most 5 years from today in &#x60;timezone&#x60;. An empty list is a valid result.  Callable with a client token carrying the &#x60;water_logs:read&#x60; scope.
     * Responses:
     *  - 200: One total per local day with water logged, oldest first; an empty array is a valid result.
     *  - 400: A date is missing, malformed, or the range is inverted; `start_date` is more than 5 years before today in `timezone` (`date_range_too_large`); timezone is missing or not a valid IANA name; or unit is missing or not `fl_oz`/`ml`/`cup`.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, `POST /v1.2/chat/completions`, `POST /v1.2/literature/search`, `POST /v1.2/extractions`, `GET /v1.2/extractions/{job_id}`, `GET /v1.2/patients/{patient_id}/snapshot`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description, and `end_user_id_mismatch` means the `January-End-User-ID` header disagrees with the end user the token is bound to — omit it, or send exactly that id.
     *  - 429: Two failures share this status; use `code` to choose the response.  - `rate_limited` — a rolling abuse window was reached: the partner-wide v1.2 minute cap, a configured short-window limit, or a client-token limit. Back off and retry; `Retry-After` carries the wait when Redis reports it. - `credit_limit_exceeded` — this call costs more credits than you have left for the billing period, or they are spent. Never returned to an Enterprise account, whose usage past the allowance is billed as overage instead.  Credits reset at the end of your billing period; `GET /v1.2/credits` reports that instant as `resets_at` and remains available when either limit refuses other operations.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param startDate First local calendar date in &#x60;timezone&#x60;, inclusive. May be at most 5 years before today in &#x60;timezone&#x60;.
     * @param endDate Last local calendar date in &#x60;timezone&#x60;, inclusive. May equal start_date for a single day. When more than 100 days in the range have water logged, the most recent 100 are returned.
     * @param timezone IANA timezone that defines the local calendar days this range covers. Required: without it the days would be cut in UTC, which silently shifts anything logged near midnight into the wrong day for anyone not on UTC.
     * @param unit The unit every daily total is returned in.
     * @param januaryEndUserID Your stable ID for the end user whose logs this request reads or writes. Opaque to January — use the same ID your system already uses for them.  | Credential | Header | Result | | --- | --- | --- | | API key (&#x60;sk-…&#x60;) | absent | &#x60;400 end_user_id_required&#x60; | | API key (&#x60;sk-…&#x60;) | present | the request acts on that end user | | Client token (&#x60;ct-…&#x60;) | absent | filled in from the token | | Client token (&#x60;ct-…&#x60;) | the end user the token is bound to | accepted | | Client token (&#x60;ct-…&#x60;) | any other end user | &#x60;403 end_user_id_mismatch&#x60; | (optional)
     * @return [ListWaterLogsResponse]
     */
    @GET("v1.2/water-logs")
    suspend fun listWaterLogs(@Query("start_date") startDate: java.time.LocalDate, @Query("end_date") endDate: java.time.LocalDate, @Query("timezone") timezone: kotlin.String, @Query("unit") unit: kotlin.String, @Header("January-End-User-ID") januaryEndUserID: kotlin.String? = null): Response<ListWaterLogsResponse>

}
