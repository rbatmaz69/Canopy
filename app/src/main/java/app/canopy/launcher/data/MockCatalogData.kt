package app.canopy.launcher.data

import android.provider.Settings
import androidx.compose.ui.graphics.Color
import app.canopy.launcher.layout.LeafNode
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.SplitDirection
import app.canopy.launcher.layout.SplitNode
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.layout.TogglePreset
import app.canopy.launcher.ui.tiles.AppCategory
import app.canopy.launcher.ui.tiles.AppDefinition
import app.canopy.launcher.ui.tiles.ContactItem
import app.canopy.launcher.ui.tiles.SettingItem

object MockCatalogData {
    val APPS_CATALOG: List<AppDefinition> = listOf(
        // Favoriten & Social
        AppDefinition("app-telefon", "Telefon", "Phone", Color(0xFF10B981), 160f, AppCategory.SOCIAL, null, 98, "Anrufe & Favoriten"),
        AppDefinition("app-nachrichten", "Nachrichten", "MessageCircle", Color(0xFF3B82F6), 217f, AppCategory.SOCIAL, 3, 96, "3 ungelesene Chats"),
        AppDefinition("app-kamera", "Kamera", "Camera", Color(0xFFF59E0B), 38f, AppCategory.MEDIEN, null, 95, "RAW · 50 MP Bereit"),
        AppDefinition("app-browser", "Netz", "Compass", Color(0xFF0EA5E9), 199f, AppCategory.PRODUKTIVITAET, null, 94, "4 geöffnete Tabs"),
        AppDefinition("app-mail", "Postfach", "Mail", Color(0xFFEF4444), 0f, AppCategory.PRODUKTIVITAET, 5, 91, "5 neue E-Mails"),
        AppDefinition("app-musik", "Klang", "Music2", Color(0xFFEC4899), 330f, AppCategory.MEDIEN, null, 93, "Lo-fi Mix läuft"),
        AppDefinition("app-karten", "Karten", "MapPin", Color(0xFF22C55E), 142f, AppCategory.WERKZEUGE, null, 89, "14 Min. zur Arbeit"),
        AppDefinition("app-notizen", "Notizen", "FileText", Color(0xFFEAB308), 48f, AppCategory.PRODUKTIVITAET, null, 88, "12 Entwürfe"),

        // Social
        AppDefinition("app-funk", "Funk", "Radio", Color(0xFF6366F1), 239f, AppCategory.SOCIAL, 2, 84, "2 neue Sprachnotizen"),
        AppDefinition("app-kreis", "Kreis", "Users", Color(0xFF8B5CF6), 258f, AppCategory.SOCIAL, null, 79, "Familien-Gruppe aktiv"),
        AppDefinition("app-videochat", "Videoanruf", "Video", Color(0xFF14B8A6), 173f, AppCategory.SOCIAL, null, 76, "Letzter Call gestern"),
        AppDefinition("app-forum", "Debatte", "MessagesSquare", Color(0xFFF97316), 25f, AppCategory.SOCIAL, 1, 71, "Design-Thread aktiv"),
        AppDefinition("app-teilen", "Schnappschuss", "Aperture", Color(0xFFD946EF), 292f, AppCategory.SOCIAL, null, 68, "Story von Lena"),
        AppDefinition("app-kanäle", "Kanäle", "AtSign", Color(0xFF06B6D4), 189f, AppCategory.SOCIAL, null, 65, "Tech-Feed aktualisiert"),

        // Spiele
        AppDefinition("app-schach", "Schachmatt", "Crown", Color(0xFF7C3AED), 263f, AppCategory.SPIELE, 1, 82, "Du bist am Zug"),
        AppDefinition("app-rallye", "Kurve Rallye", "Gamepad2", Color(0xFFDC2626), 0f, AppCategory.SPIELE, null, 77, "Streckenrekord 1:42"),
        AppDefinition("app-rätsel", "Logikbox", "Puzzle", Color(0xFF2563EB), 221f, AppCategory.SPIELE, null, 72, "Tagesrätsel offen"),
        AppDefinition("app-würfel", "Taktik", "Dices", Color(0xFF059669), 161f, AppCategory.SPIELE, null, 61, "Saison Rang 4"),
        AppDefinition("app-sterne", "Orbit", "Rocket", Color(0xFFDB2777), 333f, AppCategory.SPIELE, null, 59, "Level 28"),

        // Werkzeuge
        AppDefinition("app-rechner", "Rechner", "Calculator", Color(0xFF475569), 215f, AppCategory.WERKZEUGE, null, 86, "Wissenschaftlich"),
        AppDefinition("app-uhr", "Wecker", "AlarmClock", Color(0xFF0284C7), 200f, AppCategory.WERKZEUGE, null, 85, "07:15 täglich"),
        AppDefinition("app-kompass", "Kompass", "Navigation", Color(0xFF0D9488), 175f, AppCategory.WERKZEUGE, null, 54, "24° NNO"),
        AppDefinition("app-scanner", "Dokument-Scan", "ScanLine", Color(0xFF4F46E5), 243f, AppCategory.WERKZEUGE, null, 67, "PDF-Export bereit"),
        AppDefinition("app-diktat", "Diktiergerät", "Mic", Color(0xFFE11D48), 347f, AppCategory.WERKZEUGE, null, 73, "3 Aufnahmen"),
        AppDefinition("app-lampe", "Lichtwerk", "Flashlight", Color(0xFFD97706), 32f, AppCategory.WERKZEUGE, null, 64, "Stufe 3 bereit"),
        AppDefinition("app-übersetzer", "Übersetzer", "Languages", Color(0xFF2563EB), 221f, AppCategory.WERKZEUGE, null, 70, "DE ↔ EN Offline"),
        AppDefinition("app-tresor", "Passwörter", "KeyRound", Color(0xFF0F766E), 176f, AppCategory.WERKZEUGE, null, 81, "94 Einträge gesichert"),
        AppDefinition("app-wasserwaage", "Wasserwaage", "Ruler", Color(0xFF65A30D), 84f, AppCategory.WERKZEUGE, null, 43, "Kalibriert"),
        AppDefinition("app-dateien", "Dateien", "FolderOpen", Color(0xFFEA580C), 21f, AppCategory.WERKZEUGE, null, 83, "42 GB frei"),
        AppDefinition("app-stoppuhr", "Timer", "Timer", Color(0xFF7C3AED), 262f, AppCategory.WERKZEUGE, null, 66, "25:00 Fokus"),
        AppDefinition("app-qr", "QR-Leser", "QrCode", Color(0xFF334155), 215f, AppCategory.WERKZEUGE, null, 52, "Blitzscan aktiv"),
        AppDefinition("app-wetter", "Wetter", "CloudRain", Color(0xFF0369A1), 201f, AppCategory.WERKZEUGE, null, 90, "Regen ab 17 Uhr"),

        // Medien
        AppDefinition("app-podcast", "Podcast", "Headphones", Color(0xFF9333EA), 271f, AppCategory.MEDIEN, 2, 87, "Lage der Nation · 14 Min."),
        AppDefinition("app-galerie", "Galerie", "Image", Color(0xFFF43F5E), 350f, AppCategory.MEDIEN, null, 88, "1.240 Fotos"),
        AppDefinition("app-kino", "Mediathek", "Film", Color(0xFF2563EB), 220f, AppCategory.MEDIEN, null, 74, "2 Merklisten-Filme"),
        AppDefinition("app-radio", "Welle FM", "Disc3", Color(0xFFDB2777), 332f, AppCategory.MEDIEN, null, 62, "Kulturradio Live"),

        // Lesen
        AppDefinition("app-bücher", "Leselicht", "BookOpen", Color(0xFFB45309), 30f, AppCategory.LESEN, null, 80, "Kapitel 7 · 64 %"),
        AppDefinition("app-presse", "Tagesblatt", "Newspaper", Color(0xFF1D4ED8), 224f, AppCategory.LESEN, 4, 78, "Morgenausgabe da"),
        AppDefinition("app-rss", "Blätter", "Rss", Color(0xFFEA580C), 22f, AppCategory.LESEN, null, 69, "18 Artikel ungelesen"),
        AppDefinition("app-magazin", "Essenz", "Bookmark", Color(0xFF0D9488), 175f, AppCategory.LESEN, null, 55, "9 gespeicherte Essays"),

        // Produktivität
        AppDefinition("app-kalender", "Kalender", "Calendar", Color(0xFF2563EB), 221f, AppCategory.PRODUKTIVITAET, null, 92, "Nächster Termin 10:00"),
        AppDefinition("app-aufgaben", "Erinnerung", "CheckSquare", Color(0xFF059669), 161f, AppCategory.PRODUKTIVITAET, 3, 85, "3 offen heute"),
        AppDefinition("app-tabellen", "Zahlen", "BarChart3", Color(0xFF16A34A), 142f, AppCategory.PRODUKTIVITAET, null, 63, "Budget Nov."),
        AppDefinition("app-code", "Terminal", "Terminal", Color(0xFF1E293B), 217f, AppCategory.PRODUKTIVITAET, null, 75, "SSH bereit"),
        AppDefinition("app-skizze", "Vektor", "PenTool", Color(0xFF9333EA), 272f, AppCategory.PRODUKTIVITAET, null, 60, "UI-Entwurf v4", isNew = true),
        AppDefinition("app-fokus", "Fokuszeit", "Focus", Color(0xFF0EA5E9), 199f, AppCategory.PRODUKTIVITAET, null, 58, "Deep Work Modus", isNew = true),
        AppDefinition("app-wolke", "Ablage", "Cloud", Color(0xFF38BDF8), 199f, AppCategory.PRODUKTIVITAET, null, 74, "Synchronisiert"),
        AppDefinition("app-brief", "Signatur", "FileSignature", Color(0xFF6366F1), 239f, AppCategory.PRODUKTIVITAET, null, 49, "PDF unterzeichnen"),

        // System & Fitness
        AppDefinition("app-fitness", "Fitness", "Activity", Color(0xFF10B981), 158f, AppCategory.SYSTEM, null, 89, "7.420 Schritte heute"),
        AppDefinition("app-einstellungen", "Einstellungen", "Settings", Color(0xFF64748B), 215f, AppCategory.SYSTEM, null, 87, "System aktuell"),
        AppDefinition("app-akku", "Energie", "BatteryCharging", Color(0xFF22C55E), 142f, AppCategory.SYSTEM, null, 70, "84 % · ca. 19 Std."),
        AppDefinition("app-wallet", "Kartenfach", "Wallet", Color(0xFF0F172A), 222f, AppCategory.SYSTEM, null, 76, "Deutschlandticket"),
        AppDefinition("app-smarthome", "Zuhause", "Home", Color(0xFFF59E0B), 38f, AppCategory.SYSTEM, null, 79, "Wohnzimmer 21,5°"),
        AppDefinition("app-bahn", "Abfahrt", "Train", Color(0xFFE11D48), 347f, AppCategory.WERKZEUGE, null, 82, "S3 in 6 Min."),
        AppDefinition("app-klima", "Luftblick", "Wind", Color(0xFF06B6D4), 188f, AppCategory.WERKZEUGE, null, 51, "AQI 24 · Sehr gut", isNew = true),
        AppDefinition("app-backup", "Sicherung", "HardDrive", Color(0xFF475569), 215f, AppCategory.SYSTEM, null, 45, "Gestern 23:10"),
        AppDefinition("app-bluetooth", "Geräte", "Bluetooth", Color(0xFF2563EB), 221f, AppCategory.SYSTEM, null, 68, "Kopfhörer verbunden"),
        AppDefinition("app-store", "App-Markt", "ShoppingBag", Color(0xFF16A34A), 142f, AppCategory.SYSTEM, 2, 72, "2 Updates verfügbar"),
        AppDefinition("app-schlaf", "Schlafphasen", "Moon", Color(0xFF4338CA), 243f, AppCategory.SYSTEM, null, 57, "7 Std. 42 Min."),
        AppDefinition("app-globus", "Weltzeit", "Globe", Color(0xFF0284C7), 201f, AppCategory.WERKZEUGE, null, 48, "Berlin · Tokio · NYC"),
        AppDefinition("app-bank", "Giro", "Landmark", Color(0xFF0D9488), 175f, AppCategory.PRODUKTIVITAET, null, 81, "Kontostand aktuell"),
        AppDefinition("app-rezepte", "Küche", "Utensils", Color(0xFFEA580C), 21f, AppCategory.LESEN, null, 53, "Wochenplan"),
        AppDefinition("app-pflanzen", "Botanik", "Sprout", Color(0xFF22C55E), 142f, AppCategory.WERKZEUGE, null, 44, "Gießen in 2 Tagen"),
        AppDefinition("app-studio", "Klangstudio", "Sliders", Color(0xFFEC4899), 330f, AppCategory.MEDIEN, null, 56, "Equalizer · Warm"),
        AppDefinition("app-paket", "Sendungen", "Package", Color(0xFFD97706), 32f, AppCategory.WERKZEUGE, 1, 66, "In Zustellung heute"),
        AppDefinition("app-drucker", "Drucken", "Printer", Color(0xFF64748B), 215f, AppCategory.WERKZEUGE, null, 39, "Studio-Drucker bereit"),
        AppDefinition("app-netzwerk", "WLAN-Monitor", "Wifi", Color(0xFF0EA5E9), 199f, AppCategory.SYSTEM, null, 50, "420 Mbit/s · 5 GHz"),
        AppDefinition("app-lupe", "Lupe", "Search", Color(0xFF4F46E5), 243f, AppCategory.WERKZEUGE, null, 41, "Kontrastverstärker"),
        AppDefinition("app-sprachen", "Wortschatz", "BookMarked", Color(0xFF8B5CF6), 258f, AppCategory.LESEN, null, 59, "12 Tage Serie"),
        AppDefinition("app-wandern", "Gipfel", "Mountain", Color(0xFF15803D), 142f, AppCategory.SPIELE, null, 47, "Harz Route 14 km"),
        AppDefinition("app-pinsel", "Palette", "Palette", Color(0xFFF43F5E), 350f, AppCategory.PRODUKTIVITAET, null, 63, "6 Farbpaletten"),
        AppDefinition("app-bibliothek", "Hörbuch", "Headset", Color(0xFF9333EA), 271f, AppCategory.MEDIEN, null, 61, "Kapitel 4 · 28 Min."),
        AppDefinition("app-archiv", "Tresor-ZIP", "Archive", Color(0xFF78716C), 25f, AppCategory.WERKZEUGE, null, 37, "Entpacker"),
        AppDefinition("app-herz", "Puls", "HeartPulse", Color(0xFFEF4444), 0f, AppCategory.SYSTEM, null, 64, "Ruhepuls 61 bpm"),
    )

