package com.morainet.widget.ai

import com.morainet.widget.dsl.*
import kotlinx.serialization.json.*

/**
 * Prompt 模板引擎：将用户自然语言 + 约束条件编译为结构化的 LLM Prompt。
 *
 * 职责：
 * - 注入 Schema 定义（WidgetBlueprint 的 JSON 结构）
 * - 注入 Canonical Layout 描述
 * - 注入组件类型文档
 * - 注入约束条件
 * - 注入 Few-shot 示例
 */
object PromptTemplateEngine {

    /**
     * 编译完整的 System Prompt + User Prompt。
     */
    fun compile(
        userPrompt: String,
        constraints: AiGenerationConstraints? = null,
    ): AiPrompt {
        val systemPrompt = buildSystemPrompt(constraints)
        val userMessage = buildUserMessage(userPrompt, constraints)
        return AiPrompt(system = systemPrompt, user = userMessage)
    }

    // ---- System Prompt ----

    private fun buildSystemPrompt(constraints: AiGenerationConstraints?): String {
        val sb = StringBuilder()
        sb.appendLine("You are a Widget Blueprint generator for Android Widget Kit.")
        sb.appendLine()
        sb.appendLine("Output ONLY valid JSON matching this schema:")
        sb.appendLine("```json")
        sb.appendLine(SCHEMA_JSON)
        sb.appendLine("```")
        sb.appendLine()

        // Layout 约束
        if (constraints?.preferredLayout != null) {
            sb.appendLine("REQUIRED layout: ${constraints.preferredLayout.name}")
        } else {
            sb.appendLine("Available layouts: ${WidgetLayout.entries.joinToString { it.name }}")
        }
        sb.appendLine()

        // 组件约束
        val allowedTypes = constraints?.allowedComponentTypes ?: ComponentType.entries.toSet()
        sb.appendLine("Allowed component types: ${allowedTypes.joinToString { it.name }}")
        if (constraints != null && constraints.maxComponents > 0) {
            sb.appendLine("Maximum components: ${constraints.maxComponents}")
        }
        sb.appendLine()

        // 主题约束
        sb.appendLine("Theme style: ${constraints?.themeStyle ?: "material_you"}")
        sb.appendLine("useDynamicColor: true")
        sb.appendLine()

        // Canonical Layout 参考
        sb.appendLine("## Canonical Layout Reference")
        sb.appendLine()
        sb.appendLine(LAYOUT_REFERENCE)
        sb.appendLine()

        // Component Type 参考
        sb.appendLine("## Component Type Reference")
        sb.appendLine()
        sb.appendLine(COMPONENT_REFERENCE)
        sb.appendLine()

        // Rules
        sb.appendLine("## Rules")
        sb.appendLine("- Every component MUST have a unique `id` (snake_case)")
        sb.appendLine("- TEXT components: `id` should describe the data (e.g. city, temperature, count)")
        sb.appendLine("- IMAGE components: `id` should describe the icon (e.g. weather_icon, app_icon)")
        sb.appendLine("- BUTTON components: `id` should describe the action (e.g. refresh_btn)")
        sb.appendLine("- PROGRESS components: `id` should describe the metric (e.g. progress_bar)")
        sb.appendLine("- LIST components: `id` should describe the data source (e.g. task_list)")
        sb.appendLine("- CHART components: `id` should describe the data (e.g. weekly_chart)")
        sb.appendLine("- Target surfaces default to [\"PHONE\"] unless the user says otherwise")
        sb.appendLine("- minSdk defaults to 26")
        sb.appendLine("- Output ONLY the JSON object, no markdown fences, no explanations")

        return sb.toString()
    }

    // ---- User Prompt ----

    private fun buildUserMessage(
        prompt: String,
        constraints: AiGenerationConstraints?,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Generate a WidgetBlueprint JSON for the following description:")
        sb.appendLine()
        sb.appendLine("\"$prompt\"")
        sb.appendLine()

        if (!constraints?.context.isNullOrEmpty()) {
            sb.appendLine("Additional context:")
            constraints.context.forEach { (k, v) ->
                sb.appendLine("- $k: $v")
            }
            sb.appendLine()
        }

        if (constraints?.allowAnimations == true) {
            sb.appendLine("You may include animations if the widget would benefit (e.g. counter tick, pulse on update).")
        } else {
            sb.appendLine("Do NOT include animations.")
        }

        return sb.toString()
    }

    // ---- Vision Prompt（Image → WidgetBlueprint） ----

    /**
     * 编译 Vision Prompt：用于 Gemini Vision API 从截图/设计稿生成 WidgetBlueprint。
     */
    fun compileVisionPrompt(
        hint: String? = null,
        constraints: AiGenerationConstraints? = null,
        mimeType: String = "image/png",
    ): AiPrompt {
        val systemPrompt = buildVisionSystemPrompt(constraints)
        val userMessage = buildVisionUserMessage(hint, constraints, mimeType)
        return AiPrompt(system = systemPrompt, user = userMessage)
    }

