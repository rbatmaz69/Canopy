import {
  ComputedDivider,
  ComputedIntersection,
  ComputedLayout,
  ComputedLeaf,
  DropZone,
  LeafNode,
  Rect,
  SplitNode,
  TemplateKind,
  TileData,
  TileSizeTier,
  TreeNode,
} from './types';

export const MIN_TILE_SIZE = 56;
export const SNAP_POINTS = [0.25, 1 / 3, 0.5, 2 / 3, 0.75] as const;

let idCounter = 1;
export function createNodeId(prefix = 'node'): string {
  idCounter += 1;
  return `${prefix}-${Date.now().toString(36)}-${idCounter}`;
}

export function collectLeafIds(node: TreeNode): string[] {
  if (node.kind === 'leaf') return [node.id];
  return [...collectLeafIds(node.first), ...collectLeafIds(node.second)];
}

export function collectLeaves(node: TreeNode): LeafNode[] {
  if (node.kind === 'leaf') return [node];
  return [...collectLeaves(node.first), ...collectLeaves(node.second)];
}

export function findLeaf(node: TreeNode, leafId: string): LeafNode | null {
  if (node.kind === 'leaf') return node.id === leafId ? node : null;
  return findLeaf(node.first, leafId) ?? findLeaf(node.second, leafId);
}

/**
 * Computes minimum width and height required by a subtree so every leaf
 * stays at least `minLeafSize` dp in both dimensions.
 */
export function computeMinSizeSubtree(
  node: TreeNode,
  gap: number,
  minLeafSize = MIN_TILE_SIZE
): { minWidth: number; minHeight: number } {
  if (node.kind === 'leaf') {
    return { minWidth: minLeafSize, minHeight: minLeafSize };
  }

  const firstMin = computeMinSizeSubtree(node.first, gap, minLeafSize);
  const secondMin = computeMinSizeSubtree(node.second, gap, minLeafSize);

  if (node.direction === 'horizontal') {
    return {
      minWidth: firstMin.minWidth + gap + secondMin.minWidth,
      minHeight: Math.max(firstMin.minHeight, secondMin.minHeight),
    };
  }

  return {
    minWidth: Math.max(firstMin.minWidth, secondMin.minWidth),
    minHeight: firstMin.minHeight + gap + secondMin.minHeight,
  };
}

/**
 * Computes valid [minRatio, maxRatio] bounds for a split node given available span.
 */
export function computeRatioBounds(
  node: SplitNode,
  parentSpan: number,
  gap: number,
  minLeafSize = MIN_TILE_SIZE
): { minRatio: number; maxRatio: number; minFirstPx: number; minSecondPx: number; usableSpan: number } {
  const usableSpan = Math.max(1, parentSpan - gap);
  const firstMin = computeMinSizeSubtree(node.first, gap, minLeafSize);
  const secondMin = computeMinSizeSubtree(node.second, gap, minLeafSize);

  const minFirstPx = node.direction === 'horizontal' ? firstMin.minWidth : firstMin.minHeight;
  const minSecondPx = node.direction === 'horizontal' ? secondMin.minWidth : secondMin.minHeight;

  if (minFirstPx + minSecondPx >= usableSpan) {
    const mid = minFirstPx / (minFirstPx + minSecondPx);
    return { minRatio: mid, maxRatio: mid, minFirstPx, minSecondPx, usableSpan };
  }

  const minRatio = Math.max(0.05, minFirstPx / usableSpan);
  const maxRatio = Math.min(0.95, 1 - minSecondPx / usableSpan);

  return {
    minRatio: Math.min(minRatio, maxRatio),
    maxRatio: Math.max(minRatio, maxRatio),
    minFirstPx,
    minSecondPx,
    usableSpan,
  };
}

/**
 * Clamps or rubberbands a dragged split ratio when pushed past the 56dp minimum size.
 */
export function clampOrRubberbandRatio(
  rawRatio: number,
  parentSpan: number,
  minFirstPx: number,
  minSecondPx: number,
  rubberbandFactor = 0.3,
  gap = 12
): number {
  const usableSpan = Math.max(1, parentSpan - gap);
  if (minFirstPx + minSecondPx >= usableSpan) {
    return minFirstPx / (minFirstPx + minSecondPx);
  }

  const minRatio = minFirstPx / usableSpan;
  const maxRatio = 1 - minSecondPx / usableSpan;

  if (rawRatio < minRatio) {
    const excess = minRatio - rawRatio;
    return Math.max(0.02, minRatio - excess * rubberbandFactor);
  }
  if (rawRatio > maxRatio) {
    const excess = rawRatio - maxRatio;
    return Math.min(0.98, maxRatio + excess * rubberbandFactor);
  }
  return rawRatio;
}

