import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useEffect, useMemo } from "react";
import { Section } from "@/components/ui/Section";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { JsonEditor } from "@/components/JsonEditor";
import { StructurePreview } from "@/three/StructurePreview";
import {
  JSON_PRESETS,
  JSON_PRESET_NAMES,
  useDataDriven,
  type ReloadStage,
} from "@/three/dataDrivenStore";
import { cn } from "@/lib/utils";

const STAGES: { id: ReloadStage; label: string; sub: string; icon: string; color: string }[] = [
  { id: "scanning", label: "scan", sub: "data/*/cumulus/structures/*.json", icon: "FolderSearch", color: "#7c5cff" },
  { id: "parsing", label: "parse", sub: "RuntimeAssetJsonLoader", icon: "Braces", color: "#22d3ee" },
  { id: "validating", label: "validate", sub: "schema + structure rules", icon: "ShieldCheck", color: "#fbbf24" },
  { id: "indexing", label: "index", sub: "RuntimeAssetRepository", icon: "Database", color: "#a3e635" },
  { id: "registering", label: "register", sub: "RuntimeAssetEngineBridge", icon: "Plug", color: "#f0abfc" },
  { id: "live", label: "live", sub: "RuntimeCheckEngine", icon: "Activity", color: "#22d3ee" },
];

