package ai.january.partner.demo

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import ai.january.partner.JanuaryClientToken
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.forJanuaryDevelopment
import ai.january.partner.scanner.JanuaryFoodScanner
import ai.january.partner.scanner.JanuaryFoodScannerResult
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ScannerWorkflowTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After fun tearDown() {
        server.shutdown()
    }

    @Test fun capturedPhotoReachesApiAndCanBeRetriedAfterFailure() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.CAMERA)
        server.enqueue(json("""{"message":"Temporary scan failure"}""", 500).setBodyDelay(2, TimeUnit.SECONDS))
        server.enqueue(json("""{"meal_name":"Camera fixture","detections":[]}"""))
        val client = JanuaryPartnerClient.forJanuaryDevelopment(
            provider = { JanuaryClientToken("fixture-client-token", 3600) },
            apiBaseUrl = server.url("/").toString(),
        )
        val result = AtomicReference<JanuaryFoodScannerResult?>()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                JanuaryDemoTheme {
                    JanuaryFoodScanner(client, onResult = result::set, onCancel = {})
                }
            }
        }

        waitForShutter()
        composeRule.onNodeWithContentDescription("Take meal photo").performClick()
        composeRule.waitUntil(20_000) { server.requestCount == 1 }
        composeRule.onNodeWithText("Barcode").assertIsNotEnabled()
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText("Try Again").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Try Again").performClick()
        waitForShutter()
        composeRule.onNodeWithContentDescription("Take meal photo").performClick()
        composeRule.waitUntil(20_000) { result.get() != null }

        assertTrue(result.get() is JanuaryFoodScannerResult.Photo)
        repeat(2) {
            val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertEquals("/v1.2/food-scans/photo", request.path)
            assertTrue(JSONObject(request.body.readUtf8()).getString("image").startsWith("data:image/jpeg;base64,"))
        }
        assertEquals(2, server.requestCount)
    }

    private fun waitForShutter() {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithContentDescription("Take meal photo").fetchSemanticsNodes().any {
                it.config.getOrNull(SemanticsProperties.Disabled) == null
            }
        }
    }

    private fun json(body: String, status: Int = 200) = MockResponse()
        .setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)
}
