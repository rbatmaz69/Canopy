package app.canopy.launcher.ui.tiles

import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.overlays.AppGroupEditorActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BentoGroupLayoutTest {
    @Test
    fun `computes symmetric 4x2, 2x2, 3x2 and 5x2 bento grids with scaled icons`() {
        val fav8 = BentoGridPlanner.computeSpec(widthDp = 360f, heightDp = 210f, appCount = 8, tier = TileSizeTier.L)
        assertFalse(fav8.isCompactVerticalList)
        assertEquals(4, fav8.cols)
        assertEquals(2, fav8.rows)
        assertFalse(fav8.needsOverflow)
        assertEquals(8, fav8.visibleCount)
        assertTrue(fav8.iconSizeDp in 36f..60f)

        val med4 = BentoGridPlanner.computeSpec(widthDp = 175f, heightDp = 180f, appCount = 4, tier = TileSizeTier.M)
        assertEquals(2, med4.cols)
        assertEquals(2, med4.rows)
        assertFalse(med4.needsOverflow)

        val compact8 = BentoGridPlanner.computeSpec(widthDp = 210f, heightDp = 190f, appCount = 8, tier = TileSizeTier.M)
        assertEquals(3, compact8.cols)
        assertEquals(2, compact8.rows)
        assertTrue(compact8.needsOverflow)
        assertEquals(5, compact8.visibleCount)
        assertEquals(3, compact8.hiddenCount)

        val werk14 = BentoGridPlanner.computeSpec(widthDp = 360f, heightDp = 210f, appCount = 14, tier = TileSizeTier.L)
        assertEquals(5, werk14.cols)
        assertEquals(2, werk14.rows)
        assertTrue(werk14.needsOverflow)
        assertEquals(9, werk14.visibleCount)
        assertEquals(5, werk14.hiddenCount)
    }

    @Test
    fun `uses compact vertical list for small 2-3 app groups and collapses XS to folder`() {
        val lesen = BentoGridPlanner.computeSpec(widthDp = 170f, heightDp = 170f, appCount = 3, tier = TileSizeTier.M)
        assertTrue(lesen.isCompactVerticalList)
        assertFalse(lesen.needsOverflow)

        val xs = BentoGridPlanner.computeSpec(widthDp = 70f, heightDp = 70f, appCount = 8, tier = TileSizeTier.XS)
        assertEquals(0, xs.visibleCount)
        assertEquals(8, xs.hiddenCount)
        assertTrue(xs.needsOverflow)
    }

    @Test
    fun `live group editor renames title and toggles apps while keeping at least 1 app`() {
        val group = TileData(
            id = "g1",
            type = TileType.APP_GROUP,
            title = "Favoriten",
            appIds = listOf("app-telefon", "app-kamera"),
        )
        val renamed = AppGroupEditorActions.renameGroup(group, "  Studio Apps  ")
        assertEquals("Studio Apps", renamed.title)
        assertEquals("Studio Apps", AppGroupEditorActions.renameGroup(renamed, "   ").title)

        val added = AppGroupEditorActions.toggleAppInGroup(renamed.appIds!!, "app-browser")
        assertEquals(listOf("app-telefon", "app-kamera", "app-browser"), added)

        val removedOne = AppGroupEditorActions.toggleAppInGroup(listOf("app-telefon", "app-kamera"), "app-telefon")
        assertEquals(listOf("app-kamera"), removedOne)

        // Cannot remove last remaining app
        val keptLast = AppGroupEditorActions.toggleAppInGroup(removedOne, "app-kamera")
        assertEquals(listOf("app-kamera"), keptLast)
    }
}
