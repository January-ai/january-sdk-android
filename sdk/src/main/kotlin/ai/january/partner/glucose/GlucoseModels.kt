package ai.january.partner.glucose

import ai.january.partner.PartnerUserId
import ai.january.partner.models.FoodSelection
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.OffsetDateTime

public enum class Sex { @Json(name = "male") MALE, @Json(name = "female") FEMALE }
public typealias Gender = Sex

public enum class HeightUnit(public val value: String) {
    @Json(name = "in") INCHES("in"),
    @Json(name = "cm") CENTIMETERS("cm"),
}

@JsonClass(generateAdapter = false)
public data class Height(public val value: Double, public val unit: HeightUnit)

public enum class WeightUnit(public val value: String) {
    @Json(name = "lb") POUNDS("lb"),
    @Json(name = "kg") KILOGRAMS("kg"),
}

@JsonClass(generateAdapter = false)
public data class Weight(public val value: Double, public val unit: WeightUnit)

public enum class ActivityLevel {
    @Json(name = "sedentary") SEDENTARY,
    @Json(name = "lightly_active") LIGHTLY_ACTIVE,
    @Json(name = "moderately_active") MODERATELY_ACTIVE,
    @Json(name = "very_active") VERY_ACTIVE,
}

public enum class MedicalCondition {
    @Json(name = "type_2_diabetes") TYPE_2_DIABETES,
    @Json(name = "prediabetes") PREDIABETES,
}

@JsonClass(generateAdapter = false)
public data class GlucosePredictionProfile(
    public val age: Double,
    public val sex: Sex,
    public val height: Height,
    public val weight: Weight,
    @Json(name = "activity_level") public val activityLevel: ActivityLevel? = null,
    @Json(name = "health_conditions") public val healthConditions: List<MedicalCondition>? = null,
) {
    public constructor(
        age: Double,
        gender: Sex,
        height: Double,
        weight: Double,
        activityLevel: ActivityLevel? = null,
        healthConditions: List<MedicalCondition>? = null,
    ) : this(
        age = age,
        sex = gender,
        height = Height(height, HeightUnit.INCHES),
        weight = Weight(weight, WeightUnit.POUNDS),
        activityLevel = activityLevel,
        healthConditions = healthConditions,
    )

    public val gender: Sex get() = sex
}

@JsonClass(generateAdapter = false)
public data class CgmReading(public val timestamp: String, public val value: Double)

@JsonClass(generateAdapter = false)
public data class ConsumedHistoricalServing(public val id: String, public val quantity: Double) {
    public constructor(id: Long, quantity: Double) : this(id.toString(), quantity)
}

@JsonClass(generateAdapter = false)
public data class ConsumedHistoricalFood(
    public val timestamp: String,
    public val id: String,
    public val serving: ConsumedHistoricalServing,
)

public data class PredictGlucoseRequest(
    public val userProfile: GlucosePredictionProfile,
    public val foods: List<FoodSelection>,
    public val startTime: OffsetDateTime,
    public val cgmData: List<CgmReading>? = null,
    public val consumedFoods: List<ConsumedHistoricalFood>? = null,
    public val endUserId: PartnerUserId? = null,
    public val timezone: String? = null,
)

@JvmInline
public value class GlucoseImpact(public val value: String) {
    public companion object {
        public val LOW: GlucoseImpact = GlucoseImpact("low")
        public val MEDIUM: GlucoseImpact = GlucoseImpact("medium")
        public val HIGH: GlucoseImpact = GlucoseImpact("high")
    }
}

public data class GlucosePredictionPoint(public val minutes: Double, public val value: Double)
public data class GlucoseChart(public val min: Double?, public val max: Double?)

public data class GlucosePrediction(
    public val prediction: List<GlucosePredictionPoint>,
    public val impact: GlucoseImpact?,
    public val chart: GlucoseChart,
) {
    public val curve: List<List<Double>> get() = prediction.map { listOf(it.minutes, it.value) }
    public val scoring: GlucoseImpact? get() = impact
    public val minimum: Double? get() = chart.min
    public val maximum: Double? get() = chart.max
}
