// ============================================================
// Component 原语：每种 component 的展开行为 + 用法
// ============================================================

export interface ComponentDoc {
  id: "point" | "box" | "hollowBox" | "region" | "substructure";
  signature: string;
  title: string;
  emits: string;
  count: string; // how many points it emits
  description: string;
  api: string;
  // grid preview: an array of "x,y,z" occupancy for the visual demo
  demo: { from: [number, number, number]; to: [number, number, number]; hollow?: boolean };
  color: string;
}

export const COMPONENTS: ComponentDoc[] = [
  {
    id: "point",
    signature: 'point(name, location)',
    title: "point",
    emits: "1 StructurePoint",
    count: "exactly 1",
    description:
      "最基本的原语。放一个方块/目标在指定相对坐标。绝大多数 controller、port、key block 都用 point。",
    api: `Components.point("controller",
    RelativeBlockPos.of(0, 1, 0))
  .target(Targets.block()
    .where(PredicateDescriptors.id("cumulus:controller"))
    .capture("controller"))`,
    demo: { from: [1, 1, 1], to: [1, 1, 1] },
    color: "#7c5cff",
  },
  {
    id: "box",
    signature: 'box(name, from, to)',
    title: "box (filled)",
    emits: "N×M×K StructurePoints",
    count: "(dx+1)(dy+1)(dz+1)",
    description:
      "实心立方体。每个坐标都会展开成一个独立 point（id 自动按 name_x_y_z 生成）。常用于机器外壳。",
    api: `Components.box("casing",
    RelativeBlockPos.of(-1, 0, -1),
    RelativeBlockPos.of(1, 2, 1))
  .target(Targets.block()
    .where(PredicateDescriptors.id("cumulus:casing")))`,
    demo: { from: [0, 0, 0], to: [2, 2, 2] },
    color: "#22d3ee",
  },
  {
    id: "hollowBox",
    signature: 'hollowBox(name, from, to)',
    title: "hollowBox",
    emits: "外壳点",
    count: "(dx+1)(dy+1)(dz+1) - (dx-1)(dy-1)(dz-1)",
    description:
      "只展开外壳的 6 个面，内部空气区域不发射 point。是机壳、机房、晾晒房最经济的写法。",
    api: `Components.hollowBox("shell",
    RelativeBlockPos.of(-2, 0, -2),
    RelativeBlockPos.of(2, 3, 2))
  .target(Targets.block()
    .where(PredicateDescriptors.id("cumulus:reinforced")))`,
    demo: { from: [0, 0, 0], to: [2, 2, 2], hollow: true },
    color: "#f0abfc",
  },
  {
    id: "region",
    signature: 'region(name, from, to)',
    title: "region",
    emits: "1 Region Target",
    count: "1（不展开方块）",
    description:
      "整个区域作为一个 Target 发射，predicate 在区域粒度评估。门、通道、净空区域必须用 region，否则会发射大量 point。",
    api: `Components.region("door",
    RelativeBlockPos.of(-1, 0, 0),
    RelativeBlockPos.of(1, 2, 0))
  .target(Targets.region(
    RelativeBlockPos.of(-1, 0, 0),
    RelativeBlockPos.of(1, 2, 0)))`,
    demo: { from: [0, 0, 0], to: [2, 2, 0] },
    color: "#fb7185",
  },
  {
    id: "substructure",
    signature: 'substructure(name, structureId, offset)',
    title: "substructure",
    emits: "0（运行时复合子定义）",
    count: "0 (deferred)",
    description:
      "在 DSL 阶段不发射任何 point。运行时 SubstructureRuntimeComposer 会按累加 offset 把子定义的 point/region/capture 拼进父定义。",
    api: `Components.substructure("left_arm",
    ResourceLocation.parse("cumulus:arm"),
    RelativeBlockPos.of(-3, 0, 0))`,
    demo: { from: [0, 0, 0], to: [0, 0, 0] },
    color: "#a3e635",
  },
];
