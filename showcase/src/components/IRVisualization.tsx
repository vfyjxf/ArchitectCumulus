import { motion } from "framer-motion";
import { AlertCircle, CheckCircle2, Star } from "lucide-react";
import type { IRSummary } from "@/three/builderStoreExtras";
import { cn } from "@/lib/utils";

export function IRVisualization({ summary }: { summary: IRSummary }) {
  if (summary.points.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-white/10 p-8 text-center">
        <p className="text-[12.5px] text-white/40">
          放置方块后，build() 会构造 StructureDefinition record，这里展示它的字段值。
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* header card */}
      <motion.div
        initial={{ opacity: 0, y: 5 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-xl border border-brand-400/30 bg-gradient-to-br from-brand-500/10 to-transparent p-4"
      >
        <div className="flex items-baseline justify-between flex-wrap gap-2">
          <div className="flex items-baseline gap-3">
            <span className="text-[10px] uppercase tracking-wider text-brand-300/60 font-mono">
              StructureDefinition
            </span>
            <code className="font-mono text-[14px] font-bold text-brand-200">
              {summary.id}
            </code>
          </div>
          <span className="font-mono text-[10.5px] text-white/40">
            schemaVersion = {summary.schemaVersion}
          </span>
        </div>
        <div className="text-[11px] text-white/50 mt-2 font-mono">
          // build() 之后冻结的 record · 下游消费者 (compiler/datagen/test) 读同一份字节
        </div>
      </motion.div>

      {/* transforms */}
      <div className="rounded-xl border border-white/8 bg-white/[0.02] p-3">
        <div className="flex items-center justify-between mb-2">
          <span className="text-[10px] uppercase tracking-wider text-white/40 font-mono">
            transforms · List({summary.transforms.length})
          </span>
          <span className="text-[10.5px] text-cyan2 font-mono">
            总候选 {summary.transforms.reduce((a, t) => a + t.candidateCount, 0)}
          </span>
        </div>
        <div className="flex flex-wrap gap-1.5">
          {summary.transforms.map((t) => (
            <span
              key={t.typeId}
              className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md border text-[11px] font-mono"
              style={{
                background: t.color + "15",
                borderColor: t.color + "40",
                color: t.color,
              }}
            >
              ◇ {t.label}
              <span className="text-white/40">×{t.candidateCount}</span>
            </span>
          ))}
        </div>
      </div>

      {/* context rules */}
      {summary.contextRules.length > 0 && (
        <div className="rounded-xl border border-white/8 bg-white/[0.02] p-3">
          <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-2">
            contextRules · List({summary.contextRules.length})
          </div>
          <div className="space-y-1">
            {summary.contextRules.map((r) => (
              <div
                key={r.name}
                className="flex items-baseline gap-2 font-mono text-[11.5px]"
              >
                <span className="text-magenta">←</span>
                <code className="text-magenta font-semibold w-20">{r.name}</code>
                <span className="text-white/30">=</span>
                <code className="text-white/70">{r.expression}</code>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* points table */}
      <div className="rounded-xl border border-white/8 bg-white/[0.02] overflow-hidden">
        <div className="flex items-center justify-between p-3 border-b border-white/5">
          <span className="text-[10px] uppercase tracking-wider text-white/40 font-mono">
            points · List({summary.stats.total} StructurePointDefinition)
          </span>
          <div className="flex gap-2 text-[10.5px] font-mono">
            {Object.entries(summary.stats.byTarget).map(([k, n]) => (
              <span key={k} className="text-white/50">
                {n} <span className="text-white/30">{k.toLowerCase()}</span>
              </span>
            ))}
          </div>
        </div>
        <div className="overflow-x-auto scrollbar-thin">
          <table className="w-full text-[11.5px] font-mono">
            <thead>
              <tr className="border-b border-white/5 text-[10px] uppercase tracking-wider text-white/40">
                <th className="text-left px-3 py-1.5 font-normal">#</th>
                <th className="text-left px-3 py-1.5 font-normal">id</th>
                <th className="text-left px-3 py-1.5 font-normal">location</th>
                <th className="text-left px-3 py-1.5 font-normal">target</th>
                <th className="text-left px-3 py-1.5 font-normal">predicate</th>
                <th className="text-left px-3 py-1.5 font-normal">req</th>
                <th className="text-left px-3 py-1.5 font-normal">capture</th>
              </tr>
            </thead>
            <tbody>
              {summary.points.slice(0, 18).map((p, i) => (
                <motion.tr
                  key={`${p.id}-${i}`}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: i * 0.02 }}
                  className={cn(
                    "border-b border-white/[0.03] hover:bg-white/[0.03] transition-colors",
                    p.isController && "bg-brand-500/[0.06]"
                  )}
                >
                  <td className="px-3 py-1.5 text-white/30">
                    {(i + 1).toString().padStart(2, "0")}
                  </td>
                  <td className="px-3 py-1.5">
                    <div className="flex items-center gap-1.5">
                      {p.isController && (
                        <Star className="w-3 h-3 text-brand-300 fill-brand-300" />
                      )}
                      <code className="text-white/85">{p.id}</code>
                    </div>
                  </td>
                  <td className="px-3 py-1.5 text-white/60">
                    ({p.location.join(",")})
                  </td>
                  <td className="px-3 py-1.5">
                    <span
                      className="inline-block px-1.5 py-0.5 rounded text-[10px] font-bold"
                      style={{
                        background: p.targetColor + "20",
                        color: p.targetColor,
                      }}
                    >
                      {p.targetType.replace("cumulus:", "").toUpperCase()}
                    </span>
                  </td>
                  <td className="px-3 py-1.5">
                    <code
                      className={cn(
                        p.predicateKind === "tag" ? "text-amber2" : "text-cyan2"
                      )}
                    >
                      {p.predicate}
                    </code>
                  </td>
                  <td className="px-3 py-1.5">
                    <span className="text-lime2 text-[10px] font-bold">
                      {p.requirement}
                    </span>
                  </td>
                  <td className="px-3 py-1.5">
                    {p.captures.length > 0 ? (
                      <div className="flex gap-1 flex-wrap">
                        {p.captures.map((c) => (
                          <span
                            key={c}
                            className="px-1 py-0.5 rounded bg-magenta/15 border border-magenta/30 text-magenta text-[9.5px]"
                          >
                            {c}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <span className="text-white/20">—</span>
                    )}
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
        {summary.points.length > 18 && (
          <div className="p-2 text-center text-[11px] text-white/40 font-mono bg-white/[0.02] border-t border-white/5">
            ... 还有 {summary.points.length - 18} 行 (provenance: Components.box/hollowBox 展开后的等价 point)
          </div>
        )}
      </div>

      {/* validation */}
      <div
        className={cn(
          "rounded-xl border p-3",
          summary.validation.ok && !summary.validation.missingController
            ? "border-lime-400/30 bg-lime-400/[0.05]"
            : "border-amber-400/30 bg-amber-400/[0.05]"
        )}
      >
        <div className="text-[10px] uppercase tracking-wider text-white/40 font-mono mb-2">
          build-time validation · StructureDefinitionValidator
        </div>
        <div className="grid sm:grid-cols-2 gap-1.5">
          <ValidationRow
            ok={summary.validation.duplicateIds.length === 0}
            label={
              summary.validation.duplicateIds.length === 0
                ? "no duplicate point ids"
                : `duplicate ids: ${summary.validation.duplicateIds.join(", ")}`
            }
          />
          <ValidationRow
            ok={summary.validation.duplicateCaptures.length === 0}
            label={
              summary.validation.duplicateCaptures.length === 0
                ? "no duplicate captures"
                : `duplicate captures: ${summary.validation.duplicateCaptures.join(", ")}`
            }
          />
          <ValidationRow
            ok={summary.validation.spatialConflicts.length === 0}
            label="no spatial conflicts"
          />
          <ValidationRow
            ok={!summary.validation.missingController}
            label={
              summary.validation.missingController
                ? "missing controller capture (context rule 'facing' will fail)"
                : "controller capture present"
            }
          />
        </div>
      </div>
    </div>
  );
}

function ValidationRow({ ok, label }: { ok: boolean; label: string }) {
  return (
    <div className="flex items-center gap-1.5 text-[11.5px]">
      {ok ? (
        <CheckCircle2 className="w-3 h-3 text-lime2 shrink-0" />
      ) : (
        <AlertCircle className="w-3 h-3 text-amber2 shrink-0" />
      )}
      <span className={ok ? "text-white/70" : "text-amber2"}>{label}</span>
    </div>
  );
}
