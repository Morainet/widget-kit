package com.morainet.widget.ai

import com.morainet.widget.dsl.WidgetBlueprint
import com.morainet.widget.dsl.WidgetLayout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * 图片输入源，支持多种来源。
 */
sealed class ImageSource {
    /** 来自文件路径 */
    data class File(val path: String) : ImageSource()

    /** 来自字节数组（已加载到内存） */
    data class Bytes(val data: ByteArray, val mimeType: String = "image/png") : ImageSource()

    /** 来自 InputStream（如 ContentResolver） */
    data class Stream(val input: InputStream, val mimeType: String = "image/png") : ImageSource()
}

/**
 * Image → WidgetBlueprint 生成结果。
 */
data class ImageToWidgetResult(
    val blueprint: WidgetBlueprint,
    val metadata: ImageGenerationMetadata,
)

/**
 * Image 生成元数据。
 */
data class ImageGenerationMetadata(
    val model: String,
    val latencyMs: Long,
    val totalTokens: Int? = null,
    val imageSizeBytes: Long = 0,
    val imageMimeType: String = "image/png",
    val detectedLayout: WidgetLayout? = null,
    val isFallback: Boolean = false,
    val qualityScore: Float? = null,
)

/**
 * Image → WidgetBlueprint 生成器接口。
 *
 * 基于 Widget2Code 论文思路：Vision LLM 分析截图/设计稿 →
 * 提取 UI 组件树 → 映射到 WidgetBlueprint DSL。
 *
 * 实现类：
 * - [GeminiImageToWidgetGenerator]：使用 Gemini Vision API（多模态）
 * - [MockImageToWidgetGenerator]：基于图片元数据匹配预设模板
 */
interface ImageToWidgetGenerator {

    /**
     * 从图片生成 WidgetBlueprint。
     *
     * @param image 输入图片
     * @param hint 可选的文字提示（如 "这是一个 2x2 天气 Widget"）
     * @param constraints 生成约束
     * @return 生成结果
     */
    suspend fun generate(
        image: ImageSource,
        hint: String? = null,
        constraints: AiGenerationConstraints? = null,
    ): ImageToWidgetResult
}

/**
 * Gemini Vision API 实现 — 将 Widget 截图/设计稿转为 WidgetBlueprint。
 *
 * 使用 Gemini 多模态能力（gemini-2.5-flash 支持图片输入），
 * 结合 Vision Prompt 模板进行 Widget 结构识别与 DSL 生成。
 *
 * 用法：
 * ```kotlin
 * val generator = GeminiImageToWidgetGenerator(apiKey = "...")
 * val result = generator.generate(
 *     image = ImageSource.File("/sdcard/widget_design.png"),
 *     hint = "2x2 weather widget with icon and temperature",
 * )
 * // result.blueprint → WidgetBlueprint → BlueprintRenderer → Glance
 * ```
 */
