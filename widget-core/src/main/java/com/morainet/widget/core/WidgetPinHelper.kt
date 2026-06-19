package com.morainet.widget.core

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Pin Widget 辅助 API（API 26+）。
 */
object WidgetPinHelper {

    fun isPinSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    }

    fun requestPin(
        context: Context,
        receiver: Class<out GlanceAppWidgetReceiver>,
        successCallback: android.app.PendingIntent? = null,
    ): Boolean {
        if (!isPinSupported(context)) return false
        val manager = AppWidgetManager.getInstance(context)
        return manager.requestPinAppWidget(
            ComponentName(context, receiver),
            null,
            successCallback,
        )
    }

    fun requestPin(
        context: Context,
        widget: GlanceAppWidget,
        receiver: Class<out GlanceAppWidgetReceiver>,
        successCallback: android.app.PendingIntent? = null,
    ): Boolean = requestPin(context, receiver, successCallback)
}
