package app.canopy.launcher.ui.overlays

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.AnimationMood
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.data.WallpaperPresetId
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.ui.theme.CanopyThemes
import java.util.Locale
import kotlin.math.roundToInt

object FrameMetricsMath {
    fun computeRollingFps(frameDeltasNanos: List<Long>): Pair<Int, Float> {
        val valid = frameDeltasNanos.filter { it > 0L }
        if (valid.isEmpty()) return 0 to 0f
        val avgNs = valid.sum().toDouble() / valid.size.toDouble()
        val avgMs = (avgNs / 1_000_000.0).toFloat()
        val fps = (1_000_000_000.0 / avgNs).roundToInt().coerceIn(0, 240)
        return fps to avgMs
    }
}

@Composable
fun DeveloperMenuSheet(
    isOpen: Boolean,
    themeId: ThemeVariantId,
    colorMode: ColorMode,
    wallpaperId: WallpaperPresetId,
    tonalIcons: Boolean,
    shortcutBarMode: ShortcutBarMode,
    motionSettings: MotionSettings,
    showHapticHud: Boolean,
    useSystemWallpaper: Boolean = false,
    showFpsHud: Boolean = false,
    backupStatusMessage: String? = null,
    onClose: () -> Unit,
    onChangeThemeId: (ThemeVariantId) -> Unit,
    onChangeColorMode: (ColorMode) -> Unit,
    onChangeWallpaperId: (WallpaperPresetId) -> Unit,
    onChangeTonalIcons: (Boolean) -> Unit,
    onChangeShortcutBarMode: (ShortcutBarMode) -> Unit,
    onChangeMotionSettings: (MotionSettings) -> Unit,
    onChangeShowHapticHud: (Boolean) -> Unit,
    onChangeShowFpsHud: (Boolean) -> Unit = {},
    onOpenGestureGuide: () -> Unit = {},
    onOpenSetupAssistant: () -> Unit = {},
    onChangeUseSystemWallpaper: (Boolean) -> Unit = {},
    onRequestDefaultHome: () -> Unit = {},
    onExportLayoutJson: () -> Unit = {},
    onImportLayoutJson: () -> Unit = {},
    onResetLayout: () -> Unit,
    developerMode: Boolean = false,
    lockPortrait: Boolean = true,
    onChangeLockPortrait: (Boolean) -> Unit = {},
    hiddenApps: List<Pair<String, String>> = emptyList(),
    onUnhideApp: (String) -> Unit = {},
    versionLabel: String = "",
    onUnlockDeveloperMode: () -> Unit = {},
) {
    if (!isOpen) return
    var versionTapCount by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x7A000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161922))
                .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* consume */ }
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Text("Einstellungen", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Einstellungen schließen", tint = Color(0xFFF8FAFC), modifier = Modifier.size(17.dp))
                }
            }

            // Stilvariante
            DevSection(label = "Stilvariante") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        ThemeVariantId.MATERIAL_YOU to "Material You",
                        ThemeVariantId.DARK_BOLD to "Dunkel & kräftig",
                        ThemeVariantId.GLASS to "Glas",
                    ).forEach { (id, title) ->
                        DevChip(
                            label = title,
                            active = themeId == id,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                                onChangeThemeId(id)
                                if (id == ThemeVariantId.DARK_BOLD) onChangeColorMode(ColorMode.DARK)
                                if (id == ThemeVariantId.MATERIAL_YOU) onChangeColorMode(ColorMode.LIGHT)
                            },
                        )
                    }
                }
            }

            // Farbmodus
            DevSection(label = "Farbmodus") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    DevChip(
                        label = "Hell",
                        active = colorMode == ColorMode.LIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                            onChangeColorMode(ColorMode.LIGHT)
                        },
                    )
                    DevChip(
                        label = "Dunkel",
                        active = colorMode == ColorMode.DARK,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                            onChangeColorMode(ColorMode.DARK)
                        },
                    )
                }
            }

            // Farbwelten
            DevSection(label = "Farbwelt") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    CanopyThemes.WALLPAPER_PRESETS.forEach { preset ->
                        val active = wallpaperId == preset.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) preset.previewColor.copy(alpha = 0.25f) else Color(0x0DFFFFFF))
                                .border(1.dp, if (active) preset.previewColor else Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
                                .clickable {
                                    CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                    onChangeWallpaperId(preset.id)
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = preset.label.substringBefore(" "),
                                color = if (active) Color.White else Color(0xFFCBD5E1),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Animationsstärke
            DevSection(label = "Animationsstärke") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        AnimationMood.VERSPIELT to "Verspielt",
                        AnimationMood.NORMAL to "Normal",
                        AnimationMood.RUHIG to "Ruhig",
                    ).forEach { (mood, title) ->
                        DevChip(
                            label = title,
                            active = motionSettings.mood == mood,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                onChangeMotionSettings(motionSettings.copy(mood = mood))
                            },
                        )
                    }
                }
            }

            // Spring Sliders (developer only)
            if (developerMode) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SliderRow(
                    label = "Federstärke (Steifigkeit)",
                    valueText = String.format(Locale.US, "%.2f×", motionSettings.stiffnessScale),
                    value = motionSettings.stiffnessScale,
                    range = 0.5f..2.0f,
                    onValueChange = { onChangeMotionSettings(motionSettings.copy(stiffnessScale = it)) },
                )
                SliderRow(
                    label = "Dämpfung",
                    valueText = String.format(Locale.US, "%.2f×", motionSettings.dampingScale),
                    value = motionSettings.dampingScale,
                    range = 0.5f..1.8f,
                    onValueChange = { onChangeMotionSettings(motionSettings.copy(dampingScale = it)) },
                )
            }

            // Toggles
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DevToggleBtn(
                        icon = Icons.Rounded.Vibration,
                        label = "Haptik: ${if (motionSettings.hapticsEnabled) "An" else "Aus"}",
                        active = motionSettings.hapticsEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onChangeMotionSettings(motionSettings.copy(hapticsEnabled = !motionSettings.hapticsEnabled)) },
                    )
                    if (developerMode) DevToggleBtn(
                        icon = Icons.Rounded.AutoAwesome,
                        label = "Zeitlupe 0,25×: ${if (motionSettings.slowMotion) "An" else "Aus"}",
                        active = motionSettings.slowMotion,
                        modifier = Modifier.weight(1f),
                        onClick = { onChangeMotionSettings(motionSettings.copy(slowMotion = !motionSettings.slowMotion)) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DevToggleBtn(
                        icon = Icons.Rounded.Palette,
                        label = "Tonale Icons: ${if (tonalIcons) "An" else "Aus"}",
                        active = tonalIcons,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                            onChangeTonalIcons(!tonalIcons)
                        },
                    )
                    if (developerMode) DevToggleBtn(
                        icon = Icons.Rounded.Tune,
                        label = "Haptik-HUD: ${if (showHapticHud) "An" else "Aus"}",
                        active = showHapticHud,
                        modifier = Modifier.weight(1f),
                        onClick = { onChangeShowHapticHud(!showHapticHud) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DevToggleBtn(
                        icon = Icons.Rounded.Wallpaper,
                        label = "System-Wallpaper: ${if (useSystemWallpaper) "An" else "Aus"}",
                        active = useSystemWallpaper,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                            onChangeUseSystemWallpaper(!useSystemWallpaper)
                        },
                    )
                    DevToggleBtn(
                        icon = Icons.Rounded.ScreenLockPortrait,
                        label = "Hochformat: ${if (lockPortrait) "Fest" else "Drehbar"}",
                        active = lockPortrait,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                            onChangeLockPortrait(!lockPortrait)
                        },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (developerMode) DevToggleBtn(
                        icon = Icons.Rounded.Speed,
                        label = "FPS-HUD: ${if (showFpsHud) "An" else "Aus"}",
                        active = showFpsHud,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                            onChangeShowFpsHud(!showFpsHud)
                        },
                    )
                    DevToggleBtn(
                        icon = Icons.Rounded.Info,
                        label = "Gesten-Guide",
                        active = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                            onOpenGestureGuide()
                        },
                    )
                }
            }

            // 4. Reihe
            DevSection(label = "4. Reihe (Kürzel-Leiste)") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        ShortcutBarMode.ALL to "Alle Seiten",
                        ShortcutBarMode.START_ONLY to "Nur Start",
                        ShortcutBarMode.OFF to "Aus",
                    ).forEach { (mode, label) ->
                        DevChip(
                            label = label,
                            active = shortcutBarMode == mode,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                                onChangeShortcutBarMode(mode)
                            },
                        )
                    }
                }
            }

            // System-Launcher & JSON Layout-Backup
            if (hiddenApps.isNotEmpty()) {
                DevSection(label = "Ausgeblendete Apps") {
                    hiddenApps.forEach { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0DFFFFFF))
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(name, color = Color(0xFFF8FAFC), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(
                                text = "Einblenden",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUnhideApp(id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            DevSection(label = "Launcher & Sicherung") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DevToggleBtn(
                        icon = Icons.Rounded.Home,
                        label = "Standard-Launcher",
                        active = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                            onRequestDefaultHome()
                        },
                    )
                    DevToggleBtn(
                        icon = Icons.Rounded.Tune,
                        label = "Einrichtung",
                        active = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                            onOpenSetupAssistant()
                        },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DevToggleBtn(
                        icon = Icons.Rounded.FileUpload,
                        label = "Layout kopieren",
                        active = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                            onExportLayoutJson()
                        },
                    )
                    DevToggleBtn(
                        icon = Icons.Rounded.FileDownload,
                        label = "Layout einfügen",
                        active = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                            onImportLayoutJson()
                        },
                    )
                }
                if (!backupStatusMessage.isNullOrBlank()) {
                    Text(
                        text = backupStatusMessage,
                        color = Color(0xFF7DD3FC),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x29EF4444))
                    .border(1.dp, Color(0x73EF4444), RoundedCornerShape(14.dp))
                    .clickable {
                        CanopyHaptics.trigger(HapticKind.REMOVE, motionSettings.hapticsEnabled)
                        onResetLayout()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text("Layout zurücksetzen", color = Color(0xFFFCA5A5), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }

            if (versionLabel.isNotBlank()) {
                Text(
                    text = "Canopy $versionLabel" + if (developerMode) " · Entwickleroptionen aktiv" else "",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!developerMode) {
                                versionTapCount += 1
                                if (versionTapCount >= 7) {
                                    versionTapCount = 0
                                    onUnlockDeveloperMode()
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DevSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        content()
    }
}

@Composable
private fun DevChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color(0x3338BDF8) else Color(0x0DFFFFFF))
            .border(1.dp, if (active) Color(0xFF38BDF8) else Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) Color.White else Color(0xFFCBD5E1),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFFF8FAFC), fontSize = 12.sp)
            Text(valueText, color = Color(0xFF38BDF8), style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
        )
    }
}

