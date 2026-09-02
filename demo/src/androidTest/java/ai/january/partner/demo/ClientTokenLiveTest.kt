package ai.january.partner.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class ClientTokenLiveTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun foodSearchUsesLocalClientTokenRelay() {
        assumeTrue(
            "Configure januaryPartnerTokenUrl to run the live client-token check.",
            BuildConfig.JANUARY_PARTNER_TOKEN_URL.isNotBlank(),
        )
        assertTrue("The live test must not embed an API key.", BuildConfig.JANUARY_API_KEY.isBlank())

        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onAllNodesWithText("Food name").fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Food name").performTextInput("pizza")
        composeRule.onNodeWithText("Search foods").performClick()
        composeRule.waitUntil(20_000) {
            runCatching {
                composeRule.onNodeWithTag("search-content").performScrollToIndex(5)
                composeRule.onAllNodesWithText("RESULTS · JANUARY FOOD DATABASE")
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("RESULTS · JANUARY FOOD DATABASE").assertIsDisplayed()
        assertTrue(
            "The client-token search displayed an authentication error.",
            composeRule.onAllNodesWithText("Couldn’t use the configured credentials")
                .fetchSemanticsNodes().isEmpty(),
        )
    }
}
