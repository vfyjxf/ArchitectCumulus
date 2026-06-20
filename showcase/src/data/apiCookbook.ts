// ============================================================
// API Cookbook: 18 个真实场景的 API 用法 + 逐步解释 + 运行时 trace
// ============================================================

export interface ApiCard {
  id: string;
  category: ApiCategory;
  api: string;
  title: string;
  scenario: string;
  difficulty: "basic" | "intermediate" | "advanced";
  accent: "brand" | "cyan" | "magenta" | "lime" | "amber" | "rose";
  icon: string;
  code: string;
  explanation: string[];
  trace: { step: string; internal: string }[];
  pitfalls: string[];
  related?: string[];
}

export type ApiCategory =
  | "Definition"
  | "Runtime"
  | "Substructure"
  | "Build"
  | "Editor"
  | "Minecraft"
  | "Datagen"
  | "Migration";

export const CATEGORY_LABEL: Record<ApiCategory, string> = {
  Definition: "Definition Authoring",
  Runtime: "Runtime Engine",
  Substructure: "Substructure Lifecycle",
  Build: "Build & Construction",
  Editor: "Editor & Ghost Preview",
  Minecraft: "Minecraft Integration",
  Datagen: "Datapack & Datagen",
  Migration: "Migration & Versioning",
};

export const CATEGORY_ACCENT: Record<ApiCategory, "brand" | "cyan" | "magenta" | "lime" | "amber" | "rose"> = {
  Definition: "brand",
  Runtime: "cyan",
  Substructure: "magenta",
  Build: "amber",
  Editor: "lime",
  Minecraft: "rose",
  Datagen: "cyan",
  Migration: "lime",
};

