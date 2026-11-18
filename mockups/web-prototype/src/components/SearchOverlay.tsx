import React, { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { CornerDownLeft, Search, User, X } from 'lucide-react';
import { APPS_CATALOG, CONTACTS_MOCK, SETTINGS_MOCK } from '../data/mockData';
import { triggerHaptic } from '../motion/haptics';
import { getSpringTransition, MotionSettings } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';
import { AppSquircle, renderGlyph } from './AppIcon';

interface SearchOverlayProps {
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  onClose: () => void;
  onOpenApp: (appId: string, sourceRect?: DOMRect) => void;
  onSearchWeb: (query: string, sourceRect?: DOMRect) => void;
}

export const SearchOverlay: React.FC<SearchOverlayProps> = ({
  theme,
  motionSettings,
  onClose,
  onOpenApp,
  onSearchWeb,
}) => {
  const [query, setQuery] = useState('');
  const trimmedQuery = query.trim();
  const q = trimmedQuery.toLowerCase();

  const results = useMemo(() => {
    const apps = q
      ? APPS_CATALOG.filter(
          (a) =>
            a.name.toLowerCase().includes(q) ||
            a.category.toLowerCase().includes(q) ||
            a.subtitle.toLowerCase().includes(q)
        )
      : APPS_CATALOG.slice(0, 8);

    const contacts = q
      ? CONTACTS_MOCK.filter(
          (c) => c.name.toLowerCase().includes(q) || c.role.toLowerCase().includes(q)
        )
      : CONTACTS_MOCK.slice(0, 2);

    const settings = q
      ? SETTINGS_MOCK.filter(
          (s) => s.name.toLowerCase().includes(q) || s.detail.toLowerCase().includes(q)
        )
      : SETTINGS_MOCK.slice(0, 2);

    return { apps: apps.slice(0, 12), contacts, settings };
  }, [q]);

  const spring = getSpringTransition('groupExpand', motionSettings);

  return (
    <motion.div
      initial={{ opacity: 0, y: 48 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 48 }}
      transition={spring}
      onPointerDown={(e) => e.stopPropagation()}
      style={{
        position: 'absolute',
        inset: 0,
        zIndex: 78,
        background: theme.sheetBg,
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        color: theme.textPrimary,
        display: 'flex',
        flexDirection: 'column',
        padding: '44px 14px 20px',
      }}
    >
      {/* Search Bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          padding: '6px 12px',
          borderRadius: 20,
          border: `1px solid ${theme.sheetBorder}`,
          background: theme.mode === 'light' && theme.id !== 'glass' ? 'rgba(0,0,0,0.05)' : 'rgba(255,255,255,0.09)',
          marginBottom: 14,
        }}
      >
        <Search size={18} color={theme.textSecondary} />
        <input
          autoFocus
          value={query}
          enterKeyHint="search"
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              const rect = e.currentTarget.getBoundingClientRect();
              triggerHaptic('snap', {
                enabled: motionSettings.hapticsEnabled,
                x: rect.left + rect.width * 0.5,
                y: rect.top + rect.height * 0.5,
              });
              onSearchWeb(trimmedQuery || 'KachelOS Android Launcher', rect);
            } else if (e.key === 'Escape') {
              e.preventDefault();
              onClose();
            }
          }}
          placeholder="Apps, Kontakte oder Web-Suche (Enter)…"
          style={{
            flex: 1,
            border: 'none',
            outline: 'none',
            background: 'transparent',
            color: theme.textPrimary,
            fontSize: 15,
            fontWeight: 500,
            minHeight: 36,
          }}
        />
        <button
          type="button"
          onClick={() => {
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
            if (query) {
              setQuery('');
            } else {
              onClose();
            }
          }}
          style={{
            width: 34,
            height: 34,
            borderRadius: 99,
            border: 'none',
            background: 'transparent',
            color: theme.textSecondary,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
          }}
          aria-label="Suche schließen"
        >
          <X size={18} />
        </button>
      </div>

      {/* Results Grid as Tiles */}
      <div
        data-scrollable="true"
        style={{
          flex: 1,
          overflowY: 'auto',
          touchAction: 'pan-y',
          display: 'flex',
          flexDirection: 'column',
          gap: 14,
        }}
      >
        {/* Web-Suche (Enter) */}
        {trimmedQuery.length > 0 && (() => {
          const webSurf = theme.getTileSurface(199, 'normal');
          return (
            <div>
              <div style={sectionHeaderStyle(theme.textMuted)}>Web-Suche</div>
              <button
                type="button"
                onClick={(e) => {
                  triggerHaptic('snap', {
                    enabled: motionSettings.hapticsEnabled,
                    x: e.clientX,
                    y: e.clientY,
                  });
                  onSearchWeb(trimmedQuery, e.currentTarget.getBoundingClientRect());
                }}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 10,
                  padding: 10,
                  borderRadius: 18,
                  background: webSurf.background,
                  border: webSurf.border,
                  color: webSurf.textPrimary,
                  textAlign: 'left',
                  cursor: 'pointer',
                  minHeight: 60,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
                  <AppSquircle iconKey="Compass" color="#0EA5E9" size={38} />
                  <div style={{ minWidth: 0 }}>
                    <div
                      style={{
                        fontSize: 13.5,
                        fontWeight: 700,
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                    >
                      Im Browser suchen: „{trimmedQuery}“
                    </div>
                    <div style={{ fontSize: 11, color: webSurf.textSecondary }}>
                      Netz · Web-Suche öffnen
                    </div>
                  </div>
                </div>
                <span
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 4,
                    padding: '4px 8px',
                    borderRadius: 99,
                    background: webSurf.accentBg,
                    color: webSurf.accentColor,
                    fontSize: 11,
                    fontWeight: 700,
                    flexShrink: 0,
                  }}
                >
                  <CornerDownLeft size={12} />
                  <span>Enter</span>
                </span>
              </button>
            </div>
          );
        })()}

        {/* Apps */}
        {results.apps.length > 0 && (
          <div>
            <div style={sectionHeaderStyle(theme.textMuted)}>Apps</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8 }}>
              {results.apps.map((app) => {
                const surf = theme.getTileSurface(app.hue, 'normal');
                return (
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
                      alignItems: 'center',
                      gap: 10,
                      padding: 10,
                      borderRadius: 18,
                      background: surf.background,
                      border: surf.border,
                      color: surf.textPrimary,
                      textAlign: 'left',
                      cursor: 'pointer',
                      minHeight: 60,
                    }}
                  >
                    <AppSquircle appId={app.id} iconKey={app.iconKey} color={app.color} size={38} badge={app.badge} />
                    <div style={{ minWidth: 0 }}>
                      <div style={{ fontSize: 13.5, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {app.name}
                      </div>
                      <div style={{ fontSize: 11, color: surf.textSecondary, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {app.category}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Kontakte */}
        {results.contacts.length > 0 && (
          <div>
            <div style={sectionHeaderStyle(theme.textMuted)}>Kontakte</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8 }}>
              {results.contacts.map((c) => {
                const surf = theme.getTileSurface(c.hue, 'muted');
                return (
                  <button
                    key={c.id}
                    type="button"
                    onClick={(e) => {
                      triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                      onOpenApp('app-telefon', e.currentTarget.getBoundingClientRect());
                    }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 10,
                      padding: 10,
                      borderRadius: 18,
                      background: surf.background,
                      border: surf.border,
                      color: surf.textPrimary,
                      textAlign: 'left',
                      cursor: 'pointer',
                    }}
                  >
                    <div
                      style={{
                        width: 36,
                        height: 36,
                        borderRadius: 99,
                        background: surf.accentBg,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}
                    >
                      <User size={18} color={surf.accentColor} />
                    </div>
                    <div style={{ minWidth: 0 }}>
                      <div style={{ fontSize: 13, fontWeight: 700 }}>{c.name}</div>
                      <div style={{ fontSize: 11, color: surf.textSecondary, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {c.role}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Einstellungen */}
        {results.settings.length > 0 && (
          <div>
            <div style={sectionHeaderStyle(theme.textMuted)}>Einstellungen</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {results.settings.map((s) => {
                const surf = theme.getTileSurface(s.hue, 'muted');
                return (
                  <button
                    key={s.id}
                    type="button"
                    onClick={(e) => {
                      triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                      onOpenApp('app-einstellungen', e.currentTarget.getBoundingClientRect());
                    }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 12,
                      padding: '10px 12px',
                      borderRadius: 18,
                      background: surf.background,
                      border: surf.border,
                      color: surf.textPrimary,
                      textAlign: 'left',
                      cursor: 'pointer',
                    }}
                  >
                    <div
                      style={{
                        width: 36,
                        height: 36,
                        borderRadius: 12,
                        background: surf.accentBg,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      {renderGlyph(s.iconKey, 18, surf.accentColor)}
                    </div>
                    <div>
                      <div style={{ fontSize: 13.5, fontWeight: 700 }}>{s.name}</div>
                      <div style={{ fontSize: 11.5, color: surf.textSecondary }}>{s.detail}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {results.apps.length === 0 && results.contacts.length === 0 && results.settings.length === 0 && (
          <div style={{ padding: 24, textAlign: 'center', color: theme.textMuted, fontSize: 13 }}>
            Keine lokalen Treffer für „{query}“ – oben tippen oder Enter für Web-Suche.
          </div>
        )}
      </div>

      <button
        type="button"
        onClick={onClose}
        style={{
          marginTop: 10,
          minHeight: 44,
          borderRadius: 14,
          border: `1px solid ${theme.sheetBorder}`,
          background: 'transparent',
          color: theme.textPrimary,
          fontSize: 13.5,
          fontWeight: 600,
          cursor: 'pointer',
        }}
      >
        Schließen
      </button>
    </motion.div>
  );
};

function sectionHeaderStyle(color: string): React.CSSProperties {
  return {
    fontSize: 11,
    fontWeight: 700,
    letterSpacing: '0.05em',
    textTransform: 'uppercase',
    color,
    marginBottom: 8,
    paddingLeft: 2,
  };
}
