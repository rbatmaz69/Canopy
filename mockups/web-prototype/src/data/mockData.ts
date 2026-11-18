import { removeLeaf } from '../layout/tree';
import { LauncherPage, TileData } from '../layout/types';

export type ShortcutBarMode = 'all' | 'start-only' | 'off';

export type AppCategory =
  | 'Social'
  | 'Spiele'
  | 'Werkzeuge'
  | 'Medien'
  | 'Lesen'
  | 'Produktivität'
  | 'System';

export interface AppDefinition {
  id: string;
  name: string;
  iconKey: string;
  color: string;
  hue: number;
  category: AppCategory;
  badge?: number;
  usageScore: number;
  subtitle: string;
  isNew?: boolean;
}

export interface ContactItem {
  id: string;
  name: string;
  role: string;
  status: string;
  hue: number;
}

export interface SettingItem {
  id: string;
  name: string;
  detail: string;
  iconKey: string;
  hue: number;
}

export interface MediaTrack {
  id: string;
  title: string;
  artist: string;
  album: string;
  durationSec: number;
  accentHue: number;
}

export interface CalendarEvent {
  id: string;
  title: string;
  time: string;
  endTime: string;
  location: string;
  startHour: number;
  durationHours: number;
  hue: number;
}

export const APPS_CATALOG: AppDefinition[] = [
  // Favoriten & Social (8+ apps)
  { id: 'app-telefon', name: 'Telefon', iconKey: 'Phone', color: '#10B981', hue: 160, category: 'Social', usageScore: 98, subtitle: 'Anrufe & Favoriten' },
  { id: 'app-nachrichten', name: 'Nachrichten', iconKey: 'MessageCircle', color: '#3B82F6', hue: 217, category: 'Social', badge: 3, usageScore: 96, subtitle: '3 ungelesene Chats' },
  { id: 'app-kamera', name: 'Kamera', iconKey: 'Camera', color: '#F59E0B', hue: 38, category: 'Medien', usageScore: 95, subtitle: 'RAW · 50 MP Bereit' },
  { id: 'app-browser', name: 'Netz', iconKey: 'Compass', color: '#0EA5E9', hue: 199, category: 'Produktivität', usageScore: 94, subtitle: '4 geöffnete Tabs' },
  { id: 'app-mail', name: 'Postfach', iconKey: 'Mail', color: '#EF4444', hue: 0, category: 'Produktivität', badge: 5, usageScore: 91, subtitle: '5 neue E-Mails' },
  { id: 'app-musik', name: 'Klang', iconKey: 'Music2', color: '#EC4899', hue: 330, category: 'Medien', usageScore: 93, subtitle: 'Lo-fi Mix läuft' },
  { id: 'app-karten', name: 'Karten', iconKey: 'MapPin', color: '#22C55E', hue: 142, category: 'Werkzeuge', usageScore: 89, subtitle: '14 Min. zur Arbeit' },
  { id: 'app-notizen', name: 'Notizen', iconKey: 'FileText', color: '#EAB308', hue: 48, category: 'Produktivität', usageScore: 88, subtitle: '12 Entwürfe' },

  // Social (8 total)
  { id: 'app-funk', name: 'Funk', iconKey: 'Radio', color: '#6366F1', hue: 239, category: 'Social', badge: 2, usageScore: 84, subtitle: '2 neue Sprachnotizen' },
  { id: 'app-kreis', name: 'Kreis', iconKey: 'Users', color: '#8B5CF6', hue: 258, category: 'Social', usageScore: 79, subtitle: 'Familien-Gruppe aktiv' },
  { id: 'app-videochat', name: 'Videoanruf', iconKey: 'Video', color: '#14B8A6', hue: 173, category: 'Social', usageScore: 76, subtitle: 'Letzter Call gestern' },
  { id: 'app-forum', name: 'Debatte', iconKey: 'MessagesSquare', color: '#F97316', hue: 25, category: 'Social', badge: 1, usageScore: 71, subtitle: 'Design-Thread aktiv' },
  { id: 'app-teilen', name: 'Schnappschuss', iconKey: 'Aperture', color: '#D946EF', hue: 292, category: 'Social', usageScore: 68, subtitle: 'Story von Lena' },
  { id: 'app-kanäle', name: 'Kanäle', iconKey: 'AtSign', color: '#06B6D4', hue: 189, category: 'Social', usageScore: 65, subtitle: 'Tech-Feed aktualisiert' },

  // Spiele (5 apps)
  { id: 'app-schach', name: 'Schachmatt', iconKey: 'Crown', color: '#7C3AED', hue: 263, category: 'Spiele', badge: 1, usageScore: 82, subtitle: 'Du bist am Zug' },
  { id: 'app-rallye', name: 'Kurve Rallye', iconKey: 'Gamepad2', color: '#DC2626', hue: 0, category: 'Spiele', usageScore: 77, subtitle: 'Streckenrekord 1:42' },
  { id: 'app-rätsel', name: 'Logikbox', iconKey: 'Puzzle', color: '#2563EB', hue: 221, category: 'Spiele', usageScore: 72, subtitle: 'Tagesrätsel offen' },
  { id: 'app-würfel', name: 'Taktik', iconKey: 'Dices', color: '#059669', hue: 161, category: 'Spiele', usageScore: 61, subtitle: 'Saison Rang 4' },
  { id: 'app-sterne', name: 'Orbit', iconKey: 'Rocket', color: '#DB2777', hue: 333, category: 'Spiele', usageScore: 59, subtitle: 'Level 28' },

  // Werkzeuge (14 apps)
  { id: 'app-rechner', name: 'Rechner', iconKey: 'Calculator', color: '#475569', hue: 215, category: 'Werkzeuge', usageScore: 86, subtitle: 'Wissenschaftlich' },
  { id: 'app-uhr', name: 'Wecker', iconKey: 'AlarmClock', color: '#0284C7', hue: 200, category: 'Werkzeuge', usageScore: 85, subtitle: '07:15 täglich' },
  { id: 'app-kompass', name: 'Kompass', iconKey: 'Navigation', color: '#0D9488', hue: 175, category: 'Werkzeuge', usageScore: 54, subtitle: '24° NNO' },
  { id: 'app-scanner', name: 'Dokument-Scan', iconKey: 'ScanLine', color: '#4F46E5', hue: 243, category: 'Werkzeuge', usageScore: 67, subtitle: 'PDF-Export bereit' },
  { id: 'app-diktat', name: 'Diktiergerät', iconKey: 'Mic', color: '#E11D48', hue: 347, category: 'Werkzeuge', usageScore: 73, subtitle: '3 Aufnahmen' },
  { id: 'app-lampe', name: 'Lichtwerk', iconKey: 'Flashlight', color: '#D97706', hue: 32, category: 'Werkzeuge', usageScore: 64, subtitle: 'Stufe 3 bereit' },
  { id: 'app-übersetzer', name: 'Übersetzer', iconKey: 'Languages', color: '#2563EB', hue: 221, category: 'Werkzeuge', usageScore: 70, subtitle: 'DE ↔ EN Offline' },
  { id: 'app-tresor', name: 'Passwörter', iconKey: 'KeyRound', color: '#0F766E', hue: 176, category: 'Werkzeuge', usageScore: 81, subtitle: '94 Einträge gesichert' },
  { id: 'app-wasserwaage', name: 'Wasserwaage', iconKey: 'Ruler', color: '#65A30D', hue: 84, category: 'Werkzeuge', usageScore: 43, subtitle: 'Kalibriert' },
  { id: 'app-dateien', name: 'Dateien', iconKey: 'FolderOpen', color: '#EA580C', hue: 21, category: 'Werkzeuge', usageScore: 83, subtitle: '42 GB frei' },
  { id: 'app-stoppuhr', name: 'Timer', iconKey: 'Timer', color: '#7C3AED', hue: 262, category: 'Werkzeuge', usageScore: 66, subtitle: '25:00 Fokus' },
  { id: 'app-qr', name: 'QR-Leser', iconKey: 'QrCode', color: '#334155', hue: 215, category: 'Werkzeuge', usageScore: 52, subtitle: 'Blitzscan aktiv' },
  { id: 'app-wetter', name: 'Wetter', iconKey: 'CloudRain', color: '#0369A1', hue: 201, category: 'Werkzeuge', usageScore: 90, subtitle: 'Regen ab 17 Uhr' },

  // Medien (4+ apps)
  { id: 'app-podcast', name: 'Podcast', iconKey: 'Headphones', color: '#9333EA', hue: 271, category: 'Medien', badge: 2, usageScore: 87, subtitle: 'Lage der Nation · 14 Min.' },
  { id: 'app-galerie', name: 'Galerie', iconKey: 'Image', color: '#F43F5E', hue: 350, category: 'Medien', usageScore: 88, subtitle: '1.240 Fotos' },
  { id: 'app-kino', name: 'Mediathek', iconKey: 'Film', color: '#2563EB', hue: 220, category: 'Medien', usageScore: 74, subtitle: '2 Merklisten-Filme' },
  { id: 'app-radio', name: 'Welle FM', iconKey: 'Disc3', color: '#DB2777', hue: 332, category: 'Medien', usageScore: 62, subtitle: 'Kulturradio Live' },

  // Lesen (3+ apps)
  { id: 'app-bücher', name: 'Leselicht', iconKey: 'BookOpen', color: '#B45309', hue: 30, category: 'Lesen', usageScore: 80, subtitle: 'Kapitel 7 · 64 %' },
  { id: 'app-presse', name: 'Tagesblatt', iconKey: 'Newspaper', color: '#1D4ED8', hue: 224, category: 'Lesen', badge: 4, usageScore: 78, subtitle: 'Morgenausgabe da' },
  { id: 'app-rss', name: 'Blätter', iconKey: 'Rss', color: '#EA580C', hue: 22, category: 'Lesen', usageScore: 69, subtitle: '18 Artikel ungelesen' },
  { id: 'app-magazin', name: 'Essenz', iconKey: 'Bookmark', color: '#0D9488', hue: 175, category: 'Lesen', usageScore: 55, subtitle: '9 gespeicherte Essays' },

  // Produktivität
  { id: 'app-kalender', name: 'Kalender', iconKey: 'Calendar', color: '#2563EB', hue: 221, category: 'Produktivität', usageScore: 92, subtitle: 'Nächster Termin 10:00' },
  { id: 'app-aufgaben', name: 'Erinnerung', iconKey: 'CheckSquare', color: '#059669', hue: 161, category: 'Produktivität', badge: 3, usageScore: 85, subtitle: '3 offen heute' },
  { id: 'app-tabellen', name: 'Zahlen', iconKey: 'BarChart3', color: '#16A34A', hue: 142, category: 'Produktivität', usageScore: 63, subtitle: 'Budget Nov.' },
  { id: 'app-code', name: 'Terminal', iconKey: 'Terminal', color: '#1E293B', hue: 217, category: 'Produktivität', usageScore: 75, subtitle: 'SSH bereit' },
  { id: 'app-skizze', name: 'Vektor', iconKey: 'PenTool', color: '#9333EA', hue: 272, category: 'Produktivität', usageScore: 60, subtitle: 'UI-Entwurf v4', isNew: true },
  { id: 'app-fokus', name: 'Fokuszeit', iconKey: 'Focus', color: '#0EA5E9', hue: 199, category: 'Produktivität', usageScore: 58, subtitle: 'Deep Work Modus', isNew: true },
  { id: 'app-wolke', name: 'Ablage', iconKey: 'Cloud', color: '#38BDF8', hue: 199, category: 'Produktivität', usageScore: 74, subtitle: 'Synchronisiert' },
  { id: 'app-brief', name: 'Signatur', iconKey: 'FileSignature', color: '#6366F1', hue: 239, category: 'Produktivität', usageScore: 49, subtitle: 'PDF unterzeichnen' },

  // System & Neu installiert & Fitness
  { id: 'app-fitness', name: 'Fitness', iconKey: 'Activity', color: '#10B981', hue: 158, category: 'System', usageScore: 89, subtitle: '7.420 Schritte heute' },
  { id: 'app-einstellungen', name: 'Einstellungen', iconKey: 'Settings', color: '#64748B', hue: 215, category: 'System', usageScore: 87, subtitle: 'System aktuell' },
  { id: 'app-akku', name: 'Energie', iconKey: 'BatteryCharging', color: '#22C55E', hue: 142, category: 'System', usageScore: 70, subtitle: '84 % · ca. 19 Std.' },
  { id: 'app-wallet', name: 'Kartenfach', iconKey: 'Wallet', color: '#0F172A', hue: 222, category: 'System', usageScore: 76, subtitle: 'Deutschlandticket' },
  { id: 'app-smarthome', name: 'Zuhause', iconKey: 'Home', color: '#F59E0B', hue: 38, category: 'System', usageScore: 79, subtitle: 'Wohnzimmer 21,5°' },
  { id: 'app-bahn', name: 'Abfahrt', iconKey: 'Train', color: '#E11D48', hue: 347, category: 'Werkzeuge', usageScore: 82, subtitle: 'S3 in 6 Min.' },
  { id: 'app-klima', name: 'Luftblick', iconKey: 'Wind', color: '#06B6D4', hue: 188, category: 'Werkzeuge', usageScore: 51, subtitle: 'AQI 24 · Sehr gut', isNew: true },
  { id: 'app-backup', name: 'Sicherung', iconKey: 'HardDrive', color: '#475569', hue: 215, category: 'System', usageScore: 45, subtitle: 'Gestern 23:10' },
  { id: 'app-bluetooth', name: 'Geräte', iconKey: 'Bluetooth', color: '#2563EB', hue: 221, category: 'System', usageScore: 68, subtitle: 'Kopfhörer verbunden' },
  { id: 'app-store', name: 'App-Markt', iconKey: 'ShoppingBag', color: '#16A34A', hue: 142, category: 'System', badge: 2, usageScore: 72, subtitle: '2 Updates verfügbar' },
  { id: 'app-schlaf', name: 'Schlafphasen', iconKey: 'Moon', color: '#4338CA', hue: 243, category: 'System', usageScore: 57, subtitle: '7 Std. 42 Min.' },
  { id: 'app-globus', name: 'Weltzeit', iconKey: 'Globe', color: '#0284C7', hue: 201, category: 'Werkzeuge', usageScore: 48, subtitle: 'Berlin · Tokio · NYC' },
  { id: 'app-bank', name: 'Giro', iconKey: 'Landmark', color: '#0D9488', hue: 175, category: 'Produktivität', usageScore: 81, subtitle: 'Kontostand aktuell' },
  { id: 'app-rezepte', name: 'Küche', iconKey: 'Utensils', color: '#EA580C', hue: 21, category: 'Lesen', usageScore: 53, subtitle: 'Wochenplan' },
  { id: 'app-pflanzen', name: 'Botanik', iconKey: 'Sprout', color: '#22C55E', hue: 142, category: 'Werkzeuge', usageScore: 44, subtitle: 'Gießen in 2 Tagen' },
  { id: 'app-studio', name: 'Klangstudio', iconKey: 'Sliders', color: '#EC4899', hue: 330, category: 'Medien', usageScore: 56, subtitle: 'Equalizer · Warm' },
  { id: 'app-paket', name: 'Sendungen', iconKey: 'Package', color: '#D97706', hue: 32, category: 'Werkzeuge', badge: 1, usageScore: 66, subtitle: 'In Zustellung heute' },
  { id: 'app-drucker', name: 'Drucken', iconKey: 'Printer', color: '#64748B', hue: 215, category: 'Werkzeuge', usageScore: 39, subtitle: 'Studio-Drucker bereit' },
  { id: 'app-netzwerk', name: 'WLAN-Monitor', iconKey: 'Wifi', color: '#0EA5E9', hue: 199, category: 'System', usageScore: 50, subtitle: '420 Mbit/s · 5 GHz' },
  { id: 'app-lupe', name: 'Lupe', iconKey: 'Search', color: '#4F46E5', hue: 243, category: 'Werkzeuge', usageScore: 41, subtitle: 'Kontrastverstärker' },
  { id: 'app-sprachen', name: 'Wortschatz', iconKey: 'BookMarked', color: '#8B5CF6', hue: 258, category: 'Lesen', usageScore: 59, subtitle: '12 Tage Serie' },
  { id: 'app-wandern', name: 'Gipfel', iconKey: 'Mountain', color: '#15803D', hue: 142, category: 'Spiele', usageScore: 47, subtitle: 'Harz Route 14 km' },
  { id: 'app-pinsel', name: 'Palette', iconKey: 'Palette', color: '#F43F5E', hue: 350, category: 'Produktivität', usageScore: 63, subtitle: '6 Farbpaletten' },
  { id: 'app-bibliothek', name: 'Hörbuch', iconKey: 'Headset', color: '#9333EA', hue: 271, category: 'Medien', usageScore: 61, subtitle: 'Kapitel 4 · 28 Min.' },
  { id: 'app-archiv', name: 'Tresor-ZIP', iconKey: 'Archive', color: '#78716C', hue: 25, category: 'Werkzeuge', usageScore: 37, subtitle: 'Entpacker' },
  { id: 'app-herz', name: 'Puls', iconKey: 'HeartPulse', color: '#EF4444', hue: 0, category: 'System', usageScore: 64, subtitle: 'Ruhepuls 61 bpm' },
];

