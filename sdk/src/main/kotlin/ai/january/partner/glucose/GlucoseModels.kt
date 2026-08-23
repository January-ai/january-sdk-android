package ai.january.partner.glucose

import ai.january.partner.PartnerUserId
import ai.january.partner.models.FoodSelection
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.OffsetDateTime

public enum class Gender { @Json(name = "male") MALE, @Json(name = "female") FEMALE }
public enum class ActivityLevel {
    @Json(name = "sedentary") SEDENTARY, @Json(name = "lightly_active") LIGHTLY_ACTIVE,
    @Json(name = "moderately_active") MODERATELY_ACTIVE, @Json(name = "very_active") VERY_ACTIVE,
}
public enum class MedicalCondition {
    @Json(name = "Type 2 diabetes") TYPE_2_DIABETES,
    @Json(name = "Prediabetes") PREDIABETES,
    @Json(name = "None of the above") NONE_OF_THE_ABOVE,
}

@JsonClass(generateAdapter = false)
public data class GlucosePredictionProfile(
    public val age: Double, public val gender: Gender, public val height: Double, public val weight: Double,
    @Json(name = "activity_level") public val activityLevel: ActivityLevel? = null,
    @Json(name = "health_conditions") public val healthConditions: List<MedicalCondition>? = null,
)

@JsonClass(generateAdapter = false)
public data class CgmReading(public val timestamp: String, public val value: Double)

@JsonClass(generateAdapter = false)
public data class ConsumedHistoricalServing(public val id: Long, public val quantity: Double)

@JsonClass(generateAdapter = false)
public data class ConsumedHistoricalFood(
    public val timestamp: String, public val id: Long, public val serving: ConsumedHistoricalServing,
)

public data class PredictGlucoseRequest(
    public val userProfile: GlucosePredictionProfile, public val foods: List<FoodSelection>,
    public val startTime: OffsetDateTime, public val cgmData: List<CgmReading>? = null,
    public val consumedFoods: List<ConsumedHistoricalFood>? = null,
    public val endUserId: PartnerUserId? = null, public val timezone: String? = null,
)

public enum class GlucoseImpact {
    @Json(name = "low_impact") LOW_IMPACT, @Json(name = "medium_impact") MEDIUM_IMPACT,
    @Json(name = "high_impact") HIGH_IMPACT,
}

@JsonClass(generateAdapter = false)
public data class GlucosePrediction(
    @Json(name = "cgp") public val curve: List<List<Double>>,
    public val scoring: GlucoseImpact,
    @Json(name = "cgp_min") public val minimum: Double,
    @Json(name = "cgp_max") public val maximum: Double,
)

