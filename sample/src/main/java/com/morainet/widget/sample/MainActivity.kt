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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morainet.widget.ai.AppFunctionRegistry
import com.morainet.widget.ai.AppFunctions
import com.morainet.widget.ai.ImageSource
import com.morainet.widget.ai.MockImageToWidgetGenerator
import com.morainet.widget.ai.MockWidgetAiGenerator
import com.morainet.widget.ai.QualityEvaluator
import com.morainet.widget.ai.StandardActions
import com.morainet.widget.ai.WidgetAiPipeline
import com.morainet.widget.ai.WidgetAiResult
import com.morainet.widget.core.WidgetPinHelper
import com.morainet.widget.debugger.WidgetDebugSnapshot
import com.morainet.widget.debugger.WidgetDebuggerPanel
import com.morainet.widget.debugger.WidgetInspector
import com.morainet.widget.dsl.DrawableResolver
import com.morainet.widget.dsl.WidgetBlueprintParser
import com.morainet.widget.preview.WidgetPreviewHost
import com.morainet.widget.preview.WidgetPreviewSizes
import com.morainet.widget.sample.widget.CounterWidgetReceiver
import com.morainet.widget.sample.widget.WeatherRefreshWorker
import com.morainet.widget.sample.widget.WeatherRepository
import com.morainet.widget.state.WidgetUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册 AppFunctions（AI-Ready 桥接）
        registerAppFunctions()

        // 注册图标映射
        DrawableResolver.registerMapping(
            mapOf(
                "weather_sunny" to R.drawable.ic_weather_sunny,
                "weather_cloudy" to R.drawable.ic_weather_cloudy,
                "weather_rainy" to R.drawable.ic_weather_rainy,
                "weather_snow" to R.drawable.ic_weather_snow,
                "weather_thunder" to R.drawable.ic_weather_thunder,
                "weather_wind" to R.drawable.ic_weather_windy,
            ),
        )

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

    private fun registerAppFunctions() {
        AppFunctionRegistry.register(object : AppFunctions {
            override val bindings: Map<String, (android.content.Context) -> Unit> = mapOf(
                StandardActions.OPEN_APP to { context: android.content.Context ->
                    // Already in app, could navigate to a specific screen
                    android.widget.Toast.makeText(
                        context, "Open App action triggered", android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                StandardActions.REFRESH_DATA to { context: android.content.Context ->
                    WeatherRefreshWorker.trigger(context)
                    android.widget.Toast.makeText(
                        context, "Refreshing weather data...", android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                StandardActions.INCREMENT_COUNTER to { context: android.content.Context ->
                    android.widget.Toast.makeText(
                        context, "Counter incremented!", android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                StandardActions.RESET_COUNTER to { context: android.content.Context ->
                    android.widget.Toast.makeText(
                        context, "Counter reset!", android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                StandardActions.CHECK_IN to { context: android.content.Context ->
                    android.widget.Toast.makeText(
                        context, "Check-in recorded!", android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                StandardActions.VIEW_DETAILS to { context: android.content.Context ->
                    android.widget.Toast.makeText(
                        context, "View details triggered", android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
            )
        })
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
    val scope = rememberCoroutineScope()

    // AI 生成状态
    var aiPrompt by remember { mutableStateOf("一个 2x2 天气 Widget，显示城市名、温度和天气图标") }
    var aiResult by remember { mutableStateOf<WidgetAiResult?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    val pipeline = remember {
        WidgetAiPipeline(fallback = MockWidgetAiGenerator())
    }

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
            // 使用标准 Compose 组件模拟 Glance CounterWidget 预览
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Count: 3", style = MaterialTheme.typography.headlineMedium)
            }
        }

        Button(onClick = onPinCounter) {
            Text("Pin Counter Widget")
        }

        HorizontalDivider()

        // ---------- Weather Widget ----------
        Text("Weather Widget", style = MaterialTheme.typography.titleMedium)

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

        Button(onClick = onRefreshWeather, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh Weather Widget")
        }

        HorizontalDivider()

        // ---------- AI Widget Generator ----------
        Text("AI Widget Generator", style = MaterialTheme.typography.titleMedium)
        Text("Describe a widget in natural language and generate its Blueprint", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = aiPrompt,
            onValueChange = { aiPrompt = it },
            label = { Text("Prompt") },
            placeholder = { Text("e.g. 一个 2x2 天气 Widget，显示城市、温度和图标") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        // 预设 Prompt 快速选择
        Text("Quick Prompts:", style = MaterialTheme.typography.labelSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val quickPrompts = listOf(
                "天气" to "一个 2x2 天气 Widget，显示城市名、温度和天气图标",
                "计数器" to "一个计数器 Widget，有 + 和 Reset 按钮",
            )
            quickPrompts.forEach { (label, prompt) ->
                Button(
                    onClick = { aiPrompt = prompt },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val quickPrompts = listOf(
                "待办" to "一个待办事项列表 Widget，显示前3个任务",
                "打卡" to "一个连续打卡 Widget，显示打卡天数和进度条",
            )
            quickPrompts.forEach { (label, prompt) ->
                Button(
                    onClick = { aiPrompt = prompt },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    aiLoading = true
                    aiError = null
                    try {
                        val result = withContext(Dispatchers.IO) {
                            pipeline.generate(aiPrompt)
                        }
                        aiResult = result
                    } catch (e: Exception) {
                        aiError = e.message ?: "Unknown error"
                    } finally {
                        aiLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !aiLoading,
        ) {
            if (aiLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (aiLoading) "Generating..." else "Generate Widget Blueprint")
        }

        // AI 生成结果
        aiResult?.let { result ->
            AiResultCard(result)
        }

        aiError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        HorizontalDivider()

        // ---------- AppFunctions 信息 ----------
        Text("AppFunctions (AI-Ready)", style = MaterialTheme.typography.titleMedium)
        Text("Actions available for AI-generated BUTTON components:", style = MaterialTheme.typography.bodySmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val actions = AppFunctionRegistry.availableActions()
                actions.forEach { action ->
                    val isRegistered = AppFunctionRegistry.hasAction(action)
                    val icon = if (isRegistered) "✓" else "✗"
                    Text(
                        text = "  $icon $action",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                if (actions.isEmpty()) {
                    Text("No actions registered", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider()

        // ---------- Debugger ----------
        Text("Widget Debugger", style = MaterialTheme.typography.titleMedium)
        WidgetDebuggerPanel(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AiResultCard(result: WidgetAiResult) {
    val quality = QualityEvaluator.evaluate(result.blueprint)
    val qualityColor = when {
        quality.score >= 0.8f -> MaterialTheme.colorScheme.primary
        quality.score >= 0.6f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 元数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Generated Blueprint", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Quality: ${"%.0f".format(quality.score * 100)}%",
                    color = qualityColor,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Model: ${result.metadata.model}", style = MaterialTheme.typography.labelSmall)
                Text("Latency: ${result.metadata.latencyMs}ms", style = MaterialTheme.typography.labelSmall)
                if (result.metadata.isFallback) {
                    Text("fallback", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Blueprint 信息
            Text("Layout: ${result.blueprint.layout.name}", style = MaterialTheme.typography.bodySmall)
            Text("Components: ${result.blueprint.components.size}", style = MaterialTheme.typography.bodySmall)
            result.blueprint.components.forEach { component ->
                val actionTag = component.props["action"]
                val actionNote = if (actionTag != null && AppFunctionRegistry.hasAction(actionTag)) {
                    " [bound: $actionTag]"
                } else if (actionTag != null) {
                    " [unbound: $actionTag]"
                } else ""
                Text(
                    text = "  - [${component.type.name}] ${component.id}$actionNote",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 质量检查详情
            Text("Quality Checks:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            quality.checks.forEach { check ->
                val icon = if (check.passed >= 0.8f) "+" else if (check.passed >= 0.5f) "~" else "-"
                Text(
                    text = "  $icon ${check.name}: ${check.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Blueprint JSON 预览
            Text("Blueprint JSON:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                text = WidgetBlueprintParser.toJson(result.blueprint),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 视觉预览
            Text("Preview:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            WidgetPreviewHost(displaySize = WidgetPreviewSizes.Medium_2x2) {
                // 使用标准 Compose 组件模拟 Blueprint 预览
                // BlueprintRenderer 是 Glance Composable，无法在标准 Compose 中渲染
                // 实际渲染通过 AppWidgetHostView + RemoteViews 完成
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = result.blueprint.layout.name,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    result.blueprint.components.take(3).forEach { component ->
                        Text(
                            text = "[${component.type.name}] ${component.id}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}
