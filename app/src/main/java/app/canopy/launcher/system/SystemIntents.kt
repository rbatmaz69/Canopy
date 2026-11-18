package app.canopy.launcher.system

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.ui.tiles.SystemControlState
import app.canopy.launcher.ui.tiles.SystemToggleKey

/**
 * What tapping the free area of a tile does. Tiles with their own inner controls
 * (apps, groups, widgets, search, toggles) handle taps themselves and map to [NONE].
 */
enum class TileTapTarget {
    NONE,
    ALARMS,
    CALENDAR,
    WEATHER_DETAIL,
    NOTES_EDITOR,
    MEDIA_APP,
    GALLERY,
    BATTERY,
}

object TileTapRouter {
    fun resolve(tile: TileData): TileTapTarget = when (tile.type) {
        TileType.CLOCK -> TileTapTarget.ALARMS
        TileType.CALENDAR, TileType.CALENDAR_TIMELINE -> TileTapTarget.CALENDAR
        TileType.WEATHER, TileType.TEMP_CURVE, TileType.GREETING -> TileTapTarget.WEATHER_DETAIL
        TileType.REMINDERS, TileType.NOTE -> TileTapTarget.NOTES_EDITOR
        TileType.MEDIA -> TileTapTarget.MEDIA_APP
        TileType.PHOTOS -> TileTapTarget.GALLERY
        TileType.BATTERY, TileType.BATTERY_STEPS -> TileTapTarget.BATTERY
        TileType.STEPS,
        TileType.QUICK_TOGGLES,
        TileType.APP_GROUP,
        TileType.SINGLE_APP,
        TileType.THIRD_PARTY_WIDGET,
        TileType.SEARCH_BAR,
        TileType.NEWLY_INSTALLED,
        TileType.ALL_APPS_BOX -> TileTapTarget.NONE
    }
}

/**
 * Starts real system apps and panels. Every function returns false instead of crashing
 * when no app on the device can handle the request.
 */
object SystemIntents {
    private fun Context.launch(intent: Intent): Boolean = runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    fun openAlarms(context: Context): Boolean =
        context.launch(Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openTimer(context: Context): Boolean =
        context.launch(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_SKIP_UI, false)) ||
            openAlarms(context)

    fun openCalendarToday(context: Context): Boolean {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").let {
            ContentUris.appendId(it, System.currentTimeMillis())
        }.build()
        return context.launch(Intent(Intent.ACTION_VIEW).setData(uri)) ||
            context.launch(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))
    }

    fun insertCalendarEvent(context: Context): Boolean =
        context.launch(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)) ||
            openCalendarToday(context)

    fun openGallery(context: Context): Boolean =
        context.launch(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)) ||
            context.launch(Intent(Intent.ACTION_VIEW).setType("image/*"))

    fun openBatteryUsage(context: Context): Boolean =
        context.launch(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) ||
            context.launch(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))

    /** Opens the app behind the active media session, or the default music app. */
    fun openMediaApp(context: Context, sessionPackage: String?): Boolean {
        if (!sessionPackage.isNullOrBlank()) {
            val launch = context.packageManager.getLaunchIntentForPackage(sessionPackage)
            if (launch != null && context.launch(launch)) return true
        }
        return context.launch(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
    }

    fun openCalculator(context: Context): Boolean =
        context.launch(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR))

    fun openCamera(context: Context): Boolean =
        context.launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

    fun openSystemSettings(context: Context): Boolean =
        context.launch(Intent(Settings.ACTION_SETTINGS))

    fun openVolumePanel(context: Context): Boolean =
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context.launch(Intent(Settings.Panel.ACTION_VOLUME))) ||
            context.launch(Intent(Settings.ACTION_SOUND_SETTINGS))

    fun openWebSearch(context: Context, query: String): Boolean =
        context.launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query)) ||
            context.launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))

    /**
     * Opens the closest real system control for a toggle. Modern Android does not let a
     * launcher switch Wi-Fi, Bluetooth or Do-Not-Disturb directly, so we show the system panel.
     */
    fun openSystemPanel(context: Context, key: SystemToggleKey): Boolean = when (key) {
        SystemToggleKey.WIFI ->
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context.launch(Intent(Settings.Panel.ACTION_WIFI))) ||
                context.launch(Intent(Settings.ACTION_WIFI_SETTINGS))
        SystemToggleKey.BLUETOOTH -> context.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        SystemToggleKey.DND ->
            context.launch(Intent("android.settings.ZEN_MODE_SETTINGS")) ||
                context.launch(Intent(Settings.ACTION_SOUND_SETTINGS))
        SystemToggleKey.AUTO_ROTATE -> context.launch(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        SystemToggleKey.VOLUME -> openVolumePanel(context)
        SystemToggleKey.FLASHLIGHT -> false
    }

    /** Reads the real on/off state of the system toggles shown in Canopy. */
    fun readSystemToggleState(context: Context, current: SystemControlState, torchOn: Boolean): SystemControlState {
        val wifiOn = runCatching {
            (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.isWifiEnabled
        }.getOrNull() ?: false
        val btOn = runCatching {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled
        }.getOrNull() ?: false
        val dndOn = runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm != null && nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        }.getOrDefault(false)
        val rotateOn = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        }.getOrDefault(false)
        return current.copy(
            wifi = wifiOn,
            bluetooth = btOn,
            dnd = dndOn,
            autoRotate = rotateOn,
            flashlight = torchOn,
        )
    }
}
