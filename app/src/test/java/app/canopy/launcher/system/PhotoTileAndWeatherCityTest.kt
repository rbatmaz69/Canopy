package app.canopy.launcher.system

import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.tiles.EigenTileActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoTileAndWeatherCityTest {
    @Test
    fun `applyCustomPhotoUri stores non-blank URI and clears blank values`() {
        val base = TileData(id = "t-photo", type = TileType.PHOTOS, title = "Fotos")
        val withUri = EigenTileActions.applyCustomPhotoUri(base, "content://media/external/images/media/42")
        assertEquals("content://media/external/images/media/42", withUri.customPhotoUri)

        val cleared = EigenTileActions.applyCustomPhotoUri(withUri, "   ")
        assertNull(cleared.customPhotoUri)
    }

    @Test
    fun `resolveCityPreset resolves DACH cities and falls back to Berlin`() {
        val muenchen = OpenMeteoWeatherParser.resolveCityPreset("MUENCHEN")
        assertEquals("muenchen", muenchen.id)
        assertEquals("München", muenchen.name)
        assertEquals(48.14, muenchen.latitude, 0.01)

        val wien = OpenMeteoWeatherParser.resolveCityPreset("wien")
        assertEquals("Wien", wien.name)

        val fallback = OpenMeteoWeatherParser.resolveCityPreset("unknown-city")
        assertEquals("berlin", fallback.id)
        assertEquals("Berlin", fallback.name)
    }
}

class GeocoderAndMediaProgressTest {
    @org.junit.Test
    fun `geocoder parses places with region and skips incomplete entries`() {
        val raw = """
            {"results":[
              {"id":2867714,"name":"München","latitude":48.137,"longitude":11.575,"country":"Deutschland","admin1":"Bayern"},
              {"id":1,"name":"Ohne Koordinaten"}
            ]}
        """.trimIndent()
        val places = OpenMeteoGeocoder.parseResults(raw)
        org.junit.Assert.assertEquals(1, places.size)
        org.junit.Assert.assertEquals("München", places[0].name)
        org.junit.Assert.assertEquals("Bayern, Deutschland", places[0].region)
        org.junit.Assert.assertEquals("geo-2867714", places[0].id)
        org.junit.Assert.assertTrue(OpenMeteoGeocoder.parseResults("{}").isEmpty())
    }

    @org.junit.Test
    fun `media progress is extrapolated only while playing`() {
        val playing = LiveMediaSessionSnapshot("pkg", "T", "A", "B", durationMs = 100_000L, positionMs = 10_000L,
            isPlaying = true, positionUpdatedAtElapsedMs = 1_000L)
        org.junit.Assert.assertEquals(0.30f, playing.progressAt(21_000L), 0.001f)
        org.junit.Assert.assertEquals(0.10f, playing.copy(isPlaying = false).progressAt(21_000L), 0.001f)
        org.junit.Assert.assertEquals(1f, playing.progressAt(1_000_000L), 0.001f)
    }
}
