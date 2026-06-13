package net.morainet.widget.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetAnimationTest {

    @Test
    fun select_remoteCompose_onApi36() {
        assertEquals(AnimationEngine.REMOTE_COMPOSE, AnimationEngineSelector.select(36))
    }

    @Test
    fun select_legacy_onApi30() {
        assertEquals(AnimationEngine.LEGACY_LAYOUT_ANIMATION, AnimationEngineSelector.select(30))
    }

    @Test
    fun applier_legacyFade() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(AnimationPreset.FADE, durationMs = 200),
            sdkInt = 30,
        )
        assertTrue(result is AnimationResult.Legacy)
    }

    @Test
    fun applier_remoteComposePulse() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(AnimationPreset.PULSE, durationMs = 400),
            sdkInt = 36,
        )
        assertTrue(result is AnimationResult.RemoteCompose)
    }
}
