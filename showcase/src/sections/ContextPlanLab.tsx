import { Section } from "@/components/ui/Section";
import { Badge } from "@/components/ui/Badge";
import { CodeTabs } from "@/components/ui/CodeTabs";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { cn } from "@/lib/utils";
import {
  OPS,
  OpId,
  useContextPlan,
  evaluate,
  generatePlanDsl,
  shellPointCount,
  interiorVolume,
} from "@/three/contextPlanStore";

const ALL_OPS: OpId[] = [
  "count",
  "sum",
  "interiorVolume",
  "derive",
  "compute",
  "bounds",
  "first",
];

const SIZES = [3, 5, 7, 9];

const ACCENT_CLASSES: Record<string, { dot: string; text: string; border: string; bg: string }> = {
  brand: { dot: "bg-brand-400", text: "text-brand-200", border: "border-brand-400/40", bg: "bg-brand-500/10" },
  cyan: { dot: "bg-cyan-400", text: "text-cyan2", border: "border-cyan-400/40", bg: "bg-cyan-500/10" },
  magenta: { dot: "bg-fuchsia-400", text: "text-magenta", border: "border-fuchsia-400/40", bg: "bg-fuchsia-500/10" },
  lime: { dot: "bg-lime-400", text: "text-lime2", border: "border-lime-400/40", bg: "bg-lime-500/10" },
  amber: { dot: "bg-amber-400", text: "text-amber2", border: "border-amber-400/40", bg: "bg-amber-500/10" },
  rose: { dot: "bg-rose-400", text: "text-rose2", border: "border-rose-400/40", bg: "bg-rose-500/10" },
};

/** 简易 SVG 中空盒子俯视图：N×N 网格，shell 着色，内部留空 */
function HollowBoxSvg({ size }: { size: number }) {
  const cell = 22;
  const gap = 1;
  const dim = size * (cell + gap);
  const cells = [];
  for (let x = 0; x < size; x++) {
    for (let y = 0; y < size; y++) {
      const onShell =
        size < 3 ||
        x === 0 || x === size - 1 ||
        y === 0 || y === size - 1;
      cells.push(
        <rect
          key={`${x}-${y}`}
          x={x * (cell + gap)}
          y={y * (cell + gap)}
          width={cell}
          height={cell}
          rx={3}
          className={onShell ? "fill-brand-500/70 stroke-brand-300/40" : "fill-white/[0.03] stroke-white/5"}
          strokeWidth={0.5}
        />
      );
      if (onShell) {
        cells.push(
          <text
            key={`t-${x}-${y}`}
            x={x * (cell + gap) + cell / 2}
            y={y * (cell + gap) + cell / 2 + 3}
            textAnchor="middle"
            className="fill-white/40 font-mono"
            style={{ fontSize: 8 }}
          >
            c
          </text>
        );
      }
    }
  }
  return (
    <svg viewBox={`-2 -2 ${dim + 4} ${dim + 4}`} className="w-full h-auto max-w-[320px] mx-auto">
      <rect x={-2} y={-2} width={dim + 4} height={dim + 4} rx={6} className="fill-white/[0.02]" />
      {cells}
    </svg>
  );
}

