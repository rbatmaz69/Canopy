package app.canopy.launcher.ui.tiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.AlarmState
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.ui.theme.SurfaceEmphasis
import app.canopy.launcher.ui.theme.ThemeTokens

/**
 * Adaptive tile container resolving size tier (XS, S, M, L, TICKER, LIST)
 * with 8dp hysteresis, theme surface tokens, top specular edge highlight, and full-bleed rules.
 */
@Composable
fun TileRenderer(
    tile: TileData,
    leafId: String,
    widthDp: Float,
    heightDp: Float,
    theme: ThemeTokens,
    motionSettings: MotionSettings,
    isEditMode: Boolean,
    alarmState: AlarmState,
    onToggleAlarm: () -> Unit,
    onOpenApp: (String, Rect?) -> Unit,
    onOpenGroup: (TileData, String, Rect?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFakeTileApp: (TileData, Rect?) -> Unit,
    onUpdateTile: (String, (TileData) -> TileData) -> Unit,
    calendarEvents: List<CalendarEvent>,
    mediaState: MediaPlayerState,
    systemState: SystemControlState,
    allAppsCatalog: List<AppDefinition>,
    modifier: Modifier = Modifier,
    bodyRenderer: @Composable (TileContentProps) -> Unit = { DefaultTileBodyRouter(it) },
) {
    var prevTier by remember(leafId) { mutableStateOf<TileSizeTier?>(null) }
    val tier = TilingEngine.computeSizeTier(widthDp, heightDp, prevTier)
    SideEffect { prevTier = tier }

    val isHero = TileContainerMetrics.isHeroTile(tile)
    val surface = theme.getTileSurface(
        accentHue = tile.accentHue,
        emphasis = if (isHero) SurfaceEmphasis.HERO else SurfaceEmphasis.NORMAL,
    )
    val paddingDp = TileContainerMetrics.resolvePaddingDp(tile, tier)
    val shape = RoundedCornerShape(theme.tileRadiusDp.dp)

    val contentProps = TileContentProps(
        tile = tile,
        leafId = leafId,
        widthDp = widthDp,
        heightDp = heightDp,
        tier = tier,
        theme = theme,
        surface = surface,
        motionSettings = motionSettings,
        isEditMode = isEditMode,
        alarmState = alarmState,
        onToggleAlarm = onToggleAlarm,
        onOpenApp = onOpenApp,
        onOpenGroup = onOpenGroup,
        onOpenSearch = onOpenSearch,
        onOpenFakeTileApp = onOpenFakeTileApp,
        onUpdateTile = onUpdateTile,
        calendarEvents = calendarEvents,
        mediaState = mediaState,
        systemState = systemState,
        allAppsCatalog = allAppsCatalog,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(
                elevation = if (theme.isGlass) 6.dp else 4.dp,
                shape = shape,
                ambientColor = surface.shadowColor,
                spotColor = surface.shadowColor,
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(surface.backgroundStart, surface.backgroundEnd)
                )
            )
            .border(surface.borderWidthDp.dp, surface.borderColor, shape),
    ) {
        // Subtle top-edge specular highlight for physical tile tactility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = surface.specularHighlightAlpha))
                .align(Alignment.TopCenter)
        )

        AnimatedContent(
            targetState = tier,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tile-tier-morph-$leafId",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingDp.dp),
        ) { activeTier ->
            bodyRenderer(contentProps.copy(tier = activeTier))
        }
    }
}

@Composable
fun DefaultTileBodyRouter(props: TileContentProps) {
    when (props.tile.type) {
        TileType.GREETING -> GreetingTile(props)
        TileType.TEMP_CURVE -> TempCurveTile(props)
        TileType.CALENDAR_TIMELINE -> CalendarTimelineTile(props)
        TileType.REMINDERS -> RemindersTile(props)
        TileType.BATTERY_STEPS -> BatteryStepsTile(props)
        TileType.CLOCK -> ClockTile(props)
        TileType.WEATHER -> WeatherTile(props)
        TileType.CALENDAR -> CalendarTile(props)
        TileType.MEDIA -> MediaTile(props)
        TileType.NOTE -> NoteTile(props)
        TileType.PHOTOS -> PhotosTile(props)
        TileType.BATTERY -> BatteryTile(props)
        TileType.STEPS -> StepsTile(props)
        TileType.QUICK_TOGGLES -> QuickTogglesTile(props)
        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "${props.tile.title} (${props.tier})",
                color = props.surface.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
