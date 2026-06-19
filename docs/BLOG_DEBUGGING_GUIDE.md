# Android Widget 调试指南：从抓瞎到可视化

> **副标题**：基于 Morainet Widget Kit 的 Widget Inspector 实战  
> **适用读者**：Android 开发者（有 Widget 开发经验更佳）  
> **日期**：2026-06-19

---

## 一、痛点：Widget 调试为什么这么难？

Android Widget 开发有一个众所周知的尴尬——**调试体验极差**。具体来说，主要有三大痛点：

### 1.1 RemoteViews 无法断点

Widget 的 UI 本质上是一棵 `RemoteViews` 树，它运行在 Launcher 进程中。你在 `provideGlance()` 里设置的断点，只能命中构造过程；一旦 `RemoteViews` 被序列化并发送到 Launcher 进程，你就完全失去了对它的可见性。无法断点、无法 Layout Inspector、无法 Compose Tooling——你只能"猜"最终渲染出了什么。

```kotlin
// 你只能在这打断点，但最终的 RemoteViews 树是什么样子？完全不知道。
override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
        Text("Count: $count")  // <-- 断点只能打在这里
    }
}
```

### 1.2 PendingIntent 是黑盒

Widget 的交互完全依赖 `PendingIntent`：点击按钮、切换页面、触发刷新，都是通过 Intent 跨进程传递。问题在于：

- 你无法在 Widget 端调试 Intent 的构造过程
- 出错时只能看到"点击没反应"，却不知道是 action 写错了、extra 丢了，还是路由没命中
- 多个 Widget 实例共享同一个 Receiver，实例 ID 混乱时极难定位

### 1.3 更新链路追踪困难

一个典型的 Widget 更新可能经过以下链路：

```text
WorkManager 周期触发 → fetchData() → WidgetManager.updateAll()
        → provideGlance() → RemoteViews → Launcher 进程
```

任何一个环节出问题——Worker 被系统杀死、网络请求失败、状态序列化错误——最终表现都是"Widget 不更新"，但错误现场早已丢失。

---

## 二、Morainet Widget Kit 的调试方案

Morainet Widget Kit 的 `widget-debugger` 模块专为解决上述痛点而设计。它提供三大核心能力：

| 能力 | 说明 | 对应痛点 |
|------|------|----------|
| RemoteViews 树可视化 | 运行时捕获 Widget 的完整视图层级 | 痛点 1 |
| PendingIntent 链路追踪 | 递归收集所有 Action，展示 action / requestCode / targetPackage | 痛点 2 |
| 更新来源追踪 | 记录每次更新的触发源（WorkManager / Receiver / Manual）与时间戳 | 痛点 3 |

### 2.1 架构概览

```text
                    Widget 更新事件
                          │
                          ▼
                 WidgetInspector.record()
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
   RemoteViewNode    PendingIntentInfo   WidgetUpdateEvent
     (视图树)          (Action 列表)       (时间线)
          │               │               │
          └───────────────┼───────────────┘
                          ▼
                 WidgetDebuggerPanel (Compose UI)
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
      Tree View      Action Inspector  Update Timeline
```

### 2.2 核心 API

```kotlin
// widget-debugger 模块提供的核心入口
object WidgetInspector {

    // 记录一次更新快照
    fun record(snapshot: WidgetDebugSnapshot)

    // 查询指定 Widget 实例的快照
    fun getSnapshot(widgetId: Int): WidgetDebugSnapshot?

    // 获取所有已记录的快照
    fun getAllSnapshots(): List<WidgetDebugSnapshot>

    // 清空所有记录
    fun clear()
}

// 快照数据结构
data class WidgetDebugSnapshot(
    val widgetId: Int,
    val widgetClass: String,
    val remoteViewTree: RemoteViewNode?,
    val pendingIntentList: List<PendingIntentInfo>,
    val lastUpdatedAt: Long,
    val updateSource: String,        // "WorkManager" / "Receiver" / "Manual"
)

// RemoteViews 节点
data class RemoteViewNode(
    val viewId: Int,
    val viewType: String,            // "LinearLayout", "TextView", "ImageView" ...
    val children: List<RemoteViewNode> = emptyList(),
    val actions: List<PendingIntentInfo> = emptyList(),
)

// PendingIntent 元信息
data class PendingIntentInfo(
    val action: String?,
    val requestCode: Int,
    val targetPackage: String?,
)
```

### 2.3 接入方式

```kotlin
// app/build.gradle.kts
dependencies {
    // 生产依赖
    implementation(project(":widget-core"))
    implementation(project(":widget-state"))
    implementation(project(":widget-workmanager"))

    // 仅 Debug 构建
    debugImplementation(project(":widget-preview"))
    debugImplementation(project(":widget-debugger"))
}
```

在 `SampleApplication` 中启用调试模式：

