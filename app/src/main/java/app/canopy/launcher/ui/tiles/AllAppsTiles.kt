package app.canopy.launcher.ui.tiles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.ui.components.AppSquircleIcon
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import kotlin.math.floor

enum class AllAppsSortMode(val label: String) {
    A_Z("A–Z"),
    CATEGORY("Kategorie"),
    COLOR("Farbe"),
    USAGE("Nutzung");

    fun next(): AllAppsSortMode = entries[(ordinal + 1) % entries.size]
}

data class AppSection(
    val label: String,
    val apps: List<AppDefinition>,
)

object AllAppsCatalogSorter {
    private val germanCollator: Collator = Collator.getInstance(Locale.GERMAN).apply {
        strength = Collator.PRIMARY
    }

    fun groupApps(apps: List<AppDefinition>, sortMode: AllAppsSortMode): List<AppSection> {
        return when (sortMode) {
            AllAppsSortMode.A_Z -> {
                val sorted = apps.sortedWith { a, b -> germanCollator.compare(a.name, b.name) }
                val map = linkedMapOf<String, MutableList<AppDefinition>>()
                for (app in sorted) {
                    val ch = app.name.firstOrNull()?.uppercaseChar() ?: '#'
                    val key = if (ch in 'A'..'Z' || ch in listOf('Ä', 'Ö', 'Ü')) ch.toString() else "#"
                    map.getOrPut(key) { mutableListOf() }.add(app)
                }
                map.map { (label, list) -> AppSection(label, list) }
            }

            AllAppsSortMode.CATEGORY -> {
                val sorted = apps.sortedWith { a, b ->
                    val catCmp = germanCollator.compare(a.category.label, b.category.label)
                    if (catCmp != 0) catCmp else germanCollator.compare(a.name, b.name)
                }
                val map = linkedMapOf<String, MutableList<AppDefinition>>()
                for (app in sorted) {
                    map.getOrPut(app.category.label) { mutableListOf() }.add(app)
                }
                map.map { (label, list) -> AppSection(label, list) }
            }

            AllAppsSortMode.COLOR -> {
                val sorted = apps.sortedBy { it.hue }
                listOf(AppSection("Nach Farbspektrum", sorted))
            }

            AllAppsSortMode.USAGE -> {
                val sorted = apps.sortedByDescending { it.usageScore }
                listOf(AppSection("Meistgenutzt", sorted))
            }
        }
    }

    fun computeSectionHeaderIndices(sections: List<AppSection>): Map<String, Int> {
        val result = linkedMapOf<String, Int>()
        var gridItemIndex = 0
        for (section in sections) {
            result[section.label] = gridItemIndex
            gridItemIndex += 1 + section.apps.size
        }
        return result
    }

    fun letterForDragRatio(letters: List<String>, yPx: Float, heightPx: Float): String? {
        if (letters.isEmpty() || heightPx <= 1f) return null
        val ratio = (yPx / heightPx).coerceIn(0f, 0.999f)
        val idx = floor(ratio * letters.size).toInt().coerceIn(0, letters.lastIndex)
        return letters[idx]
    }
}

