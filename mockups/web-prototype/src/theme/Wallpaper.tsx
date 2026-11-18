import React from 'react';
import { motion, MotionValue } from 'framer-motion';
import { ColorMode, ThemeVariantId, WallpaperPresetId } from './themes';

interface WallpaperProps {
  themeId: ThemeVariantId;
  mode: ColorMode;
  wallpaperId?: WallpaperPresetId;
  parallaxX: MotionValue<number>;
}

interface PaletteStops {
  bg1: string;
  bg2: string;
  bg3: string;
  orb1: string;
  orb2: string;
  orb3: string;
  wave1: string;
  wave2: string;
}

const MATERIAL_PALETTES: Record<WallpaperPresetId, { light: PaletteStops; dark: PaletteStops }> = {
  sand: {
    light: {
      bg1: '#EFE5D8',
      bg2: '#E7D8C5',
      bg3: '#DAE2D6',
      orb1: '#E9B996',
      orb2: '#A9C7B5',
      orb3: '#F59E0B',
      wave1: 'rgba(196, 139, 98, 0.22)',
      wave2: 'rgba(128, 164, 142, 0.20)',
    },
    dark: {
      bg1: '#1A1612',
      bg2: '#241D17',
      bg3: '#161A18',
      orb1: '#7E4E34',
      orb2: '#3E5A4C',
      orb3: '#B45309',
      wave1: 'rgba(218, 178, 134, 0.12)',
      wave2: 'rgba(132, 168, 148, 0.12)',
    },
  },
  emerald: {
    light: {
      bg1: '#DCFCE7',
      bg2: '#CCFBF1',
      bg3: '#E0F2FE',
      orb1: '#6EE7B7',
      orb2: '#5EEAD4',
      orb3: '#34D399',
      wave1: 'rgba(16, 185, 129, 0.22)',
      wave2: 'rgba(14, 165, 233, 0.16)',
    },
    dark: {
      bg1: '#071E18',
      bg2: '#0B2B26',
      bg3: '#091A24',
      orb1: '#047857',
      orb2: '#0F766E',
      orb3: '#10B981',
      wave1: 'rgba(52, 211, 153, 0.16)',
      wave2: 'rgba(45, 212, 191, 0.12)',
    },
  },
  cobalt: {
    light: {
      bg1: '#DBEAFE',
      bg2: '#E0E7FF',
      bg3: '#EDE9FE',
      orb1: '#93C5FD',
      orb2: '#A5B4FC',
      orb3: '#60A5FA',
      wave1: 'rgba(59, 130, 246, 0.22)',
      wave2: 'rgba(99, 102, 241, 0.18)',
    },
    dark: {
      bg1: '#0A1328',
      bg2: '#111D3D',
      bg3: '#181438',
      orb1: '#1D4ED8',
      orb2: '#4338CA',
      orb3: '#2563EB',
      wave1: 'rgba(96, 165, 250, 0.18)',
      wave2: 'rgba(129, 140, 248, 0.14)',
    },
  },
  terracotta: {
    light: {
      bg1: '#FFE4E6',
      bg2: '#FFEDD5',
      bg3: '#FEF3C7',
      orb1: '#FDA4AF',
      orb2: '#FDBA74',
      orb3: '#FB7185',
      wave1: 'rgba(244, 63, 94, 0.20)',
      wave2: 'rgba(249, 115, 22, 0.18)',
    },
    dark: {
      bg1: '#240E14',
      bg2: '#2B1418',
      bg3: '#221510',
      orb1: '#9F1239',
      orb2: '#9A3412',
      orb3: '#E11D48',
      wave1: 'rgba(251, 113, 133, 0.16)',
      wave2: 'rgba(251, 146, 60, 0.14)',
    },
  },
};

export const Wallpaper: React.FC<WallpaperProps> = ({
  themeId,
  mode,
  wallpaperId = 'sand',
  parallaxX,
}) => {
  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        overflow: 'hidden',
        pointerEvents: 'none',
        zIndex: 0,
      }}
    >
      <motion.div
        style={{
          position: 'absolute',
          top: -20,
          bottom: -20,
          left: -180,
          width: 'calc(100% + 900px)',
          x: parallaxX,
          willChange: 'transform',
        }}
      >
        {themeId === 'material-you' && <MaterialYouArt mode={mode} wallpaperId={wallpaperId} />}
        {themeId === 'dark-bold' && <DarkBoldArt mode={mode} wallpaperId={wallpaperId} />}
        {themeId === 'glass' && <GlassAuroraArt mode={mode} wallpaperId={wallpaperId} />}
      </motion.div>
    </div>
  );
};