```kotlin
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug 构建自动开启 Inspector 记录
        if (BuildConfig.DEBUG) {
            WidgetDebugger.init(this)
        }
    }
}
```

---

## 三、实战：用 Inspector 排查"Weather Widget 不更新"

下面以 Morainet Widget Kit 的 Weather Widget 为例，演示如何用 Inspector 定位问题。

### 3.1 场景描述

用户报告：Weather Widget 显示"Shanghai 26°C Sunny"，但实际应该是 30°C。点击刷新按钮也没用。

### 3.2 Step 1：打开 Inspector UI

启动 Sample 应用，进入 Debugger 面板。你会看到：

```text
┌──────────────────────────────────────────┐
│  Widget Debugger                          │
│  ─────────────────────────────────────── │
│  [WeatherWidget] [CounterWidget]          │ ← 多 Widget 切换
│  ─────────────────────────────────────── │
│  RemoteViews Tree:                        │
│  ├── LinearLayout (root)                  │
│  │   ├── TextView  id:city  "Shanghai"    │
│  │   ├── ImageView id:weather_icon        │
│  │   └── TextView  id:updated_at  "..."   │
│  ─────────────────────────────────────── │
│  PendingIntent Actions:                   │
│  ├── action=ACTION_REFRESH  rc=42         │
│  └── action=ACTION_RETRY    rc=99         │
│  ─────────────────────────────────────── │
│  Update Timeline:                         │
│  ├── 10:35:21  WorkManager  ✓            │
│  ├── 10:20:15  WorkManager  ✓            │
│  └── 10:05:08  WorkManager  ✗ (error)    │
└──────────────────────────────────────────┘
```

### 3.3 Step 2：检查 RemoteViews 树

从树结构可以确认：

- `city` TextView 存在，显示 "Shanghai"——数据确实没更新
- `weather_icon` ImageView 存在——布局结构正确
- 没有多余的空白 View——不是布局问题

### 3.4 Step 3：检查 PendingIntent

点击 ACTION_REFRESH 行，展开详情：

```text
PendingIntentInfo(
    action = "com.morainet.widget.ACTION_REFRESH",
    requestCode = 42,
    targetPackage = "com.morainet.widget.sample"
)
```

Action 正确，requestCode 正确，targetPackage 正确——点击事件本身没问题。

### 3.5 Step 4：检查 Update Timeline

时间线显示最近三次更新：

```
10:35:21  WorkManager  ✓    ← 成功但数据没变
10:20:15  WorkManager  ✓    ← 成功但数据没变
10:05:08  WorkManager  ✗    ← 失败！
```

这说明问题出在 **10:05 那次失败后，后续更新都在用缓存的旧数据**。

### 3.6 Step 5：结合日志定位根因

使用 `WidgetRefreshLogger` 查看调度日志：

```kotlin
// widget-workmanager 提供
object WidgetRefreshLogger {
    // 获取最近 100 条刷新日志
    fun getRecentLogs(): List<String>
}
```

日志输出：

```text
[10:05:08] WeatherRefreshWorker: fetchData() failed: java.net.UnknownHostException
[10:05:08] WeatherRefreshWorker: saving error state
[10:05:08] WeatherRefreshWorker: using cached data from 10:04:55
[10:20:15] WeatherRefreshWorker: fetchData() success (from cache)
[10:35:21] WeatherRefreshWorker: fetchData() success (from cache)
```

根因确认：**DNS 解析失败导致网络请求失败，Worker 进入离线模式，一直使用缓存数据。** 后续两次"成功"实际上是缓存命中，不是真实网络请求。

### 3.7 解决方案

问题定位后，修复方案很明确：

1. **增加网络恢复 Worker**：使用 `WidgetNetworkRefreshWorker`，在网络恢复时自动重新拉取
2. **在缓存数据上标注来源**：UI 显示 "Shanghai 26°C (cached)"，让用户知道这是缓存数据
3. **缩短重试间隔**：`WidgetRefreshWorker` 基类已支持重试逻辑，可配置 `retryCount` 和 `retryDelayMs`

修复后的 Worker：

```kotlin
class WeatherRefreshWorker : WidgetRefreshWorker() {

    override val widget: GlanceAppWidget = WeatherWidget()
    override val retryCount: Int = 3
    override val retryDelayMs: Long = 30_000L  // 30 秒后重试

    override suspend fun fetchData() {
        val weather = WeatherApiService.fetchCurrentWeather(lat = 31.23, lon = 121.47)
        val state = WidgetUiState.Success(weather.toDisplayMap())
        WeatherWidget.saveState(applicationContext, state)
    }
}
```

---

## 四、开发期最佳实践

### 4.1 用 Preview 提前发现布局问题

```kotlin
// 在 Activity 中嵌入 Widget 预览
setContent {
    WidgetPreviewHost(
        displaySize = WidgetPreviewSizes.Medium_2x2,
    ) {
        WeatherWidgetContent(state = currentState)
    }
}
```

