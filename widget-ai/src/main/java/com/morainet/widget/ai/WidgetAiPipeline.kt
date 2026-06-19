package com.morainet.widget.ai

import com.morainet.widget.dsl.WidgetBlueprint

/**
 * Widget AI 管线：编排从 Prompt → Blueprint → 验证 的完整流程。
 *
 * 职责：
 * 1. 自动选择后端（Gemini API → Mock 回退）
 * 2. 质量评估
 * 3. 回退机制
 *
 * 用法：
 * ```kotlin
 * val pipeline = WidgetAiPipeline(
 *     primary = GeminiWidgetAiGenerator(apiKey = "..."),
 *     fallback = MockWidgetAiGenerator(),
 * )
 * val result = pipeline.generate("一个天气 Widget")
 * ```
 */
class WidgetAiPipeline(
    private val primary: WidgetAiGenerator? = null,
    private val fallback: WidgetAiGenerator = MockWidgetAiGenerator(),
    private val qualityThreshold: Float = 0.6f,
) {

    /**
     * 生成 WidgetBlueprint。
     *
     * 流程：
     * 1. 尝试 primary 生成器
     * 2. 质量评估
     * 3. 不达标时自动回退到 fallback
     */
    suspend fun generate(
        prompt: String,
        constraints: AiGenerationConstraints? = null,
    ): WidgetAiResult {
        // 1. 尝试 primary 生成器
        if (primary != null) {
            try {
                val result = primary.generate(prompt, constraints)
                val quality = QualityEvaluator.evaluate(result.blueprint, constraints)

                if (quality.score >= qualityThreshold) {
                    return result.copy(
                        metadata = result.metadata.copy(qualityScore = quality.score),
                    )
                }

                // 质量不达标，记录并回退
                println("[WidgetAiPipeline] Primary quality ${quality.score} < threshold $qualityThreshold, falling back")
            } catch (e: Exception) {
                println("[WidgetAiPipeline] Primary generator failed: ${e.message}, falling back")
            }
        }

        // 2. 回退到 fallback
        val fallbackResult = fallback.generate(prompt, constraints)
        val quality = QualityEvaluator.evaluate(fallbackResult.blueprint, constraints)

        return fallbackResult.copy(
            metadata = fallbackResult.metadata.copy(
                isFallback = true,
                qualityScore = quality.score,
            ),
        )
    }
}
