package com.morainet.widget.core

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * 统一 Widget 更新入口，封装批量更新与去重逻辑。
 */
object WidgetManager {

    private const val DEBOUNCE_MS = 500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val pendingUpdates = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /**
     * 更新指定 [GlanceAppWidget] 的所有实例。
     * 500ms 防抖窗口内多次调用会合并为一次实际更新。
     */
    suspend fun updateAll(widget: GlanceAppWidget, context: Context) {
        scheduleUpdate(widget, context) { widget.updateAll(context) }
    }

    /**
     * 更新指定 widget 实例 ID 列表。
     */
    suspend fun update(
        widget: GlanceAppWidget,
        context: Context,
        glanceIds: List<GlanceId>,
    ) {
        if (glanceIds.isEmpty()) return
        scheduleUpdate(widget, context) {
            glanceIds.forEach { id -> widget.update(context, id) }
        }
    }

    /**
     * 获取当前已安装的 widget 实例 [GlanceId]。
     */
    suspend fun getInstalledIds(
        context: Context,
        widgetClass: Class<out GlanceAppWidget>,
    ): List<GlanceId> {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(widgetClass)
    }

    private suspend fun scheduleUpdate(
        widget: GlanceAppWidget,
        context: Context,
        block: suspend () -> Unit,
    ) {
        val key = widget::class.java.name
        val deferred = CompletableDeferred<Unit>()

        mutex.withLock {
            pendingUpdates[key]?.cancel()
            pendingUpdates[key] = deferred
        }

        scope.launch {
            delay(DEBOUNCE_MS)
            try {
                block()
                deferred.complete(Unit)
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            } finally {
                mutex.withLock {
                    if (pendingUpdates[key] === deferred) {
                        pendingUpdates.remove(key)
                    }
                }
            }
        }

        deferred.await()
    }
}
