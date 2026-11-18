package app.canopy.launcher.system

import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.layout.WidgetKind
import app.canopy.launcher.ui.tiles.CalendarEvent
import app.canopy.launcher.ui.tiles.ContactItem
import app.canopy.launcher.ui.tiles.MediaTrack
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class LiveMediaSessionSnapshot(
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val positionMs: Long,
    val isPlaying: Boolean,
    /** SystemClock.elapsedRealtime() at which [positionMs] was reported. */
    val positionUpdatedAtElapsedMs: Long = 0L,
    val playbackSpeed: Float = 1f,
) {
    /** Playback progress (0..1) extrapolated to [nowElapsedMs] while playing. */
    fun progressAt(nowElapsedMs: Long): Float {
        val duration = max(1000L, durationMs)
        val advanced = if (isPlaying && positionUpdatedAtElapsedMs > 0L) {
            ((nowElapsedMs - positionUpdatedAtElapsedMs).coerceAtLeast(0L) * playbackSpeed).toLong()
        } else 0L
        return ((positionMs + advanced).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
}

data class NativeWidgetProviderSpec(
    val providerPackage: String,
    val providerClass: String,
    val label: String,
    val minWidthDp: Float,
    val minHeightDp: Float,
)

/**
 * Pure Kotlin transformation & fallback engine bridging Android MediaSession,
 * CalendarContract, ContactsContract, and AppWidgetProviderInfo into Canopy tiles.
 */
object SystemDataAndWidgetEngine {
    fun mapMediaSnapshotToTrack(snapshot: LiveMediaSessionSnapshot): Pair<MediaTrack, Float> {
        val durationMsSafe = max(1000L, snapshot.durationMs)
        val durationSec = (durationMsSafe / 1000L).toInt().coerceAtLeast(1)
        val progress = (snapshot.positionMs.toFloat() / durationMsSafe.toFloat()).coerceIn(0f, 1f)
        val hue = InstalledAppsMapper.stableHueForPackage(snapshot.packageName)
        return MediaTrack(
            id = "live-${snapshot.packageName}",
            title = snapshot.title.ifBlank { "Unbekannter Titel" },
            artist = snapshot.artist.ifBlank { snapshot.packageName },
            album = snapshot.album.ifBlank { "Live-Wiedergabe" },
            durationSec = durationSec,
            accentHue = hue,
        ) to progress
    }

    fun mergeCalendarEvents(
        deviceEvents: List<CalendarEvent>,
        fallbackEvents: List<CalendarEvent> = emptyList(),
    ): List<CalendarEvent> {
        if (deviceEvents.isEmpty()) return fallbackEvents
        return deviceEvents.sortedBy { it.startHour }
    }

    fun mergeContacts(
        deviceContacts: List<ContactItem>,
        fallbackContacts: List<ContactItem> = emptyList(),
    ): List<ContactItem> {
        if (deviceContacts.isEmpty()) return fallbackContacts
        val seenNames = mutableSetOf<String>()
        return (deviceContacts + fallbackContacts).filter { item ->
            val key = item.name.trim().lowercase(Locale.ROOT)
            key.isNotEmpty() && seenNames.add(key)
        }
    }

    fun buildWidgetTileFromProvider(
        spec: NativeWidgetProviderSpec,
        appWidgetId: Int,
        matchedAppId: String? = null,
    ): TileData = TileData(
        id = TilingEngine.createNodeId("widget"),
        type = TileType.THIRD_PARTY_WIDGET,
        title = spec.label.ifBlank { spec.providerPackage },
        subtitle = spec.providerPackage,
        accentHue = InstalledAppsMapper.stableHueForPackage(spec.providerPackage),
        appId = matchedAppId ?: "pkg:${spec.providerPackage}",
        widgetKind = WidgetKind.PODCAST,
        widgetFixedWidth = spec.minWidthDp.coerceIn(110f, 320f),
        widgetFixedHeight = spec.minHeightDp.coerceIn(90f, 260f),
        widgetMutedColor = "#1E293B",
        androidAppWidgetId = appWidgetId,
    )
}

/**
 * Android runtime provider loading installed AppWidgets, active MediaSessions,
 * calendar events, and contacts safely with automatic demo fallback.
 */
class SystemDataProvider(private val context: Context) {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    companion object {
        const val MAX_CONTACTS = 2000
    }

    fun loadInstalledWidgetProviders(): List<NativeWidgetProviderSpec> {
        val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return emptyList()
        val pm = context.packageManager
        val list = runCatching { manager.installedProviders }.getOrDefault(emptyList())
        return list.mapNotNull { info: AppWidgetProviderInfo ->
            val provider = info.provider ?: return@mapNotNull null
            val label = runCatching { info.loadLabel(pm) }.getOrNull()?.toString()?.trim().orEmpty()
                .ifEmpty { provider.shortClassName }
            val minW = info.minWidth.takeIf { it > 0 }?.toFloat() ?: 150f
            val minH = info.minHeight.takeIf { it > 0 }?.toFloat() ?: 120f
            NativeWidgetProviderSpec(
                providerPackage = provider.packageName,
                providerClass = provider.className,
                label = label,
                minWidthDp = minW,
                minHeightDp = minH,
            )
        }.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    fun queryActiveMediaController(): MediaController? {
        val msm = runCatching {
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        }.getOrNull() ?: return null
        val component = ComponentName(context, CanopyNotificationListenerService::class.java)
        val controllers = runCatching { msm.getActiveSessions(component) }.getOrDefault(emptyList())
        return controllers.firstOrNull { ctrl ->
            ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull()
    }

    fun readActiveMediaSnapshot(): LiveMediaSessionSnapshot? {
        val ctrl = queryActiveMediaController() ?: return null
        return snapshotOf(ctrl)
    }

    /** Builds a snapshot from a specific controller (used by the callback-driven media monitor). */
    fun snapshotOf(ctrl: MediaController): LiveMediaSessionSnapshot? {
        val meta = ctrl.metadata ?: return null
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (title.isBlank()) return null
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val album = meta.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).takeIf { it > 0L } ?: 180_000L
        val playback = ctrl.playbackState
        val positionMs = playback?.position?.takeIf { it >= 0L } ?: 0L
        val isPlaying = playback?.state == PlaybackState.STATE_PLAYING
        return LiveMediaSessionSnapshot(
            packageName = ctrl.packageName,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            positionMs = positionMs,
            isPlaying = isPlaying,
            positionUpdatedAtElapsedMs = playback?.lastPositionUpdateTime ?: 0L,
            playbackSpeed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
        )
    }

    fun loadTodayCalendarEvents(): List<CalendarEvent> {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyList()

        return runCatching {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val startMs = cal.timeInMillis
            val endMs = startMs + 24L * 3600L * 1000L
            val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(uriBuilder, startMs)
            ContentUris.appendId(uriBuilder, endMs)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_LOCATION,
            )
            val events = mutableListOf<CalendarEvent>()
            context.contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                val colId = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val colTitle = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val colBegin = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val colEnd = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val colLoc = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
                val c = Calendar.getInstance()
                while (cursor.moveToNext() && events.size < 8) {
                    val begin = cursor.getLong(colBegin)
                    val end = max(begin + 15L * 60L * 1000L, cursor.getLong(colEnd))
                    c.timeInMillis = begin
                    val startHour = c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60f
                    val durHours = ((end - begin) / 3_600_000f).coerceIn(0.25f, 8f)
                    val idStr = cursor.getLong(colId).toString()
                    val title = cursor.getString(colTitle).orEmpty().ifBlank { "Termin" }
                    val loc = cursor.getString(colLoc).orEmpty().ifBlank { "Kalender" }
                    events += CalendarEvent(
                        id = "cal-$idStr-$begin",
                        title = title,
                        time = timeFormat.format(Date(begin)),
                        endTime = timeFormat.format(Date(end)),
                        location = loc,
                        startHour = startHour,
                        durationHours = durHours,
                        hue = InstalledAppsMapper.stableHueForPackage(title),
                    )
                }
            }
            events
        }.getOrDefault(emptyList())
    }

    fun loadDeviceContacts(): List<ContactItem> {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyList()

        return runCatching {
            val contacts = mutableListOf<ContactItem>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC",
            )?.use { cursor ->
                val colId = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val colName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val colNum = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = HashSet<Long>()
                while (cursor.moveToNext() && contacts.size < MAX_CONTACTS) {
                    val name = cursor.getString(colName).orEmpty().trim()
                    if (name.isBlank()) continue
                    val number = cursor.getString(colNum).orEmpty().trim()
                    val cid = cursor.getLong(colId)
                    if (!seen.add(cid)) continue
                    contacts += ContactItem(
                        id = "contact-os-$cid",
                        name = name,
                        role = "Gerätekontakt",
                        status = number.ifEmpty { "Im Adressbuch" },
                        hue = InstalledAppsMapper.stableHueForPackage(name),
                    )
                }
            }
            contacts
        }.getOrDefault(emptyList())
    }
}
