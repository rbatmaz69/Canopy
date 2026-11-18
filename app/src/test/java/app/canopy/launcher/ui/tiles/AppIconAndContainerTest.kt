package app.canopy.launcher.ui.tiles

import androidx.compose.ui.graphics.Color
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.components.AppIconRegistry
import app.canopy.launcher.ui.components.IconStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconAndContainerTest {
    @Test
    fun `every iconKey resolves to a valid Material vector icon without text fallback`() {
        val sampleKeys = listOf(
            "Phone", "MessageCircle", "Camera", "Compass", "Mail", "Music2",
            "MapPin", "FileText", "Headphones", "Activity", "Settings", "Calendar",
        )
        sampleKeys.forEach { key ->
            assertTrue("Missing key $key", key in AppIconRegistry.supportedKeys)
            val vector = AppIconRegistry.resolveIcon(key)
            assertTrue(vector.name.isNotBlank())
        }
        assertTrue(AppIconRegistry.supportedKeys.size >= 70)
    }

    @Test
    fun `tonal vs vibrant icon style changes glyph and background colors immediately`() {
        val brand = Color(0xFF3B82F6)
        val tonal = AppIconRegistry.resolveSquircleColors(
            brandColor = brand,
            style = IconStyleConfig(tonalIcons = true, themeId = ThemeVariantId.MATERIAL_YOU, colorMode = ColorMode.LIGHT),
        )
        val vibrant = AppIconRegistry.resolveSquircleColors(
            brandColor = brand,
            style = IconStyleConfig(tonalIcons = false, themeId = ThemeVariantId.MATERIAL_YOU, colorMode = ColorMode.LIGHT),
        )
        assertNotEquals(tonal.glyphColor, vibrant.glyphColor)
        assertEquals(Color.White, vibrant.glyphColor)

        val glassTonal = AppIconRegistry.resolveSquircleColors(
            brandColor = brand,
            style = IconStyleConfig(tonalIcons = true, themeId = ThemeVariantId.GLASS, colorMode = ColorMode.DARK),
        )
        assertEquals(Color.White, glassTonal.glyphColor)
    }

    @Test
    fun `clearedBadgeAppIds suppresses unread badges synchronously`() {
        assertEquals(3, AppIconRegistry.effectiveBadge("app-nachrichten", 3, emptySet()))
        assertNull(AppIconRegistry.effectiveBadge("app-nachrichten", 3, setOf("app-nachrichten")))
        assertEquals(5, AppIconRegistry.effectiveBadge("app-mail", 5, setOf("app-nachrichten")))
        assertNull(AppIconRegistry.effectiveBadge("app-telefon", 0, emptySet()))
    }

    @Test
    fun `container metrics apply full-bleed and tier-specific padding rules`() {
        val mediaHero = TileData(id = "m1", type = TileType.MEDIA, title = "Medien", mediaLarge = true)
        val photos = TileData(id = "p1", type = TileType.PHOTOS, title = "Fotos")
        val widget = TileData(id = "w1", type = TileType.THIRD_PARTY_WIDGET, title = "Podcast")
        val clock = TileData(id = "c1", type = TileType.CLOCK, title = "Uhr")

        assertTrue(TileContainerMetrics.isHeroTile(clock))
        assertTrue(TileContainerMetrics.isHeroTile(mediaHero))
        assertFalse(TileContainerMetrics.isHeroTile(photos))

        assertEquals(0f, TileContainerMetrics.resolvePaddingDp(mediaHero, TileSizeTier.M), 1e-4f)
        assertEquals(0f, TileContainerMetrics.resolvePaddingDp(photos, TileSizeTier.M), 1e-4f)
        assertEquals(6f, TileContainerMetrics.resolvePaddingDp(photos, TileSizeTier.XS), 1e-4f)
        assertEquals(8f, TileContainerMetrics.resolvePaddingDp(widget, TileSizeTier.M), 1e-4f)
        assertEquals(13f, TileContainerMetrics.resolvePaddingDp(clock, TileSizeTier.M), 1e-4f)
    }
}
