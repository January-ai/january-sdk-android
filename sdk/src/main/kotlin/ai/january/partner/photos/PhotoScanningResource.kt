package ai.january.partner.photos

import ai.january.partner.bridgeModel
import ai.january.partner.executeApiCall
import ai.january.partner.foods.DetectedFood
import ai.january.partner.foods.ServingSummary
import ai.january.partner.transport.apis.PhotoScanningApi
import ai.january.partner.transport.models.AnalysisReasoning
import ai.january.partner.transport.models.CorrectPhotoScanBody
import ai.january.partner.transport.models.CorrectionAnalysis
import ai.january.partner.transport.models.CorrectionDetection
import ai.january.partner.transport.models.CorrectionFood
import ai.january.partner.transport.models.CorrectionServing
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
                    detection.food.serving.quantity.toDouble(),
                    detection.food.serving.unit,
                    detection.food.serving.weightGrams?.toDouble(),
                ),
                quantity = detection.food.quantity.toDouble(),
            ),
        )
    },
)

/**
 * A correction sends the prior scan back field for field. Every detection the
 * API returned carries a food id, a serving id and size, and a quantity; one
 * that lacks any of them (only possible for a hand-built value) cannot be
 * expressed in the correction request and is left out, like the API leaves out
 * detections it cannot size. Nothing is filled in on its behalf. Describe such
 * a food in the instruction instead.
 */
private fun FoodScan.toTransport() = CorrectionAnalysis(
    mealName = mealName,
    totalNutrients = bridgeModel(totalNutrients),
    detections = detections.mapNotNull { detection ->
        val foodId = detection.food.id ?: return@mapNotNull null
        val servingId = detection.food.serving.id ?: return@mapNotNull null
        val servingQuantity = detection.food.serving.quantity ?: return@mapNotNull null
        val quantity = detection.food.quantity ?: return@mapNotNull null
        CorrectionDetection(
            confidence = detection.confidenceScore,
            food = CorrectionFood(
                id = foodId,
                name = detection.food.name,
                brandName = detection.food.brandName,
                nutrients = bridgeModel(detection.food.nutrients),
                serving = CorrectionServing(
                    id = servingId,
                    quantity = java.math.BigDecimal.valueOf(servingQuantity),
                    unit = detection.food.serving.unit,
                    weightGrams = detection.food.serving.weightGrams?.let(java.math.BigDecimal::valueOf),
                ),
                quantity = java.math.BigDecimal.valueOf(quantity),
            ),
        )
    },
)
