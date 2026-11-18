package app.canopy.launcher.system

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect as AndroidRect
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.provider.Settings
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.LeafNode
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.SplitDirection
import app.canopy.launcher.layout.SplitNode
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.tiles.AppCategory
import app.canopy.launcher.ui.tiles.AppDefinition
import app.canopy.launcher.ui.tiles.AppIdentity
import app.canopy.launcher.ui.tiles.AppProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class InstalledAppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val androidCategory: Int = -1,
    val isNewInstall: Boolean = false,
    /** True for the activity the package's launch intent points to. */
    val isMainActivity: Boolean = true,
    val userSerial: Long = 0L,
    val profile: AppProfile = AppProfile.PERSONAL,
    val isPaused: Boolean = false,
)

data class NativeShortcutEntry(
    val id: String,
    val packageName: String,
    val label: String,
)

data class ClipRevealSpec(
    val leftPx: Int,
    val topPx: Int,
    val widthPx: Int,
    val heightPx: Int,
)

/**
 * Pure Kotlin mapper and launch geometry helper for merging real Android device apps
 * into Canopy's adaptive icon catalog and computing ActivityOptions bounds.
 */
object InstalledAppsMapper {
    const val NATIVE_SHORTCUT_PREFIX = "↗ "

    fun stableHueForPackage(packageName: String): Float {
        val raw = packageName.fold(17) { acc, ch -> acc * 31 + ch.code }
        return (abs(raw) % 360).toFloat()
    }

    fun colorFromHue(hue: Float): Color = Color.hsl(
        hue = ((hue % 360f) + 360f) % 360f,
        saturation = 0.72f,
        lightness = 0.52f,
    )

    fun inferIconKey(packageName: String, label: String): String {
        val token = "${packageName.lowercase(Locale.ROOT)} ${label.lowercase(Locale.ROOT)}"
        return when {
            "dialer" in token || "phone" in token || "telefon" in token -> "Phone"
            "mms" in token || "messages" in token || "whatsapp" in token || "signal" in token || "telegram" in token -> "MessageCircle"
            "camera" in token || "kamera" in token -> "Camera"
            "chrome" in token || "browser" in token || "firefox" in token || "edge" in token -> "Compass"
            "gmail" in token || ".gm" in token || "mail" in token || "outlook" in token -> "Mail"
            "music" in token || "spotify" in token || "ytmusic" in token -> "Music2"
            "maps" in token || "waze" in token || "osmand" in token -> "MapPin"
            "calendar" in token || "kalender" in token -> "Calendar"
            "deskclock" in token || "clock" in token || "wecker" in token -> "AlarmClock"
            "keep" in token || "notes" in token || "notiz" in token || "obsidian" in token -> "FileText"
            "settings" in token || "einstellungen" in token -> "Settings"
            "calculator" in token || "rechner" in token -> "Calculator"
            "documentsui" in token || "files" in token || "dateien" in token -> "FolderOpen"
            "photos" in token || "gallery" in token || "galerie" in token -> "Image"
            "weather" in token || "wetter" in token -> "CloudRain"
            "podcast" in token || "antennapod" in token -> "Headphones"
            "fitness" in token || "health" in token || "fit" in token -> "Activity"
            else -> "Sparkles"
        }
    }

    fun mapCategory(androidCategory: Int, packageName: String, label: String): AppCategory =
        when (androidCategory) {
            0 -> AppCategory.SPIELE          // ApplicationInfo.CATEGORY_GAME
            1, 2, 3 -> AppCategory.MEDIEN    // CATEGORY_AUDIO, VIDEO, IMAGE
            4 -> AppCategory.SOCIAL          // CATEGORY_SOCIAL
            5 -> AppCategory.LESEN           // CATEGORY_NEWS
            6 -> AppCategory.WERKZEUGE       // CATEGORY_MAPS
            7 -> AppCategory.PRODUKTIVITAET  // CATEGORY_PRODUCTIVITY
            else -> when (inferIconKey(packageName, label)) {
                "Phone", "MessageCircle" -> AppCategory.SOCIAL
                "Camera", "Music2", "Image", "Headphones" -> AppCategory.MEDIEN
                "Compass", "Mail", "Calendar", "FileText" -> AppCategory.PRODUKTIVITAET
                "Settings", "Activity" -> AppCategory.SYSTEM
                "MapPin", "AlarmClock", "Calculator", "FolderOpen", "CloudRain" -> AppCategory.WERKZEUGE
                else -> AppCategory.WERKZEUGE
            }
        }

