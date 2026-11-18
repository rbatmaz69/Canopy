package app.canopy.launcher.ui.tiles

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
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
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.layout.WidgetKind
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.ui.components.AppSquircleIcon
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Provides an optional Android AppWidgetHost creator for native widget tiles.
 */
val LocalAppWidgetHostFactory = staticCompositionLocalOf<((Int) -> AppWidgetHostView?)?> { null }

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
}

@Composable
fun SingleAppTile(props: TileContentProps) {
    val appsById = props.allAppsCatalog.associateBy { it.id }.ifEmpty { MockCatalogData.APPS_BY_ID }
    val app = appsById[props.tile.appId.orEmpty()] ?: AppDefinition(
        id = props.tile.appId ?: "app-fallback",
        name = props.tile.title.ifBlank { "App" },
        iconKey = "Compass",
        color = Color(0xFF3B82F6),
        hue = props.tile.accentHue ?: 217f,
        category = AppCategory.WERKZEUGE,
        usageScore = 50,
        subtitle = "Bereit",
    )
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
                Text(app.subtitle, color = props.surface.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                if (props.tier != TileSizeTier.S) {
                    Spacer(Modifier.height(2.dp))
                    Text(app.subtitle, color = props.surface.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

    var bonusSteps by remember { mutableIntStateOf(0) }
    val podcastDurationSec = 42 * 60
    val podcastElapsedSec = (props.mediaState.progress * podcastDurationSec).roundToInt()
    val podMm = (podcastElapsedSec / 60).toString().padStart(2, '0')
    val podSs = (podcastElapsedSec % 60).toString().padStart(2, '0')
    val activeBarCount = max(1, min(12, (props.mediaState.progress * 12f).roundToInt()))

    val totalSteps = 7420 + bonusSteps
    val fitnessPct = min(100, ((totalSteps / 10000f) * 100f).roundToInt())
    val fitnessKcal = 420 + (bonusSteps * 0.045f).roundToInt()
    val fitnessKm = String.format(Locale.GERMAN, "%.1f", 5.4f + bonusSteps * 0.00075f)

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
                        .background(if (isPodcast) Color(0xFFA855F7) else Color(0xFF10B981))
                )
                Text(
                    text = if (isPodcast) "Podcast · 14m" else "Ziel $fitnessPct %",
                    color = props.surface.textSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
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
                        props.onOpenApp(
                            props.tile.appId ?: if (isPodcast) "app-podcast" else "app-fitness",
                            openBtnRect,
                        )
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (isPodcast) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF6366F1)))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Headphones, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Lage der Nation", color = innerText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Folge 408 · 14m", color = innerSub, fontSize = 10.sp)
                            }
                        }

                        val bars = remember { listOf(42, 78, 60, 95, 50, 82, 68, 90, 44, 72, 85, 52) }
                        Row(
                            modifier = Modifier.fillMaxWidth().height(22.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            bars.forEachIndexed { idx, baseH ->
                                val animH = if (props.mediaState.isPlaying) {
                                    (baseH + (((props.mediaState.progress * 100).roundToInt() + idx) % 3 - 1) * 14).coerceIn(24, 98)
                                } else {
                                    baseH
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(animH / 100f)
                                        .clip(CircleShape)
                                        .background(if (idx < activeBarCount) Color(0xFFA855F7) else Color(0x47A855F7))
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$podMm:$podSs / 42:00",
                                color = innerSub,
                                style = TextStyle(fontSize = 11.sp, fontFeatureSettings = "tnum"),
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF9333EA))
                                    .clickable(enabled = !props.isEditMode) {
                                        CanopyHaptics.trigger(HapticKind.PRESS, props.motionSettings.hapticsEnabled)
                                        props.mediaState.onTogglePlay()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (props.mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Podcast abspielen",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                Text("Fitness", color = innerText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "$fitnessPct %",
                                color = Color(0xFF10B981),
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = String.format(Locale.GERMAN, "%,d", totalSteps),
                                color = innerText,
                                style = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                            )
                            Text(
                                text = "+250",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x3810B981))
                                    .clickable(enabled = !props.isEditMode) {
                                        CanopyHaptics.trigger(HapticKind.SNAP, props.motionSettings.hapticsEnabled)
                                        bonusSteps += 250
                                    }
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("$fitnessKcal kcal", color = innerSub, fontSize = 10.5.sp)
                            Text("$fitnessKm km", color = innerSub, fontSize = 10.5.sp)
                        }
                    }
                }
            }
        }
    }
}
