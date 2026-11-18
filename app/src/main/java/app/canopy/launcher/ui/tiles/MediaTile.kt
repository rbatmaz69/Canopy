package app.canopy.launcher.ui.tiles

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.layout.TileSizeTier
import app.canopy.launcher.ui.theme.CanopyThemes
import kotlin.math.floor

object MediaTileHelpers {
    fun formatTime(sec: Float): String {
        val safeSec = sec.coerceAtLeast(0f).toInt()
        val m = floor(safeSec / 60.0).toInt()
        val s = safeSec % 60
        return "$m:${if (s < 10) "0" else ""}$s"
    }

    fun clampSeekRatio(xPx: Float, widthPx: Float): Float {
        if (widthPx <= 1f) return 0f
        return (xPx / widthPx).coerceIn(0f, 1f)
    }
}

@Composable
fun MediaTile(props: TileContentProps) {
    val track = props.mediaState.overrideTrack ?: SystemTileDefaults.MEDIA_TRACKS[
        props.mediaState.trackIndex % SystemTileDefaults.MEDIA_TRACKS.size
    ]
    val isLargeHero = props.tile.mediaLarge == true || props.tier == TileSizeTier.L

    when (props.tier) {
        TileSizeTier.XS -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(props.surface.accentColor)
                        .clickable(enabled = !props.isEditMode) { props.mediaState.onTogglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (props.mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (props.mediaState.isPlaying) "Pause" else "Abspielen",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        TileSizeTier.TICKER -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        CanopyThemes.hslColor(track.accentHue, 82f, 55f),
                                        CanopyThemes.hslColor((track.accentHue + 55f) % 360f, 75f, 42f),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Album, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = props.surface.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track.artist,
                            color = props.surface.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(props.surface.accentBg)
                        .clickable(enabled = !props.isEditMode) { props.mediaState.onTogglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (props.mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = props.surface.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        else -> {
            if (isLargeHero) {
                LargeHeroMediaContent(props = props, track = track)
            } else {
                CompactMediaContent(props = props, track = track)
            }
        }
    }
}

@Composable
private fun CoverArtwork(track: MediaTrack, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    CanopyThemes.hslColor(track.accentHue, 78f, 48f),
                    CanopyThemes.hslColor((track.accentHue + 42f) % 360f, 72f, 30f),
                    Color(0xFF0F1020),
                ),
                start = Offset.Zero,
                end = Offset(w, h),
            )
        )
        val center = Offset(w * 0.5f, h * 0.44f)
        val minDim = minOf(w, h)
        drawCircle(Color.White.copy(alpha = 0.22f), radius = minDim * 0.30f, center = center, style = Stroke(2f))
        drawCircle(Color.White.copy(alpha = 0.34f), radius = minDim * 0.21f, center = center, style = Stroke(18f))
        drawCircle(Color.White.copy(alpha = 0.88f), radius = minDim * 0.075f, center = center)

        val wave = Path().apply {
            moveTo(0f, h * 0.80f)
            quadraticTo(w * 0.26f, h * 0.64f, w * 0.53f, h * 0.75f)
            quadraticTo(w * 0.80f, h * 0.85f, w, h * 0.69f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(wave, color = Color.Black.copy(alpha = 0.35f))
    }
}

@Composable
private fun LargeHeroMediaContent(props: TileContentProps, track: MediaTrack) {
    val eqTransition = rememberInfiniteTransition(label = "eq-bars")
    val barHeights = List(4) { idx ->
        val animated by eqTransition.animateFloat(
            initialValue = 3f,
            targetValue = 11f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520 + idx * 140),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "eq-bar-$idx",
        )
        if (props.mediaState.isPlaying) animated else 4f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CoverArtwork(track = track, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x590A0B12),
                            Color(0x380A0B12),
                            Color(0xE00A0B12),
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        if (props.mediaState.isPlaying) {
                            Row(
                                modifier = Modifier.height(11.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                barHeights.forEach { hDp ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(hDp.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (props.mediaState.isPlaying) "Wiedergabe läuft" else "Pausiert",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Text(
                        text = track.album,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track.artist,
                            color = Color.White.copy(alpha = 0.84f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    DraggableSeekBar(
                        progress = props.mediaState.progress,
                        enabled = !props.isEditMode,
                        trackBg = Color.White.copy(alpha = 0.26f),
                        fillColor = Color.White,
                        showThumb = true,
                        onSeek = props.mediaState.onSeek,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = MediaTileHelpers.formatTime(track.durationSec * props.mediaState.progress),
                            color = Color.White.copy(alpha = 0.78f),
                            style = TextStyle(fontSize = 11.sp, fontFeatureSettings = "tnum"),
                        )
                        Text(
                            text = MediaTileHelpers.formatTime(track.durationSec.toFloat()),
                            color = Color.White.copy(alpha = 0.78f),
                            style = TextStyle(fontSize = 11.sp, fontFeatureSettings = "tnum"),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable(enabled = !props.isEditMode) { props.mediaState.onPrev() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Zurück", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(enabled = !props.isEditMode) { props.mediaState.onTogglePlay() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (props.mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (props.mediaState.isPlaying) "Pause" else "Abspielen",
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable(enabled = !props.isEditMode) { props.mediaState.onNext() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Weiter", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMediaContent(props: TileContentProps, track: MediaTrack) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CoverArtwork(
                track = track,
                modifier = Modifier
                    .size(if (props.tier == TileSizeTier.S) 36.dp else 42.dp)
                    .shadow(3.dp, RoundedCornerShape(11.dp))
                    .clip(RoundedCornerShape(11.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = props.surface.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    color = props.surface.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (props.heightDp >= 165f) {
            val waveform = remember {
                listOf(0.35f, 0.68f, 0.48f, 0.88f, 0.62f, 0.95f, 0.42f, 0.74f, 0.86f, 0.54f, 0.78f, 0.92f, 0.46f, 0.70f, 0.58f, 0.40f)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    waveform.forEachIndexed { idx, frac ->
                        val active = idx.toFloat() / waveform.size <= props.mediaState.progress
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(frac)
                                .clip(CircleShape)
                                .background(if (active) props.surface.accentColor else props.surface.accentBg)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = MediaTileHelpers.formatTime(track.durationSec * props.mediaState.progress),
                        color = props.surface.textSecondary,
                        style = TextStyle(fontSize = 10.sp, fontFeatureSettings = "tnum"),
                    )
                    Text(
                        text = MediaTileHelpers.formatTime(track.durationSec.toFloat()),
                        color = props.surface.textSecondary,
                        style = TextStyle(fontSize = 10.sp, fontFeatureSettings = "tnum"),
                    )
                }
            }
        }

        DraggableSeekBar(
            progress = props.mediaState.progress,
            enabled = !props.isEditMode,
            trackBg = props.surface.accentBg,
            fillColor = props.surface.accentColor,
            showThumb = false,
            onSeek = props.mediaState.onSeek,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !props.isEditMode) { props.mediaState.onPrev() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Zurück", tint = props.surface.textPrimary, modifier = Modifier.size(16.dp))
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(props.surface.accentColor)
                    .clickable(enabled = !props.isEditMode) { props.mediaState.onTogglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (props.mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (props.mediaState.isPlaying) "Pause" else "Abspielen",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !props.isEditMode) { props.mediaState.onNext() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Weiter", tint = props.surface.textPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun DraggableSeekBar(
    progress: Float,
    enabled: Boolean,
    trackBg: Color,
    fillColor: Color,
    showThumb: Boolean,
    onSeek: (Float) -> Unit,
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    val clamped = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onSeek(MediaTileHelpers.clampSeekRatio(offset.x, widthPx))
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek(MediaTileHelpers.clampSeekRatio(change.position.x, widthPx))
                }
            }
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showThumb) 5.dp else 4.dp)
                .clip(CircleShape)
                .background(trackBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(fillColor)
            )
        }
        if (showThumb) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = ((widthPx / 3f) * clamped).coerceAtLeast(0f).dp.let { 0.dp })
            )
        }
    }
}
