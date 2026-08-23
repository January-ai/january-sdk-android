package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.CorrectPhotoScanBody
import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.PhotoScan
import ai.january.partner.transport.models.ScanFoodPhotoBody

internal interface PhotoScanningApi {
    /**
     * POST v1.2/meal-scan/fix-ai
     * Correct a scan with a plain-English instruction
     * Refines a meal-scan result. Send back &#x60;meal_name&#x60; and &#x60;detections&#x60; exactly as the scan returned them, plus &#x60;user_input&#x60; describing the correction; the response is a corrected scan result with recalculated totals.
     * Responses:
     *  - 200:
     *  - 400: A field is missing or a detection is incomplete; the message names the exact detection index and missing keys.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *  - 504: The vision model took too long; retry.
     *
     * @param correctPhotoScanBody
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [PhotoScan]
     */
    @POST("v1.2/meal-scan/fix-ai")
    suspend fun correctPhotoScan(@Body correctPhotoScanBody: CorrectPhotoScanBody, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<PhotoScan>

    /**
     * POST v1.2/meal-scan
     * Detect foods and nutrition in a meal photo
     * Analyzes a meal photo and returns the detected foods with their nutrition and an aggregated total. &#x60;image&#x60; accepts either an http(s) URL or a base64 data URI. Analysis can take tens of seconds for complex meals.
     * Responses:
     *  - 200:
     *  - 400: The image is missing or not an http(s) URL / data URI.
     *  - 401: The Authorization header is missing or the API key is invalid.
     *  - 413: The request body exceeds 5 MB. Keep raw images under ~3.5 MB before base64-encoding.
     *  - 429: A rate limit was exceeded; retry after a short delay.
     *  - 504: The vision model took too long; retry, ideally with a smaller image.
     *
     * @param scanFoodPhotoBody
     * @param xEndUserId Optional: your stable ID for the end user this request acts on behalf of. Opaque to January. (optional)
     * @return [PhotoScan]
     */
    @POST("v1.2/meal-scan")
    suspend fun scanFoodPhoto(@Body scanFoodPhotoBody: ScanFoodPhotoBody, @Header("x-end-user-id") xEndUserId: kotlin.String? = null): Response<PhotoScan>

}
