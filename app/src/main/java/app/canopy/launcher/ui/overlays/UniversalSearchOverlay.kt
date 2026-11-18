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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

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
    /** Lowercase without accents, so "uber" finds "Über" and "e" finds "é". */
    fun normalize(text: String): String =
        java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .trim()

    private val wordSplit = Regex("[\\s._\\-]+")

    /** Relevance of [name] for the normalized query [q]; 0 means no match. */
    fun scoreName(name: String, q: String): Int {
        if (q.isEmpty()) return 0
        val n = normalize(name)
        val words = n.split(wordSplit).filter { it.isNotEmpty() }
        val initials = words.joinToString("") { it.take(1) }
        return when {
            n == q -> 120
            n.startsWith(q) -> 100
            words.any { it.startsWith(q) } -> 85
            q.length >= 2 && initials.startsWith(q) -> 75
            n.contains(q) -> 60
            q.length >= 3 && isSubsequence(q, n) -> 30
            else -> 0
        }
    }

    private fun isSubsequence(q: String, text: String): Boolean {
        var i = 0
        for (ch in text) {
            if (i < q.length && ch == q[i]) i++
        }
        return i == q.length
    }

    fun rankApps(apps: List<AppDefinition>, query: String, limit: Int = 12): List<AppDefinition> {
        val q = normalize(query)
        if (q.isEmpty()) return apps.sortedByDescending { it.usageScore }.take(8)
        return apps.mapNotNull { app ->
            val byName = scoreName(app.name, q)
            val byCategory = if (normalize(app.category.label).startsWith(q)) 15 else 0
            val score = maxOf(byName, byCategory)
            if (score == 0) null else app to (score * 1000 + app.usageScore.coerceIn(0, 999))
        }
            .sortedWith(compareByDescending<Pair<AppDefinition, Int>> { it.second }.thenBy { it.first.name.lowercase(Locale.ROOT) })
            .take(limit)
            .map { it.first }
    }

    fun filterAll(
        query: String,
        apps: List<AppDefinition> = MockCatalogData.APPS_CATALOG,
        contacts: List<ContactItem> = MockCatalogData.CONTACTS_MOCK,
        settings: List<SettingItem> = MockCatalogData.SETTINGS_MOCK,
    ): SearchResults {
        val q = normalize(query)
        val matchedApps = rankApps(apps, query)
        if (q.isEmpty()) return SearchResults(matchedApps, emptyList(), emptyList())
        val matchedContacts = contacts
            .mapNotNull { c -> scoreName(c.name, q).takeIf { it > 0 }?.let { c to it } }
            .sortedByDescending { it.second }
            .take(6)
            .map { it.first }
        val matchedSettings = settings
            .mapNotNull { s ->
                val score = maxOf(scoreName(s.name, q), if (normalize(s.detail).contains(q)) 40 else 0)
                if (score > 0) s to score else null
            }
            .sortedByDescending { it.second }
            .take(4)
            .map { it.first }
        return SearchResults(matchedApps, matchedContacts, matchedSettings)
    }

    sealed interface EnterAction {
        data class OpenApp(val appId: String) : EnterAction
        data class WebSearch(val query: String) : EnterAction
        data object Nothing : EnterAction
    }

    /** Enter opens the best local app match; only without one does it search the web. */
    fun resolveEnterAction(query: String, results: SearchResults): EnterAction {
        if (query.isBlank()) return EnterAction.Nothing
        results.apps.firstOrNull()?.let { return EnterAction.OpenApp(it.id) }
        return EnterAction.WebSearch(query.trim())
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

    fun buildContactDialUri(statusOrPhone: String): String? {
        val trimmed = statusOrPhone.trim()
        if (trimmed.isEmpty()) return null
        val normalized = buildString {
            trimmed.forEach { ch ->
                if (ch.isDigit() || (ch == '+' && isEmpty())) append(ch)
            }
        }
        val digitCount = normalized.count { it.isDigit() }
        return if (digitCount >= 5) "tel:$normalized" else null
    }

    fun resolveSettingIntentAction(item: SettingItem): String =
        item.intentAction.ifBlank { android.provider.Settings.ACTION_SETTINGS }

    fun evaluateMathExpression(rawQuery: String): String? {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return null
        val percentNormalized = Regex("""(\d+(?:[.,]\d+)?)\s*%\s*(?:von|of)\s*""", RegexOption.IGNORE_CASE)
            .replace(trimmed) { mr -> "(${mr.groupValues[1]}/100)*" }
        val normalized = percentNormalized
            .replace(',', '.')
            .replace('×', '*')
            .replace('÷', '/')
            .replace(" ", "")
        if (!normalized.any { it in setOf('+', '-', '*', '/') }) return null
        if (normalized.any { !it.isDigit() && it !in setOf('.', '+', '-', '*', '/', '(', ')') }) return null

        val result = MathExprParser(normalized).parse() ?: return null
        if (!result.isFinite() || abs(result) > 1e15) return null
        val roundedZero = if (abs(result) < 1e-10) 0.0 else result
        val fmt = DecimalFormat("0.####", DecimalFormatSymbols(Locale.GERMANY))
        return fmt.format(roundedZero)
    }

    private class MathExprParser(private val s: String) {
        private var pos = 0

        fun parse(): Double? {
            val value = parseExpression() ?: return null
            return if (pos == s.length) value else null
        }

        private fun parseExpression(): Double? {
            var left = parseTerm() ?: return null
            while (pos < s.length) {
                when (s[pos]) {
                    '+' -> {
                        pos++
                        left += parseTerm() ?: return null
                    }
                    '-' -> {
                        pos++
                        left -= parseTerm() ?: return null
                    }
                    else -> break
                }
            }
            return left
        }

        private fun parseTerm(): Double? {
            var left = parseFactor() ?: return null
            while (pos < s.length) {
                when (s[pos]) {
                    '*' -> {
                        pos++
                        left *= parseFactor() ?: return null
                    }
                    '/' -> {
                        pos++
                        val rhs = parseFactor() ?: return null
                        if (abs(rhs) < 1e-12) return null
                        left /= rhs
                    }
                    else -> break
                }
            }
            return left
        }

        private fun parseFactor(): Double? {
            if (pos >= s.length) return null
            return when (s[pos]) {
                '+' -> {
                    pos++
                    parseFactor()
                }
                '-' -> {
                    pos++
                    parseFactor()?.let { -it }
                }
                '(' -> {
                    pos++
                    val inside = parseExpression() ?: return null
                    if (pos >= s.length || s[pos] != ')') return null
                    pos++
                    inside
                }
                else -> parseNumber()
            }
        }

        private fun parseNumber(): Double? {
            val start = pos
            var dotSeen = false
            while (pos < s.length) {
                val ch = s[pos]
                if (ch.isDigit()) {
                    pos++
                } else if (ch == '.' && !dotSeen) {
                    dotSeen = true
                    pos++
                } else {
                    break
                }
            }
            if (pos == start) return null
            return s.substring(start, pos).toDoubleOrNull()
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
    onDialContact: (contact: ContactItem, sourceRect: Rect?) -> Unit = { _, r -> onOpenApp("app-telefon", r) },
    onOpenSetting: (setting: SettingItem, sourceRect: Rect?) -> Unit = { _, r -> onOpenApp("app-einstellungen", r) },
    onSearchWeb: (query: String, sourceRect: Rect?) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedCalc by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val mathResult = remember(query) { UniversalSearchEngine.evaluateMathExpression(query) }
    val results = remember(query, allAppsCatalog, contactsCatalog) {
        UniversalSearchEngine.filterAll(query, apps = allAppsCatalog, contacts = contactsCatalog)
    }
    val onEnter: () -> Unit = {
        when (val action = UniversalSearchEngine.resolveEnterAction(query, results)) {
            is UniversalSearchEngine.EnterAction.OpenApp -> {
                CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                onOpenApp(action.appId, null)
            }
            is UniversalSearchEngine.EnterAction.WebSearch -> {
                CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                onSearchWeb(action.query, null)
            }
            UniversalSearchEngine.EnterAction.Nothing -> Unit
        }
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
    ) {

        // Scrollable Results Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), reverseScrolling = true),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom),
        ) {
            if (mathResult != null) {
                val calcSurf = theme.getTileSurface(160f, SurfaceEmphasis.NORMAL)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SOFORT-RECHNER", color = theme.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(calcSurf.backgroundStart, calcSurf.backgroundEnd)))
                            .border(calcSurf.borderWidthDp.dp, calcSurf.borderColor, RoundedCornerShape(18.dp))
                            .clickable {
                                CanopyHaptics.trigger(HapticKind.SNAP, hapticsEnabled)
                                clipboardManager.setText(AnnotatedString(mathResult))
                                copiedCalc = true
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(calcSurf.accentBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Calculate, contentDescription = null, tint = calcSurf.accentColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("= $mathResult", color = calcSurf.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (copiedCalc) "Ergebnis kopiert ($trimmedQuery)" else "$trimmedQuery · Tippen zum Kopieren",
                                    color = calcSurf.textSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Ergebnis kopieren", tint = calcSurf.accentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }

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
                                            onDialContact(c, cardRect)
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
                                    onOpenSetting(s, cardRect)
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
                    text = "Keine Treffer für „$query“ – Enter sucht im Web.",
                    color = theme.textMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        // Search field at the bottom: in thumb reach and right above the keyboard.
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
                        text = "Apps, Kontakte, Einstellungen …",
                        color = theme.textMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        copiedCalc = false
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = theme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(theme.pillActive),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onEnter() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Enter) {
                                onEnter()
                                true
                            } else {
                                false
                            }
                        },
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        CanopyHaptics.trigger(HapticKind.PRESS, hapticsEnabled)
                        if (query.isNotEmpty()) query = "" else onClose()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = if (query.isNotEmpty()) "Eingabe löschen" else "Suche schließen", tint = theme.textSecondary, modifier = Modifier.size(20.dp))
            }
        }

    }
}
