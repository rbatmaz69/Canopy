package app.canopy.launcher.data

import app.canopy.launcher.layout.ReminderItem
import app.canopy.launcher.ui.tiles.CalendarEvent
import app.canopy.launcher.ui.tiles.MediaTrack

data class TempPoint(val hour: String, val tempC: Float)
data class WeatherHour(val hour: String, val temp: String, val icon: String)
data class PhotoScene(
    val title: String,
    val meta: String,
    val c1Hex: Long,
    val c2Hex: Long,
    val c3Hex: Long,
    val sunHex: Long,
)

object SystemTileDefaults {
    val TEMP_DAY_SERIES = listOf(
        TempPoint("06h", 4f),
        TempPoint("09h", 7f),
        TempPoint("12h", 10f),
        TempPoint("15h", 11f),
        TempPoint("18h", 8f),
        TempPoint("21h", 6f),
    )

    val WEATHER_HOURS = listOf(
        WeatherHour("10h", "8°", "Cloud"),
        WeatherHour("13h", "11°", "CloudSun"),
        WeatherHour("16h", "9°", "Cloud"),
        WeatherHour("19h", "6°", "CloudRain"),
    )

    val MEDIA_TRACKS = listOf(
        MediaTrack("t1", "Lo-fi Mix", "Nachtschicht · Kosmos", "Mitternachtsfrequenz", 214, 275f),
        MediaTrack("t2", "Regen über Berlin", "Klangwerk Süd", "Beton & Moos", 186, 198f),
        MediaTrack("t3", "Analog Drift", "Studio 84", "Tape Loops Vol. 2", 242, 22f),
    )

    val CALENDAR_EVENTS = listOf(
        CalendarEvent("ev1", "Game Engines", "10:00", "11:30", "Raum B2", 10f, 1.5f, 220f),
        CalendarEvent("ev2", "Design Kritik", "12:30", "13:30", "Atelier Nord", 12.5f, 1f, 335f),
        CalendarEvent("ev3", "Physik Simulation", "15:00", "16:15", "Labor 4", 15f, 1.25f, 162f),
    )

    val PHOTO_SCENES = listOf(
        PhotoScene("Erinnerungen", "Herbst im Studio · 14 Fotos", 0xFFF97316, 0xFFEC4899, 0xFF4F46E5, 0xFFFDE047),
        PhotoScene("Nordlicht am Fjord", "Norwegen Trip · 9 Fotos", 0xFF065F46, 0xFF10B981, 0xFF0F172A, 0xFFA7F3D0),
        PhotoScene("Abendrot Berlin", "Spaziergang Mitte · 22 Fotos", 0xFFFB7185, 0xFFF59E0B, 0xFF1E1B4B, 0xFFFDBA74),
    )

    val DEFAULT_REMINDERS = listOf(
        ReminderItem("r1", "Prototyp testen", true, "09:30"),
        ReminderItem("r2", "Paket abholen", false, "14:00"),
        ReminderItem("r3", "Kaffeebohnen kaufen", false, "18:00"),
    )

    val QUICK_REMINDER_IDEAS = listOf(
        "Design-Review vorbereiten",
        "Backup prüfen",
        "Ladekabel einpacken",
        "Flugmodus fürs Meeting",
        "Wasser trinken",
    )

    val DEFAULT_NOTE_LINES = listOf(
        "Federwerte 0,75 / 400 prüfen",
        "Fugen 6 dp → 12 dp",
        "Haptik-Tick testen",
    )
}
