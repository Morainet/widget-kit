package com.morainet.widget.dsl

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import com.morainet.widget.core.WidgetActions
import com.morainet.widget.state.WidgetUiState

// ---------------------------------------------------------------------------
// Drawable 解析 - 可注册映射表
// ---------------------------------------------------------------------------

/**
 * Drawable 资源解析器。内置常用图标映射，同时支持通过 [register] / [registerMapping]
 * 动态注册自定义映射。
 */
object DrawableResolver {

    /** 内置映射表：逻辑名称 -> Android 系统 drawable 资源 ID。 */
    private val mappings: MutableMap<String, Int> = mutableMapOf(
        // 天气图标（ic_weather_* 命名规范）
        "ic_weather_sunny" to android.R.drawable.ic_menu_day,
        "ic_weather_cloudy" to android.R.drawable.ic_menu_compass,
        "ic_weather_rainy" to android.R.drawable.ic_menu_agenda,
        "ic_weather_snow" to android.R.drawable.ic_menu_month,
        "ic_weather_windy" to android.R.drawable.ic_menu_directions,
        "ic_weather_thunder" to android.R.drawable.ic_dialog_alert,
        "ic_weather_unknown" to android.R.drawable.ic_menu_help,
        // 兼容旧名称
        "ic_sunny" to android.R.drawable.ic_menu_day,
        "ic_cloudy" to android.R.drawable.ic_menu_compass,
        "ic_rain" to android.R.drawable.ic_menu_agenda,
        "ic_snow" to android.R.drawable.ic_menu_month,
        "ic_wind" to android.R.drawable.ic_menu_directions,
        // 通用图标
        "ic_thermometer" to android.R.drawable.ic_menu_manage,
        "ic_notification" to android.R.drawable.ic_menu_info_details,
        "ic_alert" to android.R.drawable.ic_dialog_alert,
        "ic_check" to android.R.drawable.ic_menu_manage,
        "ic_star" to android.R.drawable.star_on,
    )

    /** 默认 fallback 资源 ID。 */
    var fallbackResId: Int = android.R.drawable.ic_menu_gallery

    /**
     * 注册单个图标映射。
     *
     * @param name  逻辑图标名称（如 "ic_custom"）
     * @param resId Android drawable 资源 ID
     */
    fun register(name: String, resId: Int) {
        mappings[name] = resId
    }

    /**
     * 批量注册映射。
     *
     * @param map 逻辑名称到资源 ID 的映射
     */
    fun registerMapping(map: Map<String, Int>) {
        mappings.putAll(map)
    }

    /**
     * 解析 drawable 引用字符串，返回对应的 Android 资源 ID。
     *
     * 支持格式：
     * - `@drawable/<name>`         — 从内置/注册映射中查找
     * - `@android:drawable/<name>` — 尝试反射获取系统资源
     * - 纯数字字符串               — 直接解析为资源 ID
     *
     * @param src drawable 引用字符串
     * @return 对应的资源 ID，未找到时返回 [fallbackResId]
     */
    fun resolve(src: String): Int {
        return when {
            src.startsWith("@drawable/") -> {
                val name = src.removePrefix("@drawable/")
                mappings[name] ?: fallbackResId
            }
            src.startsWith("@android:drawable/") -> {
                val name = src.removePrefix("@android:drawable/")
                resolveAndroidDrawable(name) ?: fallbackResId
            }
            src.toIntOrNull() != null -> src.toInt()
            else -> fallbackResId
        }
    }

    private fun resolveAndroidDrawable(name: String): Int? {
        return try {
            val field = android.R.drawable::class.java.getField(name)
            field.getInt(null)
        } catch (_: Exception) {
            mappings[name]
        }
    }

