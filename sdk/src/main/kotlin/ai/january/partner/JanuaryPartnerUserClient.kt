package ai.january.partner

import ai.january.partner.foodlogs.CreateFoodLogRequest
import ai.january.partner.foodlogs.DeleteFoodLogRequest
import ai.january.partner.foodlogs.DeleteFoodLogResponse
import ai.january.partner.foodlogs.FoodLog
import ai.january.partner.foodlogs.FoodLogsResource
import ai.january.partner.foodlogs.ListFoodLogsRequest
import ai.january.partner.foodlogs.ListFoodLogsResponse
import ai.january.partner.foodlogs.UpdateFoodLogRequest
import ai.january.partner.glucose.GlucosePrediction
import ai.january.partner.glucose.GlucoseResource
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.models.FoodSelection

/** A lightweight January client scoped to one partner-owned end-user identity. */
public class JanuaryPartnerUserClient internal constructor(
    client: JanuaryPartnerClient,
    public val context: PartnerUserContext,
) {
    public val foodLogs: UserFoodLogsResource = UserFoodLogsResource(client.foodLogs, context)
    public val glucose: UserGlucoseResource = UserGlucoseResource(client.glucose, context)
}

/** Food Log operations that automatically reuse a [PartnerUserContext]. */
public class UserFoodLogsResource internal constructor(
    private val resource: FoodLogsResource,
    private val context: PartnerUserContext,
) {
    public suspend fun create(
        foods: List<FoodSelection>,
        timestampUtc: String? = null,
        name: String? = null,
    ): FoodLog = resource.create(CreateFoodLogRequest(foods, timestampUtc, name, context))

    public suspend fun list(start: String, end: String): ListFoodLogsResponse =
        resource.list(ListFoodLogsRequest(start, end, context))

    public suspend fun update(
        id: String,
        foods: List<FoodSelection>? = null,
        timestampUtc: String? = null,
        name: String? = null,
    ): FoodLog = resource.update(UpdateFoodLogRequest(id, foods, timestampUtc, name, context))

    public suspend fun delete(id: String): DeleteFoodLogResponse =
        resource.delete(DeleteFoodLogRequest(id, context))
}

/** Glucose operations that replace request identity with the scoped context. */
public class UserGlucoseResource internal constructor(
    private val resource: GlucoseResource,
    private val context: PartnerUserContext,
) {
    public suspend fun predict(request: PredictGlucoseRequest): GlucosePrediction =
        resource.predict(
            request.copy(
                endUserId = context.endUserId,
                timezone = context.timezone,
            ),
        )
}
