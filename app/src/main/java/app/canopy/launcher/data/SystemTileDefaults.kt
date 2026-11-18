package app.canopy.launcher.data

import app.canopy.launcher.layout.ReminderItem
import kotlinx.serialization.Serializable

@Serializable
data class TempPoint(val hour: String, val tempC: Float)

@Serializable
data class WeatherHour(val hour: String, val temp: String, val icon: String)

object SystemTileDefaults {
    /** New reminder and note tiles start empty — the user fills them in. */
    val DEFAULT_REMINDERS: List<ReminderItem> = emptyList()

    val DEFAULT_NOTE_LINES: List<String> = emptyList()
}
