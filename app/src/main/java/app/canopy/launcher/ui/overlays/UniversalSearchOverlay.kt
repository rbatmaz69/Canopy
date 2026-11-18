package app.canopy.launcher.ui.overlays

import android.app.SearchManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.MockCatalogData
import app.canopy.launcher.motion.CanopyHaptics
import app.canopy.launcher.motion.HapticKind
import app.canopy.launcher.ui.components.AppIconRegistry
import app.canopy.launcher.ui.components.AppSquircleIcon
import app.canopy.launcher.ui.theme.SurfaceEmphasis
import app.canopy.launcher.ui.theme.ThemeTokens
import app.canopy.launcher.ui.tiles.AppDefinition
import app.canopy.launcher.ui.tiles.ContactItem
import app.canopy.launcher.ui.tiles.SettingItem
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class WebSearchSpec(
    val intentAction: String,
    val query: String,
    val url: String,
)

data class SearchResults(
    val apps: List<AppDefinition>,
    val contacts: List<ContactItem>,
    val settings: List<SettingItem>,
) {
    val isEmpty: Boolean get() = apps.isEmpty() && contacts.isEmpty() && settings.isEmpty()
}

object UniversalSearchEngine {
    fun filterAll(
        query: String,
        apps: List<AppDefinition> = MockCatalogData.APPS_CATALOG,
        contacts: List<ContactItem> = MockCatalogData.CONTACTS_MOCK,
        settings: List<SettingItem> = MockCatalogData.SETTINGS_MOCK,
    ): SearchResults {
        val q = query.trim().lowercase()
        val matchedApps = if (q.isEmpty()) {
            apps.take(8)
        } else {
            apps.filter { a ->
                a.name.lowercase().contains(q) ||
                    a.category.label.lowercase().contains(q) ||
                    a.subtitle.lowercase().contains(q)
            }.take(12)
        }
        val matchedContacts = if (q.isEmpty()) {
            contacts.take(2)
        } else {
            contacts.filter { c ->
                c.name.lowercase().contains(q) || c.role.lowercase().contains(q)
            }
        }
        val matchedSettings = if (q.isEmpty()) {
            settings.take(2)
        } else {
            settings.filter { s ->
                s.name.lowercase().contains(q) || s.detail.lowercase().contains(q)
            }
        }
        return SearchResults(matchedApps, matchedContacts, matchedSettings)
    }

    fun shouldShowBrowserCard(rawQuery: String): Boolean = rawQuery.trim().isNotEmpty()

    fun resolveWebSearchQuery(rawQuery: String): String =
        rawQuery.trim().ifEmpty { "Canopy Android Launcher" }

    fun buildWebSearchUrl(rawQuery: String): String {
        val encoded = URLEncoder.encode(resolveWebSearchQuery(rawQuery), StandardCharsets.UTF_8.name())
        return "https://www.google.com/search?q=$encoded"
    }

    fun createWebSearchSpec(rawQuery: String): WebSearchSpec {
        val resolved = resolveWebSearchQuery(rawQuery)
        return WebSearchSpec(
            intentAction = Intent.ACTION_WEB_SEARCH,
            query = resolved,
            url = buildWebSearchUrl(resolved),
        )
    }

