// ============================================================
// Target 类型数据：每种 Target 的 API 形态、游戏场景、诊断示例
// ============================================================

export type TargetId =
  | "block"
  | "block_entity"
  | "entity"
  | "fluid"
  | "capability"
  | "context"
  | "region";

export interface TargetCard {
  id: TargetId;
  icon: string; // lucide icon name
  accent: string; // tailwind color name from our palette
  title: string;
  tagline: string;
  description: string;
  api: string; // Java DSL snippet
  gameScenarios: { title: string; detail: string }[];
  predicate: string[]; // built-in predicates
  diagnostic: string; // typical DiagnosticCode if it fails
}

export const TARGETS: TargetCard[] = [
  {
    id: "block",
    icon: "Box",
    accent: "brand",
    title: "block",
    tagline: "最经典的方块检查",
    description:
      "检查某个相对位置是否是某类方块。这是绝大多数多方块机器（高炉外壳、机械外壳）的默认 Target。",
    api: `Targets.block()
  .where(PredicateDescriptors.id("cumulus:casing"))
  .capture("casing")`,
    gameScenarios: [
      {
        title: "工业高炉外壳",
        detail:
          "3×3×3 的加固石头外壳，每一个方块都要满足 heat_resistant_casing tag。",
      },
      {
        title: "机械引擎机座",
        detail:
          "通过 block tag 而不是具体 block id，让任何兼容 mod 的方块都可以替换。",
      },
      {
        title: "魔法仪式中央符文",
        detail: "需要金块 + 上方附魔台复合判定，配合 block state property 校验。",
      },
    ],
    predicate: ["id", "tag", "state", "block_state_property"],
    diagnostic: "PREDICATE_FAILED · MISSING_TARGET",
  },
  {
    id: "block_entity",
    icon: "Cpu",
    accent: "cyan",
    title: "block_entity",
    tagline: "需要 NBT / 类型识别的方块实体",
    description:
      "检查方块实体类型 + NBT 内容。比 block 多一层：读取后可以拿到 BlockEntity 持有的物品、能量、流体等内部状态。",
    api: `Targets.blockEntity(
    ResourceLocation.parse("cumulus:input_bus"),
    ResourceLocation.parse("cumulus:item_bus"),
    "{Slots:[{Slot:0b,Item:{id:\"minecraft:iron_ingot\"}}]}"
)`,
    gameScenarios: [
      {
        title: "AE2 玻璃线缆接口",
        detail: "读取其 grid 连接状态，作为多方块是否接入 AE2 网络的依据。",
      },
      {
        title: "Mekanism 气体罐输入",
        detail: "校验气体种类 + 容量，自动匹配化学处理配方。",
      },
      {
        title: "/Create/ 动能输入轴",
        detail: "读取 rotation stress，多方块机器按 stress 上限做配速。",
      },
    ],
    predicate: ["block_entity_type", "nbt_path", "block_entity_field"],
    diagnostic: "WRONG_TARGET_TYPE · CONTEXT_CAPTURE_FAILED",
  },
  {
    id: "entity",
    icon: "User",
    accent: "magenta",
    title: "entity",
    tagline: "区域内的实体",
    description:
      "检查 AABB 区域内是否存在指定类型/数量/NBT 的实体。比 block 多一层动态语义，玩家、抛射物、矿车都可以做检查。",
    api: `Targets.entity(
    ResourceLocation.parse("minecraft:villager"),
    "{VillagerData:{level:5,type:\"minecraft:weaponsmith\"}}"
)`,
    gameScenarios: [
      {
        title: "村庄核心召唤大师",
        detail: "多方块成型需要附近有一名大师级武器匠村民作为关键 NPC。",
      },
      {
        title: "雷电法阵消耗闪电",
        detail: "区域内的 LightningBolt 实体被作为仪式能量源消耗。",
      },
      {
        title: "矿车物流终端",
        detail: "检查 1 个 chest minecart 停在指定偏移，作为输入物品缓冲。",
      },
    ],
    predicate: ["entity_type", "entity_distance", "entity_count", "entity_nbt"],
    diagnostic: "MISSING_TARGET · UNLOADED_AREA",
  },
  {
    id: "fluid",
    icon: "Droplets",
    accent: "lime",
    title: "fluid",
    tagline: "流体源 / 流",
    description:
      "检查某个方块位置是否是指定流体（含 level 等状态）。可识别水、岩浆、mod 流体，并能区分 source / flowing。",
    api: `Targets.fluid()
  .where(PredicateDescriptors.id("minecraft:lava"))
  // 通过 properties 取得 [level]=0 即 source`,
    gameScenarios: [
      {
        title: "岩浆炉燃料接入",
        detail: "底部一格必须是岩浆源，从 lava source tank 自动抽取。",
      },
      {
        title: "沉浸工程 水车水池",
        detail: "水流方向决定水车产生的 RPM，多方块按水流向量调节转向。",
      },
      {
        title: "Tinkers' 浇铸盆冷却",
        detail: "上方流体必须是液态金属 + 下方冷却液，按 fluid tag 匹配冷却结果。",
      },
    ],
    predicate: ["fluid_id", "fluid_tag", "fluid_state"],
    diagnostic: "MISSING_TARGET · PREDICATE_FAILED",
  },
  {
    id: "capability",
    icon: "PlugZap",
    accent: "amber",
    title: "capability",
    tagline: "NeoForge 能力接口",
    description:
      "通过 NeoForge BlockCapability 查询某方块对外暴露的能量/物品/流体/自定义能力。能感知真正接进了哪种管道。",
    api: `Targets.capability("cumulus:energy_input")
  // 内部解析为 Capabilities.EnergyStorage.BLOCK
  // 朝向由 controller.facing 决定 side`,
    gameScenarios: [
      {
        title: "GTCEu 高压动力舱",
        detail: "8 个能量输入口分别校验能量等级 ≥ 8192 EU/t，作为机器通电条件。",
      },
      {
        title: "Create 应力总线",
      detail: "所有面都要能 expose StressCapability，否则机器报应力不足。",
      },
      {
        title: "Botania 魔力塔",
        detail: "顶部方块需暴露 ManaCapability，按mana 量级决定多方块等级。",
      },
    ],
    predicate: ["capability_present", "capability_amount", "capability_side"],
    diagnostic: "MISSING_TARGET · PREDICATE_FAILED",
  },
  {
    id: "context",
    icon: "Brain",
    accent: "brand",
    title: "context",
    tagline: "外部世界 / 上下文值",
    description:
      "不直接查世界，而是读 RuntimeStructureContext 里的派生键。让父结构把已捕获的状态传给子结构，做跨多方块通讯。",
    api: `Targets.context("controller.facing")
  // 来自 controller 捕获的 BlockState property "facing"
  // 在子结构里直接读取，无需再次扫描世界`,
    gameScenarios: [
      {
        title: "巨型工厂总控朝向",
        detail: "factory 把 controller.facing 派生进 context，子结构 left_arm/right_arm 读取后镜像旋转。",
      },
      {
        title: "白天 / 夜晚双模式",
        detail: "context 派生 day_time=true/false，机器按昼夜切换不同子结构激活条件。",
      },
      {
        title: "玩家解锁进度",
        detail: "context 派生 player_advancement_done=true，作为多方块是否启动的前置。",
      },
    ],
    predicate: ["context_present", "context_equals", "context_range"],
    diagnostic: "CONTEXT_DERIVATION_FAILED · CONTEXT_CAPTURE_FAILED",
  },
  {
    id: "region",
    icon: "Frame",
    accent: "rose",
    title: "region",
    tagline: "矩形区域整体检查",
    description:
      "区域作为单个 Target，不需要逐方块展开。门、走廊、管道槽、开口这类\"空间留白\"检查都用 region。",
    api: `Targets.region(
    RelativeBlockPos.of(-1, 0, 0),
    RelativeBlockPos.of(1, 2, 0)
)
  .where(PredicateDescriptors.id("cumulus:air_or_passthrough"))`,
    gameScenarios: [
      {
        title: "工业自动门通道",
        detail: "门洞是 3×3 的 region，全部为 air/可穿透方块。region 比 9 个 point 节省 9 倍 check。",
      },
      {
        title: "Tinkers' 浇铸通道",
        detail: "中间 1×N 流体流通道必须为空，否则浇铸不流通。",
      },
      {
        title: "AE2 无线网关净空",
        detail: "围绕ME 控制器 5×5×5 region 必须无遮挡，保证 wireless range。",
      },
    ],
    predicate: ["region_all_match", "region_count_in_range", "region_center_in"],
    diagnostic: "PREDICATE_FAILED · COMPONENT_CONFLICT",
  },
];
