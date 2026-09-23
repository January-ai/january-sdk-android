package ai.january.partner

import ai.january.partner.foodlogs.FoodLogSummaryGrouping
import ai.january.partner.foodlogs.GetFoodLogSummaryRequest
import ai.january.partner.foodlogs.UpdateFoodLogRequest
import ai.january.partner.foodlogs.WeekStart
import ai.january.partner.foods.DetectedFood
import ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest
import ai.january.partner.foods.ServingSummary
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.Height
import ai.january.partner.glucose.HeightUnit
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.glucose.Sex
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.models.CompleteScanNutritionFacts
import ai.january.partner.foods.SuggestFoodAlternativesRequest
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import ai.january.partner.photos.AnalysisEffort
import ai.january.partner.photos.CorrectPhotoScanRequest
import ai.january.partner.photos.FoodDetection
import ai.january.partner.photos.FoodScan
import ai.january.partner.photos.ScanFoodPhotoRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Decodes responses captured from the live Partner API after the September 2026 contract change. */
class ContractShapeTest {
    private lateinit var server: MockWebServer
    private lateinit var client: JanuaryPartnerClient

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = JanuaryPartnerClient.testing("fixture-api-key", server.url("/").toString(), OkHttpClient.Builder())
    }

    @After
    fun tearDown() = server.shutdown()

    private fun enqueue(body: String) {
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body))
    }

    @Test
    fun detectionCarriesSelectedServingAndQuantity(): Unit = runBlocking {
        enqueue(TEXT_ANALYSIS)
        val scan = client.foodAnalysis.analyzeDescription(SearchFoodsByNaturalLanguageRequest("three eggs"))

        val food = scan.detections.single().food
        assertEquals("eggs", food.name)
        assertEquals(2.0, food.quantity!!, 0.0)
        assertEquals("34073350", food.serving.id)
        assertEquals(1.0, food.serving.quantity!!, 0.0)
        assertEquals("large", food.serving.unit)
        assertEquals(143.0, food.nutrients.calories!!.value, 0.0)
        assertEquals(206.8, scan.totalNutrients.calories!!.value, 0.0)
        // The detection is ready to log as is.
        assertEquals(FoodSelection("70382174", ServingSelection("34073350", 2.0)), FoodSelection(food.id!!, ServingSelection(food.serving.id!!, food.quantity!!)))
    }

    @Test
    fun correctionRoundTripsServingAndQuantity(): Unit = runBlocking {
        enqueue(TEXT_ANALYSIS)
        enqueue(TEXT_ANALYSIS)
        val scan = client.foodAnalysis.analyzeDescription(SearchFoodsByNaturalLanguageRequest("three eggs"))
        server.takeRequest()

        client.foodAnalysis.correct(CorrectPhotoScanRequest(scan, "make it two eggs"))

        val sent = server.takeRequest().body.readUtf8()
        assertTrue(sent, sent.contains(""""quantity":2.0"""))
        assertTrue(sent, sent.contains(""""serving":{"id":"34073350","quantity":1.0,"unit":"large"}"""))
        assertTrue(sent, !sent.contains("servings"))
        // The prior scan goes back field for field inside the correction wrapper.
        assertTrue(sent, sent.startsWith(""""analysis":{"detections":[{"food":{"name":"eggs","id":"70382174","quantity":2.0,""".let { "{$it" }))
        assertTrue(sent, sent.endsWith(""","instruction":"make it two eggs"}"""))
        assertTrue(sent, sent.contains(""""total_nutrients":{"calories":{"value":206.8,"unit":"kcal"}"""))
    }

    @Test
    fun correctionKeepsTheServingWeightItWasGiven(): Unit = runBlocking {
        enqueue(TEXT_ANALYSIS.replace(""""unit": "large"}""", """"unit": "large", "weight_grams": 50}"""))
        enqueue(TEXT_ANALYSIS)
        val scan = client.foodAnalysis.analyzeDescription(SearchFoodsByNaturalLanguageRequest("three eggs"))
        assertEquals(50.0, scan.detections.single().food.serving.weightGrams!!, 0.0)
        server.takeRequest()

        client.foodAnalysis.correct(CorrectPhotoScanRequest(scan, "make it two eggs"))

        val sent = server.takeRequest().body.readUtf8()
        assertTrue(sent, sent.contains(""""serving":{"id":"34073350","quantity":1.0,"unit":"large","weight_grams":50.0}"""))
    }

    @Test
    fun correctionLeavesOutADetectionWithoutAServingSize(): Unit = runBlocking {
        enqueue(TEXT_ANALYSIS)
        fun detection(id: String, servingQuantity: Double?) = FoodDetection(
            DetectedFood(
                id = id,
                name = "food $id",
                brandName = null,
                nutrients = CompleteScanNutritionFacts(),
                serving = ServingSummary("11", servingQuantity, "cup"),
                quantity = 2.0,
            ),
        )
        val scan = FoodScan("Meal", CompleteScanNutritionFacts(), listOf(detection("1", 0.5), detection("2", null)))

        client.foodAnalysis.correct(CorrectPhotoScanRequest(scan, "make it one cup"))

        val sent = server.takeRequest().body.readUtf8()
        assertTrue(sent, sent.contains(""""id":"1""""))
        assertTrue(sent, sent.contains(""""serving":{"id":"11","quantity":0.5,"unit":"cup"}"""))
        // No serving size is invented for the second food: it is left out.
        assertTrue(sent, !sent.contains(""""id":"2""""))
    }

    @Test
    fun glucosePredictionRejectsAFractionalAgeBeforeSending(): Unit = runBlocking {
        fun request(age: Double) = PredictGlucoseRequest(
            userProfile = GlucosePredictionProfile(age, Sex.FEMALE, Height(66.0, HeightUnit.INCHES), Weight(150.0, WeightUnit.POUNDS)),
            foods = listOf(FoodSelection("70382174", ServingSelection("34073350", 1.0))),
            startTime = java.time.OffsetDateTime.parse("2026-09-22T12:00:00-04:00"),
        )

        val failure = runCatching { client.glucose.predict(request(35.5)) }.exceptionOrNull()
        assertTrue(failure.toString(), failure is JanuaryException)
        assertEquals(ErrorCategory.VALIDATION, (failure as JanuaryException).category)
        assertEquals(0, server.requestCount)

        enqueue("""{"points":[{"minutes":0,"value":90}],"impact_score":"low","chart":{"min":70,"max":140}}""")
        client.glucose.predict(request(35.0))
        assertTrue(server.takeRequest().body.readUtf8().contains(""""age":35"""))
    }

    @Test
    fun foodLogUpdateSendsOnlyTheFieldsSetAndRejectsAnEmptyPatch(): Unit = runBlocking {
        enqueue("""{"id":"78129823-8ba2-4183-b13b-71f0e963c606","foods":[],"eaten_at":"2026-09-13T11:34:56Z","name":"Lunch"}""")
        val user = PartnerUserContext(PartnerUserId("fixture-user"), "America/Chicago")

        client.foodLogs.update(UpdateFoodLogRequest("78129823-8ba2-4183-b13b-71f0e963c606", name = "Lunch", user = user))
        assertEquals("""{"name":"Lunch"}""", server.takeRequest().body.readUtf8())

        val failure = runCatching {
            client.foodLogs.update(UpdateFoodLogRequest("78129823-8ba2-4183-b13b-71f0e963c606", user = user))
        }.exceptionOrNull() as JanuaryException
        assertEquals(ErrorCategory.VALIDATION, failure.category)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun foodAnalysisWaitsLongerThanOtherRequests(): Unit = runBlocking {
        // A builder with a 1-second read timeout, and answers that take 2 seconds.
        val impatient = JanuaryPartnerClient.testing(
            "fixture-api-key",
            server.url("/").toString(),
            OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS),
        )
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json")
                .setBody("""{"meal_name":null,"total_nutrients":{},"detections":[]}""")
                .setHeadersDelay(2, TimeUnit.SECONDS),
        )
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json")
                .setBody("""{"total_count":0,"items":[]}""")
                .setHeadersDelay(2, TimeUnit.SECONDS),
        )

        // The analysis waits for its answer; any other request keeps the builder's timeout.
        assertTrue(impatient.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest("https://example.com/meal.jpg")).detections.isEmpty())
        val search = runCatching { impatient.foods.search(ai.january.partner.foods.SearchFoodsRequest("banana")) }.exceptionOrNull()
        assertEquals(ErrorCategory.TIMEOUT, (search as JanuaryException).category)
    }

    @Test
    fun photoScanSendsReasoningEffortOnlyWhenAsked(): Unit = runBlocking {
        enqueue("""{"meal_name":null,"total_nutrients":{},"detections":[]}""")
        enqueue("""{"meal_name":null,"total_nutrients":{},"detections":[]}""")
        enqueue("""{"meal_name":null,"total_nutrients":{},"detections":[]}""")

        client.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest("https://example.com/meal.jpg"))
        client.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest("https://example.com/meal.jpg", reasoningEffort = AnalysisEffort.XHIGH))
        client.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest("https://example.com/meal.jpg", reasoningEffort = AnalysisEffort.NONE))

        // Left out, the API picks its default (the reasoning-based analyzer); the SDK never sends one of its own.
        assertTrue(!server.takeRequest().body.readUtf8().contains("reasoning"))
        assertTrue(server.takeRequest().body.readUtf8().contains(""""reasoning":{"effort":"xhigh"}"""))
        assertTrue(server.takeRequest().body.readUtf8().contains(""""reasoning":{"effort":"none"}"""))
    }

    @Test
    fun alternativesKeepTheirServingList(): Unit = runBlocking {
        enqueue("""{"alternatives":[{"id":"70372230","name":"brown rice","brand_name":null,"nutrients":{"calories":{"value":108,"unit":"kcal"}},"servings":[{"id":"34113801","quantity":0.5,"unit":"cup"}]}]}""")

        val alternative = client.foods.suggestAlternatives(SuggestFoodAlternativesRequest("1")).alternatives.single()

        assertEquals("brown rice", alternative.name)
        assertEquals("cup", alternative.servings.single().unit)
        assertEquals(0.5, alternative.servings.single().quantity!!, 0.0)
        assertNull(alternative.servings.single().weightGrams)
    }

    @Test
    fun alternativesKeepTheServingWeight(): Unit = runBlocking {
        enqueue("""{"alternatives":[{"id":"70372230","name":"brown rice","brand_name":null,"nutrients":{"calories":{"value":108,"unit":"kcal"}},"servings":[{"id":"34113801","quantity":0.5,"unit":"cup","weight_grams":97.5}]}]}""")

        val serving = client.foods.suggestAlternatives(SuggestFoodAlternativesRequest("1")).alternatives.single().servings.single()

        assertEquals(97.5, serving.weightGrams!!, 0.0)
    }

    @Test
    fun foodLogSummaryDecodesAndSendsRangeParameters(): Unit = runBlocking {
        enqueue(SUMMARY)
        val user = PartnerUserContext(PartnerUserId("fixture-user"), "America/Chicago")

        val summary = client.foodLogs.getSummary(GetFoodLogSummaryRequest("2026-09-14", "2026-09-14", user = user))

        val request = server.takeRequest()
        assertEquals("/v1.2/food-logs/summary", request.requestUrl!!.encodedPath)
        assertEquals("2026-09-14", request.requestUrl!!.queryParameter("start_date"))
        assertEquals("2026-09-14", request.requestUrl!!.queryParameter("end_date"))
        assertEquals("America/Chicago", request.requestUrl!!.queryParameter("timezone"))
        assertEquals("day", request.requestUrl!!.queryParameter("group_by"))
        assertEquals("monday", request.requestUrl!!.queryParameter("week_start"))
        assertEquals("fixture-user", request.getHeader("January-End-User-ID"))

        assertEquals(FoodLogSummaryGrouping.DAY, summary.groupBy)
        assertNull(summary.weekStart)
        assertEquals("America/Chicago", summary.timezone)
        val day = summary.buckets.single()
        assertEquals("2026-09-14", day.startDate)
        assertEquals(1, day.logsCount)
        assertEquals(1853.06, day.nutrients.calories!!.value, 0.001)
        assertEquals(3, summary.totals.logsCount)
        assertEquals(2, summary.totals.daysWithLogs)
        assertEquals(1828.244, summary.averagePerLoggedDay.nutrients.calories!!.value, 0.001)
    }

    @Test
    fun weeklySummaryUsesTheRequestedWeekStart(): Unit = runBlocking {
        enqueue(SUMMARY.replace("\"group_by\": \"day\", \"week_start\": null", "\"group_by\": \"week\", \"week_start\": \"sunday\""))
        val user = client.forUser(PartnerUserId("fixture-user"), "America/Chicago")

        val summary = user.foodLogs.getSummary("2026-09-01", "2026-09-30", FoodLogSummaryGrouping.WEEK, WeekStart.SUNDAY)

        val url = server.takeRequest().requestUrl!!
        assertEquals("week", url.queryParameter("group_by"))
        assertEquals("sunday", url.queryParameter("week_start"))
        assertEquals(FoodLogSummaryGrouping.WEEK, summary.groupBy)
        assertEquals(WeekStart.SUNDAY, summary.weekStart)
    }

    private companion object {
        const val TEXT_ANALYSIS = """{"meal_name": null, "total_nutrients": {"calories": {"value": 206.8, "unit": "kcal"}, "protein": {"value": 14.58, "unit": "g"}, "carbohydrates": {"value": 12.72, "unit": "g"}, "total_fat": {"value": 10.39, "unit": "g"}}, "detections": [{"confidence": null, "food": {"id": "70382174", "name": "eggs", "brand_name": null, "nutrients": {"calories": {"value": 143, "unit": "kcal"}, "protein": {"value": 12.6, "unit": "g"}, "carbohydrates": {"value": 0.72, "unit": "g"}, "total_fat": {"value": 9.51, "unit": "g"}}, "quantity": 2, "serving": {"id": "34073350", "quantity": 1, "unit": "large"}}}]}"""
        const val SUMMARY = """{"group_by": "day", "week_start": null, "timezone": "America/Chicago", "start_date": "2026-09-14", "end_date": "2026-09-14", "buckets": [{"start_date": "2026-09-14", "end_date": "2026-09-14", "logs_count": 1, "days_with_logs": 1, "nutrients": {"calories": {"value": 1853.06, "unit": "kcal"}, "protein": {"value": 79.8822, "unit": "g"}, "carbohydrates": {"value": 199.5376, "unit": "g"}, "total_fat": {"value": 83.5442, "unit": "g"}}}], "totals": {"logs_count": 3, "days_with_logs": 2, "nutrients": {"calories": {"value": 3656.4883824999997, "unit": "kcal"}, "protein": {"value": 160.916828855, "unit": "g"}, "carbohydrates": {"value": 315.50554525, "unit": "g"}, "total_fat": {"value": 192.22509300000002, "unit": "g"}}}, "average_per_logged_day": {"nutrients": {"calories": {"value": 1828.2441912499999, "unit": "kcal"}, "protein": {"value": 80.4584144275, "unit": "g"}, "carbohydrates": {"value": 157.752772625, "unit": "g"}, "total_fat": {"value": 96.11254650000001, "unit": "g"}}}}"""
    }
}
