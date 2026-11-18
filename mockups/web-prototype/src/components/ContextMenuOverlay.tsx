import React from 'react';
import { motion } from 'framer-motion';
import { ArrowUpRight, Info, Maximize2, SplitSquareHorizontal, Sparkles, Trash2 } from 'lucide-react';
import { TileData } from '../layout/types';
import { triggerHaptic } from '../motion/haptics';
import { getSpringTransition, MotionSettings } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';

export interface ContextMenuState {
  leafId: string;
  pageId: string;
  tile: TileData;
  rect: { x: number; y: number; width: number; height: number };
}

interface ContextMenuOverlayProps {
  menu: ContextMenuState;
  containerRect: { width: number; height: number };
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  onClose: () => void;
  onEnterEditMode: () => void;
  onQuickGrow: (pageId: string, leafId: string) => void;
  onQuickSplit: (pageId: string, leafId: string, rect: ContextMenuState['rect']) => void;
  onRemoveTile: (pageId: string, leafId: string) => void;
  onShowInfo: (tile: TileData, rect: ContextMenuState['rect']) => void;
  onShortcutAction: (label: string, tile: TileData, rect: ContextMenuState['rect']) => void;
}

export const ContextMenuOverlay: React.FC<ContextMenuOverlayProps> = ({
  menu,
  containerRect,
  theme,
  motionSettings,
  onClose,
  onEnterEditMode,
  onQuickGrow,
  onQuickSplit,
  onRemoveTile,
  onShowInfo,
  onShortcutAction,
}) => {
  const menuWidth = 232;
  const menuHeight = 328;

  // Position cleanly below or above the target tile without clipping outside screen
  const preferredLeft = Math.min(
    containerRect.width - menuWidth - 12,
    Math.max(12, menu.rect.x + menu.rect.width * 0.5 - menuWidth * 0.5)
  );
  const spaceBelow = containerRect.height - (menu.rect.y + menu.rect.height);
  const preferredTop =
    spaceBelow >= menuHeight + 16
      ? menu.rect.y + menu.rect.height + 8
      : Math.max(46, menu.rect.y - menuHeight - 8);

  const shortcuts = getShortcutsForTile(menu.tile);
  const spring = getSpringTransition('gapOpen', motionSettings);

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
        zIndex: 75,
        background: 'rgba(0,0,0,0.28)',
        backdropFilter: 'blur(4px)',
      }}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.9, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.92, y: 6 }}
        transition={spring}
        onClick={(e) => e.stopPropagation()}
        style={{
          position: 'absolute',
          left: preferredLeft,
          top: preferredTop,
          width: menuWidth,
          borderRadius: 20,
          background: theme.sheetBg,
          border: `1px solid ${theme.sheetBorder}`,
          color: theme.textPrimary,
          boxShadow: '0 20px 44px rgba(0,0,0,0.36)',
          overflow: 'hidden',
          padding: 6,
        }}
      >
        <div
          style={{
            padding: '8px 12px 6px',
            fontSize: 11.5,
            fontWeight: 700,
            color: theme.textMuted,
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
          }}
        >
          {menu.tile.title}
        </div>

        {shortcuts.map((sc) => (
          <button
            key={sc}
            type="button"
            onClick={() => {
              triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
              onShortcutAction(sc, menu.tile, menu.rect);
            }}
            style={menuItemStyle(theme.textPrimary)}
          >
            <Sparkles size={15} />
            <span>{sc}</span>
          </button>
        ))}

        <div style={{ height: 1, background: theme.sheetBorder, margin: '4px 8px' }} />

        <button
          type="button"
          onClick={() => {
            triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
            onQuickGrow(menu.pageId, menu.leafId);
          }}
          style={menuItemStyle(theme.textPrimary)}
        >
          <ArrowUpRight size={15} />
          <span>Größer (+15 %)</span>
        </button>

        <button
          type="button"
          onClick={() => {
            triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
            onQuickSplit(menu.pageId, menu.leafId, menu.rect);
          }}
          style={menuItemStyle(theme.textPrimary)}
        >
          <SplitSquareHorizontal size={15} />
          <span>Kachel halbieren</span>
        </button>

        <button
          type="button"
          onClick={() => {
            triggerHaptic('editEnter', { enabled: motionSettings.hapticsEnabled });
            onEnterEditMode();
          }}
          style={menuItemStyle(theme.textPrimary)}
        >
          <Maximize2 size={15} />
          <span>Größe ändern</span>
        </button>

        <button
          type="button"
          onClick={() => {
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
            onShowInfo(menu.tile, menu.rect);
          }}
          style={menuItemStyle(theme.textPrimary)}
        >
          <Info size={15} />
          <span>App-Info</span>
        </button>

        <button
          type="button"
          onClick={() => {
            triggerHaptic('remove', { enabled: motionSettings.hapticsEnabled });
            onRemoveTile(menu.pageId, menu.leafId);
          }}
          style={menuItemStyle('#EF4444')}
        >
          <Trash2 size={15} />
          <span>Von Seite entfernen</span>
        </button>
      </motion.div>
    </motion.div>
  );
};

function menuItemStyle(color: string): React.CSSProperties {
  return {
    width: '100%',
    minHeight: 42,
    padding: '8px 12px',
    borderRadius: 13,
    border: 'none',
    background: 'transparent',
    color,
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    fontSize: 13.5,
    fontWeight: 600,
    cursor: 'pointer',
    textAlign: 'left',
  };
}

function getShortcutsForTile(tile: TileData): string[] {
  switch (tile.type) {
    case 'clock':
      return ['Wecker stellen', 'Timer 25 Min.'];
    case 'weather':
    case 'temp-curve':
    case 'greeting':
      return ['Regenradar öffnen', 'Standort wechseln'];
    case 'calendar':
    case 'calendar-timeline':
      return ['Neuer Termin', 'Wochenansicht'];
    case 'media':
      return ['Mix wechseln', 'Warteschlange'];
    case 'app-group':
      return ['Gruppe umbenennen', 'Apps verwalten'];
    case 'reminders':
    case 'note':
      return ['Neuer Eintrag', 'Alle abhaken'];
    default:
      return ['Schnellaktion', 'App-Info'];
  }
}
