import React, { useState } from 'react';
import { Activity, Headphones, Pause, Play } from 'lucide-react';
import { APPS_BY_ID } from '../data/mockData';
import { AppSquircle } from '../components/AppIcon';
import { triggerHaptic } from '../motion/haptics';
import { TileContentProps } from './tileProps';

export const SingleAppTile: React.FC<TileContentProps> = ({
  tile,
  tier,
  surface,
  motionSettings,
  isEditMode,
  onOpenApp,
}) => {
  const app = APPS_BY_ID[tile.appId ?? ''] ?? {
    id: tile.appId ?? 'app-fallback',
    name: tile.title || 'App',
    iconKey: 'Compass',
    color: '#3B82F6',
    hue: tile.accentHue ?? 217,
    category: 'Werkzeuge',
    usageScore: 50,
    subtitle: 'Bereit',
    badge: undefined,
  };

  const handleTap = (e: React.MouseEvent) => {
    if (isEditMode) return;
    e.stopPropagation();
    triggerHaptic('press', {
      enabled: motionSettings.hapticsEnabled,
      x: e.clientX,
      y: e.clientY,
    });
    onOpenApp(app.id, (e.currentTarget as HTMLElement).getBoundingClientRect());
  };

  if (tier === 'XS') {
    return (
      <div className="tile-center" onClick={handleTap} style={{ cursor: 'pointer' }}>
        <AppSquircle appId={app.id} iconKey={app.iconKey} color={app.color} size={42} badge={app.badge} />
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between" onClick={handleTap} style={{ cursor: 'pointer' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <AppSquircle appId={app.id} iconKey={app.iconKey} color={app.color} size={36} badge={app.badge} />
          <div>
            <div style={{ fontSize: 14, fontWeight: 700 }}>{app.name}</div>
            <div style={{ fontSize: 11, color: surface.textSecondary }}>{app.subtitle}</div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      className="tile-col-between"
      onClick={handleTap}
      style={{ height: '100%', cursor: 'pointer' }}
    >
      <AppSquircle
        appId={app.id}
        iconKey={app.iconKey}
        color={app.color}
        size={tier === 'L' ? 54 : 44}
        badge={app.badge}
      />
      <div>
        <div style={{ fontSize: 15, fontWeight: 700 }}>{app.name}</div>
        {tier !== 'S' && (
          <div style={{ fontSize: 12, color: surface.textSecondary, marginTop: 2 }}>
            {app.subtitle}
          </div>
        )}
      </div>
    </div>
  );
};

export const ThirdPartyWidgetTile: React.FC<TileContentProps> = (props) => {
  const { tile, width, height, surface, theme, mediaState, motionSettings, isEditMode, onOpenApp } = props;
  const isPodcast = tile.widgetKind === 'podcast';
  const [bonusSteps, setBonusSteps] = useState(0);

  const podcastDurationSec = 42 * 60;
  const podcastElapsedSec = Math.round(mediaState.progress * podcastDurationSec);
  const podMm = String(Math.floor(podcastElapsedSec / 60)).padStart(2, '0');
  const podSs = String(podcastElapsedSec % 60).padStart(2, '0');
  const activeBarCount = Math.max(1, Math.min(12, Math.round(mediaState.progress * 12)));

  const totalSteps = 7420 + bonusSteps;
  const fitnessPct = Math.min(100, Math.round((totalSteps / 10000) * 100));
  const fitnessKcal = 420 + Math.round(bonusSteps * 0.045);
  const fitnessKm = (5.4 + bonusSteps * 0.00075).toFixed(1).replace('.', ',');

  const fixedW = tile.widgetFixedWidth ?? (isPodcast ? 158 : 148);
  const fixedH = tile.widgetFixedHeight ?? (isPodcast ? 148 : 112);

  // If tile shrinks below the fixed widget minimum (accounting for tile padding ~16px),
  // automatically morph into a SingleAppTile!
  const fitsWidget = width >= fixedW + 10 && height >= fixedH + 10;

  if (!fitsWidget) {
    return <SingleAppTile {...props} />;
  }

  // Adaptive breathing scale up to 1.15x so the fixed widget blends naturally into larger frames
  const fitScale = Math.min(
    1.15,
    Math.max(0.95, Math.min((width - 20) / fixedW, (height - 42) / fixedH))
  );

  // Passepartout frame around centered widget with functional header bar
  const passepartoutBg = theme.isGlass
    ? 'rgba(255, 255, 255, 0.14)'
    : surface.accentBg;

  const innerCardBg = theme.mode === 'dark' || theme.isBold || theme.isGlass
    ? 'rgba(17, 20, 30, 0.88)'
    : 'rgba(255, 255, 255, 0.94)';
  const innerText = theme.mode === 'dark' || theme.isBold || theme.isGlass ? '#FFFFFF' : '#111827';
  const innerSub = theme.mode === 'dark' || theme.isBold || theme.isGlass
    ? 'rgba(255,255,255,0.72)'
    : '#4B5563';

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'space-between',
        background: passepartoutBg,
        borderRadius: Math.max(12, theme.tileRadius - 6),
        padding: '6px 8px 8px',
        position: 'relative',
      }}
    >
      {/* Functional Passepartout Header Bar */}
      <div
        style={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 6,
          flexShrink: 0,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, minWidth: 0 }}>
          <span
            style={{
              width: 7,
              height: 7,
              borderRadius: 99,
              background: isPodcast ? '#A855F7' : '#10B981',
              flexShrink: 0,
            }}
          />
          <span
            style={{
              fontSize: 10.5,
              fontWeight: 700,
              color: surface.textSecondary,
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {isPodcast ? 'Podcast · 14m' : `Ziel ${fitnessPct} %`}
          </span>
        </div>
        <button
          type="button"
          onClick={(e) => {
            if (isEditMode) return;
            e.stopPropagation();
            triggerHaptic('press', {
              enabled: motionSettings.hapticsEnabled,
              x: e.clientX,
              y: e.clientY,
            });
            onOpenApp(tile.appId ?? (isPodcast ? 'app-podcast' : 'app-fitness'), e.currentTarget.getBoundingClientRect());
          }}
          style={{
            padding: '2px 7px',
            borderRadius: 99,
            border: 'none',
            background: 'rgba(125,125,145,0.18)',
            color: surface.textPrimary,
            fontSize: 10,
            fontWeight: 700,
            cursor: 'pointer',
            flexShrink: 0,
          }}
        >
          Öffnen ↗
        </button>
      </div>

      <div
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '100%',
        }}
      >
        <div
          style={{
            width: fixedW,
            height: fixedH,
            transform: `scale(${fitScale})`,
            transformOrigin: 'center center',
            borderRadius: 16,
            background: innerCardBg,
            color: innerText,
            padding: 12,
            boxShadow: '0 8px 22px rgba(0,0,0,0.22), inset 0 1px 0 rgba(255,255,255,0.16)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            flexShrink: 0,
          }}
        >
        {isPodcast ? (
          <>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <div
                style={{
                  width: 30,
                  height: 30,
                  borderRadius: 9,
                  background: 'linear-gradient(135deg, #9333EA, #6366F1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Headphones size={16} color="#FFFFFF" />
              </div>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontSize: 12, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  Lage der Nation
                </div>
                <div style={{ fontSize: 10, color: innerSub }}>Folge 408 · 14m</div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 3, height: 22 }}>
              {[42, 78, 60, 95, 50, 82, 68, 90, 44, 72, 85, 52].map((h, idx) => {
                const animatedH = mediaState.isPlaying
                  ? Math.max(24, Math.min(98, h + (((Math.round(mediaState.progress * 100) + idx) % 3) - 1) * 14))
                  : h;
                return (
                  <div
                    key={idx}
                    style={{
                      flex: 1,
                      height: `${animatedH}%`,
                      borderRadius: 99,
                      background: idx < activeBarCount ? '#A855F7' : 'rgba(168,85,247,0.28)',
                      transition: 'height 220ms ease, background 180ms ease',
                    }}
                  />
                );
              })}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span className="display-num" style={{ fontSize: 11, color: innerSub }}>
                {podMm}:{podSs} / 42:00
              </span>
              <button
                type="button"
                onClick={(e) => {
                  if (isEditMode) return;
                  e.stopPropagation();
                  triggerHaptic('press', {
                    enabled: motionSettings.hapticsEnabled,
                    x: e.clientX,
                    y: e.clientY,
                  });
                  mediaState.onTogglePlay();
                }}
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: 99,
                  border: 'none',
                  background: '#9333EA',
                  color: '#FFFFFF',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                }}
                aria-label="Podcast abspielen"
              >
                {mediaState.isPlaying ? <Pause size={13} /> : <Play size={13} style={{ marginLeft: 1 }} />}
              </button>
            </div>
          </>
        ) : (
          <>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <Activity size={15} color="#10B981" />
                <span style={{ fontSize: 12, fontWeight: 700 }}>Fitness</span>
              </div>
              <span className="display-num" style={{ fontSize: 11, color: '#10B981', fontWeight: 700 }}>
                {fitnessPct} %
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 6 }}>
              <div className="display-num" style={{ fontSize: 22, fontWeight: 700, lineHeight: 1 }}>
                {totalSteps.toLocaleString('de-DE')}
              </div>
              <button
                type="button"
                aria-label="250 Schritte hinzufügen"
                onClick={(e) => {
                  if (isEditMode) return;
                  e.stopPropagation();
                  triggerHaptic('snap', {
                    enabled: motionSettings.hapticsEnabled,
                    x: e.clientX,
                    y: e.clientY,
                  });
                  setBonusSteps((s) => s + 250);
                }}
                style={{
                  padding: '3px 7px',
                  borderRadius: 99,
                  border: 'none',
                  background: 'rgba(16, 185, 129, 0.22)',
                  color: '#10B981',
                  fontSize: 10,
                  fontWeight: 700,
                  cursor: 'pointer',
                  flexShrink: 0,
                }}
              >
                +250
              </button>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10.5, color: innerSub }}>
              <span>{fitnessKcal} kcal</span>
              <span>{fitnessKm} km</span>
            </div>
          </>
        )}
        </div>
      </div>
    </div>
  );
};
