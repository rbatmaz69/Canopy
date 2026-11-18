package app.canopy.launcher.ui

import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.canopy.launcher.data.AlarmState
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.SharedPreferencesLauncherStore
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.layout.DropZone
import app.canopy.launcher.layout.EditHistoryState
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.RectFBounds
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.CanopySprings
import app.canopy.launcher.motion.HapticEvent
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.motion.HapticVisualFeedbackLayer
import app.canopy.launcher.motion.SpringRole
import app.canopy.launcher.system.DeviceAppsController
import app.canopy.launcher.system.HomeRoleAndBackupController
import app.canopy.launcher.system.LiveHardwareController
import app.canopy.launcher.system.LiveMediaSessionSnapshot
import app.canopy.launcher.system.LiveWeatherController
import app.canopy.launcher.system.LocalHardwareTelemetry
import app.canopy.launcher.system.LocalWeatherSnapshot
import app.canopy.launcher.system.NativeWidgetProviderSpec
import app.canopy.launcher.system.NotificationBadgeStore
import app.canopy.launcher.system.SystemDataAndWidgetEngine
import app.canopy.launcher.system.SystemDataProvider
import app.canopy.launcher.ui.components.BottomDockBar
import app.canopy.launcher.ui.components.IconStyleConfig
import app.canopy.launcher.ui.components.LocalIconStyle
import app.canopy.launcher.ui.navigation.HorizontalTilingPager
import app.canopy.launcher.ui.navigation.PagingPhysics
import app.canopy.launcher.ui.navigation.ViewportInsetsSpec
import app.canopy.launcher.ui.navigation.WindowInsetsAndWallpaperMath
import app.canopy.launcher.ui.overlays.AppGroupOverlay
import app.canopy.launcher.ui.overlays.CatalogAndPageManager
import app.canopy.launcher.ui.overlays.ContextMenuOverlay
import app.canopy.launcher.ui.overlays.ContextMenuState
import app.canopy.launcher.ui.overlays.DeveloperMenuSheet
import app.canopy.launcher.ui.overlays.EditToolbar
import app.canopy.launcher.ui.overlays.FakeAppOverlay
import app.canopy.launcher.ui.overlays.FakeAppTarget
import app.canopy.launcher.ui.overlays.FakeAppTargetKind
import app.canopy.launcher.ui.overlays.GroupOverlayMode
import app.canopy.launcher.ui.overlays.LauncherSettingsBridge
import app.canopy.launcher.ui.overlays.PageOverviewModal
import app.canopy.launcher.ui.overlays.QuickSettingsNotificationShade
import app.canopy.launcher.ui.overlays.TileCatalogSheet
import app.canopy.launcher.ui.overlays.UniversalSearchEngine
import app.canopy.launcher.ui.overlays.UniversalSearchOverlay
import app.canopy.launcher.ui.pages.ActiveTileDrag
import app.canopy.launcher.ui.pages.DragSourceKind
import app.canopy.launcher.ui.pages.EdgeFlipSide
import app.canopy.launcher.ui.pages.FloatingDragGhostOverlay
import app.canopy.launcher.ui.pages.TileDragEngine
import app.canopy.launcher.ui.pages.TilingPageCanvas
import app.canopy.launcher.ui.pages.UndoSnapshot
import app.canopy.launcher.ui.pages.UndoTileRemovalBanner
import app.canopy.launcher.ui.theme.CanopyThemes
import app.canopy.launcher.ui.theme.LocalCanopyTheme
import app.canopy.launcher.ui.theme.WallpaperCanvas
import app.canopy.launcher.ui.tiles.AllAppsCatalogSorter
import app.canopy.launcher.ui.tiles.LocalAppWidgetHostFactory
import app.canopy.launcher.ui.tiles.MediaPlayerState
import app.canopy.launcher.ui.tiles.SystemControlState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val APP_WIDGET_HOST_ID = 0xCA90

