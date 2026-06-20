import { create } from "zustand";

// ============================================================
// 数据驱动热重载实验室：JSON 编辑 + 解析 + 3D 预览 + 管线可视化
// ============================================================

export interface JsonComponent {
  kind: "point" | "box" | "hollowBox" | "region";
  name: string;
  from: [number, number, number];
  to: [number, number, number];
  target: string;
  predicate: string;
  capture?: string;
}

export interface ParsedStructure {
  id: string;
  schemaVersion: number;
  transforms: string[];
  components: JsonComponent[];
  contextRules: { name: string; expression: string }[];
}

export type ReloadStage =
  | "idle"
  | "scanning"
  | "parsing"
  | "validating"
  | "indexing"
  | "registering"
  | "live";

export interface ReloadLogEntry {
  ts: number;
  stage: ReloadStage;
  level: "info" | "warn" | "error" | "ok";
  msg: string;
}

export interface ParseDiagnostic {
  severity: "error" | "warn";
  line: number;
  column?: number;
  msg: string;
}

interface State {
  // input
  rawJson: string;
  structureId: string;
  // last successfully parsed
  parsed: ParsedStructure | null;
  // currently displayed in 3D
  displayed: ParsedStructure | null;
  // parse errors
  diagnostics: ParseDiagnostic[];
  // reload simulation
  stage: ReloadStage;
  stageProgress: number; // 0..1 within current stage
  log: ReloadLogEntry[];
  reloads: number;
  lastReloadMs: number;
  // actions
  setRawJson: (s: string) => void;
  loadPreset: (id: string) => void;
  triggerReload: () => void;
  resetStage: () => void;
}

// ============================================================
// 预设 JSON
// ============================================================

export const JSON_PRESETS: Record<string, { label: string; description: string; json: string }> = {
  pylon: {
    label: "pylon",
    description: "1 个 controller 方块",
    json: `{
  "id": "cumulus:pylon",
  "schema_version": 1,
  "transforms": [
    { "id": "cumulus:identity" },
    { "id": "cumulus:horizontal_facing" }
  ],
  "components": [
    {
      "kind": "point",
      "name": "controller",
      "from": [0, 0, 0],
      "to": [0, 0, 0],
      "target": "cumulus:block",
      "predicate": "cumulus:controller",
      "capture": "controller"
    }
  ],
  "context_rules": [
    { "name": "facing", "expression": "controller.facing" }
  ]
}`,
  },
  forge: {
    label: "mini_forge",
    description: "3×3×3 hollowBox + controller",
    json: `{
  "id": "cumulus:mini_forge",
  "schema_version": 2,
  "transforms": [
    { "id": "cumulus:identity" },
    { "id": "cumulus:horizontal_facing" }
  ],
  "components": [
    {
      "kind": "point",
      "name": "controller",
      "from": [0, 1, 0],
      "to": [0, 1, 0],
      "target": "cumulus:block",
      "predicate": "cumulus:forge_controller",
      "capture": "controller"
    },
    {
      "kind": "hollowBox",
      "name": "casing",
      "from": [-1, 0, -1],
      "to": [1, 2, 1],
      "target": "cumulus:block",
      "predicate": "#cumulus:heat_casing"
    }
  ],
  "context_rules": [
    { "name": "facing", "expression": "controller.facing" }
  ]
}`,
  },
  broken: {
    label: "broken_json",
    description: "故意写错（少了逗号）",
    json: `{
  "id": "cumulus:broken",
  "schema_version": 1,
  "transforms": [
    { "id": "cumulus:identity" }
  ],
  "components": [
    {
      "kind": "point"
      "name": "controller",
      "from": [0, 0, 0],
      "to": [0, 0, 0],
      "target": "cumulus:block",
      "predicate": "cumulus:controller"
    }
  ]
}`,
  },
  duplicate: {
    label: "duplicate_id",
    description: "两个 point 同名 → 验证失败",
    json: `{
  "id": "cumulus:duplicate",
  "schema_version": 1,
  "transforms": [
    { "id": "cumulus:identity" }
  ],
  "components": [
    {
      "kind": "point",
      "name": "ctrl",
      "from": [0, 0, 0],
      "to": [0, 0, 0],
      "target": "cumulus:block",
      "predicate": "cumulus:controller"
    },
    {
      "kind": "point",
      "name": "ctrl",
      "from": [1, 0, 0],
      "to": [1, 0, 0],
      "target": "cumulus:block",
      "predicate": "cumulus:casing"
    }
  ]
}`,
  },
  bigbox: {
    label: "big_factory",
    description: "5×3×5 hollowBox + region 净空 + substructure-like",
    json: `{
  "id": "cumulus:big_factory",
  "schema_version": 3,
  "transforms": [
    { "id": "cumulus:identity" },
    { "id": "cumulus:horizontal_facing" },
    { "id": "cumulus:mirror_x" }
  ],
  "components": [
    {
      "kind": "point",
      "name": "controller",
      "from": [0, 0, 0],
      "to": [0, 0, 0],
      "target": "cumulus:block",
      "predicate": "cumulus:factory_ctrl",
      "capture": "controller"
    },
    {
      "kind": "hollowBox",
      "name": "shell",
      "from": [-2, 0, -2],
      "to": [2, 3, 2],
      "target": "cumulus:block",
      "predicate": "#cumulus:factory_casing"
    },
    {
      "kind": "region",
      "name": "door",
      "from": [-1, 0, 2],
      "to": [1, 2, 2],
      "target": "cumulus:region",
      "predicate": "cumulus:air_or_door"
    }
  ],
  "context_rules": [
    { "name": "facing", "expression": "controller.facing" },
    { "name": "level", "expression": "controller.level" }
  ]
}`,
  },
};

