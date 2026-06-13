package net.morainet.widget.state

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionSendBroadcast
import net.morainet.widget.core.MoraineWidgetReceiver
import net.morainet.widget.core.WidgetActions

/**
 * Error 态自动绑定 [WidgetActions.ACTION_RETRY] 的 Action 工厂。
 */
object WidgetRetryAction {

    fun create(
        context: Context,
        receiver: Class<out MoraineWidgetReceiver>,
        glanceId: GlanceId? = null,
    ): Action {
        val intent = Intent(context, receiver).apply {
            action = WidgetActions.ACTION_RETRY
            component = ComponentName(context, receiver)
            glanceId?.let { putExtra(WidgetActions.EXTRA_WIDGET_ID, it.toString()) }
        }
        return actionSendBroadcast(intent)
    }
}