    val APPS_BY_ID: Map<String, AppDefinition> = APPS_CATALOG.associateBy { it.id }

    val CONTACTS_MOCK: List<ContactItem> = listOf(
        ContactItem("c1", "Lena Weber", "Design Lead", "Im Studio · Verfügbar", 335f),
        ContactItem("c2", "Jonas Richter", "Engine Architektur", "In Termin bis 11:00", 215f),
        ContactItem("c3", "Mira Hoffmann", "Audio & Haptik", "Mobil erreichbar", 162f),
        ContactItem("c4", "Tariq Al-Mansoor", "Grafik-Programmierung", "Zuletzt vor 8 Min.", 32f),
    )

    val SETTINGS_MOCK: List<SettingItem> = listOf(
        SettingItem("s1", "WLAN & Internet", "Studio_5GHz · Verbunden", "Wifi", 205f, Settings.ACTION_WIFI_SETTINGS),
        SettingItem("s2", "Display & Helligkeit", "120 Hz Adaptiv · Nachtlicht aus", "Sun", 40f, Settings.ACTION_DISPLAY_SETTINGS),
        SettingItem("s3", "Töne & Haptik", "System-Vibration aktiv", "Vibrate", 265f, Settings.ACTION_SOUND_SETTINGS),
        SettingItem("s4", "Akku & Laden", "84 % · Optimiertes Laden", "BatteryCharging", 145f, Settings.ACTION_BATTERY_SAVER_SETTINGS),
    )

