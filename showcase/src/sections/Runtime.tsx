import { motion, AnimatePresence } from "framer-motion";
import { Activity, Pause, Play, RotateCcw, SkipForward, Zap } from "lucide-react";
import { useEffect } from "react";
import { Section } from "@/components/ui/Section";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { RuntimeViewport } from "@/three/RuntimeViewport";
import { useRuntimeSim } from "@/three/runtimeSim";
import { cn } from "@/lib/utils";

function StatBox({
  label,
  value,
  color,
}: {
  label: string;
  value: number | string;
  color: string;
}) {
  return (
    <div className="rounded-xl border border-white/8 bg-white/[0.02] p-3">
      <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-1">
        {label}
      </div>
      <div className={cn("text-xl font-bold font-mono", color)}>{value}</div>
    </div>
  );
}

const TONE_CLASS = {
  ok: "border-lime-400/30 bg-lime-400/5 text-lime2",
  warn: "border-amber-400/30 bg-amber-400/5 text-amber2",
  err: "border-rose-500/30 bg-rose-500/8 text-rose2",
};

export function Runtime() {
  const tick = useRuntimeSim((s) => s.tick);
  const budget = useRuntimeSim((s) => s.budget);
  const mutationRate = useRuntimeSim((s) => s.mutationRate);
  const loadedRate = useRuntimeSim((s) => s.loadedRate);
  const paused = useRuntimeSim((s) => s.paused);
  const mode = useRuntimeSim((s) => s.mode);
  const stats = useRuntimeSim((s) => s.stats);
  const log = useRuntimeSim((s) => s.log);
  const setBudget = useRuntimeSim((s) => s.setBudget);
  const setMutation = useRuntimeSim((s) => s.setMutation);
  const setLoaded = useRuntimeSim((s) => s.setLoaded);
  const togglePause = useRuntimeSim((s) => s.togglePause);
  const toggleMode = useRuntimeSim((s) => s.toggleMode);
  const reset = useRuntimeSim((s) => s.reset);
  const isFast = mode === "fastTick";

  return (
    <Section
      id="runtime"
      index="05"
      title={
        <>
          运行时调度器：
          <span className="text-white/50"> 脏 Section × Tick 预算 × 请求账本</span>
        </>
      }
      lede={
        <>
          真实服务器上 1 个 tick 只有 50ms。下方是一个可交互的"8×8 区块网格"，
          每个区块有几率被玩家改动而变{" "}
          <em className="text-amber2">dirty</em>。调度器每 tick 仅消费{" "}
          <strong className="text-brand-300">budgetPerTick</strong> 个检查；
          多余 dirty 进入下一帧排队。没加载的区块会被记入{" "}
          <em className="text-cyan2">RequestLedger</em>，而不会强制载入。
        </>
      }
    >
      <div className="grid lg:grid-cols-[1.3fr,1fr] gap-5">
        {/* 3D viewport */}
        <div className="relative rounded-2xl border border-white/8 bg-ink-950/80 overflow-hidden h-[480px]">
          <RuntimeViewport />
          <div className="absolute top-4 left-4 flex flex-col gap-2">
            <div className="chip bg-ink-950/70 backdrop-blur-md">
              <span className={cn("w-1.5 h-1.5 rounded-full", paused ? "bg-rose2" : "bg-lime2 animate-pulse")} />
              <span className="font-mono text-[11px]">
                {paused ? "PAUSED" : "LIVE"} · tick {tick}
              </span>
            </div>
            <div className="chip bg-ink-950/70 backdrop-blur-md">
              <Zap className={cn("w-3 h-3", isFast ? "text-amber2" : "text-white/40")} />
              <span className="font-mono text-[11px] text-white/60">
                {mode} · budget {budget}
              </span>
            </div>
          </div>

          {/* legend */}
          <div className="absolute bottom-4 left-4 right-4 flex flex-wrap gap-x-3 gap-y-1 text-[10.5px] font-mono">
            <LegendItem color="#a3e635" label="valid" />
            <LegendItem color="#fb7185" label="invalid" />
            <LegendItem color="#fbbf24" label="incomplete" />
            <LegendItem color="#7c5cff" label="stale (待检)" />
          </div>
        </div>

        {/* controls */}
        <div className="space-y-4">
          <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-4">
            <div className="flex items-center gap-2 mb-3">
              <Activity className="w-4 h-4 text-brand-300" />
              <span className="text-[13px] font-semibold">调度器参数</span>
              {isFast && <Badge tone="amber">fastTick</Badge>}
            </div>

            <label className="block mb-3">
              <div className="flex justify-between mb-1">
                <span className="text-[11.5px] text-white/70 font-mono">Tick Budget</span>
                <span className="text-[12px] text-brand-300 font-mono font-bold">{budget}</span>
              </div>
              <input
                type="range"
                min={1}
                max={32}
                value={budget}
                onChange={(e) => setBudget(Number(e.target.value))}
                className="w-full accent-brand-400"
              />
              <div className="text-[10px] text-white/40 mt-0.5 font-mono">
                每 tick 最多检查 N 个 section，超出排队下一帧
              </div>
            </label>

            <label className="block mb-3">
              <div className="flex justify-between mb-1">
                <span className="text-[11.5px] text-white/70 font-mono">Mutation Rate</span>
                <span className="text-[12px] text-amber2 font-mono font-bold">{mutationRate}%</span>
              </div>
              <input
                type="range"
                min={0}
                max={100}
                value={mutationRate}
                onChange={(e) => setMutation(Number(e.target.value))}
                className="w-full accent-amber-400"
              />
              <div className="text-[10px] text-white/40 mt-0.5 font-mono">
                模拟玩家改动方块的概率
              </div>
            </label>

            <label className="block">
              <div className="flex justify-between mb-1">
                <span className="text-[11.5px] text-white/70 font-mono">Loaded Chunk %</span>
                <span className="text-[12px] text-cyan2 font-mono font-bold">{loadedRate}%</span>
              </div>
              <input
                type="range"
                min={20}
                max={100}
                value={loadedRate}
                onChange={(e) => setLoaded(Number(e.target.value))}
                className="w-full accent-cyan-400"
              />
              <div className="text-[10px] text-white/40 mt-0.5 font-mono">
                未加载的 section 会发出 DataRequest 而不强制载入
              </div>
            </label>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button variant="primary" onClick={togglePause}>
              {paused ? <Play className="w-3.5 h-3.5" /> : <Pause className="w-3.5 h-3.5" />}
              {paused ? "Resume" : "Pause"}
            </Button>
            <Button variant="outline" onClick={() => useRuntimeSim.getState().step()}>
              <SkipForward className="w-3.5 h-3.5" /> Step
            </Button>
            <Button variant="outline" onClick={reset}>
              <RotateCcw className="w-3.5 h-3.5" /> Reset
            </Button>
            <Button variant={isFast ? "primary" : "outline"} onClick={toggleMode}>
              <Zap className="w-3.5 h-3.5" /> {isFast ? "fastTick" : "tick"}
            </Button>
          </div>

          {/* stats */}
          <div className="grid grid-cols-3 gap-2">
            <StatBox label="checked" value={stats.checked} color="text-cyan2" />
            <StatBox label="requests" value={stats.requests} color="text-brand-300" />
            <StatBox label="stale" value={stats.stale} color="text-brand-300" />
            <StatBox label="valid" value={stats.valid} color="text-lime2" />
            <StatBox label="invalid" value={stats.invalid} color="text-rose2" />
            <StatBox label="incomplete" value={stats.incomplete} color="text-amber2" />
          </div>

          {/* log */}
          <div className="rounded-2xl border border-white/8 bg-ink-950/80 p-3">
            <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-2">
              engine log
            </div>
            <div className="space-y-1 max-h-[160px] overflow-auto scrollbar-thin">
              <AnimatePresence initial={false}>
                {log.map((l, i) => (
                  <motion.div
                    key={`${l.tick}-${i}`}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0 }}
                    className={cn(
                      "text-[11px] font-mono p-1.5 rounded border",
                      TONE_CLASS[l.tone]
                    )}
                  >
                    <span className="text-white/30">t{l.tick.toString().padStart(3, "0")}</span>
                    {" "}
                    {l.msg}
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>
          </div>
        </div>
      </div>
    </Section>
  );
}

function LegendItem({ color, label }: { color: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-ink-950/70 backdrop-blur-md">
      <span
        className="w-2 h-2 rounded-sm"
        style={{ background: color }}
      />
      <span className="text-white/60">{label}</span>
    </span>
  );
}
