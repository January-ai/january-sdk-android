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
    GLUTEN("gluten"), LACTOSE("lactose"), YEAST("yeast"), TREE_NUTS("tree_nuts"),
    PEANUTS("peanuts"), DAIRY("dairy"), EGGS("eggs"), SULFITES("sulfites"),
    SOY("soy"), WHEAT("wheat"), SHELLFISH("shellfish"), FISH("fish"),
    MUSHROOMS("mushrooms"), SESAME("sesame"), MONOSODIUM_GLUTAMATE("msg"),
    CAFFEINE("caffeine"), FODMAPS("fodmaps"),
}

public enum class DietPreference(public val value: String) {
    VEGETARIAN("vegetarian"), VEGAN("vegan"), KETO("keto"), PALEO("paleo"),
    PESCATARIAN("pescatarian"), LOW_CARBOHYDRATE("low_carbohydrate"),
    HIGH_PROTEIN("high_protein"), KOSHER("kosher"), HALAL("halal"),
}

public data class SuggestFoodAlternativesRequest(
    public val foodId: Long,
    public val dietRestrictions: List<DietRestriction> = emptyList(),
    public val dietPreferences: List<DietPreference> = emptyList(),
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
