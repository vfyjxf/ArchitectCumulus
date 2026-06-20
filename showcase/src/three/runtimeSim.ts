import { create } from "zustand";

// ============================================================
// Runtime 调度器模拟 —— 忠实反映真实 RuntimeCheckEngine 语义
//
// 真实 API（src/main/java/.../impl/runtime/）：
//   InstanceStatus = valid | invalid | incomplete | stale
//   RuntimeCheckEngine.tick(tick, maxChecks)        → 完整检查 (StructureCheckExecutor.check)
//   RuntimeCheckEngine.fastTick(tick, maxChecks)    → 快速预检 (fastCheck)
//   RuntimeCheckEngine.markDirty(SectionKey)        → 把 section 里的实例入队
//   RuntimeScheduler.drainDue(tick, maxChecks, fn)  → 每 tick 最多消费 maxChecks 个
//
// 模拟映射：
//   每个网格 cell = 一个 SectionKey，内含 0~N 个结构实例。
//   玩家改动方块 → markDirty → 实例状态变 stale（"需要重新检查"）。
//   tick 消费预算检查 stale 实例：
//     - 区块已加载 + 匹配 → valid / invalid
//     - 区块未加载 → incomplete（发 DataRequest，不报 INVALID）
//   未被改动的上次结果保留为上一次的 valid/invalid（stale 只在被 markDirty 后短暂出现）。
// ============================================================

/** 真实 InstanceStatus 的四态 */
export type InstanceStatus = "valid" | "invalid" | "incomplete" | "stale";

export const STATUS_COLOR: Record<InstanceStatus, string> = {
  valid: "#a3e635",
  invalid: "#fb7185",
  incomplete: "#fbbf24",
  stale: "#7c5cff",
};

export const STATUS_LABEL: Record<InstanceStatus, string> = {
  valid: "valid",
  invalid: "invalid",
  incomplete: "incomplete",
  stale: "stale (待检)",
};

/** 网格 cell 的状态（一个 SectionKey） */
interface SectionData {
  /** 该 section 是否已加载（模拟 chunk loaded） */
  loaded: boolean;
  /** 该 section 是否有结构实例锚定 */
  hasInstance: boolean;
  /** 实例当前状态 */
  status: InstanceStatus;
  /** 是否被 markDirty 入队（玩家改动） */
  dirty: boolean;
}

interface RuntimeState {
  sections: SectionData[];
  tick: number;
  /** maxChecksPerTick — 每 tick 最多检查的实例数 */
  budget: number;
  /** 玩家改动方块的概率（模拟 markDirty 触发率） */
  mutationRate: number;
  /** 区块加载率 */
  loadedRate: number;
  /** tick (完整检查) vs fastTick (快速预检) */
  mode: "tick" | "fastTick";
  paused: boolean;
  cursor: { x: number; z: number } | null;
  stats: {
    checked: number;
    requests: number;
    valid: number;
    invalid: number;
    incomplete: number;
    stale: number;
  };
  log: { tick: number; msg: string; tone: "ok" | "warn" | "err" }[];
  setBudget: (n: number) => void;
  setMutation: (n: number) => void;
  setLoaded: (n: number) => void;
  togglePause: () => void;
  toggleMode: () => void;
  reset: () => void;
  step: () => void;
}

const N = 8;
const TOTAL = N * N;

function makeInitial(): SectionData[] {
  const arr: SectionData[] = [];
  for (let i = 0; i < TOTAL; i++) {
    arr.push({
      loaded: true,
      hasInstance: Math.random() < 0.18,
      status: "stale",
      dirty: false,
    });
  }
  return arr;
}

