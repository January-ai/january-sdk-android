package ai.january.partner.glucose

import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.transport.apis.GlucoseApi
import ai.january.partner.transport.models.PredictGlucoseBody
import ai.january.partner.transport.models.CgmReading as TransportCgmReading
import ai.january.partner.transport.models.ConsumedHistoricalFood as TransportConsumedHistoricalFood
import ai.january.partner.transport.models.FoodLogInputFood
import java.math.BigDecimal

public class GlucoseResource internal constructor(private val api: GlucoseApi) {
    public suspend fun predict(request: PredictGlucoseRequest): GlucosePrediction {
        val body = PredictGlucoseBody(
            userProfile = bridgeModel(request.userProfile),
            timezone = request.timezone ?: "UTC",
            foods = request.foods.map { food -> FoodLogInputFood(
                food.id, food.serving.id, BigDecimal.valueOf(food.serving.quantity),
            ) },
            startTime = request.startTime,
            cgmData = request.cgmData?.map { reading -> TransportCgmReading(
                java.time.OffsetDateTime.parse(reading.timestamp), BigDecimal.valueOf(reading.value),
            ) },
            consumedFoods = request.consumedFoods?.map { food -> TransportConsumedHistoricalFood(
                java.time.OffsetDateTime.parse(food.timestamp),
                food.id,
                food.serving.id,
                BigDecimal.valueOf(food.serving.quantity),
            ) },
        )
        return executeApiCall(
            operation = { api.predictGlucose(body) },
            transform = { prediction ->
                GlucosePrediction(
                    prediction = prediction.points.map {
                        GlucosePredictionPoint(it.minutes.toDouble(), it.value.toDouble())
                    },
                    impact = prediction.impactScore?.let(::GlucoseImpact),
                    chart = GlucoseChart(prediction.chart.min?.toDouble(), prediction.chart.max?.toDouble()),
                )
            },
        )
    }
}
