package app.canopy.launcher.ui.tiles

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.layout.ReminderItem
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.layout.TogglePreset
import app.canopy.launcher.system.LocalHardwareTelemetry
import app.canopy.launcher.system.LocalWeatherSnapshot
import app.canopy.launcher.ui.theme.CanopyThemes
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure micro-interaction state transformers used by system tiles and verified by unit tests.
 */
object EigenTileActions {
    fun toggleReminder(items: List<ReminderItem>, id: String): List<ReminderItem> =
        items.map { if (it.id == id) it.copy(done = !it.done) else it }

    fun addQuickReminder(items: List<ReminderItem>): List<ReminderItem> {
        val candidate = SystemTileDefaults.QUICK_REMINDER_IDEAS
            .firstOrNull { idea -> items.none { it.text == idea } }
            ?: "Notiz #${items.size + 1}"
        val next = ReminderItem(
            id = "r-${items.size + 1}-${candidate.hashCode().toUInt()}",
            text = candidate,
            done = false,
        )
        return listOf(next) + items
    }

    fun checkAllReminders(items: List<ReminderItem>): List<ReminderItem> =
        items.map { it.copy(done = true) }

    fun nextPhotoSceneIndex(currentIdx: Int, count: Int = SystemTileDefaults.PHOTO_SCENES.size): Int =
        if (count <= 0) 0 else (currentIdx + 1) % count

    fun addQuickNoteLine(lines: List<String>): List<String> {
        val idea = SystemTileDefaults.QUICK_REMINDER_IDEAS
            .firstOrNull { candidate -> lines.none { it == candidate } }
            ?: "Notiz #${lines.size + 1}"
        return listOf(idea) + lines
    }

    fun applyCustomPhotoUri(tile: TileData, uriString: String?): TileData =
        tile.copy(customPhotoUri = uriString?.trim()?.takeIf { it.isNotEmpty() })
}

