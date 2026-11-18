import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Check, Pencil, Plus, X } from 'lucide-react';
import { APPS_BY_ID, APPS_CATALOG } from '../data/mockData';
import { TileData } from '../layout/types';
import { triggerHaptic } from '../motion/haptics';
import { getSpringTransition, MotionSettings } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';
import { AppSquircle } from './AppIcon';

interface AppGroupOverlayProps {
  tile: TileData;
  initialMode?: 'view' | 'rename' | 'manage';
  sourceRect?: { x: number; y: number; width: number; height: number };
  containerRect: { width: number; height: number };
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  onClose: () => void;
  onOpenApp: (appId: string, rect?: DOMRect) => void;
  onUpdateGroup: (updater: (prev: TileData) => TileData) => void;
}

export const AppGroupOverlay: React.FC<AppGroupOverlayProps> = ({
  tile,
  initialMode = 'view',
  sourceRect,
  containerRect,
  theme,
  motionSettings,
  onClose,
  onOpenApp,
  onUpdateGroup,
}) => {
  const [isRenaming, setIsRenaming] = useState(initialMode === 'rename');
  const [isManaging, setIsManaging] = useState(initialMode === 'manage');
  const [titleDraft, setTitleDraft] = useState(tile.title);

  useEffect(() => {
    setTitleDraft(tile.title);
  }, [tile.title]);

  const currentIds = tile.appIds ?? [];
  const apps = currentIds.map((id) => APPS_BY_ID[id]).filter(Boolean);
  const surface = theme.getTileSurface(tile.accentHue, 'hero');
  const spring = getSpringTransition('groupExpand', motionSettings);

  const commitRename = () => {
    const clean = titleDraft.trim();
    if (clean) {
      triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
      onUpdateGroup((prev) => ({ ...prev, title: clean }));
    }
    setIsRenaming(false);
  };

  const toggleAppInGroup = (appId: string, clientX?: number, clientY?: number) => {
    triggerHaptic('press', {
      enabled: motionSettings.hapticsEnabled,
      x: clientX,
      y: clientY,
    });
    onUpdateGroup((prev) => {
      const ids = prev.appIds ?? [];
      const exists = ids.includes(appId);
      if (exists && ids.length <= 1) {
        // Keep at least 1 app so group stays usable
        return prev;
      }
      return {
        ...prev,
        appIds: exists ? ids.filter((id) => id !== appId) : [...ids, appId],
      };
    });
  };

  const initialBox = sourceRect
    ? {
        top: sourceRect.y,
        left: sourceRect.x,
        width: sourceRect.width,
        height: sourceRect.height,
        borderRadius: theme.tileRadius,
      }
    : {
        top: 120,
        left: 24,
        width: containerRect.width - 48,
        height: 280,
        borderRadius: theme.tileRadius,
      };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.18 }}
      onClick={onClose}
      onPointerDown={(e) => e.stopPropagation()}
      style={{
        position: 'absolute',
        inset: 0,
        zIndex: 70,
        background: theme.overlayBackdrop,
        backdropFilter: 'blur(14px)',
        WebkitBackdropFilter: 'blur(14px)',
      }}
    >
      <motion.div
        initial={initialBox}
        animate={{
          top: 52,
          left: 14,
          width: containerRect.width - 28,
          height: containerRect.height - 96,
          borderRadius: 28,
        }}
        exit={initialBox}
        transition={spring}
        onClick={(e) => e.stopPropagation()}
        style={{
          position: 'absolute',
          background: theme.isGlass ? 'rgba(26, 32, 54, 0.85)' : surface.background,
          border: surface.border,
          color: surface.textPrimary,
          padding: 20,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          boxShadow: '0 24px 60px rgba(0,0,0,0.45)',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 8,
            marginBottom: 14,
          }}
        >
          <div style={{ minWidth: 0, flex: 1 }}>
            {isRenaming ? (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  commitRename();
                }}
                style={{ display: 'flex', alignItems: 'center', gap: 6 }}
              >
                <input
                  autoFocus
                  value={titleDraft}
                  onChange={(e) => setTitleDraft(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Escape') {
                      setIsRenaming(false);
                      setTitleDraft(tile.title);
                    }
                  }}
                  aria-label="Gruppenname bearbeiten"
                  style={{
                    flex: 1,
                    minWidth: 0,
                    fontSize: 18,
                    fontWeight: 700,
                    padding: '6px 10px',
                    borderRadius: 12,
                    border: `1px solid ${surface.accentColor}`,
                    background: 'rgba(0,0,0,0.16)',
                    color: surface.textPrimary,
                    outline: 'none',
                  }}
                />
                <button
                  type="submit"
                  aria-label="Speichern"
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 12,
                    border: 'none',
                    background: surface.accentColor,
                    color: '#FFFFFF',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    cursor: 'pointer',
                    flexShrink: 0,
                  }}
                >
                  <Check size={17} />
                </button>
              </form>
            ) : (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <button
                  type="button"
                  onClick={() => {
                    triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                    setIsRenaming(true);
                  }}
                  title="Gruppenname ändern"
                  style={{
                    background: 'transparent',
                    border: 'none',
                    padding: 0,
                    color: surface.textPrimary,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 7,
                    cursor: 'pointer',
                    textAlign: 'left',
                    minWidth: 0,
                  }}
                >
                  <span
                    style={{
                      fontSize: 22,
                      fontWeight: 700,
                      letterSpacing: '-0.02em',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {tile.title}
                  </span>
                  <Pencil size={14} style={{ opacity: 0.72, flexShrink: 0 }} />
                </button>
              </div>
            )}
            <div style={{ fontSize: 12, color: surface.textSecondary, marginTop: 2 }}>
              {isManaging
                ? `${apps.length} ausgewählt · Tippen zum Hinzufügen/Entfernen`
                : `${apps.length} Apps in dieser Gruppe`}
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
            <button
              type="button"
              aria-label="Apps verwalten"
              aria-pressed={isManaging}
              onClick={() => {
                triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                setIsManaging((v) => !v);
              }}
              style={{
                height: 38,
                padding: '0 10px',
                borderRadius: 99,
                border: isManaging ? `1.5px solid ${surface.accentColor}` : 'none',
                background: isManaging ? surface.accentColor : surface.accentBg,
                color: isManaging ? '#FFFFFF' : surface.textPrimary,
                display: 'flex',
                alignItems: 'center',
                gap: 4,
                fontSize: 11.5,
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              {isManaging ? <Check size={14} /> : <Plus size={14} />}
              <span>{isManaging ? 'Fertig' : 'Apps +/−'}</span>
            </button>

            <button
              type="button"
              onClick={() => {
                triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                onClose();
              }}
              style={{
                width: 40,
                height: 40,
                borderRadius: 99,
                border: 'none',
                background: surface.accentBg,
                color: surface.textPrimary,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
              aria-label="Gruppe schließen"
            >
              <X size={20} />
            </button>
          </div>
        </div>

        <div
          data-scrollable="true"
          style={{
            flex: 1,
            overflowY: 'auto',
            touchAction: 'pan-y',
            display: 'grid',
            gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
            gap: '14px 8px',
            alignContent: 'start',
          }}
        >
          {isManaging
            ? APPS_CATALOG.map((app) => {
                const selected = currentIds.includes(app.id);
                return (
                  <button
                    key={app.id}
                    type="button"
                    aria-pressed={selected}
                    onClick={(e) => toggleAppInGroup(app.id, e.clientX, e.clientY)}
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 5,
                      padding: '6px 2px',
                      borderRadius: 14,
                      border: selected ? `1.5px solid ${surface.accentColor}` : '1.5px solid transparent',
                      background: selected ? surface.accentBg : 'transparent',
                      color: surface.textPrimary,
                      opacity: selected ? 1 : 0.62,
                      cursor: 'pointer',
                      position: 'relative',
                    }}
                  >
                    <AppSquircle appId={app.id} iconKey={app.iconKey} color={app.color} size={46} />
                    {selected && (
                      <span
                        style={{
                          position: 'absolute',
                          top: 4,
                          right: 6,
                          width: 16,
                          height: 16,
                          borderRadius: 99,
                          background: surface.accentColor,
                          color: '#FFFFFF',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <Check size={10} strokeWidth={3} />
                      </span>
                    )}
                    <span
                      style={{
                        fontSize: 10.5,
                        fontWeight: 600,
                        lineHeight: 1.2,
                        textAlign: 'center',
                        maxWidth: 74,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {app.name}
                    </span>
                  </button>
                );
              })
            : apps.map((app) => (
                <button
                  key={app.id}
                  type="button"
                  onClick={(e) => {
                    triggerHaptic('press', {
                      enabled: motionSettings.hapticsEnabled,
                      x: e.clientX,
                      y: e.clientY,
                    });
                    onOpenApp(app.id, e.currentTarget.getBoundingClientRect());
                  }}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: 6,
                    cursor: 'pointer',
                    minHeight: 44,
                    background: 'transparent',
                    border: 'none',
                    color: surface.textPrimary,
                    padding: '4px 2px',
                  }}
                >
                  <AppSquircle
                    appId={app.id}
                    iconKey={app.iconKey}
                    color={app.color}
                    size={52}
                    badge={app.badge}
                    isNew={app.isNew}
                  />
                  <span
                    style={{
                      fontSize: 11,
                      fontWeight: 500,
                      lineHeight: 1.2,
                      textAlign: 'center',
                      maxWidth: 78,
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden',
                    }}
                  >
                    {app.name}
                  </span>
                </button>
              ))}
        </div>
      </motion.div>
    </motion.div>
  );
};
