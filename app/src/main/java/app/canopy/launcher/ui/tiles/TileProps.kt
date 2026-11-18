package app.canopy.launcher.ui.tiles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import app.canopy.launcher.data.AlarmState
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.theme.ThemeTokens
import app.canopy.launcher.ui.theme.TileSurfaceTokens
import kotlinx.serialization.Serializable

enum class AppCategory(val label: String) {
    SOCIAL("Social"),
    SPIELE("Spiele"),
    WERKZEUGE("Werkzeuge"),
    MEDIEN("Medien"),
    LESEN("Lesen"),
    PRODUKTIVITAET("Produktivität"),
    SYSTEM("System"),
}

@Immutable
data class AppDefinition(
    val id: String,
    val name: String,
    val iconKey: String,
    val color: Color,
    val hue: Float,
    val category: AppCategory,
    val badge: Int? = null,
    val usageScore: Int,
    val subtitle: String,
    val isNew: Boolean = false,
    val packageName: String? = null,
    val componentActivityName: String? = null,
    /** Which Android profile the app belongs to (personal, work, private space). */
    val profile: AppProfile = AppProfile.PERSONAL,
    /** UserManager serial of the owning profile; 0 for the personal profile. */
    val userSerial: Long = 0L,
    /** Work apps are paused (quiet mode): shown greyed out, tapping offers to resume. */
    val isPaused: Boolean = false,
) {
    /** Key used to match notifications/badges: package + profile. */
    val badgeKey: String get() = AppIdentity.badgeKey(packageName.orEmpty(), userSerial)
}

enum class AppProfile { PERSONAL, WORK, PRIVATE }

/** App management hooks for All Apps (hide, rename, private space) without threading params through every tile. */
data class AppManagementActions(
    val hiddenAppIds: Set<String> = emptySet(),
    val privateSpaceLocked: Boolean = false,
    val onHideApp: (appId: String) -> Unit = {},
    val onRenameApp: (appId: String) -> Unit = {},
    val onUnlockPrivateSpace: () -> Unit = {},
)

val LocalAppManagement = androidx.compose.runtime.staticCompositionLocalOf { AppManagementActions() }

/**
 * Stable app ids. Personal-profile apps keep the historic `pkg:<package>` id for their main
 * launcher activity, so existing layouts stay valid. Additional launcher activities of the same
 * package get `/<activity>`, apps from other profiles get `@<userSerial>`.
 */
object AppIdentity {
    fun appId(packageName: String, activityName: String?, isMainActivity: Boolean, userSerial: Long): String {
        val base = if (isMainActivity || activityName.isNullOrBlank()) "pkg:$packageName" else "pkg:$packageName/$activityName"
        return if (userSerial == 0L) base else "$base@$userSerial"
    }

    fun badgeKey(packageName: String, userSerial: Long): String =
        if (userSerial == 0L) "pkg:$packageName" else "pkg:$packageName@$userSerial"

    fun packageOf(appId: String?): String? {
        if (appId == null || !appId.startsWith("pkg:")) return null
        return appId.removePrefix("pkg:").substringBefore('@').substringBefore('/').trim().takeIf { it.isNotEmpty() }
    }

    fun userSerialOf(appId: String?): Long =
        appId?.substringAfter('@', "")?.toLongOrNull() ?: 0L
}

@Immutable
data class ContactItem(
    val id: String,
    val name: String,
    val role: String,
    val status: String,
    val hue: Float,
)

@Immutable
data class SettingItem(
    val id: String,
    val name: String,
    val detail: String,
    val iconKey: String,
    val hue: Float,
    val intentAction: String = android.provider.Settings.ACTION_SETTINGS,
)

@Immutable
data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val accentHue: Float,
)

@Immutable
@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val time: String,
    val endTime: String,
    val location: String,
    val startHour: Float,
    val durationHours: Float,
    val hue: Float,
)

enum class SystemToggleKey {
    WIFI,
    BLUETOOTH,
    FLASHLIGHT,
    DND,
    AUTO_ROTATE,
    VOLUME,
}

@Immutable
data class SystemControlState(
    val wifi: Boolean = false,
    val bluetooth: Boolean = false,
    val flashlight: Boolean = false,
    val dnd: Boolean = false,
    val autoRotate: Boolean = false,
    val onToggle: (SystemToggleKey) -> Unit = {},
    val onOpenSystemPanel: (SystemToggleKey) -> Unit = {},
)

@Immutable
data class MediaPlayerState(
    val trackIndex: Int = 0,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    /** Package of the app owning the active media session, if any. */
    val sessionPackage: String? = null,
    /** Raw session snapshot; tiles extrapolate live progress from it locally. */
    val snapshot: app.canopy.launcher.system.LiveMediaSessionSnapshot? = null,
    val overrideTrack: MediaTrack? = null,
    val onTogglePlay: () -> Unit = {},
    val onPrev: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onSeek: (Float) -> Unit = {},
)

data class TileContentProps(
    val tile: TileData,
    val leafId: String,
    val widthDp: Float,
    val heightDp: Float,
    val tier: TileSizeTier,
    val theme: ThemeTokens,
    val surface: TileSurfaceTokens,
    val motionSettings: MotionSettings,
    val isEditMode: Boolean,
    val alarmState: AlarmState,
    val onToggleAlarm: () -> Unit,
    val onOpenApp: (appId: String, sourceRect: Rect?) -> Unit,
    val onOpenGroup: (tile: TileData, leafId: String, sourceRect: Rect?) -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenTileTarget: (tile: TileData, sourceRect: Rect?) -> Unit,
    val onUpdateTile: (leafId: String, updater: (TileData) -> TileData) -> Unit,
    val calendarEvents: List<CalendarEvent>,
    val mediaState: MediaPlayerState,
    val systemState: SystemControlState,
    val allAppsCatalog: List<AppDefinition>,
    val onPinAppToHome: (appId: String) -> Unit = {},
    val onShowAppDetails: (appId: String, sourceRect: Rect?) -> Unit = { _, _ -> },
    val onUninstallApp: (appId: String) -> Unit = {},
)

object TileContainerMetrics {
    fun isHeroTile(tile: TileData): Boolean =
        tile.type == TileType.GREETING || tile.type == TileType.CLOCK || tile.mediaLarge == true

    fun isFullBleedTile(tile: TileData, tier: TileSizeTier): Boolean =
        (tile.type == TileType.MEDIA && (tile.mediaLarge == true || tier == TileSizeTier.L)) ||
            (tile.type == TileType.PHOTOS && tier != TileSizeTier.XS)

    fun resolvePaddingDp(tile: TileData, tier: TileSizeTier): Float = when {
        isFullBleedTile(tile, tier) -> 0f
        tier == TileSizeTier.XS -> 6f
        tile.type == TileType.THIRD_PARTY_WIDGET -> 8f
        else -> 13f
    }
}
