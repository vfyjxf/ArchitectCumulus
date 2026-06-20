import { motion, AnimatePresence } from "framer-motion";
import {
  Box, Boxes, CircleDot, Cog, Eye, FileText, Frame, Layers,
  Plus, Sparkles, Trash2, User, X, Zap, ChevronDown, RotateCw,
} from "lucide-react";
import { useMemo, useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { CodeTabs } from "@/components/ui/CodeTabs";
import { IRVisualization } from "@/components/IRVisualization";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import {
  COMPOSER_PRESETS,
  generateDSL,
  generateIRSummary,
  pointCountFor,
  useComposer,
  type ComponentKind,
  type TargetKind,
  type TransformKind,
} from "@/three/composerStore";
import { summarizeComposerIR } from "@/three/builderStoreExtras";
import { cn } from "@/lib/utils";

const targetColors: Record<TargetKind, string> = {
  block: "#7c5cff",
  block_entity: "#22d3ee",
  entity: "#f0abfc",
  fluid: "#a3e635",
  capability: "#fbbf24",
  context: "#fb7185",
  region: "#fb923c",
};

const targetLabel: Record<TargetKind, string> = {
  block: "block",
  block_entity: "block_entity",
  entity: "entity",
  fluid: "fluid",
  capability: "capability",
  context: "context",
  region: "region",
};

const componentIcons: Record<ComponentKind, typeof Box> = {
  point: CircleDot,
  box: Box,
  hollowBox: Boxes,
  region: Frame,
  substructure: Layers,
};

const transformLabels: Record<TransformKind, string> = {
  identity: "identity",
  horizontal_facing: "horizontal_facing",
  rotate_y_90: "rotate_y_90",
  rotate_y_180: "rotate_y_180",
  mirror_x: "mirror_x",
  mirror_z: "mirror_z",
};

const transformCandidates: Record<TransformKind, number> = {
  identity: 1,
  horizontal_facing: 4,
  rotate_y_90: 4,
  rotate_y_180: 2,
  mirror_x: 2,
  mirror_z: 2,
};

// ============================================================
// Component Editor Row
// ============================================================

function ComponentRow({
  id,
  index,
}: {
  id: string;
  index: number;
}) {
  const c = useComposer((s) => s.components.find((x) => x.id === id)!);
  const update = useComposer((s) => s.updateComponent);
  const remove = useComposer((s) => s.removeComponent);
  const [expanded, setExpanded] = useState(false);

  const Icon = componentIcons[c.kind];
  const emitCount = pointCountFor(c);
  const color = targetColors[c.target];

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, x: -10 }}
      className={cn(
        "rounded-xl border bg-white/[0.02]",
        c.required ? "border-white/10" : "border-amber-400/20 bg-amber-400/[0.02]"
      )}
    >
      <div className="flex items-center gap-2 p-2.5">
        <span className="font-mono text-[10px] text-white/30 w-5">{(index + 1).toString().padStart(2, "0")}</span>
        <div
          className="w-7 h-7 rounded-md flex items-center justify-center border shrink-0"
          style={{ background: color + "20", borderColor: color + "40" }}
        >
          <Icon className="w-3.5 h-3.5" style={{ color }} />
        </div>
        <input
          value={c.name}
          onChange={(e) => update(id, { name: e.target.value })}
          className="bg-transparent text-[13px] font-mono font-semibold text-white/90 outline-none flex-1 min-w-0"
        />
        <span className="font-mono text-[10.5px] text-white/40 hidden md:inline">
          emits <span className="text-white/70">{emitCount}</span>
        </span>
        {!c.required && (
          <Badge tone="amber">optional</Badge>
        )}
        <button
          onClick={() => setExpanded((v) => !v)}
          className="text-white/40 hover:text-white/80"
        >
          <ChevronDown className={cn("w-3.5 h-3.5 transition-transform", expanded && "rotate-180")} />
        </button>
        <button
          onClick={() => remove(id)}
          className="text-rose2/50 hover:text-rose2"
        >
          <Trash2 className="w-3.5 h-3.5" />
        </button>
      </div>

      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0 }}
            animate={{ height: "auto" }}
            exit={{ height: 0 }}
            className="overflow-hidden border-t border-white/5"
          >
            <div className="p-3 grid sm:grid-cols-2 gap-3">
              {/* target type */}
              <label className="block">
                <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-1">
                  target type
                </div>
                <select
                  value={c.target}
                  onChange={(e) => update(id, { target: e.target.value as TargetKind })}
                  className="w-full bg-ink-950 border border-white/10 rounded-lg px-2 py-1.5 text-[12px] font-mono outline-none focus:border-brand-400"
                >
                  {Object.entries(targetLabel).map(([k, v]) => (
                    <option key={k} value={k}>{v}</option>
                  ))}
                </select>
              </label>

              {/* predicate */}
              {c.kind !== "substructure" && (
                <label className="block">
                  <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-1">
                    predicate id / tag
                  </div>
                  <input
                    value={c.predicate}
                    onChange={(e) => update(id, { predicate: e.target.value })}
                    placeholder={c.target === "capability" ? "cumulus:energy" : "#cumulus:heat_casing"}
                    className="w-full bg-ink-950 border border-white/10 rounded-lg px-2 py-1.5 text-[12px] font-mono outline-none focus:border-brand-400"
                  />
                </label>
              )}

              {/* child id for substructure */}
              {c.kind === "substructure" && (
                <label className="block">
                  <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-1">
                    child structure id
                  </div>
                  <input
                    value={c.childId ?? ""}
                    onChange={(e) => update(id, { childId: e.target.value })}
                    placeholder="cumulus:arm"
                    className="w-full bg-ink-950 border border-white/10 rounded-lg px-2 py-1.5 text-[12px] font-mono outline-none focus:border-brand-400"
                  />
                </label>
              )}

              {/* capture */}
              {c.kind !== "substructure" && c.kind !== "region" && (
                <label className="block">
                  <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-1">
                    capture name (optional)
                  </div>
                  <input
                    value={c.capture ?? ""}
                    onChange={(e) => update(id, { capture: e.target.value || undefined })}
                    placeholder="controller"
                    className="w-full bg-ink-950 border border-white/10 rounded-lg px-2 py-1.5 text-[12px] font-mono outline-none focus:border-brand-400"
                  />
                </label>
              )}

              {/* from */}
              <label className="block">
                <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-1">
                  from (x, y, z)
                </div>
                <div className="grid grid-cols-3 gap-1">
                  {c.from.map((v, i) => (
                    <input
                      key={i}
                      type="number"
                      value={v}
                      onChange={(e) => {
                        const next = [...c.from] as [number, number, number];
                        next[i] = parseInt(e.target.value) || 0;
                        update(id, { from: next });
                      }}
                      className="bg-ink-950 border border-white/10 rounded-md px-1 py-1 text-[11.5px] font-mono text-center outline-none focus:border-brand-400"
                    />
                  ))}
                </div>
              </label>

              {/* to (only for box/hollow/region) */}
              {c.kind !== "point" && c.kind !== "substructure" && (
                <label className="block">
                  <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-1">
                    to (x, y, z)
                  </div>
                  <div className="grid grid-cols-3 gap-1">
                    {c.to.map((v, i) => (
                      <input
                        key={i}
                        type="number"
                        value={v}
                        onChange={(e) => {
                          const next = [...c.to] as [number, number, number];
                          next[i] = parseInt(e.target.value) || 0;
                          update(id, { to: next });
                        }}
                        className="bg-ink-950 border border-white/10 rounded-md px-1 py-1 text-[11.5px] font-mono text-center outline-none focus:border-brand-400"
                      />
                    ))}
                  </div>
                </label>
              )}

              {/* required toggle */}
              <label className="flex items-center gap-2 sm:col-span-2 mt-1">
                <input
                  type="checkbox"
                  checked={c.required}
                  onChange={(e) => update(id, { required: e.target.checked })}
                  className="accent-brand-400"
                />
                <span className="text-[12px] text-white/70">REQUIRED (unchecked = OPTIONAL)</span>
              </label>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ============================================================
