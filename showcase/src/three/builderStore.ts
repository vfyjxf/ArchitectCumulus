import { create } from "zustand";

export type BlockKind =
  | "controller"
  | "casing"
  | "input_bus"
  | "energy_port"
  | "fluid_port"
  | "air"
  | "marker";

export interface PaletteEntry {
  kind: BlockKind;
  label: string;
  color: string;
  role: string;
  predicate: string;
}

export const PALETTE: PaletteEntry[] = [
  {
    kind: "controller",
    label: "Controller",
    color: "#7c5cff",
    role: "point",
    predicate: 'PredicateDescriptors.id("cumulus:controller")',
  },
  {
    kind: "casing",
    label: "Heat Casing",
    color: "#fb7185",
    role: "block",
    predicate: 'PredicateDescriptors.tag("cumulus:heat_casing")',
  },
  {
    kind: "input_bus",
    label: "Item Bus",
    color: "#22d3ee",
    role: "block_entity",
    predicate: 'PredicateDescriptors.id("cumulus:item_bus")',
  },
  {
    kind: "energy_port",
    label: "Energy Port",
    color: "#fbbf24",
    role: "capability",
    predicate: 'capability("cumulus:energy")',
  },
  {
    kind: "fluid_port",
    label: "Fluid Port",
    color: "#a3e635",
    role: "fluid",
    predicate: 'Targets.fluid()',
  },
];

export interface PlacedBlock {
  x: number;
  y: number;
  z: number;
  kind: BlockKind;
}

export interface DiagnosticIssue {
  severity: "error" | "warn" | "info";
  code: string;
  point: string;
  location: [number, number, number];
  message: string;
  fix?: string;
}

interface BuilderState {
  blocks: PlacedBlock[];
  selectedKind: BlockKind;
  diagnostics: DiagnosticIssue[];
  status: "idle" | "valid" | "invalid" | "pending";
  ghost: boolean;
  preset: string;
  setKind: (k: BlockKind) => void;
  place: (x: number, y: number, z: number) => void;
  remove: (x: number, y: number, z: number) => void;
  clear: () => void;
  toggleGhost: () => void;
  loadPreset: (id: string) => void;
  validate: () => void;
}

// a "valid" sample structure: 3x3 base hollowBox shell + controller on top + 4 ports
const PRESETS: Record<string, PlacedBlock[]> = {
  "Mini Forge": [
    // base ring 3x3 (level 0)
    { x: -1, y: 0, z: -1, kind: "casing" },
    { x: 0, y: 0, z: -1, kind: "casing" },
    { x: 1, y: 0, z: -1, kind: "casing" },
    { x: -1, y: 0, z: 0, kind: "casing" },
    { x: 1, y: 0, z: 0, kind: "casing" },
    { x: -1, y: 0, z: 1, kind: "casing" },
    { x: 0, y: 0, z: 1, kind: "casing" },
    { x: 1, y: 0, z: 1, kind: "casing" },
    // mid level (1) - 4 ports on sides
    { x: -1, y: 1, z: -1, kind: "casing" },
    { x: 1, y: 1, z: -1, kind: "casing" },
    { x: -1, y: 1, z: 1, kind: "casing" },
    { x: 1, y: 1, z: 1, kind: "casing" },
    { x: 0, y: 1, z: -1, kind: "input_bus" },
    { x: 0, y: 1, z: 1, kind: "energy_port" },
    { x: -1, y: 1, z: 0, kind: "fluid_port" },
    // top level (2) ring
    { x: -1, y: 2, z: -1, kind: "casing" },
    { x: 0, y: 2, z: -1, kind: "casing" },
    { x: 1, y: 2, z: -1, kind: "casing" },
    { x: -1, y: 2, z: 0, kind: "casing" },
    { x: 1, y: 2, z: 0, kind: "casing" },
    { x: -1, y: 2, z: 1, kind: "casing" },
    { x: 0, y: 2, z: 1, kind: "casing" },
    { x: 1, y: 2, z: 1, kind: "casing" },
    // controller on top
    { x: 0, y: 3, z: 0, kind: "controller" },
  ],
  "Pylon": [
    { x: 0, y: 0, z: 0, kind: "controller" },
    { x: 0, y: 1, z: 0, kind: "energy_port" },
    { x: 1, y: 1, z: 0, kind: "casing" },
    { x: -1, y: 1, z: 0, kind: "casing" },
    { x: 0, y: 1, z: 1, kind: "casing" },
    { x: 0, y: 1, z: -1, kind: "casing" },
  ],
  "Conduit": [
    // 1x1x5 line of casing + controller + ports
    { x: 0, y: 0, z: -2, kind: "fluid_port" },
    { x: 0, y: 0, z: -1, kind: "casing" },
    { x: 0, y: 0, z: 0, kind: "controller" },
    { x: 0, y: 0, z: 1, kind: "casing" },
    { x: 0, y: 0, z: 2, kind: "energy_port" },
  ],
};

