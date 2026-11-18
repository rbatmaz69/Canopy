package app.canopy.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CompassCalibration
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.system.AppIconRepository
import app.canopy.launcher.system.AdaptiveIconGeometry
import app.canopy.launcher.ui.tiles.AppIdentity
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxSize
import app.canopy.launcher.system.InstalledAppsMapper
import app.canopy.launcher.ui.theme.CanopyThemes

@Immutable
data class IconStyleConfig(
    val tonalIcons: Boolean = true,
    val themeId: ThemeVariantId = ThemeVariantId.MATERIAL_YOU,
    val colorMode: ColorMode = ColorMode.LIGHT,
    val clearedBadgeAppIds: Set<String> = emptySet(),
)

val LocalIconStyle = staticCompositionLocalOf { IconStyleConfig() }

@Immutable
data class ResolvedSquircleColors(
    val backgroundBrush: Brush,
    val glyphColor: Color,
    val borderColor: Color?,
    val borderWidthDp: Float,
)

object AppIconRegistry {
    private val VECTOR_MAP: Map<String, ImageVector> = mapOf(
        "Activity" to Icons.AutoMirrored.Rounded.DirectionsRun,
        "AlarmClock" to Icons.Rounded.Alarm,
        "Aperture" to Icons.Rounded.Camera,
        "Archive" to Icons.Rounded.Archive,
        "AtSign" to Icons.Rounded.AlternateEmail,
        "BarChart3" to Icons.Rounded.BarChart,
        "BatteryCharging" to Icons.Rounded.BatteryChargingFull,
        "BookMarked" to Icons.Rounded.Bookmarks,
        "BookOpen" to Icons.AutoMirrored.Rounded.MenuBook,
        "Bookmark" to Icons.Rounded.Bookmark,
        "Bluetooth" to Icons.Rounded.Bluetooth,
        "Calculator" to Icons.Rounded.Calculate,
        "Calendar" to Icons.Rounded.CalendarToday,
        "Camera" to Icons.Rounded.CameraAlt,
        "CheckSquare" to Icons.Rounded.CheckBox,
        "Cloud" to Icons.Rounded.Cloud,
        "CloudRain" to Icons.Rounded.CloudQueue,
        "Compass" to Icons.Rounded.Explore,
        "Crown" to Icons.Rounded.WorkspacePremium,
        "Dices" to Icons.Rounded.Grain,
        "Disc3" to Icons.Rounded.Radio,
        "FileSignature" to Icons.Rounded.EditNote,
        "FileText" to Icons.Rounded.Description,
        "Film" to Icons.Rounded.Movie,
        "Flashlight" to Icons.Rounded.FlashlightOn,
        "Focus" to Icons.Rounded.CenterFocusStrong,
        "FolderOpen" to Icons.Rounded.FolderOpen,
        "Gamepad2" to Icons.Rounded.SportsEsports,
        "Globe" to Icons.Rounded.Explore,
        "HardDrive" to Icons.Rounded.Storage,
        "Headphones" to Icons.Rounded.Headphones,
        "Headset" to Icons.Rounded.HeadsetMic,
        "HeartPulse" to Icons.Rounded.Favorite,
        "Home" to Icons.Rounded.Home,
        "Image" to Icons.Rounded.Image,
        "KeyRound" to Icons.Rounded.Key,
        "Landmark" to Icons.Rounded.AccountBalance,
        "Languages" to Icons.Rounded.Translate,
        "Mail" to Icons.Rounded.Mail,
        "MapPin" to Icons.Rounded.Map,
        "MessageCircle" to Icons.Rounded.ChatBubble,
        "MessagesSquare" to Icons.Rounded.Forum,
        "Mic" to Icons.Rounded.Mic,
        "Moon" to Icons.Rounded.DarkMode,
        "Mountain" to Icons.Rounded.Landscape,
        "Music2" to Icons.Rounded.MusicNote,
        "Navigation" to Icons.Rounded.Navigation,
        "Newspaper" to Icons.AutoMirrored.Rounded.Article,
        "Package" to Icons.Rounded.LocalShipping,
        "Palette" to Icons.Rounded.Palette,
        "PenTool" to Icons.Rounded.Draw,
        "Phone" to Icons.Rounded.Phone,
        "Printer" to Icons.Rounded.Print,
        "Puzzle" to Icons.Rounded.Extension,
        "QrCode" to Icons.Rounded.QrCodeScanner,
        "Radio" to Icons.Rounded.Radio,
        "Rocket" to Icons.Rounded.RocketLaunch,
        "Rss" to Icons.Rounded.RssFeed,
        "Ruler" to Icons.Rounded.Straighten,
        "ScanLine" to Icons.Rounded.DocumentScanner,
        "Search" to Icons.Rounded.Search,
        "Settings" to Icons.Rounded.Settings,
        "ShoppingBag" to Icons.Rounded.ShoppingBag,
        "Sliders" to Icons.Rounded.Tune,
        "Sparkles" to Icons.Rounded.AutoAwesome,
        "Sprout" to Icons.Rounded.Eco,
        "Sun" to Icons.Rounded.WbSunny,
        "Terminal" to Icons.Rounded.Terminal,
        "Timer" to Icons.Rounded.Timer,
        "Train" to Icons.Rounded.Train,
        "Users" to Icons.Rounded.Group,
        "Utensils" to Icons.Rounded.Restaurant,
        "Vibrate" to Icons.Rounded.Vibration,
        "Video" to Icons.Rounded.Videocam,
        "Wallet" to Icons.Rounded.Wallet,
        "Wifi" to Icons.Rounded.Wifi,
        "Wind" to Icons.Rounded.CompassCalibration,
    )