class GeminiImageToWidgetGenerator(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash",
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
) : ImageToWidgetGenerator {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    override suspend fun generate(
        image: ImageSource,
        hint: String?,
        constraints: AiGenerationConstraints?,
    ): ImageToWidgetResult {
        val start = System.currentTimeMillis()
        val imageBytes = image.toBytes()
        val base64Image = Base64.getEncoder().encodeToString(imageBytes.data)
        val mimeType = imageBytes.mimeType

        val visionPrompt = PromptTemplateEngine.compileVisionPrompt(
            hint = hint,
            constraints = constraints,
            mimeType = mimeType,
        )

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                putJsonObject {
                    putJsonArray("parts") {
                        putJsonObject {
                            put("text", visionPrompt.user)
                        }
                        putJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", mimeType)
                                put("data", base64Image)
                            }
                        }
                    }
                }
            }
            putJsonObject("generation_config") {
                put("temperature", 0.3)
                put("top_p", 0.95)
                put("max_output_tokens", 2048)
                put("response_mime_type", "application/json")
            }
            putJsonObject("system_instruction") {
                putJsonArray("parts") {
                    putJsonObject {
                        put("text", visionPrompt.system)
                    }
                }
            }
        }

        val url = URL("$baseUrl/v1beta/models/$model:generateContent?key=$apiKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 60_000
            readTimeout = 60_000
        }

        return try {
            connection.outputStream.use { os: OutputStream ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseBody = if (connection.responseCode in 200..299) {
                connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } else {
                val errorBody = connection.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: "Unknown error"
                throw AiGenerationException("Gemini Vision API error ${connection.responseCode}: $errorBody")
            }

            val responseJson = json.parseToJsonElement(responseBody).jsonObject
            val text = extractResponseText(responseJson)
            val blueprint = ResultParser.parse(text)
                ?: throw AiGenerationException("Failed to parse WidgetBlueprint from Vision response")

            val latency = System.currentTimeMillis() - start
            val quality = QualityEvaluator.evaluate(blueprint, constraints)

            ImageToWidgetResult(
                blueprint = blueprint,
                metadata = ImageGenerationMetadata(
                    model = model,
                    latencyMs = latency,
                    totalTokens = responseJson.extractUsageTokens(),
                    imageSizeBytes = imageBytes.data.size.toLong(),
                    imageMimeType = mimeType,
                    detectedLayout = blueprint.layout,
                    qualityScore = quality.score,
                ),
            )
        } catch (e: AiGenerationException) {
            throw e
        } catch (e: Exception) {
            throw AiGenerationException("Image to WidgetBlueprint generation failed: ${e.message}", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun extractResponseText(response: JsonObject): String {
        return try {
            response["candidates"]
                ?.jsonArray
                ?.getOrNull(0)
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("parts")
                ?.jsonArray
                ?.getOrNull(0)
                ?.jsonObject
                ?.get("text")
                ?.toString()
                ?.removeSurrounding("\"")
                ?: throw AiGenerationException("Empty response from Gemini Vision API")
        } catch (e: AiGenerationException) {
            throw e
        } catch (e: Exception) {
            throw AiGenerationException("Failed to extract text from Gemini Vision response", e)
        }
    }

    private fun JsonObject.extractUsageTokens(): Int? {
        return try {
            this["usageMetadata"]
                ?.jsonObject
                ?.get("totalTokenCount")
                ?.toString()
                ?.toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun ImageSource.toBytes(): ImageBytes {
        return when (this) {
            is ImageSource.Bytes -> ImageBytes(data, mimeType)
            is ImageSource.File -> {
                val file = java.io.File(path)
                ImageBytes(file.readBytes(), mimeType = detectMimeType(path))
            }
            is ImageSource.Stream -> {
                val baos = ByteArrayOutputStream()
                input.copyTo(baos)
                ImageBytes(baos.toByteArray(), mimeType)
            }
        }
    }

    private fun detectMimeType(path: String): String {
        return when {
            path.endsWith(".png", ignoreCase = true) -> "image/png"
            path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            path.endsWith(".webp", ignoreCase = true) -> "image/webp"
            path.endsWith(".gif", ignoreCase = true) -> "image/gif"
            path.endsWith(".bmp", ignoreCase = true) -> "image/bmp"
            else -> "image/png"
        }
    }

    private data class ImageBytes(val data: ByteArray, val mimeType: String)
}

/**
 * Mock Image → WidgetBlueprint 生成器。
 *
 * 当 Gemini API 不可用时，基于图片文件名的关键词匹配
 * 回退到预设模板（与 MockWidgetAiGenerator 共享 10 种模板）。
 */
class MockImageToWidgetGenerator : ImageToWidgetGenerator {

    private val mockTextGenerator = MockWidgetAiGenerator()

    override suspend fun generate(
        image: ImageSource,
        hint: String?,
        constraints: AiGenerationConstraints?,
    ): ImageToWidgetResult {
        val start = System.currentTimeMillis()

        // 从文件名和 hint 提取关键词
        val keywords = buildString {
            hint?.let { append(it).append(" ") }
            if (image is ImageSource.File) {
                append(java.io.File(image.path).nameWithoutExtension)
            }
        }

        val textResult = mockTextGenerator.generate(
            prompt = keywords.ifBlank { "generic widget" },
            constraints = constraints,
        )

        val latency = System.currentTimeMillis() - start

        return ImageToWidgetResult(
            blueprint = textResult.blueprint,
            metadata = ImageGenerationMetadata(
                model = "mock-vision",
                latencyMs = latency,
                imageSizeBytes = when (image) {
                    is ImageSource.File -> java.io.File(image.path).length()
                    is ImageSource.Bytes -> image.data.size.toLong()
                    is ImageSource.Stream -> 0L
                },
                isFallback = true,
                qualityScore = textResult.metadata.qualityScore,
            ),
        )
    }
}
