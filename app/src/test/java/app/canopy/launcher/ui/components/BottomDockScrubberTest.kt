package app.canopy.launcher.ui.components

import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.ui.theme.CanopyThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomDockScrubberTest {
    @Test
    fun `progress start fraction tracks horizontal pager offset across all pages`() {
        assertEquals(0f, PageScrubberMath.computeProgressStartFraction(0f, 390f, 5), 1e-4f)
        assertEquals(0.2f, PageScrubberMath.computeProgressStartFraction(-390f, 390f, 5), 1e-4f)
        assertEquals(0.8f, PageScrubberMath.computeProgressStartFraction(-1560f, 390f, 5), 1e-4f)
        // Clamps during rubberband
        assertEquals(0f, PageScrubberMath.computeProgressStartFraction(80f, 390f, 5), 1e-4f)
        assertEquals(0.8f, PageScrubberMath.computeProgressStartFraction(-1700f, 390f, 5), 1e-4f)
    }

    @Test
    fun `scrub pointer coordinates map cleanly to page indices`() {
        assertEquals(0, PageScrubberMath.pageIndexForScrubX(5f, 250f, 5))
        assertEquals(1, PageScrubberMath.pageIndexForScrubX(60f, 250f, 5))
        assertEquals(2, PageScrubberMath.pageIndexForScrubX(125f, 250f, 5))
        assertEquals(4, PageScrubberMath.pageIndexForScrubX(249f, 250f, 5))
        assertEquals(4, PageScrubberMath.pageIndexForScrubX(320f, 250f, 5))
    }

    @Test
    fun `compact labels activate cleanly for 6 or more pages and capsule tokens match theme`() {
        assertFalse(PageScrubberMath.shouldUseCompactLabels(5))
        assertTrue(PageScrubberMath.shouldUseCompactLabels(6))

        val glass = CanopyThemes.getDockCapsuleTokens(
            CanopyThemes.getThemeTokens(ThemeVariantId.GLASS, ColorMode.DARK)
        )
        assertNotEquals(glass.activePillBg, glass.activeTextColor)
    }
}
