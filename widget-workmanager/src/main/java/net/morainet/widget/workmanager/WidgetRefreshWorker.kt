package net.morainet.widget.workmanager

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.morainet.widget.core.WidgetManager
import java.util.concurrent.TimeUnit

/**
 * 抽象 Worker：子类实现 [fetchData]，基类负责触发 Glance 更新。
 */
abstract class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    abstract val widget: GlanceAppWidget

    override suspend fun doWork(): Result {
        return try {
            WidgetRefreshLogger.logStart(workerClassName, "periodic")
            fetchData()
            WidgetManager.updateAll(widget, applicationContext)
            WidgetRefreshLogger.logSuccess(workerClassName)
            Result.success()
        } catch (e: Exception) {
            WidgetRefreshLogger.logFailure(workerClassName, e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    abstract suspend fun fetchData()

    private val workerClassName: String
        get() = this::class.java.simpleName
}

/**
 * 网络恢复时触发的一次性刷新 Worker。
 */
abstract class WidgetNetworkRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : WidgetRefreshWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetRefreshLogger.logStart(workerClassName, "network")
        return super.doWork()
    }

    private val workerClassName: String
        get() = this::class.java.simpleName
}

/**
 * 登录态变化时触发的一次性刷新 Worker。
 */
abstract class WidgetLoginRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : WidgetRefreshWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetRefreshLogger.logStart(workerClassName, "login")
        return super.doWork()
    }

    private val workerClassName: String
        get() = this::class.java.simpleName
}

/**
 * Widget 刷新调度器，统一周期/事件更新策略。
 */
object WidgetScheduler {

    inline fun <reified W : WidgetRefreshWorker> schedulePeriodic(
        context: Context,
        uniqueWorkName: String,
        intervalMinutes: Long = 15,
    ) {
        val request = PeriodicWorkRequestBuilder<W>(
            intervalMinutes,
            TimeUnit.MINUTES,
        )
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        WidgetRefreshLogger.logScheduled(uniqueWorkName, "periodic", intervalMinutes)
    }

    inline fun <reified W : WidgetNetworkRefreshWorker> scheduleOnNetworkAvailable(
        context: Context,
        uniqueWorkName: String,
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<W>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        WidgetRefreshLogger.logScheduled(uniqueWorkName, "network", null)
    }

    inline fun <reified W : WidgetLoginRefreshWorker> scheduleOnLogin(
        context: Context,
        uniqueWorkName: String,
    ) {
        val request = OneTimeWorkRequestBuilder<W>().build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        WidgetRefreshLogger.logScheduled(uniqueWorkName, "login", null)
    }

    inline fun <reified W : WidgetRefreshWorker> scheduleOneTime(
        context: Context,
        uniqueWorkName: String,
    ) {
        val request = OneTimeWorkRequestBuilder<W>().build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        WidgetRefreshLogger.logScheduled(uniqueWorkName, "one_time", null)
    }

    fun cancel(context: Context, uniqueWorkName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
        WidgetRefreshLogger.logCancelled(uniqueWorkName)
    }
}

/**
 * 刷新频率与结果日志，便于调试更新风暴。
 */
object WidgetRefreshLogger {

    private val history = mutableListOf<RefreshLogEntry>()
    private const val MAX_HISTORY = 100

    fun logScheduled(workName: String, trigger: String, intervalMinutes: Long?) {
        append(RefreshLogEntry(workName, trigger, "scheduled", intervalMinutes = intervalMinutes))
    }

    fun logStart(workerName: String, trigger: String) {
        append(RefreshLogEntry(workerName, trigger, "started"))
    }

    fun logSuccess(workerName: String) {
        append(RefreshLogEntry(workerName, "", "success"))
    }

    fun logFailure(workerName: String, error: Throwable) {
        append(RefreshLogEntry(workerName, "", "failure", error.message))
    }

    fun logCancelled(workName: String) {
        append(RefreshLogEntry(workName, "", "cancelled"))
    }

    fun getHistory(): List<RefreshLogEntry> = history.toList()

    fun clear() {
        history.clear()
    }

    private fun append(entry: RefreshLogEntry) {
        synchronized(history) {
            history.add(entry)
            if (history.size > MAX_HISTORY) {
                history.removeAt(0)
            }
        }
    }
}

data class RefreshLogEntry(
    val workName: String,
    val trigger: String,
    val status: String,
    val message: String? = null,
    val intervalMinutes: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
