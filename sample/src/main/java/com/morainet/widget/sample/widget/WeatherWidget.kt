package com.morainet.widget.sample.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.morainet.widget.dsl.BlueprintRenderer
import com.morainet.widget.dsl.WidgetBlueprintParser
import com.morainet.widget.sample.R
import com.morainet.widget.state.WidgetStateStore
import com.morainet.widget.state.WidgetUiState
import com.morainet.widget.state.WidgetUiStateDefinition
import kotlinx.serialization.builtins.serializer

/**
 * 天气数据模型。
 *
 * @param city         城市名称
 * @param temperature  温度（如 "26°C"）
 * @param condition    天气状况描述（如 "Sunny"）
 * @param weatherCode  WMO 天气码
 * @param updatedAt    更新时间（如 "10:30"）
 */
@Serializable
data class WeatherData(
    val city: String,
    val temperature: String,
    val condition: String,
    val weatherCode: Int = 0,
    val updatedAt: String,
)

class WeatherWidget : GlanceAppWidget() {

    override val stateDefinition = WidgetUiStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val blueprint = WidgetBlueprintParser.parseYaml(
            context.resources.openRawResource(R.raw.weather_widget).bufferedReader().readText(),
        )
        val state = WidgetStateStore.load(
            context = context,
            glanceId = id,
            decode = { json -> Json.decodeFromString<WeatherData>(json) },
            default = WidgetUiState.Loading,
        )
        provideContent {
            val displayState = when (state) {
                is WidgetUiState.Success -> WidgetUiState.Success(state.data.toDisplayMap())
                is WidgetUiState.Error -> state
                WidgetUiState.Loading -> WidgetUiState.Loading
            }
            BlueprintRenderer(blueprint = blueprint, state = displayState)
        }
    }

    companion object {
        suspend fun saveState(context: Context, glanceId: GlanceId, data: WeatherData) {
            WidgetStateStore.save(
                context = context,
                glanceId = glanceId,
                state = WidgetUiState.Success(data),
                encode = { Json.encodeToString(WeatherData.serializer(), it) },
            )
        }

        suspend fun saveError(context: Context, glanceId: GlanceId, message: String) {
            WidgetStateStore.save(
                context = context,
                glanceId = glanceId,
                state = WidgetUiState.Error(message, retryable = true),
                encode = { Json.encodeToString(String.serializer(), it) },
            )
        }
    }
}

/**
 * 将 [WeatherData] 转换为渲染器可用的显示映射。
 *
 * weather_icon 的值为 drawable 资源名（不含 @drawable/ 前缀），
 * 渲染器中会通过 DrawableResolver 按名称查找对应图标。
 */
private fun WeatherData.toDisplayMap(): Map<String, String> = mapOf(
    "city" to city,
    "weather_icon" to conditionIcon(),
    "updated_at" to "Updated $updatedAt · $temperature",
)

// ---------------------------------------------------------------------------
// WMO Weather Code → Condition + Icon 映射
// 参考: https://open-meteo.com/en/docs#weathervariables
// ---------------------------------------------------------------------------

/** WMO 天气码 → 天气状况描述。 */
private val wmoConditionMap: Map<Int, String> = mapOf(
    0 to "Clear Sky",
    1 to "Mainly Clear",
    2 to "Partly Cloudy",
    3 to "Overcast",
    45 to "Fog",
    48 to "Depositing Rime Fog",
    51 to "Light Drizzle",
    53 to "Moderate Drizzle",
    55 to "Dense Drizzle",
    56 to "Light Freezing Drizzle",
    57 to "Dense Freezing Drizzle",
    61 to "Slight Rain",
    63 to "Moderate Rain",
    65 to "Heavy Rain",
    66 to "Light Freezing Rain",
    67 to "Heavy Freezing Rain",
    71 to "Slight Snow",
    73 to "Moderate Snow",
    75 to "Heavy Snow",
    77 to "Snow Grains",
    80 to "Slight Rain Showers",
    81 to "Moderate Rain Showers",
    82 to "Violent Rain Showers",
    85 to "Slight Snow Showers",
    86 to "Heavy Snow Showers",
    95 to "Thunderstorm",
    96 to "Thunderstorm with Slight Hail",
    99 to "Thunderstorm with Heavy Hail",
)

/** WMO 天气码 → drawable 图标资源名。 */
private val wmoIconMap: Map<Int, String> = mapOf(
    0 to "ic_weather_sunny",
    1 to "ic_weather_sunny",
    2 to "ic_weather_cloudy",
    3 to "ic_weather_cloudy",
    45 to "ic_weather_windy",
    48 to "ic_weather_windy",
    51 to "ic_weather_rainy",
    53 to "ic_weather_rainy",
    55 to "ic_weather_rainy",
    56 to "ic_weather_rainy",
    57 to "ic_weather_rainy",
    61 to "ic_weather_rainy",
    63 to "ic_weather_rainy",
    65 to "ic_weather_rainy",
    66 to "ic_weather_rainy",
    67 to "ic_weather_rainy",
    71 to "ic_weather_snow",
    73 to "ic_weather_snow",
    75 to "ic_weather_snow",
    77 to "ic_weather_snow",
    80 to "ic_weather_rainy",
    81 to "ic_weather_rainy",
    82 to "ic_weather_rainy",
    85 to "ic_weather_snow",
    86 to "ic_weather_snow",
    95 to "ic_weather_thunder",
    96 to "ic_weather_thunder",
    99 to "ic_weather_thunder",
)

/** 根据 WMO weatherCode 获取对应的 drawable 图标资源名。 */
fun WeatherData.conditionIcon(): String {
    return wmoIconMap[weatherCode] ?: "ic_weather_unknown"
}

/** 根据 WMO weatherCode 获取天气状况描述。 */
fun conditionText(weatherCode: Int): String {
    return wmoConditionMap[weatherCode] ?: "Unknown"
}

class WeatherWidgetReceiver : com.morainet.widget.core.MoraineWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WeatherRefreshWorker.trigger(context)
    }

    override suspend fun onRetry(context: Context, intent: android.content.Intent) {
        WeatherRefreshWorker.trigger(context)
    }
}
