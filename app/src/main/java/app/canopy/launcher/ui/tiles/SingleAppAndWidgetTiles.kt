package app.canopy.launcher.ui.tiles

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.layout.WidgetKind
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.system.LocalHardwareTelemetry
import app.canopy.launcher.ui.components.AppSquircleIcon
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Provides an optional Android AppWidgetHost creator for native widget tiles.
 */
val LocalAppWidgetHostFactory = staticCompositionLocalOf<((Int) -> AppWidgetHostView?)?> { null }

data class WidgetSizeRangeDp(
    val minWidthDp: Int,
    val minHeightDp: Int,
    val maxWidthDp: Int,
    val maxHeightDp: Int,
)

object WidgetFrameEngine {
    const val PASSEPARTOUT_MARGIN_DP = 10f

    fun fitsPassepartoutWidget(
        tileWidthDp: Float,
        tileHeightDp: Float,
        fixedWidthDp: Float,
        fixedHeightDp: Float,
    ): Boolean = tileWidthDp >= fixedWidthDp + PASSEPARTOUT_MARGIN_DP &&
        tileHeightDp >= fixedHeightDp + PASSEPARTOUT_MARGIN_DP

    fun computeBreathingScale(
        tileWidthDp: Float,
        tileHeightDp: Float,
        fixedWidthDp: Float,
        fixedHeightDp: Float,
    ): Float {
        val raw = min((tileWidthDp - 20f) / max(1f, fixedWidthDp), (tileHeightDp - 42f) / max(1f, fixedHeightDp))
        return min(1.15f, max(0.95f, raw))
    }

    fun computeWidgetSizeBundleRanges(
        tileWidthDp: Float,
        tileHeightDp: Float,
        fixedWidthDp: Float,
        fixedHeightDp: Float,
    ): WidgetSizeRangeDp {
        val usableW = max(56f, tileWidthDp - 16f)
        val usableH = max(56f, tileHeightDp - 34f)
        val minW = max(48, min(usableW, fixedWidthDp).roundToInt())
        val minH = max(48, min(usableH, fixedHeightDp).roundToInt())
        val maxW = max(minW, usableW.roundToInt())
        val maxH = max(minH, usableH.roundToInt())
        return WidgetSizeRangeDp(
            minWidthDp = minW,
            minHeightDp = minH,
            maxWidthDp = maxW,
            maxHeightDp = maxH,
        )
    }
}

