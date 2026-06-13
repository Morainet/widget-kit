package net.morainet.widget.dsl

import kotlinx.serialization.Serializable

/**
 * Widget DSL 根模型。AI 生成器与手写配置共用此 Schema。
 */
@Serializable
data class WidgetBlueprint(
    val meta: WidgetMeta,
    val layout: WidgetLayout,
    val components: List<WidgetComponent>,
    val state: WidgetStateConfig? = null,
    val theme: WidgetTheme? = null,
    val animations: List<WidgetAnimationConfig> = emptyList(),
)

@Serializable
data class WidgetMeta(
    val name: String,
    val description: String = "",
    val minSdk: Int = 26,
    val targetSurfaces: List<WidgetSurface> = listOf(WidgetSurface.PHONE),
)

@Serializable
enum class WidgetSurface {
    PHONE,
    WEAR,
    AUTO,
}

@Serializable
enum class WidgetLayout {
    COUNTER_2X2,
    SINGLE_ENTITY_2X2,
    SINGLE_ENTITY_2X1,
    LIST_4X2,
    STREAK_2X2,
    CUSTOM,
}

@Serializable
data class WidgetComponent(
    val type: ComponentType,
    val id: String,
    val props: Map<String, String> = emptyMap(),
)

@Serializable
enum class ComponentType {
    TEXT,
    IMAGE,
    BUTTON,
    PROGRESS,
    LIST,
    CHART,
}

@Serializable
data class WidgetStateConfig(
    val defaultState: String = "loading",
    val onError: ErrorConfig? = null,
)

@Serializable
data class ErrorConfig(
    val layout: String = "compact",
    val showRetry: Boolean = true,
)

@Serializable
data class WidgetTheme(
    val style: String = "material_you",
    val primaryColor: String? = null,
    val useDynamicColor: Boolean = true,
)

@Serializable
data class WidgetAnimationConfig(
    val preset: String,
    val targetId: String? = null,
    val durationMs: Long = 300,
)
