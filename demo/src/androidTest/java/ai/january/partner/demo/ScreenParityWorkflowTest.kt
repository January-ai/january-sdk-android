package ai.january.partner.demo

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import ai.january.partner.JanuaryClientToken
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.forJanuaryDevelopment
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.mockwebserver.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

/** Real Compose workflows with deterministic API responses; never writes production data. */
class ScreenParityWorkflowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var server: MockWebServer
    private val requests = CopyOnWriteArrayList<Pair<String, String>>()
    private val nutrients = """{"calories":{"value":100,"unit":"kcal"},"protein":{"value":4,"unit":"g"},"carbohydrates":{"value":20,"unit":"g"},"total_fat":{"value":2,"unit":"g"},"fiber":{"value":3,"unit":"g"},"sodium":{"value":10,"unit":"mg"}}"""
    private val servings = """[{"id":11,"quantity":1,"unit":"cup","scaling_factor":1,"weight_grams":100,"is_primary":true},{"id":12,"quantity":1,"unit":"oz","scaling_factor":0.2835,"weight_grams":28.35,"is_primary":false}]"""
    private fun food(id: Int = 101, name: String = "Fixture oatmeal", complete: Boolean = true) = """{"id":$id,"name":"$name","brand_name":"January fixture","nutrients":$nutrients,"servings":${if (complete) servings else JSONArray(servings).let { JSONArray().put(it.get(0)).toString() }}}"""
    private fun detected(id: Int = 101, name: String = "Fixture oatmeal") = """{"id":$id,"name":"$name","nutrients":$nutrients,"servings":[{"id":11,"quantity":1,"unit":"cup"}]}"""
    private val prediction = """{"prediction":[{"minutes":0,"value":90},{"minutes":30,"value":125},{"minutes":60,"value":140},{"minutes":90,"value":115},{"minutes":120,"value":95}],"impact_score":"medium","chart":{"min":70,"max":140}}"""
    private fun scan(name: String) = """{"meal_name":"$name","detections":[{"food":${detected()},"confidence_score":"high"}],"total_nutrients":$nutrients,"glucose_prediction":$prediction}"""

    @Before fun setup() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath
                requests += path to request.body.readUtf8()
                val body = when {
                    path == "/v1.2/foods/autocomplete" -> """{"items":[]}"""
                    path.endsWith("/alternatives") -> """{"alternatives":[{"food":${detected(102, "Fixture lentils")}}]}"""
                    path == "/v1.2/foods/101" -> food()
                    path == "/v1.2/foods/102" -> food(102, "Fixture lentils")
                    path == "/v1.2/foods" || path.contains("/foods/barcode/") -> """{"total_count":1,"items":[${food(complete = false)}]}"""
                    path == "/v1.2/restaurants" -> """{"total_count":1,"items":[{"type":"restaurant","id":"cafe","name":"Fixture Cafe","city":"San Francisco","address1":"123 Test Street","is_chain":false}]}"""
                    path == "/v1.2/restaurants/menu-items" -> """{"total_count":1,"items":[{"type":"menu_item","id":"101","name":"Fixture bowl","restaurant_name":"Fixture Cafe","nutrients":$nutrients,"servings":$servings}]}"""
                    path == "/v1.2/glucose/predictions" -> prediction
                    path == "/v1.2/food-scans/photo" -> scan("Fixture breakfast")
                    path.contains("correct") -> scan("Corrected breakfast")
                    path == "/v1.2/food-scans/text" -> """{"detections":[{"food":${detected()}}],"total_nutrients":$nutrients}"""
                    else -> return MockResponse().setResponseCode(404).setBody("""{"message":"Unmapped test route $path"}""")
                }
                return MockResponse().setHeader("Content-Type", "application/json").setBody(body)
            }
        }
        server.start()
        val client = JanuaryPartnerClient.forJanuaryDevelopment(provider = { JanuaryClientToken("fixture-client-token", 3600) }, apiBaseUrl = server.url("/").toString())
        ui.activityRule.scenario.onActivity { activity ->
            val state = DemoState(activity.applicationContext, client)
            activity.setContent { JanuaryDemoTheme { JanuaryDemoApp(state) } }
        }
    }
    @After fun cleanup() { server.shutdown() }

    @Test fun foodDetailsHydrateServingsPredictAndOpenAlternatives() {
        ui.onNodeWithText("Recipe").assertIsDisplayed()
        ui.onNodeWithText("Food name").performTextInput("oatmeal")
        click("Search foods")
        click("Fixture oatmeal")
        ui.waitUntil(10_000) { requests.any { it.first == "/v1.2/foods/101" } }
        clickDescription("Choose a serving", scroll = true)
        click("oz · 28.35 g")
        clickDescription("Decrease quantity", scroll = true)
        ui.onNodeWithText("Quantity: 0.75 oz").assertExists()
        capture("food-detail-fixture")
        click("Check glucose")
        waitText("Medium impact")
        capture("food-glucose-fixture")
        click("Prediction data")
        ui.onNodeWithText("+60 min").assertExists()
        val body = JSONObject(requests.first { it.first == "/v1.2/glucose/predictions" }.second)
        assertEquals(12, body.getJSONArray("foods").getJSONObject(0).getJSONObject("serving").getInt("id"))
        assertEquals(0.75, body.getJSONArray("foods").getJSONObject(0).getJSONObject("serving").getDouble("quantity"), 0.0)
        clickDescription("Close Glucose response")
        click("Find alternatives")
        click("Find alternatives")
        click("Fixture lentils")
        waitText("Food details")
        ui.onNodeWithText("Fixture lentils").assertExists()
    }

    @Test fun restaurantFiltersMenuDetailsAndGlucoseAreConnected() {
        click("Restaurants")
        click("Search location")
        waitText("Search filters")
        capture("restaurant-filters-fixture")
        click("Apply filters")
        ui.onNodeWithText("Restaurant name").performTextInput("Fixture Cafe")
        click("Search nearby")
        click("Fixture Cafe")
        click("Fixture bowl")
        clickDescription("Choose a serving", scroll = true)
        click("oz · 28.35 g")
        click("See glucose impact")
        waitText("Medium impact")
        assertTrue(requests.any { it.first == "/v1.2/restaurants/menu-items" })
        assertTrue(requests.any { it.first == "/v1.2/glucose/predictions" })
    }

    @Test fun photoPreviewResultsCorrectionAndUrlEntryWork() {
        clickDescription("Scan")
        click("Image URL")
        ui.onAllNodesWithText("Use image URL").onFirst().assertExists()
        clickDescription("Close Use image URL")
        click("Sample meal")
        click("Analyze meal")
        waitText("Fixture breakfast")
        capture("scan-results-fixture")
        click("Correct result")
        ui.onNodeWithText("Describe the correction").performTextInput("This was lentils")
        click("Submit correction")
        waitText("Corrected breakfast")
        click("Scan another meal")
        ui.onNodeWithText("Take photo").assertExists()
        assertTrue(requests.any { it.first.contains("correct") })
    }

    @Test fun glucoseProfileConditionsFoodSelectionResultAndSettingsWork() {
        clickDescription("Glucose")
        capture("glucose-profile-fixture")
        click("Health conditions")
        click("Prediabetes")
        clickDescription("Back from Health conditions")
        click("＋  Add food to prediction")
        ui.onNodeWithText("Search foods").performTextInput("oatmeal")
        ui.onNode(hasSetTextAction() and hasText("oatmeal")).performImeAction()
        click("Fixture oatmeal")
        waitText("Choose serving")
        capture("serving-picker-fixture")
        click("Add to meal")
        click("Estimate glucose response")
        waitText("LIKELY PEAK")
        capture("glucose-result-fixture")
        click("Adjust meal")
        clickDescription("Settings", scroll = true)
        waitText("January SDK")
        capture("settings-fixture")
        ui.onNodeWithText("End user ID").assertExists()
    }

    private fun waitText(text: String) { ui.waitUntil(12_000) { ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) {
        waitText(text)
        val node = ui.onAllNodesWithText(text).onLast()
        runCatching { node.performScrollTo() }
        node.performClick()
    }
    private fun clickDescription(description: String, scroll: Boolean = false) {
        val node = ui.onAllNodesWithContentDescription(description, useUnmergedTree = true).onLast()
        if (scroll) runCatching { node.performScrollTo() }
        node.performClick()
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
