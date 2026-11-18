import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  AlarmClock,
  BatteryCharging,
  Bluetooth,
  Calculator,
  Calendar,
  CalendarPlus,
  Camera,
  Check,
  Cloud,
  CloudRain,
  CloudSun,
  Droplets,
  FilePlus2,
  FileText,
  Flashlight,
  Footprints,
  Headphones,
  Image as ImageIcon,
  Moon,
  Plus,
  QrCode,
  Radio,
  RefreshCw,
  RotateCw,
  Search,
  Sliders,
  Sun,
  Thermometer,
  Timer,
  Wifi,
  WifiOff,
} from 'lucide-react';
import {
  CALENDAR_EVENTS,
  TEMP_DAY_SERIES,
  WEATHER_HOURS,
} from '../data/mockData';
import { triggerHaptic } from '../motion/haptics';
import { SystemToggleKey, TileContentProps } from './tileProps';

export const GreetingTile: React.FC<TileContentProps> = ({
  tier,
  surface,
  theme,
  width,
}) => {
  const isBold = theme.isBold;
  const fluidGreetingSize = Math.round(
    Math.max(23, Math.min(isBold ? 38 : 34, width * 0.135))
  );

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <Sun size={24} color={surface.accentColor} />
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between" style={{ padding: '0 4px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Sun size={20} color={surface.accentColor} />
          <span style={{ fontSize: 15, fontWeight: 600 }}>Guten Morgen</span>
        </div>
        <span style={{ fontSize: 13, color: surface.textSecondary }}>
          Mi., 18. Nov · 7° Bewölkt
        </span>
      </div>
    );
  }

  if (tier === 'S' || tier === 'LIST') {
    return (
      <div className="tile-col-between">
        <Sun size={20} color={surface.accentColor} />
        <div>
          <div style={{ fontSize: 15, fontWeight: 700, lineHeight: 1.15 }}>Guten Morgen</div>
          <div style={{ fontSize: 12, color: surface.textSecondary, marginTop: 2 }}>
            18. Nov · 7°
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <motion.span
          layout="position"
          style={{
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: '0.03em',
            textTransform: 'uppercase',
            color: surface.accentColor,
          }}
        >
          Mittwoch, 18. Nov
        </motion.span>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 5,
            padding: '4px 9px',
            borderRadius: 999,
            background: surface.accentBg,
            fontSize: 12,
            fontWeight: 600,
            color: surface.textPrimary,
          }}
        >
          <Cloud size={14} color={surface.accentColor} />
          <span className="display-num">7°</span>
        </div>
      </div>

      <div>
        <motion.div
          layout="position"
          className="display-num"
          style={{
            fontSize: fluidGreetingSize,
            fontWeight: 700,
            lineHeight: 1.06,
            letterSpacing: '-0.025em',
          }}
        >
          Guten
          <br />
          Morgen
        </motion.div>
        <div
          style={{
            fontSize: 12,
            color: surface.textSecondary,
            marginTop: 8,
            lineHeight: 1.35,
          }}
        >
          Bewölkt, ab 17 Uhr leichter Regen. 3 Termine heute.
        </div>
      </div>
    </div>
  );
};

