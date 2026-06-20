// ============================================================
// 设计决策深度解析：每个 API 形态背后的 why
// ============================================================

export interface RationaleCard {
  id: string;
  icon: string;
  accent: "brand" | "cyan" | "magenta" | "lime" | "amber" | "rose";
  question: string;
  oneLiner: string;
  problem: string;
  alternatives: { name: string; verdict: string; rejectedBecause: string }[];
  solution: string;
  whyThisWay: string;
  realCost: string;
  codeSnippet: string;
  metrics?: { label: string; cumulus: string; vanilla: string }[];
}

export const RATIONALE: RationaleCard[] = [
  {
    id: "immutable-ir",
    icon: "Lock",
    accent: "brand",
    question: "为什么 IR 是不可变 record 而不是 mutable builder？",
    oneLiner: "Builder 在 build() 之后就死掉，IR 是冻结的真源。",
    problem:
      "传统多方块库的 'MultiblockPattern' 经常是一个有状态的 Java 对象：开发者 addBlock() 之后还能继续 add，运行时校验时又有人会无意中改它。结果就是：同一个 pattern 在不同线程、不同 reload 周期里行为可能不一致；cache key 不稳定；测试无法复现。",
    alternatives: [
      {
        name: "mutable config object",
        verdict: "rejected",
        rejectedBecause:
          "Builder pattern 一直持有引用，datagen 写出去的 JSON 在下一次 tick 可能已经变了；运行时缓存无法基于对象身份索引。",
      },
      {
        name: "deep clone at consumption",
        verdict: "rejected",
        rejectedBecause:
          "每个消费者各 clone 一份，内存爆炸；而且 clone 仍然不能阻止作者在 build 后修改原对象。",
      },
      {
        name: "immutable records + copyOf at ctor",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "StructureDefinition / StructurePointDefinition / TargetDescriptor / PredicateDescriptor 全部是 Java record。canonical 构造器对每个 List/Map/Set 调用 List.copyOf / Map.copyOf，得到真正不可变的快照。Builder 只在 build() 阶段活着，build 完就被 GC。",
    whyThisWay:
      "Record 的好处是 JDK 21 原生支持 + 自动 equals/hashCode/toString。copyOf 是 O(n) 但只在 build() 时调用一次，热路径完全走 record 引用，零拷贝。这意味着：(1) IR 可以安全地作为 Map key 用作 cache；(2) 多个消费者（compile/datagen/test）读到的是同一份字节；(3) Java 21 的 record 解构 + pattern matching 让校验代码更短。",
    realCost:
      "13 个 record 类型；构造器拒绝 null + defensive copy 是 CPU 开销；不支持的修改需要回 builder 重 build。但比起 mutable 带来的 debug 时间，划算。",
    codeSnippet: `public record StructureDefinition(
        ResourceLocation id,
        int schemaVersion,                     // IR_SCHEMA_VERSION = 1
        List<TransformDescriptor> transforms,
        List<StructurePointDefinition> points,
        List<ContextRuleDescriptor> contextRules,
        DiagnosticMetadata diagnostics) {

    public StructureDefinition {               // canonical ctor
        if (id == null) throw new IllegalArgumentException();
        transforms = List.copyOf(transforms);  // 真正不可变
        points = List.copyOf(points);
        contextRules = List.copyOf(contextRules);
    }
}

// 现在可以安全地做：
StructureDefinition cached = CACHE.computeIfAbsent(def, k -> compile(k));
// def 在 cache 期间不会被任何人修改，引用相等性稳定`,
    metrics: [
      { label: "cache key 稳定性", cumulus: "✓ 引用相等", vanilla: "✗ 需 deep equals" },
      { label: "线程安全", cumulus: "✓ 自动", vanilla: "需手动同步" },
      { label: "build 后修改", cumulus: "不可能", vanilla: "可能，bug 源" },
    ],
  },
  {
    id: "descriptor-based",
    icon: "FileJson",
    accent: "cyan",
    question: "为什么 Target / Predicate 是 open interface + typed Properties record？",
    oneLiner: "open interface 让 modder 扩展；typed record 让 IDE 和编译器帮你查错。",
    problem:
      "传统写法是 `interface BlockPredicate { boolean test(BlockState); }` 然后到处 new MyCustomPredicate()。这种 predicate 跑得动，但：(1) 不能序列化到 JSON（lambda/匿名类无法跨 reload）；(2) 不能跨 class loader（mod 热更新会爆）；(3) 不能在 datagen 阶段做静态校验。早期 Cumulus 用过 (ResourceLocation typeId, Map<String,String> properties) 兜底，但 properties map 没有类型信息——拼错字段名只能 runtime NPE。",
    alternatives: [
      {
        name: "interface + Java lambda",
        verdict: "rejected",
        rejectedBecause:
          "lambda 持有外部 this 引用，序列化靠 Java SerializableLambda（脆弱）；mod 更新后旧 lambda 失效。",
      },
      {
        name: "Serializable anonymous class",
        verdict: "rejected",
        rejectedBecause:
          "序列化体积大；NBT 反序列化需要类名 → Class.forName，class loader 复杂。",
      },
      {
        name: "open interface + typed Properties record (no sealed!)",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "Cumulus v2 用 (TargetType type, TargetProperties props) 取代旧的 (ResourceLocation typeId, Map<String,String> properties)。TargetType 是 open interface，modder 实现 TargetType 直接注册新类型；每个 TargetType 自带一个 record Properties，字段是强类型 (BlockEntityTargetType.Props 里 blockId / blockEntityId / nbt 都是 ResourceLocation / String 而非 Object)。PredicateType / TransformType / DiagnosticCode / MigrationOp / ContextKey 同样模式。",
    whyThisWay:
      "拒绝 sealed：modder 必须能注册新 TargetType/PredicateType/DiagnosticCode/MigrationOp 而不需要 Cumulus 改源码重新发版。Open interface + 中心 Registry（TargetTypes.register / PredicateTypes.register / DiagnosticCodes.register）给扩展性；typed record 给编译期检查。Wire format (JSON) 通过 PropertyExtractor / UnknownTargetType 桥接保留 v1 兼容——id 不在 registry 时 wrap 成 UnknownType 保留原 id。",
    realCost:
      "lookup 是一层 indirection（HashMap get），但 O(1)；wire format 多了一层 PropertyExtractor 转换；modder 注册新类型时需要写自己的 Properties record + 实现 TargetType 接口（5 行 boilerplate）。换来的是编译期类型安全 + 完全开放的扩展性。",
    codeSnippet: `// v2: open interface + typed Properties record
public final class BlockEntityTargetType implements TargetType {
    public static final ResourceLocation ID =
        ResourceLocation.parse("cumulus:block_entity");

    @Override public ResourceLocation id() { return ID; }
    @Override public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    // 强类型 properties — IDE 补全、编译期检查
    public record Props(
            ResourceLocation blockId,
            ResourceLocation blockEntityId,
            String nbt) implements TargetProperties { }
}

// Modder 注册自定义 target:
public final class MyCustomTargetType implements TargetType {
    public static final ResourceLocation ID =
        ResourceLocation.parse("mymod:custom");
    @Override public ResourceLocation id() { return ID; }
    @Override public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }
    public record Props(String biomeTag) implements TargetProperties { }
}

// mod setup:
TargetTypes.register(new MyCustomTargetType());

// 不再写：
// public class MyControllerPredicate implements BlockPredicate { ... }  // 无法序列化
// 或：new TargetDescriptor(ResourceLocation.parse("cumulus:block_entity"),
//                          Map.of("block", "...", "nbt", "..."))  // Map 无类型`,
    metrics: [
      { label: "序列化", cumulus: "✓ PropertyExtractor 双向", vanilla: "✗ 不支持" },
      { label: "扩展性", cumulus: "✓ open interface + Registry", vanilla: "△ sealed 限制" },
      { label: "类型安全", cumulus: "✓ 编译期", vanilla: "✗ runtime NPE" },
      { label: "热重载", cumulus: "✓ typeId 解析", vanilla: "✗ 类已加载" },
      { label: "datagen 校验", cumulus: "✓ typeId 静态查", vanilla: "✗ 需运行时" },
    ],
  },
  {
    id: "request-not-load",
    icon: "Hourglass",
    accent: "amber",
    question: '为什么未加载区块是 "request" 而不是 "force load" ？',
    oneLiner: "force-load 一个区块 = 50ms+ tick 浪费；request 让引擎保持预算。",
    problem:
      "玩家在机器附近活动，远处的 'remote_sensor' point 在未加载的区块里。如果引擎强制 force-load 那个区块：单个 chunk gen 50ms+，导致 server tick 跌 0 TPS；可被恶意利用（让服务器一直 force load 远程区块）；和原版 chunk-loading 模型冲突。",
    alternatives: [
      {
        name: "force-load target chunk",
        verdict: "rejected",
        rejectedBecause:
          "50ms+ per chunk gen；攻击者可以触发大量 force load 制造 lag；与 vanilla chunk ticket 系统冲突。",
      },
      {
        name: "skip target until loaded",
        verdict: "rejected",
        rejectedBecause:
          "结构永远不完整，玩家放好方块也通不过校验；机器永远停机。",
      },
      {
        name: "request + retry on chunk load event",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "TargetSnapshotSource 接口允许返回 'absent'，不是 'null block' 而是 '数据不在'。StructureCheckExecutor 看到 absent 必需 point，不报 INVALID 而是发 DataRequest，状态 PENDING。RequestLedger 记录请求，监听 chunk load 事件，区块到了再 markDirty 触发重检。",
    whyThisWay:
      "把'数据不存在'和'数据是 air'区分开是关键。Minecraft 自己也是这套语义：ChunkAccess 可能为 null，但 Level#getBlockState 永远不抛。Cumulus 沿用这个边界：snapshot source 可以是 absent，executor 把 absent 翻译成 request。结构状态变成三态：VALID / INVALID / PENDING，机器逻辑可以基于 PENDING 显示 'loading...' 而不是 fail。",
    realCost:
      "需要 RequestLedger + chunk load 监听 + PENDING 状态在 InstanceDelta 里传播。机器作者要处理三态。但换来的是：服务器 tick budget 永不被 chunk gen 打穿。",
    codeSnippet: `// TargetSnapshotSource 是这样设计的：
public interface TargetSnapshotSource {
    /**
     * @return Optional.empty() if data is unavailable
     *         (e.g. chunk not loaded). NOT Optional.of(air).
     */
    Optional<TargetSnapshot> read(TargetReadRequest req);
}

// Executor 看到必需 point 缺数据：
if (snapshot.isEmpty() && point.requirement() == REQUIRED) {
    requests.add(new DataRequest(point, candidate, sectionKey));
    status = InstanceStatus.PENDING;          // 不是 INVALID
    diagnostics.add(UNLOADED_AREA);            // severity = INFO
    continue;
}

// Engine 把 request 入 ledger：
RequestLedger ledger = engine.ledger();
ledger.onChunkLoad(sectionKey, () -> engine.markDirty(sectionKey));`,
    metrics: [
      { label: "tick budget 保护", cumulus: "✓ 50ms 严格", vanilla: "✗ 可被穿" },
      { label: "DDoS 抵抗", cumulus: "✓ 不能远程触发", vanilla: "✗ 可触发" },
      { label: "玩家体验", cumulus: "显示 loading", vanilla: "假阳性 fail" },
    ],
  },
  {
    id: "precompute-transforms",
    icon: "BoxSelect",
    accent: "magenta",
    question: "为什么 transform 候选要在编译时预算，不运行时算？",
    oneLiner: "O(N×T) 矩阵乘法移出热路径，运行时只剩 O(1) lookup。",
    problem:
      "horizontal facing 4 候选 + identity + rotate90 = 6 个 transform。每个 transform 要对 N 个 point 应用旋转矩阵 + AABB 变换。如果每 tick 重算：100 个 point × 6 transform = 600 次 matrix op，每次还涉及浮点数误差。1 个机器这样还可以，但 1000 个 tracked instance 同时检查就爆炸。",
    alternatives: [
      {
        name: "runtime matrix per tick",
        verdict: "rejected",
        rejectedBecause:
          "1000 instance × 100 point × 6 transform = 600k 次 op/tick，单帧 ≥5ms 浮点计算。",
      },
      {
        name: "cache by (transform, point) globally",
        verdict: "rejected",
        rejectedBecause:
          "全局缓存命中率低（不同结构不同 point 列表），且需要失效机制。",
      },
      {
        name: "precompute per CheckPlan",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "StructureCheckPlan 在 build() 时为每个 transform 候选预计算所有 point 的世界 AABB（PointBound）+ region 边界（RegionBound）+ footprint sections。运行时 fastTick 直接按 candidate 顺序 lookup，无需任何矩阵计算。",
    whyThisWay:
      "transform 是 declarative 的（identity/horizontalFacing/rotateY90/mirrorX），数量有限且静态。把几何预算挪到编译期等价于把 hot path 从 'compute' 降级为 'memory access'。CPU cache 友好（连续 PointBound[] 数组），SIMD 也容易。",
    realCost:
      "每个 plan 多占内存（N×T 个 PointBound）。但 plan 是 per-definition（不是 per-instance），全局只有 definition 数量个 plan，开销可忽略。换来 fastTick 是 O(candidate) 短路。",
    codeSnippet: `// build 一次：
StructureCheckPlan plan = StructureCheckPlan.compile(definition);
// 内部：
for (TransformDescriptor t : definition.transforms()) {
    var candidate = new CheckPlanCandidate(t);
    for (StructurePointDefinition p : definition.points()) {
        RelativeBlockPos transformed = t.apply(p.location());
        candidate.bounds().add(new PointBound(
            transformed, p.target(), p.predicates()));
    }
    candidate.footprint = computeSections(candidate.bounds);
    candidates.add(candidate);
}

// 每 tick 仅 lookup：
for (var candidate : plan.candidates()) {
    if (fastCheckAll(candidate, snapshot)) {
        return VALID;            // 短路，剩下 candidate 跳过
    }
}`,
    metrics: [
      { label: "fastTick CPU", cumulus: "≈ O(候选数)", vanilla: "O(N×T) 矩阵" },
      { label: "1000 instance 成本", cumulus: "~0.3ms", vanilla: "~5ms" },
      { label: "浮点误差", cumulus: "无（编译期）", vanilla: "累积" },
    ],
  },
  {
    id: "dual-tick-path",
    icon: "GitBranch",
    accent: "lime",
    question: "为什么 fastTick 和 diagnostic tick 是两条独立路径？",
    oneLiner: "99% 时间结构是有效的，没必要每次都建 DiagnosticGraph。",
    problem:
      "DiagnosticGraph 节点带 code/severity/evidence/fixHint/edge，一个失败结构可能产生 20+ 节点 + 30+ 边 = 几 KB allocation。一个服务器 2000 个 tracked instance 每 tick 全跑 diagnostic path = 8MB+/tick GC pressure，10 秒后 GC 抖动到玩家感知。",
    alternatives: [
      {
        name: "always full diagnostic",
        verdict: "rejected",
        rejectedBecause:
          "GC pressure 实测在 1500 instance 起显著；玩家感知卡顿。",
      },
      {
        name: "lazy diagnostic on demand",
        verdict: "rejected",
        rejectedBecause:
          "玩家需要立刻看到 'why invalid'，再触发诊断等于延迟反馈。",
      },
      {
        name: "fast path (status only) + on-demand diag",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "fastTick 跑相同的候选短路逻辑但只返回 InstanceStatus + 评估计数；tick() 才分配 DiagnosticGraph。生产 server 默认 fastTick，状态 INVALID 后才请求一次 diag tick（一次性）获取详情给玩家。",
    whyThisWay:
      "JVM JIT 对不带 allocation 的 hot loop 优化极好；带 allocation 的 loop 即使逃逸分析能消除也不如纯标量版本。两条路径共享 PredicateMatcher / TargetSnapshotSource，差异只在结果对象。代价是 fastCheck/check 两个 executor 方法，但代码生成器友好。",
    realCost:
      "代码重复（fastCheck 和 check 各一套短路逻辑），但逻辑短。维护时需要同时改两处，CI 强制要求两边结果一致。",
    codeSnippet: `// 默认 server tick：
RuntimeFrameResult frame = engine.fastTick(serverTick, BUDGET);
//  → 仅返回 status + counters，零 graph allocation

// 玩家右键询问 'why invalid'：
if (anyInvalid(frame)) {
    RuntimeFrameResult diag = engine.tick(serverTick, BUDGET);
    //  → 这次分配 DiagnosticGraph
    showPlayerOverlay(diag.deltas().get(0).diagnostics());
}

// fastTick 实测数据（同套测试）：
//   500 instance · 100 point · 6 candidates
//   fastTick: 0.8ms · 0 allocation
//   tick:     2.3ms · 47 KB allocation`,
    metrics: [
      { label: "0 allocation", cumulus: "fastTick ✓", vanilla: "无法做到" },
      { label: "INVALID 详情", cumulus: "按需触发", vanilla: "始终持有" },
      { label: "1000 inst cost", cumulus: "~1.6ms", vanilla: "~5ms + GC" },
    ],
  },
  {
    id: "diagnostic-graph",
    icon: "GitGraph",
    accent: "rose",
    question: "为什么诊断是图而不是 string 列表？",
    oneLiner: "失败是一个事件树，不是一句话。",
    problem:
      "传统多方块失败诊断是 'Multiblock incomplete'。这句话丢失了：(1) 哪个 point 失败；(2) 期望什么实际是什么；(3) 哪个 transform 候选被试过；(4) 玩家应该怎么修。开发者要查日志，玩家要盲猜。",
    alternatives: [
      {
        name: "string error messages",
        verdict: "rejected",
        rejectedBecause:
          "无法程序化处理；本地化需要重新拼字符串；无法表达 'rule 包含多个 point 失败' 的嵌套关系。",
      },
      {
        name: "structured exceptions",
        verdict: "rejected",
        rejectedBecause:
          "stack trace 是线性不是树；evidence/fixHint 没有合适位置；玩家看到 stack trace 反而困惑。",
      },
      {
        name: "DiagnosticGraph (DAG)",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "DiagnosticGraph 节点 DiagnosticNode 带 code（11 标准码）/ severity / 本地化 key / evidence / fixHint / source path。DiagnosticEdge 连接 rule → predicate → evidence，形成 DAG。同一张图投影成 4 种格式：developer log / player message / visualization overlay / datagen validation。",
    whyThisWay:
      "失败本身就是结构化的：'machine invalid' 包含 'casing rule failed' + 'energy rule failed'，casing rule 又包含 'point_n missing' + 'point_e wrong tag'，point_e 又有 evidence 'actual: stone, expected: heat_casing tag'。这套结构天然是树（实际是 DAG 因为多个 rule 可能引用同一 evidence）。线性 string 强行 flatten 就丢失了。",
    realCost:
      "节点/边 allocation 比单 string 多 50× 字节。所以默认 fastTick 不构建 graph，只在 INVALID 后按需构建。构建后 projection 到 4 种格式是 cheap 的（只是遍历）。",
    codeSnippet: `// 节点：
public record DiagnosticNode(
    String code,                    // MISSING_TARGET, PREDICATE_FAILED, ...
    Severity severity,              // ERROR / WARN / INFO
    String localizationKey,         // i18n 入口
    DiagnosticEvidence evidence,    // target + actual + expected
    FixHint fixHint,                // 自动构造提示
    String sourcePath,              // rule#2 > point#casing_n
    List<DiagnosticEdge> edges) { } // → 子节点

// 投影 1: 开发者日志
graph.projectStructured(logger::info);

// 投影 2: 玩家提示（带 i18n）
graph.projectLocalized(player::sendSystemMessage);

// 投影 3: 世界内 overlay
graph.projectVisualization(overlay::addElement);

// 投影 4: datagen 构建时失败
graph.projectDatagen(buildException::add);`,
    metrics: [
      { label: "信息密度", cumulus: "tree / 11 codes", vanilla: "1 string" },
      { label: "本地化", cumulus: "key-based", vanilla: "重新拼串" },
      { label: "可程序化", cumulus: "✓", vanilla: "✗" },
    ],
  },
  {
    id: "dirty-section-index",
    icon: "LayoutGrid",
    accent: "cyan",
    question: "为什么脏追踪是 section (16³) 粒度，不是 block 粒度？",
    oneLiner: "1 个结构 = 50 point，section 索引让 50× handler 变 1× section handler。",
    problem:
      "如果每个 point 都注册一个 BlockEvent listener：一个 100 point 的结构 × 1000 个 instance = 10 万 listener。NeoForge event bus 每次方块事件要遍历这 10 万 listener，单事件几毫秒。",
    alternatives: [
      {
        name: "per-block listener",
        verdict: "rejected",
        rejectedBecause:
          "O(N×M) listener 数（N point × M instance）；event bus 单事件成本爆炸。",
      },
      {
        name: "global BlockEvent + filter",
        verdict: "rejected",
        rejectedBecause:
          "每次都要遍历所有 instance 找谁受影响，O(M) 复杂度，仍然没解决 1000 instance 的问题。",
      },
      {
        name: "section (16³) → instance index",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "SectionKey 是 BlockPos >> 4 的 3-tuple，对应 Minecraft 自己的 chunk section。结构编译时把 footprint 转成 Set<SectionKey>。RuntimeScheduler 维护 sectionKey → Set<StructureInstanceKey> 反向索引。BlockEvent 触发时 O(1) 查 section，找到 instance，markDirty。",
    whyThisWay:
      "Minecraft 自己就用 chunk section 做内部索引（每 16×16×16 一个 SectionStorage），cumulus 与之对齐有 3 个好处：(1) section 边界和 Minecraft 一致，可以直接复用 ChunkEvent；(2) 同一 section 内多个结构变动只触发 1 次 markDirty（去重）；(3) section 是热数据局部性单元，CPU cache 友好。",
    realCost:
      "同 section 内非 footprint block 变化也会触发 markDirty（false positive），但下一次 fastCheck 只花 0.1ms 确认无效就清除。换来的 O(1) listener 路由远超这点开销。",
    codeSnippet: `// 编译时：
StructureRuntimePlan plan = compiler.compile(definition);
Set<SectionKey> footprint = plan.footprint();
// 比如机器是 (10,64,10) 到 (12,66,12)：
//   section key 集合 = {(0,4,0)}        // 1 个 section
// 大机器 (10,64,10) 到 (50,80,50)：
//   section key 集合 = {(0..3, 4..5, 0..3)}  // 4×2×4 = 32 个

// 运行时反向索引：
sectionIndex.put(sectionKey, instanceKey);

// BlockEvent 进来：
@SubscribeEvent
void onBlock(BlockEvent event) {
    SectionKey key = SectionKey.fromBlock(event.getPos());
    Set<StructureInstanceKey> affected = sectionIndex.get(key);
    if (affected != null) {
        affected.forEach(engine::markDirty);
    }
}`,
    metrics: [
      { label: "listener 数量", cumulus: "section 数", vanilla: "N×M" },
      { label: "事件路由", cumulus: "O(1) HashMap", vanilla: "O(N) 遍历" },
      { label: "false positive", cumulus: "~30%", vanilla: "0%" },
    ],
  },
  {
    id: "substructure-composition",
    icon: "Boxes",
    accent: "brand",
    question: "为什么子结构是引用 + 运行时复合，而不是 DSL 期宏展开？",
    oneLiner: "宏展开抹平所有权，复合保留 child occurrence 独立性。",
    problem:
      "一个 'factory' 嵌入了 'arm' 子结构，arm 又嵌入了 'tool'。如果在 DSL 期宏展开：所有 point 拍平到一个 flat 列表，每个 point 都是 factory 的直接成员。后果：(1) tool 失败无法单独报告；(2) 同一个 arm 被两个 factory claim 时无法仲裁；(3) 玩家想知道 'left arm 是否有效' 无法回答。",
    alternatives: [
      {
        name: "macro expansion (flatten at build)",
        verdict: "rejected",
        rejectedBecause:
          "丢失 child occurrence 边界；无法独立 publish child InstanceDelta；多父所有权无法检测。",
      },
      {
        name: "true nested Pattern objects",
        verdict: "rejected",
        rejectedBecause:
          "需要递归 check API，运行时复杂；不便于 footprint 推导；失去 plan 缓存价值。",
      },
      {
        name: "runtime composition with occurrence id",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "DSL 阶段 substructure 只保留引用 (id + offset)，不展开。运行时 SubstructureRuntimeComposer 把 child 的 point 按 cumulative offset 拼到 parent 上，但每个 child occurrence 拿到独立 occurrenceId。每个 occurrence 独立 check、独立 delta、独立 diagnostic。",
    whyThisWay:
      "子结构的关系是 runtime 的，不是 compile-time 的。同一种 arm 可以被多个 factory 引用（共享），同一 arm occurrence 也可以被多个父结构 claim（必须冲突检测）。只有 runtime 才能做这件事。代价是 composer 比 flatten 复杂，但保留 child autonomy 的好处远大。",
    realCost:
      "composer 比简单 flatten 多 2-3 倍代码量；child occurrence 上下文镜像需要 prefix capture name（'tool.spell' → 'substructure.<occid>.context.tool.spell'）；父级 context 体积膨胀。",
    codeSnippet: `// DSL 阶段 (零展开)：
.component(Components.substructure("left_arm",
    ResourceLocation.parse("cumulus:arm"),
    RelativeBlockPos.of(-3, 0, 0)))

// 运行时 SubstructureRuntimeComposer：
ComposedDefinition composed = composer.compose(factoryDef);
// composed.points() 包含：
//   factory.controller        (offset 0,0,0)
//   arm_left.shoulder         (offset -3,0,0)
//   arm_left.elbow            (offset -3,1,0)
//   arm_left.tool.spell_book  (offset -3,2,0)

// 但 occurrence 仍然独立：
for (var occ : composed.occurrences()) {
    InstanceDelta childDelta = checkChild(occ);
    if (childDelta.status() == INVALID) {
        // child 失败不直接 fail parent，
        // 而是镜像到 parent context 让 parent 自己决定
        parentContext.set("substructure." + occ.id() + ".status",
                          childDelta.status());
    }
}`,
    metrics: [
      { label: "child 独立 delta", cumulus: "✓", vanilla: "✗ (flatten)" },
      { label: "所有权仲裁", cumulus: "✓ claim state", vanilla: "✗" },
      { label: "复用 child def", cumulus: "✓", vanilla: "✗ (展开)" },
    ],
  },
  {
    id: "java-dsl-first",
    icon: "Code2",
    accent: "amber",
    question: "为什么是 Java DSL 而不是 JSON / YAML 数据驱动？",
    oneLiner: "Java 给 IDE/类型检查/重构；JSON 是产物不是源。",
    problem:
      "如果结构定义是 JSON：作者写 'pount' 而不是 'point'，构建时才报错；改 PredicateDescriptor 字段名要全项目搜索；IDE 看不到字段补全；refactor rename 跨 JSON 不工作；测试无法 import 一个 StructureDefinition。",
    alternatives: [
      {
        name: "JSON-first (datapack 风格)",
        verdict: "rejected",
        rejectedBecause:
          "无类型检查；字段拼写错误只在运行时暴露；IDE 补全差；测试困难。",
      },
      {
        name: "Kotlin DSL",
        verdict: "rejected",
        rejectedBecause:
          "NeoForge 生态主流仍是 Java；引入 Kotlin 增加依赖、增 class loader 风险。",
      },
      {
        name: "Java DSL + JSON datagen export",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "Java DSL 是 source of truth。build() 调用 StructureDefinitionValidator 做编译期校验（duplicate id、duplicate capture、spatial conflict）。RuntimeAssetExporter 把 IR 转 JSON 写到 datagen 输出。RuntimeAssetJsonLoader 在服务器 reload 时解析 JSON，重建等价 IR。",
    whyThisWay:
      "Java 21 record + builder pattern + 静态 factory 方法（Structures.define / Components.box）的组合，让 IDE 自动补全完整链路。重构 rename 一个 point field 全项目秒级生效。单元测试可以直接 new StructureDefinition(...)。JSON 是 'release artifact'，让玩家可以热重载/修改而无需 mod 源码。",
    realCost:
      "改定义需要重新编译 mod；玩家无法直接改 Java 源码（但可以改 JSON）。两全方案：modder 写 Java，玩家改 JSON，两边都开心。",
    codeSnippet: `// Modder 在 Java 里写：
public static final StructureDefinition MACHINE =
    Structures.define(id("machine"))
        .point("controller", at(0,1,0), Targets.block()
            .where(PredicateDescriptors.id("cumulus:controller"))
            .capture("controller"))
        .build();
// IDE 自动补全 .point()/.component()/.context()
// Rename "controller" 全项目秒级 refactor

// Datagen 时输出：
runtimeAssetProvider.add(MACHINE);
// → 生成 data/cumulus/structures/machine.json

// 玩家在 datapack 里覆盖：
// data/my_pack/cumulus/structures/machine.json
// 服务器 reload 时自动生效，无需 mod 重编译`,
    metrics: [
      { label: "类型安全", cumulus: "✓ Java 编译", vanilla: "✗ JSON 拼写" },
      { label: "IDE 补全", cumulus: "✓ 完整链路", vanilla: "△ schema 限定" },
      { label: "玩家可改", cumulus: "✓ 通过 JSON", vanilla: "✓" },
    ],
  },
  {
    id: "versioned-migration",
    icon: "GitPullRequestArrow",
    accent: "lime",
    question: "为什么 RuntimeStructureContext 是带 revision 的、可迁移的？",
    oneLiner: "mod 升级时玩家机器不会失效，context key 自动迁移。",
    problem:
      "mod v1.0 用 'facing' 作为 context key，v1.1 改成 'orientation'。升级后所有已组装机器的 BlockEntity NBT 里存的还是 'facing'，机器逻辑读 'orientation' 拿到 null 直接 fail。",
    alternatives: [
      {
        name: "hardcode 双字段读取",
        verdict: "rejected",
        rejectedBecause:
          "代码里到处 if (ctx.has('facing')) use facing else use orientation；多年累积变成屎山。",
      },
      {
        name: "reset all machines on upgrade",
        verdict: "rejected",
        rejectedBecause:
          "玩家一周搭建的工厂瞬间报废，体验灾难。",
      },
      {
        name: "StructureVersionDescriptor + migration chain",
        verdict: "accepted",
        rejectedBecause: "",
      },
    ],
    solution:
      "StructureDefinition 持有 StructureVersionDescriptor(schema, migrateTo)。ContextMigrationExecutor 注册 'facing → orientation' 迁移规则。check 完成后，如果当前 context revision 旧，按链式规则迁移 keys/values，输出新 revision 的 context。机器逻辑只看新 revision。",
    whyThisWay:
      "Minecraft 自己的 DataFixer 就是这套语义（区块 NBT 升级）。Cumulus 沿用：定义版本是单调递增的，每次升级配一个 migration 步骤。代价是首次 check 时多一步 migration（cheap），换来机器永远存活。",
    realCost:
      "每个 schema bump 需要写一个 MigrationStep（10 行代码）；migration 失败需要 diagnostic；玩家旧版 mod 装新版数据包要 fallback 处理。",
    codeSnippet: `// v1 definition:
.version(new StructureVersionDescriptor(1, null))

// v2 definition (with migration):
.version(new StructureVersionDescriptor(2,
    ResourceLocation.parse("cumulus:machine/v2")))

// 注册 migration：
ContextMigrationExecutor.register(
    "cumulus:machine",
    new MigrationStep(1, 2) {
        @Override public void migrate(RuntimeStructureContext ctx) {
            // 'facing' → 'orientation'
            Object facing = ctx.get("facing");
            if (facing != null) {
                ctx.set("orientation", facing);
                ctx.remove("facing");
            }
        }
    });

// 玩家 v1 时代组装的机器：
//   BlockEntity NBT: { facing: 'north', controller: ... }
// v2 mod 加载后第一次 tick：
//   migration 1→2 执行 → context = { orientation: 'north' }
//   机器逻辑 ctx.get("orientation") 拿到 'north'，正常工作`,
    metrics: [
      { label: "升级机器存活", cumulus: "✓ 自动迁移", vanilla: "✗ 重置" },
      { label: "代码积累", cumulus: "线性 MigrationStep", vanilla: "if 蛇" },
      { label: "可回溯", cumulus: "✓ 版本号", vanilla: "✗ 无版本" },
    ],
  },
];
