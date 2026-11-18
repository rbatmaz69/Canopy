package app.canopy.launcher.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.canopy.launcher.CanopyApplication
import app.canopy.launcher.data.LauncherStateStore
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.layout.EditHistoryState
import app.canopy.launcher.layout.LauncherPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Pure state holder for the persisted launcher model + undo history. Kept free of Android
 * types so it can be unit-tested; [LauncherViewModel] wires it to storage and lifecycle.
 *
 * State is Compose snapshot state: reads are always current (also right after a write in the
 * same event handler) and Compose recomposes on change.
 */
class LauncherStateHolder(
    initial: PersistedLauncherState,
    private val onChanged: (PersistedLauncherState) -> Unit = {},
) {
    var state by mutableStateOf(initial)
        private set

    var editHistory by mutableStateOf(EditHistoryState())

    /** True while a live gesture (divider drag) is changing the layout every frame. */
    var isInteracting by mutableStateOf(false)

    fun update(transform: (PersistedLauncherState) -> PersistedLauncherState) {
        val next = transform(state)
        if (next != state) {
            state = next
            onChanged(next)
        }
    }

    /** Applies a page change and records the previous pages for undo. */
    fun updatePagesWithHistory(next: List<LauncherPage>) {
        if (next != state.pages) {
            editHistory = editHistory.recordBeforeMutation(state.pages)
            update { it.copy(pages = next) }
        }
    }

    fun undo() {
        val (nextHistory, previous) = editHistory.undo(state.pages)
        editHistory = nextHistory
        if (previous != null) update { it.copy(pages = previous) }
    }

    fun redo() {
        val (nextHistory, nextPages) = editHistory.redo(state.pages)
        editHistory = nextHistory
        if (nextPages != null) update { it.copy(pages = nextPages) }
    }

    fun replaceAll(next: PersistedLauncherState, clearHistory: Boolean = true) {
        if (clearHistory) editHistory = EditHistoryState()
        update { next }
    }

    fun historyField(): ReadWriteProperty<Any?, EditHistoryState> = object : ReadWriteProperty<Any?, EditHistoryState> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): EditHistoryState = editHistory
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: EditHistoryState) {
            editHistory = value
        }
    }

    /** Property delegate for one field, so UI code can keep `var pages by …` style. */
    fun <T> field(
        get: (PersistedLauncherState) -> T,
        set: PersistedLauncherState.(T) -> PersistedLauncherState,
    ): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = get(state)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = update { it.set(value) }
    }
}

/**
 * Owns the launcher model across configuration changes and persists it off the main thread:
 * saves are debounced (never per drag frame) and flushed when the launcher goes to background.
 */
@OptIn(FlowPreview::class)
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val services = (application as CanopyApplication).services
    private val store: LauncherStateStore = services.store

    /** True when no stored layout existed (first run or after reset) — app ids still need remapping. */
    var needsRealAppRemap: Boolean
        private set

    val holder: LauncherStateHolder

    init {
        val loaded = store.load()
        needsRealAppRemap = loaded == null
        holder = LauncherStateHolder(loaded ?: defaultState())
        viewModelScope.launch {
            snapshotFlow { holder.state to holder.isInteracting }
                .drop(1)
                .debounce(SAVE_DEBOUNCE_MS)
                .collect { (state, interacting) -> if (!interacting) persist(state) }
        }
    }

    fun markRealAppRemapDone() {
        needsRealAppRemap = false
    }

    /** Synchronous-enough flush for onStop: runs on the app scope so it survives the activity. */
    fun flush() {
        persist(holder.state)
    }

    fun resetToDefaults(): PersistedLauncherState {
        store.reset()
        needsRealAppRemap = true
        val fresh = defaultState()
        holder.replaceAll(fresh)
        return fresh
    }

    private fun persist(state: PersistedLauncherState) {
        saveScope.launch { runCatching { store.save(state) } }
    }

    // One writer at a time, in submission order, so an older state never overwrites a newer one.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val saveScope = CoroutineScope(services.appScope.coroutineContext + Dispatchers.IO.limitedParallelism(1))

    override fun onCleared() {
        flush()
        super.onCleared()
    }

    companion object {
        const val SAVE_DEBOUNCE_MS = 500L

        fun defaultState(): PersistedLauncherState =
            PersistedLauncherState(pages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL))
    }
}