export const TempCurveTile: React.FC<TileContentProps> = ({
  tier,
  surface,
  width,
  height,
}) => {
  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <Thermometer size={20} color={surface.accentColor} />
        <span className="display-num" style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>
          7°
        </span>
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Thermometer size={18} color={surface.accentColor} />
          <span style={{ fontSize: 14, fontWeight: 600 }}>Tagesverlauf</span>
        </div>
        <span className="display-num" style={{ fontSize: 15, fontWeight: 700 }}>
          4° – 11°
        </span>
      </div>
    );
  }

  const pts = TEMP_DAY_SERIES.map((d, idx) => {
    const x = (idx / (TEMP_DAY_SERIES.length - 1)) * 100;
    const y = 52 - ((d.temp - 3) / 10) * 38;
    return { x, y, ...d };
  });
  const pathD = pts
    .map((p, i) => (i === 0 ? `M ${p.x},${p.y}` : `L ${p.x},${p.y}`))
    .join(' ');
  const areaD = `${pathD} L 100,58 L 0,58 Z`;

  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 6 }}>
        <span style={{ fontSize: 11.5, fontWeight: 600, color: surface.textSecondary }}>
          Verlauf
        </span>
        <span className="display-num" style={{ fontSize: 11.5, fontWeight: 700, color: surface.accentColor }}>
          {width < 135 ? '↑11°' : 'max 11°'}
        </span>
      </div>

      <svg
        viewBox="-6 0 112 62"
        style={{ width: '100%', height: Math.max(54, Math.min(110, height * 0.48)), overflow: 'visible' }}
      >
        <defs>
          <linearGradient id="tempArea" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={surface.accentColor} stopOpacity="0.35" />
            <stop offset="100%" stopColor={surface.accentColor} stopOpacity="0.0" />
          </linearGradient>
        </defs>
        <path d={areaD} fill="url(#tempArea)" />
        <path
          d={pathD}
          fill="none"
          stroke={surface.accentColor}
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle cx={pts[1].x} cy={pts[1].y} r="4.5" fill={surface.accentColor} />
        <circle cx={pts[1].x} cy={pts[1].y} r="8" fill={surface.accentColor} opacity="0.25" />
      </svg>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div>
          <div className="display-num" style={{ fontSize: 24, fontWeight: 700, lineHeight: 1 }}>
            7°
          </div>
          <div style={{ fontSize: 11, color: surface.textSecondary, marginTop: 2 }}>
            Tief 4°
          </div>
        </div>
        {tier !== 'S' && (
          <div style={{ fontSize: 11, color: surface.textSecondary, textAlign: 'right' }}>
            15h Peak
          </div>
        )}
      </div>
      {height >= 175 && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 2,
            paddingTop: 5,
            borderTop: `1px solid ${surface.accentBg}`,
            textAlign: 'center',
          }}
          className="display-num"
        >
          {[
            { t: '09h', v: '7°' },
            { t: '15h', v: '11°' },
            { t: '21h', v: '6°' },
          ].map((pt) => (
            <div key={pt.t}>
              <div style={{ fontSize: 9.5, color: surface.textSecondary }}>{pt.t}</div>
              <div style={{ fontSize: 10.5, fontWeight: 700 }}>{pt.v}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export const CalendarTimelineTile: React.FC<TileContentProps> = ({
  tier,
  surface,
  calendarEvents,
}) => {
  const events = calendarEvents ?? CALENDAR_EVENTS;
  const firstEv = events[0];

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <Calendar size={22} color={surface.accentColor} />
        <span className="display-num" style={{ fontSize: 12, fontWeight: 700, marginTop: 2 }}>
          {events.length}
        </span>
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Calendar size={17} color={surface.accentColor} />
          <span style={{ fontSize: 14, fontWeight: 600 }}>
            {firstEv ? `${firstEv.title}, ${firstEv.time}` : 'Keine Termine'}
          </span>
        </div>
        <span style={{ fontSize: 12, color: surface.textSecondary }}>
          {events.length > 1 ? `+${events.length - 1} heute` : 'Heute'}
        </span>
      </div>
    );
  }

  if (tier === 'S' || tier === 'LIST') {
    return (
      <div className="tile-col-between" style={{ gap: 6 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Calendar size={15} color={surface.accentColor} />
          <span style={{ fontSize: 13, fontWeight: 700 }}>Heute</span>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {events.slice(0, tier === 'LIST' ? 3 : 2).map((ev) => (
            <div key={ev.id} style={{ borderLeft: `3px solid ${surface.accentColor}`, paddingLeft: 7 }}>
              <div style={{ fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {ev.title}
              </div>
              <div className="display-num" style={{ fontSize: 11, color: surface.textSecondary }}>
                {ev.time}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="tile-col-between" style={{ height: '100%', gap: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
          <Calendar size={15} color={surface.accentColor} />
          <span style={{ fontSize: 15, fontWeight: 700 }}>Tagesplan</span>
        </div>
        <span className="display-num" style={{ fontSize: 12, color: '#EF4444', fontWeight: 600 }}>
          ● Jetzt 09:01
        </span>
      </div>

      <div style={{ position: 'relative', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between', paddingLeft: 44 }}>
        {/* Current time red marker line */}
        <div
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            top: 8,
            display: 'flex',
            alignItems: 'center',
            pointerEvents: 'none',
          }}
        >
          <span className="display-num" style={{ fontSize: 10, fontWeight: 700, color: '#EF4444', width: 36 }}>
            09:01
          </span>
          <div style={{ width: 7, height: 7, borderRadius: 99, background: '#EF4444', marginRight: 2 }} />
          <div style={{ flex: 1, height: 1.5, background: '#EF4444', opacity: 0.7 }} />
        </div>

        <div style={{ height: 16 }} />

        {events.slice(0, 3).map((ev, idx, arr) => (
          <div
            key={ev.id}
            style={{
              position: 'relative',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '5px 4px 5px 10px',
              borderLeft: `3px solid hsl(${ev.hue} 80% 60%)`,
              borderBottom:
                idx < arr.length - 1 ? `1px solid ${surface.accentBg}` : 'none',
            }}
          >
            <span
              className="display-num"
              style={{
                position: 'absolute',
                left: -44,
                fontSize: 11,
                fontWeight: 600,
                color: surface.textSecondary,
              }}
            >
              {ev.time}
            </span>
            <span style={{ fontSize: 13.5, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '64%' }}>
              {ev.title}
            </span>
            <span style={{ fontSize: 11, color: surface.textSecondary }}>{ev.location}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

const QUICK_REMINDER_IDEAS = [
  'Design-Review vorbereiten',
  'Backup prüfen',
  'Ladekabel einpacken',
  'Flugmodus fürs Meeting',
  'Wasser trinken',
];

export const RemindersTile: React.FC<TileContentProps> = ({
  tile,
  leafId,
  tier,
  surface,
  motionSettings,
  isEditMode,
  onUpdateTile,
}) => {
  const items = tile.reminders ?? [
    { id: 'r1', text: 'Prototyp testen', done: true },
    { id: 'r2', text: 'Paket abholen', done: false },
    { id: 'r3', text: 'Kaffeebohnen kaufen', done: false },
  ];
  const openCount = items.filter((i) => !i.done).length;

  const toggleItem = (e: React.MouseEvent, id: string) => {
    if (isEditMode) return;
    e.stopPropagation();
    triggerHaptic('press', {
      enabled: motionSettings.hapticsEnabled,
      x: e.clientX,
      y: e.clientY,
    });
    onUpdateTile(leafId, (prev) => ({
      ...prev,
      reminders: (prev.reminders ?? items).map((it) =>
        it.id === id ? { ...it, done: !it.done } : it
      ),
    }));
  };

  const addQuickReminder = (e: React.MouseEvent) => {
    if (isEditMode) return;
    e.stopPropagation();
    triggerHaptic('snap', {
      enabled: motionSettings.hapticsEnabled,
      x: e.clientX,
      y: e.clientY,
    });
    onUpdateTile(leafId, (prev) => {
      const current = prev.reminders ?? items;
      const candidate =
        QUICK_REMINDER_IDEAS.find((idea) => !current.some((c) => c.text === idea)) ??
        `Notiz #${current.length + 1}`;
      return {
        ...prev,
        reminders: [{ id: `r-${Date.now()}`, text: candidate, done: false }, ...current],
      };
    });
  };

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <Check size={20} color={surface.accentColor} />
        <span className="display-num" style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>
          {openCount}
        </span>
      </div>
    );
  }

  if (tier === 'TICKER') {
    const next = items.find((i) => !i.done) ?? items[0];
    return (
      <div className="tile-row-between">
        <span style={{ fontSize: 14, fontWeight: 600 }}>{next.text}</span>
        <span className="display-num" style={{ fontSize: 12, color: surface.accentColor, fontWeight: 700 }}>
          {openCount} offen
        </span>
      </div>
    );
  }

  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 6 }}>
        <span style={{ fontSize: 15, fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          Erinnerungen
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
          <span className="display-num" style={{ fontSize: 12, fontWeight: 700, color: surface.accentColor }}>
            {openCount} offen
          </span>
          <button
            type="button"
            aria-label="Erinnerung hinzufügen"
            title="Schnelle Erinnerung hinzufügen"
            onClick={addQuickReminder}
            style={{
              width: 22,
              height: 22,
              borderRadius: 99,
              border: 'none',
              background: surface.accentBg,
              color: surface.accentColor,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <Plus size={13} strokeWidth={2.5} />
          </button>
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 4 }}>
        {items.slice(0, tier === 'S' ? 2 : 3).map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={(e) => toggleItem(e, item.id)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              background: 'transparent',
              border: 'none',
              padding: '2px 0',
              color: item.done ? surface.textSecondary : surface.textPrimary,
              cursor: 'pointer',
              textAlign: 'left',
              fontSize: 12,
              fontWeight: 500,
              minHeight: 24,
            }}
          >
            <span
              style={{
                width: 17,
                height: 17,
                borderRadius: 6,
                border: `1.5px solid ${item.done ? surface.accentColor : surface.textSecondary}`,
                background: item.done ? surface.accentColor : 'transparent',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              {item.done && <Check size={11} color="#FFFFFF" strokeWidth={3} />}
            </span>
            <span
              style={{
                textDecoration: item.done ? 'line-through' : 'none',
                opacity: item.done ? 0.65 : 1,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {item.text}
            </span>
          </button>
        ))}
      </div>
    </div>
  );
};

export const BatteryStepsTile: React.FC<TileContentProps> = ({
  tier,
  surface,
  height,
}) => {
  const batteryPct = 84;
  const stepsPct = 74;

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <BatteryCharging size={20} color={surface.accentColor} />
        <span className="display-num" style={{ fontSize: 12, fontWeight: 700, marginTop: 2 }}>
          84%
        </span>
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between">
        <span className="display-num" style={{ fontSize: 14, fontWeight: 700 }}>Akku 84 %</span>
        <span className="display-num" style={{ fontSize: 13, color: surface.textSecondary }}>7.420 Schritte</span>
      </div>
    );
  }

  const renderRing = (pct: number, color: string, label: string, val: string) => {
    const r = 21;
    const circ = 2 * Math.PI * r;
    const offset = circ * (1 - pct / 100);
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
        <div style={{ position: 'relative', width: 54, height: 54, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <svg width="54" height="54" viewBox="0 0 54 54">
            <circle cx="27" cy="27" r={r} fill="none" stroke={surface.accentBg} strokeWidth="5.5" />
            <circle
              cx="27"
              cy="27"
              r={r}
              fill="none"
              stroke={color}
              strokeWidth="5.5"
              strokeDasharray={circ}
              strokeDashoffset={offset}
              strokeLinecap="round"
              transform="rotate(-90 27 27)"
            />
          </svg>
          <span className="display-num" style={{ position: 'absolute', fontSize: 11, fontWeight: 700 }}>
            {val}
          </span>
        </div>
        <span style={{ fontSize: 11, color: surface.textSecondary, fontWeight: 600 }}>{label}</span>
      </div>
    );
  };

  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <span style={{ fontSize: 13, fontWeight: 700 }}>Status</span>
      <div style={{ display: 'flex', justifyContent: 'space-around', alignItems: 'center', flex: 1 }}>
        {renderRing(batteryPct, '#22C55E', 'Akku', '84%')}
        {tier !== 'LIST' && renderRing(stepsPct, surface.accentColor, 'Schritte', '7,4k')}
      </div>
      {height >= 150 && (
        <div
          className="display-num"
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            fontSize: 10.5,
            color: surface.textSecondary,
            paddingTop: 5,
            borderTop: `1px solid ${surface.accentBg}`,
          }}
        >
          <span>ca. 19 Std.</span>
          <span>5,4 km heute</span>
        </div>
      )}
    </div>
  );
};

export const ClockTile: React.FC<TileContentProps> = ({
  tile,
  leafId,
  tier,
  surface,
  theme,
  width,
  height,
  motionSettings,
  isEditMode,
  onUpdateTile,
}) => {
  const isBold = theme.isBold;
  const isAlarmOff = tile.subtitle === 'Wecker aus';
  const alarmLabel = isAlarmOff ? 'Wecker aus' : 'Wecker 07:15';
  const fluidClockSize = Math.round(
    Math.max(28, Math.min(isBold ? 58 : 52, Math.min(width * 0.24, height * 0.28)))
  );

  const handleToggleAlarm = (e: React.MouseEvent) => {
    if (isEditMode) return;
    e.stopPropagation();
    triggerHaptic('snap', {
      enabled: motionSettings.hapticsEnabled,
      x: e.clientX,
      y: e.clientY,
    });
    onUpdateTile(leafId, (prev) => ({
      ...prev,
      subtitle: prev.subtitle === 'Wecker aus' ? 'Wecker 07:15' : 'Wecker aus',
    }));
  };

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <span className="display-num" style={{ fontSize: 18, fontWeight: 700 }}>
          09:01
        </span>
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between">
        <span className="display-num" style={{ fontSize: 26, fontWeight: 700 }}>09:01</span>
        <span style={{ fontSize: 12, color: surface.textSecondary }}>Mi., 18. Nov · Wecker 07:15</span>
      </div>
    );
  }

  if (tier === 'LIST') {
    return (
      <div className="tile-col-between">
        <div className="display-num" style={{ fontSize: 28, fontWeight: 700, lineHeight: 1 }}>
          09
          <br />
          01
        </div>
        <div style={{ fontSize: 11, color: surface.textSecondary }}>Mi., 18. Nov</div>
      </div>
    );
  }

  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 12, fontWeight: 600, color: surface.textSecondary }}>
          Mi., 18. Nov
        </span>
        <span style={{ fontSize: 11, color: surface.accentColor, fontWeight: 600 }}>Berlin</span>
      </div>

      <motion.div
        layout="position"
        className="display-num"
        style={{
          fontSize: fluidClockSize,
          fontWeight: 700,
          lineHeight: 0.96,
          letterSpacing: '-0.04em',
        }}
      >
        09:01
      </motion.div>

      <button
        type="button"
        aria-pressed={!isAlarmOff}
        aria-label="Wecker 07:15 umschalten"
        onClick={handleToggleAlarm}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 6,
          padding: '4px 10px',
          borderRadius: 999,
          border: 'none',
          background: isAlarmOff ? 'rgba(125,125,145,0.16)' : surface.accentBg,
          alignSelf: 'flex-start',
          fontSize: 12,
          fontWeight: 600,
          color: isAlarmOff ? surface.textSecondary : surface.textPrimary,
          opacity: isAlarmOff ? 0.72 : 1,
          cursor: 'pointer',
        }}
      >
        <AlarmClock size={13} color={isAlarmOff ? surface.textSecondary : surface.accentColor} />
        <span className="display-num">{alarmLabel}</span>
      </button>
    </div>
  );
};

export const WeatherTile: React.FC<TileContentProps> = ({
  tier,
  surface,
  theme,
  width,
  height,
}) => {
  const isBold = theme.isBold;
  const fluidTempSize = Math.round(
    Math.max(28, Math.min(isBold ? 46 : 42, Math.min(width * 0.25, height * 0.25)))
  );

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <CloudRain size={20} color={surface.accentColor} />
        <span className="display-num" style={{ fontSize: 14, fontWeight: 700, marginTop: 2 }}>
          7°
        </span>
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <CloudRain size={18} color={surface.accentColor} />
          <span className="display-num" style={{ fontSize: 20, fontWeight: 700 }}>7°</span>
        </div>
        <span style={{ fontSize: 12, color: surface.textSecondary }}>Regen ab 17 Uhr</span>
      </div>
    );
  }

  const renderHourIcon = (name: string) => {
    if (name === 'CloudSun') return <CloudSun size={13} color={surface.accentColor} />;
    if (name === 'CloudRain') return <CloudRain size={13} color={surface.accentColor} />;
    return <Cloud size={13} color={surface.textSecondary} />;
  };

  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <motion.div
          layout="position"
          className="display-num"
          style={{
            fontSize: fluidTempSize,
            fontWeight: 700,
            lineHeight: 0.95,
          }}
        >
          7°
        </motion.div>
        <CloudRain size={22} color={surface.accentColor} />
      </div>

      {tier !== 'LIST' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
          <div style={{ fontSize: 12, fontWeight: 600, color: surface.textSecondary, lineHeight: 1.25 }}>
            Regen ab 17 Uhr
          </div>
          {tier !== 'S' && (
            <div
              className="display-num"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '3px 8px',
                borderRadius: 99,
                background: surface.accentBg,
                alignSelf: 'flex-start',
                fontSize: 10.5,
                fontWeight: 600,
                color: surface.textPrimary,
              }}
            >
              <span>H 11° · T 4°</span>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 2, color: surface.accentColor }}>
                <Droplets size={11} />
                80%
              </span>
            </div>
          )}
        </div>
      )}

      {tier !== 'S' && tier !== 'LIST' && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: 4,
            paddingTop: 6,
            borderTop: `1px solid ${surface.accentBg}`,
          }}
        >
          {WEATHER_HOURS.map((h) => (
            <div key={h.hour} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
              <span className="display-num" style={{ fontSize: 10, color: surface.textSecondary }}>
                {h.hour}
              </span>
              {renderHourIcon(h.icon)}
              <span className="display-num" style={{ fontSize: 11, fontWeight: 700 }}>
                {h.temp}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export const CalendarTile: React.FC<TileContentProps> = ({
  tier,
  surface,
  height,
  calendarEvents,
}) => {
  const events = calendarEvents ?? CALENDAR_EVENTS;
  const firstEv = events[0];

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <Calendar size={20} color={surface.accentColor} />
        <span className="display-num" style={{ fontSize: 12, fontWeight: 700, marginTop: 2 }}>
          18
        </span>
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between">
        <span style={{ fontSize: 14, fontWeight: 600 }}>{firstEv?.title ?? 'Keine Termine'}</span>
        <span className="display-num" style={{ fontSize: 13, color: surface.accentColor, fontWeight: 700 }}>
          {firstEv?.time ?? '—'}
        </span>
      </div>
    );
  }

  const eventCount = tier === 'L' || height >= 175 ? 3 : 2;

  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 15, fontWeight: 700 }}>Kalender</span>
        <span
          className="display-num"
          style={{
            fontSize: 10.5,
            fontWeight: 700,
            padding: '2px 7px',
            borderRadius: 99,
            background: surface.accentBg,
            color: surface.accentColor,
          }}
        >
          {events.length} heute
        </span>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {events.slice(0, eventCount).map((ev, idx, arr) => (
          <div
            key={ev.id}
            style={{
              padding: '3px 0 4px 9px',
              borderLeft: `3px solid hsl(${ev.hue} 80% 62%)`,
              borderBottom: idx < arr.length - 1 && tier !== 'S' ? `1px solid ${surface.accentBg}` : 'none',
            }}
          >
            <div style={{ fontSize: 12.5, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {ev.title}
            </div>
            <div className="display-num" style={{ fontSize: 10.5, color: surface.textSecondary, marginTop: 1 }}>
              {ev.time} · {ev.location}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export const NoteTile: React.FC<TileContentProps> = ({ tile, tier, surface }) => {
  const lines = tile.noteLines ?? [
    'Federwerte 0,75 / 400 prüfen',
    'Fugen 6 dp → 12 dp',
    'Haptik-Tick testen',
  ];

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <FileText size={21} color={surface.accentColor} />
      </div>
    );
  }

  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 15, fontWeight: 700 }}>{tile.title || 'Notiz'}</span>
        <FileText size={15} color={surface.accentColor} />
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
        {lines.slice(0, tier === 'S' ? 2 : 3).map((line, i) => (
          <div
            key={i}
            style={{
              fontSize: 12,
              color: surface.textSecondary,
              paddingLeft: 8,
              borderLeft: `2px solid ${surface.accentColor}`,
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {line}
          </div>
        ))}
      </div>
    </div>
  );
};

const PHOTO_SCENES = [
  {
    title: 'Erinnerungen',
    meta: 'Herbst im Studio · 14 Fotos',
    c1: '#F97316',
    c2: '#EC4899',
    c3: '#4F46E5',
    sun: '#FDE047',
  },
  {
    title: 'Nordlicht am Fjord',
    meta: 'Norwegen Trip · 9 Fotos',
    c1: '#065F46',
    c2: '#10B981',
    c3: '#0F172A',
    sun: '#A7F3D0',
  },
  {
    title: 'Abendrot Berlin',
    meta: 'Spaziergang Mitte · 22 Fotos',
    c1: '#FB7185',
    c2: '#F59E0B',
    c3: '#1E1B4B',
    sun: '#FDBA74',
  },
];

export const PhotosTile: React.FC<TileContentProps> = ({
  tier,
  surface,
  motionSettings,
  isEditMode,
}) => {
  const [sceneIdx, setSceneIdx] = useState(0);
  const scene = PHOTO_SCENES[sceneIdx];

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <ImageIcon size={21} color={surface.accentColor} />
      </div>
    );
  }

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%', overflow: 'hidden', borderRadius: 'inherit' }}>
      <motion.div
        animate={{ scale: [1, 1.12, 1], x: [0, -10, 0], y: [0, -6, 0] }}
        transition={{ duration: 14, repeat: Infinity, ease: 'easeInOut' }}
        style={{ position: 'absolute', inset: -16 }}
      >
        <svg viewBox="0 0 300 300" preserveAspectRatio="xMidYMid slice" style={{ width: '100%', height: '100%' }}>
          <defs>
            <linearGradient id={`photo-sky-${sceneIdx}`} x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor={scene.c1} />
              <stop offset="55%" stopColor={scene.c2} />
              <stop offset="100%" stopColor={scene.c3} />
            </linearGradient>
          </defs>
          <rect width="300" height="300" fill={`url(#photo-sky-${sceneIdx})`} />
          <circle cx="215" cy="95" r="42" fill={scene.sun} opacity="0.85" />
          <path d="M0,210 Q75,155 155,195 T300,175 L300,300 L0,300 Z" fill="#1E1B4B" opacity="0.72" />
          <path d="M0,245 Q90,210 190,240 T300,225 L300,300 L0,300 Z" fill="#0F172A" opacity="0.88" />
        </svg>
      </motion.div>

      <button
        type="button"
        aria-label="Nächstes Foto anzeigen"
        onClick={(e) => {
          if (isEditMode) return;
          e.stopPropagation();
          triggerHaptic('tick', {
            enabled: motionSettings.hapticsEnabled,
            x: e.clientX,
            y: e.clientY,
          });
          setSceneIdx((i) => (i + 1) % PHOTO_SCENES.length);
        }}
        style={{
          position: 'absolute',
          top: 9,
          right: 9,
          zIndex: 4,
          display: 'flex',
          alignItems: 'center',
          gap: 4,
          padding: '4px 8px',
          borderRadius: 99,
          border: '1px solid rgba(255,255,255,0.28)',
          background: 'rgba(15, 23, 42, 0.52)',
          backdropFilter: 'blur(8px)',
          color: '#FFFFFF',
          fontSize: 10.5,
          fontWeight: 700,
          cursor: 'pointer',
        }}
      >
        <span className="display-num">{sceneIdx + 1}/{PHOTO_SCENES.length}</span>
        <RefreshCw size={11} />
      </button>

      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(to top, rgba(0,0,0,0.68), transparent 55%)',
          padding: 12,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'flex-end',
        }}
      >
        <div style={{ fontSize: 15, fontWeight: 700, color: '#FFFFFF' }}>{scene.title}</div>
        {tier !== 'S' && (
          <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.82)' }}>{scene.meta}</div>
        )}
      </div>
    </div>
  );
};

