package app.canopy.launcher.layout

import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.system.OpenMeteoWeatherParser
import app.canopy.launcher.ui.pages.ActiveTileDrag
import app.canopy.launcher.ui.pages.DragSourceKind
import app.canopy.launcher.ui.pages.TileDragEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryAndWeatherTest {
    @Test
    fun `EditHistoryState records undo redo transitions and caps stack to maxDepth`() {
        val basePages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL)
        var history = EditHistoryState(maxDepth = 3)
        var current = basePages

        repeat(5) { step ->
            history = history.recordBeforeMutation(current)
            current = current.map { it.copy(title = "${it.title}-$step") }
        }

        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(3, history.undoStack.size)

        val (afterUndo, restored) = history.undo(current)
        assertNotNull(restored)
        assertTrue(afterUndo.canRedo)

        val (afterRedo, reapplied) = afterUndo.redo(restored!!)
        assertEquals(current, reapplied)
        assertFalse(afterRedo.canRedo)
    }

    @Test
    fun `OpenMeteoWeatherParser parses temperature WMO condition curve and rain chance`() {
        val hourlyTemps = (0..23).joinToString(",") { (5.0 + it * 0.4).toString() }
        val hourlyRain = (0..23).joinToString(",") { if (it == 17) "65" else "15" }
        val payload = """
            {
              "current": { "temperature_2m": 12.4, "weather_code": 61 },
              "hourly": {
                "temperature_2m": [$hourlyTemps],
                "precipitation_probability": [$hourlyRain]
              }
            }
        """.trimIndent()

        val parsed = OpenMeteoWeatherParser.parseForecastJson(payload)
        assertNotNull(parsed)
        assertTrue(parsed!!.isLiveFromNetwork)
        assertEquals(12, parsed.currentTempC)
        assertEquals("Leichter Regen", parsed.shortCondition)
        assertEquals(65, parsed.rainChancePercent)
        assertEquals(6, parsed.hourlyPoints.size)
        assertEquals(4, parsed.hourlyForecasts.size)

        assertNull(OpenMeteoWeatherParser.parseForecastJson("invalid-json"))
    }

    @Test
    fun `shouldThrowRemove triggers when fast downward flick exceeds throw threshold`() {
        val drag = ActiveTileDrag(
            sourceKind = DragSourceKind.PAGE_TILE,
            sourcePageId = "page-start",
            sourceLeafId = "leaf-1",
            tile = TileData("t1", TileType.CLOCK, "Uhr"),
            widthDp = 180f,
            heightDp = 160f,
            pointerXRootPx = 300f,
            pointerYRootPx = 1200f,
        )

        assertTrue(
            TileDragEngine.shouldThrowRemove(
                dropPreview = null,
                drag = drag,
                pointerLocalYDp = 520f,
                viewportHeightDp = 740f,
                velocityYDpPerSec = 900f,
            )
        )
        assertFalse(
            TileDragEngine.shouldThrowRemove(
                dropPreview = null,
                drag = drag,
                pointerLocalYDp = 520f,
                viewportHeightDp = 740f,
                velocityYDpPerSec = 120f,
            )
        )
    }
}
