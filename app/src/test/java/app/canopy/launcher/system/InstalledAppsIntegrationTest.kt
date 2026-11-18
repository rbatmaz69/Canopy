package app.canopy.launcher.system

import androidx.compose.ui.geometry.Rect
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.overlays.ContextMenuPositioner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledAppsIntegrationTest {
    @Test
    fun `mergeWithCatalog binds device packages onto canonical roles and adds third-party apps`() {
        val installed = listOf(
            InstalledAppEntry("com.google.android.dialer", "com.android.dialer.DialtactsActivity", "Telefon", androidCategory = -1),
            InstalledAppEntry("com.android.chrome", "com.google.android.apps.chrome.Main", "Chrome", androidCategory = 7),
            InstalledAppEntry("org.mozilla.fennec", "org.mozilla.gecko.LauncherActivity", "Firefox Focus", androidCategory = 7, isNewInstall = true),
            InstalledAppEntry("app.canopy.launcher", "app.canopy.launcher.CanopyLauncherActivity", "Canopy"),
        )

        val merged = InstalledAppsMapper.mergeWithCatalog(
            catalog = MockCatalogData.APPS_CATALOG,
            installed = installed,
            selfPackage = "app.canopy.launcher",
        )

        val phone = merged.first { it.id == "app-telefon" }
        assertEquals("com.google.android.dialer", phone.packageName)
        assertEquals("com.android.dialer.DialtactsActivity", phone.componentActivityName)

        val browser = merged.first { it.id == "app-browser" }
        assertEquals("com.android.chrome", browser.packageName)

        val firefox = merged.firstOrNull { it.id == "pkg:org.mozilla.fennec" }
        assertNotNull(firefox)
        assertTrue(firefox!!.isNew)
        assertEquals("Compass", firefox.iconKey)

        // Canopy itself must never appear inside the launcher catalog
        assertFalse(merged.any { it.packageName == "app.canopy.launcher" })
    }

    @Test
    fun `computeClipRevealSpec scales dp bounds and clamps inside screen dimensions`() {
        val spec = InstalledAppsMapper.computeClipRevealSpec(
            sourceRectDp = Rect(left = 350f, top = 790f, right = 430f, bottom = 860f),
            density = 3f,
            screenWidthPx = 1080,
            screenHeightPx = 2400,
        )
        assertNotNull(spec)
        assertEquals(1050, spec!!.leftPx)
        assertEquals(2370, spec.topPx)
        assertEquals(30, spec.widthPx)
        assertEquals(30, spec.heightPx)

        assertNull(InstalledAppsMapper.computeClipRevealSpec(null, 3f, 1080, 2400))
    }

    @Test
    fun `mergeContextShortcuts prepends native Android shortcuts ahead of tile actions`() {
        val singleAppTile = TileData(
            id = "tile-msg",
            type = TileType.SINGLE_APP,
            title = "Nachrichten",
            appId = "app-nachrichten",
        )
        val nativeShortcuts = listOf(
            NativeShortcutEntry("sc-1", "com.google.android.apps.messaging", "Neuer Chat"),
            NativeShortcutEntry("sc-2", "com.google.android.apps.messaging", "Mara Weber"),
            NativeShortcutEntry("sc-3", "com.google.android.apps.messaging", "Extra ignoriert"),
        )

        val combined = ContextMenuPositioner.shortcutsForTile(singleAppTile, nativeShortcuts)
        assertEquals("↗ Neuer Chat", combined[0])
        assertEquals("↗ Mara Weber", combined[1])
        assertFalse(combined.any { it.contains("Extra ignoriert") })
        assertTrue(combined.contains("Schnellaktion"))
        assertTrue(combined.contains("App-Info"))
    }
}
