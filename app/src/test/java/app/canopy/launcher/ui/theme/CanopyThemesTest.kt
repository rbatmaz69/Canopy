package app.canopy.launcher.ui.theme

import androidx.compose.ui.graphics.luminance
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.data.WallpaperPresetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanopyThemesTest {
    @Test
    fun `all 3 theme variants and 2 color modes resolve expected radius blur and flags`() {
        val myLight = CanopyThemes.getThemeTokens(ThemeVariantId.MATERIAL_YOU, ColorMode.LIGHT)
        assertEquals(22f, myLight.tileRadiusDp, 1e-4f)
        assertFalse(myLight.isGlass)
        assertFalse(myLight.isBold)
        assertEquals(0f, myLight.blurDp, 1e-4f)
        assertEquals(6f, myLight.gapUseDp, 1e-4f)
        assertEquals(12f, myLight.gapEditDp, 1e-4f)

        val boldDark = CanopyThemes.getThemeTokens(ThemeVariantId.DARK_BOLD, ColorMode.DARK)
        assertEquals(20f, boldDark.tileRadiusDp, 1e-4f)
        assertTrue(boldDark.isBold)
        assertFalse(boldDark.isGlass)

        val glassDark = CanopyThemes.getThemeTokens(ThemeVariantId.GLASS, ColorMode.DARK)
        assertEquals(24f, glassDark.tileRadiusDp, 1e-4f)
        assertTrue(glassDark.isGlass)
        assertEquals(20f, glassDark.blurDp, 1e-4f)
    }

    @Test
    fun `switching wallpaper worlds updates pill accents and extracts Material You surface tones`() {
        val sand = CanopyThemes.getThemeTokens(ThemeVariantId.MATERIAL_YOU, ColorMode.LIGHT, WallpaperPresetId.SAND)
        val cobalt = CanopyThemes.getThemeTokens(ThemeVariantId.MATERIAL_YOU, ColorMode.LIGHT, WallpaperPresetId.COBALT)
        val emerald = CanopyThemes.getThemeTokens(ThemeVariantId.MATERIAL_YOU, ColorMode.DARK, WallpaperPresetId.EMERALD)
        val terracotta = CanopyThemes.getThemeTokens(ThemeVariantId.MATERIAL_YOU, ColorMode.DARK, WallpaperPresetId.TERRACOTTA)

        assertNotEquals(sand.pillActive, cobalt.pillActive)
        assertNotEquals(emerald.pillActive, terracotta.pillActive)
        assertNotEquals(
            sand.getTileSurface(accentHue = 32f).backgroundStart,
            cobalt.getTileSurface(accentHue = 32f).backgroundStart,
        )
    }

    @Test
    fun `all 24 theme x mode x wallpaper combinations maintain readable text contrast`() {
        for (variant in ThemeVariantId.entries) {
            for (mode in ColorMode.entries) {
                for (wp in WallpaperPresetId.entries) {
                    val tokens = CanopyThemes.getThemeTokens(variant, mode, wp)
                    val dock = CanopyThemes.getDockCapsuleTokens(tokens)
                    assertTrue(dock.activePillBg != dock.activeTextColor)

                    for (emphasis in SurfaceEmphasis.entries) {
                        val surface = tokens.getTileSurface(accentHue = 200f, emphasis = emphasis)
                        assertTrue(surface.specularHighlightAlpha > 0f)
                        if (!tokens.isGlass) {
                            val bgLum = surface.backgroundStart.luminance()
                            val fgLum = surface.textPrimary.luminance()
                            assertTrue(
                                "Low contrast for $variant/$mode/$wp/$emphasis: bg=$bgLum fg=$fgLum",
                                kotlin.math.abs(bgLum - fgLum) > 0.25f,
                            )
                        } else {
                            assertEquals(1f, surface.textPrimary.luminance(), 0.01f)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `rgbToHue extracts primary hues and overrideBaseHue adapts Material You palette`() {
        assertEquals(0f, CanopyThemes.rgbToHue(255, 0, 0), 0.1f)
        assertEquals(120f, CanopyThemes.rgbToHue(0, 255, 0), 0.1f)
        assertEquals(240f, CanopyThemes.rgbToHue(0, 0, 255), 0.1f)
        assertEquals(215f, CanopyThemes.rgbToHue(128, 128, 128), 0.1f)

        val defaultTokens = CanopyThemes.getThemeTokens(ThemeVariantId.MATERIAL_YOU, ColorMode.LIGHT, WallpaperPresetId.SAND)
        val extractedGreen = CanopyThemes.getThemeTokens(
            ThemeVariantId.MATERIAL_YOU,
            ColorMode.LIGHT,
            WallpaperPresetId.SAND,
            overrideBaseHue = 120f,
        )
        assertNotEquals(defaultTokens.pillActive, extractedGreen.pillActive)
        assertNotEquals(
            defaultTokens.getTileSurface(accentHue = null).backgroundStart,
            extractedGreen.getTileSurface(accentHue = null).backgroundStart,
        )
    }
}
