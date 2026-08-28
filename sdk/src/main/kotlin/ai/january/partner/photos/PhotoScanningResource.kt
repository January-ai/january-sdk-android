package ai.january.partner.photos

import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.foods.SearchFoodsByNaturalLanguageResponse
import ai.january.partner.transport.apis.PhotoScanningApi
import ai.january.partner.transport.models.CorrectPhotoScanBody
import ai.january.partner.transport.models.ScanFoodPhotoBody
import ai.january.partner.transport.models.SearchFoodsByNaturalLanguageBody
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Operations that analyze food from photos or natural-language descriptions. */
public class FoodAnalysisResource internal constructor(private val api: PhotoScanningApi) {
    public suspend fun analyzePhoto(request: ScanFoodPhotoRequest): FoodScan = executeApiCall(
        operation = { api.scanFoodPhoto(ScanFoodPhotoBody(request.image), request.endUserId?.value) },
        transform = { bridgeModel(it) },
    )

    public suspend fun analyzeDescription(
        request: ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest,
    ): SearchFoodsByNaturalLanguageResponse = executeApiCall(
        operation = {
            api.searchFoodsByNaturalLanguage(
                SearchFoodsByNaturalLanguageBody(request.query),
                request.endUserId?.value,
            )
        },
        transform = { bridgeModel(it) },
    )

    public suspend fun correct(request: CorrectPhotoScanRequest): FoodScan {
        val body: CorrectPhotoScanBody = bridgeModel(
            CorrectBody(request.mealName, request.detections, request.userInput),
        )
        return executeApiCall(
            operation = { api.correctPhotoScan(body, request.endUserId?.value) },
            transform = { bridgeModel(it) },
        )
    }
}

@JsonClass(generateAdapter = false)
private data class CorrectBody(
    @Json(name = "meal_name") val mealName: String,
    val detections: List<FoodDetection>,
    @Json(name = "user_input") val userInput: String,
)
