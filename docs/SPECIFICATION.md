# Canopy – Technische Spezifikation (Referenz-Architektur)

Diese Spezifikation beschreibt das Design-, Layout- und Interaktionssystem von **Canopy** auf Basis des interaktiven Web-Prototyps unter [`mockups/web-prototype/`](../mockups/web-prototype/).

---

## 1. Kernkonzept: Lückenloses Tiling per binärem Teilungsbaum

Anders als klassische Raster-Launcher arbeitet Canopy ohne feste Rasterzellen, ohne leere Lücken und ohne frei schwebende Icons:

- Jede reguläre Seite besteht aus einem **binären Teilungsbaum (`TreeNode = SplitNode | LeafNode`)**.
- Jeder `SplitNode` teilt seinen verfügbaren Bereich **horizontal** (links/rechts) oder **vertikal** (oben/unten) in einem definierten Verhältnis `ratio ∈ (0, 1)` unter Berücksichtigung einer einstellbaren Fugenbreite (`gapUse = 6 dp`, `gapEdit = 12 dp`).
- Jedes `LeafNode` entspricht genau einer Kachel (`TileData`) und füllt sein zugewiesenes Rechteck vollständig aus.
- Zwischen den Kacheln schimmert in den Fugen (`6 dp` bzw. `12 dp`) das Hintergrundbild mit **15 % horizontaler Parallaxe** durch.
- Einzige strukturell abweichende Ausnahme ist die ganz rechts liegende Seite **„Alle Apps“** (mit vertikaler Scroll-Box und A–Z-Schnellscroller).

---

## 2. Layout-Mathematik, Constraints & Einrasten

Referenz-Implementierung: [`mockups/web-prototype/src/layout/tree.ts`](../mockups/web-prototype/src/layout/tree.ts), [`types.ts`](../mockups/web-prototype/src/layout/types.ts), [`tree.test.ts`](../mockups/web-prototype/src/layout/tree.test.ts).

### 2.1 Mindestgröße & Gummiband
- **`MIN_TILE_SIZE = 56 dp`** in Breite und Höhe für jeden Teilbaum (`computeMinSizeSubtree`).
- Beim Überschreiten der Grenzen greift ein Gummiband-Widerstand mit Faktor **`0.30`** (`clampOrRubberbandRatio`).

### 2.2 Geschwindigkeitsprojiziertes Einrasten (`projectAndSnapRatio`)
- Einrastpunkte: **`¼ (0.25), ⅓ (0.333…), ½ (0.5), ⅔ (0.666…), ¾ (0.75)`**.
- Beim Loslassen einer Trennlinie entscheidet die projizierte Position:
  `projected = currentRatio + (velocityPxPerSec * 0.12) / parentSpan`
- Es wird auf den gültigen Punkt innerhalb `[minRatio, maxRatio]` eingerastet, begleitet von einem Feder-Überschwinger und `snap`-Haptik.

### 2.3 2D-Kreuzungspunkte (`ComputedIntersection`)
- Wenn eine horizontale und vertikale Trennlinie direkt aneinanderstoßen (z. B. 2×2-Raster oder T-Kreuzung), erkennt `computeLayout` einen gemeinsamen Kreuzungspunkt, an dem sich beide Dimensionen simultan ziehen lassen.

### 2.4 Adaptive Kachel-Größenstufen & Hysterese (`computeSizeTier`)
Jede Kachel passt Inhalt, Typografie und Informationsdichte live an ihre Pixelgröße an – mit einem **`±4 dp` (`8 dp` Gesamtband) Hysterese-Band** gegen Flackern:

| Stufe | Bedingung (nominal) | Darstellung |
|---|---|---|
| `XS` | `min(w, h) < 80 dp` | Nur Icon / Minimalwert + Badge |
| `TICKER` | `h < 86 dp` & `w >= 175 dp` | Flache horizontale Ticker-Zeile |
| `LIST` | `w < 100 dp` & `h >= 185 dp` | Schmale vertikale Streifen-/Listenansicht |
| `S` | `min(w, h) < 130 dp` oder `Fläche < 18.000` | Kompakt: Icon + Hauptwert |
| `M` | `min(w, h) < 195 dp` oder `Fläche < 36.500` | Ausgewogen: Icon, Wert, Titel, Zusatzzeile |
| `L` | Größer | Vollansicht mit Listen, Graphen, Steuerung oder Cover |

---

## 3. Seiten-Architektur & Kachel-Ökosystem

Referenz: [`mockups/web-prototype/src/data/mockData.ts`](../mockups/web-prototype/src/data/mockData.ts), [`src/tiles/`](../mockups/web-prototype/src/tiles/).

