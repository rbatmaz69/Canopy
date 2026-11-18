package app.canopy.launcher.ui.navigation

import androidx.compose.ui.geometry.Rect
import app.canopy.launcher.layout.PageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HorizontalPagingAndSharedElementTest {
    @Test
    fun `edge rubberband applies 30 percent resistance at left and right limits`() {
        val minOffset = -4 * 390f // 5 pages
        assertEquals(-500f, PagingPhysics.applyEdgeRubberband(-500f, minOffset, 0f), 1e-4f)

        val leftOver = PagingPhysics.applyEdgeRubberband(100f, minOffset, 0f)
        assertEquals(30f, leftOver, 1e-4f)

        val rightOver = PagingPhysics.applyEdgeRubberband(minOffset - 100f, minOffset, 0f)
        assertEquals(minOffset - 30f, rightOver, 1e-4f)
    }

    @Test
    fun `wallpaper parallax moves strictly at 15 percent of page offset`() {
        assertEquals(0f, PagingPhysics.computeParallaxOffsetPx(0f), 1e-4f)
        assertEquals(-58.5f, PagingPhysics.computeParallaxOffsetPx(-390f), 1e-4f)
        assertEquals(-117f, PagingPhysics.computeParallaxOffsetPx(-780f), 1e-4f)
    }

    @Test
    fun `target page resolver honors velocity fling threshold and clamping`() {
        // Stationary snap near page 2
        assertEquals(2, PagingPhysics.resolveTargetPageIndex(-740f, 390f, 0f, 5))

        // Fast swipe left from page 1 -> advances to page 2 even with small drag
        assertEquals(2, PagingPhysics.resolveTargetPageIndex(-450f, 390f, -650f, 5))

        // Fast swipe right from page 2 -> returns to page 1
        assertEquals(1, PagingPhysics.resolveTargetPageIndex(-720f, 390f, 650f, 5))

        // Clamps to valid bounds
        assertEquals(0, PagingPhysics.resolveTargetPageIndex(120f, 390f, 900f, 5))
        assertEquals(4, PagingPhysics.resolveTargetPageIndex(-1800f, 390f, -900f, 5))
    }

    @Test
    fun `shared element bounds interpolate smoothly from exact sourceRect to fullscreen`() {
        val src = Rect(left = 20f, top = 120f, right = 180f, bottom = 300f)
        val start = PagingPhysics.computeSharedElementProgressBounds(src, 390f, 844f, 22f, 0f)
        assertEquals(20f, start.xPx, 1e-4f)
        assertEquals(120f, start.yPx, 1e-4f)
        assertEquals(160f, start.widthPx, 1e-4f)
        assertEquals(180f, start.heightPx, 1e-4f)
        assertEquals(22f, start.cornerRadiusDp, 1e-4f)

        val mid = PagingPhysics.computeSharedElementProgressBounds(src, 390f, 844f, 22f, 0.5f)
        assertTrue(mid.xPx in 0f..20f)
        assertTrue(mid.widthPx in 160f..390f)
        assertEquals(11f, mid.cornerRadiusDp, 1e-4f)

        val end = PagingPhysics.computeSharedElementProgressBounds(src, 390f, 844f, 22f, 1f)
        assertEquals(0f, end.xPx, 1e-4f)
        assertEquals(0f, end.yPx, 1e-4f)
        assertEquals(390f, end.widthPx, 1e-4f)
        assertEquals(844f, end.heightPx, 1e-4f)
        assertEquals(0f, end.cornerRadiusDp, 1e-4f)
    }

    @Test
    fun `resolveVerticalSwipeAction distinguishes swipe-up search swipe-down shade and guards edit or all-apps`() {
        assertEquals(
            VerticalSwipeAction.OPEN_SEARCH,
            PagingPhysics.resolveVerticalSwipeAction(-90f, 46f, isEditMode = false, pageKind = PageKind.TILES),
        )
        assertEquals(
            VerticalSwipeAction.OPEN_QUICK_SHADE,
            PagingPhysics.resolveVerticalSwipeAction(90f, 46f, isEditMode = false, pageKind = PageKind.TILES),
        )
        assertEquals(
            VerticalSwipeAction.NONE,
            PagingPhysics.resolveVerticalSwipeAction(90f, 46f, isEditMode = true, pageKind = PageKind.TILES),
        )
        assertEquals(
            VerticalSwipeAction.NONE,
            PagingPhysics.resolveVerticalSwipeAction(90f, 46f, isEditMode = false, pageKind = PageKind.ALL_APPS),
        )
        assertEquals(
            VerticalSwipeAction.NONE,
            PagingPhysics.resolveVerticalSwipeAction(20f, 46f, isEditMode = false, pageKind = PageKind.TILES),
        )
    }
}