// Add component menu
// ============================================================

function AddComponentMenu() {
  const [open, setOpen] = useState(false);
  const addComponent = useComposer((s) => s.addComponent);

  const options: { kind: ComponentKind; label: string; icon: typeof Box; color: string }[] = [
    { kind: "point", label: "point", icon: CircleDot, color: "#7c5cff" },
    { kind: "box", label: "box (filled)", icon: Box, color: "#22d3ee" },
    { kind: "hollowBox", label: "hollowBox (shell)", icon: Boxes, color: "#f0abfc" },
    { kind: "region", label: "region (1 target)", icon: Frame, color: "#fb923c" },
    { kind: "substructure", label: "substructure (ref)", icon: Layers, color: "#a3e635" },
  ];

  return (
    <div className="relative">
      <Button variant="outline" size="sm" onClick={() => setOpen((v) => !v)}>
        <Plus className="w-3.5 h-3.5" /> 添加 component
      </Button>
      <AnimatePresence>
        {open && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
            <motion.div
              initial={{ opacity: 0, y: -5 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -5 }}
              className="absolute top-full mt-1 left-0 z-20 glass-strong rounded-xl p-1 min-w-[180px]"
            >
              {options.map((o) => (
                <button
                  key={o.kind}
                  onClick={() => {
                    addComponent({
                      kind: o.kind,
                      name: `new_${o.kind}`,
                      target: o.kind === "region" ? "region" : "block",
                      from: [0, 0, 0],
                      to: o.kind === "box" || o.kind === "hollowBox" || o.kind === "region"
                        ? [1, 1, 1]
                        : [0, 0, 0],
                      predicate: o.kind === "substructure" ? "" : "cumulus:casing",
                      childId: o.kind === "substructure" ? "cumulus:child" : undefined,
                    });
                    setOpen(false);
                  }}
                  className="w-full flex items-center gap-2 px-2.5 py-1.5 rounded text-[12px] hover:bg-white/5 text-white/70 hover:text-white/90"
                >
                  <o.icon className="w-3.5 h-3.5" style={{ color: o.color }} />
                  <code className="font-mono">{o.label}</code>
                </button>
              ))}
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}

// ============================================================
// Main
// ============================================================

export function Composer() {
  const structureId = useComposer((s) => s.structureId);
  const setStructureId = useComposer((s) => s.setStructureId);
  const components = useComposer((s) => s.components);
  const transforms = useComposer((s) => s.transforms);
  const toggleTransform = useComposer((s) => s.toggleTransform);
  const contextRules = useComposer((s) => s.contextRules);
  const addContextRule = useComposer((s) => s.addContextRule);
  const updateContextRule = useComposer((s) => s.updateContextRule);
  const removeContextRule = useComposer((s) => s.removeContextRule);
  const loadPreset = useComposer((s) => s.loadPreset);
  const reset = useComposer((s) => s.reset);

  const state = useComposer();
  const dsl = useMemo(() => generateDSL(state), [state]);
  const summary = useMemo(() => generateIRSummary(state), [state]);
  const irSummary = useMemo(() => summarizeComposerIR(state), [state]);

  const totalCandidates = transforms.reduce((acc, t) => Math.max(acc, transformCandidates[t]), 1);

  return (
    <Section
      id="composer"
      index="02·6"
      alt
      title={
        <>
          交互式结构组装器：
          <span className="text-white/50"> 拼组件 → 看 DSL / IR / 校验实时变</span>
        </>
      }
      lede={
        <>
          下方是一个完整的「真定义」组装器。改任何一个字段（坐标、target、predicate、transform），
          右侧 Java DSL、IR 摘要、check plan 统计、校验问题都会<strong className="text-white/80">同步</strong>变化。
          这是 StructureDefinitionBuilder + StructureDefinitionValidator 在浏览器里的轻量等价物。
        </>
      }
    >
      {/* preset bar */}
      <div className="flex flex-wrap items-center gap-2 mb-5">
        <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
          预设:
        </span>
        {COMPOSER_PRESETS.map((p) => (
          <button
            key={p}
            onClick={() => loadPreset(p)}
            className={cn(
              "px-3 py-1.5 rounded-full text-[12px] font-mono border transition-all",
              structureId.includes(p) || (p === "forge" && structureId.includes("forge"))
                ? "bg-brand-400/20 border-brand-400/40 text-brand-100"
                : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
            )}
          >
            {p}
          </button>
        ))}
        <Button variant="ghost" size="sm" onClick={reset}>
          <RotateCw className="w-3 h-3" /> 清空
        </Button>
      </div>

      <div className="grid lg:grid-cols-[1.2fr,1fr] gap-5">
        {/* LEFT: editor */}
        <div className="space-y-4">
          {/* structure id */}
          <div className="rounded-xl border border-white/8 bg-white/[0.02] p-3">
            <label className="block">
              <div className="flex items-center justify-between mb-1">
                <span className="text-[10px] uppercase tracking-wider text-white/40 font-mono">
                  structure id
                </span>
                <Badge tone="brand">schema v{state.schemaVersion}</Badge>
              </div>
              <input
                value={structureId}
                onChange={(e) => setStructureId(e.target.value)}
                className="w-full bg-ink-950 border border-white/10 rounded-lg px-3 py-1.5 text-[13px] font-mono outline-none focus:border-brand-400"
              />
            </label>
          </div>

          {/* transforms */}
          <div className="rounded-xl border border-white/8 bg-white/[0.02] p-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] uppercase tracking-wider text-white/40 font-mono">
                transforms (候选用尽顺序短路)
              </span>
              <span className="font-mono text-[11px] text-cyan2">
                max {totalCandidates} candidates
              </span>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {(Object.keys(transformLabels) as TransformKind[]).map((t) => {
                const active = transforms.includes(t);
                return (
                  <button
                    key={t}
                    onClick={() => toggleTransform(t)}
                    className={cn(
                      "px-2.5 py-1 rounded-lg text-[11.5px] font-mono border transition-all flex items-center gap-1",
                      active
                        ? "bg-brand-400/20 border-brand-400/40 text-brand-100"
                        : "bg-white/[0.02] border-white/8 text-white/40 hover:text-white/70"
                    )}
                  >
                    <span
                      className={cn(
                        "w-1.5 h-1.5 rounded-full",
                        active ? "bg-brand-300" : "bg-white/20"
                      )}
                    />
                    {transformLabels[t]}
                    <span className="text-white/40">·{transformCandidates[t]}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* components */}
          <div className="rounded-xl border border-white/8 bg-white/[0.02] p-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] uppercase tracking-wider text-white/40 font-mono">
                components ({components.length})
              </span>
              <AddComponentMenu />
            </div>
            <div className="space-y-1.5">
              <AnimatePresence initial={false}>
                {components.map((c, i) => (
                  <ComponentRow key={c.id} id={c.id} index={i} />
                ))}
              </AnimatePresence>
              {components.length === 0 && (
                <div className="text-center py-6 text-[12px] text-white/30">
                  还没有 component。点击右上角 "添加 component" 开始拼装。
                </div>
              )}
            </div>
          </div>

          {/* context rules */}
          <div className="rounded-xl border border-white/8 bg-white/[0.02] p-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] uppercase tracking-wider text-white/40 font-mono">
                context rules (派生 RuntimeStructureContext 键)
              </span>
              <Button variant="ghost" size="sm" onClick={addContextRule}>
                <Plus className="w-3 h-3" /> add
              </Button>
            </div>
            <div className="space-y-1.5">
              {contextRules.map((r) => (
                <div key={r.id} className="flex items-center gap-2">
                  <input
                    value={r.name}
                    onChange={(e) => updateContextRule(r.id, { name: e.target.value })}
                    className="bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11.5px] font-mono w-32 outline-none focus:border-brand-400"
                  />
                  <span className="text-white/30 text-[11px]">←</span>
                  <input
                    value={r.expression}
                    onChange={(e) => updateContextRule(r.id, { expression: e.target.value })}
                    className="bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11.5px] font-mono flex-1 outline-none focus:border-brand-400"
                  />
                  <button
                    onClick={() => removeContextRule(r.id)}
                    className="text-rose2/50 hover:text-rose2"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
              {contextRules.length === 0 && (
                <div className="text-[11.5px] text-white/30 text-center py-3">
                  没有 context rule。结构匹配后 RuntimeStructureContext 为空。
                </div>
              )}
            </div>
          </div>
        </div>

        {/* RIGHT: live output */}
        <div className="space-y-4">
          {/* IR summary */}
          <div className="rounded-2xl border border-white/8 bg-gradient-to-br from-brand-500/5 to-cyan-400/5 p-4">
            <div className="flex items-center gap-2 mb-3">
              <Sparkles className="w-4 h-4 text-brand-300" />
              <span className="text-[13px] font-semibold">IR 摘要 (实时)</span>
              <Badge tone="cyan">live</Badge>
            </div>
            <div className="grid grid-cols-2 gap-2 mb-3">
              <Metric label="points (展开)" value={summary.points} color="text-brand-300" />
              <Metric label="region targets" value={summary.regions} color="text-amber2" />
              <Metric label="substructures" value={summary.substructures} color="text-lime2" />
              <Metric label="captures" value={summary.captures.length} color="text-magenta" />
              <Metric label="transform 候选" value={summary.candidates} color="text-cyan2" />
              <Metric label="footprint sections" value={summary.footprint} color="text-rose2" />
            </div>
            <div className="grid grid-cols-2 gap-2 text-[11px]">
              <div className="rounded-lg bg-white/[0.02] border border-white/8 p-2">
                <div className="text-white/40 font-mono text-[10px] uppercase mb-0.5">fastTick cost</div>
                <div className="font-mono text-cyan2">
                  ~{summary.estimatedCheckMs.toFixed(3)} ms
                </div>
              </div>
              <div className="rounded-lg bg-white/[0.02] border border-white/8 p-2">
                <div className="text-white/40 font-mono text-[10px] uppercase mb-0.5">plan memory</div>
                <div className="font-mono text-magenta">
                  ~{summary.estimatedMemoryKb.toFixed(1)} KB
                </div>
              </div>
            </div>
          </div>

          {/* DSL + IR + Plan — multi-tab */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <FileText className="w-4 h-4 text-cyan2" />
              <span className="text-[12.5px] font-semibold">输出</span>
              <span className="text-[10.5px] text-white/40">切换查看：可视化 IR / DSL / record / plan</span>
            </div>
            <CodeTabs
              defaultTab="visual"
              tabs={[
                {
                  id: "visual",
                  label: "IR · 可视化",
                  tone: "cyan",
                  description: "React 渲染的 cards + 表格",
                  custom: <IRVisualization summary={irSummary} />,
                },
                {
                  id: "dsl",
                  label: "DSL · Java",
                  tone: "amber",
                  description: "作者写出的源码",
                  code: dsl,
                  title: "ShowcaseStructure.java",
                  maxHeight: "400px",
                },
              ]}
            />
          </div>

          {/* issues */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Zap className="w-4 h-4 text-amber2" />
              <span className="text-[12.5px] font-semibold">
                StructureDefinitionValidator 输出
              </span>
              <Badge tone={summary.issues.length === 0 ? "lime" : "rose"}>
                {summary.issues.length === 0 ? "OK" : `${summary.issues.length} issue(s)`}
              </Badge>
            </div>
            {summary.issues.length === 0 ? (
              <div className="rounded-xl border border-lime-400/30 bg-lime-400/5 p-3 text-[12.5px] text-lime2">
                ✓ build() 成功。13 项编译期校验全部通过：
                无重复 point id、无重复 capture、无 spatial conflict、substructure childId 齐全。
              </div>
            ) : (
              <ul className="space-y-1">
                {summary.issues.map((iss, i) => (
                  <motion.li
                    key={i}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    className={cn(
                      "rounded-lg border p-2.5 text-[12px]",
                      iss.severity === "error"
                        ? "border-rose-500/30 bg-rose-500/8 text-rose2"
                        : "border-amber-400/30 bg-amber-400/5 text-amber2"
                    )}
                  >
                    <span className="font-mono text-[10.5px] uppercase opacity-70 mr-2">
                      {iss.severity}
                    </span>
                    <code className="font-mono font-semibold">{iss.code}</code>
                    <span className="text-white/70 ml-2">{iss.msg}</span>
                  </motion.li>
                ))}
              </ul>
            )}
          </div>

          {/* check plan */}
          <div className="rounded-2xl border border-white/8 bg-white/[0.02] p-4">
            <div className="flex items-center gap-2 mb-2">
              <Eye className="w-4 h-4 text-magenta" />
              <span className="text-[12.5px] font-semibold">StructureCheckPlan (编译产物)</span>
            </div>
            <div className="text-[12px] text-white/60 leading-relaxed">
              <div className="font-mono text-[11px] text-white/80 mb-1">
                plan.candidates.length = <span className="text-cyan2">{summary.candidates}</span>
              </div>
              <div className="space-y-0.5 ml-3 font-mono text-[11px] text-white/50">
                {transforms.map((t, i) => (
                  <div key={t}>
                    candidate[{i}] = transform:{" "}
                    <span className="text-brand-200">{transformLabels[t]}</span>
                    {" · "}
                    bounds: <span className="text-magenta">{summary.points}</span> pointBound
                    {" · "}
                    footprint: <span className="text-amber2">{summary.footprint}</span> sections
                  </div>
                ))}
              </div>
              <div className="mt-3 p-2 rounded-lg bg-ink-950/60 text-[11.5px] text-white/60">
                fastCheck 按 candidate 顺序短路：第 1 个通过的 candidate 即返回 VALID，
                跳过剩余 {Math.max(0, summary.candidates - 1)} 个候选的所有 pointBound 检查。
              </div>
            </div>
          </div>
        </div>
      </div>
    </Section>
  );
}

function Metric({
  label,
  value,
  color,
}: {
  label: string;
  value: number | string;
  color: string;
}) {
  return (
    <div className="rounded-lg bg-white/[0.02] border border-white/8 p-2.5">
      <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-0.5">
        {label}
      </div>
      <div className={cn("text-xl font-bold font-mono", color)}>{value}</div>
    </div>
  );
}