    fun buildBrowserSearchIntent(rawQuery: String): Intent {
        val spec = createWebSearchSpec(rawQuery)
        return Intent(spec.intentAction).apply {
            putExtra(SearchManager.QUERY, spec.query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

@Composable
fun UniversalSearchOverlay(
    theme: ThemeTokens,
    hapticsEnabled: Boolean,
    allAppsCatalog: List<AppDefinition> = MockCatalogData.APPS_CATALOG,
    contactsCatalog: List<ContactItem> = MockCatalogData.CONTACTS_MOCK,
    onClose: () -> Unit,
    onOpenApp: (appId: String, sourceRect: Rect?) -> Unit,
    onSearchWeb: (query: String, sourceRect: Rect?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val results = remember(query, allAppsCatalog, contactsCatalog) {
        UniversalSearchEngine.filterAll(query, apps = allAppsCatalog, contacts = contactsCatalog)
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val inputBg = if (theme.mode == ColorMode.LIGHT && !theme.isGlass) Color(0x0D000000) else Color(0x17FFFFFF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.sheetBg)
            .padding(start = 14.dp, end = 14.dp, top = 44.dp, bottom = 20.dp),
    ) {
        // Search Input Bar (IME_ACTION_SEARCH + hardware Enter support)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(inputBg)
                .border(1.dp, theme.sheetBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = theme.textSecondary, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = "Apps, Kontakte oder Web-Suche (Enter)…",
                        color = theme.textMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = theme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(theme.pillActive),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                            onSearchWeb(UniversalSearchEngine.resolveWebSearchQuery(query), null)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Enter) {
                                CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                                onSearchWeb(UniversalSearchEngine.resolveWebSearchQuery(query), null)
                                true
                            } else {
                                false
                            }
                        },
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable {
                        CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                        if (query.isNotEmpty()) query = "" else onClose()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Suche schließen", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        // Scrollable Results Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (UniversalSearchEngine.shouldShowBrowserCard(query)) {
                val webSurf = theme.getTileSurface(199f, SurfaceEmphasis.NORMAL)
                var cardBounds: Rect? = null
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WEB-SUCHE", color = theme.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(webSurf.backgroundStart, webSurf.backgroundEnd)))
                            .border(webSurf.borderWidthDp.dp, webSurf.borderColor, RoundedCornerShape(18.dp))
                            .onGloballyPositioned { cardBounds = it.boundsInRoot() }
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                                onSearchWeb(trimmedQuery, cardBounds)
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AppSquircleIcon(appId = "app-browser", iconKey = "Compass", color = Color(0xFF0EA5E9), size = 38.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Im Browser suchen: „$trimmedQuery“",
                                    color = webSurf.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text("Netz · Web-Suche öffnen", color = webSurf.textSecondary, fontSize = 11.sp)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(webSurf.accentBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardReturn, contentDescription = null, tint = webSurf.accentColor, modifier = Modifier.size(12.dp))
                            Text("Enter", color = webSurf.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (results.apps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("APPS", color = theme.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    results.apps.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { app ->
                                val surf = theme.getTileSurface(app.hue, SurfaceEmphasis.NORMAL)
                                var cardRect: Rect? = null
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 60.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                        .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(18.dp))
                                        .onGloballyPositioned { cardRect = it.boundsInRoot() }
                                        .clickable {
                                            CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                                            onOpenApp(app.id, cardRect)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    AppSquircleIcon(appId = app.id, iconKey = app.iconKey, color = app.color, size = 38.dp, badge = app.badge)
                                    Column(Modifier.weight(1f)) {
                                        Text(app.name, color = surf.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(app.category.label, color = surf.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (results.contacts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KONTAKTE", color = theme.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    results.contacts.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { c ->
                                val surf = theme.getTileSurface(c.hue, SurfaceEmphasis.MUTED)
                                var cardRect: Rect? = null
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                        .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(18.dp))
                                        .onGloballyPositioned { cardRect = it.boundsInRoot() }
                                        .clickable {
                                            CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                                            onOpenApp("app-telefon", cardRect)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(surf.accentBg),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Person, contentDescription = null, tint = surf.accentColor, modifier = Modifier.size(18.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(c.name, color = surf.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(c.role, color = surf.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (results.settings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("EINSTELLUNGEN", color = theme.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    results.settings.forEach { s ->
                        val surf = theme.getTileSurface(s.hue, SurfaceEmphasis.MUTED)
                        var cardRect: Rect? = null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(listOf(surf.backgroundStart, surf.backgroundEnd)))
                                .border(surf.borderWidthDp.dp, surf.borderColor, RoundedCornerShape(18.dp))
                                .onGloballyPositioned { cardRect = it.boundsInRoot() }
                                .clickable {
                                    CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                                    onOpenApp("app-einstellungen", cardRect)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(surf.accentBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(AppIconRegistry.resolveIcon(s.iconKey), contentDescription = null, tint = surf.accentColor, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(s.name, color = surf.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                Text(s.detail, color = surf.textSecondary, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }

            if (results.isEmpty) {
                Text(
                    text = "Keine lokalen Treffer für „$query“ – oben tippen oder Enter für Web-Suche.",
                    color = theme.textMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, theme.sheetBorder, RoundedCornerShape(14.dp))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Text("Schließen", color = theme.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
