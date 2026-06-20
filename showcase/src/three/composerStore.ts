import { create } from "zustand";

// ============================================================
// Structure Composer Store
// 用户在 UI 里拼装一个 StructureDefinition，
// 实时生成 Java DSL + IR 摘要 + check plan 统计 + 校验结果
// ============================================================

export type TargetKind =
  | "block"
  | "block_entity"
  | "entity"
  | "fluid"
  | "capability"
  | "context"
  | "region";

export type ComponentKind = "point" | "box" | "hollowBox" | "region" | "substructure";

export interface ComposerComponent {
  id: string;
  kind: ComponentKind;
  name: string;
  from: [number, number, number];
  to: [number, number, number];
  target: TargetKind;
  predicate: string; // predicate id
  capture?: string;
  required: boolean;
  // for substructure
  childId?: string;
}

export type TransformKind =
  | "identity"
  | "horizontal_facing"
  | "rotate_y_90"
  | "rotate_y_180"
  | "mirror_x"
  | "mirror_z";

export interface ComposerContextRule {
  id: string;
  name: string;
  expression: string;
}

export interface ComposerState {
  structureId: string;
  components: ComposerComponent[];
  transforms: TransformKind[];
  contextRules: ComposerContextRule[];
  schemaVersion: number;

  // actions
  setStructureId: (id: string) => void;
  toggleTransform: (t: TransformKind) => void;
  addComponent: (c: Partial<ComposerComponent> & { kind: ComponentKind }) => void;
  updateComponent: (id: string, patch: Partial<ComposerComponent>) => void;
  removeComponent: (id: string) => void;
  addContextRule: () => void;
  updateContextRule: (id: string, patch: Partial<ComposerContextRule>) => void;
  removeContextRule: (id: string) => void;
  loadPreset: (presetId: string) => void;
  reset: () => void;
}

// ============================================================
// 预设
// ============================================================

const PRESETS: Record<string, Omit<ComposerState, "setStructureId" | "toggleTransform" | "addComponent" | "updateComponent" | "removeComponent" | "addContextRule" | "updateContextRule" | "removeContextRule" | "loadPreset" | "reset">> = {
  pylon: {
    structureId: "cumulus:pylon",
    components: [
      { id: "c1", kind: "point", name: "controller", from: [0, 0, 0], to: [0, 0, 0], target: "block", predicate: "cumulus:controller", capture: "controller", required: true },
      { id: "c2", kind: "point", name: "cap", from: [0, 1, 0], to: [0, 1, 0], target: "block", predicate: "cumulus:casing", required: true },
    ],
    transforms: ["identity", "horizontal_facing"],
    contextRules: [
      { id: "r1", name: "facing", expression: "controller.facing" },
    ],
    schemaVersion: 1,
  },
  forge: {
    structureId: "cumulus:mini_forge",
    components: [
      { id: "c1", kind: "point", name: "controller", from: [0, 1, 0], to: [0, 1, 0], target: "block", predicate: "cumulus:forge_controller", capture: "controller", required: true },
      { id: "c2", kind: "hollowBox", name: "casing", from: [-1, 0, -1], to: [1, 2, 1], target: "block", predicate: "#cumulus:heat_casing", required: true },
      { id: "c3", kind: "point", name: "energy_in", from: [0, 0, 1], to: [0, 0, 1], target: "capability", predicate: "cumulus:energy", required: false },
      { id: "c4", kind: "point", name: "item_in", from: [-1, 1, 0], to: [-1, 1, 0], target: "block_entity", predicate: "cumulus:item_bus", required: false },
    ],
    transforms: ["identity", "horizontal_facing"],
    contextRules: [
      { id: "r1", name: "facing", expression: "controller.facing" },
    ],
    schemaVersion: 2,
  },
  factory: {
    structureId: "cumulus:factory",
    components: [
      { id: "c1", kind: "point", name: "controller", from: [0, 0, 0], to: [0, 0, 0], target: "block", predicate: "cumulus:factory_controller", capture: "controller", required: true },
      { id: "c2", kind: "hollowBox", name: "shell", from: [-3, 0, -2], to: [3, 3, 2], target: "block", predicate: "#cumulus:factory_casing", required: true },
      { id: "c3", kind: "region", name: "door", from: [-1, 0, 2], to: [1, 2, 2], target: "region", predicate: "cumulus:air_or_door", required: true },
      { id: "c4", kind: "substructure", name: "left_arm", from: [-4, 0, 0], to: [-4, 0, 0], target: "block", predicate: "", required: true, childId: "cumulus:arm" },
      { id: "c5", kind: "substructure", name: "right_arm", from: [4, 0, 0], to: [4, 0, 0], target: "block", predicate: "", required: true, childId: "cumulus:arm" },
    ],
    transforms: ["identity", "horizontal_facing", "mirror_x"],
    contextRules: [
      { id: "r1", name: "facing", expression: "controller.facing" },
      { id: "r2", name: "level", expression: "controller.level" },
    ],
    schemaVersion: 3,
  },
  altar: {
    structureId: "cumulus:ritual_altar",
    components: [
      { id: "c1", kind: "point", name: "altar", from: [0, 0, 0], to: [0, 0, 0], target: "capability", predicate: "blood_magic:blood", capture: "altar", required: true },
      { id: "c2", kind: "hollowBox", name: "runes", from: [-2, 0, -2], to: [2, 0, 2], target: "block", predicate: "#blood_magic:rune", required: true },
      { id: "c3", kind: "point", name: "master", from: [0, 1, 3], to: [0, 1, 3], target: "entity", predicate: "minecraft:player", required: false },
    ],
    transforms: ["identity", "horizontal_facing"],
    contextRules: [],
    schemaVersion: 1,
  },
};

