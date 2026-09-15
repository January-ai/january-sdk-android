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

/** Bucket size for a food-log summary. */
public enum class FoodLogSummaryGrouping { DAY, WEEK }

/** Which weekday a week bucket begins on. Ignored when grouping by day. */
public enum class WeekStart { MONDAY, SUNDAY }

/**
 * Summarizes the logs between [start] and [end] (inclusive calendar dates in the user's timezone,
 * at most 366 days) into day or week buckets with summed nutrients.
 */
public data class GetFoodLogSummaryRequest(
    public val start: String,
    public val end: String,
    public val groupBy: FoodLogSummaryGrouping = FoodLogSummaryGrouping.DAY,
    public val weekStart: WeekStart = WeekStart.MONDAY,
    public val user: PartnerUserContext,
)
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

/** One day or week of a food-log summary. Buckets tile the requested range, so an empty period is present with zero counts. */
@JsonClass(generateAdapter = false)
public data class FoodLogSummaryBucket(
    @Json(name = "start_date") public val startDate: String,
    @Json(name = "end_date") public val endDate: String,
    @Json(name = "logs_count") public val logsCount: Int,
    @Json(name = "days_with_logs") public val daysWithLogs: Int,
    /** Nutrients summed over the bucket. Sparse: a key is absent when nothing could be totalled. */
    public val nutrients: NutritionFacts,
)

@JsonClass(generateAdapter = false)
public data class FoodLogSummaryTotals(
    @Json(name = "logs_count") public val logsCount: Int,
    @Json(name = "days_with_logs") public val daysWithLogs: Int,
    public val nutrients: NutritionFacts,
)

/** Totals divided by the number of days that have at least one log. */
@JsonClass(generateAdapter = false)
public data class FoodLogSummaryAverage(public val nutrients: NutritionFacts)

@JsonClass(generateAdapter = false)
public data class FoodLogSummary(
    @Json(name = "group_by") public val groupBy: FoodLogSummaryGrouping,
    /** Null when grouped by day. */
    @Json(name = "week_start") public val weekStart: WeekStart?,
    public val timezone: String,
    @Json(name = "start_date") public val startDate: String,
    @Json(name = "end_date") public val endDate: String,
    public val buckets: List<FoodLogSummaryBucket>,
    public val totals: FoodLogSummaryTotals,
    @Json(name = "average_per_logged_day") public val averagePerLoggedDay: FoodLogSummaryAverage,
)
