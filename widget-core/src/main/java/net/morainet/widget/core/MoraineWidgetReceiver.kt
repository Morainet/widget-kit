package net.morainet.widget.core

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 带协程作用域的 [GlanceAppWidgetReceiver] 基类，统一 Action 路由。
 */
abstract class MoraineWidgetReceiver : GlanceAppWidgetReceiver() {

    protected val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        if (routeCustomAction(context, intent)) return
        super.onReceive(context, intent)
    }

    /**
     * 处理自定义 Action。返回 true 表示已消费，不再走默认 Glance 流程。
     */
    protected open fun routeCustomAction(context: Context, intent: Intent): Boolean {
        return when (intent.action) {
            WidgetActions.ACTION_REFRESH -> {
                launchUpdate(context, glanceAppWidget)
                true
            }
            WidgetActions.ACTION_RETRY -> {
                launchUpdate(context, glanceAppWidget) { onRetry(context, intent) }
                true
            }
            else -> false
        }
    }

    /**
     * 用户点击 Error 态 Retry 按钮时回调，子类可重新拉取数据。
     */
    protected open suspend fun onRetry(context: Context, intent: Intent) {}

    /**
     * 在协程中安全更新 widget。
     */
    protected fun launchUpdate(
        context: Context,
        widget: GlanceAppWidget,
        block: suspend () -> Unit = {},
    ) {
        receiverScope.launch {
            block()
            WidgetManager.updateAll(widget, context)
        }
    }
}
