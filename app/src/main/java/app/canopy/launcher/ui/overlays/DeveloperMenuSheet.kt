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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    backupStatusMessage: String? = null,
    onClose: () -> Unit,
    onChangeThemeId: (ThemeVariantId) -> Unit,
    onChangeColorMode: (ColorMode) -> Unit,
    onChangeWallpaperId: (WallpaperPresetId) -> Unit,
    onChangeTonalIcons: (Boolean) -> Unit,
    onChangeShortcutBarMode: (ShortcutBarMode) -> Unit,
    onChangeMotionSettings: (MotionSettings) -> Unit,
    onChangeShowHapticHud: (Boolean) -> Unit,
    onChangeUseSystemWallpaper: (Boolean) -> Unit = {},
    onRequestDefaultHome: () -> Unit = {},
    onExportLayoutJson: () -> Unit = {},
    onImportLayoutJson: () -> Unit = {},
    onResetLayout: () -> Unit,
) {
    if (!isOpen) return

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
                    Text("Entwicklermenü", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Entwicklermenü schließen", tint = Color(0xFFF8FAFC), modifier = Modifier.size(17.dp))
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

            // Spring Sliders
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    DevToggleBtn(
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
                    DevToggleBtn(
                        icon = Icons.Rounded.Tune,
                        label = "Haptik-HUD: ${if (showHapticHud) "An" else "Aus"}",
                        active = showHapticHud,
                        modifier = Modifier.weight(1f),
                        onClick = { onChangeShowHapticHud(!showHapticHud) },
                    )
                }
                DevToggleBtn(
                    icon = Icons.Rounded.Wallpaper,
                    label = "System-Wallpaper: ${if (useSystemWallpaper) "An" else "Aus"}",
                    active = useSystemWallpaper,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        CanopyHaptics.trigger(HapticKind.TICK, motionSettings.hapticsEnabled)
                        onChangeUseSystemWallpaper(!useSystemWallpaper)
                    },
                )
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
            DevSection(label = "System-Launcher & Backup") {
                DevToggleBtn(
                    icon = Icons.Rounded.Home,
                    label = "Als Standard-Launcher festlegen",
                    active = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        CanopyHaptics.trigger(HapticKind.PRESS, motionSettings.hapticsEnabled)
                        onRequestDefaultHome()
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DevToggleBtn(
                        icon = Icons.Rounded.FileUpload,
                        label = "JSON Export",
                        active = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CanopyHaptics.trigger(HapticKind.SNAP, motionSettings.hapticsEnabled)
                            onExportLayoutJson()
                        },
                    )
                    DevToggleBtn(
                        icon = Icons.Rounded.FileDownload,
                        label = "JSON Import",
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
