package app.canopy.launcher.ui.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.layout.DropZone
import app.canopy.launcher.layout.LauncherPage
import app.canopy.launcher.layout.LeafNode
import app.canopy.launcher.layout.PageKind
import app.canopy.launcher.layout.RectFBounds
import app.canopy.launcher.layout.SplitDirection
import app.canopy.launcher.layout.SplitNode
import app.canopy.launcher.layout.TemplateKind
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.layout.TileType
import app.canopy.launcher.layout.TilingEngine
import app.canopy.launcher.layout.WidgetKind
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.system.InstalledAppsMapper
import app.canopy.launcher.system.NativeWidgetProviderSpec
import app.canopy.launcher.ui.components.AppSquircleIcon
import app.canopy.launcher.ui.theme.SurfaceEmphasis
import app.canopy.launcher.ui.theme.ThemeTokens
import app.canopy.launcher.ui.tiles.AppDefinition

enum class CatalogTab(val label: String) {
    EIGEN("Eigene Kacheln"),
    WIDGETS("Widgets"),
    APPS("Apps"),
    GRUPPEN("Gruppen"),
}

object CatalogAndPageManager {
    val WIDGET_TEMPLATES: List<TileData> = listOf(
        TileData(
            id = "cat-w-podcast",
            type = TileType.THIRD_PARTY_WIDGET,
            title = "Podcast",
            appId = "app-podcast",
            widgetKind = WidgetKind.PODCAST,
            widgetFixedWidth = 158f,
            widgetFixedHeight = 148f,
            accentHue = 271f,
        ),
        TileData(
            id = "cat-w-fitness",
            type = TileType.THIRD_PARTY_WIDGET,
            title = "Fitness",
            appId = "app-fitness",
            widgetKind = WidgetKind.FITNESS,
            widgetFixedWidth = 148f,
            widgetFixedHeight = 112f,
            accentHue = 158f,
        ),
    )

    val GROUP_TEMPLATES: List<TileData> = listOf(
        TileData(
            id = "cat-g-prod",
            type = TileType.APP_GROUP,
            title = "Produktivität",
            accentHue = 215f,
            appIds = listOf("app-mail", "app-kalender", "app-notizen", "app-aufgaben", "app-code", "app-tabellen"),
        ),
        TileData(
            id = "cat-g-unterwegs",
            type = TileType.APP_GROUP,
            title = "Unterwegs",
            accentHue = 160f,
            appIds = listOf("app-karten", "app-bahn", "app-wetter", "app-kamera", "app-wallet"),
        ),
        TileData(
            id = "cat-g-studio",
            type = TileType.APP_GROUP,
            title = "Kreativ",
            accentHue = 328f,
            appIds = listOf("app-skizze", "app-pinsel", "app-musik", "app-galerie"),
        ),
    )

    fun insertCatalogTileIntoLargestLeaf(
        page: LauncherPage,
        newTile: TileData,
        boundsDp: RectFBounds,
        gapDp: Float,
    ): LauncherPage {
        if (page.kind == PageKind.ALL_APPS) return page
        val layout = TilingEngine.computeLayout(page.root, boundsDp, gapDp)
        val largest = layout.leaves.maxByOrNull { it.rect.area } ?: return page
        val zone = if (largest.rect.width > largest.rect.height) DropZone.RIGHT else DropZone.BOTTOM
        return page.copy(
            root = TilingEngine.splitLeaf(page.root, largest.id, newTile, zone, 0.5f)
        )
    }

    fun reorderRegularPage(
        pages: List<LauncherPage>,
        regularIndex: Int,
        delta: Int,
    ): List<LauncherPage> {
        val regular = pages.filter { it.kind != PageKind.ALL_APPS }.toMutableList()
        val allApps = pages.filter { it.kind == PageKind.ALL_APPS }
        val nextIdx = regularIndex + delta
        if (regularIndex !in regular.indices || nextIdx !in regular.indices) return pages
        val moved = regular.removeAt(regularIndex)
        regular.add(nextIdx, moved)
        return regular + allApps
    }

