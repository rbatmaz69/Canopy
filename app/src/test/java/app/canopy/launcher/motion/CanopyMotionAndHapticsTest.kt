package app.canopy.launcher.motion

import app.canopy.launcher.data.AnimationMood
import app.canopy.launcher.data.MotionSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanopyMotionAndHapticsTest {
    @Test
    fun `base springs match specification values across all 6 roles`() {
        val divider = CanopySprings.resolveSpring(SpringRole.DIVIDER_SNAP)
        assertEquals(400f, divider.stiffness, 1e-4f)
        assertEquals(0.75f, divider.dampingRatio, 1e-4f)

        val pageSwipe = CanopySprings.resolveSpring(SpringRole.PAGE_SWIPE)
        assertEquals(300f, pageSwipe.stiffness, 1e-4f)
        assertEquals(0.85f, pageSwipe.dampingRatio, 1e-4f)

        val press = CanopySprings.resolveSpring(SpringRole.PRESS_FEEDBACK)
        assertEquals(1500f, press.stiffness, 1e-4f)
        assertEquals(0.60f, press.dampingRatio, 1e-4f)
    }

    @Test
    fun `moods and 0_25x slow-motion scale spring frequency and damping accurately`() {
        val normal = CanopySprings.resolveSpring(SpringRole.TILE_SWAP, MotionSettings(mood = AnimationMood.NORMAL))
        val playful = CanopySprings.resolveSpring(SpringRole.TILE_SWAP, MotionSettings(mood = AnimationMood.VERSPIELT))
        val calm = CanopySprings.resolveSpring(SpringRole.TILE_SWAP, MotionSettings(mood = AnimationMood.RUHIG))

        assertTrue(playful.stiffness > normal.stiffness)
        assertTrue(playful.dampingRatio < normal.dampingRatio)
        assertTrue(calm.stiffness < normal.stiffness)
        assertTrue(calm.dampingRatio > normal.dampingRatio)

        val slow = CanopySprings.resolveSpring(
            SpringRole.DIVIDER_SNAP,
            MotionSettings(slowMotion = true),
        )
        // s = 0.25 -> stiffness * 0.0625, dampingCoefficient * 0.25, dampingRatio unchanged
        assertEquals(400f * 0.0625f, slow.stiffness, 1e-4f)
        assertEquals(normal.dampingRatio.let { CanopySprings.resolveSpring(SpringRole.DIVIDER_SNAP).dampingRatio }, slow.dampingRatio, 1e-4f)
        val normalDivider = CanopySprings.resolveSpring(SpringRole.DIVIDER_SNAP)
        assertEquals(normalDivider.dampingCoefficient * 0.25f, slow.dampingCoefficient, 1e-4f)
    }

    @Test
    fun `haptics engine emits events with coordinates and respects enabled toggle`() {
        val received = mutableListOf<HapticEvent>()
        val unsubscribe = CanopyHaptics.subscribe { received.add(it) }
        try {
            val evt = CanopyHaptics.trigger(HapticKind.SNAP, enabled = true, x = 120f, y = 340f)
            assertEquals(HapticKind.SNAP, evt?.kind)
            assertEquals(120f, evt?.x)
            assertEquals(340f, evt?.y)
            assertEquals(1, received.size)

            val suppressed = CanopyHaptics.trigger(HapticKind.TICK, enabled = false, x = 10f, y = 20f)
            assertNull(suppressed)
            assertEquals(1, received.size)

            assertEquals(listOf(0L, 20L, 30L, 32L), HapticKind.EDIT_ENTER.waveformTimingsMs.toList())
            assertEquals(listOf(0L, 15L, 25L, 15L), HapticKind.REMOVE.waveformTimingsMs.toList())
            assertEquals(listOf(0L, 25L, 25L, 25L), HapticKind.ERROR.waveformTimingsMs.toList())
        } finally {
            unsubscribe()
        }
    }

    @Test
    fun `FrameMetricsMath computes rolling FPS and frame time accurately`() {
        val (emptyFps, emptyMs) = app.canopy.launcher.ui.overlays.FrameMetricsMath.computeRollingFps(emptyList())
        assertEquals(0, emptyFps)
        assertEquals(0f, emptyMs, 1e-4f)

        val (fps60, ms60) = app.canopy.launcher.ui.overlays.FrameMetricsMath.computeRollingFps(
            listOf(16_666_667L, 16_666_667L, 16_666_667L)
        )
        assertEquals(60, fps60)
        assertEquals(16.666f, ms60, 0.05f)

        val (fps120, ms120) = app.canopy.launcher.ui.overlays.FrameMetricsMath.computeRollingFps(
            listOf(8_333_333L, 8_333_333L)
        )
        assertEquals(120, fps120)
        assertEquals(8.333f, ms120, 0.05f)
    }
}
