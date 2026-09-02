package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.CorrectPhotoScanBody
import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.FoodScan
import ai.january.partner.transport.models.ScanFoodPhotoBody
import ai.january.partner.transport.models.SearchFoodsByNaturalLanguageBody

internal interface PhotoScanningApi {
    /**
     * POST v1.2/food-analysis/corrections
     * Correct an analysis in plain English
     * **API key or client token.**  Revises an analysis result. Send back the &#x60;analysis&#x60; object exactly as &#x60;POST /v1.2/food-analysis/image&#x60; or &#x60;/text&#x60; returned it, plus &#x60;instruction&#x60; describing the correction; the response is a corrected result with recalculated totals. Adjust portions through &#x60;instruction&#x60; (\&quot;it was about half of that\&quot;) rather than editing serving quantities by hand. Nutrient keys a detection omits are filled in as zero automatically, and each detection must carry at least one serving.  Callable with a client token carrying the &#x60;food_analysis:write&#x60; scope.
     * Responses:
     *  - 200: The corrected analysis, with recalculated per-item and total nutrition.
     *  - 400: A field is missing or a detection is incomplete; the message names the exact detection index and problem.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 504: The vision model took too long; retry.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param correctPhotoScanBody
     * @return [FoodScan]
     */
    @POST("v1.2/food-analysis/corrections")
    suspend fun correctPhotoScan(@Body correctPhotoScanBody: CorrectPhotoScanBody): Response<FoodScan>

    /**
     * POST v1.2/food-analysis/image
     * Analyze a food or label photo
     * **API key or client token.**  Analyzes a food photo and returns the detected foods with their nutrition and an aggregated total. The photo can show the food itself or a packaged product — the front of the pack, the ingredient list, or the Nutrition Facts panel all work, and a packaged product comes back as a single detection in the usual result shape. &#x60;image&#x60; accepts either an http(s) URL or a base64 data URI. Analysis can take tens of seconds for complex meals.  **Beta:** label reading is in testing — returned nutrition can be incomplete or differ from the printed values, so validate results before relying on them. A photo of nothing but a barcode is rejected; use &#x60;GET /v1.2/foods/barcode/{barcode}&#x60; for those. Best results come from sharp, well-lit photos with the food or the complete panel large in the frame; ~1,024 px on the shorter side is plenty, and downsizing huge images lowers latency.  Callable with a client token carrying the &#x60;food_analysis:write&#x60; scope.
     * Responses:
     *  - 200: The foods detected in the photo, with per-item and total nutrition.
     *  - 400: `invalid_request`: the image is missing or not an http(s) URL / data URI, or the photo shows nothing but a barcode. `image_unreachable`, `image_corrupt`, `image_format_unsupported`, `image_invalid_base64`: the image itself could not be used — the message says what to fix, and retrying the same image fails the same way.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 413: The request body exceeds 5 MB (a transport cap, not an image-quality limit). Keep raw images under ~3.5 MB before base64-encoding, or send a URL instead.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 504: The vision model took too long; retry, ideally with a smaller image.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param scanFoodPhotoBody
     * @return [FoodScan]
     */
    @POST("v1.2/food-analysis/image")
    suspend fun scanFoodPhoto(@Body scanFoodPhotoBody: ScanFoodPhotoBody): Response<FoodScan>

    /**
     * POST v1.2/food-analysis/text
     * Analyze a meal description
     * **API key or client token.**  Parses free text like \&quot;a bowl of oatmeal with honey\&quot; into detected foods with quantities and nutrition — the text counterpart of &#x60;POST /v1.2/food-analysis/image&#x60;. Text analyses return &#x60;meal_name: null&#x60; (the caller already has the words) and grade nothing, so every detection carries &#x60;confidence: null&#x60;. For keyword search over the food database, use &#x60;GET /v1.2/foods&#x60;.  Callable with a client token carrying the &#x60;food_analysis:write&#x60; scope.
     * Responses:
     *  - 200: The foods detected in the text, with per-item and total nutrition.
     *  - 400: `invalid_request`: the text is missing or exceeds 512 characters.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 429: Either a rate limit was exceeded (`code: rate_limited`) or the monthly credit allowance is spent (`code: credit_limit_exceeded`). When Retry-After is present, wait that many seconds; a per-day allowance resets 24 hours after the first request in its window. Credit exhaustion carries no Retry-After and retrying does not help — the allowance returns at the start of the next calendar month. Call `GET /v1.2/credits` for the balance and reset date.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param searchFoodsByNaturalLanguageBody
     * @return [FoodScan]
     */
    @POST("v1.2/food-analysis/text")
    suspend fun searchFoodsByNaturalLanguage(@Body searchFoodsByNaturalLanguageBody: SearchFoodsByNaturalLanguageBody): Response<FoodScan>

}
