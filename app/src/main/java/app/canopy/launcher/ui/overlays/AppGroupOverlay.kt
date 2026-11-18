package app.canopy.launcher.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.layout.TileData
import app.canopy.launcher.ui.components.AppSquircleIcon
import app.canopy.launcher.ui.theme.CanopyThemes
import app.canopy.launcher.ui.theme.SurfaceEmphasis
import app.canopy.launcher.ui.theme.ThemeTokens
import app.canopy.launcher.ui.tiles.AppDefinition

enum class GroupOverlayMode {
    VIEW,
    RENAME,
    MANAGE,
}

object AppGroupEditorActions {
    val GROUP_ACCENT_SWATCH_HUES: List<Float> = listOf(215f, 160f, 38f, 271f, 330f, 14f)

    fun renameGroup(tile: TileData, draftTitle: String): TileData {
        val clean = draftTitle.trim()
        return if (clean.isEmpty()) tile else tile.copy(title = clean)
    }

    fun toggleAppInGroup(currentIds: List<String>, appId: String): List<String> {
        val exists = appId in currentIds
        if (exists && currentIds.size <= 1) {
            return currentIds
        }
        return if (exists) currentIds.filter { it != appId } else currentIds + appId
    }

    fun moveAppInGroup(currentIds: List<String>, appId: String, delta: Int): List<String> {
        val idx = currentIds.indexOf(appId)
        val nextIdx = idx + delta
        if (idx !in currentIds.indices || nextIdx !in currentIds.indices) return currentIds
        val mutable = currentIds.toMutableList()
        val moved = mutable.removeAt(idx)
        mutable.add(nextIdx, moved)
        return mutable
    }

    fun updateGroupAccentHue(tile: TileData, hue: Float): TileData =
        tile.copy(accentHue = ((hue % 360f) + 360f) % 360f)
}

@Composable
fun AppGroupOverlay(
    tile: TileData,
    initialMode: GroupOverlayMode = GroupOverlayMode.VIEW,
    theme: ThemeTokens,
    allAppsCatalog: List<AppDefinition> = MockCatalogData.APPS_CATALOG,
    onClose: () -> Unit,
    onOpenApp: (String, Rect?) -> Unit,
    onUpdateGroup: ((TileData) -> TileData) -> Unit,
) {
    var isRenaming by remember(tile.id, initialMode) { mutableStateOf(initialMode == GroupOverlayMode.RENAME) }
    var isManaging by remember(tile.id, initialMode) { mutableStateOf(initialMode == GroupOverlayMode.MANAGE) }
    var titleDraft by remember(tile.title) { mutableStateOf(tile.title) }

    val appsById = remember(allAppsCatalog) { allAppsCatalog.associateBy { it.id } }
    val currentIds = tile.appIds ?: emptyList()
    val apps = currentIds.mapNotNull { appsById[it] }
    val surface = theme.getTileSurface(tile.accentHue, SurfaceEmphasis.HERO)

    val commitRename = {
        onUpdateGroup { prev -> AppGroupEditorActions.renameGroup(prev, titleDraft) }
        isRenaming = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.overlayBackdrop)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() }
            .padding(horizontal = 14.dp, vertical = 48.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (theme.isGlass) {
                        Brush.linearGradient(listOf(Color(0xD91A2036), Color(0xD91A2036)))
                    } else {
                        Brush.linearGradient(listOf(surface.backgroundStart, surface.backgroundEnd))
                    }
                )
                .border(surface.borderWidthDp.dp, surface.borderColor, RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* consume backdrop click */ }
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    if (isRenaming) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            OutlinedTextField(
                                value = titleDraft,
                                onValueChange = { titleDraft = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = surface.textPrimary,
                                    unfocusedTextColor = surface.textPrimary,
                                    focusedBorderColor = surface.accentColor,
                                ),
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(surface.accentColor)
                                    .clickable { commitRename() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = "Speichern", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.clickable { isRenaming = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = tile.title,
                                color = surface.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(Icons.Rounded.Edit, contentDescription = "Gruppenname ändern", tint = surface.textPrimary.copy(alpha = 0.72f), modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isManaging) "${apps.size} ausgewählt · Tippen zum Hinzufügen/Entfernen" else "${apps.size} Apps in dieser Gruppe",
                        color = surface.textSecondary,
                        fontSize = 12.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(if (isManaging) surface.accentColor else surface.accentBg)
                            .clickable { isManaging = !isManaging }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (isManaging) Icons.Rounded.Check else Icons.Rounded.Add,
                            contentDescription = "Apps verwalten",
                            tint = if (isManaging) Color.White else surface.textPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (isManaging) "Fertig" else "Apps +/−",
                            color = if (isManaging) Color.White else surface.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(surface.accentBg)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Gruppe schließen", tint = surface.textPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (isManaging) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Akzent:", color = surface.textSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    AppGroupEditorActions.GROUP_ACCENT_SWATCH_HUES.forEach { swatchHue ->
                        val activeHue = tile.accentHue == swatchHue
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(CanopyThemes.hslColor(swatchHue, 78f, 54f))
                                .border(
                                    width = if (activeHue) 2.dp else 1.dp,
                                    color = if (activeHue) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    onUpdateGroup { prev -> AppGroupEditorActions.updateGroupAccentHue(prev, swatchHue) }
                                }
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isManaging) {
                    items(allAppsCatalog, key = { it.id }) { app ->
                        val selected = app.id in currentIds
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) surface.accentBg else Color.Transparent)
                                .border(
                                    width = 1.5.dp,
                                    color = if (selected) surface.accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .alpha(if (selected) 1f else 0.62f)
                                .clickable {
                                    onUpdateGroup { prev ->
                                        prev.copy(
                                            appIds = AppGroupEditorActions.toggleAppInGroup(
                                                prev.appIds ?: emptyList(),
                                                app.id,
                                            )
                                        )
                                    }
                                }
                                .padding(horizontal = 2.dp, vertical = 6.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                AppSquircleIcon(appId = app.id, iconKey = app.iconKey, color = app.color, size = 46.dp)
                                Text(app.name, color = surface.textPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                if (selected && currentIds.size > 1) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "←",
                                            color = surface.accentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                onUpdateGroup { prev ->
                                                    prev.copy(appIds = AppGroupEditorActions.moveAppInGroup(prev.appIds ?: emptyList(), app.id, -1))
                                                }
                                            },
                                        )
                                        Text(
                                            text = "→",
                                            color = surface.accentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                onUpdateGroup { prev ->
                                                    prev.copy(appIds = AppGroupEditorActions.moveAppInGroup(prev.appIds ?: emptyList(), app.id, 1))
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 2.dp, end = 4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(surface.accentColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                } else {
                    items(apps, key = { it.id }) { app ->
                        var itemRect: Rect? = null
                        Column(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .onGloballyPositioned { itemRect = it.boundsInRoot() }
                                .clickable { onOpenApp(app.id, itemRect) }
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AppSquircleIcon(
                                appId = app.id,
                                iconKey = app.iconKey,
                                color = app.color,
                                size = 52.dp,
                                badge = app.badge,
                                isNew = app.isNew,
                            )
                            Text(
                                text = app.name,
                                color = surface.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
