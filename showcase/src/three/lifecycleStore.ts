import { create } from "zustand";

// ============================================================
// Lifecycle Event Lab: 父+子+孙 三层嵌套的 lifecycle 事件可交互演示
// ============================================================

export type OccurrenceStatus = "valid" | "invalid" | "incomplete" | "stale";

export interface Occurrence {
  id: string;
  parentId: string | null;
  childDef: string; // "factory" / "arm" / "tool"
  instanceName: string; // "factory#0" / "left_arm#0" / "tool_a#0"
  status: OccurrenceStatus;
  offsetInParent: string; // "(0,0,0)" / "(-3,0,0)"
  // tool-specific contextual data
  context: Record<string, string>;
  // simulation: which "block" needs to be valid
  requiredBlocks: string[];
  brokenBlocks: string[];
}

export interface LifecycleEvent {
  ts: number;
  transition: "added" | "statusChanged";
  occurrenceId: string;
  childPath: string;
  previousStatus: OccurrenceStatus | null;
  currentStatus: OccurrenceStatus;
  reason: string;
}

interface State {
  occurrences: Occurrence[];
  events: LifecycleEvent[];
  parentContext: Record<string, string>; // 镜像的 context
  selected: string | null;
  policy: "NONE" | "statusOnly" | "statusAndContext";

  // actions
  addOccurrence: (parentId: string, childDef: string, name: string, offset: string) => void;
  removeOccurrence: (id: string) => void;
  toggleBlock: (occId: string, block: string) => void;
  select: (id: string | null) => void;
  setPolicy: (p: "NONE" | "statusOnly" | "statusAndContext") => void;
  reset: () => void;
}

let idCounter = 100;

function makeId() {
  return `o${idCounter++}`;
}

function evaluateStatus(occ: Occurrence): OccurrenceStatus {
  if (occ.brokenBlocks.length > 0) return "invalid";
  return "valid";
}

function buildChildPath(occurrences: Occurrence[], id: string): string {
  const occ = occurrences.find((o) => o.id === id);
  if (!occ) return "";
  if (!occ.parentId) return occ.instanceName;
  return buildChildPath(occurrences, occ.parentId) + " > " + occ.instanceName;
}

function recomputeContext(state: State): Record<string, string> {
  const ctx: Record<string, string> = {};
  const root = state.occurrences.find((o) => o.parentId === null);
  if (!root) return ctx;

  if (state.policy === "NONE") return ctx;

  // walk all occurrences, mirror status
  for (const occ of state.occurrences) {
    if (occ.id === root.id) continue;
    if (state.policy === "statusOnly" || state.policy === "statusAndContext") {
      ctx[`substructure.${occ.instanceName}.status`] = occ.status;
      ctx[`substructure.${occ.instanceName}.anchor_offset`] = occ.offsetInParent;
    }
    if (state.policy === "statusAndContext") {
      for (const [k, v] of Object.entries(occ.context)) {
        ctx[`substructure.${occ.instanceName}.context.${k}`] = v;
      }
    }
  }

  // root context (controller always mirrored)
  ctx["controller"] = root.context["controller"] ?? "(10,64,10)";
  ctx["facing"] = root.context["facing"] ?? "north";

  return ctx;
}

function pushEvent(
  state: State,
  transition: LifecycleEvent["transition"],
  occId: string,
  prev: OccurrenceStatus | null,
  curr: OccurrenceStatus,
  reason: string
): LifecycleEvent[] {
  const occ = state.occurrences.find((o) => o.id === occId);
  const e: LifecycleEvent = {
    ts: Date.now(),
    transition,
    occurrenceId: occ?.instanceName ?? occId,
    childPath: occ ? buildChildPath(state.occurrences, occId) : "",
    previousStatus: prev,
    currentStatus: curr,
    reason,
  };
  return [e, ...state.events].slice(0, 30);
}