export const COMPOSER_PRESETS = Object.keys(PRESETS);

// ============================================================
// 计算：每个组件发射多少 StructurePoint
// ============================================================

export function pointCountFor(c: ComposerComponent): number {
  switch (c.kind) {
    case "point":
      return 1;
    case "box": {
      const dx = c.to[0] - c.from[0] + 1;
      const dy = c.to[1] - c.from[1] + 1;
      const dz = c.to[2] - c.from[2] + 1;
      return dx * dy * dz;
    }
    case "hollowBox": {
      const dx = c.to[0] - c.from[0] + 1;
      const dy = c.to[1] - c.from[1] + 1;
      const dz = c.to[2] - c.from[2] + 1;
      if (dx <= 2 || dy <= 2 || dz <= 2) return dx * dy * dz - Math.max(0, dx - 2) * Math.max(0, dy - 2) * Math.max(0, dz - 2);
      return dx * dy * dz - (dx - 2) * (dy - 2) * (dz - 2);
    }
    case "region":
      return 1; // 1 region target, not expanded
    case "substructure":
      return 0; // deferred
  }
}

// ============================================================
// footprint section 计算
// ============================================================

export function footprintSections(c: ComposerComponent): number {
  // section = 16×16×16, simplified: count distinct sections in component bbox
  const fx = Math.floor(c.from[0] / 16);
  const fy = Math.floor(c.from[1] / 16);
  const fz = Math.floor(c.from[2] / 16);
  const tx = Math.floor(c.to[0] / 16);
  const ty = Math.floor(c.to[1] / 16);
  const tz = Math.floor(c.to[2] / 16);
  return (tx - fx + 1) * (ty - fy + 1) * (tz - fz + 1);
}

// ============================================================
// 生成 Java DSL
// ============================================================

