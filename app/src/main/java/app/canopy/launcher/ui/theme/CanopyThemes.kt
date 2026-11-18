package app.canopy.launcher.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import app.canopy.launcher.data.ColorMode
import app.canopy.launcher.data.ThemeVariantId
import app.canopy.launcher.data.WallpaperPresetId
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SurfaceEmphasis {
    NORMAL,
    HERO,
    MUTED,
}

data class WallpaperPresetMeta(
    val id: WallpaperPresetId,
    val label: String,
    val previewColor: Color,
    val baseHue: Float,
)

@Immutable
data class TileSurfaceTokens(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val borderColor: Color,
    val borderWidthDp: Float,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentColor: Color,
    val accentBg: Color,
    val shadowColor: Color,
    val specularHighlightAlpha: Float,
)

@Immutable
data class DockCapsuleTokens(
    val dockBg: Color,
    val activePillBg: Color,
    val activeTextColor: Color,
)

@Immutable
data class ThemeTokens(
    val id: ThemeVariantId,
    val mode: ColorMode,
    val wallpaperId: WallpaperPresetId,
    val name: String,
    val description: String,
    val tileRadiusDp: Float,
    val edgePaddingDp: Float,
    val gapUseDp: Float,
    val gapEditDp: Float,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val statusText: Color,
    val sheetBg: Color,
    val sheetBorder: Color,
    val overlayBackdrop: Color,
    val pillBg: Color,
    val pillActive: Color,
    val dividerHandleBg: Color,
    val dividerHandleDot: Color,
    val isGlass: Boolean,
    val isBold: Boolean,
    val blurDp: Float,
    private val surfaceFactory: (accentHue: Float?, emphasis: SurfaceEmphasis) -> TileSurfaceTokens,
) {
    fun getTileSurface(
        accentHue: Float? = null,
        emphasis: SurfaceEmphasis = SurfaceEmphasis.NORMAL,
    ): TileSurfaceTokens = surfaceFactory(accentHue, emphasis)
}

object CanopyThemes {
    val WALLPAPER_PRESETS: List<WallpaperPresetMeta> = listOf(
        WallpaperPresetMeta(WallpaperPresetId.SAND, "Sand & Bernstein", Color(0xFFD97706), 28f),
        WallpaperPresetMeta(WallpaperPresetId.EMERALD, "Nordlicht", Color(0xFF10B981), 158f),
        WallpaperPresetMeta(WallpaperPresetId.COBALT, "Kobalt", Color(0xFF3B82F6), 220f),
        WallpaperPresetMeta(WallpaperPresetId.TERRACOTTA, "Terrakotta", Color(0xFFF43F5E), 12f),
    )

    private data class WallpaperAccentSpec(
        val baseHue: Float,
        val pillDark: Color,
        val pillLight: Color,
        val dividerDark: Color,
        val dividerLight: Color,
    )

    private val WALLPAPER_ACCENTS = mapOf(
        WallpaperPresetId.SAND to WallpaperAccentSpec(
            baseHue = 34f,
            pillDark = Color(0xFFF5E3C8),
            pillLight = Color(0xFF2C2217),
            dividerDark = Color(0xFFF2DFC6),
            dividerLight = Color(0xFF3A2E20),
        ),
        WallpaperPresetId.EMERALD to WallpaperAccentSpec(
            baseHue = 158f,
            pillDark = Color(0xFFA7F3D0),
            pillLight = Color(0xFF064E3B),
            dividerDark = Color(0xFFA7F3D0),
            dividerLight = Color(0xFF065F46),
        ),
        WallpaperPresetId.COBALT to WallpaperAccentSpec(
            baseHue = 220f,
            pillDark = Color(0xFFBFDBFE),
            pillLight = Color(0xFF1E3A8A),
            dividerDark = Color(0xFFBFDBFE),
            dividerLight = Color(0xFF1E40AF),
        ),
        WallpaperPresetId.TERRACOTTA to WallpaperAccentSpec(
            baseHue = 12f,
            pillDark = Color(0xFFFECDD3),
            pillLight = Color(0xFF7F1D1D),
            dividerDark = Color(0xFFFECDD3),
            dividerLight = Color(0xFF991B1B),
        ),
    )

