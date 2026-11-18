package app.canopy.launcher.layout

import app.canopy.launcher.data.AlarmState
import app.canopy.launcher.data.AnimationMood
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.InMemoryLauncherStore
import app.canopy.launcher.data.LauncherPersistenceCodec
import app.canopy.launcher.data.MotionSettings
import app.canopy.launcher.data.PersistedLauncherState
import app.canopy.launcher.data.ShortcutBarMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.data.WallpaperPresetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TilingMutationsAndPersistenceTest {
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
    fun `splits a leaf along any edge and preserves existing tile`() {
        val updated = TilingEngine.splitLeaf(
            root = sampleTree,
            targetLeafId = "leaf-a",
            newTile = makeTile("d", "Delta"),
            dropZone = DropZone.RIGHT,
            ratio = 0.25f,
            newLeafId = "leaf-d",
        )
        val layout = TilingEngine.computeLayout(updated, RectFBounds(0f, 0f, 360f, 600f), 6f)
        assertEquals(4, layout.leaves.size)
        val leafA = layout.leaves.first { it.id == "leaf-a" }
        val leafD = layout.leaves.first { it.id == "leaf-d" }
        assertEquals(0f, leafA.rect.x, 1e-4f)
        assertTrue(leafD.rect.x > leafA.rect.x)
    }

    @Test
    fun `removes a leaf and collapses its parent split so sibling expands`() {
        val res = TilingEngine.removeLeaf(sampleTree, "leaf-b")
        assertEquals("leaf-b", res.removed?.id)
        assertNotNull(res.tree)
        val layout = TilingEngine.computeLayout(res.tree!!, RectFBounds(0f, 0f, 360f, 600f), 6f)
        assertEquals(2, layout.leaves.size)
        val gamma = layout.leaves.first { it.id == "leaf-c" }
        assertEquals(360f, gamma.rect.width, 1e-4f)
    }

    @Test
    fun `swaps two leaves cleanly`() {
        val swapped = TilingEngine.swapLeaves(sampleTree, "leaf-a", "leaf-c")
        val layout = TilingEngine.computeLayout(swapped, RectFBounds(0f, 0f, 360f, 600f), 6f)
        assertEquals("leaf-c", layout.leaves[0].id)
        assertEquals("leaf-a", layout.leaves[2].id)
    }

    @Test
    fun `moves a leaf onto another leaf edge while preserving its ID`() {
        val moved = TilingEngine.moveLeafToSplit(sampleTree, "leaf-c", "leaf-a", DropZone.LEFT, 0.33f)
        val layout = TilingEngine.computeLayout(moved, RectFBounds(0f, 0f, 360f, 600f), 6f)
        assertEquals(3, layout.leaves.size)
        val movedLeaf = layout.leaves.first { it.id == "leaf-c" }
        assertEquals(0f, movedLeaf.rect.x, 1e-4f)
        assertEquals(0f, movedLeaf.rect.y, 1e-4f)
    }

    @Test
    fun `flips split direction on toggleSplitDirection`() {
        val toggled = TilingEngine.toggleSplitDirection(sampleTree, "split-root")
        assertTrue(toggled is SplitNode && toggled.direction == SplitDirection.HORIZONTAL)
    }

    @Test
    fun `updates split ratio immutably`() {
        val updated = TilingEngine.updateSplitRatio(sampleTree, "split-sub", 0.75f)
        val layout = TilingEngine.computeLayout(updated, RectFBounds(0f, 0f, 406f, 600f), 6f)
        val b = layout.leaves.first { it.id == "leaf-b" }
        val c = layout.leaves.first { it.id == "leaf-c" }
        assertEquals(300f, b.rect.width, 1e-3f)
        assertEquals(100f, c.rect.width, 1e-3f)
    }

    @Test
    fun `applies all 3 templates preserving existing leaf IDs and adding fallbacks`() {
        val fallback = (1..5).map { makeTile("f$it") }
        val grid2x2 = TilingEngine.applyTemplate(sampleTree, TemplateKind.GRID_2X2, fallback)
        val layout = TilingEngine.computeLayout(grid2x2, RectFBounds(0f, 0f, 374f, 700f), 6f)
        assertEquals(4, layout.leaves.size)
        assertEquals(listOf("leaf-a", "leaf-b", "leaf-c"), layout.leaves.take(3).map { it.id })

        val bigPlus3 = TilingEngine.applyTemplate(sampleTree, TemplateKind.BIG_PLUS_3, fallback)
        assertEquals(4, TilingEngine.collectLeaves(bigPlus3).size)

        val colPlusGrid = TilingEngine.applyTemplate(sampleTree, TemplateKind.COL_PLUS_GRID, fallback)
        assertEquals(5, TilingEngine.collectLeaves(colPlusGrid).size)
    }

    @Test
    fun `detects 2D intersections in 2x2 and T-junction layouts and updates both axes atomically`() {
        val fallback = (1..4).map { makeTile("f$it") }
        val grid2x2 = TilingEngine.applyTemplate(sampleTree, TemplateKind.GRID_2X2, fallback)
        val layout2x2 = TilingEngine.computeLayout(grid2x2, RectFBounds(0f, 0f, 374f, 700f), 12f)
        assertEquals(1, layout2x2.intersections.size)
        assertEquals(2, layout2x2.intersections[0].horizontalSplits.size)
        assertEquals(1, layout2x2.intersections[0].verticalSplits.size)

        val ix = layout2x2.intersections[0]
        val updates = buildMap {
            put(ix.verticalSplits[0].id, 0.35f)
            ix.horizontalSplits.forEach { put(it.id, 0.65f) }
        }
        val morphed = TilingEngine.updateMultipleSplitRatios(grid2x2, updates)
        val morphedLayout = TilingEngine.computeLayout(morphed, RectFBounds(0f, 0f, 374f, 700f), 12f)
        assertTrue(morphedLayout.intersections[0].x > 220f)
        assertTrue(morphedLayout.intersections[0].y < 260f)

        // T-junction from sampleTree (vertical split-root meets horizontal split-sub)
        val tLayout = TilingEngine.computeLayout(sampleTree, RectFBounds(0f, 0f, 374f, 700f), 12f)
        assertEquals(1, tLayout.intersections.size)
    }

    @Test
    fun `nudges a leaf larger or smaller via nudgeLeafSize`() {
        val grown = TilingEngine.nudgeLeafSize(sampleTree, "leaf-b", 0.14f)
        val bounds = RectFBounds(0f, 0f, 360f, 600f)
        val bBefore = TilingEngine.computeLayout(sampleTree, bounds, 6f).leaves.first { it.id == "leaf-b" }
        val bAfter = TilingEngine.computeLayout(grown, bounds, 6f).leaves.first { it.id == "leaf-b" }
        assertTrue(bAfter.rect.width > bBefore.rect.width)
    }

    @Test
    fun `determines center and directional drop zones accurately`() {
        assertEquals(DropZone.CENTER, TilingEngine.determineDropZone(50f, 50f, 100f, 100f))
        assertEquals(DropZone.LEFT, TilingEngine.determineDropZone(10f, 50f, 100f, 100f))
        assertEquals(DropZone.RIGHT, TilingEngine.determineDropZone(92f, 50f, 100f, 100f))
        assertEquals(DropZone.TOP, TilingEngine.determineDropZone(50f, 8f, 100f, 100f))
        assertEquals(DropZone.BOTTOM, TilingEngine.determineDropZone(50f, 92f, 100f, 100f))
    }

    @Test
    fun `persists full launcher state round-trip and supports factory reset`() {
        val defaultState = PersistedLauncherState(
            pages = listOf(LauncherPage("page-start", "Start", PageKind.TILES, sampleTree)),
        )
        val store = InMemoryLauncherStore { defaultState }

        val customState = PersistedLauncherState(
            pages = listOf(
                LauncherPage(
                    id = "page-custom",
                    title = "Custom",
                    kind = PageKind.TILES,
                    root = LeafNode(
                        id = "leaf-group",
                        tile = TileData(
                            id = "tile-group",
                            type = TileType.APP_GROUP,
                            title = "Studio",
                            appIds = listOf("app-telefon", "app-kamera"),
                        ),
                    ),
                )
            ),
            homePageId = "page-custom",
            themeId = ThemeVariantId.GLASS,
            colorMode = ColorMode.DARK,
            wallpaperId = WallpaperPresetId.COBALT,
            tonalIcons = false,
            shortcutBarMode = ShortcutBarMode.START_ONLY,
            motionSettings = MotionSettings(
                stiffnessScale = 1.2f,
                dampingScale = 0.9f,
                mood = AnimationMood.VERSPIELT,
                hapticsEnabled = true,
                slowMotion = true,
            ),
            alarmState = AlarmState(enabled = false, timeLabel = "06:45"),
        )

        store.save(customState)
        val restored = store.load()
        assertNotNull(restored)
        assertEquals(customState, restored)
        assertNull(LauncherPersistenceCodec.decode("{invalid-json"))

        store.reset()
        assertNull(store.load())
        assertEquals(defaultState, store.loadOrReset())
    }
}