export const APPS_BY_ID: Record<string, AppDefinition> = Object.fromEntries(
  APPS_CATALOG.map((app) => [app.id, app])
);

export const CONTACTS_MOCK: ContactItem[] = [
  { id: 'c1', name: 'Lena Weber', role: 'Design Lead', status: 'Im Studio · Verfügbar', hue: 335 },
  { id: 'c2', name: 'Jonas Richter', role: 'Engine Architektur', status: 'In Termin bis 11:00', hue: 215 },
  { id: 'c3', name: 'Mira Hoffmann', role: 'Audio & Haptik', status: 'Mobil erreichbar', hue: 162 },
  { id: 'c4', name: 'Tariq Al-Mansoor', role: 'Grafik-Programmierung', status: 'Zuletzt vor 8 Min.', hue: 32 },
];

export const SETTINGS_MOCK: SettingItem[] = [
  { id: 's1', name: 'WLAN & Internet', detail: 'Studio_5GHz · Verbunden', iconKey: 'Wifi', hue: 205 },
  { id: 's2', name: 'Display & Helligkeit', detail: '120 Hz Adaptiv · Nachtlicht aus', iconKey: 'Sun', hue: 40 },
  { id: 's3', name: 'Töne & Haptik', detail: 'System-Vibration aktiv', iconKey: 'Vibrate', hue: 265 },
  { id: 's4', name: 'Akku & Laden', detail: '84 % · Optimiertes Laden', iconKey: 'BatteryCharging', hue: 145 },
];

