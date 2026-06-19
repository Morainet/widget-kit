package com.morainet.widget.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morainet.widget.core.WidgetPinHelper
import com.morainet.widget.debugger.WidgetDebugSnapshot
import com.morainet.widget.debugger.WidgetDebuggerPanel
import com.morainet.widget.debugger.WidgetInspector
import com.morainet.widget.preview.WidgetPreviewHost
import com.morainet.widget.preview.WidgetPreviewSizes
import com.morainet.widget.sample.widget.CounterWidgetContent
import com.morainet.widget.sample.widget.CounterWidgetReceiver
import com.morainet.widget.sample.widget.WeatherRefreshWorker
import com.morainet.widget.state.WidgetUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        seedDebuggerSnapshots()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleScreen(
                        onRefreshWeather = { WeatherRefreshWorker.trigger(this) },
                        onPinCounter = {
                            WidgetPinHelper.requestPin(
                                context = this,
                                receiver = CounterWidgetReceiver::class.java,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun seedDebuggerSnapshots() {
        WidgetInspector.record(
            WidgetDebugSnapshot(
                widgetId = 1,
                widgetClass = "com.morainet.widget.sample.widget.CounterWidget",
                remoteViewTree = null,
                lastUpdatedAt = System.currentTimeMillis(),
                updateSource = "manual",
            ),
        )
        WidgetInspector.record(
            WidgetDebugSnapshot(
                widgetId = 2,
                widgetClass = "com.morainet.widget.sample.widget.WeatherWidget",
                remoteViewTree = null,
                lastUpdatedAt = System.currentTimeMillis(),
                updateSource = "WorkManager",
            ),
        )
    }
}

@Composable
private fun SampleScreen(
    onRefreshWeather: () -> Unit,
    onPinCounter: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Morainet Widget Kit Sample", style = MaterialTheme.typography.headlineSmall)
        Text("Add Counter / Weather widgets from launcher")

        Text("Counter Preview (2x2)", style = MaterialTheme.typography.titleMedium)
        WidgetPreviewHost(displaySize = WidgetPreviewSizes.Medium_2x2) {
            CounterWidgetContent(count = 3, state = WidgetUiState.Success(3))
        }

        Button(onClick = onPinCounter) {
            Text("Pin Counter Widget")
        }

        Button(onClick = onRefreshWeather) {
            Text("Refresh Weather Widget")
        }

        Text("Widget Debugger", style = MaterialTheme.typography.titleMedium)
        WidgetDebuggerPanel(modifier = Modifier.fillMaxWidth())
    }
}