const MaterialYouArt: React.FC<{ mode: ColorMode; wallpaperId: WallpaperPresetId }> = ({
  mode,
  wallpaperId,
}) => {
  const pal = MATERIAL_PALETTES[wallpaperId][mode === 'dark' ? 'dark' : 'light'];
  return (
    <svg
      viewBox="0 0 1200 900"
      preserveAspectRatio="xMidYMid slice"
      style={{ width: '100%', height: '100%' }}
    >
      <defs>
        <linearGradient id="my-bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={pal.bg1} />
          <stop offset="50%" stopColor={pal.bg2} />
          <stop offset="100%" stopColor={pal.bg3} />
        </linearGradient>
        <radialGradient id="my-orb1" cx="0.28" cy="0.25" r="0.45">
          <stop offset="0%" stopColor={pal.orb1} stopOpacity="0.85" />
          <stop offset="100%" stopColor={pal.orb1} stopOpacity="0" />
        </radialGradient>
        <radialGradient id="my-orb2" cx="0.72" cy="0.68" r="0.5">
          <stop offset="0%" stopColor={pal.orb2} stopOpacity="0.82" />
          <stop offset="100%" stopColor={pal.orb2} stopOpacity="0" />
        </radialGradient>
      </defs>
      <rect width="1200" height="900" fill="url(#my-bg)" />
      <circle cx="340" cy="230" r="320" fill="url(#my-orb1)" />
      <circle cx="880" cy="610" r="360" fill="url(#my-orb2)" />
      <path
        d="M-40,560 C240,450 480,640 760,520 C980,435 1100,510 1250,460 L1250,920 L-40,920 Z"
        fill={pal.wave1}
      />
      <path
        d="M-40,290 C260,190 540,370 840,260 C1020,195 1140,255 1250,235 L1250,0 L-40,0 Z"
        fill={pal.wave2}
      />
    </svg>
  );
};

const DarkBoldArt: React.FC<{ mode: ColorMode; wallpaperId: WallpaperPresetId }> = ({
  mode,
  wallpaperId,
}) => {
  const isLight = mode === 'light';
  const pal = MATERIAL_PALETTES[wallpaperId][isLight ? 'light' : 'dark'];
  return (
    <svg
      viewBox="0 0 1200 900"
      preserveAspectRatio="xMidYMid slice"
      style={{ width: '100%', height: '100%' }}
    >
      <defs>
        <linearGradient id="db-bg" x1="0" y1="0" x2="0.8" y2="1">
          <stop offset="0%" stopColor={isLight ? pal.bg1 : '#090B10'} />
          <stop offset="60%" stopColor={isLight ? pal.bg2 : pal.bg1} />
          <stop offset="100%" stopColor={isLight ? pal.bg3 : '#0A0C12'} />
        </linearGradient>
        <linearGradient id="db-ribbon" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={pal.orb1} stopOpacity={isLight ? '0.32' : '0.42'} />
          <stop offset="100%" stopColor={pal.orb2} stopOpacity={isLight ? '0.24' : '0.34'} />
        </linearGradient>
      </defs>
      <rect width="1200" height="900" fill="url(#db-bg)" />
      <polygon points="-60,180 1260,-40 1260,180 -60,400" fill="url(#db-ribbon)" />
      <polygon points="-60,650 1260,440 1260,660 -60,860" fill={pal.wave2} />
      <circle
        cx="780"
        cy="260"
        r="210"
        fill="none"
        stroke={pal.wave1}
        strokeWidth="44"
      />
      <circle
        cx="320"
        cy="710"
        r="160"
        fill="none"
        stroke={pal.wave2}
        strokeWidth="36"
      />
    </svg>
  );
};

const GlassAuroraArt: React.FC<{ mode: ColorMode; wallpaperId: WallpaperPresetId }> = ({
  mode,
  wallpaperId,
}) => {
  const isDark = mode === 'dark';
  const glassAccents: Record<WallpaperPresetId, [string, string, string, string]> = {
    sand: ['#2D3FA8', '#7B2CBF', '#FF5E9C', '#38BDF8'],
    emerald: ['#064E3B', '#0F766E', '#34D399', '#38BDF8'],
    cobalt: ['#1E3A8A', '#4338CA', '#60A5FA', '#C084FC'],
    terracotta: ['#7F1D1D', '#9A3412', '#FB7185', '#FBBF24'],
  };
  const [g1, g2, a1, a2] = glassAccents[wallpaperId];
  return (
    <svg
      viewBox="0 0 1200 900"
      preserveAspectRatio="xMidYMid slice"
      style={{ width: '100%', height: '100%' }}
    >
      <defs>
        <linearGradient id="gl-bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={isDark ? '#0D1024' : g1} />
          <stop offset="45%" stopColor={isDark ? g1 : g2} />
          <stop offset="100%" stopColor={isDark ? '#0D2A3F' : g1} />
        </linearGradient>
        <radialGradient id="gl-a" cx="0.25" cy="0.22" r="0.45">
          <stop offset="0%" stopColor={a1} stopOpacity={isDark ? '0.68' : '0.88'} />
          <stop offset="100%" stopColor={a1} stopOpacity="0" />
        </radialGradient>
        <radialGradient id="gl-b" cx="0.72" cy="0.46" r="0.48">
          <stop offset="0%" stopColor={a2} stopOpacity={isDark ? '0.66' : '0.88'} />
          <stop offset="100%" stopColor={a2} stopOpacity="0" />
        </radialGradient>
        <radialGradient id="gl-c" cx="0.45" cy="0.82" r="0.48">
          <stop offset="0%" stopColor="#FBBF24" stopOpacity={isDark ? '0.45' : '0.72'} />
          <stop offset="100%" stopColor="#FBBF24" stopOpacity="0" />
        </radialGradient>
      </defs>
      <rect width="1200" height="900" fill="url(#gl-bg)" />
      <circle cx="320" cy="210" r="340" fill="url(#gl-a)" />
      <circle cx="880" cy="410" r="360" fill="url(#gl-b)" />
      <circle cx="520" cy="730" r="340" fill="url(#gl-c)" />
      <path
        d="M-50,380 Q320,250 640,410 T1260,340"
        fill="none"
        stroke="rgba(255,255,255,0.22)"
        strokeWidth="64"
        strokeLinecap="round"
      />
    </svg>
  );
};
