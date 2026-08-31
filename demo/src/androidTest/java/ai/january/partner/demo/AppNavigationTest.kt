package ai.january.partner.demo

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun compactTitleRemainsCenteredAndFixedWhenContentScrolls() {
        ui.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                JanuaryDemoTheme {
                    AppScreenScaffold(
                        title = "Details",
                        modifier = Modifier.padding(top = 24.dp),
                        leading = { AppNavigationButton(AppNavigationButtonKind.Back, onClick = {}) },
                        trailing = {
                            AppNavigationButton(AppNavigationButtonKind.Add, onClick = {})
                            AppNavigationButton(AppNavigationButtonKind.Settings, onClick = {})
                        },
                    ) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            repeat(30) { Text("Content $it", Modifier.height(80.dp).testTag("content-$it")) }
                        }
                    }
                }
            }
        }
        val bar = ui.onNodeWithTag("app-navigation-bar")
        val title = ui.onNodeWithTag("app-navigation-title")
        bar.assertHeightIsEqualTo(56.dp)
        val initialBounds = bar.getUnclippedBoundsInRoot()
        assertEquals("The host inset must be applied exactly once", 24f, initialBounds.top.value, 0.5f)
        val titleBounds = title.getUnclippedBoundsInRoot()
        assertEquals(
            "Asymmetric actions must not move the title",
            (initialBounds.left.value + initialBounds.right.value) / 2,
            (titleBounds.left.value + titleBounds.right.value) / 2,
            0.5f,
        )
        ui.onNodeWithTag("content-29").performScrollTo().assertIsDisplayed()
        assertEquals(initialBounds, bar.getUnclippedBoundsInRoot())
        ui.onNodeWithContentDescription("Back").assertIsDisplayed()
        ui.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
    @Test fun largeRootTitleCollapsesWhileNavigationActionsRemainReachable() {
        ui.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                JanuaryDemoTheme {
                    AppScreenScaffold(
                        title = "Search", style = AppNavigationTitleStyle.Leading,
                        trailing = { AppNavigationButton(AppNavigationButtonKind.Settings, onClick = {}) },
                    ) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("scroll-content")) {
                            repeat(30) { Text("Content $it", Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
        ui.onNodeWithTag("app-navigation-bar").assertHeightIsEqualTo(112.dp)
        ui.onNodeWithTag("scroll-content").performTouchInput { swipeUp() }
        ui.onNodeWithTag("app-navigation-bar").assertHeightIsEqualTo(56.dp)
        ui.onNodeWithText("Search").assertIsDisplayed()
        ui.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

}