export const MEDIA_TRACKS: MediaTrack[] = [
  { id: 't1', title: 'Lo-fi Mix', artist: 'Nachtschicht · Kosmos', album: 'Mitternachtsfrequenz', durationSec: 214, accentHue: 275 },
  { id: 't2', title: 'Regen über Berlin', artist: 'Klangwerk Süd', album: 'Beton & Moos', durationSec: 186, accentHue: 198 },
  { id: 't3', title: 'Analog Drift', artist: 'Studio 84', album: 'Tape Loops Vol. 2', durationSec: 242, accentHue: 22 },
];

export const CALENDAR_EVENTS: CalendarEvent[] = [
  { id: 'ev1', title: 'Game Engines', time: '10:00', endTime: '11:30', location: 'Raum B2', startHour: 10, durationHours: 1.5, hue: 220 },
  { id: 'ev2', title: 'Design Kritik', time: '12:30', endTime: '13:30', location: 'Atelier Nord', startHour: 12.5, durationHours: 1, hue: 335 },
  { id: 'ev3', title: 'Physik Simulation', time: '15:00', endTime: '16:15', location: 'Labor 4', startHour: 15, durationHours: 1.25, hue: 162 },
];

export const TEMP_DAY_SERIES = [
  { hour: '06h', temp: 4 },
  { hour: '09h', temp: 7 },
  { hour: '12h', temp: 10 },
  { hour: '15h', temp: 11 },
  { hour: '18h', temp: 8 },
  { hour: '21h', temp: 6 },
];

