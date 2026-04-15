# ArchitectCumulus v1：Minecraft 多方块结构检查/编译库（API + 流程设计）

> 状态：**设计稿（API + 流程）** + **v2 数据驱动骨架（Codec/Reload 占位）**  
> 范围：**Minecraft 场景优先（NeoForge）**，不考虑非 MC 泛化；v1 以 **API/流程/DSL/ dummy common** 为主；v2 追加 **Codec(JSON) + ReloadListener/Manager** 骨架（仍不强制加载区块，不做 UI；datagen 可后续再接）。

## 0. 目标与非目标

### 0.1 目标（v1）
- **高性能**：支持低频全量检查 + 中频小块（分块）检查；无事件流时也能预算化自驱动收敛。
- **高灵活/强扩展**：检查谓语不局限单方块，可作用于集合/关系/图；支持方块/方块实体/实体等多类型“部件”。
- **可编译**：结构合法后可编译为运行时 `RuntimeContext`（供机器运行逻辑使用）。
- **跨结构只读依赖**：结构实例之间允许只读上下文依赖；可检测并诊断循环依赖。
- **完整非法分析**：输出结构化诊断（错误码、证据、因果/trace、修复建议/自动构建提示数据）。
- **可视化/数据生产支持（仅数据访问）**：提供 `ExportView`/调试数据访问接口（不做 UI）。
- **数据驱动友好（仅设计）**：schema 是“纯数据”，通过 `TypeRegistry` 构建运行时组件；预留 codec/serializer 接口（v1 不做 datapack 接入）。
- **Builder DSL**：用代码构建 `ArchitectSchema`（与数据驱动 schema 同构）。

### 0.2 非目标（v1 明确不做）
- datapack JSON 加载、/reload 热重载、datagen 生成（后续版本实现；v1 只提供可接入的设计与接口占位）。
- 强制加载区块/主动下 ticket（只产出 `DataRequest` 建议；宿主决定是否加载）。
- UI 可视化（只提供数据访问/导出视图）。
- 跨维度统一（每个维度/Level 一套引擎；API 维度无关）。

## 1. 核心共识（本设计已确认）

### 1.1 作用域与实例键
- `ArchitectEngine` **绑定单个 Level**（维度无关），不在 engine 内处理跨维度。
- `ArchitectSchema` 表示**单个结构类型**（一台机器/一种多方块结构蓝图）。
- v1 实例锚点 `AnchorRef` **仅支持 `BlockPos`**：
  - `InstanceKey = (schemaId, anchorPos)`（不含维度）。

### 1.2 分块（Partition）
- 默认分块键：`SectionKey = (chunkX, sectionY, chunkZ)`，对齐 MC section（16×16×16）。
- 分块用于：缓存/索引边界、增量 dirty 推导、调度并行粒度、缺数据请求粒度。

### 1.3 缺数据语义（REQUEST）
- 检查/编译/链接阶段遇到缺数据：返回 `INCOMPLETE`，并附带 `DataRequests<SectionKey>`。
- `DataRequest` 是**建议**：引擎不强制加载；宿主（模组/服务器系统）决定是否加载。
- **双层粒度**：
  - 核心输出 `SectionKey` 请求；
  - MC 默认适配器（后续实现）可聚合为 `ChunkPos(+sections)` 给宿主更友好地处理。

### 1.4 Anchor 发现限制
- `AnchorFinder` 默认**只在已加载范围内**发现/探测 anchor：
  - 不产生 `DataRequest`（不跨未加载范围做确认）。
  - 需要跨未加载范围才能确认的 anchor：视为“暂不可发现/不可确认”，等待未来加载后再发现。

### 1.5 无事件流也能收敛（Drivers）
- 同时支持：
  - push：外部可选 `submit(ChangeSet)`（有事件就喂）
  - pull：引擎 `tick()` 自驱动（预算化扫描）
- 主动扫描包含 **两个策略**（可同时启用）：
  - `LoadedReconcileScanStrategy`：仅扫描已加载 section（对账/巡检）
  - `RequestingPrefetchScanStrategy`：对已知 `INCOMPLETE` 实例发出缺失 section 请求（不加载）