function StageRow({
  stage,
  active,
  done,
  failed,
  progress,
}: {
  stage: (typeof STAGES)[number];
  active: boolean;
  done: boolean;
  failed: boolean;
  progress: number;
}) {
  const Icon = (Icons as any)[stage.icon];
  const accentColor = failed ? "#fb7185" : stage.color;

  return (
    <motion.div
      layout
      className={cn(
        "relative flex items-center gap-2 p-2.5 rounded-xl border transition-all",
        active
          ? "bg-white/[0.06] border-white/20"
          : done
          ? "bg-white/[0.02] border-white/10"
          : failed
          ? "bg-rose-500/8 border-rose-500/30"
          : "bg-white/[0.01] border-white/5 opacity-50"
      )}
    >
      <div
        className={cn(
          "w-7 h-7 rounded-md flex items-center justify-center border shrink-0",
          active && "animate-pulse"
        )}
        style={{
          background: accentColor + "20",
          borderColor: accentColor + "40",
        }}
      >
        {done ? (
          <Icons.Check className="w-3.5 h-3.5 text-lime2" />
        ) : failed ? (
          <Icons.X className="w-3.5 h-3.5 text-rose2" />
        ) : (
          <Icon className="w-3.5 h-3.5" style={{ color: accentColor }} />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-baseline justify-between gap-2">
          <code
            className="font-mono text-[12px] font-semibold"
            style={{ color: active || done ? accentColor : "#9999b0" }}
          >
            {stage.label}
          </code>
          {active && (
            <span className="text-[10px] font-mono text-white/50">
              {Math.round(progress * 100)}%
            </span>
          )}
        </div>
        <div className="font-mono text-[10px] text-white/40 truncate">
          {stage.sub}
        </div>
        {active && (
          <div className="mt-1 h-0.5 bg-white/10 rounded-full overflow-hidden">
            <motion.div
              className="h-full"
              style={{ background: accentColor }}
              animate={{ width: `${progress * 100}%` }}
              transition={{ duration: 0.1 }}
            />
          </div>
        )}
      </div>
    </motion.div>
  );
}

function Stat({
  label,
  value,
  color,
}: {
  label: string;
  value: string | number;
  color: string;
}) {
  return (
    <div className="rounded-lg bg-white/[0.02] border border-white/8 p-2">
      <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono">
        {label}
      </div>
      <div className={cn("text-[15px] font-bold font-mono", color)}>{value}</div>
    </div>
  );
}

const LEVEL_COLOR = {
  info: "text-white/50",
  ok: "text-lime2",
  warn: "text-amber2",
  error: "text-rose2",
};

export function DataDriven() {
  const rawJson = useDataDriven((s) => s.rawJson);
  const setRawJson = useDataDriven((s) => s.setRawJson);
  const loadPreset = useDataDriven((s) => s.loadPreset);
  const triggerReload = useDataDriven((s) => s.triggerReload);
  const displayed = useDataDriven((s) => s.displayed);
  const stage = useDataDriven((s) => s.stage);
  const stageProgress = useDataDriven((s) => s.stageProgress);
  const log = useDataDriven((s) => s.log);
  const reloads = useDataDriven((s) => s.reloads);
  const lastReloadMs = useDataDriven((s) => s.lastReloadMs);
  const diagnostics = useDataDriven((s) => s.diagnostics);
  const structureId = useDataDriven((s) => s.structureId);

  // 第一次加载，预热一次 reload
  useEffect(() => {
    if (displayed === null && stage === "live") {
      // already loaded by store default
    }
  }, [displayed, stage]);

  const currentStageIdx = STAGES.findIndex((s) => s.id === stage);
  const isRunning = stage !== "idle" && stage !== "live";
  const errorLine = diagnostics.find((d) => d.line > 0)?.line;
  const errorColumn = diagnostics.find((d) => d.line > 0)?.column;

  // stats
  const points = useMemo(() => {
    if (!displayed) return 0;
    return displayed.components.reduce((acc, c) => {
      if (c.kind === "point") return acc + 1;
      if (c.kind === "region") return acc + 1;
      const dx = c.to[0] - c.from[0] + 1;
      const dy = c.to[1] - c.from[1] + 1;
      const dz = c.to[2] - c.from[2] + 1;
      if (c.kind === "box") return acc + dx * dy * dz;
      if (c.kind === "hollowBox") {
        const onShell = dx * dy * dz - Math.max(0, dx - 2) * Math.max(0, dy - 2) * Math.max(0, dz - 2);
        return acc + onShell;
      }
      return acc;
    }, 0);
  }, [displayed]);

  return (
    <Section
      id="datadriven"
      index="11·5"
      title={
        <>
          数据驱动热重载实验室：
          <span className="text-white/50"> 改 JSON → 看管线 → 3D 实时更新</span>
        </>
      }
      lede={
        <>
          这一段还原了 <strong className="text-white/80">Cumulus 在服务器上真实的 datapack reload 路径</strong>：
          左侧编辑 <code className="font-mono text-cyan2">data/*/cumulus/structures/*.json</code>，
          点 "Reload"，6 个阶段（扫描 → 解析 → 校验 → 索引 → 注册 → 上线）依次推进，
          中间 3D 视图随 reload 完成<strong className="text-white/80">原子替换</strong>。
          故意改错 JSON 会停在 parse/validate 阶段并保留旧结构。
        </>
      }
    >
      {/* preset bar */}
      <div className="flex flex-wrap items-center gap-2 mb-5">
        <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
          预设:
        </span>
        {JSON_PRESET_NAMES.map((p) => {
          const isActive = rawJson === JSON_PRESETS[p].json;
          return (
            <button
              key={p}
              onClick={() => loadPreset(p)}
              title={JSON_PRESETS[p].description}
              className={cn(
                "px-2.5 py-1 rounded-full text-[11.5px] font-mono border transition-all",
                isActive
                  ? "bg-cyan-400/15 border-cyan-400/30 text-cyan2"
                  : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
              )}
            >
              {JSON_PRESETS[p].label}
            </button>
          );
        })}
        <div className="flex-1" />
        <Button
          variant={isRunning ? "outline" : "primary"}
          onClick={triggerReload}
          disabled={isRunning}
        >
          {isRunning ? (
            <>
              <Icons.Loader2 className="w-3.5 h-3.5 animate-spin" />
              reloading...
            </>
          ) : (
            <>
              <Icons.RefreshCw className="w-3.5 h-3.5" />
              Reload
            </>
          )}
        </Button>
      </div>

      <div className="grid lg:grid-cols-[1fr,1fr,280px] gap-4">
        {/* LEFT: JSON editor */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <Icons.FileJson className="w-4 h-4 text-cyan2" />
              <span className="text-[12.5px] font-semibold">datapack asset</span>
              <Badge tone="cyan">editable</Badge>
            </div>
            {diagnostics.length > 0 && (
              <Badge tone="rose">{diagnostics.length} diagnostic(s)</Badge>
            )}
          </div>
          <JsonEditor
            value={rawJson}
            onChange={setRawJson}
            errorLine={errorLine}
            errorColumn={errorColumn}
            className="h-[560px]"
          />
        </div>

        {/* CENTER: 3D preview */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <Icons.Box className="w-4 h-4 text-brand-300" />
              <span className="text-[12.5px] font-semibold">runtime preview</span>
              <Badge tone={stage === "live" ? "lime" : isRunning ? "amber" : "rose"}>
                {stage === "live" ? "LIVE" : isRunning ? "RELOADING" : "IDLE"}
              </Badge>
            </div>
            <span className="text-[10.5px] font-mono text-white/40">
              {displayed?.id ?? "(none)"}
            </span>
          </div>

          <div className="relative rounded-2xl border border-white/8 bg-ink-950/80 overflow-hidden h-[400px]">
            <AnimatePresence mode="wait">
              <motion.div
                key={displayed?.id ?? "empty"}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="absolute inset-0"
              >
                {displayed ? (
                  <StructurePreview structure={displayed} />
                ) : (
                  <div className="flex items-center justify-center h-full text-white/30 text-[12px]">
                    等待第一次 reload…
                  </div>
                )}
              </motion.div>
            </AnimatePresence>

            {isRunning && (
              <div className="absolute inset-0 bg-ink-950/60 backdrop-blur-sm flex items-center justify-center">
                <div className="text-center">
                  <Icons.Loader2 className="w-8 h-8 mx-auto text-brand-300 animate-spin mb-2" />
                  <div className="text-[12px] font-mono text-white/70">
                    stage: <span className="text-brand-300">{stage}</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* stats below 3D */}
          <div className="grid grid-cols-4 gap-1.5 mt-2">
            <Stat label="points" value={points} color="text-brand-300" />
            <Stat label="components" value={displayed?.components.length ?? 0} color="text-cyan2" />
            <Stat label="transforms" value={displayed?.transforms.length ?? 0} color="text-magenta" />
            <Stat label="schema" value={"v" + (displayed?.schemaVersion ?? 0)} color="text-lime2" />
          </div>
        </div>

        {/* RIGHT: pipeline stages */}
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Icons.GitBranch className="w-4 h-4 text-magenta" />
            <span className="text-[12.5px] font-semibold">reload pipeline</span>
          </div>
          <div className="space-y-1.5">
            {STAGES.map((s, i) => {
              const idx = currentStageIdx;
              const isActive = isRunning && stage === s.id;
              const isDone =
                (stage === "live" && i <= idx) ||
                (isRunning && i < idx);
              const isFailed = !isRunning && stage === "idle" && diagnostics.length > 0 && i === idx;
              return (
                <StageRow
                  key={s.id}
                  stage={s}
                  active={isActive}
                  done={isDone}
                  failed={isFailed}
                  progress={isActive ? stageProgress : isDone ? 1 : 0}
                />
              );
            })}
          </div>

          {/* diagnostics */}
          {diagnostics.length > 0 && (
            <div className="mt-3 rounded-xl border border-rose-500/30 bg-rose-500/5 p-3">
              <div className="text-[10px] uppercase tracking-wider text-rose2 font-mono mb-1.5">
                DiagnosticGraph
              </div>
              <ul className="space-y-1">
                {diagnostics.map((d, i) => (
                  <li key={i} className="text-[11.5px] font-mono text-rose2">
                    {d.line > 0 && (
                      <span className="text-white/40 mr-1">L{d.line}{d.column ? `:${d.column}` : ""}</span>
                    )}
                    {d.msg}
                  </li>
                ))}
              </ul>
              <div className="mt-2 text-[10.5px] text-white/50 italic">
                引擎保留上一次有效 snapshot，不替换 3D 预览。
              </div>
            </div>
          )}

          {/* reload stats */}
          <div className="mt-3 grid grid-cols-2 gap-1.5">
            <Stat label="reload #" value={reloads} color="text-cyan2" />
            <Stat label="last (ms)" value={lastReloadMs.toFixed(0)} color="text-lime2" />
          </div>
        </div>
      </div>

      {/* log */}
      <div className="mt-4 rounded-2xl border border-white/8 bg-ink-950/80 p-3">
        <div className="flex items-center gap-2 mb-2">
          <Icons.Terminal className="w-3.5 h-3.5 text-lime2" />
          <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
            engine log · RuntimeAssetReloadListener
          </span>
        </div>
        <div className="font-mono text-[11.5px] space-y-0.5 max-h-[200px] overflow-auto scrollbar-thin">
          <AnimatePresence initial={false}>
            {log.map((l, i) => (
              <motion.div
                key={`${l.ts}-${i}`}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                className={cn("flex items-baseline gap-2", LEVEL_COLOR[l.level])}
              >
                <span className="text-white/30 shrink-0">
                  [{new Date(l.ts).toLocaleTimeString("en-US", { hour12: false })}]
                </span>
                <span className="text-white/40 shrink-0 w-20">[{l.stage}]</span>
                <span className="flex-1">{l.msg}</span>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>

      {/* explainer */}
      <div className="mt-5 grid md:grid-cols-3 gap-3">
        <div className="rounded-xl border border-cyan-400/20 bg-cyan-400/5 p-3">
          <div className="text-[11px] uppercase tracking-wider text-cyan2 font-mono mb-1">
            玩家可以做什么
          </div>
          <p className="text-[12px] text-white/65 leading-relaxed">
            在自己的 datapack 里覆盖任意 Cumulus 结构 JSON：平衡数值、改方块 tag、加新机器。
            服务器 <code className="text-cyan2">/reload</code> 命令即生效，无需 mod 重新发布。
          </p>
        </div>
        <div className="rounded-xl border border-lime-400/20 bg-lime-400/5 p-3">
          <div className="text-[11px] uppercase tracking-wider text-lime2 font-mono mb-1">
            原子性
          </div>
          <p className="text-[12px] text-white/65 leading-relaxed">
            RuntimeAssetRepository 在所有 JSON 都通过 validate 后才替换 active snapshot。
            中途任何一个失败，旧 snapshot 保留，引擎继续运行不受影响。
          </p>
        </div>
        <div className="rounded-xl border border-rose-500/20 bg-rose-500/5 p-3">
          <div className="text-[11px] uppercase tracking-wider text-rose2 font-mono mb-1">
            失败可观测
          </div>
          <p className="text-[12px] text-white/65 leading-relaxed">
            解析/校验失败都会进 DiagnosticGraph。服务器日志、玩家聊天、可视化 overlay、datagen CI 都拿到同一份诊断。
          </p>
        </div>
      </div>
    </Section>
  );
}
