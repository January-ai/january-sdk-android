package ai.january.partner.foodlogs

import ai.january.partner.PartnerUserContext
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.NutritionFacts
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Deprecated("Use PartnerUserContext.", ReplaceWith("PartnerUserContext(endUserId, timezone)"))
public typealias FoodLogUserContext = PartnerUserContext
public data class CreateFoodLogRequest(
    public val foods: List<FoodSelection>, public val timestampUtc: String? = null,
    public val name: String? = null, public val user: PartnerUserContext,
)
public data class ListFoodLogsRequest(public val start: String, public val end: String, public val user: PartnerUserContext)
public data class UpdateFoodLogRequest(
    public val id: String, public val foods: List<FoodSelection>? = null,
    public val timestampUtc: String? = null, public val name: String? = null,
    public val user: PartnerUserContext,
)
public data class GetFoodLogRequest(public val id: String, public val user: PartnerUserContext)
public data class DeleteFoodLogRequest(public val id: String, public val user: PartnerUserContext)

@JsonClass(generateAdapter = false)
public data class ConsumedServing(public val id: String?, public val quantity: Double?)

@JsonClass(generateAdapter = false)
public data class ServingDetails(
    public val id: String?, public val quantity: Double?, public val unit: String?,
    @Json(name = "weight_grams") public val weightGrams: Double? = null,
)

@JsonClass(generateAdapter = false)
public data class LoggedFood(
    public val id: String?, public val name: String?,
    @Json(name = "brand_name") public val brandName: String? = null,
    @Json(name = "image_url") public val imageUrl: String? = null,
    @Json(name = "glycemic_index") public val glycemicIndex: Double? = null,
    @Json(name = "glycemic_load") public val glycemicLoad: Double? = null,
    public val nutrients: NutritionFacts,
    @Json(name = "consumed_serving") public val consumedServing: ConsumedServing,
    @Json(name = "serving_details") public val servingDetails: ServingDetails,
)

@JsonClass(generateAdapter = false)
public data class FoodLog(
    public val id: String?, public val foods: List<LoggedFood>,
    @Json(name = "timestamp_utc") public val timestampUtc: String,
    public val name: String? = null,
)

@JsonClass(generateAdapter = false)
public data class ListFoodLogsResponse(
    @Json(name = "total_count") public val totalCount: Int,
    public val items: List<FoodLog>,
)

public typealias DeleteFoodLogResponse = Unit
