import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { SCENARIOS, ROLE_COLOR, ROLE_LABEL, type GameScenario, type BlockRole } from "@/data/scenarios";
import { ScenarioViewport } from "@/three/ScenarioViewport";
import { cn } from "@/lib/utils";

const accent: Record<string, { text: string; bg: string; border: string }> = {
  brand: { text: "text-brand-300", bg: "bg-brand-500/10", border: "border-brand-400/30" },
  cyan: { text: "text-cyan2", bg: "bg-cyan-400/10", border: "border-cyan-400/30" },
  magenta: { text: "text-magenta", bg: "bg-pink-400/10", border: "border-pink-400/30" },
  lime: { text: "text-lime2", bg: "bg-lime-400/10", border: "border-lime-400/30" },
  amber: { text: "text-amber2", bg: "bg-amber-400/10", border: "border-amber-400/30" },
  rose: { text: "text-rose2", bg: "bg-rose-500/10", border: "border-rose-500/30" },
};

function ScenarioCard({
  s,
  onOpen,
}: {
  s: GameScenario;
  onOpen: () => void;
}) {
  const Icon = (Icons as any)[s.icon] ?? Icons.Box;
  const a = accent[s.accent] ?? accent.brand;

  return (
    <motion.button
      layout
      onClick={onOpen}
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-60px" }}
      whileHover={{ y: -3 }}
      className={cn(
        "group text-left rounded-2xl border bg-ink-900/70 backdrop-blur-xl overflow-hidden flex flex-col",
        a.border,
        "hover:shadow-[0_18px_40px_-15px_rgba(124,92,255,0.25)]"
      )}
    >
      <div className={cn("p-5 border-b border-white/5", a.bg)}>
        <div className="flex items-center justify-between mb-3">
          <div className={cn("w-10 h-10 rounded-xl flex items-center justify-center border", a.bg, a.border)}>
            <Icon className={cn("w-5 h-5", a.text)} />
          </div>
          <span className="font-mono text-[10px] text-white/40 uppercase tracking-wider">
            {s.mod}
          </span>
        </div>
        <h3 className="text-[15px] font-bold text-white/95 mb-1">{s.title}</h3>
        <p className="text-[12px] text-white/55 leading-relaxed">{s.hook}</p>
      </div>
      <div className="p-5 flex-1 flex flex-col">
        <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
          涉及 Target
        </div>
        <div className="flex flex-wrap gap-1 mb-3">
          {s.targets.map((t) => (
            <Badge key={t} tone="neutral">
              {t}
            </Badge>
          ))}
        </div>
        <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
          关键 API
        </div>
        <ul className="space-y-1 mb-4 flex-1">
          {s.apiPieces.slice(0, 4).map((p) => (
            <li
              key={p}
              className="font-mono text-[11.5px] text-white/60 flex items-start gap-1.5"
            >
              <span className={cn("mt-1", a.text)}>▸</span>
              {p}
            </li>
          ))}
        </ul>
        <div className={cn("text-[11px] flex items-center gap-1 mt-auto", a.text)}>
          <Icons.Box className="w-3 h-3" />
          查看 3D 场景 + API 映射
          <Icons.ChevronRight className="w-3 h-3 transition-transform group-hover:translate-x-1" />
        </div>
      </div>
    </motion.button>
  );
}

/** 角色图例 */
function RoleLegend({ roles }: { roles: BlockRole[] }) {
  const seen = [...new Set(roles)];
  return (
    <div className="flex flex-wrap gap-x-3 gap-y-1 text-[10.5px] font-mono">
      {seen.map((r) => (
        <span key={r} className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-ink-950/70">
          <span className="w-2 h-2 rounded-sm" style={{ background: ROLE_COLOR[r] }} />
          <span className="text-white/60">{ROLE_LABEL[r]}</span>
        </span>
      ))}
    </div>
  );
}

/** DSL 带 hover 行高亮 */
function DslWithHover({
  s,
  activeTag,
  onHoverTag,
}: {
  s: GameScenario;
  activeTag: string | null;
  onHoverTag: (t: string | null) => void;
}) {
  const lines = s.dslLines ?? s.snippet.split("\n").map((text) => ({ tag: "none", text }));
  return (
    <div className="rounded-xl border border-white/8 bg-ink-950/90 overflow-auto scrollbar-thin" style={{ maxHeight: "420px" }}>
      <pre className="text-[11.5px] font-mono leading-relaxed p-3">
        {lines.map((l, i) => {
          const isActive = activeTag != null && l.tag === activeTag;
          return (
            <div
              key={i}
              onMouseEnter={() => l.tag !== "none" && l.tag !== "header" && l.tag !== "footer" && onHoverTag(l.tag)}
              onMouseLeave={() => onHoverTag(null)}
              className={cn(
                "px-2 -mx-2 rounded transition-colors cursor-default",
                isActive && "bg-brand-500/15 border-l-2 border-brand-400 -ml-2 pl-2",
              )}
            >
              <span className={cn(isActive ? "text-brand-200" : "text-white/75")}>
                {l.text}
              </span>
            </div>
          );
        })}
      </pre>
    </div>
  );
}

