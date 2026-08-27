package ai.january.partner.photos

import ai.january.partner.PartnerUserId
import ai.january.partner.foods.DetectedFood
import ai.january.partner.models.CompleteScanNutritionFacts
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

public data class ScanFoodPhotoRequest(public val image: String, public val endUserId: PartnerUserId? = null) {
    public companion object {
        /** Creates a request from local image bytes after resizing and JPEG compression. */
        @JvmStatic
        public fun fromImageData(
            imageData: ByteArray,
            endUserId: PartnerUserId? = null,
            maxDimension: Int = PhotoScanImage.DEFAULT_MAX_DIMENSION,
            jpegQuality: Int = PhotoScanImage.DEFAULT_JPEG_QUALITY,
        ): ScanFoodPhotoRequest = ScanFoodPhotoRequest(
            image = PhotoScanImage.dataUri(imageData, maxDimension, jpegQuality),
            endUserId = endUserId,
        )
    }
}

@JsonClass(generateAdapter = false)
public data class FoodDetection(
    public val food: DetectedFood,
    @Json(name = "confidence_score") public val confidenceScore: String? = null,
)

@JsonClass(generateAdapter = false)
public data class GlucosePredictionPoint(public val minutes: Double, public val value: Double)

@JsonClass(generateAdapter = false)
public data class PhotoScanGlucoseImpact(
    @Json(name = "impact_score") public val impactScore: String,
    public val prediction: List<GlucosePredictionPoint>,
)

@JsonClass(generateAdapter = false)
public data class FoodScan(
    @Json(name = "meal_name") public val mealName: String? = null,
    @Json(name = "total_nutrients") public val totalNutrients: CompleteScanNutritionFacts? = null,
    public val detections: List<FoodDetection>? = null,
    @Json(name = "glucose_impact") public val glucoseImpact: PhotoScanGlucoseImpact? = null,
)

@Deprecated("Use FoodScan.", ReplaceWith("FoodScan"))
public typealias PhotoScan = FoodScan

public data class CorrectPhotoScanRequest(
    public val mealName: String, public val detections: List<FoodDetection>,
    public val userInput: String, public val endUserId: PartnerUserId? = null,
)
