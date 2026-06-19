package com.morainet.widget.core

/**
 * 主流 Android Launcher 尺寸与刷新行为差异数据。
 *
 * 各厂商 Launcher 在 Widget 尺寸计算、圆角裁剪、刷新策略上存在差异。
 * 此数据类供 [WidgetPreviewHost] 模拟多厂商环境，以及 [WidgetDebuggerPanel] 生成兼容性报告。
 *
 * @param manufacturer  厂商名称（如 "Samsung", "Xiaomi"）
 * @param gridColumns   桌面列数（默认 4 列手机 / 5 列平板）
 * @param gridRows      桌面行数
 * @param cellSizeDp    单格尺寸（dp），为近似值
 * @param marginDp      系统自动添加的边距（dp）
 * @param cornerRadiusDp 系统裁剪圆角（dp），0 表示无裁剪
 * @param minRefreshIntervalMs 厂商 ROM 对 Widget 刷新的最小间隔（ms），null 表示无限制
 * @param notes          备注（已知兼容性问题）
 */
data class LauncherProfile(
    val manufacturer: String,
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val cellSizeDp: Int = 90,
    val marginDp: Int = 8,
    val cornerRadiusDp: Int = 0,
    val minRefreshIntervalMs: Long? = null,
    val notes: String = "",
) {
    /** 计算 2x1 Widget 在桌面的实际渲染尺寸（dp）。 */
    fun sizeForGrid(cols: Int, rows: Int): Pair<Int, Int> {
        val width = cols * cellSizeDp - marginDp * 2
        val height = rows * cellSizeDp - marginDp * 2
        return width to height
    }

    companion object {
        /** Google Pixel Launcher（参考基准）。 */
        val PIXEL = LauncherProfile(
            manufacturer = "Pixel",
            gridColumns = 4,
            gridRows = 5,
            cellSizeDp = 90,
            marginDp = 8,
            cornerRadiusDp = 16,
            notes = "Android 默认 Launcher，行为最标准",
        )

        /** Samsung One UI Home。 */
        val SAMSUNG = LauncherProfile(
            manufacturer = "Samsung",
            gridColumns = 4,
            gridRows = 6,
            cellSizeDp = 88,
            marginDp = 10,
            cornerRadiusDp = 20,
            minRefreshIntervalMs = 30_000L,
            notes = "One UI 对 Widget 刷新有 30s 最小间隔限制；圆角裁剪更激进",
        )

        /** Xiaomi MIUI / HyperOS Launcher。 */
        val XIAOMI = LauncherProfile(
            manufacturer = "Xiaomi",
            gridColumns = 4,
            gridRows = 6,
            cellSizeDp = 86,
            marginDp = 12,
            cornerRadiusDp = 12,
            notes = "MIUI 默认 4x6 布局，边距较大；部分机型会杀后台 Worker",
        )

        /** Huawei HarmonyOS / EMUI Launcher。 */
        val HUAWEI = LauncherProfile(
            manufacturer = "Huawei",
            gridColumns = 4,
            gridRows = 5,
            cellSizeDp = 88,
            marginDp = 8,
            cornerRadiusDp = 8,
            notes = "HarmonyOS Launcher 使用独立 Widget 框架，RemoteViews 兼容性待验证",
        )

        /** OPPO ColorOS Launcher。 */
        val OPPO = LauncherProfile(
            manufacturer = "OPPO",
            gridColumns = 4,
            gridRows = 6,
            cellSizeDp = 84,
            marginDp = 14,
            cornerRadiusDp = 10,
            minRefreshIntervalMs = 60_000L,
            notes = "ColorOS 有较严格的电池优化，Widget 刷新可能被延迟",
        )

        /** 所有内置 Profile 列表。 */
        val ALL = listOf(PIXEL, SAMSUNG, XIAOMI, HUAWEI, OPPO)
    }
}

/**
 * Launcher 兼容性检测结果。
 */
data class LauncherCompatibilityReport(
    val profile: LauncherProfile,
    val widgetSizeIssues: List<String> = emptyList(),
    val refreshIssues: List<String> = emptyList(),
    val renderingIssues: List<String> = emptyList(),
) {
    val hasIssues: Boolean
        get() = widgetSizeIssues.isNotEmpty() || refreshIssues.isNotEmpty() || renderingIssues.isNotEmpty()

    val issueCount: Int
        get() = widgetSizeIssues.size + refreshIssues.size + renderingIssues.size
}

/**
 * Launcher 兼容性检测工具。
 *
 * 根据 Widget 设计尺寸与目标 Launcher Profile，生成兼容性报告。
 */
object LauncherCompatibilityChecker {

    /**
     * 检测 Widget 在指定 Launcher 下的兼容性。
     *
     * @param designWidthDp  Widget 设计宽度（dp）
     * @param designHeightDp Widget 设计高度（dp）
     * @param profile        目标 Launcher Profile
     * @return 兼容性报告
     */
    fun check(
        designWidthDp: Int,
        designHeightDp: Int,
        profile: LauncherProfile,
    ): LauncherCompatibilityReport {
        val sizeIssues = mutableListOf<String>()
        val refreshIssues = mutableListOf<String>()
        val renderingIssues = mutableListOf<String>()

        // 尺寸检测
        val (gridWidth, gridHeight) = profile.sizeForGrid(2, 2)
        if (designWidthDp > gridWidth) {
            sizeIssues.add(
                "宽度 ${designWidthDp}dp 超出 ${profile.manufacturer} 2x2 格宽 ${gridWidth}dp，可能被裁剪",
            )
        }
        if (designHeightDp > gridHeight) {
            sizeIssues.add(
                "高度 ${designHeightDp}dp 超出 ${profile.manufacturer} 2x2 格高 ${gridHeight}dp，可能被裁剪",
            )
        }

        // 圆角裁剪
        if (profile.cornerRadiusDp > 0) {
            renderingIssues.add(
                "${profile.manufacturer} 应用 ${profile.cornerRadiusDp}dp 圆角裁剪，边缘内容可能被截断",
            )
        }

        // 刷新限制
        if (profile.minRefreshIntervalMs != null) {
            refreshIssues.add(
                "${profile.manufacturer} ROM 限制 Widget 最小刷新间隔 ${profile.minRefreshIntervalMs}ms，" +
                    "高频更新可能被合并或丢弃",
            )
        }

        // 后台限制（MIUI / ColorOS）
        if (profile.manufacturer == "Xiaomi" || profile.manufacturer == "OPPO") {
            refreshIssues.add(
                "${profile.manufacturer} 有较严格的后台限制，" +
                    "建议在 App 内引导用户关闭电池优化以确保 WorkManager 正常运行",
            )
        }

        return LauncherCompatibilityReport(
            profile = profile,
            widgetSizeIssues = sizeIssues,
            refreshIssues = refreshIssues,
            renderingIssues = renderingIssues,
        )
    }

    /**
     * 批量检测 Widget 在所有内置 Launcher Profile 下的兼容性。
     */
    fun checkAll(
        designWidthDp: Int,
        designHeightDp: Int,
    ): List<LauncherCompatibilityReport> {
        return LauncherProfile.ALL.map { profile ->
            check(designWidthDp, designHeightDp, profile)
        }
    }
}
