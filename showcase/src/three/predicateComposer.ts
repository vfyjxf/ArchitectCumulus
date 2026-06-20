import { create } from "zustand";

// ============================================================
// Predicate Composer: 拖拽/选择谓词，针对一个 sample block 求值
// ============================================================

export type PredicateType =
  | "id"
  | "tag"
  | "state_property"
  | "block_entity_type"
  | "block_entity_nbt"
  | "capability_present"
  | "capability_amount"
  | "fluid_id"
  | "entity_type"
  | "context_equals";

export interface PredicateDef {
  type: PredicateType;
  label: string;
  description: string;
  fields: { key: string; label: string; placeholder: string }[];
  // 在 UI 上显示的 icon 颜色
  color: string;
}

export const PREDICATE_LIBRARY: PredicateDef[] = [
  {
    type: "id",
    label: "id",
    description: "Block id 精确匹配",
    fields: [{ key: "id", label: "block id", placeholder: "cumulus:forge_controller" }],
    color: "#7c5cff",
  },
  {
    type: "tag",
    label: "tag",
    description: "Block 在某个 tag 里",
    fields: [{ key: "tag", label: "tag id", placeholder: "#cumulus:heat_casing" }],
    color: "#22d3ee",
  },
  {
    type: "state_property",
    label: "state_property",
    description: "BlockState 属性匹配",
    fields: [
      { key: "property", label: "property name", placeholder: "facing" },
      { key: "value", label: "expected value", placeholder: "north" },
    ],
    color: "#f0abfc",
  },
  {
    type: "block_entity_type",
    label: "block_entity_type",
    description: "BlockEntity 类型匹配",
    fields: [{ key: "type", label: "be type id", placeholder: "cumulus:item_bus" }],
    color: "#fbbf24",
  },
  {
    type: "block_entity_nbt",
    label: "block_entity_nbt",
    description: "BlockEntity NBT 路径匹配",
    fields: [
      { key: "path", label: "nbt path", placeholder: "Items[0].id" },
      { key: "value", label: "expected", placeholder: "minecraft:iron_ingot" },
    ],
    color: "#a3e635",
  },
  {
    type: "capability_present",
    label: "capability_present",
    description: "BlockCapability 存在",
    fields: [{ key: "capability", label: "capability name", placeholder: "cumulus:energy" }],
    color: "#fb923c",
  },
  {
    type: "capability_amount",
    label: "capability_amount",
    description: "Capability 数值范围（>=）",
    fields: [
      { key: "capability", label: "capability name", placeholder: "cumulus:energy" },
      { key: "amount", label: "min amount", placeholder: "8192" },
    ],
    color: "#fb7185",
  },
  {
    type: "fluid_id",
    label: "fluid_id",
    description: "Fluid id 匹配",
    fields: [{ key: "fluid", label: "fluid id", placeholder: "minecraft:lava" }],
    color: "#06b6d4",
  },
];

export interface ChainPredicate {
  id: string;
  type: PredicateType;
  fields: Record<string, string>;
  // optional or required (filter chain)
  required: boolean;
}

// sample block state
export interface SampleBlock {
  blockId: string;
  tags: string[];
  stateProperties: Record<string, string>;
  blockEntityType: string;
  blockEntityNbt: string;
  capabilities: { name: string; amount: number }[];
  fluidId: string;
}

interface State {
  chain: ChainPredicate[];
  sample: SampleBlock;
  addPredicate: (type: PredicateType) => void;
  updatePredicate: (id: string, patch: Partial<ChainPredicate>) => void;
  removePredicate: (id: string) => void;
  movePredicate: (id: string, dir: -1 | 1) => void;
  setSample: (patch: Partial<SampleBlock>) => void;
  loadSamplePreset: (id: string) => void;
}

