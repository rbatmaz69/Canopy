package app.canopy.launcher.ui.overlays

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.AlarmState
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.data.WallpaperPresetId
import app.canopy.launcher.layout.ReminderItem
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.system.InstalledAppsMapper
import app.canopy.launcher.system.LocalWeatherSnapshot
import app.canopy.launcher.system.NativeShortcutEntry
import app.canopy.launcher.system.OpenMeteoWeatherParser
import app.canopy.launcher.system.OpenMeteoGeocoder
import app.canopy.launcher.data.WeatherLocation
import app.canopy.launcher.ui.components.AppSquircleIcon
import app.canopy.launcher.ui.navigation.SharedElementExpandContainer
import app.canopy.launcher.ui.theme.CanopyThemes
import app.canopy.launcher.ui.theme.ThemeTokens
import app.canopy.launcher.ui.tiles.AppDefinition
import app.canopy.launcher.ui.tiles.CalendarEvent
import app.canopy.launcher.ui.tiles.MediaPlayerState
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

data class ContextMenuState(
    val leafId: String,
    val pageId: String,
    val tile: TileData,
    val rectDp: Rect,
)

enum class TileDetailKind {
    WEATHER,
    NOTES,
}

/** In-launcher editors for data Canopy itself owns (weather location, notes & reminders). */
data class TileDetailTarget(
    val kind: TileDetailKind,
    val tile: TileData? = null,
    val pageId: String? = null,
    val leafId: String? = null,
    val sourceRect: Rect? = null,
    val shortcutAction: String? = null,
)

/** Stable context-menu action labels shared by the menu and its dispatcher. */
object TileShortcutLabels {
    const val OPEN_ALARMS = "Wecker öffnen"
    const val SET_TIMER = "Timer stellen"
    const val CHOOSE_LOCATION = "Ort wählen"
    const val NEW_EVENT = "Neuer Termin"
    const val OPEN_CALENDAR = "Kalender öffnen"
    const val NEXT_TRACK = "Nächster Titel"
    const val OPEN_MEDIA_APP = "Medien-App öffnen"
    const val RENAME_GROUP = "Gruppe umbenennen"
    const val MANAGE_GROUP = "Apps verwalten"
    const val EDIT_ENTRIES = "Einträge bearbeiten"
    const val CHECK_ALL = "Alle abhaken"
    const val CHOOSE_PHOTO = "Foto öffnen"
    const val RECONFIGURE_WIDGET = "Widget einrichten"
}


object TargetViewEditorPersistence {
    fun normalizeAlarmTime(raw: String, fallback: String = "07:15"): String {
        val parts = raw.trim().split(":")
        if (parts.size != 2) return fallback
        val h = parts[0].trim().toIntOrNull() ?: return fallback
        val m = parts[1].trim().toIntOrNull() ?: return fallback
        if (h !in 0..23 || m !in 0..59) return fallback
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    fun parseAlarmHoursAndMinutes(time: String): Pair<Int, Int> {
        val normalized = normalizeAlarmTime(time)
        val parts = normalized.split(":")
        return (parts[0].toIntOrNull() ?: 7) to (parts[1].toIntOrNull() ?: 15)
    }

    fun applyReminderUpdateToTile(tile: TileData, items: List<ReminderItem>): TileData =
        tile.copy(
            reminders = items,
            noteLines = items.map { it.text },
        )
}

data class ContextMenuPlacement(
    val leftDp: Float,
    val topDp: Float,
)

object ContextMenuPositioner {
    const val MENU_WIDTH_DP = 232f
    const val MENU_HEIGHT_DP = 328f

    fun computePlacement(
        targetRectDp: Rect,
        containerWidthDp: Float,
        containerHeightDp: Float,
        menuWidthDp: Float = MENU_WIDTH_DP,
        menuHeightDp: Float = MENU_HEIGHT_DP,
    ): ContextMenuPlacement {
        val preferredLeft = min(
            containerWidthDp - menuWidthDp - 12f,
            max(12f, targetRectDp.left + targetRectDp.width * 0.5f - menuWidthDp * 0.5f),
        )
        val spaceBelow = containerHeightDp - targetRectDp.bottom
        val preferredTop = if (spaceBelow >= menuHeightDp + 16f) {
            targetRectDp.bottom + 8f
        } else {
            max(46f, targetRectDp.top - menuHeightDp - 8f)
        }
        return ContextMenuPlacement(
            leftDp = preferredLeft.coerceIn(12f, max(12f, containerWidthDp - menuWidthDp - 12f)),
            topDp = preferredTop.coerceIn(46f, max(46f, containerHeightDp - menuHeightDp - 12f)),
        )
    }

