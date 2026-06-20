import { motion, AnimatePresence } from "framer-motion";
import * as Icons from "lucide-react";
import { useMemo, useState } from "react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import {
  PREDICATE_LIBRARY,
  SAMPLE_PRESET_NAMES,
  evaluateChain,
  usePredicateComposer,
  type ChainPredicate,
  type PredicateType,
} from "@/three/predicateComposer";
import { cn } from "@/lib/utils";

// ============================================================
// 谓词库 (左侧)
// ============================================================

function PredicateLibraryPanel() {
  const addPredicate = usePredicateComposer((s) => s.addPredicate);
  const [filter, setFilter] = useState("");

  const filtered = PREDICATE_LIBRARY.filter((p) =>
    p.label.toLowerCase().includes(filter.toLowerCase()) ||
    p.description.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-3">
      <div className="flex items-center gap-2 mb-2">
        <Icons.Library className="w-3.5 h-3.5 text-brand-300" />
        <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
          predicate library
        </span>
      </div>
      <input
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        placeholder="搜索..."
        className="w-full bg-ink-950 border border-white/10 rounded-md px-2 py-1.5 text-[11.5px] font-mono mb-2 outline-none focus:border-brand-400"
      />
      <div className="space-y-1 max-h-[440px] overflow-auto scrollbar-thin">
        {filtered.map((p) => (
          <button
            key={p.type}
            onClick={() => addPredicate(p.type)}
            className="w-full text-left rounded-lg border border-white/8 bg-white/[0.02] p-2 hover:bg-white/[0.05] hover:border-white/15 transition-all group"
          >
            <div className="flex items-center gap-1.5 mb-0.5">
              <span
                className="w-2 h-2 rounded-sm"
                style={{ background: p.color }}
              />
              <code
                className="font-mono text-[11.5px] font-semibold"
                style={{ color: p.color }}
              >
                {p.label}
              </code>
              <Icons.Plus className="w-3 h-3 ml-auto text-white/20 group-hover:text-white/60" />
            </div>
            <div className="text-[10.5px] text-white/45 leading-tight">
              {p.description}
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

// ============================================================
// 谓词链 (中间)
// ============================================================

function ChainPredicateRow({
  p,
  index,
  total,
  result,
}: {
  p: ChainPredicate;
  index: number;
  total: number;
  result?: { passed: boolean; skipped: boolean; reason: string };
}) {
  const def = PREDICATE_LIBRARY.find((x) => x.type === p.type)!;
  const update = usePredicateComposer((s) => s.updatePredicate);
  const remove = usePredicateComposer((s) => s.removePredicate);
  const move = usePredicateComposer((s) => s.movePredicate);

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, x: -10 }}
      className={cn(
        "rounded-xl border bg-white/[0.02] overflow-hidden transition-all",
        result
          ? result.passed
            ? "border-lime-400/30 bg-lime-400/[0.03]"
            : result.skipped
            ? "border-white/8 opacity-60"
            : "border-rose-500/40 bg-rose-500/[0.05]"
          : "border-white/8"
      )}
    >
      <div className="flex items-center gap-2 p-2.5">
        <span className="font-mono text-[10px] text-white/30 w-5">
          {(index + 1).toString().padStart(2, "0")}
        </span>
        <div
          className="w-6 h-6 rounded-md flex items-center justify-center border shrink-0"
          style={{ background: def.color + "20", borderColor: def.color + "40" }}
        >
          <span
            className="font-mono text-[10px] font-bold"
            style={{ color: def.color }}
          >
            {def.label[0].toUpperCase()}
          </span>
        </div>
        <code
          className="font-mono text-[12.5px] font-semibold"
          style={{ color: def.color }}
        >
          {def.label}
        </code>
        <span className="text-white/30 text-[10px]">·</span>
        <select
          value={p.required ? "required" : "optional"}
          onChange={(e) =>
            update(p.id, { required: e.target.value === "required" })
          }
          className="bg-ink-950 border border-white/10 rounded-md px-1.5 py-0.5 text-[10.5px] font-mono outline-none"
        >
          <option value="required">REQUIRED</option>
          <option value="optional">OPTIONAL</option>
        </select>

        <div className="ml-auto flex items-center gap-0.5">
          {result && (
            <>
              {result.passed ? (
                <Icons.Check className="w-3.5 h-3.5 text-lime2" />
              ) : result.skipped ? (
                <Icons.Minus className="w-3.5 h-3.5 text-white/30" />
              ) : (
                <Icons.X className="w-3.5 h-3.5 text-rose2" />
              )}
            </>
          )}
          <button
            onClick={() => move(p.id, -1)}
            disabled={index === 0}
            className="text-white/30 hover:text-white/70 disabled:opacity-20"
          >
            <Icons.ChevronUp className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => move(p.id, 1)}
            disabled={index === total - 1}
            className="text-white/30 hover:text-white/70 disabled:opacity-20"
          >
            <Icons.ChevronDown className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => remove(p.id)}
            className="text-rose2/50 hover:text-rose2"
          >
            <Icons.Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      <div className="px-2.5 pb-2.5 grid sm:grid-cols-2 gap-2">
        {def.fields.map((f) => (
          <label key={f.key} className="block">
            <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-0.5">
              {f.label}
            </div>
            <input
              value={p.fields[f.key] ?? ""}
              onChange={(e) =>
                update(p.id, {
                  fields: { ...p.fields, [f.key]: e.target.value },
                })
              }
              placeholder={f.placeholder}
              className="w-full bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11.5px] font-mono outline-none focus:border-brand-400"
            />
          </label>
        ))}
      </div>

      {result && (
        <div
          className={cn(
            "px-2.5 py-1.5 border-t text-[11px] font-mono flex items-baseline gap-2",
            result.passed
              ? "border-lime-400/20 bg-lime-400/[0.05] text-lime2"
              : result.skipped
              ? "border-white/5 bg-white/[0.02] text-white/40"
              : "border-rose-500/20 bg-rose-500/[0.05] text-rose2"
          )}
        >
          <span>{result.reason}</span>
        </div>
      )}
    </motion.div>
  );
}

