package app.canopy.launcher.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.CanopySprings
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.motion.SpringRole
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class InterpolatedBounds(
    val xPx: Float,
    val yPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val cornerRadiusDp: Float,
)

object PagingPhysics {
    const val EDGE_RUBBERBAND_RESISTANCE = 0.30f
    const val WALLPAPER_PARALLAX_FACTOR = 0.15f
    const val FLING_VELOCITY_THRESHOLD_PX_PER_SEC = 320f

    fun applyEdgeRubberband(
        rawOffsetPx: Float,
        minOffsetPx: Float,
        maxOffsetPx: Float = 0f,
        resistance: Float = EDGE_RUBBERBAND_RESISTANCE,
    ): Float = when {
        rawOffsetPx > maxOffsetPx -> maxOffsetPx + (rawOffsetPx - maxOffsetPx) * resistance
        rawOffsetPx < minOffsetPx -> minOffsetPx + (rawOffsetPx - minOffsetPx) * resistance
        else -> rawOffsetPx
    }

    fun computeParallaxOffsetPx(pageOffsetPx: Float): Float =
        pageOffsetPx * WALLPAPER_PARALLAX_FACTOR

    fun resolveTargetPageIndex(
        offsetPx: Float,
        pageWidthPx: Float,
        velocityPxPerSec: Float,
        pageCount: Int,
    ): Int {
        if (pageCount <= 1 || pageWidthPx <= 1f) return 0
        val rawIdx = (-offsetPx / pageWidthPx).coerceIn(0f, (pageCount - 1).toFloat())
        val target = if (abs(velocityPxPerSec) > FLING_VELOCITY_THRESHOLD_PX_PER_SEC) {
            if (velocityPxPerSec < 0f) ceil(rawIdx).toInt() else floor(rawIdx).toInt()
        } else {
            rawIdx.roundToInt()
        }
        return target.coerceIn(0, pageCount - 1)
    }

    fun computeSharedElementProgressBounds(
        sourceRect: Rect?,
        containerWidthPx: Float,
        containerHeightPx: Float,
        tileRadiusDp: Float,
        progress: Float,
    ): InterpolatedBounds {
        val t = progress.coerceIn(0f, 1f)
        val startX = sourceRect?.left ?: (containerWidthPx * 0.2f)
        val startY = sourceRect?.top ?: (containerHeightPx * 0.35f)
        val startW = max(48f, sourceRect?.width ?: (containerWidthPx * 0.6f))
        val startH = max(48f, sourceRect?.height ?: 160f)

        return InterpolatedBounds(
            xPx = startX + (0f - startX) * t,
            yPx = startY + (0f - startY) * t,
            widthPx = startW + (containerWidthPx - startW) * t,
            heightPx = startH + (containerHeightPx - startH) * t,
            cornerRadiusDp = tileRadiusDp * (1f - t),
        )
    }
}

/**
 * High-framerate 1:1 horizontal pager with 30% boundary rubberband,
 * 15% wallpaper parallax signal, swipe-up search trigger, and pinch-to-overview trigger.
 */
