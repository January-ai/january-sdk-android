package ai.january.partner.foods

import ai.january.partner.FoodId
import ai.january.partner.PartnerUserId
import ai.january.partner.ServingId
import ai.january.partner.models.NutritionFacts

public enum class FoodCategory {
    GENERAL,
    BRANDED,
    RECIPE,
}

public enum class AutocompleteFoodCategory {
    GENERAL,
    BRANDED,
}

public data class AutocompleteFoodsRequest(
    public val query: String,
    public val category: AutocompleteFoodCategory? = null,
    public val limit: Int = 8,
    public val endUserId: PartnerUserId? = null,
)

public data class FoodSuggestion(
    public val id: FoodId,
    public val name: String,
    public val brandName: String? = null,
    public val imageUrl: String? = null,
    public val nutrients: NutritionFacts? = null,
)

public data class AutocompleteFoodsResponse(public val items: List<FoodSuggestion>)

public data class GetFoodRequest(
    public val foodId: FoodId,
    public val endUserId: PartnerUserId? = null,
)

public data class SearchFoodsRequest(
    public val query: String,
    public val category: FoodCategory? = null,
    public val limit: Int = 10,
    public val endUserId: PartnerUserId? = null,
)

public data class FoodSearchResults(
    public val totalCount: Int,
    public val items: List<FoodSearchItem>,
)

public data class FoodSearchItem(
    public val id: FoodId,
    public val name: String,
    public val brandName: String?,
    public val calories: Double?,
    public val protein: Double?,
    public val carbohydrates: Double?,
    public val netCarbohydrates: Double?,
    public val totalFat: Double?,
    public val saturatedFat: Double?,
    public val fiber: Double?,
    public val totalSugars: Double?,
    public val addedSugars: Double?,
    public val sodium: Double?,
    public val potassium: Double?,
    public val cholesterol: Double?,
    public val glycemicIndex: Double?,
    public val glycemicLoad: Double?,
    public val photoUrl: String?,
    public val servings: List<ServingOption>,
    public val nutrients: NutritionFacts? = null,
)

public data class ServingOption(
    public val id: ServingId,
    public val quantity: Double,
    public val unit: String,
    public val scalingFactor: Double,
    public val weightGrams: Double?,
    public val isPrimary: Boolean,
)
