package app.canopy.launcher.ui.overlays

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchTest {
    @Test
    fun `empty query suggests most used apps only`() {
        val res = UniversalSearchEngine.filterAll("")
        assertEquals(8, res.apps.size)
        assertEquals(0, res.contacts.size)
        assertEquals(0, res.settings.size)
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

    private fun app(id: String, name: String, usage: Int = 50) = app.canopy.launcher.ui.tiles.AppDefinition(
        id = id, name = name, iconKey = "Sparkles", color = androidx.compose.ui.graphics.Color.Gray, hue = 0f,
        category = app.canopy.launcher.ui.tiles.AppCategory.WERKZEUGE, usageScore = usage, subtitle = "com.example.$id",
    )

    @Test
    fun `ranking prefers prefix, then word start, initials, contains and fuzzy`() {
        val apps = listOf(
            app("maps", "Google Maps"),
            app("mail", "Mail"),
            app("gmail", "Gmail"),
            app("mastodon", "Mastodon"),
            app("ham", "Hamburger Nahverkehr"),
        )
        val ranked = UniversalSearchEngine.rankApps(apps, "ma")
        assertEquals(listOf("mail", "mastodon", "maps"), ranked.take(3).map { it.id })
        assertEquals("gmail", UniversalSearchEngine.rankApps(apps, "gm").first().id)
        assertTrue(UniversalSearchEngine.rankApps(apps, "gm").any { it.id == "maps" })
        assertTrue(UniversalSearchEngine.rankApps(apps, "hnv").any { it.id == "ham" })
    }

    @Test
    fun `search ignores package names and accents`() {
        val apps = listOf(app("ueber", "Über Uns"), app("x", "Rechner"))
        assertTrue(UniversalSearchEngine.rankApps(apps, "example").isEmpty())
        assertEquals("ueber", UniversalSearchEngine.rankApps(apps, "uber").first().id)
    }

    @Test
    fun `enter opens the best app and only falls back to the web without a match`() {
        val apps = listOf(app("cam", "Kamera"))
        val hit = UniversalSearchEngine.filterAll("kam", apps = apps, contacts = emptyList(), settings = emptyList())
        assertEquals(UniversalSearchEngine.EnterAction.OpenApp("cam"), UniversalSearchEngine.resolveEnterAction("kam", hit))
        val miss = UniversalSearchEngine.filterAll("wetter tokyo", apps = apps, contacts = emptyList(), settings = emptyList())
        assertEquals(UniversalSearchEngine.EnterAction.WebSearch("wetter tokyo"), UniversalSearchEngine.resolveEnterAction("wetter tokyo", miss))
        assertEquals(UniversalSearchEngine.EnterAction.Nothing, UniversalSearchEngine.resolveEnterAction("  ", miss))
    }
}