export const JSON_PRESET_NAMES = Object.keys(JSON_PRESETS);

// ============================================================
// 极简 JSON 解析（带行列号诊断）
// ============================================================

function locateChar(text: string, idx: number): { line: number; column: number } {
  let line = 1;
  let column = 1;
  for (let i = 0; i < idx && i < text.length; i++) {
    if (text[i] === "\n") {
      line++;
      column = 1;
    } else {
      column++;
    }
  }
  return { line, column };
}

function parseJsonWithDiagnostics(raw: string): {
  parsed: any;
  diagnostics: ParseDiagnostic[];
} {
  const diagnostics: ParseDiagnostic[] = [];
  let parsed: any = null;
  try {
    parsed = JSON.parse(raw);
  } catch (e: any) {
    const msg = e.message || "parse error";

    // 尝试从 message 里提取 "position N"
    let pos: number | undefined;
    const m1 = msg.match(/position (\d+)/);
    if (m1) pos = parseInt(m1[1]);

    // 兜底：二分搜索找最长可解析前缀
    if (pos === undefined) {
      pos = findFirstBrokenPos(raw);
    }

    let line = 1;
    let column = 1;
    if (typeof pos === "number") {
      const loc = locateChar(raw, pos);
      line = loc.line;
      column = loc.column;
    }

    // 从 message 里提取实际 token
    const tokenMatch = msg.match(/(Unexpected token|Expected|Unexpected end|Unexpected non-whitespace|Unexpected number)(.+)?/);
    const friendly = tokenMatch ? msg : `JSON 解析失败：${msg}`;

    diagnostics.push({
      severity: "error",
      line,
      column,
      msg: friendly,
    });
  }
  return { parsed, diagnostics };
}

// 找到第一个让 JSON.parse 失败的位置（前缀已经合法，加 1 字符就坏）
function findFirstBrokenPos(raw: string): number | undefined {
  // 二分：最大 lo 使得 raw.slice(0, lo) 不报 'position'
  // 但 JSON.parse 即使前缀不完整也会报错。改用 "尝试解析 raw.slice(0, lo) + ']'/'}'" 不可行。
  // 简单做法：从 1 到 raw.length 二分找最大 parseable-length prefix（需要合法 closing）。
  // 实际上更实用：扫描行，逐行检查 'position' 是否在那一行附近。
  // 这里用：尝试 parse 每一行删除最后一个字符的版本，找到稳定点。
  // 退而求其次：返回 raw 里第一个非空白、看起来"出问题"的字符。
  // 简化版：扫到第一个不是合法 JSON 字符的位置（unclosed string, dangling comma 等）。
  return scanForFirstIssue(raw);
}

