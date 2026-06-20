// ============================================================
// 真实游戏场景 → Cumulus API 映射
// 每个场景包含结构化的 3D 方块布局 + 每个方块对应的 API 抽象
// 点击/悬停方块 → 高亮它映射到哪个 point/target/predicate
// ============================================================

export type BlockRole =
  | "controller"
  | "casing"
  | "block_entity"
  | "capability"
  | "entity"
  | "fluid"
  | "region"
  | "context";

export interface ScenarioBlock {
  /** 相对坐标 (x,y,z) */
  pos: [number, number, number];
  /** 尺寸（region 用大于 1 的值，普通方块 [1,1,1]） */
  size?: [number, number, number];
  role: BlockRole;
  /** 显示标签 */
  label: string;
  /** 对应的 point id / component name */
  pointId?: string;
  /** 对应的 target 类型 */
  target?: string;
  /** predicate（id 或 #tag） */
  predicate?: string;
  /** 是否有 capture */
  capture?: string;
  /** 该方块在 DSL 代码片段里的标识（用于 hover 联动高亮代码行） */
  dslTag?: string;
  /** 说明：这个方块在场景里的作用 */
  note?: string;
}

export interface GameScenario {
  id: string;
  title: string;
  mod: string;
  icon: string;
  accent: string;
  hook: string;
  multiblockShape: string;
  apiPieces: string[];
  targets: string[];
  pitfalls: string[];
  snippet: string;
  /** 结构化 3D 方块布局（驱动 3D 查看器 + API 映射联动） */
  blocks: ScenarioBlock[];
  /** DSL 片段中每行的注释标记（行号 → dslTag），用于 hover 高亮 */
  dslLines?: { tag: string; text: string }[];
}

// 方块颜色映射
export const ROLE_COLOR: Record<BlockRole, string> = {
  controller: "#7c5cff",
  casing: "#fb7185",
  block_entity: "#22d3ee",
  capability: "#fbbf24",
  entity: "#a3e635",
  fluid: "#38bdf8",
  region: "#fb923c",
  context: "#f0abfc",
};

export const ROLE_LABEL: Record<BlockRole, string> = {
  controller: "Controller",
  casing: "Casing",
  block_entity: "BlockEntity",
  capability: "Capability",
  entity: "Entity",
  fluid: "Fluid",
  region: "Region",
  context: "Context",
};

/** 生成 hollow box 的 shell 点 */
function shell(name: string, from: [number, number, number], to: [number, number, number], predicate: string): ScenarioBlock[] {
  const out: ScenarioBlock[] = [];
  for (let x = from[0]; x <= to[0]; x++) {
    for (let y = from[1]; y <= to[1]; y++) {
      for (let z = from[2]; z <= to[2]; z++) {
        const onShell =
          x === from[0] || x === to[0] ||
          y === from[1] || y === to[1] ||
          z === from[2] || z === to[2];
        if (!onShell) continue;
        out.push({
          pos: [x, y, z],
          role: "casing",
          label: name,
          pointId: `${name}_${x}_${y}_${z}`,
          target: "cumulus:block",
          predicate,
          dslTag: "casing",
          note: `${name} 外壳方块 → Components.hollowBox 展开后的 point`,
        });
      }
    }
  }
  return out;
}

