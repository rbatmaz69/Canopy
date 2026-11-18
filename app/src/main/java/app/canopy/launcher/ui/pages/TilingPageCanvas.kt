package app.canopy.launcher.ui.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.AlarmState
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.layout.ComputedDivider
import app.canopy.launcher.layout.ComputedIntersection
import app.canopy.launcher.layout.ComputedLeaf
import app.canopy.launcher.layout.DropZone
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.RectFBounds
import app.canopy.launcher.layout.SplitDirection
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.CanopySprings
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.motion.SpringRole
import app.canopy.launcher.system.TileTapRouter
import app.canopy.launcher.system.TileTapTarget
import app.canopy.launcher.ui.theme.ThemeTokens
import app.canopy.launcher.ui.tiles.AppDefinition
import app.canopy.launcher.ui.tiles.CalendarEvent
import app.canopy.launcher.ui.tiles.MediaPlayerState
import app.canopy.launcher.ui.tiles.SystemControlState
import app.canopy.launcher.ui.tiles.TileRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

enum class DragSourceKind {
    PAGE_TILE,
    CATALOG_TILE,
}

data class ActiveTileDrag(
    val sourceKind: DragSourceKind,
    val sourcePageId: String? = null,
    val sourceLeafId: String? = null,
    val tile: TileData,
    val widthDp: Float,
    val heightDp: Float,
    val pointerXRootPx: Float,
    val pointerYRootPx: Float,
)

sealed interface DropPreviewResult {
    val pageId: String

    data class LeafTarget(
        override val pageId: String,
        val leaf: ComputedLeaf,
        val zone: DropZone,
    ) : DropPreviewResult

    data class RemoveTarget(
        override val pageId: String,
    ) : DropPreviewResult
}

object TilingEditMath {
    private val FRACTION_LABELS = listOf("¼", "⅓", "½", "⅔", "¾")

    fun crossedSnapPoint(prevRatio: Float, nextRatio: Float, lastSnapCrossed: Float?): Float? {
        for (snap in TilingEngine.SNAP_POINTS) {
            val crossed = (prevRatio < snap && nextRatio >= snap) || (prevRatio > snap && nextRatio <= snap)
            if (crossed && lastSnapCrossed != snap) {
                return snap
            }
        }
        return null
    }

    fun fractionLabelAtIndex(index: Int): String = FRACTION_LABELS.getOrElse(index) { "" }

    fun ghostTierBadgeText(widthDp: Float, heightDp: Float): String {
        val tier = TilingEngine.computeSizeTier(widthDp, heightDp)
        return "$tier · ${widthDp.roundToInt()}×${heightDp.roundToInt()}"
    }

    fun resolveDropZoneLabel(
        drag: ActiveTileDrag?,
        targetTile: TileData,
        zone: DropZone,
    ): String = when {
        zone == DropZone.CENTER &&
            targetTile.type == TileType.APP_GROUP &&
            drag?.tile?.type == TileType.SINGLE_APP -> "In Gruppe"
        zone == DropZone.CENTER -> "Tauschen"
        else -> "Teilen"
    }

    fun buildDividerStateDescription(direction: SplitDirection, ratio: Float): String {
        val pct = (ratio.coerceIn(0f, 1f) * 100f).roundToInt()
        val dirLabel = if (direction == SplitDirection.HORIZONTAL) "Horizontale Teilung" else "Vertikale Teilung"
        return "$dirLabel, $pct Prozent"
    }

    /** Returns -1 for first side squish, +1 for second side squish, 0 for none. */
    fun squishSideDirection(deltaDp: Float): Int = when {
        deltaDp > 2f -> 1
        deltaDp < -2f -> -1
        else -> 0
    }
}

