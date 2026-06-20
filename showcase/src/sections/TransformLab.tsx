import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useMemo } from "react";
import { Section } from "@/components/ui/Section";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { TransformLabViewport } from "@/three/TransformLabViewport";
import {
  TRANSFORMS,
  useTransformLab,
  evaluateTransform,
  type TransformId,
} from "@/three/transformLab";
import { cn } from "@/lib/utils";

const accentForT: Record<string, string> = {
  identity: "brand",
  facing_n: "cyan",
  facing_e: "cyan",
  facing_s: "cyan",
  facing_w: "cyan",
  rotate_y_90: "magenta",
  rotate_y_180: "magenta",
  rotate_y_270: "magenta",
  mirror_x: "lime",
  mirror_z: "lime",
};

const badgeTone = {
  brand: "brand",
  cyan: "cyan",
  magenta: "magenta",
  lime: "lime",
  amber: "amber",
  rose: "rose",
} as const;

export function TransformLab() {
  const selected = useTransformLab((s) => s.selected);
  const setSelected = useTransformLab((s) => s.setSelected);
  const enabled = useTransformLab((s) => s.enabled);
  const toggle = useTransformLab((s) => s.toggle);
  const enableAll = useTransformLab((s) => s.enableAll);
  const disableAll = useTransformLab((s) => s.disableAll);

  const current = TRANSFORMS.find((t) => t.id === selected)!;
  const currentEval = useMemo(() => evaluateTransform(current), [current]);

  // 求所有 enabled transforms的结果
  const allEvals = useMemo(
    () => TRANSFORMS.filter((t) => enabled[t.id]).map(evaluateTransform),
    [enabled]
  );

  // 第一个 PASS 的（短路语义）
  const shortCircuitWinner = allEvals.find((e) => e.passed) ?? null;

  return (
    <Section
      id="transform-lab"
      index="08·5"
      alt
      title={
        <>
          Transform Lab：
          <span className="text-white/50"> 看 check plan 怎么短路 transform 候选</span>
        </>
      }
      lede={
        <>
          一个 structure 会声明多个 transform 候选（identity / horizontalFacing N/E/S/W / rotateY90 ...）。
          check plan 按<strong className="text-white/80">声明顺序</strong>逐个尝试，
          第一个让所有 point 都匹配的候选立即短路返回 VALID。
          下方左侧切换 transform 看结构如何变形，右侧看每个候选的通过/失败 + 短路位置。
        </>
      }
    >
      <div className="grid lg:grid-cols-[1.3fr,1fr] gap-4">
        {/* LEFT: 3D scene + transform picker */}
        <div className="space-y-3">
          <div className="relative rounded-2xl border border-white/8 bg-ink-950/80 overflow-hidden h-[460px]">
            <TransformLabViewport />
            <div className="absolute top-3 left-3 flex flex-col gap-1 pointer-events-none">
              <div className="chip bg-ink-950/70 backdrop-blur-md pointer-events-auto">
                <span className="w-1.5 h-1.5 rounded-full" style={{ background: current.color }} />
                <span className="font-mono text-[11px] text-white/70">
                  {current.label}
                </span>
              </div>
              <div className={cn(
                "chip backdrop-blur-md pointer-events-auto",
                currentEval.passed ? "bg-lime-400/15 text-lime2" : "bg-rose-500/15 text-rose2"
              )}>
                {currentEval.passed ? (
                  <>
                    <Icons.Check className="w-3 h-3" />
                    <span className="font-mono text-[11px]">ALL MATCH</span>
                  </>
                ) : (
                  <>
                    <Icons.X className="w-3 h-3" />
                    <span className="font-mono text-[11px]">fail @ #{currentEval.firstFailIndex + 1}</span>
                  </>
                )}
              </div>
            </div>
            <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between gap-2 pointer-events-none">
              <div className="font-mono text-[10px] text-white/40 bg-ink-950/70 backdrop-blur-md rounded px-2 py-1">
                anchor @ (0,0,0) · 雾色 = 世界方块 · 实色 = 转换后结构
              </div>
            </div>
          </div>

          {/* transform picker grid */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
                pick a transform
              </span>
              <div className="flex gap-1">
                <Button variant="ghost" size="sm" onClick={enableAll}>
                  <Icons.CheckCheck className="w-3 h-3" /> enable all
                </Button>
                <Button variant="ghost" size="sm" onClick={disableAll}>
                  <Icons.X className="w-3 h-3" /> clear
                </Button>
              </div>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
              {TRANSFORMS.map((t) => {
                const isSelected = selected === t.id;
                const isEnabled = enabled[t.id];
                return (
                  <button
                    key={t.id}
                    onClick={() => setSelected(t.id)}
                    onDoubleClick={() => toggle(t.id)}
                    title={`${t.description} · 双击启用/禁用`}
                    className={cn(
                      "rounded-lg border p-2 text-left transition-all",
                      isSelected
                        ? "bg-white/[0.08] border-white/25"
                        : isEnabled
                        ? "bg-white/[0.03] border-white/10"
                        : "bg-white/[0.01] border-white/5 opacity-40"
                    )}
                  >
                    <div className="flex items-center gap-1.5 mb-0.5">
                      <span
                        className="w-1.5 h-1.5 rounded-full shrink-0"
                        style={{ background: t.color }}
                      />
                      <code
                        className="font-mono text-[10.5px] font-semibold truncate"
                        style={{ color: t.color }}
                      >
                        {t.label}
                      </code>
                      <input
                        type="checkbox"
                        checked={isEnabled}
                        onChange={() => toggle(t.id)}
                        onClick={(e) => e.stopPropagation()}
                        className="ml-auto accent-brand-400"
                      />
                    </div>
                    <div className="text-[10px] text-white/45 leading-tight truncate">
                      {t.description}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* RIGHT: candidate evaluation list */}
        <div className="space-y-3">
          {/* short-circuit result */}
          <div className={cn(
            "rounded-2xl border p-4",
            shortCircuitWinner
              ? "border-lime-400/30 bg-lime-400/[0.05]"
              : "border-rose-500/30 bg-rose-500/[0.05]"
          )}>
            <div className="flex items-center gap-2 mb-2">
              {shortCircuitWinner ? (
                <Icons.Zap className="w-4 h-4 text-lime2" />
              ) : (
                <Icons.X className="w-4 h-4 text-rose2" />
              )}
              <span className="text-[12.5px] font-semibold">
                {shortCircuitWinner ? "fastCheck 短路结果" : "无候选通过"}
              </span>
            </div>
            {shortCircuitWinner ? (
              <>
                <div className="font-mono text-[13px] text-lime2 mb-1">
                  ✓ {shortCircuitWinner.transform.label}
                </div>
                <div className="text-[11.5px] text-white/60">
                  这是 plan.candidates 中第一个让所有 point 都匹配的候选。
                  fastCheck 跳过其余 {allEvals.length - 1} 个候选的所有 pointBound 检查，立即返回 VALID。
                </div>
              </>
            ) : (
              <>
                <div className="font-mono text-[13px] text-rose2 mb-1">
                  ✗ TRANSFORM_REJECTED
                </div>
                <div className="text-[11.5px] text-white/60">
                  所有启用的候选都不能让结构匹配。玩家会看到{" "}
                  <code className="text-rose2">DiagnosticGraph</code> 包含每个候选的最前失败位置。
                </div>
              </>
            )}
          </div>

          {/* candidate list */}
          <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-3">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Icons.ListChecks className="w-3.5 h-3.5 text-brand-300" />
                <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
                  candidate evaluation
                </span>
              </div>
              <span className="text-[10.5px] text-white/40 font-mono">
                {allEvals.filter((e) => e.passed).length}/{allEvals.length} pass
              </span>
            </div>

            <div className="space-y-1.5">
              <AnimatePresence initial={false}>
                {allEvals.map((e, idx) => {
                  const t = e.transform;
                  const isSelected = selected === t.id;
                  const isWinner = shortCircuitWinner?.transform.id === t.id;
                  return (
                    <motion.button
                      key={t.id}
                      layout
                      onClick={() => setSelected(t.id)}
                      className={cn(
                        "w-full text-left rounded-lg border p-2.5 transition-all flex items-center gap-2",
                        isSelected
                          ? "bg-white/[0.06] border-white/25"
                          : isWinner
                          ? "bg-lime-400/[0.05] border-lime-400/30"
                          : "bg-white/[0.02] border-white/8 hover:bg-white/[0.04]"
                      )}
                    >
                      <span className="font-mono text-[10px] text-white/30 w-5">
                        {(idx + 1).toString().padStart(2, "0")}
                      </span>
                      <span
                        className="w-1.5 h-1.5 rounded-full shrink-0"
                        style={{ background: t.color }}
                      />
                      <code
                        className="font-mono text-[11.5px] font-semibold flex-1 min-w-0 truncate"
                        style={{ color: t.color }}
                      >
                        {t.label}
                      </code>
                      {e.passed ? (
                        <>
                          {isWinner && (
                            <Badge tone="lime">
                              <Icons.Zap className="w-2.5 h-2.5" /> short-circuit
                            </Badge>
                          )}
                          <span className="font-mono text-[11px] text-lime2">
                            ✓ {e.matchCount}/{e.totalRequired}
                          </span>
                        </>
                      ) : (
                        <span className="font-mono text-[11px] text-rose2">
                          ✗ @{e.firstFailIndex + 1}
                        </span>
                      )}
                    </motion.button>
                  );
                })}
              </AnimatePresence>
            </div>
          </div>

          {/* current transform details */}
          <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-3">
            <div className="flex items-center gap-2 mb-2">
              <span
                className="w-1.5 h-5 rounded-full"
                style={{ background: current.color }}
              />
              <span className="text-[11px] uppercase tracking-wider text-white/50 font-mono">
                current · {current.label}
              </span>
            </div>
            <p className="text-[12px] text-white/60 leading-relaxed mb-2">
              {current.description}
            </p>
            <div className="space-y-1 font-mono text-[11px]">
              {currentEval.placedBlocks.map((b, i) => (
                <div
                  key={i}
                  className={cn(
                    "flex items-center gap-2 px-2 py-1 rounded border",
                    b.matched
                      ? "border-lime-400/20 bg-lime-400/[0.03]"
                      : i === currentEval.firstFailIndex
                      ? "border-rose-500/30 bg-rose-500/[0.05]"
                      : "border-white/5 bg-white/[0.02] opacity-50"
                  )}
                >
                  <span className="text-white/30 w-6">
                    {(i + 1).toString().padStart(2, "0")}
                  </span>
                  {b.matched ? (
                    <Icons.Check className="w-3 h-3 text-lime2" />
                  ) : i === currentEval.firstFailIndex ? (
                    <Icons.X className="w-3 h-3 text-rose2" />
                  ) : (
                    <Icons.Minus className="w-3 h-3 text-white/30" />
                  )}
                  <span style={{ color: b.color }}>●</span>
                  <span className="text-white/70 w-24 truncate">{b.label}</span>
                  <span className="text-white/30">
                    {b.rel.join(",")} → {b.world.join(",")}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </Section>
  );
}
