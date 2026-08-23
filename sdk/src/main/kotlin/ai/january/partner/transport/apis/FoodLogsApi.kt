package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.CreateFoodLogBody
import ai.january.partner.transport.models.DeleteFoodLogResponse
import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.FoodLog
import ai.january.partner.transport.models.ListFoodLogsResponse
import ai.january.partner.transport.models.UpdateFoodLogBody

internal interface FoodLogsApi {
    /**
     * POST v1.2/food-logs
     * Log foods for a user
     * Creates a food log from food + serving ids (from search, scan, or NLP results). The response echoes the log hydrated with full nutrition — save its &#x60;id&#x60; to update or delete the log. Not idempotent: verify with the list endpoint before retrying a timed-out create.
     * Responses:
     *  - 200:
     *  - 400: A field is missing or malformed; the message names it.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *
     * @param xEndUserId Your stable ID for the end user this request acts on — the data is per-user. Opaque to January; use the same ID your system already uses.
     * @param createFoodLogBody
     * @param xEndUserTimezone Optional: the end user&#39;s IANA timezone, forwarded upstream for local-day date handling. (optional)
     * @return [FoodLog]
     */
    @POST("v1.2/food-logs")
    suspend fun createFoodLog(@Header("x-end-user-id") xEndUserId: kotlin.String, @Body createFoodLogBody: CreateFoodLogBody, @Header("x-end-user-timezone") xEndUserTimezone: kotlin.String? = null): Response<FoodLog>

    /**
     * DELETE v1.2/food-logs/{log_id}
     * Delete a food log
     * Idempotent: deleting an unknown or already-deleted log returns the same success response, so it is safe to retry.
     * Responses:
     *  - 200:
     *  - 400: log_id is not a UUID.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *
     * @param xEndUserId Your stable ID for the end user this request acts on — the data is per-user. Opaque to January; use the same ID your system already uses.
     * @param logId The log id returned when the log was created.
     * @param xEndUserTimezone Optional: the end user&#39;s IANA timezone, forwarded upstream for local-day date handling. (optional)
     * @return [DeleteFoodLogResponse]
     */
    @DELETE("v1.2/food-logs/{log_id}")
    suspend fun deleteFoodLog(@Header("x-end-user-id") xEndUserId: kotlin.String, @Path("log_id") logId: java.util.UUID, @Header("x-end-user-timezone") xEndUserTimezone: kotlin.String? = null): Response<DeleteFoodLogResponse>

    /**
     * GET v1.2/food-logs
     * List a user&#39;s food logs in a date range
     * Returns the logs between &#x60;start&#x60; and &#x60;end&#x60; (both inclusive UTC calendar days), ordered by timestamp. An empty list is a valid result.
     * Responses:
     *  - 200:
     *  - 400: A date is missing, malformed, or the range is inverted; the message names it.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *
     * @param xEndUserId Your stable ID for the end user this request acts on — the data is per-user. Opaque to January; use the same ID your system already uses.
     * @param start First UTC day of the range (inclusive, from 00:00:00 UTC).
     * @param end Last UTC day of the range (inclusive, through 23:59:59 UTC). Must be after start.
     * @param xEndUserTimezone Optional: the end user&#39;s IANA timezone, forwarded upstream for local-day date handling. (optional)
     * @return [ListFoodLogsResponse]
     */
    @GET("v1.2/food-logs")
    suspend fun listFoodLogs(@Header("x-end-user-id") xEndUserId: kotlin.String, @Query("start") start: kotlin.String, @Query("end") end: kotlin.String, @Header("x-end-user-timezone") xEndUserTimezone: kotlin.String? = null): Response<ListFoodLogsResponse>

    /**
     * PATCH v1.2/food-logs/{log_id}
     * Update a food log
     * Replaces any subset of the log: &#x60;foods&#x60;, &#x60;timestamp_utc&#x60;, &#x60;name&#x60;.
     * Responses:
     *  - 200:
     *  - 400: A field is malformed; the message names it.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 404: No log with this id for this user.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *
     * @param xEndUserId Your stable ID for the end user this request acts on — the data is per-user. Opaque to January; use the same ID your system already uses.
     * @param logId The log id returned when the log was created.
     * @param updateFoodLogBody
     * @param xEndUserTimezone Optional: the end user&#39;s IANA timezone, forwarded upstream for local-day date handling. (optional)
     * @return [FoodLog]
     */
    @PATCH("v1.2/food-logs/{log_id}")
    suspend fun updateFoodLog(@Header("x-end-user-id") xEndUserId: kotlin.String, @Path("log_id") logId: java.util.UUID, @Body updateFoodLogBody: UpdateFoodLogBody, @Header("x-end-user-timezone") xEndUserTimezone: kotlin.String? = null): Response<FoodLog>

}
