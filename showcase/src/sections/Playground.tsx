import { motion, AnimatePresence } from "framer-motion";
import { ChevronRight, Terminal } from "lucide-react";
import { useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { cn } from "@/lib/utils";

interface Step {
  id: number;
  title: string;
  description: string;
  code: string;
  output: string;
  tone: "brand" | "cyan" | "magenta" | "lime" | "amber";
}

const STEPS: Step[] = [
  {
    id: 1,
    title: "1. 定义结构",
    description: "用 Structures.define() 描述一个有 controller + hollowBox 的机器",
    tone: "brand",
    code: `public static final StructureDefinition MACHINE = Structures.define(
        ResourceLocation.parse("cumulus:machine"))
    .transforms(
        Transforms.identity(),
        Transforms.horizontalFacing())
    .point("controller",
        RelativeBlockPos.of(0, 1, 0),
        Targets.block()
            .where(PredicateDescriptors.id("cumulus:controller"))
            .capture("controller"))
    .component(Components.hollowBox("casing",
        RelativeBlockPos.of(-1, 0, -1),
        RelativeBlockPos.of(1, 2, 1))
        .target(Targets.block())
        .where(PredicateDescriptors.tag("cumulus:heat_casing")))
    .context(ContextRuleDescriptor.derive(
        ContextKey.string("facing"),
        ContextSources.capture("controller").property("facing")))
    .build();`,
    output: `✓ StructureDefinition compiled
  id          = cumulus:machine
  schema      = irSchemaVersion = 2
  transforms  = [identity, horizontalFacing]
  points      = 26 (1 controller + 25 casing)
  context     = [facing ← controller.facing]
  validation  = ok (no duplicate ids / captures / spatial conflicts)`,
  },
  {
    id: 2,
    title: "2. 注册到运行时",
    description: "RuntimeCheckEngine 是核心，注册定义 + 在 anchor 处跟踪实例",
    tone: "cyan",
    code: `RuntimeCheckEngine engine = new RuntimeCheckEngine()
    .register(MACHINE)
    .substructureSyncPolicy(
        SubstructureSyncPolicy.statusAndContext)
    .addSubstructureLifecycleListener(event -> {
        log.info("child {}: {} -> {}",
            event.occurrenceId(),
            event.previousStatus(),
            event.currentStatus());
    });

// 在世界中跟踪一个 anchor
BlockPos anchor = new BlockPos(10, 64, 10);
engine.track(
    MACHINE.id(),
    anchor,
    minecraftSnapshotSource);`,
    output: `✓ engine registered
  definitions : 1 (cumulus:machine)
  anchors     : 1 @ (10,64,10)
  policy      : statusAndContext
  listener    : SubstructureLifecycleListener#1
  plan cache  : StructureCheckPlan built
                (3 transform candidates × 26 points = 78 bounds)`,
  },
  {
    id: 3,
    title: "3. 把世界事件接入调度器",
    description: "MinecraftRuntimeEventBridge 自动把方块改动映射成 dirty section",
    tone: "magenta",
    code: `// NeoForge event (auto-wired by MinecraftRuntimeEventBridge)
@SubscribeEvent
public void onBlockPlace(BlockEvent.EntityPlace event) {
    BlockPos pos = event.getPos();
    SectionKey key = SectionKey.fromBlock(pos);
    // mark dirty — next tick will check this section
    engine.markDirty(key);
}

@SubscribeEvent
public void onTick(ServerTickEvent.Post event) {
    // NeoForge post-tick driver
    // uses fastTick by default (status-only, no diagnostic alloc)
    RuntimeFrameResult frame = engine.fastTick(
        event.getServerTick(),
        BUDGET_PER_TICK);
    processDeltas(frame.deltas());
    processRequests(frame.requests());
}`,
    output: `→ tick 1   dirty=1   budget=8   processed=1
            delta: cumulus:machine@(10,64,10) VALID
            requests: []
→ tick 2   dirty=0   budget=8   processed=0   (idle)
→ tick 3   dirty=2   budget=8   processed=2
            delta: cumulus:machine@(10,64,10) INVALID
            delta: cumulus:machine@(50,64,30) VALID`,
  },
  {
    id: 4,
    title: "4. 消费 InstanceDelta",
    description: "每次状态变化，机器逻辑通过 delta 收到通知",
    tone: "lime",
    code: `void processDeltas(List<InstanceDelta> deltas) {
    for (var delta : deltas) {
        if (delta.status() == InstanceStatus.VALID) {
            // 启动机器
            startMachine(
                delta.context().revision(),
                delta.context().value("facing"),
                delta.context().capture("controller"));
        } else if (delta.status() == InstanceStatus.INVALID) {
            // 停机 + 把 DiagnosticGraph 投影给玩家
            stopMachine(delta.key());
            showPlayerOverlay(delta.diagnostics()
                .projectLocalized(player));
        }
    }
}

// RuntimeStructureContext 形态：
// {
//   revision: 7,
//   controller: BlockPos(10,65,10),
//   facing: north,
//   controller_id: cumulus:controller
// }`,
    output: `[machine #3] started · revision=7 · facing=north
[machine #3] controller captured: cumulus:controller @ (10,65,10)
[machine #2] stopping · 2 diagnostic nodes projected to player:
            ⚠ missingTarget @ (10,64,9) "place heat casing"
            ⚠ predicateFailed @ (11,64,10) "wrong tag"`,
  },
  {
    id: 5,
    title: "5. 自动构造（可选）",
    description: "FixHint 转 BuildPlanExecutor，自动修复",
    tone: "amber",
    code: `MinecraftBuildTarget buildTarget = new MinecraftBuildTarget(level);
BuildPlanExecutor executor = new BuildPlanExecutor(buildTarget);

// 把诊断图的 FixHint 转成 build plan
List<BuildAction> actions = diagnostics().stream()
    .flatMap(d -> d.fixHints().stream())
    .map(FixHint::toBuildAction)
    .toList();

executor.execute(actions);
// 内部对所有写入做 idempotent ensure，
// 已正确的方块不会被重写，重复执行幂等。`,
    output: `→ BuildPlanExecutor executed 2 actions
    place  cumulus:heat_casing @ (10,64,9)         OK
    place  cumulus:heat_casing @ (11,64,10)        OK (replaced minecraft:stone)
→ BuildPlan complete · 0 unsupported · 0 skipped
→ engine.markDirty(SectionKey(10,4,9))
→ next tick: re-validation triggered automatically`,
  },
];

const toneClass = {
  brand: { dot: "bg-brand-400", text: "text-brand-300", border: "border-brand-400/30", bg: "bg-brand-500/5" },
  cyan: { dot: "bg-cyan2", text: "text-cyan2", border: "border-cyan-400/30", bg: "bg-cyan-400/5" },
  magenta: { dot: "bg-magenta", text: "text-magenta", border: "border-pink-400/30", bg: "bg-pink-400/5" },
  lime: { dot: "bg-lime2", text: "text-lime2", border: "border-lime-400/30", bg: "bg-lime-400/5" },
  amber: { dot: "bg-amber2", text: "text-amber2", border: "border-amber-400/30", bg: "bg-amber-400/5" },
};

export function Playground() {
  const [active, setActive] = useState(1);
  const activeStep = STEPS.find((s) => s.id === active)!;
  const tc = toneClass[activeStep.tone];

  return (
    <Section
      id="playground"
      index="09"
      title={
        <>
          完整 API 玩法
          <span className="text-white/50"> 终端 → 上下文 → 数据请求</span>
        </>
      }
      lede={
        <>
          这是开发者真正会写的 Java 代码形态：从{" "}
          <em className="text-brand-300">Structures.define</em> 到{" "}
          <em className="text-cyan2">RuntimeCheckEngine.tick</em>，
          再到捕获产生的 <em className="text-magenta">InstanceDelta</em>。下方是一个可读、可逐段展开的端到端示例。
        </>
      }
    >
      <div className="grid lg:grid-cols-[260px,1fr] gap-5">
        {/* step nav */}
        <div className="space-y-2">
          {STEPS.map((s) => {
            const sc = toneClass[s.tone];
            return (
              <button
                key={s.id}
                onClick={() => setActive(s.id)}
                className={cn(
                  "w-full text-left p-3 rounded-xl border transition-all",
                  active === s.id
                    ? sc.border + " " + sc.bg
                    : "border-white/8 bg-white/[0.02] hover:bg-white/[0.04]"
                )}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span
                    className={cn(
                      "w-6 h-6 rounded-md flex items-center justify-center text-[11px] font-bold font-mono",
                      active === s.id ? sc.text + " bg-ink-950/60" : "text-white/50 bg-white/5"
                    )}
                  >
                    {s.id}
                  </span>
                  <ChevronRight
                    className={cn(
                      "w-3 h-3 transition-opacity",
                      active === s.id ? "opacity-100 " + sc.text : "opacity-0"
                    )}
                  />
                </div>
                <div
                  className={cn(
                    "text-[12.5px] font-semibold",
                    active === s.id ? "text-white/95" : "text-white/70"
                  )}
                >
                  {s.title}
                </div>
                <div className="text-[11px] text-white/45 mt-0.5 leading-tight">
                  {s.description}
                </div>
              </button>
            );
          })}
        </div>

        {/* code + output */}
        <AnimatePresence mode="wait">
          <motion.div
            key={active}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.25 }}
            className="space-y-4"
          >
            <div className="flex items-center gap-2">
              <Badge tone={activeStep.tone}>step {activeStep.id}</Badge>
              <span className="text-[13px] text-white/80">{activeStep.title.slice(3)}</span>
            </div>

            <CodeBlock code={activeStep.code} title={`step-${activeStep.id}.java`} maxHeight="380px" />

            <div
              className={cn(
                "rounded-2xl border bg-ink-950/90 p-4 font-mono text-[12px] leading-relaxed",
                tc.border
              )}
            >
              <div className="flex items-center gap-2 mb-2 text-[10.5px] uppercase tracking-wider text-white/40">
                <Terminal className="w-3 h-3" />
                <span>output</span>
              </div>
              <pre className="whitespace-pre-wrap text-white/80">{activeStep.output}</pre>
            </div>
          </motion.div>
        </AnimatePresence>
      </div>
    </Section>
  );
}
