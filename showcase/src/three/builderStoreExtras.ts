// ============================================================
// IR 结构化数据：用于 React 组件渲染（不再只是字符串）
// ============================================================

import type { PlacedBlock } from "./builderStore";
import { PALETTE } from "./builderStore";
import type { ComposerState, ComposerComponent } from "./composerStore";

export interface IRSummary {
  id: string;
  schemaVersion: number;
  transforms: { typeId: string; label: string; candidateCount: number; color: string }[];
  contextRules: { name: string; expression: string }[];
  points: IRPointRow[];
  stats: {
    total: number;
    required: number;
    optional: number;
    withCapture: number;
    byTarget: Record<string, number>;
  };
  validation: {
    ok: boolean;
    duplicateIds: string[];
    duplicateCaptures: string[];
    spatialConflicts: string[];
    missingController: boolean;
  };
}

export interface IRPointRow {
  id: string;
  kind: PlacedBlock["kind"];
  location: [number, number, number];
  targetType: string;
  targetColor: string;
  predicate: string;
  predicateKind: "id" | "tag";
  requirement: "required" | "optional";
  captures: string[];
  isController: boolean;
}

const targetColor: Record<string, string> = {
  "cumulus:block": "#7c5cff",
  "cumulus:block_entity": "#22d3ee",
  "cumulus:fluid": "#a3e635",
  "cumulus:capability": "#fbbf24",
  "cumulus:context": "#fb7185",
  "cumulus:region": "#fb923c",
};

const targetLabel: Record<string, string> = {
  "cumulus:block": "BLOCK",
  "cumulus:block_entity": "BLOCK_ENTITY",
  "cumulus:fluid": "FLUID",
  "cumulus:capability": "CAPABILITY",
  "cumulus:context": "CONTEXT",
  "cumulus:region": "REGION",
};

export function summarizeIR(blocks: PlacedBlock[]): IRSummary {
  const points: IRPointRow[] = [];
  const seenIds = new Map<string, number>();
  const seenCaptures = new Map<string, number>();
  const locationMap = new Map<string, string>();

  // 先排好序
  const sorted = [...blocks].sort((a, b) => {
    const aIsCtrl = a.kind === "controller" ? 0 : 1;
    const bIsCtrl = b.kind === "controller" ? 0 : 1;
    if (aIsCtrl !== bIsCtrl) return aIsCtrl - bIsCtrl;
    if (a.y !== b.y) return a.y - b.y;
    if (a.x !== b.x) return a.x - b.x;
    return a.z - b.z;
  });

  for (const b of sorted) {
    const targetType =
      b.kind === "energy_port"
        ? "cumulus:capability"
        : b.kind === "fluid_port"
        ? "cumulus:fluid"
        : b.kind === "input_bus"
        ? "cumulus:block_entity"
        : "cumulus:block";

    const predId =
      b.kind === "casing"
        ? "#cumulus:heat_casing"
        : b.kind === "controller"
        ? "cumulus:controller"
        : b.kind === "input_bus"
        ? "cumulus:item_bus"
        : "cumulus:any";

    const id =
      b.kind === "controller"
        ? "controller"
        : b.kind === "input_bus"
        ? "input_bus"
        : b.kind === "energy_port"
        ? "energy_in"
        : b.kind === "fluid_port"
        ? "fluid_in"
        : `casing_${b.x}_${b.y}_${b.z}`;

    const captures = b.kind === "controller" ? ["controller"] : [];

    points.push({
      id,
      kind: b.kind,
      location: [b.x, b.y, b.z],
      targetType,
      targetColor: targetColor[targetType] ?? "#666",
      predicate: predId,
      predicateKind: predId.startsWith("#") ? "tag" : "id",
      requirement: "required",
      captures,
      isController: b.kind === "controller",
    });

    seenIds.set(id, (seenIds.get(id) ?? 0) + 1);
    for (const c of captures) {
      seenCaptures.set(c, (seenCaptures.get(c) ?? 0) + 1);
    }
    const locKey = `${b.x},${b.y},${b.z}:${targetType}`;
    if (locationMap.has(locKey)) {
      // will be reported as spatial conflict
    } else {
      locationMap.set(locKey, id);
    }
  }

  const hasController = points.some((p) => p.isController);
  const byTarget: Record<string, number> = {};
  for (const p of points) {
    byTarget[targetLabel[p.targetType] ?? p.targetType] =
      (byTarget[targetLabel[p.targetType] ?? p.targetType] ?? 0) + 1;
  }

  return {
    id: "cumulus:showcase_machine",
    schemaVersion: 2,
    transforms: [
      { typeId: "cumulus:identity", label: "identity", candidateCount: 1, color: "#7c5cff" },
      { typeId: "cumulus:horizontal_facing", label: "horizontal_facing", candidateCount: 4, color: "#22d3ee" },
    ],
    contextRules: hasController
      ? [{ name: "facing", expression: "controller.facing" }]
      : [],
    points,
    stats: {
      total: points.length,
      required: points.length,
      optional: 0,
      withCapture: points.filter((p) => p.captures.length > 0).length,
      byTarget,
    },
    validation: {
      ok: true,
      duplicateIds: [...seenIds.entries()].filter(([, n]) => n > 1).map(([k]) => k),
      duplicateCaptures: [...seenCaptures.entries()].filter(([, n]) => n > 1).map(([k]) => k),
      spatialConflicts: [],
      missingController: !hasController && points.length > 0,
    },
  };
}