@Composable
fun GreetingTile(props: TileContentProps) {
    val hw = LocalHardwareTelemetry.current
    val weather = LocalWeatherSnapshot.current
    val fluidGreetingSize = max(23f, min(if (props.theme.isBold) 38f else 34f, props.widthDp * 0.135f))
    when (props.tier) {
        TileSizeTier.XS -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(24.dp))
        }
        TileSizeTier.TICKER -> Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
                Text(hw.greetingSingleLine, color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("${hw.shortDateLabel} · ${weather.currentTempC}° ${weather.shortCondition}", color = props.surface.textSecondary, fontSize = 13.sp)
        }
        TileSizeTier.S, TileSizeTier.LIST -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
            Column {
                Text(hw.greetingSingleLine, color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
                Text("${hw.shortDateLabel} · ${weather.currentTempC}°", color = props.surface.textSecondary, fontSize = 12.sp)
            }
        }
        else -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = hw.upperDateLabel,
                    color = props.surface.accentColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(props.surface.accentBg)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Rounded.Cloud, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${weather.currentTempC}°",
                        color = props.surface.textPrimary,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    )
                }
            }
            Column {
                Text(
                    text = hw.greetingTwoLine,
                    color = props.surface.textPrimary,
                    style = TextStyle(
                        fontSize = fluidGreetingSize.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (fluidGreetingSize * 1.06f).sp,
                        letterSpacing = (-0.6).sp,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${weather.conditionLabel}. ${props.calendarEvents.size} Termine heute.",
                    color = props.surface.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun TempCurveTile(props: TileContentProps) {
    val weather = LocalWeatherSnapshot.current
    when (props.tier) {
        TileSizeTier.XS -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
            Text("${weather.currentTempC}°", color = props.surface.textPrimary, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        TileSizeTier.TICKER -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(18.dp))
                Text("Tagesverlauf", color = props.surface.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("${weather.lowTempC}° – ${weather.highTempC}°", color = props.surface.textPrimary, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        else -> {
            val series = weather.hourlyPoints.ifEmpty { SystemTileDefaults.TEMP_DAY_SERIES }
            val minCurveTemp = series.minOf { it.tempC }
            val maxCurveSpan = max(4f, series.maxOf { it.tempC } - minCurveTemp)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Verlauf", color = props.surface.textSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (props.widthDp < 135f) "↑${weather.highTempC}°" else "max ${weather.highTempC}°",
                        color = props.surface.accentColor,
                        style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(max(50f, min(96f, props.heightDp * 0.42f)).dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val pts = series.mapIndexed { idx, pt ->
                        val x = (idx.toFloat() / max(1, series.size - 1)) * w
                        val normY = (pt.tempC - minCurveTemp) / maxCurveSpan
                        val y = h * (0.85f - normY * 0.70f)
                        Offset(x, y)
                    }
                    val linePath = Path().apply {
                        pts.forEachIndexed { idx, offset ->
                            if (idx == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                        }
                    }
                    val areaPath = Path().apply {
                        addPath(linePath)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(props.surface.accentColor.copy(alpha = 0.35f), Color.Transparent)
                        ),
                    )
                    drawPath(
                        linePath,
                        color = props.surface.accentColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    val activePt = pts[1]
                    drawCircle(props.surface.accentColor.copy(alpha = 0.25f), radius = 14f, center = activePt)
                    drawCircle(props.surface.accentColor, radius = 8f, center = activePt)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = "${weather.currentTempC}°",
                            color = props.surface.textPrimary,
                            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        )
                        Text("Tief ${weather.lowTempC}°", color = props.surface.textSecondary, fontSize = 11.sp)
                    }
                    if (props.tier != TileSizeTier.S) {
                        Text(weather.peakHourLabel, color = props.surface.textSecondary, fontSize = 11.sp)
                    }
                }

                if (props.heightDp >= 175f) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        listOf(
                            series.getOrNull(1),
                            series.getOrNull(3),
                            series.getOrNull(5),
                        ).filterNotNull().forEach { pt ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(pt.hour, color = props.surface.textSecondary, fontSize = 9.5.sp)
                                Text("${pt.tempC.roundToInt()}°", color = props.surface.textPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClockTile(props: TileContentProps) {
    val hw = LocalHardwareTelemetry.current
    val isAlarmOff = !props.alarmState.enabled || props.tile.subtitle == "Wecker aus"
    val alarmLabel = if (isAlarmOff) "Wecker aus" else "Wecker ${props.alarmState.timeLabel}"
    val fluidClockSize = max(28f, min(if (props.theme.isBold) 56f else 50f, min(props.widthDp * 0.24f, props.heightDp * 0.28f)))

    when (props.tier) {
        TileSizeTier.XS -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(hw.timeLabel, color = props.surface.textPrimary, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        TileSizeTier.TICKER -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(hw.timeLabel, color = props.surface.textPrimary, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
            Text("${hw.shortDateLabel} · $alarmLabel", color = props.surface.textSecondary, fontSize = 12.sp)
        }
        TileSizeTier.LIST -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = hw.stackedTimeLabel,
                color = props.surface.textPrimary,
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, fontFeatureSettings = "tnum"),
            )
            Text(hw.shortDateLabel, color = props.surface.textSecondary, fontSize = 11.sp)
        }
        else -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(hw.shortDateLabel, color = props.surface.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(LocalWeatherSnapshot.current.cityName, color = props.surface.accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Text(
                text = hw.timeLabel,
                color = props.surface.textPrimary,
                style = TextStyle(
                    fontSize = fluidClockSize.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp,
                    fontFeatureSettings = "tnum",
                ),
            )

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isAlarmOff) Color(0x287D7D91) else props.surface.accentBg)
                    .clickable(enabled = !props.isEditMode) {
                        props.onToggleAlarm()
                        props.onUpdateTile(props.leafId) { prev ->
                            prev.copy(subtitle = if (isAlarmOff) "Wecker ${props.alarmState.timeLabel}" else "Wecker aus")
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = "Wecker umschalten",
                    tint = if (isAlarmOff) props.surface.textSecondary else props.surface.accentColor,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = alarmLabel,
                    color = if (isAlarmOff) props.surface.textSecondary else props.surface.textPrimary,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

@Composable
fun WeatherTile(props: TileContentProps) {
    val weather = LocalWeatherSnapshot.current
    val fluidTempSize = max(28f, min(if (props.theme.isBold) 46f else 42f, min(props.widthDp * 0.25f, props.heightDp * 0.25f)))
    when (props.tier) {
        TileSizeTier.XS -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.CloudQueue, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
            Text("${weather.currentTempC}°", color = props.surface.textPrimary, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        TileSizeTier.TICKER -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CloudQueue, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(18.dp))
                Text("${weather.currentTempC}°", color = props.surface.textPrimary, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
            }
            Text(weather.shortCondition, color = props.surface.textSecondary, fontSize = 12.sp)
        }
        else -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "${weather.currentTempC}°",
                    color = props.surface.textPrimary,
                    style = TextStyle(fontSize = fluidTempSize.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                )
                Icon(Icons.Rounded.CloudQueue, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(22.dp))
            }

            if (props.tier != TileSizeTier.LIST) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = weather.shortCondition,
                        color = props.surface.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (props.tier != TileSizeTier.S) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(props.surface.accentBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("H ${weather.highTempC}° · T ${weather.lowTempC}°", color = props.surface.textPrimary, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
                            Text("${weather.rainChancePercent}%", color = props.surface.accentColor, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
                        }
                    }
                }
            }

            if (props.tier != TileSizeTier.S && props.tier != TileSizeTier.LIST) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    weather.hourlyForecasts.forEach { hr ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(hr.hour, color = props.surface.textSecondary, fontSize = 10.sp)
                            Icon(
                                imageVector = if (hr.icon == "CloudSun") Icons.Rounded.WbSunny else Icons.Rounded.CloudQueue,
                                contentDescription = null,
                                tint = props.surface.accentColor,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(hr.temp, color = props.surface.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarTimelineTile(props: TileContentProps) {
    val hw = LocalHardwareTelemetry.current
    val events = props.calendarEvents
    val firstEv = events.firstOrNull()
    when (props.tier) {
        TileSizeTier.XS -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(22.dp))
            Text("${events.size}", color = props.surface.textPrimary, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        TileSizeTier.TICKER -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(17.dp))
                Text(
                    text = firstEv?.let { "${it.title}, ${it.time}" } ?: "Keine Termine",
                    color = props.surface.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (events.size > 1) "+${events.size - 1} heute" else "Heute",
                color = props.surface.textSecondary,
                fontSize = 12.sp,
            )
        }
        TileSizeTier.S, TileSizeTier.LIST -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(15.dp))
                Text("Heute", color = props.surface.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                events.take(if (props.tier == TileSizeTier.LIST) 3 else 2).forEach { ev ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(3.dp).height(26.dp).background(props.surface.accentColor))
                        Spacer(Modifier.width(7.dp))
                        Column {
                            Text(ev.title, color = props.surface.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ev.time, color = props.surface.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        else -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(15.dp))
                    Text("Tagesplan", color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "● Jetzt ${hw.timeLabel}",
                    color = Color(0xFFEF4444),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                )
            }

            // Red live current time marker line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = hw.timeLabel,
                    color = Color(0xFFEF4444),
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    modifier = Modifier.width(38.dp),
                )
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                Box(Modifier.weight(1f).height(1.5.dp).background(Color(0xB3EF4444)))
            }

            events.take(3).forEach { ev ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ev.time,
                        color = props.surface.textSecondary,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        modifier = Modifier.width(42.dp),
                    )
                    Box(Modifier.width(3.dp).height(24.dp).background(CanopyThemes.hslColor(ev.hue, 80f, 60f)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ev.title,
                        color = props.surface.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(ev.location, color = props.surface.textSecondary, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun CalendarTile(props: TileContentProps) {
    val hw = LocalHardwareTelemetry.current
    val events = props.calendarEvents
    val firstEv = events.firstOrNull()
    when (props.tier) {
        TileSizeTier.XS -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
            Text(hw.dayOfMonthLabel, color = props.surface.textPrimary, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        TileSizeTier.TICKER -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(firstEv?.title ?: "Keine Termine", color = props.surface.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(firstEv?.time ?: "—", color = props.surface.accentColor, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        else -> {
            val eventCount = if (props.tier == TileSizeTier.L || props.heightDp >= 175f) 3 else 2
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Kalender", color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${events.size} heute",
                        color = props.surface.accentColor,
                        style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(props.surface.accentBg)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    events.take(eventCount).forEach { ev ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(28.dp).background(CanopyThemes.hslColor(ev.hue, 80f, 62f)))
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text(ev.title, color = props.surface.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${ev.time} · ${ev.location}", color = props.surface.textSecondary, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersTile(props: TileContentProps) {
    val items = props.tile.reminders ?: SystemTileDefaults.DEFAULT_REMINDERS
    val openCount = items.count { !it.done }

    when (props.tier) {
        TileSizeTier.XS -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
            Text("$openCount", color = props.surface.textPrimary, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        TileSizeTier.TICKER -> {
            val next = items.firstOrNull { !it.done } ?: items.firstOrNull()
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(next?.text ?: "Erledigt", color = props.surface.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("$openCount offen", color = props.surface.accentColor, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
            }
        }
        else -> Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Erinnerungen",
                    color = props.surface.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "$openCount offen",
                        color = props.surface.accentColor,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(props.surface.accentBg)
                            .clickable(enabled = !props.isEditMode) {
                                props.onUpdateTile(props.leafId) { prev ->
                                    prev.copy(reminders = EigenTileActions.addQuickReminder(prev.reminders ?: items))
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Erinnerung hinzufügen", tint = props.surface.accentColor, modifier = Modifier.size(13.dp))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.take(if (props.tier == TileSizeTier.S) 2 else 3).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !props.isEditMode) {
                                props.onUpdateTile(props.leafId) { prev ->
                                    prev.copy(reminders = EigenTileActions.toggleReminder(prev.reminders ?: items, item.id))
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (item.done) props.surface.accentColor else Color.Transparent)
                                .border(1.5.dp, if (item.done) props.surface.accentColor else props.surface.textSecondary, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item.done) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                        }
                        Text(
                            text = item.text,
                            color = if (item.done) props.surface.textSecondary.copy(alpha = 0.65f) else props.surface.textPrimary,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryStepsTile(props: TileContentProps) {
    val hw = LocalHardwareTelemetry.current
    when (props.tier) {
        TileSizeTier.XS -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
            Text("${hw.batteryPercent}%", color = props.surface.textPrimary, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        TileSizeTier.TICKER -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Akku ${hw.batteryPercent} %", color = props.surface.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${hw.steps.formattedSteps} Schritte", color = props.surface.textSecondary, fontSize = 13.sp)
        }
        else -> Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Status", color = props.surface.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProgressRingItem(hw.batteryPercent / 100f, Color(0xFF22C55E), props.surface.accentBg, props.surface.textPrimary, props.surface.textSecondary, "Akku", "${hw.batteryPercent}%")
                if (props.tier != TileSizeTier.LIST) {
                    ProgressRingItem(hw.steps.progressRatio, props.surface.accentColor, props.surface.accentBg, props.surface.textPrimary, props.surface.textSecondary, "Schritte", hw.steps.compactLabel)
                }
            }
            if (props.heightDp >= 150f) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(if (hw.isCharging) "Lädt (${hw.estimatedHoursLeft} Std.)" else "ca. ${hw.estimatedHoursLeft} Std.", color = props.surface.textSecondary, fontSize = 10.5.sp)
                    Text("${hw.steps.distanceKmLabel} km heute", color = props.surface.textSecondary, fontSize = 10.5.sp)
                }
            }
        }
    }
}

@Composable
private fun ProgressRingItem(
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    valueColor: Color,
    labelColor: Color,
    label: String,
    valueText: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 11f, cap = StrokeCap.Round)
                drawArc(trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
                drawArc(ringColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = stroke)
            }
            Text(valueText, color = valueColor, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        Text(label, color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BatteryTile(props: TileContentProps) {
    val hw = LocalHardwareTelemetry.current
    if (props.tier == TileSizeTier.XS) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
            Text("${hw.batteryPercent}%", color = props.surface.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Akku", color = props.surface.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
            }
            Text("${hw.batteryPercent}%", color = props.surface.textPrimary, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
            Text(if (hw.isCharging) "Lädt gerade" else "Noch ca. ${hw.estimatedHoursLeft} Std.", color = props.surface.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun StepsTile(props: TileContentProps) {
    val hw = LocalHardwareTelemetry.current
    if (props.tier == TileSizeTier.XS) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
            Text(hw.steps.compactLabel, color = props.surface.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Schritte", color = props.surface.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(18.dp))
            }
            Text(hw.steps.formattedSteps, color = props.surface.textPrimary, style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(props.surface.accentBg)) {
                Box(Modifier.fillMaxWidth(hw.steps.progressRatio).fillMaxHeight().clip(CircleShape).background(props.surface.accentColor))
            }
        }
    }
}

@Composable
fun NoteTile(props: TileContentProps) {
    val lines = props.tile.noteLines ?: SystemTileDefaults.DEFAULT_NOTE_LINES
    if (props.tier == TileSizeTier.XS) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Description, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(21.dp))
        }
    } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(props.tile.title.ifBlank { "Notiz" }, color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(props.surface.accentBg)
                        .clickable(enabled = !props.isEditMode) {
                            props.onUpdateTile(props.leafId) { prev ->
                                prev.copy(noteLines = EigenTileActions.addQuickNoteLine(prev.noteLines ?: lines))
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Notiz hinzufügen", tint = props.surface.accentColor, modifier = Modifier.size(13.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                lines.take(if (props.tier == TileSizeTier.S) 2 else 3).forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(2.dp).height(14.dp).background(props.surface.accentColor))
                        Spacer(Modifier.width(8.dp))
                        Text(line, color = props.surface.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun PhotosTile(props: TileContentProps) {
    val context = LocalContext.current
    var sceneIdx by remember { mutableIntStateOf(0) }
    val scene = SystemTileDefaults.PHOTO_SCENES[sceneIdx]
    val customPhotoUri = props.tile.customPhotoUri

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            props.onUpdateTile(props.leafId) { prev ->
                EigenTileActions.applyCustomPhotoUri(prev, uri.toString())
            }
        }
    }

    val customBitmap = remember(customPhotoUri) {
        if (customPhotoUri.isNullOrBlank()) null
        else runCatching {
            context.contentResolver.openInputStream(Uri.parse(customPhotoUri))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (props.tier == TileSizeTier.XS) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(21.dp))
        }
        return
    }

    val kenBurns = rememberInfiniteTransition(label = "ken-burns")
    val kbScale by kenBurns.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(14000), RepeatMode.Reverse),
        label = "kb-scale",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = "Eigenes Foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kbScale
                        scaleY = kbScale
                    },
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kbScale
                        scaleY = kbScale
                    }
            ) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(scene.c1Hex), Color(scene.c2Hex), Color(scene.c3Hex)),
                        start = Offset.Zero,
                        end = Offset(w, h),
                    )
                )
                drawCircle(Color(scene.sunHex).copy(alpha = 0.85f), radius = min(w, h) * 0.14f, center = Offset(w * 0.72f, h * 0.32f))
                val hill1 = Path().apply {
                    moveTo(0f, h * 0.70f)
                    quadraticTo(w * 0.25f, h * 0.52f, w * 0.52f, h * 0.65f)
                    quadraticTo(w * 0.78f, h * 0.75f, w, h * 0.58f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(hill1, color = Color(0xB81E1B4B))
                val hill2 = Path().apply {
                    moveTo(0f, h * 0.82f)
                    quadraticTo(w * 0.30f, h * 0.70f, w * 0.63f, h * 0.80f)
                    quadraticTo(w * 0.82f, h * 0.86f, w, h * 0.75f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(hill2, color = Color(0xE00F172A))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xAD000000)),
                        startY = 120f,
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x850F172A))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable(enabled = !props.isEditMode) {
                            runCatching { photoPickerLauncher.launch(arrayOf("image/*")) }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = "Eigenes Foto wählen", tint = Color.White, modifier = Modifier.size(11.dp))
                    Text("Foto", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x850F172A))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable(enabled = !props.isEditMode) {
                            if (customPhotoUri != null) {
                                props.onUpdateTile(props.leafId) { prev ->
                                    EigenTileActions.applyCustomPhotoUri(prev, null)
                                }
                            } else {
                                sceneIdx = EigenTileActions.nextPhotoSceneIndex(sceneIdx)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (customBitmap != null) "Galerie" else "${sceneIdx + 1}/${SystemTileDefaults.PHOTO_SCENES.size}",
                        color = Color.White,
                        style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    )
                    Icon(Icons.Rounded.Refresh, contentDescription = "Nächstes Foto", tint = Color.White, modifier = Modifier.size(11.dp))
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(if (customBitmap != null) "Eigenes Galerie-Foto" else scene.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (props.tier != TileSizeTier.S) {
                    Text(if (customBitmap != null) "Tippen auf Szene zum Wechseln" else scene.meta, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                }
            }
        }
    }
}

private data class QuickActionItem(
    val key: String,
    val label: String,
    val shortLabel: String,
    val subOn: String,
    val subOff: String,
    val active: Boolean,
    val dotColor: Color? = null,
    val icon: ImageVector,
    val onPress: () -> Unit,
)

@Composable
fun QuickTogglesTile(props: TileContentProps) {
    var eqIndex by remember { mutableIntStateOf(0) }
    var sleepTimerOn by remember { mutableStateOf(false) }
    val eqModes = remember { listOf("Warm", "Bass", "Klar") }
    val preset = props.tile.togglePreset ?: TogglePreset.SYSTEM

    val items: List<QuickActionItem> = when (preset) {
        TogglePreset.TODAY -> listOf(
            QuickActionItem("new-event", "Termin", "+ Termin", "10:00", "Neu", false, null, Icons.Rounded.Event) {
                props.onOpenApp("app-kalender", null)
            },
            QuickActionItem("new-note", "Notiz", "+ Notiz", "Neu", "Neu", false, null, Icons.AutoMirrored.Rounded.NoteAdd) {
                props.onOpenApp("app-notizen", null)
            },
            QuickActionItem("alarm", "Wecker", "Wecker", props.alarmState.timeLabel, "Aus", props.alarmState.enabled, null, Icons.Rounded.Alarm) {
                props.onToggleAlarm()
            },
            QuickActionItem("today-focus", "Fokus", "Fokus", "25m an", "25m", props.systemState.dnd, null, Icons.Rounded.DarkMode) {
                props.systemState.onToggle(SystemToggleKey.DND)
            },
        )
        TogglePreset.MEDIA -> listOf(
            QuickActionItem("headphones", "Kopfhörer", "Audio", "Verbunden", "Lautspr.", props.systemState.bluetooth, null, Icons.Rounded.Headphones) {
                props.systemState.onToggle(SystemToggleKey.BLUETOOTH)
            },
            QuickActionItem("eq", "Klang", eqModes[eqIndex], eqModes[eqIndex], eqModes[eqIndex], true, null, Icons.Rounded.Tune) {
                eqIndex = (eqIndex + 1) % eqModes.size
            },
            QuickActionItem("sleep-timer", "Timer", if (sleepTimerOn) "15m an" else "Timer", "15 Min.", "Aus", sleepTimerOn, Color(0xFFF59E0B), Icons.Rounded.Timer) {
                sleepTimerOn = !sleepTimerOn
            },
            QuickActionItem("podcast-open", "Podcast", "Podcast", "Folge 408", "Öffnen", false, null, Icons.Rounded.Radio) {
                props.onOpenApp("app-podcast", null)
            },
        )
        TogglePreset.TOOLS -> listOf(
            QuickActionItem("quick-search", "Suche", "Suche", "Alle Apps", "Alle Apps", true, null, Icons.Rounded.Search) {
                props.onOpenSearch()
            },
            QuickActionItem("quick-qr", "QR-Scan", "QR-Scan", "Bereit", "Bereit", false, null, Icons.Rounded.QrCodeScanner) {
                props.onOpenApp("app-qr", null)
            },
            QuickActionItem("quick-calc", "Rechner", "Rechner", "Schnell", "Schnell", false, null, Icons.Rounded.Calculate) {
                props.onOpenApp("app-rechner", null)
            },
            QuickActionItem("quick-cam", "Kamera", "Kamera", "50 MP", "50 MP", false, null, Icons.Rounded.CameraAlt) {
                props.onOpenApp("app-kamera", null)
            },
        )
        TogglePreset.SYSTEM -> listOf(
            QuickActionItem("wifi", "WLAN", "WLAN", "Studio_5G", "Aus", props.systemState.wifi, null, if (props.systemState.wifi) Icons.Rounded.Wifi else Icons.Rounded.WifiOff) {
                props.systemState.onToggle(SystemToggleKey.WIFI)
            },
            QuickActionItem("bluetooth", "Bluetooth", "BT", "Kopfhörer", "Aus", props.systemState.bluetooth, null, Icons.Rounded.Bluetooth) {
                props.systemState.onToggle(SystemToggleKey.BLUETOOTH)
            },
            QuickActionItem("flashlight", "Licht", "Licht", "Stufe 3", "Bereit", props.systemState.flashlight, Color(0xFFF59E0B), Icons.Rounded.FlashlightOn) {
                props.systemState.onToggle(SystemToggleKey.FLASHLIGHT)
            },
            QuickActionItem("dnd", "Fokus", "Fokus", "Aktiv", "Aus", props.systemState.dnd, null, Icons.Rounded.DarkMode) {
                props.systemState.onToggle(SystemToggleKey.DND)
            },
            QuickActionItem("hotspot", "Hotspot", "Hotspot", "5 GHz", "Aus", props.systemState.hotspot, null, Icons.Rounded.Radio) {
                props.systemState.onToggle(SystemToggleKey.HOTSPOT)
            },
            QuickActionItem("autoRotate", "Drehen", "Drehen", "Auto", "Gesperrt", props.systemState.autoRotate, null, Icons.Rounded.ScreenRotation) {
                props.systemState.onToggle(SystemToggleKey.AUTO_ROTATE)
            },
        )
    }

    val activeText = when {
        props.theme.isGlass -> Color(0xFF0F172A)
        props.theme.mode == app.canopy.launcher.data.ColorMode.LIGHT && !props.theme.isBold -> Color.White
        else -> Color(0xFF0F172A)
    }
    val activeBg = if (props.theme.isGlass) Color(0xEBFFFFFF) else props.surface.accentColor
    val offBg = when {
        props.theme.isGlass -> Color(0x24FFFFFF)
        props.theme.mode == app.canopy.launcher.data.ColorMode.DARK || props.theme.isBold -> Color(0x14FFFFFF)
        else -> Color(0x0F0F172A)
    }

    if (props.tier == TileSizeTier.XS && !(props.widthDp >= 200f && props.heightDp >= 54f)) {
        val first = items.first()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (first.active) activeBg else offBg)
                    .clickable(enabled = !props.isEditMode) { first.onPress() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(first.icon, contentDescription = first.label, tint = if (first.active) activeText else props.surface.textPrimary, modifier = Modifier.size(18.dp))
            }
        }
        return
    }

    if (props.tier == TileSizeTier.TICKER || (props.widthDp >= 200f && props.heightDp < 104f)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.take(4).forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (item.active) activeBg else offBg)
                        .let { m ->
                            if (item.active) m else m.border(1.dp, props.surface.accentBg, RoundedCornerShape(15.dp))
                        }
                        .clickable(enabled = !props.isEditMode) { item.onPress() }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(item.icon, contentDescription = item.label, tint = if (item.active) activeText else props.surface.textPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = item.shortLabel,
                            color = if (item.active) activeText else props.surface.textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (item.active) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 5.dp, end = 6.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(item.dotColor ?: Color(0xFF10B981))
                        )
                    }
                }
            }
        }
        return
    }

    if (props.tier == TileSizeTier.LIST) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.take(4).forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (item.active) activeBg else offBg)
                        .clickable(enabled = !props.isEditMode) { item.onPress() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = if (item.active) activeText else props.surface.textPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
        return
    }

    val visibleCount = if (props.tier == TileSizeTier.L) items.size else 4
    val showBrightness = (props.tier == TileSizeTier.M || props.tier == TileSizeTier.L) && preset == TogglePreset.SYSTEM
    val cols = if (props.tier == TileSizeTier.L && props.widthDp > 240f && visibleCount > 4) 3 else 2

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(props.tile.title.ifBlank { "Schnellschalter" }, color = props.surface.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${items.take(visibleCount).count { it.active }} aktiv", color = props.surface.textSecondary, fontSize = 11.sp)
        }

        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items.take(visibleCount).chunked(cols).forEach { rowItems ->
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    rowItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (item.active) activeBg else offBg)
                                .clickable(enabled = !props.isEditMode) { item.onPress() }
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(item.icon, contentDescription = item.label, tint = if (item.active) activeText else props.surface.textPrimary, modifier = Modifier.size(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.label, color = if (item.active) activeText else props.surface.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (props.tier != TileSizeTier.S) {
                                    Text(
                                        text = if (item.active) item.subOn else item.subOff,
                                        color = (if (item.active) activeText else props.surface.textPrimary).copy(alpha = 0.78f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showBrightness) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(offBg)
                    .padding(horizontal = 9.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(14.dp))
                Slider(
                    value = props.systemState.brightness,
                    onValueChange = { if (!props.isEditMode) props.systemState.onChangeBrightness(it) },
                    valueRange = 0.25f..1f,
                    modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = props.surface.accentColor,
                        activeTrackColor = props.surface.accentColor,
                    ),
                )
                Text(
                    text = "${(props.systemState.brightness * 100).roundToInt()}%",
                    color = props.surface.textPrimary,
                    style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}
