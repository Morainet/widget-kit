package com.morainet.widget.ai

import com.morainet.widget.dsl.WidgetBlueprint

/**
 * AI Widget 生成器的入口接口。
 *
 * 实现类负责将自然语言 Prompt 或截图转换为 [WidgetBlueprint]，
 * 支持多种后端（Gemini API / 本地模型 / Mock 回退）。
 */
interface WidgetAiGenerator {

    /**
     * 从自然语言 Prompt 生成 WidgetBlueprint。
     *
     * @param prompt 用户自然语言描述，如 "一个 2x2 天气 Widget，显示城市名、温度、天气图标"
     * @param constraints 可选约束条件（尺寸、目标表面等）
     * @return 生成的 [WidgetBlueprint]，如果生成失败返回 null
     */
    suspend fun generate(prompt: String, constraints: AiGenerationConstraints? = null): WidgetAiResult
}

/**
 * AI 生成结果，包含 Blueprint 和元数据。
 */
data class WidgetAiResult(
    val blueprint: WidgetBlueprint,
    val metadata: AiGenerationMetadata,
)

/**
 * AI 生成过程的元数据，用于调试和质量评估。
 */
data class AiGenerationMetadata(
    /** 使用的模型名称，如 "gemini-2.5-flash" */
    val model: String,
    /** 生成耗时（毫秒） */
    val latencyMs: Long,
    /** 消耗的 Token 数（输入 + 输出） */
    val totalTokens: Int? = null,
    /** 是否为 mock / 回退结果 */
    val isFallback: Boolean = false,
    /** 质量评分（0.0 ~ 1.0），由质量评估器给出 */
    val qualityScore: Float? = null,
)

/**
 * 生成约束条件，用于限定 AI 输出范围。
 */
data class AiGenerationConstraints(
    /** 目标布局类型 */
    val preferredLayout: com.morainet.widget.dsl.WidgetLayout? = null,
    /** 最大组件数 */
    val maxComponents: Int = 6,
    /** 允许的组件类型白名单（null 表示全部允许） */
    val allowedComponentTypes: Set<com.morainet.widget.dsl.ComponentType>? = null,
    /** 目标主题风格 */
    val themeStyle: String = "material_you",
    /** 是否允许动画 */
    val allowAnimations: Boolean = true,
    /** 额外上下文（如 App 名称、品牌色等） */
    val context: Map<String, String> = emptyMap(),
)
