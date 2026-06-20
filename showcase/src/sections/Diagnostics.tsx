import { motion, AnimatePresence } from "framer-motion";
import { AlertCircle, AlertTriangle, Info } from "lucide-react";
import { useMemo, useState } from "react";
import { Section } from "@/components/ui/Section";
import { Badge } from "@/components/ui/Badge";
import { DIAG_NODES, DIAG_EDGES, DIAGNOSTIC_CODES, type DiagNode } from "@/data/diagnostics";
import { cn } from "@/lib/utils";

const severityColor = {
  ERROR: { fill: "#fb7185", text: "text-rose2", bg: "bg-rose-500/10", border: "border-rose-500/30" },
  WARN: { fill: "#fbbf24", text: "text-amber2", bg: "bg-amber-400/10", border: "border-amber-400/30" },
  INFO: { fill: "#22d3ee", text: "text-cyan2", bg: "bg-cyan-400/10", border: "border-cyan-400/30" },
};

const NODE_POSITIONS: Record<string, { x: number; y: number }> = {
  root: { x: 50, y: 8 },
  "rule-controller": { x: 18, y: 28 },
  "rule-casing": { x: 50, y: 28 },
  "rule-energy": { x: 82, y: 28 },
  "rule-remote": { x: 90, y: 60 },
  "point-ctrl": { x: 12, y: 55 },
  "evidence-ctrl": { x: 6, y: 80 },
  "point-casing-n": { x: 38, y: 55 },
  "point-casing-e": { x: 56, y: 55 },
  "evidence-casing-e": { x: 65, y: 78 },
  "point-energy": { x: 82, y: 55 },
  "point-remote": { x: 92, y: 78 },
};