- 实体变化默认走 **兴趣集（Interest Set）轮询**（不全世界扫）：
  - `InterestEntityPollStrategy`：只轮询引擎关注的 sections/实体。

### 1.6 实例卸载策略
- v1 选 **Evict**：anchor 所在区块/section 卸载或 probe 失败 → 直接移除实例（诊断/context/export 失效）；下次加载重新发现再重建。

## 2. 总体架构（组件视角）

```
                (optional) submit(ChangeSet)
                           │
                           ▼
┌───────────────┐   ┌───────────────┐     ┌────────────────┐
│ Driver(s)     │──▶│ Dirty Sections │───▶ │ AnchorIndex    │
│ - LoadedOnly  │   └───────────────┘     │ + AnchorFinder │
│ - Requesting  │           │              └───────┬────────┘
│ - EntityPoll  │           ▼                      │
└───────┬───────┘   ┌───────────────┐             ▼
        │           │ InstanceSet    │      ┌───────────────┐
        │           │ (affected)     │─────▶│ PipelineRunner │
        │           └───────────────┘      │ Bind/Check/... │
        │                    │              └───────┬────────┘
        │                    ▼                      │
        │           ┌────────────────┐              ▼
        └──────────▶│ RequestLedger  │◀──────┌───────────────┐
                    └────────────────┘       │ DataRequests   │
                                              └───────────────┘

辅助：
- PartitionCache：按 SectionKey 缓存/索引世界数据（只读视图），缺失可被请求
- InstanceStore：实例状态、diagnostics、runtime context、export view、interest/watch
- DependencyGraph：跨实例依赖边（只读），循环检测
```

## 3. Engine 运行时主循环（tick）

### 3.1 输入合并（每 tick）
1) 消费外部 push：`submit(ChangeSet)` 队列（若有）。
2) 运行 driver（预算化）：
   - `LoadedReconcileScanStrategy`：生成 `dirtySections`（仅已加载）
   - `InterestEntityPollStrategy`：在兴趣集内检测实体变化 → `dirtySections`
   - `RequestingPrefetchScanStrategy`：对 `INCOMPLETE` 实例生成 `DataRequests`
3) 合并 dirty：
   - `dirtySections` 并集
   - 可选 `dirtyAnchors`（更直接的 anchor hint）

### 3.2 发现/更新实例集合
- 根据 `dirtySections`：
  - 更新 `AnchorIndex`（对这些 section 做 `AnchorFinder.discover()`）
  - 对已知实例做 `probe(anchorPos)`（轻量验证仍有效）
- 形成 “受影响实例集合”：
  - 新发现 → 创建实例（状态从 `INCOMPLETE` 起步也可）
  - probe 失败/卸载 → Evict

### 3.3 跑管线（仅对受影响实例）
对每个实例按固定阶段执行（各阶段可产出请求/诊断/interest 更新）：
1) `Bind/Scan`：围绕 anchor 绑定部件集合、建立索引视图
2) `Check*`：多个 checker；可输出 `ERROR` 或 `INCOMPLETE+requests`
3) `Compile`：合法且数据完整时产出 `RuntimeContext`
4) `DeclareDeps`：声明只读依赖边（实例→实例）
5) `Link`：拓扑序读取依赖 context；缺依赖/缺数据 → `INCOMPLETE+requests`；循环 → 诊断
6) `Export`：产出调试/可视化/数据生产视图（数据访问，不做 UI）

### 3.4 输出
`EngineFrame` 应包含：
- 实例状态变更（status、diagnostics、context、export、interest 的 delta）
- 新增 `DataRequests<SectionKey>`（经 ledger 去重/节流）

## 4. ChangeSet（最小字段）

> 目标：让“有事件时更精确、无事件时也能只靠 dirtySections 运行”。