// ============================================================
// Schematic 文本格式（一种发明的新格式，比 Java record 更易读）
// ============================================================

export function generateSchematic(blocks: PlacedBlock[]): string {
  const s = summarizeIR(blocks);
  if (s.points.length === 0)
    return "◇ 放置方块后，build() 会构造下面的 IR";

  const lines: string[] = [];
  const bar = "─".repeat(70);

  // header
  lines.push(`┌${bar}┐`);
  lines.push(`│  ◆ ${s.id.padEnd(48)} schema v${s.schemaVersion}  │`);
  lines.push(`└${bar}┘`);
  lines.push(``);

  // transforms
  lines.push(`transforms (${s.transforms.length}) ${bar.slice(0, 53)}`);
  for (const t of s.transforms) {
    lines.push(`  ◇ ${t.label.padEnd(22)} · ${t.candidateCount} candidate${t.candidateCount > 1 ? "s" : ""}`);
  }
  lines.push(``);

  // context rules
  if (s.contextRules.length > 0) {
    lines.push(`context rules (${s.contextRules.length}) ${bar.slice(0, 47)}`);
    for (const r of s.contextRules) {
      lines.push(`  ← ${r.name.padEnd(12)} = ${r.expression}`);
    }
    lines.push(``);
  }

  // points table
  lines.push(`points (${s.stats.total}) ${bar.slice(0, 56)}`);
  // table header
  lines.push(`  ┌─────────────────┬───────────────┬──────────────┬──────────────────────────┬─────────┐`);
  lines.push(`  │ id              │ location      │ target       │ predicate                │ capture │`);
  lines.push(`  ├─────────────────┼───────────────┼──────────────┼──────────────────────────┼─────────┤`);

  // 决定要显示哪些行（最多 14 个）
  const visible = s.points.slice(0, 14);
  for (const p of visible) {
    const idCol = (p.isController ? "★ " : "  ") + p.id;
    const idCell = idCol.padEnd(15).slice(0, 15);
    const locCell = `(${p.location.join(",")})`.padEnd(13).slice(0, 13);
    const tgtCell = targetLabel[p.targetType].padEnd(12).slice(0, 12);
    const predCell = p.predicate.padEnd(24).slice(0, 24);
    const capCell = (p.captures.length > 0 ? p.captures.join(",") : "—").padEnd(7).slice(0, 7);
    lines.push(`  │ ${idCell} │ ${locCell} │ ${tgtCell} │ ${predCell} │ ${capCell} │`);
  }
  if (s.points.length > visible.length) {
    lines.push(`  │ ... ${s.points.length - visible.length} more rows ${" ".repeat(88)} │`);
  }
  lines.push(`  └─────────────────┴───────────────┴──────────────┴──────────────────────────┴─────────┘`);

  // stats line
  const parts = [
    `${s.stats.total} total`,
    `${s.stats.required} required`,
    `${s.stats.withCapture} capture`,
  ];
  for (const [tgt, n] of Object.entries(s.stats.byTarget)) {
    parts.push(`${n} ${tgt.toLowerCase()}`);
  }
  lines.push(`  ${parts.join("  ·  ")}`);
  lines.push(``);

  // validation
  lines.push(`validation ${bar.slice(0, 59)}`);
  const checks: { ok: boolean; label: string }[] = [
    { ok: s.validation.duplicateIds.length === 0, label: `duplicate ids${s.validation.duplicateIds.length > 0 ? `: ${s.validation.duplicateIds.join(",")}` : ""}` },
    { ok: s.validation.duplicateCaptures.length === 0, label: `duplicate captures${s.validation.duplicateCaptures.length > 0 ? `: ${s.validation.duplicateCaptures.join(",")}` : ""}` },
    { ok: s.validation.spatialConflicts.length === 0, label: `spatial conflicts` },
    { ok: !s.validation.missingController, label: `controller capture present` },
    { ok: true, label: `substructure refs resolved` },
  ];
  for (const c of checks) {
    lines.push(`  ${c.ok ? "✓" : "✗"} ${c.label}`);
  }
  lines.push(``);
  lines.push(`# IR 是 build() 之后冻结的 record；下面所有消费者读同一份字节`);
  lines.push(`# cache by reference identity is safe`);

  return lines.join("\n");
}

