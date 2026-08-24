package ai.january.partner.foodlogs

import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.models.FoodSelection
import ai.january.partner.transport.apis.FoodLogsApi
import ai.january.partner.transport.models.CreateFoodLogBody
import ai.january.partner.transport.models.UpdateFoodLogBody
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

public class FoodLogsResource internal constructor(private val api: FoodLogsApi) {
    public suspend fun create(request: CreateFoodLogRequest): FoodLog {
        val body: CreateFoodLogBody = bridgeModel(
            CreateBody(request.foods, request.timestampUtc?.let(OffsetDateTime::parse), request.name),
        )
        return executeApiCall(
            operation = { api.createFoodLog(request.user.endUserId.value, body, request.user.timezone) },
            transform = { bridgeModel(it) },
        )
    }

    public suspend fun list(request: ListFoodLogsRequest): ListFoodLogsResponse = executeApiCall(
        operation = {
            api.listFoodLogs(
                request.user.endUserId.value,
                LocalDate.parse(request.start),
                LocalDate.parse(request.end),
                request.user.timezone,
            )
        },
        transform = { bridgeModel(it) },
    )

    public suspend fun update(request: UpdateFoodLogRequest): FoodLog {
        val body: UpdateFoodLogBody = bridgeModel(UpdateBody(request.foods, request.timestampUtc, request.name))
        return executeApiCall(
            operation = { api.updateFoodLog(request.user.endUserId.value, UUID.fromString(request.id), body, request.user.timezone) },
            transform = { bridgeModel(it) },
        )
    }

    public suspend fun delete(request: DeleteFoodLogRequest): DeleteFoodLogResponse = executeApiCall(
        operation = { api.deleteFoodLog(request.user.endUserId.value, UUID.fromString(request.id), request.user.timezone) },
        transform = { bridgeModel(it) },
    )
}

@JsonClass(generateAdapter = false)
private data class CreateBody(
    val foods: List<FoodSelection>,
    @Json(name = "timestamp_utc") val timestampUtc: OffsetDateTime?,
    val name: String?,
)

@JsonClass(generateAdapter = false)
private data class UpdateBody(
    val foods: List<FoodSelection>?,
    @Json(name = "timestamp_utc") val timestampUtc: String?,
    val name: String?,
)
