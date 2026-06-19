package com.morainet.widget.ai.cli

import com.morainet.widget.ai.*
import com.morainet.widget.dsl.WidgetBlueprint
import com.morainet.widget.dsl.WidgetBlueprintParser
import kotlinx.coroutines.runBlocking

/**
 * Morainet Widget CLI — 命令行 WidgetBlueprint 生成工具。
 *
 * 用法：
 * ```
 * # 从文字 Prompt 生成
 * java -jar widget-cli.jar generate --prompt "2x2 weather widget"
 *
 * # 从图片生成
 * java -jar widget-cli.jar generate --image /path/to/design.png --hint "weather widget"
 *
 * # 指定输出格式
 * java -jar widget-cli.jar generate --prompt "todo list" --output json
 * java -jar widget-cli.jar generate --prompt "todo list" --output yaml
 *
 * # 使用 Gemini API（需要 API Key）
 * java -jar widget-cli.jar generate --prompt "counter widget" --api-key YOUR_KEY
 *
 * # 质量阈值（默认 0.6）
 * java -jar widget-cli.jar generate --prompt "stock ticker" --quality 0.7
 * ```
 *
 * 环境变量：
 * - `WIDGET_KIT_API_KEY`：Gemini API Key（与 --api-key 参数互斥）
 */
