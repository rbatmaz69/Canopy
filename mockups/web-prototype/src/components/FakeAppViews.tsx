import React, { useEffect, useState } from 'react';
import {
  AlarmClock,
  Camera,
  Check,
  CheckCircle2,
  CloudRain,
  Delete,
  Music2,
  Pause,
  Phone,
  PhoneOff,
  Play,
  Plus,
  RotateCcw,
  Send,
  SkipBack,
  SkipForward,
  Sun,
  Trash2,
} from 'lucide-react';
import {
  CalendarEvent,
  CONTACTS_MOCK,
  MEDIA_TRACKS,
  ShortcutBarMode,
  WEATHER_HOURS,
} from '../data/mockData';
import { triggerHaptic } from '../motion/haptics';
import { MotionSettings } from '../motion/springs';
import {
  ColorMode,
  ThemeVariantId,
  WALLPAPER_PRESETS,
  WallpaperPresetId,
} from '../theme/themes';
import { TileContentProps } from '../tiles/tileProps';

export interface AppViewStyleContext {
  hue: number;
  fg: string;
  muted: string;
  cardBg: string;
  accentBg: string;
  accentColor: string;
  borderColor: string;
  hapticsEnabled: boolean;
}

export interface LauncherSettingsBridge {
  themeId: ThemeVariantId;
  colorMode: ColorMode;
  wallpaperId: WallpaperPresetId;
  tonalIcons: boolean;
  shortcutBarMode: ShortcutBarMode;
  onChangeThemeId: (id: ThemeVariantId) => void;
  onChangeColorMode: (m: ColorMode) => void;
  onChangeWallpaperId: (w: WallpaperPresetId) => void;
  onChangeTonalIcons: (v: boolean) => void;
  onChangeShortcutBarMode: (m: ShortcutBarMode) => void;
}

/* ---------------- Weather View ---------------- */
const WEATHER_CITIES = [
  { name: 'Berlin', temp: '7°', summary: 'Regen ab 17 Uhr' },
  { name: 'Hamburg', temp: '9°', summary: 'Frischer Wind · Bewölkt' },
  { name: 'München', temp: '11°', summary: 'Heiter · 15 % Regen' },
];