    fun mergeWithCatalog(
        catalog: List<AppDefinition>,
        installed: List<InstalledAppEntry>,
        selfPackage: String = "app.canopy.launcher",
    ): List<AppDefinition> {
        val validInstalled = installed.filter {
            it.packageName.isNotBlank() &&
                it.packageName != selfPackage &&
                it.label.isNotBlank()
        }
        if (validInstalled.isEmpty()) return catalog

        // Every launcher activity in every profile is its own app entry.
        val byPackageAndUser = validInstalled.groupBy { it.packageName to it.userSerial }
        return validInstalled
            .distinctBy { Triple(it.packageName, it.activityName, it.userSerial) }
            .map { entry ->
                val hue = stableHueForPackage(entry.packageName)
                val siblings = byPackageAndUser[entry.packageName to entry.userSerial].orEmpty()
                // Exactly one activity per package+profile keeps the plain id.
                val mainActivity = siblings.firstOrNull { it.isMainActivity }?.activityName
                    ?: siblings.firstOrNull()?.activityName
                val isMain = entry.activityName == mainActivity
                AppDefinition(
                    id = AppIdentity.appId(entry.packageName, entry.activityName, isMain, entry.userSerial),
                    name = entry.label,
                    iconKey = inferIconKey(entry.packageName, entry.label),
                    color = colorFromHue(hue),
                    hue = hue,
                    category = mapCategory(entry.androidCategory, entry.packageName, entry.label),
                    badge = null,
                    usageScore = 75,
                    subtitle = entry.packageName,
                    isNew = entry.isNewInstall,
                    packageName = entry.packageName,
                    componentActivityName = entry.activityName,
                    profile = entry.profile,
                    userSerial = entry.userSerial,
                    isPaused = entry.isPaused,
                )
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    fun resolveRealAppIdsForPages(
        apps: List<AppDefinition>,
        pages: List<LauncherPage>,
    ): List<LauncherPage> {
        if (apps.isEmpty()) return pages
        val byIcon = apps.groupBy { it.iconKey }
        fun pickForMockId(mockId: String): String? {
            val directPkg = apps.firstOrNull { it.id == mockId }?.id
            if (directPkg != null) return directPkg
            val iconMatch = when (mockId) {
                "app-telefon" -> byIcon["Phone"]?.firstOrNull()?.id
                "app-nachrichten" -> byIcon["MessageCircle"]?.firstOrNull()?.id
                "app-kamera" -> byIcon["Camera"]?.firstOrNull()?.id
                "app-browser" -> byIcon["Compass"]?.firstOrNull()?.id
                "app-mail" -> byIcon["Mail"]?.firstOrNull()?.id
                "app-musik", "app-podcast" -> byIcon["Music2"]?.firstOrNull()?.id ?: byIcon["Headphones"]?.firstOrNull()?.id
                "app-karten" -> byIcon["MapPin"]?.firstOrNull()?.id
                "app-kalender" -> byIcon["Calendar"]?.firstOrNull()?.id
                "app-uhr" -> byIcon["AlarmClock"]?.firstOrNull()?.id
                "app-notizen" -> byIcon["FileText"]?.firstOrNull()?.id
                "app-einstellungen" -> byIcon["Settings"]?.firstOrNull()?.id
                "app-rechner" -> byIcon["Calculator"]?.firstOrNull()?.id
                "app-galerie", "app-fotos" -> byIcon["Image"]?.firstOrNull()?.id
                "app-wetter" -> byIcon["CloudRain"]?.firstOrNull()?.id
                "app-fitness" -> byIcon["Activity"]?.firstOrNull()?.id
                else -> null
            }
            return iconMatch
        }

        // Groups are filled by real Android category, never with arbitrary apps.
        val categoryForGroupTitle = mapOf(
            "spiele" to AppCategory.SPIELE,
            "social" to AppCategory.SOCIAL,
            "lesen" to AppCategory.LESEN,
            "medien" to AppCategory.MEDIEN,
            "audio & podcast" to AppCategory.MEDIEN,
            "werkzeuge" to AppCategory.WERKZEUGE,
            "produktivität" to AppCategory.PRODUKTIVITAET,
        )
        val usedInGroups = mutableSetOf<String>()
        val usedSingles = mutableSetOf<String>()

        fun remapGroup(tile: TileData): List<String> {
            val ids = tile.appIds.orEmpty()
            val resolved = mutableListOf<String>()
            ids.forEach { oldId ->
                val picked = pickForMockId(oldId)
                if (picked != null && picked !in resolved) resolved += picked
            }
            val category = categoryForGroupTitle[tile.title.trim().lowercase(Locale.ROOT)]
            if (category != null) {
                apps.filter { it.category == category && it.id !in resolved && it.id !in usedInGroups }
                    .sortedByDescending { it.usageScore }
                    .forEach { if (resolved.size < ids.size.coerceAtLeast(4)) resolved += it.id }
            }
            usedInGroups += resolved
            return resolved
        }

        fun remapSingle(oldId: String): String? {
            pickForMockId(oldId)?.let { usedSingles += it; return it }
            // No equivalent installed: leave the tile pointing nowhere rather than at a random app.
            return null
        }

        fun remapNode(node: app.canopy.launcher.layout.TreeNode): app.canopy.launcher.layout.TreeNode = when (node) {
            is LeafNode -> {
                val tile = node.tile
                val mappedAppId = tile.appId?.let { oldId ->
                    if (apps.any { it.id == oldId }) oldId else remapSingle(oldId)
                }
                val mappedGroupIds = if (tile.type == TileType.APP_GROUP && tile.appIds != null) {
                    remapGroup(tile)
                } else tile.appIds
                val mappedNewIds = if (tile.type == TileType.NEWLY_INSTALLED) {
                    apps.filter { it.isNew }.map { it.id }.take(4)
                } else tile.newAppIds
                node.copy(
                    tile = tile.copy(
                        appId = mappedAppId,
                        appIds = mappedGroupIds,
                        newAppIds = mappedNewIds,
                    )
                )
            }
            is SplitNode -> node.copy(
                first = remapNode(node.first),
                second = remapNode(node.second),
            )
        }

        return pages.map { p -> p.copy(root = remapNode(p.root)) }
    }

    fun computeClipRevealSpec(
        sourceRectDp: Rect?,
        density: Float,
        screenWidthPx: Int,
        screenHeightPx: Int,
    ): ClipRevealSpec? {
        if (sourceRectDp == null || density <= 0f || screenWidthPx <= 0 || screenHeightPx <= 0) {
            return null
        }
        val left = (sourceRectDp.left * density).roundToInt().coerceIn(0, max(0, screenWidthPx - 24))
        val top = (sourceRectDp.top * density).roundToInt().coerceIn(0, max(0, screenHeightPx - 24))
        val width = max(24, (sourceRectDp.width * density).roundToInt()).coerceAtMost(screenWidthPx - left)
        val height = max(24, (sourceRectDp.height * density).roundToInt()).coerceAtMost(screenHeightPx - top)
        return ClipRevealSpec(leftPx = left, topPx = top, widthPx = max(24, width), heightPx = max(24, height))
    }

    fun mergeContextShortcuts(
        defaultShortcuts: List<String>,
        nativeShortcuts: List<NativeShortcutEntry>,
        maxNative: Int = 2,
    ): List<String> {
        val nativeLabels = nativeShortcuts
            .mapNotNull { entry ->
                val clean = entry.label.trim()
                if (clean.isEmpty()) null else "$NATIVE_SHORTCUT_PREFIX$clean"
            }
            .distinct()
            .take(maxNative.coerceAtLeast(0))
        return (nativeLabels + defaultShortcuts).distinct()
    }

    fun isThirdPartyAppId(appId: String?): Boolean =
        appId != null && appId.startsWith("pkg:")

    fun extractPackageFromAppId(appId: String?): String? = AppIdentity.packageOf(appId)

    fun buildAppDetailsSettingsUri(packageName: String): String =
        "package:${packageName.trim()}"
}

/** A package that was really uninstalled (not just paused/locked), per profile. */
data class RemovedPackage(val packageName: String, val userSerial: Long)

/**
 * Runtime controller connecting Android's LauncherApps / UserManager APIs with Canopy's app
 * catalog: all profiles (personal, work, private space), launches, shortcuts and app details.
 */
class DeviceAppsController(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private var refreshJob: Job? = null
    private val launcherApps: LauncherApps? = runCatching {
        context.getSystemService(LauncherApps::class.java)
    }.getOrNull()
    private val userManager: UserManager? = runCatching {
        context.getSystemService(Context.USER_SERVICE) as? UserManager
    }.getOrNull()

    private val _catalogFlow = MutableStateFlow<List<AppDefinition>>(emptyList())
    val catalogFlow: StateFlow<List<AppDefinition>> = _catalogFlow.asStateFlow()

    /** Private space exists but is locked → All Apps shows an "unlock" entry instead of its apps. */
    private val _privateSpaceLocked = MutableStateFlow(false)
    val privateSpaceLocked: StateFlow<Boolean> = _privateSpaceLocked.asStateFlow()
    private var privateSpaceUser: UserHandle? = null

    private val _removedPackages = MutableSharedFlow<RemovedPackage>(extraBufferCapacity = 16)
    /** Emits only for real uninstalls, so tiles are never pruned because a profile is paused. */
    val removedPackages: SharedFlow<RemovedPackage> = _removedPackages.asSharedFlow()

    private val _packageChanged = MutableSharedFlow<String>(extraBufferCapacity = 32)
    /** Packages that were updated/changed — icon caches listen to this. */
    val packageChanged: SharedFlow<String> = _packageChanged.asSharedFlow()

    private var cachedShortcuts: Map<String, List<ShortcutInfo>> = emptyMap()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            if (packageName != null) {
                _removedPackages.tryEmit(RemovedPackage(packageName, serialOf(user)))
                _packageChanged.tryEmit(packageName)
            }
            refreshInstalledApps()
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            packageName?.let { _packageChanged.tryEmit(it) }
            refreshInstalledApps()
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            packageName?.let { _packageChanged.tryEmit(it) }
            refreshInstalledApps()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            packageNames?.forEach { _packageChanged.tryEmit(it) }
            refreshInstalledApps()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            refreshInstalledApps()

        override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) = refreshInstalledApps()

        override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) = refreshInstalledApps()
    }

    /** Work profile paused/resumed or private space locked/unlocked. */
    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) = refreshInstalledApps()
    }

    fun startListening() {
        refreshInstalledApps()
        runCatching { launcherApps?.registerCallback(callback) }
        runCatching {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
                addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    addAction(Intent.ACTION_PROFILE_AVAILABLE)
                    addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                }
            }
            ContextCompat.registerReceiver(context, profileReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    fun stopListening() {
        runCatching { launcherApps?.unregisterCallback(callback) }
        runCatching { context.unregisterReceiver(profileReceiver) }
    }

    /** Re-reads the installed apps off the main thread; bursts of package events are coalesced. */
    fun refreshInstalledApps() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            delay(150L)
            loadInstalledAppsNow()
        }
    }

    private fun serialOf(user: UserHandle?): Long {
        if (user == null || user == Process.myUserHandle()) return 0L
        return runCatching { userManager?.getSerialNumberForUser(user) }.getOrNull() ?: 0L
    }

    fun userFor(app: AppDefinition?): UserHandle {
        val serial = app?.userSerial ?: 0L
        if (serial == 0L) return Process.myUserHandle()
        return runCatching { userManager?.getUserForSerialNumber(serial) }.getOrNull() ?: Process.myUserHandle()
    }

    private fun profileTypeOf(user: UserHandle): AppProfile {
        if (user == Process.myUserHandle()) return AppProfile.PERSONAL
        if (Build.VERSION.SDK_INT >= 35) {
            val type = runCatching { launcherApps?.getLauncherUserInfo(user)?.userType }.getOrNull()
            if (type == UserManager.USER_TYPE_PROFILE_PRIVATE) return AppProfile.PRIVATE
        }
        return AppProfile.WORK
    }

    private fun loadInstalledAppsNow() {
        val nowMs = System.currentTimeMillis()
        val la = launcherApps ?: return
        val profiles = runCatching { la.profiles }.getOrNull().orEmpty().ifEmpty { listOf(Process.myUserHandle()) }
        var privateLocked = false
        privateSpaceUser = null
        val entries = profiles.flatMap { user ->
            val profile = profileTypeOf(user)
            val quiet = runCatching { userManager?.isQuietModeEnabled(user) }.getOrNull() == true
            if (profile == AppProfile.PRIVATE) {
                privateSpaceUser = user
                if (quiet) {
                    privateLocked = true
                    return@flatMap emptyList()
                }
            }
            val serial = serialOf(user)
            runCatching {
                la.getActivityList(null, user).map { info ->
                    val appInfo = info.applicationInfo
                    val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appInfo.category
                    } else {
                        ApplicationInfo.CATEGORY_UNDEFINED
                    }
                    val pkg = info.componentName.packageName
                    val mainActivity = runCatching {
                        context.packageManager.getLaunchIntentForPackage(pkg)?.component?.className
                    }.getOrNull()
                    val isRecentInstall = (nowMs - info.firstInstallTime) in 0L..(7L * 24L * 3600L * 1000L)
                    InstalledAppEntry(
                        packageName = pkg,
                        activityName = info.componentName.className,
                        label = info.label?.toString().orEmpty(),
                        androidCategory = category,
                        isNewInstall = isRecentInstall,
                        isMainActivity = mainActivity == null || mainActivity == info.componentName.className,
                        userSerial = serial,
                        profile = profile,
                        isPaused = quiet || ((appInfo.flags and ApplicationInfo.FLAG_SUSPENDED) != 0),
                    )
                }
            }.getOrDefault(emptyList())
        }

        val merged = InstalledAppsMapper.mergeWithCatalog(
            catalog = emptyList(),
            installed = entries,
            selfPackage = context.packageName ?: "app.canopy.launcher",
        )
        merged.forEach { app ->
            val pkg = app.packageName ?: return@forEach
            AppIconRepository.register(app.id, pkg, app.componentActivityName, userFor(app))
        }
        _privateSpaceLocked.value = privateLocked
        _catalogFlow.value = merged
    }

    /** Unlocks (false) or locks (true) the private space / resumes a paused work profile. */
    fun setProfileQuietMode(profile: AppProfile, quiet: Boolean): Boolean {
        val um = userManager ?: return false
        val target = when (profile) {
            AppProfile.PRIVATE -> privateSpaceUser
            AppProfile.WORK -> runCatching { launcherApps?.profiles }.getOrNull()
                ?.firstOrNull { it != Process.myUserHandle() && profileTypeOf(it) == AppProfile.WORK }
            AppProfile.PERSONAL -> null
        } ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) um.requestQuietModeEnabled(quiet, target) else false
        }.getOrDefault(false).also { refreshInstalledApps() }
    }

    fun hasShortcutPermission(): Boolean =
        runCatching { launcherApps?.hasShortcutHostPermission() }.getOrNull() == true

    fun queryShortcutsForApp(appDef: AppDefinition?): List<NativeShortcutEntry> {
        val pkg = appDef?.packageName ?: return emptyList()
        val la = launcherApps ?: return emptyList()
        if (!hasShortcutPermission()) return emptyList() // only the default launcher may read shortcuts
        val shortcuts = runCatching {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(pkg)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            la.getShortcuts(query, userFor(appDef)).orEmpty()
                .filter { it.isEnabled }
                .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
        }.getOrDefault(emptyList())

        if (shortcuts.isNotEmpty()) {
            cachedShortcuts = cachedShortcuts + (appDef.id to shortcuts)
        }
        return shortcuts.mapNotNull { info ->
            val label = (info.shortLabel ?: info.longLabel)?.toString()?.trim().orEmpty()
            if (label.isEmpty()) null else NativeShortcutEntry(info.id, pkg, label)
        }
    }

    fun launchNativeShortcut(
        rootView: View?,
        appDef: AppDefinition?,
        shortcutLabelWithPrefix: String,
        sourceRectPx: Rect?,
        screenWidthPx: Int,
        screenHeightPx: Int,
        shortcutId: String? = null,
    ): Boolean {
        appDef ?: return false
        val cleanLabel = shortcutLabelWithPrefix.removePrefix(InstalledAppsMapper.NATIVE_SHORTCUT_PREFIX).trim()
        val shortcuts = cachedShortcuts[appDef.id].orEmpty()
        val target = shortcuts.firstOrNull { shortcutId != null && it.id == shortcutId }
            ?: shortcuts.firstOrNull {
                (it.shortLabel?.toString() == cleanLabel) || (it.longLabel?.toString() == cleanLabel)
            } ?: return false
        val la = launcherApps ?: return false
        val (bounds, bundle) = revealOptions(rootView, sourceRectPx, screenWidthPx, screenHeightPx)
        return runCatching {
            la.startShortcut(target, bounds, bundle)
            true
        }.getOrDefault(false)
    }

    private fun revealOptions(
        rootView: View?,
        sourceRectPx: Rect?,
        screenWidthPx: Int,
        screenHeightPx: Int,
    ): Pair<AndroidRect?, android.os.Bundle?> {
        // Bounds come from Compose boundsInRoot() and are already in pixels.
        val reveal = InstalledAppsMapper.computeClipRevealSpec(sourceRectPx, 1f, screenWidthPx, screenHeightPx)
        val bundle = if (rootView != null && reveal != null) {
            ActivityOptions.makeClipRevealAnimation(rootView, reveal.leftPx, reveal.topPx, reveal.widthPx, reveal.heightPx).toBundle()
        } else null
        val bounds = reveal?.let { AndroidRect(it.leftPx, it.topPx, it.leftPx + it.widthPx, it.topPx + it.heightPx) }
        return bounds to bundle
    }

    /**
     * Starts an app in its own profile. [sourceRectPx] is the tapped element in root pixels and
     * drives the clip-reveal animation.
     */
    fun launchApp(
        rootView: View?,
        appDef: AppDefinition?,
        sourceRectPx: Rect?,
        screenWidthPx: Int,
        screenHeightPx: Int,
        shortcutLabel: String? = null,
    ): Boolean {
        if (appDef == null || appDef.packageName.isNullOrBlank()) return false
        if (shortcutLabel != null && shortcutLabel.startsWith(InstalledAppsMapper.NATIVE_SHORTCUT_PREFIX)) {
            if (launchNativeShortcut(rootView, appDef, shortcutLabel, sourceRectPx, screenWidthPx, screenHeightPx)) {
                return true
            }
        }
        val (bounds, bundle) = revealOptions(rootView, sourceRectPx, screenWidthPx, screenHeightPx)
        val pkg = appDef.packageName
        val act = appDef.componentActivityName
        val la = launcherApps
        if (la != null && !act.isNullOrBlank()) {
            val started = runCatching {
                la.startMainActivity(ComponentName(pkg, act), userFor(appDef), bounds, bundle)
                true
            }.getOrDefault(false)
            if (started) return true
        }
        if (appDef.userSerial != 0L) return false // never fall back to the personal copy of a work app

        return runCatching {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: return@runCatching false
            context.startActivity(launchIntent, bundle)
            true
        }.getOrDefault(false)
    }

    /** Opens Android's app info screen for the app's own profile. */
    fun openSystemAppDetails(app: AppDefinition?): Boolean {
        val pkg = app?.packageName?.trim().orEmpty()
        if (pkg.isEmpty()) return false
        val act = app?.componentActivityName
        if (!act.isNullOrBlank()) {
            val ok = runCatching {
                launcherApps?.startAppDetailsActivity(ComponentName(pkg, act), userFor(app), null, null)
                launcherApps != null
            }.getOrDefault(false)
            if (ok) return true
        }
        return openSystemAppDetails(pkg)
    }

    fun openSystemAppDetails(packageName: String?): Boolean {
        val cleanPkg = packageName?.trim().orEmpty()
        if (cleanPkg.isEmpty()) return false
        return runCatching {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse(InstalledAppsMapper.buildAppDetailsSettingsUri(cleanPkg)),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /** Asks Android to uninstall the app (system confirmation dialog). */
    fun requestUninstall(app: AppDefinition?): Boolean {
        val pkg = app?.packageName ?: return false
        return runCatching {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (app.userSerial != 0L) putExtra(Intent.EXTRA_USER, userFor(app))
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
