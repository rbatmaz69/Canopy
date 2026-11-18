package app.canopy.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import app.canopy.launcher.system.LiveHardwareMath.computeStepMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
data class StepMetrics(
    val totalSteps: Int,
    val progressRatio: Float,
    val goalPercent: Int,
    val compactLabel: String,
    val formattedSteps: String,
    val distanceKmLabel: String,
    val kcal: Int,
)

@Immutable
data class HardwareTelemetrySnapshot(
    val timeLabel: String = "09:01",
    val stackedTimeLabel: String = "09\n01",
    val shortDateLabel: String = "Mi., 18. Nov",
    val upperDateLabel: String = "MITTWOCH, 18. NOV",
    val dayOfMonthLabel: String = "18",
    val greetingSingleLine: String = "Guten Morgen",
    val greetingTwoLine: String = "Guten\nMorgen",
    val batteryPercent: Int = 84,
    val isCharging: Boolean = false,
    val estimatedHoursLeft: Int = 19,
    val steps: StepMetrics = computeStepMetrics(baseSteps = 7420, sensorDelta = 0, bonusSteps = 0),
    val onAddBonusSteps: (Int) -> Unit = {},
)

val LocalHardwareTelemetry = staticCompositionLocalOf { HardwareTelemetrySnapshot() }

/**
 * Pure Kotlin formatting & calculation helper for system clock, greeting,
 * battery telemetry, and synchronized step counters.
 */
object LiveHardwareMath {
    fun greetingForHour(hourOfDay: Int): Pair<String, String> = when (hourOfDay) {
        in 5..10 -> "Guten Morgen" to "Guten\nMorgen"
        in 11..17 -> "Guten Tag" to "Guten\nTag"
        in 18..21 -> "Guten Abend" to "Guten\nAbend"
        else -> "Gute Nacht" to "Gute\nNacht"
    }

    fun formatClockFields(
        epochMillis: Long,
        locale: Locale = Locale.GERMANY,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): HardwareTelemetrySnapshot {
        val date = Date(epochMillis)
        val cal = Calendar.getInstance(timeZone, locale).apply { time = date }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val (singleGreeting, twoLineGreeting) = greetingForHour(hour)

        val timeFmt = SimpleDateFormat("HH:mm", locale).apply { this.timeZone = timeZone }
        val stackedFmt = SimpleDateFormat("HH\nmm", locale).apply { this.timeZone = timeZone }
        val shortDateFmt = SimpleDateFormat("EE, d. MMM", locale).apply { this.timeZone = timeZone }
        val fullDateFmt = SimpleDateFormat("EEEE, d. MMM", locale).apply { this.timeZone = timeZone }
        val dayFmt = SimpleDateFormat("d", locale).apply { this.timeZone = timeZone }

        return HardwareTelemetrySnapshot(
            timeLabel = timeFmt.format(date),
            stackedTimeLabel = stackedFmt.format(date),
            shortDateLabel = shortDateFmt.format(date),
            upperDateLabel = fullDateFmt.format(date).uppercase(locale),
            dayOfMonthLabel = dayFmt.format(date),
            greetingSingleLine = singleGreeting,
            greetingTwoLine = twoLineGreeting,
        )
    }

    fun computeBatteryMetrics(
        level: Int,
        scale: Int,
        isCharging: Boolean,
    ): Triple<Int, Float, Int> {
        val safePercent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale.toFloat()).roundToInt().coerceIn(0, 100)
        } else {
            84
        }
        val ratio = (safePercent / 100f).coerceIn(0f, 1f)
        val estHours = if (isCharging) {
            max(1, ((100 - safePercent) / 35f).roundToInt())
        } else {
            max(1, (safePercent * 0.22f).roundToInt())
        }
        return Triple(safePercent, ratio, estHours)
    }

    fun computeStepMetrics(
        baseSteps: Int = 7420,
        sensorDelta: Int = 0,
        bonusSteps: Int = 0,
        goalSteps: Int = 10_000,
    ): StepMetrics {
        val total = max(0, baseSteps + max(0, sensorDelta) + max(0, bonusSteps))
        val safeGoal = max(1, goalSteps)
        val ratio = (total.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
        val goalPct = min(100, ((total.toFloat() / safeGoal.toFloat()) * 100f).roundToInt())
        val compact = if (total >= 1000) {
            String.format(Locale.GERMAN, "%.1fk", total / 1000f)
        } else {
            total.toString()
        }
        val formatted = String.format(Locale.GERMAN, "%,d", total)
        val distKm = String.format(Locale.GERMAN, "%.1f", total * 0.00073f)
        val kcal = (total * 0.045f).roundToInt()
        return StepMetrics(
            totalSteps = total,
            progressRatio = ratio,
            goalPercent = goalPct,
            compactLabel = compact,
            formattedSteps = formatted,
            distanceKmLabel = distKm,
            kcal = kcal,
        )
    }
}