    fun clampHue(hue: Float?, fallback: Float = 215f): Float {
        if (hue == null || hue.isNaN()) return fallback
        return ((hue % 360f) + 360f) % 360f
    }

    /**
     * Pure Kotlin RGB (0..255) -> Hue (0..360) conversion for Material You wallpaper extraction.
     */
    fun rgbToHue(r: Int, g: Int, b: Int): Float {
        val rf = r.coerceIn(0, 255) / 255f
        val gf = g.coerceIn(0, 255) / 255f
        val bf = b.coerceIn(0, 255) / 255f
        val maxC = maxOf(rf, gf, bf)
        val minC = minOf(rf, gf, bf)
        val delta = maxC - minC
        if (delta < 1e-4f) return 215f
        val rawHue = when (maxC) {
            rf -> ((gf - bf) / delta) % 6f
            gf -> ((bf - rf) / delta) + 2f
            else -> ((rf - gf) / delta) + 4f
        } * 60f
        return clampHue(rawHue, 215f)
    }

    /**
     * Pure Kotlin HSL -> Compose Color conversion (independent of android.graphics.Color for unit tests).
     */
    fun hslColor(hDeg: Float, sPct: Float, lPct: Float, alpha: Float = 1f): Color {
        val h = clampHue(hDeg, 0f) / 360f
        val s = (sPct / 100f).coerceIn(0f, 1f)
        val l = (lPct / 100f).coerceIn(0f, 1f)

        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h * 6f) % 2f - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when ((h * 6f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f),
        )
    }

    fun getThemeTokens(
        id: ThemeVariantId,
        mode: ColorMode,
        wallpaperId: WallpaperPresetId = WallpaperPresetId.SAND,
        overrideBaseHue: Float? = null,
    ): ThemeTokens {
        val wpAccent = WALLPAPER_ACCENTS[wallpaperId] ?: WALLPAPER_ACCENTS.getValue(WallpaperPresetId.SAND)
        val effectiveBaseHue = overrideBaseHue?.let { clampHue(it, wpAccent.baseHue) } ?: wpAccent.baseHue
        val hasCustomBaseHue = overrideBaseHue != null

        return when (id) {
            ThemeVariantId.MATERIAL_YOU -> {
                val isDark = mode == ColorMode.DARK
                ThemeTokens(
                    id = id,
                    mode = mode,
                    wallpaperId = wallpaperId,
                    name = "Material You",
                    description = "Tonale Palette, weiche Formen, ruhige Typografie",
                    tileRadiusDp = 22f,
                    edgePaddingDp = 8f,
                    gapUseDp = 6f,
                    gapEditDp = 12f,
                    textPrimary = if (isDark) Color(0xFFF2ECE4) else Color(0xFF1E1B16),
                    textSecondary = if (isDark) Color(0xFFCBC2B4) else Color(0xFF534A3F),
                    textMuted = if (isDark) Color(0xFF9A9185) else Color(0xFF7C7265),
                    statusText = if (isDark) Color(0xFFF5EFE6) else Color(0xFF26211A),
                    sheetBg = if (isDark) Color(0xF5221E1A) else Color(0xF5FAF5EE),
                    sheetBorder = if (isDark) Color(0x1FFFF0DC) else Color(0x1A463422),
                    overlayBackdrop = if (isDark) Color(0xA60F0D0A) else Color(0x592A2218),
                    pillBg = if (isDark) Color(0x47FFF4E4) else Color(0x4030261C),
                    pillActive = when {
                        hasCustomBaseHue -> if (isDark) hslColor(effectiveBaseHue, 70f, 82f) else hslColor(effectiveBaseHue, 65f, 22f)
                        isDark -> wpAccent.pillDark
                        else -> wpAccent.pillLight
                    },
                    dividerHandleBg = if (isDark) wpAccent.dividerDark else wpAccent.dividerLight,
                    dividerHandleDot = if (isDark) Color(0xFF2A2218) else Color(0xFFFAF3EA),
                    isGlass = false,
                    isBold = false,
                    blurDp = 0f,
                    surfaceFactory = { accentHue, emphasis ->
                        val rawH = clampHue(accentHue, effectiveBaseHue)
                        val h = if (wallpaperId == WallpaperPresetId.SAND && !hasCustomBaseHue) {
                            rawH
                        } else {
                            ((rawH * 0.38f + effectiveBaseHue * 0.62f).roundToInt() % 360).toFloat()
                        }
                        if (isDark) {
                            val l = when (emphasis) {
                                SurfaceEmphasis.HERO -> 24f
                                SurfaceEmphasis.MUTED -> 17f
                                SurfaceEmphasis.NORMAL -> 20f
                            }
                            val bg = hslColor(h, 24f, l)
                            TileSurfaceTokens(
                                backgroundStart = bg,
                                backgroundEnd = bg,
                                borderColor = hslColor(h, 40f, 75f, alpha = 0.14f),
                                borderWidthDp = 1f,
                                textPrimary = hslColor(h, 35f, 93f),
                                textSecondary = hslColor(h, 20f, 74f),
                                accentColor = hslColor(h, 78f, 74f),
                                accentBg = hslColor(h, 65f, 68f, alpha = 0.18f),
                                shadowColor = Color(0x47000000),
                                specularHighlightAlpha = 0.24f,
                            )
                        } else {
                            val l = when (emphasis) {
                                SurfaceEmphasis.HERO -> 88f
                                SurfaceEmphasis.MUTED -> 91f
                                SurfaceEmphasis.NORMAL -> 93f
                            }
                            val bg = hslColor(h, 42f, l)
                            TileSurfaceTokens(
                                backgroundStart = bg,
                                backgroundEnd = bg,
                                borderColor = hslColor(h, 35f, 40f, alpha = 0.10f),
                                borderWidthDp = 1f,
                                textPrimary = hslColor(h, 28f, 15f),
                                textSecondary = hslColor(h, 18f, 36f),
                                accentColor = hslColor(h, 62f, 38f),
                                accentBg = hslColor(h, 58f, 50f, alpha = 0.14f),
                                shadowColor = Color(0x14342614),
                                specularHighlightAlpha = 0.24f,
                            )
                        }
                    },
                )
            }

            ThemeVariantId.DARK_BOLD -> {
                val isLight = mode == ColorMode.LIGHT
                ThemeTokens(
                    id = id,
                    mode = mode,
                    wallpaperId = wallpaperId,
                    name = "Dunkel & kräftig",
                    description = "Satte Farben, riesige Zahlen, hoher Kontrast",
                    tileRadiusDp = 20f,
                    edgePaddingDp = 8f,
                    gapUseDp = 6f,
                    gapEditDp = 12f,
                    textPrimary = if (isLight) Color(0xFF101218) else Color.White,
                    textSecondary = if (isLight) Color(0xFF384052) else Color(0xC7FFFFFF),
                    textMuted = if (isLight) Color(0xFF5E667A) else Color(0x8FFFFFFF),
                    statusText = if (isLight) Color(0xFF101218) else Color.White,
                    sheetBg = if (isLight) Color(0xFAF4F6FA) else Color(0xF7101218),
                    sheetBorder = if (isLight) Color(0x1F101218) else Color(0x24FFFFFF),
                    overlayBackdrop = if (isLight) Color(0x730C0E14) else Color(0xB8000000),
                    pillBg = if (isLight) Color(0x40101218) else Color(0x47FFFFFF),
                    pillActive = if (wallpaperId == WallpaperPresetId.SAND) {
                        if (isLight) Color(0xFF101218) else Color.White
                    } else {
                        if (isLight) wpAccent.pillLight else wpAccent.pillDark
                    },
                    dividerHandleBg = Color.White,
                    dividerHandleDot = Color(0xFF101218),
                    isGlass = false,
                    isBold = true,
                    blurDp = 0f,
                    surfaceFactory = { accentHue, emphasis ->
                        val rawH = clampHue(accentHue, effectiveBaseHue)
                        val h = if (wallpaperId == WallpaperPresetId.SAND && !hasCustomBaseHue) {
                            rawH
                        } else {
                            ((rawH * 0.65f + effectiveBaseHue * 0.35f).roundToInt() % 360).toFloat()
                        }
                        if (isLight) {
                            val bgL = when (emphasis) {
                                SurfaceEmphasis.HERO -> 42f
                                SurfaceEmphasis.MUTED -> 92f
                                SurfaceEmphasis.NORMAL -> 87f
                            }
                            val isDarkCard = bgL < 55f
                            val bg = hslColor(h, if (isDarkCard) 74f else 45f, bgL)
                            TileSurfaceTokens(
                                backgroundStart = bg,
                                backgroundEnd = bg,
                                borderColor = hslColor(h, 60f, if (isDarkCard) 70f else 35f, alpha = 0.22f),
                                borderWidthDp = 1.5f,
                                textPrimary = if (isDarkCard) Color.White else Color(0xFF11131A),
                                textSecondary = if (isDarkCard) Color(0xD1FFFFFF) else Color(0xFF394155),
                                accentColor = if (isDarkCard) Color.White else hslColor(h, 78f, 40f),
                                accentBg = if (isDarkCard) Color(0x2EFFFFFF) else hslColor(h, 75f, 45f, alpha = 0.14f),
                                shadowColor = Color(0x1F0A0E1A),
                                specularHighlightAlpha = 0.24f,
                            )
                        } else {
                            val sat = when (emphasis) {
                                SurfaceEmphasis.HERO -> 68f
                                SurfaceEmphasis.MUTED -> 32f
                                SurfaceEmphasis.NORMAL -> 54f
                            }
                            val light = when (emphasis) {
                                SurfaceEmphasis.HERO -> 28f
                                SurfaceEmphasis.MUTED -> 14f
                                SurfaceEmphasis.NORMAL -> 21f
                            }
                            TileSurfaceTokens(
                                backgroundStart = hslColor(h, sat, light + 4f),
                                backgroundEnd = hslColor(h, sat, light - 3f),
                                borderColor = hslColor(h, 75f, 68f, alpha = 0.26f),
                                borderWidthDp = 1f,
                                textPrimary = Color.White,
                                textSecondary = Color(0xCCFFFFFF),
                                accentColor = hslColor(h, 90f, 72f),
                                accentBg = hslColor(h, 85f, 62f, alpha = 0.22f),
                                shadowColor = Color(0x70000000),
                                specularHighlightAlpha = 0.24f,
                            )
                        }
                    },
                )
            }

            ThemeVariantId.GLASS -> {
                val isDarkGlass = mode == ColorMode.DARK
                ThemeTokens(
                    id = id,
                    mode = mode,
                    wallpaperId = wallpaperId,
                    name = "Glas",
                    description = "Milchglas-Kacheln, feine Kanten, leuchtender Hintergrund",
                    tileRadiusDp = 24f,
                    edgePaddingDp = 8f,
                    gapUseDp = 6f,
                    gapEditDp = 12f,
                    textPrimary = Color.White,
                    textSecondary = Color(0xD6FFFFFF),
                    textMuted = Color(0xA3FFFFFF),
                    statusText = Color.White,
                    sheetBg = if (isDarkGlass) Color(0xD1121626) else Color(0xB81C223A),
                    sheetBorder = Color(0x42FFFFFF),
                    overlayBackdrop = Color(0x8F080A16),
                    pillBg = Color(0x57FFFFFF),
                    pillActive = Color.White,
                    dividerHandleBg = Color(0xF0FFFFFF),
                    dividerHandleDot = Color(0xFF1E2540),
                    isGlass = true,
                    isBold = false,
                    blurDp = 20f,
                    surfaceFactory = { accentHue, emphasis ->
                        val h = clampHue(accentHue, 260f)
                        val alpha = if (isDarkGlass) {
                            if (emphasis == SurfaceEmphasis.HERO) 0.24f else 0.16f
                        } else {
                            if (emphasis == SurfaceEmphasis.HERO) 0.28f else 0.20f
                        }
                        TileSurfaceTokens(
                            backgroundStart = hslColor(
                                h,
                                65f,
                                if (isDarkGlass) 32f else 76f,
                                alpha = alpha + 0.06f,
                            ),
                            backgroundEnd = hslColor(
                                (h + 24f) % 360f,
                                50f,
                                if (isDarkGlass) 20f else 62f,
                                alpha = alpha,
                            ),
                            borderColor = Color(0x47FFFFFF),
                            borderWidthDp = 1f,
                            textPrimary = Color.White,
                            textSecondary = Color(0xD6FFFFFF),
                            accentColor = hslColor(h, 95f, 84f),
                            accentBg = Color(0x2EFFFFFF),
                            shadowColor = Color(0x3D0A0C1E),
                            specularHighlightAlpha = 0.32f,
                        )
                    },
                )
            }
        }
    }

    fun getDockCapsuleTokens(theme: ThemeTokens): DockCapsuleTokens {
        val dockBg = when {
            theme.isGlass -> Color(0x29FFFFFF)
            theme.mode == ColorMode.DARK || theme.isBold -> Color(0x8F121622)
            else -> Color(0x9EFFFAF4)
        }
        val activePillBg = if (theme.isGlass) Color(0xEBFFFFFF) else theme.pillActive
        val activeTextColor = when {
            theme.isGlass -> Color(0xFF0F172A)
            theme.mode == ColorMode.DARK || theme.isBold -> Color(0xFF0F172A)
            else -> Color(0xFFFAF3EA)
        }
        return DockCapsuleTokens(dockBg, activePillBg, activeTextColor)
    }
}

