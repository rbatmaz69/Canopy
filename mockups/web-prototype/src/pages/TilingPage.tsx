import React, { useMemo, useRef, useState } from 'react';
import { motion, MotionStyle } from 'framer-motion';
import { Move, Trash2 } from 'lucide-react';
import { CalendarEvent } from '../data/mockData';
import {
  clampOrRubberbandRatio,
  computeLayout,
  computeSizeTier,
  projectAndSnapRatio,
  SNAP_POINTS,
} from '../layout/tree';
import {
  ComputedDivider,
  ComputedIntersection,
  ComputedLeaf,
  DropZone,
  LauncherPage,
  Rect,
  TileData,
} from '../layout/types';
import { triggerHaptic } from '../motion/haptics';
import { getSpringTransition, MotionSettings, SpringRole } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';
import { TileRenderer } from '../tiles/TileRenderer';
import { SystemControlState, TileContentProps } from '../tiles/tileProps';

export interface ActiveTileDrag {
  sourceKind: 'page-tile' | 'catalog-tile';
  sourcePageId?: string;
  sourceLeafId?: string;
  tile: TileData;
  width: number;
  height: number;
  clientX: number;
  clientY: number;
}

export interface DropPreviewResult {
  kind: 'leaf' | 'remove';
  pageId: string;
  leaf?: ComputedLeaf;
  zone?: DropZone;
}

interface TilingPageProps {
  page: LauncherPage;
  bounds: Rect;
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  isEditMode: boolean;
  isActivePage: boolean;
  activeDrag: ActiveTileDrag | null;
  dropPreview: DropPreviewResult | null;
  onEnterEditMode: () => void;
  onUpdateDividerRatio: (pageId: string, splitId: string, ratio: number) => void;
  onUpdateMultipleDividerRatios: (pageId: string, updates: Record<string, number>) => void;
  onToggleDividerDirection: (pageId: string, splitId: string) => void;
  onUpdateTile: (pageId: string, leafId: string, updater: (t: TileData) => TileData) => void;
  onOpenApp: (appId: string, sourceRect?: DOMRect) => void;
  onOpenGroup: (tile: TileData, leafId: string, sourceRect?: DOMRect) => void;
  onOpenSearch: () => void;
  onOpenFakeTileApp: (tile: TileData, sourceRect?: DOMRect) => void;
  onOpenContextMenu: (leaf: ComputedLeaf, pageId: string, domRect: DOMRect) => void;
  onCloseContextMenu: () => void;
  onStartTileDrag: (drag: ActiveTileDrag) => void;
  calendarEvents?: CalendarEvent[];
  mediaState: TileContentProps['mediaState'];
  systemState: SystemControlState;
}

