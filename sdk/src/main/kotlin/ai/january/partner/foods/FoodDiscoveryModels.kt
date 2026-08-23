package ai.january.partner.foods

import ai.january.partner.PartnerUserId
import ai.january.partner.models.CompleteScanNutritionFacts
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

public data class LookupFoodByBarcodeRequest(public val upc: String, public val endUserId: PartnerUserId? = null)
public data class SearchFoodsByNaturalLanguageRequest(public val query: String, public val endUserId: PartnerUserId? = null)

@JsonClass(generateAdapter = false)
public data class NaturalLanguageServing(
    public val id: Long,
    public val quantity: Double? = null,
    public val unit: String,
    @Json(name = "selected_quantity") public val selectedQuantity: Double? = null,
)

@JsonClass(generateAdapter = false)
public data class NaturalLanguageFood(
    public val id: Long? = null,
    public val name: String,
    @Json(name = "brand_name") public val brandName: String? = null,
    public val nutrients: CompleteScanNutritionFacts,
    public val servings: List<NaturalLanguageServing>? = null,
)

@JsonClass(generateAdapter = false)
public data class NaturalLanguageFoodDetection(public val food: NaturalLanguageFood)

@JsonClass(generateAdapter = false)
public data class SearchFoodsByNaturalLanguageResponse(
    @Json(name = "total_nutrients") public val totalNutrients: CompleteScanNutritionFacts? = null,
    public val detections: List<NaturalLanguageFoodDetection>,
)

public enum class DietRestriction(public val value: String) {
    @Json(name = "None") NONE("None"), @Json(name = "Gluten") GLUTEN("Gluten"),
    @Json(name = "Lactose") LACTOSE("Lactose"), @Json(name = "Yeast") YEAST("Yeast"),
    @Json(name = "Tree nuts") TREE_NUTS("Tree nuts"), @Json(name = "Peanuts") PEANUTS("Peanuts"),
    @Json(name = "Dairy") DAIRY("Dairy"), @Json(name = "Eggs") EGGS("Eggs"),
    @Json(name = "Sulfites") SULFITES("Sulfites"), @Json(name = "Soy") SOY("Soy"),
    @Json(name = "Wheat") WHEAT("Wheat"), @Json(name = "Shellfish") SHELLFISH("Shellfish"),
    @Json(name = "Fish") FISH("Fish"), @Json(name = "Mushrooms") MUSHROOMS("Mushrooms"),
    @Json(name = "Sesame") SESAME("Sesame"),
    @Json(name = "Monosodium glutamate (MSG)") MONOSODIUM_GLUTAMATE("Monosodium glutamate (MSG)"),
    @Json(name = "Caffeine") CAFFEINE("Caffeine"), @Json(name = "FODMAPs") FODMAPS("FODMAPs"),
}

public enum class DietPreference(public val value: String) {
    @Json(name = "None") NONE("None"), @Json(name = "Vegetarian") VEGETARIAN("Vegetarian"),
    @Json(name = "Vegan") VEGAN("Vegan"), @Json(name = "Keto") KETO("Keto"),
    @Json(name = "Paleo") PALEO("Paleo"), @Json(name = "Pescatarian") PESCATARIAN("Pescatarian"),
    @Json(name = "Low carbohydrate") LOW_CARBOHYDRATE("Low carbohydrate"),
    @Json(name = "High protein") HIGH_PROTEIN("High protein"),
    @Json(name = "Kosher") KOSHER("Kosher"), @Json(name = "Halal") HALAL("Halal"),
}

public data class SuggestFoodAlternativesRequest(
    public val foodId: Long,
    public val dietRestrictions: List<DietRestriction> = listOf(DietRestriction.NONE),
    public val dietPreferences: List<DietPreference> = listOf(DietPreference.NONE),
    public val endUserId: PartnerUserId? = null,
)

@JsonClass(generateAdapter = false)
public data class DetectedServing(public val id: Long, public val quantity: Double? = null, public val unit: String)

@JsonClass(generateAdapter = false)
public data class DetectedFood(
    public val id: Long? = null,
    public val name: String,
    @Json(name = "brand_name") public val brandName: String? = null,
    public val nutrients: CompleteScanNutritionFacts,
    public val servings: List<DetectedServing>? = null,
)

@JsonClass(generateAdapter = false)
public data class FoodAlternative(public val food: DetectedFood)

@JsonClass(generateAdapter = false)
public data class SuggestFoodAlternativesResponse(public val alternatives: List<FoodAlternative>)

