package app.canopy.launcher.system

import android.provider.Settings
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.ui.tiles.SystemToggleKey
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickSettingsAndNotificationDeepLinkTest {
    @Test
    fun `resolveCanonicalAppId maps messaging mail and third-party packages accurately`() {
        assertEquals(
            "app-nachrichten",
            NotificationBadgeStore.resolveCanonicalAppId("com.google.android.apps.messaging", MockCatalogData.APPS_CATALOG),
        )
        assertEquals(
            "app-mail",
            NotificationBadgeStore.resolveCanonicalAppId("com.google.android.gm", MockCatalogData.APPS_CATALOG),
        )
        assertEquals(
            "pkg:org.example.custom",
            NotificationBadgeStore.resolveCanonicalAppId("org.example.custom", MockCatalogData.APPS_CATALOG),
        )
    }

    @Test
    fun `settingsIntentForToggle and clampWindowBrightness return safe system targets`() {
        assertEquals(Settings.ACTION_WIFI_SETTINGS, NotificationBadgeStore.settingsIntentForToggle(SystemToggleKey.WIFI))
        assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, NotificationBadgeStore.settingsIntentForToggle(SystemToggleKey.BLUETOOTH))
        assertEquals(0.25f, NotificationBadgeStore.clampWindowBrightness(0.05f), 0.001f)
        assertEquals(1.0f, NotificationBadgeStore.clampWindowBrightness(1.4f), 0.001f)
        assertEquals(0.72f, NotificationBadgeStore.clampWindowBrightness(0.72f), 0.001f)
    }
}
