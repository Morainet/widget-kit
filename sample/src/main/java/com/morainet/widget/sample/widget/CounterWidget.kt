package com.morainet.widget.sample.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.morainet.widget.core.MoraineWidgetReceiver
import com.morainet.widget.core.WidgetActions
import com.morainet.widget.state.WidgetUiState

private val CountKey = intPreferencesKey("counter_value")
private object CounterStateDefinition : androidx.glance.state.GlanceStateDefinition<Preferences> by PreferencesGlanceStateDefinition

class CounterWidget : GlanceAppWidget() {

    override val stateDefinition = CounterStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, CounterStateDefinition, id)
        val count = prefs[CountKey] ?: 0
        provideContent {
            CounterWidgetContent(count = count, state = WidgetUiState.Success(count))
        }
    }

    companion object {
        suspend fun increment(context: Context, glanceId: GlanceId) {
            updateAppWidgetState(context, CounterStateDefinition, glanceId) { prefs ->
                val current = prefs[CountKey] ?: 0
                prefs[CountKey] = current + 1
            }
        }
    }
}

@Composable
fun CounterWidgetContent(count: Int, state: WidgetUiState<Int>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(
                actionSendBroadcast(
                    android.content.Intent(WidgetActions.ACTION_REFRESH).apply {
                        component = android.content.ComponentName(
                            "com.morainet.widget.sample",
                            "com.morainet.widget.sample.widget.CounterWidgetReceiver",
                        )
                        putExtra("counter_increment", true)
                    },
                ),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            is WidgetUiState.Loading -> Text(text = "Loading...")
            is WidgetUiState.Success -> Text(text = "Count: ${state.data}")
            is WidgetUiState.Error -> Text(text = state.message)
        }
    }
}

class CounterWidgetReceiver : MoraineWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CounterWidget()

    override suspend fun onRetry(context: Context, intent: android.content.Intent) {
        // Counter 无网络请求，Retry 重置为 0
    }

    override fun routeCustomAction(context: Context, intent: android.content.Intent): Boolean {
        if (intent.action == WidgetActions.ACTION_REFRESH && intent.getBooleanExtra("counter_increment", false)) {
            launchUpdate(context, glanceAppWidget) {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(CounterWidget::class.java)
                ids.forEach { id -> CounterWidget.increment(context, id) }
            }
            return true
        }
        return super.routeCustomAction(context, intent)
    }
}
