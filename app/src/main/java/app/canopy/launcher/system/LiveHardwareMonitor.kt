package app.canopy.launcher.system

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
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
    /** True once a real step counter reading for today exists (sensor present + permission granted). */
    val stepsAvailable: Boolean = false,
    val steps: StepMetrics = computeStepMetrics(baseSteps = 0, sensorDelta = 0, bonusSteps = 0),
    /** Next system alarm (from AlarmManager), formatted HH:mm, or null when none is set. */
    val nextAlarmLabel: String? = null,
    /** Whether the next alarm rings today (false = a later day). */
    val nextAlarmIsToday: Boolean = true,
    val torchOn: Boolean = false,
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

    /**
     * Persisted per-day step baseline for TYPE_STEP_COUNTER (which counts since boot).
     * [carry] keeps steps counted earlier today before a reboot reset the hardware counter.
     */
    data class DailyStepBaseline(
        val dayKey: String,
        val baseline: Int,
        val carry: Int,
        val lastRaw: Int,
    )

    /**
     * Returns the updated baseline and today's step count for a new raw sensor reading.
     * New day → baseline is the current raw value. Counter went backwards (reboot) → bank
     * what was counted so far into [DailyStepBaseline.carry] and restart from zero.
     */
    fun advanceDailySteps(previous: DailyStepBaseline?, todayKey: String, raw: Int): Pair<DailyStepBaseline, Int> {
        val safeRaw = max(0, raw)
        val next = when {
            previous == null || previous.dayKey != todayKey -> DailyStepBaseline(todayKey, safeRaw, 0, safeRaw)
            safeRaw < previous.lastRaw -> DailyStepBaseline(
                dayKey = todayKey,
                baseline = 0,
                carry = previous.carry + max(0, previous.lastRaw - previous.baseline),
                lastRaw = safeRaw,
            )
            else -> previous.copy(lastRaw = safeRaw)
        }
        return next to (next.carry + max(0, next.lastRaw - next.baseline))
    }

    fun computeStepMetrics(
        baseSteps: Int = 0,
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
 * the next system alarm, the hardware step counter (with a persisted daily baseline)
 * and the camera torch state.
 */
class LiveHardwareController(private val context: Context) {
    private val prefs = context.getSharedPreferences("canopy_hardware", Context.MODE_PRIVATE)
    private var stepBaseline: LiveHardwareMath.DailyStepBaseline? = loadStepBaseline()
    private var stepListenerRegistered = false

    private val _telemetryFlow = MutableStateFlow(createInitialSnapshot())
    val telemetryFlow: StateFlow<HardwareTelemetrySnapshot> = _telemetryFlow.asStateFlow()

    private val sensorManager: SensorManager? = runCatching {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }.getOrNull()

    private val cameraManager: CameraManager? = runCatching {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }.getOrNull()

    private val alarmManager: AlarmManager? = runCatching {
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }.getOrNull()

    private val torchCameraId: String? by lazy {
        runCatching {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) _telemetryFlow.update { it.copy(torchOn = enabled) }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == torchCameraId) _telemetryFlow.update { it.copy(torchOn = false) }
        }
    }

    private val timeAndBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    updateClock()
                    updateNextAlarm()
                }
                Intent.ACTION_BATTERY_CHANGED -> updateBatteryFromIntent(intent)
                AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> updateNextAlarm()
            }
        }
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val raw = event?.values?.firstOrNull()?.roundToInt() ?: return
            val (nextBaseline, today) = LiveHardwareMath.advanceDailySteps(stepBaseline, todayKey(), raw)
            stepBaseline = nextBaseline
            saveStepBaseline(nextBaseline)
            _telemetryFlow.update { cur ->
                cur.copy(stepsAvailable = true, steps = computeStepMetrics(baseSteps = today))
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun createInitialSnapshot(): HardwareTelemetrySnapshot =
        LiveHardwareMath.formatClockFields(System.currentTimeMillis())

    fun start() {
        updateClock()
        updateNextAlarm()
        runCatching {
            val stickyBattery = context.registerReceiver(
                timeAndBatteryReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
                },
            )
            updateBatteryFromIntent(stickyBattery)
        }
        runCatching { cameraManager?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper())) }
        registerStepSensorIfPermitted()
    }

    fun stop() {
        runCatching { context.unregisterReceiver(timeAndBatteryReceiver) }
        runCatching { cameraManager?.unregisterTorchCallback(torchCallback) }
        runCatching { sensorManager?.unregisterListener(stepListener) }
        stepListenerRegistered = false
    }

    /** Re-checks runtime permissions (e.g. after onboarding) and registers the step sensor once allowed. */
    fun registerStepSensorIfPermitted() {
        if (stepListenerRegistered) return
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        runCatching {
            val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return@runCatching
            stepListenerRegistered = sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun setTorchEnabled(enabled: Boolean): Boolean {
        val cm = cameraManager ?: return false
        val id = torchCameraId ?: return false
        return runCatching {
            cm.setTorchMode(id, enabled)
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

    private fun updateNextAlarm() {
        val next = runCatching { alarmManager?.nextAlarmClock }.getOrNull()
        val label: String?
        val isToday: Boolean
        if (next == null) {
            label = null
            isToday = true
        } else {
            val date = Date(next.triggerTime)
            label = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            isToday = todayKey() == dayKeyFormat().format(date)
        }
        _telemetryFlow.update { it.copy(nextAlarmLabel = label, nextAlarmIsToday = isToday) }
    }

    private fun updateBatteryFromIntent(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val (pct, _, _) = LiveHardwareMath.computeBatteryMetrics(level, scale, charging)
        _telemetryFlow.update { cur -> cur.copy(batteryPercent = pct, isCharging = charging) }
    }

    private fun dayKeyFormat() = SimpleDateFormat("yyyyMMdd", Locale.US)

    private fun todayKey(): String = dayKeyFormat().format(Date())

    private fun loadStepBaseline(): LiveHardwareMath.DailyStepBaseline? {
        val day = prefs.getString("steps_day", null) ?: return null
        return LiveHardwareMath.DailyStepBaseline(
            dayKey = day,
            baseline = prefs.getInt("steps_baseline", 0),
            carry = prefs.getInt("steps_carry", 0),
            lastRaw = prefs.getInt("steps_last_raw", 0),
        )
    }

    private fun saveStepBaseline(b: LiveHardwareMath.DailyStepBaseline) {
        prefs.edit()
            .putString("steps_day", b.dayKey)
            .putInt("steps_baseline", b.baseline)
            .putInt("steps_carry", b.carry)
            .putInt("steps_last_raw", b.lastRaw)
            .apply()
    }
}
