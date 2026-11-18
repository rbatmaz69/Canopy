package app.canopy.launcher.ui.overlays

import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.RectFBounds
import app.canopy.launcher.layout.TemplateKind
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAndPageOverviewTest {
    @Test
    fun `inserting catalog tile splits the largest existing leaf on active page`() {
        val pages = MockCatalogData.createInitialPages(ShortcutBarMode.OFF)
        val start = pages[1]
        val initialCount = TilingEngine.collectLeaves(start.root).size
        val note = TileData("cat-note-new", TileType.NOTE, "Notiz")

        val updated = CatalogAndPageManager.insertCatalogTileIntoLargestLeaf(
            page = start,
            newTile = note,
            boundsDp = RectFBounds(8f, 4f, 374f, 740f),
            gapDp = 12f,
        )
        val leaves = TilingEngine.collectLeaves(updated.root)
        assertEquals(initialCount + 1, leaves.size)
        assertTrue(leaves.any { it.tile.id == "cat-note-new" })
    }

    @Test
    fun `all 4 catalog tabs provide rich templates and 3 layout morph templates work cleanly`() {
        assertEquals(10, MockCatalogData.CATALOG_EXTRA_TILES.size)
        assertEquals(2, CatalogAndPageManager.WIDGET_TEMPLATES.size)
        assertTrue(MockCatalogData.APPS_CATALOG.size >= 50)
        assertEquals(3, CatalogAndPageManager.GROUP_TEMPLATES.size)

        val start = MockCatalogData.createInitialPages(ShortcutBarMode.OFF)[1]
        for (tpl in TemplateKind.entries) {
            val morphed = TilingEngine.applyTemplate(start.root, tpl, MockCatalogData.CATALOG_EXTRA_TILES)
            assertTrue(TilingEngine.collectLeaves(morphed).size >= 4)
        }
    }

    @Test
    fun `page overview reorders, adds, and deletes pages while keeping Alle Apps anchored far right`() {
        val pages = MockCatalogData.createInitialPages(ShortcutBarMode.OFF)
        assertEquals(PageKind.ALL_APPS, pages.last().kind)

        // Reorder Start (idx 1) to idx 0
        val reordered = CatalogAndPageManager.reorderRegularPage(pages, 1, -1)
        assertEquals("Start", reordered[0].title)
        assertEquals("Heute", reordered[1].title)
        assertEquals(PageKind.ALL_APPS, reordered.last().kind)

        // Add new page inserted right before Alle Apps
        val (withAdded, newIdx) = CatalogAndPageManager.addNewRegularPage(reordered)
        assertEquals(6, withAdded.size)
        assertEquals(4, newIdx)
        assertEquals(PageKind.ALL_APPS, withAdded.last().kind)

        // Delete page falling back homePageId cleanly
        val newPageId = withAdded[newIdx].id
        val (afterDelete, nextHome) = CatalogAndPageManager.deleteRegularPage(withAdded, newPageId, newPageId)
        assertEquals(5, afterDelete.size)
        assertEquals(afterDelete.first().id, nextHome)
        assertEquals(PageKind.ALL_APPS, afterDelete.last().kind)
    }
}
