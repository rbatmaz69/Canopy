package app.canopy.launcher.system

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import app.canopy.launcher.data.SystemTileDefaults
import app.canopy.launcher.data.TempPoint
import app.canopy.launcher.data.WeatherHour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

@Immutable
data class WeatherCityPreset(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

@Immutable
data class WeatherSnapshot(
    val cityId: String = "berlin",
    val cityName: String = "Berlin",
    val currentTempC: Int = 7,
    val highTempC: Int = 11,
    val lowTempC: Int = 4,
    val conditionLabel: String = "Bewölkt, ab 17 Uhr leichter Regen",
    val shortCondition: String = "Regen ab 17 Uhr",
    val rainChancePercent: Int = 80,
    val hourlyPoints: List<TempPoint> = SystemTileDefaults.TEMP_DAY_SERIES,
    val hourlyForecasts: List<WeatherHour> = SystemTileDefaults.WEATHER_HOURS,
    val peakHourLabel: String = "15h Peak",
    val isLiveFromNetwork: Boolean = false,
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

        val highTemp = temps.maxOrNull()?.roundToInt() ?: maxOf(currentTemp + 2, 11)
        val lowTemp = temps.minOrNull()?.roundToInt() ?: minOf(currentTemp - 3, 4)
        val maxRain = rains.maxOrNull()?.coerceIn(0, 100) ?: 10

        val curveIndices = listOf(6, 9, 12, 15, 18, 21)
        val curveLabels = listOf("06h", "09h", "12h", "15h", "18h", "21h")
        val curvePoints = if (temps.size >= 22) {
            curveIndices.zip(curveLabels).map { (idx, lbl) ->
                TempPoint(lbl, temps[idx].toFloat())
            }
        } else {
            SystemTileDefaults.TEMP_DAY_SERIES
        }

        val slotIndices = listOf(10, 13, 16, 19)
        val slotLabels = listOf("10h", "13h", "16h", "19h")
        val hourlySlots = if (temps.size >= 20) {
            slotIndices.zip(slotLabels).map { (idx, lbl) ->
                val icon = iconKeyForWmo(wmoCode)
                WeatherHour(lbl, "${temps[idx].roundToInt()}°", icon)
            }
        } else {
            SystemTileDefaults.WEATHER_HOURS
        }

        val peakIdx = temps.indices.maxByOrNull { temps[it] } ?: 15
        val peakHourLabel = "${peakIdx.toString().padStart(2, '0')}h Peak"

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

/**
 * Fetches live Berlin/configurable forecast from Open-Meteo on Dispatchers.IO,
 * keeping instant offline fallback data if network is disabled.
 */
class LiveWeatherController {
    private val _weatherFlow = MutableStateFlow(WeatherSnapshot())
    val weatherFlow: StateFlow<WeatherSnapshot> = _weatherFlow.asStateFlow()

    suspend fun refreshForCity(cityId: String) {
        val preset = OpenMeteoWeatherParser.resolveCityPreset(cityId)
        _weatherFlow.value = _weatherFlow.value.copy(
            cityId = preset.id,
            cityName = preset.name,
        )
        refresh(
            latitude = preset.latitude,
            longitude = preset.longitude,
            cityName = preset.name,
            cityId = preset.id,
        )
    }

    suspend fun refresh(
        latitude: Double = 52.52,
        longitude: Double = 13.41,
        cityName: String = "Berlin",
        cityId: String = "berlin",
    ) {
        val parsed = withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$latitude&longitude=$longitude" +
                        "&current=temperature_2m,weather_code" +
                        "&hourly=temperature_2m,precipitation_probability&forecast_days=1"
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
            _weatherFlow.value = parsed
        }
    }
}
