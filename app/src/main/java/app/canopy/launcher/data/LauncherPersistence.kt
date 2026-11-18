package app.canopy.launcher.data

import android.content.Context
import android.content.SharedPreferences
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.ui.tiles.CalendarEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PersistedLauncherState(
    val pages: List<LauncherPage>,
    val homePageId: String = "page-start",
    val themeId: ThemeVariantId = ThemeVariantId.MATERIAL_YOU,
    val colorMode: ColorMode = ColorMode.LIGHT,
    val wallpaperId: WallpaperPresetId = WallpaperPresetId.SAND,
    val tonalIcons: Boolean = true,
    val shortcutBarMode: ShortcutBarMode = ShortcutBarMode.ALL,
    val motionSettings: MotionSettings = MotionSettings(),
    val alarmState: AlarmState = AlarmState(),
    val useSystemWallpaper: Boolean = false,
    val appUsageCounts: Map<String, Int> = emptyMap(),
    val customCalendarEvents: List<CalendarEvent> = emptyList(),
)

interface LauncherStateStore {
    fun save(state: PersistedLauncherState)
    fun load(): PersistedLauncherState?
    fun reset()
}

/**
 * JSON serialization codec and persistence helper for Canopy launcher state.
 */
object LauncherPersistenceCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "kind"
    }

    fun encode(state: PersistedLauncherState): String = json.encodeToString(state)

    fun decode(raw: String?): PersistedLauncherState? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val parsed = json.decodeFromString<PersistedLauncherState>(raw)
            if (parsed.pages.isEmpty()) null else parsed
        }.getOrNull()
    }
}

class SharedPreferencesLauncherStore(
    context: Context,
    private val prefsName: String = PREFS_NAME,
    private val storageKey: String = STORAGE_KEY,
) : LauncherStateStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun save(state: PersistedLauncherState) {
        prefs.edit().putString(storageKey, LauncherPersistenceCodec.encode(state)).apply()
    }

    override fun load(): PersistedLauncherState? =
        LauncherPersistenceCodec.decode(prefs.getString(storageKey, null))

    override fun reset() {
        prefs.edit().remove(storageKey).apply()
    }

    companion object {
        const val PREFS_NAME = "canopy_launcher_prefs"
        const val STORAGE_KEY = "kachelos_launcher_state_v5"
    }
}

class InMemoryLauncherStore(
    private val defaultFactory: () -> PersistedLauncherState,
) : LauncherStateStore {
    private var serialized: String? = null

    override fun save(state: PersistedLauncherState) {
        serialized = LauncherPersistenceCodec.encode(state)
    }

    override fun load(): PersistedLauncherState? = LauncherPersistenceCodec.decode(serialized)

    override fun reset() {
        serialized = null
    }

    fun loadOrReset(): PersistedLauncherState = load() ?: defaultFactory()
}
