export type ThemeVariantId = 'material-you' | 'dark-bold' | 'glass';
export type ColorMode = 'light' | 'dark';
export type WallpaperPresetId = 'sand' | 'emerald' | 'cobalt' | 'terracotta';

export const WALLPAPER_PRESETS: {
  id: WallpaperPresetId;
  label: string;
  previewColor: string;
  baseHue: number;
}[] = [
  { id: 'sand', label: 'Sand & Bernstein', previewColor: '#D97706', baseHue: 28 },
  { id: 'emerald', label: 'Nordlicht', previewColor: '#10B981', baseHue: 158 },
  { id: 'cobalt', label: 'Kobalt', previewColor: '#3B82F6', baseHue: 220 },
  { id: 'terracotta', label: 'Terrakotta', previewColor: '#F43F5E', baseHue: 12 },
];

export interface ThemeTokens {
  id: ThemeVariantId;
  mode: ColorMode;
  name: string;
  description: string;
  tileRadius: number;
  edgePadding: number;
  gapUse: number;
  gapEdit: number;
  // Text
  textPrimary: string;
  textSecondary: string;
  textMuted: string;
  // Chrome & Overlays
  statusText: string;
  sheetBg: string;
  sheetBorder: string;
  overlayBackdrop: string;
  pillBg: string;
  pillActive: string;
  dividerHandleBg: string;
  dividerHandleDot: string;
  // Surface styles
  isGlass: boolean;
  isBold: boolean;
  blurPx: number;
  // Dynamic tile surface generator
  getTileSurface: (accentHue?: number, emphasis?: 'normal' | 'hero' | 'muted') => {
    background: string;
    border: string;
    textPrimary: string;
    textSecondary: string;
    accentColor: string;
    accentBg: string;
    shadow: string;
  };
}

function clampHue(hue?: number, fallback = 215): number {
  if (typeof hue !== 'number' || Number.isNaN(hue)) return fallback;
  return ((hue % 360) + 360) % 360;
}

const WALLPAPER_ACCENT: Record<
  WallpaperPresetId,
  { baseHue: number; pillDark: string; pillLight: string; dividerDark: string; dividerLight: string }
> = {
  sand: {
    baseHue: 34,
    pillDark: '#F5E3C8',
    pillLight: '#2C2217',
    dividerDark: '#F2DFC6',
    dividerLight: '#3A2E20',
  },
  emerald: {
    baseHue: 158,
    pillDark: '#A7F3D0',
    pillLight: '#064E3B',
    dividerDark: '#A7F3D0',
    dividerLight: '#065F46',
  },
  cobalt: {
    baseHue: 220,
    pillDark: '#BFDBFE',
    pillLight: '#1E3A8A',
    dividerDark: '#BFDBFE',
    dividerLight: '#1E40AF',
  },
  terracotta: {
    baseHue: 12,
    pillDark: '#FECDD3',
    pillLight: '#7F1D1D',
    dividerDark: '#FECDD3',
    dividerLight: '#991B1B',
  },
};

