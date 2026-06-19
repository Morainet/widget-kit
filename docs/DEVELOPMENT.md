# Morainet Widget Kit — 开发技术文档

> 版本：0.1.0-SNAPSHOT  
> 更新日期：2026-06-14  
> 目标周期：2026 Q3（Phase 1，3 个月）

---

## 1. 项目概述

### 1.1 背景

Android Widget 在 2026 年迎来平台级升级：Google 主推 **Jetpack Glance**，Android 16/17 引入 **Remote Compose** 渲染引擎，系统级 **Create My Widget**（Gemini）开始 rollout。但开发者侧体验仍然落后：

- 教程与开源工具稀缺
- WorkManager 与 Widget 更新策略文档分散
- RemoteViews / Glance 双栈并存，降级复杂
- 各厂商 Launcher 尺寸与刷新策略不一致

**Morainet Widget Kit** 定位为 Jetpack Glance 之上的开源工具链，解决「难开发、难调试、难调度、难维护」四类问题，并为 Phase 2 的 AI Widget 生成提供 DSL 底座。

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| 互补而非替代 | 不重复实现 Glance 核心，在其上封装生产力 API |
| DSL 优先 | 运行时、AI 生成、手写配置共用 `WidgetBlueprint` |
| 可降级 | Android 16+ Remote Compose，低版本 Legacy fallback |
| 模块化发布 | 各 artifact 可独立依赖，降低接入成本 |
| Debug 优先 | `widget-preview` / `widget-debugger` 优先于功能堆叠 |

### 1.3 技术栈

| 类别 | 选型 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.1.0 |
| 构建 | AGP + Gradle KTS | 8.7.3 / 8.11.1 |
| Widget 框架 | Jetpack Glance | 1.1.1（stable） |
| 后台调度 | WorkManager | 2.10.0 |
| 序列化 | kotlinx.serialization | 1.7.3 |
| UI 预览 | Jetpack Compose | BOM 2024.12.01 |
| minSdk / targetSdk | 26 / 35 | Android 8.0+ |

---

## 2. 仓库结构

```text
widget-kit/
├── build.gradle.kts              # 根构建脚本
├── settings.gradle.kts           # 模块注册
├── gradle/
│   ├── libs.versions.toml        # 版本目录（Version Catalog）
│   └── wrapper/
├── docs/
│   ├── DEVELOPMENT.md            # 本文档
│   ├── ARCHITECTURE.md           # 架构与数据流
│   └── ROADMAP.md                # 版本路线图
├── widget-core/                  # 生命周期、更新、Action 路由
├── widget-state/                 # UI 状态机
├── widget-workmanager/           # 周期/事件刷新
├── widget-animation/             # 动画预设与引擎选择
├── widget-preview/               # @WidgetPreview 与真机预览
├── widget-debugger/              # Inspector 与快照
├── widget-dsl/                   # Blueprint Schema 与解析
└── sample/                       # 集成示例 App
```

---

## 3. 模块依赖图

```text
                    ┌─────────────┐
                    │   sample    │
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌────────────┐  ┌─────────────┐  ┌──────────────┐
    │widget-dsl  │  │widget-preview│  │widget-debugger│
    └─────┬──────┘  └──────┬──────┘  └──────┬───────┘
          │                │                 │
          ▼                └────────┬────────┘
    ┌────────────┐                  ▼
    │widget-state│           ┌────────────┐
    └─────┬──────┘           │ widget-core │  ← 基础层
          │                  └──────┬──────┘
          │         ┌───────────────┼───────────────┐
          │         ▼               ▼               ▼
          │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
          └──│widget-workmgr│ │widget-animation│              │
             └──────────────┘ └──────────────┘               │
```

### 3.1 依赖规则

- `widget-core`：**无内部依赖**，仅依赖 Glance + Coroutines
- `widget-state` → `widget-core`
- `widget-workmanager` → `widget-core`
- `widget-animation` → `widget-core`
- `widget-preview` / `widget-debugger` → `widget-core`（建议 `debugImplementation`）
- `widget-dsl` → `widget-core` + `widget-state`

---

## 4. 模块 API 设计

### 4.1 widget-core

**职责**：统一 Widget 更新入口、Receiver 基类、Action 常量。

#### WidgetManager

