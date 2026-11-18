package app.canopy.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class CanopyLauncherComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<CanopyLauncherActivity>()

    @Test
    fun launcherDisplaysStatusBarAndOpensSearchOverlayOnDockTap() {
        composeRule.onNodeWithText("09:01").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Suche öffnen").performClick()
        composeRule.onNodeWithText("Apps, Kontakte oder Web-Suche (Enter)…").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Suche schließen").performClick()
    }
}
