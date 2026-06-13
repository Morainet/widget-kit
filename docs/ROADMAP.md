# Morainet Widget Kit — 路线图

## 总览

```text
2026 Q3          2026 Q4              2027
────────         ────────             ────
Widget Kit  →   Widget AI      →    完整生态
(基础设施)       (AI 生成)            (Gallery + Marketplace)
```

---

## Phase 1：Widget Kit（2026 Q3，v0.1.x）

**目标**：解决 Widget 开发体验，GitHub Star 500+

### v0.1.0-alpha（Month 1）

- [x] 多模块项目脚手架
- [x] 核心 API 骨架（Manager / State / Worker / DSL Schema）
- [x] Sample Counter Widget
- [ ] `widget-preview` RemoteViews 真机宿主
- [ ] `widget-debugger` Compose Inspector UI

### v0.1.0-beta（Month 2）

- [ ] `widget-workmanager` 网络/登录/周期完整策略
- [ ] `widget-state` GlanceStateDefinition 集成
- [ ] `widget-animation` Fade / Pulse / CounterTick
- [ ] Sample Weather Widget（完整数据流）

### v0.1.0（Month 3，正式版）

- [ ] `widget-dsl` BlueprintRenderer + YAML 解析
- [ ] Maven Central 发布
- [ ] 中英文文档站
- [ ] 技术文章 × 2（调试指南 + WorkManager 踩坑）

---

## Phase 2：Widget AI（2026 Q4，v0.2.x）

**目标**：一句话生成 Widget，差异化能力

### v0.2.0-alpha

- [ ] `morainet-widget-ai` 模块（独立仓库或子模块）
- [ ] Prompt → WidgetBlueprint（Gemini API）
- [ ] Widget Studio AI 插件（生成 → 预览 → 导出）

### v0.2.0-beta

- [ ] Image → WidgetBlueprint（参考 Widget2Code）
- [ ] 10 种 canonical layout 模板约束
- [ ] 生成质量评估与回退机制

### v0.2.0

- [ ] AppFunctions Scaffold（AI-Ready 桥接）
- [ ] CLI 工具：`morainet-widget generate "2x2 weather widget"`

---

## Phase 3：生态（2027，v1.x）

### v1.0.0

- [ ] Widget Gallery（开源模板库）
- [ ] Remote Compose 完整动画 preset
- [ ] Wear / Auto 多 surface 支持
- [ ] Launcher 兼容性数据库（Pixel / Samsung / 小米 / 华为）

### v1.1.0

- [ ] Widget Marketplace（模板交易/分发）
- [ ] 社区贡献 Blueprint 审核流程
- [ ] AppFunctions 公开 API 一键接入

---

## 版本策略

| 版本 | 阶段 | API 稳定性 |
|------|------|------------|
| 0.1.x | Phase 1 | 不保证，快速迭代 |
| 0.2.x | Phase 2 | 核心 API 冻结 |
| 1.0.0 | 生态 | 语义化版本 |

---

## 成功指标

| 阶段 | 指标 |
|------|------|
| Q3 2026 | GitHub Star 500+，Maven 下载 1000+ |
| Q4 2026 | AI 生成成功率 80%+（10 种模板内） |
| 2027 H1 | Gallery 50+ 模板，3+ 外部贡献者 |

---

## 不做清单

- ❌ 复刻 Google 系统级 Create My Widget
- ❌ 替代 Jetpack Glance 核心
- ❌ Phase 1 直接做 AI 代码生成（跳过 DSL）
- ❌ 过早做 Marketplace（需 Gallery 供给先行）