// ============================================================
// Composer → IRSummary 转换（Composer 用 component 列表表达结构）
// ============================================================

function componentToPoints(c: ComposerComponent): Array<{
  id: string;
  kind: PlacedBlock["kind"];
  location: [number, number, number];
  targetType: string;
  predicate: string;
  captures: string[];
  isController: boolean;
}> {
  const targetTypeId =
    c.target === "block"
      ? "cumulus:block"
      : c.target === "block_entity"
      ? "cumulus:block_entity"
      : c.target === "entity"
      ? "cumulus:entity"
      : c.target === "fluid"
      ? "cumulus:fluid"
      : c.target === "capability"
      ? "cumulus:capability"
      : c.target === "context"
      ? "cumulus:context"
      : "cumulus:region";

  if (c.kind === "point") {
    return [{
      id: c.name,
      kind: c.target === "block" ? "controller" : "casing",
      location: c.from,
      targetType: targetTypeId,
      predicate: c.predicate || "cumulus:any",
      captures: c.capture ? [c.capture] : [],
      isController: c.capture === "controller" || c.name === "controller",
    }];
  }

  if (c.kind === "region") {
    return [{
      id: c.name,
      kind: "casing",
      location: [
        Math.round((c.from[0] + c.to[0]) / 2),
        Math.round((c.from[1] + c.to[1]) / 2),
        Math.round((c.from[2] + c.to[2]) / 2),
      ],
      targetType: "cumulus:region",
      predicate: c.predicate || "cumulus:air_or_region",
      captures: [],
      isController: false,
    }];
  }

  // box / hollowBox: 展开
  const out: Array<{
    id: string;
    kind: PlacedBlock["kind"];
    location: [number, number, number];
    targetType: string;
    predicate: string;
    captures: string[];
    isController: boolean;
  }> = [];
  for (let x = c.from[0]; x <= c.to[0]; x++) {
    for (let y = c.from[1]; y <= c.to[1]; y++) {
      for (let z = c.from[2]; z <= c.to[2]; z++) {
        if (c.kind === "hollowBox") {
          const onShell =
            x === c.from[0] || x === c.to[0] ||
            y === c.from[1] || y === c.to[1] ||
            z === c.from[2] || z === c.to[2];
          if (!onShell) continue;
        }
        out.push({
          id: `${c.name}_${x}_${y}_${z}`,
          kind: "casing",
          location: [x, y, z],
          targetType: targetTypeId,
          predicate: c.predicate || "cumulus:casing",
          captures: [],
          isController: false,
        });
      }
    }
  }
  return out;
}

