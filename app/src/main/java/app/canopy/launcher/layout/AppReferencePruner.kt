package app.canopy.launcher.layout

import app.canopy.launcher.ui.tiles.AppIdentity

/**
 * Removes references to an uninstalled app from every page: app tiles and the app's widgets
 * disappear (the neighbouring tile fills the gap), groups simply lose the member.
 */
object AppReferencePruner {
    data class Result(
        val pages: List<LauncherPage>,
        val removedTiles: Int,
        /** AppWidget ids that must be released with AppWidgetHost.deleteAppWidgetId. */
        val releasedWidgetIds: List<Int>,
    )

    fun matchesPackage(appId: String?, packageName: String, userSerial: Long): Boolean =
        AppIdentity.packageOf(appId) == packageName && AppIdentity.userSerialOf(appId) == userSerial

    fun prune(pages: List<LauncherPage>, isRemoved: (String) -> Boolean): Result {
        var removedTiles = 0
        val widgetIds = mutableListOf<Int>()
        val next = pages.map { page ->
            if (page.kind == PageKind.ALL_APPS) return@map page
            var root = page.root
            TilingEngine.collectLeaves(root).forEach { leaf ->
                val tile = leaf.tile
                val removesWholeTile = (tile.type == TileType.SINGLE_APP || tile.type == TileType.THIRD_PARTY_WIDGET) &&
                    tile.appId != null && isRemoved(tile.appId)
                when {
                    removesWholeTile -> {
                        val res = TilingEngine.removeLeaf(root, leaf.id)
                        if (res.tree != null && res.removed != null) {
                            root = res.tree
                            removedTiles += 1
                            tile.androidAppWidgetId?.let { widgetIds += it }
                        }
                    }
                    tile.appIds?.any(isRemoved) == true || tile.newAppIds?.any(isRemoved) == true -> {
                        root = TilingEngine.updateLeafTile(root, leaf.id) { t ->
                            t.copy(
                                appIds = t.appIds?.filterNot(isRemoved),
                                newAppIds = t.newAppIds?.filterNot(isRemoved),
                            )
                        }
                    }
                }
            }
            if (root === page.root) page else page.copy(root = root)
        }
        return Result(next, removedTiles, widgetIds)
    }

    /** All AppWidget ids referenced by the layout (used to clean up leaked host ids). */
    fun referencedWidgetIds(pages: List<LauncherPage>): Set<Int> =
        pages.flatMap { TilingEngine.collectLeaves(it.root) }.mapNotNull { it.tile.androidAppWidgetId }.toSet()
}
