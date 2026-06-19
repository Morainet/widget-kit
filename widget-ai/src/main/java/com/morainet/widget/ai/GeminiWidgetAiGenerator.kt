package com.morainet.widget.ai

import com.morainet.widget.dsl.WidgetBlueprint
import com.morainet.widget.dsl.WidgetBlueprintParser
import kotlinx.serialization.json.*

/**
 * Gemini API 实现的 WidgetAiGenerator。
 *
 * 使用 Google Gemini API 将自然语言 Prompt 转换为 WidgetBlueprint JSON。
 *
 * 用法：
 * ```kotlin
 * val generator = GeminiWidgetAiGenerator(
 *     apiKey = "YOUR_API_KEY",
 *     model = "gemini-2.5-flash",  // 默认
 * )
 * val result = generator.generate("一个 2x2 天气 Widget，显示城市、温度和天气图标")
 * ```
 *
 * API 文档：https://ai.google.dev/gemini-api/docs
 */
class GeminiWidgetAiGenerator(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash",
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
) : WidgetAiGenerator {

    private val templateEngine = PromptTemplateEngine
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    override suspend fun generate(
        prompt: String,
        constraints: AiGenerationConstraints?,
    ): WidgetAiResult {
        val startTime = System.currentTimeMillis()

        val aiPrompt = templateEngine.compile(prompt, constraints)

        val response = callGeminiApi(aiPrompt)

        val rawText = extractResponseText(response)
        val blueprint = ResultParser.parse(rawText)
            ?: throw AiGenerationException("Failed to parse LLM output as WidgetBlueprint")

        val qualityReport = QualityEvaluator.evaluate(blueprint, constraints)

        return WidgetAiResult(
            blueprint = blueprint,
            metadata = AiGenerationMetadata(
                model = model,
                latencyMs = System.currentTimeMillis() - startTime,
                totalTokens = extractTokenCount(response),
                isFallback = false,
                qualityScore = qualityReport.score,
            ),
        )
    }

    // ---- Gemini API 调用 ----

    private suspend fun callGeminiApi(prompt: AiPrompt): JsonObject {
        val url = "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", prompt.system + "\n\n" + prompt.user)
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.4)
                put("topP", 0.95)
                put("maxOutputTokens", 2048)
                put("responseMimeType", "application/json")
            }
        }

        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000

        return try {
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } else {
                val errorBody = connection.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }
                throw AiGenerationException("Gemini API error ${connection.responseCode}: $errorBody")
            }

            json.parseToJsonElement(responseText).jsonObject
        } finally {
            connection.disconnect()
        }
    }

    // ---- 响应解析 ----

    private fun extractResponseText(response: JsonObject): String {
        return try {
            response
                .jsonArray("candidates")
                ?.getOrNull(0)
                ?.jsonObject
                ?.jsonObject("content")
                ?.jsonArray("parts")
                ?.getOrNull(0)
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
                ?: throw AiGenerationException("Unexpected Gemini response structure")
        } catch (e: AiGenerationException) {
            throw e
        } catch (e: Exception) {
            throw AiGenerationException("Failed to extract text from Gemini response: ${e.message}")
        }
    }

    private fun extractTokenCount(response: JsonObject): Int? {
        return try {
            response
                .jsonObject("usageMetadata")
                ?.get("totalTokenCount")
                ?.jsonPrimitive
                ?.int
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * AI 生成异常。
 */
class AiGenerationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
