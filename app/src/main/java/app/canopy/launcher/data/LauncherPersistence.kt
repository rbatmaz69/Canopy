package app.canopy.launcher.data

import android.content.Context
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.ui.tiles.CalendarEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.FileOutputStream

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
    val useSystemWallpaper: Boolean = true,
    val appUsageCounts: Map<String, Int> = emptyMap(),
    val customCalendarEvents: List<CalendarEvent> = emptyList(),
    val weatherCityId: String = "berlin",
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val onboardingCompleted: Boolean = false,
    /** Keep the home screen in portrait (default, like most launchers). */
    val lockPortrait: Boolean = true,
    /** A place picked via city search; takes precedence over [weatherCityId] presets. */
    val weatherLocation: WeatherLocation? = null,
    /** Apps hidden from All Apps and search (still usable from existing tiles). */
    val hiddenAppIds: Set<String> = emptySet(),
    /** User-chosen app names, keyed by app id. */
    val appLabelOverrides: Map<String, String> = emptyMap(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
    }
}

@Serializable
data class WeatherLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val region: String = "",
)

interface LauncherStateStore {
    fun save(state: PersistedLauncherState)
    fun load(): PersistedLauncherState?
    fun reset()
}

/**
 * Step-wise JSON migrations. Each entry upgrades a raw state object from version N to N+1,
 * so old layouts are never thrown away when the model changes.
 */
object LauncherStateMigrations {
    private val steps: Map<Int, (JsonObject) -> JsonObject> = mapOf(
        // v1 → v2: real app ids were introduced; the remap runs in the app once the catalog is known.
        1 to { obj -> obj },
        // v2 → v3: added lockPortrait + weatherLocation (defaults are fine).
        2 to { obj -> obj },
    )

    fun migrate(raw: JsonObject): JsonObject {
        var obj = raw
        var version = (obj["schemaVersion"] as? JsonPrimitive)?.intOrNull ?: 1
        while (version < PersistedLauncherState.CURRENT_SCHEMA_VERSION) {
            val step = steps[version] ?: break
            obj = step(obj)
            version += 1
        }
        return obj
    }
}

/**
 * JSON serialization codec and persistence helper for Canopy launcher state.
 */
object LauncherPersistenceCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        classDiscriminator = "kind"
    }

    fun encode(state: PersistedLauncherState): String = json.encodeToString(state)

    /**
     * Decodes and migrates a stored layout. Returns null for corrupt or empty data — callers
     * fall back to a backup copy instead of silently replacing the user's layout.
     */
    fun decode(raw: String?): PersistedLauncherState? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val element = json.parseToJsonElement(raw)
            val migrated = LauncherStateMigrations.migrate(element.jsonObject)
            val parsed = json.decodeFromJsonElement(PersistedLauncherState.serializer(), migrated)
            if (parsed.pages.isEmpty()) null else parsed
        }.getOrNull()
    }
}

/**
 * Crash-safe file store: writes go to a temp file that is renamed over the real one, and the
 * previous good version is kept as a backup. Loading falls back main → backup → legacy prefs.
 */
class FileLauncherStore(
    private val directory: File,
    private val legacyLoader: () -> String? = { null },
    private val onLegacyMigrated: () -> Unit = {},
) : LauncherStateStore {
    private val lock = Any()
    private val mainFile get() = File(directory, FILE_NAME)
    private val backupFile get() = File(directory, BACKUP_NAME)
    private val tempFile get() = File(directory, TEMP_NAME)

    /** Where the last [load] came from — useful to tell the user a backup was restored. */
    @Volatile
    var lastLoadSource: LoadSource = LoadSource.NONE
        private set

    enum class LoadSource { NONE, MAIN, BACKUP, LEGACY }

    override fun save(state: PersistedLauncherState) {
        val encoded = LauncherPersistenceCodec.encode(state)
        synchronized(lock) {
            directory.mkdirs()
            val current = mainFile
            if (current.exists() && LauncherPersistenceCodec.decode(current.readTextOrNull()) != null) {
                current.copyTo(backupFile, overwrite = true)
            }
            FileOutputStream(tempFile).use { out ->
                out.write(encoded.toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            if (!tempFile.renameTo(current)) {
                // Some file systems refuse to rename over an existing file.
                current.delete()
                tempFile.renameTo(current)
            }
        }
    }

    override fun load(): PersistedLauncherState? = synchronized(lock) {
        LauncherPersistenceCodec.decode(mainFile.readTextOrNull())?.let {
            lastLoadSource = LoadSource.MAIN
            return@synchronized it
        }
        LauncherPersistenceCodec.decode(backupFile.readTextOrNull())?.let {
            lastLoadSource = LoadSource.BACKUP
            return@synchronized it
        }
        LauncherPersistenceCodec.decode(legacyLoader())?.let { legacy ->
            lastLoadSource = LoadSource.LEGACY
            runCatching { save(legacy) }.onSuccess { onLegacyMigrated() }
            return@synchronized legacy
        }
        lastLoadSource = LoadSource.NONE
        null
    }

    override fun reset() {
        synchronized(lock) {
            if (mainFile.exists()) mainFile.copyTo(backupFile, overwrite = true)
            mainFile.delete()
            tempFile.delete()
        }
    }

    private fun File.readTextOrNull(): String? =
        if (exists()) runCatching { readText(Charsets.UTF_8) }.getOrNull() else null

    companion object {
        const val FILE_NAME = "launcher_state.json"
        const val BACKUP_NAME = "launcher_state.bak.json"
        const val TEMP_NAME = "launcher_state.json.tmp"

        /** Production store in app-private storage, migrating once from the old SharedPreferences key. */
        fun create(context: Context): FileLauncherStore {
            val prefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            return FileLauncherStore(
                directory = File(context.filesDir, "launcher"),
                legacyLoader = { prefs.getString(LEGACY_STORAGE_KEY, null) },
                onLegacyMigrated = { prefs.edit().remove(LEGACY_STORAGE_KEY).apply() },
            )
        }

        const val LEGACY_PREFS_NAME = "canopy_launcher_prefs"
        const val LEGACY_STORAGE_KEY = "kachelos_launcher_state_v5"
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
