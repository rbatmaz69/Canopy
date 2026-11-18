import React from 'react';
import { motion } from 'framer-motion';
import { Palette, RotateCcw, Sliders, Sparkles, Vibrate, X } from 'lucide-react';
import { ShortcutBarMode } from '../data/mockData';
import { AnimationMood, MotionSettings } from '../motion/springs';
import { triggerHaptic } from '../motion/haptics';
import { ColorMode, ThemeVariantId } from '../theme/themes';

interface DeveloperMenuProps {
  isOpen: boolean;
  onClose: () => void;
  themeId: ThemeVariantId;
  colorMode: ColorMode;
  tonalIcons: boolean;
  shortcutBarMode: ShortcutBarMode;
  motionSettings: MotionSettings;
  onChangeThemeId: (id: ThemeVariantId) => void;
  onChangeColorMode: (mode: ColorMode) => void;
  onChangeTonalIcons: (next: boolean) => void;
  onChangeShortcutBarMode: (mode: ShortcutBarMode) => void;
  onChangeMotionSettings: (next: MotionSettings) => void;
  onResetLayout: () => void;
}

export const DeveloperMenu: React.FC<DeveloperMenuProps> = ({
  isOpen,
  onClose,
  themeId,
  colorMode,
  tonalIcons,
  shortcutBarMode,
  motionSettings,
  onChangeThemeId,
  onChangeColorMode,
  onChangeTonalIcons,
  onChangeShortcutBarMode,
  onChangeMotionSettings,
  onResetLayout,
}) => {
  if (!isOpen) return null;

  const themes: { id: ThemeVariantId; label: string }[] = [
    { id: 'material-you', label: 'Material You' },
    { id: 'dark-bold', label: 'Dunkel & kräftig' },
    { id: 'glass', label: 'Glas' },
  ];

  const moods: { id: AnimationMood; label: string }[] = [
    { id: 'verspielt', label: 'Verspielt' },
    { id: 'normal', label: 'Normal' },
    { id: 'ruhig', label: 'Ruhig' },
  ];

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 120,
        background: 'rgba(0,0,0,0.48)',
        backdropFilter: 'blur(6px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
      }}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.94, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.94, y: 12 }}
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%',
          maxWidth: 360,
          maxHeight: '88vh',
          overflowY: 'auto',
          borderRadius: 24,
          background: '#161922',
          color: '#F8FAFC',
          border: '1px solid rgba(255,255,255,0.14)',
          boxShadow: '0 24px 60px rgba(0,0,0,0.5)',
          padding: 18,
          display: 'flex',
          flexDirection: 'column',
          gap: 16,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Sliders size={18} color="#38BDF8" />
            <span style={{ fontSize: 16, fontWeight: 700 }}>Entwicklermenü</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Entwicklermenü schließen"
            style={{
              width: 34,
              height: 34,
              borderRadius: 99,
              border: 'none',
              background: 'rgba(255,255,255,0.1)',
              color: '#F8FAFC',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <X size={17} />
          </button>
        </div>

        {/* Stilvarianten */}
        <div>
          <div style={labelStyle}>Stilvariante</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 6 }}>
            {themes.map((t) => {
              const active = themeId === t.id;
              return (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => {
                    triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                    onChangeThemeId(t.id);
                    if (t.id === 'dark-bold') onChangeColorMode('dark');
                    if (t.id === 'material-you') onChangeColorMode('light');
                  }}
                  style={chipBtnStyle(active)}
                >
                  {t.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Hell / Dunkel */}
        <div>
          <div style={labelStyle}>Farbmodus</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 6 }}>
            {(['light', 'dark'] as ColorMode[]).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => {
                  triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
                  onChangeColorMode(m);
                }}
                style={chipBtnStyle(colorMode === m)}
              >
                {m === 'light' ? 'Hell' : 'Dunkel'}
              </button>
            ))}
          </div>
        </div>

        {/* Animationsstärke */}
        <div>
          <div style={labelStyle}>Animationsstärke</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 6 }}>
            {moods.map((m) => (
              <button
                key={m.id}
                type="button"
                onClick={() => {
                  triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
                  onChangeMotionSettings({ ...motionSettings, mood: m.id });
                }}
                style={chipBtnStyle(motionSettings.mood === m.id)}
              >
                {m.label}
              </button>
            ))}
          </div>
        </div>

        {/* Sliders */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 4 }}>
              <span>Federstärke (Steifigkeit)</span>
              <span className="display-num" style={{ color: '#38BDF8', fontWeight: 700 }}>
                {motionSettings.stiffnessScale.toFixed(2)}×
              </span>
            </div>
            <input
              type="range"
              min="0.5"
              max="2.0"
              step="0.05"
              value={motionSettings.stiffnessScale}
              onChange={(e) =>
                onChangeMotionSettings({ ...motionSettings, stiffnessScale: Number(e.target.value) })
              }
              style={{ width: '100%', accentColor: '#38BDF8' }}
            />
          </div>

          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 4 }}>
              <span>Dämpfung</span>
              <span className="display-num" style={{ color: '#38BDF8', fontWeight: 700 }}>
                {motionSettings.dampingScale.toFixed(2)}×
              </span>
            </div>
            <input
              type="range"
              min="0.5"
              max="1.8"
              step="0.05"
              value={motionSettings.dampingScale}
              onChange={(e) =>
                onChangeMotionSettings({ ...motionSettings, dampingScale: Number(e.target.value) })
              }
              style={{ width: '100%', accentColor: '#38BDF8' }}
            />
          </div>
        </div>

        {/* Toggles */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          <button
            type="button"
            onClick={() =>
              onChangeMotionSettings({
                ...motionSettings,
                hapticsEnabled: !motionSettings.hapticsEnabled,
              })
            }
            style={{
              ...toggleBtnStyle(motionSettings.hapticsEnabled),
              minHeight: 44,
            }}
          >
            <Vibrate size={15} />
            <span>Haptik: {motionSettings.hapticsEnabled ? 'An' : 'Aus'}</span>
          </button>

          <button
            type="button"
            onClick={() =>
              onChangeMotionSettings({
                ...motionSettings,
                slowMotion: !motionSettings.slowMotion,
              })
            }
            style={{
              ...toggleBtnStyle(motionSettings.slowMotion),
              minHeight: 44,
            }}
          >
            <Sparkles size={15} />
            <span>Zeitlupe 0,25×: {motionSettings.slowMotion ? 'An' : 'Aus'}</span>
          </button>

          <button
            type="button"
            onClick={() => {
              triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
              onChangeTonalIcons(!tonalIcons);
            }}
            style={{
              ...toggleBtnStyle(tonalIcons),
              gridColumn: '1 / -1',
              minHeight: 42,
            }}
          >
            <Palette size={15} />
            <span>Tonale App-Icons: {tonalIcons ? 'An (Harmonisch)' : 'Aus (Bunt)'}</span>
          </button>
        </div>

        {/* 4. Reihe Kürzel-Leiste */}
        <div>
          <div style={labelStyle}>4. Reihe (Kürzel-Leiste)</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 6 }}>
            {(
              [
                { id: 'all', label: 'Alle Seiten' },
                { id: 'start-only', label: 'Nur Start' },
                { id: 'off', label: 'Aus' },
              ] as const
            ).map((opt) => (
              <button
                key={opt.id}
                type="button"
                onClick={() => {
                  triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
                  onChangeShortcutBarMode(opt.id);
                }}
                style={chipBtnStyle(shortcutBarMode === opt.id)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Reset */}
        <button
          type="button"
          onClick={() => {
            triggerHaptic('remove', { enabled: motionSettings.hapticsEnabled });
            onResetLayout();
          }}
          style={{
            minHeight: 44,
            borderRadius: 14,
            border: '1px solid rgba(239, 68, 68, 0.45)',
            background: 'rgba(239, 68, 68, 0.16)',
            color: '#FCA5A5',
            fontSize: 13.5,
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 8,
            cursor: 'pointer',
          }}
        >
          <RotateCcw size={15} />
          <span>Layout zurücksetzen</span>
        </button>
      </motion.div>
    </div>
  );
};

const labelStyle: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  color: '#94A3B8',
  marginBottom: 6,
};

function chipBtnStyle(active: boolean): React.CSSProperties {
  return {
    padding: '8px 6px',
    borderRadius: 12,
    border: active ? '1px solid #38BDF8' : '1px solid rgba(255,255,255,0.12)',
    background: active ? 'rgba(56, 189, 248, 0.2)' : 'rgba(255,255,255,0.05)',
    color: active ? '#FFFFFF' : '#CBD5E1',
    fontSize: 11.5,
    fontWeight: 700,
    cursor: 'pointer',
    minHeight: 38,
  };
}

function toggleBtnStyle(active: boolean): React.CSSProperties {
  return {
    borderRadius: 14,
    border: active ? '1px solid #34D399' : '1px solid rgba(255,255,255,0.12)',
    background: active ? 'rgba(52, 211, 153, 0.18)' : 'rgba(255,255,255,0.05)',
    color: active ? '#A7F3D0' : '#CBD5E1',
    fontSize: 12,
    fontWeight: 700,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    cursor: 'pointer',
  };
}
