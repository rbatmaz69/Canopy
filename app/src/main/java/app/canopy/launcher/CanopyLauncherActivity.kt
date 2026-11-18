package app.canopy.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import app.canopy.launcher.system.LayoutBackupEngine
import app.canopy.launcher.ui.CanopyLauncherApp
import app.canopy.launcher.ui.LauncherViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Main Launcher activity hosting Canopy's Jetpack Compose tiling shell.
 */
class CanopyLauncherActivity : ComponentActivity() {
    private val homeIntentCounter = MutableStateFlow(0L)
    /** Whether the last Home press came back from another app (launcher was not visible). */
    private val homeFromAppFlag = MutableStateFlow(false)
    private var stoppedSinceLastHome = true
    private val launcherViewModel: LauncherViewModel by viewModels()
    private val services get() = (application as CanopyApplication).services

    override fun onResume() {
        super.onResume()
        stoppedSinceLastHome = false
    }

    override fun onStart() {
        super.onStart()
        services.onLauncherStarted()
    }

    override fun onStop() {
        // Persist immediately when leaving home and release sensors, callbacks and widget updates.
        launcherViewModel.flush()
        stoppedSinceLastHome = true
        services.onLauncherStopped()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val homeSignal by homeIntentCounter.collectAsState()
            val homeFromApp by homeFromAppFlag.collectAsState()
            CanopyLauncherApp(
                homeIntentSignal = homeSignal,
                homeFromApp = homeFromApp,
                onSystemBarAppearance = { useLightIcons ->
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !useLightIcons
                    controller.isAppearanceLightNavigationBars = !useLightIcons
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (LayoutBackupEngine.isHomeIntent(intent.action, intent.categories)) {
            homeFromAppFlag.value = stoppedSinceLastHome
            stoppedSinceLastHome = false
            homeIntentCounter.value += 1L
        }
    }
}
