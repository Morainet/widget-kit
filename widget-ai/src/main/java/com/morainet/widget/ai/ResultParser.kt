package com.morainet.widget.ai

import com.morainet.widget.dsl.WidgetBlueprint
import com.morainet.widget.dsl.WidgetBlueprintParser

/**
 * 结果解析器：将 LLM 原始输出解析为 [WidgetBlueprint]。
 *
 * 处理常见的 LLM 输出问题：
 * - JSON 被包裹在 ```json ... ``` 代码块中
 * - 前后有额外文字说明
 * - 字段名大小写不一致
 */
object ResultParser {

    /**
     * 解析 LLM 原始输出文本。
     *
     * @param rawOutput LLM 的原始响应文本
     * @return 解析成功返回 [WidgetBlueprint]，失败返回 null
     */
    fun parse(rawOutput: String): WidgetBlueprint? {
        val json = extractJson(rawOutput) ?: return null
        return try {
            WidgetBlueprintParser.parseJson(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 LLM 输出中提取纯 JSON 字符串。
     *
     * 处理策略（按优先级）：
     * 1. 整个输出就是 JSON（以 `{` 开头）
     * 2. JSON 被包裹在 ```json ... ``` 中
     * 3. JSON 被包裹在 ``` ... ``` 中
     * 4. 查找第一个 `{` 到最后一个 `}` 的范围
     */
    private fun extractJson(text: String): String? {
        val trimmed = text.trim()

        // 策略 1：整个输出就是 JSON
        if (trimmed.startsWith("{")) {
            return trimmed
        }

        // 策略 2：```json ... ```
        val jsonFence = Regex("```json\\s*\\n?([\\s\\S]*?)```")
        jsonFence.find(trimmed)?.let {
            return it.groupValues[1].trim()
        }

        // 策略 3：``` ... ```
        val codeFence = Regex("```\\s*\\n?([\\s\\S]*?)```")
        codeFence.find(trimmed)?.let {
            return it.groupValues[1].trim()
        }

        // 策略 4：查找 JSON 对象范围
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim()
        }

        return null
    }
}