| 预设尺寸 | 尺寸 (dp) | 对应格数 |
|----------|-----------|----------|
| `Small_2x1` | 180 × 55 | 2×1 |
| `Medium_2x2` | 180 × 110 | 2×2 |
| `Large_4x2` | 360 × 110 | 4×2 |

**最佳实践**：在开发期至少覆盖 3 种尺寸的预览，确保布局在窄屏、宽屏下都不出问题。

### 4.2 用 Inspector 验证运行时行为

```text
开发流程建议：

1. 用 Preview 快速迭代 UI        ← 开发期
2. 安装到真机，添加 Widget        ← 验证期
3. 打开 Inspector 检查视图树      ← 调试期
4. 检查 PendingIntent 链路        ← 调试期
5. 触发刷新，查看 Timeline        ← 调试期
```

### 4.3 用 RefreshLogger 追踪调度链路

```kotlin
// 在 Worker 中添加日志
class MyRefreshWorker : WidgetRefreshWorker() {
    override suspend fun fetchData() {
        WidgetRefreshLogger.log("MyRefreshWorker", "Starting data fetch...")
        try {
            val data = repository.fetch()
            WidgetRefreshLogger.log("MyRefreshWorker", "Fetch success: $data")
        } catch (e: Exception) {
            WidgetRefreshLogger.log("MyRefreshWorker", "Fetch failed: ${e.message}")
            throw e
        }
    }
}
```

`WidgetRefreshLogger` 维护环形缓冲区（最近 100 条），不会造成内存压力，可以在 Debug 菜单中实时查看。

---

## 五、Launcher 兼容性陷阱

不同厂商 Launcher 对 Widget 的实现差异巨大。Morainet Widget Kit 内置了 `LauncherCompatibilityChecker` 来帮助检测和适配。

### 5.1 常见兼容性问题

| 厂商 | 问题 | 影响 |
|------|------|------|
| Samsung One UI | 最小刷新间隔限制为 30 分钟 | `updatePeriodMillis` 无效 |
| Xiaomi MIUI | 后台 Widget 可能被冻结 | Worker 无法执行 |
| Huawei EMUI | 自定义 Grid 尺寸不标准 | 布局被裁剪或留白 |
| OPPO ColorOS | PendingIntent flags 冲突 | 点击事件丢失 |
| Google Pixel | 标准实现，问题最少 | 基线参考 |

### 5.2 使用 LauncherCompatibilityChecker

```kotlin
val profile = LauncherCompatibilityChecker.detect(context)
when (profile) {
    is LauncherProfile.Pixel -> {
        // 标准尺寸，无特殊处理
    }
    is LauncherProfile.Samsung -> {
        // Samsung: 使用 WorkManager 替代 updatePeriodMillis
        WidgetScheduler.schedulePeriodic<WeatherRefreshWorker>(
            context, "weather_refresh", intervalMinutes = 15
        )
    }
    is LauncherProfile.Xiaomi -> {
        // Xiaomi: 添加自启动权限引导
        showAutoStartGuide()
    }
    is LauncherProfile.Huawei -> {
        // Huawei: 使用保守的 padding 值
        applyHuaweiPaddingFix()
    }
}
```

### 5.3 通用兜底策略

1. **始终使用 WorkManager 调度**，不依赖 `updatePeriodMillis`
2. **提供手动刷新按钮**（`ACTION_REFRESH`），作为所有 ROM 的兜底
3. **布局使用 `wrap_content` + 合理的 padding**，避免固定尺寸
4. **PendingIntent 使用 `FLAG_IMMUTABLE`**（Android 12+ 强制要求）

---

## 六、总结

Widget 调试的核心思路是 **让不可见的变成可见的**：

| 传统方式 | Morainet Widget Kit |
|----------|---------------------|
| 打印 Log 猜视图结构 | Inspector Tree View 可视化 |
| 手动构造 Intent 验证 | PendingIntent 链路追踪 |
| 凭感觉判断更新时间 | Update Timeline 时间线 |
| 逐厂商测试适配 | LauncherCompatibilityChecker |

通过 `widget-preview`（开发期预览）+ `widget-debugger`（运行时检查）+ `WidgetRefreshLogger`（调度追踪）三重工具，Widget 开发体验可以接近正常的 Android 开发。

---

## 参考资源

- [Jetpack Glance 官方文档](https://developer.android.com/develop/ui/compose/glance)
- [glance-experimental-tools](https://github.com/google/glance-experimental-tools)
- [Morainet Widget Kit — 开发技术文档](DEVELOPMENT.md)
- [Morainet Widget Kit — 架构设计](ARCHITECTURE.md)

---

*本文是 Morainet Widget Kit Phase 1 技术文章系列第一篇。下一篇预告：**WorkManager + Widget 的调度陷阱与最佳实践**。*
