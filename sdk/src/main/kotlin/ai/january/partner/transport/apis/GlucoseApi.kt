package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.GlucosePrediction
import ai.january.partner.transport.models.PredictGlucoseBody

internal interface GlucoseApi {
    /**
     * POST v1.2/glucose/predictions
     * Predict the glucose response to a meal
     * Predicts the glucose curve a meal will produce for the given profile. Optionally personalize by sending cgm_data with the consumed_foods eaten during it (both together; the upstream needs at least five complete days of paired history). Without x-end-user-id, the partner itself is the acting user.
     * Responses:
     *  - 200:
     *  - 400: A field is missing or invalid; the message names it and the accepted values.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 504: The prediction took too long; retry.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the 5xx codes.
     *
     * @param predictGlucoseBody
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @param xEndUserTimezone The end user&#39;s IANA timezone, e.g. America/New_York. Defaults to UTC when omitted; must be sent when using cgm_data — the history is bucketed into their local days. (optional)
     * @return [GlucosePrediction]
     */
    @POST("v1.2/glucose/predictions")
    suspend fun predictGlucose(@Body predictGlucoseBody: PredictGlucoseBody, @Header("x-end-user-id") xEndUserId: kotlin.String? = null, @Header("x-end-user-timezone") xEndUserTimezone: kotlin.String? = null): Response<GlucosePrediction>

}
