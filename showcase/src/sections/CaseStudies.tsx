import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { CASES, type CaseStudy } from "@/data/cases";
import { cn } from "@/lib/utils";

const accentMap = {
  brand: { text: "text-brand-300", bg: "bg-brand-500/10", border: "border-brand-400/30" },
  cyan: { text: "text-cyan2", bg: "bg-cyan-400/10", border: "border-cyan-400/30" },
  magenta: { text: "text-magenta", bg: "bg-pink-400/10", border: "border-pink-400/30" },
  lime: { text: "text-lime2", bg: "bg-lime-400/10", border: "border-lime-400/30" },
  amber: { text: "text-amber2", bg: "bg-amber-400/10", border: "border-amber-400/30" },
  rose: { text: "text-rose2", bg: "bg-rose-500/10", border: "border-rose-500/30" },
};

function CaseTabs({
  active,
  onSelect,
}: {
  active: string;
  onSelect: (id: string) => void;
}) {
  return (
    <div className="flex flex-wrap gap-2 mb-6">
      {CASES.map((c) => {
        const a = accentMap[c.accent as keyof typeof accentMap];
        const isActive = c.id === active;
        return (
          <button
            key={c.id}
            onClick={() => onSelect(c.id)}
            className={cn(
              "px-3 py-1.5 rounded-xl text-[12.5px] font-medium border transition-all flex items-center gap-2",
              isActive
                ? cn(a.bg, a.border, a.text)
                : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
            )}
          >
            {(() => {
              const Icon = (Icons as any)[c.icon] ?? Icons.Box;
              return <Icon className="w-3.5 h-3.5" />;
            })()}
            {c.title}
          </button>
        );
      })}
    </div>
  );
}

