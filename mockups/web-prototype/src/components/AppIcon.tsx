import React, { useContext } from 'react';
import { ColorMode, ThemeVariantId } from '../theme/themes';
import {
  Activity,
  AlarmClock,
  Aperture,
  Archive,
  AtSign,
  BarChart3,
  BatteryCharging,
  BookMarked,
  BookOpen,
  Bookmark,
  Bluetooth,
  Calculator,
  Calendar,
  Camera,
  CheckSquare,
  Cloud,
  CloudRain,
  Compass,
  Crown,
  Dices,
  Disc3,
  FileSignature,
  FileText,
  Film,
  Flashlight,
  Focus,
  FolderOpen,
  Gamepad2,
  Globe,
  HardDrive,
  Headphones,
  Headset,
  HeartPulse,
  Home,
  Image,
  KeyRound,
  Landmark,
  Languages,
  Mail,
  MapPin,
  MessageCircle,
  MessagesSquare,
  Mic,
  Moon,
  Mountain,
  Music2,
  Navigation,
  Newspaper,
  Package,
  Palette,
  PenTool,
  Phone,
  Printer,
  Puzzle,
  QrCode,
  Radio,
  Rocket,
  Rss,
  Ruler,
  ScanLine,
  Search,
  Settings,
  ShoppingBag,
  Sliders,
  Sprout,
  Sun,
  Terminal,
  Timer,
  Train,
  Users,
  Utensils,
  Vibrate,
  Video,
  Wallet,
  Wifi,
  Wind,
  type LucideIcon,
} from 'lucide-react';

const ICON_MAP: Record<string, LucideIcon> = {
  Activity,
  AlarmClock,
  Aperture,
  Archive,
  AtSign,
  BarChart3,
  BatteryCharging,
  BookMarked,
  BookOpen,
  Bookmark,
  Bluetooth,
  Calculator,
  Calendar,
  Camera,
  CheckSquare,
  Cloud,
  CloudRain,
  Compass,
  Crown,
  Dices,
  Disc3,
  FileSignature,
  FileText,
  Film,
  Flashlight,
  Focus,
  FolderOpen,
  Gamepad2,
  Globe,
  HardDrive,
  Headphones,
  Headset,
  HeartPulse,
  Home,
  Image,
  KeyRound,
  Landmark,
  Languages,
  Mail,
  MapPin,
  MessageCircle,
  MessagesSquare,
  Mic,
  Moon,
  Mountain,
  Music2,
  Navigation,
  Newspaper,
  Package,
  Palette,
  PenTool,
  Phone,
  Printer,
  Puzzle,
  QrCode,
  Radio,
  Rocket,
  Rss,
  Ruler,
  ScanLine,
  Search,
  Settings,
  ShoppingBag,
  Sliders,
  Sprout,
  Sun,
  Terminal,
  Timer,
  Train,
  Users,
  Utensils,
  Vibrate,
  Video,
  Wallet,
  Wifi,
  Wind,
};

export function renderGlyph(iconKey: string, size: number, color = '#FFFFFF', strokeWidth = 2) {
  const Icon = ICON_MAP[iconKey] ?? Compass;
  return <Icon size={size} color={color} strokeWidth={strokeWidth} />;
}

export interface IconStyleConfig {
  tonalIcons: boolean;
  themeId: ThemeVariantId;
  colorMode: ColorMode;
  clearedBadgeAppIds?: string[];
}

export const IconStyleContext = React.createContext<IconStyleConfig>({
  tonalIcons: false,
  themeId: 'material-you',
  colorMode: 'light',
  clearedBadgeAppIds: [],
});

interface AppSquircleProps {
  appId?: string;
  iconKey: string;
  color: string;
  size?: number;
  badge?: number;
  isNew?: boolean;
  onClick?: (e: React.MouseEvent) => void;
}

export const AppSquircle: React.FC<AppSquircleProps> = ({
  appId,
  iconKey,
  color,
  size = 44,
  badge,
  isNew,
  onClick,
}) => {
  const { tonalIcons, themeId, colorMode, clearedBadgeAppIds } = useContext(IconStyleContext);
  const effectiveBadge = appId && clearedBadgeAppIds?.includes(appId) ? undefined : badge;
  const radius = Math.round(size * 0.29);
  const glyphSize = Math.round(size * 0.48);

  let squircleBg = `linear-gradient(155deg, ${color}, ${color}DD)`;
  let glyphColor = '#FFFFFF';
  let border = 'none';
  let boxShadow = 'inset 0 1px 1px rgba(255,255,255,0.34), 0 4px 10px rgba(0,0,0,0.22)';

  if (tonalIcons) {
    if (themeId === 'glass') {
      squircleBg = 'rgba(255, 255, 255, 0.20)';
      glyphColor = '#FFFFFF';
      border = '1px solid rgba(255, 255, 255, 0.34)';
      boxShadow = 'inset 0 1px 0 rgba(255,255,255,0.28), 0 4px 10px rgba(0,0,0,0.16)';
    } else if (themeId === 'dark-bold') {
      const isLight = colorMode === 'light';
      squircleBg = isLight ? 'hsl(222 48% 85%)' : 'hsl(222 42% 24%)';
      glyphColor = isLight ? 'hsl(222 76% 32%)' : 'hsl(222 92% 82%)';
      border = isLight
        ? '1px solid hsla(222 50% 45% / 0.16)'
        : '1px solid hsla(222 80% 70% / 0.24)';
      boxShadow = 'inset 0 1px 0 rgba(255,255,255,0.18), 0 4px 10px rgba(0,0,0,0.22)';
    } else {
      const isDark = colorMode === 'dark';
      squircleBg = isDark ? 'hsl(34 26% 25%)' : 'hsl(34 44% 84%)';
      glyphColor = isDark ? 'hsl(34 65% 82%)' : 'hsl(34 55% 26%)';
      border = isDark
        ? '1px solid hsla(34 45% 75% / 0.16)'
        : '1px solid hsla(34 40% 45% / 0.14)';
      boxShadow = 'inset 0 1px 0 rgba(255,255,255,0.22), 0 3px 8px rgba(40,28,14,0.10)';
    }
  }

  return (
    <div
      onClick={onClick}
      style={{
        position: 'relative',
        width: size,
        height: size,
        borderRadius: radius,
        background: squircleBg,
        border,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxShadow,
        flexShrink: 0,
      }}
    >
      {renderGlyph(iconKey, glyphSize, glyphColor, 2.1)}
      {typeof effectiveBadge === 'number' && effectiveBadge > 0 && (
        <span
          style={{
            position: 'absolute',
            top: -4,
            right: -4,
            minWidth: 17,
            height: 17,
            padding: '0 4px',
            borderRadius: 999,
            background: '#EF4444',
            color: '#FFFFFF',
            fontSize: 10,
            fontWeight: 700,
            lineHeight: '17px',
            textAlign: 'center',
            boxShadow: '0 2px 5px rgba(0,0,0,0.35)',
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          {effectiveBadge}
        </span>
      )}
      {isNew && (
        <span
          style={{
            position: 'absolute',
            top: -2,
            right: -2,
            width: 10,
            height: 10,
            borderRadius: 999,
            background: '#34D399',
            boxShadow: '0 0 8px #34D399, 0 0 0 2px rgba(15, 23, 42, 0.7)',
          }}
        />
      )}
    </div>
  );
};
