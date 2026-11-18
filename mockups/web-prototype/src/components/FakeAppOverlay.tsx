import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowLeft,
  Compass,
  ExternalLink,
  Globe,
  Info,
  Search,
  Sparkles,
} from 'lucide-react';
import { APPS_BY_ID, CalendarEvent } from '../data/mockData';
import { TileData } from '../layout/types';
import { getSpringTransition, MotionSettings } from '../motion/springs';
import { triggerHaptic } from '../motion/haptics';
import { ThemeTokens } from '../theme/themes';
import { TileContentProps } from '../tiles/tileProps';
import { AppSquircle } from './AppIcon';
import {
  AppViewStyleContext,
  CalculatorAppView,
  CalendarAppView,
  CameraAppView,
  ClockAppView,
  FallbackAppView,
  LauncherSettingsBridge,
  MediaAppView,
  MessagesAppView,
  NotesAppView,
  PhoneAppView,
  SettingsAppView,
  WeatherAppView,
} from './FakeAppViews';

export interface FakeAppTarget {
  kind: 'app' | 'tile';
  appId?: string;
  tile?: TileData;
  sourceRect?: { x: number; y: number; width: number; height: number };
  showInfoOnly?: boolean;
  webQuery?: string;
  shortcutAction?: string;
}

interface WebResultItem {
  id: string;
  domain: string;
  url: string;
  title: string;
  snippet: string;
  badge: string;
}

function buildMockWebSearch(rawQuery: string): {
  summaryLabel: string;
  summaryValue: string;
  summaryDetail: string;
  results: WebResultItem[];
} {
  const q = rawQuery.trim();
  const ql = q.toLowerCase();

  if (ql.includes('wetter') || ql.includes('regen') || ql.includes('temp')) {
    return {
      summaryLabel: 'Direktantwort · Wettervorhersage',
      summaryValue: '7°C · Leicht bewölkt (Regen ab 17:00 Uhr)',
      summaryDetail: 'Gefühlt 5°C · Wind 14 km/h W · Regenwahrscheinlichkeit 65 % am Abend',
      results: [
        {
          id: 'w-1',
          domain: 'wetterdienst.de',
          url: `wetterdienst.de/suche?q=${encodeURIComponent(q)}`,
          title: `Wetter aktuell & 7-Tage-Trend: ${q}`,
          snippet: 'Stündliche Temperaturen, Regenradar und Unwetterwarnungen im kompakten Überblick.',
          badge: 'Aktuell',
        },
        {
          id: 'w-2',
          domain: 'kachelos-radar.de',
          url: 'kachelos-radar.de/niederschlag',
          title: 'Regenradar Live · Niederschlagsfilm Deutschland',
          snippet: 'Die nächste Schauerfront erreicht die Region gegen 17:00 Uhr mit leichtem bis mäßigem Regen.',
          badge: 'Radar',
        },
        {
          id: 'w-3',
          domain: 'klima-atlas.de',
          url: 'klima-atlas.de/tagesverlauf',
          title: 'Sonnenuntergang, UV-Index & Luftqualität heute',
          snippet: 'Luftqualität: Gut (AQI 22) · Sonnenuntergang um 16:12 Uhr.',
          badge: 'Daten',
        },
      ],
    };
  }

  return {
    summaryLabel: 'Direktantwort · Web-Überblick',
    summaryValue: q,
    summaryDetail: `Kurze Zusammenfassung und aktuelle Treffer zu „${q}“ im mobilen KachelOS-Browser.`,
    results: [
      {
        id: 'g-1',
        domain: 'wissen.de',
        url: `wissen.de/artikel/${encodeURIComponent(ql.replace(/\s+/g, '-'))}`,
        title: `${q} – Überblick, Fakten & aktuelle Einordnung`,
        snippet: `Kompakter Leitfaden zu ${q} mit Hintergründen, technischen Details und häufigen Fragen.`,
        badge: 'Lexikon',
      },
      {
        id: 'g-2',
        domain: 'nachrichten-kompakt.de',
        url: `nachrichten-kompakt.de/thema/${encodeURIComponent(ql.replace(/\s+/g, '-'))}`,
        title: `Aktuelle Meldungen & Artikel zu „${q}“`,
        snippet: `Die wichtigsten Updates und Analysen rund um ${q}, übersichtlich für unterwegs aufbereitet.`,
        badge: 'Vor 14 Min.',
      },
      {
        id: 'g-3',
        domain: 'tipps-mobil.de',
        url: `tipps-mobil.de/suche?q=${encodeURIComponent(q)}`,
        title: `${q}: Praxis-Tipps, Shortcuts & Anleitungen`,
        snippet: `So nutzt du Funktionen und Werkzeuge rund um ${q} direkt effizienter auf Android.`,
        badge: 'Ratgeber',
      },
    ],
  };
}

