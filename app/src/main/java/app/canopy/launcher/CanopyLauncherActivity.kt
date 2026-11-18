package app.canopy.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.canopy.launcher.system.LayoutBackupEngine
import app.canopy.launcher.ui.CanopyLauncherApp
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Main Launcher activity hosting Canopy's Jetpack Compose tiling shell.
 */
class CanopyLauncherActivity : ComponentActivity() {
    private val homeIntentCounter = MutableStateFlow(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val homeSignal by homeIntentCounter.collectAsState()
            CanopyLauncherApp(homeIntentSignal = homeSignal)
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
