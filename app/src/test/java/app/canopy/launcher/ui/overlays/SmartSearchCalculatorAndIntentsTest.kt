package app.canopy.launcher.ui.overlays

import android.provider.Settings
import app.canopy.launcher.ui.tiles.SettingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartSearchCalculatorAndIntentsTest {
    @Test
    fun `evaluateMathExpression computes arithmetic brackets decimals and percentages accurately`() {
        assertEquals("2755", UniversalSearchEngine.evaluateMathExpression("145 * 19"))
        assertEquals("80", UniversalSearchEngine.evaluateMathExpression("(240 + 80) / 4"))
        assertEquals("47,5", UniversalSearchEngine.evaluateMathExpression("19% von 250"))
        assertEquals("15", UniversalSearchEngine.evaluateMathExpression("7,5 * 2"))
        assertEquals("-15", UniversalSearchEngine.evaluateMathExpression("-5 * 3"))

        assertNull(UniversalSearchEngine.evaluateMathExpression("Kamera"))
        assertNull(UniversalSearchEngine.evaluateMathExpression("42"))
        assertNull(UniversalSearchEngine.evaluateMathExpression("10 / 0"))
    }

    @Test
    fun `buildContactDialUri extracts valid phone numbers and ignores non-phone statuses`() {
        assertEquals("tel:+49170123456", UniversalSearchEngine.buildContactDialUri("+49 170 123 456"))
        assertEquals("tel:0151987654", UniversalSearchEngine.buildContactDialUri("0151-987654"))
        assertNull(UniversalSearchEngine.buildContactDialUri("Design Team Lead"))
        assertNull(UniversalSearchEngine.buildContactDialUri(""))
    }

    @Test
    fun `resolveSettingIntentAction returns specific Android settings action or fallback`() {
        val wifi = SettingItem("s-wifi", "WLAN", "Studio_5G", "Wifi", 200f, Settings.ACTION_WIFI_SETTINGS)
        assertEquals(Settings.ACTION_WIFI_SETTINGS, UniversalSearchEngine.resolveSettingIntentAction(wifi))

        val blank = SettingItem("s-other", "System", "Info", "Settings", 210f, "")
        assertEquals(Settings.ACTION_SETTINGS, UniversalSearchEngine.resolveSettingIntentAction(blank))
    }
}