1. **Heute (`1/5`):** Begrüßung (`Guten Morgen` + Wetter-Pill) & Temperaturverlauf | Kalender-Tagesplan (mit roter 09:01-Uhrzeitlinie) | Erinnerungen (mit `+` Schnelleintrag & Häkchen) & Akku/Schritte-Ringe | optional 4. Reihe (`+ Termin`, `+ Notiz`, `Wecker`, `Fokus`).
2. **Start (`2/5`, Heimatseite):** Uhr (mit direktem Wecker-Toggle) & Wetter | Kalender & Live-Medienplayer (mit 4-Balken-Equalizer & Seekbar) | Bento App-Gruppe `Favoriten` | optional 4. Reihe (`WLAN`, `BT`, `Licht`, `Fokus`).
3. **Medien (`3/5`):** Großes Cover-Medien-Tile (`52 %`) | Fremd-Widget `Podcast` (mit Passepartout-Rahmen) & App-Gruppe `Lesen` | Fotos-Erinnerungen (Ken-Burns + Szenenwechsel `1/3`) | optional 4. Reihe (`Audio`, `Warm`, `Timer`, `Podcast`).
4. **Apps (`4/5`):** 3 ausgewogene Bänder (`Werkzeuge`, `Social` | `Spiele`, `Medien` | Fremd-Widget `Fitness`) inkl. `+N Alle`-Kapsel | optional 4. Reihe (`Suche`, `QR-Scan`, `Rechner`, `Kamera`).
5. **Alle Apps (`5/5`, immer ganz rechts):** „Neu installiert“-Leiste | vertikale Box aller Apps (~60 Einträge) mit Sortier-Chip (`A–Z`, `Kategorie`, `Farbe`, `Nutzung`) und A–Z-Schnellscroller | **unten** breite Suchleiste für direkte Einhand-Bedienung.

### Optionale 4. Reihe (`ShortcutBarMode`)
Der Nutzer wählt zwischen `Alle` (auf jeder Seite 1–4 kontextspezifisch), `Start` (nur Startseite) oder `Aus` (`syncShortcutBars`).

### Bottom-Dock
Dreiteilige untere Navigationsleiste (28 px Höhe):
- **Links:** Direktsuche-Kapsel (`Suche öffnen`).
- **Mitte:** Interaktive `PageIndicator`-Pille mit Flüssig-Übergängen (Tippen oder Streichen zum Seitenwechsel; Gedrückthalten öffnet die Seitenübersicht).
- **Rechts:** Schnelleinstellungen-Kapsel (WLAN, Bluetooth, Taschenlampe, Fokus, Drehen, Helligkeit + Benachrichtigungs-Center).

---

## 4. Feder-Physik & Haptik-Tokens

Referenz: [`mockups/web-prototype/src/motion/springs.ts`](../mockups/web-prototype/src/motion/springs.ts), [`haptics.ts`](../mockups/web-prototype/src/motion/haptics.ts).

| Rolle | Stiffness | Dämpfungsverhältnis (`zeta`) | Einsatz |
|---|---|---|---|
| `dividerSnap` | `400` | `0.75` | Einrasten gezogener Trennlinien |
| `pageSwipe` | `300` | `0.85` | Horizontales Seiten-Einrasten |
| `tileSwap` | `380` | `0.70` | Kacheltausch & Drehung der Teilung |
| `gapOpen` | `500` | `0.80` | Öffnen/Schließen der Edit-Fugen & Menüs |
| `pressFeedback` | `1500` | `0.60` | Taktiles Antippen (Skalierung `0.96`) |
| `groupExpand` | `260` | `0.85` | Shared-Element App-/Gruppen-Expansion |

Zusätzlich existieren Stimmungs-Profile (`verspielt`, `normal`, `ruhig`) und `0.25x Slow-Motion` im Entwicklermenü.

**Haptik-Muster (`navigator.vibrate` + visueller HUD-Tick):**
- `tick` (`8 ms`), `press` (`10 ms`), `pageSettle` (`12 ms`), `snap` (`18 ms`), `editEnter` (`[20, 30, 32] ms`), `remove` (`[15, 25, 15] ms`), `error` (`[25, 25, 25] ms`).

---

## 5. Stilvarianten & Material-You-Farbwelten

Referenz: [`mockups/web-prototype/src/theme/themes.ts`](../mockups/web-prototype/src/theme/themes.ts), [`Wallpaper.tsx`](../mockups/web-prototype/src/theme/Wallpaper.tsx).

- **3 Stilvarianten (jeweils Hell & Dunkel):** `Material You` (tonale Wärme), `Dunkel & kräftig` (hoher Kontrast & große Zahlen), `Glas` (Transluzenz mit `backdrop-filter: blur(20px)`).
- **4 Farbwelten (`WallpaperPresetId`):** `Sand & Bernstein`, `Nordlicht`, `Kobalt`, `Terrakotta` – steuern sowohl das Parallax-Wallpaper als auch die Material-You-Akzente (`getThemeTokens`).
- **Tonale Icons:** Ruhige einheitliche App-Squircles (`tonalIcons: true` Standard) optional umschaltbar auf bunte Original-Appfarben.
