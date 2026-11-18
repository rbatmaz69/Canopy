package app.canopy.launcher.system

import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import org.junit.Assert.assertEquals
import org.junit.Test

class TileTapRouterTest {
    private fun tile(type: TileType) = TileData(id = "t", type = type, title = "T")

    @Test
    fun `system tiles open the matching real app or launcher editor`() {
        assertEquals(TileTapTarget.ALARMS, TileTapRouter.resolve(tile(TileType.CLOCK)))
        assertEquals(TileTapTarget.CALENDAR, TileTapRouter.resolve(tile(TileType.CALENDAR)))
        assertEquals(TileTapTarget.CALENDAR, TileTapRouter.resolve(tile(TileType.CALENDAR_TIMELINE)))
        assertEquals(TileTapTarget.WEATHER_DETAIL, TileTapRouter.resolve(tile(TileType.WEATHER)))
        assertEquals(TileTapTarget.NOTES_EDITOR, TileTapRouter.resolve(tile(TileType.REMINDERS)))
        assertEquals(TileTapTarget.MEDIA_APP, TileTapRouter.resolve(tile(TileType.MEDIA)))
        assertEquals(TileTapTarget.GALLERY, TileTapRouter.resolve(tile(TileType.PHOTOS)))
        assertEquals(TileTapTarget.BATTERY, TileTapRouter.resolve(tile(TileType.BATTERY)))
    }

    @Test
    fun `tiles with their own controls never get a second tap action`() {
        listOf(
            TileType.SINGLE_APP,
            TileType.APP_GROUP,
            TileType.THIRD_PARTY_WIDGET,
            TileType.SEARCH_BAR,
            TileType.QUICK_TOGGLES,
            TileType.ALL_APPS_BOX,
            TileType.NEWLY_INSTALLED,
        ).forEach { type ->
            assertEquals(type.name, TileTapTarget.NONE, TileTapRouter.resolve(tile(type)))
        }
    }
}
