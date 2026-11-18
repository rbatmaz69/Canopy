package app.canopy.launcher.ui.tiles

import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.layout.ReminderItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EigenTilesBehaviorTest {
    @Test
    fun `new reminder tiles start empty instead of demo entries`() {
        assertTrue(SystemTileDefaults.DEFAULT_REMINDERS.isEmpty())
        assertTrue(SystemTileDefaults.DEFAULT_NOTE_LINES.isEmpty())
    }

    @Test
    fun `toggling and adding typed reminders updates state directly`() {
        val initial = listOf(ReminderItem("r1", "Paket abholen", false))
        val toggled = EigenTileActions.toggleReminder(initial, "r1")
        assertTrue(toggled.first { it.id == "r1" }.done)
        assertFalse(EigenTileActions.toggleReminder(toggled, "r1").first { it.id == "r1" }.done)

        val added = EigenTileActions.addReminder(initial, "  Arzt anrufen ", nowMillis = 42L)
        assertEquals(2, added.size)
        assertEquals("Arzt anrufen", added.first().text)
        assertFalse(added.first().done)

        // Blank input never creates an entry.
        assertEquals(initial, EigenTileActions.addReminder(initial, "   "))

        val allChecked = EigenTileActions.checkAllReminders(added)
        assertTrue(allChecked.all { it.done })
    }

    @Test
    fun `note lines only grow from real text`() {
        assertEquals(listOf("Neu", "Alt"), EigenTileActions.addNoteLine(listOf("Alt"), " Neu "))
        assertEquals(listOf("Alt"), EigenTileActions.addNoteLine(listOf("Alt"), ""))
    }

    @Test
    fun `photo decoder picks a power of two sample size close to the tile`() {
        assertEquals(1, PhotoDecoder.computeSampleSize(800, 600, 400))
        assertEquals(4, PhotoDecoder.computeSampleSize(4000, 3000, 600))
        assertEquals(1, PhotoDecoder.computeSampleSize(0, 0, 600))
    }

    @Test
    fun `media tile formats time and clamps seekbar drags cleanly`() {
        assertEquals("0:00", MediaTileHelpers.formatTime(0f))
        assertEquals("3:34", MediaTileHelpers.formatTime(214f))
        assertEquals(0f, MediaTileHelpers.clampSeekRatio(-20f, 300f), 1e-4f)
        assertEquals(0.5f, MediaTileHelpers.clampSeekRatio(150f, 300f), 1e-4f)
        assertEquals(1f, MediaTileHelpers.clampSeekRatio(450f, 300f), 1e-4f)
    }
}
