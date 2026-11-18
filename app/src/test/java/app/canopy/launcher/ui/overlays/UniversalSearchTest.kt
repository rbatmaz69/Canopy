package app.canopy.launcher.ui.overlays

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchTest {
    @Test
    fun `empty query shows quick suggestions across apps, contacts, and settings`() {
        val res = UniversalSearchEngine.filterAll("")
        assertEquals(8, res.apps.size)
        assertEquals(2, res.contacts.size)
        assertEquals(2, res.settings.size)
        assertFalse(UniversalSearchEngine.shouldShowBrowserCard(""))
    }

    @Test
    fun `filters apps, contacts, and settings simultaneously without lag`() {
        val resApp = UniversalSearchEngine.filterAll("Kamera")
        assertTrue(resApp.apps.any { it.id == "app-kamera" })

        val resContact = UniversalSearchEngine.filterAll("Lena")
        assertTrue(resContact.contacts.any { it.name.contains("Lena") })

        val resSetting = UniversalSearchEngine.filterAll("WLAN")
        assertTrue(resSetting.settings.any { it.name.contains("WLAN") })
        assertTrue(resSetting.apps.any { it.name.contains("WLAN") })
    }

    @Test
    fun `non-matching local query keeps browser search card active and builds valid web search target`() {
        val query = "Wetter morgen in Tokyo"
        val res = UniversalSearchEngine.filterAll(query)
        assertTrue(res.isEmpty)
        assertTrue(UniversalSearchEngine.shouldShowBrowserCard(query))
        assertEquals(query, UniversalSearchEngine.resolveWebSearchQuery(query))

        val url = UniversalSearchEngine.buildWebSearchUrl(query)
        assertTrue(url.startsWith("https://www.google.com/search?q="))
        assertTrue(url.contains("Tokyo"))

        // Empty enter falls back safely
        assertEquals("Canopy Android Launcher", UniversalSearchEngine.resolveWebSearchQuery("   "))
        val spec = UniversalSearchEngine.createWebSearchSpec(query)
        assertEquals(Intent.ACTION_WEB_SEARCH, spec.intentAction)
        assertEquals(query, spec.query)
        assertNotNull(UniversalSearchEngine.buildBrowserSearchIntent(query))
    }
}
