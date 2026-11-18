package app.canopy.launcher.ui.tiles

import androidx.compose.ui.geometry.Rect
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.overlays.ContextMenuPositioner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFallbackAndContextShortcutsTest {
    @Test
    fun `third-party widget enforces Passepartout +10dp threshold and auto-falls back when cramped`() {
        // Podcast fixed size 164x144 -> needs >= 174w x 154h
        assertTrue(WidgetFrameEngine.fitsPassepartoutWidget(180f, 160f, 164f, 144f))
        assertFalse(WidgetFrameEngine.fitsPassepartoutWidget(173f, 160f, 164f, 144f))
        assertFalse(WidgetFrameEngine.fitsPassepartoutWidget(180f, 153f, 164f, 144f))

        // Breathing scale stays bounded between 0.95x and 1.15x
        assertEquals(0.95f, WidgetFrameEngine.computeBreathingScale(174f, 154f, 164f, 144f), 1e-4f)
        assertEquals(1.15f, WidgetFrameEngine.computeBreathingScale(340f, 340f, 164f, 144f), 1e-4f)
    }

    @Test
    fun `context menu placement never clips outside screen bounds near corners`() {
        val topLeftTile = Rect(8f, 40f, 120f, 140f)
        val p1 = ContextMenuPositioner.computePlacement(topLeftTile, 390f, 844f)
        assertTrue(p1.leftDp >= 12f)
        assertTrue(p1.topDp >= 46f)
        assertTrue(p1.leftDp + ContextMenuPositioner.MENU_WIDTH_DP <= 390f - 12f)

        val bottomRightTile = Rect(260f, 680f, 382f, 780f)
        val p2 = ContextMenuPositioner.computePlacement(bottomRightTile, 390f, 844f)
        assertTrue(p2.leftDp + ContextMenuPositioner.MENU_WIDTH_DP <= 390f - 12f)
        assertTrue(p2.topDp + ContextMenuPositioner.MENU_HEIGHT_DP <= 844f - 12f)
        assertTrue(p2.topDp < bottomRightTile.top)
    }

    @Test
    fun `every tile type yields targeted direct shortcuts`() {
        assertEquals(
            listOf("Wecker stellen", "Timer 25 Min."),
            ContextMenuPositioner.shortcutsForTile(TileData("c", TileType.CLOCK, "Uhr")),
        )
        assertEquals(
            listOf("Neuer Termin", "Wochenansicht"),
            ContextMenuPositioner.shortcutsForTile(TileData("cal", TileType.CALENDAR, "Kalender")),
        )
        assertEquals(
            listOf("Gruppe umbenennen", "Apps verwalten"),
            ContextMenuPositioner.shortcutsForTile(TileData("grp", TileType.APP_GROUP, "Favoriten")),
        )
        assertEquals(
            listOf("Neuer Eintrag", "Alle abhaken"),
            ContextMenuPositioner.shortcutsForTile(TileData("rem", TileType.REMINDERS, "Erinnerungen")),
        )
        assertEquals(
            listOf("Mix wechseln", "Warteschlange"),
            ContextMenuPositioner.shortcutsForTile(TileData("med", TileType.MEDIA, "Medien")),
        )
    }

    @Test
    fun `computeWidgetSizeBundleRanges computes responsive min and max bounds`() {
        val compact = WidgetFrameEngine.computeWidgetSizeBundleRanges(
            tileWidthDp = 174f,
            tileHeightDp = 154f,
            fixedWidthDp = 158f,
            fixedHeightDp = 148f,
        )
        assertEquals(158, compact.minWidthDp)
        assertEquals(120, compact.minHeightDp)
        assertTrue(compact.maxWidthDp >= compact.minWidthDp)
        assertTrue(compact.maxHeightDp >= compact.minHeightDp)

        val large = WidgetFrameEngine.computeWidgetSizeBundleRanges(
            tileWidthDp = 360f,
            tileHeightDp = 280f,
            fixedWidthDp = 158f,
            fixedHeightDp = 148f,
        )
        assertEquals(158, large.minWidthDp)
        assertEquals(148, large.minHeightDp)
        assertEquals(344, large.maxWidthDp)
        assertEquals(246, large.maxHeightDp)
    }
}
