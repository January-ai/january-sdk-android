package ai.january.partner.weightlogs

import ai.january.partner.PartnerUserContext
import ai.january.partner.glucose.Weight

/**
 * Logs one weight measurement for [user]: 10–1000 lb or 4.5–453.6 kg, kept in
 * the unit it is sent in. [measuredAt] is an ISO-8601 offset date-time;
 * omitted, the API uses the time it receives the request. Every measurement is
 * kept, and a day's listing shows the latest one. Not idempotent: a retried
 * create records the weight again.
 */
public data class CreateWeightLogRequest(
    public val weight: Weight,
    public val measuredAt: String? = null,
    public val user: PartnerUserContext,
)

/**
 * Lists the latest weight per local calendar day between [start] and [end]
 * (inclusive `YYYY-MM-DD` dates in the user's timezone, at most five years
 * back). Days without a weight are absent; at most the most recent 100 days
 * with one are returned.
 */
public data class ListWeightLogsRequest(
    public val start: String,
    public val end: String,
    public val user: PartnerUserContext,
)

/** A logged weight as stored, in the unit it was sent in, and [measuredAt] in UTC. */
public data class WeightLog(public val weight: Weight, public val measuredAt: String)

/** The weight with the latest measurement time on one local calendar [date]. */
public data class DailyWeight(public val date: String, public val weight: Weight)

/** Daily weights, oldest first. An empty list is a valid result. */
public data class ListWeightLogsResponse(public val items: List<DailyWeight>)
