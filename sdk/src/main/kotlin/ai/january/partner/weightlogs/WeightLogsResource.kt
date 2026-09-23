package ai.january.partner.weightlogs

import ai.january.partner.ErrorCategory
import ai.january.partner.JanuaryException
import ai.january.partner.executeApiCall
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.transport.apis.WeightLogsApi
import ai.january.partner.transport.models.CreateWeightLogBody
import ai.january.partner.transport.models.Weight as TransportWeight
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

/** Body weight logs for one end user. */
public class WeightLogsResource internal constructor(private val api: WeightLogsApi) {
    public suspend fun create(request: CreateWeightLogRequest): WeightLog {
        val body = CreateWeightLogBody(
            weight = TransportWeight(BigDecimal.valueOf(request.weight.value), request.weight.unit.value),
            createdAt = request.measuredAt?.let(OffsetDateTime::parse),
        )
        return executeApiCall(
            operation = { api.createWeightLog(body, request.user.endUserId.value) },
            transform = { WeightLog(it.weight.toPublic(), it.createdAt.toString()) },
        )
    }

    public suspend fun list(request: ListWeightLogsRequest): ListWeightLogsResponse = executeApiCall(
        operation = {
            api.listWeightLogs(
                LocalDate.parse(request.start),
                LocalDate.parse(request.end),
                request.user.timezone ?: "UTC",
                request.user.endUserId.value,
            )
        },
        transform = { response ->
            ListWeightLogsResponse(response.items.map { day -> DailyWeight(day.date.toString(), day.weight.toPublic()) })
        },
    )
}

private fun TransportWeight.toPublic(): Weight {
    val publicUnit = WeightUnit.entries.firstOrNull { it.value == unit } ?: throw JanuaryException(
        ErrorCategory.DECODING,
        "The January API returned the weight unit \"$unit\", which this SDK version does not support.",
    )
    return Weight(value.toDouble(), publicUnit)
}