function runValidation(blocks: PlacedBlock[]): {
  diagnostics: DiagnosticIssue[];
  status: "valid" | "invalid" | "pending";
} {
  const diags: DiagnosticIssue[] = [];

  if (blocks.length === 0) return { diagnostics: [], status: "idle" as never };

  const byKey = new Map<string, PlacedBlock>();
  for (const b of blocks) byKey.set(`${b.x},${b.y},${b.z}`, b);

  // 1. must have exactly 1 controller
  const controllers = blocks.filter((b) => b.kind === "controller");
  if (controllers.length === 0) {
    diags.push({
      severity: "error",
      code: "missingTarget",
      point: "controller",
      location: [0, 0, 0],
      message: "no controller found anywhere; structure is anonymous",
      fix: "place a Controller block at any position",
    });
  } else if (controllers.length > 1) {
    for (let i = 1; i < controllers.length; i++) {
      diags.push({
        severity: "error",
        code: "componentConflict",
        point: `controller#${i}`,
        location: [controllers[i].x, controllers[i].y, controllers[i].z],
        message: "duplicate controller; only one is allowed",
        fix: "remove the extra controller",
      });
    }
  }

  // 2. controller must have at least 3 casing neighbors (a frame)
  if (controllers.length === 1) {
    const c = controllers[0];
    const neighbors = [
      [1, 0, 0],
      [-1, 0, 0],
      [0, 1, 0],
      [0, -1, 0],
      [0, 0, 1],
      [0, 0, -1],
    ];
    const casingNbrs = neighbors.filter(([dx, dy, dz]) => {
      const k = `${c.x + dx},${c.y + dy},${c.z + dz}`;
      const b = byKey.get(k);
      return b && b.kind === "casing";
    }).length;
    if (casingNbrs < 2) {
      diags.push({
        severity: "warn",
        code: "predicateFailed",
        point: "controller",
        location: [c.x, c.y, c.z],
        message: `controller only has ${casingNbrs} casing neighbor(s); a frame needs ≥2`,
        fix: "place Heat Casing blocks adjacent to the controller",
      });
    }
  }

  // 3. require at least one energy port
  if (!blocks.some((b) => b.kind === "energy_port")) {
    diags.push({
      severity: "warn",
      code: "missingTarget",
      point: "energy_in",
      location: [0, 0, 1],
      message: "no Energy Port — engine cannot receive power",
      fix: "place an Energy Port adjacent to casing",
    });
  }

  // 4. ports must be adjacent to casing or controller
  for (const b of blocks) {
    if (
      b.kind === "input_bus" ||
      b.kind === "energy_port" ||
      b.kind === "fluid_port"
    ) {
      const neighbors = [
        [1, 0, 0],
        [-1, 0, 0],
        [0, 1, 0],
        [0, -1, 0],
        [0, 0, 1],
        [0, 0, -1],
      ];
      const hasCasing = neighbors.some(([dx, dy, dz]) => {
        const k = `${b.x + dx},${b.y + dy},${b.z + dz}`;
        const nb = byKey.get(k);
        return nb && (nb.kind === "casing" || nb.kind === "controller");
      });
      if (!hasCasing) {
        diags.push({
          severity: "error",
          code: "predicateFailed",
          point: `${b.kind}`,
          location: [b.x, b.y, b.z],
          message: `${b.kind} is floating — must touch casing or controller`,
          fix: "move it next to a casing block",
        });
      }
    }
  }

  // 5. casing must form a connected shape
  if (controllers.length === 1) {
    const c = controllers[0];
    const visited = new Set<string>();
    const queue: PlacedBlock[] = [c];
    while (queue.length) {
      const b = queue.shift()!;
      const k = `${b.x},${b.y},${b.z}`;
      if (visited.has(k)) continue;
      visited.add(k);
      for (const [dx, dy, dz] of [
        [1, 0, 0],
        [-1, 0, 0],
        [0, 1, 0],
        [0, -1, 0],
        [0, 0, 1],
        [0, 0, -1],
      ]) {
        const nk = `${b.x + dx},${b.y + dy},${b.z + dz}`;
        const nb = byKey.get(nk);
        if (nb && !visited.has(nk)) queue.push(nb);
      }
    }
    if (visited.size !== blocks.length) {
      diags.push({
        severity: "error",
        code: "componentConflict",
        point: "graph",
        location: [0, 0, 0],
        message: `structure is disconnected (${blocks.length - visited.size} orphan blocks)`,
        fix: "ensure all blocks are reachable from the controller",
      });
    }
  }

  const status =
    diags.length === 0
      ? "valid"
      : diags.some((d) => d.severity === "error")
      ? "invalid"
      : "valid";
  return { diagnostics: diags, status };
}