```kotlin
// 更新所有实例
WidgetManager.updateAll(widget, context)

// 更新指定 ID
WidgetManager.update(widget, context, widgetIds)

// 查询已安装实例
WidgetManager.getInstalledIds(context, CounterWidget::class.java)
```

#### MoraineWidgetReceiver

继承 `GlanceAppWidgetReceiver`，提供：

- 自定义 Action 路由（`routeCustomAction`）
- 协程安全更新（`launchUpdate`）

#### WidgetActions

```kotlin
WidgetActions.ACTION_REFRESH
WidgetActions.ACTION_RETRY
WidgetActions.EXTRA_WIDGET_ID
```

**Phase 1 待实现**：

- [ ] 批量更新去重（防抖窗口 500ms）
- [ ] `SizeClass` 响应式尺寸适配
- [ ] Pin Widget 辅助 API

---

### 4.2 widget-state

**职责**：Widget 三态 UI 状态机，与 DSL `state` 节点对齐。

```kotlin
sealed class WidgetUiState<out T> {
    data object Loading
    data class Success<T>(val data: T)
    data class Error(val message: String, val retryable: Boolean)
}

// DSL 风格构建
val state = widgetState {
    loading()
    success(weatherData)
    error("Network unavailable")
}
```

**Glance 集成模式**：

```kotlin
@Composable
fun WeatherWidgetContent(state: WidgetUiState<WeatherData>) {
    when (state) {
        is WidgetUiState.Loading -> LoadingLayout()
        is WidgetUiState.Success -> SuccessLayout(state.data)
        is WidgetUiState.Error -> ErrorLayout(state.message, state.retryable)
    }
}
```

**Phase 1 待实现**：

- [ ] `WidgetStateHolder`（Glance `GlanceStateDefinition` 集成）
- [ ] Error 态自动绑定 `ACTION_RETRY`

---

### 4.3 widget-workmanager

**职责**：封装 Widget 周期刷新，规避 `updatePeriodMillis` 15 分钟限制与厂商限流。

#### WidgetRefreshWorker

```kotlin
abstract class WidgetRefreshWorker : CoroutineWorker {
    abstract val widget: GlanceAppWidget
    abstract suspend fun fetchData()
    // doWork() 自动 fetchData() → WidgetManager.updateAll()
}
```

#### WidgetScheduler

```kotlin
WidgetScheduler.schedulePeriodic<WeatherRefreshWorker>(
    context = context,
    uniqueWorkName = "weather_widget_refresh",
    intervalMinutes = 15,
)

WidgetScheduler.cancel(context, "weather_widget_refresh")
```

**调度策略建议**：

| 场景 | 策略 |
|------|------|
| 周期数据（天气、股票） | `PeriodicWorkRequest` 15min+ |
| 登录态变化 | `OneTimeWorkRequest` + 登录回调 |
| 网络恢复 | `NetworkType.CONNECTED` 约束 |
| 用户手动刷新 | `ACTION_REFRESH` → `OneTimeWorkRequest` |

**已知坑（文档化）**：

1. `updatePeriodMillis` 最小 30 分钟（API 限制），短周期必须用 WorkManager
2. 国产 ROM 可能杀死后台 Worker，需配合 `ACTION_REFRESH` 兜底
3. 避免 Worker 与 `onReceive` 双写导致更新风暴

**Phase 1 待实现**：

- [ ] `WidgetNetworkRefreshWorker`（网络恢复触发）
- [ ] `WidgetLoginRefreshWorker`（登录事件触发）
- [ ] 更新频率监控与日志

---

### 4.4 widget-animation

**职责**：统一动画 API，按 SDK 自动选择引擎。

```kotlin
enum class AnimationEngine {
    REMOTE_COMPOSE,           // Android 16+ (API 36)
    LEGACY_LAYOUT_ANIMATION,  // API 26–35
    NONE,
}

data class WidgetAnimationSpec(
    val preset: AnimationPreset,  // FADE, PULSE, COUNTER_TICK, SNAP_SCROLL
    val durationMs: Long = 300,
    val targetViewId: Int? = null,
)
```

**双路径策略**：

```text
API 36+  → Remote Compose 表达式动画
API 26–35 → layoutAnimation / ViewFlipper hack
API < 26  → 静态布局（不保证支持，minSdk 26）
```

