package com.morainet.widget.ai

import com.morainet.widget.dsl.ComponentType
import com.morainet.widget.dsl.WidgetBlueprint
import com.morainet.widget.dsl.WidgetLayout
import com.morainet.widget.dsl.WidgetComponent
import com.morainet.widget.dsl.WidgetMeta
import com.morainet.widget.dsl.WidgetStateConfig
import com.morainet.widget.dsl.WidgetTheme
import com.morainet.widget.dsl.WidgetSurface
import com.morainet.widget.dsl.WidgetAnimationConfig
import com.morainet.widget.dsl.ErrorConfig

/**
 * 质量评估器：对 AI 生成的 [WidgetBlueprint] 进行质量评分。
 *
 * 评分维度：
 * - 结构完整性（必填字段）
 * - 布局匹配度
 * - 组件数量合理性
 * - ID 命名规范
 * - 组件与布局的适配度
 */
object QualityEvaluator {

    /**
     * 对生成的 Blueprint 进行质量评估。
     *
     * @param blueprint 生成的 Blueprint
     * @param constraints 原始约束条件（用于判断布局是否匹配）
     * @return 0.0 ~ 1.0 的质量评分
     */
    fun evaluate(
        blueprint: WidgetBlueprint,
        constraints: AiGenerationConstraints? = null,
    ): QualityReport {
        val checks = mutableListOf<QualityCheck>()
        var totalWeight = 0f
        var weightedScore = 0f

        // 1. 结构完整性 (权重 0.3)
        val completeness = checkCompleteness(blueprint)
        checks.add(completeness)
        totalWeight += 0.3f
        weightedScore += completeness.passed * 0.3f

        // 2. 布局匹配度 (权重 0.2)
        if (constraints?.preferredLayout != null) {
            val layoutMatch = checkLayoutMatch(blueprint, constraints.preferredLayout)
            checks.add(layoutMatch)
            totalWeight += 0.2f
            weightedScore += layoutMatch.passed * 0.2f
        }

        // 3. 组件数量合理性 (权重 0.2)
        val componentCount = checkComponentCount(blueprint, constraints?.maxComponents ?: 6)
        checks.add(componentCount)
        totalWeight += 0.2f
        weightedScore += componentCount.passed * 0.2f

        // 4. ID 命名规范 (权重 0.15)
        val idNaming = checkIdNaming(blueprint)
        checks.add(idNaming)
        totalWeight += 0.15f
        weightedScore += idNaming.passed * 0.15f

        // 5. 组件与布局适配度 (权重 0.15)
        val layoutFit = checkLayoutFit(blueprint)
        checks.add(layoutFit)
        totalWeight += 0.15f
        weightedScore += layoutFit.passed * 0.15f

        val score = if (totalWeight > 0f) weightedScore / totalWeight else 1.0f

        return QualityReport(
            score = score.coerceIn(0f, 1f),
            checks = checks,
            isPassable = score >= 0.6f,
        )
    }

    // ---- 检查方法 ----

    private fun checkCompleteness(blueprint: WidgetBlueprint): QualityCheck {
        var passed = 0
        var total = 0

        // meta.name 非空
        total++
        if (blueprint.meta.name.isNotBlank()) passed++

        // components 非空
        total++
        if (blueprint.components.isNotEmpty()) passed++

        // layout 有效
        total++
        passed++ // enum 总是有效

        return QualityCheck(
            name = "结构完整性",
            passed = passed.toFloat() / total,
            detail = "必填字段 $passed/$total",
        )
    }

    private fun checkLayoutMatch(
        blueprint: WidgetBlueprint,
        preferred: WidgetLayout,
    ): QualityCheck {
        val matched = blueprint.layout == preferred
        return QualityCheck(
            name = "布局匹配度",
            passed = if (matched) 1f else 0.5f,
            detail = "期望 ${preferred.name}，实际 ${blueprint.layout.name}",
        )
    }

    private fun checkComponentCount(
        blueprint: WidgetBlueprint,
        maxComponents: Int,
    ): QualityCheck {
        val count = blueprint.components.size
        val passed = when {
            count == 0 -> 0f
            count <= maxComponents -> 1f
            count <= maxComponents + 2 -> 0.6f
            else -> 0.3f
        }
        return QualityCheck(
            name = "组件数量合理性",
            passed = passed,
            detail = "$count 个组件（上限 $maxComponents）",
        )
    }

    private fun checkIdNaming(blueprint: WidgetBlueprint): QualityCheck {
        val total = blueprint.components.size
        if (total == 0) {
            return QualityCheck(name = "ID 命名规范", passed = 1f, detail = "无组件")
        }
        val valid = blueprint.components.count { component ->
            component.id.matches(Regex("^[a-z][a-z0-9_]*$"))
        }
        return QualityCheck(
            name = "ID 命名规范",
            passed = valid.toFloat() / total,
            detail = "$valid/$total 符合 snake_case",
        )
    }

    private fun checkLayoutFit(blueprint: WidgetBlueprint): QualityCheck {
        val types = blueprint.components.map { it.type }.toSet()
        val recommendedTypes = recommendedTypesForLayout(blueprint.layout)
        val overlap = types.intersect(recommendedTypes).size
        val score = if (recommendedTypes.isEmpty()) 1f
        else overlap.toFloat() / recommendedTypes.size.coerceAtLeast(1)

        return QualityCheck(
            name = "组件与布局适配度",
            passed = score,
            detail = "组件类型 ${types}，推荐 ${recommendedTypes}",
        )
    }

    private fun recommendedTypesForLayout(layout: WidgetLayout): Set<ComponentType> = when (layout) {
        WidgetLayout.COUNTER_2X2 -> setOf(ComponentType.TEXT, ComponentType.BUTTON)
        WidgetLayout.SINGLE_ENTITY_2X2 -> setOf(ComponentType.TEXT, ComponentType.IMAGE, ComponentType.BUTTON)
        WidgetLayout.SINGLE_ENTITY_2X1 -> setOf(ComponentType.TEXT, ComponentType.IMAGE)
        WidgetLayout.LIST_4X2 -> setOf(ComponentType.LIST, ComponentType.TEXT)
        WidgetLayout.STREAK_2X2 -> setOf(ComponentType.TEXT, ComponentType.PROGRESS)
        WidgetLayout.CUSTOM -> emptySet()
    }
}

/**
 * 质量评估报告。
 */
data class QualityReport(
    /** 综合评分 0.0 ~ 1.0 */
    val score: Float,
    /** 各项检查结果 */
    val checks: List<QualityCheck>,
    /** 是否可通过（评分 >= 0.6） */
    val isPassable: Boolean,
)

/**
 * 单项质量检查结果。
 */
data class QualityCheck(
    val name: String,
    /** 0.0 ~ 1.0 */
    val passed: Float,
    val detail: String = "",
)
