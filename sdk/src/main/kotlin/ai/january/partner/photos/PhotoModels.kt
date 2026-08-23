package ai.january.partner.photos

import ai.january.partner.PartnerUserId
import ai.january.partner.foods.DetectedFood
import ai.january.partner.models.CompleteScanNutritionFacts
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

public data class ScanFoodPhotoRequest(public val image: String, public val endUserId: PartnerUserId? = null)

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
public data class PhotoScan(
    @Json(name = "meal_name") public val mealName: String? = null,
    @Json(name = "total_nutrients") public val totalNutrients: CompleteScanNutritionFacts? = null,
    public val detections: List<FoodDetection>? = null,
    @Json(name = "glucose_impact") public val glucoseImpact: PhotoScanGlucoseImpact? = null,
)

public data class CorrectPhotoScanRequest(
    public val mealName: String, public val detections: List<FoodDetection>,
    public val userInput: String, public val endUserId: PartnerUserId? = null,
)

