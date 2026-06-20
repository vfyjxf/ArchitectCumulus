import { motion, AnimatePresence } from "framer-motion";
import { useState } from "react";
import { ArrowRight, ChevronRight } from "lucide-react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { cn } from "@/lib/utils";

type StageId = "dsl" | "ir" | "assets" | "plan" | "runtime" | "output";

const STAGES: {
  id: StageId;
  num: string;
  label: string;
  hint: string;
  color: string;
  badge: string;
}[] = [
  { id: "dsl", num: "1", label: "Definition DSL", hint: "Structures.define(...).build()", color: "from-brand-400 to-brand-600", badge: "author-facing" },
  { id: "ir", num: "2", label: "StructureDefinition IR", hint: "immutable records · schema v2", color: "from-cyan-400 to-cyan-600", badge: "immutable" },
  { id: "assets", num: "3", label: "Runtime / Debug Assets", hint: "data/*/cumulus/structures/*.json", color: "from-pink-400 to-pink-600", badge: "datagen" },
  { id: "plan", num: "4", label: "Check Plan", hint: "precomputed bounds × transform", color: "from-amber-400 to-amber-600", badge: "compiled" },
  { id: "runtime", num: "5", label: "RuntimeCheckEngine", hint: "dirty sections · budget · delta", color: "from-violet-400 to-violet-600", badge: "scheduler" },
  { id: "output", num: "6", label: "Context · Diagnostic · Ghost", hint: "RuntimeStructureContext + DiagnosticGraph", color: "from-lime-400 to-lime-600", badge: "consumers" },
];