export const WEATHER_HOURS = [
  { hour: '10h', temp: '8°', icon: 'Cloud' },
  { hour: '13h', temp: '11°', icon: 'CloudSun' },
  { hour: '16h', temp: '9°', icon: 'Cloud' },
  { hour: '19h', temp: '6°', icon: 'CloudRain' },
];

export const CATALOG_EXTRA_TILES: TileData[] = [
  { id: 'cat-clock', type: 'clock', title: 'Uhr', accentHue: 215 },
  { id: 'cat-weather', type: 'weather', title: 'Wetter', accentHue: 200 },
  { id: 'cat-calendar', type: 'calendar', title: 'Kalender', accentHue: 224 },
  { id: 'cat-media', type: 'media', title: 'Medien', accentHue: 280 },
  {
    id: 'cat-note',
    type: 'note',
    title: 'Notiz',
    accentHue: 44,
    noteLines: [
      'Federwerte 0,75 / 400 prüfen',
      'Haptik-Muster auf Pixel testen',
      'Fugen-Abstand 6 / 12 dp',
    ],
  },
  { id: 'cat-photos', type: 'photos', title: 'Fotos', accentHue: 342 },
  { id: 'cat-battery', type: 'battery', title: 'Akku', accentHue: 146 },
  { id: 'cat-steps', type: 'steps', title: 'Schritte', accentHue: 168 },
  { id: 'cat-toggles', type: 'quick-toggles', title: 'Schnellschalter', accentHue: 199 },
  {
    id: 'cat-reminders',
    type: 'reminders',
    title: 'Erinnerungen',
    accentHue: 158,
    reminders: [
      { id: 'cr1', text: 'Prototyp testen', done: true, time: '09:30' },
      { id: 'cr2', text: 'Paket abholen', done: false, time: '14:00' },
      { id: 'cr3', text: 'Kaffeebohnen kaufen', done: false, time: '18:00' },
    ],
  },
];

