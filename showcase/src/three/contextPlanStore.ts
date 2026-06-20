import { create } from "zustand";

// ============================================================
// Context Plan Lab Store
// 演示通用上下文操作管线 (ContextPlan)：一个 hollow-box 结构，
// 用户勾选标准库操作 (count/sum/interiorVolume/derive/compute/bounds/first)，
// 实时生成 ContextPlan DSL + computed 值（TS 端按 Java 语义算）。
// ============================================================

export type OpId =
  | "count"
  | "sum"
  | "interiorVolume"
  | "derive"
  | "compute"
  | "bounds"
  | "first";

export interface OpDef {
  id: OpId;
  label: string;
  /** 输出键名（Java ContextKey.name） */
  outKey: string;
  /** DSL 片段（.op(...)） */
  dsl: (size: number) => string;
  /** 人类可读说明 */
  hint: string;
  accent: "brand" | "cyan" | "magenta" | "lime" | "amber" | "rose";
}

export const OPS: Record<OpId, OpDef> = {
  count: {
    id: "count",
    label: "count()",
    outKey: "casingCount",
    dsl: () =>
      `.op(count(casingCount, p -> p.inGroup("casing")))`,
    hint: "统计匹配点数量（按 group 过滤）",
    accent: "brand",
  },
  sum: {
    id: "sum",
    label: "sum()",
    outKey: "totalLevel",
    dsl: () =>
      `.op(sum(totalLevel, p -> p.inGroup("casing"),\n        p -> p.intProperty("level").orElse(0)))`,
    hint: "对每个匹配点提取 int 值求和",
    accent: "amber",
  },
  interiorVolume: {
    id: "interiorVolume",
    label: "interiorVolume()",
    outKey: "cavityVolume",
    dsl: () => `.op(interiorVolume(cavityVolume, "casing"))`,
    hint: "中空外壳的内部体积 = (w-2)(h-2)(d-2)",
    accent: "cyan",
  },
  derive: {
    id: "derive",
    label: "derive()",
    outKey: "upgradeFactor",
    dsl: () =>
      `.op(derive(upgradeFactor, casingCount,\n        c -> 1.0 + c / 9.0))`,
    hint: "读已有键 (anyValue) 转换成新值",
    accent: "magenta",
  },
  compute: {
    id: "compute",
    label: "compute()",
    outKey: "throughput",
    dsl: () =>
      `.op(compute(throughput, (matches, ctx) -> {\n        int vol = ctx.<Integer>anyValue(cavityVolume).orElse(0);\n        int casings = ctx.<Integer>anyValue(casingCount).orElse(0);\n        return vol * casings * 1.5;\n    }))`,
    hint: "自定义：读 matches + context，返回任意值",
    accent: "lime",
  },
  bounds: {
    id: "bounds",
    label: "bounds()",
    outKey: "casingBounds",
    dsl: () => `.op(bounds(casingBounds, p -> p.inGroup("casing")))`,
    hint: "匹配点的轴对齐包围盒 (Bounds3d)",
    accent: "rose",
  },
  first: {
    id: "first",
    label: "first()",
    outKey: "controllerId",
    dsl: () =>
      `.op(first(controllerId,\n        p -> p.inGroup("controller"),\n        MatchedPoint::value))`,
    hint: "第一个匹配点（声明顺序）",
    accent: "brand",
  },
};

/** 默认启用顺序（也是 DSL/trace 的执行顺序） */
export const DEFAULT_OPS: OpId[] = [
  "count",
  "interiorVolume",
  "derive",
  "compute",
];

// ── 几何计算（与 Java MatchResults/Bounds3d 语义一致） ──────────────

/** hollow box size N → shell point 数量 = N^3 - (N-2)^3（N<3 时无内部） */
export function shellPointCount(size: number): number {
  if (size < 1) return 0;
  const inner = size >= 3 ? Math.pow(size - 2, 3) : 0;
  return Math.pow(size, 3) - inner;
}

