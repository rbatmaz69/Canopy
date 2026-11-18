import { describe, expect, it } from 'vitest';
import {
  applyTemplate,
  clampOrRubberbandRatio,
  computeLayout,
  computeSizeTier,
  moveLeafToSplit,
  nudgeLeafSize,
  projectAndSnapRatio,
  removeLeaf,
  splitLeaf,
  swapLeaves,
  toggleSplitDirection,
  updateMultipleSplitRatios,
  updateSplitRatio,
} from './tree';
import { TileData, TreeNode } from './types';

const makeTile = (id: string, title = id): TileData => ({
  id,
  type: 'note',
  title,
});

const sampleTree: TreeNode = {
  kind: 'split',
  id: 'split-root',
  direction: 'vertical',
  ratio: 0.5,
  first: {
    kind: 'leaf',
    id: 'leaf-a',
    tile: makeTile('a', 'Alpha'),
  },
  second: {
    kind: 'split',
    id: 'split-sub',
    direction: 'horizontal',
    ratio: 0.5,
    first: {
      kind: 'leaf',
      id: 'leaf-b',
      tile: makeTile('b', 'Beta'),
    },
    second: {
      kind: 'leaf',
      id: 'leaf-c',
      tile: makeTile('c', 'Gamma'),
    },
  },
};

