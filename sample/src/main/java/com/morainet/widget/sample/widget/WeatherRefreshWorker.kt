package com.morainet.widget.sample.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import com.morainet.widget.state.WidgetUiState
import com.morainet.widget.state.WidgetStateStore
import com.morainet.widget.workmanager.WidgetRefreshWorker
import com.morainet.widget.workmanager.WidgetScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : WidgetRefreshWorker(context, params) {

    override val widget: GlanceAppWidget = WeatherWidget()

    override suspend fun fetchData() {
        val manager = GlanceAppWidgetManager(applicationContext)
        val ids = manager.getGlanceIds(WeatherWidget::class.java)

        try {
            val apiResponse = WeatherApiService.fetchCurrentWeather()
            val data = WeatherRepository.transform(apiResponse)

            ids.forEach { id ->
                WeatherWidget.saveState(applicationContext, id, data)
            }
        } catch (e: Exception) {
            // 尝试从离线缓存恢复
            val restored = WeatherRepository.restoreFromCache(applicationContext, ids)
            if (restored) return

            // 缓存不可用，保存错误状态
            val errorMsg = "Weather unavailable: ${e.message ?: "network error"}"
            ids.forEach { id ->
                WeatherWidget.saveError(applicationContext, id, errorMsg)
            }
        }
    }

    companion object {
        const val WORK_NAME = "weather_widget_refresh"

        fun schedule(context: Context) {
            WidgetScheduler.schedulePeriodic<WeatherRefreshWorker>(
                context = context,
                uniqueWorkName = WORK_NAME,
                intervalMinutes = 15,
            )
        }

        fun trigger(context: Context) {
            WidgetScheduler.scheduleOneTime<WeatherRefreshWorker>(
                context = context,
                uniqueWorkName = "${WORK_NAME}_on_demand",
            )
        }
    }
}

/**
 * 天气数据仓库：将 API 响应转换为 [WeatherData]。
 *
 * 支持：
 * - 真实 Open-Meteo API → WeatherData 转换
 * - 离线缓存回退
 * - WMO weather code → 天气描述 + 图标映射
 */
object WeatherRepository {

    /** 当前默认城市坐标（上海）。 */
    var latitude: Double = 31.23
    var longitude: Double = 121.47

    /** 城市名称映射（可通过 [registerCityName] 扩展）。 */
    private val cityNames: MutableMap<String, String> = mutableMapOf(
        "31.23,121.47" to "Shanghai",
        "39.90,116.40" to "Beijing",
        "22.54,114.06" to "Shenzhen",
        "30.57,104.07" to "Chengdu",
        "34.34,108.94" to "Xi'an",
    )

    fun registerCityName(lat: Double, lon: Double, name: String) {
        cityNames["$lat,$lon"] = name
    }

    private fun resolveCityName(): String {
        return cityNames["$latitude,$longitude"] ?: "Unknown City"
    }

    /**
     * 将 API 响应转换为 [WeatherData]。
     */
    fun transform(api: WeatherApiResponse): WeatherData {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return WeatherData(
            city = resolveCityName(),
            temperature = "${api.temperature.toInt()}°C",
            condition = conditionText(api.weatherCode),
            weatherCode = api.weatherCode,
            updatedAt = timeFormat.format(Date()),
        )
    }

    /**
     * 从缓存恢复上一次的天气数据（用于离线 fallback）。
     *
     * @return 是否成功恢复
     */
    suspend fun restoreFromCache(context: Context, ids: List<androidx.glance.GlanceId>): Boolean {
        if (ids.isEmpty()) return false
        val state = WidgetStateStore.load(
            context = context,
            glanceId = ids.first(),
            decode = { kotlinx.serialization.json.Json.decodeFromString<WeatherData>(it) },
            default = WidgetUiState.Loading,
        )
        return if (state is WidgetUiState.Success) {
            ids.forEach { id ->
                WeatherWidget.saveState(context, id, state.data)
            }
            true
        } else {
            false
        }
    }
}
