import React, { useMemo, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { ArrowUpDown, Search, Sparkles } from 'lucide-react';
import { APPS_BY_ID, APPS_CATALOG, AppDefinition } from '../data/mockData';
import { AppSquircle } from '../components/AppIcon';
import { triggerHaptic } from '../motion/haptics';
import { TileContentProps } from './tileProps';

type SortMode = 'A–Z' | 'Kategorie' | 'Farbe' | 'Nutzung';
const SORT_MODES: SortMode[] = ['A–Z', 'Kategorie', 'Farbe', 'Nutzung'];

export const SearchBarTile: React.FC<TileContentProps> = ({
  surface,
  isEditMode,
  onOpenSearch,
  motionSettings,
}) => {
  return (
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
        onOpenSearch();
      }}
      style={{
        width: '100%',
        height: '100%',
        background: 'transparent',
        border: 'none',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 4px',
        color: surface.textSecondary,
        cursor: 'pointer',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <Search size={18} color={surface.accentColor} />
        <span style={{ fontSize: 14, fontWeight: 500 }}>
          Apps, Kontakte & Einstellungen suchen…
        </span>
      </div>
      <span
        style={{
          fontSize: 11,
          fontWeight: 600,
          padding: '3px 8px',
          borderRadius: 99,
          background: surface.accentBg,
          color: surface.accentColor,
        }}
      >
        Suche
      </span>
    </button>
  );
};

export const NewlyInstalledTile: React.FC<TileContentProps> = ({
  tile,
  tier,
  surface,
  isEditMode,
  onOpenApp,
  motionSettings,
}) => {
  const apps = (tile.newAppIds ?? ['app-skizze', 'app-fokus', 'app-klima'])
    .map((id) => APPS_BY_ID[id])
    .filter(Boolean);

  if (tier === 'XS') {
    return (
      <div className="tile-center">
        <Sparkles size={20} color={surface.accentColor} />
      </div>
    );
  }

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Sparkles size={13} color={surface.accentColor} />
          <span style={{ fontSize: 12.5, fontWeight: 700, color: surface.textPrimary }}>
            Neu installiert
          </span>
        </div>
        <span style={{ fontSize: 11, color: surface.textSecondary }}>3 Apps</span>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-around', gap: 8 }}>
        {apps.map((app) => (
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
              gap: 8,
              cursor: 'pointer',
              minHeight: 44,
              padding: '2px 6px',
            }}
          >
            <AppSquircle iconKey={app.iconKey} color={app.color} size={38} isNew />
            <span style={{ fontSize: 12, fontWeight: 600, color: surface.textPrimary }}>
              {app.name}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export const AllAppsBoxTile: React.FC<TileContentProps> = ({
  surface,
  isEditMode,
  onOpenApp,
  motionSettings,
}) => {
  const [sortMode, setSortMode] = useState<SortMode>('A–Z');
  const [activeLetter, setActiveLetter] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const sectionRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const lastHapticLetter = useRef<string | null>(null);

  const grouped = useMemo(() => {
    const list = [...APPS_CATALOG];

    if (sortMode === 'A–Z') {
      list.sort((a, b) => a.name.localeCompare(b.name, 'de'));
      const map = new Map<string, AppDefinition[]>();
      for (const app of list) {
        const ch = app.name[0].toUpperCase();
        const key = /[A-ZÄÖÜ]/.test(ch) ? ch : '#';
        if (!map.has(key)) map.set(key, []);
        map.get(key)!.push(app);
      }
      return Array.from(map.entries()).map(([label, apps]) => ({ label, apps }));
    }

    if (sortMode === 'Kategorie') {
      list.sort((a, b) => a.category.localeCompare(b.category, 'de') || a.name.localeCompare(b.name, 'de'));
      const map = new Map<string, AppDefinition[]>();
      for (const app of list) {
        if (!map.has(app.category)) map.set(app.category, []);
        map.get(app.category)!.push(app);
      }
      return Array.from(map.entries()).map(([label, apps]) => ({ label, apps }));
    }

    if (sortMode === 'Farbe') {
      list.sort((a, b) => a.hue - b.hue);
      return [{ label: 'Nach Farbspektrum', apps: list }];
    }

    list.sort((a, b) => b.usageScore - a.usageScore);
    return [{ label: 'Meistgenutzt', apps: list }];
  }, [sortMode]);

  const letters = useMemo(() => {
    if (sortMode !== 'A–Z') return [];
    return grouped.map((g) => g.label);
  }, [grouped, sortMode]);

  const jumpToLetter = (letter: string, clientX: number, clientY: number) => {
    setActiveLetter(letter);
    if (lastHapticLetter.current !== letter) {
      lastHapticLetter.current = letter;
      triggerHaptic('tick', {
        enabled: motionSettings.hapticsEnabled,
        x: clientX,
        y: clientY,
      });
    }
    const target = sectionRefs.current[letter];
    if (target && scrollRef.current) {
      scrollRef.current.scrollTo({
        top: target.offsetTop - 4,
        behavior: 'auto',
      });
    }
  };

  const handleScrub = (e: React.PointerEvent<HTMLDivElement>) => {
    if (letters.length === 0) return;
    e.stopPropagation();
    const rect = e.currentTarget.getBoundingClientRect();
    const ratio = Math.min(0.999, Math.max(0, (e.clientY - rect.top) / Math.max(1, rect.height)));
    const idx = Math.floor(ratio * letters.length);
    jumpToLetter(letters[idx], e.clientX, e.clientY);
  };

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
      }}
    >
      {/* Header + Sort Chip */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 8,
          flexShrink: 0,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
          <span style={{ fontSize: 15, fontWeight: 700 }}>Alle Apps</span>
          <span className="display-num" style={{ fontSize: 11, color: surface.textSecondary }}>
            {APPS_CATALOG.length}
          </span>
        </div>

        <button
          type="button"
          onClick={(e) => {
            if (isEditMode) return;
            e.stopPropagation();
            triggerHaptic('snap', {
              enabled: motionSettings.hapticsEnabled,
              x: e.clientX,
              y: e.clientY,
            });
            const nextIdx = (SORT_MODES.indexOf(sortMode) + 1) % SORT_MODES.length;
            setSortMode(SORT_MODES[nextIdx]);
          }}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 5,
            padding: '5px 10px',
            borderRadius: 999,
            border: 'none',
            background: surface.accentBg,
            color: surface.accentColor,
            fontSize: 11.5,
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          <ArrowUpDown size={12} />
          <span>{sortMode}</span>
        </button>
      </div>

      {/* Scrollable Inner Box (ONLY vertical scroll container on home pages) */}
      <div style={{ position: 'relative', flex: 1, minHeight: 0, display: 'flex' }}>
        <div
          ref={scrollRef}
          data-scrollable="true"
          onPointerDown={(e) => e.stopPropagation()}
          style={{
            flex: 1,
            height: '100%',
            overflowY: 'auto',
            overflowX: 'hidden',
            paddingRight: letters.length > 0 ? 22 : 4,
            touchAction: 'pan-y',
            WebkitOverflowScrolling: 'touch',
          }}
        >
          {grouped.map((section) => (
            <div
              key={section.label}
              ref={(el) => {
                sectionRefs.current[section.label] = el;
              }}
              style={{ marginBottom: 14 }}
            >
              <div
                style={{
                  fontSize: 11,
                  fontWeight: 700,
                  letterSpacing: '0.04em',
                  color: surface.accentColor,
                  padding: '2px 4px 6px',
                  borderBottom: `1px solid ${surface.accentBg}`,
                  marginBottom: 8,
                }}
              >
                {section.label}
              </div>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
                  gap: '10px 6px',
                }}
              >
                {section.apps.map((app) => (
                  <motion.div
                    key={app.id}
                    layout
                    transition={{ type: 'spring', stiffness: 380, damping: 30 }}
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
                      gap: 4,
                      cursor: 'pointer',
                      minHeight: 44,
                    }}
                  >
                    <AppSquircle
                      appId={app.id}
                      iconKey={app.iconKey}
                      color={app.color}
                      size={44}
                      badge={app.badge}
                      isNew={app.isNew}
                    />
                    <span
                      style={{
                        fontSize: 10.5,
                        fontWeight: 500,
                        color: surface.textSecondary,
                        maxWidth: 68,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        textAlign: 'center',
                      }}
                    >
                      {app.name}
                    </span>
                  </motion.div>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Right A-Z Fast Scroller */}
        {letters.length > 0 && (
          <div
            onPointerDown={(e) => {
              e.stopPropagation();
              (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
              handleScrub(e);
            }}
            onPointerMove={(e) => {
              if (e.buttons === 0) return;
              handleScrub(e);
            }}
            onPointerUp={() => {
              setActiveLetter(null);
              lastHapticLetter.current = null;
            }}
            onPointerCancel={() => {
              setActiveLetter(null);
              lastHapticLetter.current = null;
            }}
            style={{
              position: 'absolute',
              right: -4,
              top: 4,
              bottom: 4,
              width: 22,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '4px 0',
              borderRadius: 99,
              background: surface.accentBg,
              touchAction: 'none',
              cursor: 'ns-resize',
              zIndex: 4,
            }}
          >
            {letters.map((l) => (
              <span
                key={l}
                style={{
                  fontSize: 9.5,
                  fontWeight: activeLetter === l ? 800 : 600,
                  color: activeLetter === l ? surface.accentColor : surface.textSecondary,
                  lineHeight: 1,
                }}
              >
                {l}
              </span>
            ))}
          </div>
        )}

        {/* Active fast-scroller floating letter bubble */}
        {activeLetter && (
          <div
            style={{
              position: 'absolute',
              right: 28,
              top: '50%',
              transform: 'translateY(-50%)',
              width: 48,
              height: 48,
              borderRadius: 16,
              background: surface.accentColor,
              color: '#FFFFFF',
              fontSize: 22,
              fontWeight: 700,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 8px 20px rgba(0,0,0,0.32)',
              pointerEvents: 'none',
              zIndex: 8,
            }}
          >
            {activeLetter}
          </div>
        )}
      </div>
    </div>
  );
};
