package ai.january.partner.foods

import ai.january.partner.PartnerUserId
import ai.january.partner.models.CompleteScanNutritionFacts
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

public data class LookupFoodByBarcodeRequest(public val upc: String, public val endUserId: PartnerUserId? = null)
public data class SearchFoodsByNaturalLanguageRequest(public val query: String, public val endUserId: PartnerUserId? = null)

@Deprecated("analyzeDescription returns FoodScan.", ReplaceWith("FoodScan", "ai.january.partner.photos.FoodScan"))
public typealias SearchFoodsByNaturalLanguageResponse = ai.january.partner.photos.FoodScan

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

/** The catalog serving a detected or alternative food is expressed in. `quantity` is the size of one serving, not the amount eaten. */
@JsonClass(generateAdapter = false)
public data class ServingSummary(
    public val id: String?,
    public val quantity: Double? = null,
    public val unit: String?,
    /** Weight in grams of one serving, when the catalog knows it. Consumed grams = `quantity` × `weightGrams`. */
    @Json(name = "weight_grams") public val weightGrams: Double? = null,
) {
    /** The shape before [weightGrams], kept for callers (Java callers too) that pass three arguments. */
    public constructor(id: String?, quantity: Double?, unit: String?) : this(id, quantity, unit, null)

    public constructor(id: Long, quantity: Double? = null, unit: String) : this(id.toString(), quantity, unit, null)
}

@Deprecated("Use ServingSummary. The amount eaten is now DetectedFood.quantity.", ReplaceWith("ServingSummary"))
public typealias DetectedServing = ServingSummary

/**
 * A food recognized from a photo or a description.
 *
 * `serving` is the selected catalog serving and `quantity` is how many of that serving were eaten
 * (`0.4` for 40 g of a 100 g serving); together they are ready to use as a food-log entry.
 * `nutrients` are already scaled to `quantity`. `quantity` is null when no usable portion was found.
 */
@JsonClass(generateAdapter = false)
public data class DetectedFood(
    public val id: String? = null,
    public val name: String?,
    @Json(name = "brand_name") public val brandName: String? = null,
    public val nutrients: CompleteScanNutritionFacts,
    public val serving: ServingSummary,
    public val quantity: Double? = null,
) {
    public constructor(
        id: Long?, name: String?, brandName: String? = null,
        nutrients: CompleteScanNutritionFacts, serving: ServingSummary, quantity: Double? = null,
    ) : this(id?.toString(), name, brandName, nutrients, serving, quantity)
}

/** A healthier alternative to a food, with the servings its nutrition can be read against. */
@JsonClass(generateAdapter = false)
public data class AlternativeFood(
    public val id: String? = null,
    public val name: String?,
    @Json(name = "brand_name") public val brandName: String? = null,
    public val nutrients: CompleteScanNutritionFacts,
    public val servings: List<ServingSummary> = emptyList(),
)

public typealias FoodAlternative = AlternativeFood

@JsonClass(generateAdapter = false)
public data class SuggestFoodAlternativesResponse(public val alternatives: List<AlternativeFood>)
