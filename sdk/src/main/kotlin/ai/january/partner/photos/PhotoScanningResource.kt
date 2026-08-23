package ai.january.partner.photos

import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.transport.apis.PhotoScanningApi
import ai.january.partner.transport.models.CorrectPhotoScanBody
import ai.january.partner.transport.models.ScanFoodPhotoBody
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

public class PhotoScanningResource internal constructor(private val api: PhotoScanningApi) {
    public suspend fun scan(request: ScanFoodPhotoRequest): PhotoScan = executeApiCall(
        operation = { api.scanFoodPhoto(ScanFoodPhotoBody(request.image), request.endUserId?.value) },
        transform = { bridgeModel(it) },
    )

    public suspend fun correct(request: CorrectPhotoScanRequest): PhotoScan {
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

