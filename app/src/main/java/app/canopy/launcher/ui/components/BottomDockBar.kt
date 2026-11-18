package app.canopy.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.ui.theme.CanopyThemes
import app.canopy.launcher.ui.theme.ThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

object PageScrubberMath {
    fun computeProgressStartFraction(
        pageOffsetPx: Float,
        pageWidthPx: Float,
        pageCount: Int,
    ): Float {
        if (pageCount <= 0 || pageWidthPx <= 1f) return 0f
        val maxIdx = max(1, pageCount - 1).toFloat()
        val p = (-pageOffsetPx / pageWidthPx).coerceIn(0f, maxIdx)
        return (p / pageCount.toFloat()).coerceIn(0f, 1f)
    }

    fun pageIndexForScrubX(
        xPx: Float,
        pillWidthPx: Float,
        pageCount: Int,
    ): Int {
        if (pageCount <= 1 || pillWidthPx <= 1f) return 0
        val ratio = (xPx / pillWidthPx).coerceIn(0f, 0.999f)
        return floor(ratio * pageCount).toInt().coerceIn(0, pageCount - 1)
    }

    fun shouldUseCompactLabels(pageCount: Int): Boolean = pageCount > 5
}

@Composable
fun BottomDockBar(
    pages: List<LauncherPage>,
    activePageIndex: Int,
    pageWidthPx: Float,
    pageOffsetPx: Float,
    theme: ThemeTokens,
    motionSettings: MotionSettings,
    showSearchActive: Boolean,
    showQuickShadeActive: Boolean,
    hasAnyOverlay: Boolean,
    onOpenSearch: () -> Unit,
    onToggleQuickShade: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onOpenOverview: () -> Unit,
    onHomeAction: () -> Unit,
    onBackAction: () -> Unit,
    modifier: Modifier = Modifier,
    dockHeightDp: Dp = 62.dp,
) {
    val dockTokens = CanopyThemes.getDockCapsuleTokens(theme)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(dockHeightDp)
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Three-part dock row (28 dp height)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left capsule: Search (38 x 28 dp)
            DockFlankCapsule(
                active = showSearchActive,
                dockBg = dockTokens.dockBg,
                activePillBg = dockTokens.activePillBg,
                activeTextColor = dockTokens.activeTextColor,
                inactiveTextColor = theme.statusText,
                borderColor = theme.sheetBorder,
                description = "Suche öffnen",
                onClick = {
                    CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                    onOpenSearch()
                },
            ) { tint ->
                Icon(Icons.Rounded.Search, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }

            // Center interactive PageIndicator scrubber pill
            Box(
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                PageIndicatorScrubber(
                    pages = pages,
                    activePageIndex = activePageIndex,
                    pageWidthPx = pageWidthPx,
                    pageOffsetPx = pageOffsetPx,
                    theme = theme,
                    motionSettings = motionSettings,
                    onSelectPage = onSelectPage,
                    onOpenOverview = onOpenOverview,
                )
            }

            // Right capsule: Quick Settings (38 x 28 dp)
            DockFlankCapsule(
                active = showQuickShadeActive,
                dockBg = dockTokens.dockBg,
                activePillBg = dockTokens.activePillBg,
                activeTextColor = dockTokens.activeTextColor,
                inactiveTextColor = theme.statusText,
                borderColor = theme.sheetBorder,
                description = "Schnelleinstellungen öffnen",
                onClick = {
                    CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                    onToggleQuickShade()
                },
            ) { tint ->
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }
        }

        // Android Home Pill + Back row
        Box(
            modifier = Modifier.fillMaxWidth().height(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (hasAnyOverlay) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .clip(CircleShape)
                        .background(Color(0x387D7D91))
                        .clickable {
                            CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                            onBackAction()
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = theme.statusText, modifier = Modifier.size(13.dp))
                    Text("Zurück", color = theme.statusText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(
                modifier = Modifier
                    .width(118.dp)
                    .height(18.dp)
                    .semantics { contentDescription = "Home-Taste (Startseite)" }
                    .clickable {
                        CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                        onHomeAction()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(theme.statusText.copy(alpha = 0.72f))
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(width = 26.dp, height = 24.dp)
                    .clip(CircleShape)
                    .background(Color(0x2E7D7D91))
                    .semantics { contentDescription = "Startseite" }
                    .clickable {
                        CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                        onHomeAction()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Home, contentDescription = null, tint = theme.statusText, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun DockFlankCapsule(
    active: Boolean,
    dockBg: Color,
    activePillBg: Color,
    activeTextColor: Color,
    inactiveTextColor: Color,
    borderColor: Color,
    description: String,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val bg by animateColorAsState(if (active) activePillBg else dockBg, label = "dock-capsule-bg")
    val fg by animateColorAsState(if (active) activeTextColor else inactiveTextColor, label = "dock-capsule-fg")
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 28.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .semantics { contentDescription = description }
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        icon(fg)
    }
}

@Composable
fun PageIndicatorScrubber(
    pages: List<LauncherPage>,
    activePageIndex: Int,
    pageWidthPx: Float,
    pageOffsetPx: Float,
    theme: ThemeTokens,
    motionSettings: MotionSettings,
    onSelectPage: (Int) -> Unit,
    onOpenOverview: () -> Unit,
) {
    val dockTokens = CanopyThemes.getDockCapsuleTokens(theme)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var pillWidthPx by remember { mutableFloatStateOf(1f) }
    val scrubSlopPx = with(density) { 10.dp.toPx() }
    val isCompact = PageScrubberMath.shouldUseCompactLabels(pages.size)

    BoxWithConstraints(
        modifier = Modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(dockTokens.dockBg)
            .border(1.dp, theme.sheetBorder, CircleShape)
            .onSizeChanged { pillWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(pages.size, activePageIndex) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    var scrubbing = false
                    var longPressed = false
                    var lastScrubIdx = activePageIndex

                    val longPressJob = scope.launch {
                        delay(420L)
                        if (!scrubbing) {
                            longPressed = true
                            CanopyHaptics.trigger(HapticKind.EDIT_ENTER, motionSettings.hapticsEnabled)
                            onOpenOverview()
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            longPressJob.cancel()
                            if (!scrubbing && !longPressed) {
                                val tappedIdx = PageScrubberMath.pageIndexForScrubX(
                                    xPx = down.position.x,
                                    pillWidthPx = pillWidthPx,
                                    pageCount = pages.size,
                                )
                                CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                onSelectPage(tappedIdx)
                            }
                            break
                        }

                        if (abs(change.position.x - startX) > scrubSlopPx) {
                            scrubbing = true
                            longPressJob.cancel()
                            change.consume()
                            val idx = PageScrubberMath.pageIndexForScrubX(
                                xPx = change.position.x,
                                pillWidthPx = pillWidthPx,
                                pageCount = pages.size,
                            )
                            if (idx != lastScrubIdx) {
                                lastScrubIdx = idx
                                CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                onSelectPage(idx)
                            }
                        } else if (change.positionChange() != androidx.compose.ui.geometry.Offset.Zero && scrubbing) {
                            change.consume()
                        }
                    }
                }
            }
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        val totalW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val count = pages.size.coerceAtLeast(1)
        val segWidthPx = totalW / count
        val startFrac = PageScrubberMath.computeProgressStartFraction(pageOffsetPx, pageWidthPx, count)

        // Subtle gliding 2dp bottom progress bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset((startFrac * totalW).roundToInt(), 3) }
                .width(with(density) { segWidthPx.toDp() })
                .height(2.dp)
                .clip(CircleShape)
                .background(theme.pillActive.copy(alpha = 0.45f))
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.forEachIndexed { idx, page ->
                val isAllApps = page.kind == PageKind.ALL_APPS
                val isActive = idx == activePageIndex
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .widthIn(min = if (isAllApps) 24.dp else 32.dp)
                        .clip(CircleShape)
                        .background(if (isActive) dockTokens.activePillBg else Color.Transparent)
                        .padding(horizontal = if (isAllApps) 5.dp else if (isCompact) 4.dp else 7.dp)
                        .alpha(if (isActive) 1f else 0.72f),
                    contentAlignment = Alignment.Center,
                ) {
                    val textColor = if (isActive) dockTokens.activeTextColor else theme.statusText
                    if (isAllApps) {
                        Icon(
                            imageVector = Icons.Rounded.GridView,
                            contentDescription = "Seite ${page.title}",
                            tint = textColor,
                            modifier = Modifier.size(12.dp),
                        )
                    } else {
                        Text(
                            text = page.title,
                            color = textColor,
                            fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