/**
 * Runtime controller subscribing to Android system time ticks, battery broadcasts,
 * optional hardware step counter sensors, and camera LED torch mode.
 */
class LiveHardwareController(private val context: Context) {
    private var initialSensorSteps: Int? = null
    private var sensorDeltaSteps: Int = 0
    private var bonusSteps: Int = 0

    private val _telemetryFlow = MutableStateFlow(createInitialSnapshot())
    val telemetryFlow: StateFlow<HardwareTelemetrySnapshot> = _telemetryFlow.asStateFlow()

    private val sensorManager: SensorManager? = runCatching {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }.getOrNull()

    private val cameraManager: CameraManager? = runCatching {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }.getOrNull()

    private val timeAndBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> updateClock()
                Intent.ACTION_BATTERY_CHANGED -> updateBatteryFromIntent(intent)
            }
        }
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val raw = event?.values?.firstOrNull()?.roundToInt() ?: return
            val base = initialSensorSteps ?: raw.also { initialSensorSteps = it }
            sensorDeltaSteps = max(0, raw - base)
            updateSteps()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun createInitialSnapshot(): HardwareTelemetrySnapshot {
        val base = LiveHardwareMath.formatClockFields(System.currentTimeMillis())
        return base.copy(
            steps = computeStepMetrics(baseSteps = 7420, sensorDelta = sensorDeltaSteps, bonusSteps = bonusSteps),
            onAddBonusSteps = ::addBonusSteps,
        )
    }

    fun start() {
        updateClock()
        runCatching {
            val stickyBattery = context.registerReceiver(
                timeAndBatteryReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                },
            )
            updateBatteryFromIntent(stickyBattery)
        }
        runCatching {
            val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepSensor != null) {
                sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stop() {
        runCatching { context.unregisterReceiver(timeAndBatteryReceiver) }
        runCatching { sensorManager?.unregisterListener(stepListener) }
    }

    fun addBonusSteps(delta: Int = 250) {
        bonusSteps = max(0, bonusSteps + delta)
        updateSteps()
    }

    fun setTorchEnabled(enabled: Boolean): Boolean {
        val cm = cameraManager ?: return false
        return runCatching {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return@runCatching false
            cm.setTorchMode(cameraId, enabled)
            true
        }.getOrDefault(false)
    }

    private fun updateClock() {
        val clockFields = LiveHardwareMath.formatClockFields(System.currentTimeMillis())
        _telemetryFlow.update { cur ->
            cur.copy(
                timeLabel = clockFields.timeLabel,
                stackedTimeLabel = clockFields.stackedTimeLabel,
                shortDateLabel = clockFields.shortDateLabel,
                upperDateLabel = clockFields.upperDateLabel,
                dayOfMonthLabel = clockFields.dayOfMonthLabel,
                greetingSingleLine = clockFields.greetingSingleLine,
                greetingTwoLine = clockFields.greetingTwoLine,
            )
        }
    }

    private fun updateBatteryFromIntent(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val (pct, _, estHours) = LiveHardwareMath.computeBatteryMetrics(level, scale, charging)
        _telemetryFlow.update { cur ->
            cur.copy(
                batteryPercent = pct,
                isCharging = charging,
                estimatedHoursLeft = estHours,
            )
        }
    }

    private fun updateSteps() {
        val nextSteps = computeStepMetrics(baseSteps = 7420, sensorDelta = sensorDeltaSteps, bonusSteps = bonusSteps)
        _telemetryFlow.update { cur -> cur.copy(steps = nextSteps) }
    }
}
