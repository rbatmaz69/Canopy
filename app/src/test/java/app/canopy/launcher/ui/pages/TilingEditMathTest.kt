package app.canopy.launcher.ui.pages

import app.canopy.launcher.layout.DropZone
import app.canopy.launcher.layout.SplitDirection
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TilingEditMathTest {
    @Test
    fun `detects snap crossings across fraction points without duplicate ticks`() {
        assertEquals(0.5f, TilingEditMath.crossedSnapPoint(0.48f, 0.51f, null))
        assertNull(TilingEditMath.crossedSnapPoint(0.48f, 0.51f, 0.5f))
        assertEquals(1f / 3f, TilingEditMath.crossedSnapPoint(0.36f, 0.32f, 0.5f)!!, 1e-5f)
        assertNull(TilingEditMath.crossedSnapPoint(0.40f, 0.44f, null))
    }

    @Test
    fun `formats fraction labels and ghost size-tier badges accurately`() {
        val labels = (0..4).map { TilingEditMath.fractionLabelAtIndex(it) }
        assertEquals(listOf("¼", "⅓", "½", "⅔", "¾"), labels)

        val badgeM = TilingEditMath.ghostTierBadgeText(184f, 162f)
        assertEquals("M · 184×162", badgeM)

        val badgeXs = TilingEditMath.ghostTierBadgeText(68f, 120f)
        assertEquals("XS · 68×120", badgeXs)
    }

    @Test
    fun `computes neighbor squish side from drag delta`() {
        assertEquals(1, TilingEditMath.squishSideDirection(5f))
        assertEquals(-1, TilingEditMath.squishSideDirection(-5f))
        assertEquals(0, TilingEditMath.squishSideDirection(1f))
        assertTrue(TilingEditMath.ghostTierBadgeText(260f, 78f).startsWith("TICKER"))
    }

    @Test
    fun `resolveDropZoneLabel and buildDividerStateDescription produce context-aware accessibility labels`() {
        val groupTile = TileData("g1", TileType.APP_GROUP, "Favoriten")
        val singleTile = TileData("a1", TileType.SINGLE_APP, "Telefon", appId = "app-telefon")
        val dragSingle = ActiveTileDrag(
            sourceKind = DragSourceKind.PAGE_TILE,
            tile = singleTile,
            widthDp = 120f,
            heightDp = 120f,
            pointerXRootPx = 0f,
            pointerYRootPx = 0f,
        )

        assertEquals("In Gruppe", TilingEditMath.resolveDropZoneLabel(dragSingle, groupTile, DropZone.CENTER))
        assertEquals("Tauschen", TilingEditMath.resolveDropZoneLabel(dragSingle, singleTile, DropZone.CENTER))
        assertEquals("Teilen", TilingEditMath.resolveDropZoneLabel(dragSingle, groupTile, DropZone.RIGHT))

        assertEquals(
            "Horizontale Teilung, 50 Prozent",
            TilingEditMath.buildDividerStateDescription(SplitDirection.HORIZONTAL, 0.5f),
        )
        assertEquals(
            "Vertikale Teilung, 33 Prozent",
            TilingEditMath.buildDividerStateDescription(SplitDirection.VERTICAL, 0.33f),
        )
    }
}
