package ai.january.partner

import ai.january.partner.foodlogs.CreateFoodLogRequest
import ai.january.partner.foodlogs.DeleteFoodLogRequest
import ai.january.partner.foodlogs.FoodLogUserContext
import ai.january.partner.foodlogs.ListFoodLogsRequest
import ai.january.partner.foodlogs.UpdateFoodLogRequest
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.SuggestFoodAlternativesRequest
import ai.january.partner.glucose.Gender
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import ai.january.partner.photos.CorrectPhotoScanRequest
import ai.january.partner.photos.FoodDetection
import ai.january.partner.photos.ScanFoodPhotoRequest
import ai.january.partner.foods.DetectedFood
import ai.january.partner.foods.DetectedServing
import ai.january.partner.models.CompleteScanNutritionFacts
import ai.january.partner.restaurants.SearchRestaurantsRequest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

public class PublicSurfaceTest {
    private lateinit var server: MockWebServer

    @Before
    public fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    public fun tearDown() {
        server.shutdown()
    }

    @Test
    public fun allThirteenOperationsAreExposedThroughThePublicClient(): Unit = runBlocking {
        val responses = listOf(
            envelope, envelope, """{"detections":[]}""", """{"alternatives":[]}""",
            envelope, envelope, photo, photo, foodLog, """{"total_count":0,"items":[]}""",
            foodLog, """{"status":"deleted"}""",
            """{"prediction":[{"minutes":0,"value":100}],"impact_score":"low","chart":{"min":70,"max":140}}""",
        )
        responses.forEach { body ->
            server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body))
        }
        val client = JanuaryPartnerClient.testing(
            apiKey = "fixture-api-key",
            baseUrl = server.url("/").toString(),
            clientBuilder = OkHttpClient.Builder(),
        )
        val userId = PartnerUserId("fixture-user")
        val user = FoodLogUserContext(userId, "America/New_York")
        val food = FoodSelection(1, ServingSelection(2, 1.0))
        val detection = FoodDetection(
            DetectedFood(
                1,
                "Banana",
                nutrients = CompleteScanNutritionFacts(),
                servings = listOf(DetectedServing(2, 1.0, "serving")),
            ),
        )

        client.foods.search(SearchFoodsRequest("banana", endUserId = userId))
        client.foods.lookupBarcode(LookupFoodByBarcodeRequest("049000006346", userId))
        client.foods.searchNaturalLanguage(SearchFoodsByNaturalLanguageRequest("one banana", userId))
        client.foods.suggestAlternatives(SuggestFoodAlternativesRequest(1, endUserId = userId))
        client.restaurants.search(SearchRestaurantsRequest("cafe", 40.0, -74.0, endUserId = userId))
        client.restaurants.searchMenuItems(SearchRestaurantsRequest("salad", 40.0, -74.0, endUserId = userId))
        client.photoScanning.scan(ScanFoodPhotoRequest("fixture-image", userId))
        client.photoScanning.correct(CorrectPhotoScanRequest("Meal", listOf(detection), "Add banana", userId))
        val created = client.foodLogs.create(CreateFoodLogRequest(listOf(food), user = user))
        client.foodLogs.list(ListFoodLogsRequest("2026-08-21", "2026-08-23", user))
        client.foodLogs.update(UpdateFoodLogRequest(created.id, name = "Updated", user = user))
        client.foodLogs.delete(DeleteFoodLogRequest(created.id, user))
        client.glucose.predict(
            PredictGlucoseRequest(
                GlucosePredictionProfile(35.0, Gender.MALE, 70.0, 175.0),
                listOf(food), OffsetDateTime.now(ZoneOffset.UTC), endUserId = userId,
            ),
        )

        val paths = List(13) { server.takeRequest().requestUrl!!.encodedPath }
        assertEquals(
            listOf(
                "/v1.2/foods", "/v1.2/foods/barcode/049000006346", "/v1.2/food-scans/text",
                "/v1.2/foods/1/alternatives", "/v1.2/restaurants", "/v1.2/restaurants/menu-items",
                "/v1.2/food-scans/photo", "/v1.2/food-scans/corrections", "/v1.2/food-logs", "/v1.2/food-logs",
                "/v1.2/food-logs/00000000-0000-0000-0000-000000000001",
                "/v1.2/food-logs/00000000-0000-0000-0000-000000000001", "/v1.2/glucose/predictions",
            ),
            paths,
        )
    }

    private companion object {
        const val envelope = """{"total_count":0,"items":[]}"""
        const val photo = """{"meal_name":"Fixture meal","detections":[]}"""
        const val foodLog = """{"id":"00000000-0000-0000-0000-000000000001","foods":[],"timestamp_utc":"2026-08-22T12:00:00Z","name":"Fixture"}"""
    }
}
