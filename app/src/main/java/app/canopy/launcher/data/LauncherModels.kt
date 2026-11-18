package app.canopy.launcher.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ShortcutBarMode {
    @SerialName("all")
    ALL,

    @SerialName("start-only")
    START_ONLY,

    @SerialName("off")
    OFF,
}

@Serializable
enum class ThemeVariantId {
    @SerialName("material-you")
    MATERIAL_YOU,

    @SerialName("dark-bold")
    DARK_BOLD,

    @SerialName("glass")
    GLASS,
}

@Serializable
enum class ColorMode {
    @SerialName("light")
    LIGHT,

    @SerialName("dark")
    DARK,
}

@Serializable
enum class WallpaperPresetId {
    @SerialName("sand")
    SAND,

    @SerialName("emerald")
    EMERALD,

    @SerialName("cobalt")
    COBALT,

    @SerialName("terracotta")
    TERRACOTTA,
}

@Serializable
enum class AnimationMood {
    @SerialName("verspielt")
    VERSPIELT,

    @SerialName("normal")
    NORMAL,

    @SerialName("ruhig")
    RUHIG,
}

@Serializable
data class MotionSettings(
    val stiffnessScale: Float = 1f,
    val dampingScale: Float = 1f,
    val mood: AnimationMood = AnimationMood.NORMAL,
    val hapticsEnabled: Boolean = true,
    val slowMotion: Boolean = false,
)

@Serializable
data class AlarmState(
    val enabled: Boolean = true,
    val timeLabel: String = "07:15",
)