function DiagnosticGraphSvg({
  active,
  onSelect,
}: {
  active: string | null;
  onSelect: (n: DiagNode) => void;
}) {
  const nodes = DIAG_NODES;
  const edges = DIAG_EDGES;

  return (
    <svg viewBox="0 0 100 100" className="w-full h-full" preserveAspectRatio="xMidYMid meet">
      <defs>
        <marker
          id="arrow"
          viewBox="0 0 10 10"
          refX="8"
          refY="5"
          markerWidth="5"
          markerHeight="5"
          orient="auto"
        >
          <path d="M 0 0 L 10 5 L 0 10 z" fill="#3a3a72" />
        </marker>
        <filter id="glow">
          <feGaussianBlur stdDeviation="0.8" result="coloredBlur" />
          <feMerge>
            <feMergeNode in="coloredBlur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      {/* edges */}
      {edges.map((e, i) => {
        const from = NODE_POSITIONS[e.from];
        const to = NODE_POSITIONS[e.to];
        if (!from || !to) return null;
        const isActive = active === e.from || active === e.to;
        return (
          <g key={i}>
            <line
              x1={from.x}
              y1={from.y}
              x2={to.x}
              y2={to.y}
              stroke={isActive ? "#7c5cff" : "#272750"}
              strokeWidth={isActive ? 0.5 : 0.3}
              markerEnd="url(#arrow)"
              opacity={active && !isActive ? 0.2 : 0.8}
              vectorEffect="non-scaling-stroke"
            />
            <text
              x={(from.x + to.x) / 2}
              y={(from.y + to.y) / 2 - 0.5}
              fontSize="1.5"
              fill="#5a5a8a"
              textAnchor="middle"
              className="font-mono"
              opacity={active && !isActive ? 0.2 : 0.7}
            >
              {e.label}
            </text>
          </g>
        );
      })}

      {/* nodes */}
      {nodes.map((n) => {
        const pos = NODE_POSITIONS[n.id];
        if (!pos) return null;
        const sc = severityColor[n.severity];
        const isActive = active === n.id;
        const isRoot = n.id === "root";
        const radius = isRoot ? 3 : 2.2;

        return (
          <g
            key={n.id}
            style={{ cursor: "pointer" }}
            onClick={() => onSelect(n)}
            filter={isActive ? "url(#glow)" : undefined}
          >
            <circle
              cx={pos.x}
              cy={pos.y}
              r={radius + (isActive ? 0.8 : 0.4)}
              fill={sc.fill}
              opacity={active && !isActive ? 0.25 : 1}
              stroke="#000"
              strokeWidth={0.2}
            />
            <circle
              cx={pos.x}
              cy={pos.y}
              r={radius}
              fill="#0c0c1a"
              opacity={0.85}
            />
            <text
              x={pos.x}
              y={pos.y + 0.6}
              fontSize="1.4"
              fill={sc.fill}
              textAnchor="middle"
              className="font-mono"
              fontWeight="bold"
            >
              {n.code.split("_")[0][0]}
            </text>
            <text
              x={pos.x}
              y={pos.y + radius + 2}
              fontSize="1.5"
              fill={isActive ? "#ffffff" : "#9999c0"}
              textAnchor="middle"
              className="font-mono"
              opacity={active && !isActive ? 0.4 : 1}
            >
              {n.label.length > 16 ? n.label.slice(0, 14) + "…" : n.label}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

export function Diagnostics() {
  const [active, setActive] = useState<DiagNode | null>(null);

  return (
    <Section
      id="diagnostics"
      index="08"
      alt
      title={
        <>
          诊断图：
          <span className="text-white/50"> 失败为什么不是一个 string</span>
        </>
      }
      lede={
        <>
          传统多方块诊断是 <code className="text-rose2 font-mono">"multiblock incomplete"</code>。
          Cumulus 输出的是 <strong className="text-brand-300">DiagnosticGraph</strong>：节点带 code / severity / 本地化 key / 证据 / 修复提示，
          边连接 rule → predicate → evidence。同一张图可投影成开发者日志、玩家提示、可视化 overlay、datagen 校验。
        </>
      }
    >
      <div className="grid lg:grid-cols-[1.4fr,1fr] gap-5">
        {/* graph */}
        <div className="relative rounded-2xl border border-white/8 bg-gradient-to-br from-ink-950 to-ink-900/50 p-4 h-[520px]">
          <div className="absolute top-4 left-4 z-10">
            <Badge tone="rose">DiagnosticGraph</Badge>
          </div>
          <DiagnosticGraphSvg
            active={active?.id ?? null}
            onSelect={(n) => setActive(n)}
          />
          <div className="absolute bottom-4 left-4 right-4 flex flex-wrap gap-3 text-[10.5px] font-mono">
            <span className="inline-flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-rose2" /> ERROR
            </span>
            <span className="inline-flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-amber2" /> WARN
            </span>
            <span className="inline-flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-cyan2" /> INFO
            </span>
            <span className="text-white/40 ml-auto">点击节点查看详情</span>
          </div>
        </div>

        {/* side panel */}
        <div className="space-y-4">
          <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-4 min-h-[200px]">
            <AnimatePresence mode="wait">
              {active ? (
                <motion.div
                  key={active.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                >
                  <div className="flex items-center gap-2 mb-2">
                    {active.severity === "ERROR" && (
                      <AlertCircle className="w-4 h-4 text-rose2" />
                    )}
                    {active.severity === "WARN" && (
                      <AlertTriangle className="w-4 h-4 text-amber2" />
                    )}
                    {active.severity === "INFO" && (
                      <Info className="w-4 h-4 text-cyan2" />
                    )}
                    <span className="font-mono text-[10.5px] uppercase tracking-wider text-white/40">
                      {active.severity}
                    </span>
                    <code className={cn("font-mono text-[12.5px] font-bold", severityColor[active.severity].text)}>
                      {active.code}
                    </code>
                  </div>
                  <div className="text-[14px] font-semibold text-white/90 mb-2">
                    {active.label}
                  </div>
                  <div className="text-[12px] text-white/65 leading-relaxed mb-3">
                    {active.details}
                  </div>
                  {active.fixHint && (
                    <div className="mt-3 p-2.5 rounded-lg bg-lime-400/8 border border-lime-400/25">
                      <div className="text-[10px] uppercase tracking-wider text-lime2 font-mono mb-1">
                        FixHint
                      </div>
                      <code className="text-[11.5px] text-lime2 font-mono">
                        {active.fixHint}
                      </code>
                    </div>
                  )}
                </motion.div>
              ) : (
                <motion.div
                  key="empty"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="text-white/40 text-[13px]"
                >
                  点击左侧任意节点查看详细信息。
                  <br />
                  <span className="text-[11px] text-white/30 mt-2 block">
                    每个节点对应一个 DiagnosticNode，可通过 DiagnosticEdge 连接到上下游规则和证据。
                  </span>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <div>
            <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
              11 类标准 Code
            </div>
            <div className="grid grid-cols-1 gap-1.5 max-h-[280px] overflow-auto scrollbar-thin">
              {DIAGNOSTIC_CODES.map((c) => {
                const sc = severityColor[c.severity];
                return (
                  <button
                    key={c.code}
                    onClick={() => {
                      const node = DIAG_NODES.find((n) => n.code === c.code);
                      if (node) setActive(node);
                    }}
                    className={cn(
                      "flex items-center gap-2 p-2 rounded-lg border bg-white/[0.02] hover:bg-white/[0.05] text-left transition-all",
                      sc.border
                    )}
                  >
                    <span
                      className="w-2 h-2 rounded-full shrink-0"
                      style={{ background: sc.fill }}
                    />
                    <code className={cn("font-mono text-[11.5px] font-semibold", sc.text)}>
                      {c.code}
                    </code>
                    <span className="text-[11px] text-white/50 truncate flex-1">
                      {c.description.split("。")[0]}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* projections */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="mt-6 p-5 rounded-2xl border border-white/8 bg-gradient-to-br from-brand-500/5 to-cyan-400/5"
      >
        <div className="text-[11px] uppercase tracking-wider text-brand-300 font-mono mb-3">
          4 种投影（同一张图）
        </div>
        <div className="grid md:grid-cols-4 gap-3">
          {[
            { title: "Developer Log", format: "stdout + structured json", color: "text-cyan2" },
            { title: "Player Message", format: "本地化短文本 + 修复提示", color: "text-lime2" },
            { title: "Visualization Overlay", format: "GhostPreviewModel (in-world)", color: "text-magenta" },
            { title: "Datagen Validation", format: "build-time fail / fix-hint report", color: "text-amber2" },
          ].map((p) => (
            <div
              key={p.title}
              className="rounded-xl border border-white/8 bg-white/[0.02] p-3"
            >
              <div className={cn("text-[12.5px] font-semibold mb-1", p.color)}>
                {p.title}
              </div>
              <div className="text-[11.5px] text-white/55">{p.format}</div>
            </div>
          ))}
        </div>
      </motion.div>
    </Section>
  );
}
