import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { SCENARIOS, type GameScenario } from "@/data/scenarios";
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
          查看完整实现
          <Icons.ChevronRight className="w-3 h-3 transition-transform group-hover:translate-x-1" />
        </div>
      </div>
    </motion.button>
  );
}

function ScenarioModal({
  s,
  onClose,
}: {
  s: GameScenario;
  onClose: () => void;
}) {
  const Icon = (Icons as any)[s.icon] ?? Icons.Box;
  const a = accent[s.accent] ?? accent.brand;

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
        className="max-w-4xl w-full max-h-[88vh] overflow-auto scrollbar-thin rounded-3xl border border-white/10 bg-ink-900/95 backdrop-blur-2xl shadow-2xl"
      >
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

        <div className="p-6">
          <div className="grid md:grid-cols-2 gap-5 mb-5">
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

          <div>
            <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
              最小 Java DSL
            </div>
            <CodeBlock code={s.snippet} title={`${s.id}.java`} />
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
          把现有知名多方块模组的设计模式翻译成 Cumulus API 组合。
          这些不是已实现的功能，而是
          <strong className="text-white/80">同一种数据模型可以表达的全部玩法形态</strong>。
          点击卡片展开实现要点。
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
