package net.morainet.widget.state

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Glance Preferences 状态定义，存储 [WidgetUiStateSnapshot] JSON。
 */
object WidgetUiStateDefinition : GlanceStateDefinition<Preferences> by PreferencesGlanceStateDefinition

private val KeyUiStateJson = stringPreferencesKey("widget_ui_state_json")

private val json = Json { ignoreUnknownKeys = true }

/**
 * Widget 状态读写助手，集成 GlanceStateDefinition。
 */
object WidgetStateStore {

    suspend fun <T> save(
        context: Context,
        glanceId: GlanceId,
        state: WidgetUiState<T>,
        encode: (T) -> String,
    ) {
        val snapshot = WidgetUiStateCodec.toSnapshot(state, encode)
        updateAppWidgetState(context, WidgetUiStateDefinition, glanceId) { prefs ->
            prefs[KeyUiStateJson] = json.encodeToString(snapshot)
        }
    }

    suspend fun <T> load(
        context: Context,
        glanceId: GlanceId,
        decode: (String) -> T,
        default: WidgetUiState<T> = WidgetUiState.Loading,
    ): WidgetUiState<T> {
        val prefs = getAppWidgetState(context, WidgetUiStateDefinition, glanceId)
        val raw = prefs[KeyUiStateJson] ?: return default
        return runCatching {
            val snapshot = json.decodeFromString<WidgetUiStateSnapshot>(raw)
            WidgetUiStateCodec.fromSnapshot(snapshot, decode)
        }.getOrDefault(default)
    }
}

/**
 * Error 态 Retry 按钮的 Preferences 标记（是否显示 Retry）。
 */
object WidgetRetryPrefs {
    val showRetry = booleanPreferencesKey("widget_show_retry")
}