export const useLifecycle = create<State>((set, get) => ({
  occurrences: [
    {
      id: "factory",
      parentId: null,
      childDef: "factory",
      instanceName: "factory#0",
      status: "valid",
      offsetInParent: "(0,0,0)",
      context: { controller: "(10,64,10)", facing: "north" },
      requiredBlocks: ["controller", "casing_x5"],
      brokenBlocks: [],
    },
  ],
  events: [],
  parentContext: { controller: "(10,64,10)", facing: "north" },
  selected: "factory",
  policy: "statusAndContext",

  addOccurrence: (parentId, childDef, name, offset) => {
    const parent = get().occurrences.find((o) => o.id === parentId);
    if (!parent) return;

    const newOcc: Occurrence = {
      id: makeId(),
      parentId,
      childDef,
      instanceName: name,
      status: "valid",
      offsetInParent: offset,
      context: childDef === "tool"
        ? { spell_book: "ars_nouveau:flight", charge: "100" }
        : childDef === "arm"
        ? { energy: "8192" }
        : {},
      requiredBlocks:
        childDef === "arm"
          ? ["frame_x6", "joint"]
          : childDef === "tool"
          ? ["bit", "spell_book"]
          : [],
      brokenBlocks: [],
    };

    const occurrences = [...get().occurrences, newOcc];
    const events = pushEvent(
      { ...get(), occurrences },
      "added",
      newOcc.id,
      null,
      "valid",
      `parent ${parent.instanceName} composed new child`
    );
    set((s) => ({
      occurrences,
      events,
      parentContext: recomputeContext({ ...s, occurrences }),
    }));
  },

  removeOccurrence: (id) => {
    const occ = get().occurrences.find((o) => o.id === id);
    if (!occ || !occ.parentId) return; // 不能删 root

    // 真实 API 没有 "removed" transition：子结构被移除时父结构通过 statusChanged 反映
    const parent = get().occurrences.find((o) => o.id === occ.parentId);
    const events = parent
      ? pushEvent(
          get(),
          "statusChanged",
          parent.id,
          parent.status,
          parent.status,
          `child ${occ.instanceName} removed from parent`
        )
      : get().events;
    const occurrences = get().occurrences.filter((o) => o.id !== id && o.parentId !== id);
    set((s) => ({
      occurrences,
      events,
      parentContext: recomputeContext({ ...s, occurrences }),
      selected: s.selected === id ? occ.parentId : s.selected,
    }));
  },

  toggleBlock: (occId, block) => {
    const occ = get().occurrences.find((o) => o.id === occId);
    if (!occ) return;

    const newBroken = occ.brokenBlocks.includes(block)
      ? occ.brokenBlocks.filter((b) => b !== block)
      : [...occ.brokenBlocks, block];

    const prevStatus = occ.status;
    const occurrences = get().occurrences.map((o) =>
      o.id === occId
        ? { ...o, brokenBlocks: newBroken, status: evaluateStatus({ ...o, brokenBlocks: newBroken }) }
        : o
    );

    const updated = occurrences.find((o) => o.id === occId)!;
    let events = get().events;
    if (updated.status !== prevStatus) {
      events = pushEvent(
        { ...get(), occurrences },
        "statusChanged",
        occId,
        prevStatus,
        updated.status,
        newBroken.length > 0
          ? `block(s) ${newBroken.join(", ")} broken`
          : "all blocks restored"
      );
    }

    set((s) => ({
      occurrences,
      events,
      parentContext: recomputeContext({ ...s, occurrences }),
    }));
  },

  select: (id) => set({ selected: id }),

  setPolicy: (policy) =>
    set((s) => ({
      policy,
      parentContext: recomputeContext({ ...s, policy }),
    })),

  reset: () =>
    set({
      occurrences: [
        {
          id: "factory",
          parentId: null,
          childDef: "factory",
          instanceName: "factory#0",
          status: "valid",
          offsetInParent: "(0,0,0)",
          context: { controller: "(10,64,10)", facing: "north" },
          requiredBlocks: ["controller", "casing_x5"],
          brokenBlocks: [],
        },
      ],
      events: [],
      parentContext: { controller: "(10,64,10)", facing: "north" },
      selected: "factory",
      policy: "statusAndContext",
    }),
}));

// 预设 child（按钮触发用）
export const CHILD_TEMPLATES = {
  arm: {
    childDef: "arm",
    nameFn: (n: number) => `arm#${n}`,
    offset: "(-3,0,0)",
    color: "#22d3ee",
  },
  tool: {
    childDef: "tool",
    nameFn: (n: number) => `tool#${n}`,
    offset: "(0,1,0)",
    color: "#f0abfc",
  },
  base: {
    childDef: "base",
    nameFn: (n: number) => `base#${n}`,
    offset: "(0,-1,0)",
    color: "#a3e635",
  },
};