function scanForFirstIssue(raw: string): number | undefined {
  // 极简词法扫描，找 "missing comma" 类型问题
  let i = 0;
  let inString = false;
  let escape = false;
  // 上一类"值结束"的字符：} ] "  (number/keyword 末尾不易判断，跳过)
  let lastValueEnd: number | null = null;

  while (i < raw.length) {
    const ch = raw[i];
    if (inString) {
      if (escape) escape = false;
      else if (ch === "\\") escape = true;
      else if (ch === '"') {
        inString = false;
        lastValueEnd = i;
      }
      i++;
      continue;
    }
    if (ch === '"') {
      // 如果上一个值刚结束，这个 " 又开始一个新值 → 缺逗号
      if (lastValueEnd !== null) {
        return i;
      }
      inString = true;
      i++;
      continue;
    }
    if (ch === " " || ch === "\t" || ch === "\n" || ch === "\r") {
      i++;
      continue;
    }
    // structural chars
    if (ch === "{" || ch === "[") {
      lastValueEnd = null;
      i++;
      continue;
    }
    if (ch === "}" || ch === "]") {
      lastValueEnd = i;
      i++;
      continue;
    }
    if (ch === ":") {
      lastValueEnd = null;
      i++;
      continue;
    }
    if (ch === ",") {
      lastValueEnd = null;
      i++;
      continue;
    }
    // value-like char (letter, digit, minus, dot)
    // 如果上一个值刚结束（即看到 "  }  ] 之后） 又看到 value → 缺逗号
    if (lastValueEnd !== null && /[tfn0-9.\-]/.test(ch)) {
      // 但 keyword 后跟 whitespace + 然后 value 的情况已经被前面空格分支处理
      // 这种情况下其实是数字/true/false/null 紧跟，是缺逗号
      return i;
    }
    i++;
  }
  return 0;
}

// ============================================================
// IR 校验（schema-level）
// ============================================================

function validateStructure(parsed: any): {
  ok: boolean;
  structure: ParsedStructure | null;
  diagnostics: ParseDiagnostic[];
} {
  const diagnostics: ParseDiagnostic[] = [];
  if (!parsed || typeof parsed !== "object") {
    diagnostics.push({ severity: "error", line: 0, msg: "root must be a JSON object" });
    return { ok: false, structure: null, diagnostics };
  }
  if (typeof parsed.id !== "string") {
    diagnostics.push({ severity: "error", line: 0, msg: 'missing "id" string' });
  }
  if (!Array.isArray(parsed.components)) {
    diagnostics.push({ severity: "error", line: 0, msg: '"components" must be an array' });
  }

  const components: JsonComponent[] = Array.isArray(parsed.components) ? parsed.components : [];
  const seenNames = new Map<string, number>();
  components.forEach((c: any, i: number) => {
    if (typeof c.name === "string") {
      if (seenNames.has(c.name)) {
        diagnostics.push({
          severity: "error",
          line: 0,
          msg: `duplicate component name "${c.name}" (also at #${seenNames.get(c.name)})`,
        });
      } else {
        seenNames.set(c.name, i);
      }
    }
    if (!c.from || !c.to || c.from.length !== 3 || c.to.length !== 3) {
      diagnostics.push({
        severity: "error",
        line: 0,
        msg: `component[${i}] "${c.name ?? "?"}" missing from/to (3-array)`,
      });
    }
    if (c.from && c.to) {
      for (let j = 0; j < 3; j++) {
        if (c.from[j] > c.to[j]) {
          diagnostics.push({
            severity: "error",
            line: 0,
            msg: `component[${i}] "${c.name}" has inverted bounds axis ${j}`,
          });
        }
      }
    }
  });

  const structure: ParsedStructure = {
    id: String(parsed.id ?? "<unknown>"),
    schemaVersion: Number(parsed.schema_version ?? 1),
    transforms: Array.isArray(parsed.transforms) ? parsed.transforms.map((t: any) => t.id ?? "?") : [],
    components: components.map((c: any) => ({
      kind: c.kind ?? "point",
      name: String(c.name ?? "?"),
      from: c.from ?? [0, 0, 0],
      to: c.to ?? [0, 0, 0],
      target: c.target ?? "cumulus:block",
      predicate: c.predicate ?? "",
      capture: c.capture,
    })),
    contextRules: Array.isArray(parsed.context_rules)
      ? parsed.context_rules.map((r: any) => ({ name: r.name ?? "?", expression: r.expression ?? "" }))
      : [],
  };

  return { ok: diagnostics.length === 0, structure, diagnostics };
}

// ============================================================
// store
// ============================================================

