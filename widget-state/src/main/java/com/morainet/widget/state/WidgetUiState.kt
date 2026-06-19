package com.morainet.widget.state

import kotlinx.serialization.Serializable

/**
 * Widget UI 状态密封类，对应 loading / success / error 三态。
 */
sealed class WidgetUiState<out T> {
    data object Loading : WidgetUiState<Nothing>()
    data class Success<T>(val data: T) : WidgetUiState<T>()
    data class Error(val message: String, val retryable: Boolean = true) : WidgetUiState<Nothing>()
}

/**
 * 可持久化的 UI 状态快照，供 Glance Preferences 存储。
 */
@Serializable
data class WidgetUiStateSnapshot(
    val phase: String,
    val payload: String? = null,
    val message: String? = null,
    val retryable: Boolean = true,
)

/**
 * [WidgetUiState] 与 [WidgetUiStateSnapshot] 互转。
 */
object WidgetUiStateCodec {

    fun <T> toSnapshot(state: WidgetUiState<T>, encode: (T) -> String): WidgetUiStateSnapshot {
        return when (state) {
            is WidgetUiState.Loading -> WidgetUiStateSnapshot(phase = PHASE_LOADING)
            is WidgetUiState.Success -> WidgetUiStateSnapshot(
                phase = PHASE_SUCCESS,
                payload = encode(state.data),
            )
            is WidgetUiState.Error -> WidgetUiStateSnapshot(
                phase = PHASE_ERROR,
                message = state.message,
                retryable = state.retryable,
            )
        }
    }

    fun <T> fromSnapshot(snapshot: WidgetUiStateSnapshot, decode: (String) -> T): WidgetUiState<T> {
        return when (snapshot.phase) {
            PHASE_SUCCESS -> {
                val payload = snapshot.payload
                if (payload != null) {
                    WidgetUiState.Success(decode(payload))
                } else {
                    WidgetUiState.Error("Missing payload", retryable = true)
                }
            }
            PHASE_ERROR -> WidgetUiState.Error(
                message = snapshot.message ?: "Unknown error",
                retryable = snapshot.retryable,
            )
            else -> WidgetUiState.Loading
        }
    }

    const val PHASE_LOADING = "loading"
    const val PHASE_SUCCESS = "success"
    const val PHASE_ERROR = "error"
}

/**
 * DSL 风格的状态构建辅助。
 */
class WidgetStateBuilder<T> {
    private var state: WidgetUiState<T> = WidgetUiState.Loading

    fun loading() {
        state = WidgetUiState.Loading
    }

    fun success(data: T) {
        state = WidgetUiState.Success(data)
    }

    fun error(message: String, retryable: Boolean = true) {
        state = WidgetUiState.Error(message, retryable)
    }

    fun build(): WidgetUiState<T> = state
}

fun <T> widgetState(block: WidgetStateBuilder<T>.() -> Unit): WidgetUiState<T> {
    return WidgetStateBuilder<T>().apply(block).build()
}
