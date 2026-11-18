export type SplitDirection = 'horizontal' | 'vertical';

export type TileSizeTier = 'XS' | 'S' | 'M' | 'L' | 'TICKER' | 'LIST';

export type DropZone = 'center' | 'left' | 'right' | 'top' | 'bottom';

export type TemplateKind = 'big-plus-3' | '2x2' | 'col-plus-grid';

export type TileType =
  | 'greeting'
  | 'temp-curve'
  | 'calendar-timeline'
  | 'reminders'
  | 'battery-steps'
  | 'clock'
  | 'weather'
  | 'calendar'
  | 'media'
  | 'note'
  | 'photos'
  | 'battery'
  | 'steps'
  | 'quick-toggles'
  | 'app-group'
  | 'single-app'
  | 'third-party-widget'
  | 'search-bar'
  | 'newly-installed'
  | 'all-apps-box';

export interface ReminderItem {
  id: string;
  text: string;
  done: boolean;
  time?: string;
}

export interface TileData {
  id: string;
  type: TileType;
  title: string;
  subtitle?: string;
  accentHue?: number;
  // App group
  appIds?: string[];
  // Single app
  appId?: string;
  // Third-party widget
  widgetKind?: 'podcast' | 'fitness';
  widgetFixedWidth?: number;
  widgetFixedHeight?: number;
  widgetMutedColor?: string;
  // Media
  mediaLarge?: boolean;
  // Reminders / Note
  reminders?: ReminderItem[];
  noteLines?: string[];
  // Newly installed
  newAppIds?: string[];
  // Quick toggles preset
  togglePreset?: 'system' | 'today' | 'media' | 'tools';
}

export interface LeafNode {
  kind: 'leaf';
  id: string;
  tile: TileData;
}

export interface SplitNode {
  kind: 'split';
  id: string;
  direction: SplitDirection;
  /** Ratio of first child (0..1) relative to available span excluding gap */
  ratio: number;
  first: TreeNode;
  second: TreeNode;
}

export type TreeNode = LeafNode | SplitNode;

export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface ComputedLeaf {
  id: string;
  tile: TileData;
  rect: Rect;
}

export interface ComputedDivider {
  id: string;
  direction: SplitDirection;
  ratio: number;
  rect: Rect;
  parentRect: Rect;
  minRatio: number;
  maxRatio: number;
  minFirstPx: number;
  minSecondPx: number;
  firstLeafIds: string[];
  secondLeafIds: string[];
}

export interface ComputedIntersection {
  id: string;
  x: number;
  y: number;
  horizontalSplits: ComputedDivider[];
  verticalSplits: ComputedDivider[];
}

export interface ComputedLayout {
  leaves: ComputedLeaf[];
  dividers: ComputedDivider[];
  intersections: ComputedIntersection[];
}

export interface LauncherPage {
  id: string;
  title: string;
  kind: 'tiles' | 'all-apps';
  root: TreeNode;
}
