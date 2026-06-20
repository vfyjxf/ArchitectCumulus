# ArchitectCumulus Showcase

一个交互式 3D 可视化展示，用来讲解 ArchitectCumulus 的设计。

## 运行

```bash
cd showcase
npm install        # or pnpm install / yarn
npm run dev        # open http://localhost:5174
```

构建静态产物：

```bash
npm run build      # outputs to dist/
npm run preview    # serve the dist build
```

## 技术栈

- **Vite 6 + React 18 + TypeScript 5**
- **Tailwind CSS 3**（自定义 ink/brand 配色）
- **@react-three/fiber + @react-three/drei + three.js**（9 个独立 3D 场景）
- **framer-motion**（所有 section 入场动画 + 布局动画）
- **lucide-react**（图标）
- **zustand**（Builder / Runtime 状态管理）

## 页面结构

18 个 section，总长约 24 500 px：

| # | Section | 内容 |
|---|---------|------|
| 00 | Hero | 项目简介 + 浮动 3D cube |
| 01 | Pipeline | 6 阶段管线（DSL → IR → Assets → Plan → Runtime → Output）每阶段可点击 |
| 02 | **Rationale** | **10 个核心设计决策深度解析（problem / alternatives / solution / why / cost / metrics）** |
| 03 | 3D Builder | 放置方块、hover 看 IR、4 档旋转预览 transform 候选、点击选中、放置动画 |
| 04 | **Transform Lab** | **看 check plan 怎么短路 transform 候选：10 候选 × 实时求值 × 短路结果** |
| 05 | **Composer** | **可视化结构组装器：拼组件 → 实时 IR / DSL / 校验 / check plan 统计** |
| 06 | Targets | 7 种 Target 卡片（block/block_entity/entity/fluid/capability/context/region），点击弹窗看场景 |
| 07 | **Predicate Composer** | **拖拽谓词链 + 实时求值（含 required 短路 / optional skip 语义）** |
| 08 | Components | 5 种 Component 原语，每个带 3D 预览 |
| 09 | Runtime | 8×8 section 网格，可调 budget/mutation/loaded rate，模拟 dirty → check → delta |
| 10 | Substructure | 工厂 → 双臂 → 工具 的 3D 嵌套树形可视化 |
| 11 | **Lifecycle Lab** | **可交互子结构生命周期演示：add/remove/break block → ADDED/STATUS_CHANGED/REMOVED 事件流 + 父上下文镜像** |
| 12 | Scenarios | 8 种真实游戏玩法 → Cumulus API 映射（GT/Create/AE2/Botania/Mekanism...） |
| 13 | **Case Studies** | **vanilla NeoForge vs Cumulus 3 个完整对比（代码量/性能/维护场景）** |
| 14 | **Data-Driven Reload Lab** | **JSON 编辑器 + 3D 预览 + 6 阶段 reload 管线动画 + 错误诊断（含 5 个预设）** |
| 15 | **API Cookbook** | **21 个真实 API 场景 × 8 类 × 3 难度，每张卡：场景 → 代码 → 逐行解释 → 运行时 trace → 陷阱** |
| 16 | Diagnostics | 可交互 DiagnosticGraph，11 类标准 code 列表 |
| 17 | Playground | 5 步端到端 API 流程，含每步真实输出 |

## 与项目主仓库的关系

`showcase/` 是独立的 Vite 子项目，不参与 NeoForge 构建链路。所有展示数据来源于：

- `../README.md` / `../ARCHITECTURE.md` / `../USAGE.md` / `../IMPLEMENTATION.md`
- `../docs/superpowers/specs/2026-06-08-architect-cumulus-architecture.md`
- `../docs/superpowers/specs/2026-04-20-structure-checking-architecture-design.md`
- `../docs/superpowers/plans/2026-04-20-core-ir-definition-dsl.md`
- `../src/main/java/dev/vfyjxf/cumulus/api/**`

展示页面里的所有 Java 代码片段、Target 列表、Component 列表、诊断 Code、API 调用形态都是从这些源文件提炼的。

## 文件结构

```
showcase/
├─ index.html
├─ vite.config.ts
├─ tailwind.config.js
├─ postcss.config.js
├─ tsconfig.json
├─ src/
│  ├─ main.tsx
│  ├─ App.tsx                  # section 串接
│  ├─ styles.css               # tailwind + 自定义动画
│  ├─ lib/utils.ts             # cn() helper
│  ├─ hooks/useScrollProgress  # 顶部进度条
│  ├─ components/
│  │  ├─ TopNav.tsx
│  │  └─ ui/
│  │     ├─ Badge.tsx
│  │     ├─ Button.tsx
│  │     ├─ Card.tsx
│  │     ├─ CodeBlock.tsx      # 带 Java 语法高亮 + 复制
│  │     └─ Section.tsx
│  ├─ data/                    # 静态展示数据
│  │  ├─ targets.ts            # 7 种 Target
│  │  ├─ components.ts         # 5 种 Component
│  │  ├─ scenarios.ts          # 8 种游戏场景
│  │  ├─ cases.ts              # 3 个 vanilla vs Cumulus 案例对比
│  │  ├─ rationale.ts          # 10 个设计决策深度解析
│  │  ├─ apiCookbook.ts        # 21 个 API 场景 × 8 类
│  │  └─ diagnostics.ts        # 11 种 DiagnosticCode + DiagnosticGraph 节点
│  ├─ three/                   # 3D 场景 + 状态
│  │  ├─ SceneEnv.tsx          # 灯光 / OrbitControls / 地面 公共组件
│  │  ├─ HeroCube.tsx          # 浮动立方体
│  │  ├─ BuilderViewport.tsx   # 主交互构造器（含 hover/rotate/select/animation）
│  │  ├─ builderStore.ts       # zustand store + IR 生成器 + 校验逻辑
│  │  ├─ TransformLabViewport.tsx # Transform 实验室 3D
│  │  ├─ transformLab.ts       # 10 个 transform 定义 + 求值引擎
│  │  ├─ composerStore.ts      # 结构组装器 store + DSL/IR/check plan 生成
│  │  ├─ predicateComposer.ts  # 谓词链 store + 求值引擎
│  │  ├─ dataDrivenStore.ts    # JSON reload store + 解析器 + 校验器 + 管线仿真
│  │  ├─ StructurePreview.tsx  # 从 JSON 渲染任意结构 3D 预览
│  │  ├─ lifecycleStore.ts     # 子结构生命周期 store + event stream
│  │  ├─ RuntimeViewport.tsx   # 8×8 section 网格
│  │  ├─ runtimeSim.ts         # 调度器仿真
│  │  ├─ SubstructureViewport.tsx
│  │  └─ ComponentPreview.tsx  # 每种 component 的 mini 3D
│  └─ sections/
│     ├─ Hero.tsx
│     ├─ Pipeline.tsx
│     ├─ Rationale.tsx
│     ├─ Builder.tsx
│     ├─ TransformLab.tsx
│     ├─ Composer.tsx
│     ├─ Targets.tsx
│     ├─ PredicateComposer.tsx
│     ├─ Components.tsx
│     ├─ Runtime.tsx
│     ├─ Substructure.tsx
│     ├─ LifecycleLab.tsx      # 生命周期事件可交互演示
│     ├─ Scenarios.tsx
│     ├─ CaseStudies.tsx
│     ├─ DataDriven.tsx
│     ├─ ApiCookbook.tsx       # 21 个 API 场景图鉴
│     ├─ Diagnostics.tsx
│     ├─ Playground.tsx
│     └─ Footer.tsx
└─ public/cube.svg
```
