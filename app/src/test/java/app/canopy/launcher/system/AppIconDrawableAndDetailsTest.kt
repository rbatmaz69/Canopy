package app.canopy.launcher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconDrawableAndDetailsTest {
    @Test
    fun `extractPackageFromAppId resolves third-party packages and ignores curated keys`() {
        assertTrue(InstalledAppsMapper.isThirdPartyAppId("pkg:org.mozilla.fennec"))
        assertEquals("org.mozilla.fennec", InstalledAppsMapper.extractPackageFromAppId("pkg:org.mozilla.fennec"))

        assertFalse(InstalledAppsMapper.isThirdPartyAppId("app-telefon"))
        assertNull(InstalledAppsMapper.extractPackageFromAppId("app-telefon"))
        assertNull(InstalledAppsMapper.extractPackageFromAppId("pkg:   "))
        assertNull(InstalledAppsMapper.extractPackageFromAppId(null))
    }

    @Test
    fun `buildAppDetailsSettingsUri produces canonical Android settings package URI`() {
        assertEquals(
            "package:com.spotify.music",
            InstalledAppsMapper.buildAppDetailsSettingsUri("  com.spotify.music  "),
        )
    }
}