export function generateDSL(s: ComposerState): string {
  const lines: string[] = [];
  lines.push(`var definition = Structures.define(`);
  lines.push(`    ResourceLocation.parse("${s.structureId}"))`);

  // transforms
  if (s.transforms.length > 0) {
    lines.push(`    .transforms(`);
    s.transforms.forEach((t, i) => {
      const fn =
        t === "identity" ? "Transforms.identity()" :
        t === "horizontal_facing" ? "Transforms.horizontalFacing()" :
        t === "rotate_y_90" ? "Transforms.rotateY90()" :
        t === "rotate_y_180" ? "Transforms.rotateY180()" :
        t === "mirror_x" ? "Transforms.mirrorX()" :
        "Transforms.mirrorZ()";
      lines.push(`        ${fn}${i < s.transforms.length - 1 ? "," : ""}`);
    });
    lines.push(`    )`);
  }

  // components
  for (const c of s.components) {
    if (c.kind === "point") {
      lines.push(`    .point("${c.name}",`);
      lines.push(`        RelativeBlockPos.of(${c.from.join(", ")}),`);
      lines.push(`        ${renderTarget(c)})`);
    } else if (c.kind === "substructure") {
      lines.push(`    .component(Components.substructure("${c.name}",`);
      lines.push(`        ResourceLocation.parse("${c.childId}"),`);
      lines.push(`        RelativeBlockPos.of(${c.from.join(", ")})))`);
    } else {
      const fn = c.kind === "box" ? "box" : c.kind === "hollowBox" ? "hollowBox" : "region";
      lines.push(`    .component(Components.${fn}("${c.name}",`);
      lines.push(`        RelativeBlockPos.of(${c.from.join(", ")}),`);
      lines.push(`        RelativeBlockPos.of(${c.to.join(", ")}))`);
      lines.push(`        ${renderTarget(c)})`);
    }
  }

  // context rules
  for (const r of s.contextRules) {
    lines.push(`    .context(ContextRuleDescriptor.derive(`);
    lines.push(`        "${r.name}", "${r.expression}"))`);
  }

  lines.push(`    .build();`);
  return lines.join("\n");
}

function renderTarget(c: ComposerComponent): string {
  if (c.kind === "region") {
    return `.target(Targets.region(
            RelativeBlockPos.of(${c.from.join(", ")}),
            RelativeBlockPos.of(${c.to.join(", ")})))` +
      (c.predicate ? `\n        .where(PredicateDescriptors.id("${c.predicate}"))` : "");
  }
  if (c.kind === "substructure") return "";

  const targetFn =
    c.target === "block" ? "Targets.block()" :
    c.target === "block_entity" ? "Targets.blockEntity()" :
    c.target === "entity" ? "Targets.entity()" :
    c.target === "fluid" ? "Targets.fluid()" :
    c.target === "capability" ? `Targets.capability("${c.predicate || "cumulus:custom"}")` :
    c.target === "context" ? `Targets.context("${c.predicate || "key"}")` :
    "Targets.block()";

  let s = `.target(${targetFn})`;
  if (c.predicate && c.target !== "capability" && c.target !== "context") {
    const pred = c.predicate.startsWith("#")
      ? `PredicateDescriptors.tag("${c.predicate.slice(1)}")`
      : `PredicateDescriptors.id("${c.predicate}")`;
    s += `\n        .where(${pred})`;
  }
  if (c.capture) s += `\n        .capture("${c.capture}")`;
  if (!c.required) s += `\n        .optional() // OPTIONAL`;
  return s;
}

// ============================================================
// 生成 IR 摘要
// ============================================================

