package net.morainet.widget.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetUiStateTest {

    @Test
    fun widgetStateBuilder_success() {
        val state = widgetState<String> {
            loading()
            success("ok")
        }
        assertTrue(state is WidgetUiState.Success)
        assertEquals("ok", (state as WidgetUiState.Success).data)
    }

    @Test
    fun codec_roundTrip() {
        val original = WidgetUiState.Success(42)
        val snapshot = WidgetUiStateCodec.toSnapshot(original) { it.toString() }
        val restored = WidgetUiStateCodec.fromSnapshot(snapshot) { it.toInt() }

        assertEquals(original, restored)
    }

    @Test
    fun codec_errorState() {
        val original = WidgetUiState.Error("Network error", retryable = false)
        val snapshot = WidgetUiStateCodec.toSnapshot(original) { it.toString() }
        val restored = WidgetUiStateCodec.fromSnapshot(snapshot) { it }

        assertEquals(original, restored)
    }
}