    val CATALOG_EXTRA_TILES: List<TileData> = listOf(
        TileData("cat-clock", TileType.CLOCK, "Uhr", accentHue = 215f),
        TileData("cat-weather", TileType.WEATHER, "Wetter", accentHue = 200f),
        TileData("cat-calendar", TileType.CALENDAR, "Kalender", accentHue = 224f),
        TileData("cat-media", TileType.MEDIA, "Medien", accentHue = 280f),
        TileData(
            id = "cat-note",
            type = TileType.NOTE,
            title = "Notiz",
            accentHue = 44f,
            noteLines = SystemTileDefaults.DEFAULT_NOTE_LINES,
        ),
        TileData("cat-photos", TileType.PHOTOS, "Fotos", accentHue = 342f),
        TileData("cat-battery", TileType.BATTERY, "Akku", accentHue = 146f),
        TileData("cat-steps", TileType.STEPS, "Schritte", accentHue = 168f),
        TileData("cat-toggles", TileType.QUICK_TOGGLES, "Schnellschalter", accentHue = 199f, togglePreset = TogglePreset.SYSTEM),
        TileData(
            id = "cat-reminders",
            type = TileType.REMINDERS,
            title = "Erinnerungen",
            accentHue = 158f,
            reminders = SystemTileDefaults.DEFAULT_REMINDERS,
        ),
    )

