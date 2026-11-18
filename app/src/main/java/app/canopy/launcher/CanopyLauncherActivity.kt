package app.canopy.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.canopy.launcher.ui.CanopyLauncherApp

/**
 * Main Launcher activity hosting Canopy's Jetpack Compose tiling shell.
 */
class CanopyLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CanopyLauncherApp()
        }
    }
}
