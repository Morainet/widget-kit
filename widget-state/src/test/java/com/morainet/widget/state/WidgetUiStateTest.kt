package com.morainet.widget.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetUiStateTest {

    // ---- Builder ----

    @Test
    fun widgetStateBuilder_loading() {
        val state = widgetState<String> {
            loading()
        }
        assertTrue(state is WidgetUiState.Loading)
    }

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
    fun widgetStateBuilder_error() {
        val state = widgetState<String> {
            error("Network unavailable")
        }
        assertTrue(state is WidgetUiState.Error)
        val error = state as WidgetUiState.Error
        assertEquals("Network unavailable", error.message)
        assertTrue(error.retryable)
    }

    @Test
    fun widgetStateBuilder_errorNonRetryable() {
        val state = widgetState<String> {
            error("Fatal error", retryable = false)
        }
        assertTrue(state is WidgetUiState.Error)
        assertFalse((state as WidgetUiState.Error).retryable)
    }

    // ---- Codec: round-trip ----

    @Test
    fun codec_roundTrip_success() {
        val original = WidgetUiState.Success(42)
        val snapshot = WidgetUiStateCodec.toSnapshot(original) { it.toString() }
        val restored = WidgetUiStateCodec.fromSnapshot(snapshot) { it.toInt() }

        assertEquals(original, restored)
    }

    @Test
    fun codec_roundTrip_error() {
        val original = WidgetUiState.Error("Network error", retryable = false)
        val snapshot = WidgetUiStateCodec.toSnapshot(original) { it.toString() }
        val restored = WidgetUiStateCodec.fromSnapshot(snapshot) { it }

        assertEquals(original, restored)
    }

    @Test
    fun codec_roundTrip_loading() {
        val original = WidgetUiState.Loading
        val snapshot = WidgetUiStateCodec.toSnapshot(original) { it.toString() }
        val restored = WidgetUiStateCodec.fromSnapshot(snapshot) { it.toInt() }

        assertEquals(original, restored)
    }

    // ---- Codec: phases ----

    @Test
    fun codec_phaseConstants() {
        assertEquals("loading", WidgetUiStateCodec.PHASE_LOADING)
        assertEquals("success", WidgetUiStateCodec.PHASE_SUCCESS)
        assertEquals("error", WidgetUiStateCodec.PHASE_ERROR)
    }

    // ---- Codec: edge cases ----

    @Test
    fun codec_success_withNullPayload() {
        val snapshot = WidgetUiStateCodec.toSnapshot(WidgetUiState.Success("data")) { it }
        // Simulate a snapshot with null payload (corrupted data)
        val corrupt = snapshot.copy(payload = null)
        val restored = WidgetUiStateCodec.fromSnapshot(corrupt) { it }
        assertTrue(restored is WidgetUiState.Error)
    }

    @Test
    fun codec_error_withNullMessage() {
        val snapshot = WidgetUiStateCodec.toSnapshot(
            WidgetUiState.Error("msg"),
        ) { it.toString() }
        val corrupt = snapshot.copy(message = null)
        val restored = WidgetUiStateCodec.fromSnapshot(corrupt) { it.toInt() }
        assertTrue(restored is WidgetUiState.Error)
        assertEquals("Unknown error", (restored as WidgetUiState.Error).message)
    }

    // ---- Snapshot ----

    @Test
    fun snapshot_loadingPhase() {
        val snapshot = WidgetUiStateCodec.toSnapshot(WidgetUiState.Loading) { it.toString() }
        assertEquals("loading", snapshot.phase)
        assertEquals(null, snapshot.payload)
    }

    @Test
    fun snapshot_successPhase() {
        val snapshot = WidgetUiStateCodec.toSnapshot(WidgetUiState.Success("hello")) { it }
        assertEquals("success", snapshot.phase)
        assertEquals("hello", snapshot.payload)
    }
}