@Composable
fun TilingPageCanvas(
    page: LauncherPage,
    boundsDp: RectFBounds,
    theme: ThemeTokens,
    motionSettings: MotionSettings,
    isEditMode: Boolean,
    isActivePage: Boolean,
    alarmState: AlarmState,
    activeDrag: ActiveTileDrag?,
    dropPreview: DropPreviewResult?,
    onToggleAlarm: () -> Unit,
    onEnterEditMode: () -> Unit,
    onUpdateDividerRatio: (pageId: String, splitId: String, ratio: Float) -> Unit,
    onUpdateMultipleDividerRatios: (pageId: String, updates: Map<String, Float>) -> Unit,
    onToggleDividerDirection: (pageId: String, splitId: String) -> Unit,
    onUpdateTile: (pageId: String, leafId: String, updater: (TileData) -> TileData) -> Unit,
    onOpenApp: (String, Rect?) -> Unit,
    onOpenGroup: (TileData, String, Rect?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTileTarget: (TileData, Rect?) -> Unit,
    onOpenContextMenu: (ComputedLeaf, String, Rect) -> Unit,
    onCloseContextMenu: () -> Unit,
    onStartTileDrag: (ActiveTileDrag) -> Unit,
    calendarEvents: List<CalendarEvent>,
    mediaState: MediaPlayerState,
    systemState: SystemControlState,
    allAppsCatalog: List<AppDefinition>,
    modifier: Modifier = Modifier,
    onPinAppToHome: (String) -> Unit = {},
    onShowAppDetails: (String, Rect?) -> Unit = { _, _ -> },
    onUninstallApp: (String) -> Unit = {},
) {
    val isAllApps = page.kind == PageKind.ALL_APPS
    val effectiveEdit = isEditMode && !isAllApps
    val animatedGapDp by animateFloatAsState(
        targetValue = if (effectiveEdit) theme.gapEditDp else theme.gapUseDp,
        animationSpec = CanopySprings.spec(SpringRole.GAP_OPEN, motionSettings),
        label = "tiling-gap",
    )

    val layout = remember(page.root, boundsDp, animatedGapDp) {
        TilingEngine.computeLayout(page.root, boundsDp, animatedGapDp)
    }

    var draggingDividerId by remember { mutableStateOf<String?>(null) }
    var draggingIntersectionId by remember { mutableStateOf<String?>(null) }
    var squishLeafIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastTapDividerId by remember { mutableStateOf("") }
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val activeDrop = if (dropPreview?.pageId == page.id) dropPreview else null

    val activeDivider = layout.dividers.firstOrNull { it.id == draggingDividerId }
    val activeIntersection = layout.intersections.firstOrNull { it.id == draggingIntersectionId }
    val affectedResizeIds = remember(activeDivider, activeIntersection) {
        when {
            activeDivider != null -> (activeDivider.firstLeafIds + activeDivider.secondLeafIds).toSet()
            activeIntersection != null -> buildSet {
                activeIntersection.horizontalSplits.forEach { d ->
                    addAll(d.firstLeafIds); addAll(d.secondLeafIds)
                }
                activeIntersection.verticalSplits.forEach { d ->
                    addAll(d.firstLeafIds); addAll(d.secondLeafIds)
                }
            }
            else -> emptySet()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Leaves
        layout.leaves.forEach { leaf ->
            val isDraggedSource = activeDrag?.sourceKind == DragSourceKind.PAGE_TILE &&
                activeDrag.sourceLeafId == leaf.id &&
                activeDrag.sourcePageId == page.id
            val isSquished = leaf.id in squishLeafIds
            val targetScale = when {
                isDraggedSource -> 0.92f
                isSquished -> 0.985f
                effectiveEdit -> 0.985f
                else -> 1f
            }
            val scaleAnim by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = CanopySprings.spec(SpringRole.PRESS_FEEDBACK, motionSettings),
                label = "leaf-scale-${leaf.id}",
            )
            var leafBoundsInRoot by remember(leaf.id) { mutableStateOf<Rect?>(null) }
            val isDropTarget = (activeDrop as? DropPreviewResult.LeafTarget)?.leaf?.id == leaf.id

            Box(
                modifier = Modifier
                    .offset(x = leaf.rect.x.dp, y = leaf.rect.y.dp)
                    .size(width = leaf.rect.width.dp, height = leaf.rect.height.dp)
                    .semantics {
                        contentDescription = leaf.tile.title
                        stateDescription = if (effectiveEdit) {
                            "Bearbeitungsmodus, ${TilingEditMath.ghostTierBadgeText(leaf.rect.width, leaf.rect.height)}"
                        } else {
                            "Kachel ${TilingEngine.computeSizeTier(leaf.rect.width, leaf.rect.height)}"
                        }
                    }
                    .graphicsLayer {
                        scaleX = scaleAnim
                        scaleY = scaleAnim
                        alpha = if (isDraggedSource) 0.32f else 1f
                    }
                    .onGloballyPositioned { leafBoundsInRoot = it.boundsInRoot() }
                    .pointerInput(leaf.id, effectiveEdit, isAllApps) {
                        if (isAllApps) return@pointerInput
                        val slopPx = with(density) { 9.dp.toPx() }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var longPressFired = false
                            var dragStarted = false
                            val lpJob = if (!effectiveEdit) {
                                scope.launch {
                                    delay(420L)
                                    longPressFired = true
                                    CanopyHaptics.trigger(HapticKind.EDIT_ENTER, motionSettings.hapticsEnabled)
                                    onOpenContextMenu(
                                        leaf,
                                        page.id,
                                        leafBoundsInRoot ?: Rect(0f, 0f, leaf.rect.width, leaf.rect.height),
                                    )
                                }
                            } else null

                            var totalDx = 0f
                            var totalDy = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (ch == null || !ch.pressed) {
                                    lpJob?.cancel()
                                    // An inner control (app icon, play button, checkbox, …) already
                                    // consumed this tap in the Main pass — never fire a second action.
                                    val handledByChild = ch?.isConsumed == true
                                    if (!longPressFired && !dragStarted && !effectiveEdit && !handledByChild &&
                                        TileTapRouter.resolve(leaf.tile) != TileTapTarget.NONE
                                    ) {
                                        CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                                        onOpenTileTarget(leaf.tile, leafBoundsInRoot)
                                    }
                                    break
                                }
                                val d = ch.positionChange()
                                totalDx += d.x
                                totalDy += d.y
                                val dist = hypot(totalDx, totalDy)

                                if (!effectiveEdit && !longPressFired && dist > slopPx) {
                                    lpJob?.cancel()
                                    break // Yield to HorizontalTilingPager swipe
                                }

                                if ((effectiveEdit || longPressFired) && !dragStarted && dist > slopPx) {
                                    dragStarted = true
                                    lpJob?.cancel()
                                    ch.consume()
                                    onCloseContextMenu()
                                    if (!isEditMode) onEnterEditMode()
                                    val rootOrigin = leafBoundsInRoot?.topLeft
                                    onStartTileDrag(
                                        ActiveTileDrag(
                                            sourceKind = DragSourceKind.PAGE_TILE,
                                            sourcePageId = page.id,
                                            sourceLeafId = leaf.id,
                                            tile = leaf.tile,
                                            widthDp = leaf.rect.width,
                                            heightDp = leaf.rect.height,
                                            pointerXRootPx = (rootOrigin?.x ?: 0f) + ch.position.x,
                                            pointerYRootPx = (rootOrigin?.y ?: 0f) + ch.position.y,
                                        )
                                    )
                                    break
                                }
                            }
                        }
                    }
            ) {
                TileRenderer(
                    tile = leaf.tile,
                    leafId = leaf.id,
                    widthDp = leaf.rect.width,
                    heightDp = leaf.rect.height,
                    theme = theme,
                    motionSettings = motionSettings,
                    isEditMode = effectiveEdit,
                    alarmState = alarmState,
                    onToggleAlarm = onToggleAlarm,
                    onOpenApp = onOpenApp,
                    onOpenGroup = onOpenGroup,
                    onOpenSearch = onOpenSearch,
                    onOpenTileTarget = onOpenTileTarget,
                    onUpdateTile = { lid, updater -> onUpdateTile(page.id, lid, updater) },
                    calendarEvents = calendarEvents,
                    mediaState = mediaState,
                    systemState = systemState,
                    allAppsCatalog = allAppsCatalog,
                    onPinAppToHome = onPinAppToHome,
                    onShowAppDetails = onShowAppDetails,
                    onUninstallApp = onUninstallApp,
                )

                val dropLeafTarget = activeDrop as? DropPreviewResult.LeafTarget
                if (isDropTarget && dropLeafTarget != null) {
                    DropZonePreviewOverlay(
                        zone = dropLeafTarget.zone,
                        radiusDp = theme.tileRadiusDp,
                        label = TilingEditMath.resolveDropZoneLabel(activeDrag, leaf.tile, dropLeafTarget.zone),
                    )
                }

                if (leaf.id in affectedResizeIds) {
                    Text(
                        text = TilingEditMath.ghostTierBadgeText(leaf.rect.width, leaf.rect.height),
                        color = Color.White,
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xE00F172A))
                            .border(1.dp, Color(0xB838BDF8), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
        }

        // Snap guide lines & fraction labels during 1D or 2D drag
        val guideDividers = when {
            activeDivider != null -> listOf(activeDivider)
            activeIntersection != null ->
                activeIntersection.horizontalSplits.take(1) + activeIntersection.verticalSplits.take(1)
            else -> emptyList()
        }
        guideDividers.forEach { guideDiv ->
            SnapGuideLinesOverlay(guideDiv = guideDiv, gapDp = animatedGapDp)
        }

        // 1D Divider hit strips & handles
        if (!isAllApps) {
            layout.dividers.forEach { divider ->
                val isHoriz = divider.direction == SplitDirection.HORIZONTAL
                val isDraggingThis = draggingDividerId == divider.id
                val hitX = if (isHoriz) divider.rect.x - 7f else divider.rect.x
                val hitY = if (isHoriz) divider.rect.y else divider.rect.y - 7f
                val hitW = if (isHoriz) divider.rect.width + 14f else divider.rect.width
                val hitH = if (isHoriz) divider.rect.height else divider.rect.height + 14f

                Box(
                    modifier = Modifier
                        .offset(x = hitX.dp, y = hitY.dp)
                        .size(width = hitW.dp, height = hitH.dp)
                        .semantics {
                            contentDescription = "Trennlinie ${divider.id}"
                            stateDescription = TilingEditMath.buildDividerStateDescription(divider.direction, divider.ratio)
                        }
                        .pointerInput(divider.id, effectiveEdit, animatedGapDp) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()

                                if (!effectiveEdit) {
                                    val lpJob = scope.launch {
                                        delay(380L)
                                        CanopyHaptics.trigger(HapticKind.EDIT_ENTER, motionSettings.hapticsEnabled)
                                        onEnterEditMode()
                                    }
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull()
                                        if (ch == null || !ch.pressed) {
                                            lpJob.cancel()
                                            break
                                        }
                                    }
                                    return@awaitEachGesture
                                }

                                // Double-tap flips split direction
                                val now = System.currentTimeMillis()
                                if (lastTapDividerId == divider.id && now - lastTapTimeMs < 320L) {
                                    lastTapDividerId = ""
                                    lastTapTimeMs = 0L
                                    CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                                    onToggleDividerDirection(page.id, divider.id)
                                    return@awaitEachGesture
                                }
                                lastTapDividerId = divider.id
                                lastTapTimeMs = now

                                draggingDividerId = divider.id
                                val parentSpanDp = if (isHoriz) divider.parentRect.width else divider.parentRect.height
                                val usableSpanDp = max(1f, parentSpanDp - animatedGapDp)
                                val startRatio = divider.ratio
                                var latestRatio = startRatio
                                var lastSnapCrossed: Float? = null
                                var cumulativeDeltaPx = 0f
                                val velocityTracker = VelocityTracker()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)

                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull()
                                    if (ch == null || !ch.pressed) {
                                        draggingDividerId = null
                                        squishLeafIds = emptySet()
                                        val vel = velocityTracker.calculateVelocity()
                                        val velDpPerSec = with(density) {
                                            (if (isHoriz) vel.x else vel.y).toDp().value
                                        }
                                        val snapped = TilingEngine.projectAndSnapRatio(
                                            currentRatio = latestRatio,
                                            velocityPxPerSec = velDpPerSec,
                                            parentSpan = parentSpanDp,
                                            minRatio = divider.minRatio,
                                            maxRatio = divider.maxRatio,
                                        )
                                        CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                                        onUpdateDividerRatio(page.id, divider.id, snapped)
                                        break
                                    }

                                    ch.consume()
                                    velocityTracker.addPosition(ch.uptimeMillis, ch.position)
                                    val d = ch.positionChange()
                                    cumulativeDeltaPx += if (isHoriz) d.x else d.y
                                    val deltaDp = with(density) { cumulativeDeltaPx.toDp().value }
                                    val rawRatio = startRatio + deltaDp / usableSpanDp
                                    val elasticRatio = TilingEngine.clampOrRubberbandRatio(
                                        rawRatio = rawRatio,
                                        parentSpan = parentSpanDp,
                                        minFirstPx = divider.minFirstPx,
                                        minSecondPx = divider.minSecondPx,
                                        rubberbandFactor = 0.30f,
                                        gap = animatedGapDp,
                                    )

                                    squishLeafIds = when (TilingEditMath.squishSideDirection(deltaDp)) {
                                        1 -> divider.secondLeafIds.toSet()
                                        -1 -> divider.firstLeafIds.toSet()
                                        else -> emptySet()
                                    }

                                    TilingEditMath.crossedSnapPoint(latestRatio, elasticRatio, lastSnapCrossed)
                                        ?.let { crossed ->
                                            lastSnapCrossed = crossed
                                            CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                        }

                                    latestRatio = elasticRatio
                                    onUpdateDividerRatio(page.id, divider.id, elasticRatio)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (effectiveEdit) {
                        val handleScale by animateFloatAsState(
                            targetValue = if (isDraggingThis) 1.18f else 1f,
                            animationSpec = CanopySprings.spec(SpringRole.GAP_OPEN, motionSettings),
                            label = "divider-handle-scale",
                        )
                        Box(
                            modifier = Modifier
                                .size(width = if (isHoriz) 6.dp else 36.dp, height = if (isHoriz) 36.dp else 6.dp)
                                .graphicsLayer {
                                    scaleX = handleScale
                                    scaleY = handleScale
                                }
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(if (isDraggingThis) Color(0xFF38BDF8) else theme.dividerHandleBg)
                        )
                    }
                }
            }
        }

        // 2D Intersection Crosshair Handles
        if (effectiveEdit) {
            layout.intersections.forEach { ix ->
                val isDraggingIx = draggingIntersectionId == ix.id
                val ixScale by animateFloatAsState(
                    targetValue = if (isDraggingIx) 1.18f else 1f,
                    animationSpec = CanopySprings.spec(SpringRole.GAP_OPEN, motionSettings),
                    label = "ix-scale-${ix.id}",
                )

                Box(
                    modifier = Modifier
                        .offset(x = (ix.x - 13f).dp, y = (ix.y - 13f).dp)
                        .size(26.dp)
                        .semantics {
                            contentDescription = "2D-Kreuzung ${ix.id}"
                            stateDescription = "${ix.horizontalSplits.size} horizontal, ${ix.verticalSplits.size} vertikal"
                        }
                        .graphicsLayer {
                            scaleX = ixScale
                            scaleY = ixScale
                        }
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isDraggingIx) Color(0xFF38BDF8) else theme.dividerHandleBg)
                        .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        .pointerInput(ix.id, animatedGapDp) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                draggingIntersectionId = ix.id

                                val hStates = ix.horizontalSplits.map { d ->
                                    SplitDragTrack(d, max(1f, d.parentRect.width - animatedGapDp), d.parentRect.width)
                                }
                                val vStates = ix.verticalSplits.map { d ->
                                    SplitDragTrack(d, max(1f, d.parentRect.height - animatedGapDp), d.parentRect.height)
                                }
                                var sumDxPx = 0f
                                var sumDyPx = 0f
                                var lastSnapCrossed: Float? = null
                                val velocityTracker = VelocityTracker()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)

                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull()
                                    if (ch == null || !ch.pressed) {
                                        draggingIntersectionId = null
                                        val vel = velocityTracker.calculateVelocity()
                                        val vxDp = with(density) { vel.x.toDp().value }
                                        val vyDp = with(density) { vel.y.toDp().value }
                                        val snapped = buildMap {
                                            hStates.forEach { s ->
                                                put(s.divider.id, TilingEngine.projectAndSnapRatio(s.latestRatio, vxDp, s.parentSpanDp, s.divider.minRatio, s.divider.maxRatio))
                                            }
                                            vStates.forEach { s ->
                                                put(s.divider.id, TilingEngine.projectAndSnapRatio(s.latestRatio, vyDp, s.parentSpanDp, s.divider.minRatio, s.divider.maxRatio))
                                            }
                                        }
                                        CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                                        onUpdateMultipleDividerRatios(page.id, snapped)
                                        break
                                    }

                                    ch.consume()
                                    velocityTracker.addPosition(ch.uptimeMillis, ch.position)
                                    val delta = ch.positionChange()
                                    sumDxPx += delta.x
                                    sumDyPx += delta.y
                                    val dxDp = with(density) { sumDxPx.toDp().value }
                                    val dyDp = with(density) { sumDyPx.toDp().value }

                                    val updates = buildMap {
                                        hStates.forEach { s ->
                                            val raw = s.divider.ratio + dxDp / s.usableSpanDp
                                            val next = TilingEngine.clampOrRubberbandRatio(raw, s.parentSpanDp, s.divider.minFirstPx, s.divider.minSecondPx, 0.30f, animatedGapDp)
                                            TilingEditMath.crossedSnapPoint(s.latestRatio, next, lastSnapCrossed)?.let { crossed ->
                                                lastSnapCrossed = crossed
                                                CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                            }
                                            s.latestRatio = next
                                            put(s.divider.id, next)
                                        }
                                        vStates.forEach { s ->
                                            val raw = s.divider.ratio + dyDp / s.usableSpanDp
                                            val next = TilingEngine.clampOrRubberbandRatio(raw, s.parentSpanDp, s.divider.minFirstPx, s.divider.minSecondPx, 0.30f, animatedGapDp)
                                            TilingEditMath.crossedSnapPoint(s.latestRatio, next, lastSnapCrossed)?.let { crossed ->
                                                lastSnapCrossed = crossed
                                                CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                            }
                                            s.latestRatio = next
                                            put(s.divider.id, next)
                                        }
                                    }
                                    onUpdateMultipleDividerRatios(page.id, updates)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenWith,
                        contentDescription = "2D-Kreuzung ziehen",
                        tint = if (isDraggingIx) Color(0xFF0F172A) else theme.dividerHandleDot,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }

        // Bottom Throw-to-Remove drop bar during active page tile drag
        if (activeDrag?.sourceKind == DragSourceKind.PAGE_TILE && isActivePage && layout.leaves.size > 1) {
            ThrowToRemoveDropZoneBar(
                highlighted = activeDrop is DropPreviewResult.RemoveTarget,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private class SplitDragTrack(
    val divider: ComputedDivider,
    val usableSpanDp: Float,
    val parentSpanDp: Float,
    var latestRatio: Float = divider.ratio,
)

@Composable
private fun SnapGuideLinesOverlay(guideDiv: ComputedDivider, gapDp: Float) {
    val isHoriz = guideDiv.direction == SplitDirection.HORIZONTAL
    val span = (if (isHoriz) guideDiv.parentRect.width else guideDiv.parentRect.height) - gapDp

    Box(
        modifier = Modifier
            .offset(x = guideDiv.parentRect.x.dp, y = guideDiv.parentRect.y.dp)
            .size(width = guideDiv.parentRect.width.dp, height = guideDiv.parentRect.height.dp)
    ) {
        TilingEngine.SNAP_POINTS.forEachIndexed { idx, p ->
            val pos = span * p + gapDp / 2f
            val isNear = abs(guideDiv.ratio - p) < 0.025f
            val lineColor = if (isNear) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.42f)

            Box(
                modifier = if (isHoriz) {
                    Modifier.offset(x = pos.dp).fillMaxHeight().width(if (isNear) 2.dp else 1.dp).background(lineColor)
                } else {
                    Modifier.offset(y = pos.dp).fillMaxWidth().height(if (isNear) 2.dp else 1.dp).background(lineColor)
                }
            )
            Text(
                text = TilingEditMath.fractionLabelAtIndex(idx),
                color = if (isNear) Color(0xFF0F172A) else Color.White,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .offset(
                        x = if (isHoriz) (pos - 10f).dp else 8.dp,
                        y = if (isHoriz) 8.dp else (pos - 9f).dp,
                    )
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isNear) Color(0xFF38BDF8) else Color(0xC70F172A))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
fun DropZonePreviewOverlay(
    zone: DropZone,
    radiusDp: Float,
    label: String = if (zone == DropZone.CENTER) "Tauschen" else "Teilen",
) {
    val corner = max(10f, radiusDp - 4f).dp
    Box(
        modifier = Modifier.fillMaxSize().padding(4.dp)
    ) {
        val zoneModifier = when (zone) {
            DropZone.CENTER -> Modifier.fillMaxSize()
            DropZone.LEFT -> Modifier.align(Alignment.CenterStart).fillMaxWidth(0.48f).fillMaxHeight()
            DropZone.RIGHT -> Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.48f).fillMaxHeight()
            DropZone.TOP -> Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.48f)
            DropZone.BOTTOM -> Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.48f)
        }
        Box(
            modifier = zoneModifier
                .clip(RoundedCornerShape(corner))
                .background(Color(0x5738BDF8))
                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(corner)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ThrowToRemoveDropZoneBar(highlighted: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) Color(0xEBEE4444) else Color(0x40EF4444))
            .border(1.5.dp, Color(0xD9FCA5A5), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Nach unten werfen zum Entfernen",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
