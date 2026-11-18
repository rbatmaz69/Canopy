package app.canopy.launcher.layout

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pure binary tiling tree layout engine for Canopy.
 * Ensures gap-free rectangular partitioning, minimum leaf dimensions,
 * velocity-projected snap points, 2D intersections, and immutable tree operations.
 */
object TilingEngine {
    const val GAP_USE_DP = 6f
    const val GAP_EDIT_DP = 12f
    const val EDGE_PADDING_DP = 8f
    const val MIN_TILE_SIZE = 56f
    val SNAP_POINTS = listOf(0.25f, 1f / 3f, 0.5f, 2f / 3f, 0.75f)

    private val idCounter = AtomicLong(1L)

    fun createNodeId(prefix: String = "node"): String {
        val seq = idCounter.incrementAndGet()
        return "$prefix-${System.currentTimeMillis().toString(36)}-$seq"
    }

    fun collectLeafIds(node: TreeNode): List<String> = when (node) {
        is LeafNode -> listOf(node.id)
        is SplitNode -> collectLeafIds(node.first) + collectLeafIds(node.second)
    }

    fun collectLeaves(node: TreeNode): List<LeafNode> = when (node) {
        is LeafNode -> listOf(node)
        is SplitNode -> collectLeaves(node.first) + collectLeaves(node.second)
    }

    fun findLeaf(node: TreeNode, leafId: String): LeafNode? = when (node) {
        is LeafNode -> if (node.id == leafId) node else null
        is SplitNode -> findLeaf(node.first, leafId) ?: findLeaf(node.second, leafId)
    }

    data class SubtreeMinSize(
        val minWidth: Float,
        val minHeight: Float,
    )

    data class RatioBounds(
        val minRatio: Float,
        val maxRatio: Float,
        val minFirstPx: Float,
        val minSecondPx: Float,
        val usableSpan: Float,
    )

    fun computeMinSizeSubtree(
        node: TreeNode,
        gap: Float,
        minLeafSize: Float = MIN_TILE_SIZE,
    ): SubtreeMinSize = when (node) {
        is LeafNode -> SubtreeMinSize(minWidth = minLeafSize, minHeight = minLeafSize)
        is SplitNode -> {
            val firstMin = computeMinSizeSubtree(node.first, gap, minLeafSize)
            val secondMin = computeMinSizeSubtree(node.second, gap, minLeafSize)
            if (node.direction == SplitDirection.HORIZONTAL) {
                SubtreeMinSize(
                    minWidth = firstMin.minWidth + gap + secondMin.minWidth,
                    minHeight = max(firstMin.minHeight, secondMin.minHeight),
                )
            } else {
                SubtreeMinSize(
                    minWidth = max(firstMin.minWidth, secondMin.minWidth),
                    minHeight = firstMin.minHeight + gap + secondMin.minHeight,
                )
            }
        }
    }

    fun computeRatioBounds(
        node: SplitNode,
        parentSpan: Float,
        gap: Float,
        minLeafSize: Float = MIN_TILE_SIZE,
    ): RatioBounds {
        val usableSpan = max(1f, parentSpan - gap)
        val firstMin = computeMinSizeSubtree(node.first, gap, minLeafSize)
        val secondMin = computeMinSizeSubtree(node.second, gap, minLeafSize)

        val minFirstPx =
            if (node.direction == SplitDirection.HORIZONTAL) firstMin.minWidth else firstMin.minHeight
        val minSecondPx =
            if (node.direction == SplitDirection.HORIZONTAL) secondMin.minWidth else secondMin.minHeight

        if (minFirstPx + minSecondPx >= usableSpan) {
            val mid = minFirstPx / (minFirstPx + minSecondPx)
            return RatioBounds(mid, mid, minFirstPx, minSecondPx, usableSpan)
        }

        val minRatio = max(0.05f, minFirstPx / usableSpan)
        val maxRatio = min(0.95f, 1f - minSecondPx / usableSpan)

        return RatioBounds(
            minRatio = min(minRatio, maxRatio),
            maxRatio = max(minRatio, maxRatio),
            minFirstPx = minFirstPx,
            minSecondPx = minSecondPx,
            usableSpan = usableSpan,
        )
    }

    /**
     * Clamps or rubberbands a dragged split ratio when pushed past the 56dp minimum size.
     */
    fun clampOrRubberbandRatio(
        rawRatio: Float,
        parentSpan: Float,
        minFirstPx: Float,
        minSecondPx: Float,
        rubberbandFactor: Float = 0.30f,
        gap: Float = GAP_EDIT_DP,
    ): Float {
        val usableSpan = max(1f, parentSpan - gap)
        if (minFirstPx + minSecondPx >= usableSpan) {
            return minFirstPx / (minFirstPx + minSecondPx)
        }

        val minRatio = minFirstPx / usableSpan
        val maxRatio = 1f - minSecondPx / usableSpan

        if (rawRatio < minRatio) {
            val excess = minRatio - rawRatio
            return max(0.02f, minRatio - excess * rubberbandFactor)
        }
        if (rawRatio > maxRatio) {
            val excess = rawRatio - maxRatio
            return min(0.98f, maxRatio + excess * rubberbandFactor)
        }
        return rawRatio
    }

