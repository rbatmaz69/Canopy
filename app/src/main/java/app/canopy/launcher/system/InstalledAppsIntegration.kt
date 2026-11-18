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

        return validInstalled
            .distinctBy { it.packageName }
            .map { entry ->
                val hue = stableHueForPackage(entry.packageName)
                AppDefinition(
                    id = "pkg:${entry.packageName}",
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

        val fallbackPool = apps.map { it.id }
        var fallbackCursor = 0
        fun nextFallback(exclude: Set<String>): String? {
            repeat(fallbackPool.size) {
                val cand = fallbackPool[fallbackCursor % fallbackPool.size]
                fallbackCursor += 1
                if (cand !in exclude) return cand
            }
            return fallbackPool.firstOrNull()
        }

        fun remapNode(node: app.canopy.launcher.layout.TreeNode): app.canopy.launcher.layout.TreeNode = when (node) {
            is LeafNode -> {
                val tile = node.tile
                val mappedAppId = tile.appId?.let { oldId ->
                    pickForMockId(oldId) ?: apps.firstOrNull()?.id
                }
                val mappedGroupIds = tile.appIds?.let { ids ->
                    val resolved = mutableListOf<String>()
                    val used = mutableSetOf<String>()
                    ids.forEach { oldId ->
                        val picked = pickForMockId(oldId)?.takeIf { it !in used } ?: nextFallback(used)
                        if (picked != null && used.add(picked)) {
                            resolved += picked
                        }
                    }
                    resolved
                }
                val mappedNewIds = if (tile.type == TileType.NEWLY_INSTALLED) {
                    val newest = apps.filter { it.isNew }.map { it.id }.ifEmpty { apps.take(4).map { it.id } }
                    newest.take(4)
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

    fun extractPackageFromAppId(appId: String?): String? =
        if (isThirdPartyAppId(appId)) appId!!.removePrefix("pkg:").trim().takeIf { it.isNotEmpty() } else null

    fun buildAppDetailsSettingsUri(packageName: String): String =
        "package:${packageName.trim()}"
}

/**
 * Thread-safe in-memory cache converting installed Android application Drawables
 * into Compose ImageBitmaps for native squircle icon rendering.
 */
object InstalledAppIconCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun getOrLoad(context: Context, packageName: String): ImageBitmap? {
        if (packageName.isBlank()) return null
        cache[packageName]?.let { return it }
        val loaded = runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val sizePx = 96
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }.getOrNull() ?: return null
        cache[packageName] = loaded
        return loaded
    }
}

/**
 * Runtime controller connecting Android's LauncherApps / PackageManager APIs
 * with Canopy's merged app catalog and Clip-Reveal / Shortcut execution.
 */
class DeviceAppsController(private val context: Context) {
    private val launcherApps: LauncherApps? = runCatching {
        context.getSystemService(LauncherApps::class.java)
    }.getOrNull()

    private val _catalogFlow = MutableStateFlow<List<AppDefinition>>(emptyList())
    val catalogFlow: StateFlow<List<AppDefinition>> = _catalogFlow.asStateFlow()

    private var cachedShortcuts: Map<String, List<ShortcutInfo>> = emptyMap()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refreshInstalledApps()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refreshInstalledApps()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refreshInstalledApps()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            refreshInstalledApps()

        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            refreshInstalledApps()
    }

    fun startListening() {
        refreshInstalledApps()
        runCatching { launcherApps?.registerCallback(callback) }
    }

    fun stopListening() {
        runCatching { launcherApps?.unregisterCallback(callback) }
    }

    fun refreshInstalledApps() {
        val nowMs = System.currentTimeMillis()
        val entries = runCatching {
            val la = launcherApps ?: return@runCatching emptyList()
            la.getActivityList(null, Process.myUserHandle()).map { info ->
                val appInfo = info.applicationInfo
                val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category
                } else {
                    ApplicationInfo.CATEGORY_UNDEFINED
                }
                val isRecentInstall = (nowMs - info.firstInstallTime) in 0L..(7L * 24L * 3600L * 1000L)
                InstalledAppEntry(
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    label = info.label?.toString().orEmpty(),
                    androidCategory = category,
                    isNewInstall = isRecentInstall,
                )
            }
        }.getOrDefault(emptyList())

        _catalogFlow.value = InstalledAppsMapper.mergeWithCatalog(
            catalog = MockCatalogData.APPS_CATALOG,
            installed = entries,
            selfPackage = context.packageName ?: "app.canopy.launcher",
        )
    }

    fun queryShortcutsForApp(appDef: AppDefinition?): List<NativeShortcutEntry> {
        val pkg = appDef?.packageName ?: return emptyList()
        val la = launcherApps ?: return emptyList()
        val shortcuts = runCatching {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(pkg)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )
            la.getShortcuts(query, Process.myUserHandle()).orEmpty()
        }.getOrDefault(emptyList())

        if (shortcuts.isNotEmpty()) {
            cachedShortcuts = cachedShortcuts + (pkg to shortcuts)
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
        sourceRectDp: Rect?,
        density: Float,
        screenWidthPx: Int,
        screenHeightPx: Int,
    ): Boolean {
        val pkg = appDef?.packageName ?: return false
        val cleanLabel = shortcutLabelWithPrefix.removePrefix(InstalledAppsMapper.NATIVE_SHORTCUT_PREFIX).trim()
        val shortcuts = cachedShortcuts[pkg].orEmpty()
        val target = shortcuts.firstOrNull {
            (it.shortLabel?.toString() == cleanLabel) || (it.longLabel?.toString() == cleanLabel)
        } ?: return false
        val la = launcherApps ?: return false

        val reveal = InstalledAppsMapper.computeClipRevealSpec(sourceRectDp, density, screenWidthPx, screenHeightPx)
        val bundle = if (rootView != null && reveal != null) {
            ActivityOptions.makeClipRevealAnimation(
                rootView,
                reveal.leftPx,
                reveal.topPx,
                reveal.widthPx,
                reveal.heightPx,
            ).toBundle()
        } else null
        val bounds = reveal?.let {
            AndroidRect(it.leftPx, it.topPx, it.leftPx + it.widthPx, it.topPx + it.heightPx)
        }
        return runCatching {
            la.startShortcut(target, bounds, bundle)
            true
        }.getOrDefault(false)
    }

    fun launchApp(
        rootView: View?,
        appDef: AppDefinition?,
        sourceRectDp: Rect?,
        density: Float,
        screenWidthPx: Int,
        screenHeightPx: Int,
        shortcutLabel: String? = null,
    ): Boolean {
        if (appDef == null || appDef.packageName.isNullOrBlank()) return false
        if (shortcutLabel != null && shortcutLabel.startsWith(InstalledAppsMapper.NATIVE_SHORTCUT_PREFIX)) {
            if (launchNativeShortcut(rootView, appDef, shortcutLabel, sourceRectDp, density, screenWidthPx, screenHeightPx)) {
                return true
            }
        }

        val reveal = InstalledAppsMapper.computeClipRevealSpec(sourceRectDp, density, screenWidthPx, screenHeightPx)
        val bundle = if (rootView != null && reveal != null) {
            ActivityOptions.makeClipRevealAnimation(
                rootView,
                reveal.leftPx,
                reveal.topPx,
                reveal.widthPx,
                reveal.heightPx,
            ).toBundle()
        } else null

        val pkg = appDef.packageName
        val act = appDef.componentActivityName
        val la = launcherApps
        if (la != null && !act.isNullOrBlank()) {
            val started = runCatching {
                val bounds = reveal?.let {
                    AndroidRect(it.leftPx, it.topPx, it.leftPx + it.widthPx, it.topPx + it.heightPx)
                }
                la.startMainActivity(ComponentName(pkg, act), Process.myUserHandle(), bounds, bundle)
                true
            }.getOrDefault(false)
            if (started) return true
        }

        return runCatching {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: return@runCatching false
            context.startActivity(launchIntent, bundle)
            true
        }.getOrDefault(false)
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
}
