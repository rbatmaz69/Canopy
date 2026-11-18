package app.canopy.launcher.system

import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.tiles.CalendarEvent
import app.canopy.launcher.ui.tiles.ContactItem
import app.canopy.launcher.ui.tiles.WidgetFrameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemDataAndWidgetBridgeTest {
    @Test
    fun `mapMediaSnapshotToTrack computes progress ratio and stable package hue`() {
        val snapshot = LiveMediaSessionSnapshot(
            packageName = "com.spotify.music",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up",
            durationMs = 240_000L,
            positionMs = 60_000L,
            isPlaying = true,
        )

        val (track, progress) = SystemDataAndWidgetEngine.mapMediaSnapshotToTrack(snapshot)
        assertEquals("Midnight City", track.title)
        assertEquals("M83", track.artist)
        assertEquals(240, track.durationSec)
        assertEquals(0.25f, progress, 0.001f)
        assertEquals(InstalledAppsMapper.stableHueForPackage("com.spotify.music"), track.accentHue, 0.001f)
    }

    @Test
    fun `mergeCalendarEvents and mergeContacts prioritize device data and fallback cleanly`() {
        assertEquals(SystemTileDefaults.CALENDAR_EVENTS, SystemDataAndWidgetEngine.mergeCalendarEvents(emptyList()))
        assertEquals(MockCatalogData.CONTACTS_MOCK, SystemDataAndWidgetEngine.mergeContacts(emptyList()))

        val liveEvent = CalendarEvent(
            id = "live-1",
            title = "Standup",
            time = "09:00",
            endTime = "09:30",
            location = "Meet",
            startHour = 9f,
            durationHours = 0.5f,
            hue = 210f,
        )
        val mergedEvents = SystemDataAndWidgetEngine.mergeCalendarEvents(listOf(liveEvent))
        assertEquals(listOf(liveEvent), mergedEvents)

        val dupName = MockCatalogData.CONTACTS_MOCK.first().name
        val mergedContacts = SystemDataAndWidgetEngine.mergeContacts(
            listOf(
                ContactItem("os-1", dupName, "Gerätekontakt", "+49 170 111", 120f),
                ContactItem("os-2", "Zoe Becker", "Gerätekontakt", "+49 170 222", 240f),
            )
        )
        assertEquals(1, mergedContacts.count { it.name.equals(dupName, ignoreCase = true) })
        assertTrue(mergedContacts.any { it.name == "Zoe Becker" })
    }

    @Test
    fun `buildWidgetTileFromProvider clamps dimensions and binds into Passepartout rules`() {
        val spec = NativeWidgetProviderSpec(
            providerPackage = "com.example.weather",
            providerClass = "com.example.weather.ForecastWidget",
            label = "Pro Wetter",
            minWidthDp = 180f,
            minHeightDp = 420f, // above max clamp 260dp
        )

        val tile = SystemDataAndWidgetEngine.buildWidgetTileFromProvider(
            spec = spec,
            appWidgetId = 77,
            matchedAppId = "app-wetter",
        )
        assertEquals(TileType.THIRD_PARTY_WIDGET, tile.type)
        assertEquals(77, tile.androidAppWidgetId)
        assertEquals("app-wetter", tile.appId)
        assertEquals(180f, tile.widgetFixedWidth ?: 0f, 0.01f)
        assertEquals(260f, tile.widgetFixedHeight ?: 0f, 0.01f)

        assertTrue(
            WidgetFrameEngine.fitsPassepartoutWidget(
                tileWidthDp = 195f,
                tileHeightDp = 280f,
                fixedWidthDp = tile.widgetFixedWidth!!,
                fixedHeightDp = tile.widgetFixedHeight!!,
            )
        )
        assertFalse(
            WidgetFrameEngine.fitsPassepartoutWidget(
                tileWidthDp = 182f,
                tileHeightDp = 280f,
                fixedWidthDp = tile.widgetFixedWidth!!,
                fixedHeightDp = tile.widgetFixedHeight!!,
            )
        )
    }
}