export const BatteryTile: React.FC<TileContentProps> = ({ tier, surface }) => {
  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <BatteryCharging size={20} color="#22C55E" />
        <span className="display-num" style={{ fontSize: 12, fontWeight: 700 }}>84%</span>
      </div>
    );
  }
  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 14, fontWeight: 700 }}>Akku</span>
        <BatteryCharging size={18} color="#22C55E" />
      </div>
      <div className="display-num" style={{ fontSize: 34, fontWeight: 700, lineHeight: 1 }}>
        84<span style={{ fontSize: 18 }}>%</span>
      </div>
      <div style={{ fontSize: 12, color: surface.textSecondary }}>Noch ca. 19 Std.</div>
    </div>
  );
};

export const StepsTile: React.FC<TileContentProps> = ({ tier, surface }) => {
  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <Footprints size={20} color={surface.accentColor} />
        <span className="display-num" style={{ fontSize: 11, fontWeight: 700 }}>7,4k</span>
      </div>
    );
  }
  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 14, fontWeight: 700 }}>Schritte</span>
        <Footprints size={18} color={surface.accentColor} />
      </div>
      <div className="display-num" style={{ fontSize: 30, fontWeight: 700, lineHeight: 1 }}>
        7.420
      </div>
      <div style={{ width: '100%', height: 6, borderRadius: 99, background: surface.accentBg, overflow: 'hidden' }}>
        <div style={{ width: '74%', height: '100%', background: surface.accentColor, borderRadius: 99 }} />
      </div>
    </div>
  );
};