**Phase 1 待实现**：

- [ ] `FadeTransition` preset
- [ ] `PulseTransition` preset
- [ ] `CounterTickTransition` preset

---

### 4.5 widget-preview

**职责**：Compose 风格 Widget 预览，对标 `glance-experimental-tools/appwidget-host`。

```kotlin
@WidgetPreview(widthDp = 180, heightDp = 110)
@Composable
fun CounterWidgetPreview() { ... }

// 真机预览容器
WidgetPreviewHost(
    displaySize = WidgetPreviewSizes.Medium_2x2,
) { CounterWidgetContent(...) }
```

**尺寸预设**：

| 常量 | 尺寸 (dp) | 对应格数 |
|------|-----------|----------|
| `Small_2x1` | 180 × 55 | 2×1 |
| `Medium_2x2` | 180 × 110 | 2×2 |
| `Large_4x2` | 360 × 110 | 4×2 |

**Phase 1 待实现**：

- [ ] RemoteViews 真机宿主（参考 Google appwidget-host）
- [ ] 多 Launcher 尺寸模拟（Pixel / Samsung / MIUI）
- [ ] Gradle Plugin：`@WidgetPreview` 编译期校验

---

### 4.6 widget-debugger

**职责**：Widget 运行时 Inspector。

```kotlin
WidgetInspector.record(WidgetDebugSnapshot(...))
WidgetInspector.getSnapshot(widgetId)
WidgetInspector.getAllSnapshots()
```

**快照内容**：

- RemoteViews 树（`RemoteViewNode`）
- PendingIntent 列表（`PendingIntentInfo`）
- 更新来源（WorkManager / Receiver / Manual）
- 最后更新时间

**Phase 1 待实现**：

- [ ] Compose Inspector UI（Widget Tree 可视化）
- [ ] PendingIntent 链路追踪
- [ ] Launcher 兼容性报告

---

### 4.7 widget-dsl

**职责**：Widget 声明式描述，连接 Kit 运行时与 Phase 2 AI 生成器。

#### Schema 示例

见 `widget-dsl/src/main/resources/blueprints/weather-widget.yaml`：

```yaml
meta:
  name: WeatherWidget
  targetSurfaces: [PHONE]
layout: SINGLE_ENTITY_2X2
components:
  - type: TEXT
    id: city
    props:
      text: "Shanghai"
theme:
  style: material_you
  useDynamicColor: true
```

#### Kotlin API

```kotlin
val blueprint = WidgetBlueprintParser.parseJson(jsonString)
val json = WidgetBlueprintParser.toJson(blueprint)
```

**Canonical Layouts（与 Google Glance 2026 对齐）**：

| Layout | 用途 |
|--------|------|
| `COUNTER_2X2` | 计数器、打卡 |
| `SINGLE_ENTITY_2X1` | 单行信息 |
| `SINGLE_ENTITY_2X2` | 天气、状态卡片 |
| `LIST_4X2` | 列表数据 |
| `STREAK_2X2` | 连续天数追踪 |

**Phase 1 待实现**：

- [ ] YAML 解析器（当前仅 JSON）
- [ ] `BlueprintRenderer`（DSL → Glance Composable）
- [ ] JSON Schema 校验文件

---

## 5. 开发环境搭建

### 5.1 前置条件

- Android Studio Ladybug 或更新版本
- JDK 17
- Android SDK 35

### 5.2 克隆与构建

```bash
cd widget-kit
./gradlew :sample:assembleDebug
```

Windows：

```powershell
cd widget-kit
.\gradlew.bat :sample:assembleDebug
```

### 5.3 接入自有项目

**方式 A：源码依赖（开发期）**

```kotlin
// settings.gradle.kts
includeBuild("../widget-kit")

// app/build.gradle.kts
dependencies {
    implementation("com.morainet.widget:widget-core:0.1.0-SNAPSHOT")
}
```

**方式 B：模块依赖（Monorepo）**

```kotlin
// settings.gradle.kts
include(":widget-core", ":widget-state", ...)
project(":widget-core").projectDir = file("../widget-kit/widget-core")
```

### 5.4 推荐依赖组合

