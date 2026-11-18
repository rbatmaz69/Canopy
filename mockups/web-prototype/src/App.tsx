import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, animate, motion, useMotionValue, useTransform } from 'framer-motion';
import { useDrag, usePinch } from '@use-gesture/react';
import {
  Battery,
  Bell,
  Bluetooth,
  ChevronLeft,
  Flashlight,
  Home,
  Moon,
  Radio,
  RotateCcw,
  RotateCw,
  Search,
  Settings2,
  Signal,
  SlidersHorizontal,
  Sun,
  Undo2,
  Vibrate,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react';
import { IconStyleContext } from './components/AppIcon';
import { AppGroupOverlay } from './components/AppGroupOverlay';
import { ContextMenuOverlay, ContextMenuState } from './components/ContextMenuOverlay';
import { DeveloperMenu } from './components/DeveloperMenu';
import { EditToolbar } from './components/EditToolbar';
import { FakeAppOverlay, FakeAppTarget } from './components/FakeAppOverlay';
import { PageIndicator } from './components/PageIndicator';
import { PageOverviewModal } from './components/PageOverviewModal';
import { SearchOverlay } from './components/SearchOverlay';
import { TileCatalogSheet } from './components/TileCatalogSheet';
import {
  CALENDAR_EVENTS,
  CalendarEvent,
  CATALOG_EXTRA_TILES,
  createInitialPages,
  MEDIA_TRACKS,
  ShortcutBarMode,
  syncShortcutBars,
} from './data/mockData';
import {
  applyTemplate,
  computeLayout,
  createNodeId,
  determineDropZone,
  moveLeafToSplit,
  nudgeLeafSize,
  removeLeaf,
  splitLeaf,
  toggleSplitDirection,
  updateLeafTile,
  updateMultipleSplitRatios,
  updateSplitRatio,
} from './layout/tree';
import {
  ComputedLeaf,
  DropZone,
  LauncherPage,
  Rect,
  TemplateKind,
  TileData,
} from './layout/types';
import { HapticEvent, subscribeHaptics, triggerHaptic } from './motion/haptics';
import {
  DEFAULT_MOTION_SETTINGS,
  getSpringTransition,
  MotionSettings,
} from './motion/springs';
import { ActiveTileDrag, DropPreviewResult, TilingPage } from './pages/TilingPage';
import { Wallpaper } from './theme/Wallpaper';
import { ColorMode, getDockCapsuleTokens, getThemeTokens, ThemeVariantId, WALLPAPER_PRESETS, WallpaperPresetId } from './theme/themes';
import { TileRenderer } from './tiles/TileRenderer';
import { SystemControlState, SystemToggleKey } from './tiles/tileProps';

const STORAGE_KEY = 'kachelos_launcher_state_v5';

interface PersistedState {
  pages: LauncherPage[];
  homePageId: string;
  themeId: ThemeVariantId;
  colorMode: ColorMode;
  wallpaperId?: WallpaperPresetId;
  tonalIcons?: boolean;
  shortcutBarMode?: ShortcutBarMode;
  motionSettings: MotionSettings;
}

interface LauncherNotification {
  id: string;
  appId: string;
  appName: string;
  title: string;
  body: string;
  time: string;
  hue: number;
}

const INITIAL_NOTIFICATIONS: LauncherNotification[] = [
  {
    id: 'notif-msg',
    appId: 'app-nachrichten',
    appName: 'Nachrichten',
    title: 'Mara Weber',
    body: 'Perfekt! Bis 12:30 beim Design Kritik Termin.',
    time: 'vor 3 Min.',
    hue: 217,
  },
  {
    id: 'notif-mail',
    appId: 'app-mail',
    appName: 'Postfach',
    title: 'Release-Freigabe #408',
    body: 'Build erfolgreich auf Testgerät verteilt.',
    time: 'vor 21 Min.',
    hue: 0,
  },
  {
    id: 'notif-cal',
    appId: 'app-kalender',
    appName: 'Kalender',
    title: 'Game Engines in 59 Min.',
    body: '10:00 – 11:30 · Raum B2',
    time: 'Jetzt',
    hue: 262,
  },
];

function loadSavedState(): PersistedState | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as PersistedState;
    if (!Array.isArray(parsed.pages) || parsed.pages.length === 0) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function App() {
  const saved = useMemo(() => loadSavedState(), []);

  const [shortcutBarMode, setShortcutBarMode] = useState<ShortcutBarMode>(
    () => saved?.shortcutBarMode ?? 'all'
  );
  const [pages, setPages] = useState<LauncherPage[]>(
    () => saved?.pages ?? createInitialPages(saved?.shortcutBarMode ?? 'all')
  );
  const [homePageId, setHomePageId] = useState<string>(() => saved?.homePageId ?? 'page-start');
  const [themeId, setThemeId] = useState<ThemeVariantId>(() => saved?.themeId ?? 'material-you');
  const [colorMode, setColorMode] = useState<ColorMode>(() => saved?.colorMode ?? 'light');
  const [wallpaperId, setWallpaperId] = useState<WallpaperPresetId>(() => saved?.wallpaperId ?? 'sand');
  const [tonalIcons, setTonalIcons] = useState<boolean>(() => saved?.tonalIcons ?? true);
  const [motionSettings, setMotionSettings] = useState<MotionSettings>(
    () => saved?.motionSettings ?? DEFAULT_MOTION_SETTINGS
  );
  const [calendarEvents, setCalendarEvents] = useState<CalendarEvent[]>(() => CALENDAR_EVENTS);
  const [notifications, setNotifications] = useState<LauncherNotification[]>(() => INITIAL_NOTIFICATIONS);
  const [clearedBadgeAppIds, setClearedBadgeAppIds] = useState<string[]>([]);

  // Start on designated Home page ("Start")
  const initialHomeIdx = Math.max(
    0,
    (saved?.pages ?? createInitialPages()).findIndex(
      (p) => p.id === (saved?.homePageId ?? 'page-start')
    )
  );
  const [activePageIndex, setActivePageIndex] = useState<number>(initialHomeIdx);

  // Viewport sizing: < 500px runs fullscreen on phone; >= 500px renders centered 390x844 phone frame
  const [viewport, setViewport] = useState(() => ({
    w: typeof window !== 'undefined' ? window.innerWidth : 1280,
    h: typeof window !== 'undefined' ? window.innerHeight : 860,
  }));

  useEffect(() => {
    const onResize = () => setViewport({ w: window.innerWidth, h: window.innerHeight });
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  const isMobileFullscreen = viewport.w < 500;
  const screenWidth = isMobileFullscreen ? viewport.w : 390;
  const screenHeight = isMobileFullscreen ? viewport.h : 844;

  const statusBarH = 34;
  const bottomAreaH = 62;
  const tilingAreaHeight = Math.max(320, screenHeight - statusBarH - bottomAreaH);

  const theme = useMemo(
    () => getThemeTokens(themeId, colorMode, wallpaperId),
    [themeId, colorMode, wallpaperId]
  );

  const pageBounds: Rect = useMemo(
    () => ({
      x: theme.edgePadding,
      y: 4,
      width: screenWidth - theme.edgePadding * 2,
      height: tilingAreaHeight - 8,
    }),
    [screenWidth, tilingAreaHeight, theme.edgePadding]
  );

  // Persist changes to localStorage
  useEffect(() => {
    try {
      const payload: PersistedState = {
        pages,
        homePageId,
        themeId,
        colorMode,
        wallpaperId,
        tonalIcons,
        shortcutBarMode,
        motionSettings,
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    } catch {
      // Ignore quota errors
    }
  }, [pages, homePageId, themeId, colorMode, wallpaperId, tonalIcons, shortcutBarMode, motionSettings]);

  // Mode & Overlays
  const [isEditMode, setIsEditMode] = useState(false);
  const [showDevMenu, setShowDevMenu] = useState(false);
  const [showSearch, setShowSearch] = useState(false);
  const [showCatalog, setShowCatalog] = useState(false);
  const [showPageOverview, setShowPageOverview] = useState(false);
  const [contextMenu, setContextMenu] = useState<ContextMenuState | null>(null);
  const [openGroup, setOpenGroup] = useState<{
    pageId: string;
    leafId: string;
    tile: TileData;
    initialMode?: 'view' | 'rename' | 'manage';
    sourceRect?: { x: number; y: number; width: number; height: number };
  } | null>(null);
  const [fakeApp, setFakeApp] = useState<FakeAppTarget | null>(null);

  // 5-second Undo chip state after removing a tile
  const [undoSnapshot, setUndoSnapshot] = useState<{
    pages: LauncherPage[];
    label: string;
  } | null>(null);
  const undoTimerRef = useRef<number | null>(null);

  const pushUndo = useCallback((prevPages: LauncherPage[], title: string) => {
    setUndoSnapshot({ pages: prevPages, label: title });
    if (undoTimerRef.current !== null) {
      window.clearTimeout(undoTimerRef.current);
    }
    undoTimerRef.current = window.setTimeout(() => {
      setUndoSnapshot(null);
      undoTimerRef.current = null;
    }, 5000);
  }, []);

  // Media player state shared across Start & Medien pages + Podcast widget
  const [trackIndex, setTrackIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(true);
  const [mediaProgress, setMediaProgress] = useState(0.36);

  useEffect(() => {
    if (!isPlaying) return;
    const id = window.setInterval(() => {
      setMediaProgress((p) => (p >= 0.995 ? 0 : p + 0.004));
    }, 800);
    return () => window.clearInterval(id);
  }, [isPlaying]);

  const mediaState = useMemo(
    () => ({
      trackIndex,
      isPlaying,
      progress: mediaProgress,
      onTogglePlay: () => setIsPlaying((v) => !v),
      onPrev: () => {
        setTrackIndex((i) => (i - 1 + MEDIA_TRACKS.length) % MEDIA_TRACKS.length);
        setMediaProgress(0.08);
      },
      onNext: () => {
        setTrackIndex((i) => (i + 1) % MEDIA_TRACKS.length);
        setMediaProgress(0.08);
      },
      onSeek: (ratio: number) => setMediaProgress(ratio),
    }),
    [trackIndex, isPlaying, mediaProgress]
  );

  const [showQuickShade, setShowQuickShade] = useState(false);
  const [sysToggles, setSysToggles] = useState({
    wifi: true,
    bluetooth: true,
    flashlight: false,
    dnd: false,
    hotspot: false,
    autoRotate: true,
    brightness: 0.9,
  });

  const systemState: SystemControlState = useMemo(
    () => ({
      ...sysToggles,
      onToggle: (key: SystemToggleKey) =>
        setSysToggles((prev) => ({ ...prev, [key]: !prev[key] })),
      onChangeBrightness: (val: number) =>
        setSysToggles((prev) => ({ ...prev, brightness: val })),
    }),
    [sysToggles]
  );

  // Visual Haptic Feedback monitor (HUD + localized pulse inside screen)
  const screenRef = useRef<HTMLDivElement>(null);
  const pageViewportRef = useRef<HTMLDivElement>(null);
  const [latestHaptic, setLatestHaptic] = useState<HapticEvent | null>(null);
  const [hapticRipples, setHapticRipples] = useState<
    { id: number; x: number; y: number }[]
  >([]);

  useEffect(() => {
    return subscribeHaptics((evt) => {
      setLatestHaptic(evt);
      if (evt.x !== undefined && evt.y !== undefined && screenRef.current) {
        const rect = screenRef.current.getBoundingClientRect();
        const rx = evt.x - rect.left;
        const ry = evt.y - rect.top;
        if (rx >= 0 && rx <= rect.width && ry >= 0 && ry <= rect.height) {
          setHapticRipples((prev) => [...prev.slice(-4), { id: evt.id, x: rx, y: ry }]);
          window.setTimeout(() => {
            setHapticRipples((prev) => prev.filter((r) => r.id !== evt.id));
          }, 420);
        }
      }
    });
  }, []);

  // Page offset MotionValue (no React state updates per frame during swipe!)
  const pageOffsetX = useMotionValue(-initialHomeIdx * screenWidth);
  const parallaxX = useTransform(pageOffsetX, (x) => x * 0.15);
  const swipeAnimRef = useRef<{ stop: () => void } | null>(null);

  const snapToPage = useCallback(
    (targetIdx: number, velocityPxPerSec = 0) => {
      const clamped = Math.max(0, Math.min(pages.length - 1, targetIdx));
      setActivePageIndex(clamped);
      swipeAnimRef.current?.stop();
      const spring = getSpringTransition('pageSwipe', motionSettings);
      const anim = animate(pageOffsetX, -clamped * screenWidth, {
        ...spring,
        velocity: velocityPxPerSec,
      });
      swipeAnimRef.current = anim;
    },
    [motionSettings, pageOffsetX, pages.length, screenWidth]
  );

  // Re-sync pageOffsetX if screenWidth or pages.length changes cleanly
  useEffect(() => {
    const clamped = Math.max(0, Math.min(pages.length - 1, activePageIndex));
    if (clamped !== activePageIndex) setActivePageIndex(clamped);
    pageOffsetX.set(-clamped * screenWidth);
  }, [screenWidth, pages.length]); // eslint-disable-line react-hooks/exhaustive-deps

  // Active Tile Dragging (for both Edit Mode tile moves and Catalog drops)
  const [activeDrag, setActiveDrag] = useState<ActiveTileDrag | null>(null);
  const activeDragRef = useRef<ActiveTileDrag | null>(null);
  activeDragRef.current = activeDrag;

  const edgeFlipTimerRef = useRef<number | null>(null);
  const edgeFlipSideRef = useRef<'left' | 'right' | null>(null);
  const dragVelRef = useRef<{ lastY: number; lastT: number; vy: number }>({
    lastY: 0,
    lastT: 0,
    vy: 0,
  });

  // Compute current drop target against active page
  const dropPreview: DropPreviewResult | null = useMemo(() => {
    if (!activeDrag || !pageViewportRef.current) return null;
    const activePage = pages[activePageIndex];
    if (!activePage || activePage.kind === 'all-apps') return null;

    const containerRect = pageViewportRef.current.getBoundingClientRect();
    const relX = activeDrag.clientX - containerRect.left;
    const relY = activeDrag.clientY - containerRect.top;
    const gap = isEditMode ? theme.gapEdit : theme.gapUse;
    const layout = computeLayout(activePage.root, pageBounds, gap);

    if (
      activeDrag.sourceKind === 'page-tile' &&
      relY > pageBounds.y + pageBounds.height - 6 &&
      layout.leaves.length > 1
    ) {
      return { kind: 'remove', pageId: activePage.id };
    }

    for (const leaf of layout.leaves) {
      if (
        activeDrag.sourceKind === 'page-tile' &&
        leaf.id === activeDrag.sourceLeafId &&
        activePage.id === activeDrag.sourcePageId
      ) {
        continue;
      }
      if (
        relX >= leaf.rect.x &&
        relX <= leaf.rect.x + leaf.rect.width &&
        relY >= leaf.rect.y &&
        relY <= leaf.rect.y + leaf.rect.height
      ) {
        const zone = determineDropZone(
          relX - leaf.rect.x,
          relY - leaf.rect.y,
          leaf.rect.width,
          leaf.rect.height
        );
        return { kind: 'leaf', pageId: activePage.id, leaf, zone };
      }
    }

    if (activeDrag.sourceKind === 'catalog-tile' && layout.leaves.length > 0) {
      return {
        kind: 'leaf',
        pageId: activePage.id,
        leaf: layout.leaves[layout.leaves.length - 1],
        zone: 'bottom',
      };
    }

    return null;
  }, [activeDrag, activePageIndex, isEditMode, pageBounds, pages, theme.gapEdit, theme.gapUse]);

  const latestDropPreviewRef = useRef<DropPreviewResult | null>(null);
  latestDropPreviewRef.current = dropPreview;
  const activePageIdxRef = useRef(activePageIndex);
  activePageIdxRef.current = activePageIndex;
  const pagesRef = useRef(pages);
  pagesRef.current = pages;

  // Global window pointer move/up while dragging a tile
  useEffect(() => {
    if (!activeDrag) return;

    dragVelRef.current = { lastY: activeDrag.clientY, lastT: performance.now(), vy: 0 };

    const clearEdgeFlip = () => {
      if (edgeFlipTimerRef.current !== null) {
        window.clearTimeout(edgeFlipTimerRef.current);
        edgeFlipTimerRef.current = null;
      }
      edgeFlipSideRef.current = null;
    };

    const onMove = (ev: PointerEvent) => {
      const now = performance.now();
      const dt = Math.max(1, now - dragVelRef.current.lastT) / 1000;
      const instVy = (ev.clientY - dragVelRef.current.lastY) / dt;
      dragVelRef.current = {
        lastY: ev.clientY,
        lastT: now,
        vy: dragVelRef.current.vy * 0.4 + instVy * 0.6,
      };

      setActiveDrag((prev) =>
        prev ? { ...prev, clientX: ev.clientX, clientY: ev.clientY } : null
      );

      // Edge-hold page flipping (450ms)
      if (screenRef.current) {
        const sr = screenRef.current.getBoundingClientRect();
        const relX = ev.clientX - sr.left;
        const maxRegularIdx = pagesRef.current.filter((p) => p.kind !== 'all-apps').length - 1;

        if (relX < 28 && activePageIdxRef.current > 0) {
          if (edgeFlipSideRef.current !== 'left') {
            clearEdgeFlip();
            edgeFlipSideRef.current = 'left';
            edgeFlipTimerRef.current = window.setTimeout(() => {
              triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
              snapToPage(activePageIdxRef.current - 1);
              edgeFlipSideRef.current = null;
            }, 450);
          }
        } else if (relX > sr.width - 28 && activePageIdxRef.current < maxRegularIdx) {
          if (edgeFlipSideRef.current !== 'right') {
            clearEdgeFlip();
            edgeFlipSideRef.current = 'right';
            edgeFlipTimerRef.current = window.setTimeout(() => {
              triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
              snapToPage(activePageIdxRef.current + 1);
              edgeFlipSideRef.current = null;
            }, 450);
          }
        } else {
          clearEdgeFlip();
        }
      }
    };

    const onUp = (ev: PointerEvent) => {
      clearEdgeFlip();
      const drag = activeDragRef.current;
      const drop = latestDropPreviewRef.current;
      setActiveDrag(null);

      if (!drag) return;

      // Check downward throw velocity removal
      let isThrowRemove = drop?.kind === 'remove';
      if (!isThrowRemove && drag.sourceKind === 'page-tile' && pageViewportRef.current) {
        const vr = pageViewportRef.current.getBoundingClientRect();
        const relY = ev.clientY - vr.top;
        if (dragVelRef.current.vy > 680 && relY > vr.height * 0.65) {
          isThrowRemove = true;
        }
      }

      if (isThrowRemove && drag.sourceKind === 'page-tile' && drag.sourcePageId && drag.sourceLeafId) {
        const srcPage = pagesRef.current.find((p) => p.id === drag.sourcePageId);
        if (srcPage) {
          const res = removeLeaf(srcPage.root, drag.sourceLeafId);
          if (res.tree) {
            pushUndo(pagesRef.current, drag.tile.title);
            triggerHaptic('remove', {
              enabled: motionSettings.hapticsEnabled,
              x: ev.clientX,
              y: ev.clientY,
            });
            setPages((prev) =>
              prev.map((p) => (p.id === drag.sourcePageId ? { ...p, root: res.tree! } : p))
            );
          }
        }
        return;
      }

      if (drop?.kind === 'leaf' && drop.leaf && drop.zone) {
        triggerHaptic('snap', {
          enabled: motionSettings.hapticsEnabled,
          x: ev.clientX,
          y: ev.clientY,
        });
        const targetLeafId = drop.leaf.id;
        const zone = drop.zone;

        if (drag.sourceKind === 'catalog-tile') {
          const safeZone: Exclude<DropZone, 'center'> = zone === 'center' ? 'bottom' : zone;
          setPages((prev) =>
            prev.map((p) =>
              p.id === drop.pageId
                ? { ...p, root: splitLeaf(p.root, targetLeafId, drag.tile, safeZone, 0.5) }
                : p
            )
          );
        } else if (drag.sourcePageId === drop.pageId && drag.sourceLeafId) {
          setPages((prev) =>
            prev.map((p) =>
              p.id === drop.pageId
                ? {
                    ...p,
                    root: moveLeafToSplit(p.root, drag.sourceLeafId!, targetLeafId, zone, 0.5),
                  }
                : p
            )
          );
        } else if (drag.sourcePageId && drag.sourceLeafId) {
          // Cross-page move!
          setPages((prev) => {
            const srcPage = prev.find((p) => p.id === drag.sourcePageId);
            if (!srcPage) return prev;
            const removed = removeLeaf(srcPage.root, drag.sourceLeafId!);
            if (!removed.tree) return prev; // don't leave a page completely empty
            const safeZone: Exclude<DropZone, 'center'> = zone === 'center' ? 'bottom' : zone;
            return prev.map((p) => {
              if (p.id === drag.sourcePageId) return { ...p, root: removed.tree! };
              if (p.id === drop.pageId) {
                return {
                  ...p,
                  root: splitLeaf(
                    p.root,
                    targetLeafId,
                    drag.tile,
                    safeZone,
                    0.5,
                    drag.sourceLeafId
                  ),
                };
              }
              return p;
            });
          });
        }
      }
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
    window.addEventListener('pointercancel', onUp);
    return () => {
      clearEdgeFlip();
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      window.removeEventListener('pointercancel', onUp);
    };
  }, [activeDrag !== null, motionSettings.hapticsEnabled, pushUndo, snapToPage]); // eslint-disable-line react-hooks/exhaustive-deps

  // Main Horizontal Swipe & Swipe-Up Search Gesture on Launcher Viewport
  const gestureLockRef = useRef<'none' | 'x' | 'y'>('none');
  const bindViewportDrag = useDrag(
    ({ first, last, movement: [mx, my], velocity: [vx], direction: [dx], event, memo }) => {
      if (activeDragRef.current) return memo;

      if (first) {
        gestureLockRef.current = 'none';
        swipeAnimRef.current?.stop();
        return {
          startX: pageOffsetX.get(),
          inScrollable: Boolean((event.target as HTMLElement | null)?.closest('[data-scrollable="true"]')),
        };
      }

      const state = (memo as { startX: number; inScrollable: boolean }) ?? {
        startX: pageOffsetX.get(),
        inScrollable: false,
      };

      if (gestureLockRef.current === 'none') {
        if (Math.abs(mx) > 9 || Math.abs(my) > 9) {
          gestureLockRef.current = Math.abs(mx) >= Math.abs(my) ? 'x' : 'y';
        }
      }

      // Swipe up -> Search Overlay (Use Mode, not inside scrollable inner box)
      if (gestureLockRef.current === 'y') {
        if (
          last &&
          my < -46 &&
          !isEditMode &&
          !state.inScrollable &&
          pages[activePageIndex]?.kind !== 'all-apps'
        ) {
          triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
          setShowSearch(true);
        }
        return state;
      }

      if (gestureLockRef.current === 'x') {
        const minOffset = -(pages.length - 1) * screenWidth;
        const maxOffset = 0;
        let nextX = state.startX + mx;

        // 30% rubberband resistance at ends
        if (nextX > maxOffset) {
          nextX = maxOffset + (nextX - maxOffset) * 0.3;
        } else if (nextX < minOffset) {
          nextX = minOffset + (nextX - minOffset) * 0.3;
        }

        pageOffsetX.set(nextX);

        if (last) {
          const rawIdx = -nextX / screenWidth;
          let targetIdx = Math.round(rawIdx);
          if (vx > 0.32) {
            targetIdx = dx < 0 ? Math.ceil(rawIdx) : Math.floor(rawIdx);
          }
          targetIdx = Math.max(0, Math.min(pages.length - 1, targetIdx));
          if (targetIdx !== activePageIndex) {
            triggerHaptic('pageSettle', { enabled: motionSettings.hapticsEnabled });
          } else {
            triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
          }
          snapToPage(targetIdx, vx * dx * 1000);
        }
      }

      return state;
    },
    { filterTaps: true, pointer: { touch: true } }
  );

  // Pinch gesture opens Page Overview
  const bindPinch = usePinch(
    ({ offset: [scale] }) => {
      if (scale < 0.82 && !showPageOverview) {
        triggerHaptic('editEnter', { enabled: motionSettings.hapticsEnabled });
        setShowPageOverview(true);
      }
    },
    { pointer: { touch: true } }
  );

  // Helper to convert DOMRect into coordinates relative to phone screen
  const toScreenRect = useCallback((domRect?: DOMRect) => {
    if (!domRect || !screenRef.current) return undefined;
    const sr = screenRef.current.getBoundingClientRect();
    return {
      x: domRect.left - sr.left,
      y: domRect.top - sr.top,
      width: domRect.width,
      height: domRect.height,
    };
  }, []);

  const clearAppNotificationsAndBadge = useCallback((appId: string) => {
    setClearedBadgeAppIds((prev) => (prev.includes(appId) ? prev : [...prev, appId]));
    setNotifications((prev) => prev.filter((n) => n.appId !== appId));
  }, []);

  const openAppWithBadgeSync = useCallback(
    (appId: string, domRect?: DOMRect, shortcutAction?: string) => {
      clearAppNotificationsAndBadge(appId);
      setFakeApp({
        kind: 'app',
        appId,
        sourceRect: toScreenRect(domRect),
        shortcutAction,
      });
    },
    [clearAppNotificationsAndBadge, toScreenRect]
  );

  const dismissNotification = useCallback(
    (notif: LauncherNotification) => {
      triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
      setNotifications((prev) => prev.filter((n) => n.id !== notif.id));
      setClearedBadgeAppIds((prev) =>
        prev.includes(notif.appId) ? prev : [...prev, notif.appId]
      );
    },
    [motionSettings.hapticsEnabled]
  );

  // Home button / Home gesture: closes overlays or jumps back to designated Start page
  const handleHomeAction = () => {
    triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
    if (fakeApp) {
      setFakeApp(null);
      return;
    }
    if (openGroup) {
      setOpenGroup(null);
      return;
    }
    if (showSearch) {
      setShowSearch(false);
      return;
    }
    if (contextMenu) {
      setContextMenu(null);
      return;
    }
    if (showPageOverview) {
      setShowPageOverview(false);
      return;
    }
    if (showCatalog) {
      setShowCatalog(false);
      return;
    }
    if (showQuickShade) {
      setShowQuickShade(false);
      return;
    }
    if (isEditMode) {
      setIsEditMode(false);
      return;
    }
    const homeIdx = Math.max(
      0,
      pages.findIndex((p) => p.id === homePageId)
    );
    snapToPage(homeIdx);
  };

  const handleBackAction = () => {
    triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
    if (fakeApp) setFakeApp(null);
    else if (openGroup) setOpenGroup(null);
    else if (contextMenu) setContextMenu(null);
    else if (showSearch) setShowSearch(false);
    else if (showCatalog) setShowCatalog(false);
    else if (showPageOverview) setShowPageOverview(false);
    else if (showQuickShade) setShowQuickShade(false);
    else if (isEditMode) setIsEditMode(false);
  };

  const handleChangeShortcutBarMode = useCallback((mode: ShortcutBarMode) => {
    setShortcutBarMode(mode);
    setPages((prev) => syncShortcutBars(prev, mode));
  }, []);

  const handleResetAll = () => {
    localStorage.removeItem(STORAGE_KEY);
    const fresh = createInitialPages('all');
    setPages(fresh);
    setShortcutBarMode('all');
    setHomePageId('page-start');
    setThemeId('material-you');
    setColorMode('light');
    setWallpaperId('sand');
    setTonalIcons(true);
    setMotionSettings(DEFAULT_MOTION_SETTINGS);
    setCalendarEvents(CALENDAR_EVENTS);
    setNotifications(INITIAL_NOTIFICATIONS);
    setClearedBadgeAppIds([]);
    setSysToggles({
      wifi: true,
      bluetooth: true,
      flashlight: false,
      dnd: false,
      hotspot: false,
      autoRotate: true,
      brightness: 0.9,
    });
    setShowQuickShade(false);
    setIsEditMode(false);
    setFakeApp(null);
    setOpenGroup(null);
    setShowSearch(false);
    setShowCatalog(false);
    setShowPageOverview(false);
    setContextMenu(null);
    setUndoSnapshot(null);
    const startIdx = fresh.findIndex((p) => p.id === 'page-start');
    setActivePageIndex(startIdx);
    animate(pageOffsetX, -startIdx * screenWidth, getSpringTransition('pageSwipe', DEFAULT_MOTION_SETTINGS));
  };

  const handleInsertCatalogTile = (tile: TileData) => {
    const activePage = pages[activePageIndex];
    if (!activePage || activePage.kind === 'all-apps') return;
    const gap = isEditMode ? theme.gapEdit : theme.gapUse;
    const layout = computeLayout(activePage.root, pageBounds, gap);
    // Split the largest existing leaf so the new tile fits comfortably
    const largest = [...layout.leaves].sort(
      (a, b) => b.rect.width * b.rect.height - a.rect.width * a.rect.height
    )[0];
    if (!largest) return;
    const dirZone: Exclude<DropZone, 'center'> =
      largest.rect.width > largest.rect.height ? 'right' : 'bottom';
    setPages((prev) =>
      prev.map((p) =>
        p.id === activePage.id
          ? { ...p, root: splitLeaf(p.root, largest.id, tile, dirZone, 0.5) }
          : p
      )
    );
    setShowCatalog(false);
  };

  const handleRemoveLeaf = (pageId: string, leafId: string) => {
    const pg = pages.find((p) => p.id === pageId);
    if (!pg) return;
    const res = removeLeaf(pg.root, leafId);
    if (!res.tree || !res.removed) return;
    pushUndo(pages, res.removed.tile.title);
    setPages((prev) => prev.map((p) => (p.id === pageId ? { ...p, root: res.tree! } : p)));
    setContextMenu(null);
  };

  const handleApplyTemplate = (tpl: TemplateKind) => {
    const activePage = pages[activePageIndex];
    if (!activePage || activePage.kind === 'all-apps') return;
    setPages((prev) =>
      prev.map((p) =>
        p.id === activePage.id
          ? { ...p, root: applyTemplate(p.root, tpl, CATALOG_EXTRA_TILES) }
          : p
      )
    );
  };

  const handleAddPage = () => {
    const newPage: LauncherPage = {
      id: createNodeId('page'),
      title: `Seite ${pages.length}`,
      kind: 'tiles',
      root: {
        kind: 'split',
        id: createNodeId('split'),
        direction: 'vertical',
        ratio: 0.5,
        first: {
          kind: 'leaf',
          id: createNodeId('leaf'),
          tile: { ...CATALOG_EXTRA_TILES[4], id: createNodeId('tile') }, // Notiz
        },
        second: {
          kind: 'split',
          id: createNodeId('split'),
          direction: 'horizontal',
          ratio: 0.5,
          first: {
            kind: 'leaf',
            id: createNodeId('leaf'),
            tile: { ...CATALOG_EXTRA_TILES[5], id: createNodeId('tile') }, // Fotos
          },
          second: {
            kind: 'leaf',
            id: createNodeId('leaf'),
            tile: { ...CATALOG_EXTRA_TILES[6], id: createNodeId('tile') }, // Akku
          },
        },
      },
    };
    const allAppsIdx = pages.findIndex((p) => p.kind === 'all-apps');
    const insertIdx = allAppsIdx >= 0 ? allAppsIdx : pages.length;
    const next = [...pages];
    next.splice(insertIdx, 0, newPage);
    setPages(next);
    setShowPageOverview(false);
    snapToPage(insertIdx);
  };

  const hasAnyOverlay = Boolean(
    fakeApp || openGroup || showSearch || contextMenu || showCatalog || showPageOverview || showQuickShade
  );

  const phoneScreenContent = (
    <div
      ref={screenRef}
      onScroll={(e) => {
        e.currentTarget.scrollLeft = 0;
        e.currentTarget.scrollTop = 0;
      }}
      style={{
        position: 'relative',
        width: screenWidth,
        height: screenHeight,
        borderRadius: isMobileFullscreen ? 0 : 40,
        overflow: 'clip',
        background: '#0F1117',
        touchAction: 'none',
        display: 'flex',
        flexDirection: 'column',
        filter: `brightness(${(0.68 + systemState.brightness * 0.32).toFixed(2)})`,
      }}
    >
      {/* Dynamic Procedural Wallpaper with 15% horizontal parallax */}
      <Wallpaper themeId={themeId} mode={colorMode} wallpaperId={wallpaperId} parallaxX={parallaxX} />

      {/* Android Status Bar (Tap to open Quick-Settings Pull-Down Shade) */}
      <div
        onClick={() => {
          triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
          setShowQuickShade((v) => !v);
        }}
        role="button"
        aria-label="Schnelleinstellungen öffnen"
        title="Tippen für Schnelleinstellungen"
        style={{
          height: statusBarH,
          padding: '8px 18px 0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          position: 'relative',
          zIndex: 45,
          color: theme.statusText,
          fontSize: 12,
          fontWeight: 700,
          flexShrink: 0,
          cursor: 'pointer',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span className="display-num">09:01</span>
          {notifications.length > 0 && <Bell size={11} style={{ opacity: 0.85 }} />}
          {systemState.dnd && <Moon size={12} />}
        </div>
        {!isMobileFullscreen && (
          <div
            style={{
              width: 14,
              height: 14,
              borderRadius: 99,
              background: systemState.flashlight ? '#FEF08A' : '#090A0D',
              border: '1.5px solid rgba(255,255,255,0.16)',
              boxShadow: systemState.flashlight
                ? '0 0 24px 8px rgba(253, 224, 71, 0.85)'
                : undefined,
              transition: 'background 200ms, box-shadow 200ms',
            }}
          />
        )}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          {systemState.bluetooth && <Bluetooth size={12} />}
          <Signal size={13} />
          {systemState.wifi ? <Wifi size={13} /> : <WifiOff size={13} opacity={0.55} />}
          <Battery size={15} />
          {isMobileFullscreen && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                setShowDevMenu(true);
              }}
              style={{
                marginLeft: 4,
                width: 26,
                height: 26,
                borderRadius: 99,
                border: 'none',
                background: 'rgba(125,125,145,0.25)',
                color: theme.statusText,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
              aria-label="Entwicklermenü"
            >
              <Settings2 size={14} />
            </button>
          )}
        </div>
      </div>

      {/* Horizontal Tiling Pages Strip */}
      <div
        ref={pageViewportRef}
        {...bindViewportDrag()}
        {...bindPinch()}
        onScroll={(e) => {
          e.currentTarget.scrollLeft = 0;
          e.currentTarget.scrollTop = 0;
        }}
        style={{
          position: 'relative',
          flex: 1,
          overflow: 'clip',
          touchAction: 'none',
          zIndex: 10,
        }}
      >
        <motion.div
          style={{
            display: 'flex',
            height: '100%',
            width: pages.length * screenWidth,
            x: pageOffsetX,
            willChange: 'transform',
          }}
        >
          {pages.map((page, idx) => (
            <div
              key={page.id}
              style={{
                width: screenWidth,
                height: '100%',
                flexShrink: 0,
                position: 'relative',
              }}
            >
              <TilingPage
                page={page}
                bounds={pageBounds}
                theme={theme}
                motionSettings={motionSettings}
                isEditMode={isEditMode}
                isActivePage={idx === activePageIndex}
                activeDrag={activeDrag}
                dropPreview={dropPreview}
                onEnterEditMode={() => setIsEditMode(true)}
                onUpdateDividerRatio={(pageId, splitId, ratio) =>
                  setPages((prev) =>
                    prev.map((p) =>
                      p.id === pageId ? { ...p, root: updateSplitRatio(p.root, splitId, ratio) } : p
                    )
                  )
                }
                onUpdateMultipleDividerRatios={(pageId, updates) =>
                  setPages((prev) =>
                    prev.map((p) =>
                      p.id === pageId
                        ? { ...p, root: updateMultipleSplitRatios(p.root, updates) }
                        : p
                    )
                  )
                }
                onToggleDividerDirection={(pageId, splitId) =>
                  setPages((prev) =>
                    prev.map((p) =>
                      p.id === pageId ? { ...p, root: toggleSplitDirection(p.root, splitId) } : p
                    )
                  )
                }
                onUpdateTile={(pageId, leafId, updater) =>
                  setPages((prev) =>
                    prev.map((p) =>
                      p.id === pageId ? { ...p, root: updateLeafTile(p.root, leafId, updater) } : p
                    )
                  )
                }
                onOpenApp={openAppWithBadgeSync}
                onOpenGroup={(tile, leafId, domRect) =>
                  setOpenGroup({
                    pageId: page.id,
                    leafId,
                    tile,
                    initialMode: 'view',
                    sourceRect: toScreenRect(domRect),
                  })
                }
                onOpenSearch={() => setShowSearch(true)}
                onOpenFakeTileApp={(tile, domRect) =>
                  setFakeApp({ kind: 'tile', tile, sourceRect: toScreenRect(domRect) })
                }
                onOpenContextMenu={(leaf: ComputedLeaf, pageId: string, domRect: DOMRect) => {
                  const r = toScreenRect(domRect) ?? { x: 40, y: 120, width: 140, height: 120 };
                  setContextMenu({ leafId: leaf.id, pageId, tile: leaf.tile, rect: r });
                }}
                onCloseContextMenu={() => setContextMenu(null)}
                onStartTileDrag={(drag) => setActiveDrag(drag)}
                calendarEvents={calendarEvents}
                mediaState={mediaState}
                systemState={systemState}
              />
            </div>
          ))}
        </motion.div>
      </div>

      {/* Bottom Indicator + Home Gesture Navigation Bar */}
      <div
        style={{
          height: bottomAreaH,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '2px 12px 8px',
          position: 'relative',
          zIndex: 35,
          flexShrink: 0,
        }}
      >
        {(() => {
          const { dockBg, activePillBg, activeTextColor } = getDockCapsuleTokens(theme);
          const shortcutCapsuleStyle = (active: boolean): React.CSSProperties => ({
            width: 38,
            height: 28,
            borderRadius: 999,
            background: active ? activePillBg : dockBg,
            border: `1px solid ${theme.sheetBorder}`,
            color: active ? activeTextColor : theme.statusText,
            backdropFilter: 'blur(14px)',
            WebkitBackdropFilter: 'blur(14px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            flexShrink: 0,
            transition: 'background 150ms ease, color 150ms ease',
          });
          return (
            <div
              style={{
                width: '100%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 6,
              }}
            >
              <button
                type="button"
                onPointerDown={(e) => e.stopPropagation()}
                onClick={(e) => {
                  triggerHaptic('press', {
                    enabled: motionSettings.hapticsEnabled,
                    x: e.clientX,
                    y: e.clientY,
                  });
                  setShowQuickShade(false);
                  setShowSearch(true);
                }}
                aria-label="Suche öffnen"
                title="Schnellsuche & Web-Suche öffnen"
                style={shortcutCapsuleStyle(showSearch)}
              >
                <Search size={14} strokeWidth={2.3} />
              </button>

              <PageIndicator
                pages={pages}
                activePageIndex={activePageIndex}
                pageWidth={screenWidth}
                pageOffsetX={pageOffsetX}
                theme={theme}
                motionSettings={motionSettings}
                onSelectPage={(idx) => snapToPage(idx)}
                onOpenOverview={() => setShowPageOverview(true)}
              />

              <button
                type="button"
                onPointerDown={(e) => e.stopPropagation()}
                onClick={(e) => {
                  triggerHaptic('press', {
                    enabled: motionSettings.hapticsEnabled,
                    x: e.clientX,
                    y: e.clientY,
                  });
                  setShowSearch(false);
                  setShowQuickShade((v) => !v);
                }}
                aria-label="Schnelleinstellungen öffnen"
                title="Schnelleinstellungen (WLAN, Bluetooth, Taschenlampe)"
                style={shortcutCapsuleStyle(showQuickShade)}
              >
                <SlidersHorizontal size={14} strokeWidth={2.3} />
              </button>
            </div>
          );
        })()}

        {/* Android Gesture / Home Pill Row */}
        <div
          style={{
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative',
            height: 22,
          }}
        >
          {hasAnyOverlay && (
            <button
              type="button"
              onClick={handleBackAction}
              style={{
                position: 'absolute',
                left: 6,
                display: 'flex',
                alignItems: 'center',
                gap: 2,
                padding: '2px 8px',
                borderRadius: 99,
                border: 'none',
                background: 'rgba(125,125,145,0.22)',
                color: theme.statusText,
                fontSize: 11,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              <ChevronLeft size={13} />
              <span>Zurück</span>
            </button>
          )}

          <button
            type="button"
            onClick={handleHomeAction}
            aria-label="Home-Taste (Startseite)"
            title="Home-Geste: Zur Startseite"
            style={{
              width: 118,
              height: 18,
              background: 'transparent',
              border: 'none',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              padding: 0,
            }}
          >
            <span
              style={{
                width: 110,
                height: 4.5,
                borderRadius: 99,
                background: theme.statusText,
                opacity: 0.72,
              }}
            />
          </button>

          <button
            type="button"
            onClick={handleHomeAction}
            aria-label="Startseite"
            title="Zur Startseite springen"
            style={{
              position: 'absolute',
              right: 6,
              width: 26,
              height: 24,
              borderRadius: 99,
              border: 'none',
              background: 'rgba(125,125,145,0.18)',
              color: theme.statusText,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <Home size={13} />
          </button>
        </div>
      </div>

      {/* Edit Mode Bottom Toolbar */}
      <AnimatePresence>
        {isEditMode && !showCatalog && !showPageOverview && (
          <EditToolbar
            theme={theme}
            motionSettings={motionSettings}
            isAllAppsPage={pages[activePageIndex]?.kind === 'all-apps'}
            onOpenCatalog={() => setShowCatalog(true)}
            onApplyTemplate={handleApplyTemplate}
            onOpenPages={() => setShowPageOverview(true)}
            onDone={() => setIsEditMode(false)}
          />
        )}
      </AnimatePresence>

      {/* 5-second Undo Chip */}
      <AnimatePresence>
        {undoSnapshot && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 16 }}
            style={{
              position: 'absolute',
              left: '50%',
              bottom: isEditMode ? 84 : 68,
              transform: 'translateX(-50%)',
              zIndex: 68,
            }}
          >
            <button
              type="button"
              onClick={() => {
                triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                setPages(undoSnapshot.pages);
                setUndoSnapshot(null);
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '9px 14px',
                borderRadius: 999,
                border: '1px solid rgba(255,255,255,0.2)',
                background: '#0F172A',
                color: '#FFFFFF',
                fontSize: 12.5,
                fontWeight: 700,
                boxShadow: '0 12px 28px rgba(0,0,0,0.4)',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              <Undo2 size={14} color="#38BDF8" />
              <span>„{undoSnapshot.label}“ entfernt · Rückgängig</span>
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Floating Tile Drag Ghost */}
      {activeDrag && screenRef.current && (
        <div
          style={{
            position: 'absolute',
            left: activeDrag.clientX - screenRef.current.getBoundingClientRect().left - activeDrag.width * 0.45,
            top: activeDrag.clientY - screenRef.current.getBoundingClientRect().top - activeDrag.height * 0.45,
            width: activeDrag.width * 0.9,
            height: activeDrag.height * 0.9,
            transform: 'scale(1.04)',
            pointerEvents: 'none',
            zIndex: 95,
            opacity: 0.94,
            filter: 'drop-shadow(0 18px 34px rgba(0,0,0,0.45))',
          }}
        >
          <TileRenderer
            tile={activeDrag.tile}
            leafId="drag-ghost"
            width={activeDrag.width * 0.9}
            height={activeDrag.height * 0.9}
            theme={theme}
            motionSettings={motionSettings}
            isEditMode={false}
            onOpenApp={() => {}}
            onOpenGroup={() => {}}
            onOpenSearch={() => {}}
            onOpenFakeTileApp={() => {}}
            onUpdateTile={() => {}}
            mediaState={mediaState}
            systemState={systemState}
          />
        </div>
      )}

      {/* Visual Haptic Ripples inside screen */}
      <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 110 }}>
        {hapticRipples.map((r) => (
          <motion.div
            key={r.id}
            initial={{ scale: 0.25, opacity: 0.8 }}
            animate={{ scale: 1.9, opacity: 0 }}
            transition={{ duration: 0.36, ease: 'easeOut' }}
            style={{
              position: 'absolute',
              left: r.x - 18,
              top: r.y - 18,
              width: 36,
              height: 36,
              borderRadius: 99,
              border: '2px solid #38BDF8',
              boxShadow: '0 0 12px rgba(56,189,248,0.65)',
            }}
          />
        ))}
      </div>

      {/* Quick-Settings Pull-Down Shade */}
      <AnimatePresence>
        {showQuickShade && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setShowQuickShade(false)}
            onPointerDown={(e) => e.stopPropagation()}
            style={{
              position: 'absolute',
              inset: 0,
              zIndex: 74,
              background: 'rgba(0,0,0,0.28)',
              backdropFilter: 'blur(4px)',
            }}
          >
              <div
                onClick={(e) => e.stopPropagation()}
                style={{
                  position: 'absolute',
                  top: statusBarH + 8,
                  left: 10,
                  right: 10,
                  maxHeight: screenHeight - statusBarH - bottomAreaH - 16,
                  overflowY: 'auto',
                  borderRadius: 24,
                  background: theme.sheetBg,
                  border: `1px solid ${theme.sheetBorder}`,
                  color: theme.textPrimary,
                  padding: 14,
                  boxShadow: '0 22px 48px rgba(0,0,0,0.36)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 12,
                }}
              >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 13, fontWeight: 700 }}>Schnelleinstellungen</span>
                <span style={{ fontSize: 11, color: theme.textMuted }}>Statusleiste antippen</span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
                {(
                  [
                    { key: 'wifi', label: 'WLAN', active: systemState.wifi, icon: systemState.wifi ? <Wifi size={16} /> : <WifiOff size={16} /> },
                    { key: 'bluetooth', label: 'Bluetooth', active: systemState.bluetooth, icon: <Bluetooth size={16} /> },
                    { key: 'flashlight', label: 'Taschenlampe', active: systemState.flashlight, icon: <Flashlight size={16} /> },
                    { key: 'dnd', label: 'Nicht stören', active: systemState.dnd, icon: <Moon size={16} /> },
                    { key: 'hotspot', label: 'Hotspot', active: systemState.hotspot, icon: <Radio size={16} /> },
                    { key: 'autoRotate', label: 'Drehen', active: systemState.autoRotate, icon: <RotateCw size={16} /> },
                  ] as const
                ).map((item) => (
                  <button
                    key={item.key}
                    type="button"
                    aria-pressed={item.active}
                    onClick={() => {
                      triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                      systemState.onToggle(item.key);
                    }}
                    style={{
                      minHeight: 48,
                      borderRadius: 15,
                      border: item.active ? 'none' : `1px solid ${theme.sheetBorder}`,
                      background: item.active ? '#2563EB' : 'rgba(125,125,145,0.12)',
                      color: item.active ? '#FFFFFF' : theme.textPrimary,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 3,
                      fontSize: 10.5,
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    {item.icon}
                    <span>{item.label}</span>
                  </button>
                ))}
              </div>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  padding: '6px 10px',
                  borderRadius: 14,
                  background: 'rgba(125,125,145,0.12)',
                }}
              >
                <Sun size={15} color="#38BDF8" />
                <input
                  type="range"
                  min="0.25"
                  max="1"
                  step="0.05"
                  value={systemState.brightness}
                  onChange={(e) => systemState.onChangeBrightness(Number(e.target.value))}
                  aria-label="Display-Helligkeit"
                  style={{ flex: 1, accentColor: '#38BDF8', cursor: 'pointer' }}
                />
                <span className="display-num" style={{ fontSize: 11, fontWeight: 700 }}>
                  {Math.round(systemState.brightness * 100)} %
                </span>
              </div>

              {/* Notification Center inside Shade */}
              <div style={{ height: 1, background: theme.sheetBorder }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12.5, fontWeight: 700 }}>
                  <Bell size={14} color="#38BDF8" />
                  <span>Benachrichtigungen ({notifications.length})</span>
                </div>
                {notifications.length > 0 ? (
                  <button
                    type="button"
                    onClick={() => {
                      triggerHaptic('remove', { enabled: motionSettings.hapticsEnabled });
                      setClearedBadgeAppIds((prev) => [
                        ...new Set([...prev, ...notifications.map((n) => n.appId)]),
                      ]);
                      setNotifications([]);
                    }}
                    style={{
                      padding: '4px 9px',
                      borderRadius: 99,
                      border: 'none',
                      background: 'rgba(125,125,145,0.18)',
                      color: theme.textPrimary,
                      fontSize: 11,
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    Alle löschen
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => {
                      triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                      setNotifications(INITIAL_NOTIFICATIONS);
                      setClearedBadgeAppIds([]);
                    }}
                    style={{
                      padding: '4px 9px',
                      borderRadius: 99,
                      border: 'none',
                      background: 'rgba(56,189,248,0.18)',
                      color: '#38BDF8',
                      fontSize: 11,
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    Demo-Mitteilungen
                  </button>
                )}
              </div>

              {notifications.length === 0 ? (
                <div style={{ fontSize: 12, color: theme.textMuted, padding: '4px 2px' }}>
                  Keine ungelesenen Mitteilungen.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 7 }}>
                  {notifications.map((n) => (
                    <motion.div
                      key={n.id}
                      drag="x"
                      dragConstraints={{ left: 0, right: 120 }}
                      dragElastic={0.3}
                      onDragEnd={(_, info) => {
                        if (Math.abs(info.offset.x) > 65) dismissNotification(n);
                      }}
                      style={{
                        borderRadius: 16,
                        background: 'rgba(125,125,145,0.12)',
                        padding: '9px 11px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: 8,
                        cursor: 'pointer',
                      }}
                      onClick={(e) => {
                        triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
                        setShowQuickShade(false);
                        openAppWithBadgeSync(n.appId, e.currentTarget.getBoundingClientRect());
                      }}
                    >
                      <div style={{ minWidth: 0, flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 10.5, color: theme.textMuted }}>
                          <span
                            style={{
                              width: 7,
                              height: 7,
                              borderRadius: 99,
                              background: `hsl(${n.hue} 80% 58%)`,
                              flexShrink: 0,
                            }}
                          />
                          <span style={{ fontWeight: 700 }}>{n.appName}</span>
                          <span>· {n.time}</span>
                        </div>
                        <div style={{ fontSize: 12.5, fontWeight: 700, marginTop: 2 }}>{n.title}</div>
                        <div
                          style={{
                            fontSize: 11.5,
                            color: theme.textSecondary,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                          }}
                        >
                          {n.body}
                        </div>
                      </div>
                      <button
                        type="button"
                        aria-label={`${n.appName} Benachrichtigung entfernen`}
                        onClick={(e) => {
                          e.stopPropagation();
                          dismissNotification(n);
                        }}
                        style={{
                          width: 28,
                          height: 28,
                          borderRadius: 99,
                          border: 'none',
                          background: 'rgba(125,125,145,0.16)',
                          color: theme.textSecondary,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          cursor: 'pointer',
                          flexShrink: 0,
                        }}
                      >
                        <X size={13} />
                      </button>
                    </motion.div>
                  ))}
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Overlays */}
      <AnimatePresence>
        {contextMenu && (
          <ContextMenuOverlay
            menu={contextMenu}
            containerRect={{ width: screenWidth, height: screenHeight }}
            theme={theme}
            motionSettings={motionSettings}
            onClose={() => setContextMenu(null)}
            onEnterEditMode={() => {
              setContextMenu(null);
              setIsEditMode(true);
            }}
            onQuickGrow={(pageId, leafId) => {
              setContextMenu(null);
              setPages((prev) =>
                prev.map((p) =>
                  p.id === pageId ? { ...p, root: nudgeLeafSize(p.root, leafId, 0.14) } : p
                )
              );
            }}
            onQuickSplit={(pageId, leafId, rect) => {
              setContextMenu(null);
              const zone: Exclude<DropZone, 'center'> =
                rect.width >= rect.height ? 'right' : 'bottom';
              const companion = {
                ...CATALOG_EXTRA_TILES[4], // Notiz
                id: createNodeId('tile'),
              };
              setPages((prev) =>
                prev.map((p) =>
                  p.id === pageId
                    ? { ...p, root: splitLeaf(p.root, leafId, companion, zone, 0.5) }
                    : p
                )
              );
            }}
            onRemoveTile={handleRemoveLeaf}
            onShowInfo={(tile, rect) => {
              setContextMenu(null);
              if (tile.type === 'single-app' && tile.appId) {
                setFakeApp({ kind: 'app', appId: tile.appId, sourceRect: rect, showInfoOnly: true });
              } else {
                setFakeApp({ kind: 'tile', tile, sourceRect: rect, showInfoOnly: true });
              }
            }}
            onShortcutAction={(label, tile, rect) => {
              const ctxLeafId = contextMenu.leafId;
              const ctxPageId = contextMenu.pageId;
              setContextMenu(null);
              if (label === 'Gruppe umbenennen' || label === 'Apps verwalten') {
                setOpenGroup({
                  pageId: ctxPageId,
                  leafId: ctxLeafId,
                  tile,
                  initialMode: label === 'Gruppe umbenennen' ? 'rename' : 'manage',
                  sourceRect: rect,
                });
                return;
              }
              if (label === 'Mix wechseln') {
                mediaState.onNext();
                return;
              }
              if (label === 'App-Info' && tile.type === 'single-app' && tile.appId) {
                setFakeApp({ kind: 'app', appId: tile.appId, sourceRect: rect, showInfoOnly: true });
                return;
              }
              if (tile.type === 'single-app' && tile.appId) {
                clearAppNotificationsAndBadge(tile.appId);
                setFakeApp({ kind: 'app', appId: tile.appId, sourceRect: rect, shortcutAction: label });
                return;
              }
              setFakeApp({ kind: 'tile', tile, sourceRect: rect, shortcutAction: label });
            }}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {openGroup && (() => {
          const livePage = pages.find((p) => p.id === openGroup.pageId);
          const findLiveLeafTile = (node: LauncherPage['root']): TileData | null => {
            if (node.kind === 'leaf') return node.id === openGroup.leafId ? node.tile : null;
            return findLiveLeafTile(node.first) ?? findLiveLeafTile(node.second);
          };
          const liveTile = (livePage ? findLiveLeafTile(livePage.root) : null) ?? openGroup.tile;
          return (
            <AppGroupOverlay
              tile={liveTile}
              initialMode={openGroup.initialMode}
              sourceRect={openGroup.sourceRect}
              containerRect={{ width: screenWidth, height: screenHeight }}
              theme={theme}
              motionSettings={motionSettings}
              onClose={() => setOpenGroup(null)}
              onOpenApp={openAppWithBadgeSync}
              onUpdateGroup={(updater) =>
                setPages((prev) =>
                  prev.map((p) =>
                    p.id === openGroup.pageId
                      ? { ...p, root: updateLeafTile(p.root, openGroup.leafId, updater) }
                      : p
                  )
                )
              }
            />
          );
        })()}
      </AnimatePresence>

      <AnimatePresence>
        {showSearch && (
          <SearchOverlay
            theme={theme}
            motionSettings={motionSettings}
            onClose={() => setShowSearch(false)}
            onOpenApp={openAppWithBadgeSync}
            onSearchWeb={(query, domRect) =>
              setFakeApp({
                kind: 'app',
                appId: 'app-browser',
                webQuery: query,
                sourceRect: toScreenRect(domRect),
              })
            }
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showCatalog && (
          <TileCatalogSheet
            theme={theme}
            motionSettings={motionSettings}
            onClose={() => setShowCatalog(false)}
            onSelectTile={handleInsertCatalogTile}
            onStartCatalogDrag={(tile, cx, cy) => {
              setShowCatalog(false);
              setActiveDrag({
                sourceKind: 'catalog-tile',
                tile,
                width: 165,
                height: 150,
                clientX: cx,
                clientY: cy,
              });
            }}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showPageOverview && (
          <PageOverviewModal
            pages={pages}
            activePageIndex={activePageIndex}
            homePageId={homePageId}
            theme={theme}
            motionSettings={motionSettings}
            onClose={() => setShowPageOverview(false)}
            onSelectPage={(idx) => snapToPage(idx)}
            onSetHomePage={(pid) => setHomePageId(pid)}
            onAddPage={handleAddPage}
            onReorderRegularPages={(reorderedRegular) => {
              const allApps = pages.find((p) => p.kind === 'all-apps');
              setPages(allApps ? [...reorderedRegular, allApps] : reorderedRegular);
            }}
            onDeletePage={(pid) => {
              const filtered = pages.filter((p) => p.id !== pid);
              setPages(filtered);
              if (homePageId === pid && filtered.length > 0) {
                setHomePageId(filtered[0].id);
              }
              if (activePageIndex >= filtered.length) {
                snapToPage(Math.max(0, filtered.length - 1));
              }
            }}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {fakeApp && (
          <FakeAppOverlay
            target={fakeApp}
            containerRect={{ width: screenWidth, height: screenHeight }}
            theme={theme}
            motionSettings={motionSettings}
            calendarEvents={calendarEvents}
            onAddCalendarEvent={(ev) => setCalendarEvents((prev) => [ev, ...prev])}
            mediaState={mediaState}
            launcherSettings={{
              themeId,
              colorMode,
              wallpaperId,
              tonalIcons,
              shortcutBarMode,
              onChangeThemeId: (id) => {
                setThemeId(id);
                if (id === 'dark-bold') setColorMode('dark');
                if (id === 'material-you') setColorMode('light');
              },
              onChangeColorMode: setColorMode,
              onChangeWallpaperId: setWallpaperId,
              onChangeTonalIcons: setTonalIcons,
              onChangeShortcutBarMode: handleChangeShortcutBarMode,
            }}
            onClose={() => setFakeApp(null)}
          />
        )}
      </AnimatePresence>
    </div>
  );

  return (
    <IconStyleContext.Provider value={{ tonalIcons, themeId, colorMode, clearedBadgeAppIds }}>
      {isMobileFullscreen ? (
        phoneScreenContent
      ) : (
        <div
          style={{
            width: '100vw',
            height: '100vh',
            background:
              'radial-gradient(circle at 22% 20%, #1E2436 0%, #10131C 55%, #090B10 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 28,
            padding: 20,
            overflow: 'hidden',
          }}
        >
          {/* Phone Device Frame (390 × 844 Inner Screen) */}
          <div
            style={{
              padding: 12,
              borderRadius: 52,
              background: 'linear-gradient(145deg, #2A2F3D, #13161E)',
              boxShadow:
                '0 34px 80px rgba(0,0,0,0.65), inset 0 1px 1px rgba(255,255,255,0.22)',
              border: '1px solid rgba(255,255,255,0.12)',
              position: 'relative',
              transform: viewport.h < 900 ? `scale(${Math.min(1, (viewport.h - 32) / 868)})` : undefined,
            }}
          >
            {phoneScreenContent}
          </div>

          {/* Desktop Companion Panel (Abseits der Launcher-Fläche) */}
          {viewport.w >= 860 && (
            <div
              style={{
                width: 280,
                display: 'flex',
                flexDirection: 'column',
                gap: 14,
                color: '#E2E8F0',
              }}
            >
              <div>
                <div style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#38BDF8' }}>
                  KachelOS · Prototyp
                </div>
                <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>
                  Tiling-Launcher
                </div>
                <div style={{ fontSize: 12.5, color: '#94A3B8', marginTop: 4, lineHeight: 1.45 }}>
                  Seite: <strong style={{ color: '#FFFFFF' }}>{pages[activePageIndex]?.title}</strong> ({activePageIndex + 1}/{pages.length})
                </div>
              </div>

              {/* Quick Theme Switcher */}
              <div style={panelCardStyle}>
                <div style={{ fontSize: 11, fontWeight: 700, color: '#94A3B8', marginBottom: 8, textTransform: 'uppercase' }}>
                  Stilvariante
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {(
                    [
                      { id: 'material-you', label: 'Material You' },
                      { id: 'dark-bold', label: 'Dunkel & kräftig' },
                      { id: 'glass', label: 'Glas' },
                    ] as const
                  ).map((t) => {
                    const active = themeId === t.id;
                    return (
                      <button
                        key={t.id}
                        type="button"
                        onClick={() => {
                          setThemeId(t.id);
                          if (t.id === 'dark-bold') setColorMode('dark');
                          if (t.id === 'material-you') setColorMode('light');
                        }}
                        style={{
                          padding: '8px 12px',
                          borderRadius: 12,
                          border: active ? '1px solid #38BDF8' : '1px solid rgba(255,255,255,0.08)',
                          background: active ? 'rgba(56,189,248,0.16)' : 'rgba(255,255,255,0.03)',
                          color: active ? '#FFFFFF' : '#CBD5E1',
                          fontSize: 12.5,
                          fontWeight: 600,
                          textAlign: 'left',
                          cursor: 'pointer',
                        }}
                      >
                        {t.label}
                      </button>
                    );
                  })}
                </div>
                <button
                  type="button"
                  onClick={() => {
                    triggerHaptic('tick', { enabled: motionSettings.hapticsEnabled });
                    setTonalIcons((v) => !v);
                  }}
                  style={{
                    marginTop: 8,
                    width: '100%',
                    padding: '7px 12px',
                    borderRadius: 12,
                    border: tonalIcons
                      ? '1px solid rgba(52, 211, 153, 0.55)'
                      : '1px solid rgba(255,255,255,0.1)',
                    background: tonalIcons
                      ? 'rgba(52, 211, 153, 0.14)'
                      : 'rgba(255,255,255,0.03)',
                    color: tonalIcons ? '#A7F3D0' : '#CBD5E1',
                    fontSize: 12,
                    fontWeight: 600,
                    textAlign: 'left',
                    cursor: 'pointer',
                  }}
                >
                  Tonale Icons: {tonalIcons ? 'An (Ruhig)' : 'Aus (Bunt)'}
                </button>
              </div>

              {/* Wallpaper & Color World Picker */}
              <div style={panelCardStyle}>
                <div style={{ fontSize: 11, fontWeight: 700, color: '#94A3B8', marginBottom: 8, textTransform: 'uppercase' }}>
                  Hintergrund & Farbwelt
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 6 }}>
                  {WALLPAPER_PRESETS.map((wp) => {
                    const active = wallpaperId === wp.id;
                    return (
                      <button
                        key={wp.id}
                        type="button"
                        onClick={() => {
                          triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                          setWallpaperId(wp.id);
                        }}
                        style={{
                          padding: '7px 8px',
                          borderRadius: 11,
                          border: active ? '1px solid #38BDF8' : '1px solid rgba(255,255,255,0.08)',
                          background: active ? 'rgba(56,189,248,0.16)' : 'rgba(255,255,255,0.03)',
                          color: active ? '#FFFFFF' : '#CBD5E1',
                          fontSize: 11,
                          fontWeight: 600,
                          display: 'flex',
                          alignItems: 'center',
                          gap: 6,
                          cursor: 'pointer',
                          textAlign: 'left',
                        }}
                      >
                        <span
                          style={{
                            width: 10,
                            height: 10,
                            borderRadius: 99,
                            background: wp.previewColor,
                            flexShrink: 0,
                          }}
                        />
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {wp.label}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* 4. Reihe Kürzel-Leiste Option */}
              <div style={panelCardStyle}>
                <div style={{ fontSize: 11, fontWeight: 700, color: '#94A3B8', marginBottom: 8, textTransform: 'uppercase' }}>
                  4. Reihe · Kürzel-Leiste
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 5 }}>
                  {(
                    [
                      { id: 'all', label: 'Alle' },
                      { id: 'start-only', label: 'Start' },
                      { id: 'off', label: 'Aus' },
                    ] as const
                  ).map((opt) => {
                    const active = shortcutBarMode === opt.id;
                    return (
                      <button
                        key={opt.id}
                        type="button"
                        onClick={() => {
                          triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                          handleChangeShortcutBarMode(opt.id);
                        }}
                        style={{
                          padding: '7px 6px',
                          borderRadius: 10,
                          border: active ? '1px solid #38BDF8' : '1px solid rgba(255,255,255,0.08)',
                          background: active ? 'rgba(56,189,248,0.18)' : 'rgba(255,255,255,0.03)',
                          color: active ? '#FFFFFF' : '#CBD5E1',
                          fontSize: 11.5,
                          fontWeight: 600,
                          cursor: 'pointer',
                        }}
                      >
                        {opt.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Visual Haptic Tick HUD */}
              <div style={panelCardStyle}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, fontWeight: 700, color: '#94A3B8', textTransform: 'uppercase' }}>
                    <Vibrate size={13} color="#38BDF8" />
                    <span>Haptik-Tick (Visuell)</span>
                  </div>
                  <span
                    style={{
                      width: 8,
                      height: 8,
                      borderRadius: 99,
                      background: latestHaptic && Date.now() - latestHaptic.timestamp < 900 ? '#38BDF8' : '#334155',
                    }}
                  />
                </div>
                <div style={{ fontSize: 13, fontWeight: 600, color: '#FFFFFF', minHeight: 20 }}>
                  {latestHaptic ? `${latestHaptic.label}` : 'Bereit für Interaktion'}
                </div>
              </div>

              {/* Action Buttons: Gear + Reset + Edit Mode */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                <button
                  type="button"
                  onClick={() => setShowDevMenu(true)}
                  style={{
                    minHeight: 44,
                    borderRadius: 14,
                    border: '1px solid rgba(255,255,255,0.14)',
                    background: 'rgba(255,255,255,0.08)',
                    color: '#FFFFFF',
                    fontSize: 12.5,
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 6,
                    cursor: 'pointer',
                  }}
                >
                  <Settings2 size={15} />
                  <span>Entwickler</span>
                </button>

                <button
                  type="button"
                  onClick={handleResetAll}
                  style={{
                    minHeight: 44,
                    borderRadius: 14,
                    border: '1px solid rgba(239,68,68,0.35)',
                    background: 'rgba(239,68,68,0.14)',
                    color: '#FCA5A5',
                    fontSize: 12.5,
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 6,
                    cursor: 'pointer',
                  }}
                >
                  <RotateCcw size={15} />
                  <span>Zurücksetzen</span>
                </button>
              </div>

              <div style={{ fontSize: 11.5, color: '#64748B', lineHeight: 1.5 }}>
                • Horizontal wischen: Seiten wechseln
                <br />• Nach oben wischen: Suche öffnen
                <br />• Kachel halten: Kontextmenü / Ziehen
                <br />• Fuge halten: Bearbeiten-Modus
                <br />• Doppeltipp auf Trennlinie: Drehen
              </div>
            </div>
          )}

          {/* Compact Floating Gear + Reset for medium windows (500px–859px) */}
          {viewport.w < 860 && (
            <div
              style={{
                position: 'fixed',
                top: 14,
                right: 14,
                display: 'flex',
                gap: 8,
                zIndex: 90,
              }}
            >
              <button
                type="button"
                onClick={handleResetAll}
                title="Zurücksetzen"
                style={floatingIconBtn}
              >
                <RotateCcw size={17} />
              </button>
              <button
                type="button"
                onClick={() => setShowDevMenu(true)}
                title="Entwicklermenü"
                style={floatingIconBtn}
              >
                <Settings2 size={17} />
              </button>
            </div>
          )}
        </div>
      )}

      {/* Developer Menu Modal */}
      <DeveloperMenu
        isOpen={showDevMenu}
        onClose={() => setShowDevMenu(false)}
        themeId={themeId}
        colorMode={colorMode}
        tonalIcons={tonalIcons}
        shortcutBarMode={shortcutBarMode}
        motionSettings={motionSettings}
        onChangeThemeId={setThemeId}
        onChangeColorMode={setColorMode}
        onChangeTonalIcons={setTonalIcons}
        onChangeShortcutBarMode={handleChangeShortcutBarMode}
        onChangeMotionSettings={setMotionSettings}
        onResetLayout={handleResetAll}
      />
    </IconStyleContext.Provider>
  );
}

const panelCardStyle: React.CSSProperties = {
  padding: 14,
  borderRadius: 18,
  background: 'rgba(255,255,255,0.04)',
  border: '1px solid rgba(255,255,255,0.09)',
};

const floatingIconBtn: React.CSSProperties = {
  width: 42,
  height: 42,
  borderRadius: 99,
  border: '1px solid rgba(255,255,255,0.18)',
  background: 'rgba(15,23,42,0.84)',
  color: '#FFFFFF',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'pointer',
  boxShadow: '0 8px 20px rgba(0,0,0,0.35)',
};

export default App;