建议 v1 只保留这些字段（可扩展但不强依赖）：
- `dirtySections: Set<SectionKey>`（核心字段，必备）
- `dirtyAnchors: Set<BlockPos>`（可选 hint：某些 controller 已知变化）
- `touchedEntities: Set<UUID>`（可选 hint：某些关键实体变化）
- `cause: enum`（可选：EVENT/SCAN/RELOAD/ADMIN... 仅用于指标/调试）

说明：
- 外部事件流若只给 `BlockPos`：引擎可自行映射到 `SectionKey` 再合并。
- driver 扫描可只输出 `dirtySections`，无需提供 touched 细节。

## 5. DataRequest（Section 级请求）

字段建议：
- `SectionKey key`
- `RequestReason reason`（例如：BIND_NEEDS_NEIGHBOR / CHECK_NEEDS_CONTEXT / LINK_NEEDS_DEP）
- `Priority priority`
- `Duration ttl`
- `String consumerId`（哪个 schema/pass/checker 发起；用于调试与节流）

引擎应维护 `RequestLedger`：
- 去重（同 key+reason+consumer 在 ttl 内不重复发）
- 节流（每 tick 最大 request 数；按 priority 选择）

## 6. 诊断（Diagnostic）与“非法结构解释”

`Diagnostic` 必须结构化，建议最小字段：
- `String code`（稳定错误码）
- `Severity severity`（INFO/WARN/ERROR）
- `String summary`（一行摘要）
- `Evidence evidence`（`BlockPos`/`SectionKey`/role/实体 UUID 等）
- `Trace trace`（可选因果树：规则→子谓语→证据；支撑“完整非法分析”）
- `List<FixHint> fixHints`（可选：用于“自动构建提示”的数据，不负责执行）

状态三态：
- `OK`
- `ERROR`（诊断中至少一个 ERROR）
- `INCOMPLETE`（缺数据/缺依赖；必须同时携带 `DataRequests` 与说明性诊断）

## 7. Pipeline API（接口面：v1 设计）

> 下面是“方法级”接口轮廓（伪 Java）。实现细节与线程模型留给后续。

### 7.1 Engine
- `ArchitectEngine`
  - `void register(ArchitectSchema schema)`
  - `void submit(ChangeSet changeSet)`（可选）
  - `EngineFrame tick(TickContext ctx)`
  - `Optional<InstanceView> instance(ResourceLocation schemaId, BlockPos anchorPos)`

- `EngineFrame`
  - `List<InstanceDelta> deltas()`
  - `List<DataRequest> newRequests()`

### 7.2 Schema（单结构类型）
- `ArchitectSchema`
  - `ResourceLocation id()`
  - `AnchorFinder anchorFinder()`
  - `Binder binder()`
  - `List<Checker> checkers()`
  - `Compiler compiler()`
  - `DependencyDeclarer dependencyDeclarer()`
  - `Linker linker()`
  - `Exporter exporter()`

### 7.3 Anchor（加载范围内发现/探测）
- `AnchorFinder`
  - `Stream<BlockPos> discover(DiscoverContext ctx)`（仅已加载范围内）
  - `ProbeResult probe(BlockPos anchorPos, ProbeContext ctx)`

### 7.4 世界/分块访问（只读视图）
- `PartitionAccess`
  - `LoadState loadState(SectionKey key)`
  - `SectionView section(SectionKey key)`（若 missing，返回 MissingView 或受控失败供上层转 INCOMPLETE）

- `SectionView`
  - `SectionKey key()`
  - `LoadState loadState()`
  - `BlockStateView blockState(long blockPos)`
  - `Optional<BlockEntityView> blockEntity(long blockPos)`
  - `Iterable<EntityRef> entities()`（实现可用索引加速）

### 7.5 Pass 上下文（统一能力）
- `PassContext`
  - `ResourceLocation schemaId()`
  - `BlockPos anchorPos()`
  - `PartitionAccess world()`
  - `BindingsView bindings()`（Bind 后可用）
  - `InstanceDirectory instances()`（跨实例只读访问）
  - `Budget budget()`
  - `void request(SectionKey key, RequestReason reason, Priority priority, Duration ttl, String consumerId)`
  - `void diag(Diagnostic d)`
  - `void interest(InterestUpdate update)`