    val supportedKeys: Set<String> get() = VECTOR_MAP.keys

    fun resolveIcon(iconKey: String): ImageVector = VECTOR_MAP[iconKey] ?: Icons.Rounded.Explore

    fun effectiveBadge(
        appId: String?,
        badge: Int?,
        clearedBadgeAppIds: Set<String>,
    ): Int? {
        if (badge == null || badge <= 0) return null
        if (appId != null && appId in clearedBadgeAppIds) return null
        return badge
    }

    fun resolveSquircleColors(
        brandColor: Color,
        style: IconStyleConfig,
    ): ResolvedSquircleColors {
        if (!style.tonalIcons) {
            return ResolvedSquircleColors(
                backgroundBrush = Brush.linearGradient(
                    colors = listOf(brandColor, brandColor.copy(alpha = 0.87f))
                ),
                glyphColor = Color.White,
                borderColor = Color.White.copy(alpha = 0.25f),
                borderWidthDp = 0.75f,
            )
        }

        return when (style.themeId) {
            ThemeVariantId.GLASS -> ResolvedSquircleColors(
                backgroundBrush = Brush.linearGradient(
                    colors = listOf(Color(0x33FFFFFF), Color(0x24FFFFFF))
                ),
                glyphColor = Color.White,
                borderColor = Color(0x57FFFFFF),
                borderWidthDp = 1f,
            )
            ThemeVariantId.DARK_BOLD -> {
                val isLight = style.colorMode == ColorMode.LIGHT
                val bg = if (isLight) CanopyThemes.hslColor(222f, 48f, 85f) else CanopyThemes.hslColor(222f, 42f, 24f)
                ResolvedSquircleColors(
                    backgroundBrush = Brush.linearGradient(colors = listOf(bg, bg)),
                    glyphColor = if (isLight) CanopyThemes.hslColor(222f, 76f, 32f) else CanopyThemes.hslColor(222f, 92f, 82f),
                    borderColor = if (isLight) CanopyThemes.hslColor(222f, 50f, 45f, 0.16f) else CanopyThemes.hslColor(222f, 80f, 70f, 0.24f),
                    borderWidthDp = 1f,
                )
            }
            ThemeVariantId.MATERIAL_YOU -> {
                val isDark = style.colorMode == ColorMode.DARK
                val bg = if (isDark) CanopyThemes.hslColor(34f, 26f, 25f) else CanopyThemes.hslColor(34f, 44f, 84f)
                ResolvedSquircleColors(
                    backgroundBrush = Brush.linearGradient(colors = listOf(bg, bg)),
                    glyphColor = if (isDark) CanopyThemes.hslColor(34f, 65f, 82f) else CanopyThemes.hslColor(34f, 55f, 26f),
                    borderColor = if (isDark) CanopyThemes.hslColor(34f, 45f, 75f, 0.16f) else CanopyThemes.hslColor(34f, 40f, 45f, 0.14f),
                    borderWidthDp = 1f,
                )
            }
        }
    }
}

