package app.canopy.launcher.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.AlarmState
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.layout.DropZone
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.RectFBounds
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.ui.theme.ThemeTokens
import app.canopy.launcher.ui.tiles.AppDefinition
import app.canopy.launcher.ui.tiles.CalendarEvent
import app.canopy.launcher.ui.tiles.MediaPlayerState
import app.canopy.launcher.ui.tiles.SystemControlState
import app.canopy.launcher.ui.tiles.TileRenderer
import kotlin.math.roundToInt

enum class EdgeFlipSide {
    LEFT,
    RIGHT,
}

data class UndoSnapshot(
    val pages: List<LauncherPage>,
    val removedTitle: String,
)

data class DropOutcome(
    val pages: List<LauncherPage>,
    val undoSnapshot: UndoSnapshot? = null,
    val hapticKind: HapticKind? = null,
)

object TileDragEngine {
    const val EDGE_HOLD_THRESHOLD_DP = 30f
    const val EDGE_HOLD_DELAY_MS = 450L
    const val UNDO_WINDOW_MS = 5000L
    const val THROW_VELOCITY_DP_PER_SEC = 680f

    fun computeDropPreview(
        drag: ActiveTileDrag?,
        activePage: LauncherPage?,
        boundsDp: RectFBounds,
        gapDp: Float,
        pointerLocalXDp: Float,
        pointerLocalYDp: Float,
    ): DropPreviewResult? {
        if (drag == null || activePage == null || activePage.kind == PageKind.ALL_APPS) {
            return null
        }

        val layout = TilingEngine.computeLayout(activePage.root, boundsDp, gapDp)

        if (drag.sourceKind == DragSourceKind.PAGE_TILE &&
            pointerLocalYDp > boundsDp.y + boundsDp.height - 6f &&
            layout.leaves.size > 1
        ) {
            return DropPreviewResult.RemoveTarget(activePage.id)
        }

        for (leaf in layout.leaves) {
            if (drag.sourceKind == DragSourceKind.PAGE_TILE &&
                leaf.id == drag.sourceLeafId &&
                activePage.id == drag.sourcePageId
            ) {
                continue
            }
            if (pointerLocalXDp in leaf.rect.x..leaf.rect.right &&
                pointerLocalYDp in leaf.rect.y..leaf.rect.bottom
            ) {
                val zone = TilingEngine.determineDropZone(
                    relX = pointerLocalXDp - leaf.rect.x,
                    relY = pointerLocalYDp - leaf.rect.y,
                    width = leaf.rect.width,
                    height = leaf.rect.height,
                )
                return DropPreviewResult.LeafTarget(activePage.id, leaf, zone)
            }
        }

        if (drag.sourceKind == DragSourceKind.CATALOG_TILE && layout.leaves.isNotEmpty()) {
            return DropPreviewResult.LeafTarget(
                pageId = activePage.id,
                leaf = layout.leaves.last(),
                zone = DropZone.BOTTOM,
            )
        }

        return null
    }

    fun resolveEdgeFlipDirection(
        pointerLocalXDp: Float,
        screenWidthDp: Float,
        activePageIndex: Int,
        pages: List<LauncherPage>,
    ): EdgeFlipSide? {
        val maxRegularIdx = pages.count { it.kind != PageKind.ALL_APPS } - 1
        return when {
            pointerLocalXDp < EDGE_HOLD_THRESHOLD_DP && activePageIndex > 0 -> EdgeFlipSide.LEFT
            pointerLocalXDp > screenWidthDp - EDGE_HOLD_THRESHOLD_DP && activePageIndex < maxRegularIdx -> EdgeFlipSide.RIGHT
            else -> null
        }
    }

    fun shouldThrowRemove(
        dropPreview: DropPreviewResult?,
        drag: ActiveTileDrag,
        pointerLocalYDp: Float,
        viewportHeightDp: Float,
        velocityYDpPerSec: Float,
    ): Boolean {
        if (dropPreview is DropPreviewResult.RemoveTarget) return true
        if (drag.sourceKind == DragSourceKind.PAGE_TILE &&
            velocityYDpPerSec > THROW_VELOCITY_DP_PER_SEC &&
            pointerLocalYDp > viewportHeightDp * 0.65f
        ) {
            return true
        }
        return false
    }