@Composable
fun HorizontalTilingPager(
    pages: List<LauncherPage>,
    activePageIndex: Int,
    pageWidthPx: Float,
    pageOffsetAnim: Animatable<Float, *>,
    motionSettings: MotionSettings,
    isEditMode: Boolean,
    gestureDisabled: Boolean,
    onSnapToPage: (targetIndex: Int, velocityPxPerSec: Float) -> Unit,
    onSwipeUpSearch: () -> Unit,
    onPinchOverview: () -> Unit,
    modifier: Modifier = Modifier,
    pageContent: @Composable (index: Int, page: LauncherPage) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pageWidthDp = with(density) { pageWidthPx.toDp() }
    val swipeSlopPx = with(density) { 9.dp.toPx() }
    val swipeUpThresholdPx = with(density) { 46.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pages.size, pageWidthPx, activePageIndex, isEditMode, gestureDisabled) {
                if (gestureDisabled || pageWidthPx <= 1f) return@pointerInput

                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    var gestureLock: Char = 'n' // 'n'=none, 'x'=horizontal, 'y'=vertical, 'p'=pinch
                    var totalDx = 0f
                    var totalDy = 0f
                    var cumulativeZoom = 1f
                    var pinchTriggered = false
                    val velocityTracker = VelocityTracker()
                    val startOffset = pageOffsetAnim.value
                    val minOffset = -(pages.size - 1).coerceAtLeast(0) * pageWidthPx

                    scope.launch { pageOffsetAnim.stop() }
                    velocityTracker.addPosition(firstDown.uptimeMillis, firstDown.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }

                        if (pressedCount >= 2) {
                            gestureLock = 'p'
                            val zoom = event.calculateZoom()
                            cumulativeZoom *= zoom
                            event.changes.forEach { it.consume() }
                            if (cumulativeZoom < 0.82f && !pinchTriggered) {
                                pinchTriggered = true
                                CanopyHaptics.trigger(HapticKind.EDIT_ENTER, motionSettings.hapticsEnabled)
                                onPinchOverview()
                            }
                            continue
                        }

                        val change = event.changes.firstOrNull { it.id == firstDown.id }
                            ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            if (gestureLock == 'x') {
                                val vx = velocityTracker.calculateVelocity().x
                                val target = PagingPhysics.resolveTargetPageIndex(
                                    offsetPx = pageOffsetAnim.value,
                                    pageWidthPx = pageWidthPx,
                                    velocityPxPerSec = vx,
                                    pageCount = pages.size,
                                )
                                if (target != activePageIndex) {
                                    CanopyHaptics.trigger(HapticKind.PAGE_SETTLE, motionSettings.hapticsEnabled)
                                } else {
                                    CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                }
                                onSnapToPage(target, vx)
                            } else if (gestureLock == 'y') {
                                val currentPage = pages.getOrNull(activePageIndex)
                                if (totalDy < -swipeUpThresholdPx && !isEditMode && currentPage?.kind != PageKind.ALL_APPS) {
                                    CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                                    onSwipeUpSearch()
                                }
                            }
                            break
                        }

                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        if (gestureLock == 'n' && (abs(totalDx) > swipeSlopPx || abs(totalDy) > swipeSlopPx)) {
                            gestureLock = if (abs(totalDx) >= abs(totalDy)) 'x' else 'y'
                        }

                        if (gestureLock == 'x') {
                            change.consume()
                            val rubberbanded = PagingPhysics.applyEdgeRubberband(
                                rawOffsetPx = startOffset + totalDx,
                                minOffsetPx = minOffset,
                                maxOffsetPx = 0f,
                            )
                            scope.launch { pageOffsetAnim.snapTo(rubberbanded) }
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .width(pageWidthDp * pages.size.coerceAtLeast(1))
                .graphicsLayer {
                    translationX = pageOffsetAnim.value
                }
        ) {
            pages.forEachIndexed { idx, page ->
                Box(
                    modifier = Modifier
                        .width(pageWidthDp)
                        .fillMaxHeight()
                ) {
                    pageContent(idx, page)
                }
            }
        }
    }
}

/**
 * Expands content from sourceRect to fullscreen using Canopy's GROUP_EXPAND spring.
 */
@Composable
fun SharedElementExpandContainer(
    sourceRect: Rect?,
    containerWidthPx: Float,
    containerHeightPx: Float,
    tileRadiusDp: Float,
    motionSettings: MotionSettings,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(sourceRect) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = CanopySprings.spec(SpringRole.GROUP_EXPAND, motionSettings),
        )
    }

    val bounds = PagingPhysics.computeSharedElementProgressBounds(
        sourceRect = sourceRect,
        containerWidthPx = containerWidthPx,
        containerHeightPx = containerHeightPx,
        tileRadiusDp = tileRadiusDp,
        progress = progress.value,
    )

    val widthDp = with(density) { bounds.widthPx.toDp() }
    val heightDp = with(density) { bounds.heightPx.toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset(bounds.xPx.roundToInt(), bounds.yPx.roundToInt()) }
            .size(width = widthDp, height = heightDp)
            .clip(RoundedCornerShape(bounds.cornerRadiusDp.dp))
    ) {
        content()
    }
}

fun Modifier.canopyPressFeedback(
    enabled: Boolean,
    motionSettings: MotionSettings,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.96f else 1f,
        animationSpec = CanopySprings.spec(SpringRole.PRESS_FEEDBACK, motionSettings),
        label = "canopy-press-scale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                }
            )
        }
}
