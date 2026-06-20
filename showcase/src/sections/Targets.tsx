import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { TARGETS, type TargetId } from "@/data/targets";
import { cn } from "@/lib/utils";

const accentColor: Record<string, { text: string; bg: string; border: string; dot: string }> = {
  brand: { text: "text-brand-300", bg: "bg-brand-500/10", border: "border-brand-400/30", dot: "bg-brand-400" },
  cyan: { text: "text-cyan2", bg: "bg-cyan-400/10", border: "border-cyan-400/30", dot: "bg-cyan2" },
  magenta: { text: "text-magenta", bg: "bg-pink-400/10", border: "border-pink-400/30", dot: "bg-magenta" },
  lime: { text: "text-lime2", bg: "bg-lime-400/10", border: "border-lime-400/30", dot: "bg-lime2" },
  amber: { text: "text-amber2", bg: "bg-amber-400/10", border: "border-amber-400/30", dot: "bg-amber2" },
  rose: { text: "text-rose2", bg: "bg-rose-500/10", border: "border-rose-500/30", dot: "bg-rose2" },
};

function TargetCard({
  t,
  onOpen,
}: {
  t: (typeof TARGETS)[number];
  onOpen: () => void;
}) {
  const Icon = (Icons as any)[t.icon] ?? Icons.Box;
  const accent = accentColor[t.accent];

  return (
    <motion.button
      layout
      onClick={onOpen}
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px" }}
      whileHover={{ y: -4 }}
      className={cn(
        "group text-left rounded-2xl border p-5 transition-all bg-ink-900/70 backdrop-blur-xl flex flex-col",
        accent.border,
        "hover:shadow-[0_18px_40px_-15px_rgba(124,92,255,0.3)]"
      )}
    >
      <div className="flex items-start justify-between mb-3">
        <div
          className={cn(
            "w-11 h-11 rounded-xl flex items-center justify-center border",
            accent.bg,
            accent.border
          )}
        >
          <Icon className={cn("w-5 h-5", accent.text)} />
        </div>
        <span className="font-mono text-[10px] text-white/30 uppercase tracking-wider">
          Target
        </span>
      </div>

      <div className="flex items-baseline gap-2 mb-1">
        <code className={cn("font-mono text-[15px] font-semibold", accent.text)}>
          {t.title}
        </code>
      </div>
      <div className="text-[12px] text-white/50 mb-3 leading-relaxed">
        {t.tagline}
      </div>

      <p className="text-[12.5px] text-white/65 leading-relaxed mb-3 flex-1">
        {t.description}
      </p>

      <div className="flex flex-wrap gap-1 mb-3">
        {t.predicate.map((p) => (
          <span
            key={p}
            className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-white/5 text-white/50 border border-white/5"
          >
            {p}
          </span>
        ))}
      </div>

      <div className="text-[10.5px] font-mono text-white/30">
        场景数: {t.gameScenarios.length} · 诊断: {t.diagnostic}
      </div>
    </motion.button>
  );
}

function TargetModal({
  t,
  onClose,
}: {
  t: (typeof TARGETS)[number];
  onClose: () => void;
}) {
  const Icon = (Icons as any)[t.icon] ?? Icons.Box;
  const accent = accentColor[t.accent];

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
        <div className="p-6 md:p-8">
          <div className="flex items-start gap-4 mb-6">
            <div
              className={cn(
                "w-14 h-14 rounded-2xl flex items-center justify-center border",
                accent.bg,
                accent.border
              )}
            >
              <Icon className={cn("w-7 h-7", accent.text)} />
            </div>
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <code className={cn("font-mono text-xl font-bold", accent.text)}>
                  {t.title}
                </code>
                <Badge tone={t.accent as any}>{t.tagline}</Badge>
              </div>
              <p className="text-[13.5px] text-white/65 leading-relaxed">
                {t.description}
              </p>
            </div>
            <button
              onClick={onClose}
              className="w-9 h-9 rounded-lg bg-white/5 hover:bg-white/10 text-white/60 flex items-center justify-center"
            >
              <Icons.X className="w-4 h-4" />
            </button>
          </div>

          <div className="grid md:grid-cols-2 gap-5 mb-6">
            <div>
              <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
                Java DSL 调用
              </div>
              <CodeBlock code={t.api} title={`${t.id}.java`} />
            </div>
            <div>
              <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
                内置 Predicate
              </div>
              <div className="space-y-1.5 mb-4">
                {t.predicate.map((p) => (
                  <div
                    key={p}
                    className="font-mono text-[12px] px-2.5 py-1.5 rounded-lg bg-white/[0.03] border border-white/8 text-white/70"
                  >
                    {p}
                  </div>
                ))}
              </div>
              <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-2">
                失败时诊断 Code
              </div>
              <div className="font-mono text-[11.5px] px-2.5 py-1.5 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose2">
                {t.diagnostic}
              </div>
            </div>
          </div>

          <div>
            <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-3">
              对应游戏玩法场景
            </div>
            <div className="grid md:grid-cols-3 gap-3">
              {t.gameScenarios.map((g, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.08 }}
                  className={cn(
                    "p-4 rounded-xl border bg-white/[0.02]",
                    accent.border
                  )}
                >
                  <div className={cn("text-[12px] font-semibold mb-1", accent.text)}>
                    {g.title}
                  </div>
                  <div className="text-[11.5px] text-white/55 leading-relaxed">
                    {g.detail}
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}

export function Targets() {
  const [filter, setFilter] = useState<TargetId | "all">("all");
  const [open, setOpen] = useState<(typeof TARGETS)[number] | null>(null);

  const filtered =
    filter === "all" ? TARGETS : TARGETS.filter((t) => t.id === filter);

  return (
    <Section
      id="targets"
      index="03"
      title={
        <>
          Target 图鉴：
          <span className="text-white/50"> 多方块里"什么算一方块"</span>
        </>
      }
      lede={
        <>
          传统多方块库几乎只能识别"方块"。ArchitectCumulus 把 Target 抽象成可扩展的描述符，
          让同一个结构定义里可以同时检查
          <strong className="text-white/80">机器外壳方块、流体管道、物品容器、能量接口、玩家实体、外部上下文</strong>。
          下方是七种内置 Target 的常见 API 场景，每张卡都给出"游戏玩法场景"与 Java DSL 写法。
        </>
      }
    >
      <div className="flex flex-wrap gap-2 mb-8">
        <button
          onClick={() => setFilter("all")}
          className={cn(
            "px-3 py-1.5 rounded-full text-[12px] font-medium border transition-all",
            filter === "all"
              ? "bg-brand-400/20 border-brand-400/40 text-brand-100"
              : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
          )}
        >
          全部 ({TARGETS.length})
        </button>
        {TARGETS.map((t) => (
          <button
            key={t.id}
            onClick={() => setFilter(t.id)}
            className={cn(
              "px-3 py-1.5 rounded-full text-[12px] font-medium border transition-all font-mono",
              filter === t.id
                ? accentColor[t.accent].bg + " " + accentColor[t.accent].border + " " + accentColor[t.accent].text
                : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
            )}
          >
            {t.id}
          </button>
        ))}
      </div>

      <motion.div layout className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <AnimatePresence mode="popLayout">
          {filtered.map((t) => (
            <TargetCard key={t.id} t={t} onOpen={() => setOpen(t)} />
          ))}
        </AnimatePresence>
      </motion.div>

      <AnimatePresence>
        {open && <TargetModal t={open} onClose={() => setOpen(null)} />}
      </AnimatePresence>
    </Section>
  );
}
