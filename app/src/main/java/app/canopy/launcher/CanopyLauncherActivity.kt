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
    private val launcherViewModel: LauncherViewModel by viewModels()
    private val services get() = (application as CanopyApplication).services

    override fun onStart() {
        super.onStart()
        services.onLauncherStarted()
    }

    override fun onStop() {
        // Persist immediately when leaving home and release sensors, callbacks and widget updates.
        launcherViewModel.flush()
        services.onLauncherStopped()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val homeSignal by homeIntentCounter.collectAsState()
            CanopyLauncherApp(
                homeIntentSignal = homeSignal,
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
            homeIntentCounter.value += 1L
        }
    }
}
