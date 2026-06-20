// ============================================================
// 真实游戏场景 → Cumulus API 映射
// 每个场景列出：玩法名、参考 mod、Cumulus API 组合、设计要点
// ============================================================

export interface GameScenario {
  id: string;
  title: string;
  mod: string; // 参考模组 / 风格
  icon: string; // lucide icon
  accent: string;
  hook: string; // one-liner
  multiblockShape: string; // human readable shape
  apiPieces: string[]; // API building blocks used
  targets: string[]; // target types involved
  pitfalls: string[]; // design pitfalls
  snippet: string; // minimal java dsl
}

export const SCENARIOS: GameScenario[] = [
  {
    id: "gastronomic-forge",
    title: "巨型工业高炉",
    mod: "GregTech CEu / IE风格",
    icon: "Flame",
    accent: "amber",
    hook: "3×4×3 实心外壳 + 1 个 controller + 多个 item/fluid/energy bus",
    multiblockShape: "3×4×3 hollowBox + 多个 capability port",
    apiPieces: [
      "Components.hollowBox",
      "Targets.block + block tag",
      "Targets.capability × N",
      "ContextRuleDescriptor.derive(facing)",
      "Transforms.horizontalFacing",
    ],
    targets: ["block", "capability", "context"],
    pitfalls: [
      "外壳如果用 box 会发射 60+ point，必须用 hollowBox",
      "controller 朝向决定 bus side，必须用 context 派生 facing",
      "4 种 transform 候选要按 horizontalFacing 顺序短路",
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
  .context(ContextRuleDescriptor.derive("facing","controller.facing"))
  .build();`,
  },
  {
    id: "kinetic-assembly",
    title: "应力组装线",
    mod: "Create 风格",
    icon: "Cog",
    accent: "amber",
    hook: "线性 1×N×1 走带 + 中段工具站 + 末端应力总线",
    multiblockShape: "1×1×N box 走带 + 多 point 工具站 + 1 capability 总线",
    apiPieces: [
      "Components.box (linear)",
      "Targets.capability (stress)",
      "Targets.blockEntity (tool station)",
      "Targets.context (player_sneak)",
      "Substructure per tool",
    ],
    targets: ["block", "block_entity", "capability", "context"],
    pitfalls: [
      "工具站可换，用 blockEntity + tag 而非固定 block id",
      "应力输入必须 expose capability，普通方块不算",
      "玩家潜行作为运行模式切换，用 context 派生",
    ],
    snippet: `Structures.define(id("assembly_line"))
  .transforms(Transforms.horizontalFacing())
  .component(Components.box("belt", at(0,0,0), at(0,0,8)))
  .point("stress_in", at(0,0,0), Targets.capability("create:stress"))
  .point("tool_a", at(0,1,2), Targets.blockEntity()
    .where(PredicateDescriptors.tag("create:saw")))
  .context(ContextRuleDescriptor.derive("mode","controller.sneak_state"))
  .build();`,
  },
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
      "Targets.entity (可选作为触发)",
      "ContextRuleDescriptor (玩家进度)",
    ],
    targets: ["block", "capability", "entity", "region"],
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
  },
  {
    id: "ae2-controller",
    title: "ME 控制器簇",
    mod: "AE2 风格",
    icon: "Network",
    accent: "cyan",
    hook: "8 个 controller 方块直接相邻 + region 净空 + 任意 N 个 iface",
    multiblockShape: "compact cluster + region 净空 + 散布 iface points",
    apiPieces: [
      "Components.box (controller cluster)",
      "Targets.blockEntity (controller)",
      "Targets.region (净空)",
      "Targets.context (network_id)",
    ],
    targets: ["block_entity", "block", "region", "context"],
    pitfalls: [
      "controller 之间必须直接相邻，且 blockEntity NBT 持有相同 network id",
      "净空 region 防止 5×5×5 内有遮挡，影响 wireless range",
      "iface 数量上限用 context count 派生",
    ],
    snippet: `Structures.define(id("ae2_cluster"))
  .component(Components.box("ctrl", at(0,0,0), at(1,1,1))
    .where(PredicateDescriptors.id("ae2:controller")))
  .component(Components.region("wireless", at(-2,-2,-2), at(2,2,2)))
  .context(ContextRuleDescriptor.derive("net","ctrl[0].network"))
  .build();`,
  },
  {
    id: "ritual-altar",
    title: "血魔法祭坛",
    mod: "Blood Magic 风格",
    icon: "Skull",
    accent: "rose",
    hook: "中央祭坛 + 围绕 12 个符文 + 1 个 blood altar 持有者",
    multiblockShape: "ring of 12 points + 1 center + 1 capability holder",
    apiPieces: [
      "Components.hollowBox (ring 作为 box 的特例)",
      "Targets.block (rune)",
      "Targets.entity (玩家作为 仪式主)",
      "Targets.capability (blood)",
    ],
    targets: ["block", "entity", "capability"],
    pitfalls: [
      "ring 用 hollowBox 边界 + 限定 1 层（手写 12 个 point 更清晰）",
      "玩家 entity 必须在场，否则视为仪式中断",
      "blood 能力数值决定仪式等级",
    ],
    snippet: `Structures.define(id("ritual_altar"))
  .point("altar", at(0,0,0), Targets.capability("blood_magic:blood"))
  // 12 个 rune point 围绕，由 datagen 生成
  .point("master", at(0,1,2), Targets.entity("minecraft:player"))
  .build();`,
  },
  {
    id: "fusion-reactor",
    title: "聚变反应堆",
    mod: "Mekanism / GT 风格",
    icon: "Atom",
    accent: "brand",
    hook: "3 层中空立方体 + 8 个 toroid magnet + 中央 plasma 容器",
    multiblockShape: "hollowBox × 3 layer + ring of magnets + center region",
    apiPieces: [
      "Components.hollowBox × 3",
      "Targets.block (magnet)",
      "Targets.region (plasma chamber)",
      "Substructure (per toroid)",
    ],
    targets: ["block", "region"],
    pitfalls: [
      "三层 hollowBox 共 50+ 方块，必须用 region 净空检查内部",
      "plasma chamber 用 region 检查 air 状态",
      "8 个 toroid 可拆为子结构，每个子结构持有 magnet 列表",
    ],
    snippet: `Structures.define(id("fusion_reactor"))
  .component(Components.hollowBox("outer", at(-3,0,-3), at(3,5,3)))
  .component(Components.region("plasma", at(-1,1,-1), at(1,4,1)))
  .component(Components.substructure("toroid_n",
    id("cumulus:toroid"), at(0,0,-3)))
  .build();`,
  },
  {
    id: "magic-circle",
    title: "魔法阵法",
    mod: "Ars Nouveau / 阵法风",
    icon: "CircleDot",
    accent: "magenta",
    hook: "9 格圆环 + 中央书 + 四方位 candle",
    multiblockShape: "ring of 9 points + 4 candle points + 1 center book",
    apiPieces: [
      "Multiple Components.point",
      "Targets.blockEntity (book with spell)",
      "Targets.context (current_spell)",
    ],
    targets: ["block_entity", "block", "context"],
    pitfalls: [
      "圆环的 9 个 point 必须 enable horizontalFacing transform",
      "spell 通过 blockEntity NBT 读出，并用 context 传递给子结构",
      "candle 朝向无意义，但燃烧状态要从 block state 读",
    ],
    snippet: `Structures.define(id("magic_circle"))
  .transforms(Transforms.horizontalFacing())
  .point("book", at(0,0,0), Targets.blockEntity(
    id("ars_nouveau:enchanting_phylactery"),
    id("ars_nouveau:spell_book"),
    "{spell:'ars_nouveau:flight'}"))
  .context(ContextRuleDescriptor.derive("spell","book.spell"))
  .build();`,
  },
  {
    id: "village-hub",
    title: "村民交易中心",
    mod: "Vanilla+ 风格",
    icon: "Users",
    accent: "lime",
    hook: "1 个 market block + 周围 N 名大师级村民实体",
    multiblockShape: "1 point + entity region + context (village level)",
    apiPieces: [
      "Targets.entity (村民)",
      "Targets.region (村民活动区)",
      "Targets.context (village_level)",
    ],
    targets: ["block", "entity", "context"],
    pitfalls: [
      "村民会移动，entity target 要允许多匹配 + 数量阈值",
      "村民级别在 NBT 里，必须用 entity NBT predicate",
      "village_level 来自外部 POI 系统，用 context 派生",
    ],
    snippet: `Structures.define(id("village_hub"))
  .point("market", at(0,0,0), Targets.block())
  .component(Components.region("activity", at(-8,0,-8), at(8,3,8)))
  .context(ContextRuleDescriptor.derive("level","poi.village_level"))
  .build();`,
  },
];