export function ContextPlanLab() {
  const { size, enabled, setSize, toggle, reset } = useContextPlan();
  const computed = useMemo(() => evaluate(enabled, size), [enabled, size]);
  const planDsl = useMemo(() => generatePlanDsl(enabled, size), [enabled, size]);

  // ── CheckPhase demo state ──
  const [phaseMin, setPhaseMin] = useState(2);
  const [phaseMax, setPhaseMax] = useState(5);
  const [actualLayers, setActualLayers] = useState(4);

  const phaseResult: "valid" | "invalid" | "incomplete" = useMemo(() => {
    if (actualLayers < phaseMin) return "invalid";
    if (actualLayers < phaseMax) return "incomplete";
    return "valid";
  }, [actualLayers, phaseMin, phaseMax]);

  const phaseDsl = useMemo(() => [
    "import static dev.vfyjxf.cumulus.api.context.plan.stdlib.CheckPhases.*;",
    "",
    "Structures.define(id(\"adaptive_tower\"))",
    "    .point(\"controller\", at(0,0,0), Targets.block()",
    "      .where(PredicateDescriptors.id(\"cumulus:controller\"))",
    "      .capture(\"controller\"))",
    "    .checkPhase(repeatRange(",
    "        \"ring\",",
    "        Components.hollowBox(\"ring\", at(-1,0,-1), at(1,0,2)),",
    `        Axis.y, ${phaseMin}, ${phaseMax}, TOWER_HEIGHT))`,
    "    .build();",
    "",
    `// min=${phaseMin} (必须满足), max=${phaseMax} (尽量多探测)`,
    `// 实际匹配 ${Math.min(actualLayers, phaseMax)} 层 → status = ${phaseResult}`,
  ].join("\n"), [phaseMin, phaseMax, actualLayers, phaseResult]);

  // trace 字符串：每步操作 → 写入的键
  const trace = useMemo(() => {
    const lines: string[] = [
      "// 执行管线：操作按声明顺序运行，后写的能读前面写的值",
      `// 结构：${size}×${size}×${size} hollow box → ${shellPointCount(size)} 个 casing 点，内部体积 ${interiorVolume(size)}`,
      "",
    ];
    computed.forEach((e, i) => {
      const dep = e.dependsOn && e.dependsOn.length > 0 ? `  // 读 ${e.dependsOn.join(", ")}` : "";
      lines.push(`[${i + 1}] ${OPS[e.op].label.padEnd(20)} → putComputed(${e.key}, ${e.value})${dep}`);
    });
    lines.push("");
    lines.push("// toRuntime() 合并 derived + computed → RuntimeStructureContext");
    return lines.join("\n");
  }, [computed, size]);

  return (
    <Section
      id="context-plan"
      index="08"
      title="Context Plan Lab"
      lede={
        <>
          两个层次的动态判定：<strong className="text-white/80">CheckPhase</strong> 在静态 point 匹配后运行，
          能读已匹配数据、判断结构合规性、写计算上下文、并返回额外 point（实现动态形态、区间重复等）。
          <strong className="text-white/80"> ContextPlan</strong> 操作（count / sum / interiorVolume / derive / compute）
          在 CheckPhase 之后执行，能看到所有匹配点（静态 + 动态）。下方可交互演示两者。
        </>
      }
    >
      <div className="grid lg:grid-cols-[1fr,1.1fr] gap-6">
        {/* 左：结构 + 操作面板 */}
        <div className="space-y-4">
          {/* 结构预览 */}
          <div className="rounded-2xl border border-white/8 bg-white/[0.02] p-5">
            <div className="flex items-center justify-between mb-3">
              <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
                结构预览 · hollow box
              </span>
              <Badge tone="brand">{size}×{size}×{size}</Badge>
            </div>
            <HollowBoxSvg size={size} />
            <div className="mt-3 flex gap-2 justify-center">
              {SIZES.map((s) => (
                <button
                  key={s}
                  onClick={() => setSize(s)}
                  className={cn(
                    "px-3 py-1 rounded-md font-mono text-[11px] border transition-colors",
                    size === s
                      ? "border-brand-400/60 bg-brand-500/15 text-brand-200"
                      : "border-white/10 text-white/50 hover:border-white/20 hover:text-white/70"
                  )}
                >
                  {s}³
                </button>
              ))}
            </div>
            <div className="mt-3 flex justify-center gap-4 text-[11px] font-mono text-white/50">
              <span><span className="inline-block w-2.5 h-2.5 rounded-sm bg-brand-500/70 align-middle mr-1" />casing ×{shellPointCount(size)}</span>
              <span>interior = {interiorVolume(size)}</span>
            </div>
          </div>

          {/* 操作面板 */}
          <div className="rounded-2xl border border-white/8 bg-white/[0.02] p-5">
            <div className="flex items-center justify-between mb-3">
              <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
                标准库操作 · 勾选启用
              </span>
              <button
                onClick={reset}
                className="text-[11px] text-white/40 hover:text-white/70 font-mono"
              >
                reset
              </button>
            </div>
            <div className="space-y-1.5">
              {ALL_OPS.map((opId) => {
                const op = OPS[opId];
                const on = enabled.includes(opId);
                const acc = ACCENT_CLASSES[op.accent];
                return (
                  <button
                    key={opId}
                    onClick={() => toggle(opId)}
                    className={cn(
                      "w-full flex items-center gap-3 px-3 py-2 rounded-lg border text-left transition-all",
                      on
                        ? cn(acc.border, acc.bg)
                        : "border-white/8 hover:border-white/15"
                    )}
                  >
                    <span
                      className={cn(
                        "w-4 h-4 rounded border flex items-center justify-center shrink-0",
                        on ? cn(acc.border, "bg-white/10") : "border-white/20"
                      )}
                    >
                      {on && <span className={cn("w-2 h-2 rounded-sm", acc.dot)} />}
                    </span>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-baseline gap-2">
                        <code className={cn("font-mono text-[12px] font-semibold", on ? acc.text : "text-white/60")}>
                          {op.label}
                        </code>
                        <code className="font-mono text-[10.5px] text-white/40">→ {op.outKey}</code>
                      </div>
                      <div className="text-[10.5px] text-white/40 mt-0.5">{op.hint}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* 右：DSL / computed / trace */}
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Badge tone="cyan">实时输出</Badge>
            <span className="text-[11px] text-white/40">随操作勾选与结构尺寸更新</span>
          </div>
          <CodeTabs
            defaultTab="computed"
            tabs={[
              {
                id: "computed",
                label: "Computed · IR",
                tone: "cyan",
                description: "操作执行后写入 computed 层的值（→ RuntimeStructureContext）",
                custom: <ComputedTable entries={computed} />,
              },
              {
                id: "plan",
                label: "DSL · Java",
                tone: "amber",
                description: "ContextPlan.of(...) 生成 DSL，import static Operations.*",
                code: planDsl,
                title: "ContextPlan.java",
                maxHeight: "380px",
              },
              {
                id: "trace",
                label: "Trace · 执行顺序",
                tone: "lime",
                description: "操作按声明顺序运行，后写的 anyValue 能读前面的输出",
                code: trace,
                title: "execution.trace",
                maxHeight: "380px",
              },
            ]}
          />

          {/* modder 自定义操作说明 */}
          <div className="mt-4 rounded-xl border border-white/8 bg-white/[0.02] p-4">
            <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-2">
              自定义操作 · 实现 ContextOperation
            </div>
            <CodeBlock
              title="PlayerNearbyRecorder.java"
              maxHeight="200px"
              code={`public final class PlayerNearbyRecorder implements ContextOperation {
    public void execute(OperationContext ctx) {
        long n = ctx.matches()
            .matching(p -> "minecraft:player".equals(p.value()))
            .count();
        ctx.context().putComputed(PLAYER_COUNT, (int) n);
    }
}

// DSL:
.contextPlan(plan -> plan.op(new PlayerNearbyRecorder()))`}
            />
          </div>
        </div>
      </div>

      {/* ── CheckPhase: 动态结构判定 ── */}
      <div className="mt-10 pt-8 border-t border-white/8">
        <div className="flex items-baseline gap-3 mb-4">
          <span className="font-mono text-[12px] text-magenta/60 tracking-widest">CHECK PHASE</span>
          <span className="h-px w-12 bg-gradient-to-r from-magenta/40 to-transparent" />
        </div>
        <div className="grid lg:grid-cols-[1fr,1.1fr] gap-6">
          {/* 左：交互演示 */}
          <div className="space-y-4">
            <div className="rounded-2xl border border-white/8 bg-white/[0.02] p-5">
              <div className="flex items-center justify-between mb-3">
                <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
                  repeatRange · 轴向区间重复
                </span>
                <Badge tone="magenta">CheckPhase</Badge>
              </div>
              <p className="text-[12px] text-white/55 leading-relaxed mb-4">
                调整 min/max 层数，看 <code className="text-magenta font-mono">CheckPhase</code> 如何在运行时
                动态决定结构要检查多少层。min 层必须存在（否则 invalid），max 层尽量多。
              </p>
              {/* min/max sliders */}
              <label className="block mb-3">
                <div className="flex justify-between mb-1">
                  <span className="text-[11.5px] text-white/70 font-mono">min layers (required)</span>
                  <span className="text-[12px] text-rose2 font-mono font-bold">{phaseMin}</span>
                </div>
                <input
                  type="range" min={1} max={6} value={phaseMin}
                  onChange={(e) => setPhaseMin(Math.min(Number(e.target.value), phaseMax))}
                  className="w-full accent-rose-400"
                />
              </label>
              <label className="block mb-3">
                <div className="flex justify-between mb-1">
                  <span className="text-[11.5px] text-white/70 font-mono">max layers (probe)</span>
                  <span className="text-[12px] text-magenta font-mono font-bold">{phaseMax}</span>
                </div>
                <input
                  type="range" min={1} max={8} value={phaseMax}
                  onChange={(e) => setPhaseMax(Math.max(Number(e.target.value), phaseMin))}
                  className="w-full accent-fuchsia-400"
                />
              </label>
              {/* 模拟实际匹配的层数 */}
              <label className="block mb-4">
                <div className="flex justify-between mb-1">
                  <span className="text-[11.5px] text-white/70 font-mono">实际存在层数（模拟世界数据）</span>
                  <span className="text-[12px] text-lime2 font-mono font-bold">{actualLayers}</span>
                </div>
                <input
                  type="range" min={0} max={8} value={actualLayers}
                  onChange={(e) => setActualLayers(Number(e.target.value))}
                  className="w-full accent-lime-400"
                />
              </label>
              {/* 结果 */}
              <div className={cn(
                "rounded-lg border p-3 text-[12px] font-mono",
                phaseResult === "valid" ? "border-lime-400/30 bg-lime-400/5 text-lime2" :
                phaseResult === "invalid" ? "border-rose-500/30 bg-rose-500/5 text-rose2" :
                "border-amber-400/30 bg-amber-400/5 text-amber2"
              )}>
                status = {phaseResult} · matched {Math.min(actualLayers, phaseMax)} / {phaseMax} layers
                {phaseResult === "invalid" && " (min 未满足)"}
                {phaseResult === "incomplete" && " (min 满足，但部分 max 层数据缺失)"}
              </div>
            </div>

            {/* 3D-ish tower preview */}
            <div className="rounded-2xl border border-white/8 bg-white/[0.02] p-5">
              <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-3">
                tower visualization
              </div>
              <TowerSvg
                min={phaseMin}
                max={phaseMax}
                actual={actualLayers}
              />
            </div>
          </div>

          {/* 右：DSL + 自定义 phase */}
          <div className="space-y-3">
            <div>
              <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
                repeatRange DSL
              </div>
              <CodeBlock
                title="AdaptiveTower.java"
                maxHeight="280px"
                code={phaseDsl}
              />
            </div>
            <div>
              <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
                完全自定义 CheckPhase
              </div>
              <CodeBlock
                title="CustomPhase.java"
                maxHeight="280px"
                code={`// CheckPhase 是通用的动态判定 API：
// 读已匹配的数据 → 判断合规性 → 写上下文 → 返回额外 point
.checkPhase(ctx -> {
    var controller = ctx.matches()
        .first(p -> p.id().value().equals("controller"));
    if (controller.isEmpty()) return List.of();

    // 根据 controller 周围世界数据动态决定结构形态
    var points = new ArrayList<StructurePointDefinition>();
    for (int r = 1; r <= 3; r++) {
        // 探测 1~3 格半径的外壳...
        points.add(casingRing(r));
    }
    // 写入计算上下文，供后续操作读取
    ctx.context().putComputed(SHELL_RADIUS, detectedRadius);
    return points;
})

// repeatRange 只是 CheckPhase 的一种便利用法：
.checkPhase(CheckPhases.repeatRange(
    "ring", Components.hollowBox("ring",
        at(-1,0,-1), at(1,0,2)),
    Axis.y, 2, 5, TOWER_HEIGHT))`}
              />
            </div>
          </div>
        </div>
      </div>
    </Section>
  );
}

/** computed 值表格（IR 风格） */
function ComputedTable({ entries }: { entries: ReturnType<typeof evaluate> }) {
  if (entries.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-white/10 p-8 text-center">
        <p className="text-[12.5px] text-white/40">勾选左侧操作后，这里展示它们写入 computed 层的值。</p>
      </div>
    );
  }
  return (
    <div className="rounded-xl border border-white/8 bg-white/[0.02] overflow-hidden">
      <div className="p-3 border-b border-white/5">
        <span className="text-[10px] uppercase tracking-wider text-white/40 font-mono">
          computed · {entries.length} 个键 → toRuntime() 合并进 RuntimeStructureContext
        </span>
      </div>
      <div className="overflow-x-auto scrollbar-thin">
        <table className="w-full text-[11.5px] font-mono">
          <thead>
            <tr className="border-b border-white/5 text-[10px] uppercase tracking-wider text-white/40">
              <th className="text-left px-3 py-1.5 font-normal">#</th>
              <th className="text-left px-3 py-1.5 font-normal">operation</th>
              <th className="text-left px-3 py-1.5 font-normal">ContextKey</th>
              <th className="text-left px-3 py-1.5 font-normal">type</th>
              <th className="text-left px-3 py-1.5 font-normal">value</th>
              <th className="text-left px-3 py-1.5 font-normal">deps</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e, i) => {
              const acc = ACCENT_CLASSES[OPS[e.op].accent];
              return (
                <motion.tr
                  key={`${e.op}-${i}`}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: i * 0.04 }}
                  className="border-b border-white/[0.03] hover:bg-white/[0.03]"
                >
                  <td className="px-3 py-1.5 text-white/30">
                    {(i + 1).toString().padStart(2, "0")}
                  </td>
                  <td className="px-3 py-1.5">
                    <span className={cn("inline-flex items-center gap-1", acc.text)}>
                      <span className={cn("w-1.5 h-1.5 rounded-full", acc.dot)} />
                      {OPS[e.op].label}
                    </span>
                  </td>
                  <td className="px-3 py-1.5 text-magenta font-semibold">{e.key}</td>
                  <td className="px-3 py-1.5 text-white/40">{e.type}</td>
                  <td className="px-3 py-1.5">
                    <code className={cn("font-bold", acc.text)}>{e.value}</code>
                  </td>
                  <td className="px-3 py-1.5 text-white/30 text-[10px]">
                    {e.dependsOn && e.dependsOn.length > 0 ? e.dependsOn.join(", ") : "—"}
                  </td>
                </motion.tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="p-2.5 text-center text-[10.5px] text-white/40 font-mono bg-white/[0.02] border-t border-white/5">
        derived ∪ computed → machine 通过 ctx.&lt;T&gt;get(key) typed 读取
      </div>
    </div>
  );
}

/** 塔状可视化：展示 repeatRange 的 min/max/actual 层 */
function TowerSvg({ min, max, actual }: { min: number; max: number; actual: number }) {
  const layers = Array.from({ length: max }, (_, i) => i);
  const layerH = 18;
  const layerW = 70;
  const gap = 2;
  const totalH = max * (layerH + gap) + 20;

  return (
    <svg viewBox={`0 0 ${layerW + 40} ${totalH}`} className="w-full h-auto max-w-[200px] mx-auto">
      {/* layers bottom→top */}
      {layers.map((layer) => {
        const y = totalH - 10 - (layer + 1) * (layerH + gap);
        const isRequired = layer < min;
        const isPresent = layer < actual;
        const isMaxProbe = layer >= min;
        let fill: string;
        let stroke: string;
        if (isPresent) {
          fill = isRequired ? "#fb7185aa" : "#a3e635aa";
          stroke = isRequired ? "#fb7185" : "#a3e635";
        } else {
          fill = isRequired ? "#fb718522" : "#ffffff08";
          stroke = isRequired ? "#fb718555" : "#ffffff15";
        }
        return (
          <g key={layer}>
            <rect
              x={20} y={y} width={layerW} height={layerH} rx={3}
              fill={fill} stroke={stroke} strokeWidth={1}
            />
            <text
              x={layerW + 26} y={y + layerH / 2 + 3}
              className="fill-white/40 font-mono"
              style={{ fontSize: 8 }}
            >
              L{layer}
            </text>
            {isRequired && (
              <text x={24} y={y + layerH / 2 + 3} className="fill-white/60 font-mono" style={{ fontSize: 7 }}>
                {isPresent ? "✓req" : "✗req"}
              </text>
            )}
            {isMaxProbe && !isRequired && (
              <text x={24} y={y + layerH / 2 + 3} className="fill-white/40 font-mono" style={{ fontSize: 7 }}>
                {isPresent ? "✓opt" : "·opt"}
              </text>
            )}
          </g>
        );
      })}
    </svg>
  );
}
