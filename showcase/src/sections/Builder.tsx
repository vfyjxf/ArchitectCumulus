import { motion } from "framer-motion";
import {
  CheckCircle2, CircleSlash, Eraser, Eye, RotateCw, Sparkles, Trash2,
} from "lucide-react";
import { useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { CodeTabs } from "@/components/ui/CodeTabs";
import { IRVisualization } from "@/components/IRVisualization";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { BuilderViewport } from "@/three/BuilderViewport";
import {
  PALETTE,
  PRESET_NAMES,
  generateDSL,
  generateIR,
  generateCheckPlan,
  useBuilder,
} from "@/three/builderStore";
import { summarizeIR, generateSchematic } from "@/three/builderStoreExtras";
import { cn } from "@/lib/utils";

const ROTATE_LABELS = ["0°", "90°", "180°", "270°"];

export function Builder() {
  const blocks = useBuilder((s) => s.blocks);
  const kind = useBuilder((s) => s.selectedKind);
  const setKind = useBuilder((s) => s.setKind);
  const status = useBuilder((s) => s.status);
  const diagnostics = useBuilder((s) => s.diagnostics);
  const ghost = useBuilder((s) => s.ghost);
  const validate = useBuilder((s) => s.validate);
  const clear = useBuilder((s) => s.clear);
  const toggleGhost = useBuilder((s) => s.toggleGhost);
  const loadPreset = useBuilder((s) => s.loadPreset);
  const preset = useBuilder((s) => s.preset);

  const [presetOpen, setPresetOpen] = useState(false);
  const [rotateStep, setRotateStep] = useState(0);
  const [selectedLoc, setSelectedLoc] = useState<string | null>(null);

  const ir = generateIR(blocks);
  const dsl = generateDSL(blocks);
  const plan = generateCheckPlan(blocks);
  const schematic = generateSchematic(blocks);
  const summary = summarizeIR(blocks);

  const selectedBlock = selectedLoc
    ? blocks.find((b) => `${b.x},${b.y},${b.z}` === selectedLoc)
    : null;

  return (
    <Section
      id="builder"
      index="03"
      alt
      title={
        <>
          3D 多方块构造器
          <span className="text-white/50"> hover 查看 IR · 旋转预览 transform 候选</span>
        </>
      }
      lede={
        <>
          在虚拟世界里手动放置方块，左下角会实时编译出对应的{" "}
          <em className="text-brand-300">StructureDefinition</em>，
          右侧面板显示当前 check plan 的判定结果。
          <strong className="text-white/80">新增</strong>：hover 方块看 IR 详情、
          4 档旋转预览 transform 候选、点击方块查看选中态、放置动画。
        </>
      }
    >
      <div className="grid lg:grid-cols-[1.4fr,1fr] gap-5">
        {/* viewport */}
        <div className="relative rounded-2xl border border-white/8 bg-ink-950/80 overflow-hidden h-[560px]">
          <BuilderViewport
            rotateStep={rotateStep}
            selectedLoc={selectedLoc}
            onSelect={(k) => setSelectedLoc((prev) => (prev === k ? null : k))}
          />

          {/* palette overlay */}
          <div className="absolute top-4 left-4 right-4 flex items-start justify-between gap-3 pointer-events-none">
            <div className="pointer-events-auto glass-strong rounded-xl p-2 flex gap-1.5 flex-wrap max-w-md">
              {PALETTE.map((p) => (
                <button
                  key={p.kind}
                  onClick={() => setKind(p.kind)}
                  className={cn(
                    "flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-[12px] font-medium transition-all border",
                    kind === p.kind
                      ? "border-white/30 bg-white/15 text-white"
                      : "border-transparent text-white/60 hover:bg-white/5"
                  )}
                >
                  <span
                    className="w-3 h-3 rounded-sm shadow-inner"
                    style={{
                      background: p.color,
                      boxShadow: `0 0 10px ${p.color}80`,
                    }}
                  />
                  {p.label}
                </button>
              ))}
            </div>
            <div className="pointer-events-auto glass-strong rounded-xl px-3 py-2 text-[11px] font-mono text-white/60">
              <div>结构: <span className="text-cyan2">{blocks.length}</span> 方块</div>
              <div>模式: <span className="text-brand-300">{kind}</span></div>
            </div>
          </div>

          {/* rotate controls bottom-center */}
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 pointer-events-auto glass-strong rounded-xl p-1 flex items-center gap-1">
            <span className="text-[10px] font-mono text-white/40 px-2">rotate Y</span>
            {ROTATE_LABELS.map((label, i) => (
              <button
                key={label}
                onClick={() => setRotateStep(i)}
                className={cn(
                  "px-2 py-1 rounded-md text-[11px] font-mono transition-all",
                  rotateStep === i
                    ? "bg-brand-400/20 text-brand-200 border border-brand-400/30"
                    : "text-white/50 hover:text-white/80"
                )}
              >
                {label}
              </button>
            ))}
            <button
              onClick={() => setRotateStep((r) => (r + 1) % 4)}
              className="ml-1 w-7 h-7 rounded-md flex items-center justify-center text-white/60 hover:text-white hover:bg-white/5"
              title="rotate next"
            >
              <RotateCw className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="absolute bottom-4 left-4 right-4 flex items-center justify-between gap-3 pointer-events-none">
            <div className="pointer-events-auto glass-strong rounded-lg px-3 py-1.5 text-[10.5px] text-white/50">
              <span className="text-white/80">拖拽</span> 旋转 ·{" "}
              <span className="text-white/80">滚轮</span> 缩放 ·{" "}
              <span className="text-white/80">左键</span> 放置 ·{" "}
              <span className="text-white/80">hover</span> 看 IR ·{" "}
              <span className="text-white/80">点击</span> 选中
            </div>
            {ghost && (
              <div className="pointer-events-none chip bg-brand-500/20 text-brand-200 border-brand-400/30">
                <Eye className="w-3 h-3" /> ghost preview ON
              </div>
            )}
          </div>
        </div>

        {/* panel */}
        <div className="space-y-4">
          {/* status */}
          <div
            className={cn(
              "rounded-2xl border p-4 flex items-center gap-3 transition-all",
              status === "valid" && "border-lime-400/30 bg-lime-400/5",
              status === "invalid" && "border-rose-500/40 bg-rose-500/8",
              (status === "idle" || status === "pending") && "border-white/10 bg-white/[0.02]"
            )}
          >
            {status === "valid" ? (
              <CheckCircle2 className="w-5 h-5 text-lime2 shrink-0" />
            ) : status === "invalid" ? (
              <CircleSlash className="w-5 h-5 text-rose2 shrink-0" />
            ) : (
              <div className="w-5 h-5 rounded-full border-2 border-white/30 shrink-0" />
            )}
            <div>
              <div className="text-[13px] font-semibold text-white/90">
                {status === "valid" && "结构有效 ✓"}
                {status === "invalid" && `${diagnostics.length} 个问题需要修复`}
                {status === "idle" && "等待校验"}
                {status === "pending" && "校验中..."}
              </div>
              <div className="text-[11px] text-white/50 font-mono">
                {status === "valid" && `rotateStep=${rotateStep}° 候选可见 · fast check OK`}
                {status === "invalid" && "diagnostic graph built, ready to project"}
                {status === "idle" && "点击 Validate 触发 fast check"}
              </div>
            </div>
          </div>

          {/* selected block info */}
          {selectedBlock && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              className="rounded-2xl border border-brand-400/30 bg-brand-500/5 p-3"
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span
                    className="w-3 h-3 rounded-sm"
                    style={{
                      background: PALETTE.find((p) => p.kind === selectedBlock.kind)?.color,
                    }}
                  />
                  <code className="font-mono text-[12px] font-semibold text-brand-200">
                    {PALETTE.find((p) => p.kind === selectedBlock.kind)?.label}
                  </code>
                  <Badge tone="brand">selected</Badge>
                </div>
                <button
                  onClick={() => setSelectedLoc(null)}
                  className="text-white/40 hover:text-white"
                >
                  ✕
                </button>
              </div>
              <div className="font-mono text-[11px] text-white/60 space-y-0.5">
                <div>location: ({selectedBlock.x}, {selectedBlock.y}, {selectedBlock.z})</div>
                <div>role: {PALETTE.find((p) => p.kind === selectedBlock.kind)?.role}</div>
                <div className="text-white/40">
                  predicate: {PALETTE.find((p) => p.kind === selectedBlock.kind)?.predicate}
                </div>
                {rotateStep > 0 && (
                  <div className="text-cyan2">
                    rotated: ({(() => {
                      const b = selectedBlock;
                      if (rotateStep === 1) return `${-b.z}, ${b.y}, ${b.x}`;
                      if (rotateStep === 2) return `${-b.x}, ${b.y}, ${-b.z}`;
                      if (rotateStep === 3) return `${b.z}, ${b.y}, ${-b.x}`;
                      return "";
                    })()})
                  </div>
                )}
              </div>
            </motion.div>
          )}

          {/* actions */}
          <div className="flex flex-wrap gap-2">
            <Button variant="primary" size="md" onClick={validate}>
              <Sparkles className="w-3.5 h-3.5" /> Validate
            </Button>
            <Button variant="outline" size="md" onClick={toggleGhost}>
              <Eye className="w-3.5 h-3.5" /> {ghost ? "Hide Ghost" : "Ghost"}
            </Button>
            <Button variant="outline" size="md" onClick={clear}>
              <Eraser className="w-3.5 h-3.5" /> Clear
            </Button>
            <div className="relative">
              <Button
                variant="outline"
                size="md"
                onClick={() => setPresetOpen((v) => !v)}
              >
                Preset: {preset}
              </Button>
              {presetOpen && (
                <div className="absolute top-full mt-1 z-10 glass-strong rounded-lg p-1 min-w-[160px]">
                  {PRESET_NAMES.map((p) => (
                    <button
                      key={p}
                      onClick={() => {
                        loadPreset(p);
                        setPresetOpen(false);
                      }}
                      className={cn(
                        "block w-full text-left px-3 py-1.5 rounded text-[12px] hover:bg-white/5",
                        preset === p ? "text-brand-300" : "text-white/70"
                      )}
                    >
                      {p}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* IR / DSL / Plan — multi-tab view */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Badge tone="cyan">实时输出</Badge>
              <span className="text-[11px] text-white/40">
                从 3D 视图自动生成 · 切换查看不同形态
              </span>
            </div>
            <CodeTabs
              defaultTab="visual"
              tabs={[
                {
                  id: "visual",
                  label: "IR · 可视化",
                  tone: "cyan",
                  description: "React 渲染的 cards + 表格",
                  custom: <IRVisualization summary={summary} />,
                },
                {
                  id: "schematic",
                  label: "IR · 图谱",
                  tone: "cyan",
                  description: "ASCII schematic · 自创可读格式",
                  code: schematic,
                  title: "structure.schematic",
                  maxHeight: "360px",
                },
                {
                  id: "ir-tree",
                  label: "IR · record",
                  tone: "magenta",
                  description: "build() 后的不可变 record 字段树",
                  code: ir,
                  title: "StructureDefinition.ir",
                  maxHeight: "360px",
                },
                {
                  id: "dsl",
                  label: "DSL · Java",
                  tone: "amber",
                  description: "作者写出的源码（输入侧）",
                  code: dsl,
                  title: "Builder.java",
                  maxHeight: "360px",
                },
                {
                  id: "plan",
                  label: "Check Plan",
                  tone: "lime",
                  description: "编译后的预算几何 + footprint",
                  code: plan,
                  title: "StructureCheckPlan.plan",
                  maxHeight: "360px",
                },
              ]}
            />
          </div>

          {/* diagnostics */}
          {diagnostics.length > 0 && (
            <div>
              <div className="flex items-center gap-2 mb-2">
                <Badge tone="rose">诊断 ({diagnostics.length})</Badge>
                <span className="text-[11px] text-white/40">DiagnosticGraph 节点</span>
              </div>
              <ul className="space-y-1.5 max-h-[200px] overflow-auto scrollbar-thin">
                {diagnostics.map((d, i) => (
                  <motion.li
                    key={i}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.05 }}
                    className={cn(
                      "p-2.5 rounded-lg border text-[12px]",
                      d.severity === "ERROR" && "border-rose-500/30 bg-rose-500/8",
                      d.severity === "WARN" && "border-amber-400/30 bg-amber-400/5"
                    )}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-mono text-[10px] uppercase text-white/40">
                        {d.severity}
                      </span>
                      <span className="font-mono text-[11px] text-rose2">{d.code}</span>
                      <span className="text-white/40 font-mono text-[10px]">
                        @({d.location.join(", ")})
                      </span>
                    </div>
                    <div className="text-white/80">{d.message}</div>
                    {d.fix && (
                      <div className="mt-1 text-[11px] text-lime2 font-mono">
                        → {d.fix}
                      </div>
                    )}
                  </motion.li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    </Section>
  );
}
