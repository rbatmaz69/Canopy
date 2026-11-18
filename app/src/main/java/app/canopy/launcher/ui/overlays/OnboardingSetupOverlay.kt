package app.canopy.launcher.ui.overlays

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.system.NotificationBadgeStore
import app.canopy.launcher.system.OpenMeteoWeatherParser

data class OnboardingStatusSnapshot(
    val isDefaultHome: Boolean,
    val notificationAccess: Boolean,
    val calendarGranted: Boolean,
    val contactsGranted: Boolean,
    val activityGranted: Boolean,
) {
    val allCoreGranted: Boolean
        get() = isDefaultHome && notificationAccess && calendarGranted && contactsGranted && activityGranted
}

object OnboardingPermissionsHelper {
    fun requiredRuntimePermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> = buildList {
        add(Manifest.permission.READ_CALENDAR)
        add(Manifest.permission.READ_CONTACTS)
        if (sdkInt >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    fun inspectStatus(context: Context, isDefaultHome: Boolean): OnboardingStatusSnapshot {
        fun granted(perm: String): Boolean =
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

        val activityOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            granted(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            true
        }
        return OnboardingStatusSnapshot(
            isDefaultHome = isDefaultHome,
            notificationAccess = NotificationBadgeStore.isNotificationAccessEnabled(context),
            calendarGranted = granted(Manifest.permission.READ_CALENDAR),
            contactsGranted = granted(Manifest.permission.READ_CONTACTS),
            activityGranted = activityOk,
        )
    }
}

@Composable
fun OnboardingSetupOverlay(
    status: OnboardingStatusSnapshot,
    themeId: ThemeVariantId,
    colorMode: ColorMode,
    useSystemWallpaper: Boolean,
    weatherCityId: String,
    onRefreshStatus: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onChangeThemeId: (ThemeVariantId) -> Unit,
    onChangeColorMode: (ColorMode) -> Unit,
    onChangeUseSystemWallpaper: (Boolean) -> Unit,
    onSelectWeatherCity: (String) -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }

    val runtimePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshStatus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB8000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { /* modal barrier */ }
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF111827))
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(26.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Canopy Einrichtung",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Schritt ${step + 1} von 3",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Überspringen",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onComplete() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            // Step Progress Dots
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (idx <= step) Color(0xFF38BDF8) else Color(0x29FFFFFF))
                    )
                }
            }

            when (step) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Home, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            Text("Als Standard-Launcher festlegen", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Damit dein Motorola-Launcher beim Home-Wisch nicht mehr dazwischenspringt, setze Canopy als deine Standard-Start-App.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                        SetupCheckRow(
                            title = "Standard-Start-App (ROLE_HOME)",
                            subtitle = if (status.isDefaultHome) "Aktiv – Canopy ist dein Startbildschirm" else "Tippen zum Festlegen",
                            checked = status.isDefaultHome,
                            onClick = onRequestDefaultHome,
                        )
                    }
                }

                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            Text("Live-Daten & Berechtigungen", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Erlaube den Zugriff, damit Benachrichtigungen, Termine, Kontakte und Schrittzähler mit deinen echten Daten arbeiten.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.5.sp,
                        )
                        SetupCheckRow(
                            title = "Benachrichtigungszugriff",
                            subtitle = if (status.notificationAccess) "Erlaubt" else "In Android-Einstellungen aktivieren",
                            checked = status.notificationAccess,
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            },
                        )
                        SetupCheckRow(
                            title = "Kalender-Termine",
                            subtitle = if (status.calendarGranted) "Erlaubt" else "Tippen zum Anfragen",
                            checked = status.calendarGranted,
                            onClick = {
                                runtimePermLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR))
                            },
                        )
                        SetupCheckRow(
                            title = "Kontakte & Schnellsuche",
                            subtitle = if (status.contactsGranted) "Erlaubt" else "Tippen zum Anfragen",
                            checked = status.contactsGranted,
                            onClick = {
                                runtimePermLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                            },
                        )
                        SetupCheckRow(
                            title = "Schrittzähler / Aktivität",
                            subtitle = if (status.activityGranted) "Erlaubt" else "Tippen zum Anfragen",
                            checked = status.activityGranted,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    runtimePermLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
                                }
                            },
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x2438BDF8))
                                .border(1.dp, Color(0x7338BDF8), RoundedCornerShape(14.dp))
                                .clickable {
                                    runtimePermLauncher.launch(OnboardingPermissionsHelper.requiredRuntimePermissions().toTypedArray())
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Alle Runtime-Berechtigungen jetzt anfragen",
                                color = Color(0xFF7DD3FC),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            Text("Design & Wetter-Standort", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        SetupCheckRow(
                            title = "Echtes System-Wallpaper nutzen",
                            subtitle = if (useSystemWallpaper) "Dein Motorola-Wallpaper scheint durch" else "Canopy-Hintergrund aktiv",
                            checked = useSystemWallpaper,
                            onClick = { onChangeUseSystemWallpaper(!useSystemWallpaper) },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                ThemeVariantId.MATERIAL_YOU to "Material You",
                                ThemeVariantId.DARK_BOLD to "Kräftig",
                                ThemeVariantId.GLASS to "Glas",
                            ).forEach { (id, label) ->
                                val active = themeId == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) Color(0xFF38BDF8) else Color(0x14FFFFFF))
                                        .clickable { onChangeThemeId(id) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) Color(0xFF0F172A) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(ColorMode.LIGHT to "Hell", ColorMode.DARK to "Dunkel").forEach { (mode, label) ->
                                val active = colorMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) Color(0xFF38BDF8) else Color(0x14FFFFFF))
                                        .clickable { onChangeColorMode(mode) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) Color(0xFF0F172A) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Text("Wetter-Stadt (Open-Meteo Live)", color = Color(0xFF94A3B8), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            OpenMeteoWeatherParser.CITY_PRESETS.forEach { preset ->
                                val active = preset.id == weatherCityId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) Color(0xFF38BDF8) else Color(0x14FFFFFF))
                                        .clickable { onSelectWeatherCity(preset.id) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        text = preset.name,
                                        color = if (active) Color(0xFF0F172A) else Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (step > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x1AFFFFFF))
                            .clickable { step -= 1 }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Zurück", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF38BDF8))
                        .clickable {
                            if (step < 2) step += 1 else onComplete()
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (step < 2) "Weiter" else "Fertig & Starten",
                        color = Color(0xFF0F172A),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupCheckRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color(0x1E34D399) else Color(0x0DFFFFFF))
            .border(1.dp, if (checked) Color(0x7334D399) else Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = if (checked) Color(0xFFA7F3D0) else Color(0xFF94A3B8), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) Color(0xFF34D399) else Color(0xFF64748B),
            modifier = Modifier.size(20.dp),
        )
    }
}