const SAMPLE_PRESETS: Record<string, SampleBlock> = {
  controller: {
    blockId: "cumulus:forge_controller",
    tags: ["cumulus:controller", "minecraft:mineable/pickaxe"],
    stateProperties: { facing: "north", active: "true" },
    blockEntityType: "",
    blockEntityNbt: "{}",
    capabilities: [{ name: "cumulus:energy", amount: 16384 }],
    fluidId: "",
  },
  casing: {
    blockId: "cumulus:heat_casing",
    tags: ["cumulus:heat_casing", "cumulus:heat_resistant"],
    stateProperties: { axis: "y" },
    blockEntityType: "",
    blockEntityNbt: "{}",
    capabilities: [],
    fluidId: "",
  },
  bus: {
    blockId: "cumulus:item_bus",
    tags: ["cumulus:item_container"],
    stateProperties: { facing: "north" },
    blockEntityType: "cumulus:item_bus",
    blockEntityNbt: '{"Items":[{"Slot":0,"Item":{"id":"minecraft:iron_ingot"}}]}',
    capabilities: [{ name: "cumulus:item", amount: 64 }],
    fluidId: "",
  },
  energy: {
    blockId: "cumulus:energy_port",
    tags: ["cumulus:energy_port"],
    stateProperties: { facing: "south" },
    blockEntityType: "",
    blockEntityNbt: "{}",
    capabilities: [
      { name: "cumulus:energy", amount: 8192 },
      { name: "cumulus:item", amount: 0 },
    ],
    fluidId: "",
  },
  empty: {
    blockId: "minecraft:stone",
    tags: ["minecraft:mineable/pickaxe"],
    stateProperties: {},
    blockEntityType: "",
    blockEntityNbt: "{}",
    capabilities: [],
    fluidId: "",
  },
};

let chainIdCounter = 100;
export const SAMPLE_PRESET_NAMES = Object.keys(SAMPLE_PRESETS);

export const usePredicateComposer = create<State>((set, get) => ({
  chain: [
    { id: "p1", type: "tag", fields: { tag: "#cumulus:heat_casing" }, required: true },
  ],
  sample: SAMPLE_PRESETS.controller,
  addPredicate: (type) => {
    const def = PREDICATE_LIBRARY.find((p) => p.type === type)!;
    set((s) => ({
      chain: [
        ...s.chain,
        {
          id: `p${chainIdCounter++}`,
          type,
          fields: {},
          required: true,
        },
      ],
    }));
  },
  updatePredicate: (id, patch) =>
    set((s) => ({
      chain: s.chain.map((p) => (p.id === id ? { ...p, ...patch } : p)),
    })),
  removePredicate: (id) =>
    set((s) => ({ chain: s.chain.filter((p) => p.id !== id) })),
  movePredicate: (id, dir) =>
    set((s) => {
      const idx = s.chain.findIndex((p) => p.id === id);
      if (idx < 0) return {};
      const newIdx = idx + dir;
      if (newIdx < 0 || newIdx >= s.chain.length) return {};
      const newChain = [...s.chain];
      [newChain[idx], newChain[newIdx]] = [newChain[newIdx], newChain[idx]];
      return { chain: newChain };
    }),
  setSample: (patch) => set((s) => ({ sample: { ...s.sample, ...patch } })),
  loadSamplePreset: (id) => set({ sample: SAMPLE_PRESETS[id] }),
}));

// ============================================================
// 求值
// ============================================================

export interface EvalResult {
  predicateId: string;
  passed: boolean;
  skipped: boolean; // optional + failed → skipped (not contributing to fail)
  reason: string;
  matched?: string;
  expected?: string;
}

export function evaluateChain(
  chain: ChainPredicate[],
  sample: SampleBlock
): { results: EvalResult[]; finalPassed: boolean; shortCircuitedAt?: number } {
  const results: EvalResult[] = [];
  let finalPassed = true;
  let shortCircuitedAt: number | undefined;

  for (let i = 0; i < chain.length; i++) {
    const p = chain[i];
    const r = evaluateOne(p, sample);
    results.push(r);

    if (!r.passed) {
      if (p.required) {
        finalPassed = false;
        shortCircuitedAt = i;
        break; // 短路：required fail → 立刻 fail
      }
      // optional fail → 改为 skipped
      r.skipped = true;
    }
  }

  // 后续 predicate 不再 evaluate（已被短路）
  for (let i = (shortCircuitedAt ?? chain.length) + 1; i < chain.length; i++) {
    results.push({
      predicateId: chain[i].id,
      passed: false,
      skipped: true,
      reason: "short-circuited (前序 required fail)",
    });
  }

  return { results, finalPassed, shortCircuitedAt };
}

