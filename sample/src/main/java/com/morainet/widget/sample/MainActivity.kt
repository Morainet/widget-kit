package com.morainet.widget.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.morainet.widget.sample.widget.WeatherRepository
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
                        onLocationChange = { lat, lon ->
                            WeatherRepository.latitude = lat
                            WeatherRepository.longitude = lon
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
    onLocationChange: (Double, Double) -> Unit,
) {
    var latInput by remember { mutableStateOf("31.23") }
    var lonInput by remember { mutableStateOf("121.47") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---------- 标题 ----------
        Text("Morainet Widget Kit Sample", style = MaterialTheme.typography.headlineSmall)
        Text("Add Counter / Weather widgets from launcher", style = MaterialTheme.typography.bodyMedium)

        // ---------- Counter Widget ----------
        Text("Counter Preview (2x2)", style = MaterialTheme.typography.titleMedium)
        WidgetPreviewHost(displaySize = WidgetPreviewSizes.Medium_2x2) {
            CounterWidgetContent(count = 3, state = WidgetUiState.Success(3))
        }

        Button(onClick = onPinCounter) {
            Text("Pin Counter Widget")
        }

        HorizontalDivider()

        // ---------- Weather Widget ----------
        Text("Weather Widget", style = MaterialTheme.typography.titleMedium)

        // 位置设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Location", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = lonInput,
                        onValueChange = { lonInput = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        latInput.toDoubleOrNull()?.let { lat ->
                            lonInput.toDoubleOrNull()?.let { lon ->
                                onLocationChange(lat, lon)
                            }
                        }
                    },
                ) {
                    Text("Update Location & Refresh")
                }
            }
        }

        // 刷新按钮
        Button(onClick = onRefreshWeather, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh Weather Widget")
        }

        HorizontalDivider()

        // ---------- Debugger ----------
        Text("Widget Debugger", style = MaterialTheme.typography.titleMedium)
        WidgetDebuggerPanel(modifier = Modifier.fillMaxWidth())
    }
}
