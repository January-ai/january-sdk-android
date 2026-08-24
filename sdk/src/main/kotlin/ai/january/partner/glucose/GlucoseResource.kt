package ai.january.partner.glucose

import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.models.FoodSelection
import ai.january.partner.transport.apis.GlucoseApi
import ai.january.partner.transport.models.PredictGlucoseBody
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.OffsetDateTime

public class GlucoseResource internal constructor(private val api: GlucoseApi) {
    public suspend fun predict(request: PredictGlucoseRequest): GlucosePrediction {
        val body: PredictGlucoseBody = bridgeModel(
            PredictBody(
                request.userProfile, request.foods, request.startTime,
                request.cgmData, request.consumedFoods,
            ),
        )
        return executeApiCall(
            operation = { api.predictGlucose(body, request.endUserId?.value, request.timezone) },
            transform = { prediction ->
                GlucosePrediction(
                    prediction = prediction.prediction.map {
                        GlucosePredictionPoint(it.minutes.toDouble(), it.value.toDouble())
                    },
                    impact = GlucoseImpact(prediction.impactScore),
                    chart = GlucoseChart(prediction.chart.min.toDouble(), prediction.chart.max.toDouble()),
                )
            },
        )
    }
}

@JsonClass(generateAdapter = false)
private data class PredictBody(
    @Json(name = "user_profile") val userProfile: GlucosePredictionProfile,
    val foods: List<FoodSelection>,
    @Json(name = "start_time") val startTime: OffsetDateTime,
    @Json(name = "cgm_data") val cgmData: List<CgmReading>?,
    @Json(name = "consumed_foods") val consumedFoods: List<ConsumedHistoricalFood>?,
)
