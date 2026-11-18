package app.canopy.launcher.system

import app.canopy.launcher.layout.AppReferencePruner
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.LeafNode
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.SplitDirection
import app.canopy.launcher.layout.SplitNode
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.ui.navigation.WindowInsetsAndWallpaperMath
import app.canopy.launcher.ui.overlays.CatalogAndPageManager
import app.canopy.launcher.ui.tiles.AppCategory
import app.canopy.launcher.ui.tiles.AppIdentity
import app.canopy.launcher.ui.tiles.AppProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIdentityAndProfilesTest {
    @Test
    fun `app ids stay backward compatible and encode activity and profile`() {
        assertEquals("pkg:com.a", AppIdentity.appId("com.a", "com.a.Main", isMainActivity = true, userSerial = 0))
        assertEquals("pkg:com.a/com.a.Second", AppIdentity.appId("com.a", "com.a.Second", isMainActivity = false, userSerial = 0))
        assertEquals("pkg:com.a@10", AppIdentity.appId("com.a", "com.a.Main", isMainActivity = true, userSerial = 10))
        assertEquals("com.a", AppIdentity.packageOf("pkg:com.a/com.a.Second@10"))
        assertEquals(10L, AppIdentity.userSerialOf("pkg:com.a@10"))
        assertEquals(0L, AppIdentity.userSerialOf("pkg:com.a"))
        assertEquals("pkg:com.a@10", AppIdentity.badgeKey("com.a", 10))
    }

    @Test
    fun `every launcher activity and every profile becomes its own entry`() {
        val merged = InstalledAppsMapper.mergeWithCatalog(
            catalog = emptyList(),
            installed = listOf(
                InstalledAppEntry("com.google.android.apps.docs", "Drive", "Drive", isMainActivity = true),
                InstalledAppEntry("com.google.android.apps.docs", "Docs", "Docs", isMainActivity = false),
                InstalledAppEntry("com.whatsapp", "Main", "WhatsApp"),
                InstalledAppEntry("com.whatsapp", "Main", "WhatsApp", userSerial = 11, profile = AppProfile.WORK, isPaused = true),
            ),
        )
        assertEquals(4, merged.size)
        assertTrue(merged.any { it.id == "pkg:com.google.android.apps.docs" && it.name == "Drive" })
        assertTrue(merged.any { it.id == "pkg:com.google.android.apps.docs/Docs" })
        val work = merged.first { it.id == "pkg:com.whatsapp@11" }
        assertEquals(AppProfile.WORK, work.profile)
        assertTrue(work.isPaused)
        assertEquals("pkg:com.whatsapp@11", work.badgeKey)
    }

    private fun page(root: app.canopy.launcher.layout.TreeNode) = LauncherPage("p", "Start", PageKind.TILES, root)

    @Test
    fun `uninstalling an app removes its tiles and widgets and prunes groups`() {
        val root = SplitNode(
            "s1", SplitDirection.HORIZONTAL, 0.5f,
            LeafNode("l1", TileData("t1", TileType.SINGLE_APP, "Spotify", appId = "pkg:com.spotify")),
            SplitNode(
                "s2", SplitDirection.VERTICAL, 0.5f,
                LeafNode("l2", TileData("t2", TileType.THIRD_PARTY_WIDGET, "Spotify", appId = "pkg:com.spotify", androidAppWidgetId = 42)),
                LeafNode("l3", TileData("t3", TileType.APP_GROUP, "Medien", appIds = listOf("pkg:com.spotify", "pkg:com.netflix"))),
            ),
        )
        val result = AppReferencePruner.prune(listOf(page(root))) { AppReferencePruner.matchesPackage(it, "com.spotify", 0) }
        val leaves = TilingEngine.collectLeaves(result.pages.first().root)
        assertEquals(1, leaves.size)
        assertEquals(listOf("pkg:com.netflix"), leaves.first().tile.appIds)
        assertEquals(2, result.removedTiles)
        assertEquals(listOf(42), result.releasedWidgetIds)
    }

    @Test
    fun `work copies are not pruned when only the personal app is removed`() {
        assertFalse(AppReferencePruner.matchesPackage("pkg:com.spotify@11", "com.spotify", 0))
        assertTrue(AppReferencePruner.matchesPackage("pkg:com.spotify/Other", "com.spotify", 0))
    }

    @Test
    fun `template groups are filled by category without random apps`() {
        val apps = InstalledAppsMapper.mergeWithCatalog(
            emptyList(),
            listOf(
                InstalledAppEntry("com.game.chess", "M", "Schach", androidCategory = 0),
                InstalledAppEntry("com.game.race", "M", "Rennen", androidCategory = 0),
                InstalledAppEntry("com.tool.notes", "M", "Notizblock", androidCategory = 7),
            ),
        )
        val pages = listOf(page(LeafNode("g", TileData("tg", TileType.APP_GROUP, "Spiele", appIds = listOf("app-x", "app-y")))))
        val remapped = InstalledAppsMapper.resolveRealAppIdsForPages(apps, pages)
        val ids = TilingEngine.collectLeaves(remapped.first().root).first().tile.appIds.orEmpty()
        assertEquals(setOf("pkg:com.game.chess", "pkg:com.game.race"), ids.toSet())

        val templates = CatalogAndPageManager.buildGroupTemplates(apps)
        assertEquals(1, templates.size)
        assertEquals(AppCategory.SPIELE.label, templates.first().title)
    }

    @Test
    fun `icon and wallpaper geometry`() {
        assertEquals(-24 to 144, AdaptiveIconGeometry.layerBounds(96))
        assertEquals(96, AdaptiveIconGeometry.bucket(90))
        assertEquals(0.5f, WindowInsetsAndWallpaperMath.wallpaperOffsetFraction(0f, 1080f, 1), 0.001f)
        assertEquals(0.5f, WindowInsetsAndWallpaperMath.wallpaperOffsetFraction(-2160f, 1080f, 5), 0.001f)
        assertEquals(1f, WindowInsetsAndWallpaperMath.wallpaperOffsetFraction(-9999f, 1080f, 5), 0.001f)
    }
}