export const TilingPage: React.FC<TilingPageProps> = ({
  page,
  bounds,
  theme,
  motionSettings,
  isEditMode,
  isActivePage,
  activeDrag,
  dropPreview,
  onEnterEditMode,
  onUpdateDividerRatio,
  onUpdateMultipleDividerRatios,
  onToggleDividerDirection,
  onUpdateTile,
  onOpenApp,
  onOpenGroup,
  onOpenSearch,
  onOpenFakeTileApp,
  onOpenContextMenu,
  onCloseContextMenu,
  onStartTileDrag,
  calendarEvents,
  mediaState,
  systemState,
}) => {
  const isAllApps = page.kind === 'all-apps';
  const effectiveEdit = isEditMode && !isAllApps;
  const gap = effectiveEdit ? theme.gapEdit : theme.gapUse;

  const layout = useMemo(() => computeLayout(page.root, bounds, gap), [page.root, bounds, gap]);

  const [draggingDividerId, setDraggingDividerId] = useState<string | null>(null);
  const [draggingIntersectionId, setDraggingIntersectionId] = useState<string | null>(null);
  const [dividerSpringMode, setDividerSpringMode] = useState<SpringRole>('gapOpen');
  const [squishSide, setSquishSide] = useState<{ ids: Set<string>; scale: number } | null>(null);

  const lastTapRef = useRef<{ dividerId: string; time: number }>({ dividerId: '', time: 0 });
  const lastSnapCrossRef = useRef<number | null>(null);
  const suppressNextClickRef = useRef(false);
  const prevBoundsRef = useRef<{ width: number; height: number }>(bounds);
  const isBoundsResizing =
    prevBoundsRef.current.width !== bounds.width || prevBoundsRef.current.height !== bounds.height;
  prevBoundsRef.current = { width: bounds.width, height: bounds.height };

  const activeDrop = dropPreview && dropPreview.pageId === page.id ? dropPreview : null;

  const handleIntersectionPointerDown = (
    ix: ComputedIntersection,
    e: React.PointerEvent<HTMLDivElement>
  ) => {
    if (!effectiveEdit) return;
    e.stopPropagation();
    e.currentTarget.setPointerCapture(e.pointerId);
    setDraggingIntersectionId(ix.id);

    const startX = e.clientX;
    const startY = e.clientY;

    const hInit = ix.horizontalSplits.map((d) => ({
      id: d.id,
      startRatio: d.ratio,
      parentSpan: d.parentRect.width,
      usableSpan: Math.max(1, d.parentRect.width - gap),
      minFirstPx: d.minFirstPx,
      minSecondPx: d.minSecondPx,
      minRatio: d.minRatio,
      maxRatio: d.maxRatio,
      latest: d.ratio,
    }));

    const vInit = ix.verticalSplits.map((d) => ({
      id: d.id,
      startRatio: d.ratio,
      parentSpan: d.parentRect.height,
      usableSpan: Math.max(1, d.parentRect.height - gap),
      minFirstPx: d.minFirstPx,
      minSecondPx: d.minSecondPx,
      minRatio: d.minRatio,
      maxRatio: d.maxRatio,
      latest: d.ratio,
    }));

    let prevX = startX;
    let prevY = startY;
    let prevT = performance.now();
    let vx = 0;
    let vy = 0;

    const onMove = (ev: PointerEvent) => {
      const now = performance.now();
      const dt = Math.max(1, now - prevT) / 1000;
      vx = vx * 0.45 + ((ev.clientX - prevX) / dt) * 0.55;
      vy = vy * 0.45 + ((ev.clientY - prevY) / dt) * 0.55;
      prevX = ev.clientX;
      prevY = ev.clientY;
      prevT = now;

      const dx = ev.clientX - startX;
      const dy = ev.clientY - startY;
      const updates: Record<string, number> = {};

      for (const h of hInit) {
        const raw = h.startRatio + dx / h.usableSpan;
        const next = clampOrRubberbandRatio(raw, h.parentSpan, h.minFirstPx, h.minSecondPx, 0.3, gap);
        for (const snap of SNAP_POINTS) {
          if (
            ((h.latest < snap && next >= snap) || (h.latest > snap && next <= snap)) &&
            lastSnapCrossRef.current !== snap
          ) {
            lastSnapCrossRef.current = snap;
            triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled, x: ev.clientX, y: ev.clientY });
          }
        }
        h.latest = next;
        updates[h.id] = next;
      }

      for (const v of vInit) {
        const raw = v.startRatio + dy / v.usableSpan;
        const next = clampOrRubberbandRatio(raw, v.parentSpan, v.minFirstPx, v.minSecondPx, 0.3, gap);
        for (const snap of SNAP_POINTS) {
          if (
            ((v.latest < snap && next >= snap) || (v.latest > snap && next <= snap)) &&
            lastSnapCrossRef.current !== snap
          ) {
            lastSnapCrossRef.current = snap;
            triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled, x: ev.clientX, y: ev.clientY });
          }
        }
        v.latest = next;
        updates[v.id] = next;
      }

      onUpdateMultipleDividerRatios(page.id, updates);
    };

    const onUp = (ev: PointerEvent) => {
      cleanup();
      setDraggingIntersectionId(null);
      lastSnapCrossRef.current = null;
      setDividerSpringMode('dividerSnap');

      const snapped: Record<string, number> = {};
      for (const h of hInit) {
        snapped[h.id] = projectAndSnapRatio(h.latest, vx, h.parentSpan, h.minRatio, h.maxRatio);
      }
      for (const v of vInit) {
        snapped[v.id] = projectAndSnapRatio(v.latest, vy, v.parentSpan, v.minRatio, v.maxRatio);
      }
      triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled, x: ev.clientX, y: ev.clientY });
      onUpdateMultipleDividerRatios(page.id, snapped);
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

  const handleDividerPointerDown = (
    divider: ComputedDivider,
    e: React.PointerEvent<HTMLDivElement>
  ) => {
    e.stopPropagation();

    // Gap long-press in Use Mode enters Edit Mode
    if (!effectiveEdit) {
      const startX = e.clientX;
      const startY = e.clientY;
      const timer = window.setTimeout(() => {
        triggerHaptic('editEnter', {
          enabled: motionSettings.hapticsEnabled,
          x: startX,
          y: startY,
        });
        onEnterEditMode();
      }, 380);

      const cancel = () => {
        window.clearTimeout(timer);
        window.removeEventListener('pointerup', cancel);
        window.removeEventListener('pointercancel', cancel);
      };
      window.addEventListener('pointerup', cancel);
      window.addEventListener('pointercancel', cancel);
      return;
    }

    // Double-tap on divider flips split direction
    const now = Date.now();
    if (
      lastTapRef.current.dividerId === divider.id &&
      now - lastTapRef.current.time < 320
    ) {
      lastTapRef.current = { dividerId: '', time: 0 };
      setDividerSpringMode('tileSwap');
      triggerHaptic('snap', {
        enabled: motionSettings.hapticsEnabled,
        x: e.clientX,
        y: e.clientY,
      });
      onToggleDividerDirection(page.id, divider.id);
      return;
    }
    lastTapRef.current = { dividerId: divider.id, time: now };

    const targetEl = e.currentTarget;
    targetEl.setPointerCapture(e.pointerId);
    setDraggingDividerId(divider.id);

    const isHorizontalSplit = divider.direction === 'horizontal';
    const parentOrigin = isHorizontalSplit ? divider.parentRect.x : divider.parentRect.y;
    const parentSpan = isHorizontalSplit ? divider.parentRect.width : divider.parentRect.height;
    const usableSpan = Math.max(1, parentSpan - gap);

    const startPointer = isHorizontalSplit ? e.clientX : e.clientY;
    const startRatio = divider.ratio;
    let latestRatio = startRatio;
    let prevPos = startPointer;
    let prevTime = performance.now();
    let velocityPxPerSec = 0;

    const firstSet = new Set(divider.firstLeafIds);
    const secondSet = new Set(divider.secondLeafIds);

    const onMove = (ev: PointerEvent) => {
      const curPos = isHorizontalSplit ? ev.clientX : ev.clientY;
      const curTime = performance.now();
      const dt = Math.max(1, curTime - prevTime) / 1000;
      const instVel = (curPos - prevPos) / dt;
      velocityPxPerSec = velocityPxPerSec * 0.45 + instVel * 0.55;
      prevPos = curPos;
      prevTime = curTime;

      const deltaPx = curPos - startPointer;
      const rawRatio = startRatio + deltaPx / usableSpan;
      const elasticRatio = clampOrRubberbandRatio(
        rawRatio,
        parentSpan,
        divider.minFirstPx,
        divider.minSecondPx,
        0.3,
        gap
      );

      // Subtle 1-2% neighbor squish when compressing toward a side
      if (deltaPx > 2) {
        setSquishSide({ ids: secondSet, scale: 0.985 });
      } else if (deltaPx < -2) {
        setSquishSide({ ids: firstSet, scale: 0.985 });
      }

      // Feiner Haptik-Tick when crossing any snap point
      for (const snap of SNAP_POINTS) {
        const crossed =
          (latestRatio < snap && elasticRatio >= snap) ||
          (latestRatio > snap && elasticRatio <= snap);
        if (crossed && lastSnapCrossRef.current !== snap) {
          lastSnapCrossRef.current = snap;
          triggerHaptic('tick', {
            enabled: motionSettings.hapticsEnabled,
            x: ev.clientX,
            y: ev.clientY,
          });
        }
      }

      latestRatio = elasticRatio;
      onUpdateDividerRatio(page.id, divider.id, elasticRatio);
    };

    const onUp = (ev: PointerEvent) => {
      cleanup();
      setDraggingDividerId(null);
      setSquishSide(null);
      lastSnapCrossRef.current = null;
      setDividerSpringMode('dividerSnap');

      // Project position with velocity -> snap to 1/4, 1/3, 1/2, 2/3, 3/4
      const snapped = projectAndSnapRatio(
        latestRatio,
        velocityPxPerSec,
        parentSpan,
        divider.minRatio,
        divider.maxRatio
      );
      triggerHaptic('snap', {
        enabled: motionSettings.hapticsEnabled,
        x: ev.clientX,
        y: ev.clientY,
      });
      onUpdateDividerRatio(page.id, divider.id, snapped);
    };

    const cleanup = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      window.removeEventListener('pointercancel', onUp);
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
    window.addEventListener('pointercancel', onUp);
    void parentOrigin;
  };

  const handleTilePointerDown = (leaf: ComputedLeaf, e: React.PointerEvent<HTMLDivElement>) => {
    if (isAllApps) return;
    suppressNextClickRef.current = false;
    const domEl = e.currentTarget;
    const startX = e.clientX;
    const startY = e.clientY;
    let longPressFired = false;
    let dragStarted = false;

    const startDragNow = (cx: number, cy: number) => {
      dragStarted = true;
      suppressNextClickRef.current = true;
      cleanup();
      onCloseContextMenu();
      if (!isEditMode) {
        onEnterEditMode();
      }
      onStartTileDrag({
        sourceKind: 'page-tile',
        sourcePageId: page.id,
        sourceLeafId: leaf.id,
        tile: leaf.tile,
        width: leaf.rect.width,
        height: leaf.rect.height,
        clientX: cx,
        clientY: cy,
      });
    };

    const timer = effectiveEdit
      ? null
      : window.setTimeout(() => {
          longPressFired = true;
          suppressNextClickRef.current = true;
          triggerHaptic('editEnter', {
            enabled: motionSettings.hapticsEnabled,
            x: startX,
            y: startY,
          });
          onOpenContextMenu(leaf, page.id, domEl.getBoundingClientRect());
        }, 420);

    const onMove = (ev: PointerEvent) => {
      const dist = Math.hypot(ev.clientX - startX, ev.clientY - startY);

      // In Use Mode: if finger moves before long-press fires, let parent page swipe handle it!
      if (!effectiveEdit && !longPressFired && dist > 9) {
        if (timer !== null) window.clearTimeout(timer);
        cleanup();
        return;
      }

      // After Long-Press OR already in Edit Mode: start dragging tile on movement!
      if ((effectiveEdit || longPressFired) && !dragStarted && dist > 9) {
        if (timer !== null) window.clearTimeout(timer);
        startDragNow(ev.clientX, ev.clientY);
      }
    };

    const onUp = () => {
      if (timer !== null) window.clearTimeout(timer);
      cleanup();
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

  const activeDivider = layout.dividers.find((d) => d.id === draggingDividerId) ?? null;
  const activeIntersection =
    layout.intersections.find((ix) => ix.id === draggingIntersectionId) ?? null;
  const isResizingAny = Boolean(draggingDividerId || draggingIntersectionId);

  const affectedResizeIds = useMemo(() => {
    if (activeDivider) {
      return new Set([...activeDivider.firstLeafIds, ...activeDivider.secondLeafIds]);
    }
    if (activeIntersection) {
      const s = new Set<string>();
      activeIntersection.horizontalSplits.forEach((d) => {
        d.firstLeafIds.forEach((id) => s.add(id));
        d.secondLeafIds.forEach((id) => s.add(id));
      });
      activeIntersection.verticalSplits.forEach((d) => {
        d.firstLeafIds.forEach((id) => s.add(id));
        d.secondLeafIds.forEach((id) => s.add(id));
      });
      return s;
    }
    return null;
  }, [activeDivider, activeIntersection]);

  const tileSpring = getSpringTransition(
    isResizingAny ? 'dividerSnap' : effectiveEdit ? dividerSpringMode : 'gapOpen',
    motionSettings
  );

  return (
    <div
      style={{
        position: 'relative',
        width: '100%',
        height: '100%',
        touchAction: 'none',
      }}
    >
      {/* Leaves */}
      {layout.leaves.map((leaf) => {
        const isDraggedSource =
          activeDrag?.sourceKind === 'page-tile' &&
          activeDrag.sourceLeafId === leaf.id &&
          activeDrag.sourcePageId === page.id;

        const isSquished = squishSide?.ids.has(leaf.id) ?? false;
        const targetScale = isDraggedSource
          ? 0.92
          : isSquished
            ? squishSide!.scale
            : effectiveEdit
              ? 0.985
              : 1;

        const isDropTarget = activeDrop?.kind === 'leaf' && activeDrop.leaf?.id === leaf.id;

        return (
          <motion.div
            key={leaf.id}
            initial={{
              left: leaf.rect.x,
              top: leaf.rect.y,
              width: leaf.rect.width,
              height: leaf.rect.height,
              scale: 0.9,
              opacity: 0,
            }}
            animate={{
              left: leaf.rect.x,
              top: leaf.rect.y,
              width: leaf.rect.width,
              height: leaf.rect.height,
              scale: targetScale,
              opacity: isDraggedSource ? 0.32 : 1,
            }}
            transition={
              isResizingAny || isBoundsResizing
                ? {
                    left: { duration: 0 },
                    top: { duration: 0 },
                    width: { duration: 0 },
                    height: { duration: 0 },
                    scale: getSpringTransition('pressFeedback', motionSettings),
                  }
                : tileSpring
            }
            whileTap={
              !effectiveEdit && !isAllApps
                ? {
                    scale: 0.96,
                    transition: getSpringTransition('pressFeedback', motionSettings),
                  }
                : undefined
            }
            onPointerDown={(e) => handleTilePointerDown(leaf, e)}
            onClickCapture={(e) => {
              if (suppressNextClickRef.current) {
                e.stopPropagation();
                e.preventDefault();
                suppressNextClickRef.current = false;
              }
            }}
            onClick={(e) => {
              if (effectiveEdit || isAllApps || activeDrag) return;
              if (
                leaf.tile.type !== 'app-group' &&
                leaf.tile.type !== 'search-bar' &&
                leaf.tile.type !== 'all-apps-box' &&
                leaf.tile.type !== 'quick-toggles'
              ) {
                triggerHaptic('press', {
                  enabled: motionSettings.hapticsEnabled,
                  x: e.clientX,
                  y: e.clientY,
                });
                onOpenFakeTileApp(leaf.tile, e.currentTarget.getBoundingClientRect());
              }
            }}
            style={{
              position: 'absolute',
              borderRadius: theme.tileRadius,
              cursor: effectiveEdit ? 'grab' : 'pointer',
              zIndex: isDropTarget ? 12 : 2,
            }}
          >
            <TileRenderer
              tile={leaf.tile}
              leafId={leaf.id}
              width={leaf.rect.width}
              height={leaf.rect.height}
              theme={theme}
              motionSettings={motionSettings}
              isEditMode={effectiveEdit}
              onOpenApp={onOpenApp}
              onOpenGroup={onOpenGroup}
              onOpenSearch={onOpenSearch}
              onOpenFakeTileApp={onOpenFakeTileApp}
              onUpdateTile={(lid, updater) => onUpdateTile(page.id, lid, updater)}
              calendarEvents={calendarEvents}
              mediaState={mediaState}
              systemState={systemState}
            />

            {/* Animated Drop Zone Highlight (Swap or Split Preview) */}
            {isDropTarget && activeDrop?.kind === 'leaf' && activeDrop.zone && (
              <DropZonePreview zone={activeDrop.zone} radius={theme.tileRadius} />
            )}

            {/* Live Size-Tier Preview Badge ("Ghost-Stufe") anchored at bottom-right while dragging */}
            {affectedResizeIds?.has(leaf.id) && (
              <div
                style={{
                  position: 'absolute',
                  bottom: 6,
                  right: 6,
                  padding: '3px 7px',
                  borderRadius: 99,
                  background: 'rgba(15, 23, 42, 0.88)',
                  border: '1px solid rgba(56, 189, 248, 0.72)',
                  color: '#FFFFFF',
                  fontSize: 10,
                  fontWeight: 700,
                  pointerEvents: 'none',
                  zIndex: 24,
                  boxShadow: '0 4px 12px rgba(0,0,0,0.35)',
                }}
                className="display-num"
              >
                {computeSizeTier(leaf.rect.width, leaf.rect.height)} · {Math.round(leaf.rect.width)}×{Math.round(leaf.rect.height)}
              </div>
            )}
          </motion.div>
        );
      })}

      {/* Snap Guide Lines & Fraction Labels (¼, ⅓, ½, ⅔, ¾) while dragging a 1D divider or 2D intersection */}
      {(activeDivider
        ? [activeDivider]
        : activeIntersection
          ? [...activeIntersection.horizontalSplits.slice(0, 1), ...activeIntersection.verticalSplits.slice(0, 1)]
          : []
      ).map((guideDiv) => (
        <div
          key={`guide-${guideDiv.id}`}
          style={{
            position: 'absolute',
            left: guideDiv.parentRect.x,
            top: guideDiv.parentRect.y,
            width: guideDiv.parentRect.width,
            height: guideDiv.parentRect.height,
            pointerEvents: 'none',
            zIndex: 25,
          }}
        >
          {SNAP_POINTS.map((p, idx) => {
            const labels = ['¼', '⅓', '½', '⅔', '¾'];
            const isHoriz = guideDiv.direction === 'horizontal';
            const span = isHoriz
              ? guideDiv.parentRect.width - gap
              : guideDiv.parentRect.height - gap;
            const pos = span * p + gap / 2;
            const isNear = Math.abs(guideDiv.ratio - p) < 0.025;
            return (
              <div
                key={p}
                style={{
                  position: 'absolute',
                  left: isHoriz ? pos : 0,
                  top: isHoriz ? 0 : pos,
                  width: isHoriz ? (isNear ? 2 : 1) : '100%',
                  height: isHoriz ? '100%' : isNear ? 2 : 1,
                  background: isNear ? '#38BDF8' : 'rgba(255,255,255,0.42)',
                  boxShadow: isNear ? '0 0 10px rgba(56,189,248,0.85)' : undefined,
                }}
              >
                <span
                  className="display-num"
                  style={{
                    position: 'absolute',
                    left: isHoriz ? -10 : 8,
                    top: isHoriz ? 8 : -9,
                    padding: '1px 5px',
                    borderRadius: 6,
                    background: isNear ? '#38BDF8' : 'rgba(15, 23, 42, 0.78)',
                    color: isNear ? '#0F172A' : '#FFFFFF',
                    fontSize: 9.5,
                    fontWeight: 700,
                  }}
                >
                  {labels[idx]}
                </span>
              </div>
            );
          })}
        </div>
      ))}

      {/* Divider Hit Strips & Pill Handles */}
      {!isAllApps &&
        layout.dividers.map((divider) => {
          const isHoriz = divider.direction === 'horizontal';
          const isDraggingThis = draggingDividerId === divider.id;
          return (
            <div
              key={divider.id}
              onPointerDown={(e) => handleDividerPointerDown(divider, e)}
              style={{
                position: 'absolute',
                left: isHoriz ? divider.rect.x - 7 : divider.rect.x,
                top: isHoriz ? divider.rect.y : divider.rect.y - 7,
                width: isHoriz ? divider.rect.width + 14 : divider.rect.width,
                height: isHoriz ? divider.rect.height : divider.rect.height + 14,
                zIndex: isDraggingThis ? 30 : 20,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: effectiveEdit ? (isHoriz ? 'ew-resize' : 'ns-resize') : 'default',
                touchAction: 'none',
              }}
              title={
                effectiveEdit
                  ? 'Ziehen zum Anpassen · Doppeltipp dreht Teilung'
                  : 'Fuge gedrückt halten für Bearbeiten-Modus'
              }
            >
              {effectiveEdit && (
                <motion.div
                  initial={{ scale: 0.4, opacity: 0 }}
                  animate={{
                    scale: isDraggingThis ? 1.18 : 1,
                    opacity: 1,
                  }}
                  transition={getSpringTransition('gapOpen', motionSettings)}
                  style={{
                    width: isHoriz ? 6 : 36,
                    height: isHoriz ? 36 : 6,
                    borderRadius: 99,
                    background: isDraggingThis ? '#38BDF8' : theme.dividerHandleBg,
                    boxShadow: '0 2px 10px rgba(0,0,0,0.35)',
                  }}
                />
              )}
            </div>
          );
        })}

      {/* 2D Intersection Crosshair Handles in Edit Mode */}
      {effectiveEdit &&
        layout.intersections.map((ix) => {
          const isDraggingIx = draggingIntersectionId === ix.id;
          return (
            <motion.div
              key={ix.id}
              initial={{ scale: 0.4, opacity: 0 }}
              animate={{ scale: isDraggingIx ? 1.18 : 1, opacity: 1 }}
              transition={getSpringTransition('gapOpen', motionSettings)}
              onPointerDown={(e) => handleIntersectionPointerDown(ix, e)}
              title="2D-Kreuzung ziehen: X & Y gleichzeitig anpassen"
              style={{
                position: 'absolute',
                left: ix.x - 13,
                top: ix.y - 13,
                width: 26,
                height: 26,
                borderRadius: 99,
                background: isDraggingIx ? '#38BDF8' : theme.dividerHandleBg,
                color: isDraggingIx ? '#0F172A' : theme.dividerHandleDot,
                border: '2px solid rgba(255,255,255,0.55)',
                boxShadow: '0 4px 12px rgba(0,0,0,0.42)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'move',
                touchAction: 'none',
                zIndex: 35,
              }}
            >
              <Move size={13} strokeWidth={2.5} />
            </motion.div>
          );
        })}

      {/* Bottom Throw-to-Remove Drop Target during tile drag */}
      {activeDrag?.sourceKind === 'page-tile' && isActivePage && layout.leaves.length > 1 && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: 12 }}
          style={{
            position: 'absolute',
            left: 14,
            right: 14,
            bottom: 4,
            height: 44,
            borderRadius: 16,
            background:
              activeDrop?.kind === 'remove'
                ? 'rgba(239, 68, 68, 0.92)'
                : 'rgba(239, 68, 68, 0.25)',
            border: '1.5px dashed rgba(252, 165, 165, 0.85)',
            color: '#FFFFFF',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 8,
            fontSize: 12,
            fontWeight: 700,
            pointerEvents: 'none',
            zIndex: 28,
          }}
        >
          <Trash2 size={15} />
          <span>Nach unten werfen zum Entfernen</span>
        </motion.div>
      )}
    </div>
  );
};

const DropZonePreview: React.FC<{ zone: DropZone; radius: number }> = ({ zone, radius }) => {
  const boxStyle: MotionStyle =
    zone === 'center'
      ? { inset: 4 }
      : zone === 'left'
        ? { left: 4, top: 4, bottom: 4, width: '48%' }
        : zone === 'right'
          ? { right: 4, top: 4, bottom: 4, width: '48%' }
          : zone === 'top'
            ? { left: 4, right: 4, top: 4, height: '48%' }
            : { left: 4, right: 4, bottom: 4, height: '48%' };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.92 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: 'spring', stiffness: 420, damping: 28 }}
      style={{
        position: 'absolute',
        ...boxStyle,
        borderRadius: Math.max(10, radius - 4),
        background: 'rgba(56, 189, 248, 0.34)',
        border: '2px solid #38BDF8',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        pointerEvents: 'none',
        zIndex: 20,
      }}
    >
      <span
        style={{
          padding: '4px 10px',
          borderRadius: 99,
          background: '#0F172A',
          color: '#FFFFFF',
          fontSize: 11,
          fontWeight: 700,
        }}
      >
        {zone === 'center' ? 'Tauschen' : 'Teilen'}
      </span>
    </motion.div>
  );
};
