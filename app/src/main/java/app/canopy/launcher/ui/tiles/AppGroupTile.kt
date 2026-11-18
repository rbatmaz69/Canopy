package app.canopy.launcher.ui.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.ui.components.AppSquircleIcon
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class BentoGridSpec(
    val isCompactVerticalList: Boolean,
    val cols: Int,
    val rows: Int,
    val capacity: Int,
    val visibleCount: Int,
    val hiddenCount: Int,
    val needsOverflow: Boolean,
    val iconSizeDp: Float,
    val showLabels: Boolean,
)

object BentoGridPlanner {
    fun computeSpec(
        widthDp: Float,
        heightDp: Float,
        appCount: Int,
        tier: TileSizeTier,
    ): BentoGridSpec {
        if (tier == TileSizeTier.XS) {
            return BentoGridSpec(
                isCompactVerticalList = false,
                cols = 1,
                rows = 1,
                capacity = 1,
                visibleCount = 0,
                hiddenCount = appCount,
                needsOverflow = appCount > 0,
                iconSizeDp = 36f,
                showLabels = false,
            )
        }

        if (tier == TileSizeTier.TICKER) {
            val maxInline = max(2, floor((widthDp - 110f) / 46f).toInt())
            val overflow = max(0, appCount - maxInline)
            val visible = if (overflow > 0) max(1, maxInline - 1) else min(appCount, maxInline)
            return BentoGridSpec(
                isCompactVerticalList = false,
                cols = maxInline,
                rows = 1,
                capacity = maxInline,
                visibleCount = visible,
                hiddenCount = appCount - visible,
                needsOverflow = overflow > 0,
                iconSizeDp = 38f,
                showLabels = false,
            )
        }

        if (appCount <= 3 && widthDp >= 135f && heightDp >= 140f) {
            return BentoGridSpec(
                isCompactVerticalList = true,
                cols = 1,
                rows = appCount.coerceAtLeast(1),
                capacity = appCount.coerceAtLeast(1),
                visibleCount = appCount,
                hiddenCount = 0,
                needsOverflow = false,
                iconSizeDp = 36f,
                showLabels = true,
            )
        }

        val innerW = max(44f, widthDp - 26f)
        val headerH = 26f
        val innerH = max(44f, heightDp - 26f - headerH)

        val is4x2EightPack = appCount == 8 && widthDp >= 250f && heightDp >= 130f
        val is2x2FourPack = appCount == 4 && widthDp >= 130f && heightDp >= 130f
        val is2x2CompactFive = appCount == 5 && widthDp < 220f && heightDp >= 130f
        val is3x2MediumEight = appCount == 8 && widthDp < 250f && heightDp >= 130f
        val showLabels = heightDp >= 140f && widthDp >= 130f
        val cellH = if (showLabels) 58f else 44f

        val cols = when {
            is4x2EightPack -> 4
            is2x2FourPack || is2x2CompactFive -> 2
            is3x2MediumEight -> 3
            widthDp > 290f -> 5
            widthDp > 185f -> 3
            else -> 2
        }

        val isWideOverflowStrip = widthDp > 290f && heightDp < 275f && appCount > 10
        val maxRows = if (isWideOverflowStrip) {
            2
        } else {
            max(1, min(4, floor((innerH + 6f) / (cellH + 6f)).toInt()))
        }

        val rows = if (is4x2EightPack || is2x2FourPack || is2x2CompactFive || is3x2MediumEight) {
            2
        } else {
            max(1, min(maxRows, ceil(appCount.toDouble() / cols).toInt()))
        }

        val capacity = cols * rows
        val needsOverflow = appCount > capacity
        val visibleCount = if (needsOverflow) max(0, capacity - 1) else appCount
        val hiddenCount = appCount - visibleCount

        val maxIconByW = floor((innerW - (cols - 1) * 6f) / cols) - 10f
        val maxIconByH = floor((innerH - (rows - 1) * 6f) / rows) - (if (showLabels) 22f else 8f)
        val iconSize = min(60f, max(36f, min(maxIconByW, maxIconByH)))

        return BentoGridSpec(
            isCompactVerticalList = false,
            cols = cols,
            rows = rows,
            capacity = capacity,
            visibleCount = visibleCount,
            hiddenCount = hiddenCount,
            needsOverflow = needsOverflow,
            iconSizeDp = iconSize,
            showLabels = showLabels,
        )
    }
}

