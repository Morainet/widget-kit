package net.morainet.widget.sample.widget

import android.content.Context
import androidx.work.WorkerParameters
import net.morainet.widget.workmanager.WidgetNetworkRefreshWorker

class WeatherNetworkRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : WidgetNetworkRefreshWorker(context, params) {

    override val widget = WeatherWidget()

    override suspend fun fetchData() {
        val data = WeatherRepository.fetch()
        val manager = androidx.glance.appwidget.GlanceAppWidgetManager(applicationContext)
        val ids = manager.getGlanceIds(WeatherWidget::class.java)
        ids.forEach { id ->
            net.morainet.widget.state.WidgetStateStore.save(
                context = applicationContext,
                glanceId = id,
                state = net.morainet.widget.state.WidgetUiState.Success(data),
                encode = { kotlinx.serialization.json.Json.encodeToString(it) },
            )
        }
    }
}