@Composable
private fun DevToggleBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Color(0x2E34D399) else Color(0x0DFFFFFF))
            .border(1.dp, if (active) Color(0xFF34D399) else Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (active) Color(0xFFA7F3D0) else Color(0xFFCBD5E1), modifier = Modifier.size(15.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            color = if (active) Color(0xFFA7F3D0) else Color(0xFFCBD5E1),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LiveFpsHudOverlay(modifier: Modifier = Modifier) {
    var fps by remember { mutableIntStateOf(60) }
    var frameMs by remember { mutableFloatStateOf(16.6f) }

    LaunchedEffect(Unit) {
        val deltas = ArrayDeque<Long>(30)
        var lastFrameNs = 0L
        while (true) {
            withFrameNanos { nowNs ->
                if (lastFrameNs > 0L) {
                    val delta = nowNs - lastFrameNs
                    if (deltas.size >= 24) deltas.removeFirst()
                    deltas.addLast(delta)
                    val (nextFps, nextMs) = FrameMetricsMath.computeRollingFps(deltas.toList())
                    fps = nextFps
                    frameMs = nextMs
                }
                lastFrameNs = nowNs
            }
        }
    }

    val badgeColor = when {
        fps >= 55 -> Color(0xFF34D399)
        fps >= 35 -> Color(0xFFFBBF24)
        else -> Color(0xFFF87171)
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xD90F172A))
            .border(1.dp, badgeColor.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        Text(
            text = String.format(Locale.US, "%d FPS · %.1f ms", fps, frameMs),
            color = Color.White,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
fun GestureCheatSheetDialog(onClose: () -> Unit) {
    val entries = remember {
        listOf(
            "Nach oben wischen" to "Universelle Suche (Apps, Kontakte, Web) öffnen",
            "Nach unten wischen" to "Schnelleinstellungen & Mitteilungen öffnen",
            "Zwei Finger zusammenziehen" to "Seitenübersicht (Umbenennen, Duplizieren, Startseite)",
            "Kachel gedrückt halten" to "Kontextmenü mit App-Shortcuts & Farbwahl öffnen",
            "Trennlinie gedrückt halten" to "Trennlinien- & Layout-Bearbeitungsmodus starten",
            "Trennlinie doppelt tippen" to "Teilung Horizontal ↔ Vertikal wechseln",
            "2D-Kreuzung ziehen" to "Zwei angrenzende Teilungen gleichzeitig skalieren",
            "Kachel an Rand ziehen" to "Während Drag zur Nachbarseite blättern",
            "App auf Gruppe ziehen" to "App direkt in eine Bento-Gruppe einsortieren",
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x94000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 540.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF7111827))
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* consume */ }
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Canopy Gesten-Guide", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Alle Navigations- & Tiling-Gesten im Überblick", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x1FFFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Schließen", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            entries.forEach { (gesture, desc) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x0DFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(gesture, color = Color(0xFF38BDF8), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                }
            }
        }
    }
}
