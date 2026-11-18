package app.canopy.launcher.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SignalCellularAlt
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.system.LauncherNotification
import app.canopy.launcher.system.LocalHardwareTelemetry
import app.canopy.launcher.ui.theme.CanopyThemes
import app.canopy.launcher.ui.theme.ThemeTokens
import app.canopy.launcher.ui.tiles.SystemControlState
import app.canopy.launcher.ui.tiles.SystemToggleKey
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CanopyStatusBar(
    theme: ThemeTokens,
    systemState: SystemControlState,
    notificationCount: Int,
    hapticsEnabled: Boolean,
    onToggleQuickShade: () -> Unit,
    onOpenDevMenu: () -> Unit,
    modifier: Modifier = Modifier,
    barHeightDp: Dp = 34.dp,
) {
    val hw = LocalHardwareTelemetry.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeightDp)
            .clickable {
                CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                onToggleQuickShade()
            }
            .padding(start = 18.dp, end = 18.dp, top = (barHeightDp - 26.dp).coerceAtLeast(8.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = hw.timeLabel,
                color = theme.statusText,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            )
            if (notificationCount > 0) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Mitteilungen",
                    tint = theme.statusText.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp),
                )
            }
            if (systemState.dnd) {
                Icon(Icons.Rounded.DarkMode, contentDescription = "Nicht stören", tint = theme.statusText, modifier = Modifier.size(12.dp))
            }
        }

        // Camera cutout flashlight indicator glow
        Box(
            modifier = Modifier
                .size(14.dp)
                .let { m ->
                    if (systemState.flashlight) {
                        m.shadow(elevation = 16.dp, shape = CircleShape, ambientColor = Color(0xFFFDE047), spotColor = Color(0xFFFDE047))
                    } else {
                        m
                    }
                }
                .clip(CircleShape)
                .background(if (systemState.flashlight) Color(0xFFFEF08A) else Color(0xFF090A0D))
                .border(1.5.dp, Color.White.copy(alpha = 0.16f), CircleShape)
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (systemState.bluetooth) {
                Icon(Icons.Rounded.Bluetooth, contentDescription = "Bluetooth", tint = theme.statusText, modifier = Modifier.size(12.dp))
            }
            Icon(Icons.Rounded.SignalCellularAlt, contentDescription = "Mobilfunk", tint = theme.statusText, modifier = Modifier.size(13.dp))
            Icon(
                imageVector = if (systemState.wifi) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                contentDescription = "WLAN",
                tint = theme.statusText.copy(alpha = if (systemState.wifi) 1f else 0.55f),
                modifier = Modifier.size(13.dp),
            )
            Icon(Icons.Rounded.BatteryFull, contentDescription = "Akku", tint = theme.statusText, modifier = Modifier.size(14.dp))
            Text(
                text = "${hw.batteryPercent}%",
                color = theme.statusText,
                style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0x3D7D7D91))
                    .clickable { onOpenDevMenu() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = "Entwicklermenü", tint = theme.statusText, modifier = Modifier.size(13.dp))
            }
        }
    }
}

private data class ShadeToggleSpec(
    val key: SystemToggleKey,
    val label: String,
    val active: Boolean,
    val icon: ImageVector,
)

