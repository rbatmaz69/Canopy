import React, { useRef } from 'react';
import { motion, MotionValue, useTransform } from 'framer-motion';
import { LayoutGrid } from 'lucide-react';
import { LauncherPage } from '../layout/types';
import { triggerHaptic } from '../motion/haptics';
import { MotionSettings } from '../motion/springs';
import { getDockCapsuleTokens, ThemeTokens } from '../theme/themes';

interface PageIndicatorProps {
  pages: LauncherPage[];
  activePageIndex: number;
  pageWidth: number;
  pageOffsetX: MotionValue<number>;
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  onSelectPage: (index: number) => void;
  onOpenOverview: () => void;
}

export const PageIndicator: React.FC<PageIndicatorProps> = ({
  pages,
  activePageIndex,
  pageWidth,
  pageOffsetX,
  theme,
  motionSettings,
  onSelectPage,
  onOpenOverview,
}) => {
  const dockRef = useRef<HTMLDivElement>(null);
  const longPressTimer = useRef<number | null>(null);
  const longPressed = useRef(false);
  const scrubbing = useRef(false);
  const lastScrubIdx = useRef<number>(activePageIndex);

  const maxPageIdx = Math.max(1, pages.length - 1);

  // Subtle continuous progress indicator bar gliding along the bottom of the dock
  const progressRatio = useTransform(pageOffsetX, (x) => {
    const p = Math.min(maxPageIdx, Math.max(0, -x / Math.max(1, pageWidth)));
    return `${(p / pages.length) * 100}%`;
  });

  const clearLongPress = () => {
    if (longPressTimer.current !== null) {
      window.clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
  };

  const scrubToClientX = (clientX: number) => {
    if (!dockRef.current) return;
    const rect = dockRef.current.getBoundingClientRect();
    const ratio = Math.min(0.999, Math.max(0, (clientX - rect.left) / Math.max(1, rect.width)));
    const idx = Math.floor(ratio * pages.length);
    if (idx !== lastScrubIdx.current) {
      lastScrubIdx.current = idx;
      triggerHaptic('tick', {
        enabled: motionSettings.hapticsEnabled,
        x: clientX,
        y: rect.top + rect.height / 2,
      });
      onSelectPage(idx);
    }
  };

  const { dockBg, activePillBg, activeTextColor } = getDockCapsuleTokens(theme);

  return (
    <div
      ref={dockRef}
      onPointerDown={(e) => {
        e.stopPropagation();
        const startX = e.clientX;
        longPressed.current = false;
        scrubbing.current = false;
        lastScrubIdx.current = activePageIndex;
        clearLongPress();

        longPressTimer.current = window.setTimeout(() => {
          if (!scrubbing.current) {
            longPressed.current = true;
            triggerHaptic('editEnter', {
              enabled: motionSettings.hapticsEnabled,
              x: e.clientX,
              y: e.clientY,
            });
            onOpenOverview();
          }
        }, 420);

        const onMove = (ev: PointerEvent) => {
          if (Math.abs(ev.clientX - startX) > 10) {
            scrubbing.current = true;
            clearLongPress();
            scrubToClientX(ev.clientX);
          }
        };

        const onUp = () => {
          clearLongPress();
          window.removeEventListener('pointermove', onMove);
          window.removeEventListener('pointerup', onUp);
          window.removeEventListener('pointercancel', onUp);
        };

        window.addEventListener('pointermove', onMove);
        window.addEventListener('pointerup', onUp);
        window.addEventListener('pointercancel', onUp);
      }}
      style={{
        position: 'relative',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: 28,
        maxWidth: 'calc(100% - 92px)',
        padding: '3px 4px',
        borderRadius: 999,
        background: dockBg,
        border: `1px solid ${theme.sheetBorder}`,
        backdropFilter: 'blur(14px)',
        WebkitBackdropFilter: 'blur(14px)',
        touchAction: 'none',
        cursor: 'pointer',
        gap: 2,
        overflow: 'hidden',
        flexShrink: 1,
        minWidth: 0,
      }}
      title="Tippen oder streichen zum Wechseln · Gedrückt halten für Seitenübersicht"
    >
      {/* Subtle 1.5px gliding swipe progress line along bottom edge */}
      <motion.div
        style={{
          position: 'absolute',
          bottom: 0,
          left: progressRatio,
          width: `${100 / pages.length}%`,
          height: 2,
          borderRadius: 99,
          background: theme.pillActive,
          opacity: 0.45,
          pointerEvents: 'none',
        }}
      />

      {pages.map((page, idx) => {
        const isAllApps = page.kind === 'all-apps';
        const isActive = idx === activePageIndex;
        return (
          <button
            key={page.id}
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              if (longPressed.current || scrubbing.current) return;
              triggerHaptic('tick', {
                enabled: motionSettings.hapticsEnabled,
                x: e.clientX,
                y: e.clientY,
              });
              onSelectPage(idx);
            }}
            aria-label={`Seite ${page.title}`}
            style={{
              position: 'relative',
              height: 22,
              padding: isAllApps ? '0 6px' : '0 7px',
              border: 'none',
              borderRadius: 999,
              background: 'transparent',
              color: isActive ? activeTextColor : theme.statusText,
              opacity: isActive ? 1 : 0.72,
              fontSize: pages.length > 5 ? 10 : 10.5,
              fontWeight: 700,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            {isActive && (
              <motion.span
                layoutId="active-page-scrubber-pill"
                transition={{ type: 'spring', stiffness: 460, damping: 32 }}
                style={{
                  position: 'absolute',
                  inset: 0,
                  borderRadius: 999,
                  background: activePillBg,
                  zIndex: 0,
                }}
              />
            )}
            <span style={{ position: 'relative', zIndex: 1, display: 'flex', alignItems: 'center' }}>
              {isAllApps ? <LayoutGrid size={12} strokeWidth={2.4} /> : page.title}
            </span>
          </button>
        );
      })}
    </div>
  );
};