export const useDataDriven = create<State>((set, get) => ({
  rawJson: JSON_PRESETS.forge.json,
  structureId: "cumulus:mini_forge",
  parsed: null,
  displayed: null,
  diagnostics: [],
  stage: "live",
  stageProgress: 1,
  log: [
    { ts: Date.now() - 5000, stage: "live", level: "ok", msg: "initial load complete · cumulus:mini_forge live" },
  ],
  reloads: 1,
  lastReloadMs: 0,

  setRawJson: (s) => set({ rawJson: s }),

  loadPreset: (id) => {
    const p = JSON_PRESETS[id];
    if (!p) return;
    const entry: ReloadLogEntry = {
      ts: Date.now(),
      stage: "idle",
      level: "info",
      msg: `preset "${id}" loaded into editor`,
    };
    set((s) => ({
      rawJson: p.json,
      stage: "idle",
      stageProgress: 0,
      log: [entry, ...s.log].slice(0, 20),
    }));
  },

  triggerReload: () => {
    if (get().stage !== "idle" && get().stage !== "live") return;
    const start = performance.now();
    const raw = get().rawJson;

    const entry: ReloadLogEntry = {
      ts: Date.now(),
      stage: "scanning",
      level: "info",
      msg: "scanning data/*/cumulus/structures/*.json",
    };
    set((s) => ({
      stage: "scanning",
      stageProgress: 0,
      log: [entry, ...s.log].slice(0, 20),
    }));

    const runStage = (
      stage: ReloadStage,
      delay: number,
      msg: string,
      level: "info" | "ok" | "warn" | "error" = "info",
      next?: () => void
    ) => {
      setTimeout(() => {
        const e: ReloadLogEntry = { ts: Date.now(), stage, level, msg };
        set((s) => ({
          stage,
          stageProgress: 0,
          log: [e, ...s.log].slice(0, 20),
        }));
        let p = 0;
        const interval = setInterval(() => {
          p += 0.15;
          if (p >= 1) {
            clearInterval(interval);
            set({ stageProgress: 1 });
            if (next) next();
          } else {
            set({ stageProgress: p });
          }
        }, delay / 8);
      }, delay);
    };

    // scanning → parsing
    runStage("scanning", 200, "found 1 asset", "ok", () => {
      const { parsed, diagnostics } = parseJsonWithDiagnostics(raw);
      if (diagnostics.length > 0) {
        runStage(
          "parsing",
          300,
          `JSON.parse failed: ${diagnostics[0].msg} (line ${diagnostics[0].line})`,
          "error"
        );
        setTimeout(() => {
          const e: ReloadLogEntry = {
            ts: Date.now(),
            stage: "idle",
            level: "error",
            msg: "reload aborted · keeping previous snapshot",
          };
          set((s) => ({
            stage: "idle",
            diagnostics,
            displayed: s.displayed,
            lastReloadMs: performance.now() - start,
            log: [e, ...s.log].slice(0, 20),
          }));
        }, 600);
        return;
      }
      runStage("parsing", 300, `parsed 1 resource into map structure`, "ok", () => {
        const { ok, structure, diagnostics: valDiags } = validateStructure(parsed);
        if (!ok) {
          runStage(
            "validating",
            300,
            `validation failed: ${valDiags[0].msg}`,
            "error"
          );
          setTimeout(() => {
            const e: ReloadLogEntry = {
              ts: Date.now(),
              stage: "idle",
              level: "error",
              msg: "reload aborted · diagnostic emitted",
            };
            set((s) => ({
              stage: "idle",
              diagnostics: valDiags,
              lastReloadMs: performance.now() - start,
              log: [e, ...s.log].slice(0, 20),
            }));
          }, 600);
          return;
        }
        runStage("validating", 300, `${valDiags.length} diagnostic(s) · schema ok`, "ok", () => {
          runStage("indexing", 250, `RuntimeAssetRepository reindex · id=${structure!.id}`, "ok", () => {
            runStage("registering", 250,
              `RuntimeAssetEngineBridge register · ${structure!.components.length} components`,
              "ok",
              () => {
                runStage("live", 200, `${structure!.id} is LIVE · engine plan rebuilt`, "ok");
                set({
                  parsed: structure,
                  displayed: structure,
                  structureId: structure!.id,
                  diagnostics: [],
                  reloads: get().reloads + 1,
                  lastReloadMs: performance.now() - start,
                });
              }
            );
          });
        });
      });
    });
  },

  resetStage: () => set({ stage: "idle", stageProgress: 0 }),
}));
