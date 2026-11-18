package app.canopy.launcher.system

import app.canopy.launcher.ui.components.AppIconRegistry
import app.canopy.launcher.ui.tiles.SystemControlState
import app.canopy.launcher.ui.tiles.SystemToggleKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeAndSystemSyncTest {
    @Test
    fun `system quick-setting toggles synchronize bidirectionally`() {
        var sys = SystemControlState()
        assertTrue(sys.wifi)
        assertFalse(sys.flashlight)

        sys = NotificationBadgeStore.toggleSystemKey(sys, SystemToggleKey.WIFI)
        assertFalse(sys.wifi)

        sys = NotificationBadgeStore.toggleSystemKey(sys, SystemToggleKey.FLASHLIGHT)
        assertTrue(sys.flashlight)

        sys = NotificationBadgeStore.toggleSystemKey(sys, SystemToggleKey.DND)
        assertTrue(sys.dnd)
    }

    @Test
    fun `dismissing a notification or opening an app clears badges synchronously everywhere`() {
        val initial = NotificationBadgeStore.INITIAL_NOTIFICATIONS
        val state0 = NotificationSyncState(notifications = initial, clearedBadgeAppIds = emptySet())

        // Messages has badge 3 initially
        assertEquals(3, AppIconRegistry.effectiveBadge("app-nachrichten", 3, state0.clearedBadgeAppIds))

        // Dismiss single notification -> removes it and suppresses app badge
        val msgNotif = initial.first { it.appId == "app-nachrichten" }
        val state1 = NotificationBadgeStore.reduceDismissNotification(state0, msgNotif)
        assertFalse(state1.notifications.any { it.id == msgNotif.id })
        assertNull(AppIconRegistry.effectiveBadge("app-nachrichten", 3, state1.clearedBadgeAppIds))
        // Mail badge stays intact until opened or dismissed
        assertEquals(5, AppIconRegistry.effectiveBadge("app-mail", 5, state1.clearedBadgeAppIds))

        // Opening Mail removes its notification and clears its badge simultaneously
        val state2 = NotificationBadgeStore.reduceAppOpened(state1, "app-mail")
        assertFalse(state2.notifications.any { it.appId == "app-mail" })
        assertNull(AppIconRegistry.effectiveBadge("app-mail", 5, state2.clearedBadgeAppIds))

        // Dismiss all clears remaining notifications and all their app badges
        val state3 = NotificationBadgeStore.reduceDismissAll(state2)
        assertTrue(state3.notifications.isEmpty())
        assertTrue(setOf("app-nachrichten", "app-mail", "app-kalender").all { it in state3.clearedBadgeAppIds })
    }
}
