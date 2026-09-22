package ai.january.partner.waterlogs

import ai.january.partner.PartnerUserContext
import com.squareup.moshi.Json

/** The unit a water amount is logged or totalled in. */
public enum class VolumeUnit(public val value: String) {
    @Json(name = "fl_oz") FL_OZ("fl_oz"),
    @Json(name = "ml") ML("ml"),
    ;

    public companion object {
        /** The unit for a wire value, or null for one this SDK version does not know. */
        public fun fromValue(value: String): VolumeUnit? = entries.firstOrNull { it.value == value }
    }
}

/**
 * An amount of water: 1–811.5 fl oz or 30–24,000 ml. An end user's total is
 * capped at 24 L (about 811 fl oz) per day.
 */
public data class WaterAmount(public val value: Double, public val unit: VolumeUnit)

/** A volume rounded to one decimal place, in the unit the request asked for. */
public data class Volume(public val value: Double, public val unit: VolumeUnit)

/**
 * Logs one amount of water for [user]. [consumedAt] is an ISO-8601 offset
 * date-time; omitted, the API uses the time it receives the request. Its day
 * is the one the daily cap counts it against.
 */
public data class CreateWaterLogRequest(
    public val amount: WaterAmount,
    public val consumedAt: String? = null,
    public val user: PartnerUserContext,
)

/**
 * Lists one total per local calendar day between [start] and [end] (inclusive
 * `YYYY-MM-DD` dates in the user's timezone, at most five years back), in
 * [unit]. Days with nothing logged are absent; at most the most recent 100
 * days with water are returned.
 */
public data class ListWaterLogsRequest(
    public val start: String,
    public val end: String,
    public val unit: VolumeUnit,
    public val user: PartnerUserContext,
)

/** Deletes one water log. Deleting an unknown or already-deleted log also succeeds. */
public data class DeleteWaterLogRequest(public val id: String, public val user: PartnerUserContext)

/** A logged water intake: its [id] to delete it with, the [amount] as logged, and [consumedAt] in UTC. */
public data class WaterLog(
    public val id: String,
    public val amount: WaterAmount,
    public val consumedAt: String,
)

/** Everything logged on one local calendar [date], in the unit the request asked for. */
public data class DailyWaterTotal(public val date: String, public val total: Volume)

/** Daily totals, oldest first. An empty list is a valid result. */
public data class ListWaterLogsResponse(public val items: List<DailyWaterTotal>)

public typealias DeleteWaterLogResponse = Unit
