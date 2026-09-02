package ai.january.partner.demo

import android.graphics.Bitmap
import ai.january.partner.JanuaryClientToken
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.forJanuaryDevelopment
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUpDemo() {
        val fixtureOrigin = InstrumentationRegistry.getArguments().getString("fixtureOrigin")
            ?: "http://127.0.0.1:18766"
        val client = JanuaryPartnerClient.forJanuaryDevelopment(
            provider = { JanuaryClientToken("fixture-client-token", 3_600) },
            apiBaseUrl = fixtureOrigin,
        )
        composeRule.activityRule.scenario.onActivity { activity ->
            val state = DemoState(activity.applicationContext, client)
            state.endUserId = "navigation-user"
            activity.setContent { JanuaryDemoTheme { JanuaryDemoApp(state) } }
        }
    }

    @Test
    fun primaryDestinationsAreReachable() {
        composeRule.onNodeWithText("Food name").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Use voice input").assertIsDisplayed()
        capture("search-voice-capture")

        composeRule.onNodeWithContentDescription("Scan", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Scan a meal").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Food Logs", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(
            "Build one complete meal",
            substring = true,
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Add food log").performClick()
        composeRule.onNodeWithText("Build this meal").assertIsDisplayed()
        composeRule.onNodeWithText("Save food log").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Add first food").performScrollTo().performClick()
        composeRule.onNodeWithText("Search foods").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close Add food").performClick()
        composeRule.onNodeWithContentDescription("Close New food log").performClick()

        composeRule.onNodeWithContentDescription("Glucose", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Estimate this meal’s response").assertIsDisplayed()
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(directory, "$name.png")
        file.outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