    fun shortcutsForTile(
        tile: TileData,
        nativeShortcuts: List<NativeShortcutEntry> = emptyList(),
    ): List<String> {
        val base = when (tile.type) {
            TileType.CLOCK -> listOf(TileShortcutLabels.OPEN_ALARMS, TileShortcutLabels.SET_TIMER)
            TileType.WEATHER, TileType.TEMP_CURVE, TileType.GREETING -> listOf(TileShortcutLabels.CHOOSE_LOCATION)
            TileType.CALENDAR, TileType.CALENDAR_TIMELINE -> listOf(TileShortcutLabels.NEW_EVENT, TileShortcutLabels.OPEN_CALENDAR)
            TileType.MEDIA -> listOf(TileShortcutLabels.NEXT_TRACK, TileShortcutLabels.OPEN_MEDIA_APP)
            TileType.APP_GROUP -> listOf(TileShortcutLabels.RENAME_GROUP, TileShortcutLabels.MANAGE_GROUP)
            TileType.REMINDERS, TileType.NOTE -> listOf(TileShortcutLabels.EDIT_ENTRIES, TileShortcutLabels.CHECK_ALL)
            TileType.PHOTOS -> listOf(TileShortcutLabels.CHOOSE_PHOTO)
            TileType.THIRD_PARTY_WIDGET -> if (tile.androidAppWidgetId != null) listOf(TileShortcutLabels.RECONFIGURE_WIDGET) else emptyList()
            else -> emptyList()
        }
        return InstalledAppsMapper.mergeContextShortcuts(base, nativeShortcuts)
    }
}

@Composable
fun ContextMenuOverlay(
    menu: ContextMenuState,
    containerWidthDp: Float,
    containerHeightDp: Float,
    theme: ThemeTokens,
    hapticsEnabled: Boolean,
    nativeShortcuts: List<NativeShortcutEntry> = emptyList(),
    onClose: () -> Unit,
    onEnterEditMode: () -> Unit,
    onQuickGrow: (pageId: String, leafId: String) -> Unit,
    onRemoveTile: (pageId: String, leafId: String) -> Unit,
    onShowInfo: (tile: TileData, rectDp: Rect) -> Unit,
    onShortcutAction: (label: String, tile: TileData, rectDp: Rect) -> Unit,
    onChangeTileAccentHue: (pageId: String, leafId: String, hue: Float?) -> Unit = { _, _, _ -> },
) {
    val placement = remember(menu, containerWidthDp, containerHeightDp) {
        ContextMenuPositioner.computePlacement(menu.rectDp, containerWidthDp, containerHeightDp)
    }
    val shortcuts = remember(menu.tile, nativeShortcuts) {
        ContextMenuPositioner.shortcutsForTile(menu.tile, nativeShortcuts)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x47000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() }
    ) {
        Column(
            modifier = Modifier
                .offset(x = placement.leftDp.dp, y = placement.topDp.dp)
                .width(ContextMenuPositioner.MENU_WIDTH_DP.dp)
                .shadow(20.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(theme.sheetBg)
                .border(1.dp, theme.sheetBorder, RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* consume */ }
                .padding(6.dp),
        ) {
            Text(
                text = menu.tile.title.uppercase(),
                color = theme.textMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
            )

            shortcuts.forEach { sc ->
                ContextMenuRow(Icons.Rounded.AutoAwesome, sc, theme.textPrimary) {
                    CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                    onShortcutAction(sc, menu.tile, menu.rectDp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(1.dp)
                    .background(theme.sheetBorder)
            )

            ContextMenuRow(Icons.Rounded.NorthEast, "Größer (+15 %)", theme.textPrimary) {
                CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                onQuickGrow(menu.pageId, menu.leafId)
            }
            ContextMenuRow(Icons.Rounded.AspectRatio, "Größe ändern", theme.textPrimary) {
                CanopyHaptics.trigger(HapticKind.EDIT_ENTER, hapticsEnabled)
                onEnterEditMode()
            }
            if (menu.tile.appId != null && (menu.tile.type == TileType.SINGLE_APP || menu.tile.type == TileType.THIRD_PARTY_WIDGET)) {
                ContextMenuRow(Icons.Rounded.Info, "App-Info", theme.textPrimary) {
                    CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                    onShowInfo(menu.tile, menu.rectDp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf<Float?>(null, 215f, 160f, 38f, 271f, 330f).forEach { swatchHue ->
                    val active = menu.tile.accentHue == swatchHue
                    val swatchColor = if (swatchHue == null) {
                        theme.pillActive
                    } else {
                        CanopyThemes.hslColor(swatchHue, 78f, 54f)
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active) theme.textPrimary else theme.sheetBorder,
                                shape = CircleShape,
                            )
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.TICK, hapticsEnabled)
                                onChangeTileAccentHue(menu.pageId, menu.leafId, swatchHue)
                            },
                    )
                }
            }

            ContextMenuRow(Icons.Rounded.Delete, "Von Seite entfernen", Color(0xFFEF4444)) {
                CanopyHaptics.trigger(HapticKind.REMOVE, hapticsEnabled)
                onRemoveTile(menu.pageId, menu.leafId)
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 42.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Text(label, color = color, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}


@Composable
fun TileDetailOverlay(
    target: TileDetailTarget,
    containerWidthPx: Float,
    containerHeightPx: Float,
    theme: ThemeTokens,
    motionSettings: MotionSettings,
    onUpdateTile: (pageId: String, leafId: String, updater: (TileData) -> TileData) -> Unit = { _, _, _ -> },
    weatherCityId: String,
    onSelectWeatherCity: (String) -> Unit = {},
    weatherLocation: WeatherLocation? = null,
    onSelectWeatherLocation: (WeatherLocation) -> Unit = {},
    onClose: () -> Unit,
) {
    val title = when (target.kind) {
        TileDetailKind.WEATHER -> "Wetter"
        TileDetailKind.NOTES -> target.tile?.title?.ifBlank { null } ?: "Notizen"
    }
    val hue = target.tile?.accentHue ?: if (target.kind == TileDetailKind.WEATHER) 205f else 38f

    val isLight = theme.mode == ColorMode.LIGHT && !theme.isGlass
    val bg = if (isLight) Color(0xFFF8FAFC) else Color(0xFF0F131C)
    val fg = if (isLight) Color(0xFF111827) else Color(0xFFF8FAFC)
    val muted = if (isLight) Color(0xFF4B5563) else Color(0xFF94A3B8)
    val cardBg = if (isLight) Color.White else Color(0x12FFFFFF)
    val accentColor = CanopyThemes.hslColor(hue, 78f, 52f)
    val accentBg = CanopyThemes.hslColor(hue, 78f, 52f, alpha = 0.16f)

    SharedElementExpandContainer(
        sourceRect = target.sourceRect,
        containerWidthPx = containerWidthPx,
        containerHeightPx = containerHeightPx,
        tileRadiusDp = theme.tileRadiusDp,
        motionSettings = motionSettings,
    ) {
        Column(modifier = Modifier.fillMaxSize().background(bg).imePadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                CanopyThemes.hslColor(hue, 70f, 45f),
                                CanopyThemes.hslColor((hue + 35f) % 360f, 65f, 28f),
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 18.dp, top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                            onClose()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (target.kind) {
                    TileDetailKind.WEATHER -> WeatherTargetView(
                        weatherCityId = weatherCityId,
                        onSelectWeatherCity = onSelectWeatherCity,
                        weatherLocation = weatherLocation,
                        onSelectWeatherLocation = onSelectWeatherLocation,
                        fg = fg,
                        muted = muted,
                        cardBg = cardBg,
                        accentColor = accentColor,
                        accentBg = accentBg,
                    )
                    TileDetailKind.NOTES -> NotesTargetView(
                        initialReminders = target.tile?.reminders
                            ?: target.tile?.noteLines?.mapIndexed { i, line -> ReminderItem("n-$i", line, false) }
                            ?: emptyList(),
                        checkAllInitially = target.shortcutAction == TileShortcutLabels.CHECK_ALL,
                        onPersistReminders = { updated ->
                            val pid = target.pageId
                            val lid = target.leafId
                            if (pid != null && lid != null) {
                                onUpdateTile(pid, lid) { tile ->
                                    TargetViewEditorPersistence.applyReminderUpdateToTile(tile, updated)
                                }
                            }
                        },
                        fg = fg,
                        muted = muted,
                        cardBg = cardBg,
                        accentColor = accentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesTargetView(
    initialReminders: List<ReminderItem>,
    checkAllInitially: Boolean,
    onPersistReminders: (List<ReminderItem>) -> Unit,
    fg: Color,
    muted: Color,
    cardBg: Color,
    accentColor: Color,
) {
    var items by remember(initialReminders, checkAllInitially) {
        val resolved = if (checkAllInitially) initialReminders.map { it.copy(done = true) } else initialReminders
        mutableStateOf(resolved)
    }
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(checkAllInitially) {
        if (checkAllInitially) {
            onPersistReminders(items)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Neuer Eintrag") }, modifier = Modifier.weight(1f), singleLine = true)
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accentColor).clickable {
                val clean = draft.trim()
                if (clean.isNotEmpty()) {
                    val updated = listOf(ReminderItem("n-${System.currentTimeMillis()}", clean, false)) + items
                    items = updated
                    onPersistReminders(updated)
                    draft = ""
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Hinzufügen", tint = Color.White)
        }
    }

    items.forEach { n ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .clickable {
                    val updated = items.map { if (it.id == n.id) it.copy(done = !it.done) else it }
                    items = updated
                    onPersistReminders(updated)
                }
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (n.done) accentColor else muted, modifier = Modifier.size(18.dp))
            Text(
                text = n.text,
                color = fg,
                style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, textDecoration = if (n.done) TextDecoration.LineThrough else TextDecoration.None),
                modifier = Modifier.alpha(if (n.done) 0.6f else 1f),
            )
        }
    }
}


@Composable
private fun WeatherTargetView(
    weatherCityId: String,
    onSelectWeatherCity: (String) -> Unit,
    weatherLocation: WeatherLocation?,
    onSelectWeatherLocation: (WeatherLocation) -> Unit,
    fg: Color,
    muted: Color,
    cardBg: Color,
    accentColor: Color,
    accentBg: Color,
) {
    val weather = LocalWeatherSnapshot.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherLocation>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(350L) // debounce typing
        searching = true
        results = OpenMeteoGeocoder.search(query)
        searching = false
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ORT FÜR DIE VORHERSAGE", color = muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(weatherLocation?.name?.let { "Aktuell: $it" } ?: "Ort suchen") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (searching) {
            Text("Suche …", color = muted, fontSize = 12.sp)
        } else if (query.trim().length >= 2 && results.isEmpty()) {
            Text("Kein Ort gefunden.", color = muted, fontSize = 12.sp)
        }
        results.forEach { place ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSelectWeatherLocation(place)
                        query = ""
                    }
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(place.name, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (place.region.isNotBlank()) Text(place.region, color = muted, fontSize = 12.sp)
            }
        }
        Text("SCHNELLAUSWAHL", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OpenMeteoWeatherParser.CITY_PRESETS.forEach { preset ->
                val active = weatherLocation == null && preset.id == weatherCityId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) accentColor else Color(0x247D7D91))
                        .clickable { onSelectWeatherCity(preset.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = preset.name,
                        color = if (active) Color.White else fg,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (!weather.isLiveFromNetwork) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Noch keine Wetterdaten", color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Canopy lädt die Vorhersage von Open-Meteo, sobald das Gerät online ist.", color = muted, fontSize = 12.5.sp)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (weather.updatedAtLabel.isNotEmpty()) {
            Text("Stand ${weather.updatedAtLabel}", color = muted, fontSize = 11.5.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(weather.cityName, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${weather.currentTempC}°C",
                    color = fg,
                    style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("H ${weather.highTempC}° · T ${weather.lowTempC}°", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Regenrisiko ${weather.rainChancePercent}%", color = muted, fontSize = 12.sp)
            }
        }
        Text(weather.conditionLabel, color = muted, fontSize = 13.sp)
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("STUNDENVERLAUF (${weather.peakHourLabel})", color = muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weather.hourlyPoints.forEach { pt ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentBg)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(pt.hour, color = muted, fontSize = 11.sp)
                    Text("${pt.tempC.toInt()}°", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
