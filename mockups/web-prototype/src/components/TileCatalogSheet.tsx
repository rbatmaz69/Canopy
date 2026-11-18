import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Plus, X } from 'lucide-react';
import { APPS_CATALOG, CATALOG_EXTRA_TILES } from '../data/mockData';
import { createNodeId } from '../layout/tree';
import { TileData } from '../layout/types';
import { triggerHaptic } from '../motion/haptics';
import { getSpringTransition, MotionSettings } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';
import { AppSquircle } from './AppIcon';

type CatalogTab = 'Eigene Kacheln' | 'Widgets' | 'Apps' | 'Gruppen';
const TABS: CatalogTab[] = ['Eigene Kacheln', 'Widgets', 'Apps', 'Gruppen'];

interface TileCatalogSheetProps {
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  onClose: () => void;
  onSelectTile: (tile: TileData) => void;
  onStartCatalogDrag: (tile: TileData, clientX: number, clientY: number) => void;
}

export const TileCatalogSheet: React.FC<TileCatalogSheetProps> = ({
  theme,
  motionSettings,
  onClose,
  onSelectTile,
  onStartCatalogDrag,
}) => {
  const [tab, setTab] = useState<CatalogTab>('Eigene Kacheln');
  const spring = getSpringTransition('groupExpand', motionSettings);

  const widgetItems: TileData[] = [
    {
      id: 'cat-w-podcast',
      type: 'third-party-widget',
      title: 'Podcast',
      appId: 'app-podcast',
      widgetKind: 'podcast',
      widgetFixedWidth: 158,
      widgetFixedHeight: 148,
      accentHue: 271,
    },
    {
      id: 'cat-w-fitness',
      type: 'third-party-widget',
      title: 'Fitness',
      appId: 'app-fitness',
      widgetKind: 'fitness',
      widgetFixedWidth: 148,
      widgetFixedHeight: 112,
      accentHue: 158,
    },
  ];

  const groupItems: TileData[] = [
    {
      id: 'cat-g-prod',
      type: 'app-group',
      title: 'Produktivität',
      accentHue: 215,
      appIds: ['app-mail', 'app-kalender', 'app-notizen', 'app-aufgaben', 'app-code', 'app-tabellen'],
    },
    {
      id: 'cat-g-unterwegs',
      type: 'app-group',
      title: 'Unterwegs',
      accentHue: 160,
      appIds: ['app-karten', 'app-bahn', 'app-wetter', 'app-kamera', 'app-wallet'],
    },
    {
      id: 'cat-g-studio',
      type: 'app-group',
      title: 'Kreativ',
      accentHue: 328,
      appIds: ['app-skizze', 'app-pinsel', 'app-musik', 'app-galerie'],
    },
  ];

  const buildFreshTile = (template: TileData): TileData => ({
    ...template,
    id: createNodeId('tile'),
  });

  const handlePointerStart = (tileTemplate: TileData, e: React.PointerEvent) => {
    const startX = e.clientX;
    const startY = e.clientY;
    let moved = false;

    const onMove = (ev: PointerEvent) => {
      if (!moved && Math.hypot(ev.clientX - startX, ev.clientY - startY) > 10) {
        moved = true;
        cleanup();
        onStartCatalogDrag(buildFreshTile(tileTemplate), ev.clientX, ev.clientY);
      }
    };

    const onUp = () => {
      cleanup();
      if (!moved) {
        triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled, x: startX, y: startY });
        onSelectTile(buildFreshTile(tileTemplate));
      }
    };

    const cleanup = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      window.removeEventListener('pointercancel', onUp);
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
    window.addEventListener('pointercancel', onUp);
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
      onPointerDown={(e) => e.stopPropagation()}
      style={{
        position: 'absolute',
        inset: 0,
        zIndex: 82,
        background: 'rgba(0,0,0,0.4)',
        backdropFilter: 'blur(6px)',
        display: 'flex',
        alignItems: 'flex-end',
      }}
    >
      <motion.div
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        transition={spring}
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%',
          height: '72%',
          borderTopLeftRadius: 28,
          borderTopRightRadius: 28,
          background: theme.sheetBg,
          borderTop: `1px solid ${theme.sheetBorder}`,
          color: theme.textPrimary,
          display: 'flex',
          flexDirection: 'column',
          padding: '16px 14px 20px',
          boxShadow: '0 -16px 40px rgba(0,0,0,0.35)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div>
            <div style={{ fontSize: 18, fontWeight: 700 }}>Kachel hinzufügen</div>
            <div style={{ fontSize: 12, color: theme.textMuted }}>
              Tippen oder direkt auf eine Seite ziehen
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Katalog schließen"
            style={{
              width: 38,
              height: 38,
              borderRadius: 99,
              border: 'none',
              background: 'rgba(125,125,145,0.16)',
              color: theme.textPrimary,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Category Pills */}
        <div style={{ display: 'flex', gap: 6, overflowX: 'auto', marginBottom: 12, flexShrink: 0 }}>
          {TABS.map((t) => {
            const active = tab === t;
            return (
              <button
                key={t}
                type="button"
                onClick={() => {
                  triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
                  setTab(t);
                }}
                style={{
                  padding: '7px 12px',
                  borderRadius: 999,
                  border: active ? 'none' : `1px solid ${theme.sheetBorder}`,
                  background: active ? theme.pillActive : 'transparent',
                  color: active ? (theme.id === 'material-you' && theme.mode === 'light' ? '#FFF' : '#111') : theme.textSecondary,
                  fontSize: 12,
                  fontWeight: 700,
                  whiteSpace: 'nowrap',
                  cursor: 'pointer',
                }}
              >
                {t}
              </button>
            );
          })}
        </div>

        {/* Content */}
        <div
          data-scrollable="true"
          style={{
            flex: 1,
            overflowY: 'auto',
            touchAction: 'pan-y',
            display: 'grid',
            gridTemplateColumns: tab === 'Apps' ? 'repeat(3, 1fr)' : 'repeat(2, 1fr)',
            gap: 10,
            alignContent: 'start',
          }}
        >
          {tab === 'Eigene Kacheln' &&
            CATALOG_EXTRA_TILES.map((item) => {
              const surf = theme.getTileSurface(item.accentHue, 'normal');
              return (
                <div
                  key={item.id}
                  onPointerDown={(e) => handlePointerStart(item, e)}
                  style={{
                    padding: 13,
                    borderRadius: 20,
                    background: surf.background,
                    border: surf.border,
                    color: surf.textPrimary,
                    minHeight: 102,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    cursor: 'grab',
                    touchAction: 'none',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: 14, fontWeight: 700 }}>{item.title}</span>
                    <Plus size={15} color={surf.accentColor} />
                  </div>
                  <div style={{ fontSize: 11.5, color: surf.textSecondary }}>
                    {getPreviewSnippet(item)}
                  </div>
                </div>
              );
            })}

          {tab === 'Widgets' &&
            widgetItems.map((item) => {
              const surf = theme.getTileSurface(item.accentHue, 'normal');
              return (
                <div
                  key={item.id}
                  onPointerDown={(e) => handlePointerStart(item, e)}
                  style={{
                    padding: 13,
                    borderRadius: 20,
                    background: surf.background,
                    border: surf.border,
                    color: surf.textPrimary,
                    minHeight: 112,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    cursor: 'grab',
                    touchAction: 'none',
                  }}
                >
                  <span style={{ fontSize: 10.5, fontWeight: 700, color: surf.accentColor, textTransform: 'uppercase' }}>
                    Passepartout-Widget
                  </span>
                  <div>
                    <div style={{ fontSize: 15, fontWeight: 700 }}>{item.title}</div>
                    <div style={{ fontSize: 11.5, color: surf.textSecondary }}>
                      {item.widgetFixedWidth}×{item.widgetFixedHeight} dp
                    </div>
                  </div>
                </div>
              );
            })}

          {tab === 'Apps' &&
            APPS_CATALOG.slice(0, 24).map((app) => {
              const tileTemplate: TileData = {
                id: `cat-app-${app.id}`,
                type: 'single-app',
                title: app.name,
                appId: app.id,
                accentHue: app.hue,
              };
              const surf = theme.getTileSurface(app.hue, 'muted');
              return (
                <div
                  key={app.id}
                  onPointerDown={(e) => handlePointerStart(tileTemplate, e)}
                  style={{
                    padding: 10,
                    borderRadius: 18,
                    background: surf.background,
                    border: surf.border,
                    color: surf.textPrimary,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: 6,
                    cursor: 'grab',
                    touchAction: 'none',
                  }}
                >
                  <AppSquircle iconKey={app.iconKey} color={app.color} size={40} badge={app.badge} />
                  <span style={{ fontSize: 11.5, fontWeight: 600, textAlign: 'center', maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {app.name}
                  </span>
                </div>
              );
            })}

          {tab === 'Gruppen' &&
            groupItems.map((grp) => {
              const surf = theme.getTileSurface(grp.accentHue, 'normal');
              return (
                <div
                  key={grp.id}
                  onPointerDown={(e) => handlePointerStart(grp, e)}
                  style={{
                    padding: 13,
                    borderRadius: 20,
                    background: surf.background,
                    border: surf.border,
                    color: surf.textPrimary,
                    minHeight: 104,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    cursor: 'grab',
                    touchAction: 'none',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ fontSize: 14, fontWeight: 700 }}>{grp.title}</span>
                    <span className="display-num" style={{ fontSize: 12, color: surf.accentColor, fontWeight: 700 }}>
                      {grp.appIds?.length} Apps
                    </span>
                  </div>
                  <div style={{ fontSize: 11.5, color: surf.textSecondary }}>
                    Dynamisches Kachel-Gitter
                  </div>
                </div>
              );
            })}
        </div>
      </motion.div>
    </motion.div>
  );
};

function getPreviewSnippet(tile: TileData): string {
  switch (tile.type) {
    case 'clock':
      return '09:01 · Wecker 07:15';
    case 'weather':
      return '7° · Regen ab 17 Uhr';
    case 'calendar':
      return 'Game Engines, 10:00';
    case 'media':
      return 'Lo-fi Mix · Steuerung';
    case 'note':
      return 'Federwerte & Checkliste';
    case 'photos':
      return 'Ken-Burns Galerie';
    case 'battery':
      return '84 % · ca. 19 Std.';
    case 'steps':
      return '7.420 Schritte heute';
    case 'quick-toggles':
      return 'WLAN, Bluetooth, Licht, Fokus';
    case 'reminders':
      return '3 Aufgaben zum Abhaken';
    default:
      return 'Live-Kachel';
  }
}
