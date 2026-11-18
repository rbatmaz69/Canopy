package app.canopy.launcher.ui.navigation

import app.canopy.launcher.data.LauncherPersistenceCodec
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.ShortcutBarMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowInsetsAndWallpaperMathTest {
    @Test
    fun `computeViewportMetrics adapts to display cutouts and gesture navigation bar`() {
        val base = WindowInsetsAndWallpaperMath.computeViewportMetrics(
            screenWidthDp = 390f,
            screenHeightDp = 844f,
            edgePaddingDp = 8f,
            insets = ViewportInsetsSpec(),
        )
        assertEquals(34f, base.statusBarHeightDp, 0.01f)
        assertEquals(62f, base.bottomDockHeightDp, 0.01f)
        assertEquals(8f, base.pageBoundsDp.x, 0.01f)
        assertEquals(374f, base.pageBoundsDp.width, 0.01f)

        val notched = WindowInsetsAndWallpaperMath.computeViewportMetrics(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            edgePaddingDp = 8f,
            insets = ViewportInsetsSpec(
                statusBarTopDp = 48f,
                navBarBottomDp = 28f,
                cutoutLeftDp = 16f,
                cutoutRightDp = 0f,
            ),
        )
        assertEquals(48f, notched.statusBarHeightDp, 0.01f)
        assertEquals(68f, notched.bottomDockHeightDp, 0.01f)
        assertEquals(24f, notched.pageBoundsDp.x, 0.01f)
        assertEquals(364f, notched.pageBoundsDp.width, 0.01f)
    }

    @Test
    fun `shouldRenderProceduralWallpaper toggles off when system wallpaper is enabled`() {
        assertTrue(WindowInsetsAndWallpaperMath.shouldRenderProceduralWallpaper(useSystemWallpaper = false))
        assertFalse(WindowInsetsAndWallpaperMath.shouldRenderProceduralWallpaper(useSystemWallpaper = true))
    }

    @Test
    fun `PersistedLauncherState round-trips useSystemWallpaper flag`() {
        val state = PersistedLauncherState(
            pages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL),
            useSystemWallpaper = true,
        )
        val encoded = LauncherPersistenceCodec.encode(state)
        val decoded = LauncherPersistenceCodec.decode(encoded)
        assertEquals(true, decoded?.useSystemWallpaper)
    }
}
