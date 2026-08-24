package ai.january.partner

import ai.january.partner.foodlogs.CreateFoodLogRequest
import ai.january.partner.foodlogs.DeleteFoodLogRequest
import ai.january.partner.foodlogs.FoodLogUserContext
import ai.january.partner.foodlogs.ListFoodLogsRequest
import ai.january.partner.foodlogs.UpdateFoodLogRequest
import ai.january.partner.foods.DietPreference
import ai.january.partner.foods.DietRestriction
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.SuggestFoodAlternativesRequest
import ai.january.partner.glucose.ActivityLevel
import ai.january.partner.glucose.Gender
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.MedicalCondition
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import ai.january.partner.photos.CorrectPhotoScanRequest
import ai.january.partner.photos.ScanFoodPhotoRequest
import ai.january.partner.restaurants.SearchRestaurantsRequest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.Base64
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

public class AllEndpointsLiveTest {
    @Test
    public fun exercisesAllThirteenPublicOperations(): Unit = runBlocking {
        val apiKey = System.getenv("JANUARY_API_KEY")
        val rawUserId = System.getenv("JANUARY_END_USER_ID")
        assumeTrue("JANUARY_API_KEY is not configured.", !apiKey.isNullOrBlank())
        assumeTrue("JANUARY_END_USER_ID is not configured.", !rawUserId.isNullOrBlank())

        val client = JanuaryPartnerClient(apiKey.orEmpty())
        val userId = PartnerUserId(rawUserId.orEmpty())

        val search = client.foods.search(SearchFoodsRequest("banana", limit = 3, endUserId = userId))
        val food = search.items.firstOrNull() ?: error("foods.search returned no food.")
        val serving = food.servings.firstOrNull() ?: error("foods.search returned no serving.")
        pass("foods.search")

        val natural = client.foods.searchNaturalLanguage(
            SearchFoodsByNaturalLanguageRequest("one banana and a bowl of oatmeal", userId),
        )
        assertTrue(natural.detections.isNotEmpty())
        pass("foods.searchNaturalLanguage")

        client.foods.suggestAlternatives(
            SuggestFoodAlternativesRequest(
                foodId = food.id.value,
                dietRestrictions = emptyList(),
                dietPreferences = emptyList(),
                endUserId = userId,
            ),
        )
        pass("foods.suggestAlternatives")

        client.foods.lookupBarcode(LookupFoodByBarcodeRequest("049000006346", userId))
        pass("foods.lookupBarcode")

        client.restaurants.search(
            SearchRestaurantsRequest("mcdonalds", 37.7749, -122.4194, limit = 3, endUserId = userId),
        )
        pass("restaurants.search")

        client.restaurants.searchMenuItems(
            SearchRestaurantsRequest("burger", 37.7749, -122.4194, limit = 3, endUserId = userId),
        )
        pass("restaurants.searchMenuItems")

        val scan = client.photoScanning.scan(
            ScanFoodPhotoRequest(BURGER_IMAGE_URL, userId),
        )
        val mealName = requireNotNull(scan.mealName) { "photoScanning.scan returned no meal name." }
        val detections = requireNotNull(scan.detections).also { require(it.isNotEmpty()) }
        pass("photoScanning.scan")

        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("fixtures/photo-scanning/burger-and-fries.png"),
        ).use { it.readBytes() }
        val base64Scan = client.photoScanning.scan(
            ScanFoodPhotoRequest(
                "data:image/png;base64,${Base64.getEncoder().encodeToString(fixture)}",
                userId,
            ),
        )
        require(!base64Scan.mealName.isNullOrBlank() && !base64Scan.detections.isNullOrEmpty()) {
            "photoScanning.scan returned no detections for the base64 fixture."
        }
        pass("photoScanning.scan base64")

        client.photoScanning.correct(
            CorrectPhotoScanRequest(
                mealName, detections, "Rename the meal to January Android SDK smoke test meal.", userId,
            ),
        )
        pass("photoScanning.correct")

        val selectedFood = FoodSelection(
            id = food.id.value,
            serving = ServingSelection(serving.id.value, 1.0),
        )
        val user = FoodLogUserContext(userId, TIMEZONE)
        var createdLogId: String? = null
        try {
            val created = client.foodLogs.create(
                CreateFoodLogRequest(
                    foods = listOf(selectedFood),
                    timestampUtc = OffsetDateTime.now(ZoneOffset.UTC).toString(),
                    name = "January Android SDK smoke ${UUID.randomUUID()}",
                    user = user,
                ),
            )
            createdLogId = created.id
            pass("foodLogs.create")

            val today = LocalDate.now(ZoneOffset.UTC)
            val listed = client.foodLogs.list(
                ListFoodLogsRequest(today.minusDays(1).toString(), today.plusDays(1).toString(), user),
            )
            assertTrue(listed.items.any { it.id == created.id })
            pass("foodLogs.list")

            val updated = client.foodLogs.update(
                UpdateFoodLogRequest(created.id, name = "January Android SDK smoke updated", user = user),
            )
            assertEquals("January Android SDK smoke updated", updated.name)
            pass("foodLogs.update")

            val deleted = client.foodLogs.delete(DeleteFoodLogRequest(created.id, user))
            assertEquals("deleted", deleted.status)
            createdLogId = null
            pass("foodLogs.delete")
        } finally {
            createdLogId?.let { id -> runCatching { client.foodLogs.delete(DeleteFoodLogRequest(id, user)) } }
        }

        val prediction = client.glucose.predict(
            PredictGlucoseRequest(
                userProfile = GlucosePredictionProfile(
                    age = 35.0, gender = Gender.MALE, height = 70.0, weight = 175.0,
                    activityLevel = ActivityLevel.MODERATELY_ACTIVE,
                    healthConditions = emptyList(),
                ),
                foods = listOf(selectedFood),
                startTime = OffsetDateTime.now(ZoneOffset.UTC),
                endUserId = userId,
                timezone = TIMEZONE,
            ),
        )
        assertTrue(prediction.curve.isNotEmpty())
        pass("glucose.predict")
    }

    private fun pass(operation: String): Unit = println("PASS $operation")

    private companion object {
        const val TIMEZONE = "America/New_York"
        const val BURGER_IMAGE_URL = "https://friendlysrestaurants.com/assets/live/img/production/detail/menu/lunch-dinner_999-combohs_all-american-burger-fries.jpg"
    }
}