@Composable
fun SingleAppTile(props: TileContentProps) {
    val installed = remember(props.allAppsCatalog, props.tile.appId) {
        props.allAppsCatalog.firstOrNull { it.id == props.tile.appId }
    }
    val app = installed ?: AppDefinition(
        id = props.tile.appId ?: "app-fallback",
        name = props.tile.title.ifBlank { "App" },
        iconKey = "Compass",
        color = Color(0xFF3B82F6),
        hue = props.tile.accentHue ?: 217f,
        category = AppCategory.WERKZEUGE,
        usageScore = 50,
        subtitle = if (props.allAppsCatalog.isEmpty()) "" else "Nicht installiert",
    )
    // Package names are not useful to show; only show a subtitle that reads like text.
    val subtitle = app.subtitle.takeUnless { it.contains('.') && !it.contains(' ') }.orEmpty()
    var boundsRect: Rect? = null

    val handleTap = {
        if (!props.isEditMode) {
            CanopyHaptics.trigger(HapticKind.PRESS, props.motionSettings.hapticsEnabled)
            props.onOpenApp(app.id, boundsRect)
        }
    }

    when (props.tier) {
        TileSizeTier.XS -> Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { boundsRect = it.boundsInRoot() }
                .clickable(enabled = !props.isEditMode) { handleTap() },
            contentAlignment = Alignment.Center,
        ) {
            AppSquircleIcon(appId = app.id, iconKey = app.iconKey, color = app.color, size = 42.dp, badge = app.badge)
        }

        TileSizeTier.TICKER -> Row(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { boundsRect = it.boundsInRoot() }
                .clickable(enabled = !props.isEditMode) { handleTap() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppSquircleIcon(appId = app.id, iconKey = app.iconKey, color = app.color, size = 36.dp, badge = app.badge)
            Column(Modifier.weight(1f)) {
                Text(app.name, color = props.surface.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = props.surface.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { boundsRect = it.boundsInRoot() }
                .clickable(enabled = !props.isEditMode) { handleTap() },
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AppSquircleIcon(
                appId = app.id,
                iconKey = app.iconKey,
                color = app.color,
                size = if (props.tier == TileSizeTier.L) 54.dp else 44.dp,
                badge = app.badge,
            )
            Column {
                Text(app.name, color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (props.tier != TileSizeTier.S && subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = props.surface.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ThirdPartyWidgetTile(props: TileContentProps) {
    val isPodcast = props.tile.widgetKind != WidgetKind.FITNESS
    val fixedW = props.tile.widgetFixedWidth ?: if (isPodcast) 158f else 148f
    val fixedH = props.tile.widgetFixedHeight ?: if (isPodcast) 148f else 112f

    // Automatic fallback to compact SingleAppTile when tile shrinks below widget minimum
    if (!WidgetFrameEngine.fitsPassepartoutWidget(props.widthDp, props.heightDp, fixedW, fixedH)) {
        SingleAppTile(props)
        return
    }

    val fitScale = WidgetFrameEngine.computeBreathingScale(props.widthDp, props.heightDp, fixedW, fixedH)
    val passepartoutBg = if (props.theme.isGlass) Color(0x24FFFFFF) else props.surface.accentBg
    val useDarkInner = props.theme.mode == ColorMode.DARK || props.theme.isBold || props.theme.isGlass
    val innerCardBg = if (useDarkInner) Color(0xE011141E) else Color(0xF0FFFFFF)
    val innerText = if (useDarkInner) Color.White else Color(0xFF111827)
    val innerSub = if (useDarkInner) Color.White.copy(alpha = 0.72f) else Color(0xFF4B5563)

    val hostFactory = LocalAppWidgetHostFactory.current
    val appWidgetId = props.tile.androidAppWidgetId
    var openBtnRect: Rect? = null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(max(12f, props.theme.tileRadiusDp - 6f).dp))
            .background(passepartoutBg)
            .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Functional Passepartout Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(props.surface.accentColor)
                )
                Text(
                    text = props.tile.title.ifBlank { "Widget" },
                    color = props.surface.textSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (props.tile.appId != null) Text(
                text = "Öffnen ↗",
                color = props.surface.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x2E7D7D91))
                    .onGloballyPositioned { openBtnRect = it.boundsInRoot() }
                    .clickable(enabled = !props.isEditMode) {
                        CanopyHaptics.trigger(HapticKind.PRESS, props.motionSettings.hapticsEnabled)
                        props.tile.appId?.let { props.onOpenApp(it, openBtnRect) }
                    }
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }

        // Centered Fixed-Size Widget Card inside Passepartout
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val hostView = remember(appWidgetId, hostFactory) {
                if (appWidgetId != null && hostFactory != null) hostFactory(appWidgetId) else null
            }

            if (hostView != null) {
                LaunchedEffect(hostView, props.widthDp, props.heightDp, fixedW, fixedH) {
                    val ranges = WidgetFrameEngine.computeWidgetSizeBundleRanges(
                        tileWidthDp = props.widthDp,
                        tileHeightDp = props.heightDp,
                        fixedWidthDp = fixedW,
                        fixedHeightDp = fixedH,
                    )
                    runCatching {
                        @Suppress("DEPRECATION")
                        hostView.updateAppWidgetSize(
                            Bundle.EMPTY,
                            ranges.minWidthDp,
                            ranges.minHeightDp,
                            ranges.maxWidthDp,
                            ranges.maxHeightDp,
                        )
                    }
                }
                AndroidView(
                    factory = { hostView },
                    modifier = Modifier
                        .size(width = fixedW.dp, height = fixedH.dp)
                        .graphicsLayer { scaleX = fitScale; scaleY = fitScale }
                        .clip(RoundedCornerShape(16.dp)),
                )
            } else {
                Column(
                    modifier = Modifier
                        .size(width = fixedW.dp, height = fixedH.dp)
                        .graphicsLayer {
                            scaleX = fitScale
                            scaleY = fitScale
                        }
                        .shadow(10.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(innerCardBg)
                        .clickable(enabled = !props.isEditMode) {
                            CanopyHaptics.trigger(HapticKind.PRESS, props.motionSettings.hapticsEnabled)
                            val targetApp = props.tile.appId
                            if (targetApp != null) {
                                props.onOpenApp(targetApp, openBtnRect)
                            }
                        }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = props.tile.title.ifBlank { "Android-Widget" },
                        color = innerText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Über „Bearbeiten → Kachel + → Widgets“ echtes System-Widget verknüpfen.",
                        color = innerSub,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
