package ai.january.partner.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
public data class NutrientAmount(public val value: Double, public val unit: String)

@JsonClass(generateAdapter = false)
public data class CompleteScanNutritionFacts(
    public val calories: NutrientAmount? = null,
    public val protein: NutrientAmount? = null,
    public val carbohydrates: NutrientAmount? = null,
    @Json(name = "net_carbohydrates") public val netCarbohydrates: NutrientAmount? = null,
    @Json(name = "total_fat") public val totalFat: NutrientAmount? = null,
    @Json(name = "saturated_fat") public val saturatedFat: NutrientAmount? = null,
    public val fiber: NutrientAmount? = null,
    @Json(name = "total_sugars") public val totalSugars: NutrientAmount? = null,
    @Json(name = "added_sugars") public val addedSugars: NutrientAmount? = null,
    public val sodium: NutrientAmount? = null,
)

@JsonClass(generateAdapter = false)
public data class NutritionFacts(
    public val calories: NutrientAmount? = null,
    public val protein: NutrientAmount? = null,
    public val carbohydrates: NutrientAmount? = null,
    @Json(name = "net_carbohydrates") public val netCarbohydrates: NutrientAmount? = null,
    @Json(name = "total_fat") public val totalFat: NutrientAmount? = null,
    @Json(name = "trans_fat") public val transFat: NutrientAmount? = null,
    @Json(name = "saturated_fat") public val saturatedFat: NutrientAmount? = null,
    public val fiber: NutrientAmount? = null,
    @Json(name = "total_sugars") public val totalSugars: NutrientAmount? = null,
    @Json(name = "added_sugars") public val addedSugars: NutrientAmount? = null,
    public val cholesterol: NutrientAmount? = null,
    public val calcium: NutrientAmount? = null,
    public val iron: NutrientAmount? = null,
    public val potassium: NutrientAmount? = null,
    public val sodium: NutrientAmount? = null,
    @Json(name = "vitamin_d") public val vitaminD: NutrientAmount? = null,
)

@JsonClass(generateAdapter = false)
public data class ServingSelection(public val id: Long, public val quantity: Double)

@JsonClass(generateAdapter = false)
public data class FoodSelection(public val id: Long, public val serving: ServingSelection)

