package app.canopy.launcher.motion

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLong

enum class HapticKind(
    val label: String,
    val singleDurationMs: Long?,
    val waveformTimingsMs: LongArray,
) {
    TICK("Feiner Tick", 8L, longArrayOf(0L, 8L)),
    PRESS("Tippen", 10L, longArrayOf(0L, 10L)),
    PAGE_SETTLE("Seiten-Tick", 12L, longArrayOf(0L, 12L)),
    SNAP("Einrast-Klick", 18L, longArrayOf(0L, 18L)),
    EDIT_ENTER("Bearbeiten-Impuls", null, longArrayOf(0L, 20L, 30L, 32L)),
    REMOVE("Entfernt", null, longArrayOf(0L, 15L, 25L, 15L)),
    ERROR("Limit", null, longArrayOf(0L, 25L, 25L, 25L)),
}

data class HapticEvent(
    val id: Long,
    val kind: HapticKind,
    val label: String,
    val x: Float? = null,
    val y: Float? = null,
    val timestampMs: Long,
)

object CanopyHaptics {
    private val listeners = CopyOnWriteArraySet<(HapticEvent) -> Unit>()
    private val eventSeq = AtomicLong(1L)

    fun subscribe(listener: (HapticEvent) -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    fun trigger(
        kind: HapticKind,
        enabled: Boolean = true,
        x: Float? = null,
        y: Float? = null,
        context: Context? = null,
    ): HapticEvent? {
        if (!enabled) return null

        if (context != null) {
            vibrateDevice(context, kind)
        }

        val event = HapticEvent(
            id = eventSeq.incrementAndGet(),
            kind = kind,
            label = kind.label,
            x = x,
            y = y,
            timestampMs = System.currentTimeMillis(),
        )
        listeners.forEach { it(event) }
        return event
    }

    private fun vibrateDevice(context: Context, kind: HapticKind) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return

            val singleMs = kind.singleDurationMs
            if (singleMs != null) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(singleMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(kind.waveformTimingsMs, -1))
            }
        }
    }
}

@Composable
fun HapticVisualFeedbackLayer(
    latestEvent: HapticEvent?,
    showHudBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (latestEvent != null && latestEvent.x != null && latestEvent.y != null) {
            val progress = remember(latestEvent.id) { Animatable(0f) }
            LaunchedEffect(latestEvent.id) {
                progress.animateTo(1f, tween(340))
            }
            if (progress.value < 1f) {
                val px = latestEvent.x
                val py = latestEvent.y
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val alpha = (1f - progress.value) * 0.65f
                    val radius = 14f + progress.value * 44f
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = alpha),
                        radius = radius,
                        center = Offset(px, py),
                        style = Stroke(width = 4f),
                    )
                }
            }
        }

        if (showHudBadge && latestEvent != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp)
                    .clip(CircleShape)
                    .background(Color(0xD90F172A))
                    .border(1.dp, Color(0x4738BDF8), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                )
                Text(
                    text = " Haptik: ${latestEvent.label}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
