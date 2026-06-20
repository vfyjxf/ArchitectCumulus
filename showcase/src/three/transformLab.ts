import { create } from "zustand";

// ============================================================
// Transform Lab: 看一个结构在不同 transform 候选下如何变形/通过/失败
// ============================================================

export interface LabBlock {
  // relative to structure origin
  rel: [number, number, number];
  color: string;
  label: string;
  kind: "controller" | "casing" | "port";
}

export type TransformId =
  | "identity"
  | "facing_n"
  | "facing_e"
  | "facing_s"
  | "facing_w"
  | "rotate_y_90"
  | "rotate_y_180"
  | "rotate_y_270"
  | "mirror_x"
  | "mirror_z";

export interface TransformDef {
  id: TransformId;
  label: string;
  // 矩阵作用于 (x, y, z)
  apply: (p: [number, number, number]) => [number, number, number];
  color: string;
  description: string;
}

export const TRANSFORMS: TransformDef[] = [
  {
    id: "identity",
    label: "identity",
    apply: (p) => p,
    color: "#7c5cff",
    description: "不旋转。基准候选。",
  },
  {
    id: "facing_n",
    label: "horizontal_facing · N",
    apply: (p) => [p[0], p[1], p[2]],
    color: "#22d3ee",
    description: "朝北（同 identity）",
  },
  {
    id: "facing_e",
    label: "horizontal_facing · E",
    apply: (p) => [p[2], p[1], -p[0]],
    color: "#22d3ee",
    description: "朝东：x↔z 互换，原 x 取反",
  },
  {
    id: "facing_s",
    label: "horizontal_facing · S",
    apply: (p) => [-p[0], p[1], -p[2]],
    color: "#22d3ee",
    description: "朝南：x、z 均取反",
  },
  {
    id: "facing_w",
    label: "horizontal_facing · W",
    apply: (p) => [-p[2], p[1], p[0]],
    color: "#22d3ee",
    description: "朝西：x↔z 互换，原 z 取反",
  },
  {
    id: "rotate_y_90",
    label: "rotate_y_90",
    apply: (p) => [p[2], p[1], -p[0]],
    color: "#f0abfc",
    description: "绕 Y 轴顺时针 90°",
  },
  {
    id: "rotate_y_180",
    label: "rotate_y_180",
    apply: (p) => [-p[0], p[1], -p[2]],
    color: "#f0abfc",
    description: "绕 Y 轴 180°",
  },
  {
    id: "rotate_y_270",
    label: "rotate_y_270",
    apply: (p) => [-p[2], p[1], p[0]],
    color: "#f0abfc",
    description: "绕 Y 轴顺时针 270° (= 逆时针 90°)",
  },
  {
    id: "mirror_x",
    label: "mirror_x",
    apply: (p) => [-p[0], p[1], p[2]],
    color: "#a3e635",
    description: "沿 X 轴镜像",
  },
  {
    id: "mirror_z",
    label: "mirror_z",
    apply: (p) => [p[0], p[1], -p[2]],
    color: "#a3e635",
    description: "沿 Z 轴镜像",
  },
];

// 一个"L 形"结构作为示例：3 个 casing + 1 controller + 1 port
export const LAB_BLOCKS: LabBlock[] = [
  { rel: [0, 0, 0], color: "#7c5cff", label: "controller", kind: "controller" },
  { rel: [1, 0, 0], color: "#fb7185", label: "casing_e", kind: "casing" },
  { rel: [2, 0, 0], color: "#fb7185", label: "casing_ee", kind: "casing" },
  { rel: [0, 0, 1], color: "#fb7185", label: "casing_s", kind: "casing" },
  { rel: [0, 0, 2], color: "#fb7185", label: "casing_ss", kind: "casing" },
  { rel: [2, 0, 0], color: "#fbbf24", label: "energy_port", kind: "port" },
];

// 模拟"世界"：固定位置的方块
// 我们用世界方块是否匹配预期来判定 transform 是否 valid
export interface WorldBlock {
  pos: [number, number, number];
  expectedKind: LabBlock["kind"];
}

// 世界里散布了一些方块，结构在 anchor (0,0,0) 上
// 不同 transform 会让结构不同位置匹配
export const WORLD_BLOCKS: WorldBlock[] = [
  // L 形朝北的形态（identity / facing_n）
  { pos: [0, 0, 0], expectedKind: "controller" },
  { pos: [1, 0, 0], expectedKind: "casing" },
  { pos: [2, 0, 0], expectedKind: "casing" },
  { pos: [0, 0, 1], expectedKind: "casing" },
  { pos: [0, 0, 2], expectedKind: "casing" },
  // 注意 energy_port 在 [2,0,0] 与 casing 冲突 — 故意刁难某些 transform
];

interface State {
  selected: TransformId;
  enabled: Record<TransformId, boolean>;
  setSelected: (t: TransformId) => void;
  toggle: (t: TransformId) => void;
  enableAll: () => void;
  disableAll: () => void;
}

const initialEnabled: Record<TransformId, boolean> = {
  identity: true,
  facing_n: true,
  facing_e: true,
  facing_s: true,
  facing_w: true,
  rotate_y_90: false,
  rotate_y_180: false,
  rotate_y_270: false,
  mirror_x: false,
  mirror_z: false,
};

export const useTransformLab = create<State>((set) => ({
  selected: "identity",
  enabled: initialEnabled,
  setSelected: (t) => set({ selected: t }),
  toggle: (t) =>
    set((s) => ({ enabled: { ...s.enabled, [t]: !s.enabled[t] } })),
  enableAll: () => {
    const next = {} as Record<TransformId, boolean>;
    TRANSFORMS.forEach((t) => (next[t.id] = true));
    set({ enabled: next });
  },
  disableAll: () => {
    const next = {} as Record<TransformId, boolean>;
    TRANSFORMS.forEach((t) => (next[t.id] = false));
    set({ enabled: next });
  },
}));

// ============================================================
// 求值：每个启用的 transform 是否让 LAB_BLOCKS 在 WORLD_BLOCKS 上匹配
// ============================================================

export interface TransformEval {
  transform: TransformDef;
  // 转换后的方块在世界中的位置
  placedBlocks: { rel: [number, number, number]; world: [number, number, number]; color: string; label: string; kind: LabBlock["kind"]; matched: boolean }[];
  passed: boolean;
  // 短路位置
  firstFailIndex: number;
  matchCount: number;
  totalRequired: number;
}

export function evaluateTransform(t: TransformDef): TransformEval {
  const placedBlocks = LAB_BLOCKS.map((b) => {
    const world = t.apply(b.rel);
    // 检查 world block 是否存在且 kind 匹配
    const worldBlock = WORLD_BLOCKS.find((w) => w.pos[0] === world[0] && w.pos[1] === world[1] && w.pos[2] === world[2]);
    const matched = !!worldBlock && worldBlock.expectedKind === b.kind;
    return {
      rel: b.rel,
      world,
      color: b.color,
      label: b.label,
      kind: b.kind,
      matched,
    };
  });

  const matchCount = placedBlocks.filter((b) => b.matched).length;
  const totalRequired = placedBlocks.length;
  const firstFailIndex = placedBlocks.findIndex((b) => !b.matched);
  const passed = firstFailIndex === -1;

  return {
    transform: t,
    placedBlocks,
    passed,
    firstFailIndex: firstFailIndex === -1 ? placedBlocks.length : firstFailIndex,
    matchCount,
    totalRequired,
  };
}
