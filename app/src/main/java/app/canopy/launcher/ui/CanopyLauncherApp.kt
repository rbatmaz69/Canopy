package app.canopy.launcher.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import app.canopy.launcher.system.LiveHardwareController
import app.canopy.launcher.system.LiveMediaSessionSnapshot
import app.canopy.launcher.system.LocalHardwareTelemetry
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
import app.canopy.launcher.ui.overlays.CanopyStatusBar
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
import app.canopy.launcher.ui.tiles.LocalAppWidgetHostFactory
import app.canopy.launcher.ui.tiles.MediaPlayerState
import app.canopy.launcher.ui.tiles.SystemControlState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val APP_WIDGET_HOST_ID = 0xCA90

@Composable
fun CanopyLauncherApp() {
    val context = LocalContext.current
    val rootView = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

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

    var calendarEvents by remember {
        mutableStateOf(SystemDataAndWidgetEngine.mergeCalendarEvents(systemDataProvider.loadTodayCalendarEvents()))
    }
    val notifSyncState by NotificationBadgeStore.state.collectAsState()

    val startIdx = remember {
        pages.indexOfFirst { it.id == homePageId }.coerceAtLeast(0)
    }
    var activePageIndex by remember { mutableIntStateOf(startIdx) }

    // Persist launcher state changes automatically
    LaunchedEffect(
        pages, homePageId, themeId, colorMode, wallpaperId, tonalIcons, shortcutBarMode, motionSettings, alarmState, useSystemWallpaper
    ) {
        store.save(
            PersistedLauncherState(
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
            )
        )
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
    var showSearch by remember { mutableStateOf(false) }
    var showQuickShade by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var showPageOverview by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf<ContextMenuState?>(null) }
    var openGroupState by remember { mutableStateOf<Triple<String, String, GroupOverlayMode>?>(null) }
    var fakeApp by remember { mutableStateOf<FakeAppTarget?>(null) }
    var undoSnapshot by remember { mutableStateOf<UndoSnapshot?>(null) }
    var activeDrag by remember { mutableStateOf<ActiveTileDrag?>(null) }

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
        onChangeBrightness = { v -> systemState = systemState.copy(brightness = v.coerceIn(0.25f, 1f)) },
    )

    // Latest Haptic Event stream
    var latestHaptic by remember { mutableStateOf<HapticEvent?>(null) }
    DisposableEffect(Unit) {
        val unsub = CanopyHaptics.subscribe { latestHaptic = it }
        onDispose { unsub() }
    }

    val theme = CanopyThemes.getThemeTokens(themeId, colorMode, wallpaperId)
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
            val appDef = liveAppsCatalog.firstOrNull { it.id == appId } ?: MockCatalogData.APPS_BY_ID[appId]
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

        val hasAnyOverlay = fakeApp != null || openGroupState != null || showSearch ||
            contextMenu != null || showCatalog || showPageOverview || showQuickShade || showDevMenu

        BackHandler(enabled = hasAnyOverlay || isEditMode) {
            handleBackAction()
        }

        // Active drop preview during drag
        val activePage = pages.getOrNull(activePageIndex)
        val gapDp = if (isEditMode) theme.gapEditDp else theme.gapUseDp

        // Pending AppWidget bind flow
        var pendingWidgetBind by remember { mutableStateOf<Pair<Int, NativeWidgetProviderSpec>?>(null) }
        val insertBoundWidget: (Int, NativeWidgetProviderSpec) -> Unit = { widgetId, spec ->
            val targetPage = pages.getOrNull(activePageIndex)
            if (targetPage != null && targetPage.kind != PageKind.ALL_APPS) {
                val matchedAppId = liveAppsCatalog.firstOrNull { it.packageName == spec.providerPackage }?.id
                val widgetTile = SystemDataAndWidgetEngine.buildWidgetTileFromProvider(spec, widgetId, matchedAppId)
                pages = pages.map { p ->
                    if (p.id == targetPage.id) {
                        CatalogAndPageManager.insertCatalogTileIntoLargestLeaf(p, widgetTile, pageBoundsDp, gapDp)
                    } else p
                }
            }
            showCatalog = false
        }
        val widgetBindLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val pending = pendingWidgetBind
            pendingWidgetBind = null
            if (pending != null) {
                val (widgetId, spec) = pending
                if (result.resultCode == Activity.RESULT_OK) {
                    insertBoundWidget(widgetId, spec)
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
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                activeDrag = activeDrag?.copy(
                                    pointerXRootPx = change.position.x,
                                    pointerYRootPx = change.position.y,
                                )
                            },
                            onDragEnd = {
                                val drag = activeDrag
                                activeDrag = null
                                if (drag != null) {
                                    val pointerYDp = with(density) { (drag.pointerYRootPx - with(density) { statusBarH.dp.toPx() }).toDp().value }
                                    val shouldRemove = TileDragEngine.shouldThrowRemove(
                                        dropPreview = dropPreview,
                                        drag = drag,
                                        pointerLocalYDp = pointerYDp,
                                        viewportHeightDp = tilingAreaHeightDp,
                                        velocityYDpPerSec = 0f,
                                    )
                                    val outcome = TileDragEngine.applyDropMutation(pages, drag, dropPreview, shouldRemove)
                                    pages = outcome.pages
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
                    CanopyStatusBar(
                        theme = theme,
                        systemState = systemState,
                        notificationCount = notifSyncState.notifications.size,
                        hapticsEnabled = motionSettings.hapticsEnabled,
                        onToggleQuickShade = { showQuickShade = !showQuickShade },
                        onOpenDevMenu = { showDevMenu = true },
                        barHeightDp = statusBarH.dp,
                    )

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
                                pages = pages.map { p ->
                                    if (p.id == pageId) p.copy(root = TilingEngine.updateSplitRatio(p.root, splitId, ratio)) else p
                                }
                            },
                            onUpdateMultipleDividerRatios = { pageId, updates ->
                                pages = pages.map { p ->
                                    if (p.id == pageId) p.copy(root = TilingEngine.updateMultipleSplitRatios(p.root, updates)) else p
                                }
                            },
                            onToggleDividerDirection = { pageId, splitId ->
                                pages = pages.map { p ->
                                    if (p.id == pageId) p.copy(root = TilingEngine.toggleSplitDirection(p.root, splitId)) else p
                                }
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
                                fakeApp = FakeAppTarget(kind = FakeAppTargetKind.TILE, tile = tile, sourceRect = rect)
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
                            allAppsCatalog = liveAppsCatalog,
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
                        showQuickShadeActive = showQuickShade,
                        hasAnyOverlay = hasAnyOverlay || isEditMode,
                        onOpenSearch = {
                            showQuickShade = false
                            showSearch = true
                        },
                        onToggleQuickShade = {
                            showSearch = false
                            showQuickShade = !showQuickShade
                        },
                        onSelectPage = { idx -> snapToPage(idx, 0f) },
                        onOpenOverview = { showPageOverview = true },
                        onHomeAction = handleHomeAction,
                        onBackAction = handleBackAction,
                        dockHeightDp = bottomAreaH.dp,
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
                                pages = pages.map { p ->
                                    if (p.id == activePage.id) {
                                        p.copy(root = TilingEngine.applyTemplate(p.root, tpl, MockCatalogData.CATALOG_EXTRA_TILES))
                                    } else p
                                }
                            }
                        },
                        onOpenPages = { showPageOverview = true },
                        onDone = { isEditMode = false },
                        modifier = Modifier.align(Alignment.BottomCenter),
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
                    allAppsCatalog = liveAppsCatalog,
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
                            pages = pages.map { p ->
                                if (p.id == pageId) p.copy(root = TilingEngine.nudgeLeafSize(p.root, leafId, 0.14f)) else p
                            }
                        },
                        onQuickSplit = { pageId, leafId, rectDp ->
                            contextMenu = null
                            val zone = if (rectDp.width >= rectDp.height) DropZone.RIGHT else DropZone.BOTTOM
                            val companion = MockCatalogData.CATALOG_EXTRA_TILES[4].copy(id = TilingEngine.createNodeId("tile"))
                            pages = pages.map { p ->
                                if (p.id == pageId) p.copy(root = TilingEngine.splitLeaf(p.root, leafId, companion, zone, 0.5f)) else p
                            }
                        },
                        onRemoveTile = { pageId, leafId ->
                            contextMenu = null
                            val pg = pages.firstOrNull { it.id == pageId }
                            if (pg != null) {
                                val res = TilingEngine.removeLeaf(pg.root, leafId)
                                if (res.tree != null && res.removed != null) {
                                    undoSnapshot = UndoSnapshot(pages, res.removed.tile.title)
                                    pages = pages.map { p -> if (p.id == pageId) p.copy(root = res.tree) else p }
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
                                    fakeApp = FakeAppTarget(FakeAppTargetKind.TILE, tile = tile, sourceRect = rectDp, shortcutAction = label)
                                }
                            }
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
                        allAppsCatalog = liveAppsCatalog,
                        contactsCatalog = deviceContacts,
                        hapticsEnabled = motionSettings.hapticsEnabled,
                        onClose = { showSearch = false },
                        onOpenApp = { appId, rect -> openAppWithBadgeSync(appId, rect, null) },
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
                        appsCatalog = liveAppsCatalog,
                        nativeWidgetProviders = nativeWidgetProviders,
                        onClose = { showCatalog = false },
                        onSelectTile = { tile ->
                            if (activePage != null && activePage.kind != PageKind.ALL_APPS) {
                                pages = pages.map { p ->
                                    if (p.id == activePage.id) {
                                        CatalogAndPageManager.insertCatalogTileIntoLargestLeaf(p, tile, pageBoundsDp, gapDp)
                                    } else p
                                }
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
                                        insertBoundWidget(widgetId, spec)
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
                    )
                }

                fakeApp?.let { target ->
                    FakeAppOverlay(
                        target = target,
                        containerWidthPx = screenWidthPx,
                        containerHeightPx = screenHeightPx,
                        theme = theme,
                        motionSettings = motionSettings,
                        allAppsCatalog = liveAppsCatalog,
                        calendarEvents = calendarEvents,
                        onAddCalendarEvent = { ev -> calendarEvents = listOf(ev) + calendarEvents },
                        mediaState = mediaState,
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
                    onClose = { showDevMenu = false },
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
                    onChangeUseSystemWallpaper = { useSystemWallpaper = it },
                    onResetLayout = {
                        store.reset()
                        val fresh = MockCatalogData.createInitialPages(ShortcutBarMode.ALL)
                        pages = fresh
                        shortcutBarMode = ShortcutBarMode.ALL
                        homePageId = "page-start"
                        themeId = ThemeVariantId.MATERIAL_YOU
                        colorMode = ColorMode.LIGHT
                        wallpaperId = app.canopy.launcher.data.WallpaperPresetId.SAND
                        tonalIcons = true
                        motionSettings = app.canopy.launcher.data.MotionSettings()
                        alarmState = AlarmState()
                        useSystemWallpaper = false
                        calendarEvents = SystemTileDefaults.CALENDAR_EVENTS
                        NotificationBadgeStore.restoreDemoNotifications()
                        showDevMenu = false
                        isEditMode = false
                        val startPageIdx = fresh.indexOfFirst { it.id == "page-start" }.coerceAtLeast(0)
                        snapToPage(startPageIdx, 0f)
                    },
                )

                HapticVisualFeedbackLayer(
                    latestEvent = latestHaptic,
                    showHudBadge = showHapticHud,
                )
            }
        }
    }
}
