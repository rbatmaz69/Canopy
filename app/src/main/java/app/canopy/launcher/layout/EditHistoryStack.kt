package app.canopy.launcher.layout

/**
 * Immutable multi-step Undo/Redo history ring buffer for tiling layout edits.
 */
data class EditHistoryState(
    val undoStack: List<List<LauncherPage>> = emptyList(),
    val redoStack: List<List<LauncherPage>> = emptyList(),
    val maxDepth: Int = 20,
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun recordBeforeMutation(currentPages: List<LauncherPage>): EditHistoryState {
        if (undoStack.lastOrNull() == currentPages) return this
        val nextUndo = (undoStack + listOf(currentPages)).takeLast(maxDepth.coerceAtLeast(1))
        return copy(undoStack = nextUndo, redoStack = emptyList())
    }

    fun undo(currentPages: List<LauncherPage>): Pair<EditHistoryState, List<LauncherPage>?> {
        val target = undoStack.lastOrNull() ?: return this to null
        val nextUndo = undoStack.dropLast(1)
        val nextRedo = (redoStack + listOf(currentPages)).takeLast(maxDepth.coerceAtLeast(1))
        return copy(undoStack = nextUndo, redoStack = nextRedo) to target
    }

    fun redo(currentPages: List<LauncherPage>): Pair<EditHistoryState, List<LauncherPage>?> {
        val target = redoStack.lastOrNull() ?: return this to null
        val nextRedo = redoStack.dropLast(1)
        val nextUndo = (undoStack + listOf(currentPages)).takeLast(maxDepth.coerceAtLeast(1))
        return copy(undoStack = nextUndo, redoStack = nextRedo) to target
    }
}
