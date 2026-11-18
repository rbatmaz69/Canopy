package app.canopy.launcher.system

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import android.content.SharedPreferences
import app.canopy.launcher.data.TempPoint
import app.canopy.launcher.data.WeatherHour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import app.canopy.launcher.data.WeatherLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
data class WeatherCityPreset(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Weather shown by the tiles. The default value means "no data yet" — tiles render an
 * explicit empty state instead of made-up numbers while [isLiveFromNetwork] is false.
 */
@Immutable
@Serializable
data class WeatherSnapshot(
    val cityId: String = "",
    val cityName: String = "",
    val currentTempC: Int = 0,
    val highTempC: Int = 0,
    val lowTempC: Int = 0,
    val conditionLabel: String = "",
    val shortCondition: String = "",
    val rainChancePercent: Int = 0,
    val hourlyPoints: List<TempPoint> = emptyList(),
    val hourlyForecasts: List<WeatherHour> = emptyList(),
    val peakHourLabel: String = "",
    /** True once this snapshot came from a real forecast (live or cached from an earlier fetch). */
    val isLiveFromNetwork: Boolean = false,
    /** Wall-clock time of the fetch, e.g. "14:05". */
    val updatedAtLabel: String = "",
    val fetchedAtMillis: Long = 0L,
)

val LocalWeatherSnapshot = staticCompositionLocalOf { WeatherSnapshot() }

/**
 * Pure Kotlin Open-Meteo JSON parser & WMO weather-code mapper with offline fallback.
 */
object OpenMeteoWeatherParser {
    private val json = Json { ignoreUnknownKeys = true }

    val CITY_PRESETS: List<WeatherCityPreset> = listOf(
        WeatherCityPreset("berlin", "Berlin", 52.52, 13.41),
        WeatherCityPreset("muenchen", "München", 48.14, 11.58),
        WeatherCityPreset("hamburg", "Hamburg", 53.55, 9.99),
        WeatherCityPreset("koeln", "Köln", 50.94, 6.96),
        WeatherCityPreset("frankfurt", "Frankfurt", 50.11, 8.68),
        WeatherCityPreset("wien", "Wien", 48.21, 16.37),
        WeatherCityPreset("zuerich", "Zürich", 47.37, 8.54),
    )

    fun resolveCityPreset(id: String): WeatherCityPreset {
        val clean = id.trim().lowercase()
        return CITY_PRESETS.firstOrNull { it.id == clean } ?: CITY_PRESETS.first()
    }

    fun mapWmoWeatherCode(code: Int): Pair<String, String> = when (code) {
        0 -> "Klarer Himmel, sonnig" to "Sonnig"
        in 1..3 -> "Leicht bis stark bewölkt" to "Bewölkt"
        in 45..48 -> "Nebel oder Hochnebel" to "Nebelig"
        in 51..67 -> "Regnerisch, Schauer möglich" to "Leichter Regen"
        in 71..77 -> "Schneefall" to "Schnee"
        in 80..82 -> "Wechselhafte Regenschauer" to "Schauer"
        in 95..99 -> "Gewitter mit Böen" to "Gewitter"
        else -> "Bewölkt" to "Bewölkt"
    }

    private fun iconKeyForWmo(code: Int): String = when (code) {
        0, 1 -> "CloudSun"
        in 51..99 -> "CloudRain"
        else -> "Cloud"
    }

    fun parseForecastJson(
        rawJson: String,
        cityName: String = "Berlin",
        cityId: String = "berlin",
    ): WeatherSnapshot? = runCatching {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val current = root["current"]?.jsonObject ?: return null
        val currentTemp = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull?.roundToInt() ?: return null
        val wmoCode = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 2
        val (longDesc, shortDesc) = mapWmoWeatherCode(wmoCode)

        val hourly = root["hourly"]?.jsonObject
        val temps = hourly?.get("temperature_2m")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.doubleOrNull }
            .orEmpty()
        val rains = hourly?.get("precipitation_probability")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.intOrNull }
            .orEmpty()

        val highTemp = temps.maxOrNull()?.roundToInt() ?: currentTemp
        val lowTemp = temps.minOrNull()?.roundToInt() ?: currentTemp
        val maxRain = rains.maxOrNull()?.coerceIn(0, 100) ?: 0

        val curveIndices = listOf(6, 9, 12, 15, 18, 21)
        val curveLabels = listOf("06h", "09h", "12h", "15h", "18h", "21h")
        val curvePoints = if (temps.size >= 22) {
            curveIndices.zip(curveLabels).map { (idx, lbl) ->
                TempPoint(lbl, temps[idx].toFloat())
            }
        } else {
            emptyList()
        }

        val slotIndices = listOf(10, 13, 16, 19)
        val slotLabels = listOf("10h", "13h", "16h", "19h")
        val hourlySlots = if (temps.size >= 20) {
            slotIndices.zip(slotLabels).map { (idx, lbl) ->
                val icon = iconKeyForWmo(wmoCode)
                WeatherHour(lbl, "${temps[idx].roundToInt()}°", icon)
            }
        } else {
            emptyList()
        }

        val peakIdx = temps.indices.maxByOrNull { temps[it] }
        val peakHourLabel = peakIdx?.let { "Max. um ${it.toString().padStart(2, '0')} Uhr" }.orEmpty()

        WeatherSnapshot(
            cityId = cityId,
            cityName = cityName,
            currentTempC = currentTemp,
            highTempC = highTemp,
            lowTempC = lowTemp,
            conditionLabel = longDesc,
            shortCondition = shortDesc,
            rainChancePercent = maxRain,
            hourlyPoints = curvePoints,
            hourlyForecasts = hourlySlots,
            peakHourLabel = peakHourLabel,
            isLiveFromNetwork = true,
        )
    }.getOrNull()
}

