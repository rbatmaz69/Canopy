package app.canopy.launcher.layout

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TilingGeometryTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun makeTile(id: String, title: String = id) = TileData(
        id = id,
        type = TileType.NOTE,
        title = title,
    )

    private val sampleTree: TreeNode = SplitNode(
        id = "split-root",
        direction = SplitDirection.VERTICAL,
        ratio = 0.5f,
        first = LeafNode(id = "leaf-a", tile = makeTile("a", "Alpha")),
        second = SplitNode(
            id = "split-sub",
            direction = SplitDirection.HORIZONTAL,
            ratio = 0.5f,
            first = LeafNode(id = "leaf-b", tile = makeTile("b", "Beta")),
            second = LeafNode(id = "leaf-c", tile = makeTile("c", "Gamma")),
        ),
    )

    @Test
    fun `computes gap-free leaf rectangles with exact gap spacing`() {
        val bounds = RectFBounds(x = 8f, y = 8f, width = 374f, height = 740f)
        val layout = TilingEngine.computeLayout(sampleTree, bounds, TilingEngine.GAP_USE_DP)

        assertEquals(3, layout.leaves.size)
        assertEquals(2, layout.dividers.size)

        val (a, b, c) = layout.leaves
        assertEquals(8f, a.rect.x, 1e-4f)
        assertEquals(8f, a.rect.y, 1e-4f)
        assertEquals(374f, a.rect.width, 1e-4f)
        assertEquals((740f - 6f) * 0.5f, a.rect.height, 1e-4f)

        assertEquals(a.rect.y + a.rect.height + 6f, b.rect.y, 1e-4f)
        assertEquals(c.rect.x, b.rect.x + b.rect.width + 6f, 1e-4f)
        assertEquals(8f + 374f, c.rect.x + c.rect.width, 1e-4f)
    }

    @Test
    fun `partition area invariant holds across deep nesting and both gap modes`() {
        val deepTree = SplitNode(
            id = "s1",
            direction = SplitDirection.HORIZONTAL,
            ratio = 0.4f,
            first = SplitNode(
                id = "s2",
                direction = SplitDirection.VERTICAL,
                ratio = 0.25f,
                first = LeafNode("l1", makeTile("1")),
                second = SplitNode(
                    id = "s3",
                    direction = SplitDirection.HORIZONTAL,
                    ratio = 0.6f,
                    first = LeafNode("l2", makeTile("2")),
                    second = LeafNode("l3", makeTile("3")),
                ),
            ),
            second = SplitNode(
                id = "s4",
                direction = SplitDirection.VERTICAL,
                ratio = 0.7f,
                first = LeafNode("l4", makeTile("4")),
                second = LeafNode("l5", makeTile("5")),
            ),
        )

        val bounds = RectFBounds(x = 8f, y = 4f, width = 374f, height = 740f)
        for (gap in listOf(TilingEngine.GAP_USE_DP, TilingEngine.GAP_EDIT_DP)) {
            val layout = TilingEngine.computeLayout(deepTree, bounds, gap)
            assertEquals(5, layout.leaves.size)
            assertEquals(4, layout.dividers.size)

            val leafArea = layout.leaves.sumOf { it.rect.area.toDouble() }
            val dividerArea = layout.dividers.sumOf { it.rect.area.toDouble() }
            assertEquals(bounds.area.toDouble(), leafArea + dividerArea, 1e-2)

            // Verify zero pairwise overlap among leaves
            for (i in layout.leaves.indices) {
                for (j in i + 1 until layout.leaves.size) {
                    val r1 = layout.leaves[i].rect
                    val r2 = layout.leaves[j].rect
                    val overlapX = r1.x < r2.right - 1e-3f && r1.right > r2.x + 1e-3f
                    val overlapY = r1.y < r2.bottom - 1e-3f && r1.bottom > r2.y + 1e-3f
                    assertFalse("Leaves ${layout.leaves[i].id} and ${layout.leaves[j].id} overlap", overlapX && overlapY)
                }
            }
        }
    }

    @Test
    fun `dividers track firstLeafIds and secondLeafIds accurately`() {
        val bounds = RectFBounds(0f, 0f, 360f, 600f)
        val layout = TilingEngine.computeLayout(sampleTree, bounds, 6f)
        val rootDivider = layout.dividers.first { it.id == "split-root" }
        assertEquals(listOf("leaf-a"), rootDivider.firstLeafIds)
        assertEquals(listOf("leaf-b", "leaf-c"), rootDivider.secondLeafIds)
    }

    @Test
    fun `split and leaf nodes serialize and deserialize losslessly`() {
        val encoded = json.encodeToString(TreeNode.serializer(), sampleTree)
        val decoded = json.decodeFromString(TreeNode.serializer(), encoded)
        assertTrue(decoded is SplitNode)
        assertEquals(sampleTree, decoded)
    }
}
