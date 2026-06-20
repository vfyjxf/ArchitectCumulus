// ============================================================
// 11 类标准 DiagnosticCode 及详细示例
// ============================================================

export interface DiagnosticCodeDoc {
  code: string;
  severity: "ERROR" | "WARN" | "INFO";
  color: string;
  description: string;
  example: string;
}

export const DIAGNOSTIC_CODES: DiagnosticCodeDoc[] = [
  {
    code: "MISSING_TARGET",
    severity: "ERROR",
    color: "#fb7185",
    description: "目标位置上根本没有任何东西（air），或目标 entity / capability 不存在。",
    example: `point "casing_n" at (-1,0,-1) expected cumulus:casing,
actual: minecraft:air`,
  },
  {
    code: "WRONG_TARGET_TYPE",
    severity: "ERROR",
    color: "#fb7185",
    description: "目标存在但类型不匹配（比如需要 block_entity 但那里只有普通 block）。",
    example: `point "input_bus" expected cumulus:block_entity
actual: cumulus:casing (block)`,
  },
  {
    code: "PREDICATE_FAILED",
    severity: "ERROR",
    color: "#fbbf24",
    description: "类型对了但 predicate 不满足。最常见：tag 不对、NBT 字段缺失、state property 错误。",
    example: `predicate "cumulus:heat_casing" on point "casing_n"
tag missing: #cumulus:heat_resistant`,
  },
  {
    code: "CONTEXT_CAPTURE_FAILED",
    severity: "ERROR",
    color: "#fbbf24",
    description: "本应在某 point 捕获一个值（如 facing），但 point 本身失败或属性缺失。",
    example: `capture "controller.facing" required by rule ctx#0,
but point "controller" did not match`,
  },
  {
    code: "CONTEXT_DERIVATION_FAILED",
    severity: "ERROR",
    color: "#fbbf24",
    description: "ContextRuleDescriptor 表达式无效或输入值类型不对。",
    example: `derive "level" from "controller.level",
but "controller.level" is not a number`,
  },
  {
    code: "TRANSFORM_REJECTED",
    severity: "WARN",
    color: "#22d3ee",
    description: "所有声明的 transform 候选都不能让结构成型。结构 ID 可能错或者 anchor 选错。",
    example: `candidates: identity, horizontalFacing(n,e,s,w), rotateY90
all rejected: no transform satisfies all points`,
  },
  {
    code: "COMPONENT_CONFLICT",
    severity: "ERROR",
    color: "#fb7185",
    description: "两个 required point 在同一相对坐标 + 同一 target type 上重叠。在 DSL 编译期就会抛 StructureDefinitionException。",
    example: `point "a" at (0,0,0) Targets.block
point "b" at (0,0,0) Targets.block
=> duplicate target key`,
  },
  {
    code: "DEPENDENCY_UNRESOLVED",
    severity: "ERROR",
    color: "#f0abfc",
    description: "子结构引用了不存在的 structure id。",
    example: `substructure "left_arm" references cumulus:arm
but cumulus:arm is not registered`,
  },
  {
    code: "SHARED_PART_CONFLICT",
    severity: "ERROR",
    color: "#f0abfc",
    description: "两个父结构都声称对同一个 child occurrence 拥有所有权。",
    example: `parent "factory_a" claims child occurrence #arm_3
parent "factory_b" also claims #arm_3
=> ownership conflict`,
  },
  {
    code: "UNLOADED_AREA",
    severity: "INFO",
    color: "#a3e635",
    description: "目标在未加载的区块里。引擎不强制载入，而是发一个 DataRequest，等区块加载后重试。",
    example: `point "remote_sensor" at (50,0,0)
chunk not loaded, requesting via RequestLedger`,
  },
  {
    code: "RUNTIME_ONLY_UNAVAILABLE",
    severity: "WARN",
    color: "#22d3ee",
    description: "predicate 是 runtime-only（registry id），但当前注册表里没有这个 id。",
    example: `predicate registry id "cumulus:custom_rune"
no matcher registered under this id`,
  },
];

// 诊断图节点：模拟一个真实失败场景的图结构
export interface DiagNode {
  id: string;
  code: string;
  label: string;
  severity: "ERROR" | "WARN" | "INFO";
  details: string;
  fixHint?: string;
}

export interface DiagEdge {
  from: string;
  to: string;
  label: string;
}

export const DIAG_NODES: DiagNode[] = [
  {
    id: "root",
    code: "MULTIBLOCK_INVALID",
    label: "machine #3 invalid",
    severity: "ERROR",
    details: "Structure cumulus:gastronomic_forge at (10,64,10) failed validation",
  },
  {
    id: "rule-controller",
    code: "RULE_FAILED",
    label: "controller rule",
    severity: "ERROR",
    details: "Rule group 'core' failed",
  },
  {
    id: "point-ctrl",
    code: "PREDICATE_FAILED",
    label: "controller point",
    severity: "ERROR",
    details: "predicate cumulus:forge_controller did not match",
    fixHint: "place cumulus:forge_controller at (10,65,10)",
  },
  {
    id: "evidence-ctrl",
    code: "EVIDENCE",
    label: "actual block",
    severity: "INFO",
    details: "minecraft:blast_furnace found",
  },
  {
    id: "rule-casing",
    code: "RULE_FAILED",
    label: "casing rule",
    severity: "ERROR",
    details: "Rule group 'shell' failed",
  },
  {
    id: "point-casing-n",
    code: "MISSING_TARGET",
    label: "casing_north point",
    severity: "ERROR",
    details: "no block at (10,64,9)",
    fixHint: "place any block tagged #cumulus:heat_casing",
  },
  {
    id: "point-casing-e",
    code: "PREDICATE_FAILED",
    label: "casing_east point",
    severity: "ERROR",
    details: "block found but tag missing",
    fixHint: "replace with heat-resistant casing",
  },
  {
    id: "evidence-casing-e",
    code: "EVIDENCE",
    label: "actual block",
    severity: "INFO",
    details: "minecraft:stone [#minecraft:mineable/pickaxe]",
  },
  {
    id: "rule-energy",
    code: "RULE_FAILED",
    label: "energy rule",
    severity: "WARN",
    details: "Energy port has no capability",
  },
  {
    id: "point-energy",
    code: "MISSING_TARGET",
    label: "energy_in point",
    severity: "WARN",
    details: "no IEnergyStorage exposed at (10,64,11)",
    fixHint: "attach an energy conduit to the south face",
  },
  {
    id: "rule-remote",
    code: "RULE_PENDING",
    label: "remote_sensor rule",
    severity: "INFO",
    details: "section (12,0,12) not loaded",
  },
  {
    id: "point-remote",
    code: "UNLOADED_AREA",
    label: "remote_sensor point",
    severity: "INFO",
    details: "DataRequest emitted, will retry on load",
  },
];

export const DIAG_EDGES: DiagEdge[] = [
  { from: "root", to: "rule-controller", label: "contains" },
  { from: "root", to: "rule-casing", label: "contains" },
  { from: "root", to: "rule-energy", label: "contains" },
  { from: "root", to: "rule-remote", label: "contains" },
  { from: "rule-controller", to: "point-ctrl", label: "failed at" },
  { from: "point-ctrl", to: "evidence-ctrl", label: "evidence" },
  { from: "rule-casing", to: "point-casing-n", label: "failed at" },
  { from: "rule-casing", to: "point-casing-e", label: "failed at" },
  { from: "point-casing-e", to: "evidence-casing-e", label: "evidence" },
  { from: "rule-energy", to: "point-energy", label: "failed at" },
  { from: "rule-remote", to: "point-remote", label: "pending" },
];