    fun addNewRegularPage(pages: List<LauncherPage>): Pair<List<LauncherPage>, Int> {
        val newPage = LauncherPage(
            id = TilingEngine.createNodeId("page"),
            title = "Seite ${pages.size}",
            kind = PageKind.TILES,
            root = SplitNode(
                id = TilingEngine.createNodeId("split"),
                direction = SplitDirection.VERTICAL,
                ratio = 0.5f,
                first = LeafNode(
                    id = TilingEngine.createNodeId("leaf"),
                    tile = MockCatalogData.CATALOG_EXTRA_TILES[4].copy(id = TilingEngine.createNodeId("tile")),
                ),
                second = SplitNode(
                    id = TilingEngine.createNodeId("split"),
                    direction = SplitDirection.HORIZONTAL,
                    ratio = 0.5f,
                    first = LeafNode(
                        id = TilingEngine.createNodeId("leaf"),
                        tile = MockCatalogData.CATALOG_EXTRA_TILES[5].copy(id = TilingEngine.createNodeId("tile")),
                    ),
                    second = LeafNode(
                        id = TilingEngine.createNodeId("leaf"),
                        tile = MockCatalogData.CATALOG_EXTRA_TILES[6].copy(id = TilingEngine.createNodeId("tile")),
                    ),
                ),
            ),
        )
        val allAppsIdx = pages.indexOfFirst { it.kind == PageKind.ALL_APPS }
        val insertIdx = if (allAppsIdx >= 0) allAppsIdx else pages.size
        val next = pages.toMutableList().apply { add(insertIdx, newPage) }
        return next to insertIdx
    }

    fun deleteRegularPage(
        pages: List<LauncherPage>,
        pageId: String,
        currentHomePageId: String,
    ): Pair<List<LauncherPage>, String> {
        val regular = pages.filter { it.kind != PageKind.ALL_APPS }
        if (regular.size <= 1) return pages to currentHomePageId
        val target = regular.firstOrNull { it.id == pageId } ?: return pages to currentHomePageId
        val nextPages = pages.filterNot { it.id == target.id }
        val nextHome = if (currentHomePageId == target.id) {
            nextPages.first { it.kind != PageKind.ALL_APPS }.id
        } else {
            currentHomePageId
        }
        return nextPages to nextHome
    }
}

