package ai.january.partner.demo

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import ai.january.partner.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import org.junit.*
import org.junit.Assert.*

/** Same local fixture API and scenario names as the January iOS reference capture. */
class StateParityWorkflowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val http = OkHttpClient()
    private val origin by lazy {
        InstrumentationRegistry.getArguments().getString("fixtureOrigin")
            ?: "http://127.0.0.1:18766"
    }
    private var previousUser: String? = null
    private var previousTimezone: String? = null
    private val prefs get() = ui.activity.getSharedPreferences("january_demo", 0)
    private fun request(path: String): String {
        var lastConnectionFailure: IOException? = null
        repeat(3) { attempt ->
            try {
                return http.newCall(Request.Builder().url(origin + path).build()).execute().use {
                    check(it.isSuccessful)
                    it.body!!.string()
                }
            } catch (error: IOException) {
                lastConnectionFailure = error
                if (attempt < 2) Thread.sleep(1_000)
            }
        }
        throw checkNotNull(lastConnectionFailure)
    }
    private fun control(route: String, status: Int = 200, delay: Int = 0, empty: Boolean = false) {
        request("/__control?route=$route&status=$status&delay=$delay&empty=$empty")
    }
    @Before fun setup() {
        request("/__reset")
        previousUser = prefs.getString("end_user_id", null);previousTimezone = prefs.getString("timezone", null)
        val client = JanuaryPartnerClient.forJanuaryDevelopment(provider = { JanuaryClientToken("fixture-client-token",3600) }, apiBaseUrl = origin)
        ui.activityRule.scenario.onActivity { activity ->
            val state = DemoState(activity.applicationContext,client)
            state.endUserId = "parity-user";state.timezone = "America/New_York"
            activity.setContent { JanuaryDemoTheme { JanuaryDemoApp(state) } }
        }
    }
    @After fun cleanup() {
        prefs.edit().apply {
            if (previousUser == null) remove("end_user_id") else putString("end_user_id",previousUser)
            if (previousTimezone == null) remove("timezone") else putString("timezone",previousTimezone)
        }.commit()
    }
    private fun waitText(text: String) { ui.waitUntil(12_000) { ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun reveal(text: String): SemanticsNodeInteraction {
        waitText(text);return ui.onAllNodesWithText(text).onLast().also { runCatching { it.performScrollTo() } }
    }
    private fun tap(text: String) { reveal(text).performClick() }
    private fun desc(text: String) { ui.onAllNodesWithContentDescription(text,useUnmergedTree = true).onLast().performClick() }
    private fun capture(name: String) {
        ui.waitForIdle()
        val i = InstrumentationRegistry.getInstrumentation()
        val file = File(i.targetContext.getExternalFilesDir(null),"parity/android-$name.png");file.parentFile!!.mkdirs()
        file.outputStream().use { i.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    private fun addFood() {
        ui.onNodeWithText("Search foods").performTextInput("oatmeal")
        ui.onNode(hasSetTextAction() and hasText("oatmeal")).performImeAction()
        tap("Fixture oatmeal");waitText("Choose serving");capture("serving");tap("Add to meal")
    }
    @Test fun searchErrorsEmptyRecoveryAndDetailStates() {
        capture("search-initial")
        control("/v1.2/foods",500,4)
        ui.onNodeWithText("Food name").performTextInput("oatmeal");capture("search-keyboard")
        tap("Search foods");capture("search-loading")
        waitText("January couldn’t complete the request");reveal("Try again");capture("search-error")
        tap("Technical details");ui.onNodeWithText("fixture_error").assertExists();ui.onNodeWithText("parity-request").assertExists()
        for ((status,title) in listOf(401 to "Couldn’t use the configured credentials",403 to "Couldn’t use the configured credentials",404 to "No matching result was found",422 to "Check the information you entered",429 to "Too many requests",504 to "The request took too long")) {
            control("/v1.2/foods",status);tap("Try again");waitText(title);reveal("Try again");capture("search-error-$status")
        }
        control("/v1.2/foods",empty=true);tap("Try again");waitText("No foods found");capture("search-empty")
        control("/v1.2/foods");tap("Search foods");waitText("Fixture oatmeal");capture("search-results");tap("Fixture oatmeal");capture("food-detail")
        reveal("Check glucose");capture("food-nutrition")
        control("/v1.2/glucose/predictions",500,4);tap("Check glucose");capture("food-glucose-loading");waitText("January couldn’t complete the request");capture("food-glucose-error")
        control("/v1.2/glucose/predictions");tap("Try again");waitText("Medium impact");capture("food-glucose-result");desc("Close Glucose response")
        tap("Find alternatives");capture("alternatives-initial")
        control("/v1.2/foods/101/alternatives",500,4);tap("Find alternatives");capture("alternatives-loading");waitText("January couldn’t complete the request");reveal("Try again");capture("alternatives-error")
        control("/v1.2/foods/101/alternatives",empty=true);tap("Try again");waitText("No suitable alternatives");capture("alternatives-empty")
        control("/v1.2/foods/101/alternatives");tap("Refresh alternatives");waitText("Fixture lentils");capture("alternatives-results")
    }
    @Test fun scanAndCorrectionErrorsRecoverWithoutLosingInput() {
        desc("Scan");capture("scan-initial");tap("Image URL");capture("image-url");desc("Close Use image URL")
        tap("Sample meal");capture("scan-preview")
        control("/v1.2/food-analysis/image",500,4);tap("Analyze meal");capture("scan-loading");waitText("January couldn’t complete the request");reveal("Try again");capture("scan-error")
        control("/v1.2/food-analysis/image");tap("Try again");waitText("Fixture breakfast");capture("scan-result");tap("Correct result");capture("correction-initial")
        ui.onNodeWithText("Describe the correction").performTextInput("This was lentils")
        control("/v1.2/food-analysis/corrections",500,4);tap("Submit correction");capture("correction-loading");waitText("January couldn’t complete the request");reveal("Try again");capture("correction-error")
        ui.onNode(hasSetTextAction() and hasText("This was lentils")).assertExists()
        control("/v1.2/food-analysis/corrections");tap("Try again");waitText("Corrected breakfast");capture("correction-result")
    }
    @Test fun foodLogsLoadCreateEditDeleteAndRetry() {
        desc("Food Logs");capture("logs-initial");reveal("No food logs in this range");capture("logs-empty")
        control("/v1.2/food-logs",500,4);tap("Refresh food logs");capture("logs-loading");waitText("January couldn’t complete the request");reveal("Try again");capture("logs-error")
        control("/v1.2/food-logs");tap("Try again");waitText("No food logs in this range")
        desc("Add food log");capture("log-new");reveal("Save food log").assertIsNotEnabled();tap("Add first food");capture("food-picker-initial");addFood()
        control("/v1.2/food-logs",500,4);tap("Save food log");capture("log-save-loading");waitText("January couldn’t complete the request");reveal("Try again");capture("log-save-error")
        control("/v1.2/food-logs");tap("Try again");waitText("Fixture breakfast");reveal("Fixture breakfast");capture("logs-results");tap("Fixture breakfast");capture("log-detail");tap("Edit");capture("log-edit")
        tap("Update food log")
        ui.waitUntil(12_000) { ui.onAllNodesWithText("Edit food log").fetchSemanticsNodes().isEmpty() }
        waitText("BROWSE SAVED LOGS")
        val savedLog = hasText("Fixture breakfast") and hasClickAction()
        ui.waitUntil(12_000) { ui.onAllNodes(savedLog).fetchSemanticsNodes().isNotEmpty() }
        ui.onAllNodes(savedLog).onLast().performScrollTo().performClick()
        waitText("Delete food log")
        control("/v1.2/food-logs/11111111-1111-4111-8111-111111111111",500)
        tap("Delete food log")
        ui.waitUntil(12_000) { ui.onAllNodesWithTag("confirm-delete-food-log").fetchSemanticsNodes().isNotEmpty() }
        capture("log-delete-confirmation")
        ui.onNodeWithTag("confirm-delete-food-log").performClick()
        waitText("January couldn’t complete the request");reveal("Try again");capture("log-delete-error")
        control("/v1.2/food-logs/11111111-1111-4111-8111-111111111111");tap("Try again");waitText("No food logs in this range");capture("log-delete-result")
        val requests = request("/__requests")
        assertTrue(requests.contains("POST"));assertTrue(requests.contains("PATCH") || requests.contains("PUT"));assertTrue(requests.contains("DELETE"))
    }
    @Test fun glucoseFailureRetainsProfileAndMealThenRecovers() {
        desc("Glucose");capture("glucose-profile");tap("Health conditions");capture("conditions");tap("Prediabetes");desc("Back from Health conditions")
        tap("＋  Add food to prediction");capture("food-picker-initial");addFood()
        control("/v1.2/glucose/predictions",500,4);tap("Estimate glucose response");capture("glucose-loading");waitText("January couldn’t complete the request");reveal("Try again");capture("glucose-error")
        control("/v1.2/glucose/predictions");tap("Try again");waitText("LIKELY PEAK");capture("glucose-result");tap("Adjust meal");desc("Settings");capture("settings")
    }
    @Test fun restaurantSelectionLoadsItsMenuById() {
        tap("Restaurants")
        ui.onNodeWithText("Restaurant name").performTextInput("Fixture")
        tap("Search nearby");waitText("Fixture Cafe");tap("Fixture Cafe")
        waitText("Fixture bowl");capture("restaurant-id-regression")
        val requests = org.json.JSONArray(request("/__requests"))
        val menuRequest = (0 until requests.length()).map { requests.getJSONObject(it) }
            .first { it.getString("path") == "/v1.2/restaurants/cafe/menu-items" }
        assertEquals("0", menuRequest.getJSONObject("query").getString("offset"))
        assertTrue(!menuRequest.getJSONObject("query").has("query"))
        assertTrue((0 until requests.length()).none { requests.getJSONObject(it).getString("path") == "/v1.2/menu-items" })
        waitText("Fixture soup")
    }

    @Test fun restaurantMenuFallsBackWhenTheSelectedIdHasNoMenu() {
        tap("Restaurants")
        ui.onNodeWithText("Restaurant name").performTextInput("Fixture")
        tap("Search nearby");waitText("Fixture Cafe")
        control("/v1.2/restaurants/cafe/menu-items",404)
        tap("Fixture Cafe");waitText("Fixture bowl");waitText("Fixture soup")
        val requests = org.json.JSONArray(request("/__requests"))
        val paths = (0 until requests.length()).map { requests.getJSONObject(it).getString("path") }
        assertTrue(paths.contains("/v1.2/restaurants/cafe/menu-items"))
        assertTrue(paths.contains("/v1.2/menu-items"))
    }

    @Test fun restaurantSearchMenuLoadingEmptyErrorAndRecovery() {
        tap("Restaurants");capture("restaurants-initial");tap("Search location");capture("restaurant-filters");tap("Apply filters")
        ui.onNodeWithText("Restaurant name").performTextInput("Fixture Cafe")
        control("/v1.2/restaurants",500,4);tap("Search nearby");capture("restaurants-loading");waitText("January couldn’t complete the request");reveal("Try again");capture("restaurants-error")
        control("/v1.2/restaurants",empty=true);tap("Try again");waitText("No nearby matches");capture("restaurants-empty")
        control("/v1.2/restaurants");tap("Search nearby");waitText("Fixture Cafe")
        control("/v1.2/restaurants/cafe/menu-items",500,4);tap("Fixture Cafe");capture("menu-loading");waitText("January couldn’t complete the request");capture("menu-error")
        control("/v1.2/restaurants/cafe/menu-items",empty=true);tap("Try again");waitText("No menu items found");capture("menu-empty")
        desc("Back from Restaurant");control("/v1.2/restaurants/cafe/menu-items");tap("Fixture Cafe");waitText("Fixture bowl");capture("restaurant-detail");tap("Fixture bowl");capture("menu-detail")
    }
}
