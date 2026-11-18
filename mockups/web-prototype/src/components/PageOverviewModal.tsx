import React from 'react';
import { motion, Reorder } from 'framer-motion';
import { ArrowLeft, ArrowRight, Home, Plus, Trash2, X } from 'lucide-react';
import { computeLayout } from '../layout/tree';
import { LauncherPage } from '../layout/types';
import { triggerHaptic } from '../motion/haptics';
import { getSpringTransition, MotionSettings } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';

interface PageOverviewModalProps {
  pages: LauncherPage[];
  activePageIndex: number;
  homePageId: string;
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  onClose: () => void;
  onSelectPage: (index: number) => void;
  onSetHomePage: (pageId: string) => void;
  onAddPage: () => void;
  onReorderRegularPages: (reorderedRegular: LauncherPage[]) => void;
  onDeletePage: (pageId: string) => void;
}

export const PageOverviewModal: React.FC<PageOverviewModalProps> = ({
  pages,
  activePageIndex,
  homePageId,
  theme,
  motionSettings,
  onClose,
  onSelectPage,
  onSetHomePage,
  onAddPage,
  onReorderRegularPages,
  onDeletePage,
}) => {
  const spring = getSpringTransition('groupExpand', motionSettings);
  const regularPages = pages.filter((p) => p.kind !== 'all-apps');
  const allAppsPage = pages.find((p) => p.kind === 'all-apps');

  const movePage = (idx: number, delta: number) => {
    const nextIdx = idx + delta;
    if (nextIdx < 0 || nextIdx >= regularPages.length) return;
    const copy = [...regularPages];
    const [item] = copy.splice(idx, 1);
    copy.splice(nextIdx, 0, item);
    triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
    onReorderRegularPages(copy);
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.96 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.96 }}
      transition={spring}
      onPointerDown={(e) => e.stopPropagation()}
      style={{
        position: 'absolute',
        inset: 0,
        zIndex: 85,
        background: theme.overlayBackdrop,
        backdropFilter: 'blur(22px)',
        WebkitBackdropFilter: 'blur(22px)',
        color: '#FFFFFF',
        display: 'flex',
        flexDirection: 'column',
        padding: '44px 16px 22px',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
        <div>
          <div style={{ fontSize: 20, fontWeight: 700 }}>Seitenübersicht</div>
          <div style={{ fontSize: 12, opacity: 0.78 }}>
            Sortieren, Startseite festlegen oder neue Seite anlegen
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Seitenübersicht schließen"
          style={{
            width: 40,
            height: 40,
            borderRadius: 99,
            border: 'none',
            background: 'rgba(255,255,255,0.16)',
            color: '#FFFFFF',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
          }}
        >
          <X size={20} />
        </button>
      </div>

      {/* Horizontal Carousel of Scaled Pages */}
      <div
        data-scrollable="true"
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          overflowX: 'auto',
          gap: 14,
          padding: '10px 6px',
        }}
      >
        <Reorder.Group
          axis="x"
          values={regularPages}
          onReorder={(newOrder) => {
            triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
            onReorderRegularPages(newOrder);
          }}
          style={{
            display: 'flex',
            gap: 14,
            listStyle: 'none',
            padding: 0,
            margin: 0,
            alignItems: 'center',
          }}
        >
          {regularPages.map((page, idx) => {
            const fullIndex = pages.findIndex((p) => p.id === page.id);
            const isHome = page.id === homePageId;
            const isActive = fullIndex === activePageIndex;
            const miniLayout = computeLayout(page.root, { x: 4, y: 4, width: 124, height: 224 }, 3);

            return (
              <Reorder.Item
                key={page.id}
                value={page}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 8,
                  cursor: 'grab',
                  flexShrink: 0,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ fontSize: 13, fontWeight: 700 }}>{page.title}</span>
                  {isHome && (
                    <span
                      style={{
                        fontSize: 10,
                        padding: '2px 6px',
                        borderRadius: 99,
                        background: '#38BDF8',
                        color: '#0F172A',
                        fontWeight: 700,
                      }}
                    >
                      Start
                    </span>
                  )}
                </div>

                <div
                  onClick={() => {
                    triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                    onSelectPage(fullIndex);
                    onClose();
                  }}
                  style={{
                    position: 'relative',
                    width: 132,
                    height: 232,
                    borderRadius: 20,
                    background: 'rgba(15, 23, 42, 0.55)',
                    border: isActive ? '2.5px solid #FFFFFF' : '1.5px solid rgba(255,255,255,0.24)',
                    boxShadow: '0 16px 32px rgba(0,0,0,0.35)',
                    overflow: 'hidden',
                    cursor: 'pointer',
                  }}
                >
                  {miniLayout.leaves.map((leaf) => {
                    const surf = theme.getTileSurface(leaf.tile.accentHue, 'normal');
                    return (
                      <div
                        key={leaf.id}
                        style={{
                          position: 'absolute',
                          left: leaf.rect.x,
                          top: leaf.rect.y,
                          width: leaf.rect.width,
                          height: leaf.rect.height,
                          borderRadius: 7,
                          background: surf.background,
                          border: surf.border,
                          padding: 4,
                          fontSize: 7.5,
                          fontWeight: 700,
                          color: surf.textPrimary,
                          overflow: 'hidden',
                        }}
                      >
                        {leaf.tile.title}
                      </div>
                    );
                  })}
                </div>

                {/* Controls per page */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <button
                    type="button"
                    disabled={idx === 0}
                    onClick={() => movePage(idx, -1)}
                    style={miniCtrlBtn(idx === 0)}
                    title="Nach links"
                  >
                    <ArrowLeft size={13} />
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                      onSetHomePage(page.id);
                    }}
                    style={{
                      ...miniCtrlBtn(false),
                      background: isHome ? '#38BDF8' : 'rgba(255,255,255,0.16)',
                      color: isHome ? '#0F172A' : '#FFFFFF',
                    }}
                    title="Als Startseite festlegen"
                  >
                    <Home size={13} />
                  </button>
                  {regularPages.length > 1 && (
                    <button
                      type="button"
                      onClick={() => {
                        triggerHaptic('remove', { enabled: motionSettings.hapticsEnabled });
                        onDeletePage(page.id);
                      }}
                      style={{
                        ...miniCtrlBtn(false),
                        background: 'rgba(239, 68, 68, 0.25)',
                        color: '#FCA5A5',
                      }}
                      title="Seite entfernen"
                    >
                      <Trash2 size={13} />
                    </button>
                  )}
                  <button
                    type="button"
                    disabled={idx === regularPages.length - 1}
                    onClick={() => movePage(idx, 1)}
                    style={miniCtrlBtn(idx === regularPages.length - 1)}
                    title="Nach rechts"
                  >
                    <ArrowRight size={13} />
                  </button>
                </div>
              </Reorder.Item>
            );
          })}
        </Reorder.Group>

        {/* Pinned All Apps page preview on the right */}
        {allAppsPage && (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 8,
              flexShrink: 0,
              opacity: 0.9,
            }}
          >
            <span style={{ fontSize: 13, fontWeight: 700 }}>{allAppsPage.title} (Fix)</span>
            <div
              onClick={() => {
                onSelectPage(pages.length - 1);
                onClose();
              }}
              style={{
                position: 'relative',
                width: 132,
                height: 232,
                borderRadius: 20,
                background: 'rgba(15, 23, 42, 0.65)',
                border: activePageIndex === pages.length - 1 ? '2.5px solid #FFFFFF' : '1.5px solid rgba(255,255,255,0.22)',
                overflow: 'hidden',
                cursor: 'pointer',
              }}
            >
              {computeLayout(allAppsPage.root, { x: 4, y: 4, width: 124, height: 224 }, 3).leaves.map((leaf) => {
                const surf = theme.getTileSurface(leaf.tile.accentHue, 'muted');
                return (
                  <div
                    key={leaf.id}
                    style={{
                      position: 'absolute',
                      left: leaf.rect.x,
                      top: leaf.rect.y,
                      width: leaf.rect.width,
                      height: leaf.rect.height,
                      borderRadius: 7,
                      background: surf.background,
                      border: surf.border,
                      padding: 4,
                      fontSize: 7.5,
                      fontWeight: 700,
                      color: surf.textPrimary,
                    }}
                  >
                    {leaf.tile.title}
                  </div>
                );
              })}
            </div>
            <span style={{ fontSize: 11, opacity: 0.68 }}>Immer ganz rechts</span>
          </div>
        )}
      </div>

      {/* Add Page Button */}
      <button
        type="button"
        onClick={() => {
          triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
          onAddPage();
        }}
        style={{
          marginTop: 12,
          minHeight: 46,
          borderRadius: 16,
          border: '1px solid rgba(255,255,255,0.28)',
          background: 'rgba(255,255,255,0.16)',
          color: '#FFFFFF',
          fontSize: 14,
          fontWeight: 700,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
          cursor: 'pointer',
        }}
      >
        <Plus size={18} />
        <span>Seite hinzufügen</span>
      </button>
    </motion.div>
  );
};

function miniCtrlBtn(disabled: boolean): React.CSSProperties {
  return {
    width: 30,
    height: 30,
    borderRadius: 99,
    border: 'none',
    background: 'rgba(255,255,255,0.16)',
    color: '#FFFFFF',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: disabled ? 'default' : 'pointer',
    opacity: disabled ? 0.3 : 1,
  };
}
