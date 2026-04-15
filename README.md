# ArchitectCumulus

一个面向 Minecraft（NeoForge）的多方块结构检查 / 编译库的 **API 与流程**实现，目标是把：

- “结构由哪些部件组成？”（绑定/扫描）
- “结构是否合法？”（检查/诊断）
- “合法后如何转换成机器运行时上下文？”（编译/链接）
- “缺数据怎么办？”（INCOMPLETE + DataRequest）
- “没有事件流怎么收敛？”（drivers 主动扫描 + budget 预算）

统一成一套可扩展、可数据驱动（v2 骨架）的抽象。

> 重要：本仓库当前更偏“API/参考实现”。`common/dummy` 用于演示与测试，不是生产级实现。

## 核心概念

- `ArchitectSchema`：一个结构类型（“一种机器”）的流水线定义（anchor/bind/check/compile/dep/link/export）。
- `ArchitectEngine`：每 tick 收敛的引擎；通常 **每个 Level/维度一份**（API 本身不携带维度）。
- `InstanceKey`：实例键 `(schemaId, anchorPos)`（v1 anchor 默认 `BlockPos`）。
- `PartitionAccess` / `SectionKey`：世界只读访问抽象，按 MC section（16×16×16）分块。
- `DataRequest`：缺数据请求（建议）；引擎不强制加载区块，只把请求返回给宿主系统。
- `InterestView/InterestUpdate`：兴趣集，用于把主动轮询范围收敛到与实例相关的 sections/坐标/实体。
- `DriverStrategy`：主动扫描策略（无事件流/事件不完整时的兜底与收敛）。

## tick 流程（高层）

1) 合并外部 `submit(ChangeSet)`（如果你有事件流）  
2) 执行 drivers（主动扫描/预取请求/实体轮询）  
3) `AnchorFinder.discover`：在 dirty sections 内发现新实例（只在已加载范围内）  
4) 定位受影响实例（dirtyAnchors / interestIndex / anchor section dirty）  
5) `AnchorFinder.probe`：探测既有实例是否仍存在（缺数据则 UNLOADED）  
6) 对受影响实例跑流水线：Bind → Check* → Compile → Deps → Link → Export  
7) 输出 `EngineFrame(deltas, newRequests)`  

## 快速开始（代码驱动 schema + dummy world/engine）

`common/dummy` 提供了一个可跑通的内存世界与引擎，用于演示 API：

- world：`dev.vfyjxf.cumulus.common.dummy.InMemoryPartitionAccess`
- engine：`dev.vfyjxf.cumulus.common.dummy.DummyArchitectEngine`
- binder：`dev.vfyjxf.cumulus.common.binder.PlanBinder`（执行 `BindingPlan`）

推荐直接看测试用例：`src/test/java/dev/vfyjxf/cumulus/common/dummy/DummyArchitectEngineTest.java`。

## 数据驱动 schema（v2 骨架）

本仓库提供了：

- `ArchitectSchemaSpec`（Codec 可解析 JSON）
- `SchemaTypeRegistry`（typeId -> component factory）
- `ArchitectSchemaManager`（JSON -> spec -> schema）
- `ArchitectSchemaReloadListener`（资源 reload 的接入示例）

common 内置了一组可直接跑通的 type（用于测试/示例）：

- `cumulus:match_block_anchor_finder`：按 `MatchSpec` 匹配方块作为 anchor
- `cumulus:plan_binder`：执行 `BindingPlan`（scan + match + role）
- `cumulus:role_count_checker`：检查 role 绑定数量范围
- `cumulus:bindings_compiler`：把 anchor/bindings 写入运行时上下文

> datapack 接入与 /reload 的完整落地不在 v1 范围内，但骨架已准备好。

## 主动扫描（drivers）

当你没有完整事件流时，建议组合使用：

- `LoadedReconcileScanDriver`：只扫描已加载 sections（低频全量对账）
- `RequestingPrefetchScanDriver`：对 INCOMPLETE 实例缺失 sections 产出 DataRequest（不加载）
- `InterestEntityPollDriver`：只轮询兴趣集声明的 sections，hash 变化则 dirty

## 性能与工程建议（要点）

- 把“扫描”集中在 binder，后续 pass 尽量只读 bindings。
- 所有扫描/遍历都应遵守 `Budget`（循环内 tryConsume），把重活分帧。
- 默认只在“已加载范围内”发现/探测实例；需要数据时用 `DataRequest` 表达。
- 宿主系统应对 DataRequest 做去重/节流/聚合（例如 section->chunk ticket）。
- 用 `InterestUpdate` 把实例真正依赖的 sections 声明出来，减少主动轮询成本。

## 运行测试（必须 testframework）

本项目的单测使用 `net.neoforged:testframework` 的 `EphemeralTestServerProvider` 启动临时服务端：

```bash
bash ./gradlew test
```

## 设计文档

更完整的 API/流程设计说明见：`docs/superpowers/specs/2026-04-15-architect-cumulus-api-design.md`。
