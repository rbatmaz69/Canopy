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

        // Compute 2D intersection handles where horizontal (vertical-line) and vertical (horizontal-line) dividers meet
        data class MutableIntersection(
            val id: String,
            var x: Float,
            var y: Float,
            val horizontalSplits: MutableList<ComputedDivider>,
            val verticalSplits: MutableList<ComputedDivider>,
        )

        val intersections = mutableListOf<MutableIntersection>()
        val hSplits = dividers.filter { it.direction == SplitDirection.HORIZONTAL }
        val vSplits = dividers.filter { it.direction == SplitDirection.VERTICAL }
        val tol = gap + 6f

        for (h in hSplits) {
            val hx = h.rect.x + h.rect.width / 2f
            val hyMin = h.rect.y - tol
            val hyMax = h.rect.y + h.rect.height + tol

            for (v in vSplits) {
                val vy = v.rect.y + v.rect.height / 2f
                val vxMin = v.rect.x - tol
                val vxMax = v.rect.x + v.rect.width + tol

                if (hx in vxMin..vxMax && vy in hyMin..hyMax) {
                    val existing = intersections.find { abs(it.x - hx) <= 16f && abs(it.y - vy) <= 16f }
                    if (existing != null) {
                        if (existing.horizontalSplits.none { it.id == h.id }) {
                            existing.horizontalSplits.add(h)
                        }
                        if (existing.verticalSplits.none { it.id == v.id }) {
                            existing.verticalSplits.add(v)
                        }
                        existing.x = existing.horizontalSplits
                            .map { it.rect.x + it.rect.width / 2f }
                            .average()
                            .toFloat()
                        existing.y = existing.verticalSplits
                            .map { it.rect.y + it.rect.height / 2f }
                            .average()
                            .toFloat()
                    } else {
                        intersections.add(
                            MutableIntersection(
                                id = "ix-${h.id}-${v.id}",
                                x = hx,
                                y = vy,
                                horizontalSplits = mutableListOf(h),
                                verticalSplits = mutableListOf(v),
                            )
                        )
                    }
                }
            }
        }

        return ComputedLayout(
            leaves = leaves,
            dividers = dividers,
            intersections = intersections.map {
                ComputedIntersection(
                    id = it.id,
                    x = it.x,
                    y = it.y,
                    horizontalSplits = it.horizontalSplits.toList(),
                    verticalSplits = it.verticalSplits.toList(),
                )
            },
        )
    }

    fun updateSplitRatio(root: TreeNode, splitId: String, newRatio: Float): TreeNode = when (root) {
        is LeafNode -> root
        is SplitNode -> {
            if (root.id == splitId) {
                root.copy(ratio = newRatio)
            } else {
                root.copy(
                    first = updateSplitRatio(root.first, splitId, newRatio),
                    second = updateSplitRatio(root.second, splitId, newRatio),
                )
            }
        }
    }

    fun updateMultipleSplitRatios(root: TreeNode, updates: Map<String, Float>): TreeNode = when (root) {
        is LeafNode -> root
        is SplitNode -> root.copy(
            ratio = updates[root.id] ?: root.ratio,
            first = updateMultipleSplitRatios(root.first, updates),
            second = updateMultipleSplitRatios(root.second, updates),
        )
    }

    fun nudgeLeafSize(root: TreeNode, leafId: String, delta: Float): TreeNode = when (root) {
        is LeafNode -> root
        is SplitNode -> when {
            root.first is LeafNode && root.first.id == leafId ->
                root.copy(ratio = (root.ratio + delta).coerceIn(0.2f, 0.8f))
            root.second is LeafNode && root.second.id == leafId ->
                root.copy(ratio = (root.ratio - delta).coerceIn(0.2f, 0.8f))
            else -> root.copy(
                first = nudgeLeafSize(root.first, leafId, delta),
                second = nudgeLeafSize(root.second, leafId, delta),
            )
        }
    }

    fun toggleSplitDirection(root: TreeNode, splitId: String): TreeNode = when (root) {
        is LeafNode -> root
        is SplitNode -> {
            if (root.id == splitId) {
                root.copy(
                    direction = if (root.direction == SplitDirection.HORIZONTAL) {
                        SplitDirection.VERTICAL
                    } else {
                        SplitDirection.HORIZONTAL
                    }
                )
            } else {
                root.copy(
                    first = toggleSplitDirection(root.first, splitId),
                    second = toggleSplitDirection(root.second, splitId),
                )
            }
        }
    }

    fun updateLeafTile(root: TreeNode, leafId: String, updater: (TileData) -> TileData): TreeNode =
        when (root) {
            is LeafNode -> if (root.id == leafId) root.copy(tile = updater(root.tile)) else root
            is SplitNode -> root.copy(
                first = updateLeafTile(root.first, leafId, updater),
                second = updateLeafTile(root.second, leafId, updater),
            )
        }

    fun splitLeaf(
        root: TreeNode,
        targetLeafId: String,
        newTile: TileData,
        dropZone: DropZone,
        ratio: Float = 0.5f,
        newLeafId: String? = null,
    ): TreeNode {
        val safeEdgeZone = if (dropZone == DropZone.CENTER) DropZone.BOTTOM else dropZone
        return when (root) {
            is LeafNode -> {
                if (root.id != targetLeafId) {
                    root
                } else {
                    val insertedLeaf = LeafNode(
                        id = newLeafId ?: createNodeId("leaf"),
                        tile = newTile,
                    )
                    val direction =
                        if (safeEdgeZone == DropZone.LEFT || safeEdgeZone == DropZone.RIGHT) {
                            SplitDirection.HORIZONTAL
                        } else {
                            SplitDirection.VERTICAL
                        }
                    val isNewFirst = safeEdgeZone == DropZone.LEFT || safeEdgeZone == DropZone.TOP
                    SplitNode(
                        id = createNodeId("split"),
                        direction = direction,
                        ratio = ratio,
                        first = if (isNewFirst) insertedLeaf else root,
                        second = if (isNewFirst) root else insertedLeaf,
                    )
                }
            }
            is SplitNode -> root.copy(
                first = splitLeaf(root.first, targetLeafId, newTile, safeEdgeZone, ratio, newLeafId),
                second = splitLeaf(root.second, targetLeafId, newTile, safeEdgeZone, ratio, newLeafId),
            )
        }
    }

    data class RemoveLeafResult(
        val tree: TreeNode?,
        val removed: LeafNode? = null,
    )

    fun removeLeaf(root: TreeNode, targetLeafId: String): RemoveLeafResult {
        when (root) {
            is LeafNode -> {
                return if (root.id == targetLeafId) {
                    RemoveLeafResult(tree = null, removed = root)
                } else {
                    RemoveLeafResult(tree = root)
                }
            }
            is SplitNode -> {
                if (root.first is LeafNode && root.first.id == targetLeafId) {
                    return RemoveLeafResult(tree = root.second, removed = root.first)
                }
                if (root.second is LeafNode && root.second.id == targetLeafId) {
                    return RemoveLeafResult(tree = root.first, removed = root.second)
                }

                val leftRes = removeLeaf(root.first, targetLeafId)
                if (leftRes.removed != null) {
                    return RemoveLeafResult(
                        tree = leftRes.tree?.let { root.copy(first = it) } ?: root.second,
                        removed = leftRes.removed,
                    )
                }

                val rightRes = removeLeaf(root.second, targetLeafId)
                if (rightRes.removed != null) {
                    return RemoveLeafResult(
                        tree = rightRes.tree?.let { root.copy(second = it) } ?: root.first,
                        removed = rightRes.removed,
                    )
                }

                return RemoveLeafResult(tree = root)
            }
        }
    }

    fun swapLeaves(root: TreeNode, leafIdA: String, leafIdB: String): TreeNode {
        if (leafIdA == leafIdB) return root
        val leafA = findLeaf(root, leafIdA) ?: return root
        val leafB = findLeaf(root, leafIdB) ?: return root

        fun replace(node: TreeNode): TreeNode = when (node) {
            is LeafNode -> when (node.id) {
                leafIdA -> leafB.copy()
                leafIdB -> leafA.copy()
                else -> node
            }
            is SplitNode -> node.copy(
                first = replace(node.first),
                second = replace(node.second),
            )
        }

        return replace(root)
    }

    /**
     * Moves an existing leaf onto a target leaf's edge, removing it from its old slot
     * and splitting the target leaf while preserving the moved leaf's ID.
     */
    fun moveLeafToSplit(
        root: TreeNode,
        sourceLeafId: String,
        targetLeafId: String,
        dropZone: DropZone,
        ratio: Float = 0.5f,
    ): TreeNode {
        if (sourceLeafId == targetLeafId) return root
        if (dropZone == DropZone.CENTER) {
            return swapLeaves(root, sourceLeafId, targetLeafId)
        }
        val removedRes = removeLeaf(root, sourceLeafId)
        val removed = removedRes.removed ?: return root
        val remainingTree = removedRes.tree ?: return root
        return splitLeaf(
            root = remainingTree,
            targetLeafId = targetLeafId,
            newTile = removed.tile,
            dropZone = dropZone,
            ratio = ratio,
            newLeafId = removed.id,
        )
    }

    /**
     * Transforms a page's tree into one of 3 preset layout patterns while keeping
     * existing leaf IDs so animations smoothly morph tiles into their new positions.
     */
    fun applyTemplate(
        root: TreeNode,
        template: TemplateKind,
        fallbackTiles: List<TileData>,
    ): TreeNode {
        val existing = collectLeaves(root)
        fun getLeaf(idx: Int): LeafNode {
            if (idx < existing.size) return existing[idx]
            val fallback = fallbackTiles[idx % fallbackTiles.size]
            return LeafNode(
                id = createNodeId("leaf"),
                tile = fallback.copy(id = createNodeId("tile")),
            )
        }

        return when (template) {
            TemplateKind.GRID_2X2 -> SplitNode(
                id = createNodeId("split"),
                direction = SplitDirection.VERTICAL,
                ratio = 0.5f,
                first = SplitNode(
                    id = createNodeId("split"),
                    direction = SplitDirection.HORIZONTAL,
                    ratio = 0.5f,
                    first = getLeaf(0),
                    second = getLeaf(1),
                ),
                second = SplitNode(
                    id = createNodeId("split"),
                    direction = SplitDirection.HORIZONTAL,
                    ratio = 0.5f,
                    first = getLeaf(2),
                    second = getLeaf(3),
                ),
            )
            TemplateKind.BIG_PLUS_3 -> SplitNode(
                id = createNodeId("split"),
                direction = SplitDirection.VERTICAL,
                ratio = 0.58f,
                first = getLeaf(0),
                second = SplitNode(
                    id = createNodeId("split"),
                    direction = SplitDirection.HORIZONTAL,
                    ratio = 1f / 3f,
                    first = getLeaf(1),
                    second = SplitNode(
                        id = createNodeId("split"),
                        direction = SplitDirection.HORIZONTAL,
                        ratio = 0.5f,
                        first = getLeaf(2),
                        second = getLeaf(3),
                    ),
                ),
            )
            TemplateKind.COL_PLUS_GRID -> SplitNode(
                id = createNodeId("split"),
                direction = SplitDirection.HORIZONTAL,
                ratio = 0.42f,
                first = getLeaf(0),
                second = SplitNode(
                    id = createNodeId("split"),
                    direction = SplitDirection.VERTICAL,
                    ratio = 0.5f,
                    first = SplitNode(
                        id = createNodeId("split"),
                        direction = SplitDirection.HORIZONTAL,
                        ratio = 0.5f,
                        first = getLeaf(1),
                        second = getLeaf(2),
                    ),
                    second = SplitNode(
                        id = createNodeId("split"),
                        direction = SplitDirection.HORIZONTAL,
                        ratio = 0.5f,
                        first = getLeaf(3),
                        second = getLeaf(4),
                    ),
                ),
            )
        }
    }

    fun determineDropZone(relX: Float, relY: Float, width: Float, height: Float): DropZone {
        val nx = relX / max(1f, width)
        val ny = relY / max(1f, height)

        if (nx in 0.27f..0.73f && ny in 0.27f..0.73f) {
            return DropZone.CENTER
        }

        val distLeft = nx
        val distRight = 1f - nx
        val distTop = ny
        val distBottom = 1f - ny
        val minDist = min(min(distLeft, distRight), min(distTop, distBottom))

        return when (minDist) {
            distLeft -> DropZone.LEFT
            distRight -> DropZone.RIGHT
            distTop -> DropZone.TOP
            else -> DropZone.BOTTOM
        }
    }
}
