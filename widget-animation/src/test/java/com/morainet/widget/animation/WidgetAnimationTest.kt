package com.morainet.widget.animation

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetAnimationTest {

    // ---- Engine Selector ----

    @Test
    fun select_remoteCompose_onApi36() {
        assertEquals(AnimationEngine.REMOTE_COMPOSE, AnimationEngineSelector.select(36))
    }

    @Test
    fun select_legacy_onApi30() {
        assertEquals(AnimationEngine.LEGACY_LAYOUT_ANIMATION, AnimationEngineSelector.select(30))
    }

    @Test
    fun select_none_onApi25() {
        assertEquals(AnimationEngine.NONE, AnimationEngineSelector.select(25))
    }

    // ---- Applier ----

    @Test
    fun applier_legacyFade() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(AnimationPreset.FADE, durationMs = 200),
            sdkInt = 30,
        )
        assertTrue(result is AnimationResult.Legacy)
        val legacy = result as AnimationResult.Legacy
        assertTrue(legacy.animation is AlphaAnimation)
        assertEquals(200, legacy.animation.duration)
    }

    @Test
    fun applier_legacyPulse() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(AnimationPreset.PULSE, durationMs = 400),
            sdkInt = 30,
        )
        assertTrue(result is AnimationResult.Legacy)
        val legacy = result as AnimationResult.Legacy
        assertTrue(legacy.animation is ScaleAnimation)
    }

    @Test
    fun applier_legacyCounterTick() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(AnimationPreset.COUNTER_TICK, durationMs = 150),
            sdkInt = 30,
        )
        assertTrue(result is AnimationResult.Legacy)
        val legacy = result as AnimationResult.Legacy
        assertTrue(legacy.animation is AnimationSet)
    }

    @Test
    fun applier_legacySnapScroll() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(AnimationPreset.SNAP_SCROLL, durationMs = 300),
            sdkInt = 30,
        )
        assertTrue(result is AnimationResult.Legacy)
        val legacy = result as AnimationResult.Legacy
        assertTrue(legacy.animation is AnimationSet)
        val set = legacy.animation as AnimationSet
        assertEquals(2, set.animations.size)
        // 包含 TranslateAnimation 和 AlphaAnimation
        val types = set.animations.map { it::class.java }
        assertTrue(types.any { it == TranslateAnimation::class.java })
        assertTrue(types.any { it == AlphaAnimation::class.java })
    }

    @Test
    fun applier_remoteComposePulse() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(AnimationPreset.PULSE, durationMs = 400),
            sdkInt = 36,
        )
        assertTrue(result is AnimationResult.RemoteCompose)
        val rc = result as AnimationResult.RemoteCompose
        assertTrue(rc.expression.contains("pulse"))
    }

    @Test
    fun applier_none_onLowApi() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(AnimationPreset.FADE),
            sdkInt = 25,
        )
        assertTrue(result is AnimationResult.None)
    }

    // ---- targetViewId ----

    @Test
    fun applier_preservesTargetViewId() {
        val result = WidgetAnimationApplier.apply(
            WidgetAnimationSpec(
                preset = AnimationPreset.FADE,
                targetViewId = 0x7F010001,
            ),
            sdkInt = 30,
        )
        assertTrue(result is AnimationResult.Legacy)
        val legacy = result as AnimationResult.Legacy
        assertEquals(0x7F010001, legacy.targetViewId)
    }

    // ---- Transitions ----

    @Test
    fun fadeTransition_createsAlphaAnimation() {
        val anim = FadeTransition.create(500)
        assertTrue(anim is AlphaAnimation)
        assertEquals(500, anim.duration)
    }

    @Test
    fun pulseTransition_createsScaleAnimation() {
        val anim = PulseTransition.create(300)
        assertTrue(anim is ScaleAnimation)
    }

    @Test
    fun counterTickTransition_createsAnimationSet() {
        val anim = CounterTickTransition.create(200)
        assertTrue(anim is AnimationSet)
    }

    @Test
    fun snappingScrollTransition_createsAnimationSet() {
        val anim = SnappingScrollTransition.create(350)
        assertTrue(anim is AnimationSet)
        val set = anim as AnimationSet
        // 应包含平移和淡入两个子动画
        assertEquals(2, set.animations.size)
        assertEquals(350, anim.duration)
    }
}
