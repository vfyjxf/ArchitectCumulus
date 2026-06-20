import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CodeBlock } from "./CodeBlock";
import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

export interface CodeTab {
  id: string;
  label: string;
  description?: string;
  tone?: "brand" | "cyan" | "magenta" | "lime" | "amber";
  code?: string;
  title?: string;
  custom?: ReactNode;
  maxHeight?: string;
}

const toneClass = {
  brand: { active: "bg-brand-400/20 border-brand-400/40 text-brand-100", dot: "bg-brand-400" },
  cyan: { active: "bg-cyan-400/15 border-cyan-400/40 text-cyan2", dot: "bg-cyan2" },
  magenta: { active: "bg-pink-400/15 border-pink-400/40 text-magenta", dot: "bg-magenta" },
  lime: { active: "bg-lime-400/15 border-lime-400/40 text-lime2", dot: "bg-lime2" },
  amber: { active: "bg-amber-400/15 border-amber-400/40 text-amber2", dot: "bg-amber2" },
};

export function CodeTabs({
  tabs,
  defaultTab,
  maxHeight,
}: {
  tabs: CodeTab[];
  defaultTab?: string;
  maxHeight?: string;
}) {
  const [active, setActive] = useState(defaultTab ?? tabs[0]?.id);
  const current = tabs.find((t) => t.id === active) ?? tabs[0];

  return (
    <div>
      {/* tab bar */}
      <div className="flex items-center gap-1 mb-2 flex-wrap">
        {tabs.map((t) => {
          const tone = t.tone ? toneClass[t.tone] : toneClass.brand;
          const isActive = t.id === active;
          return (
            <button
              key={t.id}
              onClick={() => setActive(t.id)}
              className={cn(
                "flex items-center gap-1.5 px-3 py-1 rounded-lg text-[11.5px] font-medium border transition-all",
                isActive
                  ? tone.active
                  : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
              )}
            >
              {t.tone && (
                <span className={cn("w-1.5 h-1.5 rounded-full", tone.dot)} />
              )}
              {t.label}
            </button>
          );
        })}
        {current.description && (
          <span className="text-[10.5px] text-white/40 ml-2 hidden md:inline">
            {current.description}
          </span>
        )}
      </div>
      <AnimatePresence mode="wait">
        <motion.div
          key={current.id}
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -4 }}
          transition={{ duration: 0.15 }}
        >
          {current.custom ? (
            current.custom
          ) : (
            <CodeBlock
              code={current.code ?? ""}
              title={current.title ?? `${current.id}.txt`}
              maxHeight={current.maxHeight ?? maxHeight}
            />
          )}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}
