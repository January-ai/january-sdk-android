package ai.january.partner.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
}
