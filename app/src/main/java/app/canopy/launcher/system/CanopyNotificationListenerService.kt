package app.canopy.launcher.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
    val INITIAL_NOTIFICATIONS: List<LauncherNotification> = listOf(
        LauncherNotification(
            id = "notif-msg",
            appId = "app-nachrichten",
            appName = "Nachrichten",
            title = "Mara Weber",
            body = "Perfekt! Bis 12:30 beim Design Kritik Termin.",
            time = "vor 3 Min.",
            hue = 217f,
        ),
        LauncherNotification(
            id = "notif-mail",
            appId = "app-mail",
            appName = "Postfach",
            title = "Release-Freigabe #408",
            body = "Build erfolgreich auf Testgerät verteilt.",
            time = "vor 21 Min.",
            hue = 0f,
        ),
        LauncherNotification(
            id = "notif-cal",
            appId = "app-kalender",
            appName = "Kalender",
            title = "Game Engines in 59 Min.",
            body = "10:00 – 11:30 · Raum B2",
            time = "Jetzt",
            hue = 262f,
        ),
    )

    private val _state = MutableStateFlow(NotificationSyncState())
    val state: StateFlow<NotificationSyncState> = _state.asStateFlow()

    fun dismissNotification(notif: LauncherNotification) {
        _state.update { cur -> reduceDismissNotification(cur, notif) }
    }

    fun dismissAll() {
        _state.update { cur -> reduceDismissAll(cur) }
    }

    fun onAppOpened(appId: String) {
        _state.update { cur -> reduceAppOpened(cur, appId) }
    }

    fun restoreDemoNotifications() {
        _state.value = NotificationSyncState(
            notifications = INITIAL_NOTIFICATIONS,
            clearedBadgeAppIds = emptySet(),
        )
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
            SystemToggleKey.HOTSPOT -> state.copy(hotspot = !state.hotspot)
            SystemToggleKey.AUTO_ROTATE -> state.copy(autoRotate = !state.autoRotate)
        }
}

/**
 * Android NotificationListenerService bridging live system notifications into Canopy's badge store.
 */
class CanopyNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val body = extras.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && body.isBlank()) return

        NotificationBadgeStore.postSystemNotification(
            LauncherNotification(
                id = "sys-${sbn.key}",
                appId = sbn.packageName,
                appName = sbn.packageName.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() },
                title = title.ifBlank { sbn.packageName },
                body = body,
                time = "Jetzt",
                hue = 215f,
                systemKey = sbn.key,
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.key?.let { NotificationBadgeStore.removeBySystemKey(it) }
    }
}
