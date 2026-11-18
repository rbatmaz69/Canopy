import React, { useRef } from 'react';
import { CalendarEvent } from '../data/mockData';
import { computeSizeTier } from '../layout/tree';
import { TileData, TileSizeTier } from '../layout/types';
import { MotionSettings } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';
import { AllAppsBoxTile, NewlyInstalledTile, SearchBarTile } from './AllAppsTiles';
import { AppGroupTile } from './AppGroupTile';
import {
  BatteryStepsTile,
  BatteryTile,
  CalendarTile,
  CalendarTimelineTile,
  ClockTile,
  GreetingTile,
  NoteTile,
  PhotosTile,
  QuickTogglesTile,
  RemindersTile,
  StepsTile,
  TempCurveTile,
  WeatherTile,
} from './EigenTiles';
import { MediaTile } from './MediaTile';
import { SingleAppTile, ThirdPartyWidgetTile } from './SingleAppAndWidgetTiles';
import { SystemControlState, TileContentProps } from './tileProps';

interface TileRendererProps {
  tile: TileData;
  leafId: string;
  width: number;
  height: number;
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  isEditMode: boolean;
  onOpenApp: (appId: string, sourceRect?: DOMRect) => void;
  onOpenGroup: (tile: TileData, leafId: string, sourceRect?: DOMRect) => void;
  onOpenSearch: () => void;
  onOpenFakeTileApp: (tile: TileData, sourceRect?: DOMRect) => void;
  onUpdateTile: (leafId: string, updater: (t: TileData) => TileData) => void;
  calendarEvents?: CalendarEvent[];
  mediaState: TileContentProps['mediaState'];
  systemState: SystemControlState;
}

export const TileRenderer: React.FC<TileRendererProps> = (props) => {
  const { tile, width, height, theme } = props;
  const prevTierRef = useRef<TileSizeTier | undefined>(undefined);
  const tier = computeSizeTier(width, height, prevTierRef.current);
  prevTierRef.current = tier;

  const isHero = tile.type === 'greeting' || tile.type === 'clock' || Boolean(tile.mediaLarge);
  const surface = theme.getTileSurface(tile.accentHue, isHero ? 'hero' : 'normal');

  const contentProps: TileContentProps = {
    ...props,
    tier,
    surface,
  };

  const isFullBleed =
    (tile.type === 'media' && (Boolean(tile.mediaLarge) || tier === 'L')) ||
    (tile.type === 'photos' && tier !== 'XS');

  const padding =
    isFullBleed
      ? 0
      : tier === 'XS'
        ? 6
        : tile.type === 'third-party-widget'
          ? 8
          : 13;

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        borderRadius: theme.tileRadius,
        background: surface.background,
        border: surface.border,
        color: surface.textPrimary,
        boxShadow: `inset 0 1px 0 rgba(255, 255, 255, ${theme.isGlass ? '0.32' : '0.24'}), ${surface.shadow}`,
        backdropFilter: theme.blurPx > 0 ? `blur(${theme.blurPx}px)` : undefined,
        WebkitBackdropFilter: theme.blurPx > 0 ? `blur(${theme.blurPx}px)` : undefined,
        padding,
        overflow: 'hidden',
        position: 'relative',
      }}
    >
      {renderTileBody(tile.type, contentProps)}
    </div>
  );
};

function renderTileBody(type: TileData['type'], props: TileContentProps) {
  switch (type) {
    case 'greeting':
      return <GreetingTile {...props} />;
    case 'temp-curve':
      return <TempCurveTile {...props} />;
    case 'calendar-timeline':
      return <CalendarTimelineTile {...props} />;
    case 'reminders':
      return <RemindersTile {...props} />;
    case 'battery-steps':
      return <BatteryStepsTile {...props} />;
    case 'clock':
      return <ClockTile {...props} />;
    case 'weather':
      return <WeatherTile {...props} />;
    case 'calendar':
      return <CalendarTile {...props} />;
    case 'media':
      return <MediaTile {...props} />;
    case 'note':
      return <NoteTile {...props} />;
    case 'photos':
      return <PhotosTile {...props} />;
    case 'battery':
      return <BatteryTile {...props} />;
    case 'steps':
      return <StepsTile {...props} />;
    case 'quick-toggles':
      return <QuickTogglesTile {...props} />;
    case 'app-group':
      return <AppGroupTile {...props} />;
    case 'single-app':
      return <SingleAppTile {...props} />;
    case 'third-party-widget':
      return <ThirdPartyWidgetTile {...props} />;
    case 'search-bar':
      return <SearchBarTile {...props} />;
    case 'newly-installed':
      return <NewlyInstalledTile {...props} />;
    case 'all-apps-box':
      return <AllAppsBoxTile {...props} />;
    default:
      return <NoteTile {...props} />;
  }
}