/**
 * Unified Squircle App Icon with vector glyph (no letter placeholders),
 * tonal or vibrant coloring, live unread badge suppression, and new-install glow dot.
 */
@Composable
fun AppSquircleIcon(
    appId: String?,
    iconKey: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    badge: Int? = null,
    isNew: Boolean = false,
    onClick: ((androidx.compose.ui.geometry.Rect?) -> Unit)? = null,
    /** Accessible name (the app label). Falls back to the icon key. */
    contentLabel: String? = null,
) {
    val iconStyle = LocalIconStyle.current
    val resolved = AppIconRegistry.resolveSquircleColors(color, iconStyle)
    val effectiveBadge = AppIconRegistry.effectiveBadge(appId, badge, iconStyle.clearedBadgeAppIds)
    val cornerRadius = size * 0.29f
    val glyphSize = size * 0.48f
    var lastBounds: androidx.compose.ui.geometry.Rect? = null

    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .onGloballyPositioned { coords ->
                lastBounds = coords.boundsInRoot()
            }
            .let { base ->
                if (onClick != null) {
                    base.clickable { onClick(lastBounds) }
                } else {
                    base
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation = 3.dp, shape = shape, clip = false)
                .clip(shape)
                .background(resolved.backgroundBrush)
                .let { m ->
                    if (resolved.borderColor != null) {
                        m.border(resolved.borderWidthDp.dp, resolved.borderColor, shape)
                    } else {
                        m
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val context = LocalContext.current
            val densityPx = with(LocalDensity.current) { size.roundToPx() }
            val sizePx = AdaptiveIconGeometry.bucket(densityPx)
            val iconVersion by AppIconRepository.version.collectAsState()
            val loaded by produceState(AppIconRepository.cached(appId, sizePx), appId, sizePx, iconVersion) {
                value = AppIconRepository.load(context, appId, sizePx)
            }
            val icon = loaded
            val label = contentLabel ?: iconKey
            when {
                // Themed icon: the app's own monochrome layer on Canopy's tonal surface.
                icon != null && iconStyle.tonalIcons && icon.monochrome != null -> Image(
                    bitmap = icon.monochrome,
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(resolved.glyphColor),
                    modifier = Modifier.fillMaxSize(),
                )
                // Adaptive icon: full-bleed layers, the squircle clip is the only mask.
                icon != null && icon.isAdaptive -> Image(
                    bitmap = icon.full,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Legacy (non-adaptive) icon: drawn as-is with breathing room.
                icon != null -> Image(
                    bitmap = icon.full,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size * 0.78f),
                )
                else -> Icon(
                    imageVector = AppIconRegistry.resolveIcon(iconKey),
                    contentDescription = label,
                    tint = resolved.glyphColor,
                    modifier = Modifier.size(glyphSize),
                )
            }
        }

        if (AppIdentity.userSerialOf(appId) != 0L) {
            // Work / private-space apps get a small profile badge, like the system launcher.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size((size.value * 0.36f).coerceIn(14f, 20f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D4ED8))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Work, contentDescription = "Arbeitsprofil", tint = Color.White, modifier = Modifier.fillMaxSize(0.62f))
            }
        }

        if (effectiveBadge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = effectiveBadge.toString(),
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }

        if (isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34D399))
                    .border(1.5.dp, Color(0xB30F172A), CircleShape)
            )
        }
    }
}