    /**
     * 通过 [Context] 动态解析资源名。
     *
     * 优先级：
     * 1. 内置 mappings 映射表
     * 2. Context.resources.getIdentifier（按应用包名查找）
     * 3. 回退到 [fallbackResId]
     *
     * @param context Android Context
     * @param name    资源名（不含 @drawable/ 前缀，如 "ic_weather_sunny"）
     * @return 对应的资源 ID
     */
    fun resolveDynamic(context: android.content.Context, name: String): Int {
        // 1. 先查内置映射
        mappings[name]?.let { return it }

        // 2. 通过 Context 按包名查找
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId != 0) return resId

        // 3. fallback
        return fallbackResId
    }
}

// ---------------------------------------------------------------------------
// Theme 辅助
// ---------------------------------------------------------------------------

/**
 * 根据 [WidgetTheme] 配置解析主题颜色。
 */
private object ThemeResolver {

    fun resolvePrimaryColor(theme: WidgetTheme?): Int {
        val hex = theme?.primaryColor
        if (hex != null) {
            return try {
                android.graphics.Color.parseColor(hex)
            } catch (_: Exception) {
                0xFF1A73E8.toInt()
            }
        }
        return 0xFF1A73E8.toInt() // Material You 默认蓝
    }

    fun isDynamicColorEnabled(theme: WidgetTheme?): Boolean {
        return theme?.useDynamicColor != false
    }
}

// ---------------------------------------------------------------------------
// 主渲染入口
// ---------------------------------------------------------------------------

/**
 * 将 [WidgetBlueprint] 渲染为 Glance Composable。
 *
 * 根据 [state] 三态切换 Loading / Error / Success 视图，
 * 并在 Success 态根据 [blueprint.layout] 选择对应的布局模板。
 *
 * @param blueprint Widget DSL 描述。
 * @param state     当前 UI 状态。
 * @param modifier  Glance 修饰符。
 */
@Composable
fun BlueprintRenderer(
    blueprint: WidgetBlueprint,
    state: WidgetUiState<Map<String, String>> = WidgetUiState.Loading,
    modifier: GlanceModifier = GlanceModifier,
) {
    val useDynamicColor = ThemeResolver.isDynamicColorEnabled(blueprint.theme)
    val primaryColor = ThemeResolver.resolvePrimaryColor(blueprint.theme)
    val bgColor = if (useDynamicColor) android.graphics.Color.TRANSPARENT else (primaryColor and 0x00FFFFFF or 0x1A000000)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .background(ColorProvider(bgColor)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            is WidgetUiState.Loading -> {
                RenderLoadingState(blueprint)
            }
            is WidgetUiState.Error -> {
                RenderErrorState(blueprint, state)
            }
            is WidgetUiState.Success -> {
                RenderSuccessState(blueprint, state)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 三态渲染
// ---------------------------------------------------------------------------

@Composable
private fun RenderLoadingState(blueprint: WidgetBlueprint) {
    Text(
        text = "Loading...",
        style = TextStyle(fontWeight = FontWeight.Medium),
    )
}

@Composable
private fun RenderErrorState(
    blueprint: WidgetBlueprint,
    state: WidgetUiState.Error,
) {
    val errorConfig = blueprint.state?.onError
    Text(
        text = state.message,
        style = TextStyle(fontWeight = FontWeight.Medium),
    )
    if (state.retryable && errorConfig?.showRetry != false) {
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Tap to retry",
            modifier = GlanceModifier.clickable(
                actionSendBroadcast(
                    Intent(WidgetActions.ACTION_RETRY),
                ),
            ),
        )
    }
}

@Composable
private fun RenderSuccessState(
    blueprint: WidgetBlueprint,
    state: WidgetUiState.Success<Map<String, String>>,
) {
    val resolvedComponents = resolveComponents(blueprint, state)

    when (blueprint.layout) {
        WidgetLayout.COUNTER_2X2 -> CounterLayout(resolvedComponents, blueprint)
        WidgetLayout.SINGLE_ENTITY_2X2 -> SingleEntityLayout(resolvedComponents)
        WidgetLayout.SINGLE_ENTITY_2X1 -> SingleEntityRowLayout(resolvedComponents)
        WidgetLayout.STREAK_2X2 -> StreakLayout(resolvedComponents)
        WidgetLayout.LIST_4X2 -> ListLayout(resolvedComponents)
        WidgetLayout.CUSTOM -> DefaultLayout(resolvedComponents)
    }
}

// ---------------------------------------------------------------------------
// 布局模板
// ---------------------------------------------------------------------------

/** COUNTER_2X2：居中大号文字 + 操作按钮。 */
@Composable
private fun CounterLayout(
    components: List<WidgetComponent>,
    blueprint: WidgetBlueprint,
) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        components.forEach { component ->
            when (component.type) {
                ComponentType.TEXT -> {
                    Text(
                        text = component.props["text"] ?: component.id,
                        style = TextStyle(fontWeight = FontWeight.Bold),
                    )
                }
                ComponentType.BUTTON -> {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    RenderButton(component)
                }
                else -> RenderComponent(component, blueprint)
            }
        }
    }
}