    private data class ShortcutBarConfig(
        val splitId: String,
        val leafId: String,
        val preset: TogglePreset,
        val title: String,
        val hue: Float,
    )

    private val SHORTCUT_BAR_CONFIGS = mapOf(
        "page-heute" to ShortcutBarConfig("heute-bar-wrap", "leaf-heute-toggles", TogglePreset.TODAY, "Heute-Kürzel", 32f),
        "page-start" to ShortcutBarConfig("start-bar-wrap", "leaf-start-toggles", TogglePreset.SYSTEM, "Schnellschalter", 199f),
        "page-medien" to ShortcutBarConfig("medien-bar-wrap", "leaf-medien-toggles", TogglePreset.MEDIA, "Audio-Kürzel", 278f),
        "page-apps" to ShortcutBarConfig("apps-bar-wrap", "leaf-apps-toggles", TogglePreset.TOOLS, "Werkzeug-Kürzel", 195f),
    )

    fun createInitialPages(shortcutMode: ShortcutBarMode = ShortcutBarMode.ALL): List<LauncherPage> {
        val basePages = listOf(
            LauncherPage(
                id = "page-heute",
                title = "Heute",
                kind = PageKind.TILES,
                root = SplitNode(
                    id = "heute-root",
                    direction = SplitDirection.VERTICAL,
                    ratio = 0.34f,
                    first = SplitNode(
                        id = "heute-top",
                        direction = SplitDirection.HORIZONTAL,
                        ratio = 0.66f,
                        first = LeafNode(
                            id = "leaf-heute-greeting",
                            tile = TileData("tile-heute-greeting", TileType.GREETING, "Guten Morgen", accentHue = 32f),
                        ),
                        second = LeafNode(
                            id = "leaf-heute-temp",
                            tile = TileData("tile-heute-temp", TileType.TEMP_CURVE, "Temperatur", accentHue = 202f),
                        ),
                    ),
                    second = SplitNode(
                        id = "heute-mid-bot",
                        direction = SplitDirection.VERTICAL,
                        ratio = 0.53f,
                        first = LeafNode(
                            id = "leaf-heute-timeline",
                            tile = TileData("tile-heute-timeline", TileType.CALENDAR_TIMELINE, "Zeitstrahl", accentHue = 222f),
                        ),
                        second = SplitNode(
                            id = "heute-bottom",
                            direction = SplitDirection.HORIZONTAL,
                            ratio = 0.52f,
                            first = LeafNode(
                                id = "leaf-heute-reminders",
                                tile = TileData(
                                    id = "tile-heute-reminders",
                                    type = TileType.REMINDERS,
                                    title = "Erinnerungen",
                                    accentHue = 158f,
                                    reminders = SystemTileDefaults.DEFAULT_REMINDERS,
                                ),
                            ),
                            second = LeafNode(
                                id = "leaf-heute-rings",
                                tile = TileData("tile-heute-rings", TileType.BATTERY_STEPS, "Akku & Schritte", accentHue = 146f),
                            ),
                        ),
                    ),
                ),
            ),
            LauncherPage(
                id = "page-start",
                title = "Start",
                kind = PageKind.TILES,
                root = SplitNode(
                    id = "start-root",
                    direction = SplitDirection.VERTICAL,
                    ratio = 1f / 3f,
                    first = SplitNode(
                        id = "start-top",
                        direction = SplitDirection.HORIZONTAL,
                        ratio = 0.55f,
                        first = LeafNode("leaf-start-clock", TileData("tile-start-clock", TileType.CLOCK, "Uhr", accentHue = 216f)),
                        second = LeafNode("leaf-start-weather", TileData("tile-start-weather", TileType.WEATHER, "Wetter", accentHue = 198f)),
                    ),
                    second = SplitNode(
                        id = "start-mid-bot",
                        direction = SplitDirection.VERTICAL,
                        ratio = 0.5f,
                        first = SplitNode(
                            id = "start-middle",
                            direction = SplitDirection.HORIZONTAL,
                            ratio = 0.5f,
                            first = LeafNode("leaf-start-calendar", TileData("tile-start-calendar", TileType.CALENDAR, "Kalender", accentHue = 224f)),
                            second = LeafNode("leaf-start-media", TileData("tile-start-media", TileType.MEDIA, "Medien", accentHue = 278f)),
                        ),
                        second = LeafNode(
                            id = "leaf-start-favs",
                            tile = TileData(
                                id = "tile-start-favs",
                                type = TileType.APP_GROUP,
                                title = "Favoriten",
                                accentHue = 36f,
                                appIds = listOf(
                                    "app-telefon", "app-nachrichten", "app-kamera", "app-browser",
                                    "app-mail", "app-musik", "app-karten", "app-notizen",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            LauncherPage(
                id = "page-medien",
                title = "Medien",
                kind = PageKind.TILES,
                root = SplitNode(
                    id = "medien-root",
                    direction = SplitDirection.VERTICAL,
                    ratio = 0.48f,
                    first = LeafNode("leaf-medien-hero", TileData("tile-medien-hero", TileType.MEDIA, "Wiedergabe", mediaLarge = true, accentHue = 278f)),
                    second = SplitNode(
                        id = "medien-bottom-stack",
                        direction = SplitDirection.VERTICAL,
                        ratio = 0.58f,
                        first = SplitNode(
                            id = "medien-bottom",
                            direction = SplitDirection.HORIZONTAL,
                            ratio = 0.52f,
                            first = LeafNode(
                                id = "leaf-medien-podcast",
                                tile = TileData(
                                    id = "tile-medien-podcast",
                                    type = TileType.SINGLE_APP,
                                    title = "Audio & Podcast",
                                    appId = "app-podcast",
                                    accentHue = 271f,
                                ),
                            ),
                            second = LeafNode(
                                id = "leaf-medien-lesen",
                                tile = TileData(
                                    id = "tile-medien-lesen",
                                    type = TileType.APP_GROUP,
                                    title = "Lesen",
                                    accentHue = 28f,
                                    appIds = listOf("app-bücher", "app-presse", "app-rss"),
                                ),
                            ),
                        ),
                        second = LeafNode("leaf-medien-photos", TileData("tile-medien-photos", TileType.PHOTOS, "Fotos", accentHue = 342f)),
                    ),
                ),
            ),
            LauncherPage(
                id = "page-apps",
                title = "Apps",
                kind = PageKind.TILES,
                root = SplitNode(
                    id = "apps-root",
                    direction = SplitDirection.VERTICAL,
                    ratio = 0.34f,
                    first = LeafNode(
                        id = "leaf-apps-werkzeuge",
                        tile = TileData(
                            id = "tile-apps-werkzeuge",
                            type = TileType.APP_GROUP,
                            title = "Werkzeuge",
                            accentHue = 195f,
                            appIds = listOf(
                                "app-karten", "app-rechner", "app-uhr", "app-kompass", "app-scanner",
                                "app-diktat", "app-lampe", "app-übersetzer", "app-tresor", "app-wasserwaage",
                                "app-dateien", "app-stoppuhr", "app-qr", "app-wetter",
                            ),
                        ),
                    ),
                    second = SplitNode(
                        id = "apps-lower",
                        direction = SplitDirection.VERTICAL,
                        ratio = 0.5f,
                        first = SplitNode(
                            id = "apps-middle",
                            direction = SplitDirection.HORIZONTAL,
                            ratio = 0.54f,
                            first = LeafNode(
                                id = "leaf-apps-social",
                                tile = TileData(
                                    id = "tile-apps-social",
                                    type = TileType.APP_GROUP,
                                    title = "Social",
                                    accentHue = 242f,
                                    appIds = listOf(
                                        "app-telefon", "app-nachrichten", "app-funk", "app-kreis",
                                        "app-videochat", "app-forum", "app-teilen", "app-kanäle",
                                    ),
                                ),
                            ),
                            second = LeafNode(
                                id = "leaf-apps-spiele",
                                tile = TileData(
                                    id = "tile-apps-spiele",
                                    type = TileType.APP_GROUP,
                                    title = "Spiele",
                                    accentHue = 342f,
                                    appIds = listOf("app-schach", "app-rallye", "app-rätsel", "app-würfel", "app-sterne"),
                                ),
                            ),
                        ),
                        second = SplitNode(
                            id = "apps-bottom-row",
                            direction = SplitDirection.HORIZONTAL,
                            ratio = 0.48f,
                            first = LeafNode(
                                id = "leaf-apps-medien",
                                tile = TileData(
                                    id = "tile-apps-medien",
                                    type = TileType.APP_GROUP,
                                    title = "Medien",
                                    accentHue = 325f,
                                    appIds = listOf("app-kamera", "app-musik", "app-galerie", "app-kino"),
                                ),
                            ),
                            second = LeafNode(
                                id = "leaf-apps-fitness",
                                tile = TileData(
                                    id = "tile-apps-fitness",
                                    type = TileType.BATTERY_STEPS,
                                    title = "Akku & Schritte",
                                    accentHue = 158f,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            LauncherPage(
                id = "page-alle-apps",
                title = "Alle Apps",
                kind = PageKind.ALL_APPS,
                root = SplitNode(
                    id = "alle-root",
                    direction = SplitDirection.VERTICAL,
                    ratio = 0.15f,
                    first = LeafNode(
                        id = "leaf-alle-new",
                        tile = TileData(
                            id = "tile-alle-new",
                            type = TileType.NEWLY_INSTALLED,
                            title = "Neu installiert",
                            accentHue = 258f,
                            newAppIds = listOf("app-skizze", "app-fokus", "app-klima"),
                        ),
                    ),
                    second = SplitNode(
                        id = "alle-sub",
                        direction = SplitDirection.VERTICAL,
                        ratio = 0.85f,
                        first = LeafNode("leaf-alle-box", TileData("tile-alle-box", TileType.ALL_APPS_BOX, "Alle Apps", accentHue = 215f)),
                        second = LeafNode("leaf-alle-search", TileData("tile-alle-search", TileType.SEARCH_BAR, "Suche", accentHue = 215f)),
                    ),
                ),
            ),
        )
        return syncShortcutBars(basePages, shortcutMode)
    }

    fun syncShortcutBars(pages: List<LauncherPage>, mode: ShortcutBarMode): List<LauncherPage> =
        pages.map { page ->
            val cfg = SHORTCUT_BAR_CONFIGS[page.id] ?: return@map page
            val stripped = TilingEngine.removeLeaf(page.root, cfg.leafId)
            val baseRoot = stripped.tree ?: page.root
            val shouldHave = mode == ShortcutBarMode.ALL ||
                (mode == ShortcutBarMode.START_ONLY && page.id == "page-start")

            if (!shouldHave) {
                page.copy(root = baseRoot)
            } else {
                page.copy(
                    root = SplitNode(
                        id = cfg.splitId,
                        direction = SplitDirection.VERTICAL,
                        ratio = 0.885f,
                        first = baseRoot,
                        second = LeafNode(
                            id = cfg.leafId,
                            tile = TileData(
                                id = "tile-${cfg.leafId}",
                                type = TileType.QUICK_TOGGLES,
                                title = cfg.title,
                                togglePreset = cfg.preset,
                                accentHue = cfg.hue,
                            ),
                        ),
                    )
                )
            }
        }
}