/**
 * Projects final position from pointer velocity and snaps to nearest valid
 * proportion among 1/4, 1/3, 1/2, 2/3, 3/4 within [minRatio, maxRatio].
 */
export function projectAndSnapRatio(
  currentRatio: number,
  velocityPxPerSec: number,
  parentSpan: number,
  minRatio: number,
  maxRatio: number,
  velocityFactor = 0.12
): number {
  const safeSpan = Math.max(1, parentSpan);
  const projected = currentRatio + (velocityPxPerSec * velocityFactor) / safeSpan;
  const clampedProjected = Math.min(maxRatio, Math.max(minRatio, projected));

  const validSnaps = SNAP_POINTS.filter((p) => p >= minRatio - 0.001 && p <= maxRatio + 0.001).map((p) =>
    Math.min(maxRatio, Math.max(minRatio, p))
  );

  if (validSnaps.length === 0) {
    return clampedProjected;
  }

  let best = validSnaps[0];
  let bestDist = Math.abs(clampedProjected - best);

  for (let i = 1; i < validSnaps.length; i += 1) {
    const dist = Math.abs(clampedProjected - validSnaps[i]);
    if (dist < bestDist) {
      best = validSnaps[i];
      bestDist = dist;
    }
  }

  return best;
}

/**
 * Recursively computes leaf rectangles and divider handles for a tiling tree.
 */
export function computeLayout(root: TreeNode, bounds: Rect, gap: number): ComputedLayout {
  const leaves: ComputedLeaf[] = [];
  const dividers: ComputedDivider[] = [];

  function walk(node: TreeNode, rect: Rect) {
    if (node.kind === 'leaf') {
      leaves.push({
        id: node.id,
        tile: node.tile,
        rect,
      });
      return;
    }

    const parentSpan = node.direction === 'horizontal' ? rect.width : rect.height;
    const { minRatio, maxRatio, minFirstPx, minSecondPx, usableSpan } = computeRatioBounds(
      node,
      parentSpan,
      gap,
      MIN_TILE_SIZE
    );
    const effectiveRatio = Math.min(0.96, Math.max(0.04, node.ratio));

    const firstSpan = usableSpan * effectiveRatio;
    const secondSpan = Math.max(0, usableSpan - firstSpan);

    if (node.direction === 'horizontal') {
      const firstRect: Rect = {
        x: rect.x,
        y: rect.y,
        width: firstSpan,
        height: rect.height,
      };
      const dividerRect: Rect = {
        x: rect.x + firstSpan,
        y: rect.y,
        width: gap,
        height: rect.height,
      };
      const secondRect: Rect = {
        x: rect.x + firstSpan + gap,
        y: rect.y,
        width: secondSpan,
        height: rect.height,
      };

      dividers.push({
        id: node.id,
        direction: node.direction,
        ratio: effectiveRatio,
        rect: dividerRect,
        parentRect: rect,
        minRatio,
        maxRatio,
        minFirstPx,
        minSecondPx,
        firstLeafIds: collectLeafIds(node.first),
        secondLeafIds: collectLeafIds(node.second),
      });

      walk(node.first, firstRect);
      walk(node.second, secondRect);
    } else {
      const firstRect: Rect = {
        x: rect.x,
        y: rect.y,
        width: rect.width,
        height: firstSpan,
      };
      const dividerRect: Rect = {
        x: rect.x,
        y: rect.y + firstSpan,
        width: rect.width,
        height: gap,
      };
      const secondRect: Rect = {
        x: rect.x,
        y: rect.y + firstSpan + gap,
        width: rect.width,
        height: secondSpan,
      };

      dividers.push({
        id: node.id,
        direction: node.direction,
        ratio: effectiveRatio,
        rect: dividerRect,
        parentRect: rect,
        minRatio,
        maxRatio,
        minFirstPx,
        minSecondPx,
        firstLeafIds: collectLeafIds(node.first),
        secondLeafIds: collectLeafIds(node.second),
      });

      walk(node.first, firstRect);
      walk(node.second, secondRect);
    }
  }

  walk(root, bounds);

  // Compute 2D intersection handles where horizontal (vertical-line) and vertical (horizontal-line) dividers meet
  const intersections: ComputedIntersection[] = [];
  const hSplits = dividers.filter((d) => d.direction === 'horizontal');
  const vSplits = dividers.filter((d) => d.direction === 'vertical');
  const tol = gap + 6;

  for (const h of hSplits) {
    const hx = h.rect.x + h.rect.width / 2;
    const hyMin = h.rect.y - tol;
    const hyMax = h.rect.y + h.rect.height + tol;

    for (const v of vSplits) {
      const vy = v.rect.y + v.rect.height / 2;
      const vxMin = v.rect.x - tol;
      const vxMax = v.rect.x + v.rect.width + tol;

      if (hx >= vxMin && hx <= vxMax && vy >= hyMin && vy <= hyMax) {
        const existing = intersections.find(
          (item) => Math.abs(item.x - hx) <= 16 && Math.abs(item.y - vy) <= 16
        );
        if (existing) {
          if (!existing.horizontalSplits.some((s) => s.id === h.id)) {
            existing.horizontalSplits.push(h);
          }
          if (!existing.verticalSplits.some((s) => s.id === v.id)) {
            existing.verticalSplits.push(v);
          }
          existing.x =
            existing.horizontalSplits.reduce((acc, s) => acc + s.rect.x + s.rect.width / 2, 0) /
            existing.horizontalSplits.length;
          existing.y =
            existing.verticalSplits.reduce((acc, s) => acc + s.rect.y + s.rect.height / 2, 0) /
            existing.verticalSplits.length;
        } else {
          intersections.push({
            id: `ix-${h.id}-${v.id}`,
            x: hx,
            y: vy,
            horizontalSplits: [h],
            verticalSplits: [v],
          });
        }
      }
    }
  }

  return { leaves, dividers, intersections };
}

