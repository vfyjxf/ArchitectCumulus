import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { RATIONALE, type RationaleCard } from "@/data/rationale";
import { cn } from "@/lib/utils";

const accentMap = {
  brand: { text: "text-brand-300", bg: "bg-brand-500/10", border: "border-brand-400/30", dot: "bg-brand-400" },
  cyan: { text: "text-cyan2", bg: "bg-cyan-400/10", border: "border-cyan-400/30", dot: "bg-cyan2" },
  magenta: { text: "text-magenta", bg: "bg-pink-400/10", border: "border-pink-400/30", dot: "bg-magenta" },
  lime: { text: "text-lime2", bg: "bg-lime-400/10", border: "border-lime-400/30", dot: "bg-lime2" },
  amber: { text: "text-amber2", bg: "bg-amber-400/10", border: "border-amber-400/30", dot: "bg-amber2" },
  rose: { text: "text-rose2", bg: "bg-rose-500/10", border: "border-rose-500/30", dot: "bg-rose2" },
};

function RationaleItem({
  r,
  index,
  isOpen,
  onToggle,
}: {
  r: RationaleCard;
  index: number;
  isOpen: boolean;
  onToggle: () => void;
}) {
  const Icon = (Icons as any)[r.icon] ?? Icons.HelpCircle;
  const a = accentMap[r.accent];

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-60px" }}
      transition={{ delay: index * 0.05 }}
      className={cn(
        "rounded-2xl border bg-ink-900/70 backdrop-blur-xl overflow-hidden",
        isOpen ? a.border : "border-white/8"
      )}
    >
      <button
        onClick={onToggle}
        className="w-full text-left p-5 flex items-start gap-4"
      >
        <div className={cn("w-10 h-10 rounded-xl flex items-center justify-center border shrink-0", a.bg, a.border)}>
          <Icon className={cn("w-5 h-5", a.text)} />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-baseline gap-2 mb-1">
            <span className="font-mono text-[10px] text-white/30">#{(index + 1).toString().padStart(2, "0")}</span>
            <h3 className="text-[15px] font-semibold text-white/95">{r.question}</h3>
          </div>
          <p className={cn("text-[12.5px] font-mono", a.text)}>{r.oneLiner}</p>
        </div>
        <Icons.ChevronDown
          className={cn(
            "w-4 h-4 text-white/40 shrink-0 mt-1.5 transition-transform",
            isOpen && "rotate-180"
          )}
        />
      </button>

      <AnimatePresence initial={false}>
        {isOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="overflow-hidden"
          >
            <div className="px-5 pb-5 pt-1 border-t border-white/5">
              {/* Problem */}
              <div className="mt-4 mb-4">
                <div className="flex items-center gap-2 mb-2">
                  <span className={cn("w-1.5 h-5 rounded-full", a.dot)} />
                  <span className="text-[11px] uppercase tracking-wider font-mono text-white/50">
                    Problem · 这个 API 想解决什么问题
                  </span>
                </div>
                <p className="text-[13px] text-white/70 leading-relaxed pl-3.5">
                  {r.problem}
                </p>
              </div>

              {/* Alternatives */}
              <div className="mb-4">
                <div className="flex items-center gap-2 mb-2">
                  <span className="w-1.5 h-5 rounded-full bg-white/30" />
                  <span className="text-[11px] uppercase tracking-wider font-mono text-white/50">
                    Alternatives · 备选方案与被否原因
                  </span>
                </div>
                <div className="pl-3.5 space-y-1.5">
                  {r.alternatives.map((alt, i) => (
                    <div
                      key={i}
                      className={cn(
                        "rounded-lg border p-2.5 text-[12px]",
                        alt.verdict === "accepted"
                          ? "border-lime-400/30 bg-lime-400/5"
                          : "border-white/8 bg-white/[0.02]"
                      )}
                    >
                      <div className="flex items-center gap-2 mb-0.5">
                        <code
                          className={cn(
                            "font-mono font-semibold",
                            alt.verdict === "accepted" ? "text-lime2" : "text-rose2 line-through"
                          )}
                        >
                          {alt.name}
                        </code>
                        <Badge tone={alt.verdict === "accepted" ? "lime" : "rose"}>
                          {alt.verdict}
                        </Badge>
                      </div>
                      {alt.rejectedBecause && (
                        <div className="text-[11.5px] text-white/55 mt-1 leading-relaxed">
                          → {alt.rejectedBecause}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* Solution */}
              <div className="mb-4">
                <div className="flex items-center gap-2 mb-2">
                  <span className={cn("w-1.5 h-5 rounded-full", a.dot)} />
                  <span className="text-[11px] uppercase tracking-wider font-mono text-white/50">
                    Solution · 实际怎么做
                  </span>
                </div>
                <p className="text-[13px] text-white/75 leading-relaxed pl-3.5">
                  {r.solution}
                </p>
              </div>

              {/* Why this way */}
              <div className="mb-4">
                <div className="flex items-center gap-2 mb-2">
                  <span className="w-1.5 h-5 rounded-full bg-cyan2" />
                  <span className="text-[11px] uppercase tracking-wider font-mono text-white/50">
                    Why this way · 为什么是这种实现
                  </span>
                </div>
                <p className="text-[13px] text-white/70 leading-relaxed pl-3.5">
                  {r.whyThisWay}
                </p>
              </div>

              {/* Real cost */}
              <div className="mb-4 p-3 rounded-lg border border-amber-400/20 bg-amber-400/5">
                <div className="flex items-center gap-2 mb-1">
                  <Icons.AlertTriangle className="w-3.5 h-3.5 text-amber2" />
                  <span className="text-[11px] uppercase tracking-wider font-mono text-amber2">
                    Real cost · 代价
                  </span>
                </div>
                <p className="text-[12.5px] text-white/70 leading-relaxed">
                  {r.realCost}
                </p>
              </div>

              {/* Code */}
              <div className="mb-4">
                <CodeBlock code={r.codeSnippet} title={`${r.id}.java`} maxHeight="380px" />
              </div>

              {/* Metrics */}
              {r.metrics && r.metrics.length > 0 && (
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <span className="w-1.5 h-5 rounded-full bg-lime2" />
                    <span className="text-[11px] uppercase tracking-wider font-mono text-white/50">
                      Metrics · 实测对比
                    </span>
                  </div>
                  <div className="grid md:grid-cols-3 gap-2">
                    {r.metrics.map((m, i) => (
                      <div
                        key={i}
                        className="rounded-lg border border-white/8 bg-white/[0.02] p-3"
                      >
                        <div className="text-[10.5px] uppercase tracking-wider text-white/40 font-mono mb-2">
                          {m.label}
                        </div>
                        <div className="space-y-1">
                          <div className="flex items-center gap-1.5 text-[12px]">
                            <span className={cn("font-mono font-semibold", a.text)}>cumulus</span>
                            <span className="text-white/70 font-mono">{m.cumulus}</span>
                          </div>
                          <div className="flex items-center gap-1.5 text-[12px]">
                            <span className="font-mono font-semibold text-white/40">vanilla</span>
                            <span className="text-white/60 font-mono">{m.vanilla}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

export function Rationale() {
  const [open, setOpen] = useState<string | null>("immutable-ir");

  return (
    <Section
      id="rationale"
      index="02·5"
      title={
        <>
          设计决策深度：
          <span className="text-white/50"> 每个 API 形态背后的「为什么」</span>
        </>
      }
      lede={
        <>
          每个奇怪的 API 设计都有非平凡的取舍。下方列出 10 个核心决策，每一项展开都能看到：
          它解决了什么真实问题、被否决的备选方案是什么、为什么这种实现胜出、代价是什么。
          <strong className="text-white/80"> 不是 API 文档，是设计动机文档。</strong>
        </>
      }
    >
      <div className="space-y-3">
        {RATIONALE.map((r, i) => (
          <RationaleItem
            key={r.id}
            r={r}
            index={i}
            isOpen={open === r.id}
            onToggle={() => setOpen(open === r.id ? null : r.id)}
          />
        ))}
      </div>
    </Section>
  );
}