export function getThemeTokens(
  id: ThemeVariantId,
  mode: ColorMode,
  wallpaperId: WallpaperPresetId = 'sand'
): ThemeTokens {
  const wpAccent = WALLPAPER_ACCENT[wallpaperId] ?? WALLPAPER_ACCENT.sand;

  if (id === 'material-you') {
    const isDark = mode === 'dark';
    return {
      id,
      mode,
      name: 'Material You',
      description: 'Tonale Palette, weiche Formen, ruhige Typografie',
      tileRadius: 22,
      edgePadding: 8,
      gapUse: 6,
      gapEdit: 12,
      textPrimary: isDark ? '#F2ECE4' : '#1E1B16',
      textSecondary: isDark ? '#CBC2B4' : '#534A3F',
      textMuted: isDark ? '#9A9185' : '#7C7265',
      statusText: isDark ? '#F5EFE6' : '#26211A',
      sheetBg: isDark ? 'rgba(34, 30, 26, 0.96)' : 'rgba(250, 245, 238, 0.96)',
      sheetBorder: isDark ? 'rgba(255, 240, 220, 0.12)' : 'rgba(70, 52, 34, 0.1)',
      overlayBackdrop: isDark ? 'rgba(15, 13, 10, 0.65)' : 'rgba(42, 34, 24, 0.35)',
      pillBg: isDark ? 'rgba(255, 244, 228, 0.28)' : 'rgba(48, 38, 28, 0.25)',
      pillActive: wpAccent[isDark ? 'pillDark' : 'pillLight'],
      dividerHandleBg: wpAccent[isDark ? 'dividerDark' : 'dividerLight'],
      dividerHandleDot: isDark ? '#2A2218' : '#FAF3EA',
      isGlass: false,
      isBold: false,
      blurPx: 0,
      getTileSurface: (accentHue, emphasis = 'normal') => {
        const rawH = clampHue(accentHue, wpAccent.baseHue);
        // Material You color extraction shifts surface hues gently toward the active wallpaper seed
        const h =
          wallpaperId === 'sand'
            ? rawH
            : Math.round((rawH * 0.38 + wpAccent.baseHue * 0.62) % 360);
        if (isDark) {
          const l = emphasis === 'hero' ? 24 : emphasis === 'muted' ? 17 : 20;
          return {
            background: `hsl(${h} 24% ${l}%)`,
            border: `1px solid hsla(${h} 40% 75% / 0.14)`,
            textPrimary: `hsl(${h} 35% 93%)`,
            textSecondary: `hsl(${h} 20% 74%)`,
            accentColor: `hsl(${h} 78% 74%)`,
            accentBg: `hsla(${h} 65% 68% / 0.18)`,
            shadow: '0 6px 18px rgba(0,0,0,0.28)',
          };
        }
        const l = emphasis === 'hero' ? 88 : emphasis === 'muted' ? 91 : 93;
        return {
          background: `hsl(${h} 42% ${l}%)`,
          border: `1px solid hsla(${h} 35% 40% / 0.10)`,
          textPrimary: `hsl(${h} 28% 15%)`,
          textSecondary: `hsl(${h} 18% 36%)`,
          accentColor: `hsl(${h} 62% 38%)`,
          accentBg: `hsla(${h} 58% 50% / 0.14)`,
          shadow: '0 4px 14px rgba(52,38,20,0.08)',
        };
      },
    };
  }

  if (id === 'dark-bold') {
    const isLight = mode === 'light';
    return {
      id,
      mode,
      name: 'Dunkel & kräftig',
      description: 'Satte Farben, riesige Zahlen, hoher Kontrast',
      tileRadius: 20,
      edgePadding: 8,
      gapUse: 6,
      gapEdit: 12,
      textPrimary: isLight ? '#101218' : '#FFFFFF',
      textSecondary: isLight ? '#384052' : 'rgba(255,255,255,0.78)',
      textMuted: isLight ? '#5E667A' : 'rgba(255,255,255,0.56)',
      statusText: isLight ? '#101218' : '#FFFFFF',
      sheetBg: isLight ? 'rgba(244, 246, 250, 0.98)' : 'rgba(16, 18, 24, 0.97)',
      sheetBorder: isLight ? 'rgba(16, 18, 24, 0.12)' : 'rgba(255, 255, 255, 0.14)',
      overlayBackdrop: isLight ? 'rgba(12, 14, 20, 0.45)' : 'rgba(0, 0, 0, 0.72)',
      pillBg: isLight ? 'rgba(16, 18, 24, 0.25)' : 'rgba(255, 255, 255, 0.28)',
      pillActive: wallpaperId === 'sand' ? (isLight ? '#101218' : '#FFFFFF') : wpAccent[isLight ? 'pillLight' : 'pillDark'],
      dividerHandleBg: '#FFFFFF',
      dividerHandleDot: '#101218',
      isGlass: false,
      isBold: true,
      blurPx: 0,
      getTileSurface: (accentHue, emphasis = 'normal') => {
        const rawH = clampHue(accentHue, wpAccent.baseHue);
        const h =
          wallpaperId === 'sand'
            ? rawH
            : Math.round((rawH * 0.65 + wpAccent.baseHue * 0.35) % 360);
        if (isLight) {
          const bgL = emphasis === 'hero' ? 42 : emphasis === 'muted' ? 92 : 87;
          const isDarkCard = bgL < 55;
          return {
            background: `hsl(${h} ${isDarkCard ? 74 : 45}% ${bgL}%)`,
            border: `1.5px solid hsla(${h} 60% ${isDarkCard ? 70 : 35}% / 0.22)`,
            textPrimary: isDarkCard ? '#FFFFFF' : '#11131A',
            textSecondary: isDarkCard ? 'rgba(255,255,255,0.82)' : '#394155',
            accentColor: isDarkCard ? '#FFFFFF' : `hsl(${h} 78% 40%)`,
            accentBg: isDarkCard ? 'rgba(255,255,255,0.18)' : `hsla(${h} 75% 45% / 0.14)`,
            shadow: '0 6px 18px rgba(10,14,26,0.12)',
          };
        }
        const sat = emphasis === 'hero' ? 68 : emphasis === 'muted' ? 32 : 54;
        const light = emphasis === 'hero' ? 28 : emphasis === 'muted' ? 14 : 21;
        return {
          background: `linear-gradient(155deg, hsl(${h} ${sat}% ${light + 4}%), hsl(${h} ${sat}% ${light - 3}%))`,
          border: `1px solid hsla(${h} 75% 68% / 0.26)`,
          textPrimary: '#FFFFFF',
          textSecondary: 'rgba(255, 255, 255, 0.80)',
          accentColor: `hsl(${h} 90% 72%)`,
          accentBg: `hsla(${h} 85% 62% / 0.22)`,
          shadow: '0 10px 24px rgba(0, 0, 0, 0.44)',
        };
      },
    };
  }

  // 'glass'
  const isDarkGlass = mode === 'dark';
  return {
    id: 'glass',
    mode,
    name: 'Glas',
    description: 'Milchglas-Kacheln, feine Kanten, leuchtender Hintergrund',
    tileRadius: 24,
    edgePadding: 8,
    gapUse: 6,
    gapEdit: 12,
    textPrimary: '#FFFFFF',
    textSecondary: 'rgba(255, 255, 255, 0.84)',
    textMuted: 'rgba(255, 255, 255, 0.64)',
    statusText: '#FFFFFF',
    sheetBg: isDarkGlass ? 'rgba(18, 22, 38, 0.82)' : 'rgba(28, 34, 58, 0.72)',
    sheetBorder: 'rgba(255, 255, 255, 0.26)',
    overlayBackdrop: 'rgba(8, 10, 22, 0.56)',
    pillBg: 'rgba(255, 255, 255, 0.34)',
    pillActive: '#FFFFFF',
    dividerHandleBg: 'rgba(255, 255, 255, 0.94)',
    dividerHandleDot: '#1E2540',
    isGlass: true,
    isBold: false,
    blurPx: 20,
    getTileSurface: (accentHue, emphasis = 'normal') => {
      const h = clampHue(accentHue, 260);
      const alpha = isDarkGlass
        ? emphasis === 'hero'
          ? 0.24
          : 0.16
        : emphasis === 'hero'
          ? 0.28
          : 0.20;
      return {
        background: `linear-gradient(145deg, hsla(${h} 65% ${isDarkGlass ? 32 : 76}% / ${alpha + 0.06}), hsla(${(h + 24) % 360} 50% ${isDarkGlass ? 20 : 62}% / ${alpha}))`,
        border: '1px solid rgba(255, 255, 255, 0.28)',
        textPrimary: '#FFFFFF',
        textSecondary: 'rgba(255, 255, 255, 0.84)',
        accentColor: `hsl(${h} 95% 84%)`,
        accentBg: 'rgba(255, 255, 255, 0.18)',
        shadow: '0 8px 28px rgba(10, 12, 30, 0.24)',
      };
    },
  };
}

/** Shared translucent background and active pill colors for bottom dock capsules. */
export function getDockCapsuleTokens(theme: ThemeTokens) {
  const dockBg = theme.isGlass
    ? 'rgba(255, 255, 255, 0.16)'
    : theme.mode === 'dark' || theme.isBold
      ? 'rgba(18, 22, 34, 0.56)'
      : 'rgba(255, 250, 244, 0.62)';

  const activePillBg = theme.isGlass ? 'rgba(255, 255, 255, 0.92)' : theme.pillActive;

  const activeTextColor = theme.isGlass
    ? '#0F172A'
    : theme.mode === 'dark' || theme.isBold
      ? '#0F172A'
      : '#FAF3EA';

  return { dockBg, activePillBg, activeTextColor };
}