const STAGE_DATA: Record<StageId, { code: string; title: string; out: { kind: string; value: string }[] }> = {
  dsl: {
    title: "DSL 代码（作者写）",
    code: `var definition = Structures.define(
        ResourceLocation.parse("cumulus:machine"))
    .transforms(
        Transforms.identity(),
        Transforms.horizontalFacing(),
        Transforms.rotateY90())
    .point("controller",
        RelativeBlockPos.of(0, 1, 0),
        Targets.block()
            .where(PredicateDescriptors.id("cumulus:controller"))
            .capture("controller"))
    .component(Components.hollowBox("casing",
        RelativeBlockPos.of(-1, 0, -1),
        RelativeBlockPos.of(1, 2, 1))
        .target(Targets.block())
        .where(PredicateDescriptors.id("cumulus:casing")))
    .context(ContextRuleDescriptor.derive(
        ContextKey.string("facing"),
        ContextSources.capture("controller").property("facing")))
    .version(new StructureVersionDescriptor(2,
        ResourceLocation.parse("cumulus:machine/v2")))
    .build();`,
    out: [
      { kind: "input", value: "Java 21 source" },
      { kind: "builder", value: "StructureDefinitionBuilder (mutable)" },
      { kind: "expansion", value: "Components emit StructurePointDefinitions" },
      { kind: "validation", value: "StructureDefinitionValidator" },
      { kind: "output type", value: "StructureDefinition (immutable)" },
    ],
  },
  ir: {
    title: "不可变 IR（编译结果）",
    code: `record StructureDefinition(
    ResourceLocation id,
    int schemaVersion,                // = 2 (irSchemaVersion)
    List<TransformDescriptor> transforms,
    List<StructurePointDefinition> points,
    List<ContextRuleDescriptor> contextRules,
    DiagnosticMetadata diagnostics) {

    public static final int irSchemaVersion = 2;
    // canonical 6-arg constructor for schema-version compat
    // convenience 5-arg constructor used by builders
}

record StructurePointDefinition(
    StructurePointId id,
    RelativeBlockPos location,
    TargetDescriptor target,
    List<PredicateDescriptor> predicates,
    PointRequirement requirement,    // required / optional
    Set<String> captures,
    Set<String> groups,
    DiagnosticMetadata diagnostics) { }`,
    out: [
      { kind: "schema", value: "irSchemaVersion = 2" },
      { kind: "records", value: "13 immutable record types" },
      { kind: "validation", value: "duplicate id / capture / spatial" },
      { kind: "guarantee", value: "Map.copyOf / List.copyOf at ctor" },
      { kind: "consumers", value: "compile · datagen · test" },
    ],
  },
  assets: {
    title: "Runtime Asset（JSON）",
    code: `{
  "id": "cumulus:machine",
  "schema_version": 2,
  "transforms": [
    {"id": "cumulus:identity"},
    {"id": "cumulus:horizontal_facing"},
    {"id": "cumulus:rotate_y_90"}
  ],
  "points": [
    {
      "id": "controller",
      "location": "0,1,0",
      "target": {"type": "cumulus:block"},
      "predicates": [{"id": "cumulus:controller"}],
      "captures": ["controller"],
      "requirement": "required"
    },
    ...
  ],
  "context_rules": [
    {"name": "facing", "expression": "controller.facing"}
  ],
  "version": {"schema": 2, "migrate_to": "cumulus:machine/v2"}
}`,
    out: [
      { kind: "format", value: "JSON (deterministic ordering)" },
      { kind: "location", value: "data/<ns>/cumulus/structures/*.json" },
      { kind: "loader", value: "RuntimeAssetJsonLoader" },
      { kind: "repository", value: "RuntimeAssetRepository (indexed by id)" },
      { kind: "reload", value: "RuntimeAssetReloadListener (server)" },
    ],
  },
  plan: {
    title: "Check Plan（编译后的几何）",
    code: `class StructureCheckPlan {
    // precomputed per transform candidate
    List<CheckPlanCandidate> candidates;
    // candidates hold:
    record CheckPlanCandidate(
        TransformDescriptor transform,
        List<PointBound> pointBounds,    // AABB per point
        List<RegionBound> regionBounds,
        Set<SectionKey> footprint) { }
}

class FastCheckResult {
    InstanceStatus status;             // VALID / INVALID / PENDING
    long evaluationsShortcircuited;
    long candidateIndex;               // first valid candidate
}

// short-circuit: stop at first valid candidate,
// skip diagnostic graph allocation`,
    out: [
      { kind: "input", value: "StructureDefinition + transform list" },
      { kind: "precompute", value: "AABB bounds per candidate" },
      { kind: "footprint", value: "Set<SectionKey> sections touched" },
      { kind: "fast path", value: "FastCheckResult (no diagnostics)" },
      { kind: "diag path", value: "StructureCheckExecutor.check(...)" },
    ],
  },
  runtime: {
    title: "Runtime Engine（每 tick）",
    code: `RuntimeCheckEngine engine = new RuntimeCheckEngine()
    .register(definition)
    .track(definition.id(), anchorPos, snapshotSource);

// mark dirty when world changes
engine.markDirty(SectionKey.fromBlock(pos));

// status-only fast tick (default for production)
RuntimeFrameResult fast = engine.fastTick(serverTick, 8);

// on-demand diagnostic tick
RuntimeFrameResult diag = engine.tick(serverTick, 8);

// frame contains:
//   - List<InstanceDelta> deltas
//   - List<DataRequest> requests
//   - DiagnosticGraph (only on diag path)`,
    out: [
      { kind: "trigger", value: "ServerTickEvent.Post (NeoForge)" },
      { kind: "budget", value: "per-tick check count cap" },
      { kind: "scheduler", value: "RuntimeScheduler (dirty section index)" },
      { kind: "fast tick", value: "status-only, no diagnostics" },
      { kind: "diag tick", value: "on-demand, full DiagnosticGraph" },
    ],
  },
  output: {
    title: "下游消费（多方块机器/编辑器/datagen）",
    code: `// 1. RuntimeStructureContext — exposed to machine logic
record RuntimeStructureContext(
    long revision,
    Map<String, Object> values  // controller, facing, controller_id...
) { }

// 2. InstanceDelta — change notification
record InstanceDelta(
    StructureInstanceKey key,
    InstanceStatus status,
    RuntimeStructureContext context,
    DiagnosticGraph diagnostics,
    Set<String> changedKeys) { }

// 3. GhostPreviewModel — in-game editor
record GhostPreviewModel(
    List<GhostPreviewElement> boxes,
    Map<String, RenderStyle> styles) { }

// 4. DataDrivenStructureAsset — datagen output
record DataDrivenStructureAsset(
    ResourceLocation id,
    StructureDatagenSpec spec) { }`,
    out: [
      { kind: "machine", value: "RuntimeStructureContext + InstanceDelta" },
      { kind: "diagnostics", value: "DiagnosticGraph (projectable)" },
      { kind: "editor", value: "GhostPreviewModel + RenderPlan" },
      { kind: "datagen", value: "DataDrivenStructureAsset" },
      { kind: "build", value: "FixHint → BuildPlanExecutor" },
    ],
  },
};