describe('Tiling Layout Tree Engine', () => {
  it('computes gap-free leaf rectangles with exact gap spacing', () => {
    const layout = computeLayout(sampleTree, { x: 8, y: 8, width: 374, height: 740 }, 6);
    expect(layout.leaves).toHaveLength(3);
    expect(layout.dividers).toHaveLength(2);

    const [a, b, c] = layout.leaves;
    expect(a.rect.x).toBe(8);
    expect(a.rect.y).toBe(8);
    expect(a.rect.width).toBe(374);
    expect(a.rect.height).toBeCloseTo((740 - 6) * 0.5, 5);

    expect(b.rect.y).toBeCloseTo(a.rect.y + a.rect.height + 6, 5);
    expect(b.rect.x + b.rect.width + 6).toBeCloseTo(c.rect.x, 5);
    expect(c.rect.x + c.rect.width).toBeCloseTo(8 + 374, 5);
  });

  it('splits a leaf along any edge and preserves existing tile', () => {
    const updated = splitLeaf(sampleTree, 'leaf-a', makeTile('d', 'Delta'), 'right', 0.25, 'leaf-d');
    const layout = computeLayout(updated, { x: 0, y: 0, width: 360, height: 600 }, 6);
    expect(layout.leaves).toHaveLength(4);
    const leafA = layout.leaves.find((l) => l.id === 'leaf-a');
    const leafD = layout.leaves.find((l) => l.id === 'leaf-d');
    expect(leafA).toBeDefined();
    expect(leafD).toBeDefined();
    expect(leafA!.rect.x).toBe(0);
    expect(leafD!.rect.x).toBeGreaterThan(leafA!.rect.x);
  });

  it('removes a leaf and collapses its parent split so sibling expands', () => {
    const res = removeLeaf(sampleTree, 'leaf-b');
    expect(res.removed?.id).toBe('leaf-b');
    expect(res.tree).not.toBeNull();
    const layout = computeLayout(res.tree!, { x: 0, y: 0, width: 360, height: 600 }, 6);
    expect(layout.leaves).toHaveLength(2);
    const gamma = layout.leaves.find((l) => l.id === 'leaf-c');
    expect(gamma!.rect.width).toBe(360);
  });

  it('swaps two leaves cleanly', () => {
    const swapped = swapLeaves(sampleTree, 'leaf-a', 'leaf-c');
    const layout = computeLayout(swapped, { x: 0, y: 0, width: 360, height: 600 }, 6);
    expect(layout.leaves[0].id).toBe('leaf-c');
    expect(layout.leaves[2].id).toBe('leaf-a');
  });

  it('moves a leaf onto another leaf edge while preserving its ID', () => {
    const moved = moveLeafToSplit(sampleTree, 'leaf-c', 'leaf-a', 'left', 0.33);
    const layout = computeLayout(moved, { x: 0, y: 0, width: 360, height: 600 }, 6);
    expect(layout.leaves).toHaveLength(3);
    const movedLeaf = layout.leaves.find((l) => l.id === 'leaf-c');
    expect(movedLeaf).toBeDefined();
    expect(movedLeaf!.rect.x).toBe(0);
    expect(movedLeaf!.rect.y).toBe(0);
  });

  it('flips split direction on toggleSplitDirection', () => {
    const toggled = toggleSplitDirection(sampleTree, 'split-root');
    expect(toggled.kind === 'split' && toggled.direction).toBe('horizontal');
  });

  it('updates split ratio immutably', () => {
    const updated = updateSplitRatio(sampleTree, 'split-sub', 0.75);
    const layout = computeLayout(updated, { x: 0, y: 0, width: 406, height: 600 }, 6);
    const b = layout.leaves.find((l) => l.id === 'leaf-b')!;
    const c = layout.leaves.find((l) => l.id === 'leaf-c')!;
    expect(b.rect.width).toBeCloseTo(300, 3);
    expect(c.rect.width).toBeCloseTo(100, 3);
  });

  it('enforces 56dp minimum size with 30% rubberband resistance', () => {
    // 300px usable span -> minRatio for 56px = 56/300 = 0.1866
    const clamped = clampOrRubberbandRatio(0.0866, 300 + 12, 56, 56, 0.3, 12);
    expect(clamped).toBeCloseTo(56 / 300 - 0.1 * 0.3, 4);
    expect(clamped).toBeGreaterThan(0.0866);
  });

  it('snaps projected position (pos + velocity * factor) to 1/4, 1/3, 1/2, 2/3, 3/4', () => {
    // Stationary near 0.48 -> snaps to 0.5
    expect(projectAndSnapRatio(0.48, 0, 400, 0.15, 0.85)).toBeCloseTo(0.5, 5);
    // Moving right fast from 0.48 -> projects past 0.66 -> snaps to 2/3 or 3/4
    const flung = projectAndSnapRatio(0.48, 800, 400, 0.15, 0.85);
    expect(flung).toBeCloseTo(0.75, 5);
    // Respects minRatio / maxRatio bounds
    const bounded = projectAndSnapRatio(0.2, -500, 400, 0.3, 0.7);
    expect(bounded).toBeCloseTo(1 / 3, 5);
  });

  it('applies templates preserving existing leaf IDs and adding fallback tiles if needed', () => {
    const fallback = [makeTile('f1'), makeTile('f2'), makeTile('f3'), makeTile('f4'), makeTile('f5')];
    const grid2x2 = applyTemplate(sampleTree, '2x2', fallback);
    const layout = computeLayout(grid2x2, { x: 0, y: 0, width: 374, height: 700 }, 6);
    expect(layout.leaves).toHaveLength(4);
    expect(layout.leaves.slice(0, 3).map((l) => l.id)).toEqual(['leaf-a', 'leaf-b', 'leaf-c']);
  });

  it('computes size tiers with 8dp hysteresis to avoid boundary flickering', () => {
    expect(computeSizeTier(74, 150)).toBe('XS');
    // Moving from XS (74) up to 82 stays XS because threshold becomes 84
    expect(computeSizeTier(82, 150, 'XS')).toBe('XS');
    // Moving from M down to 82 transitions out of XS (threshold is 76) -> TICKER or S
    expect(computeSizeTier(82, 150, 'S')).toBe('S');

    // Special cases
    expect(computeSizeTier(260, 78)).toBe('TICKER');
    expect(computeSizeTier(92, 240)).toBe('LIST');
    expect(computeSizeTier(280, 260)).toBe('L');
  });

  it('detects 2D intersections in 2x2 and T-junction layouts', () => {
    const fallback = [makeTile('f1'), makeTile('f2'), makeTile('f3'), makeTile('f4')];
    const grid2x2 = applyTemplate(sampleTree, '2x2', fallback);
    const layout2x2 = computeLayout(grid2x2, { x: 0, y: 0, width: 374, height: 700 }, 12);
    // In a 2x2 grid, both row X-splits meet the center Y-split at one central 2D crosshair
    expect(layout2x2.intersections).toHaveLength(1);
    expect(layout2x2.intersections[0].horizontalSplits).toHaveLength(2);
    expect(layout2x2.intersections[0].verticalSplits).toHaveLength(1);

    // Updating both axes atomically
    const ix = layout2x2.intersections[0];
    const updates: Record<string, number> = {
      [ix.verticalSplits[0].id]: 0.35,
    };
    ix.horizontalSplits.forEach((h) => {
      updates[h.id] = 0.65;
    });
    const morphed = updateMultipleSplitRatios(grid2x2, updates);
    const morphedLayout = computeLayout(morphed, { x: 0, y: 0, width: 374, height: 700 }, 12);
    expect(morphedLayout.intersections[0].x).toBeGreaterThan(220);
    expect(morphedLayout.intersections[0].y).toBeLessThan(260);
  });

  it('nudges a leaf larger or smaller via nudgeLeafSize', () => {
    const grown = nudgeLeafSize(sampleTree, 'leaf-b', 0.14);
    const layoutBefore = computeLayout(sampleTree, { x: 0, y: 0, width: 360, height: 600 }, 6);
    const layoutAfter = computeLayout(grown, { x: 0, y: 0, width: 360, height: 600 }, 6);
    const bBefore = layoutBefore.leaves.find((l) => l.id === 'leaf-b')!;
    const bAfter = layoutAfter.leaves.find((l) => l.id === 'leaf-b')!;
    expect(bAfter.rect.width).toBeGreaterThan(bBefore.rect.width);
  });
});