val LocalCanopyTheme = staticCompositionLocalOf {
    CanopyThemes.getThemeTokens(ThemeVariantId.MATERIAL_YOU, ColorMode.LIGHT, WallpaperPresetId.SAND)
}

private data class WallpaperArtPalette(
    val bg1: Color,
    val bg2: Color,
    val bg3: Color,
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,
    val wave1: Color,
    val wave2: Color,
)

private val MATERIAL_ART_PALETTES: Map<WallpaperPresetId, Pair<WallpaperArtPalette, WallpaperArtPalette>> = mapOf(
    WallpaperPresetId.SAND to (
        WallpaperArtPalette(
            bg1 = Color(0xFFEFE5D8),
            bg2 = Color(0xFFE7D8C5),
            bg3 = Color(0xFFDAE2D6),
            orb1 = Color(0xFFE9B996),
            orb2 = Color(0xFFA9C7B5),
            orb3 = Color(0xFFF59E0B),
            wave1 = Color(0x38C48B62),
            wave2 = Color(0x3380A48E),
        ) to WallpaperArtPalette(
            bg1 = Color(0xFF1A1612),
            bg2 = Color(0xFF241D17),
            bg3 = Color(0xFF161A18),
            orb1 = Color(0xFF7E4E34),
            orb2 = Color(0xFF3E5A4C),
            orb3 = Color(0xFFB45309),
            wave1 = Color(0x1FDAB286),
            wave2 = Color(0x1F84A894),
        )
    ),
    WallpaperPresetId.EMERALD to (
        WallpaperArtPalette(
            bg1 = Color(0xFFDCFCE7),
            bg2 = Color(0xFFCCFBF1),
            bg3 = Color(0xFFE0F2FE),
            orb1 = Color(0xFF6EE7B7),
            orb2 = Color(0xFF5EEAD4),
            orb3 = Color(0xFF34D399),
            wave1 = Color(0x3810B981),
            wave2 = Color(0x290EA5E9),
        ) to WallpaperArtPalette(
            bg1 = Color(0xFF071E18),
            bg2 = Color(0xFF0B2B26),
            bg3 = Color(0xFF091A24),
            orb1 = Color(0xFF047857),
            orb2 = Color(0xFF0F766E),
            orb3 = Color(0xFF10B981),
            wave1 = Color(0x2934D399),
            wave2 = Color(0x1F2DD4BF),
        )
    ),
    WallpaperPresetId.COBALT to (
        WallpaperArtPalette(
            bg1 = Color(0xFFDBEAFE),
            bg2 = Color(0xFFE0E7FF),
            bg3 = Color(0xFFEDE9FE),
            orb1 = Color(0xFF93C5FD),
            orb2 = Color(0xFFA5B4FC),
            orb3 = Color(0xFF60A5FA),
            wave1 = Color(0x383B82F6),
            wave2 = Color(0x2E6366F1),
        ) to WallpaperArtPalette(
            bg1 = Color(0xFF0A1328),
            bg2 = Color(0xFF111D3D),
            bg3 = Color(0xFF181438),
            orb1 = Color(0xFF1D4ED8),
            orb2 = Color(0xFF4338CA),
            orb3 = Color(0xFF2563EB),
            wave1 = Color(0x2E60A5FA),
            wave2 = Color(0x24818CF8),
        )
    ),
    WallpaperPresetId.TERRACOTTA to (
        WallpaperArtPalette(
            bg1 = Color(0xFFFFE4E6),
            bg2 = Color(0xFFFFEDD5),
            bg3 = Color(0xFFFEF3C7),
            orb1 = Color(0xFFFDA4AF),
            orb2 = Color(0xFFFDBA74),
            orb3 = Color(0xFFFB7185),
            wave1 = Color(0x33F43F5E),
            wave2 = Color(0x2EF97316),
        ) to WallpaperArtPalette(
            bg1 = Color(0xFF240E14),
            bg2 = Color(0xFF2B1418),
            bg3 = Color(0xFF221510),
            orb1 = Color(0xFF9F1239),
            orb2 = Color(0xFF9A3412),
            orb3 = Color(0xFFE11D48),
            wave1 = Color(0x29FB7185),
            wave2 = Color(0x24FB923C),
        )
    ),
)

