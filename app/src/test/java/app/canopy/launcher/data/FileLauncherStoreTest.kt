package app.canopy.launcher.data

import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.LeafNode
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileLauncherStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun state(title: String) = PersistedLauncherState(
        pages = listOf(
            LauncherPage(
                id = "p1",
                title = title,
                kind = PageKind.TILES,
                root = LeafNode("l1", TileData("t1", TileType.CLOCK, "Uhr")),
            )
        ),
    )

    @Test
    fun `save then load round-trips and keeps the previous version as backup`() {
        val dir = tmp.newFolder("launcher")
        val store = FileLauncherStore(dir)
        store.save(state("Erste"))
        store.save(state("Zweite"))

        assertEquals("Zweite", store.load()?.pages?.first()?.title)
        assertEquals(FileLauncherStore.LoadSource.MAIN, store.lastLoadSource)
        val backup = LauncherPersistenceCodec.decode(File(dir, FileLauncherStore.BACKUP_NAME).readText())
        assertEquals("Erste", backup?.pages?.first()?.title)
        assertTrue(!File(dir, FileLauncherStore.TEMP_NAME).exists())
    }

    @Test
    fun `corrupt main file falls back to the last good backup instead of losing the layout`() {
        val dir = tmp.newFolder("launcher")
        val store = FileLauncherStore(dir)
        store.save(state("Gut"))
        store.save(state("Auch gut"))
        File(dir, FileLauncherStore.FILE_NAME).writeText("{kaputt")

        val loaded = store.load()
        assertEquals("Gut", loaded?.pages?.first()?.title)
        assertEquals(FileLauncherStore.LoadSource.BACKUP, store.lastLoadSource)
    }

    @Test
    fun `legacy shared preferences layout is migrated once`() {
        val dir = tmp.newFolder("launcher")
        var migrated = false
        val legacyJson = LauncherPersistenceCodec.encode(state("Alt"))
        val store = FileLauncherStore(dir, legacyLoader = { legacyJson }, onLegacyMigrated = { migrated = true })

        assertEquals("Alt", store.load()?.pages?.first()?.title)
        assertEquals(FileLauncherStore.LoadSource.LEGACY, store.lastLoadSource)
        assertTrue(migrated)
        assertTrue(File(dir, FileLauncherStore.FILE_NAME).exists())
    }

    @Test
    fun `empty store loads null and reset keeps a backup`() {
        val dir = tmp.newFolder("launcher")
        val store = FileLauncherStore(dir)
        assertNull(store.load())
        store.save(state("Vor Reset"))
        store.reset()
        assertNull(LauncherPersistenceCodec.decode(File(dir, FileLauncherStore.FILE_NAME).takeIf { it.exists() }?.readText()))
        assertNotNull(store.load()) // backup still available
    }

    @Test
    fun `old schema versions and unknown enum values still decode`() {
        val json = LauncherPersistenceCodec.encode(state("Migriert"))
            .replace("\"schemaVersion\":3", "\"schemaVersion\":2")
            .replace("\"material-you\"", "\"neon-future\"")
        val decoded = LauncherPersistenceCodec.decode(json)
        assertNotNull(decoded)
        assertEquals(ThemeVariantId.MATERIAL_YOU, decoded!!.themeId)
        assertEquals(true, decoded.lockPortrait)
        assertNull(decoded.weatherLocation)
    }
}
