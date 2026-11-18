import { CalendarEvent } from '../data/mockData';
import { TileData, TileSizeTier } from '../layout/types';
import { MotionSettings } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';

export interface TileSurfaceStyle {
  background: string;
  border: string;
  textPrimary: string;
  textSecondary: string;
  accentColor: string;
  accentBg: string;
  shadow: string;
}

export type SystemToggleKey =
  | 'wifi'
  | 'bluetooth'
  | 'flashlight'
  | 'dnd'
  | 'hotspot'
  | 'autoRotate';

export interface SystemControlState {
  wifi: boolean;
  bluetooth: boolean;
  flashlight: boolean;
  dnd: boolean;
  hotspot: boolean;
  autoRotate: boolean;
  brightness: number;
  onToggle: (key: SystemToggleKey) => void;
  onChangeBrightness: (val: number) => void;
}

export interface TileContentProps {
  tile: TileData;
  leafId: string;
  width: number;
  height: number;
  tier: TileSizeTier;
  theme: ThemeTokens;
  surface: TileSurfaceStyle;
  motionSettings: MotionSettings;
  isEditMode: boolean;
  onOpenApp: (appId: string, sourceRect?: DOMRect) => void;
  onOpenGroup: (tile: TileData, leafId: string, sourceRect?: DOMRect) => void;
  onOpenSearch: () => void;
  onOpenFakeTileApp: (tile: TileData, sourceRect?: DOMRect) => void;
  onUpdateTile: (leafId: string, updater: (t: TileData) => TileData) => void;
  calendarEvents?: CalendarEvent[];
  mediaState: {
    trackIndex: number;
    isPlaying: boolean;
    progress: number;
    onTogglePlay: () => void;
    onPrev: () => void;
    onNext: () => void;
    onSeek: (ratio: number) => void;
  };
  systemState: SystemControlState;
}
