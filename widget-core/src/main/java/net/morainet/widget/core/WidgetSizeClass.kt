package net.morainet.widget.core

/**
 * Widget 尺寸分级，用于响应式布局选择。
 */
enum class WidgetSizeClass {
    SMALL,
    MEDIUM,
    LARGE,
}

/**
 * 根据 dp 尺寸解析 [WidgetSizeClass]。
 */
object WidgetSizeClassResolver {

    fun resolve(widthDp: Int, heightDp: Int): WidgetSizeClass {
        val area = widthDp * heightDp
        return when {
            area <= 180 * 55 -> WidgetSizeClass.SMALL
            area <= 180 * 110 -> WidgetSizeClass.MEDIUM
            else -> WidgetSizeClass.LARGE
        }
    }
}
