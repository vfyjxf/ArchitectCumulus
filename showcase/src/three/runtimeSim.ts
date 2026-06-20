import { create } from "zustand";

export type SectionState =
  | "idle"
  | "loaded"
  | "dirty"
  | "queued"
  | "checking"
  | "valid"
  | "invalid"
  | "requested";

interface SectionData {
  state: SectionState;
  hasInstance: boolean;
  valid: boolean;
}

interface RuntimeState {
  sections: SectionData[];
  tick: number;
  budget: number;
  mutationRate: number; // 0-100
  loadedRate: number; // 0-100
  cursor: { x: number; z: number };
  paused: boolean;
  fast: boolean;
  stats: {
    checked: number;
    queued: number;
    requests: number;
    valid: number;
    invalid: number;
  };
  log: { tick: number; msg: string; tone: "ok" | "warn" | "err" }[];
  setBudget: (n: number) => void;
  setMutation: (n: number) => void;
  setLoaded: (n: number) => void;
  togglePause: () => void;
  toggleFast: () => void;
  reset: () => void;
  step: () => void;
}

const N = 8;
const TOTAL = N * N;

function makeInitial(): SectionData[] {
  const arr: SectionData[] = [];
  for (let i = 0; i < TOTAL; i++) {
    arr.push({
      state: "loaded",
      hasInstance: Math.random() < 0.18,
      valid: false,
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
  cursor: { x: 0, z: 0 },
  paused: false,
  fast: false,
  stats: { checked: 0, queued: 0, requests: 0, valid: 0, invalid: 0 },
  log: [{ tick: 0, msg: "runtime engine boot", tone: "ok" }],
  setBudget: (n) => set({ budget: n }),
  setMutation: (n) => set({ mutationRate: n }),
  setLoaded: (n) => set({ loadedRate: n }),
  togglePause: () => set((s) => ({ paused: !s.paused })),
  toggleFast: () => set((s) => ({ fast: !s.fast })),
  reset: () =>
    set({
      sections: makeInitial(),
      tick: 0,
      cursor: { x: 0, z: 0 },
      stats: { checked: 0, queued: 0, requests: 0, valid: 0, invalid: 0 },
      log: [{ tick: 0, msg: "runtime engine reset", tone: "ok" }],
    }),
  step: () => {
    const s = get();
    if (s.paused) return;
    const mutRate = s.mutationRate / 100;
    const loadedRate = s.loadedRate / 100;
    const sections = s.sections.map((sec) => ({ ...sec }));

    // step 1: random mutations on loaded sections
    const newLogs: RuntimeState["log"] = [];
    for (let i = 0; i < sections.length; i++) {
      const sec = sections[i];
      if (sec.state === "valid" || sec.state === "loaded" || sec.state === "invalid") {
        if (Math.random() < mutRate * 0.04) {
          sec.state = "dirty";
        }
      }
      // randomly unload/reload
      if (Math.random() < 0.02) {
        if (sec.state === "loaded" || sec.state === "valid") {
          sec.state = "idle";
        } else if (sec.state === "idle" && Math.random() < loadedRate) {
          sec.state = "loaded";
        }
      }
    }

    // step 2: collect dirty and queue them
    const dirtyIdx: number[] = [];
    for (let i = 0; i < sections.length; i++) {
      if (sections[i].state === "dirty") {
        sections[i].state = "queued";
        dirtyIdx.push(i);
      }
    }

    // step 3: process up to budget
    let processed = 0;
    let cursor = { x: -1, z: -1 };
    let valid = s.stats.valid;
    let invalid = s.stats.invalid;
    let checked = s.stats.checked;
    let requests = s.stats.requests;

    for (const idx of dirtyIdx) {
      if (processed >= s.budget) {
        // leave in queued state
        continue;
      }
      const sec = sections[idx];
      // check loaded
      if (sec.state === "idle" || (sec.state === "queued" && Math.random() > loadedRate)) {
        sec.state = "requested";
        requests++;
        newLogs.push({
          tick: s.tick + 1,
          msg: `section (${idx % N},${Math.floor(idx / N)}) UNLOADED → DataRequest`,
          tone: "warn",
        });
        processed++;
        cursor = { x: idx % N, z: Math.floor(idx / N) };
        continue;
      }

      // perform check
      sec.state = "checking";
      cursor = { x: idx % N, z: Math.floor(idx / N) };
      processed++;
      checked++;

      const ok = sec.hasInstance ? Math.random() < 0.7 : Math.random() < 0.4;
      if (ok) {
        sec.state = "valid";
        sec.valid = true;
        valid++;
        if (sec.hasInstance && newLogs.length < 4) {
          newLogs.push({
            tick: s.tick + 1,
            msg: `✓ section (${idx % N},${Math.floor(idx / N)}) valid, InstanceDelta published`,
            tone: "ok",
          });
        }
      } else {
        sec.state = "invalid";
        sec.valid = false;
        invalid++;
        if (sec.hasInstance && newLogs.length < 4) {
          newLogs.push({
            tick: s.tick + 1,
            msg: `✗ section (${idx % N},${Math.floor(idx / N)}) invalid, 2 diagnostic nodes`,
            tone: "err",
          });
        }
      }
    }

    set({
      sections,
      tick: s.tick + 1,
      cursor,
      stats: {
        checked,
        queued: dirtyIdx.length - processed + (dirtyIdx.length - processed < 0 ? 0 : 0),
        requests,
        valid,
        invalid,
      },
      log: [...newLogs.reverse(), ...s.log].slice(0, 24),
    });
  },
}));
