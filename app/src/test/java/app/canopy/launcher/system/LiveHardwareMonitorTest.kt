package app.canopy.launcher.system

import app.canopy.launcher.ui.tiles.EigenTileActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class LiveHardwareMonitorTest {
    @Test
    fun `greetingForHour maps morning afternoon evening and night accurately`() {
        assertEquals("Guten Morgen" to "Guten\nMorgen", LiveHardwareMath.greetingForHour(7))
        assertEquals("Guten Tag" to "Guten\nTag", LiveHardwareMath.greetingForHour(13))
        assertEquals("Guten Abend" to "Guten\nAbend", LiveHardwareMath.greetingForHour(19))
        assertEquals("Gute Nacht" to "Gute\nNacht", LiveHardwareMath.greetingForHour(2))
    }

    @Test
    fun `formatClockFields formats deterministic German time date and greeting`() {
        val utc = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(utc, Locale.GERMANY).apply {
            set(2026, Calendar.NOVEMBER, 18, 9, 5, 0)
        }
        val snapshot = LiveHardwareMath.formatClockFields(cal.timeInMillis, Locale.GERMANY, utc)
        assertEquals("09:05", snapshot.timeLabel)
        assertEquals("09\n05", snapshot.stackedTimeLabel)
        assertEquals("18", snapshot.dayOfMonthLabel)
        assertEquals("Guten Morgen", snapshot.greetingSingleLine)
        assertTrue(snapshot.upperDateLabel.contains("18"))
    }

    @Test
    fun `computeBatteryMetrics and computeStepMetrics synchronize tiles and widgets`() {
        val (pct, ratio, hours) = LiveHardwareMath.computeBatteryMetrics(level = 45, scale = 100, isCharging = false)
        assertEquals(45, pct)
        assertEquals(0.45f, ratio, 0.001f)
        assertEquals(10, hours)

        val steps = LiveHardwareMath.computeStepMetrics(baseSteps = 7420, sensorDelta = 80, bonusSteps = 500)
        assertEquals(8000, steps.totalSteps)
        assertEquals(80, steps.goalPercent)
        assertEquals(0.8f, steps.progressRatio, 0.001f)
        assertEquals("8,0k", steps.compactLabel)
        assertTrue(steps.formattedSteps.contains("8.000"))

        val updatedNotes = EigenTileActions.addNoteLine(listOf("Bestehende Notiz"), "Neue Notiz")
        assertEquals(2, updatedNotes.size)
    }

    @Test
    fun `daily step baseline starts at zero each day and survives a reboot`() {
        // First reading of the day becomes the baseline → 0 steps, not a made-up number.
        val (b1, s1) = LiveHardwareMath.advanceDailySteps(null, "20261118", raw = 12_000)
        assertEquals(0, s1)
        val (b2, s2) = LiveHardwareMath.advanceDailySteps(b1, "20261118", raw = 12_850)
        assertEquals(850, s2)
        // Reboot resets the hardware counter: steps so far are carried over.
        val (b3, s3) = LiveHardwareMath.advanceDailySteps(b2, "20261118", raw = 40)
        assertEquals(890, s3)
        // New day → fresh baseline.
        val (_, s4) = LiveHardwareMath.advanceDailySteps(b3, "20261119", raw = 300)
        assertEquals(0, s4)
    }

    @Test
    fun `telemetry defaults never invent steps or alarms`() {
        val snapshot = HardwareTelemetrySnapshot()
        assertEquals(false, snapshot.stepsAvailable)
        assertEquals(0, snapshot.steps.totalSteps)
        assertEquals(null, snapshot.nextAlarmLabel)
    }
}
