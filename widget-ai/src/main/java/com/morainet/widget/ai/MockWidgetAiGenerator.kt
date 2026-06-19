package com.morainet.widget.ai

import com.morainet.widget.dsl.*
import java.util.UUID

/**
 * Mock 生成器：在不依赖外部 AI API 的情况下生成 [WidgetBlueprint]。
 *
 * 用于：
 * - 开发期验证整个 AI 管线
 * - 单元测试
 * - API 不可用时的回退方案
 *
 * 基于关键词匹配，从 10 种预设模板中选择最匹配的。
 */
class MockWidgetAiGenerator : WidgetAiGenerator {

    override suspend fun generate(
        prompt: String,
        constraints: AiGenerationConstraints?,
    ): WidgetAiResult {
        val startTime = System.currentTimeMillis()

        val blueprint = matchAndGenerate(prompt, constraints)

        return WidgetAiResult(
            blueprint = blueprint,
            metadata = AiGenerationMetadata(
                model = "mock-template-v1",
                latencyMs = System.currentTimeMillis() - startTime,
                totalTokens = null,
                isFallback = true,
                qualityScore = QualityEvaluator.evaluate(blueprint, constraints).score,
            ),
        )
    }

    private fun matchAndGenerate(
        prompt: String,
        constraints: AiGenerationConstraints?,
    ): WidgetBlueprint {
        val lower = prompt.lowercase()

        // 按优先级匹配关键词
        return when {
            lower.contains("weather") || lower.contains("天气") ->
                buildWeatherWidget(constraints)
            lower.contains("counter") || lower.contains("计数器") || lower.contains("count") ->
                buildCounterWidget(constraints)
            lower.contains("todo") || lower.contains("task") || lower.contains("待办") || lower.contains("任务") ->
                buildTodoWidget(constraints)
            lower.contains("streak") || lower.contains("打卡") || lower.contains("连续") ->
                buildStreakWidget(constraints)
            lower.contains("stock") || lower.contains("股票") || lower.contains("price") ->
                buildStockWidget(constraints)
            lower.contains("calendar") || lower.contains("日历") || lower.contains("date") ->
                buildCalendarWidget(constraints)
            lower.contains("quote") || lower.contains("名言") || lower.contains("quote") ->
                buildQuoteWidget(constraints)
            lower.contains("fitness") || lower.contains("健身") || lower.contains("step") ->
                buildFitnessWidget(constraints)
            lower.contains("clock") || lower.contains("时钟") || lower.contains("time") ->
                buildClockWidget(constraints)
            else ->
                buildGenericWidget(prompt, constraints)
        }
    }

    // ---- 10 种预设模板 ----

