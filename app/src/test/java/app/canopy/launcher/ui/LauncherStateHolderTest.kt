package app.canopy.launcher.ui

import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.LeafNode
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherStateHolderTest {
    private fun page(id: String) = LauncherPage(id, id, PageKind.TILES, LeafNode("l-$id", TileData("t-$id", TileType.NOTE, "Notiz")))

    @Test
    fun `page edits are undoable and every change notifies the saver`() {
        val saved = mutableListOf<PersistedLauncherState>()
        val holder = LauncherStateHolder(PersistedLauncherState(pages = listOf(page("a"))), onChanged = { saved += it })

        holder.updatePagesWithHistory(listOf(page("a"), page("b")))
        assertEquals(2, holder.state.pages.size)
        assertTrue(holder.editHistory.canUndo)

        holder.undo()
        assertEquals(1, holder.state.pages.size)
        holder.redo()
        assertEquals(2, holder.state.pages.size)
        assertEquals(3, saved.size)
    }

    @Test
    fun `field delegate reads the latest value right after a write`() {
        val holder = LauncherStateHolder(PersistedLauncherState(pages = listOf(page("a"))))
        var themeId by holder.field({ it.themeId }, { copy(themeId = it) })
        themeId = ThemeVariantId.GLASS
        assertEquals(ThemeVariantId.GLASS, themeId)
        assertEquals(ThemeVariantId.GLASS, holder.state.themeId)
    }

    @Test
    fun `unchanged writes do not trigger saves and replaceAll clears history`() {
        var saves = 0
        val initial = PersistedLauncherState(pages = listOf(page("a")))
        val holder = LauncherStateHolder(initial, onChanged = { saves++ })
        holder.update { it }
        assertEquals(0, saves)

        holder.updatePagesWithHistory(listOf(page("x")))
        holder.replaceAll(initial)
        assertFalse(holder.editHistory.canUndo)
    }
}
