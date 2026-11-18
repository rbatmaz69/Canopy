package app.canopy.launcher

import android.app.Application
import app.canopy.launcher.system.LauncherServices

class CanopyApplication : Application() {
    val services: LauncherServices by lazy { LauncherServices(this) }
}