export const SCENARIOS: GameScenario[] = [
  // ============================================================
  {
    id: "gastronomic-forge",
    title: "巨型工业高炉",
    mod: "GregTech CEu / IE 风格",
    icon: "Flame",
    accent: "amber",
    hook: "3×4×3 中空外壳 + 1 个 controller + 多个 item/fluid/energy bus",
    multiblockShape: "3×4×3 hollowBox + 多个 capability port",
    apiPieces: [
      "Components.hollowBox",
      "Targets.block + block tag",
      "Targets.capability × N",
      "ContextRuleDescriptor.derive(facing)",
      "Transforms.horizontalFacing",
    ],
    targets: ["block", "capability"],
    pitfalls: [
      "外壳如果用 box 会产生 60+ point，必须用 hollowBox 只检查壳",
      "controller 朝向决定 bus side，必须用 context 派生 facing",
      "4 种 transform 候选要按 horizontalFacing 顺序短路",
    ],
    dslLines: [
      { tag: "header", text: "Structures.define(id(\"gastronomic_forge\"))" },
      { tag: "transform", text: "    .transforms(Transforms.horizontalFacing())" },
      { tag: "controller", text: "    .point(\"controller\", at(0,1,0), Targets.block()" },
      { tag: "controller", text: "      .where(PredicateDescriptors.id(\"cumulus:forge_controller\"))" },
      { tag: "controller", text: "      .capture(\"controller\"))" },
      { tag: "casing", text: "    .component(Components.hollowBox(\"casing\"," },
      { tag: "casing", text: "      at(-1,0,-1), at(1,3,1))" },
      { tag: "casing", text: "      .where(PredicateDescriptors.tag(\"cumulus:heat_casing\")))" },
      { tag: "energy", text: "    .point(\"energy_in\", at(0,0,1), Targets.capability(\"cumulus:energy\"))" },
      { tag: "derive", text: "    .context(ContextRuleDescriptor.derive(" },
      { tag: "derive", text: "        ContextKey.string(\"facing\")," },
      { tag: "derive", text: "        ContextSources.capture(\"controller\").property(\"facing\")))" },
      { tag: "footer", text: "    .build();" },
    ],
    snippet: `Structures.define(id("gastronomic_forge"))
    .transforms(Transforms.horizontalFacing())
    .point("controller", at(0,1,0), Targets.block()
      .where(PredicateDescriptors.id("cumulus:forge_controller"))
      .capture("controller"))
    .component(Components.hollowBox("casing",
      at(-1,0,-1), at(1,3,1))
      .where(PredicateDescriptors.tag("cumulus:heat_casing")))
    .point("energy_in", at(0,0,1), Targets.capability("cumulus:energy"))
    .context(ContextRuleDescriptor.derive(
        ContextKey.string("facing"),
        ContextSources.capture("controller").property("facing")))
    .build();`,
    blocks: [
      // controller
      {
        pos: [0, 1, 0], role: "controller", label: "controller",
        pointId: "controller", target: "cumulus:block",
        predicate: "cumulus:forge_controller", capture: "controller",
        dslTag: "controller",
        note: "controller 方块 → .point(\"controller\") + capture(\"controller\")，facing 从它的 block state 派生",
      },
      // energy bus
      {
        pos: [0, 0, 1], role: "capability", label: "energy_in",
        pointId: "energy_in", target: "cumulus:capability",
        predicate: "cumulus:energy", dslTag: "energy",
        note: "能量输入口 → Targets.capability(\"cumulus:energy\")，机器通过 capability 读能量",
      },
      // context derive (虚方块，标记 facing 派生)
      {
        pos: [0, 2, 0], role: "context", label: "facing ← controller",
        dslTag: "derive", pointId: "facing",
        note: "context 派生 → ContextSources.capture(\"controller\").property(\"facing\")，写入 RuntimeStructureContext 供机器读取",
      },
      // hollow box shell
      ...shell("casing", [-1, 0, -1], [1, 3, 1], "#cumulus:heat_casing"),
    ],
  },

  // ============================================================
  {
    id: "kinetic-assembly",
    title: "应力组装线",
    mod: "Create 风格",
    icon: "Cog",
    accent: "amber",
    hook: "线性 1×1×N 走带 + 中段工具站 + 末端应力总线",
    multiblockShape: "1×1×N box 走带 + 多 point 工具站 + 1 capability 总线",
    apiPieces: [
      "Components.box (linear)",
      "Targets.capability (stress)",
      "Targets.blockEntity (tool station)",
      "ContextRuleDescriptor (运行模式)",
    ],
    targets: ["block", "block_entity", "capability"],
    pitfalls: [
      "工具站可换，用 blockEntity + tag 而非固定 block id",
      "应力输入必须 expose capability，普通方块不算",
      "走带方向靠 horizontalFacing transform 自动旋转",
    ],
    snippet: `Structures.define(id("assembly_line"))
    .transforms(Transforms.horizontalFacing())
    .component(Components.box("belt", at(0,0,0), at(0,0,4)))
    .point("stress_in", at(0,0,0), Targets.capability("create:stress"))
    .point("tool_a", at(0,1,2), Targets.blockEntity()
      .where(PredicateDescriptors.tag("create:saw")))
    .context(ContextRuleDescriptor.derive(
        ContextKey.string("mode"),
        ContextSources.capture("stress_in").property("level")))
    .build();`,
    blocks: [
      {
        pos: [0, 0, 0], role: "capability", label: "stress_in",
        pointId: "stress_in", target: "cumulus:capability",
        predicate: "create:stress", dslTag: "stress",
        note: "应力输入 → Targets.capability(\"create:stress\")，读取旋转速度数值",
      },
      {
        pos: [0, 1, 2], role: "block_entity", label: "tool_a",
        pointId: "tool_a", target: "cumulus:block_entity",
        predicate: "#create:saw", dslTag: "tool",
        note: "工具站 → Targets.blockEntity() + tag，可替换不同刀具",
      },
      {
        pos: [0, 2, 0], role: "context", label: "mode ← stress_in",
        dslTag: "derive", pointId: "mode",
        note: "运行模式由应力等级派生 → derive(mode, capture(\"stress_in\").property(\"level\"))",
      },
      // belt (实心 box)
      { pos: [0, 0, 0], role: "casing", label: "belt", pointId: "belt_0_0_0", target: "cumulus:block", predicate: "create:belt", dslTag: "belt", note: "走带方块 → Components.box 展开" },
      { pos: [0, 0, 1], role: "casing", label: "belt", pointId: "belt_0_0_1", target: "cumulus:block", predicate: "create:belt", dslTag: "belt" },
      { pos: [0, 0, 2], role: "casing", label: "belt", pointId: "belt_0_0_2", target: "cumulus:block", predicate: "create:belt", dslTag: "belt" },
      { pos: [0, 0, 3], role: "casing", label: "belt", pointId: "belt_0_0_3", target: "cumulus:block", predicate: "create:belt", dslTag: "belt" },
      { pos: [0, 0, 4], role: "casing", label: "belt", pointId: "belt_0_0_4", target: "cumulus:block", predicate: "create:belt", dslTag: "belt" },
    ],
  },

  // ============================================================
  {
    id: "mana-reactor",
    title: "魔力塔阵",
    mod: "Botania 风格",
    icon: "Sparkles",
    accent: "magenta",
    hook: "中央 mana 池 + 4 个次级塔 + 阵法 region 净空",
    multiblockShape: "5×5×1 region 净空 + 5 个 point 塔 + 1 capability mana",
    apiPieces: [
      "Components.region",
      "Targets.capability(mana)",
      "Targets.entity (可选触发)",
      "ContextRuleDescriptor (玩家进度)",
    ],
    targets: ["block", "capability", "region"],
    pitfalls: [
      "阵法净空区必须用 region 不能展开成 25 个 point",
      "魔力输入按 capability 数值范围分级，不是简单 boolean",
      "若需要 nearby player 触发，entity target 设为 optional",
    ],
    snippet: `Structures.define(id("mana_reactor"))
    .component(Components.region("circle", at(-2,0,-2), at(2,0,2))
      .where(PredicateDescriptors.id("cumulus:air_or_rune")))
    .point("pool", at(0,0,0), Targets.capability("botania:mana"))
    .point("pillar_n", at(0,1,-2), Targets.block())
    .build();`,
    blocks: [
      {
        pos: [0, 0, 0], role: "capability", label: "pool",
        pointId: "pool", target: "cumulus:capability",
        predicate: "botania:mana", dslTag: "pool",
        note: "魔力池 → Targets.capability(\"botania:mana\")，读取 mana 储量数值",
      },
      {
        pos: [-2, 0, -2], role: "region", label: "circle (region)",
        size: [5, 1, 5], pointId: "circle", target: "cumulus:region",
        predicate: "cumulus:air_or_rune", dslTag: "region",
        note: "阵法净空区 → Components.region，检查区域内全是 air/rune 而非逐点检查",
      },
      { pos: [0, 1, -2], role: "casing", label: "pillar_n", pointId: "pillar_n", target: "cumulus:block", predicate: "botania:livingrock", dslTag: "pillar", note: "北侧塔 → .point(\"pillar_n\")" },
      { pos: [0, 1, 2], role: "casing", label: "pillar_s", pointId: "pillar_s", target: "cumulus:block", predicate: "botania:livingrock", dslTag: "pillar", note: "南侧塔" },
      { pos: [-2, 1, 0], role: "casing", label: "pillar_w", pointId: "pillar_w", target: "cumulus:block", predicate: "botania:livingrock", dslTag: "pillar", note: "西侧塔" },
      { pos: [2, 1, 0], role: "casing", label: "pillar_e", pointId: "pillar_e", target: "cumulus:block", predicate: "botania:livingrock", dslTag: "pillar", note: "东侧塔" },
    ],
  },

  // ============================================================
  {
    id: "ae2-controller",
    title: "ME 控制器簇",
    mod: "AE2 风格",
    icon: "Network",
    accent: "cyan",
    hook: "8 个 controller 方块直接相邻 + region 净空 + 任意 N 个 iface",
    multiblockShape: "2×2×2 controller cluster + region 净空",
    apiPieces: [
      "Components.box (controller cluster)",
      "Targets.blockEntity (controller)",
      "Targets.region (净空)",
      "ContextRuleDescriptor (network_id)",
    ],
    targets: ["block_entity", "block", "region"],
    pitfalls: [
      "controller 之间必须直接相邻，且 blockEntity NBT 持有相同 network id",
      "净空 region 防止 5×5×5 内有遮挡，影响 wireless range",
      "network_id 通过 blockEntity NBT 读取后用 context 派生",
    ],
    snippet: `Structures.define(id("ae2_cluster"))
    .component(Components.box("ctrl", at(0,0,0), at(1,1,1))
      .where(PredicateDescriptors.id("ae2:controller")))
    .component(Components.region("wireless", at(-2,-2,-2), at(3,3,3)))
    .context(ContextRuleDescriptor.derive(
        ContextKey.string("net"),
        ContextSources.capture("ctrl").property("network")))
    .build();`,
    blocks: [
      // 2×2×2 controller cluster
      { pos: [0, 0, 0], role: "block_entity", label: "ctrl", pointId: "ctrl_0_0_0", target: "cumulus:block_entity", predicate: "ae2:controller", dslTag: "ctrl", note: "ME controller → Targets.blockEntity + id，NBT 持有 network id" },
      { pos: [1, 0, 0], role: "block_entity", label: "ctrl", pointId: "ctrl_1_0_0", target: "cumulus:block_entity", predicate: "ae2:controller", dslTag: "ctrl" },
      { pos: [0, 1, 0], role: "block_entity", label: "ctrl", pointId: "ctrl_0_1_0", target: "cumulus:block_entity", predicate: "ae2:controller", dslTag: "ctrl" },
      { pos: [1, 1, 0], role: "block_entity", label: "ctrl", pointId: "ctrl_1_1_0", target: "cumulus:block_entity", predicate: "ae2:controller", dslTag: "ctrl" },
      { pos: [0, 0, 1], role: "block_entity", label: "ctrl", pointId: "ctrl_0_0_1", target: "cumulus:block_entity", predicate: "ae2:controller", dslTag: "ctrl" },
      { pos: [1, 0, 1], role: "block_entity", label: "ctrl", pointId: "ctrl_1_0_1", target: "cumulus:block_entity", predicate: "ae2:controller", dslTag: "ctrl" },
      { pos: [0, 1, 1], role: "block_entity", label: "ctrl", pointId: "ctrl_0_1_1", target: "cumulus:block_entity", predicate: "ae2:controller", dslTag: "ctrl" },
      { pos: [1, 1, 1], role: "block_entity", label: "ctrl", pointId: "ctrl_1_1_1", target: "cumulus:block_entity", predicate: "ae2:controller", dslTag: "ctrl" },
      // wireless region
      {
        pos: [0.5, 0.5, 0.5], role: "region", label: "wireless (region)",
        size: [6, 6, 6], pointId: "wireless", target: "cumulus:region",
        predicate: "cumulus:air", dslTag: "region",
        note: "无线净空区 → Components.region，确保 controller 周围无遮挡",
      },
      // context
      {
        pos: [0.5, 2.5, 0.5], role: "context", label: "net ← ctrl",
        dslTag: "derive", pointId: "net",
        note: "network id 从 controller BE NBT 派生 → derive(net, capture(\"ctrl\").property(\"network\"))",
      },
    ],
  },

  // ============================================================
  // 中等难度场景
  // ============================================================
  {
    id: "fluid-tank",
    title: "多层流体储罐",
    mod: "Mekanism / Thermal 风格",
    icon: "Droplets",
    accent: "cyan",
    hook: "1×N×1 竖直中空柱 + 底部 fluid port + 顶部 vent（optional）",
    multiblockShape: "1×1×N hollowBox 竖柱 + fluid capability + optional vent",
    apiPieces: [
      "Components.hollowBox (竖直)",
      "Targets.fluid",
      "PointRequirement.optional (vent)",
      "CheckPhase.repeatRange (动态高度)",
      "ContextPlan (容量计算)",
    ],
    targets: ["block", "fluid"],
    pitfalls: [
      "储罐高度不固定——用 repeatRange 让玩家可搭任意高度",
      "vent 是 optional point，缺失时仍 valid（但不排汽）",
      "容量 = 层数 × 每层内部体积，用 contextPlan 的 compute 算",
    ],
    snippet: `Structures.define(id("fluid_tank"))
    .point("drain", at(0,0,0), Targets.fluid()
      .where(PredicateDescriptors.id("cumulus:drain")))
    .checkPhase(CheckPhases.repeatRange(
        "wall", Components.hollowBox("wall", at(-1,0,-1), at(1,0,1)),
        Axis.y, 2, 8, TANK_HEIGHT))
    .build();`,
    blocks: [
      {
        pos: [0, 0, 0], role: "fluid", label: "drain",
        pointId: "drain", target: "cumulus:fluid",
        predicate: "cumulus:drain", dslTag: "drain",
        note: "流体排液口 → Targets.fluid + id 匹配 drain 方块",
      },
      // 3 层 wall（shell）
      ...shell("wall", [-1, 0, -1], [1, 2, 1], "cumulus:tank_wall"),
      // context: TANK_HEIGHT
      {
        pos: [0, 3, 0], role: "context", label: "TANK_HEIGHT ← repeatRange",
        dslTag: "derive", pointId: "tank_height",
        note: "repeatRange 探测 2~8 层，实际匹配层数写入 TANK_HEIGHT context key",
      },
    ],
  },
  {
    id: "conveyor-array",
    title: "传送带并联阵列",
    mod: "Industrial Foregoing / Thermal 风格",
    icon: "ConveyorBelt",
    accent: "amber",
    hook: "3 条平行 1×1×N 传送带 + 末端 item collector + 可选 speed upgrade",
    multiblockShape: "3×1×N box 并联 + blockEntity collector + optional upgrade slot",
    apiPieces: [
      "Components.box × 3 (并联)",
      "Targets.blockEntity (collector)",
      "PointRequirement.optional (upgrade)",
      "ContextPlan (产出速率计算)",
    ],
    targets: ["block", "block_entity"],
    pitfalls: [
      "3 条传送带可以不同长度，用 optional 尾端处理",
      "upgrade slot 缺失时降级运行，用 optional point",
      "产出速率 = base × upgrade_count，用 contextPlan compute",
    ],
    snippet: `Structures.define(id("conveyor_array"))
    .component(Components.box("belt_a", at(0,0,0), at(0,0,5)))
    .component(Components.box("belt_b", at(2,0,0), at(2,0,5)))
    .component(Components.box("belt_c", at(4,0,0), at(4,0,5)))
    .point("collector", at(2,1,5), Targets.blockEntity()
      .where(PredicateDescriptors.id("thermal:collector")))
    .point("upgrade", at(2,2,5), Targets.blockEntity()
      .where(PredicateDescriptors.tag("thermal:upgrade")))
    .build();`,
    blocks: [
      {
        pos: [2, 1, 5], role: "block_entity", label: "collector",
        pointId: "collector", target: "cumulus:block_entity",
        predicate: "thermal:collector", dslTag: "collector",
        note: "收集器 → Targets.blockEntity + id 匹配",
      },
      {
        pos: [2, 2, 5], role: "block_entity", label: "upgrade (optional)",
        pointId: "upgrade", target: "cumulus:block_entity",
        predicate: "#thermal:upgrade", dslTag: "upgrade",
        note: "升级槽 → optional point，缺失不影响 valid",
      },
      // belt A
      ...Array.from({length: 6}, (_, z) => ({
        pos: [0, 0, z] as [number, number, number], role: "casing" as BlockRole,
        label: "belt_a", pointId: `belt_a_0_0_${z}`, target: "cumulus:block",
        predicate: "thermal:conveyor", dslTag: "belt",
        note: z === 0 ? "传送带 A → Components.box 展开" : undefined,
      })),
      // belt B
      ...Array.from({length: 6}, (_, z) => ({
        pos: [2, 0, z] as [number, number, number], role: "casing" as BlockRole,
        label: "belt_b", pointId: `belt_b_2_0_${z}`, target: "cumulus:block",
        predicate: "thermal:conveyor", dslTag: "belt",
      })),
      // belt C
      ...Array.from({length: 6}, (_, z) => ({
        pos: [4, 0, z] as [number, number, number], role: "casing" as BlockRole,
        label: "belt_c", pointId: `belt_c_4_0_${z}`, target: "cumulus:block",
        predicate: "thermal:conveyor", dslTag: "belt",
      })),
    ],
  },

  // ============================================================
  // 复杂场景
  // ============================================================
  {
    id: "vacuum-freezer",
    title: "真空冷冻机",
    mod: "GregTech CEu 风格",
    icon: "Snowflake",
    accent: "cyan",
    hook: "3×3×3 中空外壳 + controller + 2 fluid port（输入/输出） + energy + item I/O + 温度传感器",
    multiblockShape: "3×3×3 hollowBox + 1 controller + 2 fluid + 1 energy + 2 item_bus + 1 sensor",
    apiPieces: [
      "Components.hollowBox",
      "Targets.fluid × 2 (输入/输出独立)",
      "Targets.capability (energy)",
      "Targets.blockEntity × 2 (item bus)",
      "ContextRuleDescriptor (温度)",
      "CheckPhase (液位约束)",
    ],
    targets: ["block", "fluid", "capability", "block_entity"],
    pitfalls: [
      "输入/输出 fluid port 必须分开声明，机器逻辑区分方向",
      "温度传感器是 blockEntity，读取 NBT 里的 temperature 属性",
      "冷却液液位约束用 CountConstraint 确保至少有 1 个输入口",
    ],
    snippet: `Structures.define(id("vacuum_freezer"))
    .transforms(Transforms.horizontalFacing())
    .point("controller", at(0,1,0), Targets.block()
      .where(PredicateDescriptors.id("cumulus:freezer_controller"))
      .capture("controller"))
    .point("coolant_in", at(-1,0,0), Targets.fluid()
      .where(PredicateDescriptors.id("cumulus:coolant_input")))
    .point("coolant_out", at(1,0,0), Targets.fluid()
      .where(PredicateDescriptors.id("cumulus:coolant_output")))
    .point("energy", at(0,0,-1), Targets.capability("cumulus:energy"))
    .point("item_in", at(-1,1,1), Targets.blockEntity()
      .where(PredicateDescriptors.id("cumulus:item_bus")))
    .point("item_out", at(1,1,1), Targets.blockEntity()
      .where(PredicateDescriptors.id("cumulus:item_bus")))
    .point("sensor", at(0,2,0), Targets.blockEntity()
      .where(PredicateDescriptors.tag("cumulus:temperature_sensor")))
    .component(Components.hollowBox("casing",
      at(-1,0,-1), at(1,2,1))
      .where(PredicateDescriptors.tag("cumulus:frost_casing"))
      .group("casing"))
    .context(ContextRuleDescriptor.derive(
        ContextKey.string("temperature"),
        ContextSources.capture("sensor").property("temperature")))
    .build();`,
    blocks: [
      {
        pos: [0, 1, 0], role: "controller", label: "controller",
        pointId: "controller", target: "cumulus:block",
        predicate: "cumulus:freezer_controller", capture: "controller", dslTag: "controller",
        note: "冷冻机控制器 → capture 后用于 facing 派生",
      },
      {
        pos: [-1, 0, 0], role: "fluid", label: "coolant_in",
        pointId: "coolant_in", target: "cumulus:fluid",
        predicate: "cumulus:coolant_input", dslTag: "fluid_in",
        note: "冷却液输入 → Targets.fluid，机器读 fluid tank 液位",
      },
      {
        pos: [1, 0, 0], role: "fluid", label: "coolant_out",
        pointId: "coolant_out", target: "cumulus:fluid",
        predicate: "cumulus:coolant_output", dslTag: "fluid_out",
        note: "冷却液输出 → 独立声明，输出方向与输入不同",
      },
      {
        pos: [0, 0, -1], role: "capability", label: "energy",
        pointId: "energy", target: "cumulus:capability",
        predicate: "cumulus:energy", dslTag: "energy",
        note: "能量输入 → Targets.capability",
      },
      {
        pos: [-1, 1, 1], role: "block_entity", label: "item_in",
        pointId: "item_in", target: "cumulus:block_entity",
        predicate: "cumulus:item_bus", dslTag: "item_in",
        note: "物品输入 bus → Targets.blockEntity",
      },
      {
        pos: [1, 1, 1], role: "block_entity", label: "item_out",
        pointId: "item_out", target: "cumulus:block_entity",
        predicate: "cumulus:item_bus", dslTag: "item_out",
        note: "物品输出 bus → 独立声明区分输入输出",
      },
      {
        pos: [0, 2, 0], role: "block_entity", label: "sensor",
        pointId: "sensor", target: "cumulus:block_entity",
        predicate: "#cumulus:temperature_sensor", dslTag: "sensor",
        note: "温度传感器 → 读取 NBT temperature 属性，派生到 context",
      },
      {
        pos: [0, 3, 0], role: "context", label: "temperature ← sensor",
        dslTag: "derive", pointId: "temperature",
        note: "温度派生 → capture(\"sensor\").property(\"temperature\")",
      },
      ...shell("casing", [-1, 0, -1], [1, 2, 1], "#cumulus:frost_casing"),
    ],
  },
  {
    id: "ore-processing-plant",
    title: "矿石处理厂",
    mod: "IE / Mekanism 混合风格",
    icon: "Factory",
    accent: "amber",
    hook: "5×3×5 双层结构：底层粉碎机 + 顶层洗矿 + 中央传送 + 4 个 capability port",
    multiblockShape: "5×3×5 分层结构 + 4 capability + 1 blockEntity + 子结构传送带",
    apiPieces: [
      "Components.hollowBox × 2 (双层)",
      "Targets.capability × 4",
      "Substructure (传送带)",
      "ContextPlan (产出计算)",
      "CountConstraint (port 限制)",
    ],
    targets: ["block", "capability", "block_entity"],
    pitfalls: [
      "双层结构需要两个独立 hollowBox（上下不连通）",
      "传送带作为子结构独立检测，通过 substructure 关联",
      "4 个 port 用 CountConstraint 限制数量（防超载）",
    ],
    snippet: `Structures.define(id("ore_plant"))
    .point("controller", at(0,0,2), Targets.block()
      .where(PredicateDescriptors.id("cumulus:plant_controller"))
      .capture("controller"))
    .point("energy", at(-2,0,0), Targets.capability("cumulus:energy"))
    .point("water_in", at(2,0,0), Targets.capability("cumulus:fluid"))
    .point("item_in", at(-2,0,2), Targets.capability("cumulus:items"))
    .point("item_out", at(2,0,2), Targets.capability("cumulus:items"))
    .component(Components.hollowBox("crusher_floor",
      at(-2,0,-2), at(2,0,2))
      .where(PredicateDescriptors.tag("cumulus:machine_frame"))
      .group("crusher"))
    .component(Components.hollowBox("washer_floor",
      at(-2,2,-2), at(2,2,2))
      .where(PredicateDescriptors.tag("cumulus:washer_frame"))
      .group("washer"))
    .component(Components.substructure("conveyor",
      id("cumulus:conveyor_belt"), at(0,1,0)))
    .build();`,
    blocks: [
      {
        pos: [0, 0, 2], role: "controller", label: "controller",
        pointId: "controller", target: "cumulus:block",
        predicate: "cumulus:plant_controller", capture: "controller", dslTag: "controller",
        note: "处理厂主控 → capture 后传给子结构",
      },
      {
        pos: [-2, 0, 0], role: "capability", label: "energy",
        pointId: "energy", target: "cumulus:capability",
        predicate: "cumulus:energy", dslTag: "energy",
        note: "能量口 → capability",
      },
      {
        pos: [2, 0, 0], role: "capability", label: "water_in",
        pointId: "water_in", target: "cumulus:capability",
        predicate: "cumulus:fluid", dslTag: "water",
        note: "水输入 → capability（fluid 类型）",
      },
      {
        pos: [-2, 0, 2], role: "capability", label: "item_in",
        pointId: "item_in", target: "cumulus:capability",
        predicate: "cumulus:items", dslTag: "item_in",
        note: "矿石输入 → items capability",
      },
      {
        pos: [2, 0, 2], role: "capability", label: "item_out",
        pointId: "item_out", target: "cumulus:capability",
        predicate: "cumulus:items", dslTag: "item_out",
        note: "产物输出 → items capability",
      },
      {
        pos: [0, 1, 0], role: "block_entity", label: "conveyor (sub)",
        pointId: "conveyor", target: "cumulus:block_entity",
        predicate: "cumulus:conveyor_belt", dslTag: "sub",
        note: "传送带子结构 → Components.substructure，独立检测后合并 context",
      },
      // crusher floor shell (y=0, 5×5 ring)
      ...shell("crusher_floor", [-2, 0, -2], [2, 0, 2], "#cumulus:machine_frame"),
      // washer floor shell (y=2, 5×5 ring)
      ...shell("washer_floor", [-2, 2, -2], [2, 2, 2], "#cumulus:washer_frame"),
    ],
  },
  {
    id: "reactor-multi-zone",
    title: "多区域反应堆",
    mod: "NuclearCraft / Extreme Reactors 风格",
    icon: "Radiation",
    accent: "rose",
    hook: "7×7×7 分区反应堆：燃料区 + 冷却区 + 屏蔽层 + 控制棒 + 热交换器 + 能量输出",
    multiblockShape: "7×7×7 分区 + 4 个 controller + 多组 casing + 多 capability + region 净空",
    apiPieces: [
      "Components.hollowBox × 3 (分层外壳)",
      "Components.region (燃料净空区)",
      "Targets.capability × 3 (energy/coolant/fuel)",
      "Targets.blockEntity (control rod)",
      "CountConstraint (冷却器最少数量)",
      "CheckPhase (温度安全检测)",
      "ContextPlan (能量产出计算)",
    ],
    targets: ["block", "capability", "block_entity", "region"],
    pitfalls: [
      "燃料区必须用 region（不展开为 point），否则方块过多",
      "冷却器至少 N 个，否则反应堆过热——用 CountConstraint 强制",
      "控制棒是 blockEntity，读取插入深度（0~100%）决定反应速率",
      "多层外壳：外层屏蔽 + 中层冷却 + 内层燃料腔",
    ],
    snippet: `Structures.define(id("multi_zone_reactor"))
    .transforms(Transforms.horizontalFacing())
    .point("controller", at(0,3,0), Targets.block()
      .where(PredicateDescriptors.id("cumulus:reactor_controller"))
      .capture("controller"))
    .point("energy_out", at(3,3,3), Targets.capability("cumulus:energy"))
    .point("coolant_in", at(-3,0,0), Targets.capability("cumulus:fluid"))
    .point("fuel_in", at(0,0,-3), Targets.capability("cumulus:fuel"))
    .point("control_rod", at(0,4,0), Targets.blockEntity()
      .where(PredicateDescriptors.id("cumulus:control_rod")))
    .component(Components.hollowBox("shielding",
      at(-3,0,-3), at(3,6,3))
      .where(PredicateDescriptors.tag("cumulus:lead_shielding"))
      .group("shield"))
    .component(Components.hollowBox("coolant_jacket",
      at(-2,1,-2), at(2,5,2))
      .where(PredicateDescriptors.tag("cumulus:coolant_cell"))
      .group("coolant"))
    .component(Components.region("fuel_chamber",
      at(-1,2,-1), at(1,4,1))
      .where(PredicateDescriptors.id("cumulus:reactor_fuel")))
    .checkPhase(ctx -> PhaseResult.of(
      List.of(),
      List.of(CountConstraint.atLeast(
          StructurePredicates.group("coolant"), 8))))
    .build();`,
    blocks: [
      {
        pos: [0, 3, 0], role: "controller", label: "controller",
        pointId: "controller", target: "cumulus:block",
        predicate: "cumulus:reactor_controller", capture: "controller", dslTag: "controller",
        note: "反应堆主控 → capture 后派生 facing + 反应速率",
      },
      {
        pos: [3, 3, 3], role: "capability", label: "energy_out",
        pointId: "energy_out", target: "cumulus:capability",
        predicate: "cumulus:energy", dslTag: "energy",
        note: "能量输出口 → capability，机器从这读发电量",
      },
      {
        pos: [-3, 0, 0], role: "capability", label: "coolant_in",
        pointId: "coolant_in", target: "cumulus:capability",
        predicate: "cumulus:fluid", dslTag: "coolant",
        note: "冷却液输入 → capability（fluid 类型）",
      },
      {
        pos: [0, 0, -3], role: "capability", label: "fuel_in",
        pointId: "fuel_in", target: "cumulus:capability",
        predicate: "cumulus:fuel", dslTag: "fuel",
        note: "燃料输入 → capability（fuel 类型）",
      },
      {
        pos: [0, 4, 0], role: "block_entity", label: "control_rod",
        pointId: "control_rod", target: "cumulus:block_entity",
        predicate: "cumulus:control_rod", dslTag: "rod",
        note: "控制棒 → blockEntity，读取插入深度（0~100%）",
      },
      // shielding shell (7×7×7 outer)
      ...shell("shielding", [-3, 0, -3], [3, 6, 3], "#cumulus:lead_shielding"),
      // coolant jacket shell (5×5×5 mid, y=1~5)
      ...shell("coolant_jacket", [-2, 1, -2], [2, 5, 2], "#cumulus:coolant_cell"),
      // fuel chamber region
      {
        pos: [0, 3, 0], role: "region", label: "fuel_chamber",
        size: [3, 3, 3], pointId: "fuel_chamber", target: "cumulus:region",
        predicate: "cumulus:reactor_fuel", dslTag: "region",
        note: "燃料腔 → Components.region，检查 3×3×3 内全是燃料而非逐点检测",
      },
      // constraint marker
      {
        pos: [0, 7, 0], role: "context", label: "constraint: coolant ≥ 8",
        dslTag: "derive", pointId: "constraint",
        note: "CountConstraint.atLeast(group(\"coolant\"), 8) — 冷却单元不足则 invalid",
      },
    ],
  },

  // ============================================================
  // 高级场景
  // ============================================================
  {
    id: "modular-factory",
    title: "模块化工厂复合体",
    mod: "GregTech CEu / AE2 混合风格",
    icon: "Building2",
    accent: "brand",
    hook: "主厂 + N 个可插拔模块（动力室/仓储室/合成室），模块数量和类型由 CheckPhase 动态决定",
    multiblockShape: "主结构 + 多个 optional 子结构 + CheckPhase 动态模块检测 + CountConstraint 模块数量限制",
    apiPieces: [
      "Components.hollowBox (主厂)",
      "Components.substructure × N (模块)",
      "CheckPhase (动态模块扫描)",
      "CountConstraint (每类模块上下限)",
      "ContextPlan (综合产出计算)",
      "SubstructureSyncPolicy.statusAndContext",
    ],
    targets: ["block", "block_entity", "capability"],
    pitfalls: [
      "模块是 optional 子结构——可以有 0~N 个，缺失不影响主厂 valid",
      "每个模块独立检测，通过 substructure lifecycle 合并 context 到父结构",
      "动态模块数量约束用 CheckPhase + CountConstraint——比如至少 1 个动力室，最多 3 个合成室",
      "SyncPolicy 决定子结构 context 如何镜像到父（statusAndContext 模式下子 context 可见）",
    ],
    snippet: `Structures.define(id("modular_factory"))
    .transforms(Transforms.horizontalFacing())
    .point("controller", at(0,0,0), Targets.block()
      .where(PredicateDescriptors.id("cumulus:factory_controller"))
      .capture("controller"))
    .point("energy", at(0,0,1), Targets.capability("cumulus:energy"))
    // 主厂外壳
    .component(Components.hollowBox("main_hall",
      at(-2,-1,-2), at(2,1,2))
      .where(PredicateDescriptors.tag("cumulus:factory_frame"))
      .group("main"))
    // 可插拔模块（4 个方位，各自 optional）
    .component(Components.substructure("power_module",
      id("cumulus:power_room"), at(-3,0,0)))
    .component(Components.substructure("storage_module",
      id("cumulus:storage_room"), at(3,0,0)))
    .component(Components.substructure("crafting_module_a",
      id("cumulus:crafting_room"), at(0,0,-3)))
    .component(Components.substructure("crafting_module_b",
      id("cumulus:crafting_room"), at(0,0,3)))
    // 动态约束
    .checkPhase(ctx -> PhaseResult.of(
      List.of(),
      List.of(
        CountConstraint.atLeast(
          StructurePredicates.group("power"), 1),
        CountConstraint.range(
          StructurePredicates.group("crafting"), 0, 2)
      )))
    .build();`,
    blocks: [
      {
        pos: [0, 0, 0], role: "controller", label: "controller",
        pointId: "controller", target: "cumulus:block",
        predicate: "cumulus:factory_controller", capture: "controller", dslTag: "controller",
        note: "工厂主控 → capture 后传给所有子模块",
      },
      {
        pos: [0, 0, 1], role: "capability", label: "energy",
        pointId: "energy", target: "cumulus:capability",
        predicate: "cumulus:energy", dslTag: "energy",
        note: "主能量口 → capability",
      },
      // 4 个模块位（substructure 引用）
      {
        pos: [-3, 0, 0], role: "block_entity", label: "power_module",
        pointId: "power_module", target: "cumulus:block_entity",
        predicate: "cumulus:power_room", dslTag: "sub",
        note: "动力室子结构 → 至少 1 个（CountConstraint）",
      },
      {
        pos: [3, 0, 0], role: "block_entity", label: "storage_module",
        pointId: "storage_module", target: "cumulus:block_entity",
        predicate: "cumulus:storage_room", dslTag: "sub",
        note: "仓储室子结构 → optional",
      },
      {
        pos: [0, 0, -3], role: "block_entity", label: "crafting_a",
        pointId: "crafting_a", target: "cumulus:block_entity",
        predicate: "cumulus:crafting_room", dslTag: "sub",
        note: "合成室 A → 最多 2 个（CountConstraint range 0~2）",
      },
      {
        pos: [0, 0, 3], role: "block_entity", label: "crafting_b",
        pointId: "crafting_b", target: "cumulus:block_entity",
        predicate: "cumulus:crafting_room", dslTag: "sub",
        note: "合成室 B → 与 A 共享数量限制",
      },
      // constraint marker
      {
        pos: [0, 2, 0], role: "context", label: "constraints",
        dslTag: "derive", pointId: "constraints",
        note: "atLeast(power, 1) + range(crafting, 0, 2) → 动态模块数量约束",
      },
      // main hall shell (5×3×5, y=-1~1)
      ...shell("main_hall", [-2, -1, -2], [2, 1, 2], "#cumulus:factory_frame"),
    ],
  },
];