export const useBuilder = create<BuilderState>((set, get) => ({
  blocks: PRESETS["Mini Forge"].map((b) => ({ ...b })),
  selectedKind: "casing",
  diagnostics: [],
  status: "idle",
  ghost: false,
  preset: "Mini Forge",
  setKind: (k) => set({ selectedKind: k }),
  place: (x, y, z) => {
    const kind = get().selectedKind;
    const blocks = [...get().blocks];
    const idx = blocks.findIndex((b) => b.x === x && b.y === y && b.z === z);
    if (idx >= 0) {
      if (blocks[idx].kind === kind) {
        // same kind, remove
        blocks.splice(idx, 1);
      } else {
        blocks[idx] = { x, y, z, kind };
      }
    } else {
      blocks.push({ x, y, z, kind });
    }
    set({ blocks, diagnostics: [], status: "idle" });
  },
  remove: (x, y, z) => {
    const blocks = get().blocks.filter(
      (b) => !(b.x === x && b.y === y && b.z === z)
    );
    set({ blocks, diagnostics: [], status: "idle" });
  },
  clear: () => set({ blocks: [], diagnostics: [], status: "idle" }),
  toggleGhost: () => set((s) => ({ ghost: !s.ghost })),
  loadPreset: (id) =>
    set({
      blocks: PRESETS[id] ? PRESETS[id].map((b) => ({ ...b })) : [],
      preset: id,
      diagnostics: [],
      status: "idle",
    }),
  validate: () => {
    const { blocks } = get();
    const r = runValidation(blocks);
    set({ diagnostics: r.diagnostics, status: r.status });
  },
}));

export const PRESET_NAMES = Object.keys(PRESETS);

