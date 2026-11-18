package app.canopy.launcher.ui.pages

import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.layout.DropZone
import app.canopy.launcher.layout.RectFBounds
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.motion.HapticKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditAndDragControllerTest {
    private val bounds = RectFBounds(8f, 4f, 374f, 740f)

    @Test
    fun `detects center swap and directional edge split previews while ignoring locked Alle Apps`() {
        val pages = MockCatalogData.createInitialPages(ShortcutBarMode.OFF)
        val startPage = pages[1] // Start
        val allAppsPage = pages.last() // Alle Apps
        val startLayout = TilingEngine.computeLayout(startPage.root, bounds, 12f)
        val sourceLeaf = startLayout.leaves[0] // Uhr (top-left)
        val targetLeaf = startLayout.leaves[1] // Wetter (top-right)

        val drag = ActiveTileDrag(
            sourceKind = DragSourceKind.PAGE_TILE,
            sourcePageId = startPage.id,
            sourceLeafId = sourceLeaf.id,
            tile = sourceLeaf.tile,
            widthDp = sourceLeaf.rect.width,
            heightDp = sourceLeaf.rect.height,
            pointerXRootPx = 0f,
            pointerYRootPx = 0f,
        )

        // Center of Wetter -> Swap preview
        val centerPreview = TileDragEngine.computeDropPreview(
            drag = drag,
            activePage = startPage,
            boundsDp = bounds,
            gapDp = 12f,
            pointerLocalXDp = targetLeaf.rect.x + targetLeaf.rect.width * 0.5f,
            pointerLocalYDp = targetLeaf.rect.y + targetLeaf.rect.height * 0.5f,
        )
        assertTrue(centerPreview is DropPreviewResult.LeafTarget)
        assertEquals(targetLeaf.id, (centerPreview as DropPreviewResult.LeafTarget).leaf.id)
        assertEquals(DropZone.CENTER, centerPreview.zone)

        // Left edge of Wetter -> Split LEFT preview
        val leftPreview = TileDragEngine.computeDropPreview(
            drag = drag,
            activePage = startPage,
            boundsDp = bounds,
            gapDp = 12f,
            pointerLocalXDp = targetLeaf.rect.x + 8f,
            pointerLocalYDp = targetLeaf.rect.y + targetLeaf.rect.height * 0.5f,
        ) as DropPreviewResult.LeafTarget
        assertEquals(DropZone.LEFT, leftPreview.zone)

        // Locked Alle Apps page yields null preview
        assertNull(
            TileDragEngine.computeDropPreview(drag, allAppsPage, bounds, 12f, 150f, 300f)
        )
    }

    @Test
    fun `edge hold resolves neighbor flip and blocks flipping onto locked Alle Apps`() {
        val pages = MockCatalogData.createInitialPages(ShortcutBarMode.OFF) // indices 0..3 regular, 4 all-apps
        assertNull(TileDragEngine.resolveEdgeFlipDirection(10f, 390f, 0, pages))
        assertEquals(EdgeFlipSide.LEFT, TileDragEngine.resolveEdgeFlipDirection(10f, 390f, 1, pages))
        assertEquals(EdgeFlipSide.RIGHT, TileDragEngine.resolveEdgeFlipDirection(375f, 390f, 2, pages))
        // Index 3 is "Apps" (last regular page): right edge MUST NOT flip onto index 4 ("Alle Apps")
        assertNull(TileDragEngine.resolveEdgeFlipDirection(375f, 390f, 3, pages))
    }

    @Test
    fun `applies swap, cross-page move, catalog split, and downward throw removal with 5s undo`() {
        val pages = MockCatalogData.createInitialPages(ShortcutBarMode.OFF)
        val startPage = pages[1]
        val heutePage = pages[0]
        val startLayout = TilingEngine.computeLayout(startPage.root, bounds, 12f)
        val clockLeaf = startLayout.leaves[0]
        val weatherLeaf = startLayout.leaves[1]

        val pageDrag = ActiveTileDrag(
            sourceKind = DragSourceKind.PAGE_TILE,
            sourcePageId = startPage.id,
            sourceLeafId = clockLeaf.id,
            tile = clockLeaf.tile,
            widthDp = clockLeaf.rect.width,
            heightDp = clockLeaf.rect.height,
            pointerXRootPx = 0f,
            pointerYRootPx = 0f,
        )

        // 1) Swap on same page
        val swapOutcome = TileDragEngine.applyDropMutation(
            pages = pages,
            drag = pageDrag,
            dropPreview = DropPreviewResult.LeafTarget(startPage.id, weatherLeaf, DropZone.CENTER),
            shouldRemove = false,
        )
        assertEquals(HapticKind.SNAP, swapOutcome.hapticKind)
        val swappedStart = TilingEngine.computeLayout(swapOutcome.pages[1].root, bounds, 12f)
        assertEquals(weatherLeaf.id, swappedStart.leaves[0].id)
        assertEquals(clockLeaf.id, swappedStart.leaves[1].id)

        // 2) Cross-page move from Start -> Heute
        val heuteLeaf = TilingEngine.computeLayout(heutePage.root, bounds, 12f).leaves.first()
        val crossOutcome = TileDragEngine.applyDropMutation(
            pages = pages,
            drag = pageDrag,
            dropPreview = DropPreviewResult.LeafTarget(heutePage.id, heuteLeaf, DropZone.BOTTOM),
            shouldRemove = false,
        )
        assertEquals(
            TilingEngine.collectLeaves(startPage.root).size - 1,
            TilingEngine.collectLeaves(crossOutcome.pages[1].root).size,
        )
        assertEquals(
            TilingEngine.collectLeaves(heutePage.root).size + 1,
            TilingEngine.collectLeaves(crossOutcome.pages[0].root).size,
        )

        // 3) Downward throw removal + UndoSnapshot restores exact prior pages
        val shouldThrow = TileDragEngine.shouldThrowRemove(null, pageDrag, 600f, 740f, 750f)
        assertTrue(shouldThrow)
        val removeOutcome = TileDragEngine.applyDropMutation(pages, pageDrag, null, shouldRemove = true)
        assertEquals(HapticKind.REMOVE, removeOutcome.hapticKind)
        assertNotNull(removeOutcome.undoSnapshot)
        assertEquals(clockLeaf.tile.title, removeOutcome.undoSnapshot?.removedTitle)
        assertEquals(
            TilingEngine.collectLeaves(startPage.root).size - 1,
            TilingEngine.collectLeaves(removeOutcome.pages[1].root).size,
        )
        // Undo restores original count
        assertEquals(pages, removeOutcome.undoSnapshot?.pages)

        // 4) Catalog tile insertion
        val catalogDrag = ActiveTileDrag(
            sourceKind = DragSourceKind.CATALOG_TILE,
            tile = TileData("new-note", TileType.NOTE, "Notiz"),
            widthDp = 160f,
            heightDp = 120f,
            pointerXRootPx = 0f,
            pointerYRootPx = 0f,
        )
        val catOutcome = TileDragEngine.applyDropMutation(
            pages = pages,
            drag = catalogDrag,
            dropPreview = DropPreviewResult.LeafTarget(startPage.id, weatherLeaf, DropZone.RIGHT),
            shouldRemove = false,
        )
        assertEquals(
            TilingEngine.collectLeaves(startPage.root).size + 1,
            TilingEngine.collectLeaves(catOutcome.pages[1].root).size,
        )
    }
}