    /**
     * Projects final position from pointer velocity and snaps to nearest valid
     * proportion among 1/4, 1/3, 1/2, 2/3, 3/4 within [minRatio, maxRatio].
     */
    fun projectAndSnapRatio(
        currentRatio: Float,
        velocityPxPerSec: Float,
        parentSpan: Float,
        minRatio: Float,
        maxRatio: Float,
        velocityFactor: Float = 0.12f,
    ): Float {
        val safeSpan = max(1f, parentSpan)
        val projected = currentRatio + (velocityPxPerSec * velocityFactor) / safeSpan
        val clampedProjected = projected.coerceIn(minRatio, maxRatio)

        val validSnaps = SNAP_POINTS
            .filter { it >= minRatio - 0.001f && it <= maxRatio + 0.001f }
            .map { it.coerceIn(minRatio, maxRatio) }

        if (validSnaps.isEmpty()) {
            return clampedProjected
        }

        var best = validSnaps[0]
        var bestDist = abs(clampedProjected - best)
        for (i in 1 until validSnaps.size) {
            val dist = abs(clampedProjected - validSnaps[i])
            if (dist < bestDist) {
                best = validSnaps[i]
                bestDist = dist
            }
        }
        return best
    }

    /**
     * Determines the tile size tier from dp dimensions with an 8dp (±4dp) hysteresis band
     * so tiles never flicker when dragged across thresholds.
     */
    fun computeSizeTier(
        width: Float,
        height: Float,
        prevTier: TileSizeTier? = null,
    ): TileSizeTier {
        val minDim = min(width, height)
        val area = width * height
        val h = 4f // +/- 4dp = 8dp total hysteresis band

        val xsLimit = if (prevTier == TileSizeTier.XS) 80f + h else 80f - h
        if (minDim < xsLimit) {
            return TileSizeTier.XS
        }

        val tickerHeightMax = if (prevTier == TileSizeTier.TICKER) 86f + h else 86f - h
        val tickerWidthMin = if (prevTier == TileSizeTier.TICKER) 175f - h else 175f + h
        if (height < tickerHeightMax && width >= tickerWidthMin) {
            return TileSizeTier.TICKER
        }

        val listWidthMax = if (prevTier == TileSizeTier.LIST) 100f + h else 100f - h
        val listHeightMin = if (prevTier == TileSizeTier.LIST) 185f - h else 185f + h
        if (width < listWidthMax && height >= listHeightMin) {
            return TileSizeTier.LIST
        }

        val sDimLimit = if (prevTier == TileSizeTier.S) 130f + h else 130f - h
        val sAreaLimit = if (prevTier == TileSizeTier.S) 20000f else 18000f
        if (minDim < sDimLimit || area < sAreaLimit) {
            return TileSizeTier.S
        }

        val mDimLimit = if (prevTier == TileSizeTier.M) 195f + h else 195f - h
        val mAreaLimit = if (prevTier == TileSizeTier.M) 40000f else 36500f
        if (minDim < mDimLimit || area < mAreaLimit) {
            return TileSizeTier.M
        }

        return TileSizeTier.L
    }

    /**
     * Recursively computes gap-free, non-overlapping leaf rectangles and divider handles.
     */
    fun computeLayout(root: TreeNode, bounds: RectFBounds, gap: Float): ComputedLayout {
        val leaves = mutableListOf<ComputedLeaf>()
        val dividers = mutableListOf<ComputedDivider>()

        fun walk(node: TreeNode, rect: RectFBounds) {
            when (node) {
                is LeafNode -> {
                    leaves.add(ComputedLeaf(id = node.id, tile = node.tile, rect = rect))
                }
                is SplitNode -> {
                    val parentSpan =
                        if (node.direction == SplitDirection.HORIZONTAL) rect.width else rect.height
                    val boundsInfo = computeRatioBounds(node, parentSpan, gap, MIN_TILE_SIZE)
                    val effectiveRatio = node.ratio.coerceIn(0.04f, 0.96f)
                    val usableSpan = boundsInfo.usableSpan
                    val firstSpan = usableSpan * effectiveRatio
                    val secondSpan = max(0f, usableSpan - firstSpan)

                    val firstRect: RectFBounds
                    val dividerRect: RectFBounds
                    val secondRect: RectFBounds

                    if (node.direction == SplitDirection.HORIZONTAL) {
                        firstRect = RectFBounds(rect.x, rect.y, firstSpan, rect.height)
                        dividerRect = RectFBounds(rect.x + firstSpan, rect.y, gap, rect.height)
                        secondRect = RectFBounds(rect.x + firstSpan + gap, rect.y, secondSpan, rect.height)
                    } else {
                        firstRect = RectFBounds(rect.x, rect.y, rect.width, firstSpan)
                        dividerRect = RectFBounds(rect.x, rect.y + firstSpan, rect.width, gap)
                        secondRect = RectFBounds(rect.x, rect.y + firstSpan + gap, rect.width, secondSpan)
                    }

                    dividers.add(
                        ComputedDivider(
                            id = node.id,
                            direction = node.direction,
                            ratio = effectiveRatio,
                            rect = dividerRect,
                            parentRect = rect,
                            minRatio = boundsInfo.minRatio,
                            maxRatio = boundsInfo.maxRatio,
                            minFirstPx = boundsInfo.minFirstPx,
                            minSecondPx = boundsInfo.minSecondPx,
                            firstLeafIds = collectLeafIds(node.first),
                            secondLeafIds = collectLeafIds(node.second),
                        )
                    )

                    walk(node.first, firstRect)
                    walk(node.second, secondRect)
                }
            }
        }

        walk(root, bounds)
        return ComputedLayout(leaves = leaves, dividers = dividers, intersections = emptyList())
    }
}