// ============================================================
// Sample Block (右侧)
// ============================================================

function SamplePanel() {
  const sample = usePredicateComposer((s) => s.sample);
  const setSample = usePredicateComposer((s) => s.setSample);
  const loadPreset = usePredicateComposer((s) => s.loadSamplePreset);

  return (
    <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-3">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <Icons.Box className="w-3.5 h-3.5 text-cyan2" />
          <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
            sample block
          </span>
        </div>
      </div>

      <div className="flex flex-wrap gap-1 mb-3">
        {SAMPLE_PRESET_NAMES.map((p) => (
          <button
            key={p}
            onClick={() => loadPreset(p)}
            className={cn(
              "px-2 py-0.5 rounded-md text-[10.5px] font-mono border transition-all",
              sample.blockId.includes(p) || (p === "empty" && sample.blockId === "minecraft:stone")
                ? "bg-cyan-400/15 border-cyan-400/30 text-cyan2"
                : "bg-white/[0.03] border-white/8 text-white/50 hover:text-white/80"
            )}
          >
            {p}
          </button>
        ))}
      </div>

      <div className="space-y-2">
        <label className="block">
          <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-0.5">
            block id
          </div>
          <input
            value={sample.blockId}
            onChange={(e) => setSample({ blockId: e.target.value })}
            className="w-full bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11.5px] font-mono outline-none focus:border-brand-400"
          />
        </label>

        <label className="block">
          <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-0.5">
            tags (逗号分隔)
          </div>
          <input
            value={sample.tags.join(", ")}
            onChange={(e) =>
              setSample({
                tags: e.target.value.split(",").map((s) => s.trim()).filter(Boolean),
              })
            }
            className="w-full bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11.5px] font-mono outline-none focus:border-brand-400"
          />
        </label>

        <div>
          <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-0.5">
            block state (property=value, ...)
          </div>
          {Object.entries(sample.stateProperties).map(([k, v]) => (
            <div key={k} className="flex items-center gap-1 mb-1">
              <input
                value={k}
                onChange={(e) => {
                  const next = { ...sample.stateProperties };
                  delete next[k];
                  next[e.target.value] = v;
                  setSample({ stateProperties: next });
                }}
                className="bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11px] font-mono w-1/2 outline-none"
              />
              <span className="text-white/30">=</span>
              <input
                value={v}
                onChange={(e) =>
                  setSample({
                    stateProperties: { ...sample.stateProperties, [k]: e.target.value },
                  })
                }
                className="bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11px] font-mono w-1/2 outline-none"
              />
              <button
                onClick={() => {
                  const next = { ...sample.stateProperties };
                  delete next[k];
                  setSample({ stateProperties: next });
                }}
                className="text-rose2/50 hover:text-rose2"
              >
                <Icons.X className="w-3 h-3" />
              </button>
            </div>
          ))}
          <button
            onClick={() =>
              setSample({
                stateProperties: { ...sample.stateProperties, ["new_prop"]: "value" },
              })
            }
            className="text-[10.5px] text-brand-300 hover:text-brand-200 font-mono"
          >
            + add property
          </button>
        </div>

        <label className="block">
          <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-0.5">
            block entity type
          </div>
          <input
            value={sample.blockEntityType}
            onChange={(e) => setSample({ blockEntityType: e.target.value })}
            placeholder="(empty = no BE)"
            className="w-full bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11.5px] font-mono outline-none focus:border-brand-400"
          />
        </label>

        <label className="block">
          <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-0.5">
            block entity NBT (JSON)
          </div>
          <textarea
            value={sample.blockEntityNbt}
            onChange={(e) => setSample({ blockEntityNbt: e.target.value })}
            rows={3}
            className="w-full bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11px] font-mono outline-none focus:border-brand-400 resize-none scrollbar-thin"
          />
        </label>

        <div>
          <div className="text-[9.5px] uppercase tracking-wider text-white/40 font-mono mb-0.5">
            capabilities (name + amount)
          </div>
          {sample.capabilities.map((c, i) => (
            <div key={i} className="flex items-center gap-1 mb-1">
              <input
                value={c.name}
                onChange={(e) => {
                  const next = [...sample.capabilities];
                  next[i] = { ...c, name: e.target.value };
                  setSample({ capabilities: next });
                }}
                className="bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11px] font-mono w-1/2 outline-none"
              />
              <input
                type="number"
                value={c.amount}
                onChange={(e) => {
                  const next = [...sample.capabilities];
                  next[i] = { ...c, amount: parseInt(e.target.value) || 0 };
                  setSample({ capabilities: next });
                }}
                className="bg-ink-950 border border-white/10 rounded-md px-2 py-1 text-[11px] font-mono w-1/4 outline-none"
              />
              <button
                onClick={() =>
                  setSample({
                    capabilities: sample.capabilities.filter((_, j) => j !== i),
                  })
                }
                className="text-rose2/50 hover:text-rose2"
              >
                <Icons.X className="w-3 h-3" />
              </button>
            </div>
          ))}
          <button
            onClick={() =>
              setSample({
                capabilities: [...sample.capabilities, { name: "cumulus:new", amount: 0 }],
              })
            }
            className="text-[10.5px] text-brand-300 hover:text-brand-200 font-mono"
          >
            + add capability
          </button>
        </div>
      </div>
    </div>
  );
}

