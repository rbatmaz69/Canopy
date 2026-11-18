import React, { useRef } from 'react';
import { Folder } from 'lucide-react';
import { APPS_BY_ID } from '../data/mockData';
import { AppSquircle } from '../components/AppIcon';
import { triggerHaptic } from '../motion/haptics';
import { TileContentProps } from './tileProps';

export const AppGroupTile: React.FC<TileContentProps> = ({
  tile,
  leafId,
  width,
  height,
  tier,
  theme,
  surface,
  motionSettings,
  isEditMode,
  onOpenApp,
  onOpenGroup,
}) => {
  const rootRef = useRef<HTMLDivElement>(null);
  const apps = (tile.appIds ?? []).map((id) => APPS_BY_ID[id]).filter(Boolean);

  const openGroupOverlay = (e?: React.MouseEvent) => {
    if (isEditMode) return;
    e?.stopPropagation();
    triggerHaptic('press', {
      enabled: motionSettings.hapticsEnabled,
      x: e?.clientX,
      y: e?.clientY,
    });
    onOpenGroup(tile, leafId, rootRef.current?.getBoundingClientRect());
  };

  if (tier === 'XS') {
    return (
      <div
        ref={rootRef}
        className="tile-center"
        onClick={openGroupOverlay}
        style={{ width: '100%', height: '100%', cursor: 'pointer' }}
      >
        <div
          style={{
            position: 'relative',
            width: 44,
            height: 44,
            borderRadius: 13,
            background: surface.accentBg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Folder size={22} color={surface.accentColor} />
          <span
            className="display-num"
            style={{
              position: 'absolute',
              bottom: -4,
              right: -4,
              minWidth: 18,
              height: 18,
              padding: '0 4px',
              borderRadius: 99,
              background: surface.accentColor,
              color: '#FFFFFF',
              fontSize: 10,
              fontWeight: 700,
              lineHeight: '18px',
              textAlign: 'center',
            }}
          >
            {apps.length}
          </span>
        </div>
      </div>
    );
  }

  if (tier === 'TICKER') {
    const maxInline = Math.max(2, Math.floor((width - 110) / 46));
    const overflow = Math.max(0, apps.length - maxInline);
    const visible = overflow > 0 ? apps.slice(0, maxInline - 1) : apps.slice(0, maxInline);

    return (
      <div
        ref={rootRef}
        className="tile-row-between"
        style={{ width: '100%', height: '100%', gap: 8 }}
      >
        <button
          type="button"
          onClick={openGroupOverlay}
          style={{
            background: 'transparent',
            border: 'none',
            padding: 0,
            color: surface.textPrimary,
            fontSize: 14,
            fontWeight: 700,
            cursor: 'pointer',
            flexShrink: 0,
          }}
        >
          {tile.title}
        </button>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          {visible.map((app) => (
            <AppSquircle
              key={app.id}
              appId={app.id}
              iconKey={app.iconKey}
              color={app.color}
              size={38}
              badge={app.badge}
              onClick={(e) => {
                if (isEditMode) return;
                e.stopPropagation();
                onOpenApp(app.id, (e.currentTarget as HTMLElement).getBoundingClientRect());
              }}
            />
          ))}
          {overflow > 0 && (
            <button
              type="button"
              onClick={openGroupOverlay}
              className="display-num"
              style={{
                width: 38,
                height: 38,
                borderRadius: 11,
                border: 'none',
                background: surface.accentBg,
                color: surface.accentColor,
                fontSize: 12,
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              +{overflow + 1}
            </button>
          )}
        </div>
      </div>
    );
  }

  // Rich vertical list layout for compact 2-3 app groups (e.g. "Lesen") so there is zero wasted height
  if (apps.length <= 3 && width >= 135 && height >= 140) {
    return (
      <div
        ref={rootRef}
        style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
        }}
      >
        <button
          type="button"
          onClick={openGroupOverlay}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            background: 'transparent',
            border: 'none',
            padding: '0 2px',
            marginBottom: 4,
            color: surface.textPrimary,
            cursor: 'pointer',
            width: '100%',
            textAlign: 'left',
          }}
        >
          <span style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-0.01em' }}>
            {tile.title}
          </span>
          <span className="display-num" style={{ fontSize: 11, fontWeight: 600, color: surface.textSecondary }}>
            {apps.length}
          </span>
        </button>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-around', gap: 4 }}>
          {apps.map((app, idx) => (
            <div
              key={app.id}
              onClick={(e) => {
                if (isEditMode) return;
                e.stopPropagation();
                triggerHaptic('press', {
                  enabled: motionSettings.hapticsEnabled,
                  x: e.clientX,
                  y: e.clientY,
                });
                onOpenApp(app.id, (e.currentTarget as HTMLElement).getBoundingClientRect());
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 9,
                padding: '3px 2px',
                cursor: 'pointer',
                borderBottom: idx < apps.length - 1 ? `1px solid ${surface.accentBg}` : 'none',
                minHeight: 40,
              }}
            >
              <AppSquircle appId={app.id} iconKey={app.iconKey} color={app.color} size={36} badge={app.badge} />
              <div style={{ minWidth: 0, flex: 1 }}>
                <div style={{ fontSize: 12.5, fontWeight: 700, color: surface.textPrimary, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {app.name}
                </div>
                <div style={{ fontSize: 10, color: surface.textSecondary, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {app.subtitle}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // Standard density-aware Bento grid calculation (>= 44dp cells, larger icons, zero empty slots)
  const innerW = Math.max(44, width - 26);
  const headerH = 26;
  const innerH = Math.max(44, height - 26 - headerH);

  const is4x2EightPack = apps.length === 8 && width >= 250 && height >= 130;
  const is2x2FourPack = apps.length === 4 && width >= 130 && height >= 130;
  const is2x2CompactFive = apps.length === 5 && width < 220 && height >= 130;
  const is3x2MediumEight = apps.length === 8 && width < 250 && height >= 130;
  const showLabels = height >= 140 && width >= 130;
  const cellH = showLabels ? 58 : 44;

  const cols = is4x2EightPack
    ? 4
    : is2x2FourPack || is2x2CompactFive
      ? 2
      : is3x2MediumEight
        ? 3
        : width > 290
          ? 5
          : width > 185
            ? 3
            : 2;

  const isWideOverflowStrip = width > 290 && height < 275 && apps.length > 10;
  const maxRows = isWideOverflowStrip
    ? 2
    : Math.max(1, Math.min(4, Math.floor((innerH + 6) / (cellH + 6))));
  const rows =
    is4x2EightPack || is2x2FourPack || is2x2CompactFive || is3x2MediumEight
      ? 2
      : Math.max(1, Math.min(maxRows, Math.ceil(apps.length / cols)));
  const capacity = cols * rows;

  const needsOverflow = apps.length > capacity;
  const visibleCount = needsOverflow ? Math.max(0, capacity - 1) : apps.length;
  const hiddenCount = apps.length - visibleCount;
  const visibleApps = apps.slice(0, visibleCount);

  const maxIconByW = Math.floor((innerW - (cols - 1) * 6) / cols) - 10;
  const maxIconByH = Math.floor((innerH - (rows - 1) * 6) / rows) - (showLabels ? 22 : 8);
  const iconSize = Math.min(60, Math.max(36, Math.min(maxIconByW, maxIconByH)));
  const labelMaxW = Math.max(44, Math.floor((innerW - (cols - 1) * 6) / cols) + 2);
  const bentoCellBg = theme.isGlass
    ? 'rgba(255, 255, 255, 0.08)'
    : theme.mode === 'dark' || theme.isBold
      ? 'rgba(255, 255, 255, 0.05)'
      : 'rgba(15, 23, 42, 0.038)';

  return (
    <div
      ref={rootRef}
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
    >
      <button
        type="button"
        onClick={openGroupOverlay}
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'transparent',
          border: 'none',
          padding: '0 2px',
          marginBottom: 6,
          color: surface.textPrimary,
          cursor: 'pointer',
          width: '100%',
          textAlign: 'left',
        }}
      >
        <span style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-0.01em' }}>
          {tile.title}
        </span>
        <span
          className="display-num"
          style={{
            fontSize: 11,
            fontWeight: 600,
            color: surface.textSecondary,
          }}
        >
          {apps.length}
        </span>
      </button>

      <div
        style={{
          flex: 1,
          display: 'grid',
          gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))`,
          gridTemplateRows: `repeat(${rows}, minmax(0, 1fr))`,
          gap: 6,
          alignItems: 'stretch',
          justifyItems: 'stretch',
        }}
      >
        {visibleApps.map((app) => (
          <div
            key={app.id}
            onClick={(e) => {
              if (isEditMode) return;
              e.stopPropagation();
              triggerHaptic('press', {
                enabled: motionSettings.hapticsEnabled,
                x: e.clientX,
                y: e.clientY,
              });
              onOpenApp(app.id, (e.currentTarget as HTMLElement).getBoundingClientRect());
            }}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 4,
              width: '100%',
              height: '100%',
              minWidth: 44,
              minHeight: 44,
              borderRadius: 14,
              background: bentoCellBg,
              boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.12)',
              padding: '4px 2px',
              cursor: 'pointer',
            }}
          >
            <AppSquircle
              appId={app.id}
              iconKey={app.iconKey}
              color={app.color}
              size={iconSize}
              badge={app.badge}
            />
            {showLabels && (
              <span
                style={{
                  fontSize: 10,
                  fontWeight: 500,
                  lineHeight: 1.15,
                  color: surface.textSecondary,
                  maxWidth: labelMaxW,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  textAlign: 'center',
                }}
              >
                {app.name}
              </span>
            )}
          </div>
        ))}

        {needsOverflow && (
          <button
            type="button"
            onClick={openGroupOverlay}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 3,
              width: '100%',
              height: '100%',
              minWidth: 44,
              minHeight: 44,
              borderRadius: 14,
              border: `1.5px dashed ${surface.accentColor}`,
              background: surface.accentBg,
              color: surface.accentColor,
              cursor: 'pointer',
              padding: '4px 2px',
            }}
          >
            <span className="display-num" style={{ fontSize: 16, fontWeight: 700 }}>
              +{hiddenCount}
            </span>
            {showLabels && (
              <span style={{ fontSize: 10, fontWeight: 600 }}>Alle</span>
            )}
          </button>
        )}
      </div>
    </div>
  );
};