@Composable
fun QuickSettingsNotificationShade(
    theme: ThemeTokens,
    systemState: SystemControlState,
    notifications: List<LauncherNotification>,
    hapticsEnabled: Boolean,
    maxShadeHeightDp: Dp = 560.dp,
    onClose: () -> Unit,
    onOpenAppFromNotification: (appId: String, sourceRect: Rect?) -> Unit,
    onDismissNotification: (LauncherNotification) -> Unit,
    onDismissAllNotifications: () -> Unit,
    onRestoreDemoNotifications: () -> Unit,
) {
    val toggles = listOf(
        ShadeToggleSpec(SystemToggleKey.WIFI, "WLAN", systemState.wifi, if (systemState.wifi) Icons.Rounded.Wifi else Icons.Rounded.WifiOff),
        ShadeToggleSpec(SystemToggleKey.BLUETOOTH, "Bluetooth", systemState.bluetooth, Icons.Rounded.Bluetooth),
        ShadeToggleSpec(SystemToggleKey.FLASHLIGHT, "Taschenlampe", systemState.flashlight, Icons.Rounded.FlashlightOn),
        ShadeToggleSpec(SystemToggleKey.DND, "Nicht stören", systemState.dnd, Icons.Rounded.DarkMode),
        ShadeToggleSpec(SystemToggleKey.HOTSPOT, "Hotspot", systemState.hotspot, Icons.Rounded.Radio),
        ShadeToggleSpec(SystemToggleKey.AUTO_ROTATE, "Drehen", systemState.autoRotate, Icons.Rounded.ScreenRotation),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x47000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() }
            .padding(start = 10.dp, end = 10.dp, top = 42.dp, bottom = 72.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxShadeHeightDp)
                .shadow(22.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(theme.sheetBg)
                .border(1.dp, theme.sheetBorder, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* consume backdrop */ }
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Schnelleinstellungen", color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Statusleiste antippen", color = theme.textMuted, fontSize = 11.sp)
            }

            // 3x2 System Toggle Grid
            toggles.chunked(3).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (item.active) Color(0xFF2563EB) else Color(0x1F7D7D91))
                                .let { m -> if (item.active) m else m.border(1.dp, theme.sheetBorder, RoundedCornerShape(15.dp)) }
                                .clickable {
                                    CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                                    systemState.onToggle(item.key)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(item.icon, contentDescription = item.label, tint = if (item.active) Color.White else theme.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.height(3.dp))
                            Text(item.label, color = if (item.active) Color.White else theme.textPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Brightness Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1F7D7D91))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                Slider(
                    value = systemState.brightness,
                    onValueChange = systemState.onChangeBrightness,
                    valueRange = 0.25f..1f,
                    modifier = Modifier.weight(1f).height(26.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
                )
                Text(
                    text = "${(systemState.brightness * 100).roundToInt()} %",
                    color = theme.textPrimary,
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(theme.sheetBorder))

            // Notification Center Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Text("Benachrichtigungen (${notifications.size})", color = theme.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                if (notifications.isNotEmpty()) {
                    Text(
                        text = "Alle löschen",
                        color = theme.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x2E7D7D91))
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.REMOVE, hapticsEnabled)
                                onDismissAllNotifications()
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                } else {
                    Text(
                        text = "Demo-Mitteilungen",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x2E38BDF8))
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                                onRestoreDemoNotifications()
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }

            if (notifications.isEmpty()) {
                Text("Keine ungelesenen Mitteilungen.", color = theme.textMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    notifications.forEach { notif ->
                        SwipeableNotificationRow(
                            notif = notif,
                            theme = theme,
                            hapticsEnabled = hapticsEnabled,
                            onTap = { rect ->
                                CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                                onClose()
                                onOpenAppFromNotification(notif.appId, rect)
                            },
                            onDismiss = {
                                CanopyHaptics.trigger(HapticKind.TICK, hapticsEnabled)
                                onDismissNotification(notif)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableNotificationRow(
    notif: LauncherNotification,
    theme: ThemeTokens,
    hapticsEnabled: Boolean,
    onTap: (Rect?) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 65.dp.toPx() }
    var dragOffsetPx by remember(notif.id) { mutableFloatStateOf(0f) }
    var rowRect: Rect? = null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
            .alpha((1f - abs(dragOffsetPx) / (dismissThresholdPx * 2f)).coerceIn(0.35f, 1f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1F7D7D91))
            .onGloballyPositioned { rowRect = it.boundsInRoot() }
            .pointerInput(notif.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetPx += dragAmount
                    },
                    onDragEnd = {
                        if (abs(dragOffsetPx) > dismissThresholdPx) {
                            onDismiss()
                        } else {
                            dragOffsetPx = 0f
                        }
                    },
                    onDragCancel = { dragOffsetPx = 0f },
                )
            }
            .clickable { onTap(rowRect) }
            .padding(horizontal = 11.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(CanopyThemes.hslColor(notif.hue, 80f, 58f)))
                Text("${notif.appName} · ${notif.time}", color = theme.textMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(2.dp))
            Text(notif.title, color = theme.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(notif.body, color = theme.textSecondary, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x297D7D91))
                .clickable {
                    CanopyHaptics.trigger(HapticKind.TICK, hapticsEnabled)
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "${notif.appName} Benachrichtigung entfernen", tint = theme.textSecondary, modifier = Modifier.size(13.dp))
        }
    }
}
