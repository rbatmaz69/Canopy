package app.canopy.launcher.ui.tiles

import app.canopy.launcher.layout.DropZone
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.LeafNode
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.RectFBounds
import app.canopy.launcher.layout.SplitDirection
import app.canopy.launcher.layout.SplitNode
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.ui.overlays.AppGroupEditorActions
import app.canopy.launcher.ui.pages.ActiveTileDrag
import app.canopy.launcher.ui.pages.DragSourceKind
import app.canopy.launcher.ui.pages.DropPreviewResult
import app.canopy.launcher.ui.pages.TileDragEngine
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

    @Test
    fun `moveAppInGroup reorders apps and applyDropMutation merges SINGLE_APP into APP_GROUP center`() {
        val reordered = AppGroupEditorActions.moveAppInGroup(
            listOf("app-telefon", "app-kamera", "app-browser"),
            "app-browser",
            -1,
        )
        assertEquals(listOf("app-telefon", "app-browser", "app-kamera"), reordered)

        val groupTile = TileData(
            id = "g-fav",
            type = TileType.APP_GROUP,
            title = "Favoriten",
            appIds = listOf("app-telefon", "app-kamera"),
            accentHue = 215f,
        )
        val tinted = AppGroupEditorActions.updateGroupAccentHue(groupTile, 330f)
        assertEquals(330f, tinted.accentHue)

        val singleTile = TileData(
            id = "t-single",
            type = TileType.SINGLE_APP,
            title = "Netz",
            appId = "app-browser",
        )
        val page = LauncherPage(
            id = "page-1",
            title = "Start",
            kind = PageKind.TILES,
            root = SplitNode(
                id = "s-root",
                direction = SplitDirection.VERTICAL,
                ratio = 0.5f,
                first = LeafNode("leaf-group", groupTile),
                second = LeafNode("leaf-single", singleTile),
            ),
        )
        val groupLeafLayout = TilingEngine.computeLayout(
            page.root,
            RectFBounds(0f, 0f, 360f, 400f),
            8f,
        ).leaves.first { it.id == "leaf-group" }
        val outcome = TileDragEngine.applyDropMutation(
            pages = listOf(page),
            drag = ActiveTileDrag(
                sourceKind = DragSourceKind.PAGE_TILE,
                sourcePageId = "page-1",
                sourceLeafId = "leaf-single",
                tile = singleTile,
                widthDp = 160f,
                heightDp = 140f,
                pointerXRootPx = 100f,
                pointerYRootPx = 100f,
            ),
            dropPreview = DropPreviewResult.LeafTarget(
                pageId = "page-1",
                leaf = groupLeafLayout,
                zone = DropZone.CENTER,
            ),
            shouldRemove = false,
        )
        val remainingLeaves = TilingEngine.collectLeaves(outcome.pages.first().root)
        assertEquals(1, remainingLeaves.size)
        assertEquals(
            listOf("app-telefon", "app-kamera", "app-browser"),
            remainingLeaves.first().tile.appIds,
        )
    }
}
