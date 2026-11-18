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
