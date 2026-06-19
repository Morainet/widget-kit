package com.morainet.widget.animation

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation

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
    /**
     * Legacy 动画结果，携带 [animation] 实例及可选的 [targetViewId]，
     * 调用方可根据 [targetViewId] 将动画应用到指定 View。
     */
    data class Legacy(
        val animation: Animation,
        val targetViewId: Int? = null,
    ) : AnimationResult()

    /**
     * RemoteCompose 动画结果，携带表达式字符串。
     * API 尚未稳定，当前为占位实现。
     */
    data class RemoteCompose(val expression: String) : AnimationResult()

    /** 无动画。 */
    data object None : AnimationResult()
}

/**
 * 将 [WidgetAnimationSpec] 绑定到具体动画引擎。
 */
object WidgetAnimationApplier {

    /**
     * 根据 [spec] 与 [sdkInt] 创建对应的 [AnimationResult]。
     *
     * @param spec   动画规格配置。
     * @param sdkInt 目标设备 SDK 版本，默认取当前设备值。
     */
    fun apply(
        spec: WidgetAnimationSpec,
        sdkInt: Int = android.os.Build.VERSION.SDK_INT,
    ): AnimationResult {
        return when (AnimationEngineSelector.select(sdkInt)) {
            AnimationEngine.REMOTE_COMPOSE -> applyRemoteCompose(spec)
            AnimationEngine.LEGACY_LAYOUT_ANIMATION -> applyLegacy(spec)
            AnimationEngine.NONE -> AnimationResult.None
        }
    }

    // ---- RemoteCompose 路径（占位） ----

    private fun applyRemoteCompose(spec: WidgetAnimationSpec): AnimationResult {
        val expression = when (spec.preset) {
            AnimationPreset.FADE -> "fade(duration=${spec.durationMs})"
            AnimationPreset.PULSE -> "pulse(duration=${spec.durationMs})"
            AnimationPreset.COUNTER_TICK -> "counterTick(duration=${spec.durationMs})"
            AnimationPreset.SNAP_SCROLL -> "snapScroll(duration=${spec.durationMs})"
        }
        return AnimationResult.RemoteCompose(expression)
    }

    // ---- Legacy 路径 ----

    private fun applyLegacy(spec: WidgetAnimationSpec): AnimationResult {
        val animation = createLegacyAnimation(spec.preset, spec.durationMs)
        return AnimationResult.Legacy(
            animation = animation,
            targetViewId = spec.targetViewId,
        )
    }

    private fun createLegacyAnimation(
        preset: AnimationPreset,
        durationMs: Long,
    ): Animation {
        return when (preset) {
            AnimationPreset.FADE -> FadeTransition.create(durationMs)
            AnimationPreset.PULSE -> PulseTransition.create(durationMs)
            AnimationPreset.COUNTER_TICK -> CounterTickTransition.create(durationMs)
            AnimationPreset.SNAP_SCROLL -> SnappingScrollTransition.create(durationMs)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 动画预设
// ═══════════════════════════════════════════════════════════════════════════

/** 淡入淡出预设。 */
object FadeTransition {

    /** 创建 AlphaAnimation：0.3 → 1.0 循环反向播放。 */
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

    /** 创建 ScaleAnimation：以自身中心缩放 1.0 ↔ 1.08，循环反向播放。 */
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

    /** 创建 ScaleAnimation + AlphaAnimation 组合，模拟计数器跳动效果。 */
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
 * Snap 滑动预设：View 从屏幕右侧滑入并伴随淡入。
 *
 * 动画组合：
 * - 水平平移动画：100%（屏幕右侧之外）→ 0%（目标位置）
 * - Alpha 淡入：0.0（全透明）→ 1.0（不透明）
 * - 动画集合同步播放（shareInterpolator = true）
 */
object SnappingScrollTransition {

    /**
     * 创建水平滑入 + 淡入的组合动画。
     *
     * @param durationMs 动画时长，单位毫秒。
     * @return 包含 TranslateAnimation 与 AlphaAnimation 的 [AnimationSet]。
     */
    fun create(durationMs: Long): Animation {
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 1.0f,  // fromXDelta: 父容器宽度的 100%（右侧之外）
            Animation.RELATIVE_TO_PARENT, 0.0f,  // toXDelta:   父容器宽度的 0%（原位）
            Animation.RELATIVE_TO_SELF, 0.0f,     // fromYDelta: 自身 0%
            Animation.RELATIVE_TO_SELF, 0.0f,     // toYDelta:   自身 0%
        )

        val alpha = AlphaAnimation(0.0f, 1.0f)

        return AnimationSet(true).apply {
            addAnimation(translate)
            addAnimation(alpha)
            duration = durationMs
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 动画绑定工具
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Legacy 引擎辅助：将 [Animation] 注入目标 [View]。
 *
 * 使用示例：
 * ```kotlin
 * LegacyAnimationBinder.bind(myView, myAnimation)
 * ```
 */
object LegacyAnimationBinder {

    /**
     * 将 [animation] 应用到 [view] 上，立即启动动画。
     *
     * @param view      承载动画的目标 View。
     * @param animation 待应用的 [Animation] 实例。
     */
    fun bind(view: View, animation: Animation) {
        view.startAnimation(animation)
    }
}

/**
 * 动画绑定工具类：提供便捷方法将 [AnimationResult] 自动解包并应用到目标 View。
 *
 * 使用示例：
 * ```kotlin
 * val result = WidgetAnimationApplier.apply(spec)
 * WidgetAnimationBinder.bindAnimation(myView, result)
 * ```
 */
object WidgetAnimationBinder {

    /**
     * 将 [result] 中的动画应用到 [view]。
     *
     * - [AnimationResult.Legacy]：取出 [Animation] 并调用 [View.startAnimation]。
     * - [AnimationResult.RemoteCompose] 与 [AnimationResult.None]：no-op（不做任何操作）。
     *
     * @param view   承载动画的目标 View。
     * @param result 由 [WidgetAnimationApplier.apply] 产生的动画结果。
     */
    fun bindAnimation(view: View, result: AnimationResult) {
        when (result) {
            is AnimationResult.Legacy -> {
                view.startAnimation(result.animation)
            }
            is AnimationResult.RemoteCompose -> {
                // RemoteCompose 表达式由宿主 Compose 引擎消费，此处 no-op
            }
            is AnimationResult.None -> {
                // 无动画
            }
        }
    }
}