function ScenarioModal({
  s,
  onClose,
}: {
  s: GameScenario;
  onClose: () => void;
}) {
  const [activeTag, setActiveTag] = useState<string | null>(null);
  const Icon = (Icons as any)[s.icon] ?? Icons.Box;
  const a = accent[s.accent] ?? accent.brand;
  const roles = s.blocks.map((b) => b.role);

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
      className="fixed inset-0 z-50 bg-ink-950/90 backdrop-blur-md p-4 md:p-8 flex items-center justify-center"
    >
      <motion.div
        initial={{ scale: 0.92, y: 30 }}
        animate={{ scale: 1, y: 0 }}
        exit={{ scale: 0.92, y: 30 }}
        onClick={(e) => e.stopPropagation()}
        className="max-w-6xl w-full max-h-[90vh] overflow-auto scrollbar-thin rounded-3xl border border-white/10 bg-ink-900/95 backdrop-blur-2xl shadow-2xl"
      >
        {/* header */}
        <div className={cn("p-6 border-b border-white/5", a.bg)}>
          <div className="flex items-start gap-4">
            <div className={cn("w-14 h-14 rounded-2xl flex items-center justify-center border bg-ink-950/60", a.border)}>
              <Icon className={cn("w-7 h-7", a.text)} />
            </div>
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <h2 className="text-2xl font-bold">{s.title}</h2>
                <Badge tone={s.accent as any}>{s.mod}</Badge>
              </div>
              <p className="text-[13px] text-white/70">{s.hook}</p>
              <div className="font-mono text-[11.5px] text-white/50 mt-1">
                shape: {s.multiblockShape}
              </div>
            </div>
            <button
              onClick={onClose}
              className="w-9 h-9 rounded-lg bg-white/5 hover:bg-white/10 text-white/60 flex items-center justify-center"
            >
              <Icons.X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* 3D + DSL 联动 */}
        <div className="p-6">
          <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
            游戏世界方块 ↔ Cumulus API（悬停方块或代码行高亮映射）
          </div>
          <div className="grid lg:grid-cols-[1.2fr,1fr] gap-4">
            {/* 3D viewport */}
            <div className="relative rounded-2xl border border-white/8 bg-ink-950/80 overflow-hidden h-[440px]">
              <ScenarioViewport
                blocks={s.blocks}
                activeTag={activeTag}
                onHoverTag={setActiveTag}
              />
              <div className="absolute bottom-3 left-3 right-3">
                <RoleLegend roles={roles} />
              </div>
            </div>
            {/* DSL */}
            <div className="space-y-2">
              <DslWithHover s={s} activeTag={activeTag} onHoverTag={setActiveTag} />
            </div>
          </div>
        </div>

        {/* API pieces + pitfalls */}
        <div className="px-6 pb-6 grid md:grid-cols-2 gap-5">
          <div>
            <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
              关键 API 组合
            </div>
            <div className="space-y-1.5">
              {s.apiPieces.map((p) => (
                <div
                  key={p}
                  className="font-mono text-[12px] px-2.5 py-1.5 rounded-lg bg-white/[0.03] border border-white/8 text-white/80 flex items-start gap-2"
                >
                  <Icons.Check className={cn("w-3 h-3 mt-0.5 shrink-0", a.text)} />
                  {p}
                </div>
              ))}
            </div>
          </div>
          <div>
            <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
              设计陷阱
            </div>
            <ul className="space-y-1.5">
              {s.pitfalls.map((p, i) => (
                <li
                  key={i}
                  className="text-[12px] text-white/65 px-2.5 py-1.5 rounded-lg bg-amber-400/5 border border-amber-400/20 flex items-start gap-2"
                >
                  <Icons.AlertTriangle className="w-3 h-3 mt-0.5 shrink-0 text-amber2" />
                  {p}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}

export function Scenarios() {
  const [open, setOpen] = useState<GameScenario | null>(null);

  return (
    <Section
      id="scenarios"
      index="07"
      title={
        <>
          真实 3D 模拟游戏场景
          <span className="text-white/50"> → Cumulus API 映射</span>
        </>
      }
      lede={
        <>
          把现有知名多方块模组的设计模式翻译成 Cumulus API 组合。点击卡片进入
          <strong className="text-white/80"> 3D 场景查看器</strong>：鼠标悬停方块或代码行，
          看到游戏世界每个方块如何映射到 <code className="text-cyan2">point / target / predicate / context</code>。
        </>
      }
    >
      <motion.div layout className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <AnimatePresence>
          {SCENARIOS.map((s) => (
            <ScenarioCard key={s.id} s={s} onOpen={() => setOpen(s)} />
          ))}
        </AnimatePresence>
      </motion.div>

      <AnimatePresence>
        {open && <ScenarioModal s={open} onClose={() => setOpen(null)} />}
      </AnimatePresence>
    </Section>
  );
}