### 7.6 Bind/Check/Compile/Deps/Link/Export
- `Binder`
  - `BindReport bind(BindContext ctx)`
- `Checker`
  - `CheckReport check(CheckContext ctx)`
- `Compiler`
  - `CompileReport compile(CompileContext ctx)`
- `DependencyDeclarer`
  - `DepReport declare(DepContext ctx)`（只声明边）
- `Linker`
  - `LinkReport link(LinkContext ctx)`（拓扑序读取依赖 context）
- `Exporter`
  - `ExportView export(ExportContext ctx)`（数据访问视图）

报告对象统一包含：
- `InstanceStatus status()`（OK/ERROR/INCOMPLETE）
- `List<Diagnostic> diagnostics()`
- `List<DataRequest> requests()`
- `InterestUpdate interestUpdate()`

### 7.7 RuntimeContext（编译产物）
- `RuntimeContext`
  - `Optional<T> get(ContextKey<T> key)`
  - `Set<ContextKey<?>> keys()`

### 7.8 跨实例依赖与目录
- `InstanceDirectory`
  - `Optional<RuntimeContextView> context(ResourceLocation schemaId, BlockPos anchorPos)`
  - `Optional<InstanceStatus> status(ResourceLocation schemaId, BlockPos anchorPos)`

循环依赖：
- 由引擎维护 `DependencyGraph`；
- `Link` 阶段在拓扑序上执行；若存在环，产出诊断并将环上实例标为 `ERROR` 或 `INCOMPLETE`（策略可配置）。

## 8. Interest / Watch（无事件流增量的关键）

`InterestUpdate`（由各 pass 产出，engine 汇总）建议包含：
- `sectionsAdd / sectionsRemove: Set<SectionKey>`
- `watchBlockPosAdd/Remove: Set<BlockPos>`（少量关键点）
- `watchEntitiesAdd/Remove: Set<UUID>`
- `ttl`（防止兴趣集无限膨胀；过期后需要重新声明）

引擎暴露一个 `EngineInterestView`（可选，仅调试/数据生产）：
- 每个实例当前兴趣 sections 与 watch 集合摘要。

## 9. Drivers（无事件也能跑）

- `DriverStrategy`
  - `DriverOutput run(DriverContext ctx)`

`DriverOutput` 最小字段：
- `dirtySections: Set<SectionKey>`
- `dirtyAnchors: Set<BlockPos>`（可选）
- `requests: List<DataRequest>`（仅 Requesting 策略会用）

v1 默认组合：
- `LoadedReconcileScanStrategy`：round-robin 扫已加载 sections → dirty
- `InterestEntityPollStrategy`：只在兴趣 sections 内轮询实体 → dirty
- `RequestingPrefetchScanStrategy`：对 `INCOMPLETE` 实例缺失 sections 发 request（节流/ttl）

当前仓库的 common 参考实现（名称可能会调整，但语义对齐）：
- `LoadedReconcileScanDriver`
- `InterestEntityPollDriver`
- `RequestingPrefetchScanDriver`

## 10. Builder DSL（v1 必备）

目标：用链式 DSL 构建一个 `ArchitectSchema`（无需数据驱动）。

示意（接口形态，不代表最终命名）。v1 推荐两种用法：
- **直接注入实现**：`bind(b -> b.binder(new MyBinder()))`
- **Plan DSL + 默认 Binder**：`bind(b -> b.plan(p -> ...))`（默认实现：`PlanBinder`）

