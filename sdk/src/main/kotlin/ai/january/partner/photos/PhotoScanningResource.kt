package ai.january.partner.photos

import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.foods.DetectedFood
import ai.january.partner.foods.ServingSummary
import ai.january.partner.transport.apis.PhotoScanningApi
import ai.january.partner.transport.models.AnalysisReasoning
import ai.january.partner.transport.models.CorrectPhotoScanBody
import ai.january.partner.transport.models.ScanFoodPhotoBody
import ai.january.partner.transport.models.SearchFoodsByNaturalLanguageBody

/** Operations that analyze food from photos or natural-language descriptions. */
public class FoodAnalysisResource internal constructor(private val api: PhotoScanningApi) {
    public suspend fun analyzePhoto(request: ScanFoodPhotoRequest): FoodScan = executeApiCall(
        operation = {
            api.scanFoodPhoto(
                ScanFoodPhotoBody(
                    image = request.image,
                    reasoning = request.reasoningEffort?.let { effort ->
                        AnalysisReasoning(
                            when (effort) {
                                AnalysisEffort.NONE -> AnalysisReasoning.Effort.NONE
                                AnalysisEffort.XHIGH -> AnalysisReasoning.Effort.XHIGH
                            },
                        )
                    },
                ),
            )
        },
        transform = { it.toPublic() },
    )

    public suspend fun analyzeDescription(
        request: ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest,
    ): FoodScan = executeApiCall(
        operation = {
            api.searchFoodsByNaturalLanguage(
                SearchFoodsByNaturalLanguageBody(request.query),
            )
        },
        transform = { it.toPublic() },
    )

    public suspend fun correct(request: CorrectPhotoScanRequest): FoodScan {
        val body = CorrectPhotoScanBody(
            analysis = request.analysis.toTransport(),
            instruction = request.instruction,
        )
        return executeApiCall(
            operation = { api.correctPhotoScan(body) },
            transform = { it.toPublic() },
        )
    }
}

private fun ai.january.partner.transport.models.FoodScan.toPublic() = FoodScan(
    mealName = mealName,
    totalNutrients = bridgeModel(totalNutrients),
    detections = detections.map { detection ->
        FoodDetection(
            confidenceScore = detection.confidence,
            food = DetectedFood(
                id = detection.food.id,
                name = detection.food.name,
                brandName = detection.food.brandName,
                nutrients = bridgeModel(detection.food.nutrients),
                serving = ServingSummary(
                    detection.food.serving.id,
                    detection.food.serving.quantity?.toDouble(),
                    detection.food.serving.unit,
                ),
                quantity = detection.food.quantity?.toDouble(),
            ),
        )
    },
)

private fun FoodScan.toTransport() = ai.january.partner.transport.models.FoodScan(
    mealName = mealName,
    totalNutrients = bridgeModel(totalNutrients),
    detections = detections.map { detection ->
        ai.january.partner.transport.models.FoodDetection(
            confidence = detection.confidenceScore,
            food = ai.january.partner.transport.models.DetectedFood(
                id = detection.food.id,
                name = detection.food.name,
                brandName = detection.food.brandName,
                nutrients = bridgeModel(detection.food.nutrients),
                serving = ai.january.partner.transport.models.ServingSummary(
                    id = detection.food.serving.id,
                    quantity = detection.food.serving.quantity?.let(java.math.BigDecimal::valueOf),
                    unit = detection.food.serving.unit,
                ),
                quantity = detection.food.quantity?.let(java.math.BigDecimal::valueOf),
            ),
        )
    },
)
