package com.morainet.widget.ai

/**
 * AppFunctions Scaffold — AI-Ready 桥接层。
 *
 * 为 AI 生成的 WidgetBlueprint 提供可绑定的应用功能定义。
 * AI 生成器可以在 Blueprint 的 BUTTON 组件 `props` 中引用这些函数名，
 * 宿主 App 实现对应接口即可完成功能绑定。
 *
 * ## 用法
 *
 * ### 1. 定义 App 功能
 * ```kotlin
 * class MyAppFunctions : AppFunctions {
 *     override val bindings = mapOf(
 *         "open_app" to { context -> context.startActivity(Intent(context, MainActivity::class.java)) },
 *         "refresh_data" to { context -> /* trigger data refresh */ },
 *     )
 * }
 * ```
 *
 * ### 2. 注册到全局
 * ```kotlin
 * AppFunctionRegistry.register(MyAppFunctions())
 * ```
 *
 * ### 3. AI 生成的 Blueprint 自动绑定
 * ```json
 * {
 *   "type": "BUTTON",
 *   "id": "refresh_btn",
 *   "props": { "action": "refresh_data", "label": "Refresh" }
 * }
 * ```
 * BlueprintRenderer 会自动查找 `AppFunctionRegistry` 并绑定 `action` prop。
 */
interface AppFunctions {

    /**
     * 功能绑定映射表。
     *
     * Key：功能名（对应 BUTTON 组件的 `action` prop）
     * Value：执行函数（接收 Android Context，无返回值）
     */
    val bindings: Map<String, (android.content.Context) -> Unit>

    companion object {
        /** 空实现，用于默认值 */
        val EMPTY = object : AppFunctions {
            override val bindings: Map<String, (android.content.Context) -> Unit> = emptyMap()
        }
    }
}

/**
 * 全局 AppFunctions 注册表。
 *
 * 宿主 App 在 Application.onCreate() 中注册自己的功能实现，
 * BlueprintRenderer 在渲染 BUTTON 组件时从此注册表查找绑定。
 */
object AppFunctionRegistry {

    private var instance: AppFunctions = AppFunctions.EMPTY

    /**
     * 注册全局 AppFunctions 实现。
     *
     * 应在 Application.onCreate() 中调用。
     *
     * @param functions 应用功能实现
     */
    fun register(functions: AppFunctions) {
        instance = functions
    }

    /**
     * 获取当前注册的 AppFunctions。
     */
    fun get(): AppFunctions = instance

    /**
     * 检查指定 action 是否已注册。
     */
    fun hasAction(action: String): Boolean = action in instance.bindings

    /**
     * 获取所有已注册的 action 名称列表。
     *
     * 用于 PromptTemplateEngine 在生成 Prompt 时注入可用功能列表，
     * 让 AI 生成器知道可以引用哪些 action。
     */
    fun availableActions(): List<String> = instance.bindings.keys.toList()

    /**
     * 执行指定的 action。
     *
     * @param action action 名称
     * @param context Android Context
     * @return true 如果 action 存在并已执行
     */
    fun execute(action: String, context: android.content.Context): Boolean {
        val handler = instance.bindings[action] ?: return false
        handler(context)
        return true
    }

    /**
     * 生成可用功能列表的文档字符串（用于 Prompt 注入）。
     */
    fun toPromptDocs(): String {
        val actions = availableActions()
        if (actions.isEmpty()) return "No app functions registered."

        return buildString {
            appendLine("## Available App Functions (for BUTTON action props)")
            appendLine("When creating BUTTON components, you may use these action names:")
            actions.forEach { action ->
                appendLine("- `$action`")
            }
        }
    }
}

/**
 * 预定义的常用 AppFunction 名称常量。
 *
 * 推荐 AI 生成器优先使用这些标准名称，以提高跨 App 兼容性。
 */
object StandardActions {
    const val OPEN_APP = "open_app"
    const val REFRESH_DATA = "refresh_data"
    const val OPEN_SETTINGS = "open_settings"
    const val SHARE_WIDGET = "share_widget"
    const val ADD_ITEM = "add_item"
    const val REMOVE_ITEM = "remove_item"
    const val TOGGLE_FAVORITE = "toggle_favorite"
    const val VIEW_DETAILS = "view_details"
    const val PLAY_MEDIA = "play_media"
    const val PAUSE_MEDIA = "pause_media"
    const val NEXT_ITEM = "next_item"
    const val PREV_ITEM = "prev_item"
    const val RESET_COUNTER = "reset_counter"
    const val INCREMENT_COUNTER = "increment_counter"
    const val CHECK_IN = "check_in"
}