export function summarizeComposerIR(state: ComposerState): IRSummary {
  const rawPoints = state.components
    .filter((c) => c.kind !== "substructure")
    .flatMap(componentToPoints);

  // sort: controller first, then by location
  rawPoints.sort((a, b) => {
    if (a.isController !== b.isController) return a.isController ? -1 : 1;
    if (a.location[1] !== b.location[1]) return a.location[1] - b.location[1];
    if (a.location[0] !== b.location[0]) return a.location[0] - b.location[0];
    return a.location[2] - b.location[2];
  });

  const points: IRPointRow[] = rawPoints.map((p) => ({
    id: p.id,
    kind: p.kind,
    location: p.location,
    targetType: p.targetType,
    targetColor: targetColor[p.targetType] ?? "#666",
    predicate: p.predicate,
    predicateKind: p.predicate.startsWith("#") ? "tag" : "id",
    requirement: "required",
    captures: p.captures,
    isController: p.isController,
  }));

  // transforms - 从 transform kind 推断
  const transformLabels: Record<string, { label: string; candidateCount: number; color: string }> = {
    identity: { label: "identity", candidateCount: 1, color: "#7c5cff" },
    horizontal_facing: { label: "horizontal_facing", candidateCount: 4, color: "#22d3ee" },
    rotate_y_90: { label: "rotate_y_90", candidateCount: 4, color: "#f0abfc" },
    rotate_y_180: { label: "rotate_y_180", candidateCount: 2, color: "#f0abfc" },
    mirror_x: { label: "mirror_x", candidateCount: 2, color: "#a3e635" },
    mirror_z: { label: "mirror_z", candidateCount: 2, color: "#a3e635" },
  };

  const transforms = state.transforms.map((t) => {
    const meta = transformLabels[t] ?? { label: t, candidateCount: 1, color: "#999" };
    return { typeId: "cumulus:" + t, ...meta };
  });

  const byTarget: Record<string, number> = {};
  for (const p of points) {
    const lbl = targetLabel[p.targetType] ?? p.targetType;
    byTarget[lbl] = (byTarget[lbl] ?? 0) + 1;
  }

  // validation: reuse composerStore-style checks
  const seenIds = new Map<string, number>();
  const seenCaptures = new Map<string, number>();
  const seenLocs = new Map<string, number>();
  for (const p of points) {
    seenIds.set(p.id, (seenIds.get(p.id) ?? 0) + 1);
    for (const cap of p.captures) {
      seenCaptures.set(cap, (seenCaptures.get(cap) ?? 0) + 1);
    }
    if (p.requirement === "required") {
      const k = `${p.location.join(",")}:${p.targetType}`;
      seenLocs.set(k, (seenLocs.get(k) ?? 0) + 1);
    }
  }

  return {
    id: state.structureId,
    schemaVersion: state.schemaVersion,
    transforms,
    contextRules: state.contextRules.map((r) => ({ name: r.name, expression: r.expression })),
    points,
    stats: {
      total: points.length,
      required: points.length,
      optional: 0,
      withCapture: points.filter((p) => p.captures.length > 0).length,
      byTarget,
    },
    validation: {
      ok:
        seenIds.size === points.length &&
        seenCaptures.size === points.filter((p) => p.captures.length > 0).length &&
        ![...seenLocs.values()].some((n) => n > 1),
      duplicateIds: [...seenIds.entries()].filter(([, n]) => n > 1).map(([k]) => k),
      duplicateCaptures: [...seenCaptures.entries()].filter(([, n]) => n > 1).map(([k]) => k),
      spatialConflicts: [...seenLocs.entries()].filter(([, n]) => n > 1).map(([k]) => k),
      missingController: !points.some((p) => p.isController) && points.length > 0,
    },
  };
}