// helper to generate Java DSL (author-facing source) from current blocks
export function generateDSL(blocks: PlacedBlock[]): string {
  if (blocks.length === 0)
    return "// 放置一些方块以生成 Java DSL";

  const byKind = new Map<BlockKind, PlacedBlock[]>();
  for (const b of blocks) {
    if (!byKind.has(b.kind)) byKind.set(b.kind, []);
    byKind.get(b.kind)!.push(b);
  }

  const lines: string[] = [];
  lines.push("var definition = Structures.define(");
  lines.push('    ResourceLocation.parse("cumulus:showcase_machine"))');
  lines.push("    .transforms(");
  lines.push("        Transforms.identity(),");
  lines.push("        Transforms.horizontalFacing())");

  // controller as named point
  const ctrl = byKind.get("controller")?.[0];
  if (ctrl) {
    lines.push(`    .point("controller",`);
    lines.push(
      `        RelativeBlockPos.of(${ctrl.x}, ${ctrl.y}, ${ctrl.z}), Targets.block()`
    );
    lines.push(
      `        .where(PredicateDescriptors.id("cumulus:controller"))`
    );
    lines.push(`        .capture("controller"))`);
  }

  // other named ports
  const ports: { kind: BlockKind; name: string; target: string }[] = [
    { kind: "input_bus", name: "input_bus", target: "Targets.blockEntity()" },
    { kind: "energy_port", name: "energy_in", target: 'Targets.capability("cumulus:energy")' },
    { kind: "fluid_port", name: "fluid_in", target: "Targets.fluid()" },
  ];
  for (const p of ports) {
    const arr = byKind.get(p.kind) ?? [];
    arr.forEach((b, i) => {
      const name = arr.length === 1 ? p.name : `${p.name}_${i}`;
      lines.push(`    .point("${name}",`);
      lines.push(
        `        RelativeBlockPos.of(${b.x}, ${b.y}, ${b.z}), ${p.target})`
      );
    });
  }

  // casing as a filled box (approximate bounding box)
  const casings = byKind.get("casing") ?? [];
  if (casings.length > 0) {
    const xs = casings.map((c) => c.x);
    const ys = casings.map((c) => c.y);
    const zs = casings.map((c) => c.z);
    const fx = Math.min(...xs);
    const fy = Math.min(...ys);
    const fz = Math.min(...zs);
    const tx = Math.max(...xs);
    const ty = Math.max(...ys);
    const tz = Math.max(...zs);
    const total = (tx - fx + 1) * (ty - fy + 1) * (tz - fz + 1);
    const isHollow = casings.length < total;
    lines.push(
      `    .component(Components.${isHollow ? "hollowBox" : "box"}("casing",`
    );
    lines.push(
      `        RelativeBlockPos.of(${fx}, ${fy}, ${fz}),`
    );
    lines.push(
      `        RelativeBlockPos.of(${tx}, ${ty}, ${tz}))`
    );
    lines.push(`        .target(Targets.block())`);
    lines.push(
      `        .where(PredicateDescriptors.tag("cumulus:heat_casing")))`
    );
  }

  if (ctrl) {
    lines.push(
      `    .context(ContextRuleDescriptor.derive(
        ContextKey.string("facing"),
        ContextSources.capture("controller").property("facing")))`
    );
  }
  lines.push(`    .build();`);

  return lines.join("\n");
}

// ============================================================
// IR dump: 展示 build() 之后真正构造出的 StructureDefinition record
// 这是所有下游消费者（compile / datagen / test）看到的真源
// ============================================================

interface IRPoint {
  id: string;
  location: [number, number, number];
  target: { type: string; props: Record<string, string> };
  predicates: { id: string; props?: Record<string, string> }[];
  requirement: "required" | "optional";
  captures: string[];
  groups: string[];
}

function blocksToIRPoints(blocks: PlacedBlock[]): IRPoint[] {
  const out: IRPoint[] = [];
  for (const b of blocks) {
    const target: IRPoint["target"] =
      b.kind === "energy_port"
        ? { type: "cumulus:capability", props: { name: "cumulus:energy" } }
        : b.kind === "fluid_port"
        ? { type: "cumulus:fluid", props: {} }
        : b.kind === "input_bus"
        ? { type: "cumulus:block_entity", props: {} }
        : { type: "cumulus:block", props: {} };

    const predId =
      b.kind === "casing"
        ? "#cumulus:heat_casing"
        : b.kind === "controller"
        ? "cumulus:controller"
        : b.kind === "input_bus"
        ? "cumulus:item_bus"
        : "cumulus:any";

    out.push({
      id:
        b.kind === "controller"
          ? "controller"
          : b.kind === "input_bus"
          ? "input_bus"
          : b.kind === "energy_port"
          ? "energy_in"
          : b.kind === "fluid_port"
          ? "fluid_in"
          : `casing_${b.x}_${b.y}_${b.z}`,
      location: [b.x, b.y, b.z],
      target,
      predicates: [{ id: predId }],
      requirement: "required",
      captures: b.kind === "controller" ? ["controller"] : [],
      groups: [],
    });
  }
  // controller 第一；其余按 location 排序
  out.sort((a, b) => {
    if (a.id === "controller") return -1;
    if (b.id === "controller") return 1;
    if (a.location[1] !== b.location[1]) return a.location[1] - b.location[1];
    if (a.location[0] !== b.location[0]) return a.location[0] - b.location[0];
    return a.location[2] - b.location[2];
  });
  return out;
}

