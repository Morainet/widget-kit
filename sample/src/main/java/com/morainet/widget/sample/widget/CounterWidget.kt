package com.morainet.widget.sample.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider
import com.morainet.widget.core.MoraineWidgetReceiver
import com.morainet.widget.core.WidgetActions
import com.morainet.widget.sample.R
import com.morainet.widget.state.WidgetUiState

private val CountKey = intPreferencesKey("counter_value")
object CounterStateDefinition : androidx.glance.state.GlanceStateDefinition<Preferences> by PreferencesGlanceStateDefinition

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
                prefs.toMutablePreferences().apply {
                    val current = prefs[CountKey] ?: 0
                    this[CountKey] = current + 1
                }
            }
        }
    }
}

@Composable
fun CounterWidgetContent(count: Int, state: WidgetUiState<Int>) {
    val res = LocalContext.current.resources
    val bgColor = res.getColor(R.color.widget_bg_dark, null)
    val textWhite = res.getColor(R.color.widget_text_white, null)
    val textLabel = res.getColor(R.color.widget_text_label, null)
    val textHint = res.getColor(R.color.widget_text_hint, null)
    val textError = res.getColor(R.color.widget_text_error, null)
    val textLoading = res.getColor(R.color.widget_text_loading, null)

    // 美观的计数器 Widget：半透明深色背景 + 圆角卡片感
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(ComposeColor(bgColor)))
            .padding(12.dp)
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
            is WidgetUiState.Loading -> {
                Text(
                    text = "Loading...",
                    style = TextStyle(
                        color = ColorProvider(ComposeColor(textLoading)),
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            is WidgetUiState.Success -> {
                // 标签
                Text(
                    text = "TAP COUNTER",
                    style = TextStyle(
                        color = ColorProvider(ComposeColor(textLabel)),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                // 大数字
                Text(
                    text = "${state.data}",
                    style = TextStyle(
                        color = ColorProvider(ComposeColor(textWhite)),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                // 底部提示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "👆",
                        style = TextStyle(textAlign = TextAlign.Center),
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Tap to +1",
                        style = TextStyle(
                            color = ColorProvider(ComposeColor(textHint)),
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
            is WidgetUiState.Error -> {
                Text(
                    text = state.message,
                    style = TextStyle(
                        color = ColorProvider(ComposeColor(textError)),
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
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