/**
 * Full-bleed parallax background artwork rendered behind tile gaps (15% page swipe parallax).
 */
@Composable
fun WallpaperCanvas(
    themeId: ThemeVariantId,
    mode: ColorMode,
    wallpaperId: WallpaperPresetId,
    parallaxXPx: () -> Float,
    modifier: Modifier = Modifier,
) {
    val pair = MATERIAL_ART_PALETTES[wallpaperId] ?: MATERIAL_ART_PALETTES.getValue(WallpaperPresetId.SAND)
    val pal = if (mode == ColorMode.DARK) pair.second else pair.first

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxXPx()
                    scaleX = 1.45f
                    scaleY = 1.06f
                }
        ) {
            val w = size.width
            val h = size.height

            when (themeId) {
                ThemeVariantId.MATERIAL_YOU -> {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(pal.bg1, pal.bg2, pal.bg3),
                            start = Offset.Zero,
                            end = Offset(w, h),
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(pal.orb1.copy(alpha = 0.85f), Color.Transparent),
                            center = Offset(w * 0.28f, h * 0.25f),
                            radius = w * 0.65f,
                        ),
                        center = Offset(w * 0.28f, h * 0.25f),
                        radius = w * 0.65f,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(pal.orb2.copy(alpha = 0.82f), Color.Transparent),
                            center = Offset(w * 0.72f, h * 0.68f),
                            radius = w * 0.72f,
                        ),
                        center = Offset(w * 0.72f, h * 0.68f),
                        radius = w * 0.72f,
                    )
                    val bottomWave = Path().apply {
                        moveTo(-w * 0.1f, h * 0.62f)
                        cubicTo(w * 0.2f, h * 0.50f, w * 0.45f, h * 0.70f, w * 0.75f, h * 0.57f)
                        cubicTo(w * 0.90f, h * 0.49f, w * 1.05f, h * 0.56f, w * 1.15f, h * 0.52f)
                        lineTo(w * 1.15f, h * 1.05f)
                        lineTo(-w * 0.1f, h * 1.05f)
                        close()
                    }
                    drawPath(bottomWave, color = pal.wave1)
                }

                ThemeVariantId.DARK_BOLD -> {
                    val isLight = mode == ColorMode.LIGHT
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = if (isLight) {
                                listOf(pal.bg1, pal.bg2, pal.bg3)
                            } else {
                                listOf(Color(0xFF090B10), pal.bg1, Color(0xFF0A0C12))
                            },
                            start = Offset.Zero,
                            end = Offset(w * 0.8f, h),
                        )
                    )
                    val ribbon = Path().apply {
                        moveTo(-w * 0.1f, h * 0.20f)
                        lineTo(w * 1.15f, -h * 0.04f)
                        lineTo(w * 1.15f, h * 0.20f)
                        lineTo(-w * 0.1f, h * 0.44f)
                        close()
                    }
                    drawPath(
                        ribbon,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                pal.orb1.copy(alpha = if (isLight) 0.32f else 0.42f),
                                pal.orb2.copy(alpha = if (isLight) 0.24f else 0.34f),
                            )
                        ),
                    )
                    drawCircle(
                        color = pal.wave1,
                        radius = w * 0.32f,
                        center = Offset(w * 0.66f, h * 0.28f),
                        style = Stroke(width = 44f),
                    )
                    drawCircle(
                        color = pal.wave2,
                        radius = w * 0.24f,
                        center = Offset(w * 0.26f, h * 0.78f),
                        style = Stroke(width = 36f),
                    )
                }

                ThemeVariantId.GLASS -> {
                    val isDark = mode == ColorMode.DARK
                    val glassAccents = when (wallpaperId) {
                        WallpaperPresetId.SAND -> listOf(
                            Color(0xFF2D3FA8), Color(0xFF7B2CBF), Color(0xFFFF5E9C), Color(0xFF38BDF8)
                        )
                        WallpaperPresetId.EMERALD -> listOf(
                            Color(0xFF064E3B), Color(0xFF0F766E), Color(0xFF34D399), Color(0xFF38BDF8)
                        )
                        WallpaperPresetId.COBALT -> listOf(
                            Color(0xFF1E3A8A), Color(0xFF4338CA), Color(0xFF60A5FA), Color(0xFFC084FC)
                        )
                        WallpaperPresetId.TERRACOTTA -> listOf(
                            Color(0xFF7F1D1D), Color(0xFF9A3412), Color(0xFFFB7185), Color(0xFFFBBF24)
                        )
                    }
                    val (g1, g2, a1, a2) = glassAccents
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                if (isDark) Color(0xFF0D1024) else g1,
                                if (isDark) g1 else g2,
                                if (isDark) Color(0xFF0D2A3F) else g1,
                            ),
                            start = Offset.Zero,
                            end = Offset(w, h),
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(a1.copy(alpha = if (isDark) 0.68f else 0.88f), Color.Transparent),
                            center = Offset(w * 0.25f, h * 0.22f),
                            radius = w * 0.55f,
                        ),
                        center = Offset(w * 0.25f, h * 0.22f),
                        radius = w * 0.55f,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(a2.copy(alpha = if (isDark) 0.66f else 0.88f), Color.Transparent),
                            center = Offset(w * 0.72f, h * 0.46f),
                            radius = w * 0.60f,
                        ),
                        center = Offset(w * 0.72f, h * 0.46f),
                        radius = w * 0.60f,
                    )
                    val aurora = Path().apply {
                        moveTo(-w * 0.1f, h * 0.42f)
                        quadraticTo(w * 0.35f, h * 0.28f, w * 0.60f, h * 0.45f)
                        quadraticTo(w * 0.88f, h * 0.60f, w * 1.15f, h * 0.38f)
                    }
                    drawPath(
                        aurora,
                        color = Color.White.copy(alpha = 0.22f),
                        style = Stroke(width = 64f, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}