interface FakeAppOverlayProps {
  target: FakeAppTarget;
  containerRect: { width: number; height: number };
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  calendarEvents: CalendarEvent[];
  onAddCalendarEvent: (ev: CalendarEvent) => void;
  mediaState: TileContentProps['mediaState'];
  launcherSettings: LauncherSettingsBridge;
  onClose: () => void;
}

export const FakeAppOverlay: React.FC<FakeAppOverlayProps> = ({
  target,
  containerRect,
  theme,
  motionSettings,
  calendarEvents,
  onAddCalendarEvent,
  mediaState,
  launcherSettings,
  onClose,
}) => {
  const app = target.appId ? APPS_BY_ID[target.appId] : undefined;
  const isBrowserMode =
    !target.showInfoOnly && (target.webQuery !== undefined || app?.id === 'app-browser');
  const title =
    target.webQuery !== undefined
      ? 'Netz · Web-Suche'
      : app?.name ?? target.tile?.title ?? 'App';
  const hue = app?.hue ?? target.tile?.accentHue ?? 218;

  const [browserQuery, setBrowserQuery] = useState(target.webQuery ?? '');
  const [browserInput, setBrowserInput] = useState(target.webQuery ?? '');
  const [openedResult, setOpenedResult] = useState<WebResultItem | null>(null);

  useEffect(() => {
    const q = target.webQuery ?? '';
    setBrowserQuery(q);
    setBrowserInput(q);
    setOpenedResult(null);
  }, [target.webQuery]);

  const subtitle =
    isBrowserMode && browserQuery.trim()
      ? `Suche: „${browserQuery.trim()}“`
      : target.shortcutAction
        ? `Aktion: ${target.shortcutAction}`
        : app?.subtitle ?? 'System-Kachel geöffnet';

  const initialClip = target.sourceRect
    ? {
        top: target.sourceRect.y,
        left: target.sourceRect.x,
        width: Math.max(48, target.sourceRect.width),
        height: Math.max(48, target.sourceRect.height),
        borderRadius: theme.tileRadius,
      }
    : {
        top: containerRect.height * 0.35,
        left: containerRect.width * 0.2,
        width: containerRect.width * 0.6,
        height: 160,
        borderRadius: theme.tileRadius,
      };

  const spring = getSpringTransition('groupExpand', motionSettings);
  const bg = theme.mode === 'light' && theme.id !== 'glass' ? '#F8FAFC' : '#0F131C';
  const fg = theme.mode === 'light' && theme.id !== 'glass' ? '#111827' : '#F8FAFC';
  const muted = theme.mode === 'light' && theme.id !== 'glass' ? '#4B5563' : '#94A3B8';
  const cardBg = theme.mode === 'light' && theme.id !== 'glass' ? '#FFFFFF' : 'rgba(255,255,255,0.07)';
  const accentColor = `hsl(${hue} 78% 52%)`;
  const accentBg = `hsla(${hue} 78% 52% / 0.16)`;
  const borderColor = theme.mode === 'light' && theme.id !== 'glass'
    ? 'rgba(15, 23, 42, 0.12)'
    : 'rgba(255, 255, 255, 0.12)';

  const viewCtx: AppViewStyleContext = {
    hue,
    fg,
    muted,
    cardBg,
    accentBg,
    accentColor,
    borderColor,
    hapticsEnabled: motionSettings.hapticsEnabled,
  };

  const activeWebQuery = browserQuery.trim();
  const webData = activeWebQuery ? buildMockWebSearch(activeWebQuery) : null;

  const submitBrowserSearch = (nextQuery: string) => {
    const cleaned = nextQuery.trim();
    triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
    setBrowserQuery(cleaned);
    setBrowserInput(cleaned);
    setOpenedResult(null);
  };

  return (
    <motion.div
      initial={{ ...initialClip, opacity: 0.7 }}
      animate={{
        top: 0,
        left: 0,
        width: containerRect.width,
        height: containerRect.height,
        borderRadius: 0,
        opacity: 1,
      }}
      exit={{ ...initialClip, opacity: 0 }}
      transition={spring}
      style={{
        position: 'absolute',
        zIndex: 80,
        background: bg,
        color: fg,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
      onPointerDown={(e) => e.stopPropagation()}
    >
      {/* Top Hero Header */}
      <div
        style={{
          padding: '44px 18px 20px',
          background: `linear-gradient(155deg, hsl(${hue} 70% 45%), hsl(${(hue + 35) % 360} 65% 28%))`,
          color: '#FFFFFF',
          display: 'flex',
          flexDirection: 'column',
          gap: 16,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <button
            type="button"
            onClick={() => {
              triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
              onClose();
            }}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              padding: '8px 12px',
              borderRadius: 99,
              border: 'none',
              background: 'rgba(255,255,255,0.2)',
              color: '#FFFFFF',
              fontSize: 13,
              fontWeight: 600,
              cursor: 'pointer',
              minHeight: 44,
            }}
          >
            <ArrowLeft size={17} />
            <span>Zurück</span>
          </button>
          <span style={{ fontSize: 12, opacity: 0.85 }}>
            {target.showInfoOnly ? 'App-Info' : 'Aktiv'}
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          {app ? (
            <AppSquircle appId={app.id} iconKey={app.iconKey} color={app.color} size={56} badge={app.badge} />
          ) : (
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: 16,
                background: 'rgba(255,255,255,0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 22,
                fontWeight: 700,
              }}
            >
              {title[0]}
            </div>
          )}
          <div>
            <div style={{ fontSize: 24, fontWeight: 700, lineHeight: 1.1 }}>{title}</div>
            <div style={{ fontSize: 13, opacity: 0.86, marginTop: 4 }}>{subtitle}</div>
          </div>
        </div>
      </div>

      {/* Interactive App Content */}
      <div style={{ flex: 1, padding: 18, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {target.showInfoOnly ? (
          <>
            <div style={{ padding: 16, borderRadius: 18, background: cardBg, display: 'flex', gap: 12, alignItems: 'center' }}>
              <Info size={20} color={`hsl(${hue} 75% 58%)`} />
              <div>
                <div style={{ fontSize: 14, fontWeight: 700 }}>Version 4.2.0 · KachelOS</div>
                <div style={{ fontSize: 12, color: muted, marginTop: 2 }}>
                  Speicher: 48,4 MB · Berechtigungen: Mitteilungen, Standort
                </div>
              </div>
            </div>
            <div style={{ padding: 16, borderRadius: 18, background: cardBg }}>
              <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>Kachel-Anpassung</div>
              <div style={{ fontSize: 12, color: muted, lineHeight: 1.5 }}>
                Diese App passt sich im Tiling-Grid automatisch an XS-, S-, M- und L-Flächen an.
              </div>
            </div>
          </>
        ) : isBrowserMode ? (
          <>
            {/* Interactive Browser Address / Search Bar */}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                submitBrowserSearch(browserInput);
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '6px 10px 6px 12px',
                borderRadius: 16,
                background: cardBg,
                border: `1px solid ${theme.mode === 'light' && theme.id !== 'glass' ? 'rgba(15,23,42,0.12)' : 'rgba(255,255,255,0.12)'}`,
              }}
            >
              <Globe size={16} color={`hsl(${hue} 78% 54%)`} style={{ flexShrink: 0 }} />
              <input
                value={browserInput}
                onChange={(e) => setBrowserInput(e.target.value)}
                enterKeyHint="search"
                placeholder="Web durchsuchen oder URL eingeben…"
                style={{
                  flex: 1,
                  minWidth: 0,
                  border: 'none',
                  outline: 'none',
                  background: 'transparent',
                  color: fg,
                  fontSize: 13.5,
                  fontWeight: 500,
                  minHeight: 32,
                }}
              />
              <button
                type="submit"
                aria-label="Im Browser suchen"
                style={{
                  height: 30,
                  padding: '0 10px',
                  borderRadius: 10,
                  border: 'none',
                  background: `hsl(${hue} 75% 48%)`,
                  color: '#FFFFFF',
                  fontSize: 12,
                  fontWeight: 600,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 4,
                  cursor: 'pointer',
                  flexShrink: 0,
                }}
              >
                <Search size={13} />
                <span>Los</span>
              </button>
            </form>

            {openedResult ? (
              <div
                style={{
                  padding: 16,
                  borderRadius: 20,
                  background: cardBg,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 10,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontSize: 11.5, color: `hsl(${hue} 80% 56%)`, fontWeight: 700 }}>
                    {openedResult.domain}
                  </span>
                  <button
                    type="button"
                    onClick={() => {
                      triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                      setOpenedResult(null);
                    }}
                    style={{
                      padding: '4px 10px',
                      borderRadius: 99,
                      border: 'none',
                      background: 'rgba(125,125,145,0.18)',
                      color: fg,
                      fontSize: 11.5,
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    Alle Treffer
                  </button>
                </div>
                <div style={{ fontSize: 17, fontWeight: 700, lineHeight: 1.25 }}>
                  {openedResult.title}
                </div>
                <div style={{ fontSize: 13, color: muted, lineHeight: 1.55 }}>
                  {openedResult.snippet}
                </div>
                <div
                  style={{
                    marginTop: 4,
                    padding: 12,
                    borderRadius: 14,
                    background: 'rgba(125,125,145,0.1)',
                    fontSize: 12,
                    color: muted,
                    lineHeight: 1.5,
                  }}
                >
                  Leseansicht im KachelOS-Browser · Quelle:{' '}
                  <span style={{ color: fg, fontWeight: 600 }}>{openedResult.url}</span>
                </div>
              </div>
            ) : webData ? (
              <>
                <div
                  style={{
                    fontSize: 11.5,
                    color: muted,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                    paddingLeft: 2,
                  }}
                >
                  <Compass size={13} color={`hsl(${hue} 78% 54%)`} />
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    suche.kachelos.de/?q={encodeURIComponent(activeWebQuery)}
                  </span>
                </div>

                {/* Direktantwort */}
                <div
                  style={{
                    padding: 15,
                    borderRadius: 20,
                    background: cardBg,
                    borderLeft: `4px solid hsl(${hue} 78% 54%)`,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 6,
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 6,
                      fontSize: 11.5,
                      fontWeight: 700,
                      color: `hsl(${hue} 80% 56%)`,
                    }}
                  >
                    <Sparkles size={13} />
                    <span>{webData.summaryLabel}</span>
                  </div>
                  <div style={{ fontSize: 16, fontWeight: 700, lineHeight: 1.2 }}>
                    {webData.summaryValue}
                  </div>
                  <div style={{ fontSize: 12.5, color: muted, lineHeight: 1.45 }}>
                    {webData.summaryDetail}
                  </div>
                </div>

                {/* Web-Ergebnisse */}
                {webData.results.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => {
                      triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                      setOpenedResult(item);
                    }}
                    style={{
                      padding: 14,
                      borderRadius: 18,
                      background: cardBg,
                      border: 'none',
                      color: fg,
                      textAlign: 'left',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 5,
                      cursor: 'pointer',
                    }}
                  >
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        width: '100%',
                        gap: 8,
                      }}
                    >
                      <span
                        style={{
                          fontSize: 11.5,
                          color: `hsl(${hue} 80% 56%)`,
                          fontWeight: 600,
                          display: 'flex',
                          alignItems: 'center',
                          gap: 5,
                        }}
                      >
                        <ExternalLink size={12} />
                        {item.domain}
                      </span>
                      <span
                        style={{
                          fontSize: 10.5,
                          color: muted,
                          background: 'rgba(125,125,145,0.14)',
                          padding: '2px 7px',
                          borderRadius: 99,
                        }}
                      >
                        {item.badge}
                      </span>
                    </div>
                    <div style={{ fontSize: 14, fontWeight: 700, lineHeight: 1.25 }}>
                      {item.title}
                    </div>
                    <div style={{ fontSize: 12, color: muted, lineHeight: 1.4 }}>
                      {item.snippet}
                    </div>
                  </button>
                ))}
              </>
            ) : (
              <div
                style={{
                  padding: 16,
                  borderRadius: 20,
                  background: cardBg,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 12,
                }}
              >
                <div style={{ fontSize: 14, fontWeight: 700 }}>Schnell-Suche starten</div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {['Wetter Berlin', 'KachelOS Tipps', 'Tagesschau', 'Bahn Fahrplan'].map((chip) => (
                    <button
                      key={chip}
                      type="button"
                      onClick={() => submitBrowserSearch(chip)}
                      style={{
                        padding: '8px 12px',
                        borderRadius: 99,
                        border: 'none',
                        background: 'rgba(125,125,145,0.16)',
                        color: fg,
                        fontSize: 12.5,
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      {chip}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </>
        ) : target.tile?.type === 'weather' || target.tile?.type === 'temp-curve' || target.tile?.type === 'greeting' || app?.id === 'app-wetter' ? (
          <WeatherAppView ctx={viewCtx} shortcutAction={target.shortcutAction} />
        ) : target.tile?.type === 'calendar' || target.tile?.type === 'calendar-timeline' || app?.id === 'app-kalender' ? (
          <CalendarAppView
            ctx={viewCtx}
            events={calendarEvents}
            onAddEvent={onAddCalendarEvent}
            shortcutAction={target.shortcutAction}
          />
        ) : app?.id === 'app-nachrichten' || app?.id === 'app-mail' || app?.id === 'app-funk' || app?.id === 'app-forum' ? (
          <MessagesAppView ctx={viewCtx} appId={app.id} />
        ) : target.tile?.type === 'clock' || app?.id === 'app-uhr' ? (
          <ClockAppView ctx={viewCtx} shortcutAction={target.shortcutAction} />
        ) : app?.id === 'app-rechner' ? (
          <CalculatorAppView ctx={viewCtx} />
        ) : app?.id === 'app-telefon' ? (
          <PhoneAppView ctx={viewCtx} />
        ) : target.tile?.type === 'note' || target.tile?.type === 'reminders' || app?.id === 'app-notizen' ? (
          <NotesAppView ctx={viewCtx} shortcutAction={target.shortcutAction} />
        ) : target.tile?.type === 'photos' || app?.id === 'app-kamera' || app?.id === 'app-fotos' ? (
          <CameraAppView ctx={viewCtx} />
        ) : target.tile?.type === 'media' || app?.id === 'app-musik' || app?.id === 'app-podcast' ? (
          <MediaAppView ctx={viewCtx} mediaState={mediaState} />
        ) : app?.id === 'app-einstellungen' ? (
          <SettingsAppView
            ctx={viewCtx}
            settings={launcherSettings}
            motionSettings={motionSettings}
          />
        ) : (
          <FallbackAppView ctx={viewCtx} appTitle={title} />
        )}
      </div>
    </motion.div>
  );
};