export function updateSplitRatio(root: TreeNode, splitId: string, newRatio: number): TreeNode {
  if (root.kind === 'leaf') return root;
  if (root.id === splitId) {
    return { ...root, ratio: newRatio };
  }
  return {
    ...root,
    first: updateSplitRatio(root.first, splitId, newRatio),
    second: updateSplitRatio(root.second, splitId, newRatio),
  };
}

export function updateMultipleSplitRatios(
  root: TreeNode,
  updates: Record<string, number>
): TreeNode {
  if (root.kind === 'leaf') return root;
  const nextRatio = updates[root.id];
  return {
    ...root,
    ratio: typeof nextRatio === 'number' ? nextRatio : root.ratio,
    first: updateMultipleSplitRatios(root.first, updates),
    second: updateMultipleSplitRatios(root.second, updates),
  };
}

export function nudgeLeafSize(root: TreeNode, leafId: string, delta: number): TreeNode {
  if (root.kind === 'leaf') return root;
  if (root.first.kind === 'leaf' && root.first.id === leafId) {
    return {
      ...root,
      ratio: Math.min(0.8, Math.max(0.2, root.ratio + delta)),
    };
  }
  if (root.second.kind === 'leaf' && root.second.id === leafId) {
    return {
      ...root,
      ratio: Math.min(0.8, Math.max(0.2, root.ratio - delta)),
    };
  }
  return {
    ...root,
    first: nudgeLeafSize(root.first, leafId, delta),
    second: nudgeLeafSize(root.second, leafId, delta),
  };
}

export function toggleSplitDirection(root: TreeNode, splitId: string): TreeNode {
  if (root.kind === 'leaf') return root;
  if (root.id === splitId) {
    return {
      ...root,
      direction: root.direction === 'horizontal' ? 'vertical' : 'horizontal',
    };
  }
  return {
    ...root,
    first: toggleSplitDirection(root.first, splitId),
    second: toggleSplitDirection(root.second, splitId),
  };
}

export function updateLeafTile(root: TreeNode, leafId: string, updater: (tile: TileData) => TileData): TreeNode {
  if (root.kind === 'leaf') {
    if (root.id !== leafId) return root;
    return { ...root, tile: updater(root.tile) };
  }
  return {
    ...root,
    first: updateLeafTile(root.first, leafId, updater),
    second: updateLeafTile(root.second, leafId, updater),
  };
}