export function Pipeline() {
  const [active, setActive] = useState<StageId>("dsl");
  const data = STAGE_DATA[active];
  const activeIdx = STAGES.findIndex((s) => s.id === active);

  return (
    <Section
      id="pipeline"
      index="01"
      title={
        <>
          分层管线：
          <span className="text-white/50"> DSL → IR → 计划 → 运行时 → 诊断</span>
        </>
      }
      lede={
        <>
          这是整个库的脊椎。每一层只接受上一层的输出，
          <strong className="text-white/80"> Java DSL 是唯一真源</strong>，
          之后的 IR / 资产 / 计划 / 上下文 / 诊断都从同一条不可变记录推导。
          点击下方任意阶段，观察每一层吐出的对象类型。
        </>
      }
    >
      {/* pipeline rail */}
      <div className="relative mb-10">
        <div className="hidden md:flex items-center justify-between gap-1 mb-3">
          {STAGES.map((s, i) => (
            <div key={s.id} className="flex items-center flex-1 last:flex-none">
              <button
                onClick={() => setActive(s.id)}
                className={cn(
                  "group relative flex-1 rounded-2xl p-4 text-left transition-all border",
                  active === s.id
                    ? "bg-white/[0.06] border-brand-400/30 shadow-[0_8px_30px_-12px_rgba(124,92,255,0.5)]"
                    : "bg-white/[0.02] border-white/5 hover:bg-white/[0.04]"
                )}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span
                    className={cn(
                      "w-7 h-7 rounded-lg flex items-center justify-center text-[12px] font-bold bg-gradient-to-br text-white",
                      s.color
                    )}
                  >
                    {s.num}
                  </span>
                  <span className="text-[10.5px] uppercase tracking-wider text-white/40 font-mono">
                    {s.badge}
                  </span>
                </div>
                <div className="text-[13.5px] font-semibold text-white/90 mb-1">
                  {s.label}
                </div>
                <div className="text-[10.5px] text-white/40 font-mono leading-tight">
                  {s.hint}
                </div>
                {active === s.id && (
                  <motion.div
                    layoutId="active-stage"
                    className="absolute -bottom-px left-3 right-3 h-0.5 bg-gradient-to-r from-brand-400 to-cyan2 rounded-full"
                  />
                )}
              </button>
              {i < STAGES.length - 1 && (
                <ChevronRight className="w-4 h-4 text-white/20 mx-1 shrink-0" />
              )}
            </div>
          ))}
        </div>

        {/* mobile version: horizontal scroll */}
        <div className="md:hidden flex gap-2 overflow-x-auto pb-2 -mx-3 px-3 scrollbar-thin">
          {STAGES.map((s) => (
            <button
              key={s.id}
              onClick={() => setActive(s.id)}
              className={cn(
                "shrink-0 rounded-xl p-3 border text-left min-w-[160px]",
                active === s.id
                  ? "bg-white/[0.06] border-brand-400/30"
                  : "bg-white/[0.02] border-white/5"
              )}
            >
              <div className="flex items-center gap-1.5 mb-1">
                <span className={cn("w-5 h-5 rounded-md bg-gradient-to-br text-[10px] font-bold flex items-center justify-center text-white", s.color)}>
                  {s.num}
                </span>
                <span className="text-[10px] uppercase text-white/40 font-mono">{s.badge}</span>
              </div>
              <div className="text-[12px] font-semibold text-white/90">{s.label}</div>
            </button>
          ))}
        </div>
      </div>

      {/* content */}
      <AnimatePresence mode="wait">
        <motion.div
          key={active}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -12 }}
          transition={{ duration: 0.25 }}
          className="grid lg:grid-cols-[1.4fr,1fr] gap-5"
        >
          <div>
            <div className="flex items-center gap-2 mb-3">
              <Badge tone="brand">阶段 {activeIdx + 1} / 6</Badge>
              <span className="text-[13px] text-white/70">{data.title}</span>
            </div>
            <CodeBlock code={data.code} title={`${active}.java`} maxHeight="520px" />
          </div>

          <div>
            <div className="flex items-center gap-2 mb-3">
              <Badge tone="cyan">本阶段产物</Badge>
              <span className="text-[13px] text-white/70">运行后真实输出</span>
            </div>
            <div className="rounded-2xl border border-white/8 bg-ink-950/70 p-4 space-y-2">
              {data.out.map((o, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.08 }}
                  className="flex items-start gap-3 p-3 rounded-lg bg-white/[0.02] border border-white/5"
                >
                  <div className="w-1.5 h-1.5 rounded-full bg-brand-400 mt-1.5 shrink-0" />
                  <div>
                    <div className="text-[10.5px] uppercase tracking-wider text-brand-300/70 font-mono">
                      {o.kind}
                    </div>
                    <div className="text-[13px] text-white/85 font-mono">
                      {o.value}
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>

            <div className="mt-4 p-4 rounded-2xl border border-white/8 bg-gradient-to-br from-brand-500/5 to-transparent">
              <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
                下游消费者
              </div>
              <div className="text-[12.5px] text-white/70 leading-relaxed">
                {active === "dsl" && "被 StructureDefinitionBuilder 调用，每个 .point() / .component() 累积到 mutable builder，build() 触发编译。"}
                {active === "ir" && "被 StructureRuntimePlanCompiler、RuntimeAssetExporter、StructureCheckPlan、所有 JUnit 测试共同消费。"}
                {active === "assets" && "由 NeoForge server 在 datapack reload 时通过 RuntimeAssetReloadListener 加载，可以热更新而无需重启游戏。"}
                {active === "plan" && "被 RuntimeScheduler 缓存，每个 tracked instance 持有 plan 引用，FastTick 直接复用预算过的几何边界。"}
                {active === "runtime" && "由 RuntimeCheckEngineTickDriver 注册到 ServerTickEvent.Post；Production 场景默认走 fastTick。"}
                {active === "output" && "RuntimeStructureContext 给机器逻辑；DiagnosticGraph 投影成 4 种格式；GhostPreviewModel 喂给客户端渲染器。"}
              </div>
            </div>
          </div>
        </motion.div>
      </AnimatePresence>
    </Section>
  );
}
