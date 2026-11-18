package app.canopy.launcher.system

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * A rendered app icon. For adaptive icons [full] is the full-bleed square (background +
 * foreground, the launcher applies its own shape); [monochrome] is the Android 13+ themed layer.
 */
data class LoadedAppIcon(
    val full: ImageBitmap,
    val monochrome: ImageBitmap?,
    val isAdaptive: Boolean,
)

/** Geometry helper for adaptive icons: the layers are 108 units, of which 72 are visible. */
object AdaptiveIconGeometry {
    /** Layer bounds (left/top offset and edge length) to render [sizePx] of visible icon. */
    fun layerBounds(sizePx: Int): Pair<Int, Int> {
        val layer = (sizePx * 1.5f).toInt()
        val offset = -(layer - sizePx) / 2
        return offset to layer
    }

    /** Buckets sizes so a handful of bitmaps per app cover every tile size. */
    fun bucket(sizePx: Int): Int = (((sizePx.coerceAtLeast(16)) + 31) / 32) * 32
}

/**
 * Loads launcher-activity icons per profile off the main thread and caches them by app id and
 * size. Entries are dropped when a package changes, so updated icons appear immediately.
 */
object AppIconRepository {
    private data class Target(val packageName: String, val activityName: String?, val user: UserHandle)

    private val targets = ConcurrentHashMap<String, Target>()
    private val cache = object : LruCache<String, LoadedAppIcon>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: LoadedAppIcon): Int =
            value.full.width * value.full.height * 4 + (value.monochrome?.let { it.width * it.height } ?: 0)
    }

    private val _version = MutableStateFlow(0)
    /** Bumped whenever cached icons were invalidated; icon composables reload on change. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun register(appId: String, packageName: String, activityName: String?, user: UserHandle) {
        targets[appId] = Target(packageName, activityName, user)
    }

    fun invalidatePackage(packageName: String) {
        val ids = targets.filterValues { it.packageName == packageName }.keys
        cache.snapshot().keys.filter { key -> ids.any { key.startsWith("$it|") } }.forEach { cache.remove(it) }
        _version.value += 1
    }

    fun cached(appId: String?, sizePx: Int): LoadedAppIcon? =
        appId?.let { cache.get(key(it, sizePx)) }

    suspend fun load(context: Context, appId: String?, sizePx: Int): LoadedAppIcon? {
        if (appId == null) return null
        cached(appId, sizePx)?.let { return it }
        val target = targets[appId] ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val drawable = resolveDrawable(context, target) ?: return@runCatching null
                render(drawable, sizePx)
            }.getOrNull()?.also { cache.put(key(appId, sizePx), it) }
        }
    }

    private fun key(appId: String, sizePx: Int) = "$appId|$sizePx"

    private fun resolveDrawable(context: Context, target: Target): Drawable? {
        val density = context.resources.displayMetrics.densityDpi
        val la = context.getSystemService(LauncherApps::class.java)
        val info = runCatching {
            la?.getActivityList(target.packageName, target.user)
                ?.firstOrNull { target.activityName == null || it.componentName.className == target.activityName }
        }.getOrNull()
        return runCatching { info?.getIcon(density) }.getOrNull()
            ?: runCatching { context.packageManager.getApplicationIcon(target.packageName) }.getOrNull()
    }

    private fun render(drawable: Drawable, sizePx: Int): LoadedAppIcon {
        if (drawable is AdaptiveIconDrawable) {
            val full = renderLayers(sizePx, drawable.background, drawable.foreground)
            val mono = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                drawable.monochrome?.let { renderLayers(sizePx, null, it) }
            } else null
            return LoadedAppIcon(full.asImageBitmap(), mono?.asImageBitmap(), isAdaptive = true)
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return LoadedAppIcon(bitmap.asImageBitmap(), null, isAdaptive = false)
    }

    private fun renderLayers(sizePx: Int, background: Drawable?, foreground: Drawable?): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val (offset, layer) = AdaptiveIconGeometry.layerBounds(sizePx)
        listOfNotNull(background, foreground).forEach { d ->
            d.setBounds(offset, offset, offset + layer, offset + layer)
            d.draw(canvas)
        }
        return bitmap
    }
}
