package app.canopy.launcher.ui.overlays

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.data.WallpaperPresetId
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.system.InstalledAppsMapper
import app.canopy.launcher.system.NativeShortcutEntry
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

enum class FakeAppTargetKind {
    APP,
    TILE,
}

data class FakeAppTarget(
    val kind: FakeAppTargetKind,
    val appId: String? = null,
    val tile: TileData? = null,
    val sourceRect: Rect? = null,
    val showInfoOnly: Boolean = false,
    val webQuery: String? = null,
    val shortcutAction: String? = null,
)

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
            TileType.CLOCK -> listOf("Wecker stellen", "Timer 25 Min.")
            TileType.WEATHER, TileType.TEMP_CURVE, TileType.GREETING -> listOf("Regenradar öffnen", "Standort wechseln")
            TileType.CALENDAR, TileType.CALENDAR_TIMELINE -> listOf("Neuer Termin", "Wochenansicht")
            TileType.MEDIA -> listOf("Mix wechseln", "Warteschlange")
            TileType.APP_GROUP -> listOf("Gruppe umbenennen", "Apps verwalten")
            TileType.REMINDERS, TileType.NOTE -> listOf("Neuer Eintrag", "Alle abhaken")
            else -> listOf("Schnellaktion", "App-Info")
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
    onQuickSplit: (pageId: String, leafId: String, rectDp: Rect) -> Unit,
    onRemoveTile: (pageId: String, leafId: String) -> Unit,
    onShowInfo: (tile: TileData, rectDp: Rect) -> Unit,
    onShortcutAction: (label: String, tile: TileData, rectDp: Rect) -> Unit,
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
            ContextMenuRow(Icons.Rounded.Splitscreen, "Kachel halbieren", theme.textPrimary) {
                CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                onQuickSplit(menu.pageId, menu.leafId, menu.rectDp)
            }
            ContextMenuRow(Icons.Rounded.AspectRatio, "Größe ändern", theme.textPrimary) {
                CanopyHaptics.trigger(HapticKind.EDIT_ENTER, hapticsEnabled)
                onEnterEditMode()
            }
            ContextMenuRow(Icons.Rounded.Info, "App-Info", theme.textPrimary) {
                CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                onShowInfo(menu.tile, menu.rectDp)
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

data class LauncherSettingsBridge(
    val themeId: ThemeVariantId,
    val colorMode: ColorMode,
    val wallpaperId: WallpaperPresetId,
    val tonalIcons: Boolean,
    val shortcutBarMode: ShortcutBarMode,
    val onChangeThemeId: (ThemeVariantId) -> Unit,
    val onChangeColorMode: (ColorMode) -> Unit,
    val onChangeWallpaperId: (WallpaperPresetId) -> Unit,
    val onChangeTonalIcons: (Boolean) -> Unit,
    val onChangeShortcutBarMode: (ShortcutBarMode) -> Unit,
)

@Composable
fun FakeAppOverlay(
    target: FakeAppTarget,
    containerWidthPx: Float,
    containerHeightPx: Float,
    theme: ThemeTokens,
    motionSettings: MotionSettings,
    calendarEvents: List<CalendarEvent>,
    onAddCalendarEvent: (CalendarEvent) -> Unit,
    mediaState: MediaPlayerState,
    launcherSettings: LauncherSettingsBridge,
    allAppsCatalog: List<AppDefinition> = MockCatalogData.APPS_CATALOG,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val app = target.appId?.let { id -> allAppsCatalog.firstOrNull { it.id == id } ?: MockCatalogData.APPS_BY_ID[id] }
    val isBrowserMode = !target.showInfoOnly && (target.webQuery != null || app?.id == "app-browser")
    val title = when {
        target.webQuery != null -> "Netz · Web-Suche"
        app != null -> app.name
        target.tile != null -> target.tile.title
        else -> "App"
    }
    val hue = app?.hue ?: target.tile?.accentHue ?: 218f
    var browserQuery by remember(target.webQuery) { mutableStateOf(target.webQuery ?: "") }
    var browserInput by remember(target.webQuery) { mutableStateOf(target.webQuery ?: "") }

    val subtitle = when {
        isBrowserMode && browserQuery.isNotBlank() -> "Suche: „${browserQuery.trim()}“"
        target.shortcutAction != null -> "Aktion: ${target.shortcutAction}"
        app != null -> app.subtitle
        else -> "System-Kachel geöffnet"
    }

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
        Column(modifier = Modifier.fillMaxSize().background(bg)) {
            // Hero Header
            Column(
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
                    .padding(start = 18.dp, end = 18.dp, top = 44.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Text("Zurück", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(if (target.showInfoOnly) "App-Info" else "Aktiv", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (app != null) {
                        AppSquircleIcon(appId = app.id, iconKey = app.iconKey, color = app.color, size = 56.dp, badge = app.badge)
                    } else {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(title.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column {
                        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp)
                    }
                }
            }

            // Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    target.showInfoOnly -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardBg).padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Version 4.2.0 · Canopy", color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Speicher: 48,4 MB · Berechtigungen: Mitteilungen, Standort", color = muted, fontSize = 12.sp)
                            }
                        }
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardBg).padding(16.dp)) {
                            Text("Kachel-Anpassung", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("Diese Kachel passt sich im Tiling-Grid automatisch an XS-, S-, M- und L-Flächen an.", color = muted, fontSize = 12.sp)
                        }
                    }

                    isBrowserMode -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = browserInput,
                                onValueChange = { browserInput = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                label = { Text("Web-Suche") },
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor)
                                    .clickable {
                                        browserQuery = browserInput.trim()
                                        runCatching {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                android.net.Uri.parse(UniversalSearchEngine.buildWebSearchUrl(browserQuery))
                                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                            ) {
                                Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Im Browser öffnen", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (browserQuery.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBg)
                                    .padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("Direktantwort · Web-Überblick", color = accentColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                Text(browserQuery, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Web-Suche über Intent bereit: ${UniversalSearchEngine.buildWebSearchUrl(browserQuery)}", color = muted, fontSize = 12.sp)
                            }
                        }
                    }

                    target.tile?.type == TileType.CALENDAR || target.tile?.type == TileType.CALENDAR_TIMELINE || app?.id == "app-kalender" -> {
                        CalendarTargetView(
                            events = calendarEvents,
                            initialShowForm = target.shortcutAction == "Neuer Termin",
                            fg = fg,
                            muted = muted,
                            cardBg = cardBg,
                            accentColor = accentColor,
                            hue = hue,
                            onAddEvent = onAddCalendarEvent,
                        )
                    }

                    target.tile?.type == TileType.CLOCK || app?.id == "app-uhr" -> {
                        ClockTargetView(
                            startTimerImmediately = target.shortcutAction == "Timer 25 Min.",
                            fg = fg,
                            muted = muted,
                            cardBg = cardBg,
                            accentColor = accentColor,
                        )
                    }

                    target.tile?.type == TileType.REMINDERS || target.tile?.type == TileType.NOTE || app?.id == "app-notizen" -> {
                        NotesTargetView(
                            checkAllInitially = target.shortcutAction == "Alle abhaken",
                            fg = fg,
                            muted = muted,
                            cardBg = cardBg,
                            accentColor = accentColor,
                        )
                    }

                    target.tile?.type == TileType.MEDIA || app?.id == "app-musik" || app?.id == "app-podcast" -> {
                        MediaTargetView(
                            mediaState = mediaState,
                            fg = fg,
                            muted = muted,
                            cardBg = cardBg,
                            accentColor = accentColor,
                            accentBg = accentBg,
                        )
                    }

                    app?.id == "app-einstellungen" -> {
                        SettingsTargetView(
                            settings = launcherSettings,
                            fg = fg,
                            muted = muted,
                            cardBg = cardBg,
                            accentColor = accentColor,
                        )
                    }

                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardBg).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("$title · Live-Ansicht", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = target.shortcutAction?.let { "Zielaktion „$it“ ausgeführt." }
                                    ?: "Alle adaptiven Kachelgrößen (XS, S, M, L, TICKER, LIST) sind für $title aktiv.",
                                color = muted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTargetView(
    events: List<CalendarEvent>,
    initialShowForm: Boolean,
    fg: Color,
    muted: Color,
    cardBg: Color,
    accentColor: Color,
    hue: Float,
    onAddEvent: (CalendarEvent) -> Unit,
) {
    var showForm by remember { mutableStateOf(initialShowForm) }
    var titleDraft by remember { mutableStateOf("") }
    var timeDraft by remember { mutableStateOf("16:30") }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Mittwoch, 18. November", color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(accentColor)
                .clickable { showForm = !showForm }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text("Neuer Termin", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showForm) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardBg).border(1.dp, accentColor, RoundedCornerShape(18.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(value = titleDraft, onValueChange = { titleDraft = it }, label = { Text("Termin-Titel") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = timeDraft, onValueChange = { timeDraft = it }, label = { Text("Uhrzeit") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor)
                    .clickable {
                        val clean = titleDraft.trim()
                        if (clean.isNotEmpty()) {
                            onAddEvent(
                                CalendarEvent(
                                    id = "ev-${System.currentTimeMillis()}",
                                    title = clean,
                                    time = timeDraft.ifBlank { "16:30" },
                                    endTime = "17:30",
                                    location = "Studio",
                                    startHour = 16.5f,
                                    durationHours = 1f,
                                    hue = hue,
                                )
                            )
                            titleDraft = ""
                            showForm = false
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Termin speichern", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    events.forEach { ev ->
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardBg).padding(15.dp)
        ) {
            Text(ev.title, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${ev.time} – ${ev.endTime} · ${ev.location}", color = muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ClockTargetView(
    startTimerImmediately: Boolean,
    fg: Color,
    muted: Color,
    cardBg: Color,
    accentColor: Color,
) {
    var seconds by remember { mutableIntStateOf(1500) }
    var running by remember(startTimerImmediately) { mutableStateOf(startTimerImmediately) }

    LaunchedEffect(running) {
        while (running && seconds > 0) {
            delay(1000L)
            seconds -= 1
        }
    }

    val mm = (seconds / 60).toString().padStart(2, '0')
    val ss = (seconds % 60).toString().padStart(2, '0')

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Fokus-Timer", color = muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text("$mm:$ss", color = fg, style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x297D7D91)).clickable {
                    running = false; seconds = 1500
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = fg, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(accentColor).clickable { running = !running },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Timer Start/Pause", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun NotesTargetView(
    checkAllInitially: Boolean,
    fg: Color,
    muted: Color,
    cardBg: Color,
    accentColor: Color,
) {
    var items by remember(checkAllInitially) {
        val base = SystemTileDefaults.DEFAULT_REMINDERS
        mutableStateOf(if (checkAllInitially) base.map { it.copy(done = true) } else base)
    }
    var draft by remember { mutableStateOf("") }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Neuer Eintrag") }, modifier = Modifier.weight(1f), singleLine = true)
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accentColor).clickable {
                val clean = draft.trim()
                if (clean.isNotEmpty()) {
                    items = listOf(app.canopy.launcher.layout.ReminderItem("n-${System.currentTimeMillis()}", clean, false)) + items
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
                    items = items.map { if (it.id == n.id) it.copy(done = !it.done) else it }
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
private fun MediaTargetView(
    mediaState: MediaPlayerState,
    fg: Color,
    muted: Color,
    cardBg: Color,
    accentColor: Color,
    accentBg: Color,
) {
    val track = SystemTileDefaults.MEDIA_TRACKS[mediaState.trackIndex % SystemTileDefaults.MEDIA_TRACKS.size]
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(track.title, color = fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${track.artist} · ${track.album}", color = muted, fontSize = 12.5.sp)
            }
        }
        Slider(
            value = mediaState.progress,
            onValueChange = mediaState.onSeek,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(cardBg).clickable { mediaState.onPrev() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Zurück", tint = fg)
            }
            Box(Modifier.size(54.dp).clip(CircleShape).background(accentColor).clickable { mediaState.onTogglePlay() }, contentAlignment = Alignment.Center) {
                Icon(if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", tint = Color.White)
            }
            Box(Modifier.size(42.dp).clip(CircleShape).background(cardBg).clickable { mediaState.onNext() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Weiter", tint = fg)
            }
        }
    }

    Text("Warteschlange", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    SystemTileDefaults.MEDIA_TRACKS.forEachIndexed { idx, t ->
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (idx == mediaState.trackIndex) accentBg else cardBg).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(t.title, color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Text(t.artist, color = muted, fontSize = 11.5.sp)
            }
            if (idx == mediaState.trackIndex) Text("Läuft", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsTargetView(
    settings: LauncherSettingsBridge,
    fg: Color,
    muted: Color,
    cardBg: Color,
    accentColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardBg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("STILVARIANTE", color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                ThemeVariantId.MATERIAL_YOU to "Material",
                ThemeVariantId.DARK_BOLD to "Kräftig",
                ThemeVariantId.GLASS to "Glas",
            ).forEach { (id, label) ->
                val active = settings.themeId == id
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (active) accentColor else Color(0x247D7D91)).clickable { settings.onChangeThemeId(id) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (active) Color.White else fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardBg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().clickable { settings.onChangeColorMode(if (settings.colorMode == ColorMode.DARK) ColorMode.LIGHT else ColorMode.DARK) }, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Farbmodus", color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(if (settings.colorMode == ColorMode.DARK) "Dunkel" else "Hell", color = accentColor, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth().clickable { settings.onChangeTonalIcons(!settings.tonalIcons) }, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tonale Icons", color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(if (settings.tonalIcons) "An" else "Aus", color = accentColor, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