```kotlin
// 生产环境
implementation(project(":widget-core"))
implementation(project(":widget-state"))
implementation(project(":widget-workmanager"))

// 仅 Debug
debugImplementation(project(":widget-preview"))
debugImplementation(project(":widget-debugger"))
```

---

## 6. 开发规范

### 6.1 包命名

```text
com.morainet.widget.{module}.{feature}
```

示例：

- `com.morainet.widget.core.WidgetManager`
- `com.morainet.widget.dsl.WidgetBlueprint`

### 6.2 命名约定

| 类型 | 约定 | 示例 |
|------|------|------|
| Widget 类 | `{Name}Widget` | `CounterWidget` |
| Receiver | `{Name}WidgetReceiver` | `CounterWidgetReceiver` |
| Worker | `{Name}RefreshWorker` | `WeatherRefreshWorker` |
| Work 唯一名 | `{name}_widget_refresh` | `weather_widget_refresh` |

### 6.3 公开 API 稳定性

- `0.1.x`：API 可 Breaking Change，需更新文档
- `0.2.x`：核心 API 冻结，仅增不改
- `1.0.0`：语义化版本，Breaking Change 需 major bump

### 6.4 测试策略

| 层级 | 工具 | 覆盖目标 |
|------|------|----------|
| 单元测试 | JUnit4 | DSL 解析、状态机、调度逻辑 |
| 仪器测试 | Espresso + Glance | Widget 渲染快照 |
| 手动测试 | Widget Studio | 多尺寸、多 Launcher |

---

## 7. Sample 应用说明

`sample` 模块提供最小可运行示例：

- `CounterWidget`：Glance Widget 实现
- `CounterWidgetReceiver`：继承 `MoraineWidgetReceiver`
- `MainActivity`：嵌入 `WidgetPreviewHost`

### 验证步骤

1. 运行 `sample` 到真机/模拟器
2. 长按桌面 → 添加 Widget → 选择「Widget Kit Sample」
3. 确认 Counter Widget 显示 `Count: 0`

---

## 8. Phase 1 里程碑（2026 Q3）

### Month 1 — 能开发

- [x] 项目脚手架与模块划分
- [ ] `widget-preview` 真机 RemoteViews 宿主
- [ ] `widget-debugger` Inspector UI
- [ ] `widget-core` 更新去重

### Month 2 — 能稳定跑

- [ ] `widget-workmanager` 完整调度策略
- [ ] `widget-state` GlanceState 集成
- [ ] `widget-animation` 3 个 preset
- [ ] Sample：Weather Widget 完整示例

### Month 3 — 能被社区用

- [ ] `widget-dsl` BlueprintRenderer
- [ ] 中英文 README + 第一篇技术文章
- [ ] Maven 发布 `0.1.0`
- [ ] GitHub Star 500+

---

## 9. 与 Phase 2（AI Widget）的接口预留

Phase 2 的 `morainet-widget-ai` 将依赖本 Kit，核心契约：

```text
Prompt / Image
    ↓
WidgetBlueprint (widget-dsl)
    ↓
BlueprintRenderer (widget-dsl)
    ↓
GlanceAppWidget (widget-core + widget-state + widget-animation)
    ↓
WidgetScheduler (widget-workmanager)
```

**AI 不直接生成 Glance Kotlin**，只生成 `WidgetBlueprint` JSON/YAML，以保证：

- API 版本隔离
- Preview / Inspector 可介入
- 可测试、可回滚

---

## 10. 参考资源

- [Jetpack Glance 官方文档](https://developer.android.com/develop/ui/compose/glance)
- [Glance 版本发布](https://developer.android.com/jetpack/androidx/releases/glance)
- [glance-experimental-tools](https://github.com/google/glance-experimental-tools)
- [platform-samples/appwidgets](https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets)
- [AppFunctions（AI 接入预留）](https://developer.android.com/ai/appfunctions)
- [Widget2Code 论文](https://arxiv.org/abs/2512.19918)

---

## 11. 贡献指南

1. Fork 仓库，从 `main` 拉 feature 分支
2. 每个 PR 聚焦单一模块
3. 新公开 API 需补充 KDoc 与 `docs/` 更新
4. Breaking Change 需更新 `ROADMAP.md` 并标注版本

---

*Morainet Widget Kit — 让 Widget 开发成为正常的 Android 开发。*
