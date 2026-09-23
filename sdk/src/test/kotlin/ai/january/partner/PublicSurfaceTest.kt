package ai.january.partner

import ai.january.partner.foodlogs.CreateFoodLogRequest
import ai.january.partner.foodlogs.DeleteFoodLogRequest
import ai.january.partner.foodlogs.FoodLogUserContext
import ai.january.partner.foodlogs.GetFoodLogRequest
import ai.january.partner.foodlogs.ListFoodLogsRequest
import ai.january.partner.foodlogs.UpdateFoodLogRequest
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.foods.AutocompleteFoodsRequest
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.SuggestFoodAlternativesRequest
import ai.january.partner.glucose.Gender
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.waterlogs.CreateWaterLogRequest
import ai.january.partner.waterlogs.DeleteWaterLogRequest
import ai.january.partner.waterlogs.ListWaterLogsRequest
import ai.january.partner.waterlogs.VolumeUnit
import ai.january.partner.waterlogs.WaterAmount
import ai.january.partner.weightlogs.CreateWeightLogRequest
import ai.january.partner.weightlogs.ListWeightLogsRequest
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import ai.january.partner.photos.CorrectPhotoScanRequest
import ai.january.partner.photos.FoodDetection
import ai.january.partner.photos.ScanFoodPhotoRequest
import ai.january.partner.foods.DetectedFood
import ai.january.partner.foods.ServingSummary
import ai.january.partner.models.CompleteScanNutritionFacts
import ai.january.partner.restaurants.SearchRestaurantsRequest
import ai.january.partner.restaurants.GetRestaurantMenuItemsRequest
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
    public fun allContractOperationsAreExposedThroughThePublicClient(): Unit = runBlocking {
        val responses = listOf(
            """{"items":[]}""", foodItem, envelope, foodItem,
            photo, """{"alternatives":[]}""",
            envelope, envelope, envelope, photo, photo, foodLog, envelope,
            foodLog, foodLog, "",
            """{"points":[{"minutes":0,"value":100}],"impact_score":"low","chart":{"min":70,"max":140}}""",
            waterLog, """{"items":[]}""", "", weightLog, """{"items":[]}""",
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
                serving = ServingSummary(2, 1.0, "serving"),
                quantity = 1.0,
            ),
        )

        client.foods.autocomplete(AutocompleteFoodsRequest("ban", endUserId = userId))
        client.foods.get(GetFoodRequest(FoodId(1), userId))
        client.foods.search(SearchFoodsRequest("banana", endUserId = userId))
        client.foods.lookupBarcode(LookupFoodByBarcodeRequest("049000006346", userId))
        client.foodAnalysis.analyzeDescription(SearchFoodsByNaturalLanguageRequest("one banana", userId))
        client.foods.suggestAlternatives(SuggestFoodAlternativesRequest(1, endUserId = userId))
        client.restaurants.search(SearchRestaurantsRequest("cafe", 40.0, -74.0, endUserId = userId))
        client.restaurants.searchMenuItems(SearchRestaurantsRequest("salad", 40.0, -74.0, endUserId = userId))
        client.restaurants.getMenuItems(GetRestaurantMenuItemsRequest("restaurant-1", endUserId = userId))
        client.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest("fixture-image", userId))
        client.foodAnalysis.correct(CorrectPhotoScanRequest(ai.january.partner.photos.FoodScan("Meal", CompleteScanNutritionFacts(), listOf(detection)), "Add banana", userId))
        val created = client.foodLogs.create(CreateFoodLogRequest(listOf(food), user = user))
        val logId = requireNotNull(created.id)
        client.foodLogs.list(ListFoodLogsRequest("2026-08-21", "2026-08-23", user))
        client.foodLogs.get(GetFoodLogRequest(logId, user))
        client.foodLogs.update(UpdateFoodLogRequest(logId, name = "Updated", user = user))
        client.foodLogs.delete(DeleteFoodLogRequest(logId, user))
        client.glucose.predict(
            PredictGlucoseRequest(
                GlucosePredictionProfile(35.0, Gender.MALE, 70.0, 175.0),
                listOf(food), OffsetDateTime.now(ZoneOffset.UTC), endUserId = userId,
            ),
        )

        val water = client.waterLogs.create(CreateWaterLogRequest(WaterAmount(8.0, VolumeUnit.FL_OZ), user = user))
        client.waterLogs.list(ListWaterLogsRequest("2026-08-21", "2026-08-23", VolumeUnit.FL_OZ, user))
        client.waterLogs.delete(DeleteWaterLogRequest(water.id, user))
        client.weightLogs.create(CreateWeightLogRequest(Weight(150.0, WeightUnit.POUNDS), user = user))
        client.weightLogs.list(ListWeightLogsRequest("2026-08-21", "2026-08-23", user))

        val paths = List(22) { server.takeRequest().requestUrl!!.encodedPath }
        assertEquals(
            listOf(
                "/v1.2/foods/autocomplete", "/v1.2/foods/1", "/v1.2/foods",
                "/v1.2/foods/barcode/049000006346", "/v1.2/food-analysis/text",
                "/v1.2/foods/1/alternatives", "/v1.2/restaurants", "/v1.2/menu-items",
                "/v1.2/restaurants/restaurant-1/menu-items", "/v1.2/food-analysis/image", "/v1.2/food-analysis/corrections", "/v1.2/food-logs", "/v1.2/food-logs",
                "/v1.2/food-logs/00000000-0000-0000-0000-000000000001",
                "/v1.2/food-logs/00000000-0000-0000-0000-000000000001",
                "/v1.2/food-logs/00000000-0000-0000-0000-000000000001", "/v1.2/glucose/predictions",
                "/v1.2/water-logs", "/v1.2/water-logs", "/v1.2/water-logs/9c1f2a3b-4d5e-4f60-8a71-b2c3d4e5f607",
                "/v1.2/weight-logs", "/v1.2/weight-logs",
            ),
            paths,
        )
    }

    @Test
    public fun servingSummaryKeepsItsThreeArgumentConstructorForJava() {
        // Java callers get no overloads from Kotlin default arguments, so the shape from before
        // weightGrams must still exist as its own constructor.
        val constructor = ServingSummary::class.java.getConstructor(String::class.java, java.lang.Double::class.java, String::class.java)
        assertEquals(ServingSummary("11", 1.0, "cup", null), constructor.newInstance("11", 1.0, "cup"))
    }

    private companion object {
        const val envelope = """{"total_count":0,"items":[]}"""
        const val foodItem = """{"id":"1","type":"generic","name":"Banana","brand_name":null,"nutrients":{},"glycemic_index":null,"glycemic_load":null,"image_url":null,"barcode":null,"servings":[{"id":"2","quantity":1,"unit":"serving","scaling_factor":1,"weight_grams":100,"is_primary":true}]}"""
        const val photo = """{"meal_name":"Fixture meal","total_nutrients":{},"detections":[]}"""
        const val foodLog = """{"id":"00000000-0000-0000-0000-000000000001","foods":[],"created_at":"2026-08-22T12:00:00Z","name":"Fixture"}"""
        const val waterLog = """{"id":"9c1f2a3b-4d5e-4f60-8a71-b2c3d4e5f607","amount":{"value":8,"unit":"fl_oz"},"created_at":"2026-08-22T12:00:00.000Z"}"""
        const val weightLog = """{"weight":{"value":150,"unit":"lb"},"created_at":"2026-08-22T12:00:00.000Z"}"""
    }
}
