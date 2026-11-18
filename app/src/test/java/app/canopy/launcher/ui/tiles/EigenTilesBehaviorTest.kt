package app.canopy.launcher.ui.tiles

import app.canopy.launcher.data.SystemTileDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EigenTilesBehaviorTest {
    @Test
    fun `toggling and adding quick reminders updates state directly`() {
        val initial = SystemTileDefaults.DEFAULT_REMINDERS
        val toggled = EigenTileActions.toggleReminder(initial, "r2")
        assertTrue(toggled.first { it.id == "r2" }.done)
        assertFalse(EigenTileActions.toggleReminder(toggled, "r2").first { it.id == "r2" }.done)

        val added = EigenTileActions.addQuickReminder(initial)
        assertEquals(initial.size + 1, added.size)
        assertFalse(added.first().done)

        val allChecked = EigenTileActions.checkAllReminders(added)
        assertTrue(allChecked.all { it.done })
    }

    @Test
    fun `photo scene switcher cycles cleanly across all scenes`() {
        val count = SystemTileDefaults.PHOTO_SCENES.size
        assertEquals(3, count)
        assertEquals(1, EigenTileActions.nextPhotoSceneIndex(0))
        assertEquals(2, EigenTileActions.nextPhotoSceneIndex(1))
        assertEquals(0, EigenTileActions.nextPhotoSceneIndex(2))
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