@Composable
fun EditToolbar(
    theme: ThemeTokens,
    hapticsEnabled: Boolean,
    isAllAppsPage: Boolean,
    onOpenCatalog: () -> Unit,
    onApplyTemplate: (TemplateKind) -> Unit,
    onOpenPages: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTemplates by remember { mutableStateOf(false) }
    val templates = remember {
        listOf(
            Triple(TemplateKind.BIG_PLUS_3, "Groß + 3", "58 % Hero + 3 Kacheln"),
            Triple(TemplateKind.GRID_2X2, "2 × 2", "4 ausgewogene Felder"),
            Triple(TemplateKind.COL_PLUS_GRID, "Spalte + Gitter", "Linke Spalte + 4 Felder"),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(
            visible = showTemplates,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(14.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.sheetBg)
                    .border(1.dp, theme.sheetBorder, RoundedCornerShape(20.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                templates.forEach { (kind, label, desc) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x1A7D7D91))
                            .border(1.dp, theme.sheetBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                                onApplyTemplate(kind)
                                showTemplates = false
                            }
                            .padding(horizontal = 6.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(label, color = theme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(desc, color = theme.textMuted, fontSize = 9.5.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(theme.sheetBg)
                .border(1.dp, theme.sheetBorder, RoundedCornerShape(22.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EditToolbarButton(
                icon = Icons.Rounded.Add,
                label = "+ Kachel",
                textColor = theme.textPrimary,
                active = false,
                disabled = isAllAppsPage,
                modifier = Modifier.weight(1f),
            ) {
                CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                showTemplates = false
                onOpenCatalog()
            }
            EditToolbarButton(
                icon = Icons.Rounded.GridView,
                label = "Vorlage",
                textColor = theme.textPrimary,
                active = showTemplates,
                disabled = isAllAppsPage,
                modifier = Modifier.weight(1f),
            ) {
                CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                showTemplates = !showTemplates
            }
            EditToolbarButton(
                icon = Icons.Rounded.Layers,
                label = "Seiten",
                textColor = theme.textPrimary,
                active = false,
                disabled = false,
                modifier = Modifier.weight(1f),
            ) {
                CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                showTemplates = false
                onOpenPages()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2563EB))
                    .clickable {
                        CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                        showTemplates = false
                        onDone()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Fertig", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditToolbarButton(
    icon: ImageVector,
    label: String,
    textColor: Color,
    active: Boolean,
    disabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Color(0x387D7D91) else Color.Transparent)
            .alpha(if (disabled) 0.35f else 1f)
            .clickable(enabled = !disabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = textColor, modifier = Modifier.size(16.dp))
            Text(label, color = textColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TileCatalogSheet(
    theme: ThemeTokens,
    hapticsEnabled: Boolean,
    appsCatalog: List<AppDefinition> = MockCatalogData.APPS_CATALOG,
    nativeWidgetProviders: List<NativeWidgetProviderSpec> = emptyList(),
    onClose: () -> Unit,
    onSelectTile: (TileData) -> Unit,
    onStartCatalogDrag: (tile: TileData, pointerXRootPx: Float, pointerYRootPx: Float) -> Unit,
    onBindNativeWidget: (NativeWidgetProviderSpec) -> Unit = {},
) {
    var activeTab by remember { mutableStateOf(CatalogTab.EIGEN) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(theme.sheetBg)
                .border(1.dp, theme.sheetBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* consume */ }
                .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Kachel hinzufügen", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Tippen oder direkt auf eine Seite ziehen", color = theme.textMuted, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x297D7D91))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Katalog schließen", tint = theme.textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CatalogTab.entries.forEach { tab ->
                    val active = activeTab == tab
                    val activeText = if (theme.id == ThemeVariantId.MATERIAL_YOU && theme.mode == ColorMode.LIGHT) Color.White else Color(0xFF111827)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) theme.pillActive else Color.Transparent)
                            .let { m -> if (active) m else m.border(1.dp, theme.sheetBorder, CircleShape) }
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.TICK, hapticsEnabled)
                                activeTab = tab
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = tab.label,
                            color = if (active) activeText else theme.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (activeTab == CatalogTab.APPS) 3 else 2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (activeTab) {
                    CatalogTab.EIGEN -> {
                        items(MockCatalogData.CATALOG_EXTRA_TILES, key = { it.id }) { item ->
                            val surf = theme.getTileSurface(item.accentHue, SurfaceEmphasis.NORMAL)
                            CatalogDraggableCard(
                                tileTemplate = item,
                                hapticsEnabled = hapticsEnabled,
                                onTapInsert = onSelectTile,
                                onStartDrag = onStartCatalogDrag,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 102.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                    .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(20.dp))
                                    .padding(13.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.title, color = surf.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = surf.accentColor, modifier = Modifier.size(15.dp))
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text(previewSnippetForTile(item.type), color = surf.textSecondary, fontSize = 11.5.sp)
                                }
                            }
                        }
                    }

                    CatalogTab.WIDGETS -> {
                        items(CatalogAndPageManager.WIDGET_TEMPLATES, key = { it.id }) { item ->
                            val surf = theme.getTileSurface(item.accentHue, SurfaceEmphasis.NORMAL)
                            CatalogDraggableCard(
                                tileTemplate = item,
                                hapticsEnabled = hapticsEnabled,
                                onTapInsert = onSelectTile,
                                onStartDrag = onStartCatalogDrag,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 112.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                    .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(20.dp))
                                    .padding(13.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.SpaceBetween) {
                                    Text("PASSEPARTOUT-WIDGET", color = surf.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(14.dp))
                                    Text(item.title, color = surf.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("${item.widgetFixedWidth?.toInt()}×${item.widgetFixedHeight?.toInt()} dp", color = surf.textSecondary, fontSize = 11.5.sp)
                                }
                            }
                        }

                        items(
                            nativeWidgetProviders.take(24),
                            key = { "sys-${it.providerPackage}-${it.providerClass}" },
                        ) { provider ->
                            val hue = InstalledAppsMapper.stableHueForPackage(provider.providerPackage)
                            val surf = theme.getTileSurface(hue, SurfaceEmphasis.MUTED)
                            Column(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 112.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                    .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(20.dp))
                                    .clickable {
                                        CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                                        onBindNativeWidget(provider)
                                    }
                                    .padding(13.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("SYSTEM-WIDGET BINDEN", color = surf.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                Text(provider.label, color = surf.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${provider.minWidthDp.toInt()}×${provider.minHeightDp.toInt()} dp",
                                    color = surf.textSecondary,
                                    fontSize = 11.5.sp,
                                )
                            }
                        }
                    }

                    CatalogTab.APPS -> {
                        items(appsCatalog.take(36), key = { it.id }) { app ->
                            val template = TileData(
                                id = "cat-app-${app.id}",
                                type = TileType.SINGLE_APP,
                                title = app.name,
                                appId = app.id,
                                accentHue = app.hue,
                            )
                            val surf = theme.getTileSurface(app.hue, SurfaceEmphasis.MUTED)
                            CatalogDraggableCard(
                                tileTemplate = template,
                                hapticsEnabled = hapticsEnabled,
                                onTapInsert = onSelectTile,
                                onStartDrag = onStartCatalogDrag,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                    .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(18.dp))
                                    .padding(10.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    AppSquircleIcon(appId = app.id, iconKey = app.iconKey, color = app.color, size = 40.dp, badge = app.badge)
                                    Text(app.name, color = surf.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    CatalogTab.GRUPPEN -> {
                        items(CatalogAndPageManager.GROUP_TEMPLATES, key = { it.id }) { grp ->
                            val surf = theme.getTileSurface(grp.accentHue, SurfaceEmphasis.NORMAL)
                            CatalogDraggableCard(
                                tileTemplate = grp,
                                hapticsEnabled = hapticsEnabled,
                                onTapInsert = onSelectTile,
                                onStartDrag = onStartCatalogDrag,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 104.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                    .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(20.dp))
                                    .padding(13.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.SpaceBetween) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(grp.title, color = surf.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("${grp.appIds?.size ?: 0} Apps", color = surf.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text("Dynamisches Kachel-Gitter", color = surf.textSecondary, fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogDraggableCard(
    tileTemplate: TileData,
    hapticsEnabled: Boolean,
    onTapInsert: (TileData) -> Unit,
    onStartDrag: (TileData, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var rootOriginX = 0f
    var rootOriginY = 0f
    var dragStarted = false

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val b = coords.boundsInRoot()
                rootOriginX = b.left
                rootOriginY = b.top
            }
            .pointerInput(tileTemplate.id) {
                detectTapGestures(
                    onTap = {
                        CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                        onTapInsert(tileTemplate.copy(id = TilingEngine.createNodeId("tile")))
                    }
                )
            }
            .pointerInput(tileTemplate.id) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStarted = true
                        onStartDrag(
                            tileTemplate.copy(id = TilingEngine.createNodeId("tile")),
                            rootOriginX + offset.x,
                            rootOriginY + offset.y,
                        )
                    },
                    onDrag = { change, _ ->
                        if (dragStarted) change.consume()
                    },
                    onDragEnd = { dragStarted = false },
                    onDragCancel = { dragStarted = false },
                )
            },
    ) {
        content()
    }
}

@Composable
fun PageOverviewModal(
    pages: List<LauncherPage>,
    activePageIndex: Int,
    homePageId: String,
    theme: ThemeTokens,
    hapticsEnabled: Boolean,
    onClose: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onSetHomePage: (String) -> Unit,
    onAddPage: () -> Unit,
    onReorderRegularPage: (regularIndex: Int, delta: Int) -> Unit,
    onDeletePage: (String) -> Unit,
) {
    val regularPages = pages.filter { it.kind != PageKind.ALL_APPS }
    val allAppsPage = pages.firstOrNull { it.kind == PageKind.ALL_APPS }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.overlayBackdrop)
            .padding(start = 16.dp, end = 16.dp, top = 44.dp, bottom = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Seitenübersicht", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Sortieren, Startseite festlegen oder neue Seite anlegen", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x29FFFFFF))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Seitenübersicht schließen", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            regularPages.forEachIndexed { regIdx, page ->
                val fullIdx = pages.indexOfFirst { it.id == page.id }
                val isHome = page.id == homePageId
                val isActive = fullIdx == activePageIndex
                val miniLayout = remember(page.root) {
                    TilingEngine.computeLayout(page.root, RectFBounds(4f, 4f, 124f, 224f), 3f)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(page.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (isHome) {
                            Text(
                                text = "Start",
                                color = Color(0xFF0F172A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 132.dp, height = 232.dp)
                            .shadow(12.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x8C0F172A))
                            .border(
                                width = if (isActive) 2.5.dp else 1.5.dp,
                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                                onSelectPage(fullIdx)
                                onClose()
                            },
                    ) {
                        miniLayout.leaves.forEach { leaf ->
                            val surf = theme.getTileSurface(leaf.tile.accentHue, SurfaceEmphasis.NORMAL)
                            Box(
                                modifier = Modifier
                                    .offset(x = leaf.rect.x.dp, y = leaf.rect.y.dp)
                                    .size(width = leaf.rect.width.dp, height = leaf.rect.height.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                    .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(7.dp))
                                    .padding(4.dp),
                            ) {
                                Text(leaf.tile.title, color = surf.textPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OverviewMiniIconBtn(Icons.AutoMirrored.Rounded.ArrowBack, disabled = regIdx == 0, bg = Color(0x29FFFFFF), tint = Color.White) {
                            CanopyHaptics.trigger(HapticKind.TICK, hapticsEnabled)
                            onReorderRegularPage(regIdx, -1)
                        }
                        OverviewMiniIconBtn(
                            icon = Icons.Rounded.Home,
                            disabled = false,
                            bg = if (isHome) Color(0xFF38BDF8) else Color(0x29FFFFFF),
                            tint = if (isHome) Color(0xFF0F172A) else Color.White,
                        ) {
                            CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                            onSetHomePage(page.id)
                        }
                        if (regularPages.size > 1) {
                            OverviewMiniIconBtn(Icons.Rounded.Delete, disabled = false, bg = Color(0x40EF4444), tint = Color(0xFFFCA5A5)) {
                                CanopyHaptics.trigger(HapticKind.REMOVE, hapticsEnabled)
                                onDeletePage(page.id)
                            }
                        }
                        OverviewMiniIconBtn(Icons.AutoMirrored.Rounded.ArrowForward, disabled = regIdx == regularPages.lastIndex, bg = Color(0x29FFFFFF), tint = Color.White) {
                            CanopyHaptics.trigger(HapticKind.TICK, hapticsEnabled)
                            onReorderRegularPage(regIdx, 1)
                        }
                    }
                }
            }

            allAppsPage?.let { allPage ->
                val allIdx = pages.lastIndex
                val miniLayout = remember(allPage.root) {
                    TilingEngine.computeLayout(allPage.root, RectFBounds(4f, 4f, 124f, 224f), 3f)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.alpha(0.9f),
                ) {
                    Text("${allPage.title} (Fix)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .size(width = 132.dp, height = 232.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xA60F172A))
                            .border(
                                width = if (activePageIndex == allIdx) 2.5.dp else 1.5.dp,
                                color = if (activePageIndex == allIdx) Color.White else Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .clickable {
                                onSelectPage(allIdx)
                                onClose()
                            },
                    ) {
                        miniLayout.leaves.forEach { leaf ->
                            val surf = theme.getTileSurface(leaf.tile.accentHue, SurfaceEmphasis.MUTED)
                            Box(
                                modifier = Modifier
                                    .offset(x = leaf.rect.x.dp, y = leaf.rect.y.dp)
                                    .size(width = leaf.rect.width.dp, height = leaf.rect.height.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                    .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(7.dp))
                                    .padding(4.dp),
                            ) {
                                Text(leaf.tile.title, color = surf.textPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                    Text("Immer ganz rechts", color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .defaultMinSize(minHeight = 46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x29FFFFFF))
                .border(1.dp, Color(0x47FFFFFF), RoundedCornerShape(16.dp))
                .clickable {
                    CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                    onAddPage()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Seite hinzufügen", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OverviewMiniIconBtn(
    icon: ImageVector,
    disabled: Boolean,
    bg: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .alpha(if (disabled) 0.3f else 1f)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = !disabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
    }
}

private fun previewSnippetForTile(type: TileType): String = when (type) {
    TileType.CLOCK -> "09:01 · Wecker 07:15"
    TileType.WEATHER -> "7° · Regen ab 17 Uhr"
    TileType.CALENDAR -> "Game Engines, 10:00"
    TileType.MEDIA -> "Lo-fi Mix · Steuerung"
    TileType.NOTE -> "Federwerte & Checkliste"
    TileType.PHOTOS -> "Ken-Burns Galerie"
    TileType.BATTERY -> "84 % · ca. 19 Std."
    TileType.STEPS -> "7.420 Schritte heute"
    TileType.QUICK_TOGGLES -> "WLAN, Bluetooth, Licht, Fokus"
    TileType.REMINDERS -> "3 Aufgaben zum Abhaken"
    else -> "Live-Kachel"
}
