package com.morainet.widget.preview

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 标注 Widget 预览函数，类似 Compose [@Preview]。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class WidgetPreview(
    val name: String = "",
    val widthDp: Int = 180,
    val heightDp: Int = 110,
    val locale: String = "",
)

/**
 * 常用 Widget 尺寸预设（dp）。
 */
object WidgetPreviewSizes {
    val Small_2x1 = DpSize(180.dp, 55.dp)
    val Medium_2x2 = DpSize(180.dp, 110.dp)
    val Large_4x2 = DpSize(360.dp, 110.dp)
}

/**
 * RemoteViews 真机宿主控制器，基于 [AppWidgetHost]。
 */
class WidgetRemoteViewsController(context: Context) {

    private val appContext = context.applicationContext
    private val host = AppWidgetHost(appContext, HOST_ID)
    private var allocatedId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var hostView: AppWidgetHostView? = null

    init {
        host.startListening()
    }

    fun createHostView(): AppWidgetHostView {
        ensureWidgetId()
        val info = createFakeProviderInfo()
        val view = host.createView(appContext, allocatedId, info)
        hostView = view
        return view
    }

    fun update(remoteViews: RemoteViews) {
        hostView?.updateAppWidget(remoteViews)
    }

    fun release() {
        if (allocatedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            host.deleteAppWidgetId(allocatedId)
            allocatedId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
        host.stopListening()
    }

    private fun ensureWidgetId() {
        if (allocatedId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            allocatedId = host.allocateAppWidgetId()
        }
    }

    private fun createFakeProviderInfo(): AppWidgetProviderInfo {
        return AppWidgetProviderInfo().apply {
            provider = appContext.packageName.let { pkg ->
                android.content.ComponentName(
                    pkg,
                    "${pkg}.WidgetProvider"
                )
            }
            minWidth = 180
            minHeight = 110
            minResizeWidth = 40
            minResizeHeight = 40
            updatePeriodMillis = 0
            initialLayout = R.layout.widget_preview_host
            autoAdvanceViewId = -1
            previewLayout = 0
            resizeMode = AppWidgetProviderInfo.RESIZE_HORIZONTAL or AppWidgetProviderInfo.RESIZE_VERTICAL
            widgetCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
        }
    }

    companion object {
        const val HOST_ID = 0x4D4B0001
    }
}

/**
 * 在 App 内嵌入 Widget 预览容器（Compose 内容或 RemoteViews）。
 *
 * [content] 应为标准 Compose Composable（非 Glance 组件）。
 * Glance 组件（如 Glance Column/Text/GlanceModifier）需要 Glance Composition 环境，
 * 无法直接在 Compose 中渲染，应通过 [remoteViews] 参数传入已生成的 RemoteViews。
 */
@Composable
fun WidgetPreviewHost(
    displaySize: DpSize,
    modifier: Modifier = Modifier,
    remoteViews: RemoteViews? = null,
    content: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(displaySize)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Color(0x33000000), shape),
    ) {
        if (remoteViews != null) {
            WidgetRemoteViewsHost(
                remoteViews = remoteViews,
                modifier = Modifier.size(displaySize),
            )
        } else {
            content()
        }
    }
}

/**
 * RemoteViews 真机宿主 Composable 封装。
 */
@Composable
fun WidgetRemoteViewsHost(
    remoteViews: RemoteViews,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = remember { WidgetRemoteViewsController(context) }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { controller.createHostView() },
        update = { view ->
            view.updateAppWidget(remoteViews)
        },
    )
}