/** SINGLE_ENTITY_2X2：垂直居中排列。 */
@Composable
private fun SingleEntityLayout(components: List<WidgetComponent>) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        components.forEachIndexed { index, component ->
            RenderComponent(component, null)
            if (index < components.size - 1) {
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
        }
    }
}

/** SINGLE_ENTITY_2X1：水平排列。 */
@Composable
private fun SingleEntityRowLayout(components: List<WidgetComponent>) {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        components.forEachIndexed { index, component ->
            RenderComponent(component, null)
            if (index < components.size - 1) {
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
        }
    }
}

/** STREAK_2X2：垂直排列 + 连续天数风格。 */
@Composable
private fun StreakLayout(components: List<WidgetComponent>) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 火焰图标
        Image(
            provider = ImageProvider(android.R.drawable.star_on),
            contentDescription = "streak",
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        components.forEachIndexed { index, component ->
            RenderComponent(component, null)
            if (index < components.size - 1) {
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }
}

/** LIST_4X2：列表布局，每项之间有间距。 */
@Composable
private fun ListLayout(components: List<WidgetComponent>) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        components.forEachIndexed { index, component ->
            RenderComponent(component, null)
            if (index < components.size - 1) {
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
        }
    }
}

/** CUSTOM / 默认：简单 Column 布局。 */
@Composable
private fun DefaultLayout(components: List<WidgetComponent>) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        components.forEach { component ->
            RenderComponent(component, null)
        }
    }
}

// ---------------------------------------------------------------------------
// 组件渲染
// ---------------------------------------------------------------------------

/**
 * 按 [ComponentType] 分发渲染单个组件。
 *
 * @param component 待渲染的组件定义。
 * @param blueprint 可选，用于读取 animations 配置。
 */
@Composable
private fun RenderComponent(
    component: WidgetComponent,
    blueprint: WidgetBlueprint?,
) {
    when (component.type) {
        ComponentType.TEXT -> RenderText(component)
        ComponentType.IMAGE -> RenderImage(component)
        ComponentType.BUTTON -> RenderButton(component)
        ComponentType.PROGRESS -> RenderProgress(component)
        ComponentType.LIST -> RenderList(component)
        ComponentType.CHART -> RenderChart(component)
    }
}

// ---- TEXT ----

@Composable
private fun RenderText(component: WidgetComponent) {
    val text = component.props["text"] ?: component.id
    val style = when (component.props["style"]) {
        "headline" -> TextStyle(fontWeight = FontWeight.Bold)
        "caption" -> TextStyle(fontWeight = FontWeight.Normal)
        else -> TextStyle(fontWeight = FontWeight.Medium)
    }
    Text(text = text, style = style)
}

// ---- IMAGE ----

