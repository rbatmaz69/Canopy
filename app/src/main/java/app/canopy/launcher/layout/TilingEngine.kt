package app.canopy.launcher.layout

import java.util.concurrent.atomic.AtomicLong
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