export function createInitialPages(shortcutMode: ShortcutBarMode = 'all'): LauncherPage[] {
  const basePages: LauncherPage[] = [
    {
      id: 'page-heute',
      title: 'Heute',
      kind: 'tiles',
      root: {
        kind: 'split',
        id: 'heute-root',
        direction: 'vertical',
        ratio: 0.34,
        first: {
          kind: 'split',
          id: 'heute-top',
          direction: 'horizontal',
          ratio: 0.66,
          first: {
            kind: 'leaf',
            id: 'leaf-heute-greeting',
            tile: {
              id: 'tile-heute-greeting',
              type: 'greeting',
              title: 'Guten Morgen',
              accentHue: 32,
            },
          },
          second: {
            kind: 'leaf',
            id: 'leaf-heute-temp',
            tile: {
              id: 'tile-heute-temp',
              type: 'temp-curve',
              title: 'Temperatur',
              accentHue: 202,
            },
          },
        },
        second: {
          kind: 'split',
          id: 'heute-mid-bot',
          direction: 'vertical',
          ratio: 0.53,
          first: {
            kind: 'leaf',
            id: 'leaf-heute-timeline',
            tile: {
              id: 'tile-heute-timeline',
              type: 'calendar-timeline',
              title: 'Zeitstrahl',
              accentHue: 222,
            },
          },
          second: {
            kind: 'split',
            id: 'heute-bottom',
            direction: 'horizontal',
            ratio: 0.52,
            first: {
              kind: 'leaf',
              id: 'leaf-heute-reminders',
              tile: {
                id: 'tile-heute-reminders',
                type: 'reminders',
                title: 'Erinnerungen',
                accentHue: 158,
                reminders: [
                  { id: 'r1', text: 'Prototyp testen', done: true, time: '09:30' },
                  { id: 'r2', text: 'Paket abholen', done: false, time: '14:00' },
                  { id: 'r3', text: 'Kaffeebohnen kaufen', done: false, time: '18:00' },
                ],
              },
            },
            second: {
              kind: 'leaf',
              id: 'leaf-heute-rings',
              tile: {
                id: 'tile-heute-rings',
                type: 'battery-steps',
                title: 'Akku & Schritte',
                accentHue: 146,
              },
            },
          },
        },
      },
    },
    {
      id: 'page-start',
      title: 'Start',
      kind: 'tiles',
      root: {
        kind: 'split',
        id: 'start-root',
        direction: 'vertical',
        ratio: 1 / 3,
        first: {
          kind: 'split',
          id: 'start-top',
          direction: 'horizontal',
          ratio: 0.55,
          first: {
            kind: 'leaf',
            id: 'leaf-start-clock',
            tile: {
              id: 'tile-start-clock',
              type: 'clock',
              title: 'Uhr',
              accentHue: 216,
            },
          },
          second: {
            kind: 'leaf',
            id: 'leaf-start-weather',
            tile: {
              id: 'tile-start-weather',
              type: 'weather',
              title: 'Wetter',
              accentHue: 198,
            },
          },
        },
        second: {
          kind: 'split',
          id: 'start-mid-bot',
          direction: 'vertical',
          ratio: 0.5,
          first: {
            kind: 'split',
            id: 'start-middle',
            direction: 'horizontal',
            ratio: 0.5,
            first: {
              kind: 'leaf',
              id: 'leaf-start-calendar',
              tile: {
                id: 'tile-start-calendar',
                type: 'calendar',
                title: 'Kalender',
                accentHue: 224,
              },
            },
            second: {
              kind: 'leaf',
              id: 'leaf-start-media',
              tile: {
                id: 'tile-start-media',
                type: 'media',
                title: 'Medien',
                accentHue: 278,
              },
            },
          },
          second: {
            kind: 'leaf',
            id: 'leaf-start-favs',
            tile: {
              id: 'tile-start-favs',
              type: 'app-group',
              title: 'Favoriten',
              accentHue: 36,
              appIds: [
                'app-telefon',
                'app-nachrichten',
                'app-kamera',
                'app-browser',
                'app-mail',
                'app-musik',
                'app-karten',
                'app-notizen',
              ],
            },
          },
        },
      },
    },
    {
      id: 'page-medien',
      title: 'Medien',
      kind: 'tiles',
      root: {
        kind: 'split',
        id: 'medien-root',
        direction: 'vertical',
        ratio: 0.48,
        first: {
          kind: 'leaf',
          id: 'leaf-medien-hero',
          tile: {
            id: 'tile-medien-hero',
            type: 'media',
            title: 'Wiedergabe',
            mediaLarge: true,
            accentHue: 278,
          },
        },
        second: {
          kind: 'split',
          id: 'medien-bottom-stack',
          direction: 'vertical',
          ratio: 0.58,
          first: {
            kind: 'split',
            id: 'medien-bottom',
            direction: 'horizontal',
            ratio: 0.52,
            first: {
              kind: 'leaf',
              id: 'leaf-medien-podcast',
              tile: {
                id: 'tile-medien-podcast',
                type: 'third-party-widget',
                title: 'Podcast',
                appId: 'app-podcast',
                widgetKind: 'podcast',
                widgetFixedWidth: 164,
                widgetFixedHeight: 144,
                accentHue: 271,
              },
            },
            second: {
              kind: 'leaf',
              id: 'leaf-medien-lesen',
              tile: {
                id: 'tile-medien-lesen',
                type: 'app-group',
                title: 'Lesen',
                accentHue: 28,
                appIds: ['app-bücher', 'app-presse', 'app-rss'],
              },
            },
          },
          second: {
            kind: 'leaf',
            id: 'leaf-medien-photos',
            tile: {
              id: 'tile-medien-photos',
              type: 'photos',
              title: 'Fotos',
              accentHue: 342,
            },
          },
        },
      },
    },
    {
      id: 'page-apps',
      title: 'Apps',
      kind: 'tiles',
      root: {
        kind: 'split',
        id: 'apps-root',
        direction: 'vertical',
        ratio: 0.34,
        first: {
          kind: 'leaf',
          id: 'leaf-apps-werkzeuge',
          tile: {
            id: 'tile-apps-werkzeuge',
            type: 'app-group',
            title: 'Werkzeuge',
            accentHue: 195,
            appIds: [
              'app-karten',
              'app-rechner',
              'app-uhr',
              'app-kompass',
              'app-scanner',
              'app-diktat',
              'app-lampe',
              'app-übersetzer',
              'app-tresor',
              'app-wasserwaage',
              'app-dateien',
              'app-stoppuhr',
              'app-qr',
              'app-wetter',
            ],
          },
        },
        second: {
          kind: 'split',
          id: 'apps-lower',
          direction: 'vertical',
          ratio: 0.5,
          first: {
            kind: 'split',
            id: 'apps-middle',
            direction: 'horizontal',
            ratio: 0.54,
            first: {
              kind: 'leaf',
              id: 'leaf-apps-social',
              tile: {
                id: 'tile-apps-social',
                type: 'app-group',
                title: 'Social',
                accentHue: 242,
                appIds: [
                  'app-telefon',
                  'app-nachrichten',
                  'app-funk',
                  'app-kreis',
                  'app-videochat',
                  'app-forum',
                  'app-teilen',
                  'app-kanäle',
                ],
              },
            },
            second: {
              kind: 'leaf',
              id: 'leaf-apps-spiele',
              tile: {
                id: 'tile-apps-spiele',
                type: 'app-group',
                title: 'Spiele',
                accentHue: 342,
                appIds: [
                  'app-schach',
                  'app-rallye',
                  'app-rätsel',
                  'app-würfel',
                  'app-sterne',
                ],
              },
            },
          },
          second: {
            kind: 'split',
            id: 'apps-bottom-row',
            direction: 'horizontal',
            ratio: 0.48,
            first: {
              kind: 'leaf',
              id: 'leaf-apps-medien',
              tile: {
                id: 'tile-apps-medien',
                type: 'app-group',
                title: 'Medien',
                accentHue: 325,
                appIds: ['app-kamera', 'app-musik', 'app-galerie', 'app-kino'],
              },
            },
            second: {
              kind: 'leaf',
              id: 'leaf-apps-fitness',
              tile: {
                id: 'tile-apps-fitness',
                type: 'third-party-widget',
                title: 'Fitness',
                appId: 'app-fitness',
                widgetKind: 'fitness',
                widgetFixedWidth: 156,
                widgetFixedHeight: 132,
                accentHue: 158,
              },
            },
          },
        },
      },
    },
    {
      id: 'page-alle-apps',
      title: 'Alle Apps',
      kind: 'all-apps',
      root: {
        kind: 'split',
        id: 'alle-root',
        direction: 'vertical',
        ratio: 0.15,
        first: {
          kind: 'leaf',
          id: 'leaf-alle-new',
          tile: {
            id: 'tile-alle-new',
            type: 'newly-installed',
            title: 'Neu installiert',
            accentHue: 258,
            newAppIds: ['app-skizze', 'app-fokus', 'app-klima'],
          },
        },
        second: {
          kind: 'split',
          id: 'alle-sub',
          direction: 'vertical',
          ratio: 0.85,
          first: {
            kind: 'leaf',
            id: 'leaf-alle-box',
            tile: {
              id: 'tile-alle-box',
              type: 'all-apps-box',
              title: 'Alle Apps',
              accentHue: 215,
            },
          },
          second: {
            kind: 'leaf',
            id: 'leaf-alle-search',
            tile: {
              id: 'tile-alle-search',
              type: 'search-bar',
              title: 'Suche',
              accentHue: 215,
            },
          },
        },
      },
    },
  ];
  return syncShortcutBars(basePages, shortcutMode);
}