export const QuickTogglesTile: React.FC<TileContentProps> = ({
  tile,
  tier,
  width,
  height,
  surface,
  theme,
  motionSettings,
  isEditMode,
  onOpenApp,
  onOpenSearch,
  systemState,
}) => {
  const [alarmOn, setAlarmOn] = useState(true);
  const [eqIndex, setEqIndex] = useState(0);
  const [sleepTimerOn, setSleepTimerOn] = useState(false);

  const preset = tile.togglePreset ?? 'system';
  const eqModes = ['Warm', 'Bass', 'Klar'];

  interface ActionItem {
    key: string;
    label: string;
    shortLabel: string;
    subOn: string;
    subOff: string;
    active: boolean;
    dotColor?: string;
    icon: React.ReactNode;
    onPress: (e: React.MouseEvent) => void;
  }

  const triggerBtn = (e: React.MouseEvent, fn: () => void) => {
    if (isEditMode) return;
    e.stopPropagation();
    triggerHaptic('snap', {
      enabled: motionSettings.hapticsEnabled,
      x: e.clientX,
      y: e.clientY,
    });
    fn();
  };

  const systemItems: ActionItem[] = [
    {
      key: 'wifi',
      label: 'WLAN',
      shortLabel: 'WLAN',
      subOn: 'Studio_5G',
      subOff: 'Aus',
      active: systemState.wifi,
      icon: systemState.wifi ? <Wifi size={16} /> : <WifiOff size={16} />,
      onPress: (e) => triggerBtn(e, () => systemState.onToggle('wifi')),
    },
    {
      key: 'bluetooth',
      label: 'Bluetooth',
      shortLabel: 'BT',
      subOn: 'Kopfhörer',
      subOff: 'Aus',
      active: systemState.bluetooth,
      icon: <Bluetooth size={16} />,
      onPress: (e) => triggerBtn(e, () => systemState.onToggle('bluetooth')),
    },
    {
      key: 'flashlight',
      label: 'Licht',
      shortLabel: 'Licht',
      subOn: 'Stufe 3',
      subOff: 'Bereit',
      active: systemState.flashlight,
      dotColor: '#F59E0B',
      icon: <Flashlight size={16} />,
      onPress: (e) => triggerBtn(e, () => systemState.onToggle('flashlight')),
    },
    {
      key: 'dnd',
      label: 'Fokus',
      shortLabel: 'Fokus',
      subOn: 'Aktiv',
      subOff: 'Aus',
      active: systemState.dnd,
      icon: <Moon size={16} />,
      onPress: (e) => triggerBtn(e, () => systemState.onToggle('dnd')),
    },
    {
      key: 'hotspot',
      label: 'Hotspot',
      shortLabel: 'Hotspot',
      subOn: '5 GHz',
      subOff: 'Aus',
      active: systemState.hotspot,
      icon: <Radio size={16} />,
      onPress: (e) => triggerBtn(e, () => systemState.onToggle('hotspot')),
    },
    {
      key: 'autoRotate',
      label: 'Drehen',
      shortLabel: 'Drehen',
      subOn: 'Auto',
      subOff: 'Gesperrt',
      active: systemState.autoRotate,
      icon: <RotateCw size={16} />,
      onPress: (e) => triggerBtn(e, () => systemState.onToggle('autoRotate')),
    },
  ];

  const todayItems: ActionItem[] = [
    {
      key: 'new-event',
      label: 'Termin',
      shortLabel: '+ Termin',
      subOn: '10:00',
      subOff: 'Neu',
      active: false,
      icon: <CalendarPlus size={16} />,
      onPress: (e) =>
        triggerBtn(e, () => onOpenApp('app-kalender', (e.currentTarget as HTMLElement).getBoundingClientRect())),
    },
    {
      key: 'new-note',
      label: 'Notiz',
      shortLabel: '+ Notiz',
      subOn: 'Neu',
      subOff: 'Neu',
      active: false,
      icon: <FilePlus2 size={16} />,
      onPress: (e) =>
        triggerBtn(e, () => onOpenApp('app-notizen', (e.currentTarget as HTMLElement).getBoundingClientRect())),
    },
    {
      key: 'alarm',
      label: 'Wecker',
      shortLabel: 'Wecker',
      subOn: '07:15',
      subOff: 'Aus',
      active: alarmOn,
      icon: <AlarmClock size={16} />,
      onPress: (e) => triggerBtn(e, () => setAlarmOn((v) => !v)),
    },
    {
      key: 'today-focus',
      label: 'Fokus',
      shortLabel: 'Fokus',
      subOn: '25m an',
      subOff: '25m',
      active: systemState.dnd,
      icon: <Moon size={16} />,
      onPress: (e) => triggerBtn(e, () => systemState.onToggle('dnd')),
    },
  ];

  const mediaItems: ActionItem[] = [
    {
      key: 'headphones',
      label: 'Kopfhörer',
      shortLabel: 'Audio',
      subOn: 'Verbunden',
      subOff: 'Lautsprecher',
      active: systemState.bluetooth,
      icon: <Headphones size={16} />,
      onPress: (e) => triggerBtn(e, () => systemState.onToggle('bluetooth')),
    },
    {
      key: 'eq',
      label: 'Klang',
      shortLabel: eqModes[eqIndex],
      subOn: eqModes[eqIndex],
      subOff: eqModes[eqIndex],
      active: true,
      icon: <Sliders size={16} />,
      onPress: (e) => triggerBtn(e, () => setEqIndex((i) => (i + 1) % eqModes.length)),
    },
    {
      key: 'sleep-timer',
      label: 'Timer',
      shortLabel: sleepTimerOn ? '15m an' : 'Timer',
      subOn: '15 Min.',
      subOff: 'Aus',
      active: sleepTimerOn,
      dotColor: '#F59E0B',
      icon: <Timer size={16} />,
      onPress: (e) => triggerBtn(e, () => setSleepTimerOn((v) => !v)),
    },
    {
      key: 'podcast-open',
      label: 'Podcast',
      shortLabel: 'Podcast',
      subOn: 'Folge 408',
      subOff: 'Öffnen',
      active: false,
      icon: <Radio size={16} />,
      onPress: (e) =>
        triggerBtn(e, () => onOpenApp('app-podcast', (e.currentTarget as HTMLElement).getBoundingClientRect())),
    },
  ];

  const toolsItems: ActionItem[] = [
    {
      key: 'quick-search',
      label: 'Suche',
      shortLabel: 'Suche',
      subOn: 'Alle Apps',
      subOff: 'Alle Apps',
      active: true,
      icon: <Search size={16} />,
      onPress: (e) => triggerBtn(e, () => onOpenSearch()),
    },
    {
      key: 'quick-qr',
      label: 'QR-Scan',
      shortLabel: 'QR-Scan',
      subOn: 'Bereit',
      subOff: 'Bereit',
      active: false,
      icon: <QrCode size={16} />,
      onPress: (e) =>
        triggerBtn(e, () => onOpenApp('app-qr', (e.currentTarget as HTMLElement).getBoundingClientRect())),
    },
    {
      key: 'quick-calc',
      label: 'Rechner',
      shortLabel: 'Rechner',
      subOn: 'Schnell',
      subOff: 'Schnell',
      active: false,
      icon: <Calculator size={16} />,
      onPress: (e) =>
        triggerBtn(e, () => onOpenApp('app-rechner', (e.currentTarget as HTMLElement).getBoundingClientRect())),
    },
    {
      key: 'quick-cam',
      label: 'Kamera',
      shortLabel: 'Kamera',
      subOn: '50 MP',
      subOff: '50 MP',
      active: false,
      icon: <Camera size={16} />,
      onPress: (e) =>
        triggerBtn(e, () => onOpenApp('app-kamera', (e.currentTarget as HTMLElement).getBoundingClientRect())),
    },
  ];

  const items =
    preset === 'today'
      ? todayItems
      : preset === 'media'
        ? mediaItems
        : preset === 'tools'
          ? toolsItems
          : systemItems;

  const activeText = theme.isGlass
    ? '#0F172A'
    : theme.mode === 'light' && !theme.isBold
      ? '#FFFFFF'
      : '#0F172A';
  const activeBg = theme.isGlass ? 'rgba(255, 255, 255, 0.92)' : surface.accentColor;
  const offBg = theme.isGlass
    ? 'rgba(255, 255, 255, 0.14)'
    : theme.mode === 'dark' || theme.isBold
      ? 'rgba(255, 255, 255, 0.08)'
      : 'rgba(15, 23, 42, 0.06)';

  if (tier === 'XS' && !(width >= 200 && height >= 54)) {
    const first = items[0];
    return (
      <div className="tile-center">
        <button
          type="button"
          onClick={first.onPress}
          style={{
            width: 42,
            height: 42,
            borderRadius: 14,
            border: 'none',
            background: first.active ? activeBg : offBg,
            color: first.active ? activeText : surface.textPrimary,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
          }}
          aria-label={first.label}
        >
          {first.icon}
        </button>
      </div>
    );
  }

  // Wide horizontal thumb strip (4th row across pages)
  if (tier === 'TICKER' || (width >= 200 && height < 104)) {
    return (
      <div
        style={{
          width: '100%',
          height: '100%',
          display: 'grid',
          gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
          gap: 8,
          alignItems: 'center',
        }}
      >
        {items.slice(0, 4).map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={item.onPress}
            aria-pressed={item.active}
            style={{
              height: '100%',
              minHeight: 44,
              maxHeight: 58,
              borderRadius: 15,
              border: item.active ? 'none' : `1px solid ${surface.accentBg}`,
              background: item.active ? activeBg : offBg,
              color: item.active ? activeText : surface.textPrimary,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 3,
              padding: '4px 6px',
              cursor: 'pointer',
              position: 'relative',
              boxShadow: item.active ? '0 6px 14px rgba(0,0,0,0.18)' : undefined,
              transition: 'transform 120ms ease, background 150ms ease, color 150ms ease',
            }}
          >
            {item.icon}
            <span style={{ fontSize: 10.5, fontWeight: 700, letterSpacing: '0.01em', whiteSpace: 'nowrap' }}>
              {item.shortLabel}
            </span>
            {item.active && (
              <span
                style={{
                  position: 'absolute',
                  top: 6,
                  right: 7,
                  width: 5,
                  height: 5,
                  borderRadius: 99,
                  background: item.dotColor ?? '#10B981',
                }}
              />
            )}
          </button>
        ))}
      </div>
    );
  }

  if (tier === 'LIST') {
    return (
      <div
        style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          gap: 6,
        }}
      >
        {items.slice(0, 4).map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={item.onPress}
            aria-pressed={item.active}
            style={{
              flex: 1,
              minHeight: 40,
              borderRadius: 14,
              border: 'none',
              background: item.active ? activeBg : offBg,
              color: item.active ? activeText : surface.textPrimary,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
            title={item.label}
          >
            {item.icon}
          </button>
        ))}
      </div>
    );
  }

  const visibleCount = tier === 'L' ? items.length : 4;
  const cols = tier === 'L' && width > 240 && visibleCount > 4 ? 3 : 2;
  const showBrightness = (tier === 'M' || tier === 'L') && preset === 'system';

  return (
    <div className="tile-col-between" style={{ height: '100%', gap: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 14, fontWeight: 700 }}>{tile.title || 'Schnellschalter'}</span>
        <span className="display-num" style={{ fontSize: 11, color: surface.textSecondary }}>
          {items.slice(0, visibleCount).filter((i) => i.active).length} aktiv
        </span>
      </div>

      <div
        style={{
          flex: 1,
          display: 'grid',
          gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))`,
          gap: 7,
        }}
      >
        {items.slice(0, visibleCount).map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={item.onPress}
            aria-pressed={item.active}
            style={{
              minHeight: 44,
              borderRadius: 14,
              border: 'none',
              padding: '6px 9px',
              background: item.active ? activeBg : offBg,
              color: item.active ? activeText : surface.textPrimary,
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              cursor: 'pointer',
              textAlign: 'left',
              overflow: 'hidden',
              transition: 'transform 120ms ease, background 150ms ease, color 150ms ease',
            }}
          >
            <span style={{ flexShrink: 0, display: 'flex' }}>{item.icon}</span>
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: 11.5, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {item.label}
              </div>
              {tier !== 'S' && (
                <div style={{ fontSize: 10, opacity: 0.78, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {item.active ? item.subOn : item.subOff}
                </div>
              )}
            </div>
          </button>
        ))}
      </div>

      {showBrightness && (
        <div
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => e.stopPropagation()}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '5px 9px',
            borderRadius: 12,
            background: offBg,
          }}
        >
          <Sun size={14} color={surface.accentColor} />
          <input
            type="range"
            min="0.25"
            max="1"
            step="0.05"
            value={systemState.brightness}
            onChange={(e) => systemState.onChangeBrightness(Number(e.target.value))}
            aria-label="Helligkeit"
            style={{ flex: 1, accentColor: surface.accentColor, cursor: 'pointer' }}
          />
          <span className="display-num" style={{ fontSize: 10.5, fontWeight: 700, minWidth: 30, textAlign: 'right' }}>
            {Math.round(systemState.brightness * 100)}%
          </span>
        </div>
      )}
    </div>
  );
};