export const API_COOKBOOK: ApiCard[] = [
  // ============================================================
  // Definition Authoring
  // ============================================================
  {
    id: "custom-predicate",
    category: "Definition",
    api: "PredicateRegistry + PredicateMatcher",
    title: "注册自定义 predicate",
    scenario:
      "内置 predicate 不够用，比如要检查 'block 上方有特定 sky light'、'block 处于某个 biome'、'block 距离村庄 POI 不超过 N 格'。",
    difficulty: "intermediate",
    accent: "brand",
    icon: "Puzzle",
    code: `// 1. 实现 PredicateMatcher 接口
public final class SkyLightPredicate implements PredicateMatcher {
    public static final ResourceLocation ID =
        ResourceLocation.parse("cumulus:sky_light_min");

    private final int min;

    public SkyLightPredicate(int min) {
        this.min = min;
    }

    @Override
    public boolean match(TargetSnapshot snapshot, Map<String, String> props) {
        if (!(snapshot instanceof BlockSnapshot bs)) return false;
        // 通过 props 拿到阈值
        int threshold = Integer.parseInt(props.getOrDefault("min", "0"));
        return bs.skyLight() >= threshold;
    }

    @Override
    public Set<String> requiredProperties() {
        return Set.of("min");
    }
}

// 2. mod 启动时注册
@Mod(Cumulus.modId)
public final class Cumulus {
    public Cumulus(IEventBus bus, ModContainer c) {
        PredicateRegistry.register(
            SkyLightPredicate.ID,
            props -> new SkyLightPredicate(
                Integer.parseInt(props.get("min"))));
    }
}

// 3. 在 DSL 里使用
.point("sunlit_altar",
    RelativeBlockPos.of(0, 2, 0),
    Targets.block()
        .where(PredicateDescriptors.id("cumulus:sky_light_min")
            .withProperty("min", "12")))  // sky light ≥ 12
        .capture("altar"))`,
    explanation: [
      "PredicateMatcher 是单方法接口：接受 TargetSnapshot（封装 block state、block entity NBT、capability 等）+ properties map，返回 boolean。",
      "注册到 PredicateRegistry 时给一个 factory：properties map → matcher 实例。Cumulus 用 factory 而不是直接注册 matcher 实例，是因为 properties 可能不同（同一 predicate id 用不同阈值多处使用）。",
      "DSL 用 PredicateDescriptors.id(typeId).withProperty(key, value) 构造 descriptor。datagen 阶段只看 typeId + properties 拼写，不调用 matcher；运行时 check 才从 registry 取 factory 造 matcher。",
      "未注册的 typeId 在 check 时报 runtimeOnlyUnavailable（severity=WARN），结构进入 INVALID 但不会 crash。",
    ],
    trace: [
      { step: "mod 加载", internal: "PredicateRegistry.register(typeId, factory) → registry.put(typeId, factory)" },
      { step: "build()", internal: "PredicateDescriptor 储存 typeId + properties，不调用 factory" },
      { step: "datagen", internal: "RuntimeAssetExporter 输出 typeId + properties 到 JSON，无需 matcher 类" },
      { step: "reload", internal: "RuntimeAssetJsonLoader 解析 typeId + properties 回 descriptor" },
      { step: "check", internal: "StructureCheckExecutor 从 registry 取 factory，构造 matcher，调 match()" },
    ],
    pitfalls: [
      "matcher 不要持有 world 引用！match() 只能从 TargetSnapshot 读数据，否则会泄漏 chunk。",
      "requiredProperties 必须声明完整，否则玩家在 JSON 里漏写字段时 check 会 NPE 而不是给友好诊断。",
      "match() 必须线程安全：NeoForge 可能并行检查多个 instance。",
      "注册时机：必须在 mod 加载阶段（@Mod 构造器里），reload 阶段注册无效。",
    ],
    related: ["predicate-from-serialized", "custom-target"],
  },
  {
    id: "predicate-from-serialized",
    category: "Definition",
    api: "PredicateDescriptors.fromSerialized()",
    title: "从 NBT/JSON 反序列化 predicate",
    scenario:
      "玩家通过 datapack 提供一个 predicate 描述（比如 'item 在 tag X 里 且 数量 ≥ 5'），mod 需要把它变成 StructurePredicate。",
    difficulty: "intermediate",
    accent: "brand",
    icon: "FileJson",
    code: `// 玩家 JSON 里：
//   { "id": "cumulus:combined", "props": { "tag": "#cumulus:rune", "min": "5" } }

// mod 内部反序列化：
public static PredicateDescriptor fromSerialized(Tag tag) {
    var compound = (CompoundTag) tag;
    ResourceLocation typeId = ResourceLocation.parse(
        compound.getString("id"));
    Map<String, String> props = new HashMap<>();
    CompoundTag propsTag = compound.getCompound("props");
    for (var key : propsTag.getAllKeys()) {
        props.put(key, propsTag.getString(key));
    }
    return new PredicateDescriptor(typeId, Map.copyOf(props));
}

// 反过来序列化：
public Tag serialize(PredicateDescriptor d) {
    var compound = new CompoundTag();
    compound.putString("id", d.typeId().toString());
    var propsTag = new CompoundTag();
    d.properties().forEach(propsTag::putString);
    compound.put("props", propsTag);
    return compound;
}`,
    explanation: [
      "predicate 不能直接 Java 序列化（lambda/匿名类）。Cumulus 的方案：predicate 由 (typeId + properties) 完全描述，序列化就是写这两个字段。",
      "fromSerialized 是 modder 自己写的 helper（Cumulus 不强制 API 形态），按需从 Tag/JSON/字节流恢复 descriptor。",
      "运行时把 descriptor 交给 PredicateRegistry.get(typeId).apply(properties) 拿到真正可执行的 matcher。",
      "同一个 typeId 可以出现在多个 predicate descriptor 里（比如 sky_light_min=min:10 和 sky_light_min=min:15），共享 matcher 类但参数不同。",
    ],
    trace: [
      { step: "JSON 解析", internal: "RuntimeAssetJsonLoader 把 predicate 段读成 Map<String,Object>" },
      { step: "descriptor 构造", internal: "PredicateDescriptor(typeId, properties) — 此时还不可执行" },
      { step: "registry 查找", internal: "PredicateRegistry.get(typeId) 返回 factory" },
      { step: "matcher 构造", internal: "factory.apply(properties) 返回 PredicateMatcher 实例" },
      { step: "执行", internal: "matcher.match(snapshot, props) 返回 boolean" },
    ],
    pitfalls: [
      "properties 必须是 String map（不是 Object）— 兼容 JSON/NBT/命令行所有序列化。",
      "未注册 typeId 的 fallback：要么返回 false predicate（永远不匹配），要么给默认诊断。在注册时声明 fallback 行为。",
      "properties value 不能含特殊字符（避免 JSON 转义麻烦）；数字用 String.valueOf。",
    ],
    related: ["custom-predicate"],
  },
  {
    id: "capture-group",
    category: "Definition",
    api: "capture() + groups + ContextRuleDescriptor",
    title: "把多个 point 的捕获组成 context",
    scenario:
      "机器有 4 个 input bus，每个都暴露物品 handler。机器逻辑需要知道 4 个 bus 里都有哪些物品，按 bus 位置归类。",
    difficulty: "advanced",
    accent: "brand",
    icon: "Layers",
    code: `Structures.define(id("quad_input"))
    .point("input_n", at(-1, 1, 0), Targets.blockEntity()
        .where(PredicateDescriptors.id("cumulus:item_bus"))
        .capture("input_n"))
    .point("input_e", at(0, 1, 1), Targets.blockEntity()
        .where(PredicateDescriptors.id("cumulus:item_bus"))
        .capture("input_e"))
    .point("input_s", at(1, 1, 0), Targets.blockEntity()
        .where(PredicateDescriptors.id("cumulus:item_bus"))
        .capture("input_s"))
    .point("input_w", at(0, 1, -1), Targets.blockEntity()
        .where(PredicateDescriptors.id("cumulus:item_bus"))
        .capture("input_w"))
    // 聚合 + 派生：通用 context 操作管线在 check 之后顺序执行
    .contextPlan(plan -> plan
        // sum: 把每个 input 的 size 属性加起来
        .op(Operations.sum(
            TOTAL_ITEMS,
            p -> Set.of("input_n","input_e","input_s","input_w").contains(p.id().value()),
            p -> p.intProperty("size").orElse(0)))
        // compute: 读已聚合的 TOTAL_ITEMS，自定义逻辑选出主输入
        .op(Operations.compute(PRIMARY_INPUT, (matches, ctx) ->
            matches.all().stream()
                .filter(p -> Set.of("input_n","input_e","input_s","input_w").contains(p.id().value()))
                .max(java.util.Comparator.comparingInt(p -> p.intProperty("size").orElse(0)))
                .map(MatchedPoint::value).orElse("none"))))
    .build();

// 机器逻辑读取：
@Override
public void onDelta(InstanceDelta delta) {
    var ctx = delta.context();
    int total = ctx.get(TOTAL_ITEMS).orElse(0);
    String primary = ctx.get(PRIMARY_INPUT).orElse("none");
    log.info("total {} items, primary = {}", total, primary);
}`,
    explanation: [
      "每个 point 用 capture(name) 声明它捕获的上下文键名。check 通过后，捕获的 BlockState/NBT/capability 会写入 RuntimeStructureContext 对应的键。",
      ".contextPlan(plan -> ...) 声明一个通用操作管线：操作按声明顺序执行，每个操作通过 OperationContext 访问匹配点 (MatchResults) 和已计算的上下文值 (anyValue)。",
      "标准库操作 Operations.sum / compute / count / derive / interiorVolume 等通过 .op(...) 加入；操作结果用 typed ContextKey 写入 computed 层。",
      "ctx.get(TOTAL_ITEMS) 拿到 typed 派生值，编译期类型安全（无需强转）。context 是 revision 版本化的：每次 check 通过 revision+1。",
      "context 是 revision 版本化的：每次 check 通过 revision+1，机器逻辑用 revision 判断是否要重新处理。",
    ],
    trace: [
      { step: "check pass", internal: "每个 point 的 capture 写入 RuntimeStructureContext: input_n=BlockPos, input_e=BlockPos, ..." },
      { step: "expression 求值", internal: "ContextRuleDescriptor 求值 'input_n.size + ...' → 总数" },
      { step: "context revision++", internal: "RuntimeStructureContext revision 从 7 → 8" },
      { step: "delta 发布", internal: "InstanceDelta(changedKeys={input_*, total_items, primary_input})" },
      { step: "机器收到", internal: "machine.onDelta 被调用，ctx 拿到所有派生值" },
    ],
    pitfalls: [
      "capture name 不能跨 point 重复 → ContextRuleDescriptor.derive 会 fail；StructureDefinitionValidator 在 build() 就拒。",
      "表达式语法限制：只支持点路径 + 加减乘除 + min/max/sum。复杂逻辑自己在 onDelta 里做。",
      "context 里存 BlockPos 比存 BlockState 划算：snapshot 可能很大，pos 只是 3 个 int。",
      "revision 不连续递增也合法：跳号意味着机器逻辑要忽略中间状态。",
    ],
  },

  // ============================================================
  // Runtime Engine
  // ============================================================
  {
    id: "register-track",
    category: "Runtime",
    api: "RuntimeCheckEngine.register() + track()",
    title: "注册定义 + 跟踪实例",
    scenario:
      "mod 加载完所有 StructureDefinition 后，把它们喂给 RuntimeCheckEngine；玩家放好 controller 后，引擎开始跟踪那个 anchor。",
    difficulty: "basic",
    accent: "cyan",
    icon: "CircleDot",
    code: `// 1. 创建引擎（一般作为单例）
public static final RuntimeCheckEngine ENGINE = new RuntimeCheckEngine()
    .substructureSyncPolicy(SubstructureSyncPolicy.statusAndContext);

// 2. 注册所有定义（启动时一次性）
public void onCommonSetup(FMLCommonSetupEvent e) {
    ENGINE.register(ModStructures.MINI_FORGE);
    ENGINE.register(ModStructures.FACTORY);
    // ... 所有 StructureDefinition
}

// 3. 玩家放好 controller 时，调用 track
public void onControllerPlaced(Level level, BlockPos pos) {
    TargetSnapshotSource source = new MinecraftTargetSnapshotSource(level);
    ENGINE.track(ModStructures.MINI_FORGE.id(), pos, source);
    // 引擎会立即调度第一次 check
}

// 4. 玩家破坏 controller 时，untrack
public void onControllerRemoved(Level level, BlockPos pos) {
    ENGINE.untrack(pos);
}`,
    explanation: [
      "register 把 definition 编译成 StructureCheckPlan 缓存起来。多次 register 同一 id 不会重复编译（deduplicate）。",
      "track 把 anchor + source 注册到 scheduler。scheduler 把 footprint sections 加入反向索引（sectionKey → instanceKey），第一次 tick 会立即检查。",
      "source 是 TargetSnapshotSource 接口：engine 不直接访问 Level，通过 source 读 block/capability/entity 数据。",
      "untrack 移除 anchor + 取消所有 pending check + 释放 footprint sections 的反向索引。",
    ],
    trace: [
      { step: "register", internal: "compiler.compile(definition) → plan; planCache.put(id, plan)" },
      { step: "track", internal: "scheduler.registerAnchor(id, anchor, source); 计算footprint sections; sectionIndex.put(secKey, instanceKey)" },
      { step: "第一次 tick", internal: "scheduler 把 instance 加入 dirty queue; check 立即执行" },
      { step: "check 完成", internal: "InstanceDelta 发布给 listener; ctx revision=1" },
      { step: "untrack", internal: "scheduler.unregister; 移除 sectionIndex 项; pending check 取消" },
    ],
    pitfalls: [
      "register 必须在 track 之前；否则 track 会找不到 plan 报 INVALID_DEFINITION。",
      "source 必须是稳定的 Level 引用；玩家切维度时 source 也要换。",
      "untrack 不会自动触发 — 玩家破坏 controller 时必须自己调，否则引擎会一直跟踪一个幽灵 anchor。",
      "engine 是线程不安全的，所有调用必须在 server thread。",
    ],
    related: ["fast-tick", "consume-delta"],
  },
  {
    id: "fast-tick",
    category: "Runtime",
    api: "fastTick() vs tick()",
    title: "选择 fast 还是 diagnostic tick",
    scenario:
      "生产服务器每 tick 50ms 预算紧张。99% 时间结构是 valid 的，没必要每 tick 都构建完整 DiagnosticGraph。",
    difficulty: "basic",
    accent: "cyan",
    icon: "Zap",
    code: `// RuntimeCheckEngineTickDriver 默认行为：
@SubscribeEvent
public void onServerTick(ServerTickEvent.Post event) {
    if (playerRequestedDiagnostic) {
        // 玩家右键 controller 问 "why invalid"，走完整 path
        RuntimeFrameResult frame = ENGINE.tick(event.getServerTick(), BUDGET);
        showDiagnosticsToPlayer(frame.deltas());
        playerRequestedDiagnostic = false;
    } else {
        // 默认：仅状态遥测，零 DiagnosticGraph allocation
        RuntimeFrameResult fastFrame = ENGINE.fastTick(event.getServerTick(), BUDGET);
        // 处理状态变化（VALID/INVALID 转换）
        for (var delta : fastFrame.deltas()) {
            if (delta.statusChanged()) {
                onMachineStatusChange(delta);
            }
        }
    }
}

// 数据对比（同套测试集，1000 tracked instances）：
//   tick()     : ~5.2ms · 47KB allocation/frame · GC 压力显著
//   fastTick() : ~1.1ms · 0 allocation · 0 GC 压力`,
    explanation: [
      "fastTick 只返回 InstanceStatus + 评估计数，跳过 DiagnosticGraph 节点构造。",
      "tick() 完整构建 DiagnosticGraph：每个失败 point 生成 node + edge，每条 rule 包含子节点。",
      "默认 fastTick；玩家查询 'why' 时按需触发一次 tick()（一次性，下一帧回到 fast）。",
      "fast 和 diag 共享 PredicateMatcher / TargetSnapshotSource / check plan，差异只在结果对象。CI 强制要求两边状态判定一致。",
    ],
    trace: [
      { step: "fastTick entry", internal: "for each dirty instance: 调 plan.fastCheck(snapshot)" },
      { step: "短路评估", internal: "candidates 按顺序：第一个全 pass → return VALID，跳过剩余" },
      { step: "失败时", internal: "fastTick 只记录 candidate index + first fail index，不构造 graph" },
      { step: "tick entry", internal: "candidates 按顺序：每个失败 candidate 都构造 DiagnosticNode" },
      { step: "graph 投影", internal: "玩家拿到完整 DiagnosticGraph，可以 project 成 4 种格式" },
    ],
    pitfalls: [
      "不要在 fastTick 里读 DiagnosticGraph — 它是 null。要诊断必须 tick()。",
      "playerRequestedDiagnostic 标志必须在 tick 完成后立即清零，否则下一帧又走 diag path。",
      "fastTick 不能跳过 InstanceStatus 转换 — 即使没诊断，状态从 VALID→INVALID 也要通知机器逻辑。",
      "首次跟踪 instance 时建议用 tick()（拿到初始诊断作为 baseline）。",
    ],
    related: ["register-track", "consume-delta"],
  },
  {
    id: "consume-delta",
    category: "Runtime",
    api: "InstanceDelta + RuntimeStructureContext",
    title: "消费 InstanceDelta 驱动机器逻辑",
    scenario:
      "机器 BlockEntity 需要根据结构状态启停。VALID → 启动冶炼；INVALID → 停机 + 提示玩家；PENDING → 显示 loading。",
    difficulty: "intermediate",
    accent: "cyan",
    icon: "Activity",
    code: `// 注册 delta listener
ENGINE.addInstanceListener(delta -> {
    if (!(delta.context().value("controller") instanceof BlockPos ctrl)) return;
    BlockEntity be = level.getBlockEntity(ctrl);
    if (!(be instanceof ForgeBlockEntity forge)) return;
    forge.onStructureDelta(delta);
});

// BlockEntity 处理：
public class ForgeBlockEntity extends BlockEntity {
    private boolean running;
    private RuntimeStructureContext ctx;

    public void onStructureDelta(InstanceDelta delta) {
        this.ctx = delta.context();
        switch (delta.status()) {
            case VALID -> startSmelting();
            case INVALID -> stopMachine(delta.diagnostics());
            case PENDING -> showLoading();
        }
    }

    private void startSmelting() {
        if (running) {
            // 已经在跑，只更新 context（比如朝向变了）
            updateRecipeFromContext();
            return;
        }
        running = true;
        // v2 typed read: ctx.get(ContextKey<Direction>) → Optional<Direction>
        // 没有 String key、没有强转、编译器知道类型
        Direction facing = ctx.get(ModContextKeys.facing).orElse(Direction.NORTH);
        setInputSide(facing);
        markDirty();
    }

    private void stopMachine(DiagnosticGraph graph) {
        running = false;
        // 投影诊断给玩家
        for (var player : level.players()) {
            graph.projectLocalized(player::sendSystemMessage);
        }
        markDirty();
    }
}`,
    explanation: [
      "addInstanceListener 接受一个 callback，每次 check 完成后调用。listener 在 server thread 同步执行。",
      "delta.context() 返回当前的 RuntimeStructureContext（即使 status=INVALID 也可能有部分 context；status=PENDING 时 context 是上一次的）。",
      "delta.statusChanged() 是个便捷方法：仅当状态从上一次变化时返回 true。频繁的 ctx revision 变化但状态不变的 delta 可以忽略。",
      "DiagnosticGraph 仅在 status=INVALID 时非空；其他情况下 delta.diagnostics() 返回空图（不耗内存）。",
    ],
    trace: [
      { step: "check pass", internal: "StructureCheckExecutor.check 完成; revision+1" },
      { step: "delta 构造", internal: "new InstanceDelta(key, status, ctx, graph, changedKeys)" },
      { step: "frame 收集", internal: "RuntimeFrameResult.deltas.add(delta)" },
      { step: "listener 调用", internal: "for each listener: listener.accept(delta)" },
      { step: "机器更新", internal: "ForgeBlockEntity.onStructureDelta 处理转换" },
    ],
    pitfalls: [
      "listener 不要做重活！阻塞 server tick。重活排队到下一 tick。",
      "delta.context() 是引用共享的，机器逻辑不能 mutate。要修改先 copy。",
      "PENDING 状态下 ctx 是 stale 的 — 不要基于它启动冶炼，要等下次 VALID。",
      "player.sendSystemMessage 不要每个 tick 都发；用 revision 去重。",
    ],
    related: ["register-track", "fast-tick", "data-request-ledger"],
  },
  {
    id: "data-request-ledger",
    category: "Runtime",
    api: "RequestLedger + DataRequest",
    title: "未加载区块 → 请求重试",
    scenario:
      "结构有 remote_sensor point 在 50 格外的区块，那个区块未加载。引擎不强制载入，而是发 DataRequest，等区块加载后自动重检。",
    difficulty: "advanced",
    accent: "cyan",
    icon: "Hourglass",
    code: `// TargetSnapshotSource 可以返回 Optional.empty() 表示"数据未到"
public class MinecraftTargetSnapshotSource implements TargetSnapshotSource {
    @Override
    public Optional<TargetSnapshot> read(TargetReadRequest req) {
        if (!level.hasChunkAt(req.pos())) {
            return Optional.empty();  // 数据未加载
        }
        return Optional.of(readNow(req));
    }
}

// engine 自动处理：
//   1. required point 数据未到 → 发 DataRequest(sectionKey)
//   2. status 变 PENDING（不是 INVALID）
//   3. RequestLedger 记录 sectionKey → instanceKey
//   4. NeoForge ChunkEvent.LOAD 触发时，ledger 回调
//   5. ledger.markDirty(sectionKey) 让 engine 下一 tick 重检

@SubscribeEvent
public void onChunkLoad(ChunkEvent.Load event) {
    ChunkPos cp = event.getChunk().getPos();
    // 把整个 chunk 的 sections 都标记为可重检
    for (int y = 0; y < 16; y++) {
        SectionKey key = new SectionKey(cp.x, y, cp.z);
        ENGINE.onSectionLoaded(key);
    }
}

// 机器逻辑处理 PENDING：
public void onStructureDelta(InstanceDelta delta) {
    if (delta.status() == InstanceStatus.PENDING) {
        // 不停机，仅显示 loading
        showLoadingOverlay();
    }
}`,
    explanation: [
      "TargetSnapshotSource 返回 Optional.empty() 时，executor 把必需 point 标为 unloadedArea（severity=INFO），整体 status=PENDING。",
      "PENDING 不算 INVALID，机器不停机。机器逻辑要显式处理 PENDING（一般是显示 'loading...'）。",
      "RequestLedger 监听 ChunkEvent.LOAD，区块到时自动 markDirty 触发重检。",
      "如果一直未加载（玩家远离），PENDING 持续，不会消耗 CPU 重试。",
    ],
    trace: [
      { step: "check 尝试", internal: "snapshot.read(remote_pos) 返回 Optional.empty()" },
      { step: "诊断记录", internal: "DiagnosticNode(unloadedArea, INFO, 'chunk not loaded')" },
      { step: "status = PENDING", internal: "InstanceStatus 不是 INVALID" },
      { step: "request 入账", internal: "RequestLedger.add(sectionKey, instanceKey)" },
      { step: "chunk 加载", internal: "ChunkEvent.LOAD → ledger.onSectionLoaded(secKey)" },
      { step: "重检触发", internal: "engine.markDirty(secKey) → 下一 tick 自动 check" },
    ],
    pitfalls: [
      "Optional.empty() 必须是 '数据未到'，不是 'block 是 air'。空气要返回 Optional.of(AIR_SNAPSHOT)。",
      "PENDING 不能让机器停机 — 这是 'unknown' 而不是 'invalid'。",
      "RequestLedger 不能无限增长 — 要在 untrack 时清掉对应 entries。",
      "ChunkEvent 监听必须在 server thread；客户端事件不能用来 markDirty。",
    ],
    related: ["consume-delta", "register-track"],
  },

  // ============================================================
  // Substructure Lifecycle
  // ============================================================
  {
    id: "lifecycle-listener",
    category: "Substructure",
    api: "SubstructureLifecycleListener",
    title: "监听子结构 occurrence 增删 + 状态变化",
    scenario:
      "工厂有 left_arm / right_arm 子结构。机器要分别知道'左臂上线了'、'右臂宕机了'、'工具升级了'，而不是只看整体 status。",
    difficulty: "intermediate",
    accent: "magenta",
    icon: "Webhook",
    code: `ENGINE.addSubstructureLifecycleListener(event -> {
    // event 包含:
    //   transition    = ADDED | STATUS_CHANGED | REMOVED
    //   occurrenceId  = "left_arm#0"
    //   childPath     = "factory > left_arm"
    //   previousStatus= VALID / null (if ADDED)
    //   currentStatus = INVALID
    //   parentAnchor  = BlockPos(10,64,10)
    //   offsetInParent= RelativeBlockPos(-3, 0, 0)

    switch (event.transition()) {
        case ADDED -> log.info("子结构 {} 上线 @ {}", event.occurrenceId(), event.parentAnchor());
        case STATUS_CHANGED -> {
            if (event.currentStatus() == InstanceStatus.INVALID) {
                notifyPlayer("子结构 " + event.occurrenceId() + " 故障");
            }
        }
        case REMOVED -> log.info("子结构 {} 下线", event.occurrenceId());
    }
});

// 场景演示：
//   1. 玩家放好 left_arm → ADDED 事件
//   2. 玩家破坏 left_arm 上的工具 → STATUS_CHANGED (VALID → INVALID)
//   3. 玩家补回工具 → STATUS_CHANGED (INVALID → VALID)
//   4. 玩家破坏整个 left_arm → REMOVED 事件`,
    explanation: [
      "每个 child occurrence 拿到独立 occurrenceId（格式：${childDefName}#${index}），即使同一个 child def 被父引用两次也有不同 id。",
      "transition 是 ADDED/STATUS_CHANGED/REMOVED 三种，覆盖完整生命周期。",
      "listener 在 server thread 同步调用，建议只做轻量记录；重活排队。",
      "previousStatus 在 ADDED 时是 null，其他情况下是上一次的状态。",
    ],
    trace: [
      { step: "check pass", internal: "SubstructureRuntimeComposer 复合 child definition" },
      { step: "occurrence 检测", internal: "对比上次快照：新 occurrence? ADDED; status 变了? STATUS_CHANGED" },
      { step: "event 构造", internal: "new SubstructureLifecycleEvent(...)" },
      { step: "listener 调用", internal: "for each listener: accept(event)" },
      { step: "状态镜像", internal: "ctx.set('substructure.<occId>.status', currentStatus)" },
    ],
    pitfalls: [
      "occurrenceId 是字符串，不要假设格式。用 childPath 分割解析更稳。",
      "STATUS_CHANGED 触发频繁（每次 child 内部某个 point 变化都触发）。用 revision 去重。",
      "REMOVED 事件可能在 ctx 还没更新前触发；不要在 REMOVED 时读 ctx 的 occurrence 状态。",
      "listener 异常会被吞掉（不阻断其他 listener）— 但要日志记录。",
    ],
    related: ["status-context-mirror", "ownership-conflict"],
  },
  {
    id: "status-context-mirror",
    category: "Substructure",
    api: "SubstructureSyncPolicy.statusAndContext",
    title: "把子结构上下文镜像回父",
    scenario:
      "工厂要读 left_arm 内部工具的 spell_book NBT，决定工厂做什么配方。但工厂自己 check 不到 tool（tool 是 arm 的子结构）。",
    difficulty: "advanced",
    accent: "magenta",
    icon: "Copy",
    code: `// 启用 statusAndContext 策略
RuntimeCheckEngine engine = new RuntimeCheckEngine()
    .substructureSyncPolicy(SubstructureSyncPolicy.statusAndContext);

// 父上下文会自动收到子结构镜像键：
//   substructure.<occurrenceId>.status           = VALID / INVALID
//   substructure.<occurrenceId>.anchor_offset    = "-3,0,0"
//   substructure.<occurrenceId>.context.<childCapture> = ...

// 比如工厂的 ctx:
//   substructure.left_arm#0.status           = VALID
//   substructure.left_arm#0.context.tool.spell_book = "ars_nouveau:flight"
//   substructure.right_arm#0.status          = INVALID
//   substructure.right_arm#0.context.tool.spell_book = null

// 工厂逻辑读取：
@Override
public void onDelta(InstanceDelta delta) {
    var ctx = delta.context();
    String leftSpell = (String) ctx.value("substructure.left_arm#0.context.tool.spell_book");
    String rightSpell = (String) ctx.value("substructure.right_arm#0.context.tool.spell_book");

    if (leftSpell != null && rightSpell != null) {
        // 双臂工具都有，启动双工具配方
        runDualToolRecipe(leftSpell, rightSpell);
    }
}`,
    explanation: [
      "SubstructureSyncPolicy 有三档：NONE（只看父 status）、statusOnly（额外镜像 child status）、statusAndContext（再额外镜像 child context）。",
      "镜像 key 命名规范：substructure.<occId>.context.<captureName>，嵌套 capture 用点连。",
      "statusAndContext 会显著增大父 ctx 体积（每个 child occurrence 增加几十个 key）。生产场景按需选择。",
      "镜像值是引用共享的（不是 copy）；机器逻辑不能 mutate，要修改先 deep copy。",
    ],
    trace: [
      { step: "child check pass", internal: "child RuntimeStructureContext revision++" },
      { step: "镜像策略应用", internal: "statusAndContext：把 child ctx 拷贝到 parent 的 substructure.<occId>.context.* 键" },
      { step: "parent ctx 更新", internal: "parent revision++ (因为镜像内容变了)" },
      { step: "delta 发布", internal: "changedKeys 包含所有 substructure.* 变化" },
      { step: "机器读取", internal: "ctx.value('substructure.left_arm#0.context.tool.spell_book')" },
    ],
    pitfalls: [
      "statusAndContext 性能成本：每次 child check 都会 rebuild parent ctx。child 多时慎用。",
      "镜像键名很长（'substructure.left_arm#0.context.tool.spell_book'），频繁读取要 cache。",
      "嵌套深度大时 ctx 体积膨胀快。一般 2-3 层是上限。",
      "如果 child occurrence 被移除，对应镜像键会被删除（不是 null）。ctx.value 返回 null。",
    ],
    related: ["lifecycle-listener", "ownership-conflict"],
  },
  {
    id: "ownership-conflict",
    category: "Substructure",
    api: "SubstructureClaimState + ClaimConflict",
    title: "两个父结构抢同一个 child occurrence",
    scenario:
      "玩家把 factory_a 和 factory_b 都建在同一个 arm 旁边。两个 factory 都声称这个 arm 是自己的子结构。必须仲裁。",
    difficulty: "advanced",
    accent: "magenta",
    icon: "AlertOctagon",
    code: `// 引擎自动检测：
//   factory_a @ (0,64,0) claims arm @ (-3,64,0)
//   factory_b @ (-6,64,0) claims arm @ (-3,64,0)  ← 冲突！

// 第一个 claim 成功（factory_a），第二个会被拒绝
// factory_b 的 check 报 sharedPartConflict 诊断
// factory_b 整体 status = INVALID

// 机器逻辑处理：
ENGINE.addInstanceListener(delta -> {
    if (delta.status() == InstanceStatus.INVALID) {
        var conflictNodes = delta.diagnostics().nodes().stream()
            .filter(n -> n.code().equals("sharedPartConflict"))
            .toList();
        if (!conflictNodes.isEmpty()) {
            // 通知玩家：哪个 occurrence 被谁抢了
            for (var node : conflictNodes) {
                var owner = node.evidence().ownerAnchor();
                var occ = node.evidence().occurrenceId();
                player.sendSystemMessage(Component.literal(
                    "子结构 " + occ + " 已被 " + owner + " 占用"));
            }
        }
    }
});

// 玩家解决：破坏 factory_a 或 factory_b，冲突解除
// 引擎自动重新 check 剩下的 factory，arm 被释放`,
    explanation: [
      "SubstructureClaimState 维护 occurrenceId → owningParentAnchor 的 map。第一个 claim 写入 map，第二个看到 map 已有就拒绝。",
      "拒绝的 child delta 被标记 invalid，整体 parent check 也 fail。",
      "玩家破坏 owner parent 时，map 释放 occurrence，下一个 check 周期另一个 parent 自动获得 claim。",
      "claim 是按 occurrence 不是按 def — 同一个 def 可以同时被多个 parent 引用，只要 occurrence（不同 anchor）不冲突。",
    ],
    trace: [
      { step: "factory_a check", internal: "composer 生成 left_arm#0 occurrence; claimState.tryClaim(occ, factory_a) → OK" },
      { step: "factory_b check", internal: "composer 也生成 left_arm#0 occurrence; claimState.tryClaim(occ, factory_b) → FAIL" },
      { step: "诊断构造", internal: "DiagnosticNode(sharedPartConflict, ERROR, owner=factory_a)" },
      { step: "factory_b INVALID", internal: "child delta 被 invalidate; parent status 也 INVALID" },
      { step: "玩家解除", internal: "factory_a 被破坏 → untrack → claimState.release(occ)" },
      { step: "factory_b 重检", internal: "下一 tick factory_b 重新 check; 这次 tryClaim 成功" },
    ],
    pitfalls: [
      "claim 是 'first come first served'，不是 'biggest parent wins'。设计时考虑这个语义。",
      "玩家可能困惑：'我建的工厂为什么突然无效？' 要把诊断投影成可读的 'owned by X' 消息。",
      "性能：每次 check 都 tryClaim 是 O(1) hashmap 查询，但 untrack 时要清理 map entries。",
      "不要让玩家手动 'claim' — 引擎自动处理；玩家只需破坏/建造方块。",
    ],
    related: ["lifecycle-listener", "status-context-mirror"],
  },

  // ============================================================
  // Build & Construction
  // ============================================================
  {
    id: "build-plan-executor",
    category: "Build",
    api: "BuildPlanExecutor + FixHint",
    title: "自动构造：从 FixHint 到放方块",
    scenario:
      "玩家右键 controller 选 'auto-build'，引擎读 DiagnosticGraph 里的 FixHint，自动放缺失的方块。",
    difficulty: "intermediate",
    accent: "amber",
    icon: "Hammer",
    code: `// 1. 玩家请求 auto-build
public void onAutoBuildKeyPress(Player player, BlockPos controllerPos) {
    // 先 tick 拿诊断
    RuntimeFrameResult frame = ENGINE.tick(serverTick, BUDGET);
    var delta = frame.deltas().stream()
        .filter(d -> controllerPos.equals(d.context().value("controller")))
        .findFirst().orElse(null);
    if (delta == null || delta.status() == InstanceStatus.VALID) return;

    // 2. 收集所有 FixHint
    List<FixHint> hints = delta.diagnostics().allFixHints();

    // 3. 转成 BuildAction 列表
    List<BuildAction> actions = hints.stream()
        .map(FixHint::toBuildAction)
        .toList();

    // 4. 执行
    MinecraftBuildTarget target = new MinecraftBuildTarget((Level) player.level());
    BuildPlanExecutor executor = new BuildPlanExecutor(target);
    BuildPlanResult result = executor.execute(actions);

    // 5. 反馈
    player.sendSystemMessage(Component.literal(
        "已自动放置 " + result.succeeded() + " 个方块，"
        + result.skipped() + " 个被跳过"));
}

// FixHint 示例：
//   FixHint.placeBlock(at=(10,64,9), block="#cumulus:heat_casing")
//   FixHint.replaceBlock(at=(11,64,10), from="minecraft:stone", to="#cumulus:heat_casing")
//   FixHint.ensureCapability(at=(10,64,11), capability="cumulus:energy")`,
    explanation: [
      "FixHint 是 DiagnosticNode 的可选字段：诊断附带修复提示（place/replace/ensure 三种）。",
      "FixHint.toBuildAction() 转成 BuildAction（point/region/capability-backed/capability-mutation 四种）。",
      "BuildPlanExecutor 接受 BuildTarget 接口；MinecraftBuildTarget 是生产实现，InMemoryBuildTarget 用于测试。",
      "execute 是幂等的：已正确的方块不会重写；重复执行安全。",
    ],
    trace: [
      { step: "诊断收集", internal: "DiagnosticGraph.allFixHints() 递归遍历所有节点" },
      { step: "BuildAction 转换", internal: "FixHint.toBuildAction() 根据 hint 类型生成对应 action" },
      { step: "target 适配", internal: "MinecraftBuildTarget.resolveBlockId(hint.block()) → BlockState" },
      { step: "幂等检查", internal: "level.getBlockState(pos) 已经匹配？skip；否则 setBlock" },
      { step: "markDirty", internal: "BuildTarget 自动调 engine.markDirty(sectionKey)" },
      { step: "重检", internal: "下一 tick engine 重 check; 如果全部修复 status → VALID" },
    ],
    pitfalls: [
      "auto-build 不要给玩家免费方块 — FixHint 应该只提示位置，方块来源是玩家自己的背包。",
      "capability-backed 类型的 BuildAction（比如放一个能量接口方块）需要 mod 提供 CapabilityBuildWriter。",
      "BuildTarget 是接口，可以自定义（比如测试用 in-memory，或服务器远程 region 用 RPC）。",
      "execute 不抛异常，失败转成 BuildPlanResult.skipped + 诊断。",
    ],
    related: ["capability-build-writer"],
  },
  {
    id: "capability-build-writer",
    category: "Build",
    api: "MinecraftCapabilityBuildWriters",
    title: "自定义能量/物品/流体 ensure 逻辑",
    scenario:
      "auto-build 想给 machine 自动接入能量。但 '接入' 不只是放方块，可能要设 block state、写 NBT、调 capability API。",
    difficulty: "advanced",
    accent: "amber",
    icon: "PlugZap",
    code: `// 1. 实现 CapabilityBuildWriter（typed 能量版本）
public final class EnergyBuildWriter
        implements CapabilityBuildWriter<IEnergyStorage> {

    @Override
    public ResourceLocation capabilityId() {
        return ResourceLocation.parse("cumulus:energy");
    }

    @Override
    public BuildActionResult ensureCapability(
            Level level, BlockPos pos, Direction side, long amount) {
        BlockState state = level.getBlockState(pos);
        var be = level.getBlockEntity(pos);
        if (be == null) return BuildActionResult.unsupported("no BE");

        var cap = be.getCapability(Capabilities.EnergyStorage.BLOCK, side);
        if (cap.isEmpty()) return BuildActionResult.unsupported("no energy cap");

        IEnergyStorage storage = cap.resolve().orElse(null);
        if (storage == null) return BuildActionResult.failed("resolve failed");

        // 幂等：已经够了就不写
        if (storage.getEnergyStored() >= amount) {
            return BuildActionResult.alreadyOk();
        }

        // 写入能量（如果有 receiver 接口）
        if (storage.canReceive()) {
            storage.receiveEnergy((int) Math.min(amount, storage.getMaxEnergyStored()), false);
            return BuildActionResult.ok();
        }
        return BuildActionResult.unsupported("cannot receive");
    }
}

// 2. 注册 writer
MinecraftBuildTarget target = new MinecraftBuildTarget(level)
    .withCapabilityWriter(new EnergyBuildWriter())
    .withCapabilityWriter(new ItemBuildWriter())
    .withCapabilityWriter(new FluidBuildWriter());

// 3. BuildAction 路由：capability mutation 类型自动走对应 writer
BuildAction ensureEnergy = BuildAction.ensureCapability(
    pos, "cumulus:energy", 8192);
executor.execute(List.of(ensureEnergy));`,
    explanation: [
      "CapabilityBuildWriter 是 typed 接口：每个 writer 处理一种 capability（energy/item/fluid/自定义）。",
      "ensureCapability 是幂等操作：'至少有 N 单位'；已满足返回 alreadyOk，不写。",
      "MinecraftCapabilityBuildWriters 提供 3 个默认 writer（energy/item/fluid），modder 可以注册更多。",
      "未注册 writer 的 capability mutation 会被标 unsupported（诊断报），不会 crash。",
    ],
    trace: [
      { step: "BuildAction 路由", internal: "BuildPlanExecutor 看 action 类型 → 找对应 writer" },
      { step: "writer 查找", internal: "target.capabilityWriter(capId) 返回 EnergyBuildWriter" },
      { step: "BE 解析", internal: "level.getBlockEntity(pos).getCapability(EnergyStorage.BLOCK, side)" },
      { step: "幂等检查", internal: "storage.getEnergyStored() >= amount? alreadyOk" },
      { step: "execute", internal: "storage.receiveEnergy(amount); BE markDirty" },
      { step: "反馈", internal: "BuildActionResult.ok() / failed(reason) / unsupported(reason)" },
    ],
    pitfalls: [
      "writer 必须幂等 — 同一 action 执行多次结果相同。'累计写入' 是错的。",
      "ensureCapability 的 amount 是 long，但 IEnergyStorage 是 int；writer 自己负责截断。",
      "writer 不能抛异常 — 任何失败转 BuildActionResult.failed。executor 不 catch RuntimeException。",
      "side 必须明确；多方块机器的输入面由 controller.facing 决定。",
    ],
    related: ["build-plan-executor"],
  },

  // ============================================================
  // Editor & Ghost Preview
  // ============================================================
  {
    id: "editor-session",
    category: "Editor",
    api: "StructureEditorSession + StructureEditorController",
    title: "客户端编辑器：选点 + ghost 渲染",
    scenario:
      "玩家拿 'Structure Editor' 物品右键地面，进入编辑模式。屏幕上显示当前结构的 ghost preview，可以选 point / 切换 / 旋转。",
    difficulty: "intermediate",
    accent: "lime",
    icon: "MousePointerSquareDashed",
    code: `// 客户端按 P 键进入编辑模式
@SubscribeEvent
public void onKeyInput(KeyInputEvent event) {
    if (EDITOR_KEY.consumeClick()) {
        BlockPos anchor = mc.hitResult.getBlockPos();
        StructureDefinition def =pickStructureFromHotbar(mc.player);

        // 启动 editor session
        StructureRuntimePlan plan = StructureRuntimePlanCompiler.compile(def);
        StructureEditorSession session = StructureEditorSession.start(plan, anchor)
            .select("controller");

        // controller 接管
        StructureEditorController.open(session);
    }
}

// 在 controller 里：
public class StructureEditorController {
    private static StructureEditorController active;

    public static void open(StructureEditorSession session) {
        active = new StructureEditorController(session);
    }

    public void cycleSelection() {
        // 切换选中 point
        var all = session.points();
        int idx = all.indexOf(session.selectedPointId());
        String next = all.get((idx + 1) % all.size()).id();
        session = session.select(next);
    }

    public void applyTransform(TransformDescriptor t) {
        session = session.withTransform(t);
    }

    public GhostPreviewModel preview() {
        return GhostPreviewCompiler.compile(session);
    }
}

// 渲染 hook：
@SubscribeEvent
public void onRenderLevel(RenderLevelStageEvent event) {
    if (event.getStage() != STAGE_AFTER_TRANSLUCENT) return;
    var controller = StructureEditorController.active;
    if (controller == null) return;

    GhostPreviewModel preview = controller.preview();
    GhostPreviewRenderPlan plan = GhostPreviewRenderPlan.fromPreview(preview);
    GhostPreviewRenderer.render(plan, event.getPoseStack());
}`,
    explanation: [
      "StructureEditorSession 是不可变值对象：每次 select/withTransform 返回新 session，老 session 不变。",
      "controller 是有状态的，但 delegate 给 session。客户端按 P 键启动，按 ESC 关闭。",
      "GhostPreviewCompiler 把 session（plan + anchor + selection）转成 GhostPreviewModel（绝对坐标的方块列表）。",
      "GhostPreviewRenderPlan 把 model 转成 render commands（线框 box + 高亮），GhostPreviewRenderer 在 level render 阶段画。",
    ],
    trace: [
      { step: "key press", internal: "KeyInputEvent 触发; consumeClick 拿到事件" },
      { step: "session start", internal: "StructureEditorSession.start(plan, anchor) 创建初始 session（identity transform, no selection）" },
      { step: "select", internal: "session.select('controller') 返回新 session，selected='controller'" },
      { step: "render frame", internal: "RenderLevelStageEvent 触发; controller.preview() 编译 ghost" },
      { step: "compile", internal: "GhostPreviewCompiler 把 plan.footprint + anchor + transform 转 GhostPreviewElement 列表" },
      { step: "render", internal: "GhostPreviewRenderer 用 LevelRenderer 画 box（半透明 + 边框）" },
    ],
    pitfalls: [
      "session 不可变 — 不要 mutate。每次操作返回新 session 赋值给变量。",
      "client/server 分离：editor 完全在 client；服务器不知道玩家在看什么。需要 build 时再发包过去。",
      "render 必须在 AFTER_TRANSLUCENT 阶段，否则被世界几何遮挡。",
      "GhostPreviewRenderPlan 要 cache（编译慢），不要每帧重编。",
    ],
    related: ["ghost-preview-render-plan"],
  },
  {
    id: "ghost-preview-render-plan",
    category: "Editor",
    api: "GhostPreviewRenderPlan + RenderStyle",
    title: "自定义 ghost 颜色/材质",
    scenario:
      "controller 高亮金色，casing 用青色，missing 方块用红色闪烁。需要细粒度控制每个 point 的渲染风格。",
    difficulty: "intermediate",
    accent: "lime",
    icon: "Palette",
    code: `// 默认 render plan：
GhostPreviewRenderPlan plan = GhostPreviewRenderPlan.fromPreview(preview);

// 自定义：
GhostPreviewRenderPlan customPlan = GhostPreviewRenderPlan.builder(preview)
    .style("controller", RenderStyle.solid(0xFFFFD700).withOutline(0xFFFFFFFF))
    .style("casing", RenderStyle.translucent(0x4022D3EE).withOutline(0xFF22D3EE))
    .style("input_bus", RenderStyle.translucent(0x40FBBF24))
    .styleForMissingPoints(RenderStyle.blinking(0xFFFF0000, 500)) // 500ms blink
    .styleForRegionComponents(RenderStyle.wireframe(0xFFF0ABFC))
    .build();

// RenderStyle 选项：
//   solid(color)         实色填充
//   translucent(color)   半透明
//   wireframe(color)     仅线框
//   blinking(color, ms)  闪烁
//   textured(resourceId) 贴图
//   withOutline(color)   叠加边框
//   withEmissive(bool)   是否发光

GhostPreviewRenderer.render(customPlan, poseStack);

// 数据驱动：从 JSON 加载 style
// data/cumulus/ghost_styles/mini_forge.json
// { "styles": { "controller": { "type": "solid", "color": "FFD700" } } }
var loaded = GhostPreviewStyleLoader.load(resourceManager, def.id());
plan = GhostPreviewRenderPlan.builder(preview).applyStyles(loaded).build();`,
    explanation: [
      "RenderStyle 是不可变 record：solid/translucent/wireframe/blinking/textured 五种基础 + withOutline/withEmissive 装饰。",
      "GhostPreviewRenderPlan.builder 链式 API：先指定默认 style，再按 point id 覆盖。",
      "missingPoints 是 plan 自动识别的：当前世界没匹配上的 point。可以专门渲染提示。",
      "style 可以从 JSON 加载（玩家通过 datapack 改），支持热重载。",
    ],
    trace: [
      { step: "preview 编译", internal: "GhostPreviewCompiler 把 plan.footprint + transform 转 elements" },
      { step: "style 查找", internal: "for each element: style = styleMap.getOrDefault(element.pointId, defaultStyle)" },
      { step: "render command", internal: "RenderCommand(type=BOX, pos, size, color, outline, blinking)" },
      { step: "GPU upload", internal: "GhostPreviewRenderer 把 command buffer 上传 GPU" },
      { step: "frame draw", internal: "LevelRenderer 在 AFTER_TRANSLUCENT 画" },
    ],
    pitfalls: [
      "blinking 频率不要太快（< 200ms 会刺眼）。",
      "translucent alpha 不要太低（< 0x20 看不清）。",
      "textured style 要在 client 资源加载后才能用；过早调用会回退到 solid。",
      "每帧重建 plan 很贵；cache by (preview revision + style revision)。",
    ],
    related: ["editor-session"],
  },

  // ============================================================
  // Minecraft Integration
  // ============================================================
  {
    id: "mc-snapshot-source",
    category: "Minecraft",
    api: "MinecraftTargetSnapshotSource",
    title: "LevelAccessor → TargetSnapshotSource",
    scenario:
      "引擎需要读世界方块/BE/entity/fluid/capability，但不直接依赖 Minecraft API。需要一个适配器把 LevelAccessor 包装成 TargetSnapshotSource。",
    difficulty: "intermediate",
    accent: "rose",
    icon: "Boxes",
    code: `// 创建 source（每个 level 一个）
MinecraftTargetSnapshotSource source =
    new MinecraftTargetSnapshotSource(levelAccessor);

// 注册 capability resolvers（默认已注册 energy/item/fluid）
source.registerCapabilityResolver(
    ResourceLocation.parse("cumulus:my_custom_cap"),
    (levelBE, side) -> levelBE.getCapability(MyCapabilities.MY_CAP, side));

// 引擎用：
ENGINE.track(def.id(), anchorPos, source);

// 内部读各类数据：
public Optional<TargetSnapshot> read(TargetReadRequest req) {
    BlockPos pos = req.pos();
    if (!level.hasChunkAt(pos)) {
        return Optional.empty();  // unloadedArea
    }

    return switch (req.targetType()) {
        case BLOCK -> Optional.of(readBlock(pos));
        case BLOCK_ENTITY -> Optional.ofNullable(readBlockEntity(pos));
        case ENTITY -> Optional.of(readEntitiesInAABB(req.aabb()));
        case FLUID -> Optional.of(readFluid(pos));
        case CAPABILITY -> readCapability(pos, req.capabilityName(), req.side());
        case CONTEXT -> readContextValue(req.contextKey());
        case REGION -> Optional.of(readRegion(req.from(), req.to()));
    };
}

// BlockSnapshot 包含：
//   blockId, blockStateProperties, blockEntityNbt (optional), skyLight`,
    explanation: [
      "MinecraftTargetSnapshotSource 是 TargetSnapshotSource 的 LevelAccessor 适配器，引擎唯一的 Minecraft 入口。",
      "所有 read 都先检查 hasChunkAt — 未加载返回 empty，触发 DataRequest 路径。",
      "CapabilitySnapshot 通过 NeoForge BlockCapability 查询；resolver map 把 capabilityId 映射到具体 BlockCapability。",
      "snapshot 是只读的快照（不是 live 引用）— 引擎持有快照不会阻塞 chunk 卸载。",
    ],
    trace: [
      { step: "engine 请求", internal: "executor 调 source.read(req)" },
      { step: "chunk 检查", internal: "level.hasChunkAt(pos)? 未加载 → empty" },
      { step: "数据读取", internal: "switch target type → 读 block/BE/entity/fluid/cap" },
      { step: "snapshot 构造", internal: "BlockSnapshot/EntitySnapshot/..." },
      { step: "返回", internal: "Optional.of(snapshot) 给 executor" },
    ],
    pitfalls: [
      "snapshot 不持有 Level 引用（防泄漏）。所有字段是值类型或 immutable NBT。",
      "hasChunkAt 检查不能省 — 直接 getBlockState 会强制 chunk gen。",
      "BlockCapability 查询必须传 side；不传会拿到 null cap。",
      "snapshot 要在 server thread 读；客户端 level 数据不可信。",
    ],
    related: ["mc-event-bridge", "mc-runtime-discovery"],
  },
  {
    id: "mc-event-bridge",
    category: "Minecraft",
    api: "MinecraftRuntimeEventBridge",
    title: "NeoForge BlockEvent → markDirty",
    scenario:
      "玩家放/破坏方块、流体流动时，引擎要立刻知道。NeoForge 提供了 BlockEvent，需要 bridge 把事件转成 engine.markDirty 调用。",
    difficulty: "intermediate",
    accent: "rose",
    icon: "Cable",
    code: `// MinecraftRuntimeEventBridge 自动注册这些事件：
@SubscribeEvent
public void onBlockPlace(BlockEvent.EntityPlace event) {
    bridge.markDirty(event.getPos());
}

@SubscribeEvent
public void onBlockRemove(BlockEvent.BlockDestroy event) {
    bridge.markDirty(event.getPos());
}

@SubscribeEvent
public void onFluidPlace(BlockEvent.FluidPlaceBlock event) {
    bridge.markDirty(event.getPos());
}

@SubscribeEvent
public void onMultiPlace(BlockEvent.EntityMultiPlace event) {
    for (var state : event.getReplacedBlockStates()) {
        bridge.markDirty(event.getPos());
    }
}

// bridge 内部：
public void markDirty(BlockPos pos) {
    SectionKey key = SectionKey.fromBlock(pos);
    Set<StructureInstanceKey> affected = sectionIndex.get(key);
    if (affected != null) {
        affected.forEach(engine::markDirty);
    }
    // 也通知 discovery：可能是新 anchor
    discoveryRegistry.onBlockChange(pos);
}`,
    explanation: [
      "BlockEvent 是 NeoForge 服务器端事件，覆盖 place/destroy/multi-place/fluid-place 等场景。",
      "bridge 把 BlockPos 转 SectionKey，查反向索引找到受影响的 instance，调 engine.markDirty。",
      "同一 section 内多个 block 变化只触发一次 markDirty（去重），下一 tick 才 check。",
      "也通知 MinecraftRuntimeDiscovery：可能是新 controller 被放置。",
    ],
    trace: [
      { step: "玩家放方块", internal: "BlockEvent.EntityPlace 触发" },
      { step: "section 计算", internal: "SectionKey.fromBlock(pos) = (x>>4, y>>4, z>>4)" },
      { step: "反向索引", internal: "sectionIndex.get(secKey) 返回 instance keys" },
      { step: "markDirty", internal: "engine.markDirty(instanceKey) 加入 dirty queue" },
      { step: "下一 tick", internal: "scheduler 取出 dirty instance, 执行 check" },
      { step: "discovery", internal: "discoveryRegistry.onBlockChange 检查是否新 controller" },
    ],
    pitfalls: [
      "事件必须在 server thread；client 事件 (LevelEvent) 不能 markDirty。",
      "EntityMultiPlace 一次可能上百个 block 变化，必须批量处理。",
      "FluidPlaceBlock 涉及流动的流体，可能波及多个区块 — 注意 hasChunkAt。",
      "事件被 cancel 时不应 markDirty（玩家动作没生效）。",
    ],
    related: ["mc-snapshot-source", "mc-runtime-discovery"],
  },
  {
    id: "mc-runtime-discovery",
    category: "Minecraft",
    api: "MinecraftRuntimeDiscovery",
    title: "自动发现新 controller",
    scenario:
      "玩家放好 controller 时，引擎不知道这是新 instance。需要 discovery 机制：监听 block 变化，匹配 controller predicate，自动 track。",
    difficulty: "advanced",
    accent: "rose",
    icon: "Search",
    code: `// 注册 discovery：
public void onCommonSetup(FMLCommonSetupEvent e) {
    MinecraftRuntimeDiscovery discovery = new MinecraftRuntimeDiscovery(ENGINE);

    // 为每个定义注册 discovery 候选
    discovery.registerCandidate(MINI_FORGE.id(),
        // 候选 1：explicit anchor target
        DiscoveryCandidate.explicit("controller",
            PredicateDescriptors.id("cumulus:forge_controller")));

    discovery.registerCandidate(FACTORY.id(),
        // 候选 2：inferred required point
        DiscoveryCandidate.inferredRequiredPoint("controller"));

    // 启动 bridge（自动 wire BlockEvent）
    new MinecraftRuntimeEventBridge(level, discovery, ENGINE);
}

// 内部：
public void onBlockChange(BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    for (var entry : candidatesByPredicate.entrySet()) {
        if (entry.getKey().matches(state)) {
            // 匹配！计算 owning anchor
            // 比如 controller 偏移 (0,1,0)，那 anchor 是 pos.below()
            BlockPos anchor = computeAnchor(pos, entry.getValue());
            StructureInstanceKey key = new StructureInstanceKey(
                entry.getValue().definitionId(), anchor);

            if (!ENGINE.isTracked(key)) {
                // 新 instance！自动 track
                ENGINE.track(entry.getValue().definitionId(),
                    anchor, source);
                log.info("auto-discovered " + key);
            }
        }
    }
}`,
    explanation: [
      "DiscoveryCandidate 有两种：explicit anchor target（定义里标记 anchor=true 的 point）和 inferred required point（第一个 required point）。",
      "discovery 监听 block 变化，对每个候选 predicate 检查匹配。",
      "匹配后从 point offset 反推 anchor 位置（controller 在 (0,1,0) → anchor 是 pos.below）。",
      "如果 anchor 已 tracked，跳过（避免重复 track）。",
    ],
    trace: [
      { step: "玩家放 controller", internal: "BlockEvent 触发" },
      { step: "predicate 匹配", internal: "forge_controller predicate matches state" },
      { step: "anchor 计算", internal: "candidate.offset = (0,1,0); anchor = pos.subtract(0,1,0)" },
      { step: "duplicate check", internal: "ENGINE.isTracked(key)? 已 tracked → skip" },
      { step: "track", internal: "ENGINE.track(defId, anchor, source)" },
      { step: "第一次 check", internal: "下一 tick 验证整个结构是否成立" },
    ],
    pitfalls: [
      "discovery 性能：每个 block 变化都要遍历所有 candidate predicate。建议 candidate 数 ≤ 50。",
      "anchor 计算必须正确：offset 反向。如果 controller 在 (0,0,0)，anchor = pos；在 (0,1,0)，anchor = pos.below。",
      "玩家放非 controller 方块（外壳等）不应触发 discovery — 只匹配 declared anchor。",
      "自动 discovery 不要重复触发：同一个 anchor 已经 tracked 时跳过。",
    ],
    related: ["mc-snapshot-source", "mc-event-bridge"],
  },

  // ============================================================
  // Datapack & Datagen
  // ============================================================
  {
    id: "datagen-provider",
    category: "Datagen",
    api: "RuntimeAssetDatagenProvider",
    title: "datagen 导出 runtime JSON",
    scenario:
      "modder 在 Java 里定义了 20 个 StructureDefinition。需要在 build 时自动生成对应的 JSON 资产到 generated/resources/data/cumulus/structures/。",
    difficulty: "basic",
    accent: "cyan",
    icon: "FileOutput",
    code: `// 1. 实现 RuntimeAssetDatagenProvider
public class CumulusRuntimeAssetProvider
        extends RuntimeAssetDatagenProvider {

    public CumulusRuntimeAssetProvider(PackOutput output) {
        super(output, Cumulus.modId);
    }

    @Override
    protected void registerAssets() {
        // 注册所有 StructureDefinition
        add(ModStructures.MINI_FORGE);
        add(ModStructures.FACTORY);
        add(ModStructures.ARM);
        add(ModStructures.TOOL);
        // ...
    }
}

// 2. 在 GatherDataEvent 注册
@SubscribeEvent
public void onGatherData(GatherDataEvent.Server event) {
    var generator = event.getGenerator();
    generator.addProvider(event.includeServerData(),
        new CumulusRuntimeAssetProvider(generator.getPackOutput()));
}

// 3. 运行 gradle datagen
// ./gradlew runData
// 输出：generated/resources/data/cumulus/structures/mini_forge.json
//      generated/resources/data/cumulus/structures/factory.json
//      ...

// 4. JSON 内容（自动生成）：
// {
//   "id": "cumulus:mini_forge",
//   "schema_version": 2,
//   "transforms": [...],
//   "points": [...],
//   "context_rules": [...]
// }`,
    explanation: [
      "RuntimeAssetDatagenProvider 是 NeoForge 的标准 datagen provider，每行 add(def) 输出一个 JSON 文件。",
      "JSON 内容是确定性的：sort keys + 稳定顺序，git diff 友好。",
      "重复 path（同一文件被 add 两次）会抛 RuntimeException — 避免覆盖。",
      "运行 ./gradlew runData 输出到 generated/resources/；NeoForge 自动把这个目录当 datapack。",
    ],
    trace: [
      { step: "gradle runData", internal: "GatherDataEvent.Server 触发" },
      { step: "provider 注册", internal: "generator.addProvider(new CumulusRuntimeAssetProvider)" },
      { step: "registerAssets", internal: "调 add() 多次，累积 def list" },
      { step: "导出", internal: "for each def: serialize to JSON, write to data/<ns>/structures/<id>.json" },
      { step: "重复检测", internal: "已写入的 path 抛异常（fail fast）" },
      { step: "build", internal: "NeoForge 把 generated/ 当 datapack；玩家进游戏自动加载" },
    ],
    pitfalls: [
      "JSON 必须确定性：相同输入产出字节相同。Map 用 TreeMap，List 顺序固定。",
      "id 含 '/' 会创建子目录（cumulus:machine/v2 → machine/v2.json）— 这是 feature，用于版本化。",
      "datagen 失败要 fail fast，不能写一半文件 — 中断 build。",
      "测试：每次 build 后跑 'datagen output 没变' 测试，防止意外改动。",
    ],
    related: ["reload-listener", "bridge-to-engine"],
  },
  {
    id: "reload-listener",
    category: "Datagen",
    api: "RuntimeAssetReloadListener",
    title: "服务器 reload：扫描 + 加载",
    scenario:
      "玩家改了 datapack 里的结构 JSON，输入 /reload。服务器要重新扫描所有 data/*/cumulus/structures/*.json，加载到 RuntimeAssetRepository。",
    difficulty: "intermediate",
    accent: "cyan",
    icon: "RefreshCw",
    code: `// 1. 注册 reload listener（mod 加载时）
public void onCommonSetup(FMLCommonSetupEvent e) {
    NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
        event.addListener(new RuntimeAssetReloadListener(
            Cumulus.modId,
            "structures",
            new RuntimeAssetJsonLoader()));
    });
}

// 2. listener 在 reload 时被调用
public class RuntimeAssetReloadListener implements PreparableReloadListener {
    @Override
    protected CompletableFuture<Void> prepare(
            ResourceManager mgr, ExecutorPool pool) {
        // 在后台线程扫描 + 解析
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, ParsedAsset> assets = new HashMap<>();
            for (var res : mgr.listResources("cumulus/structures",
                    p -> p.getPath().endsWith(".json")).values()) {
                try (var in = res.open()) {
                    var json = new String(in.readAllBytes(), UTF_8);
                    var parsed = jsonLoader.parse(json, res.id());
                    assets.put(parsed.id(), parsed);
                }
            }
            return assets;
        }, pool);
    }

    @Override
    protected void apply(Map<ResourceLocation, ParsedAsset> assets,
            ResourceManager mgr) {
        // 在主线程：原子替换 repository snapshot
        repository.replaceSnapshot(assets);
        // 通知 bridge：注册新 def 到 engine
        bridge.onRepositoryReplaced(repository);
    }
}`,
    explanation: [
      "PreparableReloadListener 是 Minecraft 标准 reload 接口。prepare 在后台线程，apply 在主线程。",
      "扫描路径：data/<任意 ns>/cumulus/structures/<任意 path>.json — 多个 datapack 可以提供同名 id（后加载覆盖）。",
      "prepare 阶段解析所有 JSON，构造 ParsedAsset map（id → 资产）。",
      "apply 阶段原子替换 repository snapshot，然后通知 bridge 重新注册 engine。",
      "如果 prepare 抛异常（任何 JSON 解析失败），apply 不执行，repository 保留旧 snapshot。",
    ],
    trace: [
      { step: "/reload 命令", internal: "ServerReload command 启动" },
      { step: "prepare (后台)", internal: "扫描所有 JSON; 解析; 累积 ParsedAsset map" },
      { step: "异常?", internal: "任何 JSON 出错 → 整个 prepare fail; apply 不执行" },
      { step: "apply (主线程)", internal: "repository.replaceSnapshot(newMap) 原子替换" },
      { step: "bridge 通知", internal: "bridge.onRepositoryReplaced; re-register all defs" },
      { step: "engine 重建 plan", internal: "for each def: compile new plan; planCache.replace" },
      { step: "tracked instances 重 check", internal: "下一 tick 用新 plan 重 check" },
    ],
    pitfalls: [
      "prepare 不能访问 level（在后台线程）；只能读 ResourceManager。",
      "apply 必须原子 — 不能 partial replace。Snapshot 整个换。",
      "重复 path（多 datapack 同 id）按 datapack 加载顺序覆盖；NeoForge 默认 alphabetical。",
      "reload 失败时玩家要看到诊断（聊天/system message），不能静默。",
    ],
    related: ["datagen-provider", "bridge-to-engine"],
  },

  // ============================================================
  // Migration & Versioning
  // ============================================================
  {
    id: "versioned-migration",
    category: "Migration",
    api: "StructureVersionDescriptor + ContextMigrationExecutor",
    title: "升级 mod 时机器不失效",
    scenario:
      "mod v1.0 用 'facing' 作为 context key，v1.1 改成 'orientation'。升级后所有已组装机器的 NBT 还是旧版本，必须自动迁移。",
    difficulty: "advanced",
    accent: "lime",
    icon: "GitPullRequestArrow",
    code: `// v1.0:
public static final StructureDefinition MACHINE_V1 =
    Structures.define(id("machine"))
        .version(new StructureVersionDescriptor(1, null))
        .context(ContextRuleDescriptor.derive(
        ContextKey.string("facing"),
        ContextSources.capture("controller").property("facing")))
        .build();

// v1.1:
public static final StructureDefinition MACHINE_V2 =
    Structures.define(id("machine"))
        .version(new StructureVersionDescriptor(2,
            ResourceLocation.parse("cumulus:machine/v2")))
        .context(ContextRuleDescriptor.derive(
        ContextKey.string("orientation"),
        ContextSources.capture("controller").property("facing")))
        .context(ContextRuleDescriptor.derive(
        ContextKey.string("tier"),
        ContextSources.capture("controller").property("tier")))
        .build();

// 注册 migration chain：
ContextMigrationExecutor.register("cumulus:machine",
    new MigrationStep(1, 2) {
        @Override
        public void migrate(RuntimeStructureContext ctx) {
            // facing → orientation
            Object facing = ctx.get("facing");
            if (facing != null) {
                ctx.set("orientation", facing);
                ctx.remove("facing");
            }
            // tier 是新字段，从 controller NBT 读
            var controller = (BlockPos) ctx.get("controller");
            var be = level.getBlockEntity(controller);
            if (be != null) {
                ctx.set("tier", be.getPersistentData().getInt("tier"));
            }
        }
    });

// 玩家旧机器升级流程：
//   1. NBT 里 revision = 1
//   2. mod v2 加载，def.version = 2
//   3. 第一次 tick：engine 看 revision 不匹配，执行 migration chain
//   4. ctx 从 v1 形态迁移到 v2 形态
//   5. 机器逻辑读 'orientation' / 'tier' 拿到值，正常工作`,
    explanation: [
      "StructureVersionDescriptor(schema, migrateTo) 标记当前 def 版本 + 升级目标 id。",
      "ContextMigrationExecutor 注册 MigrationStep(fromVersion, toVersion)；chain 可以是多步（1→2→3）。",
      "engine 在 check 后比较 ctx.revision 和 def.schemaVersion；不匹配时按 chain 逐步迁移。",
      "MigrationStep.migrate 直接 mutate ctx（add/remove/update keys）。机器逻辑只看最新 revision。",
    ],
    trace: [
      { step: "首次 check", internal: "engine 读 BE NBT: revision=1; def.version=2" },
      { step: "mismatch", internal: "ctx.revision=1 < def.version=2 → 需要迁移" },
      { step: "step 查找", internal: "ContextMigrationExecutor.findStep('cumulus:machine', 1, 2)" },
      { step: "migrate", internal: "step.migrate(ctx): facing → orientation; 添加 tier" },
      { step: "revision 更新", internal: "ctx.revision = 2" },
      { step: "持久化", internal: "BE NBT revision 写回 2; 下次启动无需 migrate" },
    ],
    pitfalls: [
      "MigrationStep 必须幂等 — 迁移完的 ctx 再次 migrate 应该 no-op。",
      "chain 不能跳号：1→3 没注册会 fail；必须 1→2 然后 2→3。",
      "MigrationStep.migrate 在 server thread；不能读写 chunk（可能未加载）。",
      "ctx 不能保证有所有旧 keys（玩家可能 NBT 损坏）；migrate 要 null-safe。",
    ],
    related: ["data-driven-migration"],
  },
  {
    id: "data-driven-migration",
    category: "Migration",
    api: "MigrationStep via datapack",
    title: "玩家自定义 migration",
    scenario:
      "整合包作者把 'machine' 从 v3 改到 v4，但 modder 没发布 v4 mod。整合包作者要在 datapack 里写 migration。",
    difficulty: "advanced",
    accent: "lime",
    icon: "FileCog",
    code: `// 整合包在 datapack 里写：
// data/my_pack/cumulus/migrations/machine_v3_to_v4.json
{
  "definition_id": "cumulus:machine",
  "from_version": 3,
  "to_version": 4,
  "operations": [
    {
      "type": "rename_key",
      "from": "tier",
      "to": "level"
    },
    {
      "type": "transform_value",
      "key": "level",
      "transform": "add 1"
    },
    {
      "type": "add_key_if_absent",
      "key": "migrated_at",
      "value": "\${now}"
    }
  ]
}

// Cumulus 自动加载这个 JSON：
public class DataDrivenMigrationLoader {
    public void onReload(ResourceManager mgr) {
        for (var res : mgr.listResources("cumulus/migrations",
                p -> p.getPath().endsWith(".json")).values()) {
            var json = readJson(res);
            var step = MigrationStep.fromJson(json);
            ContextMigrationExecutor.register(
                step.definitionId(), step);
        }
    }
}

// MigrationOperation 类型：
//   rename_key        重命名 key
//   transform_value   用表达式变换值
//   add_key_if_absent 补默认值
//   remove_key        删 key
//   custom            调用注册的 custom migration function`,
    explanation: [
      "JSON-based migration 让玩家/整合包作者不用 Java 也能写迁移逻辑。",
      "operations 是声明式的：rename/transform/add/remove + 自定义（注册函数）。",
      "transform 表达式有限：add/sub/mul/div + min/max；复杂逻辑用 'custom' 类型调 Java 函数。",
      "MigrationStep.fromJson 构造等价的 Java 对象，注册到 ContextMigrationExecutor。",
    ],
    trace: [
      { step: "reload 扫描", internal: "扫描 data/*/cumulus/migrations/*.json" },
      { step: "解析", internal: "MigrationStep.fromJson(json)" },
      { step: "注册", internal: "ContextMigrationExecutor.register(defId, step)" },
      { step: "首次 check", internal: "ctx.revision=3, def.version=4 → 迁移" },
      { step: "operations 执行", internal: "rename tier→level; level+1; add migrated_at" },
      { step: "持久化", internal: "BE NBT revision=4 写回" },
    ],
    pitfalls: [
      "JSON migration 表达力有限；复杂逻辑要 modder 写 Java 注册 custom function。",
      "migrated_at 用 ${now} 模板；migration 执行时替换。要避免非确定性（每次迁移值不同）。",
      "rename_key 如果目标 key 已存在，会覆盖 — 检查 schema。",
      "JSON migration 和 Java migration 可以共存；按 version 顺序执行。",
    ],
    related: ["versioned-migration"],
  },
  // ============================================================
  // CheckPhase — 动态结构判定
  // ============================================================
  {
    id: "check-phase-repeat",
    category: "Definition",
    api: "CheckPhase + CheckPhases.repeatRange",
    title: "动态结构：轴向区间重复",
    scenario:
      "结构的外壳层数不固定——比如一座塔，外壳至少 2 层（必须存在），最多 5 层（尽量多）。层数由实际世界方块决定，定义期无法预知。",
    difficulty: "advanced",
    accent: "magenta",
    icon: "Layers",
    code: `import static dev.vfyjxf.cumulus.api.context.plan.stdlib.CheckPhases.*;

public static final ContextKey<Integer> TOWER_HEIGHT =
    ContextKey.integer("tower_height");

public static final StructureDefinition ADAPTIVE_TOWER =
    Structures.define(id("adaptive_tower"))
        .point("controller", at(0,0,0), Targets.block()
            .where(PredicateDescriptors.id("cumulus:controller"))
            .capture("controller"))
        // repeatRange: min 层必须存在，max 层尽量多
        .checkPhase(repeatRange(
            "ring",
            Components.hollowBox("ring", at(-1,0,-1), at(1,0,2)),
            Axis.y, 2, 5, TOWER_HEIGHT))
        .build();

// 运行时：
// executor 先匹配 controller (静态 point)
// → 调用 CheckPhase.evaluate()
// → repeatRange 生成 2~5 层 point（L0,L1 required; L2,L3,L4 optional）
// → executor 逐层匹配世界方块
// → L0,L1 必须满足（否则 invalid），L2+ 尽量多
// → 匹配的层数写入 TOWER_HEIGHT context key
// → 机器逻辑读 TOWER_HEIGHT 决定输出倍率`,
    explanation: [
      "CheckPhase 是通用的动态判定钩子：在静态 point 匹配后运行，能读已匹配数据、判断合规性、写上下文、返回额外 point。",
      "repeatRange 是 CheckPhase 的便利用法：沿指定轴重复模板 component，min 层 required、max 层 optional。",
      "executor 对 phase 返回的 point 执行同样的匹配流程（resolve target + predicate），匹配结果加入 MatchResults。",
      "后续的 ContextPlan 操作能看到所有匹配点（静态 + 动态），比如 derive(tower_height) 算输出倍率。",
    ],
    trace: [
      { step: "静态匹配 controller", internal: "checkCandidate → matchPoint(controller)" },
      { step: "CheckPhase.evaluate(ctx)", internal: "repeatRange 生成 L0~L4 共 5 层 point" },
      { step: "逐层匹配 L0,L1 (required)", internal: "matchPoint × 2 → 都满足 → 继续" },
      { step: "逐层匹配 L2,L3 (optional)", internal: "matchPoint × 2 → L2 满足、L3 不满足 → 停" },
      { step: "putComputed(TOWER_HEIGHT, 3)", internal: "3 层匹配成功" },
      { step: "toRuntime()", internal: "derived ∪ computed → RuntimeStructureContext" },
    ],
    pitfalls: [
      "min 设太高会导致结构永远 invalid（玩家无法满足）",
      "max 层的 optional point 如果数据未加载会发 DataRequest（incomplete），不会直接 invalid",
      "CheckPhase 回调里不要做重计算——它在每次 check 时同步执行",
    ],
    related: ["custom-context-plan"],
  },
  {
    id: "check-phase-custom",
    category: "Definition",
    api: "CheckPhase (custom)",
    title: "动态结构：完全自定义形态",
    scenario:
      "结构的形态无法用简单的重复表达——比如根据 controller 旁边是否有特殊方块决定检测半径，或者根据 context 值决定检测哪些 component。",
    difficulty: "advanced",
    accent: "brand",
    icon: "Shuffle",
    code: `// CheckPhase 是一个 @FunctionalInterface：
// List<StructurePointDefinition> evaluate(PhaseContext ctx)
//
// ctx.matches()    — 当前已匹配的点
// ctx.context()    — 活的计算上下文（读/写）
// ctx.anchor()     — 结构锚点的绝对坐标
// ctx.transform()  — 当前 transform candidate

Structures.define(id("adaptive_forge"))
    .point("controller", at(0,0,0), Targets.block()
        .where(PredicateDescriptors.id("cumulus:controller")))
    .checkPhase(ctx -> {
        var ctrl = ctx.matches()
            .first(p -> p.id().value().equals("controller"));
        if (ctrl.isEmpty()) return List.of();

        // 根据 controller 的 block state 决定检测半径
        var facing = ctrl.get().properties().get("facing");
        var points = new ArrayList<StructurePointDefinition>();
        for (int r = 1; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // 只检测 ring 外壳
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    points.add(new StructurePointDefinition(
                        new StructurePointId("casing_" + r + "_" + dx + "_" + dz),
                        RelativeBlockPos.of(dx, 0, dz),
                        Targets.block().target(),
                        List.of(), PointRequirement.required,
                        Set.of("casing"), Set.of(),
                        DiagnosticMetadata.empty()));
                }
            }
        }
        ctx.context().putComputed(SHELL_RADIUS, detectedRadius);
        return points;
    })
    .build();`,
    explanation: [
      "CheckPhase 不限于重复——它可以返回任意 point 列表，完全由 modder 逻辑决定。",
      "PhaseContext 暴露 matches()（已匹配点的值/属性/位置）、context()（读写计算上下文）、anchor()、transform()。",
      "phase 返回的 point 被 executor 匹配后，结果加入 MatchResults，后续操作（如果有的话）能看到它们。",
      "这是两阶段匹配的本质：先匹配锚点（controller），再根据锚点的实际世界数据决定结构的完整形态。",
    ],
    trace: [
      { step: "匹配 controller", internal: "controller 的 block state 被读入 matches()" },
      { step: "CheckPhase 读 controller.facing", internal: "ctrl.get().properties().get(\"facing\")" },
      { step: "根据 facing 生成外壳 ring", internal: "1~3 格半径 ring point 列表" },
      { step: "executor 匹配 ring point", internal: "逐个 resolveTarget + predicate" },
      { step: "putComputed(SHELL_RADIUS)", internal: "写入检测到的半径" },
    ],
    pitfalls: [
      "phase 回调里返回的 point 位置是相对于 anchor 的 RelativeBlockPos，executor 自动加 anchor 偏移",
      "如果 phase 返回的 point 是 required 且缺失，会发 DataRequest（incomplete）",
      "phase 回调抛异常会被 executor 捕获（静默跳过扩展），不会让整个 check 崩溃",
    ],
    related: ["check-phase-repeat"],
  },
  // ============================================================
  // PartPattern Shape DSL
  // ============================================================
  {
    id: "shape-matrix",
    category: "Definition",
    api: "PartPattern + ShapeEmitter + .shape()",
    title: "PartPattern 矩阵：用多维数组画结构",
    scenario:
      "不想逐个写 .point() 和 .component()，想用类似 GregTech aisle 的方式直观画出 3D 结构——但要强类型，不用字符/字符串。",
    difficulty: "intermediate",
    accent: "brand",
    icon: "Grid3x3",
    code: `import static dev.vfyjxf.cumulus.api.definition.Structures.*;
import static dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors.*;

// 整个结构在一个 static final 表达式里
public static final StructureDefinition FORGE = Structures.define(
    "cumulus:forge",
    def -> {
        // 声明 PartPattern（强类型，预绑定具体值）
        var casing = PartPattern.block("casing")
            .where(tag("cumulus:heat_casing"))
            .group("casing");
        var ctrl = PartPattern.block("controller")
            .where(id("cumulus:controller"))
            .capture("controller");
        var energy = PartPattern.capability("energy",
            "cumulus:energy").group("port");
        var air = PartPattern.air();

        // 多维数组 [y][z][x] 画出 3D 结构
        return def.shape(new PartPattern[][][]{ {
            // Y=0 底层
            { casing, casing,  casing },
            { casing, ctrl,    casing },
            { casing, casing,  casing },
        }, {
            // Y=1 顶层
            { casing, casing, casing },
            { casing, casing, casing },
            { casing, casing, casing },
        }, {
            // Y=2 前层
            { casing, energy, casing },
            { casing, air,    casing },
            { casing, casing, casing },
        } })
        .transforms(Transforms.horizontalFacing())
        .build();
    });`,
    explanation: [
      "PartPattern 持有完整的 point 定义（target + predicates + captures + group + requirement），放进多维数组 [y][z][x] 直观画出结构。",
      "数组里每个格子就是一个配置好的 PartPattern，不需要后绑定（不用 GT 的 where(char, predicate)）。",
      "PartPattern.air() 标记的格子被 DefaultShapeEmitter 跳过（= 空位）。",
      "Structures.define(id, lambda) 把整个定义包在一个 static final 表达式里，不泄漏局部变量。",
    ],
    trace: [
      { step: "声明 PartPattern", internal: "PartPattern.block(...).where(...).group(...)" },
      { step: "填充多维数组", internal: "new PartPattern[][][]{ { {...}, {...} } }" },
      { step: ".shape(matrix)", internal: "DefaultShapeEmitter 遍历 [y][z][x]" },
      { step: "展开为 StructurePointDefinition", internal: "每个格子 → toPoint(RelativeBlockPos)" },
      { step: ".build()", internal: "StructureDefinitionCompiler 编译为 IR" },
    ],
    pitfalls: [
      "数组维度必须是 [y][z][x]（第一维是层，第二维是行，第三维是列）",
      "同一 PartPattern 实例可以在数组里多次引用（共享配置）",
      "null 也会被 DefaultShapeEmitter 跳过，但建议用 PartPattern.air() 显式标记",
    ],
    related: ["check-phase-repeat", "check-phase-custom"],
  },
  {
    id: "custom-shape-emitter",
    category: "Definition",
    api: "ShapeEmitter (custom)",
    title: "自定义 Emitter：镜像/旋转/条件展开",
    scenario:
      "默认 emitter 只是逐格展开。如果需要更复杂的布局逻辑（比如根据某个 PartPattern 的存在决定其他格子是否展开），可以实现自定义 ShapeEmitter。",
    difficulty: "advanced",
    accent: "cyan",
    icon: "Layers",
    code: `// 自定义 emitter：只展开对称轴一侧，镜像复制到另一侧
public final class MirrorEmitter implements ShapeEmitter {
    private final Axis mirrorAxis;

    public MirrorEmitter(Axis mirrorAxis) {
        this.mirrorAxis = mirrorAxis;
    }

    @Override
    public void emit(PartPattern[][][] shape, ShapeEmitterContext ctx) {
        int depth = shape.length;
        for (int y = 0; y < depth; y++) {
            for (int z = 0; z < shape[y].length; z++) {
                for (int x = 0; x < shape[y][z].length; x++) {
                    var part = shape[y][z][x];
                    if (part == null || part.isAir()) continue;
                    // 原始位置
                    ctx.point(x, y, z, part);
                    // 镜像位置（沿 mirrorAxis 对称）
                    var mirrored = mirrorAxis == Axis.x
                        ? new int[]{shape[y][z].length - 1 - x, y, z}
                        : mirrorAxis == Axis.z
                        ? new int[]{x, y, shape[y].length - 1 - z}
                        : new int[]{x, shape.length - 1 - y, z};
                    ctx.point(mirrored[0], mirrored[1], mirrored[2], part);
                }
            }
        }
    }
}

// 使用：
.shape(shape, new MirrorEmitter(Axis.x))`,
    explanation: [
      "ShapeEmitter 是 @FunctionalInterface：emit(PartPattern[][][], ShapeEmitterContext)。",
      "ShapeEmitterContext.point(x, y, z, part) 向 builder 发射一个 point。",
      "自定义 emitter 可以实现镜像、旋转、条件展开、跨层复制等任何布局逻辑。",
      ".shape(matrix) 使用 DefaultShapeEmitter；.shape(matrix, customEmitter) 使用自定义的。",
    ],
    trace: [
      { step: "遍历原始数组", internal: "for y, z, x → 跳过 null/air" },
      { step: "发射原始位置", internal: "ctx.point(x, y, z, part)" },
      { step: "计算镜像位置", internal: "mirrorAxis 决定对称轴" },
      { step: "发射镜像位置", internal: "ctx.point(mx, my, mz, part)" },
    ],
    pitfalls: [
      "emitter 里不要修改 shape 数组本身（它可能是共享的）",
      "镜像/旋转后如果位置重叠，builder 会产生重复 point id — 使用 PartPattern.name + 坐标自动去重",
    ],
    related: ["shape-matrix"],
  },
  {
    id: "count-constraint",
    category: "Definition",
    api: "CountConstraint + PhaseResult",
    title: "Count 约束：全局/分组数量限制",
    scenario:
      "需要限制结构中某类方块的数量——比如至少 8 个 casing、最多 4 个 energy port、恰好 4 个线圈。",
    difficulty: "intermediate",
    accent: "amber",
    icon: "Hash",
    code: `import static dev.vfyjxf.cumulus.api.predicate.StructurePredicates.*;

public static final StructureDefinition COIL_MACHINE =
    Structures.define("cumulus:coil_machine", def -> {
        var coil = PartPattern.block("coil")
            .where(tag("cumulus:coil")).group("coil");
        var port = PartPattern.capability("port",
            "cumulus:energy").group("port");

        return def.shape(new PartPattern[][][]{ {
            { coil, coil, coil },
            { coil, port, coil },
            { coil, coil, coil },
        } })
        // 静态约束写在 checkPhase 里
        .checkPhase(ctx -> PhaseResult.of(
            List.of(),
            List.of(
                // 至少 8 个 coil
                CountConstraint.atLeast(group("coil"), 8),
                // 最多 4 个 port
                CountConstraint.atMost(group("port"), 4),
                // 恰好 4 个 coil（等价于 range 4~4）
                CountConstraint.exactly(
                    StructurePredicates.tag("cumulus:coil"), 4)
            )))
        .build();
    });

// executor 在所有匹配完成后验证 constraints：
// 如果某个 constraint 不满足 → status = invalid
// 错误信息："Count constraint violated: expected 8~MAX matching points, got 5"`,
    explanation: [
      "CountConstraint 在 CheckPhase 返回的 PhaseResult 里声明，executor 匹配完成后自动验证。",
      "filter 是 StructurePredicate（强类型，支持 and/or/not 组合）。",
      "atLeast/atMost/exactly/range 四种工厂覆盖常见场景。",
      "约束不满足时产生 diagnostic node（status → invalid），包含期望值和实际值。",
    ],
    trace: [
      { step: "静态 + phase point 匹配完成", internal: "MatchResults 包含所有匹配点" },
      { step: "验证每个 constraint", internal: "count(filter) vs min~max" },
      { step: "atLeast(coil, 8) 检查", internal: "count(group(\"coil\")) = 8 ✓" },
      { step: "atMost(port, 4) 检查", internal: "count(group(\"port\")) = 1 ✓" },
      { step: "全部通过 → valid", internal: "否则 → invalid + diagnostic" },
    ],
    pitfalls: [
      "constraint 在 phase 之后验证，所以 phase 返回的额外 point 也参与计数",
      "StructurePredicate 继承 Predicate<MatchedPoint>，可以直接用 lambda：p -> p.value().startsWith(\"cumulus:\")",
    ],
    related: ["check-phase-custom", "shape-matrix"],
  },
];

// utility: 按 category 分组
export function groupByCategory(cards: ApiCard[]): Record<ApiCategory, ApiCard[]> {
  const result = {} as Record<ApiCategory, ApiCard[]>;
  for (const cat of Object.keys(CATEGORY_LABEL) as ApiCategory[]) {
    result[cat] = cards.filter((c) => c.category === cat);
  }
  return result;
}
