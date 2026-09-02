package ai.january.partner.foodlogs

import ai.january.partner.executeApiCall
import ai.january.partner.executeEmptyApiCall
import ai.january.partner.transport.apis.FoodLogsApi
import ai.january.partner.transport.models.CreateFoodLogBody
import ai.january.partner.transport.models.UpdateFoodLogBody
import ai.january.partner.models.NutrientAmount
import ai.january.partner.models.NutritionFacts
import ai.january.partner.transport.models.FoodLogInputFood
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

public class FoodLogsResource internal constructor(private val api: FoodLogsApi) {
    public suspend fun create(request: CreateFoodLogRequest): FoodLog {
        val body = CreateFoodLogBody(
            foods = request.foods.map { it.toTransport() },
            eatenAt = request.timestampUtc?.let(OffsetDateTime::parse),
            name = request.name,
        )
        return executeApiCall(
            operation = { api.createFoodLog(body, request.user.endUserId.value) },
            transform = { it.toPublic() },
        )
    }

    public suspend fun list(request: ListFoodLogsRequest): ListFoodLogsResponse = executeApiCall(
        operation = {
            api.listFoodLogs(
                LocalDate.parse(request.start),
                LocalDate.parse(request.end),
                request.user.timezone ?: "UTC",
                request.user.endUserId.value,
            )
        },
        transform = { ListFoodLogsResponse(it.items.size, it.items.map { log -> log.toPublic() }) },
    )

    public suspend fun get(request: GetFoodLogRequest): FoodLog = executeApiCall(
        operation = { api.getFoodLog(UUID.fromString(request.id), request.user.endUserId.value) },
        transform = { it.toPublic() },
    )

    public suspend fun update(request: UpdateFoodLogRequest): FoodLog {
        val body = UpdateFoodLogBody(
            foods = request.foods?.map { it.toTransport() },
            eatenAt = request.timestampUtc?.let(OffsetDateTime::parse),
            name = request.name,
        )
        return executeApiCall(
            operation = { api.updateFoodLog(UUID.fromString(request.id), body, request.user.endUserId.value) },
            transform = { it.toPublic() },
        )
    }

    public suspend fun delete(request: DeleteFoodLogRequest): DeleteFoodLogResponse =
        executeEmptyApiCall { api.deleteFoodLog(UUID.fromString(request.id), request.user.endUserId.value) }
}

private fun ai.january.partner.models.FoodSelection.toTransport() = FoodLogInputFood(
    foodId = id,
    servingId = serving.id,
    quantity = BigDecimal.valueOf(serving.quantity),
)

private fun ai.january.partner.transport.models.FoodLog.toPublic() = FoodLog(
    id = id,
    foods = foods.map { food ->
        LoggedFood(
            id = food.foodId,
            name = food.name,
            brandName = food.brandName,
            imageUrl = food.imageUrl,
            glycemicIndex = food.glycemicIndex?.toDouble(),
            glycemicLoad = food.glycemicLoad?.toDouble(),
            nutrients = food.nutrients.toPublic(),
            consumedServing = ConsumedServing(food.serving.id, food.quantity?.toDouble()),
            servingDetails = ServingDetails(
                food.serving.id,
                food.serving.quantity?.toDouble(),
                food.serving.unit,
                food.serving.weightGrams?.toDouble(),
            ),
        )
    },
    timestampUtc = eatenAt.toString(),
    name = name,
)

private fun ai.january.partner.transport.models.NutritionFacts.toPublic(): NutritionFacts {
    fun ai.january.partner.transport.models.NutrientAmount?.amount(): NutrientAmount? =
        this?.let { NutrientAmount(it.value.toDouble(), it.unit) }
    return NutritionFacts(
        calories.amount(), protein.amount(), carbohydrates.amount(), netCarbohydrates.amount(),
        totalFat.amount(), transFat.amount(), saturatedFat.amount(), fiber.amount(),
        totalSugars.amount(), addedSugars.amount(), cholesterol.amount(), calcium.amount(),
        iron.amount(), potassium.amount(), sodium.amount(), vitaminD.amount(),
    )
}
