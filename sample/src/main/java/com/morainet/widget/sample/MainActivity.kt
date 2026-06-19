package com.morainet.widget.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // 从资源文件加载所有颜色
    val bgDark = colorResource(R.color.widget_bg_dark)
    val textLabel = colorResource(R.color.widget_text_label)
    val textHint = colorResource(R.color.widget_text_hint)
    val textError = colorResource(R.color.widget_text_error)
    val textSuccess = colorResource(R.color.widget_text_success)
    val textWarning = colorResource(R.color.widget_text_warning)
    val textGray = colorResource(R.color.widget_text_gray)
    val textLightGray = colorResource(R.color.widget_text_light_gray)
    val textJsonPreview = colorResource(R.color.widget_text_json_preview)
    val textSectionLabel = colorResource(R.color.widget_text_section_label)
    val accent = colorResource(R.color.widget_accent)
    val cardBg = colorResource(R.color.widget_card_bg)
    val cardErrorBg = colorResource(R.color.widget_card_error_bg)
    val buttonQuickBg = colorResource(R.color.widget_button_quick_bg)

    // 深色主题配色
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            colorResource(R.color.widget_gradient_top),
            colorResource(R.color.widget_gradient_mid),
            colorResource(R.color.widget_gradient_bottom),
        ),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---------- 标题 ----------
            Text(
                "Widget Kit Sample",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Add Counter / Weather widgets from your launcher",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---------- Counter Widget ----------
            SectionHeader(
                title = "Counter Widget",
                subtitle = "Tap the counter on your home screen to +1",
                accent = accent,
                textLabel = textLabel,
            )
            WidgetPreviewHost(displaySize = WidgetPreviewSizes.Medium_2x2) {
                // 模拟 Glance CounterWidget 的深色半透明背景预览
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgDark),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TAP COUNTER",
                            color = textLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "3",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap to +1",
                            color = textHint,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Button(
                onClick = onPinCounter,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pin Counter Widget")
            }

            HorizontalDivider(color = cardBg)

            // ---------- Weather Widget ----------
            SectionHeader(
                title = "Weather Widget",
                subtitle = "Update location to fetch real weather data",
                accent = accent,
                textLabel = textLabel,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Location",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = textSectionLabel,
                    )
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
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Location & Refresh")
                    }
                }
            }

            Button(
                onClick = onRefreshWeather,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Weather Widget")
            }

            HorizontalDivider(color = cardBg)

            // ---------- AI Widget Generator ----------
            SectionHeader(
                title = "AI Widget Generator",
                subtitle = "Describe a widget in natural language",
                accent = accent,
                textLabel = textLabel,
            )

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
            Text(
                "Quick Prompts",
                style = MaterialTheme.typography.labelSmall,
                color = textLabel,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    "天气" to "一个 2x2 天气 Widget，显示城市名、温度和天气图标",
                    "计数器" to "一个计数器 Widget，有 + 和 Reset 按钮",
                ).forEach { (label, prompt) ->
                    Button(
                        onClick = { aiPrompt = prompt },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonQuickBg),
                        modifier = Modifier.weight(1f),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Text(label, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    "待办" to "一个待办事项列表 Widget，显示前3个任务",
                    "打卡" to "一个连续打卡 Widget，显示打卡天数和进度条",
                ).forEach { (label, prompt) ->
                    Button(
                        onClick = { aiPrompt = prompt },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonQuickBg),
                        modifier = Modifier.weight(1f),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
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
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (aiLoading) "Generating..." else "Generate Widget Blueprint")
            }

            // AI 生成结果
            aiResult?.let { result ->
                AiResultCard(
                    result = result,
                    textSuccess = textSuccess,
                    textWarning = textWarning,
                    textError = textError,
                    textLabel = textLabel,
                    textLightGray = textLightGray,
                    textSectionLabel = textSectionLabel,
                    textJsonPreview = textJsonPreview,
                    cardBg = cardBg,
                    bgDark = bgDark,
                    accent = accent,
                )
            }

            aiError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardErrorBg),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.padding(12.dp),
                        color = textError,
                    )
                }
            }

            HorizontalDivider(color = cardBg)

            // ---------- AppFunctions 信息 ----------
            SectionHeader(
                title = "AppFunctions (AI-Ready)",
                subtitle = "Actions available for AI-generated components",
                accent = accent,
                textLabel = textLabel,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val actions = AppFunctionRegistry.availableActions()
                    actions.forEach { action ->
                        val isRegistered = AppFunctionRegistry.hasAction(action)
                        val icon = if (isRegistered) "✓" else "✗"
                        val iconColor = if (isRegistered) textSuccess else textError
                        Text(
                            text = "  $icon $action",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = iconColor,
                        )
                    }
                    if (actions.isEmpty()) {
                        Text(
                            "No actions registered",
                            style = MaterialTheme.typography.bodySmall,
                            color = textGray,
                        )
                    }
                }
            }

            HorizontalDivider(color = cardBg)

            // ---------- Debugger ----------
            SectionHeader(
                title = "Widget Debugger",
                subtitle = "Runtime widget inspection & timeline",
                accent = accent,
                textLabel = textLabel,
            )
            WidgetDebuggerPanel(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    accent: Color = Color(0xFF66AAFF),
    textLabel: Color = Color(0xFF8888AA),
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accent,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = textLabel,
            modifier = Modifier.padding(start = 24.dp),
        )
    }
}

