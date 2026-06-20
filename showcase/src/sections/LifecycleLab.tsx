import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useState } from "react";
import { Section } from "@/components/ui/Section";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import {
  CHILD_TEMPLATES,
  useLifecycle,
  type Occurrence,
  type OccurrenceStatus,
} from "@/three/lifecycleStore";
import { cn } from "@/lib/utils";

const statusColor: Record<OccurrenceStatus, string> = {
  valid: "#a3e635",
  invalid: "#fb7185",
  incomplete: "#fbbf24",
  stale: "#7c5cff",
};

const statusLabel: Record<OccurrenceStatus, string> = {
  valid: "valid",
  invalid: "invalid",
  incomplete: "incomplete",
  stale: "stale",
};

const childColor: Record<string, string> = {
  factory: "#7c5cff",
  arm: "#22d3ee",
  tool: "#f0abfc",
  base: "#a3e635",
};

function OccNode({
  occ,
  depth,
  occurrences,
  selectedId,
  onSelect,
  onToggleBlock,
  onRemove,
  onAddChild,
}: {
  occ: Occurrence;
  depth: number;
  occurrences: Occurrence[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  onToggleBlock: (id: string, b: string) => void;
  onRemove: (id: string) => void;
  onAddChild: (parentId: string, childDef: string, name: string, offset: string) => void;
}) {
  const isSelected = selectedId === occ.id;
  const color = childColor[occ.childDef] ?? "#7c5cff";
  const children = occurrences.filter((o) => o.parentId === occ.id);

  return (
    <div style={{ marginLeft: depth * 22 }}>
      <motion.div
        layout
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, x: -10 }}
        className={cn(
          "rounded-xl border bg-white/[0.02] p-3 transition-all cursor-pointer",
          isSelected
            ? "border-white/25 bg-white/[0.05]"
            : "border-white/8 hover:border-white/15"
        )}
        onClick={() => onSelect(occ.id)}
      >
        <div className="flex items-center gap-2">
          <span
            className="w-2 h-2 rounded-full shrink-0"
            style={{ background: color }}
          />
          <code className="font-mono text-[12.5px] font-semibold flex-1" style={{ color }}>
            {occ.instanceName}
          </code>
          <span className="font-mono text-[10px] text-white/40">
            {occ.offsetInParent}
          </span>
          <span
            className="px-1.5 py-0.5 rounded text-[9.5px] font-mono font-bold border"
            style={{
              background: statusColor[occ.status] + "20",
              borderColor: statusColor[occ.status] + "60",
              color: statusColor[occ.status],
            }}
          >
            {statusLabel[occ.status]}
          </span>
        </div>

        {/* required blocks (only show if selected) */}
        {isSelected && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            className="mt-3 pt-3 border-t border-white/5"
          >
            <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-1.5">
              required blocks ({occ.requiredBlocks.length})
            </div>
            <div className="flex flex-wrap gap-1 mb-3">
              {occ.requiredBlocks.map((b) => {
                const broken = occ.brokenBlocks.includes(b);
                return (
                  <button
                    key={b}
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleBlock(occ.id, b);
                    }}
                    className={cn(
                      "px-2 py-0.5 rounded-md text-[10.5px] font-mono border transition-all",
                      broken
                        ? "bg-rose-500/15 border-rose-500/40 text-rose2 line-through"
                        : "bg-lime-400/10 border-lime-400/30 text-lime2"
                    )}
                    title={broken ? "点击修复" : "点击破坏"}
                  >
                    {broken ? "✗" : "✓"} {b}
                  </button>
                );
              })}
            </div>

            {/* context */}
            {Object.keys(occ.context).length > 0 && (
              <>
                <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-1.5">
                  context captures
                </div>
                <div className="space-y-0.5 mb-3">
                  {Object.entries(occ.context).map(([k, v]) => (
                    <div key={k} className="text-[11px] font-mono flex items-center gap-2">
                      <span className="text-cyan2">{k}</span>
                      <span className="text-white/30">=</span>
                      <span className="text-white/70">{v}</span>
                    </div>
                  ))}
                </div>
              </>
            )}

            {/* actions */}
            <div className="flex flex-wrap gap-1.5">
              {occ.childDef === "factory" && (
                <>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      const existing = occurrences.filter((o) => o.parentId === occ.id && o.childDef === "arm").length;
                      onAddChild(occ.id, "arm", CHILD_TEMPLATES.arm.nameFn(existing), "(-3,0,0)");
                    }}
                  >
                    <Icons.Plus className="w-3 h-3" /> arm
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      const existing = occurrences.filter((o) => o.parentId === occ.id && o.childDef === "base").length;
                      onAddChild(occ.id, "base", CHILD_TEMPLATES.base.nameFn(existing), "(0,-1,0)");
                    }}
                  >
                    <Icons.Plus className="w-3 h-3" /> base
                  </Button>
                </>
              )}
              {occ.childDef === "arm" && (
                <>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      const existing = occurrences.filter((o) => o.parentId === occ.id && o.childDef === "tool").length;
                      onAddChild(occ.id, "tool", CHILD_TEMPLATES.tool.nameFn(existing), "(0,1,0)");
                    }}
                  >
                    <Icons.Plus className="w-3 h-3" /> tool
                  </Button>
                </>
              )}
              {occ.parentId && (
                <Button
                  variant="danger"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    onRemove(occ.id);
                  }}
                >
                  <Icons.Trash2 className="w-3 h-3" /> remove
                </Button>
              )}
            </div>
          </motion.div>
        )}
      </motion.div>

      {children.length > 0 && (
        <div className="mt-1.5 space-y-1.5 border-l border-white/8 ml-3 pl-3">
          {children.map((c) => (
            <OccNode
              key={c.id}
              occ={c}
              depth={depth + 1}
              occurrences={occurrences}
              selectedId={selectedId}
              onSelect={onSelect}
              onToggleBlock={onToggleBlock}
              onRemove={onRemove}
              onAddChild={onAddChild}
            />
          ))}
        </div>
      )}
    </div>
  );
}