export function generateIR(blocks: PlacedBlock[]): string {
  if (blocks.length === 0)
    return "// 放置方块后，build() 会构造下面的不可变 record";

  const points = blocksToIRPoints(blocks);
  const hasController = points.some((p) => p.id === "controller");
  const lines: string[] = [];

  lines.push(`StructureDefinition {`);
  lines.push(`  // ── 不可变 record · build() 之后冻结 ──`);
  lines.push(`  id            = cumulus:showcase_machine`);
  lines.push(`  schemaVersion = 2                              // irSchemaVersion`);
  lines.push(`  transforms    = List(${points.length > 0 ? "2" : "0"} TransformDescriptor)`);
  lines.push(`    [0] TransformDescriptor {`);
  lines.push(`        typeId = cumulus:identity`);
  lines.push(`        properties = {}`);
  lines.push(`    }`);
  lines.push(`    [1] TransformDescriptor {`);
  lines.push(`        typeId = cumulus:horizontal_facing`);
  lines.push(`        properties = {}`);
  lines.push(`    }`);
  lines.push(``);
  lines.push(`  points = List(${points.length} StructurePointDefinition) {`);

  points.forEach((p, i) => {
    const idx = `[${i.toString().padStart(2, "0")}]`;
    lines.push(`    ${idx} StructurePointDefinition {`);
    lines.push(`        id          = StructurePointId("${p.id}")`);
    lines.push(`        location    = RelativeBlockPos(${p.location.join(", ")})`);
    lines.push(`        target      = TargetDescriptor {`);
    lines.push(`            typeId      = ${p.target.type}`);
    const propKeys = Object.keys(p.target.props);
    if (propKeys.length === 0) {
      lines.push(`            properties  = {}`);
    } else {
      lines.push(`            properties  = {`);
      for (const k of propKeys) {
        lines.push(`                "${k}" = "${p.target.props[k]}",`);
      }
      lines.push(`            }`);
    }
    lines.push(`        }`);
    if (p.predicates.length === 1) {
      const pred = p.predicates[0];
      lines.push(`        predicates  = List(1 PredicateDescriptor) {`);
      lines.push(`            [0] PredicateDescriptor {`);
      lines.push(`                typeId      = ${pred.id}`);
      lines.push(`                properties  = {}`);
      lines.push(`            }`);
      lines.push(`        }`);
    } else {
      lines.push(`        predicates  = List(${p.predicates.length})`);
    }
    lines.push(`        requirement = ${p.requirement}`);
    if (p.captures.length === 0) {
      lines.push(`        captures    = Set()`);
    } else {
      const capsContent = p.captures.map((c) => '"' + c + '"').join(", ");
      lines.push(`        captures    = Set({ ${capsContent} })`);
    }
    lines.push(`        groups      = Set()`);
    lines.push(`        diagnostics = DiagnosticMetadata.empty`);
    lines.push(`    }`);
    if (i < points.length - 1) lines.push(``);
  });

  lines.push(`  }`);
  lines.push(``);
  if (hasController) {
    lines.push(`  contextRules = List(1 ContextRuleDescriptor) {`);
    lines.push(`    [0] ContextRuleDescriptor {`);
    lines.push(`        name       = "facing"`);
    lines.push(`        source    = capture("controller").property("facing")`);
    lines.push(`    }`);
    lines.push(`  }`);
  } else {
    lines.push(`  contextRules = List(0)`);
  }
  lines.push(`  diagnostics  = DiagnosticMetadata.empty`);
  lines.push(`}`);
  lines.push(``);
  lines.push(`// record 是 final + immutable：`);
  lines.push(`//   - List/Map/Set 字段全部 List.copyOf / Map.copyOf / Set.copyOf`);
  lines.push(`//   - 任何消费者 (compiler/datagen/test) 拿到的是同一份字节`);
  lines.push(`//   - 可以安全作为 HashMap key 用作 cache`);

  return lines.join("\n");
}

// ============================================================
// Check Plan dump: 编译产物 — 每个 transform 候选的 precomputed bounds
// ============================================================