export function generateIRSummary(s: ComposerState): {
  points: number;
  regions: number;
  substructures: number;
  captures: string[];
  candidates: number;
  footprint: number;
  estimatedCheckMs: number;
  estimatedMemoryKb: number;
  issues: { severity: "ERROR" | "WARN"; code: string; msg: string }[];
} {
  let points = 0;
  let regions = 0;
  let substructures = 0;
  let footprint = 0;
  const captures: string[] = [];
  const issues: { severity: "ERROR" | "WARN"; code: string; msg: string }[] = [];

  // 模拟 transform 候选数
  let candidates = 1; // identity always exists
  if (s.transforms.includes("horizontal_facing")) candidates += 3; // n,e,s,w (identity counted)
  if (s.transforms.includes("rotate_y_90")) candidates += 2; // +90, +180 (180 可能重)
  if (s.transforms.includes("rotate_y_180")) candidates += 1;
  if (s.transforms.includes("mirror_x")) candidates += 1;
  if (s.transforms.includes("mirror_z")) candidates += 1;

  // location map for spatial conflict detection
  const locationMap = new Map<string, string>();

  for (const c of s.components) {
    if (c.kind === "substructure") {
      substructures++;
      continue;
    }
    if (c.kind === "region") {
      regions++;
      footprint += footprintSections(c);
      continue;
    }
    const n = pointCountFor(c);
    points += n;

    // capture
    if (c.capture) captures.push(c.capture);

    // footprint
    footprint += footprintSections(c);

    // spatial conflict for required points (simplified)
    if (c.required && c.kind === "point") {
      const key = `${c.from.join(",")}:${c.target}`;
      if (locationMap.has(key)) {
        issues.push({
          severity: "ERROR",
          code: "COMPONENT_CONFLICT",
          msg: `${c.name} conflicts with ${locationMap.get(key)} at (${c.from.join(", ")})`,
        });
      } else {
        locationMap.set(key, c.name);
      }
    }
  }

  // duplicate capture
  const captureSeen = new Set<string>();
  for (const cap of captures) {
    if (captureSeen.has(cap)) {
      issues.push({
        severity: "ERROR",
        code: "CONTEXT_CAPTURE_FAILED",
        msg: `duplicate capture "${cap}"`,
      });
    }
    captureSeen.add(cap);
  }

  // missing controller
  if (!captures.includes("controller") && s.components.length > 0) {
    issues.push({
      severity: "WARN",
      code: "MISSING_TARGET",
      msg: 'no capture named "controller" — context rule "controller.facing" will fail',
    });
  }

  // substructure without childId
  for (const c of s.components) {
    if (c.kind === "substructure" && !c.childId) {
      issues.push({
        severity: "ERROR",
        code: "DEPENDENCY_UNRESOLVED",
        msg: `substructure "${c.name}" has no childId`,
      });
    }
  }

  // estimated check cost (very rough)
  // fastTick: ~0.5µs per point per candidate
  const estimatedCheckMs = (points * candidates * 0.0005);
  const estimatedMemoryKb = (points * candidates * 0.06) + (footprint * 0.02);

  return {
    points,
    regions,
    substructures,
    captures,
    candidates,
    footprint,
    estimatedCheckMs,
    estimatedMemoryKb,
    issues,
  };
}

// ============================================================
// Store
// ============================================================

let componentIdCounter = 100;
let contextIdCounter = 100;

export const useComposer = create<ComposerState>((set, get) => ({
  structureId: PRESETS.forge.structureId,
  components: PRESETS.forge.components.map((c) => ({ ...c })),
  transforms: [...PRESETS.forge.transforms],
  contextRules: PRESETS.forge.contextRules.map((r) => ({ ...r })),
  schemaVersion: PRESETS.forge.schemaVersion,

  setStructureId: (id) => set({ structureId: id }),

  toggleTransform: (t) =>
    set((s) => ({
      transforms: s.transforms.includes(t)
        ? s.transforms.filter((x) => x !== t)
        : [...s.transforms, t],
    })),

  addComponent: (c) =>
    set((s) => ({
      components: [
        ...s.components,
        {
          id: `c${componentIdCounter++}`,
          name: c.name ?? "new_point",
          from: c.from ?? [0, 0, 0],
          to: c.to ?? [0, 0, 0],
          target: c.target ?? "block",
          predicate: c.predicate ?? "",
          capture: c.capture,
          required: c.required ?? true,
          kind: c.kind,
          childId: c.childId,
        },
      ],
    })),

  updateComponent: (id, patch) =>
    set((s) => ({
      components: s.components.map((c) =>
        c.id === id ? { ...c, ...patch } : c
      ),
    })),

  removeComponent: (id) =>
    set((s) => ({
      components: s.components.filter((c) => c.id !== id),
    })),

  addContextRule: () =>
    set((s) => ({
      contextRules: [
        ...s.contextRules,
        {
          id: `r${contextIdCounter++}`,
          name: "new_rule",
          expression: "controller.facing",
        },
      ],
    })),

  updateContextRule: (id, patch) =>
    set((s) => ({
      contextRules: s.contextRules.map((r) =>
        r.id === id ? { ...r, ...patch } : r
      ),
    })),

  removeContextRule: (id) =>
    set((s) => ({
      contextRules: s.contextRules.filter((r) => r.id !== id),
    })),

  loadPreset: (id) => {
    const p = PRESETS[id];
    if (!p) return;
    set({
      structureId: p.structureId,
      components: p.components.map((c) => ({ ...c })),
      transforms: [...p.transforms],
      contextRules: p.contextRules.map((r) => ({ ...r })),
      schemaVersion: p.schemaVersion,
    });
  },

  reset: () =>
    set({
      structureId: "cumulus:new_structure",
      components: [],
      transforms: ["identity"],
      contextRules: [],
      schemaVersion: 1,
    }),
}));