const transitionTone = {
  added: { color: "text-lime2", bg: "bg-lime-400/5", border: "border-lime-400/20", icon: "Plus" },
  statusChanged: { color: "text-amber2", bg: "bg-amber-400/5", border: "border-amber-400/20", icon: "ArrowRight" },
} as const;

export function LifecycleLab() {
  const occurrences = useLifecycle((s) => s.occurrences);
  const events = useLifecycle((s) => s.events);
  const parentContext = useLifecycle((s) => s.parentContext);
  const selected = useLifecycle((s) => s.selected);
  const policy = useLifecycle((s) => s.policy);
  const addOccurrence = useLifecycle((s) => s.addOccurrence);
  const removeOccurrence = useLifecycle((s) => s.removeOccurrence);
  const toggleBlock = useLifecycle((s) => s.toggleBlock);
  const select = useLifecycle((s) => s.select);
  const setPolicy = useLifecycle((s) => s.setPolicy);
  const reset = useLifecycle((s) => s.reset);

  const root = occurrences.find((o) => o.parentId === null)!;

  return (
    <Section
      id="lifecycle-lab"
      index="10·5"
      alt
      title={
        <>
          Lifecycle Event Lab：
          <span className="text-white/50"> 触发子结构生命周期事件，看 listener + context 镜像</span>
        </>
      }
      lede={
        <>
          这一段把 SubstructureLifecycleListener 的 2 种 transition（added / statusChanged）
          做成可点按钮的演示。<strong className="text-white/80">点击任意 occurrence 展开</strong>，
          可以"破坏某个 block"看 status 变化、添加/删除子结构、切换 SubstructureSyncPolicy 看父上下文镜像如何变。
        </>
      }
    >
      <div className="grid lg:grid-cols-[1fr,1fr] gap-4 mb-4">
        {/* LEFT: tree */}
        <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Icons.GitBranch className="w-3.5 h-3.5 text-brand-300" />
              <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
                occurrence tree
              </span>
            </div>
            <Button variant="ghost" size="sm" onClick={reset}>
              <Icons.RotateCcw className="w-3 h-3" /> reset
            </Button>
          </div>
          <OccNode
            occ={root}
            depth={0}
            occurrences={occurrences}
            selectedId={selected}
            onSelect={select}
            onToggleBlock={toggleBlock}
            onRemove={removeOccurrence}
            onAddChild={addOccurrence}
          />
        </div>

        {/* RIGHT: parent context */}
        <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Icons.Database className="w-3.5 h-3.5 text-cyan2" />
              <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
                parent RuntimeStructureContext (live)
              </span>
            </div>
            <Badge tone="brand">revision {Object.keys(parentContext).length}</Badge>
          </div>

          {/* policy switch */}
          <div className="mb-3 flex items-center gap-1.5">
            <span className="text-[10px] uppercase tracking-wider text-white/40 font-mono mr-2">
              SubstructureSyncPolicy:
            </span>
            {(["NONE", "statusOnly", "statusAndContext"] as const).map((p) => (
              <button
                key={p}
                onClick={() => setPolicy(p)}
                className={cn(
                  "px-2 py-0.5 rounded text-[10.5px] font-mono border transition-all",
                  policy === p
                    ? "bg-brand-400/20 border-brand-400/40 text-brand-100"
                    : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
                )}
              >
                {p}
              </button>
            ))}
          </div>

          <div className="font-mono text-[11.5px] space-y-0.5 max-h-[280px] overflow-auto scrollbar-thin">
            {Object.keys(parentContext).length === 0 ? (
              <div className="text-white/30 italic">
                policy = NONE · context 不镜像子结构
              </div>
            ) : (
              Object.entries(parentContext).map(([k, v]) => (
                <motion.div
                  key={k}
                  layout
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  className={cn(
                    "flex items-baseline gap-2 px-2 py-0.5 rounded",
                    k.startsWith("substructure.") && "bg-white/[0.02]"
                  )}
                  style={{
                    paddingLeft: k.split(".").length * 8,
                  }}
                >
                  <span className={cn(
                    "font-mono",
                    k.startsWith("substructure.") ? "text-magenta" : "text-cyan2"
                  )}>
                    {k}
                  </span>
                  <span className="text-white/30">=</span>
                  <span className="text-white/70">{v}</span>
                </motion.div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* events stream */}
      <div className="rounded-2xl border border-white/8 bg-ink-950/80 p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Icons.Webhook className="w-3.5 h-3.5 text-magenta" />
            <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
              SubstructureLifecycleListener · event stream
            </span>
          </div>
          <span className="text-[10.5px] text-white/40 font-mono">
            {events.length} events
          </span>
        </div>

        <div className="space-y-1 max-h-[280px] overflow-auto scrollbar-thin">
          <AnimatePresence initial={false}>
            {events.length === 0 && (
              <div className="text-center py-6 text-[12px] text-white/30">
                点击 occurrence 展开 → 添加子结构 / 破坏 block / 删除 occurrence，看事件如何触发
              </div>
            )}
            {events.map((e, i) => {
              const tone = transitionTone[e.transition];
              const Icon = (Icons as any)[tone.icon];
              return (
                <motion.div
                  key={`${e.ts}-${i}`}
                  layout
                  initial={{ opacity: 0, y: -5 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className={cn(
                    "rounded-lg border p-2.5 text-[11.5px] font-mono flex items-baseline gap-3",
                    tone.bg,
                    tone.border
                  )}
                >
                  <span className="text-white/30 shrink-0 w-20">
                    [{new Date(e.ts).toLocaleTimeString("en-US", { hour12: false })}]
                  </span>
                  <Icon className={cn("w-3 h-3 mt-0.5 shrink-0", tone.color)} />
                  <span className={cn("font-bold shrink-0", tone.color)}>
                    {e.transition}
                  </span>
                  <span className="text-white/80 shrink-0">{e.occurrenceId}</span>
                  <span className="text-white/40 shrink-0">·</span>
                  <span className="text-white/50 shrink-0 hidden md:inline">
                    {e.previousStatus ?? "null"} → {e.currentStatus}
                  </span>
                  <span className="text-white/30 shrink-0 hidden lg:inline truncate">
                    {e.childPath}
                  </span>
                  <span className="text-white/60 flex-1 truncate">// {e.reason}</span>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>
      </div>

      {/* explainer */}
      <div className="mt-5 grid md:grid-cols-2 gap-3">
        <div className="rounded-xl border border-lime-400/20 bg-lime-400/5 p-3">
          <div className="text-[11px] uppercase tracking-wider text-lime2 font-mono mb-1">
            added
          </div>
          <p className="text-[12px] text-white/65 leading-relaxed">
            父 check 完成时，composer 检测到新 child occurrence 没在上次快照里 →
            立即触发 added 事件。listener 拿到 occurrenceId / parentAnchor / offsetInParent。
          </p>
        </div>
        <div className="rounded-xl border border-amber-400/20 bg-amber-400/5 p-3">
          <div className="text-[11px] uppercase tracking-wider text-amber2 font-mono mb-1">
            statusChanged
          </div>
          <p className="text-[12px] text-white/65 leading-relaxed">
            child occurrence 的 status 从 valid→invalid（或反向）时触发。
            previousStatus 字段告诉你之前是什么状态。这是最频繁的事件，建议用 revision 去重。
            child 被移除时父结构通过 statusChanged 反映（没有单独的 removed transition）。
          </p>
        </div>
      </div>
    </Section>
  );
}