```java
ArchitectSchema schema = ArchitectSchemaDsl.schema(id("mymod:crusher"))
  .anchor(a -> a.finder(new MyAnchorFinder()))
  .bind(b -> b.plan(p -> p
    .role("controller").fromAnchorBlock()
    .role("casing").scanBoxBlocks(-2,-1,-2, 2,1,2).matchBlockTag(id("mymod:casing"))
    .role("ports").scanEntities(6).matchEntityType(id("mymod:port"))
  ))
  .check("shape", c -> c.checker(new RequireCountChecker("casing", 40)))
  .check("ports", c -> c.checker(new RequireAtLeastChecker("ports", 2)))
  .compile(cp -> cp.compiler(new MyCompiler()))
  .depends(d -> d.declarer(new MyDependencyDeclarer()).readOnly().noCycles())
  .link(l -> l.linker(new TopoReadOnlyLinker()))
  .export(e -> e.exporter(new DebugOverlayExporter()))
  .build();
```

约束建议（保证可维护/可优化）：
- 所有 Checker/Compiler/Linker/Exporter **纯函数化**（不直接改世界），输出全部经 report/ctx sink 返回。
- DSL 允许注入自定义实现（不限制表达力）。
- DSL 支持为每个阶段配置预算（扩散半径、最大 sections/实体数、最大耗时等）。

### 10.1 matcher 扩展（用于 Bind 选取部件）
`BindingPlan` 的每个 `BindingOp` 都带一个 `MatchSpec`（默认 `any`），用于表达：
- `any` / `id` / `tag`
- 组合：`all(children)` / `any_of(children)` / `not(child)`

注意：tag 判断依赖于 `BlockStateView/EntityView/BlockEntityView` 的 `isInTag(tagId)` 能力（dummy/common 可用映射模拟；MC 默认实现应接入真实 tag 系统）。

## 11. dummy/common（跑通全流程的最小实现要求）

目的：不依赖真实 MC world，也能验证：
- 分块 dirty → 实例影响 → pipeline 输出
- INCOMPLETE+REQUEST 的收敛行为
- 跨实例依赖与循环诊断
- interest/watch 驱动的实体增量

建议包含（实现后续再做）：
- `InMemorySectionWorld`：`Map<SectionKey, SectionData>`（支持 LOADED/MISSING）
- `DummyPartitionAccess`：从内存 world 提供 `SectionView`
- `DummyDriver`：模拟 loaded-only 扫描与 requesting 请求
- `ExampleSchema`：最小 binder/checker/compiler/linker/exporter

当前仓库已落地（供参考/跑通）：
- `DummyArchitectEngine`（pipeline + evict + probe + dep-cycle 诊断）
- `InMemoryPartitionAccess`（LOADED/MISSING section + block/entity 只读视图）
- `PlanBinder`（BindingPlan + 分块 footprint → BindReport + InterestUpdate + DataRequest）

---

## 12. 后续演进（v2+）
- 数据驱动（datapack JSON + reload + datagen）把 DSL 的 schema 序列化/反序列化打通。
- 更细粒度的请求聚合与 host ticket 策略（ChunkPos 聚合、玩家附近优先等）。
- 并发/异步：snapshot 化 world 读取与纯计算 pass 解耦（实现细节）。

### 12.1 v2：Codec + Reload（骨架）
v2 的目标是把 “schema/spec = 纯数据” 真的落地到 MC 的 `Codec`/JSON 生态中：
- `ArchitectSchemaSpec.CODEC` / `ComponentSpec.CODEC`：JSON → spec
- `BindingPlan.CODEC` / `BindingOp.CODEC` / `MatchSpec.CODEC` / `ScanSpec.CODEC`：用于 `PlanBinder` 这类“可数据驱动”的 binder 配置
- `SchemaTypeRegistry`：把 spec 里的 `type` → 运行时实现（AnchorFinder/Binder/Checker/Compiler/...）
- `ArchitectSchemaLoader`：spec + registry → `ArchitectSchema`
- `ArchitectSchemaManager`：Reload 时解码 + build；保留 `problems()` 供日志/调试
- `ArchitectSchemaReloadListener`：挂载到 datapack reload（默认路径 `architect_cumulus/schemas`）

仍保持核心原则：
- 不强制加载区块：缺数据一律走 `INCOMPLETE + DataRequest`（宿主决定是否加载）
- engine 与维度/Level 解耦：每个 Level 自己持有 engine；schema manager 只是“数据源”
