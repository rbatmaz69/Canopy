package app.canopy.launcher.ui.overlays

import app.canopy.launcher.data.LauncherPersistenceCodec
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.layout.ReminderItem
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.ui.tiles.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PersistentEditorsAndPageCloneTest {
    @Test
    fun `TargetViewEditorPersistence normalizes alarm time and syncs reminders to tile`() {
        assertEquals("06:05", TargetViewEditorPersistence.normalizeAlarmTime("6:5"))
        assertEquals("07:15", TargetViewEditorPersistence.normalizeAlarmTime("29:99", "07:15"))
        assertEquals(6 to 30, TargetViewEditorPersistence.parseAlarmHoursAndMinutes("06:30"))

        val baseTile = TileData(
            id = "tile-rem",
            type = TileType.REMINDERS,
            title = "Erinnerungen",
        )
        val updatedReminders = listOf(
            ReminderItem("r1", "Test-Aufgabe", false),
            ReminderItem("r2", "Erledigt", true),
        )
        val mutated = TargetViewEditorPersistence.applyReminderUpdateToTile(baseTile, updatedReminders)
        assertEquals(2, mutated.reminders?.size)
        assertEquals(listOf("Test-Aufgabe", "Erledigt"), mutated.noteLines)
    }

    @Test
    fun `renameRegularPage and duplicateRegularPage clone tree nodes with fresh IDs`() {
        val initial = MockCatalogData.createInitialPages(ShortcutBarMode.ALL)
        val firstId = initial.first().id

        val renamed = CatalogAndPageManager.renameRegularPage(initial, firstId, "  Fokus-Workspace  ")
        assertEquals("Fokus-Workspace", renamed.first().title)

        val (duplicated, copyIndex) = CatalogAndPageManager.duplicateRegularPage(renamed, firstId)
        assertEquals(renamed.size + 1, duplicated.size)
        assertEquals(1, copyIndex)
        val copyPage = duplicated[copyIndex]
        assertEquals("Fokus-Workspace Kopie", copyPage.title)
        assertNotEquals(firstId, copyPage.id)

        val origLeafIds = TilingEngine.collectLeaves(renamed.first().root).map { it.id }.toSet()
        val copyLeafIds = TilingEngine.collectLeaves(copyPage.root).map { it.id }.toSet()
        assertEquals(origLeafIds.size, copyLeafIds.size)
        assertEquals(emptySet<String>(), origLeafIds.intersect(copyLeafIds))
    }

    @Test
    fun `PersistedLauncherState round-trips customCalendarEvents accurately`() {
        val customEvent = CalendarEvent(
            id = "ev-custom-1",
            title = "Architektur-Review",
            time = "14:00",
            endTime = "15:00",
            location = "Raum C",
            startHour = 14f,
            durationHours = 1f,
            hue = 212f,
        )
        val state = PersistedLauncherState(
            pages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL),
            customCalendarEvents = listOf(customEvent),
        )
        val decoded = LauncherPersistenceCodec.decode(LauncherPersistenceCodec.encode(state))
        assertNotNull(decoded)
        assertEquals(1, decoded?.customCalendarEvents?.size)
        assertEquals("Architektur-Review", decoded?.customCalendarEvents?.first()?.title)
    }
}