/** City search via the Open-Meteo geocoding API (no API key, no location permission needed). */
object OpenMeteoGeocoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseResults(rawJson: String): List<WeatherLocation> = runCatching {
        val results = json.parseToJsonElement(rawJson).jsonObject["results"]?.jsonArray ?: return emptyList()
        results.mapNotNull { el ->
            val obj = el.jsonObject
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val lat = obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val lon = obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "$lat,$lon"
            val region = listOfNotNull(
                obj["admin1"]?.jsonPrimitive?.contentOrNull,
                obj["country"]?.jsonPrimitive?.contentOrNull,
            ).distinct().joinToString(", ")
            WeatherLocation(id = "geo-$id", name = name, latitude = lat, longitude = lon, region = region)
        }
    }.getOrDefault(emptyList())

    suspend fun search(query: String, language: String = Locale.getDefault().language): List<WeatherLocation> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://geocoding-api.open-meteo.com/v1/search?name=" +
                        URLEncoder.encode(clean, "UTF-8") + "&count=6&format=json&language=" + language
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                try {
                    if (conn.responseCode in 200..299) {
                        parseResults(conn.inputStream.bufferedReader().use { it.readText() })
                    } else emptyList()
                } finally {
                    conn.disconnect()
                }
            }.getOrDefault(emptyList())
        }
    }
}

/**
 * Fetches the forecast from Open-Meteo on Dispatchers.IO. The last successful forecast is
 * cached so the tiles show real (if older) data after a restart while offline.
 */
class LiveWeatherController(private val cache: SharedPreferences? = null) {
    private val cacheJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val _weatherFlow = MutableStateFlow(loadCached())
    val weatherFlow: StateFlow<WeatherSnapshot> = _weatherFlow.asStateFlow()

    /** True when the cached forecast is older than [maxAgeMs] (or missing). */
    fun isStale(maxAgeMs: Long = REFRESH_INTERVAL_MS, nowMs: Long = System.currentTimeMillis()): Boolean {
        val fetched = _weatherFlow.value.fetchedAtMillis
        return fetched <= 0L || nowMs - fetched >= maxAgeMs
    }

    suspend fun refreshForCity(cityId: String) {
        val preset = OpenMeteoWeatherParser.resolveCityPreset(cityId)
        refresh(
            latitude = preset.latitude,
            longitude = preset.longitude,
            cityName = preset.name,
            cityId = preset.id,
        )
    }

    suspend fun refresh(
        latitude: Double,
        longitude: Double,
        cityName: String,
        cityId: String,
    ) {
        val parsed = withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$latitude&longitude=$longitude" +
                        "&current=temperature_2m,weather_code" +
                        "&hourly=temperature_2m,precipitation_probability" +
                        "&forecast_days=1&timezone=auto"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }
                try {
                    if (conn.responseCode in 200..299) {
                        val raw = conn.inputStream.bufferedReader().use { it.readText() }
                        OpenMeteoWeatherParser.parseForecastJson(raw, cityName = cityName, cityId = cityId)
                    } else null
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }
        if (parsed != null) {
            val now = System.currentTimeMillis()
            val stamped = parsed.copy(
                fetchedAtMillis = now,
                updatedAtLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)),
            )
            _weatherFlow.value = stamped
            runCatching { cache?.edit()?.putString(CACHE_KEY, cacheJson.encodeToString(WeatherSnapshot.serializer(), stamped))?.apply() }
        } else if (_weatherFlow.value.cityId != cityId) {
            // Never show the previous city's forecast under a newly chosen place.
            _weatherFlow.value = WeatherSnapshot(cityId = cityId, cityName = cityName)
        }
    }

    private fun loadCached(): WeatherSnapshot {
        val raw = cache?.getString(CACHE_KEY, null) ?: return WeatherSnapshot()
        return runCatching { cacheJson.decodeFromString(WeatherSnapshot.serializer(), raw) }.getOrNull() ?: WeatherSnapshot()
    }

    companion object {
        const val REFRESH_INTERVAL_MS = 30L * 60L * 1000L
        private const val CACHE_KEY = "weather_snapshot_v1"
    }
}
