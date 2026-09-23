package ai.january.partner.waterlogs

import ai.january.partner.ErrorCategory
import ai.january.partner.JanuaryException
import ai.january.partner.executeApiCall
import ai.january.partner.executeEmptyApiCall
import ai.january.partner.transport.apis.WaterLogsApi
import ai.january.partner.transport.models.CreateWaterLogBody
import ai.january.partner.transport.models.WaterAmount as TransportWaterAmount
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/** Water intake logs for one end user. */
public class WaterLogsResource internal constructor(private val api: WaterLogsApi) {
    public suspend fun create(request: CreateWaterLogRequest): WaterLog {
        val body = CreateWaterLogBody(
            amount = TransportWaterAmount(BigDecimal.valueOf(request.amount.value), request.amount.unit.value),
            createdAt = request.consumedAt?.let(OffsetDateTime::parse),
        )
        return executeApiCall(
            operation = { api.createWaterLog(body, request.user.endUserId.value) },
            transform = { it.toPublic() },
        )
    }

    public suspend fun list(request: ListWaterLogsRequest): ListWaterLogsResponse = executeApiCall(
        operation = {
            api.listWaterLogs(
                LocalDate.parse(request.start),
                LocalDate.parse(request.end),
                request.user.timezone ?: "UTC",
                request.unit.value,
                request.user.endUserId.value,
            )
        },
        transform = { response ->
            ListWaterLogsResponse(
                response.items.map { day ->
                    DailyWaterTotal(day.date.toString(), Volume(day.total.value.toDouble(), volumeUnit(day.total.unit)))
                },
            )
        },
    )

    public suspend fun delete(request: DeleteWaterLogRequest): DeleteWaterLogResponse =
        executeEmptyApiCall { api.deleteWaterLog(UUID.fromString(request.id), request.user.endUserId.value) }
}

private fun ai.january.partner.transport.models.WaterLog.toPublic() = WaterLog(
    id = id,
    amount = WaterAmount(amount.value.toDouble(), volumeUnit(amount.unit)),
    consumedAt = createdAt.toString(),
)

internal fun volumeUnit(value: String): VolumeUnit = VolumeUnit.fromValue(value)
    ?: throw JanuaryException(
        ErrorCategory.DECODING,
        "The January API returned the water unit \"$value\", which this SDK version does not support.",
    )
