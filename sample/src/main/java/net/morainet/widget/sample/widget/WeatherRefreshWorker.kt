package net.morainet.widget.sample.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.morainet.widget.state.WidgetUiState
import net.morainet.widget.state.WidgetStateStore
import net.morainet.widget.workmanager.WidgetRefreshWorker
import net.morainet.widget.workmanager.WidgetScheduler

class WeatherRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : WidgetRefreshWorker(context, params) {

    override val widget: GlanceAppWidget = WeatherWidget()

    override suspend fun fetchData() {
        delay(300)
        val data = WeatherRepository.fetch()
        val manager = GlanceAppWidgetManager(applicationContext)
        val ids = manager.getGlanceIds(WeatherWidget::class.java)
        ids.forEach { id ->
            WidgetStateStore.save(
                context = applicationContext,
                glanceId = id,
                state = WidgetUiState.Success(data),
                encode = { Json.encodeToString(it) },
            )
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

object WeatherRepository {

    suspend fun fetch(): WeatherData {
        delay(200)
        return WeatherData(
            city = "Shanghai",
            temperature = "26°C",
            condition = "Sunny",
            updatedAt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date()),
        )
    }
}
