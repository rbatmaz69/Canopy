package app.canopy.launcher.system

import android.app.Notification
import android.text.format.DateFormat
import java.util.Date

import android.app.PendingIntent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.canopy.launcher.ui.tiles.AppDefinition
import app.canopy.launcher.ui.tiles.SystemControlState
import app.canopy.launcher.ui.tiles.SystemToggleKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LauncherNotification(
    val id: String,
    val appId: String,
    val appName: String,
    val title: String,
    val body: String,
    val time: String,
    val hue: Float,
    val systemKey: String? = null,
    val contentIntent: PendingIntent? = null,
)

data class NotificationSyncState(
    val notifications: List<LauncherNotification> = NotificationBadgeStore.INITIAL_NOTIFICATIONS,
    val clearedBadgeAppIds: Set<String> = emptySet(),
)

/**
 * Shared live repository synchronizing active notifications, unread app badges,
 * and bidirectional system quick-settings toggles across the entire launcher.
 */
object NotificationBadgeStore {
    val INITIAL_NOTIFICATIONS: List<LauncherNotification> = emptyList()

    fun isNotificationAccessEnabled(context: android.content.Context): Boolean {
        return runCatching {
            androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }.getOrDefault(false)
    }

    private val _state = MutableStateFlow(NotificationSyncState())
    val state: StateFlow<NotificationSyncState> = _state.asStateFlow()
    private var activeServiceCancelCallback: ((String) -> Unit)? = null

    fun registerServiceCancelCallback(callback: ((String) -> Unit)?) {
        activeServiceCancelCallback = callback
    }

    fun dismissNotification(notif: LauncherNotification) {
        notif.systemKey?.let { key ->
            runCatching { activeServiceCancelCallback?.invoke(key) }
        }
        _state.update { cur -> reduceDismissNotification(cur, notif) }
    }

    fun dismissAll() {
        val currentKeys = _state.value.notifications.mapNotNull { it.systemKey }
        currentKeys.forEach { key ->
            runCatching { activeServiceCancelCallback?.invoke(key) }
        }
        _state.update { cur -> reduceDismissAll(cur) }
    }

    fun triggerNotificationIntent(notif: LauncherNotification): Boolean {
        val pi = notif.contentIntent ?: return false
        return runCatching {
            pi.send()
            true
        }.getOrDefault(false)
    }

    fun resolveCanonicalAppId(
        packageName: String,
        catalog: List<AppDefinition> = emptyList(),
    ): String {
        val clean = packageName.trim()
        if (clean.isEmpty()) return "app-nachrichten"
        catalog.firstOrNull { it.packageName == clean }?.let { return it.id }
        val lower = clean.lowercase()
        when {
            listOf("messaging", "sms", "mms", "whatsapp", "signal", "telegram", "chat").any { lower.contains(it) } -> return "app-nachrichten"
            listOf(".gm", "gmail", "mail", "outlook", "thunderbird").any { lower.contains(it) } -> return "app-mail"
            listOf("calendar", "kalender").any { lower.contains(it) } -> return "app-kalender"
            listOf("dialer", "phone", "telecom").any { lower.contains(it) } -> return "app-telefon"
            listOf("music", "spotify", "soundcloud").any { lower.contains(it) } -> return "app-musik"
            listOf("podcast", "pocketcasts").any { lower.contains(it) } -> return "app-podcast"
        }
        return when (InstalledAppsMapper.inferIconKey(clean, clean)) {
            "MessageCircle" -> "app-nachrichten"
            "Mail" -> "app-mail"
            "Calendar" -> "app-kalender"
            "Phone" -> "app-telefon"
            "Music2" -> "app-musik"
            "Headphones" -> "app-podcast"
            else -> "pkg:$clean"
        }
    }

    fun settingsIntentForToggle(key: SystemToggleKey): String = when (key) {
        SystemToggleKey.WIFI -> Settings.ACTION_WIFI_SETTINGS
        SystemToggleKey.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
        SystemToggleKey.FLASHLIGHT -> Settings.ACTION_DISPLAY_SETTINGS
        SystemToggleKey.DND -> "android.settings.ZEN_MODE_SETTINGS"
        SystemToggleKey.AUTO_ROTATE -> Settings.ACTION_DISPLAY_SETTINGS
        SystemToggleKey.VOLUME -> Settings.ACTION_SOUND_SETTINGS
    }