export function splitLeaf(
  root: TreeNode,
  targetLeafId: string,
  newTile: TileData,
  dropZone: Exclude<DropZone, 'center'>,
  ratio = 0.5,
  newLeafId?: string
): TreeNode {
  if (root.kind === 'leaf') {
    if (root.id !== targetLeafId) return root;
    const insertedLeaf: LeafNode = {
      kind: 'leaf',
      id: newLeafId ?? createNodeId('leaf'),
      tile: newTile,
    };
    const direction = dropZone === 'left' || dropZone === 'right' ? 'horizontal' : 'vertical';
    const isNewFirst = dropZone === 'left' || dropZone === 'top';
    return {
      kind: 'split',
      id: createNodeId('split'),
      direction,
      ratio,
      first: isNewFirst ? insertedLeaf : root,
      second: isNewFirst ? root : insertedLeaf,
    };
  }

  return {
    ...root,
    first: splitLeaf(root.first, targetLeafId, newTile, dropZone, ratio, newLeafId),
    second: splitLeaf(root.second, targetLeafId, newTile, dropZone, ratio, newLeafId),
  };
}

export function removeLeaf(
  root: TreeNode,
  targetLeafId: string
): { tree: TreeNode | null; removed?: LeafNode } {
  if (root.kind === 'leaf') {
    if (root.id === targetLeafId) {
      return { tree: null, removed: root };
    }
    return { tree: root };
  }

  if (root.first.kind === 'leaf' && root.first.id === targetLeafId) {
    return { tree: root.second, removed: root.first };
  }
  if (root.second.kind === 'leaf' && root.second.id === targetLeafId) {
    return { tree: root.first, removed: root.second };
  }

  const leftRes = removeLeaf(root.first, targetLeafId);
  if (leftRes.removed) {
    return {
      tree: leftRes.tree ? { ...root, first: leftRes.tree } : root.second,
      removed: leftRes.removed,
    };
  }

  const rightRes = removeLeaf(root.second, targetLeafId);
  if (rightRes.removed) {
    return {
      tree: rightRes.tree ? { ...root, second: rightRes.tree } : root.first,
      removed: rightRes.removed,
    };
  }

  return { tree: root };
}

export function swapLeaves(root: TreeNode, leafIdA: string, leafIdB: string): TreeNode {
  if (leafIdA === leafIdB) return root;
  const leafA = findLeaf(root, leafIdA);
  const leafB = findLeaf(root, leafIdB);
  if (!leafA || !leafB) return root;

  function replace(node: TreeNode): TreeNode {
    if (node.kind === 'leaf') {
      if (node.id === leafIdA) return { ...leafB! };
      if (node.id === leafIdB) return { ...leafA! };
      return node;
    }
    return {
      ...node,
      first: replace(node.first),
      second: replace(node.second),
    };
  }

  return replace(root);
}

/**
 * Moves an existing leaf onto a target leaf's edge, removing it from its old slot
 * and splitting the target leaf while preserving the moved leaf's ID for smooth morphing.
 */
export function moveLeafToSplit(
  root: TreeNode,
  sourceLeafId: string,
  targetLeafId: string,
  dropZone: DropZone,
  ratio = 0.5
): TreeNode {
  if (sourceLeafId === targetLeafId) return root;
  if (dropZone === 'center') {
    return swapLeaves(root, sourceLeafId, targetLeafId);
  }
  const removedRes = removeLeaf(root, sourceLeafId);
  if (!removedRes.removed || !removedRes.tree) {
    return root;
  }
  return splitLeaf(
    removedRes.tree,
    targetLeafId,
    removedRes.removed.tile,
    dropZone,
    ratio,
    removedRes.removed.id
  );
}

/**
 * Transforms a page's tree into one of 3 preset layout patterns while keeping
 * existing leaf IDs so Framer Motion smoothly morphs tiles into their new positions.
 */