@Composable
fun CanopyLauncherApp(
    homeIntentSignal: Long = 0L,
    onSystemBarAppearance: (useLightIcons: Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val homeRoleAndBackupController = remember(context) { HomeRoleAndBackupController(context) }
    var isDefaultHome by remember { mutableStateOf(homeRoleAndBackupController.isDefaultHome()) }
    var dismissDefaultHomeBanner by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    val defaultLauncherRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultHome = homeRoleAndBackupController.isDefaultHome()
        backupStatusMessage = if (isDefaultHome) "Canopy ist als Standard-Launcher aktiv." else "Standard-Launcher-Auswahl geprüft."
    }

    val deviceAppsController = remember(context) { DeviceAppsController(context) }
    DisposableEffect(deviceAppsController) {
        deviceAppsController.startListening()
        onDispose { deviceAppsController.stopListening() }
    }
    val liveAppsCatalog by deviceAppsController.catalogFlow.collectAsState()

    val hardwareController = remember(context) { LiveHardwareController(context) }
    DisposableEffect(hardwareController) {
        hardwareController.start()
        onDispose { hardwareController.stop() }
    }
    val hardwareTelemetry by hardwareController.telemetryFlow.collectAsState()

    val weatherController = remember { LiveWeatherController() }
    val weatherSnapshot by weatherController.weatherFlow.collectAsState()

    val systemDataProvider = remember(context) { SystemDataProvider(context) }
    val nativeWidgetProviders = remember(systemDataProvider) { systemDataProvider.loadInstalledWidgetProviders() }
    val deviceContacts = remember(systemDataProvider) {
        SystemDataAndWidgetEngine.mergeContacts(systemDataProvider.loadDeviceContacts())
    }

    val store = remember(context) { SharedPreferencesLauncherStore(context) }
    val initialState = remember {
        store.load() ?: PersistedLauncherState(pages = MockCatalogData.createInitialPages(ShortcutBarMode.ALL))
    }

    var shortcutBarMode by remember { mutableStateOf(initialState.shortcutBarMode) }
    var pages by remember { mutableStateOf(initialState.pages) }
    var homePageId by remember { mutableStateOf(initialState.homePageId) }
    var themeId by remember { mutableStateOf(initialState.themeId) }
    var colorMode by remember { mutableStateOf(initialState.colorMode) }
    var wallpaperId by remember { mutableStateOf(initialState.wallpaperId) }
    var tonalIcons by remember { mutableStateOf(initialState.tonalIcons) }
    var motionSettings by remember { mutableStateOf(initialState.motionSettings) }
    var alarmState by remember { mutableStateOf(initialState.alarmState) }
    var useSystemWallpaper by remember { mutableStateOf(initialState.useSystemWallpaper) }
    var appUsageCounts by remember { mutableStateOf(initialState.appUsageCounts) }
    var customCalendarEvents by remember { mutableStateOf(initialState.customCalendarEvents) }
    var weatherCityId by remember { mutableStateOf(initialState.weatherCityId) }
    var schemaVersion by remember { mutableIntStateOf(initialState.schemaVersion) }
    var didRealAppMigration by remember { mutableStateOf(initialState.schemaVersion >= PersistedLauncherState.CURRENT_SCHEMA_VERSION && store.load() != null) }

    LaunchedEffect(liveAppsCatalog, didRealAppMigration) {
        if (!didRealAppMigration && liveAppsCatalog.isNotEmpty()) {
            pages = app.canopy.launcher.system.InstalledAppsMapper.resolveRealAppIdsForPages(liveAppsCatalog, pages)
            schemaVersion = PersistedLauncherState.CURRENT_SCHEMA_VERSION
            didRealAppMigration = true
        }
    }

    LaunchedEffect(weatherController, weatherCityId) {
        weatherController.refreshForCity(weatherCityId)
    }
    val boostedAppsCatalog = remember(liveAppsCatalog, appUsageCounts) {
        AllAppsCatalogSorter.applyUsageBoost(liveAppsCatalog, appUsageCounts)
    }

    val baseCalendarEvents = remember(systemDataProvider) {
        SystemDataAndWidgetEngine.mergeCalendarEvents(systemDataProvider.loadTodayCalendarEvents())
    }
    val calendarEvents = remember(customCalendarEvents, baseCalendarEvents) {
        customCalendarEvents + baseCalendarEvents
    }
    val notifSyncState by NotificationBadgeStore.state.collectAsState()

    val startIdx = remember {
        pages.indexOfFirst { it.id == homePageId }.coerceAtLeast(0)
    }
    var activePageIndex by remember { mutableIntStateOf(startIdx) }

    // Persist launcher state changes automatically
    val currentSnapshot = PersistedLauncherState(
        pages = pages,
        homePageId = homePageId,
        themeId = themeId,
        colorMode = colorMode,
        wallpaperId = wallpaperId,
        tonalIcons = tonalIcons,
        shortcutBarMode = shortcutBarMode,
        motionSettings = motionSettings,
        alarmState = alarmState,
        useSystemWallpaper = useSystemWallpaper,
        appUsageCounts = appUsageCounts,
        customCalendarEvents = customCalendarEvents,
        weatherCityId = weatherCityId,
        schemaVersion = schemaVersion,
    )
    LaunchedEffect(currentSnapshot) {
        store.save(currentSnapshot)
    }

    // Native AppWidgetHost lifecycle
    val appWidgetHost = remember(context) { runCatching { AppWidgetHost(context, APP_WIDGET_HOST_ID) }.getOrNull() }
    val appWidgetManager = remember(context) { runCatching { AppWidgetManager.getInstance(context) }.getOrNull() }
    DisposableEffect(appWidgetHost) {
        runCatching { appWidgetHost?.startListening() }
        onDispose { runCatching { appWidgetHost?.stopListening() } }
    }

    // UI Overlays & Mode State
    var isEditMode by remember { mutableStateOf(false) }
    var showDevMenu by remember { mutableStateOf(false) }
    var showHapticHud by remember { mutableStateOf(false) }
    var showFpsHud by remember { mutableStateOf(false) }
    var showGestureGuide by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showQuickShade by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var showPageOverview by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf<ContextMenuState?>(null) }
    var openGroupState by remember { mutableStateOf<Triple<String, String, GroupOverlayMode>?>(null) }
    var fakeApp by remember { mutableStateOf<FakeAppTarget?>(null) }
    var undoSnapshot by remember { mutableStateOf<UndoSnapshot?>(null) }
    var activeDrag by remember { mutableStateOf<ActiveTileDrag?>(null) }
    var editHistory by remember { mutableStateOf(EditHistoryState()) }
    var lastDividerRecordMs by remember { mutableLongStateOf(0L) }
    val updatePagesWithHistory: (List<LauncherPage>) -> Unit = { next ->
        if (next != pages) {
            editHistory = editHistory.recordBeforeMutation(pages)
            pages = next
        }
    }

    // 5-second auto-clear for Undo Banner
    LaunchedEffect(undoSnapshot) {
        if (undoSnapshot != null) {
            delay(TileDragEngine.UNDO_WINDOW_MS)
            undoSnapshot = null
        }
    }

    // Shared Media Player State (bridges live MediaSessionManager when permitted, otherwise smooth demo tracks)
    var trackIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var mediaProgress by remember { mutableFloatStateOf(0.36f) }
    var liveMediaSnapshot by remember { mutableStateOf<LiveMediaSessionSnapshot?>(null) }

    LaunchedEffect(systemDataProvider) {
        while (true) {
            liveMediaSnapshot = systemDataProvider.readActiveMediaSnapshot()
            delay(1500L)
        }
    }

    LaunchedEffect(isPlaying, liveMediaSnapshot) {
        if (liveMediaSnapshot == null) {
            while (isPlaying) {
                delay(800L)
                mediaProgress = if (mediaProgress >= 0.995f) 0f else mediaProgress + 0.004f
            }
        }
    }

    val liveMapped = remember(liveMediaSnapshot) {
        liveMediaSnapshot?.let { SystemDataAndWidgetEngine.mapMediaSnapshotToTrack(it) }
    }
    val activeController = remember(liveMediaSnapshot) {
        if (liveMediaSnapshot != null) systemDataProvider.queryActiveMediaController() else null
    }

    val mediaState = MediaPlayerState(
        trackIndex = trackIndex,
        isPlaying = liveMediaSnapshot?.isPlaying ?: isPlaying,
        progress = liveMapped?.second ?: mediaProgress,
        overrideTrack = liveMapped?.first,
        onTogglePlay = {
            val ctrl = activeController
            if (ctrl != null) {
                runCatching {
                    if (liveMediaSnapshot?.isPlaying == true) ctrl.transportControls.pause()
                    else ctrl.transportControls.play()
                }
            } else {
                isPlaying = !isPlaying
            }
        },
        onPrev = {
            val ctrl = activeController
            if (ctrl != null) {
                runCatching { ctrl.transportControls.skipToPrevious() }
            } else {
                trackIndex = (trackIndex - 1 + SystemTileDefaults.MEDIA_TRACKS.size) % SystemTileDefaults.MEDIA_TRACKS.size
                mediaProgress = 0.08f
            }
        },
        onNext = {
            val ctrl = activeController
            if (ctrl != null) {
                runCatching { ctrl.transportControls.skipToNext() }
            } else {
                trackIndex = (trackIndex + 1) % SystemTileDefaults.MEDIA_TRACKS.size
                mediaProgress = 0.08f
            }
        },
        onSeek = { ratio ->
            val ctrl = activeController
            val snap = liveMediaSnapshot
            if (ctrl != null && snap != null) {
                val targetMs = (ratio.coerceIn(0f, 1f) * snap.durationMs.coerceAtLeast(1000L)).toLong()
                runCatching { ctrl.transportControls.seekTo(targetMs) }
            } else {
                mediaProgress = ratio.coerceIn(0f, 1f)
            }
        },
    )

    var systemState by remember { mutableStateOf(SystemControlState()) }
    systemState = systemState.copy(
        onToggle = { key ->
            val next = NotificationBadgeStore.toggleSystemKey(systemState, key)
            systemState = next
            if (key == app.canopy.launcher.ui.tiles.SystemToggleKey.FLASHLIGHT) {
                hardwareController.setTorchEnabled(next.flashlight)
            }
        },
        onChangeBrightness = { v ->
            val clamped = NotificationBadgeStore.clampWindowBrightness(v)
            systemState = systemState.copy(brightness = clamped)
            runCatching {
                val win = (context as? Activity)?.window
                if (win != null) {
                    val lp = win.attributes
                    lp.screenBrightness = clamped
                    win.attributes = lp
                }
            }
        },
    )

    // Latest Haptic Event stream
    var latestHaptic by remember { mutableStateOf<HapticEvent?>(null) }
    DisposableEffect(Unit) {
        val unsub = CanopyHaptics.subscribe { latestHaptic = it }
        onDispose { unsub() }
    }

    val systemWallpaperHue = remember(context, useSystemWallpaper) {
        if (!useSystemWallpaper) null
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                val color = WallpaperManager.getInstance(context)
                    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    ?.primaryColor
                color?.let { c ->
                    CanopyThemes.rgbToHue(
                        (c.red() * 255f).toInt(),
                        (c.green() * 255f).toInt(),
                        (c.blue() * 255f).toInt(),
                    )
                }
            }.getOrNull()
        } else null
    }

    val theme = CanopyThemes.getThemeTokens(themeId, colorMode, wallpaperId, overrideBaseHue = systemWallpaperHue)
    LaunchedEffect(theme.mode, theme.isGlass, useSystemWallpaper) {
        onSystemBarAppearance(theme.mode == ColorMode.DARK || theme.isGlass || useSystemWallpaper)
    }
    val iconStyle = IconStyleConfig(
        tonalIcons = tonalIcons,
        themeId = themeId,
        colorMode = colorMode,
        clearedBadgeAppIds = notifSyncState.clearedBadgeAppIds,
    )

    val showProceduralWallpaper = WindowInsetsAndWallpaperMath.shouldRenderProceduralWallpaper(useSystemWallpaper)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showProceduralWallpaper) Color(0xFF0F1117) else Color.Transparent)
    ) {
        val layoutDirection = LocalLayoutDirection.current
        val statusTopDp = with(density) { WindowInsets.statusBars.getTop(this).toDp().value }
        val navBottomDp = with(density) { WindowInsets.navigationBars.getBottom(this).toDp().value }
        val cutoutLeftDp = with(density) { WindowInsets.displayCutout.getLeft(this, layoutDirection).toDp().value }
        val cutoutRightDp = with(density) { WindowInsets.displayCutout.getRight(this, layoutDirection).toDp().value }

        val screenWidthDp = maxWidth.value
        val screenHeightDp = maxHeight.value
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val viewport = WindowInsetsAndWallpaperMath.computeViewportMetrics(
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            edgePaddingDp = theme.edgePaddingDp,
            insets = ViewportInsetsSpec(
                statusBarTopDp = statusTopDp,
                navBarBottomDp = navBottomDp,
                cutoutLeftDp = cutoutLeftDp,
                cutoutRightDp = cutoutRightDp,
            ),
        )
        val statusBarH = viewport.statusBarHeightDp
        val bottomAreaH = viewport.bottomDockHeightDp
        val tilingAreaHeightDp = viewport.tilingAreaHeightDp
        val pageBoundsDp = viewport.pageBoundsDp

        val pageOffsetAnim = remember { Animatable(-startIdx * screenWidthPx) }

        LaunchedEffect(screenWidthPx, pages.size) {
            val clamped = activePageIndex.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
            activePageIndex = clamped
            pageOffsetAnim.snapTo(-clamped * screenWidthPx)
        }

        val snapToPage: (Int, Float) -> Unit = { targetIdx, velocityPxPerSec ->
            val clamped = targetIdx.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
            activePageIndex = clamped
            scope.launch {
                pageOffsetAnim.animateTo(
                    targetValue = -clamped * screenWidthPx,
                    initialVelocity = velocityPxPerSec,
                    animationSpec = CanopySprings.spec(SpringRole.PAGE_SWIPE, motionSettings),
                )
            }
        }

        val openAppWithBadgeSync: (String, Rect?, String?) -> Unit = { appId, sourceRect, shortcut ->
            NotificationBadgeStore.onAppOpened(appId)
            appUsageCounts = AllAppsCatalogSorter.incrementUsageCount(appUsageCounts, appId)
            val appDef = boostedAppsCatalog.firstOrNull { it.id == appId } ?: MockCatalogData.APPS_BY_ID[appId]
            val launchedReal = deviceAppsController.launchApp(
                rootView = rootView,
                appDef = appDef,
                sourceRectDp = sourceRect,
                density = density.density,
                screenWidthPx = screenWidthPx.toInt(),
                screenHeightPx = screenHeightPx.toInt(),
                shortcutLabel = shortcut,
            )

            if (!launchedReal) {
                fakeApp = FakeAppTarget(
                    kind = FakeAppTargetKind.APP,
                    appId = appId,
                    sourceRect = sourceRect,
                    shortcutAction = shortcut,
                )
            }
        }

        val handleBackAction: () -> Unit = {
            when {
                fakeApp != null -> fakeApp = null
                openGroupState != null -> openGroupState = null
                contextMenu != null -> contextMenu = null
                showSearch -> showSearch = false
                showCatalog -> showCatalog = false
                showPageOverview -> showPageOverview = false
                showQuickShade -> showQuickShade = false
                showGestureGuide -> showGestureGuide = false
                showDevMenu -> showDevMenu = false
                isEditMode -> isEditMode = false
            }
        }

        val handleHomeAction: () -> Unit = {
            if (fakeApp != null || openGroupState != null || contextMenu != null ||
                showSearch || showCatalog || showPageOverview || showQuickShade || showDevMenu || isEditMode
            ) {
                handleBackAction()
            } else {
                val homeIdx = pages.indexOfFirst { it.id == homePageId }.coerceAtLeast(0)
                snapToPage(homeIdx, 0f)
            }
        }

        LaunchedEffect(homeIntentSignal) {
            if (homeIntentSignal > 0L) {
                fakeApp = null
                openGroupState = null
                contextMenu = null
                showSearch = false
                showCatalog = false
                showPageOverview = false
                showQuickShade = false
                showDevMenu = false
                isEditMode = false
                val homeIdx = pages.indexOfFirst { it.id == homePageId }.coerceAtLeast(0)
                snapToPage(homeIdx, 0f)
            }
        }

        val hasAnyOverlay = fakeApp != null || openGroupState != null || showSearch ||
            contextMenu != null || showCatalog || showPageOverview || showQuickShade || showGestureGuide || showDevMenu

        BackHandler(enabled = hasAnyOverlay || isEditMode) {
            handleBackAction()
        }

        // Active drop preview during drag
        val activePage = pages.getOrNull(activePageIndex)
        val gapDp = if (isEditMode) theme.gapEditDp else theme.gapUseDp

        // Pending AppWidget bind + optional configure flow
        var pendingWidgetBind by remember { mutableStateOf<Pair<Int, NativeWidgetProviderSpec>?>(null) }
        var pendingWidgetConfigure by remember { mutableStateOf<Pair<Int, NativeWidgetProviderSpec>?>(null) }
        val insertBoundWidget: (Int, NativeWidgetProviderSpec) -> Unit = { widgetId, spec ->
            val targetPage = pages.getOrNull(activePageIndex)
            if (targetPage != null && targetPage.kind != PageKind.ALL_APPS) {
                val matchedAppId = liveAppsCatalog.firstOrNull { it.packageName == spec.providerPackage }?.id
                val widgetTile = SystemDataAndWidgetEngine.buildWidgetTileFromProvider(spec, widgetId, matchedAppId)
                updatePagesWithHistory(
                    pages.map { p ->
                        if (p.id == targetPage.id) {
                            CatalogAndPageManager.insertCatalogTileIntoLargestLeaf(p, widgetTile, pageBoundsDp, gapDp)
                        } else p
                    }
                )
            }
            showCatalog = false
        }
        val widgetConfigureLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val pending = pendingWidgetConfigure
            pendingWidgetConfigure = null
            if (pending != null) {
                val (widgetId, spec) = pending
                if (result.resultCode == Activity.RESULT_OK) {
                    insertBoundWidget(widgetId, spec)
                } else {
                    runCatching { appWidgetHost?.deleteAppWidgetId(widgetId) }
                }
            }
        }
        val completeBoundWidgetOrConfigure: (Int, NativeWidgetProviderSpec) -> Unit = { widgetId, spec ->
            val info = runCatching { appWidgetManager?.getAppWidgetInfo(widgetId) }.getOrNull()
            val configureComponent = info?.configure
            if (configureComponent != null) {
                pendingWidgetConfigure = widgetId to spec
                val cfgIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = configureComponent
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                val launched = runCatching {
                    widgetConfigureLauncher.launch(cfgIntent)
                    true
                }.getOrDefault(false)
                if (!launched) {
                    pendingWidgetConfigure = null
                    insertBoundWidget(widgetId, spec)
                }
            } else {
                insertBoundWidget(widgetId, spec)
            }
        }
        val widgetBindLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val pending = pendingWidgetBind
            pendingWidgetBind = null
            if (pending != null) {
                val (widgetId, spec) = pending
                if (result.resultCode == Activity.RESULT_OK) {
                    completeBoundWidgetOrConfigure(widgetId, spec)
                } else {
                    runCatching { appWidgetHost?.deleteAppWidgetId(widgetId) }
                }
            }
        }
        val dropPreview = activeDrag?.let { drag ->
            val pointerXDp = with(density) { drag.pointerXRootPx.toDp().value }
            val pointerYDp = with(density) { (drag.pointerYRootPx - with(density) { statusBarH.dp.toPx() }).toDp().value }
            TileDragEngine.computeDropPreview(drag, activePage, pageBoundsDp, gapDp, pointerXDp, pointerYDp)
        }

        // Edge-hold auto page flip during tile drag
        LaunchedEffect(activeDrag?.pointerXRootPx, activePageIndex) {
            val drag = activeDrag ?: return@LaunchedEffect
            val pointerXDp = with(density) { drag.pointerXRootPx.toDp().value }
            val side = TileDragEngine.resolveEdgeFlipDirection(pointerXDp, screenWidthDp, activePageIndex, pages)
                ?: return@LaunchedEffect
            delay(TileDragEngine.EDGE_HOLD_DELAY_MS)
            CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
            val nextIdx = if (side == EdgeFlipSide.LEFT) activePageIndex - 1 else activePageIndex + 1
            snapToPage(nextIdx, 0f)
        }

        CompositionLocalProvider(
            LocalCanopyTheme provides theme,
            LocalIconStyle provides iconStyle,
            LocalHardwareTelemetry provides hardwareTelemetry,
            LocalWeatherSnapshot provides weatherSnapshot,
            LocalAppWidgetHostFactory provides { widgetId ->
                val info = appWidgetManager?.getAppWidgetInfo(widgetId)
                if (info != null && appWidgetHost != null) {
                    appWidgetHost.createView(context, widgetId, info)
                } else {
                    null
                }
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeDrag != null) {
                        if (activeDrag == null) return@pointerInput
                        val velocityTracker = VelocityTracker()
                        detectDragGestures(
                            onDragStart = { offset ->
                                velocityTracker.resetTracking()
                                velocityTracker.addPosition(System.currentTimeMillis(), offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                activeDrag = activeDrag?.copy(
                                    pointerXRootPx = change.position.x,
                                    pointerYRootPx = change.position.y,
                                )
                            },
                            onDragEnd = {
                                val drag = activeDrag
                                activeDrag = null
                                if (drag != null) {
                                    val velYPxPerSec = velocityTracker.calculateVelocity().y
                                    val velocityYDpPerSec = with(density) { velYPxPerSec.toDp().value }
                                    val pointerYDp = with(density) { (drag.pointerYRootPx - with(density) { statusBarH.dp.toPx() }).toDp().value }
                                    val shouldRemove = TileDragEngine.shouldThrowRemove(
                                        dropPreview = dropPreview,
                                        drag = drag,
                                        pointerLocalYDp = pointerYDp,
                                        viewportHeightDp = tilingAreaHeightDp,
                                        velocityYDpPerSec = velocityYDpPerSec,
                                    )
                                    val outcome = TileDragEngine.applyDropMutation(pages, drag, dropPreview, shouldRemove)
                                    updatePagesWithHistory(outcome.pages)
                                    outcome.undoSnapshot?.let { undoSnapshot = it }
                                    outcome.hapticKind?.let { CanopyHaptics.trigger(it, motionSettings.hapticsEnabled) }
                                }
                            },
                            onDragCancel = { activeDrag = null },
                        )
                    }
            ) {
                if (showProceduralWallpaper) {
                    WallpaperCanvas(
                        themeId = themeId,
                        mode = colorMode,
                        wallpaperId = wallpaperId,
                        parallaxXPx = { PagingPhysics.computeParallaxOffsetPx(pageOffsetAnim.value) },
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(statusBarH.dp))

                    if (!isDefaultHome && !dismissDefaultHomeBanner) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(CircleShape)
                                .background(Color(0xD90F172A))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Als Standard-Launcher festlegen →",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    val roleIntent = homeRoleAndBackupController.buildDefaultLauncherRoleIntent()
                                    if (roleIntent != null) {
                                        runCatching { defaultLauncherRoleLauncher.launch(roleIntent) }
                                            .onFailure { homeRoleAndBackupController.openHomeSettingsFallback() }
                                    } else {
                                        homeRoleAndBackupController.openHomeSettingsFallback()
                                    }
                                },
                            )
                            Text(
                                text = "✕",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { dismissDefaultHomeBanner = true },
                            )
                        }
                    }

                    HorizontalTilingPager(
                        pages = pages,
                        activePageIndex = activePageIndex,
                        pageWidthPx = screenWidthPx,
                        pageOffsetAnim = pageOffsetAnim,
                        motionSettings = motionSettings,
                        isEditMode = isEditMode,
                        gestureDisabled = hasAnyOverlay || activeDrag != null,
                        onSnapToPage = snapToPage,
                        onSwipeUpSearch = { showSearch = true },
                        onSwipeDownShade = {
                            if (!homeRoleAndBackupController.expandSystemNotificationShade()) {
                                showQuickShade = true
                            }
                        },
                        onPinchOverview = { showPageOverview = true },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) { idx, page ->
                        TilingPageCanvas(
                            page = page,
                            boundsDp = pageBoundsDp,
                            theme = theme,
                            motionSettings = motionSettings,
                            isEditMode = isEditMode,
                            isActivePage = idx == activePageIndex,
                            alarmState = alarmState,
                            activeDrag = activeDrag,
                            dropPreview = dropPreview,
                            onToggleAlarm = { alarmState = alarmState.copy(enabled = !alarmState.enabled) },
                            onEnterEditMode = { isEditMode = true },
                            onUpdateDividerRatio = { pageId, splitId, ratio ->
                                val now = System.currentTimeMillis()
                                if (now - lastDividerRecordMs > 600L) {
                                    editHistory = editHistory.recordBeforeMutation(pages)
                                    lastDividerRecordMs = now
                                }
                                pages = pages.map { p ->
                                    if (p.id == pageId) p.copy(root = TilingEngine.updateSplitRatio(p.root, splitId, ratio)) else p
                                }
                            },
                            onUpdateMultipleDividerRatios = { pageId, updates ->
                                val now = System.currentTimeMillis()
                                if (now - lastDividerRecordMs > 600L) {
                                    editHistory = editHistory.recordBeforeMutation(pages)
                                    lastDividerRecordMs = now
                                }
                                pages = pages.map { p ->
                                    if (p.id == pageId) p.copy(root = TilingEngine.updateMultipleSplitRatios(p.root, updates)) else p
                                }
                            },
                            onToggleDividerDirection = { pageId, splitId ->
                                updatePagesWithHistory(
                                    pages.map { p ->
                                        if (p.id == pageId) p.copy(root = TilingEngine.toggleSplitDirection(p.root, splitId)) else p
                                    }
                                )
                            },
                            onUpdateTile = { pageId, leafId, updater ->
                                pages = pages.map { p ->
                                    if (p.id == pageId) p.copy(root = TilingEngine.updateLeafTile(p.root, leafId, updater)) else p
                                }
                            },
                            onOpenApp = { appId, rect -> openAppWithBadgeSync(appId, rect, null) },
                            onOpenGroup = { _, leafId, _ ->
                                openGroupState = Triple(page.id, leafId, GroupOverlayMode.VIEW)
                            },
                            onOpenSearch = { showSearch = true },
                            onOpenFakeTileApp = { tile, rect ->
                                val matchedLeafId = TilingEngine.collectLeaves(page.root).firstOrNull { it.tile.id == tile.id }?.id
                                fakeApp = FakeAppTarget(
                                    kind = FakeAppTargetKind.TILE,
                                    tile = tile,
                                    pageId = page.id,
                                    leafId = matchedLeafId,
                                    sourceRect = rect,
                                )
                            },
                            onOpenContextMenu = { leaf, pageId, rectPx ->
                                val rectDp = with(density) {
                                    Rect(rectPx.left.toDp().value, rectPx.top.toDp().value, rectPx.right.toDp().value, rectPx.bottom.toDp().value)
                                }
                                contextMenu = ContextMenuState(leaf.id, pageId, leaf.tile, rectDp)
                            },
                            onCloseContextMenu = { contextMenu = null },
                            onStartTileDrag = { drag -> activeDrag = drag },
                            calendarEvents = calendarEvents,
                            mediaState = mediaState,
                            systemState = systemState,
                            allAppsCatalog = boostedAppsCatalog,
                            onPinAppToHome = { appId ->
                                val appDef = boostedAppsCatalog.firstOrNull { it.id == appId } ?: MockCatalogData.APPS_BY_ID[appId]
                                if (appDef != null) {
                                    val tile = TileData(
                                        id = TilingEngine.createNodeId("pin"),
                                        type = TileType.SINGLE_APP,
                                        title = appDef.name,
                                        appId = appDef.id,
                                        accentHue = appDef.hue,
                                    )
                                    val targetHomeIdx = pages.indexOfFirst { it.id == homePageId }.coerceAtLeast(0)
                                    val homePg = pages.getOrNull(targetHomeIdx)
                                    if (homePg != null && homePg.kind != PageKind.ALL_APPS) {
                                        pages = pages.map { p ->
                                            if (p.id == homePg.id) {
                                                CatalogAndPageManager.insertCatalogTileIntoLargestLeaf(p, tile, pageBoundsDp, gapDp)
                                            } else p
                                        }
                                        CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                                        snapToPage(targetHomeIdx, 0f)
                                    }
                                }
                            },
                            onShowAppDetails = { appId, rect ->
                                val pkg = boostedAppsCatalog.firstOrNull { it.id == appId }?.packageName
                                if (!deviceAppsController.openSystemAppDetails(pkg)) {
                                    fakeApp = FakeAppTarget(FakeAppTargetKind.APP, appId = appId, sourceRect = rect, showInfoOnly = true)
                                }
                            },
                            onUninstallApp = { appId ->
                                val pkg = boostedAppsCatalog.firstOrNull { it.id == appId }?.packageName
                                val uri = AllAppsCatalogSorter.buildUninstallUri(pkg)
                                if (uri != null) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DELETE, Uri.parse(uri)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                }
                            },
                        )
                    }

                    BottomDockBar(
                        pages = pages,
                        activePageIndex = activePageIndex,
                        pageWidthPx = screenWidthPx,
                        pageOffsetPx = pageOffsetAnim.value,
                        theme = theme,
                        motionSettings = motionSettings,
                        showSearchActive = showSearch,
                        showSettingsActive = showDevMenu,
                        onOpenSearch = {
                            showQuickShade = false
                            showSearch = true
                        },
                        onOpenSettings = {
                            showSearch = false
                            showDevMenu = true
                        },
                        onSelectPage = { idx -> snapToPage(idx, 0f) },
                        onOpenOverview = { showPageOverview = true },
                        dockHeightDp = bottomAreaH.dp,
                        navBarBottomDp = navBottomDp.dp,
                    )
                }

                if (isEditMode && !showCatalog && !showPageOverview) {
                    EditToolbar(
                        theme = theme,
                        hapticsEnabled = motionSettings.hapticsEnabled,
                        isAllAppsPage = activePage?.kind == PageKind.ALL_APPS,
                        onOpenCatalog = { showCatalog = true },
                        onApplyTemplate = { tpl ->
                            if (activePage != null && activePage.kind != PageKind.ALL_APPS) {
                                updatePagesWithHistory(
                                    pages.map { p ->
                                        if (p.id == activePage.id) {
                                            p.copy(root = TilingEngine.applyTemplate(p.root, tpl, MockCatalogData.CATALOG_EXTRA_TILES))
                                        } else p
                                    }
                                )
                            }
                        },
                        onOpenPages = { showPageOverview = true },
                        onDone = { isEditMode = false },
                        modifier = Modifier.align(Alignment.BottomCenter),
                        canUndo = editHistory.canUndo,
                        canRedo = editHistory.canRedo,
                        onUndoEdit = {
                            val (nextHist, prevPages) = editHistory.undo(pages)
                            editHistory = nextHist
                            if (prevPages != null) pages = prevPages
                        },
                        onRedoEdit = {
                            val (nextHist, nextPages) = editHistory.redo(pages)
                            editHistory = nextHist
                            if (nextPages != null) pages = nextPages
                        },
                    )
                }

                UndoTileRemovalBanner(
                    snapshot = undoSnapshot,
                    onUndo = {
                        CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                        undoSnapshot?.let { pages = it.pages }
                        undoSnapshot = null
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (isEditMode) 84.dp else 68.dp),
                )

                FloatingDragGhostOverlay(
                    drag = activeDrag,
                    screenOriginRootX = 0f,
                    screenOriginRootY = 0f,
                    theme = theme,
                    motionSettings = motionSettings,
                    alarmState = alarmState,
                    calendarEvents = calendarEvents,
                    mediaState = mediaState,
                    systemState = systemState,
                    allAppsCatalog = boostedAppsCatalog,
                )

                if (showQuickShade) {
                    QuickSettingsNotificationShade(
                        theme = theme,
                        systemState = systemState,
                        notifications = notifSyncState.notifications,
                        hapticsEnabled = motionSettings.hapticsEnabled,
                        maxShadeHeightDp = (screenHeightDp - statusBarH - bottomAreaH - 16f).dp,
                        onClose = { showQuickShade = false },
                        onOpenAppFromNotification = { appId, rect -> openAppWithBadgeSync(appId, rect, null) },
                        onDismissNotification = { NotificationBadgeStore.dismissNotification(it) },
                        onDismissAllNotifications = { NotificationBadgeStore.dismissAll() },
                        onRestoreDemoNotifications = { NotificationBadgeStore.restoreDemoNotifications() },
                        onLongPressToggle = { key ->
                            showQuickShade = false
                            val action = NotificationBadgeStore.settingsIntentForToggle(key)
                            val launched = runCatching {
                                context.startActivity(
                                    Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                                true
                            }.getOrDefault(false)
                            if (!launched) {
                                openAppWithBadgeSync("app-einstellungen", null, action)
                            }
                        },
                    )
                }

                contextMenu?.let { menu ->
                    val nativeShortcuts = remember(menu.tile, liveAppsCatalog) {
                        val appDef = menu.tile.appId?.let { id ->
                            liveAppsCatalog.firstOrNull { it.id == id } ?: MockCatalogData.APPS_BY_ID[id]
                        }
                        deviceAppsController.queryShortcutsForApp(appDef)
                    }
                    ContextMenuOverlay(
                        menu = menu,
                        containerWidthDp = screenWidthDp,
                        containerHeightDp = screenHeightDp,
                        theme = theme,
                        hapticsEnabled = motionSettings.hapticsEnabled,
                        nativeShortcuts = nativeShortcuts,
                        onClose = { contextMenu = null },
                        onEnterEditMode = {
                            contextMenu = null
                            isEditMode = true
                        },
                        onQuickGrow = { pageId, leafId ->
                            contextMenu = null
                            updatePagesWithHistory(
                                pages.map { p ->
                                    if (p.id == pageId) p.copy(root = TilingEngine.nudgeLeafSize(p.root, leafId, 0.14f)) else p
                                }
                            )
                        },
                        onQuickSplit = { pageId, leafId, rectDp ->
                            contextMenu = null
                            val zone = if (rectDp.width >= rectDp.height) DropZone.RIGHT else DropZone.BOTTOM
                            val companion = MockCatalogData.CATALOG_EXTRA_TILES[4].copy(id = TilingEngine.createNodeId("tile"))
                            updatePagesWithHistory(
                                pages.map { p ->
                                    if (p.id == pageId) p.copy(root = TilingEngine.splitLeaf(p.root, leafId, companion, zone, 0.5f)) else p
                                }
                            )
                        },
                        onRemoveTile = { pageId, leafId ->
                            contextMenu = null
                            val pg = pages.firstOrNull { it.id == pageId }
                            if (pg != null) {
                                val res = TilingEngine.removeLeaf(pg.root, leafId)
                                if (res.tree != null && res.removed != null) {
                                    undoSnapshot = UndoSnapshot(pages, res.removed.tile.title)
                                    updatePagesWithHistory(pages.map { p -> if (p.id == pageId) p.copy(root = res.tree) else p })
                                }
                            }
                        },
                        onShowInfo = { tile, rectDp ->
                            contextMenu = null
                            val appPkg = tile.appId?.let { id ->
                                liveAppsCatalog.firstOrNull { it.id == id }?.packageName
                            }
                            if (!deviceAppsController.openSystemAppDetails(appPkg)) {
                                fakeApp = if (tile.type == TileType.SINGLE_APP && tile.appId != null) {
                                    FakeAppTarget(FakeAppTargetKind.APP, appId = tile.appId, sourceRect = rectDp, showInfoOnly = true)
                                } else {
                                    FakeAppTarget(FakeAppTargetKind.TILE, tile = tile, sourceRect = rectDp, showInfoOnly = true)
                                }
                            }
                        },
                        onShortcutAction = { label, tile, rectDp ->
                            val ctxPageId = menu.pageId
                            val ctxLeafId = menu.leafId
                            contextMenu = null
                            when {
                                label == "Gruppe umbenennen" -> {
                                    openGroupState = Triple(ctxPageId, ctxLeafId, GroupOverlayMode.RENAME)
                                }
                                label == "Apps verwalten" -> {
                                    openGroupState = Triple(ctxPageId, ctxLeafId, GroupOverlayMode.MANAGE)
                                }
                                label == "Mix wechseln" -> mediaState.onNext()
                                label == "App-Info" && tile.type == TileType.SINGLE_APP && tile.appId != null -> {
                                    val appPkg = liveAppsCatalog.firstOrNull { it.id == tile.appId }?.packageName
                                    if (!deviceAppsController.openSystemAppDetails(appPkg)) {
                                        fakeApp = FakeAppTarget(FakeAppTargetKind.APP, appId = tile.appId, sourceRect = rectDp, showInfoOnly = true)
                                    }
                                }
                                tile.type == TileType.SINGLE_APP && tile.appId != null -> {
                                    openAppWithBadgeSync(tile.appId, rectDp, label)
                                }
                                else -> {
                                    fakeApp = FakeAppTarget(
                                        kind = FakeAppTargetKind.TILE,
                                        tile = tile,
                                        pageId = ctxPageId,
                                        leafId = ctxLeafId,
                                        sourceRect = rectDp,
                                        shortcutAction = label,
                                    )
                                }
                            }
                        },
                        onChangeTileAccentHue = { pageId, leafId, hue ->
                            contextMenu = null
                            updatePagesWithHistory(
                                pages.map { p ->
                                    if (p.id == pageId) {
                                        p.copy(
                                            root = TilingEngine.updateLeafTile(p.root, leafId) { prev ->
                                                prev.copy(accentHue = hue)
                                            }
                                        )
                                    } else p
                                }
                            )
                        },
                    )
                }

                openGroupState?.let { (pgId, lfId, mode) ->
                    val livePage = pages.firstOrNull { it.id == pgId }
                    val liveLeaf = livePage?.let { TilingEngine.findLeaf(it.root, lfId) }
                    if (liveLeaf != null) {
                        AppGroupOverlay(
                            tile = liveLeaf.tile,
                            initialMode = mode,
                            theme = theme,
                            allAppsCatalog = liveAppsCatalog,
                            onClose = { openGroupState = null },
                            onOpenApp = { appId, rect -> openAppWithBadgeSync(appId, rect, null) },
                            onUpdateGroup = { updater ->
                                pages = pages.map { p ->
                                    if (p.id == pgId) p.copy(root = TilingEngine.updateLeafTile(p.root, lfId, updater)) else p
                                }
                            },
                        )
                    }
                }

                if (showSearch) {
                    UniversalSearchOverlay(
                        theme = theme,
                        allAppsCatalog = boostedAppsCatalog,
                        contactsCatalog = deviceContacts,
                        hapticsEnabled = motionSettings.hapticsEnabled,
                        onClose = { showSearch = false },
                        onOpenApp = { appId, rect -> openAppWithBadgeSync(appId, rect, null) },
                        onDialContact = { contact, rect ->
                            val dialUri = UniversalSearchEngine.buildContactDialUri(contact.status)
                            val launched = if (dialUri != null) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse(dialUri)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                    true
                                }.getOrDefault(false)
                            } else false
                            if (!launched) {
                                openAppWithBadgeSync("app-telefon", rect, contact.name)
                            }
                        },
                        onOpenSetting = { setting, rect ->
                            val action = UniversalSearchEngine.resolveSettingIntentAction(setting)
                            val launched = runCatching {
                                context.startActivity(
                                    Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                                true
                            }.getOrDefault(false)
                            if (!launched) {
                                openAppWithBadgeSync("app-einstellungen", rect, setting.name)
                            }
                        },
                        onSearchWeb = { query, rect ->
                            val intent = UniversalSearchEngine.buildBrowserSearchIntent(query)
                            runCatching { context.startActivity(intent) }
                            fakeApp = FakeAppTarget(
                                kind = FakeAppTargetKind.APP,
                                appId = "app-browser",
                                webQuery = query,
                                sourceRect = rect,
                            )
                        },
                    )
                }

                if (showCatalog) {
                    TileCatalogSheet(
                        theme = theme,
                        hapticsEnabled = motionSettings.hapticsEnabled,
                        appsCatalog = boostedAppsCatalog,
                        nativeWidgetProviders = nativeWidgetProviders,
                        onClose = { showCatalog = false },
                        onSelectTile = { tile ->
                            if (activePage != null && activePage.kind != PageKind.ALL_APPS) {
                                updatePagesWithHistory(
                                    pages.map { p ->
                                        if (p.id == activePage.id) {
                                            CatalogAndPageManager.insertCatalogTileIntoLargestLeaf(p, tile, pageBoundsDp, gapDp)
                                        } else p
                                    }
                                )
                            }
                            showCatalog = false
                        },
                        onStartCatalogDrag = { tile, xPx, yPx ->
                            showCatalog = false
                            activeDrag = ActiveTileDrag(
                                sourceKind = DragSourceKind.CATALOG_TILE,
                                tile = tile,
                                widthDp = 165f,
                                heightDp = 150f,
                                pointerXRootPx = xPx,
                                pointerYRootPx = yPx,
                            )
                        },
                        onBindNativeWidget = { spec ->
                            val host = appWidgetHost
                            val manager = appWidgetManager
                            if (host != null && manager != null) {
                                runCatching {
                                    val widgetId = host.allocateAppWidgetId()
                                    val component = ComponentName(spec.providerPackage, spec.providerClass)
                                    val bound = manager.bindAppWidgetIdIfAllowed(widgetId, component)
                                    if (bound) {
                                        completeBoundWidgetOrConfigure(widgetId, spec)
                                    } else {
                                        pendingWidgetBind = widgetId to spec
                                        val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, component)
                                        }
                                        widgetBindLauncher.launch(bindIntent)
                                    }
                                }
                            }
                        },
                    )
                }

                if (showPageOverview) {
                    PageOverviewModal(
                        pages = pages,
                        activePageIndex = activePageIndex,
                        homePageId = homePageId,
                        theme = theme,
                        hapticsEnabled = motionSettings.hapticsEnabled,
                        onClose = { showPageOverview = false },
                        onSelectPage = { idx -> snapToPage(idx, 0f) },
                        onSetHomePage = { pid -> homePageId = pid },
                        onAddPage = {
                            val (next, newIdx) = CatalogAndPageManager.addNewRegularPage(pages)
                            pages = next
                            showPageOverview = false
                            snapToPage(newIdx, 0f)
                        },
                        onReorderRegularPage = { regIdx, delta ->
                            pages = CatalogAndPageManager.reorderRegularPage(pages, regIdx, delta)
                        },
                        onDeletePage = { pid ->
                            val (next, nextHome) = CatalogAndPageManager.deleteRegularPage(pages, pid, homePageId)
                            pages = next
                            homePageId = nextHome
                            if (activePageIndex >= next.size) {
                                snapToPage((next.size - 1).coerceAtLeast(0), 0f)
                            }
                        },
                        onRenamePage = { pid, newTitle ->
                            pages = CatalogAndPageManager.renameRegularPage(pages, pid, newTitle)
                        },
                        onDuplicatePage = { pid ->
                            val (next, newIdx) = CatalogAndPageManager.duplicateRegularPage(pages, pid)
                            pages = next
                            snapToPage(newIdx, 0f)
                        },
                    )
                }

                fakeApp?.let { target ->
                    val liveTile = if (target.pageId != null && target.leafId != null) {
                        pages.firstOrNull { it.id == target.pageId }
                            ?.let { TilingEngine.findLeaf(it.root, target.leafId)?.tile }
                            ?: target.tile
                    } else target.tile
                    FakeAppOverlay(
                        target = target.copy(tile = liveTile),
                        containerWidthPx = screenWidthPx,
                        containerHeightPx = screenHeightPx,
                        theme = theme,
                        motionSettings = motionSettings,
                        allAppsCatalog = liveAppsCatalog,
                        calendarEvents = calendarEvents,
                        onAddCalendarEvent = { ev -> customCalendarEvents = listOf(ev) + customCalendarEvents },
                        mediaState = mediaState,
                        alarmState = alarmState,
                        onUpdateAlarmState = { alarmState = it },
                        onUpdateTile = { pageId, leafId, updater ->
                            pages = pages.map { p ->
                                if (p.id == pageId) p.copy(root = TilingEngine.updateLeafTile(p.root, leafId, updater)) else p
                            }
                        },
                        weatherCityId = weatherCityId,
                        onSelectWeatherCity = { weatherCityId = it },
                        launcherSettings = LauncherSettingsBridge(
                            themeId = themeId,
                            colorMode = colorMode,
                            wallpaperId = wallpaperId,
                            tonalIcons = tonalIcons,
                            shortcutBarMode = shortcutBarMode,
                            onChangeThemeId = { id ->
                                themeId = id
                                if (id == ThemeVariantId.DARK_BOLD) colorMode = ColorMode.DARK
                                if (id == ThemeVariantId.MATERIAL_YOU) colorMode = ColorMode.LIGHT
                            },
                            onChangeColorMode = { colorMode = it },
                            onChangeWallpaperId = { wallpaperId = it },
                            onChangeTonalIcons = { tonalIcons = it },
                            onChangeShortcutBarMode = { mode ->
                                shortcutBarMode = mode
                                pages = MockCatalogData.syncShortcutBars(pages, mode)
                            },
                        ),
                        onClose = { fakeApp = null },
                    )
                }

                DeveloperMenuSheet(
                    isOpen = showDevMenu,
                    themeId = themeId,
                    colorMode = colorMode,
                    wallpaperId = wallpaperId,
                    tonalIcons = tonalIcons,
                    shortcutBarMode = shortcutBarMode,
                    motionSettings = motionSettings,
                    showHapticHud = showHapticHud,
                    useSystemWallpaper = useSystemWallpaper,
                    showFpsHud = showFpsHud,
                    backupStatusMessage = backupStatusMessage,
                    onClose = {
                        backupStatusMessage = null
                        showDevMenu = false
                    },
                    onChangeThemeId = { themeId = it },
                    onChangeColorMode = { colorMode = it },
                    onChangeWallpaperId = { wallpaperId = it },
                    onChangeTonalIcons = { tonalIcons = it },
                    onChangeShortcutBarMode = { mode ->
                        shortcutBarMode = mode
                        pages = MockCatalogData.syncShortcutBars(pages, mode)
                    },
                    onChangeMotionSettings = { motionSettings = it },
                    onChangeShowHapticHud = { showHapticHud = it },
                    onChangeShowFpsHud = { showFpsHud = it },
                    onOpenGestureGuide = {
                        showDevMenu = false
                        showGestureGuide = true
                    },
                    onChangeUseSystemWallpaper = { useSystemWallpaper = it },
                    onRequestDefaultHome = {
                        val roleIntent = homeRoleAndBackupController.buildDefaultLauncherRoleIntent()
                        if (roleIntent != null) {
                            runCatching { defaultLauncherRoleLauncher.launch(roleIntent) }
                                .onFailure { homeRoleAndBackupController.openHomeSettingsFallback() }
                        } else {
                            homeRoleAndBackupController.openHomeSettingsFallback()
                        }
                    },
                    onExportLayoutJson = {
                        homeRoleAndBackupController.exportToClipboardAndShare(currentSnapshot)
                        backupStatusMessage = "Layout-JSON in Zwischenablage kopiert."
                    },
                    onImportLayoutJson = {
                        homeRoleAndBackupController.importFromClipboard()
                            .onSuccess { restored ->
                                pages = restored.pages
                                homePageId = restored.homePageId
                                themeId = restored.themeId
                                colorMode = restored.colorMode
                                wallpaperId = restored.wallpaperId
                                tonalIcons = restored.tonalIcons
                                shortcutBarMode = restored.shortcutBarMode
                                motionSettings = restored.motionSettings
                                alarmState = restored.alarmState
                                useSystemWallpaper = restored.useSystemWallpaper
                                appUsageCounts = restored.appUsageCounts
                                customCalendarEvents = restored.customCalendarEvents
                                weatherCityId = restored.weatherCityId
                                backupStatusMessage = "Layout erfolgreich importiert (${restored.pages.size} Seiten)."
                            }
                            .onFailure { err ->
                                backupStatusMessage = err.message ?: "Import fehlgeschlagen."
                            }
                    },
                    onResetLayout = {
                        store.reset()
                        val freshBase = MockCatalogData.createInitialPages(ShortcutBarMode.ALL)
                        val fresh = if (liveAppsCatalog.isNotEmpty()) {
                            app.canopy.launcher.system.InstalledAppsMapper.resolveRealAppIdsForPages(liveAppsCatalog, freshBase)
                        } else freshBase
                        pages = fresh
                        shortcutBarMode = ShortcutBarMode.ALL
                        homePageId = "page-start"
                        themeId = ThemeVariantId.MATERIAL_YOU
                        colorMode = ColorMode.LIGHT
                        wallpaperId = app.canopy.launcher.data.WallpaperPresetId.SAND
                        tonalIcons = true
                        motionSettings = app.canopy.launcher.data.MotionSettings()
                        alarmState = AlarmState()
                        useSystemWallpaper = true
                        appUsageCounts = emptyMap()
                        customCalendarEvents = emptyList()
                        weatherCityId = "berlin"
                        editHistory = EditHistoryState()
                        NotificationBadgeStore.restoreDemoNotifications()
                        showDevMenu = false
                        isEditMode = false
                        val startPageIdx = fresh.indexOfFirst { it.id == "page-start" }.coerceAtLeast(0)
                        snapToPage(startPageIdx, 0f)
                    },
                )

                if (showGestureGuide) {
                    app.canopy.launcher.ui.overlays.GestureCheatSheetDialog(
                        onClose = { showGestureGuide = false },
                    )
                }

                if (showFpsHud) {
                    app.canopy.launcher.ui.overlays.LiveFpsHudOverlay(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = (statusBarH + 8).dp, end = 12.dp),
                    )
                }

                HapticVisualFeedbackLayer(
                    latestEvent = latestHaptic,
                    showHudBadge = showHapticHud,
                )
            }
        }
    }
}