export function generateCheckPlan(blocks: PlacedBlock[]): string {
  if (blocks.length === 0)
    return "// StructureCheckPlan.compile(definition) 在 IR 之后生成";

  const points = blocksToIRPoints(blocks);
  const lines: string[] = [];

  // 计算 footprint sections (simplified - all in section (0,0,0) unless blocks span sections)
  const allX = points.map((p) => p.location[0]);
  const allY = points.map((p) => p.location[1]);
  const allZ = points.map((p) => p.location[2]);
  const sectionMin = [
    Math.floor(Math.min(...allX) / 16),
    Math.floor(Math.min(...allY) / 16),
    Math.floor(Math.min(...allZ) / 16),
  ];
  const sectionMax = [
    Math.floor(Math.max(...allX) / 16),
    Math.floor(Math.max(...allY) / 16),
    Math.floor(Math.max(...allZ) / 16),
  ];
  const sectionCount =
    (sectionMax[0] - sectionMin[0] + 1) *
    (sectionMax[1] - sectionMin[1] + 1) *
    (sectionMax[2] - sectionMin[2] + 1);

  // 估算 fastTick cost
  const candidates = 4; // identity + horizontalFacing (4 dirs)
  const fastTickMs = (points.length * candidates * 0.0005).toFixed(3);
  const memoryKb = (points.length * candidates * 0.06 + sectionCount * 0.02).toFixed(2);

  lines.push(`StructureCheckPlan {`);
  lines.push(`  // ── compile 时一次性预算，运行时 hot path 仅 O(候选数) lookup ──`);
  lines.push(`  source    = cumulus:showcase_machine  (schemaVersion 2)`);
  lines.push(`  candidates = List(${candidates} CheckPlanCandidate)`);
  lines.push(``);
  lines.push(`  candidate[0] = identity {`);
  lines.push(`      transform  = TransformDescriptor(cumulus:identity, {})`);
  lines.push(`      pointBounds = List(${points.length} PointBound)`);

  // 显示前 3 个 PointBound 作为示例
  points.slice(0, 3).forEach((p, i) => {
    lines.push(`        [${i}] PointBound {`);
    lines.push(`              pointId    = "${p.id}"`);
    lines.push(`              worldPos   = (${p.location.join(", ")})    // 未旋转`);
    lines.push(`              targetType = ${p.target.type}`);
    lines.push(`              predicates = [${p.predicates.map((pr) => pr.id).join(", ")}]`);
    lines.push(`            }`);
  });
  if (points.length > 3) {
    lines.push(`        ... 还有 ${points.length - 3} 个 PointBound`);
  }
  lines.push(`      footprint  = Set(${sectionCount} SectionKey) {`);
  lines.push(`          (${sectionMin.join(", ")})`);
  if (sectionCount > 1) {
    lines.push(`          ... 还有 ${sectionCount - 1} 个 section`);
  }
  lines.push(`      }`);
  lines.push(`  }`);
  lines.push(``);
  lines.push(`  candidate[1] = horizontal_facing · NORTH {`);
  lines.push(`      transform  = TransformDescriptor(cumulus:horizontal_facing, {dir=NORTH})`);
  lines.push(`      pointBounds = List(${points.length} PointBound)    // 全部预算过`);
  lines.push(`      footprint  = Set(${sectionCount} SectionKey)`);
  lines.push(`  }`);
  lines.push(`  candidate[2] = horizontal_facing · EAST { ... }`);
  lines.push(`  candidate[3] = horizontal_facing · SOUTH { ... }`);
  lines.push(``);
  lines.push(`  // ── fastTick 执行语义 ──`);
  lines.push(`  //   for (var c : candidates) {`);
  lines.push(`  //     if (allBoundsMatch(c, snapshot)) return VALID;  // ← 短路`);
  lines.push(`  //   }`);
  lines.push(`  //   return INVALID;`);
  lines.push(``);
  lines.push(`  // 第一个通过候选短路：剩余 ${candidates - 1} 个 candidate 的 PointBound 全部跳过`);
  lines.push(`  estimated fastTick = ~${fastTickMs} ms`);
  lines.push(`  estimated memory  = ~${memoryKb} KB`);
  lines.push(`}`);

  return lines.join("\n");
}
