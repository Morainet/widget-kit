package net.morainet.widget.sample.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.morainet.widget.dsl.BlueprintRenderer
import net.morainet.widget.dsl.WidgetBlueprintParser
import net.morainet.widget.sample.R
import net.morainet.widget.state.WidgetStateStore
import net.morainet.widget.state.WidgetUiState
import net.morainet.widget.state.WidgetUiStateDefinition

@Serializable
data class WeatherData(
    val city: String,
    val temperature: String,
    val condition: String,
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
                encode = { Json.encodeToString(it) },
            )
        }
    }
}

private fun WeatherData.toDisplayMap(): Map<String, String> = mapOf(
    "city" to city,
    "weather_icon" to condition,
    "updated_at" to "Updated $updatedAt · $temperature",
)

class WeatherWidgetReceiver : net.morainet.widget.core.MoraineWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WeatherRefreshWorker.trigger(context)
    }

    override suspend fun onRetry(context: Context, intent: android.content.Intent) {
        WeatherRefreshWorker.trigger(context)
    }
}
