package app.canopy.launcher.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SplitDirection {
    @SerialName("horizontal")
    HORIZONTAL,

    @SerialName("vertical")
    VERTICAL,
}

@Serializable
enum class TileSizeTier {
    XS,
    S,
    M,
    L,
    TICKER,
    LIST,
}

@Serializable
enum class DropZone {
    @SerialName("center")
    CENTER,

    @SerialName("left")
    LEFT,

    @SerialName("right")
    RIGHT,

    @SerialName("top")
    TOP,

    @SerialName("bottom")
    BOTTOM,
}

@Serializable
enum class TemplateKind {
    @SerialName("big-plus-3")
    BIG_PLUS_3,

    @SerialName("2x2")
    GRID_2X2,

    @SerialName("col-plus-grid")
    COL_PLUS_GRID,
}

@Serializable
enum class TileType {
    @SerialName("greeting")
    GREETING,

    @SerialName("temp-curve")
    TEMP_CURVE,

    @SerialName("calendar-timeline")
    CALENDAR_TIMELINE,

    @SerialName("reminders")
    REMINDERS,

    @SerialName("battery-steps")
    BATTERY_STEPS,

    @SerialName("clock")
    CLOCK,

    @SerialName("weather")
    WEATHER,

    @SerialName("calendar")
    CALENDAR,

    @SerialName("media")
    MEDIA,

    @SerialName("note")
    NOTE,

    @SerialName("photos")
    PHOTOS,

    @SerialName("battery")
    BATTERY,

    @SerialName("steps")
    STEPS,

    @SerialName("quick-toggles")
    QUICK_TOGGLES,

    @SerialName("app-group")
    APP_GROUP,

    @SerialName("single-app")
    SINGLE_APP,

    @SerialName("third-party-widget")
    THIRD_PARTY_WIDGET,

    @SerialName("search-bar")
    SEARCH_BAR,

    @SerialName("newly-installed")
    NEWLY_INSTALLED,

    @SerialName("all-apps-box")
    ALL_APPS_BOX,
}

@Serializable
enum class WidgetKind {
    @SerialName("podcast")
    PODCAST,

    @SerialName("fitness")
    FITNESS,
}

@Serializable
enum class TogglePreset {
    @SerialName("system")
    SYSTEM,

    @SerialName("today")
    TODAY,

    @SerialName("media")
    MEDIA,

    @SerialName("tools")
    TOOLS,
}

@Serializable
data class ReminderItem(
    val id: String,
    val text: String,
    val done: Boolean,
    val time: String? = null,
)

@Serializable
data class TileData(
    val id: String,
    val type: TileType,
    val title: String,
    val subtitle: String? = null,
    val accentHue: Float? = null,
    val appIds: List<String>? = null,
    val appId: String? = null,
    val widgetKind: WidgetKind? = null,
    val widgetFixedWidth: Float? = null,
    val widgetFixedHeight: Float? = null,
    val widgetMutedColor: String? = null,
    val mediaLarge: Boolean? = null,
    val reminders: List<ReminderItem>? = null,
    val noteLines: List<String>? = null,
    val newAppIds: List<String>? = null,
    val togglePreset: TogglePreset? = null,
    val androidAppWidgetId: Int? = null,
)

@Serializable
sealed interface TreeNode {
    val id: String
}

@Serializable
@SerialName("leaf")
data class LeafNode(
    override val id: String,
    val tile: TileData,
) : TreeNode

@Serializable
@SerialName("split")
data class SplitNode(
    override val id: String,
    val direction: SplitDirection,
    /** Ratio of first child (0..1) relative to available span excluding gap */
    val ratio: Float,
    val first: TreeNode,
    val second: TreeNode,
) : TreeNode

@Serializable
data class RectFBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val area: Float get() = width * height
}

data class ComputedLeaf(
    val id: String,
    val tile: TileData,
    val rect: RectFBounds,
)

data class ComputedDivider(
    val id: String,
    val direction: SplitDirection,
    val ratio: Float,
    val rect: RectFBounds,
    val parentRect: RectFBounds,
    val minRatio: Float,
    val maxRatio: Float,
    val minFirstPx: Float,
    val minSecondPx: Float,
    val firstLeafIds: List<String>,
    val secondLeafIds: List<String>,
)

data class ComputedIntersection(
    val id: String,
    val x: Float,
    val y: Float,
    val horizontalSplits: List<ComputedDivider>,
    val verticalSplits: List<ComputedDivider>,
)

data class ComputedLayout(
    val leaves: List<ComputedLeaf>,
    val dividers: List<ComputedDivider>,
    val intersections: List<ComputedIntersection>,
)

@Serializable
enum class PageKind {
    @SerialName("tiles")
    TILES,

    @SerialName("all-apps")
    ALL_APPS,
}

@Serializable
data class LauncherPage(
    val id: String,
    val title: String,
    val kind: PageKind,
    val root: TreeNode,
)
