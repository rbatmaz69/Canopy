package app.canopy.launcher.system

import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import app.canopy.launcher.data.LauncherPersistenceCodec
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.layout.PageKind

/**
 * Pure Kotlin helper for recognizing native Android HOME intents and
 * validating JSON layout backups.
 */
object LayoutBackupEngine {
    fun isHomeIntent(action: String?, categories: Set<String>?): Boolean =
        action == Intent.ACTION_MAIN && categories?.contains(Intent.CATEGORY_HOME) == true

    fun exportBackupJson(state: PersistedLauncherState): String =
        LauncherPersistenceCodec.encode(state)

    fun importBackupJson(rawJson: String?): Result<PersistedLauncherState> {
        if (rawJson.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Zwischenablage enthält kein Canopy-Layout-JSON."))
        }
        val parsed = LauncherPersistenceCodec.decode(rawJson)
            ?: return Result.failure(IllegalArgumentException("Ungültiges Canopy-Layout-JSON."))
        if (parsed.pages.none { it.kind == PageKind.TILES }) {
            return Result.failure(IllegalArgumentException("Layout enthält keine gültige Kachel-Seite."))
        }
        return Result.success(parsed)
    }
}

/**
 * Runtime controller handling Android default launcher selection (RoleManager / HOME settings)
 * and JSON backup export/import via Clipboard and Share Sheet.
 */
class HomeRoleAndBackupController(private val context: Context) {
    fun buildDefaultLauncherRoleIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = runCatching { context.getSystemService(RoleManager::class.java) }.getOrNull()
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                return runCatching { rm.createRequestRoleIntent(RoleManager.ROLE_HOME) }.getOrNull()
            }
        }
        return null
    }

    fun openHomeSettingsFallback(): Boolean {
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_HOME_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
            true
        }.getOrElse {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
                true
            }.getOrDefault(false)
        }
    }

    fun exportToClipboardAndShare(state: PersistedLauncherState): String {
        val json = LayoutBackupEngine.exportBackupJson(state)
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("Canopy Layout JSON", json))
        }
        runCatching {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Canopy Launcher Layout Backup")
                putExtra(Intent.EXTRA_TEXT, json)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Canopy Layout exportieren").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
        return json
    }

    fun importFromClipboard(): Result<PersistedLauncherState> {
        val text = runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).coerceToText(context)?.toString()
            } else {
                null
            }
        }.getOrNull()
        return LayoutBackupEngine.importBackupJson(text)
    }
}
