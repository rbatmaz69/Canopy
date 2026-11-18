package app.canopy.launcher.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import app.canopy.launcher.data.AnimationMood
import app.canopy.launcher.data.MotionSettings
import kotlin.math.max
import kotlin.math.sqrt

enum class SpringRole {
    DIVIDER_SNAP,
    PAGE_SWIPE,
    TILE_SWAP,
    GAP_OPEN,
    PRESS_FEEDBACK,
    GROUP_EXPAND,
}

data class BaseSpringSpec(
    val zeta: Float,
    val stiffness: Float,
)

data class ResolvedSpringSpec(
    val stiffness: Float,
    val dampingRatio: Float,
    val dampingCoefficient: Float,
)

object CanopySprings {
    val BASE_SPRINGS: Map<SpringRole, BaseSpringSpec> = mapOf(
        SpringRole.DIVIDER_SNAP to BaseSpringSpec(zeta = 0.75f, stiffness = 400f),
        SpringRole.PAGE_SWIPE to BaseSpringSpec(zeta = 0.85f, stiffness = 300f),
        SpringRole.TILE_SWAP to BaseSpringSpec(zeta = 0.70f, stiffness = 380f),
        SpringRole.GAP_OPEN to BaseSpringSpec(zeta = 0.80f, stiffness = 500f),
        SpringRole.PRESS_FEEDBACK to BaseSpringSpec(zeta = 0.60f, stiffness = 1500f),
        SpringRole.GROUP_EXPAND to BaseSpringSpec(zeta = 0.85f, stiffness = 260f),
    )

    private data class MoodModifier(val zetaMult: Float, val stiffnessMult: Float)

    private val MOOD_MODIFIERS: Map<AnimationMood, MoodModifier> = mapOf(
        AnimationMood.VERSPIELT to MoodModifier(zetaMult = 0.72f, stiffnessMult = 1.12f),
        AnimationMood.NORMAL to MoodModifier(zetaMult = 1f, stiffnessMult = 1f),
        AnimationMood.RUHIG to MoodModifier(zetaMult = 1.32f, stiffnessMult = 0.82f),
    )

    fun resolveSpring(
        role: SpringRole,
        settings: MotionSettings = MotionSettings(),
    ): ResolvedSpringSpec {
        val base = BASE_SPRINGS.getValue(role)
        val moodMod = MOOD_MODIFIERS.getValue(settings.mood)

        val rawStiffness = base.stiffness * settings.stiffnessScale * moodMod.stiffnessMult
        val rawZeta = max(0.32f, base.zeta * settings.dampingScale * moodMod.zetaMult)
        val rawDamping = 2f * rawZeta * sqrt(rawStiffness)

        // Slow motion 0.25x scales angular frequency omega by s=0.25 -> stiffness * s^2, damping * s
        val timeScale = if (settings.slowMotion) 0.25f else 1f
        val stiffness = rawStiffness * timeScale * timeScale
        val dampingCoefficient = rawDamping * timeScale

        return ResolvedSpringSpec(
            stiffness = stiffness,
            dampingRatio = rawZeta,
            dampingCoefficient = dampingCoefficient,
        )
    }

    fun <T> spec(
        role: SpringRole,
        settings: MotionSettings = MotionSettings(),
        visibilityThreshold: T? = null,
    ): SpringSpec<T> {
        val resolved = resolveSpring(role, settings)
        return spring(
            dampingRatio = resolved.dampingRatio,
            stiffness = resolved.stiffness,
            visibilityThreshold = visibilityThreshold,
        )
    }
}