@Composable
private fun AiResultCard(
    result: WidgetAiResult,
    textSuccess: Color,
    textWarning: Color,
    textError: Color,
    textLabel: Color,
    textLightGray: Color,
    textSectionLabel: Color,
    textJsonPreview: Color,
    cardBg: Color,
    bgDark: Color,
    accent: Color,
) {
    val quality = QualityEvaluator.evaluate(result.blueprint)
    val qualityColor = when {
        quality.score >= 0.8f -> textSuccess
        quality.score >= 0.6f -> textWarning
        else -> textError
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 元数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Generated Blueprint",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = textSectionLabel,
                )
                Text(
                    text = "Quality: ${"%.0f".format(quality.score * 100)}%",
                    color = qualityColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Model: ${result.metadata.model}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textLabel,
                )
                Text(
                    "Latency: ${result.metadata.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = textLabel,
                )
                if (result.metadata.isFallback) {
                    Text(
                        "fallback",
                        style = MaterialTheme.typography.labelSmall,
                        color = textError,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Blueprint 信息
            Text(
                "Layout: ${result.blueprint.layout.name}",
                style = MaterialTheme.typography.bodySmall,
                color = textLightGray,
            )
            Text(
                "Components: ${result.blueprint.components.size}",
                style = MaterialTheme.typography.bodySmall,
                color = textLightGray,
            )
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
                    fontFamily = FontFamily.Monospace,
                    color = textLightGray,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 质量检查详情
            Text(
                "Quality Checks:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSectionLabel,
            )
            quality.checks.forEach { check ->
                val icon = if (check.passed >= 0.8f) "+" else if (check.passed >= 0.5f) "~" else "-"
                val iconColor = when {
                    check.passed >= 0.8f -> textSuccess
                    check.passed >= 0.5f -> textWarning
                    else -> textError
                }
                Text(
                    text = "  $icon ${check.name}: ${check.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = iconColor,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Blueprint JSON 预览
            Text(
                "Blueprint JSON:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSectionLabel,
            )
            Text(
                text = WidgetBlueprintParser.toJson(result.blueprint),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = textJsonPreview,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 视觉预览
            Text(
                "Preview:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSectionLabel,
            )
            Spacer(modifier = Modifier.height(4.dp))
            WidgetPreviewHost(displaySize = WidgetPreviewSizes.Medium_2x2) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgDark),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = result.blueprint.layout.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        result.blueprint.components.take(3).forEach { component ->
                            Text(
                                text = "[${component.type.name}] ${component.id}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = textJsonPreview,
                            )
                        }
                    }
                }
            }
        }
    }
}
