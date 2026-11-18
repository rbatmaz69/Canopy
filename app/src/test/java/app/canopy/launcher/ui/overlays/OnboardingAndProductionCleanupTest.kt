package app.canopy.launcher.ui.overlays

import android.Manifest
import android.os.Build
import app.canopy.launcher.data.LauncherPersistenceCodec
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingAndProductionCleanupTest {
    @Test
    fun `requiredRuntimePermissions includes ACTIVITY_RECOGNITION on Android Q and above`() {
        val preQ = OnboardingPermissionsHelper.requiredRuntimePermissions(Build.VERSION_CODES.P)
        assertEquals(listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.READ_CONTACTS), preQ)

        val api35 = OnboardingPermissionsHelper.requiredRuntimePermissions(35)
        assertEquals(
            listOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ),
            api35,
        )
    }

    @Test
    fun `OnboardingStatusSnapshot reports allCoreGranted only when every check passes`() {
        val incomplete = OnboardingStatusSnapshot(
            isDefaultHome = true,
            notificationAccess = false,
            calendarGranted = true,
            contactsGranted = true,
            activityGranted = true,
        )
        assertFalse(incomplete.allCoreGranted)

        val complete = incomplete.copy(notificationAccess = true)
        assertTrue(complete.allCoreGranted)
    }

    @Test
    fun `PersistedLauncherState defaults onboardingCompleted to false and round-trips true`() {
        val pages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL)
        val initial = PersistedLauncherState(pages = pages)
        assertFalse(initial.onboardingCompleted)
        assertTrue(initial.useSystemWallpaper)

        val completed = initial.copy(onboardingCompleted = true)
        val decoded = LauncherPersistenceCodec.decode(LauncherPersistenceCodec.encode(completed))
        assertNotNull(decoded)
        assertTrue(decoded!!.onboardingCompleted)

        // Initial pages no longer contain unbound demo THIRD_PARTY_WIDGET tiles
        val leaves = pages.flatMap { TilingEngine.collectLeaves(it.root) }
        assertTrue(leaves.none { it.tile.type == TileType.THIRD_PARTY_WIDGET && it.tile.androidAppWidgetId == null })
    }
}
