# Morainet Widget Kit

> 📖 Languages: [English](README.md) | **中文**

Jetpack Glance 之上的 Android Widget 开发工具链 — 状态管理、调度、动画、预览、调试与 DSL。

## 模块

| 模块 | 说明 | 状态 |
|------|------|------|
| `widget-core` | 生命周期、批量更新、Action 路由 | 🚧 第 1 阶段 |
| `widget-state` | `loading / success / error` 状态机 | 🚧 第 1 阶段 |
| `widget-workmanager` | WorkManager 周期/事件刷新封装 | 🚧 第 1 阶段 |
| `widget-animation` | 动画预设与双路径降级 | 🚧 第 1 阶段 |
| `widget-preview` | `@WidgetPreview` 多尺寸预览 | 🚧 第 1 阶段 |
| `widget-debugger` | RemoteViews 树与 PendingIntent 检查 | 🚧 第 1 阶段 |
| `widget-dsl` | YAML/JSON Schema → Glance 渲染 | 🚧 第 1 阶段末期 |
| `sample` | 参考实现与集成示例 | 🚧 第 1 阶段 |

## 快速开始

```kotlin
// settings.gradle.kts
includeBuild("path/to/widget-kit") // 或发布到 Maven 后依赖

// app/build.gradle.kts
dependencies {
    implementation(project(":widget-core"))
    implementation(project(":widget-state"))
    implementation(project(":widget-workmanager"))
    debugImplementation(project(":widget-preview"))
    debugImplementation(project(":widget-debugger"))
}
```

## 文档

- [开发技术文档](docs/DEVELOPMENT.md) — 架构、API 设计、模块依赖、开发规范
- [架构设计](docs/ARCHITECTURE.md) — 分层与数据流
- [路线图](docs/ROADMAP.md) — 2026 Q3–2027 规划

## 技术栈

- Kotlin 2.1 + Jetpack Glance 1.1.1
- WorkManager 2.10
- minSdk 26 / targetSdk 35（Android 17 适配进行中）

## License

Apache License 2.0
