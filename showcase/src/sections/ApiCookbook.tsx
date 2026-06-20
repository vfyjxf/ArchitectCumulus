import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useMemo, useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import {
  API_COOKBOOK,
  CATEGORY_LABEL,
  groupByCategory,
  type ApiCard,
  type ApiCategory,
} from "@/data/apiCookbook";
import { cn } from "@/lib/utils";

const accentMap = {
  brand: { text: "text-brand-300", bg: "bg-brand-500/10", border: "border-brand-400/30", dot: "bg-brand-400" },
  cyan: { text: "text-cyan2", bg: "bg-cyan-400/10", border: "border-cyan-400/30", dot: "bg-cyan2" },
  magenta: { text: "text-magenta", bg: "bg-pink-400/10", border: "border-pink-400/30", dot: "bg-magenta" },
  lime: { text: "text-lime2", bg: "bg-lime-400/10", border: "border-lime-400/30", dot: "bg-lime2" },
  amber: { text: "text-amber2", bg: "bg-amber-400/10", border: "border-amber-400/30", dot: "bg-amber2" },
  rose: { text: "text-rose2", bg: "bg-rose-500/10", border: "border-rose-500/30", dot: "bg-rose2" },
};

const difficultyTone = {
  basic: "lime",
  intermediate: "amber",
  advanced: "rose",
} as const;

function CookbookCard({
  card,
  onOpen,
}: {
  card: ApiCard;
  onOpen: () => void;
}) {
  const Icon = (Icons as any)[card.icon] ?? Icons.BookOpen;
  const a = accentMap[card.accent];

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
      <div className={cn("p-4 border-b border-white/5", a.bg)}>
        <div className="flex items-start gap-3">
          <div className={cn("w-9 h-9 rounded-lg flex items-center justify-center border bg-ink-950/40", a.border)}>
            <Icon className={cn("w-4 h-4", a.text)} />
          </div>
          <div className="flex-1 min-w-0">
            <code className={cn("font-mono text-[11px] font-semibold block truncate", a.text)}>
              {card.api}
            </code>
            <h3 className="text-[13.5px] font-bold text-white/95 mt-0.5">
              {card.title}
            </h3>
          </div>
        </div>
      </div>

      <div className="p-4 flex-1 flex flex-col">
        <p className="text-[12px] text-white/55 leading-relaxed mb-3 flex-1">
          {card.scenario}
        </p>
        <div className="flex items-center justify-between">
          <Badge tone={difficultyTone[card.difficulty]}>
            {card.difficulty}
          </Badge>
          <span className="text-[10.5px] font-mono text-white/40 flex items-center gap-1 group-hover:text-white/70">
            查看
            <Icons.ChevronRight className="w-3 h-3 transition-transform group-hover:translate-x-1" />
          </span>
        </div>
      </div>
    </motion.button>
  );
}