@Composable
fun SearchBarTile(props: TileContentProps) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = !props.isEditMode) { props.onOpenSearch() }
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(18.dp))
            Text(
                text = "Apps, Kontakte & Einstellungen suchen…",
                color = props.surface.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "Suche",
            color = props.surface.accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(CircleShape)
                .background(props.surface.accentBg)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun NewlyInstalledTile(props: TileContentProps) {
    val appsById = props.allAppsCatalog.associateBy { it.id }.ifEmpty { MockCatalogData.APPS_BY_ID }
    val appIds = props.tile.newAppIds ?: listOf("app-skizze", "app-fokus", "app-klima")
    val apps = appIds.mapNotNull { appsById[it] }

    if (props.tier == TileSizeTier.XS) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(20.dp))
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(13.dp))
                Text("Neu installiert", color = props.surface.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
            Text("${apps.size} Apps", color = props.surface.textSecondary, fontSize = 11.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEach { app ->
                var itemRect: Rect? = null
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .onGloballyPositioned { itemRect = it.boundsInRoot() }
                        .clickable(enabled = !props.isEditMode) { props.onOpenApp(app.id, itemRect) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSquircleIcon(appId = app.id, iconKey = app.iconKey, color = app.color, size = 38.dp, isNew = true)
                    Text(app.name, color = props.surface.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsBoxTile(
    props: TileContentProps,
    onLetterTick: (String) -> Unit = {},
) {
    val catalog = props.allAppsCatalog.ifEmpty { MockCatalogData.APPS_CATALOG }
    var sortMode by remember { mutableStateOf(AllAppsSortMode.A_Z) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var lastHapticLetter by remember { mutableStateOf<String?>(null) }
    var railHeightPx by remember { mutableFloatStateOf(1f) }

    val sections = remember(catalog, sortMode) {
        AllAppsCatalogSorter.groupApps(catalog, sortMode)
    }
    val letters = remember(sections, sortMode) {
        if (sortMode == AllAppsSortMode.A_Z) sections.map { it.label } else emptyList()
    }
    val headerIndices = remember(sections) {
        AllAppsCatalogSorter.computeSectionHeaderIndices(sections)
    }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val jumpToLetter: (String) -> Unit = { letter ->
        activeLetter = letter
        if (lastHapticLetter != letter) {
            lastHapticLetter = letter
            onLetterTick(letter)
        }
        val targetIndex = headerIndices[letter]
        if (targetIndex != null) {
            scope.launch {
                gridState.scrollToItem(targetIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Alle Apps", color = props.surface.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = catalog.size.toString(),
                    color = props.surface.textSecondary,
                    style = TextStyle(fontSize = 11.sp, fontFeatureSettings = "tnum"),
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(props.surface.accentBg)
                    .clickable(enabled = !props.isEditMode) { sortMode = sortMode.next() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = props.surface.accentColor, modifier = Modifier.size(13.dp))
                Text(sortMode.label, color = props.surface.accentColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (letters.isNotEmpty()) 24.dp else 0.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sections.forEach { section ->
                    item(
                        key = "hdr-${section.label}",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Text(
                            text = section.label,
                            color = props.surface.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                    items(
                        items = section.apps,
                        key = { "${section.label}-${it.id}" },
                    ) { app ->
                        var itemRect: Rect? = null
                        Column(
                            modifier = Modifier
                                .animateItem()
                                .defaultMinSize(minHeight = 44.dp)
                                .onGloballyPositioned { itemRect = it.boundsInRoot() }
                                .clickable(enabled = !props.isEditMode) { props.onOpenApp(app.id, itemRect) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AppSquircleIcon(
                                appId = app.id,
                                iconKey = app.iconKey,
                                color = app.color,
                                size = 44.dp,
                                badge = app.badge,
                                isNew = app.isNew,
                            )
                            Text(
                                text = app.name,
                                color = props.surface.textSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            if (letters.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .width(22.dp)
                        .clip(CircleShape)
                        .background(props.surface.accentBg)
                        .onSizeChanged { railHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                        .pointerInput(letters) {
                            detectTapGestures { offset ->
                                AllAppsCatalogSorter.letterForDragRatio(letters, offset.y, railHeightPx)
                                    ?.let(jumpToLetter)
                                activeLetter = null
                                lastHapticLetter = null
                            }
                        }
                        .pointerInput(letters) {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    AllAppsCatalogSorter.letterForDragRatio(letters, offset.y, railHeightPx)
                                        ?.let(jumpToLetter)
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    AllAppsCatalogSorter.letterForDragRatio(letters, change.position.y, railHeightPx)
                                        ?.let(jumpToLetter)
                                },
                                onDragEnd = {
                                    activeLetter = null
                                    lastHapticLetter = null
                                },
                                onDragCancel = {
                                    activeLetter = null
                                    lastHapticLetter = null
                                },
                            )
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    letters.forEach { letter ->
                        val isActive = activeLetter == letter
                        Text(
                            text = letter,
                            color = if (isActive) props.surface.accentColor else props.surface.textSecondary,
                            fontSize = 9.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (activeLetter != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 30.dp)
                        .size(48.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(props.surface.accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = activeLetter ?: "",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
