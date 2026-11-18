package app.canopy.launcher.system

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import app.canopy.launcher.data.FileLauncherStore
import app.canopy.launcher.data.LauncherStateStore
import app.canopy.launcher.ui.tiles.CalendarEvent
import app.canopy.launcher.ui.tiles.ContactItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val CANOPY_APP_WIDGET_HOST_ID = 0xCA90

/**
 * Process-wide launcher services. They live in [app.canopy.launcher.CanopyApplication] so they
 * survive activity recreation (rotation, dark-mode switch) and are started/stopped with the
 * launcher's visibility instead of running for as long as the activity exists.
 */
class LauncherServices(context: Context) {
    private val appContext = context.applicationContext

    /** Background scope for IO work and saves that must outlive a single screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val store: LauncherStateStore = FileLauncherStore.create(appContext)
    val deviceApps = DeviceAppsController(appContext, appScope).also { it.startListening() }
    val hardware = LiveHardwareController(appContext)
    val weather = LiveWeatherController(appContext.getSharedPreferences("canopy_weather", Context.MODE_PRIVATE))
    val systemData = SystemDataProvider(appContext)
    val content = SystemContentRepository(appContext, systemData, appScope)
    val media = MediaSessionMonitor(appContext, systemData)

    val appWidgetHost: AppWidgetHost? = runCatching { AppWidgetHost(appContext, CANOPY_APP_WIDGET_HOST_ID) }.getOrNull()
    val appWidgetManager: AppWidgetManager? = runCatching { AppWidgetManager.getInstance(appContext) }.getOrNull()

    private val _widgetProviders = MutableStateFlow<List<NativeWidgetProviderSpec>>(emptyList())
    val widgetProviders: StateFlow<List<NativeWidgetProviderSpec>> = _widgetProviders.asStateFlow()

    private var started = false

    /** Called from Activity.onStart: the launcher is visible, live data sources may run. */
    fun onLauncherStarted() {
        if (started) return
        started = true
        hardware.start()
        media.start()
        content.start()
        runCatching { appWidgetHost?.startListening() }
        refreshWidgetProviders()
    }

    /** Called from Activity.onStop: release sensors, receivers, callbacks and widget updates. */
    fun onLauncherStopped() {
        if (!started) return
        started = false
        hardware.stop()
        media.stop()
        content.stop()
        runCatching { appWidgetHost?.stopListening() }
    }

    fun refreshWidgetProviders() {
        appScope.launch(Dispatchers.IO) {
            _widgetProviders.value = systemData.loadInstalledWidgetProviders()
        }
    }
}

/**
 * Today's calendar events and the device contacts, loaded on IO and refreshed through
 * ContentObservers instead of being re-queried on the main thread at every Home press.
 */
class SystemContentRepository(
    private val context: Context,
    private val provider: SystemDataProvider,
    private val scope: CoroutineScope,
) {
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private var calendarGranted = false
    private var contactsGranted = false
    private var calendarDirty = true
    private var contactsDirty = true
    private var calendarDay = -1
    private var calendarJob: Job? = null
    private var contactsJob: Job? = null

    private val handler = Handler(Looper.getMainLooper())
    private val calendarObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            calendarDirty = true
            refreshCalendar()
        }
    }
    private val contactsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            contactsDirty = true
            refreshContacts(debounceMs = 2_000L)
        }
    }

    private var calendarObserving = false
    private var contactsObserving = false

    fun start() = refreshIfNeeded()

    /**
     * Cheap check (also called on every resume): reloads only when a permission was granted
     * or revoked, the day changed, or a ContentObserver reported changes.
     */
    fun refreshIfNeeded() {
        val calNow = granted(Manifest.permission.READ_CALENDAR)
        val conNow = granted(Manifest.permission.READ_CONTACTS)
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        if (calNow != calendarGranted || today != calendarDay) calendarDirty = true
        if (conNow != contactsGranted) contactsDirty = true
        calendarGranted = calNow
        contactsGranted = conNow
        calendarDay = today
        if (calNow && !calendarObserving) {
            calendarObserving = runCatching {
                context.contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, calendarObserver)
                true
            }.getOrDefault(false)
        }
        if (conNow && !contactsObserving) {
            contactsObserving = runCatching {
                context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsObserver)
                true
            }.getOrDefault(false)
        }
        refreshCalendar()
        refreshContacts()
    }

    fun stop() {
        runCatching { context.contentResolver.unregisterContentObserver(calendarObserver) }
        runCatching { context.contentResolver.unregisterContentObserver(contactsObserver) }
        calendarObserving = false
        contactsObserving = false
    }

    private fun refreshCalendar() {
        if (!calendarDirty) return
        calendarDirty = false
        calendarJob?.cancel()
        calendarJob = scope.launch(Dispatchers.IO) {
            _calendarEvents.value = SystemDataAndWidgetEngine.mergeCalendarEvents(provider.loadTodayCalendarEvents())
        }
    }

    private fun refreshContacts(debounceMs: Long = 0L) {
        if (!contactsDirty) return
        contactsDirty = false
        contactsJob?.cancel()
        contactsJob = scope.launch(Dispatchers.IO) {
            if (debounceMs > 0) delay(debounceMs)
            _contacts.value = SystemDataAndWidgetEngine.mergeContacts(provider.loadDeviceContacts())
        }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Tracks the active media session through MediaSessionManager / MediaController callbacks
 * (no polling). Requires notification access; without it [snapshot] simply stays null.
 */
class MediaSessionMonitor(
    private val context: Context,
    private val provider: SystemDataProvider,
) {
    private val _snapshot = MutableStateFlow<LiveMediaSessionSnapshot?>(null)
    val snapshot: StateFlow<LiveMediaSessionSnapshot?> = _snapshot.asStateFlow()

    /** Controller of the session currently shown, used for transport controls. */
    @Volatile
    var activeController: MediaController? = null
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val sessionManager: MediaSessionManager? = runCatching {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }.getOrNull()
    private val listenerComponent = ComponentName(context, CanopyNotificationListenerService::class.java)
    private var listening = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        selectController(controllers.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) = publish()
        override fun onSessionDestroyed() {
            val sessions = runCatching { sessionManager?.getActiveSessions(listenerComponent) }.getOrNull().orEmpty()
            selectController(sessions)
        }
    }

    fun start() {
        val msm = sessionManager ?: return
        if (!listening) {
            listening = runCatching {
                msm.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
                true
            }.getOrDefault(false) // SecurityException without notification access
        }
        val sessions = runCatching { msm.getActiveSessions(listenerComponent) }.getOrNull().orEmpty()
        selectController(sessions)
    }

    fun stop() {
        if (listening) {
            runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
            listening = false
        }
        runCatching { activeController?.unregisterCallback(controllerCallback) }
        activeController = null
    }

    private fun selectController(controllers: List<MediaController>) {
        val next = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
        if (next?.sessionToken != activeController?.sessionToken) {
            runCatching { activeController?.unregisterCallback(controllerCallback) }
            activeController = next
            runCatching { next?.registerCallback(controllerCallback, handler) }
        }
        publish()
    }

    private fun publish() {
        _snapshot.value = activeController?.let { provider.snapshotOf(it) }
    }
}
