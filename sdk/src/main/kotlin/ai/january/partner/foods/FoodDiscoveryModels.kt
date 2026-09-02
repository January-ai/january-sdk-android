package ai.january.partner.foods

import ai.january.partner.PartnerUserId
import ai.january.partner.models.CompleteScanNutritionFacts
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

public data class LookupFoodByBarcodeRequest(public val upc: String, public val endUserId: PartnerUserId? = null)
public data class SearchFoodsByNaturalLanguageRequest(public val query: String, public val endUserId: PartnerUserId? = null)

@JsonClass(generateAdapter = false)
public data class NaturalLanguageServing(
    public val id: String?,
    public val quantity: Double? = null,
    public val unit: String?,
    @Json(name = "selected_quantity") public val selectedQuantity: Double? = null,
)

@JsonClass(generateAdapter = false)
public data class NaturalLanguageFood(
    public val id: String? = null,
    public val name: String?,
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
    public val foodId: String,
    public val dietRestrictions: List<DietRestriction> = emptyList(),
    public val dietPreferences: List<DietPreference> = emptyList(),
    public val endUserId: PartnerUserId? = null,
) {
    public constructor(
        foodId: Long,
        dietRestrictions: List<DietRestriction> = emptyList(),
        dietPreferences: List<DietPreference> = emptyList(),
        endUserId: PartnerUserId? = null,
    ) : this(foodId.toString(), dietRestrictions, dietPreferences, endUserId)
}

@JsonClass(generateAdapter = false)
public data class DetectedServing(
    public val id: String?, public val quantity: Double? = null, public val unit: String?,
    public val selectedQuantity: Double? = null,
) {
    public constructor(id: Long, quantity: Double? = null, unit: String) :
        this(id.toString(), quantity, unit, null)
}

@JsonClass(generateAdapter = false)
public data class DetectedFood(
    public val id: String? = null,
    public val name: String?,
    @Json(name = "brand_name") public val brandName: String? = null,
    public val nutrients: CompleteScanNutritionFacts,
    public val servings: List<DetectedServing>? = null,
) {
    public constructor(
        id: Long?, name: String?, brandName: String? = null,
        nutrients: CompleteScanNutritionFacts, servings: List<DetectedServing>? = null,
    ) : this(id?.toString(), name, brandName, nutrients, servings)
}

public typealias FoodAlternative = DetectedFood

@JsonClass(generateAdapter = false)
public data class SuggestFoodAlternativesResponse(public val alternatives: List<FoodAlternative>)