export function applyTemplate(
  root: TreeNode,
  template: TemplateKind,
  fallbackTiles: TileData[]
): TreeNode {
  const existing = collectLeaves(root);
  const getLeaf = (idx: number): LeafNode => {
    if (idx < existing.length) return existing[idx];
    const fallback = fallbackTiles[idx % fallbackTiles.length];
    return {
      kind: 'leaf',
      id: createNodeId('leaf'),
      tile: { ...fallback, id: createNodeId('tile') },
    };
  };

  if (template === '2x2') {
    return {
      kind: 'split',
      id: createNodeId('split'),
      direction: 'vertical',
      ratio: 0.5,
      first: {
        kind: 'split',
        id: createNodeId('split'),
        direction: 'horizontal',
        ratio: 0.5,
        first: getLeaf(0),
        second: getLeaf(1),
      },
      second: {
        kind: 'split',
        id: createNodeId('split'),
        direction: 'horizontal',
        ratio: 0.5,
        first: getLeaf(2),
        second: getLeaf(3),
      },
    };
  }

  if (template === 'big-plus-3') {
    return {
      kind: 'split',
      id: createNodeId('split'),
      direction: 'vertical',
      ratio: 0.58,
      first: getLeaf(0),
      second: {
        kind: 'split',
        id: createNodeId('split'),
        direction: 'horizontal',
        ratio: 1 / 3,
        first: getLeaf(1),
        second: {
          kind: 'split',
          id: createNodeId('split'),
          direction: 'horizontal',
          ratio: 0.5,
          first: getLeaf(2),
          second: getLeaf(3),
        },
      },
    };
  }

  // 'col-plus-grid': tall column on left + 2x2 grid on right (or 3 stacked on right)
  return {
    kind: 'split',
    id: createNodeId('split'),
    direction: 'horizontal',
    ratio: 0.42,
    first: getLeaf(0),
    second: {
      kind: 'split',
      id: createNodeId('split'),
      direction: 'vertical',
      ratio: 0.5,
      first: {
        kind: 'split',
        id: createNodeId('split'),
        direction: 'horizontal',
        ratio: 0.5,
        first: getLeaf(1),
        second: getLeaf(2),
      },
      second: {
        kind: 'split',
        id: createNodeId('split'),
        direction: 'horizontal',
        ratio: 0.5,
        first: getLeaf(3),
        second: getLeaf(4),
      },
    },
  };
}

/**
 * Determines the tile size tier from pixel dimensions with ~8dp hysteresis
 * so tiles never flicker when dragged across thresholds.
 */
export function computeSizeTier(
  width: number,
  height: number,
  prevTier?: TileSizeTier
): TileSizeTier {
  const minDim = Math.min(width, height);
  const area = width * height;
  const H = 4; // +/- 4dp = 8dp hysteresis band

  const xsLimit = prevTier === 'XS' ? 80 + H : 80 - H;
  if (minDim < xsLimit) {
    return 'XS';
  }

  const tickerHeightMax = prevTier === 'TICKER' ? 86 + H : 86 - H;
  const tickerWidthMin = prevTier === 'TICKER' ? 175 - H : 175 + H;
  if (height < tickerHeightMax && width >= tickerWidthMin) {
    return 'TICKER';
  }

  const listWidthMax = prevTier === 'LIST' ? 100 + H : 100 - H;
  const listHeightMin = prevTier === 'LIST' ? 185 - H : 185 + H;
  if (width < listWidthMax && height >= listHeightMin) {
    return 'LIST';
  }

  const sDimLimit = prevTier === 'S' ? 130 + H : 130 - H;
  const sAreaLimit = prevTier === 'S' ? 20000 : 18000;
  if (minDim < sDimLimit || area < sAreaLimit) {
    return 'S';
  }

  const mDimLimit = prevTier === 'M' ? 195 + H : 195 - H;
  const mAreaLimit = prevTier === 'M' ? 40000 : 36500;
  if (minDim < mDimLimit || area < mAreaLimit) {
    return 'M';
  }

  return 'L';
}

export function determineDropZone(
  relX: number,
  relY: number,
  width: number,
  height: number
): DropZone {
  const nx = relX / Math.max(1, width);
  const ny = relY / Math.max(1, height);

  if (nx >= 0.27 && nx <= 0.73 && ny >= 0.27 && ny <= 0.73) {
    return 'center';
  }

  const distLeft = nx;
  const distRight = 1 - nx;
  const distTop = ny;
  const distBottom = 1 - ny;
  const minDist = Math.min(distLeft, distRight, distTop, distBottom);

  if (minDist === distLeft) return 'left';
  if (minDist === distRight) return 'right';
  if (minDist === distTop) return 'top';
  return 'bottom';
}