    fun applyDropMutation(
        pages: List<LauncherPage>,
        drag: ActiveTileDrag,
        dropPreview: DropPreviewResult?,
        shouldRemove: Boolean,
    ): DropOutcome {
        if (shouldRemove && drag.sourceKind == DragSourceKind.PAGE_TILE &&
            drag.sourcePageId != null && drag.sourceLeafId != null
        ) {
            val srcPage = pages.firstOrNull { it.id == drag.sourcePageId } ?: return DropOutcome(pages)
            val removed = TilingEngine.removeLeaf(srcPage.root, drag.sourceLeafId)
            val remainingTree = removed.tree ?: return DropOutcome(pages)
            val updated = pages.map { p ->
                if (p.id == drag.sourcePageId) p.copy(root = remainingTree) else p
            }
            return DropOutcome(
                pages = updated,
                undoSnapshot = UndoSnapshot(pages = pages, removedTitle = drag.tile.title),
                hapticKind = HapticKind.REMOVE,
            )
        }

        if (dropPreview is DropPreviewResult.LeafTarget) {
            val targetLeafId = dropPreview.leaf.id
            val zone = dropPreview.zone
            val safeEdgeZone = if (zone == DropZone.CENTER) DropZone.BOTTOM else zone

            if (drag.sourceKind == DragSourceKind.CATALOG_TILE) {
                val updated = pages.map { p ->
                    if (p.id == dropPreview.pageId) {
                        p.copy(root = TilingEngine.splitLeaf(p.root, targetLeafId, drag.tile, safeEdgeZone, 0.5f))
                    } else {
                        p
                    }
                }
                return DropOutcome(updated, hapticKind = HapticKind.SNAP)
            }

            if (drag.sourcePageId == dropPreview.pageId && drag.sourceLeafId != null) {
                val updated = pages.map { p ->
                    if (p.id == dropPreview.pageId) {
                        p.copy(
                            root = TilingEngine.moveLeafToSplit(
                                root = p.root,
                                sourceLeafId = drag.sourceLeafId,
                                targetLeafId = targetLeafId,
                                dropZone = zone,
                                ratio = 0.5f,
                            )
                        )
                    } else {
                        p
                    }
                }
                return DropOutcome(updated, hapticKind = HapticKind.SNAP)
            }

            if (drag.sourcePageId != null && drag.sourceLeafId != null) {
                val srcPage = pages.firstOrNull { it.id == drag.sourcePageId } ?: return DropOutcome(pages)
                val removed = TilingEngine.removeLeaf(srcPage.root, drag.sourceLeafId)
                val remainingSourceTree = removed.tree ?: return DropOutcome(pages) // Never empty source page
                val updated = pages.map { p ->
                    when (p.id) {
                        drag.sourcePageId -> p.copy(root = remainingSourceTree)
                        dropPreview.pageId -> p.copy(
                            root = TilingEngine.splitLeaf(
                                root = p.root,
                                targetLeafId = targetLeafId,
                                newTile = drag.tile,
                                dropZone = safeEdgeZone,
                                ratio = 0.5f,
                                newLeafId = drag.sourceLeafId,
                            )
                        )
                        else -> p
                    }
                }
                return DropOutcome(updated, hapticKind = HapticKind.SNAP)
            }
        }

        return DropOutcome(pages)
    }
}

@Composable
fun FloatingDragGhostOverlay(
    drag: ActiveTileDrag?,
    screenOriginRootX: Float,
    screenOriginRootY: Float,
    theme: ThemeTokens,
    motionSettings: MotionSettings,
    alarmState: AlarmState,
    calendarEvents: List<CalendarEvent>,
    mediaState: MediaPlayerState,
    systemState: SystemControlState,
    allAppsCatalog: List<AppDefinition>,
) {
    if (drag == null) return
    val density = LocalDensity.current
    val ghostW = drag.widthDp * 0.9f
    val ghostH = drag.heightDp * 0.9f
    val localXPx = drag.pointerXRootPx - screenOriginRootX
    val localYPx = drag.pointerYRootPx - screenOriginRootY
    val offsetX = (localXPx - with(density) { (ghostW * 0.5f).dp.toPx() }).roundToInt()
    val offsetY = (localYPx - with(density) { (ghostH * 0.5f).dp.toPx() }).roundToInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(width = ghostW.dp, height = ghostH.dp)
            .graphicsLayer {
                scaleX = 1.04f
                scaleY = 1.04f
            }
            .alpha(0.94f)
            .shadow(18.dp)
    ) {
        TileRenderer(
            tile = drag.tile,
            leafId = "drag-ghost",
            widthDp = ghostW,
            heightDp = ghostH,
            theme = theme,
            motionSettings = motionSettings,
            isEditMode = false,
            alarmState = alarmState,
            onToggleAlarm = {},
            onOpenApp = { _, _ -> },
            onOpenGroup = { _, _, _ -> },
            onOpenSearch = {},
            onOpenFakeTileApp = { _, _ -> },
            onUpdateTile = { _, _ -> },
            calendarEvents = calendarEvents,
            mediaState = mediaState,
            systemState = systemState,
            allAppsCatalog = allAppsCatalog,
        )
    }
}

@Composable
fun UndoTileRemovalBanner(
    snapshot: UndoSnapshot?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = snapshot != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        snapshot?.let { snap ->
            Row(
                modifier = Modifier
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onUndo() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Rückgängig",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "„${snap.removedTitle}“ entfernt · Rückgängig",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