object WidgetCli {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }

        when (args[0]) {
            "generate" -> handleGenerate(args.drop(1))
            "validate" -> handleValidate(args.drop(1))
            "version" -> printVersion()
            "help", "--help", "-h" -> printUsage()
            else -> {
                System.err.println("Unknown command: ${args[0]}")
                printUsage()
            }
        }
    }

    // ---- generate 子命令 ----

    private fun handleGenerate(args: List<String>) {
        val params = parseGenerateParams(args)

        if (params.prompt == null && params.imagePath == null) {
            System.err.println("Error: --prompt or --image is required")
            printUsage()
            return
        }

        try {
            runBlocking {
                val result: GenerateResult = if (params.imagePath != null) {
                    generateFromImage(params)
                } else {
                    generateFromPrompt(params)
                }

                outputResult(result, params.outputFormat)
            }
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
            e.printStackTrace(System.err)
        }
    }

    private suspend fun generateFromPrompt(params: GenerateParams): GenerateResult {
        val apiKey = params.apiKey ?: System.getenv("WIDGET_KIT_API_KEY")

        val primary: WidgetAiGenerator? = if (!apiKey.isNullOrBlank()) {
            GeminiWidgetAiGenerator(apiKey = apiKey)
        } else {
            null // 无 API Key 时只用 fallback
        }

        val pipeline = WidgetAiPipeline(
            primary = primary,
            fallback = MockWidgetAiGenerator(),
            qualityThreshold = params.qualityThreshold,
        )

        val result = pipeline.generate(
            prompt = params.prompt!!,
            constraints = AiGenerationConstraints(
                preferredLayout = params.layout,
                maxComponents = params.maxComponents,
                allowedComponentTypes = params.componentTypes,
                themeStyle = params.theme,
                allowAnimations = params.animations,
            ),
        )

        return GenerateResult(
            blueprint = result.blueprint,
            model = result.metadata.model,
            latencyMs = result.metadata.latencyMs,
            qualityScore = result.metadata.qualityScore,
            isFallback = result.metadata.isFallback,
        )
    }

    private suspend fun generateFromImage(params: GenerateParams): GenerateResult {
        val apiKey = params.apiKey ?: System.getenv("WIDGET_KIT_API_KEY")

        val generator: ImageToWidgetGenerator = if (!apiKey.isNullOrBlank()) {
            GeminiImageToWidgetGenerator(apiKey = apiKey)
        } else {
            MockImageToWidgetGenerator()
        }

        val result = generator.generate(
            image = ImageSource.File(params.imagePath!!),
            hint = params.prompt,
            constraints = AiGenerationConstraints(
                preferredLayout = params.layout,
                maxComponents = params.maxComponents,
                allowedComponentTypes = params.componentTypes,
                themeStyle = params.theme,
                allowAnimations = params.animations,
            ),
        )

        return GenerateResult(
            blueprint = result.blueprint,
            model = result.metadata.model,
            latencyMs = result.metadata.latencyMs,
            qualityScore = result.metadata.qualityScore,
            isFallback = result.metadata.isFallback,
        )
    }

    // ---- validate 子命令 ----

    private fun handleValidate(args: List<String>) {
        if (args.isEmpty()) {
            System.err.println("Error: file path required")
            return
        }

        val filePath = args[0]
        val file = java.io.File(filePath)
        if (!file.exists()) {
            System.err.println("Error: file not found: $filePath")
            return
        }

        try {
            val content = file.readText()
            val blueprint = when {
                filePath.endsWith(".json") -> WidgetBlueprintParser.parseJson(content)
                filePath.endsWith(".yaml") || filePath.endsWith(".yml") -> WidgetBlueprintParser.parseYaml(content)
                else -> {
                    System.err.println("Error: unsupported format. Use .json or .yaml")
                    return
                }
            }

            val quality = QualityEvaluator.evaluate(blueprint)
            println("Validation: PASSED")
            println("  Name: ${blueprint.meta.name}")
            println("  Layout: ${blueprint.layout}")
            println("  Components: ${blueprint.components.size}")
            println("  Quality Score: ${"%.1f".format(quality.score * 100)}%")
            println()
            quality.checks.forEach { check ->
                val icon = if (check.passed >= 0.8) "✓" else if (check.passed >= 0.5) "⚠" else "✗"
                println("  $icon ${check.name}: ${"%.0f".format(check.passed * 100)}% — ${check.detail}")
            }
        } catch (e: Exception) {
            System.err.println("Validation: FAILED")
            System.err.println("  ${e.message}")
        }
    }

    // ---- 输出 ----

    private fun outputResult(result: GenerateResult, format: OutputFormat) {
        val blueprint = result.blueprint
        val output = when (format) {
            OutputFormat.JSON -> WidgetBlueprintParser.toJson(blueprint)
            OutputFormat.YAML -> {
                // 简单 YAML 转换
                val json = WidgetBlueprintParser.toJson(blueprint)
                jsonToYaml(json)
            }
        }

        // 元数据以注释形式输出到 stderr
        val modelTag = if (result.isFallback) "[FALLBACK]" else ""
        System.err.println("# Model: ${result.model} $modelTag")
        System.err.println("# Latency: ${result.latencyMs}ms")
        System.err.println("# Quality: ${"%.1f".format((result.qualityScore ?: 0f) * 100)}%")
        System.err.println()

        // Blueprint 输出到 stdout
        println(output)
    }

    // ---- 参数解析 ----

    private data class GenerateParams(
        val prompt: String? = null,
        val imagePath: String? = null,
        val apiKey: String? = null,
        val outputFormat: OutputFormat = OutputFormat.JSON,
        val layout: com.morainet.widget.dsl.WidgetLayout? = null,
        val maxComponents: Int = 6,
        val componentTypes: Set<com.morainet.widget.dsl.ComponentType>? = null,
        val theme: String = "material_you",
        val animations: Boolean = true,
        val qualityThreshold: Float = 0.6f,
    )

    private enum class OutputFormat { JSON, YAML }

    private data class GenerateResult(
        val blueprint: WidgetBlueprint,
        val model: String,
        val latencyMs: Long,
        val qualityScore: Float?,
        val isFallback: Boolean,
    )

    private fun parseGenerateParams(args: List<String>): GenerateParams {
        var prompt: String? = null
        var imagePath: String? = null
        var apiKey: String? = null
        var outputFormat = OutputFormat.JSON
        var layout: com.morainet.widget.dsl.WidgetLayout? = null
        var maxComponents = 6
        var componentTypes: Set<com.morainet.widget.dsl.ComponentType>? = null
        var theme = "material_you"
        var animations = true
        var qualityThreshold = 0.6f

        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--prompt" -> prompt = args.getOrNull(++i)
                "--image" -> imagePath = args.getOrNull(++i)
                "--hint" -> {
                    // hint 作为 prompt 的别名（image 模式下）
                    if (prompt == null) prompt = args.getOrNull(++i) else ++i
                }
                "--api-key" -> apiKey = args.getOrNull(++i)
                "--output" -> {
                    outputFormat = when (args.getOrNull(++i)?.lowercase()) {
                        "yaml", "yml" -> OutputFormat.YAML
                        else -> OutputFormat.JSON
                    }
                }
                "--layout" -> {
                    layout = try {
                        com.morainet.widget.dsl.WidgetLayout.valueOf(args.getOrNull(++i) ?: "")
                    } catch (_: Exception) {
                        System.err.println("Warning: unknown layout, ignoring")
                        null
                    }
                }
                "--max-components" -> {
                    maxComponents = args.getOrNull(++i)?.toIntOrNull() ?: 6
                }
                "--components" -> {
                    componentTypes = args.getOrNull(++i)
                        ?.split(",")
                        ?.mapNotNull { it.trim().uppercase() }
                        ?.mapNotNull { name ->
                            try { com.morainet.widget.dsl.ComponentType.valueOf(name) }
                            catch (_: Exception) { null }
                        }
                        ?.toSet()
                }
                "--theme" -> theme = args.getOrNull(++i) ?: "material_you"
                "--animations" -> {
                    animations = when (args.getOrNull(++i)?.lowercase()) {
                        "false", "no", "off" -> false
                        else -> true
                    }
                }
                "--quality" -> {
                    qualityThreshold = args.getOrNull(++i)?.toFloatOrNull() ?: 0.6f
                }
                else -> { /* unknown flag, skip */ }
            }
            i++
        }

        return GenerateParams(
            prompt = prompt,
            imagePath = imagePath,
            apiKey = apiKey,
            outputFormat = outputFormat,
            layout = layout,
            maxComponents = maxComponents,
            componentTypes = componentTypes,
            theme = theme,
            animations = animations,
            qualityThreshold = qualityThreshold,
        )
    }

    // ---- 帮助信息 ----

    private fun printUsage() {
        println("""
            |Morainet Widget CLI
            |
            |Usage: widget-cli <command> [options]
            |
            |Commands:
            |  generate    Generate WidgetBlueprint from prompt or image
            |  validate    Validate a WidgetBlueprint file
            |  version     Print version
            |  help        Show this help
            |
            |Generate options:
            |  --prompt <text>          Natural language description
            |  --image <path>           Path to widget screenshot/design
            |  --hint <text>            Context hint for image mode
            |  --api-key <key>          Gemini API key (or set WIDGET_KIT_API_KEY env)
            |  --output <json|yaml>     Output format (default: json)
            |  --layout <layout>        Preferred layout type
            |  --max-components <n>     Max component count (default: 6)
            |  --components <types>     Comma-separated allowed types
            |  --theme <style>          Theme style (default: material_you)
            |  --animations <on|off>    Enable/disable animations (default: on)
            |  --quality <0.0-1.0>      Quality threshold (default: 0.6)
            |
            |Validate options:
            |  <file>                   Path to .json or .yaml blueprint file
            |
            |Examples:
            |  widget-cli generate --prompt "2x2 weather widget with icon"
            |  widget-cli generate --image design.png --hint "weather widget"
            |  widget-cli generate --prompt "todo list" --output yaml
            |  widget-cli validate my_widget.json
            |
            |Environment:
            |  WIDGET_KIT_API_KEY       Gemini API Key
        """.trimMargin())
    }

    private fun printVersion() {
        println("morainet-widget-cli v0.1.0")
    }

    // ---- 工具方法 ----

    /**
     * 简单的 JSON → YAML 转换（仅处理 WidgetBlueprint 结构，不依赖第三方库）。
     */
    private fun jsonToYaml(json: String): String {
        // 简单实现：基于缩进转换
        val sb = StringBuilder()
        val lines = json.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed == "{" || trimmed == "}") continue

            val indent = line.indexOfFirst { it != ' ' } / 2
            val yamlLine = when {
                trimmed.startsWith("\"") || trimmed.endsWith("\"") -> {
                    val cleaned = trimmed.removeSurrounding("\"").removeSuffix(",")
                    val colonIdx = cleaned.indexOf(":")
                    if (colonIdx > 0) {
                        val key = cleaned.substring(0, colonIdx).trim().removeSurrounding("\"")
                        val value = cleaned.substring(colonIdx + 1).trim()
                            .removeSurrounding("\"").removeSuffix(",").trim()
                        if (value == "{" || value == "[") {
                            "$key:"
                        } else if (value == "null" || value == "") {
                            "$key: null"
                        } else {
                            "$key: $value"
                        }
                    } else {
                        cleaned
                    }
                }
                trimmed.endsWith("[") -> trimmed.removeSuffix("[").trim().removeSurrounding("\"") + ":"
                trimmed.endsWith("{") -> trimmed.removeSuffix("{").trim().removeSurrounding("\"") + ":"
                trimmed == "]," || trimmed == "}" -> continue
                else -> trimmed
            }
            sb.appendLine("  ".repeat(indent.coerceAtMost(6)) + yamlLine)
        }
        return sb.toString()
    }
}