export const useRuntimeSim = create<RuntimeState>((set, get) => ({
  sections: makeInitial(),
  tick: 0,
  budget: 6,
  mutationRate: 20,
  loadedRate: 70,
  mode: "tick",
  paused: false,
  cursor: null,
  stats: { checked: 0, requests: 0, valid: 0, invalid: 0, incomplete: 0, stale: 0 },
  log: [{ tick: 0, msg: "runtime engine boot · tick mode", tone: "ok" }],
  setBudget: (n) => set({ budget: n }),
  setMutation: (n) => set({ mutationRate: n }),
  setLoaded: (n) => set({ loadedRate: n }),
  togglePause: () => set((s) => ({ paused: !s.paused })),
  toggleMode: () =>
    set((s) => ({
      mode: s.mode === "tick" ? "fastTick" : "tick",
      log: [
        { tick: s.tick, msg: `切换 → ${s.mode === "tick" ? "fastTick" : "tick"} 模式`, tone: "ok" as const },
        ...s.log,
      ].slice(0, 24),
    })),
  reset: () =>
    set({
      sections: makeInitial(),
      tick: 0,
      cursor: null,
      stats: { checked: 0, requests: 0, valid: 0, invalid: 0, incomplete: 0, stale: 0 },
      log: [{ tick: 0, msg: "runtime engine reset", tone: "ok" }],
    }),
  step: () => {
    const s = get();
    if (s.paused) return;
    const mutRate = s.mutationRate / 100;
    const loadedRate = s.loadedRate / 100;
    const sections = s.sections.map((sec) => ({ ...sec }));
    const newLogs: RuntimeState["log"] = [];

    // ── step 1: 模拟玩家改动 → markDirty(section) ──
    for (let i = 0; i < sections.length; i++) {
      const sec = sections[i];
      // 随机加载/卸载区块
      if (Math.random() < 0.03) {
        sec.loaded = !sec.loaded;
        if (!sec.loaded && Math.random() > loadedRate) sec.loaded = false;
        else if (sec.loaded && Math.random() < loadedRate) sec.loaded = true;
        else sec.loaded = Math.random() < loadedRate;
      }
      // 玩家改动方块 → markDirty
      if (sec.hasInstance && (sec.status === "valid" || sec.status === "invalid" || sec.status === "incomplete")) {
        if (Math.random() < mutRate * 0.05) {
          sec.dirty = true;
          sec.status = "stale";
        }
      }
    }

    // ── step 2: drainDue(tick, maxChecks, checker) ──
    // 收集 dirty 的实例（stale 状态），按预算检查
    const dirtyIdx: number[] = [];
    for (let i = 0; i < sections.length; i++) {
      if (sections[i].dirty) dirtyIdx.push(i);
    }

    let processed = 0;
    let cursor: { x: number; z: number } | null = null;
    const stats = { ...s.stats };

    // fastTick 模式：只做快速预检（跳过部分检查，更快但可能漏）
    const fastCheckSkipRate = s.mode === "fastTick" ? 0.3 : 0;

    for (const idx of dirtyIdx) {
      if (processed >= s.budget) break; // 超预算 → 留在 stale，下帧继续
      const sec = sections[idx];
      const x = idx % N;
      const z = Math.floor(idx / N);
      cursor = { x, z };
      processed++;
      stats.checked++;

      // 区块未加载 → incomplete（发 DataRequest，不是 invalid）
      if (!sec.loaded) {
        sec.status = "incomplete";
        sec.dirty = false; // request 发出后不再每帧重检，等 chunk load 事件
        stats.incomplete++;
        stats.requests++;
        if (newLogs.length < 4) {
          newLogs.push({
            tick: s.tick + 1,
            msg: `section (${x},${z}) incomplete → DataRequest 发出（chunk 未加载）`,
            tone: "warn",
          });
        }
        continue;
      }

      // fastTick 模式有概率跳过完整检查
      if (Math.random() < fastCheckSkipRate) {
        sec.status = "stale"; // 留给下次完整 tick
        stats.stale++;
        continue;
      }

      // 完整检查：StructureCheckExecutor.check → valid / invalid
      sec.dirty = false;
      const ok = sec.hasInstance ? Math.random() < 0.72 : Math.random() < 0.4;
      if (ok) {
        sec.status = "valid";
        stats.valid++;
        if (sec.hasInstance && newLogs.length < 4) {
          newLogs.push({
            tick: s.tick + 1,
            msg: `✓ section (${x},${z}) valid · InstanceDelta published (revision++)`,
            tone: "ok",
          });
        }
      } else {
        sec.status = "invalid";
        stats.invalid++;
        if (sec.hasInstance && newLogs.length < 4) {
          newLogs.push({
            tick: s.tick + 1,
            msg: `✗ section (${x},${z}) invalid · diagnostic graph published`,
            tone: "err",
          });
        }
      }
    }

    // 统计当前各状态数量
    const current = { valid: 0, invalid: 0, incomplete: 0, stale: 0 };
    for (const sec of sections) {
      if (sec.hasInstance) current[sec.status]++;
    }

    set({
      sections,
      tick: s.tick + 1,
      cursor,
      stats: {
        checked: stats.checked,
        requests: stats.requests,
        valid: current.valid,
        invalid: current.invalid,
        incomplete: current.incomplete,
        stale: current.stale,
      },
      log: [...newLogs.reverse(), ...s.log].slice(0, 24),
    });
  },
}));
