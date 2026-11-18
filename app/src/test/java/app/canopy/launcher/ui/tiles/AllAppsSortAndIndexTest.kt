package app.canopy.launcher.ui.tiles

import app.canopy.launcher.data.MockCatalogData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllAppsSortAndIndexTest {
    @Test
    fun `groups all catalog apps accurately across all 4 sort modes`() {
        val catalog = MockCatalogData.APPS_CATALOG

        val az = AllAppsCatalogSorter.groupApps(catalog, AllAppsSortMode.A_Z)
        assertTrue(az.size > 10)
        assertEquals(catalog.size, az.sumOf { it.apps.size })

        val byCategory = AllAppsCatalogSorter.groupApps(catalog, AllAppsSortMode.CATEGORY)
        assertEquals(AppCategory.entries.size, byCategory.size)
        assertEquals(catalog.size, byCategory.sumOf { it.apps.size })

        val byColor = AllAppsCatalogSorter.groupApps(catalog, AllAppsSortMode.COLOR)
        assertEquals(1, byColor.size)
        val hues = byColor.first().apps.map { it.hue }
        assertEquals(hues.sorted(), hues)

        val byUsage = AllAppsCatalogSorter.groupApps(catalog, AllAppsSortMode.USAGE)
        assertEquals(1, byUsage.size)
        val scores = byUsage.first().apps.map { it.usageScore }
        assertEquals(scores.sortedDescending(), scores)
        assertEquals("Telefon", byUsage.first().apps.first().name)
    }

    @Test
    fun `computes section header indices and resolves drag position to letters cleanly`() {
        val sections = AllAppsCatalogSorter.groupApps(MockCatalogData.APPS_CATALOG, AllAppsSortMode.A_Z)
        val indices = AllAppsCatalogSorter.computeSectionHeaderIndices(sections)

        assertEquals(0, indices[sections.first().label])
        val expectedSecondIdx = 1 + sections.first().apps.size
        assertEquals(expectedSecondIdx, indices[sections[1].label])

        val letters = sections.map { it.label }
        assertEquals(letters.first(), AllAppsCatalogSorter.letterForDragRatio(letters, 0f, 300f))
        assertEquals(letters.last(), AllAppsCatalogSorter.letterForDragRatio(letters, 299f, 300f))
        assertNull(AllAppsCatalogSorter.letterForDragRatio(emptyList(), 50f, 300f))
    }

    @Test
    fun `sort mode cycles cleanly through all 4 modes`() {
        val first = AllAppsSortMode.A_Z
        val second = first.next()
        val third = second.next()
        val fourth = third.next()
        assertEquals(AllAppsSortMode.CATEGORY, second)
        assertEquals(AllAppsSortMode.COLOR, third)
        assertEquals(AllAppsSortMode.USAGE, fourth)
        assertEquals(AllAppsSortMode.A_Z, fourth.next())
    }
}