function CookbookModal({
  card,
  onClose,
}: {
  card: ApiCard;
  onClose: () => void;
}) {
  const Icon = (Icons as any)[card.icon] ?? Icons.BookOpen;
  const a = accentMap[card.accent];

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
        className="max-w-5xl w-full max-h-[90vh] overflow-auto scrollbar-thin rounded-3xl border border-white/10 bg-ink-900/95 backdrop-blur-2xl shadow-2xl"
      >
        {/* header */}
        <div className={cn("p-6 border-b border-white/5", a.bg)}>
          <div className="flex items-start gap-4">
            <div className={cn("w-14 h-14 rounded-2xl flex items-center justify-center border bg-ink-950/40", a.border)}>
              <Icon className={cn("w-7 h-7", a.text)} />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap mb-1">
                <Badge tone={card.category.toLowerCase() as any}>
                  {CATEGORY_LABEL[card.category]}
                </Badge>
                <Badge tone={difficultyTone[card.difficulty]}>
                  {card.difficulty}
                </Badge>
              </div>
              <code className={cn("font-mono text-[14px] font-semibold block", a.text)}>
                {card.api}
              </code>
              <h2 className="text-2xl font-bold text-white/95 mt-1">{card.title}</h2>
            </div>
            <button
              onClick={onClose}
              className="w-9 h-9 rounded-lg bg-white/5 hover:bg-white/10 text-white/60 flex items-center justify-center shrink-0"
            >
              <Icons.X className="w-4 h-4" />
            </button>
          </div>
          <p className="text-[13px] text-white/65 leading-relaxed mt-4">
            {card.scenario}
          </p>
        </div>

        <div className="p-6 space-y-5">
          {/* Code */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Icons.Code2 className="w-4 h-4 text-cyan2" />
              <span className="text-[12.5px] font-semibold">完整代码</span>
            </div>
            <CodeBlock code={card.code} title={`${card.id}.java`} maxHeight="520px" />
          </div>

          {/* Explanation */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className={cn("w-1.5 h-5 rounded-full", a.dot)} />
              <span className="text-[12.5px] font-semibold">
                逐行解释（每一段说明做了什么、为什么这么写）
              </span>
            </div>
            <ol className="space-y-2 pl-3.5">
              {card.explanation.map((e, i) => (
                <motion.li
                  key={i}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.05 }}
                  className="flex items-start gap-3"
                >
                  <span className={cn("font-mono text-[10.5px] font-bold mt-1 shrink-0", a.text)}>
                    {(i + 1).toString().padStart(2, "0")}
                  </span>
                  <p className="text-[12.5px] text-white/70 leading-relaxed flex-1">
                    {e}
                  </p>
                </motion.li>
              ))}
            </ol>
          </div>

          {/* Trace */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Icons.Activity className="w-4 h-4 text-amber2" />
              <span className="text-[12.5px] font-semibold">运行时 trace</span>
              <span className="text-[10.5px] text-white/40">
                （每一步引擎内部实际做了什么）
              </span>
            </div>
            <div className="rounded-xl border border-white/8 bg-ink-950/80 overflow-hidden">
              {card.trace.map((t, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.05 }}
                  className={cn(
                    "flex items-baseline gap-3 p-3",
                    i !== card.trace.length - 1 && "border-b border-white/5"
                  )}
                >
                  <span className="font-mono text-[10.5px] text-white/30 shrink-0 w-12">
                    step {(i + 1).toString().padStart(2, "0")}
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className="text-[12px] font-mono font-semibold text-white/90 mb-0.5">
                      {t.step}
                    </div>
                    <code className="text-[11px] font-mono text-white/50 break-all">
                      {t.internal}
                    </code>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>

          {/* Pitfalls */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Icons.AlertTriangle className="w-4 h-4 text-rose2" />
              <span className="text-[12.5px] font-semibold">常见陷阱</span>
            </div>
            <ul className="space-y-1.5">
              {card.pitfalls.map((p, i) => (
                <li
                  key={i}
                  className="text-[12.5px] text-white/65 leading-relaxed px-3 py-2 rounded-lg bg-rose-500/[0.05] border border-rose-500/20 flex items-start gap-2"
                >
                  <Icons.AlertOctagon className="w-3 h-3 mt-0.5 shrink-0 text-rose2" />
                  {p}
                </li>
              ))}
            </ul>
          </div>

          {/* Related */}
          {card.related && card.related.length > 0 && (
            <div>
              <div className="flex items-center gap-2 mb-2">
                <Icons.Link2 className="w-3.5 h-3.5 text-brand-300" />
                <span className="text-[12.5px] font-semibold">相关 API</span>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {card.related.map((r) => (
                  <span
                    key={r}
                    className="px-2 py-1 rounded-md bg-white/[0.03] border border-white/8 font-mono text-[11px] text-brand-200"
                  >
                    {r}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      </motion.div>
    </motion.div>
  );
}

export function ApiCookbook() {
  const [filter, setFilter] = useState<ApiCategory | "all">("all");
  const [open, setOpen] = useState<ApiCard | null>(null);
  const [difficulty, setDifficulty] = useState<"all" | "basic" | "intermediate" | "advanced">("all");

  const filtered = useMemo(() => {
    return API_COOKBOOK.filter((c) => {
      if (filter !== "all" && c.category !== filter) return false;
      if (difficulty !== "all" && c.difficulty !== difficulty) return false;
      return true;
    });
  }, [filter, difficulty]);

  const grouped = useMemo(() => groupByCategory(filtered), [filtered]);

  return (
    <Section
      id="cookbook"
      index="13·5"
      title={
        <>
          API Cookbook：
          <span className="text-white/50"> {API_COOKBOOK.length} 个真实场景 × 8 类 API</span>
        </>
      }
      lede={
        <>
          这是开发者最常搜索的 API 用法集合。每张卡都给出
          <strong className="text-white/80">场景 → 完整代码 → 逐行解释 → 运行时 trace → 陷阱</strong>
          五个维度。点击卡片展开查看详情。可按分类 + 难度筛选。
        </>
      }
    >
      {/* filter bar */}
      <div className="flex flex-wrap items-center gap-2 mb-3">
        <button
          onClick={() => setFilter("all")}
          className={cn(
            "px-3 py-1.5 rounded-full text-[12px] font-medium border transition-all",
            filter === "all"
              ? "bg-brand-400/20 border-brand-400/40 text-brand-100"
              : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
          )}
        >
          全部 ({API_COOKBOOK.length})
        </button>
        {(Object.keys(CATEGORY_LABEL) as ApiCategory[]).map((cat) => {
          const count = API_COOKBOOK.filter((c) => c.category === cat).length;
          return (
            <button
              key={cat}
              onClick={() => setFilter(cat)}
              className={cn(
                "px-3 py-1.5 rounded-full text-[12px] font-mono border transition-all",
                filter === cat
                  ? accentMap[
                      (
                        {
                          Definition: "brand",
                          Runtime: "cyan",
                          Substructure: "magenta",
                          Build: "amber",
                          Editor: "lime",
                          Minecraft: "rose",
                          Datagen: "cyan",
                          Migration: "lime",
                        } as const
                      )[cat]
                    ].bg +
                    " " +
                    accentMap[
                      (
                        {
                          Definition: "brand",
                          Runtime: "cyan",
                          Substructure: "magenta",
                          Build: "amber",
                          Editor: "lime",
                          Minecraft: "rose",
                          Datagen: "cyan",
                          Migration: "lime",
                        } as const
                      )[cat]
                    ].border +
                    " " +
                    accentMap[
                      (
                        {
                          Definition: "brand",
                          Runtime: "cyan",
                          Substructure: "magenta",
                          Build: "amber",
                          Editor: "lime",
                          Minecraft: "rose",
                          Datagen: "cyan",
                          Migration: "lime",
                        } as const
                      )[cat]
                    ].text
                  : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
              )}
            >
              {CATEGORY_LABEL[cat]} ({count})
            </button>
          );
        })}
      </div>

      {/* difficulty filter */}
      <div className="flex items-center gap-2 mb-6">
        <span className="text-[10.5px] uppercase tracking-wider text-white/40 font-mono">
          难度:
        </span>
        {(["all", "basic", "intermediate", "advanced"] as const).map((d) => (
          <button
            key={d}
            onClick={() => setDifficulty(d)}
            className={cn(
              "px-2 py-0.5 rounded text-[11px] font-mono border transition-all",
              difficulty === d
                ? d === "basic"
                  ? "bg-lime-400/15 border-lime-400/30 text-lime2"
                  : d === "intermediate"
                  ? "bg-amber-400/15 border-amber-400/30 text-amber2"
                  : d === "advanced"
                  ? "bg-rose-500/15 border-rose-500/30 text-rose2"
                  : "bg-brand-400/15 border-brand-400/30 text-brand-200"
                : "bg-white/[0.03] border-white/8 text-white/50"
            )}
          >
            {d === "all" ? "全部" : d}
          </button>
        ))}
      </div>

      {/* cards */}
      <AnimatePresence mode="popLayout">
        {filter === "all" ? (
          // grouped by category
          (Object.keys(CATEGORY_LABEL) as ApiCategory[]).map((cat) => {
            const cards = grouped[cat];
            if (cards.length === 0) return null;
            const catAccent = accentMap[
              (
                {
                  Definition: "brand",
                  Runtime: "cyan",
                  Substructure: "magenta",
                  Build: "amber",
                  Editor: "lime",
                  Minecraft: "rose",
                  Datagen: "cyan",
                  Migration: "lime",
                } as const
              )[cat]
            ];
            return (
              <motion.div
                key={cat}
                layout
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="mb-8"
              >
                <div className="flex items-baseline gap-3 mb-4">
                  <span className={cn("w-1.5 h-6 rounded-full", catAccent.dot)} />
                  <h3 className={cn("text-[15px] font-bold", catAccent.text)}>
                    {CATEGORY_LABEL[cat]}
                  </h3>
                  <span className="text-[11px] text-white/40 font-mono">
                    {cards.length} 个 API
                  </span>
                </div>
                <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
                  {cards.map((c) => (
                    <CookbookCard key={c.id} card={c} onOpen={() => setOpen(c)} />
                  ))}
                </div>
              </motion.div>
            );
          })
        ) : (
          <motion.div
            layout
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3"
          >
            {filtered.map((c) => (
              <CookbookCard key={c.id} card={c} onOpen={() => setOpen(c)} />
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {open && <CookbookModal card={open} onClose={() => setOpen(null)} />}
      </AnimatePresence>
    </Section>
  );
}
