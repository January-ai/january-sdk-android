package ai.january.partner.photos

import ai.january.partner.PartnerUserId
import ai.january.partner.foods.DetectedFood
import ai.january.partner.models.CompleteScanNutritionFacts
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** How much analysis effort a photo scan uses. Both modes return the same [FoodScan] shape and cost the same. */
public enum class AnalysisEffort { NONE, XHIGH }

public data class ScanFoodPhotoRequest(
    public val image: String,
    public val endUserId: PartnerUserId? = null,
    /**
     * Null leaves the choice to the API, which uses the reasoning-based analyzer (as
     * [AnalysisEffort.XHIGH] does); [AnalysisEffort.NONE] uses the standard analyzer.
     */
    public val reasoningEffort: AnalysisEffort? = null,
) {
    public companion object {
        /** Creates a request from local image bytes after resizing and JPEG compression. */
        @JvmStatic
        public fun fromImageData(
            imageData: ByteArray,
            endUserId: PartnerUserId? = null,
            maxDimension: Int = PhotoScanImage.DEFAULT_MAX_DIMENSION,
            jpegQuality: Int = PhotoScanImage.DEFAULT_JPEG_QUALITY,
            reasoningEffort: AnalysisEffort? = null,
        ): ScanFoodPhotoRequest = ScanFoodPhotoRequest(
            image = PhotoScanImage.dataUri(imageData, maxDimension, jpegQuality),
            endUserId = endUserId,
            reasoningEffort = reasoningEffort,
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
    @Json(name = "total_nutrients") public val totalNutrients: CompleteScanNutritionFacts,
    public val detections: List<FoodDetection>,
)

@Deprecated("Use FoodScan.", ReplaceWith("FoodScan"))
public typealias PhotoScan = FoodScan

public data class CorrectPhotoScanRequest(
    public val analysis: FoodScan,
    public val instruction: String,
    public val endUserId: PartnerUserId? = null,
) {
    @Deprecated("Pass the complete prior analysis and an instruction.")
    public constructor(
        mealName: String,
        detections: List<FoodDetection>,
        userInput: String,
        endUserId: PartnerUserId? = null,
    ) : this(FoodScan(mealName, CompleteScanNutritionFacts(), detections), userInput, endUserId)
}
