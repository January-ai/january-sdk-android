package ai.january.partner.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryDestinationsAreReachable() {
        composeRule.onNodeWithText("Food name").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Scan", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Scan a meal").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Food logs", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(
            "One food log represents one meal and can contain one or more foods.",
            substring = true,
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Glucose", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("ABOUT YOU").assertIsDisplayed()
    }
}