    private fun buildVisionSystemPrompt(constraints: AiGenerationConstraints?): String {
        val sb = StringBuilder()
        sb.appendLine("You are a Widget Blueprint generator for Android Widget Kit.")
        sb.appendLine("Your task is to analyze the provided image (a Widget screenshot or design mockup)")
        sb.appendLine("and convert it into a WidgetBlueprint JSON.")
        sb.appendLine()
        sb.appendLine("## Steps")
        sb.appendLine("1. Identify the Widget layout type from the image (2x2, 4x2, 2x1, etc.)")
        sb.appendLine("2. Identify all visible UI components (text, images, buttons, progress bars, lists, charts)")
        sb.appendLine("3. For each component, determine its type, a semantic id, and relevant props")
        sb.appendLine("4. Output ONLY the WidgetBlueprint JSON")
        sb.appendLine()
        sb.appendLine("## Schema")
        sb.appendLine("```json")
        sb.appendLine(SCHEMA_JSON)
        sb.appendLine("```")
        sb.appendLine()

        if (constraints?.preferredLayout != null) {
            sb.appendLine("PREFERRED layout: ${constraints.preferredLayout.name}")
        } else {
            sb.appendLine("Available layouts: ${WidgetLayout.entries.joinToString { it.name }}")
        }
        sb.appendLine()

        val allowedTypes = constraints?.allowedComponentTypes ?: ComponentType.entries.toSet()
        sb.appendLine("Allowed component types: ${allowedTypes.joinToString { it.name }}")
        sb.appendLine()
        sb.appendLine(LAYOUT_REFERENCE)
        sb.appendLine()
        sb.appendLine(COMPONENT_REFERENCE)
        sb.appendLine()
        sb.appendLine("## Rules")
        sb.appendLine("- Identify the layout based on the image's visible structure")
        sb.appendLine("- Every component MUST have a unique `id` (snake_case)")
        sb.appendLine("- Match component types to what you see in the image")
        sb.appendLine("- If text is visible, create TEXT components with appropriate ids")
        sb.appendLine("- If icons/images are visible, create IMAGE components")
        sb.appendLine("- If buttons are visible, create BUTTON components with action props")
        sb.appendLine("- Target surfaces default to [\"PHONE\"]")
        sb.appendLine("- minSdk defaults to 26")
        sb.appendLine("- Output ONLY the JSON object, no markdown fences, no explanations")
        sb.appendLine("- If you cannot identify the layout, use CUSTOM")

        return sb.toString()
    }

    private fun buildVisionUserMessage(
        hint: String?,
        constraints: AiGenerationConstraints?,
        mimeType: String,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Analyze the attached $mimeType image and generate a WidgetBlueprint JSON.")
        sb.appendLine()

        if (!hint.isNullOrBlank()) {
            sb.appendLine("Context hint: \"$hint\"")
            sb.appendLine()
        }

        sb.appendLine("Look at the visual layout in the image:")
        sb.appendLine("- How many cells wide/tall is this widget?")
        sb.appendLine("- What components do you see? (text labels, icons, buttons, etc.)")
        sb.appendLine("- What is the approximate layout structure?")
        sb.appendLine()
        sb.appendLine("Generate the WidgetBlueprint JSON now.")

        if (!constraints?.context.isNullOrEmpty()) {
            sb.appendLine()
            sb.appendLine("Additional context:")
            constraints.context.forEach { (k, v) ->
                sb.appendLine("- $k: $v")
            }
        }

        return sb.toString()
    }

    // ---- Schema & Reference (编译时常量) ----

    private val SCHEMA_JSON = """
        {
          "meta": {
            "name": "WidgetName",
            "description": "Brief description",
            "minSdk": 26,
            "targetSurfaces": ["PHONE"]
          },
          "layout": "SINGLE_ENTITY_2X2",
          "components": [
            {
              "type": "TEXT",
              "id": "component_id",
              "props": { "text": "Default text", "style": "body" }
            }
          ],
          "state": {
            "defaultState": "loading",
            "onError": { "layout": "compact", "showRetry": true }
          },
          "theme": {
            "style": "material_you",
            "primaryColor": null,
            "useDynamicColor": true
          },
          "animations": []
        }
    """.trimIndent()

    private val LAYOUT_REFERENCE = """
        | COUNTER_2X2          | Counter, check-in        | 2x2 | 1-2 TEXT, 1-2 BUTTON      |
        | SINGLE_ENTITY_2X2    | Weather, status card     | 2x2 | 1-2 TEXT, 1 IMAGE, 1 BUTTON|
        | SINGLE_ENTITY_2X1    | Single-line info         | 2x1 | 1-2 TEXT, 0-1 IMAGE        |
        | LIST_4X2             | List data (tasks, news)  | 4x2 | 1 LIST + 1-2 TEXT          |
        | STREAK_2X2           | Streak tracker           | 2x2 | 2-3 TEXT, 1 PROGRESS       |
    """.trimIndent()

    private val COMPONENT_REFERENCE = """
        | TEXT     | Display text (supports style prop: headline, body, caption, value) |
        | IMAGE    | Display an icon or image (src prop: @drawable/icon_name)           |
        | BUTTON   | Action button (props: action, label)                                |
        | PROGRESS | Progress bar (props: progress="0.0~1.0", label)                    |
        | LIST     | Scrollable list container (children defined in props)              |
        | CHART    | Simple bar/line chart (props: data="csv", chartType="bar")         |
    """.trimIndent()
}

/**
 * 编译后的完整 Prompt。
 */
data class AiPrompt(
    val system: String,
    val user: String,
)