export const WeatherAppView: React.FC<{
  ctx: AppViewStyleContext;
  shortcutAction?: string;
}> = ({ ctx, shortcutAction }) => {
  const [cityIdx, setCityIdx] = useState(shortcutAction === 'Standort wechseln' ? 1 : 0);
  const city = WEATHER_CITIES[cityIdx];

  return (
    <>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {WEATHER_CITIES.map((c, idx) => (
          <button
            key={c.name}
            type="button"
            onClick={() => {
              triggerHaptic('tick', { enabled: ctx.hapticsEnabled });
              setCityIdx(idx);
            }}
            style={{
              padding: '6px 12px',
              borderRadius: 99,
              border: 'none',
              background: idx === cityIdx ? ctx.accentColor : ctx.cardBg,
              color: idx === cityIdx ? '#FFFFFF' : ctx.fg,
              fontSize: 12,
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            {c.name}
          </button>
        ))}
      </div>

      <div
        style={{
          padding: 18,
          borderRadius: 20,
          background: ctx.cardBg,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <div>
          <div className="display-num" style={{ fontSize: 48, fontWeight: 700, lineHeight: 1 }}>
            {city.temp}
          </div>
          <div style={{ fontSize: 14, color: ctx.muted, marginTop: 6 }}>
            {city.name} · {city.summary}
          </div>
        </div>
        {cityIdx === 2 ? (
          <Sun size={46} color={ctx.accentColor} />
        ) : (
          <CloudRain size={46} color={ctx.accentColor} />
        )}
      </div>

      <div style={{ padding: 16, borderRadius: 20, background: ctx.cardBg }}>
        <div style={{ fontSize: 14, fontWeight: 700, marginBottom: 12 }}>Nächste Stunden</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
          {WEATHER_HOURS.map((h) => (
            <div
              key={h.hour}
              style={{
                padding: 10,
                borderRadius: 14,
                background: 'rgba(125,125,145,0.1)',
                textAlign: 'center',
              }}
            >
              <div style={{ fontSize: 12, color: ctx.muted }}>{h.hour}</div>
              <div className="display-num" style={{ fontSize: 18, fontWeight: 700, marginTop: 4 }}>
                {h.temp}
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
};

/* ---------------- Calendar View ---------------- */
export const CalendarAppView: React.FC<{
  ctx: AppViewStyleContext;
  events: CalendarEvent[];
  onAddEvent: (ev: CalendarEvent) => void;
  shortcutAction?: string;
}> = ({ ctx, events, onAddEvent, shortcutAction }) => {
  const [showForm, setShowForm] = useState(shortcutAction === 'Neuer Termin');
  const [evTitle, setEvTitle] = useState('');
  const [evTime, setEvTime] = useState('16:30');
  const [evLoc, setEvLoc] = useState('Büro / Remote');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanedTitle = evTitle.trim();
    if (!cleanedTitle) return;
    triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
    const parsedHour = Number(evTime.split(':')[0]) || 16;
    onAddEvent({
      id: `ev-${Date.now()}`,
      title: cleanedTitle,
      time: evTime || '16:30',
      endTime: `${String(Math.min(23, parsedHour + 1)).padStart(2, '0')}:00`,
      location: evLoc.trim() || 'Online',
      startHour: parsedHour,
      durationHours: 1,
      hue: ctx.hue,
    });
    setEvTitle('');
    setShowForm(false);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ fontSize: 14, fontWeight: 700 }}>Mittwoch, 18. November</div>
        <button
          type="button"
          onClick={() => {
            triggerHaptic('press', { enabled: ctx.hapticsEnabled });
            setShowForm((v) => !v);
          }}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 5,
            padding: '7px 12px',
            borderRadius: 99,
            border: 'none',
            background: ctx.accentColor,
            color: '#FFFFFF',
            fontSize: 12,
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          <Plus size={14} />
          <span>Neuer Termin</span>
        </button>
      </div>

      {showForm && (
        <form
          onSubmit={handleSubmit}
          style={{
            padding: 14,
            borderRadius: 18,
            background: ctx.cardBg,
            border: `1px solid ${ctx.accentColor}`,
            display: 'flex',
            flexDirection: 'column',
            gap: 8,
          }}
        >
          <input
            autoFocus
            value={evTitle}
            onChange={(e) => setEvTitle(e.target.value)}
            placeholder="Termin-Titel (z. B. Sprint Demo)…"
            style={inputStyle(ctx)}
          />
          <div style={{ display: 'grid', gridTemplateColumns: '96px 1fr', gap: 8 }}>
            <input
              value={evTime}
              onChange={(e) => setEvTime(e.target.value)}
              placeholder="16:30"
              style={inputStyle(ctx)}
            />
            <input
              value={evLoc}
              onChange={(e) => setEvLoc(e.target.value)}
              placeholder="Ort"
              style={inputStyle(ctx)}
            />
          </div>
          <button
            type="submit"
            style={{
              height: 38,
              borderRadius: 12,
              border: 'none',
              background: ctx.accentColor,
              color: '#FFFFFF',
              fontSize: 13,
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            Termin speichern
          </button>
        </form>
      )}

      {events.map((ev) => (
        <div
          key={ev.id}
          style={{
            padding: 15,
            borderRadius: 18,
            background: ctx.cardBg,
            borderLeft: `4px solid hsl(${ev.hue} 80% 58%)`,
          }}
        >
          <div style={{ fontSize: 16, fontWeight: 700 }}>{ev.title}</div>
          <div className="display-num" style={{ fontSize: 13, color: ctx.muted, marginTop: 4 }}>
            {ev.time} – {ev.endTime} · {ev.location}
          </div>
        </div>
      ))}
    </div>
  );
};

/* ---------------- Messages & Mail View ---------------- */
interface ChatThread {
  id: string;
  sender: string;
  tag: string;
  messages: { fromMe: boolean; text: string; time: string }[];
}

const INITIAL_THREADS: ChatThread[] = [
  {
    id: 't-mara',
    sender: 'Mara Weber',
    tag: 'Design Lead',
    messages: [
      { fromMe: false, text: 'Kannst du die neuen Fugen im Layout kurz prüfen?', time: '08:52' },
      { fromMe: true, text: 'Sieht super aus – die Kacheln rasten richtig knackig ein!', time: '08:56' },
      { fromMe: false, text: 'Perfekt! Bis 12:30 beim Design Kritik Termin.', time: '08:58' },
    ],
  },
  {
    id: 't-release',
    sender: 'Release Team',
    tag: 'Postfach · Wichtig',
    messages: [
      { fromMe: false, text: 'Build #408 wurde erfolgreich für das Testgerät freigegeben.', time: '08:40' },
    ],
  },
  {
    id: 't-jonas',
    sender: 'Jonas K.',
    tag: 'Backend Eng',
    messages: [
      { fromMe: false, text: 'Sollen wir nachher zusammen Kaffee holen?', time: '08:15' },
    ],
  },
];

export const MessagesAppView: React.FC<{
  ctx: AppViewStyleContext;
  appId: string;
}> = ({ ctx, appId }) => {
  const [threads, setThreads] = useState<ChatThread[]>(INITIAL_THREADS);
  const [activeId, setActiveId] = useState(appId === 'app-mail' ? 't-release' : 't-mara');
  const [draft, setDraft] = useState('');

  const activeThread = threads.find((t) => t.id === activeId) ?? threads[0];

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = draft.trim();
    if (!cleaned) return;
    triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
    setThreads((prev) =>
      prev.map((t) =>
        t.id === activeThread.id
          ? { ...t, messages: [...t.messages, { fromMe: true, text: cleaned, time: 'Jetzt' }] }
          : t
      )
    );
    setDraft('');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, flex: 1 }}>
      <div style={{ display: 'flex', gap: 6, overflowX: 'auto', paddingBottom: 2 }}>
        {threads.map((t) => {
          const active = t.id === activeThread.id;
          return (
            <button
              key={t.id}
              type="button"
              onClick={() => {
                triggerHaptic('tick', { enabled: ctx.hapticsEnabled });
                setActiveId(t.id);
              }}
              style={{
                padding: '7px 12px',
                borderRadius: 99,
                border: 'none',
                background: active ? ctx.accentColor : ctx.cardBg,
                color: active ? '#FFFFFF' : ctx.fg,
                fontSize: 12,
                fontWeight: 700,
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              {t.sender}
            </button>
          );
        })}
      </div>

      <div
        style={{
          flex: 1,
          padding: 14,
          borderRadius: 20,
          background: ctx.cardBg,
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
        }}
      >
        <div style={{ fontSize: 11.5, color: ctx.muted, fontWeight: 600 }}>
          {activeThread.sender} · {activeThread.tag}
        </div>
        {activeThread.messages.map((m, idx) => (
          <div
            key={idx}
            style={{
              alignSelf: m.fromMe ? 'flex-end' : 'flex-start',
              maxWidth: '82%',
              padding: '9px 13px',
              borderRadius: 16,
              background: m.fromMe ? ctx.accentColor : 'rgba(125,125,145,0.16)',
              color: m.fromMe ? '#FFFFFF' : ctx.fg,
              fontSize: 13,
              lineHeight: 1.4,
            }}
          >
            <div>{m.text}</div>
            <div style={{ fontSize: 10, opacity: 0.72, marginTop: 3, textAlign: 'right' }}>
              {m.time}
            </div>
          </div>
        ))}
      </div>

      <form onSubmit={handleSend} style={{ display: 'flex', gap: 8 }}>
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Nachricht schreiben…"
          style={{ ...inputStyle(ctx), flex: 1 }}
        />
        <button
          type="submit"
          aria-label="Nachricht senden"
          style={{
            width: 44,
            height: 44,
            borderRadius: 14,
            border: 'none',
            background: ctx.accentColor,
            color: '#FFFFFF',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            flexShrink: 0,
          }}
        >
          <Send size={17} />
        </button>
      </form>
    </div>
  );
};

/* ---------------- Clock & Alarms View ---------------- */
export const ClockAppView: React.FC<{
  ctx: AppViewStyleContext;
  shortcutAction?: string;
}> = ({ ctx, shortcutAction }) => {
  const [alarms, setAlarms] = useState([
    { id: 'a1', time: '07:15', label: 'Werktags · Mo–Fr', enabled: true },
    { id: 'a2', time: '08:30', label: 'Wochenende', enabled: false },
    { id: 'a3', time: '22:45', label: 'Schlafenszeit-Erinnerung', enabled: true },
  ]);
  const [seconds, setSeconds] = useState(1500);
  const [timerRunning, setTimerRunning] = useState(shortcutAction === 'Timer 25 Min.');

  useEffect(() => {
    if (!timerRunning) return;
    const id = window.setInterval(() => setSeconds((s) => Math.max(0, s - 1)), 1000);
    return () => window.clearInterval(id);
  }, [timerRunning]);

  const mm = String(Math.floor(seconds / 60)).padStart(2, '0');
  const ss = String(seconds % 60).padStart(2, '0');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div
        style={{
          padding: 18,
          borderRadius: 20,
          background: ctx.cardBg,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <div>
          <div style={{ fontSize: 12.5, color: ctx.muted, fontWeight: 600 }}>Fokus-Timer</div>
          <div className="display-num" style={{ fontSize: 38, fontWeight: 700, marginTop: 2 }}>
            {mm}:{ss}
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            type="button"
            aria-label="Timer zurücksetzen"
            onClick={() => {
              triggerHaptic('tick', { enabled: ctx.hapticsEnabled });
              setTimerRunning(false);
              setSeconds(1500);
            }}
            style={{
              width: 44,
              height: 44,
              borderRadius: 99,
              border: 'none',
              background: 'rgba(125,125,145,0.16)',
              color: ctx.fg,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <RotateCcw size={18} />
          </button>
          <button
            type="button"
            aria-label={timerRunning ? 'Timer pausieren' : 'Timer starten'}
            onClick={() => {
              triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
              setTimerRunning((v) => !v);
            }}
            style={{
              width: 48,
              height: 48,
              borderRadius: 99,
              border: 'none',
              background: ctx.accentColor,
              color: '#FFFFFF',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            {timerRunning ? <Pause size={20} /> : <Play size={20} style={{ marginLeft: 2 }} />}
          </button>
        </div>
      </div>

      <div style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>Wecker</div>
      {alarms.map((al) => (
        <button
          key={al.id}
          type="button"
          aria-pressed={al.enabled}
          onClick={() => {
            triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
            setAlarms((prev) =>
              prev.map((x) => (x.id === al.id ? { ...x, enabled: !x.enabled } : x))
            );
          }}
          style={{
            padding: 15,
            borderRadius: 18,
            border: 'none',
            background: ctx.cardBg,
            color: ctx.fg,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            cursor: 'pointer',
            textAlign: 'left',
          }}
        >
          <div>
            <div
              className="display-num"
              style={{ fontSize: 24, fontWeight: 700, opacity: al.enabled ? 1 : 0.45 }}
            >
              {al.time}
            </div>
            <div style={{ fontSize: 12, color: ctx.muted }}>{al.label}</div>
          </div>
          <div
            style={{
              width: 44,
              height: 26,
              borderRadius: 99,
              background: al.enabled ? ctx.accentColor : 'rgba(125,125,145,0.28)',
              padding: 3,
              display: 'flex',
              alignItems: 'center',
              justifyContent: al.enabled ? 'flex-end' : 'flex-start',
            }}
          >
            <span style={{ width: 20, height: 20, borderRadius: 99, background: '#FFFFFF' }} />
          </div>
        </button>
      ))}
    </div>
  );
};

/* ---------------- Calculator View ---------------- */
function evaluateSimpleExpr(expr: string): string {
  const tokens = expr.match(/\d+(\.\d+)?|[+\-×÷]/g);
  if (!tokens || tokens.length === 0) return '0';
  const nums: number[] = [];
  const ops: string[] = [];
  for (const t of tokens) {
    if (/[+\-×÷]/.test(t)) ops.push(t);
    else nums.push(Number(t));
  }
  if (nums.length === 0) return '0';
  // Handle × and ÷ first
  for (let i = 0; i < ops.length; ) {
    if (ops[i] === '×' || ops[i] === '÷') {
      const a = nums[i];
      const b = nums[i + 1] ?? 1;
      const res = ops[i] === '×' ? a * b : b === 0 ? 0 : a / b;
      nums.splice(i, 2, res);
      ops.splice(i, 1);
    } else {
      i++;
    }
  }
  let acc = nums[0];
  for (let i = 0; i < ops.length; i++) {
    const b = nums[i + 1] ?? 0;
    acc = ops[i] === '+' ? acc + b : acc - b;
  }
  return Number.isInteger(acc) ? String(acc) : String(Number(acc.toFixed(4)));
}

export const CalculatorAppView: React.FC<{ ctx: AppViewStyleContext }> = ({ ctx }) => {
  const [expr, setExpr] = useState('42×18');
  const preview = evaluateSimpleExpr(expr);
  const keys = ['C', '÷', '×', '⌫', '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '=', '0', '.'];

  const pressKey = (k: string) => {
    triggerHaptic('tick', { enabled: ctx.hapticsEnabled });
    if (k === 'C') setExpr('0');
    else if (k === '⌫') setExpr((e) => (e.length <= 1 ? '0' : e.slice(0, -1)));
    else if (k === '=') setExpr(evaluateSimpleExpr(expr));
    else setExpr((e) => (e === '0' && /\d/.test(k) ? k : e + k));
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div
        style={{
          padding: 18,
          borderRadius: 20,
          background: ctx.cardBg,
          textAlign: 'right',
        }}
      >
        <div className="display-num" style={{ fontSize: 28, fontWeight: 700, wordBreak: 'break-all' }}>
          {expr}
        </div>
        <div className="display-num" style={{ fontSize: 16, color: ctx.accentColor, marginTop: 4 }}>
          = {preview}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
        {keys.map((k) => {
          const isEq = k === '=';
          const isOp = /[+\-×÷C⌫]/.test(k);
          return (
            <button
              key={k}
              type="button"
              onClick={() => pressKey(k)}
              style={{
                height: 52,
                gridColumn: k === '0' ? 'span 2' : k === '=' ? 'span 2' : undefined,
                borderRadius: 16,
                border: 'none',
                background: isEq ? ctx.accentColor : isOp ? ctx.accentBg : ctx.cardBg,
                color: isEq ? '#FFFFFF' : isOp ? ctx.accentColor : ctx.fg,
                fontSize: 18,
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              {k}
            </button>
          );
        })}
      </div>
    </div>
  );
};

/* ---------------- Phone Dialer View ---------------- */
export const PhoneAppView: React.FC<{ ctx: AppViewStyleContext }> = ({ ctx }) => {
  const [number, setNumber] = useState('');
  const [inCallWith, setInCallWith] = useState<string | null>(null);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {inCallWith ? (
        <div
          style={{
            padding: 24,
            borderRadius: 22,
            background: ctx.cardBg,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <div style={{ fontSize: 12, color: '#10B981', fontWeight: 700 }}>Anruf aktiv · 00:08</div>
          <div style={{ fontSize: 24, fontWeight: 700 }}>{inCallWith}</div>
          <button
            type="button"
            onClick={() => {
              triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
              setInCallWith(null);
            }}
            style={{
              width: 58,
              height: 58,
              borderRadius: 99,
              border: 'none',
              background: '#EF4444',
              color: '#FFFFFF',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              marginTop: 8,
            }}
            aria-label="Auflegen"
          >
            <PhoneOff size={24} />
          </button>
        </div>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8 }}>
            {CONTACTS_MOCK.slice(0, 4).map((c) => (
              <button
                key={c.id}
                type="button"
                onClick={() => {
                  triggerHaptic('press', { enabled: ctx.hapticsEnabled });
                  setInCallWith(c.name);
                }}
                style={{
                  padding: 10,
                  borderRadius: 16,
                  border: 'none',
                  background: ctx.cardBg,
                  color: ctx.fg,
                  textAlign: 'left',
                  cursor: 'pointer',
                }}
              >
                <div style={{ fontSize: 13, fontWeight: 700 }}>{c.name}</div>
                <div style={{ fontSize: 11, color: ctx.muted }}>{c.role}</div>
              </button>
            ))}
          </div>

          <div
            style={{
              padding: '12px 14px',
              borderRadius: 16,
              background: ctx.cardBg,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              minHeight: 48,
            }}
          >
            <span className="display-num" style={{ fontSize: 22, fontWeight: 700 }}>
              {number || 'Nummer wählen…'}
            </span>
            {number && (
              <button
                type="button"
                onClick={() => setNumber((n) => n.slice(0, -1))}
                style={{ background: 'transparent', border: 'none', color: ctx.muted, cursor: 'pointer' }}
                aria-label="Ziffer löschen"
              >
                <Delete size={18} />
              </button>
            )}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
            {['1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'].map((d) => (
              <button
                key={d}
                type="button"
                onClick={() => {
                  triggerHaptic('tick', { enabled: ctx.hapticsEnabled });
                  setNumber((n) => n + d);
                }}
                style={{
                  height: 46,
                  borderRadius: 14,
                  border: 'none',
                  background: ctx.cardBg,
                  color: ctx.fg,
                  fontSize: 18,
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                {d}
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={() => {
              triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
              setInCallWith(number || 'Favorit Anruf');
            }}
            style={{
              height: 48,
              borderRadius: 99,
              border: 'none',
              background: '#10B981',
              color: '#FFFFFF',
              fontSize: 15,
              fontWeight: 700,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              cursor: 'pointer',
            }}
          >
            <Phone size={18} />
            <span>Anrufen</span>
          </button>
        </>
      )}
    </div>
  );
};

/* ---------------- Notes & Reminders View ---------------- */
export const NotesAppView: React.FC<{
  ctx: AppViewStyleContext;
  shortcutAction?: string;
}> = ({ ctx, shortcutAction }) => {
  const [notes, setNotes] = useState(() => {
    const base = [
      { id: 'n1', text: 'Federwerte 0,75 / 400 prüfen', done: true },
      { id: 'n2', text: 'Paket in Filiale abholen', done: false },
      { id: 'n3', text: 'Kaffeebohnen kaufen', done: false },
    ];
    return shortcutAction === 'Alle abhaken' ? base.map((b) => ({ ...b, done: true })) : base;
  });
  const [draft, setDraft] = useState('');

  const addNote = (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = draft.trim();
    if (!cleaned) return;
    triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
    setNotes((prev) => [{ id: `n-${Date.now()}`, text: cleaned, done: false }, ...prev]);
    setDraft('');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <form onSubmit={addNote} style={{ display: 'flex', gap: 8 }}>
        <input
          autoFocus={shortcutAction === 'Neuer Eintrag'}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Neuer Eintrag oder Notiz…"
          style={{ ...inputStyle(ctx), flex: 1 }}
        />
        <button
          type="submit"
          aria-label="Eintrag hinzufügen"
          style={{
            width: 44,
            height: 44,
            borderRadius: 14,
            border: 'none',
            background: ctx.accentColor,
            color: '#FFFFFF',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
          }}
        >
          <Plus size={18} />
        </button>
      </form>

      {notes.map((n) => (
        <div
          key={n.id}
          style={{
            padding: 13,
            borderRadius: 16,
            background: ctx.cardBg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 10,
          }}
        >
          <button
            type="button"
            onClick={() => {
              triggerHaptic('press', { enabled: ctx.hapticsEnabled });
              setNotes((prev) => prev.map((x) => (x.id === n.id ? { ...x, done: !x.done } : x)));
            }}
            style={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              background: 'transparent',
              border: 'none',
              color: ctx.fg,
              textAlign: 'left',
              cursor: 'pointer',
              fontSize: 13.5,
              fontWeight: 600,
              textDecoration: n.done ? 'line-through' : 'none',
              opacity: n.done ? 0.6 : 1,
            }}
          >
            <CheckCircle2 size={18} color={n.done ? ctx.accentColor : ctx.muted} />
            <span>{n.text}</span>
          </button>
          <button
            type="button"
            aria-label="Eintrag löschen"
            onClick={() => setNotes((prev) => prev.filter((x) => x.id !== n.id))}
            style={{ background: 'transparent', border: 'none', color: ctx.muted, cursor: 'pointer' }}
          >
            <Trash2 size={15} />
          </button>
        </div>
      ))}
    </div>
  );
};

/* ---------------- Camera / Photos View ---------------- */
export const CameraAppView: React.FC<{ ctx: AppViewStyleContext }> = ({ ctx }) => {
  const [zoom, setZoom] = useState<'0.6×' | '1×' | '3×'>('1×');
  const [shots, setShots] = useState(14);
  const [flash, setFlash] = useState(false);

  const takeShot = () => {
    triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
    setFlash(true);
    setShots((s) => s + 1);
    window.setTimeout(() => setFlash(false), 160);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div
        style={{
          position: 'relative',
          height: 260,
          borderRadius: 22,
          overflow: 'hidden',
          background: 'linear-gradient(155deg, #0F172A, #1E293B)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          border: `1px solid ${ctx.borderColor}`,
        }}
      >
        <div
          style={{
            transform: `scale(${zoom === '0.6×' ? 0.8 : zoom === '3×' ? 1.4 : 1})`,
            transition: 'transform 220ms ease',
            width: 150,
            height: 150,
            borderRadius: 99,
            background: 'radial-gradient(circle, rgba(251,191,36,0.6), rgba(244,63,94,0.2) 70%, transparent)',
          }}
        />
        {flash && (
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(255,255,255,0.75)' }} />
        )}
        <div
          style={{
            position: 'absolute',
            bottom: 10,
            display: 'flex',
            gap: 6,
            padding: 4,
            borderRadius: 99,
            background: 'rgba(0,0,0,0.45)',
          }}
        >
          {(['0.6×', '1×', '3×'] as const).map((z) => (
            <button
              key={z}
              type="button"
              onClick={() => {
                triggerHaptic('tick', { enabled: ctx.hapticsEnabled });
                setZoom(z);
              }}
              style={{
                padding: '4px 10px',
                borderRadius: 99,
                border: 'none',
                background: zoom === z ? '#FBBF24' : 'transparent',
                color: zoom === z ? '#0F172A' : '#FFFFFF',
                fontSize: 11.5,
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              {z}
            </button>
          ))}
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 13, color: ctx.muted }}>Galerie · {shots} Aufnahmen</span>
        <button
          type="button"
          onClick={takeShot}
          style={{
            padding: '10px 18px',
            borderRadius: 99,
            border: 'none',
            background: ctx.accentColor,
            color: '#FFFFFF',
            fontSize: 13,
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            cursor: 'pointer',
          }}
        >
          <Camera size={16} />
          <span>Auslösen</span>
        </button>
      </div>
    </div>
  );
};

/* ---------------- Media / Music View ---------------- */
export const MediaAppView: React.FC<{
  ctx: AppViewStyleContext;
  mediaState: TileContentProps['mediaState'];
}> = ({ ctx, mediaState }) => {
  const track = MEDIA_TRACKS[mediaState.trackIndex % MEDIA_TRACKS.length];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div
        style={{
          padding: 18,
          borderRadius: 20,
          background: ctx.cardBg,
          display: 'flex',
          flexDirection: 'column',
          gap: 14,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 14,
              background: `linear-gradient(135deg, hsl(${track.accentHue} 75% 50%), hsl(${(track.accentHue + 40) % 360} 70% 34%))`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#FFFFFF',
            }}
          >
            <Music2 size={24} />
          </div>
          <div>
            <div style={{ fontSize: 17, fontWeight: 700 }}>{track.title}</div>
            <div style={{ fontSize: 12.5, color: ctx.muted }}>
              {track.artist} · {track.album}
            </div>
          </div>
        </div>

        <input
          type="range"
          min="0"
          max="1"
          step="0.01"
          value={mediaState.progress}
          onChange={(e) => mediaState.onSeek(Number(e.target.value))}
          aria-label="Wiedergabe-Position"
          style={{ width: '100%', accentColor: ctx.accentColor, cursor: 'pointer' }}
        />

        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 22 }}>
          <button
            type="button"
            onClick={mediaState.onPrev}
            aria-label="Vorheriger Titel"
            style={iconCircleBtn(ctx.cardBg, ctx.fg)}
          >
            <SkipBack size={20} />
          </button>
          <button
            type="button"
            onClick={mediaState.onTogglePlay}
            aria-label={mediaState.isPlaying ? 'Pause' : 'Abspielen'}
            style={{
              width: 54,
              height: 54,
              borderRadius: 99,
              border: 'none',
              background: ctx.accentColor,
              color: '#FFFFFF',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            {mediaState.isPlaying ? <Pause size={24} /> : <Play size={24} style={{ marginLeft: 2 }} />}
          </button>
          <button
            type="button"
            onClick={mediaState.onNext}
            aria-label="Nächster Titel"
            style={iconCircleBtn(ctx.cardBg, ctx.fg)}
          >
            <SkipForward size={20} />
          </button>
        </div>
      </div>

      <div style={{ fontSize: 13, fontWeight: 700 }}>Warteschlange</div>
      {MEDIA_TRACKS.map((t, idx) => (
        <div
          key={t.id}
          style={{
            padding: 12,
            borderRadius: 16,
            background: idx === mediaState.trackIndex ? ctx.accentBg : ctx.cardBg,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <div>
            <div style={{ fontSize: 13.5, fontWeight: 700 }}>{t.title}</div>
            <div style={{ fontSize: 11.5, color: ctx.muted }}>{t.artist}</div>
          </div>
          {idx === mediaState.trackIndex && (
            <span style={{ fontSize: 11, fontWeight: 700, color: ctx.accentColor }}>Läuft</span>
          )}
        </div>
      ))}
    </div>
  );
};

/* ---------------- Settings App View ---------------- */
export const SettingsAppView: React.FC<{
  ctx: AppViewStyleContext;
  settings: LauncherSettingsBridge;
  motionSettings: MotionSettings;
}> = ({ ctx, settings, motionSettings }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ padding: 14, borderRadius: 18, background: ctx.cardBg }}>
        <div style={{ fontSize: 12, fontWeight: 700, color: ctx.muted, marginBottom: 8 }}>
          STILVARIANTE
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 6 }}>
          {(
            [
              { id: 'material-you', label: 'Material' },
              { id: 'dark-bold', label: 'Kräftig' },
              { id: 'glass', label: 'Glas' },
            ] as const
          ).map((t) => {
            const active = settings.themeId === t.id;
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => {
                  triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                  settings.onChangeThemeId(t.id);
                }}
                style={{
                  padding: '8px 6px',
                  borderRadius: 12,
                  border: 'none',
                  background: active ? ctx.accentColor : 'rgba(125,125,145,0.14)',
                  color: active ? '#FFFFFF' : ctx.fg,
                  fontSize: 12,
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                {t.label}
              </button>
            );
          })}
        </div>
      </div>

      <div style={{ padding: 14, borderRadius: 18, background: ctx.cardBg }}>
        <div style={{ fontSize: 12, fontWeight: 700, color: ctx.muted, marginBottom: 8 }}>
          HINTERGRUND & FARBWELT
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8 }}>
          {WALLPAPER_PRESETS.map((wp) => {
            const active = settings.wallpaperId === wp.id;
            return (
              <button
                key={wp.id}
                type="button"
                onClick={() => {
                  triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                  settings.onChangeWallpaperId(wp.id);
                }}
                style={{
                  padding: '9px 10px',
                  borderRadius: 13,
                  border: active ? `1.5px solid ${ctx.accentColor}` : '1.5px solid transparent',
                  background: active ? ctx.accentBg : 'rgba(125,125,145,0.12)',
                  color: ctx.fg,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  fontSize: 12,
                  fontWeight: 700,
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <span
                  style={{
                    width: 14,
                    height: 14,
                    borderRadius: 99,
                    background: wp.previewColor,
                    flexShrink: 0,
                  }}
                />
                <span>{wp.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div style={{ padding: 14, borderRadius: 18, background: ctx.cardBg, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <button
          type="button"
          onClick={() => {
            triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
            settings.onChangeColorMode(settings.colorMode === 'dark' ? 'light' : 'dark');
          }}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            background: 'transparent',
            border: 'none',
            color: ctx.fg,
            fontSize: 13.5,
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          <span>Farbmodus</span>
          <span style={{ color: ctx.accentColor }}>
            {settings.colorMode === 'dark' ? 'Dunkel' : 'Hell'}
          </span>
        </button>

        <button
          type="button"
          onClick={() => {
            triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
            settings.onChangeTonalIcons(!settings.tonalIcons);
          }}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            background: 'transparent',
            border: 'none',
            color: ctx.fg,
            fontSize: 13.5,
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          <span>Tonale Icons</span>
          <span style={{ color: ctx.accentColor }}>{settings.tonalIcons ? 'An' : 'Aus'}</span>
        </button>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 13.5, fontWeight: 600 }}>4. Reihe Kürzel</span>
          <div style={{ display: 'flex', gap: 4 }}>
            {(['all', 'start-only', 'off'] as const).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => settings.onChangeShortcutBarMode(m)}
                style={{
                  padding: '4px 8px',
                  borderRadius: 99,
                  border: 'none',
                  background: settings.shortcutBarMode === m ? ctx.accentColor : 'rgba(125,125,145,0.16)',
                  color: settings.shortcutBarMode === m ? '#FFFFFF' : ctx.fg,
                  fontSize: 11,
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                {m === 'all' ? 'Alle' : m === 'start-only' ? 'Start' : 'Aus'}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

/* ---------------- Generic Rich Fallback View ---------------- */
export const FallbackAppView: React.FC<{
  ctx: AppViewStyleContext;
  appTitle: string;
}> = ({ ctx, appTitle }) => {
  const [syncActive, setSyncActive] = useState(true);
  return (
    <>
      <div style={{ padding: 16, borderRadius: 20, background: ctx.cardBg, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ fontSize: 14, fontWeight: 700 }}>{appTitle} · Live-Dienst</div>
          <div style={{ fontSize: 12, color: ctx.muted, marginTop: 2 }}>
            {syncActive ? 'Hintergrund-Sync & Widget-Updates aktiv' : 'Sync pausiert'}
          </div>
        </div>
        <button
          type="button"
          onClick={() => {
            triggerHaptic('snap', { enabled: ctx.hapticsEnabled });
            setSyncActive((v) => !v);
          }}
          style={{
            padding: '7px 12px',
            borderRadius: 99,
            border: 'none',
            background: syncActive ? ctx.accentColor : 'rgba(125,125,145,0.2)',
            color: syncActive ? '#FFFFFF' : ctx.fg,
            fontSize: 12,
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          {syncActive ? <Check size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} /> : null}
          {syncActive ? 'Aktiv' : 'Pausiert'}
        </button>
      </div>
      <div style={{ padding: 16, borderRadius: 20, background: ctx.cardBg, fontSize: 13, color: ctx.muted, lineHeight: 1.5 }}>
        Alle Kachel-Ansichten (XS, S, M, L) sind für <strong style={{ color: ctx.fg }}>{appTitle}</strong> freigeschaltet.
      </div>
    </>
  );
};

function inputStyle(ctx: AppViewStyleContext): React.CSSProperties {
  return {
    padding: '9px 12px',
    borderRadius: 14,
    border: `1px solid ${ctx.borderColor}`,
    background: 'rgba(125,125,145,0.12)',
    color: ctx.fg,
    fontSize: 13.5,
    fontWeight: 500,
    outline: 'none',
    minHeight: 40,
  };
}

function iconCircleBtn(bg: string, color: string): React.CSSProperties {
  return {
    width: 42,
    height: 42,
    borderRadius: 99,
    border: 'none',
    background: bg,
    color,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: 'pointer',
  };
}
