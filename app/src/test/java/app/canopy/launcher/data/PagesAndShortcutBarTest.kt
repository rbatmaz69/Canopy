package app.canopy.launcher.data

import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.RectFBounds
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.layout.TogglePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagesAndShortcutBarTest {
    @Test
    fun `initial pages include Heute, Start, Medien, Apps and anchored Alle Apps`() {
        val pages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL)
        assertEquals(5, pages.size)
        assertEquals(listOf("Heute", "Start", "Medien", "Apps", "Alle Apps"), pages.map { it.title })
        assertEquals(PageKind.ALL_APPS, pages.last().kind)
        assertTrue(pages.dropLast(1).all { it.kind == PageKind.TILES })
    }

    @Test
    fun `each regular page receives a distinct contextual 4th shortcut preset`() {
        val pages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL)
        val presets = pages.dropLast(1).map { page ->
            TilingEngine.collectLeaves(page.root)
                .first { it.tile.type == TileType.QUICK_TOGGLES }
                .tile
                .togglePreset
        }
        assertEquals(
            listOf(TogglePreset.TODAY, TogglePreset.SYSTEM, TogglePreset.MEDIA, TogglePreset.TOOLS),
            presets,
        )
        assertEquals(4, presets.toSet().size)
    }

    @Test
    fun `syncShortcutBars switches cleanly and idempotently between ALL, START_ONLY and OFF`() {
        val allMode = MockCatalogData.createInitialPages(ShortcutBarMode.ALL)
        val startOnly = MockCatalogData.syncShortcutBars(allMode, ShortcutBarMode.START_ONLY)
        val offMode = MockCatalogData.syncShortcutBars(startOnly, ShortcutBarMode.OFF)
        val backToAll = MockCatalogData.syncShortcutBars(offMode, ShortcutBarMode.ALL)

        fun toggleCounts(pages: List<app.canopy.launcher.layout.LauncherPage>): List<Int> =
            pages.dropLast(1).map { pg ->
                TilingEngine.collectLeaves(pg.root).count { it.tile.type == TileType.QUICK_TOGGLES }
            }

        assertEquals(listOf(1, 1, 1, 1), toggleCounts(allMode))
        assertEquals(listOf(0, 1, 0, 0), toggleCounts(startOnly))
        assertEquals(listOf(0, 0, 0, 0), toggleCounts(offMode))
        assertEquals(listOf(1, 1, 1, 1), toggleCounts(backToAll))
    }

    @Test
    fun `all tiles across all 5 standard pages satisfy minimum 56dp dimensions on phone viewport`() {
        val bounds = RectFBounds(x = 8f, y = 4f, width = 374f, height = 740f)
        for (mode in ShortcutBarMode.entries) {
            val pages = MockCatalogData.createInitialPages(mode)
            for (page in pages) {
                val layout = TilingEngine.computeLayout(page.root, bounds, TilingEngine.GAP_USE_DP)
                for (leaf in layout.leaves) {
                    assertTrue(
                        "Leaf ${leaf.id} on ${page.id} ($mode) too narrow: ${leaf.rect.width}",
                        leaf.rect.width >= TilingEngine.MIN_TILE_SIZE,
                    )
                    assertTrue(
                        "Leaf ${leaf.id} on ${page.id} ($mode) too short: ${leaf.rect.height}",
                        leaf.rect.height >= TilingEngine.MIN_TILE_SIZE,
                    )
                }
            }
        }
    }
}