export function CaseStudies() {
  const [active, setActive] = useState(CASES[0].id);
  const c = CASES.find((x) => x.id === active)!;
  const a = accentMap[c.accent as keyof typeof accentMap];

  return (
    <Section
      id="cases"
      index="07·5"
      title={
        <>
          真实案例对比：
          <span className="text-white/50"> vanilla NeoForge vs Cumulus</span>
        </>
      }
      lede={
        <>
          同样的多方块玩法，两种写法的差距不是"语法糖"，而是
          <strong className="text-white/80"> 代码量、性能、可维护性、可扩展性的全维度差距</strong>。
          下方 3 个真实场景，每个都给出 vanilla 实现的痛点、Cumulus 实现、性能对比、维护场景对比。
        </>
      }
    >
      <CaseTabs active={active} onSelect={setActive} />

      <AnimatePresence mode="wait">
        <motion.div
          key={c.id}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -12 }}
          transition={{ duration: 0.25 }}
        >
          {/* scenario + shape */}
          <div className={cn("rounded-2xl border p-4 mb-5", a.border, a.bg)}>
            <div className="flex items-baseline justify-between mb-2 gap-2 flex-wrap">
              <h3 className={cn("text-[16px] font-bold", a.text)}>{c.title}</h3>
              <Badge tone={c.accent as any}>{c.shape}</Badge>
            </div>
            <p className="text-[13px] text-white/70 leading-relaxed">{c.scenario}</p>
          </div>

          {/* vanilla pain points */}
          <div className="rounded-2xl border border-rose-500/20 bg-rose-500/[0.03] p-4 mb-5">
            <div className="flex items-center gap-2 mb-3">
              <Icons.AlertOctagon className="w-4 h-4 text-rose2" />
              <span className="text-[13px] font-semibold text-rose2">
                vanilla 写法的痛点
              </span>
            </div>
            <ul className="space-y-1.5">
              {c.vanillaPainPoints.map((p, i) => (
                <li
                  key={i}
                  className="flex items-start gap-2 text-[12.5px] text-white/65 leading-relaxed"
                >
                  <span className="text-rose2 mt-0.5">✗</span>
                  {p}
                </li>
              ))}
            </ul>
          </div>

          {/* code side by side */}
          <div className="grid lg:grid-cols-2 gap-4 mb-5">
            <div>
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-[11px] uppercase tracking-wider text-rose2">
                    vanilla NeoForge
                  </span>
                  <Badge tone="rose">~{c.vanillaLoc} LoC</Badge>
                </div>
              </div>
              <CodeBlock
                code={c.vanillaCode}
                title="vanilla.java"
                maxHeight="480px"
              />
            </div>
            <div>
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span className={cn("font-mono text-[11px] uppercase tracking-wider", a.text)}>
                    ArchitectCumulus
                  </span>
                  <Badge tone={c.accent as any}>~{c.cumulusLoc} LoC</Badge>
                  {c.vanillaLoc > 0 && (
                    <Badge tone="lime">
                      {Math.round((1 - c.cumulusLoc / c.vanillaLoc) * 100)}% 更短
                    </Badge>
                  )}
                </div>
              </div>
              <CodeBlock
                code={c.cumulusCode}
                title="cumulus.java"
                maxHeight="480px"
              />
            </div>
          </div>

          {/* perf comparison */}
          <div className="mb-5">
            <div className="flex items-center gap-2 mb-3">
              <Icons.Gauge className="w-4 h-4 text-cyan2" />
              <span className="text-[13px] font-semibold">性能 / 工作量对比</span>
            </div>
            <div className="grid sm:grid-cols-2 gap-2">
              {c.perf.map((p, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.08 }}
                  className="rounded-xl border border-white/8 bg-white/[0.02] p-3"
                >
                  <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-2">
                    {p.label}
                  </div>
                  <div className="grid grid-cols-[1fr,auto,1fr] gap-2 items-center">
                    <div className="text-[11.5px] font-mono text-rose2 text-right">
                      {p.vanilla}
                    </div>
                    <div className="px-2 py-0.5 rounded-md bg-lime-400/15 border border-lime-400/30 text-lime2 font-mono text-[11px] font-bold">
                      {p.ratio}
                    </div>
                    <div className={cn("text-[11.5px] font-mono", a.text)}>
                      {p.cumulus}
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>

          {/* maintenance scenarios */}
          <div className="mb-5">
            <div className="flex items-center gap-2 mb-3">
              <Icons.Wrench className="w-4 h-4 text-amber2" />
              <span className="text-[13px] font-semibold">维护场景对比</span>
              <span className="text-[11px] text-white/40">
                （"如果我要做 X，两种写法分别要花多少功夫"）
              </span>
            </div>
            <div className="space-y-1.5">
              {c.maintenance.map((m, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, y: 5 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05 }}
                  className="rounded-xl border border-white/8 bg-white/[0.02] p-3"
                >
                  <div className="text-[12.5px] font-semibold text-white/85 mb-2 flex items-center gap-2">
                    <Icons.CheckSquare className="w-3.5 h-3.5 text-brand-300" />
                    {m.task}
                  </div>
                  <div className="grid md:grid-cols-2 gap-2 ml-5">
                    <div className="text-[11.5px] text-white/55 leading-relaxed">
                      <span className="text-rose2 font-mono mr-1.5">vanilla:</span>
                      {m.vanilla}
                    </div>
                    <div className={cn("text-[11.5px] leading-relaxed")}>
                      <span className={cn("font-mono mr-1.5", a.text)}>cumulus:</span>
                      <span className="text-white/70">{m.cumulus}</span>
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>

          {/* takeaway */}
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className={cn(
              "rounded-2xl border p-4 bg-gradient-to-br",
              a.border,
              "from-white/[0.04] to-transparent"
            )}
          >
            <div className="flex items-start gap-3">
              <div className={cn("w-9 h-9 rounded-lg flex items-center justify-center border shrink-0", a.bg, a.border)}>
                <Icons.Sparkles className={cn("w-4 h-4", a.text)} />
              </div>
              <div>
                <div className="text-[10.5px] uppercase tracking-wider text-white/40 font-mono mb-1">
                  一句话总结
                </div>
                <p className="text-[13.5px] text-white/85 leading-relaxed">{c.takeaway}</p>
              </div>
            </div>
          </motion.div>
        </motion.div>
      </AnimatePresence>
    </Section>
  );
}