    private fun buildWeatherWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "WeatherWidget",
            description = "Displays current weather with city, temperature, and condition icon",
        ),
        layout = constraints?.preferredLayout ?: WidgetLayout.SINGLE_ENTITY_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "city",
                props = mapOf("text" to "Shanghai", "style" to "headline"),
            ),
            WidgetComponent(
                type = ComponentType.IMAGE,
                id = "weather_icon",
                props = mapOf("src" to "@drawable/ic_weather_sunny"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "temperature",
                props = mapOf("text" to "26°C", "style" to "value"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "condition",
                props = mapOf("text" to "Sunny", "style" to "caption"),
            ),
            WidgetComponent(
                type = ComponentType.BUTTON,
                id = "refresh_btn",
                props = mapOf("action" to "ACTION_REFRESH", "label" to "Refresh"),
            ),
        ),
        theme = WidgetTheme(style = "material_you", useDynamicColor = true),
        state = WidgetStateConfig(
            defaultState = "loading",
            onError = ErrorConfig(layout = "compact", showRetry = true),
        ),
    )

    private fun buildCounterWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "CounterWidget",
            description = "Simple counter with increment and reset buttons",
        ),
        layout = WidgetLayout.COUNTER_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "count_label",
                props = mapOf("text" to "Count", "style" to "headline"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "count_value",
                props = mapOf("text" to "0", "style" to "value"),
            ),
            WidgetComponent(
                type = ComponentType.BUTTON,
                id = "increment_btn",
                props = mapOf("action" to "ACTION_INCREMENT", "label" to "+"),
            ),
            WidgetComponent(
                type = ComponentType.BUTTON,
                id = "reset_btn",
                props = mapOf("action" to "ACTION_RESET", "label" to "Reset"),
            ),
        ),
        animations = if (constraints?.allowAnimations != false) listOf(
            WidgetAnimationConfig(preset = "COUNTER_TICK", targetId = "count_value"),
        ) else emptyList(),
    )

    private fun buildTodoWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "TodoWidget",
            description = "Displays a list of tasks with completion status",
        ),
        layout = WidgetLayout.LIST_4X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "title",
                props = mapOf("text" to "Tasks", "style" to "headline"),
            ),
            WidgetComponent(
                type = ComponentType.LIST,
                id = "task_list",
                props = mapOf("maxItems" to "3", "showCheckbox" to "true"),
            ),
        ),
        theme = WidgetTheme(style = "material_you", useDynamicColor = true),
    )

    private fun buildStreakWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "StreakWidget",
            description = "Track consecutive days of activity",
        ),
        layout = WidgetLayout.STREAK_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "streak_label",
                props = mapOf("text" to "Streak", "style" to "headline"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "streak_count",
                props = mapOf("text" to "7 days", "style" to "value"),
            ),
            WidgetComponent(
                type = ComponentType.PROGRESS,
                id = "streak_progress",
                props = mapOf("progress" to "0.7", "label" to "7/10"),
            ),
        ),
    )

    private fun buildStockWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "StockWidget",
            description = "Display stock price and daily change",
        ),
        layout = WidgetLayout.SINGLE_ENTITY_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "stock_symbol",
                props = mapOf("text" to "AAPL", "style" to "headline"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "stock_price",
                props = mapOf("text" to "$182.63", "style" to "value"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "stock_change",
                props = mapOf("text" to "+1.2%", "style" to "caption"),
            ),
        ),
    )

    private fun buildCalendarWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "CalendarWidget",
            description = "Shows today's date and upcoming events",
        ),
        layout = WidgetLayout.SINGLE_ENTITY_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "date",
                props = mapOf("text" to "June 19", "style" to "headline"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "day_of_week",
                props = mapOf("text" to "Friday", "style" to "caption"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "next_event",
                props = mapOf("text" to "Team meeting 2:00 PM", "style" to "body"),
            ),
        ),
    )

    private fun buildQuoteWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "QuoteWidget",
            description = "Daily inspirational quote",
        ),
        layout = WidgetLayout.SINGLE_ENTITY_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "quote_text",
                props = mapOf("text" to "\"The only way to do great work is to love what you do.\"", "style" to "body"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "quote_author",
                props = mapOf("text" to "— Steve Jobs", "style" to "caption"),
            ),
            WidgetComponent(
                type = ComponentType.BUTTON,
                id = "next_quote_btn",
                props = mapOf("action" to "ACTION_REFRESH", "label" to "Next"),
            ),
        ),
    )

    private fun buildFitnessWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "FitnessWidget",
            description = "Display daily step count and activity progress",
        ),
        layout = WidgetLayout.SINGLE_ENTITY_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "step_count",
                props = mapOf("text" to "8,421", "style" to "value"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "step_label",
                props = mapOf("text" to "steps today", "style" to "caption"),
            ),
            WidgetComponent(
                type = ComponentType.PROGRESS,
                id = "step_progress",
                props = mapOf("progress" to "0.84", "label" to "8,421 / 10,000"),
            ),
        ),
    )

    private fun buildClockWidget(constraints: AiGenerationConstraints?) = WidgetBlueprint(
        meta = WidgetMeta(
            name = "ClockWidget",
            description = "Analog-style clock with date",
        ),
        layout = WidgetLayout.SINGLE_ENTITY_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "time",
                props = mapOf("text" to "11:35", "style" to "value"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "date_display",
                props = mapOf("text" to "June 19, 2026", "style" to "caption"),
            ),
        ),
    )

    private fun buildGenericWidget(
        prompt: String,
        constraints: AiGenerationConstraints?,
    ): WidgetBlueprint = WidgetBlueprint(
        meta = WidgetMeta(
            name = "CustomWidget",
            description = prompt.take(80),
        ),
        layout = constraints?.preferredLayout ?: WidgetLayout.SINGLE_ENTITY_2X2,
        components = listOf(
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "title",
                props = mapOf("text" to prompt.take(30), "style" to "headline"),
            ),
            WidgetComponent(
                type = ComponentType.TEXT,
                id = "content",
                props = mapOf("text" to "Tap to configure", "style" to "body"),
            ),
        ),
    )
}
