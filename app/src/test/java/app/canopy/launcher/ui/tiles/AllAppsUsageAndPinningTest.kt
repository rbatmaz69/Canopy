package app.canopy.launcher.ui.tiles

import app.canopy.launcher.data.LauncherPersistenceCodec
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.ShortcutBarMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllAppsUsageAndPinningTest {
    @Test
    fun `incrementUsageCount and applyUsageBoost promote frequently launched apps in USAGE sort`() {
        var counts = emptyMap<String, Int>()
        repeat(12) {
            counts = AllAppsCatalogSorter.incrementUsageCount(counts, "app-pflanzen")
        }
        assertEquals(12, counts["app-pflanzen"])

        val boosted = AllAppsCatalogSorter.applyUsageBoost(MockCatalogData.APPS_CATALOG, counts)
        val sections = AllAppsCatalogSorter.groupApps(boosted, AllAppsSortMode.USAGE)
        val sortedIds = sections.first().apps.map { it.id }

        // 'app-pflanzen' started at usageScore=44; +60 puts it at 104, ahead of default apps (max 98)
        assertEquals("app-pflanzen", sortedIds.first())
    }

    @Test
    fun `buildUninstallUri creates package URI for third-party apps and skips self`() {
        assertEquals("package:com.example.tool", AllAppsCatalogSorter.buildUninstallUri("com.example.tool"))
        assertNull(AllAppsCatalogSorter.buildUninstallUri("app.canopy.launcher"))
        assertNull(AllAppsCatalogSorter.buildUninstallUri(" "))
        assertNull(AllAppsCatalogSorter.buildUninstallUri(null))
    }

    @Test
    fun `PersistedLauncherState preserves appUsageCounts across JSON round-trip`() {
        val state = PersistedLauncherState(
            pages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL),
            appUsageCounts = mapOf("app-kamera" to 7, "pkg:org.mozilla.fennec" to 3),
        )
        val decoded = LauncherPersistenceCodec.decode(LauncherPersistenceCodec.encode(state))
        assertTrue(decoded != null)
        assertEquals(7, decoded!!.appUsageCounts["app-kamera"])
        assertEquals(3, decoded.appUsageCounts["pkg:org.mozilla.fennec"])
    }
}