/** interior volume = (w-2)(h-2)(d-2)，每维 clamp ≥ 0 */
export function interiorVolume(size: number): number {
  const d = Math.max(0, size - 2);
  return d * d * d;
}

/** casingCount = shell 点数；controller 在 (0,0,0) 之外的 shell 上另算一个 group */
export function casingCount(size: number): number {
  // hollow box 的 shell 都是 casing group
  return shellPointCount(size);
}

export interface ComputedEntry {
  op: OpId;
  key: string;
  type: "int" | "double" | "string" | "bounds";
  value: string;
  dependsOn?: string[];
}

/** 计算某个操作在给定 size 下的输出值（TS 端模拟 Java 执行） */
function evalOp(op: OpId, size: number, prior: Record<string, number>): ComputedEntry | null {
  switch (op) {
    case "count":
      return { op, key: "casingCount", type: "int", value: String(casingCount(size)) };
    case "sum":
      // 每个 casing 假设 level=2
      return { op, key: "totalLevel", type: "int", value: String(casingCount(size) * 2) };
    case "interiorVolume":
      return { op, key: "cavityVolume", type: "int", value: String(interiorVolume(size)) };
    case "derive": {
      const c = prior["casingCount"] ?? casingCount(size);
      const f = 1.0 + c / 9.0;
      return {
        op, key: "upgradeFactor", type: "double",
        value: f.toFixed(2), dependsOn: ["casingCount"],
      };
    }
    case "compute": {
      const vol = prior["cavityVolume"] ?? interiorVolume(size);
      const casings = prior["casingCount"] ?? casingCount(size);
      return {
        op, key: "throughput", type: "double",
        value: (vol * casings * 1.5).toFixed(1),
        dependsOn: ["cavityVolume", "casingCount"],
      };
    }
    case "bounds":
      return {
        op, key: "casingBounds", type: "bounds",
        value: `Bounds3d((0,0,0),(${size - 1},${size - 1},${size - 1}))`,
      };
    case "first":
      return { op, key: "controllerId", type: "string", value: "cumulus:controller" };
  }
}

/** 按启用顺序执行所有操作，产出 computed 条目（含依赖追踪） */
export function evaluate(enabled: OpId[], size: number): ComputedEntry[] {
  const priorNum: Record<string, number> = {};
  const out: ComputedEntry[] = [];
  for (const op of enabled) {
    const e = evalOp(op, size, priorNum);
    if (!e) continue;
    if (e.type === "int" || e.type === "double") {
      priorNum[e.key] = parseFloat(e.value);
    }
    out.push(e);
  }
  return out;
}

// ── store ──────────────────────────────────────────────────────────

interface ContextPlanState {
  size: number;
  enabled: OpId[];
  setSize: (n: number) => void;
  toggle: (op: OpId) => void;
  reset: () => void;
}

export const useContextPlan = create<ContextPlanState>((set) => ({
  size: 3,
  enabled: [...DEFAULT_OPS],
  setSize: (n) => set({ size: Math.max(3, Math.min(9, n)) }),
  toggle: (op) =>
    set((s) => ({
      enabled: s.enabled.includes(op)
        ? s.enabled.filter((o) => o !== op)
        : [...s.enabled, op],
    })),
  reset: () => set({ size: 3, enabled: [...DEFAULT_OPS] }),
}));

/** 生成 ContextPlan DSL 字符串 */
export function generatePlanDsl(enabled: OpId[], size: number): string {
  const ops = enabled.map((id) => OPS[id].dsl(size));
  return [
    "import static dev.vfyjxf.cumulus.api.context.plan.stdlib.Operations.*;",
    "",
    ".contextPlan(plan -> plan",
    ...ops.map((o, i) => `    ${o}${i < ops.length - 1 ? "" : ""}`),
    ")",
  ].join("\n");
}
