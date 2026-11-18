package app.canopy.launcher.system

import android.content.Intent
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.data.WallpaperPresetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRoleAndBackupTest {
    @Test
    fun `isHomeIntent detects ACTION_MAIN with CATEGORY_HOME`() {
        assertTrue(LayoutBackupEngine.isHomeIntent(Intent.ACTION_MAIN, setOf(Intent.CATEGORY_HOME)))
        assertFalse(LayoutBackupEngine.isHomeIntent(Intent.ACTION_MAIN, setOf(Intent.CATEGORY_LAUNCHER)))
        assertFalse(LayoutBackupEngine.isHomeIntent(Intent.ACTION_VIEW, setOf(Intent.CATEGORY_HOME)))
        assertFalse(LayoutBackupEngine.isHomeIntent(null, null))
    }

    @Test
    fun `exportBackupJson and importBackupJson round-trip custom state and reject invalid payloads`() {
        val state = PersistedLauncherState(
            pages = MockCatalogData.createInitialPages(ShortcutBarMode.START_ONLY),
            homePageId = "page-start",
            themeId = ThemeVariantId.DARK_BOLD,
            colorMode = ColorMode.DARK,
            wallpaperId = WallpaperPresetId.COBALT,
            tonalIcons = false,
            shortcutBarMode = ShortcutBarMode.START_ONLY,
            useSystemWallpaper = true,
        )

        val json = LayoutBackupEngine.exportBackupJson(state)
        val restored = LayoutBackupEngine.importBackupJson(json)
        assertTrue(restored.isSuccess)
        assertEquals(ThemeVariantId.DARK_BOLD, restored.getOrNull()?.themeId)
        assertEquals(WallpaperPresetId.COBALT, restored.getOrNull()?.wallpaperId)
        assertEquals(true, restored.getOrNull()?.useSystemWallpaper)
        assertEquals(state.pages.size, restored.getOrNull()?.pages?.size)

        assertTrue(LayoutBackupEngine.importBackupJson("").isFailure)
        assertTrue(LayoutBackupEngine.importBackupJson("{\"pages\": []}").isFailure)
        assertTrue(LayoutBackupEngine.importBackupJson("not-json").isFailure)
    }
}
