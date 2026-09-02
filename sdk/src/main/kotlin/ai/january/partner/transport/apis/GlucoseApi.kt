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
     * **API key or client token.**  Predicts the glucose curve a meal will produce for the given profile and body.timezone (required — the IANA timezone the end user is in; the prediction depends on the meal&#39;s local time of day). Optionally personalize by sending cgm_data with the consumed_foods eaten during it (both together; the upstream needs at least five complete days of paired history). Nothing is stored, so there is no end-user identity to send: the request acts as the partner itself.  Callable with a client token carrying the &#x60;glucose:read&#x60; scope.
     * Responses:
     *  - 200: `points`, the predicted glucose curve at 15-minute intervals, plus the meal's overall impact score and suggested chart bounds for rendering it.
     *  - 400: A field is missing or invalid; the message names it and the accepted values.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 504: The prediction took too long; retry.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param predictGlucoseBody
     * @return [GlucosePrediction]
     */
    @POST("v1.2/glucose/predictions")
    suspend fun predictGlucose(@Body predictGlucoseBody: PredictGlucoseBody): Response<GlucosePrediction>

}