    fun clampWindowBrightness(brightness: Float): Float = brightness.coerceIn(0.25f, 1.0f)

    fun onAppOpened(appId: String) {
        _state.update { cur -> reduceAppOpened(cur, appId) }
    }

    /** Drops every notification mirrored from the system (used when the listener disconnects). */
    fun clearSystemNotifications() {
        _state.value = NotificationSyncState(notifications = emptyList(), clearedBadgeAppIds = emptySet())
    }

    fun postSystemNotification(notif: LauncherNotification) {
        _state.update { cur ->
            val filtered = cur.notifications.filterNot { it.id == notif.id }
            cur.copy(
                notifications = listOf(notif) + filtered,
                clearedBadgeAppIds = cur.clearedBadgeAppIds - notif.appId,
            )
        }
    }

    fun removeBySystemKey(systemKey: String) {
        _state.update { cur ->
            val matching = cur.notifications.filter { it.systemKey == systemKey }
            if (matching.isEmpty()) return@update cur
            cur.copy(
                notifications = cur.notifications.filterNot { it.systemKey == systemKey },
                clearedBadgeAppIds = cur.clearedBadgeAppIds + matching.map { it.appId },
            )
        }
    }

    fun reduceDismissNotification(
        current: NotificationSyncState,
        notif: LauncherNotification,
    ): NotificationSyncState = current.copy(
        notifications = current.notifications.filterNot { it.id == notif.id },
        clearedBadgeAppIds = current.clearedBadgeAppIds + notif.appId,
    )

    fun reduceDismissAll(current: NotificationSyncState): NotificationSyncState = current.copy(
        notifications = emptyList(),
        clearedBadgeAppIds = current.clearedBadgeAppIds + current.notifications.map { it.appId },
    )

    fun reduceAppOpened(
        current: NotificationSyncState,
        appId: String,
    ): NotificationSyncState = current.copy(
        notifications = current.notifications.filterNot { it.appId == appId },
        clearedBadgeAppIds = current.clearedBadgeAppIds + appId,
    )

    fun toggleSystemKey(state: SystemControlState, key: SystemToggleKey): SystemControlState =
        when (key) {
            SystemToggleKey.WIFI -> state.copy(wifi = !state.wifi)
            SystemToggleKey.BLUETOOTH -> state.copy(bluetooth = !state.bluetooth)
            SystemToggleKey.FLASHLIGHT -> state.copy(flashlight = !state.flashlight)
            SystemToggleKey.DND -> state.copy(dnd = !state.dnd)
            SystemToggleKey.AUTO_ROTATE -> state.copy(autoRotate = !state.autoRotate)
            SystemToggleKey.VOLUME -> state
        }
}

/**
 * Android NotificationListenerService bridging live system notifications into Canopy's badge store.
 */
class CanopyNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationBadgeStore.registerServiceCancelCallback { key ->
            runCatching { cancelNotification(key) }
        }
        // Mirror notifications that already exist, oldest first so the newest ends up on top.
        runCatching { activeNotifications?.sortedBy { it.postTime } }
            .getOrNull()
            ?.forEach { mirror(it) }
    }

    override fun onListenerDisconnected() {
        NotificationBadgeStore.registerServiceCancelCallback(null)
        NotificationBadgeStore.clearSystemNotifications()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn != null) mirror(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.key?.let { NotificationBadgeStore.removeBySystemKey(it) }
    }

    private fun mirror(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.isOngoing) return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && body.isBlank()) return

        val appLabel = runCatching {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrNull() ?: sbn.packageName.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() }

        NotificationBadgeStore.postSystemNotification(
            LauncherNotification(
                id = "sys-${sbn.key}",
                appId = "pkg:${sbn.packageName}",
                appName = appLabel,
                title = title.ifBlank { appLabel },
                body = body,
                time = DateFormat.getTimeFormat(this).format(Date(sbn.postTime)),
                hue = InstalledAppsMapper.stableHueForPackage(sbn.packageName),
                systemKey = sbn.key,
                contentIntent = notification.contentIntent,
            )
        )
    }
}
