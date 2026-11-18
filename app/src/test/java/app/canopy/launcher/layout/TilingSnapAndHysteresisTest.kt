package app.canopy.launcher.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TilingSnapAndHysteresisTest {
    private fun leaf(id: String) = LeafNode(id, TileData(id = id, type = TileType.NOTE, title = id))

    @Test
    fun `computes recursive minimum subtree sizes accounting for gaps`() {
        val nested = SplitNode(
            id = "s-root",
            direction = SplitDirection.HORIZONTAL,
            ratio = 0.5f,
            first = SplitNode(
                id = "s-left",
                direction = SplitDirection.HORIZONTAL,
                ratio = 0.5f,
                first = leaf("a"),
                second = leaf("b"),
            ),
            second = SplitNode(
                id = "s-right",
                direction = SplitDirection.VERTICAL,
                ratio = 0.5f,
                first = leaf("c"),
                second = leaf("d"),
            ),
        )

        val minSize = TilingEngine.computeMinSizeSubtree(nested, gap = 12f)
        // Left horizontal split needs 56 + 12 + 56 = 124w, 56h
        // Right vertical split needs 56w, 56 + 12 + 56 = 124h
        // Root horizontal split needs 124 + 12 + 56 = 192w, max(56, 124) = 124h
        assertEquals(192f, minSize.minWidth, 1e-4f)
        assertEquals(124f, minSize.minHeight, 1e-4f)

        val bounds = TilingEngine.computeRatioBounds(nested, parentSpan = 374f, gap = 12f)
        val usable = 374f - 12f
        assertEquals(124f, bounds.minFirstPx, 1e-4f)
        assertEquals(56f, bounds.minSecondPx, 1e-4f)
        assertEquals(124f / usable, bounds.minRatio, 1e-4f)
        assertEquals(1f - 56f / usable, bounds.maxRatio, 1e-4f)
    }

    @Test
    fun `enforces 56dp minimum size with 30 percent rubberband resistance on both ends`() {
        val clampedLow = TilingEngine.clampOrRubberbandRatio(
            rawRatio = 0.0866f,
            parentSpan = 300f + 12f,
            minFirstPx = 56f,
            minSecondPx = 56f,
            rubberbandFactor = 0.30f,
            gap = 12f,
        )
        assertEquals(56f / 300f - 0.1f * 0.30f, clampedLow, 1e-3f)
        assertTrue(clampedLow > 0.0866f)

        val maxRatio = 1f - 56f / 300f
        val rawHigh = maxRatio + 0.10f
        val clampedHigh = TilingEngine.clampOrRubberbandRatio(
            rawRatio = rawHigh,
            parentSpan = 300f + 12f,
            minFirstPx = 56f,
            minSecondPx = 56f,
            rubberbandFactor = 0.30f,
            gap = 12f,
        )
        assertEquals(maxRatio + 0.10f * 0.30f, clampedHigh, 1e-3f)
        assertTrue(clampedHigh < rawHigh)
    }

    @Test
    fun `rubberband stays within valid safe floor and ceiling`() {
        val extremeLow = TilingEngine.clampOrRubberbandRatio(-10f, 300f, 56f, 56f, 0.30f, 12f)
        val extremeHigh = TilingEngine.clampOrRubberbandRatio(10f, 300f, 56f, 56f, 0.30f, 12f)
        assertEquals(0.02f, extremeLow, 1e-4f)
        assertEquals(0.98f, extremeHigh, 1e-4f)
    }

    @Test
    fun `snaps projected position to 1_4, 1_3, 1_2, 2_3, 3_4 respecting bounds`() {
        assertEquals(0.5f, TilingEngine.projectAndSnapRatio(0.48f, 0f, 400f, 0.15f, 0.85f), 1e-5f)

        // Fast fling right from 0.48 -> projects past 2/3 and snaps to 3/4
        val flung = TilingEngine.projectAndSnapRatio(0.48f, 800f, 400f, 0.15f, 0.85f)
        assertEquals(0.75f, flung, 1e-5f)

        // Fling left clamped to [0.3, 0.7] -> nearest valid snap is 1/3
        val bounded = TilingEngine.projectAndSnapRatio(0.2f, -500f, 400f, 0.3f, 0.7f)
        assertEquals(1f / 3f, bounded, 1e-5f)

        // Zero valid snap points inside tight range -> returns clamped projection
        val tight = TilingEngine.projectAndSnapRatio(0.42f, 0f, 400f, 0.40f, 0.45f)
        assertEquals(0.42f, tight, 1e-5f)
    }

    @Test
    fun `computes size tiers and prevents boundary flicker via 8dp hysteresis corridor`() {
        assertEquals(TileSizeTier.XS, TilingEngine.computeSizeTier(74f, 150f))
        // Moving from XS (74) up to 82 stays XS because threshold is 80 + 4 = 84
        assertEquals(TileSizeTier.XS, TilingEngine.computeSizeTier(82f, 150f, TileSizeTier.XS))
        // Moving past 84 transitions out of XS into S
        assertEquals(TileSizeTier.S, TilingEngine.computeSizeTier(85f, 150f, TileSizeTier.XS))
        // Moving from S down to 82 stays S because XS entry threshold is 80 - 4 = 76
        assertEquals(TileSizeTier.S, TilingEngine.computeSizeTier(82f, 150f, TileSizeTier.S))

        // Special layout tiers: TICKER and LIST
        assertEquals(TileSizeTier.TICKER, TilingEngine.computeSizeTier(260f, 78f))
        // Hysteresis keeps TICKER until height >= 90 or width < 171
        assertEquals(TileSizeTier.TICKER, TilingEngine.computeSizeTier(172f, 89f, TileSizeTier.TICKER))

        assertEquals(TileSizeTier.LIST, TilingEngine.computeSizeTier(92f, 240f))
        assertEquals(TileSizeTier.LIST, TilingEngine.computeSizeTier(103f, 182f, TileSizeTier.LIST))

        // M and L tiers
        assertEquals(TileSizeTier.M, TilingEngine.computeSizeTier(180f, 190f))
        assertEquals(TileSizeTier.L, TilingEngine.computeSizeTier(280f, 260f))

        // S <-> M area hysteresis (18000 entry vs 20000 stay)
        // 136 x 140 = 19040 area, minDim = 136 (> 134)
        assertEquals(TileSizeTier.S, TilingEngine.computeSizeTier(136f, 140f, TileSizeTier.S))
        assertEquals(TileSizeTier.M, TilingEngine.computeSizeTier(136f, 140f, TileSizeTier.M))

        // M <-> L area hysteresis (36500 entry vs 40000 stay)
        // 199 x 192 = 38208 area, minDim = 192 (> 191, < 199)
        assertEquals(TileSizeTier.M, TilingEngine.computeSizeTier(199f, 192f, TileSizeTier.M))
        assertEquals(TileSizeTier.L, TilingEngine.computeSizeTier(200f, 195f, TileSizeTier.L))
    }
}
