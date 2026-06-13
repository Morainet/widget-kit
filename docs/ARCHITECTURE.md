# Morainet Widget Kit — 架构设计

## 1. 分层架构

```text
┌──────────────────────────────────────────────────────────────┐
│                     应用层 (Your App)                         │
│  CounterWidget / WeatherWidget / Custom Business Logic         │
├──────────────────────────────────────────────────────────────┤
│                     工具层 (Morainet Widget Kit)              │
│  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌────────┐ │
│  │preview  │ │debugger │ │workmgr   │ │animation│ │  dsl   │ │
│  └────┬────┘ └────┬────┘ └────┬─────┘ └────┬────┘ └───┬────┘ │
│       └───────────┴───────────┴────────────┴──────────┘        │
│  ┌─────────┐ ┌─────────┐                                     │
│  │  state  │ │  core   │                                     │
│  └────┬────┘ └────┬────┘                                     │
├───────┴───────────┴──────────────────────────────────────────┤
│                     平台层 (Android / Google)                 │
│  Jetpack Glance │ Remote Compose │ WorkManager │ AppWidget   │
└──────────────────────────────────────────────────────────────┘
```

## 2. Widget 更新数据流

```text
触发源                    调度层                  渲染层
───────                  ──────                  ──────

用户添加 Widget ──────► GlanceAppWidgetReceiver
                              │
手动刷新 Action ──────► MoraineWidgetReceiver.routeCustomAction()
                              │
WorkManager 周期 ─────► WidgetRefreshWorker.doWork()
                              │
网络恢复 ─────────────► WidgetNetworkRefreshWorker (Phase 1)
                              │
                              ▼
                        fetchData() / 业务逻辑
                              │
                              ▼
                        WidgetManager.updateAll()
                              │
                              ▼
                        GlanceAppWidget.provideGlance()
                              │
                              ▼
                        RemoteViews / Remote Compose
                              │
                              ▼
                        Launcher 桌面显示
```

## 3. 状态管理数据流

```text
数据源 (API / DB / Cache)
        │
        ▼
WidgetRefreshWorker.fetchData()
        │
        ▼
WidgetUiState<T>  ──► GlanceStateDefinition (Phase 1)
        │
        ▼
@Composable WidgetContent(state)
        │
        ├── Loading  → 骨架屏 / Spinner
        ├── Success  → 数据布局
        └── Error    → 错误提示 + Retry Action
```

## 4. DSL 渲染管线

```text
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│ YAML / JSON │────►│ Blueprint    │────►│ CanonicalLayout │
│ (手写/AI)   │     │ Parser       │     │ Selector        │
└─────────────┘     └──────────────┘     └────────┬────────┘
                                                   │
                     ┌──────────────┐              │
                     │ ThemeBridge  │◄─────────────┤
                     │ (Material You)│             │
                     └──────┬───────┘              │
                            │                      ▼
                     ┌──────▼───────┐     ┌─────────────────┐
                     │ Animation    │◄────│ Component       │
                     │ Binder       │     │ Renderer        │
                     └──────┬───────┘     └────────┬────────┘
                            │                        │
                            └──────────┬─────────────┘
                                       ▼
                              Glance Composable 输出
```

## 5. 动画双路径架构

```text
WidgetAnimationSpec
        │
        ▼
AnimationEngineSelector.select(sdkInt)
        │
        ├── API 36+ ──► RemoteComposeEngine
        │                 ├── 表达式驱动 (sin, pulse)
        │                 ├── 粒子效果
        │                 └── 形变过渡
        │
        └── API 26–35 ──► LegacyEngine
                          ├── layoutAnimation 注入
                          ├── ViewFlipper 轮播
                          └── ProgressBar 假动画
```

## 6. 调试架构

```text
Widget 更新事件
        │
        ▼
WidgetInspector.record(snapshot)
        │
        ├── RemoteViewNode 树
        ├── PendingIntentInfo 列表
        └── 更新元数据 (source, timestamp)
        │
        ▼
WidgetDebugger UI (Compose)
        │
        ├── Tree View
        ├── Action Inspector
        └── Update Timeline
```

## 7. 模块通信约束

- 模块间**禁止**循环依赖
- `widget-core` 不得依赖任何其他 Kit 模块
- `widget-preview` / `widget-debugger` 不得引入 WorkManager 依赖
- 跨模块共享类型放在 `widget-dsl` 或 `widget-core`

## 8. 与 AI 生成器的边界

| 层 | 归属 | 说明 |
|----|------|------|
| Prompt → DSL | morainet-widget-ai (Phase 2) | LLM 输出 Blueprint |
| DSL Schema | widget-dsl (Phase 1) | 协议定义 |
| DSL → Glance | widget-dsl (Phase 1) | 渲染器 |
| 运行时 | widget-core/state/workmanager | 稳定 API |
| 调试 | widget-preview/debugger | 人工介入 |

AI 模块**不得**绕过 DSL 直接操作 Glance API，以确保生成结果可审查、可测试。
