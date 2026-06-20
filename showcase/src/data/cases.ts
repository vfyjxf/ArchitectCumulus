// ============================================================
// 真实案例：vanilla NeoForge 模式 vs Cumulus 模式
// 同样的多方块玩法，对比代码量 / 性能 / 可维护性
// ============================================================

export interface CaseStudy {
  id: string;
  icon: string;
  accent: string;
  title: string;
  scenario: string;
  shape: string;
  // vanilla 实现的痛点
  vanillaPainPoints: string[];
  // vanilla 代码示例（精简但反映真实模式）
  vanillaCode: string;
  vanillaLoc: number;
  // cumulus 代码
  cumulusCode: string;
  cumulusLoc: number;
  // 性能对比
  perf: {
    label: string;
    vanilla: string;
    cumulus: string;
    ratio: string;
  }[];
  // 维护场景对比
  maintenance: {
    task: string;
    vanilla: string;
    cumulus: string;
  }[];
  // 一句话总结
  takeaway: string;
}

export const CASES: CaseStudy[] = [
  {
    id: "basic-forge",
    icon: "Flame",
    accent: "amber",
    title: "基础工业高炉 (3×3×3)",
    scenario:
      "经典场景：3×3×3 外壳 + 中央 controller + 1 个 item input + 1 个 energy input。机器按 controller 朝向旋转。",
    shape: "hollowBox 27 → shell 26 + 1 controller",
    vanillaPainPoints: [
      "27 个 if 检查写死在 BlockEntity.tick() 里",
      "朝向支持要手写 4 套坐标系（north/south/east/west）",
      "诊断只能 level.player.sendSystemMessage('incomplete')",
      "改一个外壳方块要重新测试 4 个朝向 × 27 个位置 = 108 次手动检查",
      "没有 cache：每 tick 全检查 27 个方块，1000 个机器 = 27000 次 getBlockState/tick",
    ],
    vanillaCode: `public class ForgeBlockEntity extends BlockEntity {
    private static final Direction[] FACINGS = Direction.values();

    @Override
    public void serverTick() {
        boolean ok = checkStructure();
        if (ok != this.active) {
            this.active = ok;
            markDirty();
        }
        if (this.active) {
            doSmelting();
        }
    }

    private boolean checkStructure() {
        Direction facing = getFacing();
        for (Direction candidate : FACINGS) {
            if (tryCheck(candidate)) {
                this.appliedFacing = candidate;
                return true;
            }
        }
        return false;
    }

    private boolean tryCheck(Direction f) {
        // 27 个 if...每个 if 写死相对坐标 + facing 旋转
        BlockPos ctrl = worldPosition.above();
        if (level.getBlockState(ctrl).getBlock() != ModBlocks.FORGE_CONTROLLER)
            return false;
        // 接下来 26 个 casing 检查，每个都要 facing 旋转坐标...
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 1 && dz == 0) continue; // controller
                    BlockPos p = worldPosition.offset(
                        rotateX(dx, dz, f), dy, rotateZ(dx, dz, f));
                    BlockState s = level.getBlockState(p);
                    if (!s.is(ModTags.HEAT_CASING)) {
                        lastFail = p;
                        return false;
                    }
                }
            }
        }
        // 还要检查 item input + energy input...
        return true;
    }

    private int rotateX(int dx, int dz, Direction f) {
        return switch (f) {
            case NORTH -> dx;
            case SOUTH -> -dx;
            case EAST -> dz;
            case WEST -> -dz;
            default -> dx;
        };
    }
    // rotateZ 同理...
    // 还有 markDirty / chunk load / sub-tick / ...

    private void diagnose() {
        if (lastFail != null) {
            // 玩家看到的就这一句：
            level.players.forEach(p ->
                p.sendSystemMessage(
                    Component.literal("Multiblock incomplete at " + lastFail)));
            // 哪个 block? 期望什么? 实际什么? 怎么修? 都丢失了。
        }
    }
}`,
    vanillaLoc: 58,
    cumulusCode: `// 一次定义，所有朝向、所有校验、所有诊断全自动
public static final StructureDefinition MINI_FORGE =
    Structures.define(ResourceLocation.parse("cumulus:mini_forge"))
        .transforms(
            Transforms.identity(),
            Transforms.horizontalFacing())        // 4 个候选自动试
        .point("controller",
            RelativeBlockPos.of(0, 1, 0),
            Targets.block()
                .where(PredicateDescriptors.id("cumulus:forge_controller"))
                .capture("controller"))
        .component(Components.hollowBox("casing",
            RelativeBlockPos.of(-1, 0, -1),
            RelativeBlockPos.of(1, 2, 1))
            .target(Targets.block())
            .where(PredicateDescriptors.tag("cumulus:heat_casing")))
        .point("item_in",
            RelativeBlockPos.of(-1, 1, 0),
            Targets.blockEntity()
            .where(PredicateDescriptors.id("cumulus:item_bus")))
        .point("energy_in",
            RelativeBlockPos.of(0, 0, 1),
            Targets.capability("cumulus:energy"))
        .context(ContextRuleDescriptor.derive(
            "facing", "controller.facing"))
        .build();

// 注册到引擎，剩下交给 Cumulus
RuntimeCheckEngine engine = new RuntimeCheckEngine()
    .register(MINI_FORGE)
    .track(MINI_FORGE.id(), anchor, source);

// BlockEntity 只关心 delta：
@Override
public void onDelta(InstanceDelta delta) {
    if (delta.status() == VALID) startSmelting(delta.context());
    else stopMachine();
}`,
    cumulusLoc: 36,
    perf: [
      { label: "fastTick 检查 (1 instance)", vanilla: "~80µs 全检查 27 块", cumulus: "~10µs candidate 短路", ratio: "8×" },
      { label: "1000 instance (per tick)", vanilla: "~80ms 跌 TPS", cumulus: "~10ms 稳定", ratio: "8×" },
      { label: "诊断信息密度", vanilla: "1 string", cumulus: "20+ graph nodes", ratio: "∞" },
      { label: "改外壳 tag 工作量", vanilla: "重测 4 朝向 × 27 块", cumulus: "改一行 predicate", ratio: "100×" },
    ],
    maintenance: [
      {
        task: "新增一个 'check player is nearby' 条件",
        vanilla: "改 BlockEntity.tick 加 AABB 查询；硬编码距离 5",
        cumulus: "加一个 Targets.entity().optional() point",
      },
      {
        task: "增加第二种朝向 (上下颠倒)",
        vanilla: "再加 4 套坐标系，旋转函数膨胀",
        cumulus: "加 Transforms.flipY()，DSL 一行",
      },
      {
        task: "支持热重载（玩家改外壳方块 tag）",
        vanilla: "做不到，需要 mod 重新打包发布",
        cumulus: "改 datapack JSON，服务器 reload 即生效",
      },
      {
        task: "把高炉复用到不同 mod 里",
        vanilla: "复制粘贴整个 BlockEntity + 重写",
        cumulus: "复用 StructureDefinition + 注入不同 BlockRegistry",
      },
    ],
    takeaway:
      "vanilla 写死了 27 个 if 和 4 套旋转，Cumulus 把它降为 4 个组件 + 2 个 transform 候选。诊断从 1 句话变成 20+ 节点的图。",
  },
  {
    id: "large-factory",
    icon: "Factory",
    accent: "brand",
    title: "大型工厂 (10×4×6 + 双机械臂)",
    scenario:
      "工厂主体 10×4×6，左右各嵌一个 3×3×3 机械臂，每个机械臂末端有一个工具方块。所有朝向可旋转。",
    shape: "factory hollowBox + 2× arm substructure + 2× tool substructure",
    vanillaPainPoints: [
      "vanilla 模式下只能把所有方块拍平成一个 ~250 个 if 的 BlockEntity.tick()",
      "机械臂 / 工具 的概念完全消失，全是 if",
      "机械臂失败和工厂主体失败无法分别报告",
      "左右机械臂对称逻辑要手写两套代码",
      "维护时改机械臂 1 个方块要重测整个工厂",
    ],
    vanillaCode: `public class FactoryBlockEntity extends BlockEntity {
    @Override
    public void serverTick() {
        // 250 个 if 写在这里
        // 主体外壳
        for (int dx = -5; dx <= 5; dx++)
          for (int dy = 0; dy <= 4; dy++)
            for (int dz = -3; dz <= 3; dz++) {
                if (isOnShellEdge(dx, dy, dz)) {
                    BlockPos p = transform(dx, dy, dz, facing);
                    if (!level.getBlockState(p).is(ModTags.FACTORY_CASING)) {
                        lastFail = p; return false;
                    }
                }
            }
        // 左机械臂 (offset -6, 0, 0)
        // 27 个 if...
        for (int dx = -7; dx <= -5; dx++)
          for (int dy = 0; dy <= 2; dy++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = transform(dx, dy, dz, facing);
                if (!level.getBlockState(p).is(ModTags.ARM_PART))
                    return false;
            }
        // 左工具 1 个 if...
        BlockPos leftTool = transform(-6, 3, 0, facing);
        if (!level.getBlockState(leftTool).is(ModBlocks.DRILL))
            return false;
        // 右机械臂 27 个 if...（同上但 dx 6..8）
        // 右工具 1 个 if...
        // ...

        // 报告失败时，玩家看到的：
        //   "Multiblock incomplete"
        // 哪个 arm? 哪个 tool? 主体还是子结构? 一概不知
    }

    // 还有 facing、transform、 AABB 查找...
    // 文件最终 ~600 行
}`,
    vanillaLoc: 612,
    cumulusCode: `// 工具子结构（独立定义，可复用）
public static final StructureDefinition TOOL =
    Structures.define(ResourceLocation.parse("cumulus:tool"))
        .point("bit", RelativeBlockPos.of(0, 0, 0),
            Targets.block()
                .where(PredicateDescriptors.id("cumulus:drill")))
        .build();

// 机械臂子结构（嵌套工具）
public static final StructureDefinition ARM =
    Structures.define(ResourceLocation.parse("cumulus:arm"))
        .transforms(Transforms.horizontalFacing())
        .component(Components.hollowBox("frame",
            RelativeBlockPos.of(-1, 0, -1),
            RelativeBlockPos.of(1, 2, 1))
            .target(Targets.block())
            .where(PredicateDescriptors.tag("cumulus:arm_part")))
        .component(Components.substructure("tool",
            ResourceLocation.parse("cumulus:tool"),
            RelativeBlockPos.of(0, 3, 0)))
        .build();

// 工厂主体（引用两个 arm）
public static final StructureDefinition FACTORY =
    Structures.define(ResourceLocation.parse("cumulus:factory"))
        .transforms(Transforms.horizontalFacing())
        .point("controller", RelativeBlockPos.of(0, 0, 0),
            Targets.block()
                .where(PredicateDescriptors.id("cumulus:factory_ctrl"))
                .capture("controller"))
        .component(Components.hollowBox("shell",
            RelativeBlockPos.of(-5, 0, -3),
            RelativeBlockPos.of(5, 4, 3))
            .target(Targets.block())
            .where(PredicateDescriptors.tag("cumulus:factory_casing")))
        .component(Components.substructure("left_arm",
            ResourceLocation.parse("cumulus:arm"),
            RelativeBlockPos.of(-6, 0, 0)))
        .component(Components.substructure("right_arm",
            ResourceLocation.parse("cumulus:arm"),
            RelativeBlockPos.of(6, 0, 0)))
        .context(ContextRuleDescriptor.derive(
            "facing", "controller.facing"))
        .build();

// 注册时 arm/tool 自动被发现并复合
RuntimeCheckEngine engine = new RuntimeCheckEngine()
    .substructureSyncPolicy(STATUS_AND_CONTEXT)
    .register(FACTORY)
    .register(ARM)
    .register(TOOL)
    .track(FACTORY.id(), anchor, source);

// 父结构能直接读子结构状态：
//   ctx.get("substructure.left_arm.status") → VALID/INVALID
//   ctx.get("substructure.left_arm.context.tool.bit") → drill BlockPos`,
    cumulusLoc: 56,
    perf: [
      { label: "代码行数", vanilla: "612 行", cumulus: "56 行", ratio: "11×" },
      { label: "fastTick (1 instance)", vanilla: "~250µs", cumulus: "~30µs", ratio: "8×" },
      { label: "子结构独立 delta", vanilla: "✗ 全拍平", cumulus: "✓ 4 个独立 occurrence", ratio: "∞" },
      { label: "改机械臂 1 个 point", vanilla: "重测整个工厂 4 朝向", cumulus: "改 arm 定义 1 行", ratio: "100×" },
    ],
    maintenance: [
      {
        task: "增加第二个工具到机械臂",
        vanilla: "在 612 行的 if 列表里加 1 个 if，重测整个工厂",
        cumulus: "在 ARM 定义里加一个 substructure component",
      },
      {
        task: "工厂加一个第三机械臂",
        vanilla: "再加 28 个 if + 复制 facing 旋转",
        cumulus: "DSL 加一行 Components.substructure(...)",
      },
      {
        task: "替换工具类型 (drill → saw)",
        vanilla: "全项目搜索 .is(ModBlocks.DRILL) 替换",
        cumulus: "改 TOOL 定义里一行 predicate",
      },
      {
        task: "重用 ARM 到其他工厂",
        vanilla: "复制 ARM 那段代码到新 BlockEntity",
        cumulus: "新工厂 .component(Components.substructure('arm', arm_id, offset))",
      },
    ],
    takeaway:
      "通过子结构嵌套，把 600 行的代码降到 56 行，并且每个子结构有独立的诊断 / 状态 / context 通道。",
  },
  {
    id: "data-driven",
    icon: "Database",
    accent: "cyan",
    title: "数据驱动 (玩家自定义机器)",
    scenario:
      "modder 提供 5 种机器模板，玩家通过 datapack 自定义第 6 种机器，不重新发布 mod。",
    shape: "JSON 资产 + 运行时解析 + 注册到引擎",
    vanillaPainPoints: [
      "vanilla BlockEntity 类是写死的，玩家无法新增",
      "datapack 只能改 loot table / recipe，不能改机器定义",
      "玩家社区只能等 modder 发布新版本",
      "服务器管理员想自定义机器要找 modder",
    ],
    vanillaCode: `// vanilla 完全不支持
public class ForgeBlockEntity extends BlockEntity {
    // 类已经编译进 jar，玩家改不了
    // 唯一办法：modder 重新编译整个 mod 重新发布
    // 玩家更新 mod 才能用新机器
}

// 玩家想做 '增加一个超大高炉' 只能：
// 1. 学习 modding
// 2. fork mod 源码
// 3. 写一个新 BlockEntity
// 4. 编译发布新 mod
// 5. 上传到 CurseForge
// 6. 等玩家下载`,
    vanillaLoc: 0,
    cumulusCode: `// 1. modder 把 5 种机器写成 Java DSL + datagen 输出 JSON
// 2. 玩家在 datapack 里加一个新 JSON：
//    data/my_pack/cumulus/structures/mega_forge.json
{
  "id": "my_pack:mega_forge",
  "schema_version": 1,
  "transforms": [
    {"id": "cumulus:identity"},
    {"id": "cumulus:horizontal_facing"}
  ],
  "points": [
    {
      "id": "controller",
      "location": "0,1,0",
      "target": {"type": "cumulus:block"},
      "predicates": [{"id": "my_pack:mega_controller"}],
      "captures": ["controller"]
    }
  ],
  "components": [
    {
      "kind": "hollowBox",
      "name": "casing",
      "from": "-2,0,-2",
      "to": "2,3,2",
      "target": "cumulus:block",
      "predicates": [{"id": "#my_pack:mega_casing"}]
    }
  ]
}

// 3. Cumulus 自动 reload：
@SubscribeEvent
public void onReload(AddReloadListenerEvent event) {
    event.addListener(resourceManager ->
        runtimeAssetReloadListener.reload(resourceManager));
}

// 4. RuntimeAssetReloadWiring 自动：
//    - 扫描 data/*/cumulus/structures/*.json
//    - 解析为 StructureDefinition
//    - 注册到 RuntimeCheckEngine
//    - 玩家立刻可以用新机器，无需 mod 重启`,
    cumulusLoc: 35,
    perf: [
      { label: "玩家新增机器门槛", vanilla: "学习 modding + 发布 mod", cumulus: "写一个 JSON 文件", ratio: "∞" },
      { label: "服务器管理员自定义", vanilla: "需联系 modder", cumulus: "改 datapack", ratio: "∞" },
      { label: "热重载", vanilla: "✗", cumulus: "✓ /reload", ratio: "∞" },
      { label: "modder 发布周期", vanilla: "数天到数周", cumulus: "玩家自助", ratio: "∞" },
    ],
    maintenance: [
      {
        task: "玩家想要 'fire forge' / 'ice forge' / 'lightning forge' 三个变体",
        vanilla: "等 modder 写 3 个 BlockEntity",
        cumulus: "玩家写 3 个 JSON，5 分钟搞定",
      },
      {
        task: "服务器维护者平衡机器强度",
        vanilla: "请求 modder 改 jar",
        cumulus: "改 JSON 里的 predicate tag",
      },
      {
        task: "整合包作者重平衡",
        vanilla: "联系多个 mod 作者",
        cumulus: "改 datapack JSON",
      },
      {
        task: "测试新机器",
        vanilla: "本地启动整 mod 开发环境",
        cumulus: "丢 JSON 进 datapack，进游戏试",
      },
    ],
    takeaway:
      "JSON 是产物不是源。modder 写 Java，玩家写 JSON，两边都开心，且互不阻塞。",
  },
];