@Composable
fun AppGroupTile(props: TileContentProps) {
    val appsById = props.allAppsCatalog.associateBy { it.id }.ifEmpty { MockCatalogData.APPS_BY_ID }
    val apps = (props.tile.appIds ?: emptyList()).mapNotNull { appsById[it] }
    var tileBounds: Rect? = null

    val openOverlay = {
        if (!props.isEditMode) {
            props.onOpenGroup(props.tile, props.leafId, tileBounds)
        }
    }

    val spec = BentoGridPlanner.computeSpec(
        widthDp = props.widthDp,
        heightDp = props.heightDp,
        appCount = apps.size,
        tier = props.tier,
    )

    if (props.tier == TileSizeTier.XS) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { tileBounds = it.boundsInRoot() }
                .clickable(enabled = !props.isEditMode) { openOverlay() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(props.surface.accentBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Folder, contentDescription = props.tile.title, tint = props.surface.accentColor, modifier = Modifier.size(22.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .clip(CircleShape)
                        .background(props.surface.accentColor)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = apps.size.toString(),
                        color = Color.White,
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    )
                }
            }
        }
        return
    }

    if (props.tier == TileSizeTier.TICKER) {
        val visibleApps = apps.take(spec.visibleCount)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { tileBounds = it.boundsInRoot() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = props.tile.title,
                color = props.surface.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !props.isEditMode) { openOverlay() },
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                visibleApps.forEach { app ->
                    AppSquircleIcon(
                        appId = app.id,
                        iconKey = app.iconKey,
                        color = app.color,
                        size = 38.dp,
                        badge = app.badge,
                        onClick = if (props.isEditMode) null else { rect -> props.onOpenApp(app.id, rect) },
                    )
                }
                if (spec.needsOverflow) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(props.surface.accentBg)
                            .clickable(enabled = !props.isEditMode) { openOverlay() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+${spec.hiddenCount}",
                            color = props.surface.accentColor,
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        )
                    }
                }
            }
        }
        return
    }

    if (spec.isCompactVerticalList) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { tileBounds = it.boundsInRoot() },
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !props.isEditMode) { openOverlay() }
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(props.tile.title, color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(apps.size.toString(), color = props.surface.textSecondary, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
            }

            Column(
                modifier = Modifier.weight(1f).padding(top = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                apps.forEach { app ->
                    var rowRect: Rect? = null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 40.dp)
                            .onGloballyPositioned { rowRect = it.boundsInRoot() }
                            .clickable(enabled = !props.isEditMode) { props.onOpenApp(app.id, rowRect) }
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        AppSquircleIcon(appId = app.id, iconKey = app.iconKey, color = app.color, size = 36.dp, badge = app.badge)
                        Column(Modifier.weight(1f)) {
                            Text(app.name, color = props.surface.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.subtitle, color = props.surface.textSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        return
    }

    val visibleApps = apps.take(spec.visibleCount)
    val bentoCellBg = when {
        props.theme.isGlass -> Color(0x14FFFFFF)
        props.theme.mode == ColorMode.DARK || props.theme.isBold -> Color(0x0DFFFFFF)
        else -> Color(0x0A0F172A)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { tileBounds = it.boundsInRoot() },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !props.isEditMode) { openOverlay() }
                .padding(horizontal = 2.dp)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(props.tile.title, color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = apps.size.toString(),
                color = props.surface.textSecondary,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val totalCells = visibleApps.size + (if (spec.needsOverflow) 1 else 0)
            for (r in 0 until spec.rows) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (c in 0 until spec.cols) {
                        val cellIndex = r * spec.cols + c
                        if (cellIndex < visibleApps.size) {
                            val app = visibleApps[cellIndex]
                            var cellRect: Rect? = null
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bentoCellBg)
                                    .onGloballyPositioned { cellRect = it.boundsInRoot() }
                                    .clickable(enabled = !props.isEditMode) { props.onOpenApp(app.id, cellRect) }
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                AppSquircleIcon(
                                    appId = app.id,
                                    iconKey = app.iconKey,
                                    color = app.color,
                                    size = spec.iconSizeDp.dp,
                                    badge = app.badge,
                                )
                                if (spec.showLabels) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = app.name,
                                        color = props.surface.textSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        } else if (spec.needsOverflow && cellIndex == visibleApps.size && cellIndex < totalCells) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(props.surface.accentBg)
                                    .border(1.5.dp, props.surface.accentColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                    .clickable(enabled = !props.isEditMode) { openOverlay() }
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = "+${spec.hiddenCount}",
                                    color = props.surface.accentColor,
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                                )
                                if (spec.showLabels) {
                                    Spacer(Modifier.height(2.dp))
                                    Text("Alle", color = props.surface.accentColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
