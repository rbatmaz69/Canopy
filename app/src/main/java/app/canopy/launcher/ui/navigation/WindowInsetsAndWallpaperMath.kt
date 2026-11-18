package app.canopy.launcher.ui.navigation

import app.canopy.launcher.layout.RectFBounds
import kotlin.math.max

data class ViewportInsetsSpec(
    val statusBarTopDp: Float = 0f,
    val navBarBottomDp: Float = 0f,
    val cutoutLeftDp: Float = 0f,
    val cutoutRightDp: Float = 0f,
)

data class ComputedViewportMetrics(
    val statusBarHeightDp: Float,
    val bottomDockHeightDp: Float,
    val tilingAreaHeightDp: Float,
    val pageBoundsDp: RectFBounds,
)

/**
 * Pure Kotlin viewport math combining Canopy's edge-padding design tokens with
 * Android WindowInsets (status bar, gesture navigation bar, and display cutouts).
 */
object WindowInsetsAndWallpaperMath {
    const val BASE_STATUS_BAR_DP = 0f
    const val BASE_BOTTOM_DOCK_DP = 44f

    fun computeViewportMetrics(
        screenWidthDp: Float,
        screenHeightDp: Float,
        edgePaddingDp: Float,
        insets: ViewportInsetsSpec = ViewportInsetsSpec(),
    ): ComputedViewportMetrics {
        val statusBarHeightDp = insets.statusBarTopDp.coerceIn(0f, 72f)
        val navBarPad = insets.navBarBottomDp.coerceIn(0f, 64f)
        val bottomDockHeightDp = BASE_BOTTOM_DOCK_DP + navBarPad
        val horizontalSafePad = edgePaddingDp + max(insets.cutoutLeftDp, insets.cutoutRightDp).coerceAtMost(36f)
        val tilingAreaHeightDp = (screenHeightDp - statusBarHeightDp - bottomDockHeightDp).coerceAtLeast(320f)
        val pageBoundsDp = RectFBounds(
            x = horizontalSafePad,
            y = 4f,
            width = (screenWidthDp - horizontalSafePad * 2f).coerceAtLeast(200f),
            height = tilingAreaHeightDp - 8f,
        )
        return ComputedViewportMetrics(
            statusBarHeightDp = statusBarHeightDp,
            bottomDockHeightDp = bottomDockHeightDp,
            tilingAreaHeightDp = tilingAreaHeightDp,
            pageBoundsDp = pageBoundsDp,
        )
    }

    fun shouldRenderProceduralWallpaper(useSystemWallpaper: Boolean): Boolean = !useSystemWallpaper

    /** Horizontal wallpaper offset (0..1) for the current pager offset. */
    fun wallpaperOffsetFraction(pageOffsetPx: Float, pageWidthPx: Float, pageCount: Int): Float {
        if (pageCount <= 1 || pageWidthPx <= 1f) return 0.5f
        val progress = -pageOffsetPx / (pageWidthPx * (pageCount - 1))
        return progress.coerceIn(0f, 1f)
    }
}
