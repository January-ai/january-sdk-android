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
     * POST v1.2/food-scans/corrections
     * Correct a scan in plain English
     * Revises a scan result. Send back &#x60;meal_name&#x60; and &#x60;detections&#x60; exactly as a photo or text scan returned them (any label works as &#x60;meal_name&#x60; for text scans), plus &#x60;user_input&#x60; describing the correction; the response is a corrected result with recalculated totals. Adjust portions through &#x60;user_input&#x60; (\&quot;it was about half of that\&quot;) rather than editing serving quantities by hand. Nutrient keys a detection omits are filled in as zero automatically; each detection must carry at least one serving.
     * Responses:
     *  - 200:
     *  - 400: A field is missing or a detection is incomplete; the message names the exact detection index and problem.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 504: The vision model took too long; retry.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the 5xx codes.
     *
     * @param correctPhotoScanBody
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [FoodScan]
     */
    @POST("v1.2/food-scans/corrections")
    suspend fun correctPhotoScan(@Body correctPhotoScanBody: CorrectPhotoScanBody, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<FoodScan>

    /**
     * POST v1.2/food-scans/photo
     * Scan a meal photo
     * Analyzes a meal photo and returns the detected foods with their nutrition and an aggregated total. &#x60;image&#x60; accepts either an http(s) URL or a base64 data URI. Analysis can take tens of seconds for complex meals.
     * Responses:
     *  - 200:
     *  - 400: The image is missing or not an http(s) URL / data URI.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 413: The request body exceeds 5 MB. Keep raw images under ~3.5 MB before base64-encoding.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 504: The vision model took too long; retry, ideally with a smaller image.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the 5xx codes.
     *
     * @param scanFoodPhotoBody
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [FoodScan]
     */
    @POST("v1.2/food-scans/photo")
    suspend fun scanFoodPhoto(@Body scanFoodPhotoBody: ScanFoodPhotoBody, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<FoodScan>

    /**
     * POST v1.2/food-scans/text
     * Scan a meal description
     * Parses free text like \&quot;a bowl of oatmeal with honey\&quot; into detected foods with quantities and nutrition — the text counterpart of &#x60;POST /v1.2/food-scans/photo&#x60;. Text scans carry no &#x60;meal_name&#x60; (the caller already has the words). For keyword search over the food database, use &#x60;GET /v1.2/foods&#x60;.
     * Responses:
     *  - 200:
     *  - 400: The text is missing or too long.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded. When Retry-After is present, wait that many seconds; a per-day allowance resets with the day.
     *  - 0: Any other error: the HTTP status plus { message, code, docs_url }. Retry only rate_limited and the 5xx codes.
     *
     * @param searchFoodsByNaturalLanguageBody
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [FoodScan]
     */
    @POST("v1.2/food-scans/text")
    suspend fun searchFoodsByNaturalLanguage(@Body searchFoodsByNaturalLanguageBody: SearchFoodsByNaturalLanguageBody, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<FoodScan>

}
