import React, { useRef } from 'react';
import { motion } from 'framer-motion';
import { Disc3, Pause, Play, SkipBack, SkipForward } from 'lucide-react';
import { MEDIA_TRACKS } from '../data/mockData';
import { triggerHaptic } from '../motion/haptics';
import { TileContentProps } from './tileProps';

export const MediaTile: React.FC<TileContentProps> = ({
  tile,
  tier,
  height,
  surface,
  motionSettings,
  isEditMode,
  mediaState,
}) => {
  const track = MEDIA_TRACKS[mediaState.trackIndex % MEDIA_TRACKS.length];
  const barRef = useRef<HTMLDivElement>(null);
  const isLargeHero = Boolean(tile.mediaLarge) || tier === 'L';

  const handleScrub = (clientX: number) => {
    if (!barRef.current || isEditMode) return;
    const rect = barRef.current.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / Math.max(1, rect.width)));
    mediaState.onSeek(ratio);
  };

  const formatTime = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <button
          type="button"
          onClick={(e) => {
            if (isEditMode) return;
            e.stopPropagation();
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled, x: e.clientX, y: e.clientY });
            mediaState.onTogglePlay();
          }}
          style={{
            width: 38,
            height: 38,
            borderRadius: 999,
            border: 'none',
            background: surface.accentColor,
            color: '#FFFFFF',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
          }}
        >
          {mediaState.isPlaying ? <Pause size={18} /> : <Play size={18} style={{ marginLeft: 2 }} />}
        </button>
      </div>
    );
  }

  if (tier === 'TICKER') {
    return (
      <div className="tile-row-between" style={{ gap: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
          <div
            style={{
              width: 34,
              height: 34,
              borderRadius: 10,
              background: `linear-gradient(135deg, hsl(${track.accentHue} 82% 55%), hsl(${(track.accentHue + 55) % 360} 75% 42%))`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Disc3 size={18} color="#FFFFFF" />
          </div>
          <div style={{ minWidth: 0 }}>
            <div style={{ fontSize: 14, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {track.title}
            </div>
            <div style={{ fontSize: 11, color: surface.textSecondary, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {track.artist}
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={(e) => {
            if (isEditMode) return;
            e.stopPropagation();
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled, x: e.clientX, y: e.clientY });
            mediaState.onTogglePlay();
          }}
          style={{
            width: 34,
            height: 34,
            borderRadius: 999,
            border: 'none',
            background: surface.accentBg,
            color: surface.textPrimary,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            flexShrink: 0,
          }}
        >
          {mediaState.isPlaying ? <Pause size={16} /> : <Play size={16} />}
        </button>
      </div>
    );
  }

  const CoverArt = (
    <svg
      viewBox="0 0 320 320"
      preserveAspectRatio="xMidYMid slice"
      style={{ width: '100%', height: '100%', display: 'block' }}
    >
      <defs>
        <linearGradient id={`cov-${track.id}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={`hsl(${track.accentHue} 78% 48%)`} />
          <stop offset="55%" stopColor={`hsl(${(track.accentHue + 42) % 360} 72% 30%)`} />
          <stop offset="100%" stopColor="#0F1020" />
        </linearGradient>
      </defs>
      <rect width="320" height="320" fill={`url(#cov-${track.id})`} />
      <circle cx="160" cy="140" r="96" fill="none" stroke="rgba(255,255,255,0.22)" strokeWidth="2" />
      <circle cx="160" cy="140" r="68" fill="none" stroke="rgba(255,255,255,0.34)" strokeWidth="18" />
      <circle cx="160" cy="140" r="24" fill="rgba(255,255,255,0.88)" />
      <path d="M0,255 Q85,205 170,240 T320,220 L320,320 L0,320 Z" fill="rgba(0,0,0,0.35)" />
    </svg>
  );

  if (isLargeHero) {
    return (
      <div
        style={{
          position: 'relative',
          width: '100%',
          height: '100%',
          borderRadius: 'inherit',
          overflow: 'hidden',
        }}
      >
        <div style={{ position: 'absolute', inset: 0 }}>{CoverArt}</div>
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: 'linear-gradient(to top, rgba(10,11,18,0.88) 12%, rgba(10,11,18,0.22) 65%, rgba(10,11,18,0.35) 100%)',
            padding: 16,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            color: '#FFFFFF',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 7,
                padding: '4px 10px',
                borderRadius: 999,
                background: 'rgba(255,255,255,0.18)',
                backdropFilter: 'blur(8px)',
                fontSize: 11,
                fontWeight: 600,
              }}
            >
              {mediaState.isPlaying && (
                <span style={{ display: 'inline-flex', alignItems: 'flex-end', gap: 2, height: 11 }}>
                  {[0, 1, 2, 3].map((bar) => (
                    <motion.span
                      key={bar}
                      animate={{ height: ['3px', '11px', '5px', '10px'] }}
                      transition={{
                        duration: 0.7 + bar * 0.14,
                        repeat: Infinity,
                        repeatType: 'mirror',
                      }}
                      style={{
                        width: 2,
                        borderRadius: 99,
                        background: '#34D399',
                      }}
                    />
                  ))}
                </span>
              )}
              <span>{mediaState.isPlaying ? 'Wiedergabe läuft' : 'Pausiert'}</span>
            </span>
            <span style={{ fontSize: 12, opacity: 0.8 }}>{track.album}</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div>
              <div style={{ fontSize: 24, fontWeight: 700, letterSpacing: '-0.02em', lineHeight: 1.1 }}>
                {track.title}
              </div>
              <div style={{ fontSize: 13, opacity: 0.84, marginTop: 3 }}>{track.artist}</div>
            </div>

            {/* Draggable Scrubber */}
            <div>
              <div
                ref={barRef}
                onPointerDown={(e) => {
                  if (isEditMode) return;
                  e.stopPropagation();
                  (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
                  handleScrub(e.clientX);
                }}
                onPointerMove={(e) => {
                  if (isEditMode || e.buttons === 0) return;
                  e.stopPropagation();
                  handleScrub(e.clientX);
                }}
                onClick={(e) => e.stopPropagation()}
                style={{
                  padding: '8px 0',
                  cursor: 'pointer',
                  touchAction: 'none',
                }}
              >
                <div style={{ position: 'relative', height: 5, borderRadius: 99, background: 'rgba(255,255,255,0.26)' }}>
                  <div
                    style={{
                      width: `${mediaState.progress * 100}%`,
                      height: '100%',
                      borderRadius: 99,
                      background: '#FFFFFF',
                    }}
                  />
                  <div
                    style={{
                      position: 'absolute',
                      top: -4,
                      left: `calc(${mediaState.progress * 100}% - 6px)`,
                      width: 13,
                      height: 13,
                      borderRadius: 99,
                      background: '#FFFFFF',
                      boxShadow: '0 2px 6px rgba(0,0,0,0.4)',
                    }}
                  />
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, opacity: 0.78 }} className="display-num">
                <span>{formatTime(track.durationSec * mediaState.progress)}</span>
                <span>{formatTime(track.durationSec)}</span>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 22 }}>
              <button
                type="button"
                onClick={(e) => {
                  if (isEditMode) return;
                  e.stopPropagation();
                  triggerHaptic('press', { enabled: motionSettings.hapticsEnabled, x: e.clientX, y: e.clientY });
                  mediaState.onPrev();
                }}
                style={heroBtnStyle}
                aria-label="Zurück"
              >
                <SkipBack size={20} />
              </button>
              <button
                type="button"
                onClick={(e) => {
                  if (isEditMode) return;
                  e.stopPropagation();
                  triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled, x: e.clientX, y: e.clientY });
                  mediaState.onTogglePlay();
                }}
                style={{
                  ...heroBtnStyle,
                  width: 52,
                  height: 52,
                  background: '#FFFFFF',
                  color: '#111827',
                }}
                aria-label={mediaState.isPlaying ? 'Pause' : 'Abspielen'}
              >
                {mediaState.isPlaying ? <Pause size={22} /> : <Play size={22} style={{ marginLeft: 2 }} />}
              </button>
              <button
                type="button"
                onClick={(e) => {
                  if (isEditMode) return;
                  e.stopPropagation();
                  triggerHaptic('press', { enabled: motionSettings.hapticsEnabled, x: e.clientX, y: e.clientY });
                  mediaState.onNext();
                }}
                style={heroBtnStyle}
                aria-label="Weiter"
              >
                <SkipForward size={20} />
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Compact M / S / LIST card (Start page)
  return (
    <div className="tile-col-between" style={{ height: '100%' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div
          style={{
            width: tier === 'S' ? 36 : 42,
            height: tier === 'S' ? 36 : 42,
            borderRadius: 11,
            overflow: 'hidden',
            flexShrink: 0,
            boxShadow: '0 4px 10px rgba(0,0,0,0.2)',
          }}
        >
          {CoverArt}
        </div>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 15, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {track.title}
          </div>
          <div style={{ fontSize: 12, color: surface.textSecondary, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {track.artist}
          </div>
        </div>
      </div>

      {/* Visual Waveform + Timestamps when tile has height */}
      {height >= 165 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 2.5, height: 22 }}>
            {[35, 68, 48, 88, 62, 95, 42, 74, 86, 54, 78, 92, 46, 70, 58, 40].map((h, i) => {
              const active = i / 16 <= mediaState.progress;
              return (
                <div
                  key={i}
                  style={{
                    flex: 1,
                    height: `${h}%`,
                    borderRadius: 99,
                    background: active ? surface.accentColor : surface.accentBg,
                    transition: 'background 180ms ease',
                  }}
                />
              );
            })}
          </div>
          <div className="display-num" style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: surface.textSecondary }}>
            <span>{formatTime(track.durationSec * mediaState.progress)}</span>
            <span>{formatTime(track.durationSec)}</span>
          </div>
        </div>
      )}

      {/* Draggable Scrubber */}
      <div
        ref={barRef}
        onPointerDown={(e) => {
          if (isEditMode) return;
          e.stopPropagation();
          (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
          handleScrub(e.clientX);
        }}
        onPointerMove={(e) => {
          if (isEditMode || e.buttons === 0) return;
          e.stopPropagation();
          handleScrub(e.clientX);
        }}
        onClick={(e) => e.stopPropagation()}
        style={{ padding: '6px 0', cursor: 'pointer', touchAction: 'none' }}
      >
        <div style={{ height: 4, borderRadius: 99, background: surface.accentBg, overflow: 'hidden' }}>
          <div style={{ width: `${mediaState.progress * 100}%`, height: '100%', background: surface.accentColor }} />
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button
          type="button"
          onClick={(e) => {
            if (isEditMode) return;
            e.stopPropagation();
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled, x: e.clientX, y: e.clientY });
            mediaState.onPrev();
          }}
          style={{ ...compactBtnStyle, color: surface.textPrimary }}
          aria-label="Zurück"
        >
          <SkipBack size={16} />
        </button>
        <button
          type="button"
          onClick={(e) => {
            if (isEditMode) return;
            e.stopPropagation();
            triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled, x: e.clientX, y: e.clientY });
            mediaState.onTogglePlay();
          }}
          style={{
            ...compactBtnStyle,
            width: 34,
            height: 34,
            background: surface.accentColor,
            color: '#FFFFFF',
          }}
          aria-label={mediaState.isPlaying ? 'Pause' : 'Abspielen'}
        >
          {mediaState.isPlaying ? <Pause size={16} /> : <Play size={16} style={{ marginLeft: 1 }} />}
        </button>
        <button
          type="button"
          onClick={(e) => {
            if (isEditMode) return;
            e.stopPropagation();
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled, x: e.clientX, y: e.clientY });
            mediaState.onNext();
          }}
          style={{ ...compactBtnStyle, color: surface.textPrimary }}
          aria-label="Weiter"
        >
          <SkipForward size={16} />
        </button>
      </div>
    </div>
  );
};

const heroBtnStyle: React.CSSProperties = {
  width: 42,
  height: 42,
  borderRadius: 999,
  border: 'none',
  background: 'rgba(255,255,255,0.18)',
  color: '#FFFFFF',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'pointer',
};

const compactBtnStyle: React.CSSProperties = {
  width: 30,
  height: 30,
  borderRadius: 999,
  border: 'none',
  background: 'transparent',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'pointer',
};