const SHORTCUT_BAR_CONFIG: Record<
  string,
  {
    splitId: string;
    leafId: string;
    preset: 'system' | 'today' | 'media' | 'tools';
    title: string;
    hue: number;
  }
> = {
  'page-heute': {
    splitId: 'heute-bar-wrap',
    leafId: 'leaf-heute-toggles',
    preset: 'today',
    title: 'Heute-Kürzel',
    hue: 32,
  },
  'page-start': {
    splitId: 'start-bar-wrap',
    leafId: 'leaf-start-toggles',
    preset: 'system',
    title: 'Schnellschalter',
    hue: 199,
  },
  'page-medien': {
    splitId: 'medien-bar-wrap',
    leafId: 'leaf-medien-toggles',
    preset: 'media',
    title: 'Audio-Kürzel',
    hue: 278,
  },
  'page-apps': {
    splitId: 'apps-bar-wrap',
    leafId: 'leaf-apps-toggles',
    preset: 'tools',
    title: 'Werkzeug-Kürzel',
    hue: 195,
  },
};

export function syncShortcutBars(
  pages: LauncherPage[],
  mode: ShortcutBarMode
): LauncherPage[] {
  return pages.map((page) => {
    const cfg = SHORTCUT_BAR_CONFIG[page.id];
    if (!cfg) return page;

    const stripped = removeLeaf(page.root, cfg.leafId);
    const baseRoot = stripped.tree ?? page.root;
    const shouldHave = mode === 'all' || (mode === 'start-only' && page.id === 'page-start');

    if (!shouldHave) {
      return { ...page, root: baseRoot };
    }

    return {
      ...page,
      root: {
        kind: 'split',
        id: cfg.splitId,
        direction: 'vertical',
        ratio: 0.885,
        first: baseRoot,
        second: {
          kind: 'leaf',
          id: cfg.leafId,
          tile: {
            id: `tile-${cfg.leafId}`,
            type: 'quick-toggles',
            title: cfg.title,
            togglePreset: cfg.preset,
            accentHue: cfg.hue,
          },
        },
      },
    };
  });
}