// ============================================================
// Main
// ============================================================

export function PredicateComposer() {
  const chain = usePredicateComposer((s) => s.chain);
  const sample = usePredicateComposer((s) => s.sample);
  const state = usePredicateComposer();

  const evalResult = useMemo(() => evaluateChain(chain, sample), [chain, sample]);

  // generate code
  const code = useMemo(() => generatePredicateCode(chain), [chain]);

  return (
    <Section
      id="predicate-composer"
      index="03·5"
      alt
      title={
        <>
          Predicate Composer：
          <span className="text-white/50"> 拖拽谓词链 → 实时求值</span>
        </>
      }
      lede={
        <>
          一个 StructurePoint 真正的检查是 predicate chain。下方是一个完整的谓词链构建器：
          <strong className="text-white/80"> 左侧挑谓词 → 中间排序/配置 → 右侧改 sample block</strong>
          ，每次变化都立刻看到链求值结果（包括 required 短路和 optional skip 的语义）。
        </>
      }
    >
      <div className="grid lg:grid-cols-[260px,1.3fr,280px] gap-4 mb-5">
        <PredicateLibraryPanel />

        {/* chain */}
        <div>
          <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-3">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Icons.ListOrdered className="w-3.5 h-3.5 text-brand-300" />
                <span className="text-[11px] uppercase tracking-wider text-white/40 font-mono">
                  predicate chain ({chain.length})
                </span>
              </div>
              <div
                className={cn(
                  "px-2 py-0.5 rounded-md font-mono text-[11px] font-bold border",
                  evalResult.finalPassed
                    ? "bg-lime-400/15 border-lime-400/30 text-lime2"
                    : "bg-rose-500/15 border-rose-500/30 text-rose2"
                )}
              >
                {evalResult.finalPassed ? "ALL PASS" : "FAIL"}
              </div>
            </div>

            <div className="space-y-1.5 min-h-[200px]">
              <AnimatePresence initial={false}>
                {chain.map((p, i) => (
                  <ChainPredicateRow
                    key={p.id}
                    p={p}
                    index={i}
                    total={chain.length}
                    result={evalResult.results[i]}
                  />
                ))}
              </AnimatePresence>

              {chain.length === 0 && (
                <div className="text-center py-10 text-[12px] text-white/30">
                  从左侧 predicate library 选一个谓词添加
                </div>
              )}
            </div>

            {/* 链尾指示 */}
            {chain.length > 0 && (
              <div className="mt-3 pt-3 border-t border-white/5 text-[11px] text-white/40 font-mono flex items-center gap-2">
                <Icons.CornerDownRight className="w-3 h-3" />
                {evalResult.finalPassed ? (
                  <span className="text-lime2">
                    ✓ StructurePoint check pass — capture / context rule 可执行
                  </span>
                ) : evalResult.shortCircuitedAt !== undefined ? (
                  <span className="text-rose2">
                    ✗ 在第 {evalResult.shortCircuitedAt + 1} 个谓词短路 —
                    后续 {chain.length - evalResult.shortCircuitedAt - 1} 个不再求值
                  </span>
                ) : (
                  <span className="text-amber2">
                    部分 optional fail，整体仍 PASS
                  </span>
                )}
              </div>
            )}
          </div>
        </div>

        <SamplePanel />
      </div>

      {/* generated code + final result */}
      <div className="grid lg:grid-cols-[1.4fr,1fr] gap-4">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Icons.Code2 className="w-4 h-4 text-cyan2" />
            <span className="text-[12.5px] font-semibold">生成的 DSL</span>
          </div>
          <CodeBlock code={code} title="predicate_chain.java" />
        </div>

        <div>
          <div className="flex items-center gap-2 mb-2">
            <Icons.Activity className="w-4 h-4 text-amber2" />
            <span className="text-[12.5px] font-semibold">求值过程</span>
          </div>
          <div className="rounded-2xl border border-white/8 bg-ink-950/80 p-3 font-mono text-[11.5px] space-y-0.5 max-h-[280px] overflow-auto scrollbar-thin">
            <div className="text-white/40">// StructureCheckExecutor 内部</div>
            {chain.map((p, i) => {
              const r = evalResult.results[i];
              const def = PREDICATE_LIBRARY.find((x) => x.type === p.type)!;
              return (
                <div key={p.id}>
                  <span className="text-white/30">[#{(i + 1).toString().padStart(2, "0")}]</span>{" "}
                  <span style={{ color: def.color }}>{def.label}</span>{" "}
                  {r ? (
                    r.passed ? (
                      <span className="text-lime2">→ PASS</span>
                    ) : r.skipped ? (
                      <span className="text-white/40">→ SKIP (optional)</span>
                    ) : (
                      <span className="text-rose2">→ FAIL (短路)</span>
                    )
                  ) : (
                    <span className="text-white/30">→ 未求值</span>
                  )}
                  {r && (
                    <div className="ml-7 text-[10.5px] text-white/50 leading-tight">
                      {r.reason}
                    </div>
                  )}
                </div>
              );
            })}
            <div className="pt-2 mt-2 border-t border-white/5">
              {evalResult.finalPassed ? (
                <span className="text-lime2 font-bold">
                  ✓ final: PASS → 结构有效，capture 触发
                </span>
              ) : (
                <span className="text-rose2 font-bold">
                  ✗ final: INVALID → 发 DiagnosticNode(PREDICATE_FAILED)
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </Section>
  );
}

function generatePredicateCode(chain: ChainPredicate[]): string {
  if (chain.length === 0) {
    return "// 从左侧添加谓词\nTargets.block()  // 没有任何 predicate";
  }
  const lines: string[] = [];
  lines.push("Targets.block()");
  for (const p of chain) {
    const def = PREDICATE_LIBRARY.find((x) => x.type === p.type)!;
    const args = def.fields
      .map((f) => `"${p.fields[f.key] ?? ""}"`)
      .join(", ");
    lines.push(`    .where(PredicateDescriptors.${p.type}(${args}))${!p.required ? " // OPTIONAL" : ""}`);
  }
  lines.push(`    .capture("controller")  // 链通过后捕获`);
  return lines.join("\n");
}
