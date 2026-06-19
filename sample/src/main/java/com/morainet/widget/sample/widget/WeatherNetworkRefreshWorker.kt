package com.morainet.widget.sample.widget

import android.content.Context
import androidx.work.WorkerParameters
import com.morainet.widget.workmanager.WidgetNetworkRefreshWorker

class WeatherNetworkRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : WidgetNetworkRefreshWorker(context, params) {

    override val widget = WeatherWidget()

    override suspend fun fetchData() {
        val manager = androidx.glance.appwidget.GlanceAppWidgetManager(applicationContext)
        val ids = manager.getGlanceIds(WeatherWidget::class.java)

        try {
            val apiResponse = WeatherApiService.fetchCurrentWeather()
            val data = WeatherRepository.transform(apiResponse)
            ids.forEach { id ->
                WeatherWidget.saveState(applicationContext, id, data)
            }
        } catch (e: Exception) {
            val restored = WeatherRepository.restoreFromCache(applicationContext, ids)
            if (restored) return

            val errorMsg = "Weather unavailable: ${e.message ?: "network error"}"
            ids.forEach { id ->
                WeatherWidget.saveError(applicationContext, id, errorMsg)
            }
        }
    }
}