function evaluateOne(p: ChainPredicate, s: SampleBlock): EvalResult {
  switch (p.type) {
    case "id": {
      const expected = p.fields.id ?? "";
      const passed = !!expected && s.blockId === expected;
      return {
        predicateId: p.id,
        passed,
        skipped: false,
        reason: passed ? "block id 匹配" : "block id 不匹配",
        matched: s.blockId,
        expected,
      };
    }
    case "tag": {
      const raw = p.fields.tag ?? "";
      const tag = raw.startsWith("#") ? raw.slice(1) : raw;
      const passed = !!tag && s.tags.includes(tag);
      return {
        predicateId: p.id,
        passed,
        skipped: false,
        reason: passed ? `block 在 #${tag} 中` : `block 不在 #${tag} 中`,
        matched: s.tags.join(", "),
        expected: `#${tag}`,
      };
    }
    case "state_property": {
      const prop = p.fields.property ?? "";
      const val = p.fields.value ?? "";
      const actual = s.stateProperties[prop];
      const passed = !!prop && actual === val;
      return {
        predicateId: p.id,
        passed,
        skipped: false,
        reason: passed
          ? `state[${prop}] = ${val}`
          : actual === undefined
          ? `state 没有 ${prop} 属性`
          : `state[${prop}] = ${actual} ≠ ${val}`,
        matched: actual ?? "(undefined)",
        expected: val,
      };
    }
    case "block_entity_type": {
      const expected = p.fields.type ?? "";
      const passed = !!expected && s.blockEntityType === expected;
      return {
        predicateId: p.id,
        passed,
        skipped: false,
        reason: passed
          ? "BlockEntity 类型匹配"
          : s.blockEntityType === ""
          ? "block 没有 BlockEntity"
          : `实际类型 ${s.blockEntityType}`,
        matched: s.blockEntityType || "(none)",
        expected,
      };
    }
    case "block_entity_nbt": {
      const path = p.fields.path ?? "";
      const expected = p.fields.value ?? "";
      try {
        const nbt = JSON.parse(s.blockEntityNbt || "{}");
        const parts = path.split(/[.[\]]+/).filter(Boolean);
        let cur: any = nbt;
        for (const part of parts) {
          if (cur == null) { cur = undefined; break; }
          cur = cur[part];
        }
        const actual = cur === undefined ? undefined : JSON.stringify(cur);
        const passed = actual !== undefined && actual.includes(expected);
        return {
          predicateId: p.id,
          passed,
          skipped: false,
          reason: passed
            ? `NBT[${path}] 包含 ${expected}`
            : actual === undefined
            ? `NBT 路径 ${path} 不存在`
            : `NBT[${path}] = ${actual}`,
          matched: actual ?? "(undefined)",
          expected,
        };
      } catch (e) {
        return {
          predicateId: p.id,
          passed: false,
          skipped: false,
          reason: "sample NBT 不是合法 JSON",
        };
      }
    }
    case "capability_present": {
      const cap = p.fields.capability ?? "";
      const found = s.capabilities.find((c) => c.name === cap);
      const passed = !!cap && !!found;
      return {
        predicateId: p.id,
        passed,
        skipped: false,
        reason: passed
          ? `暴露 capability ${cap}`
          : `未暴露 capability ${cap}`,
        matched: found ? `${found.name} (${found.amount})` : "(none)",
        expected: cap,
      };
    }
    case "capability_amount": {
      const cap = p.fields.capability ?? "";
      const amount = parseInt(p.fields.amount ?? "0") || 0;
      const found = s.capabilities.find((c) => c.name === cap);
      const passed = !!found && found.amount >= amount;
      return {
        predicateId: p.id,
        passed,
        skipped: false,
        reason: passed
          ? `${cap} amount ${found!.amount} ≥ ${amount}`
          : found
          ? `${cap} amount ${found.amount} < ${amount}`
          : `capability ${cap} 不存在`,
        matched: found ? String(found.amount) : "(none)",
        expected: `≥ ${amount}`,
      };
    }
    case "fluid_id": {
      const expected = p.fields.fluid ?? "";
      const passed = !!expected && s.fluidId === expected;
      return {
        predicateId: p.id,
        passed,
        skipped: false,
        reason: passed
          ? "fluid 匹配"
          : s.fluidId === ""
          ? "block 不是 fluid"
          : `实际 ${s.fluidId}`,
        matched: s.fluidId || "(none)",
        expected,
      };
    }
    default:
      return {
        predicateId: p.id,
        passed: false,
        skipped: false,
        reason: "未实现",
      };
  }
}