@Composable
private fun RenderImage(component: WidgetComponent) {
    val context = LocalContext.current
    val resName = component.props["resName"]
    val src = component.props["src"] ?: "@android:drawable/ic_menu_gallery"

    val resId = if (!resName.isNullOrBlank()) {
        DrawableResolver.resolveDynamic(context, resName)
    } else {
        DrawableResolver.resolve(src)
    }

    if (resId != DrawableResolver.fallbackResId || src == "@android:drawable/ic_menu_gallery") {
        Image(
            provider = ImageProvider(resId),
            contentDescription = component.id,
        )
    } else {
        Text(text = "[image:${component.id}]")
    }
}

// ---- BUTTON ----

@Composable
private fun RenderButton(component: WidgetComponent) {
    val text = component.props["text"] ?: component.id
    val action = component.props["action"] ?: WidgetActions.ACTION_RETRY
    Text(
        text = text,
        style = TextStyle(fontWeight = FontWeight.Bold),
        modifier = GlanceModifier
            .clickable(
                actionSendBroadcast(Intent(action)),
            ),
    )
}

// ---- PROGRESS ----

@Composable
private fun RenderProgress(component: WidgetComponent) {
    val value = component.props["value"]?.toIntOrNull() ?: 0
    val max = component.props["max"]?.toIntOrNull() ?: 100
    val ratio = (value.toFloat() / max).coerceIn(0f, 1f)
    val text = component.props["text"]
    val label = text ?: "$value%"

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 进度条轨道
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(8.dp)
                .background(ColorProvider(0xFFDDDDDD.toInt())),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ratio > 0f) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(ColorProvider(0xFF1A73E8.toInt()))
                        .width((ratio * 100).dp.coerceAtLeast(1.dp)),
                ) {
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(text = label, style = TextStyle(fontWeight = FontWeight.Normal))
    }
}

// ---- LIST ----

@Composable
private fun RenderList(component: WidgetComponent) {
    val items = component.props["items"]
    if (items.isNullOrBlank()) {
        Text(text = component.props["text"] ?: "Empty list")
        return
    }
    val itemList = items.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
    ) {
        itemList.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "• ", style = TextStyle(fontWeight = FontWeight.Bold))
                Text(text = item)
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
        }
    }
}

// ---- CHART ----

@Composable
private fun RenderChart(component: WidgetComponent) {
    val valuesStr = component.props["values"]
    if (valuesStr.isNullOrBlank()) {
        Text(text = component.props["text"] ?: "No data")
        return
    }
    val values = valuesStr.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
    if (values.isEmpty()) {
        Text(text = "No data")
        return
    }

    val maxValue = values.max().coerceAtLeast(1)
    val barColors = listOf(
        0xFF1A73E8.toInt(),
        0xFF34A853.toInt(),
        0xFFFBBC04.toInt(),
        0xFFEA4335.toInt(),
        0xFF8E24AA.toInt(),
        0xFF00ACC1.toInt(),
    )

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
    ) {
        values.forEachIndexed { index, value ->
            val ratio = (value.toFloat() / maxValue)
            val color = barColors[index % barColors.size]
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$value",
                    style = TextStyle(fontWeight = FontWeight.Normal),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(ColorProvider(color))
                        .width((ratio * 100).dp.coerceAtLeast(2.dp)),
                ) {
                }
            }
            if (index < values.size - 1) {
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 数据映射
// ---------------------------------------------------------------------------

/**
 * 将 Success 状态中的数据映射覆盖到组件 props。
 *
 * - TEXT 组件：覆盖 `text` prop
 * - IMAGE 组件：覆盖 `resName` prop（不含 @drawable/ 前缀，由 RenderImage 动态解析）
 */
private fun resolveComponents(
    blueprint: WidgetBlueprint,
    state: WidgetUiState.Success<Map<String, String>>,
): List<WidgetComponent> {
    return blueprint.components.map { component ->
        val overrideValue = state.data[component.id]
        when {
            overrideValue != null && component.type == ComponentType.TEXT -> {
                component.copy(props = component.props + ("text" to overrideValue))
            }
            overrideValue != null && component.type == ComponentType.IMAGE -> {
                component.copy(props = component.props + ("resName" to overrideValue))
            }
            else -> component
        }
    }
}
