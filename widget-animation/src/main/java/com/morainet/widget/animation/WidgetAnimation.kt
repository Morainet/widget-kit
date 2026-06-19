package com.morainet.widget.animation

import android.content.Context
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation

/**
 * Widget 动画引擎类型。Android 16+ 将走 Remote Compose，低版本走 Legacy fallback。
 */
enum class AnimationEngine {
    REMOTE_COMPOSE,
    LEGACY_LAYOUT_ANIMATION,
    NONE,
}

/**
 * 动画预设标识，供 DSL 与运行时共用。
 */
enum class AnimationPreset {
    FADE,
    PULSE,
    COUNTER_TICK,
    SNAP_SCROLL,
}

/**
 * 根据系统版本选择动画引擎。
 */
object AnimationEngineSelector {

    fun select(sdkInt: Int): AnimationEngine {
        return when {
            sdkInt >= 36 -> AnimationEngine.REMOTE_COMPOSE
            sdkInt >= 26 -> AnimationEngine.LEGACY_LAYOUT_ANIMATION
            else -> AnimationEngine.NONE
        }
    }
}

/**
 * 动画配置，供 DSL 与运行时共用。
 */
data class WidgetAnimationSpec(
    val preset: AnimationPreset,
    val durationMs: Long = 300,
    val targetViewId: Int? = null,
)

/**
 * 动画应用结果。
 */
sealed class AnimationResult {
    data class Legacy(val animation: Animation) : AnimationResult()
    data class RemoteCompose(val expression: String) : AnimationResult()
    data object None : AnimationResult()
}

/**
 * 将 [WidgetAnimationSpec] 绑定到具体动画引擎。
 */
object WidgetAnimationApplier {

    fun apply(spec: WidgetAnimationSpec, sdkInt: Int = android.os.Build.VERSION.SDK_INT): AnimationResult {
        return when (AnimationEngineSelector.select(sdkInt)) {
            AnimationEngine.REMOTE_COMPOSE -> applyRemoteCompose(spec)
            AnimationEngine.LEGACY_LAYOUT_ANIMATION -> applyLegacy(spec)
            AnimationEngine.NONE -> AnimationResult.None
        }
    }

    private fun applyRemoteCompose(spec: WidgetAnimationSpec): AnimationResult {
        val expression = when (spec.preset) {
            AnimationPreset.FADE -> "fade(duration=${spec.durationMs})"
            AnimationPreset.PULSE -> "pulse(duration=${spec.durationMs})"
            AnimationPreset.COUNTER_TICK -> "counterTick(duration=${spec.durationMs})"
            AnimationPreset.SNAP_SCROLL -> "snapScroll(duration=${spec.durationMs})"
        }
        return AnimationResult.RemoteCompose(expression)
    }

    private fun applyLegacy(spec: WidgetAnimationSpec): AnimationResult {
        val animation = when (spec.preset) {
            AnimationPreset.FADE -> FadeTransition.create(spec.durationMs)
            AnimationPreset.PULSE -> PulseTransition.create(spec.durationMs)
            AnimationPreset.COUNTER_TICK -> CounterTickTransition.create(spec.durationMs)
            AnimationPreset.SNAP_SCROLL -> FadeTransition.create(spec.durationMs)
        }
        return AnimationResult.Legacy(animation)
    }
}

/** 淡入淡出预设。 */
object FadeTransition {

    fun create(durationMs: Long): Animation {
        return AlphaAnimation(0.3f, 1.0f).apply {
            duration = durationMs
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
    }
}

/** 脉冲缩放预设。 */
object PulseTransition {

    fun create(durationMs: Long): Animation {
        val scale = ScaleAnimation(
            1.0f, 1.08f, 1.0f, 1.08f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
        )
        scale.duration = durationMs
        scale.repeatMode = Animation.REVERSE
        scale.repeatCount = Animation.INFINITE
        return scale
    }
}

/** 计数器跳动预设。 */
object CounterTickTransition {

    fun create(durationMs: Long): Animation {
        val scale = ScaleAnimation(
            1.0f, 1.15f, 1.0f, 1.15f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
        )
        val fade = AlphaAnimation(1.0f, 0.7f)
        return AnimationSet(true).apply {
            addAnimation(scale)
            addAnimation(fade)
            duration = durationMs
        }
    }
}

/**
 * Legacy 引擎辅助：将动画注入 RemoteViews 宿主 View。
 */
object LegacyAnimationBinder {

    fun bind(context: Context, animation: Animation): Animation = animation
}
