import { motion } from "framer-motion";
import { ChevronRight, Layers } from "lucide-react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { SubstructureViewport } from "@/three/SubstructureViewport";
import { cn } from "@/lib/utils";

interface TreeNode {
  id: string;
  name: string;
  offset: string;
  kind: "root" | "child" | "grandchild";
  color: string;
  detail: string;
  children?: TreeNode[];
}

const TREE: TreeNode = {
  id: "factory",
  name: "factory",
  offset: "(0,0,0)",
  kind: "root",
  color: "#7c5cff",
  detail: "root · registers RuntimeCheckEngine",
  children: [
    {
      id: "left_arm",
      name: "left_arm",
      offset: "(-3,0,0)",
      kind: "child",
      color: "#22d3ee",
      detail: "substructure ref → cumulus:arm",
      children: [
        {
          id: "tool_a",
          name: "tool_a",
          offset: "(-3,1,0)",
          kind: "grandchild",
          color: "#f0abfc",
          detail: "substructure ref → cumulus:tool",
        },
      ],
    },
    {
      id: "right_arm",
      name: "right_arm",
      offset: "(+3,0,0)",
      kind: "child",
      color: "#22d3ee",
      detail: "substructure ref → cumulus:arm",
      children: [
        {
          id: "tool_b",
          name: "tool_b",
          offset: "(+3,1,0)",
          kind: "grandchild",
          color: "#f0abfc",
          detail: "substructure ref → cumulus:tool",
        },
      ],
    },
    {
      id: "base",
      name: "base",
      offset: "(0,-1,0)",
      kind: "child",
      color: "#a3e635",
      detail: "substructure ref → cumulus:base",
    },
  ],
};

function TreeView({ node, depth = 0 }: { node: TreeNode; depth?: number }) {
  return (
    <div>
      <motion.div
        initial={{ opacity: 0, x: -10 }}
        whileInView={{ opacity: 1, x: 0 }}
        viewport={{ once: true }}
        transition={{ delay: depth * 0.1 }}
        className={cn(
          "flex items-start gap-2 p-2.5 rounded-lg border bg-white/[0.02]",
          depth === 0
            ? "border-brand-400/30"
            : depth === 1
            ? "border-cyan-400/20"
            : "border-pink-400/20"
        )}
        style={{ marginLeft: depth * 18 }}
      >
        <div
          className="w-2.5 h-2.5 rounded-full mt-1.5 shrink-0"
          style={{
            background: node.color,
            boxShadow: `0 0 10px ${node.color}80`,
          }}
        />
        <div className="flex-1 min-w-0">
          <div className="flex items-baseline gap-2 flex-wrap">
            <code
              className="font-mono text-[13px] font-semibold"
              style={{ color: node.color }}
            >
              {node.name}
            </code>
            <code className="font-mono text-[10.5px] text-white/40">
              offset {node.offset}
            </code>
          </div>
          <div className="text-[11px] text-white/50 mt-0.5">{node.detail}</div>
        </div>
      </motion.div>
      {node.children && (
        <div className="mt-1.5 space-y-1.5 border-l border-white/5 ml-4 pl-3">
          {node.children.map((c) => (
            <TreeView key={c.id} node={c} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
}

const FACTORY_CODE = `var factory = Structures.define(
        ResourceLocation.parse("cumulus:factory"))
    .transforms(Transforms.horizontalFacing())
    .point("controller",
        RelativeBlockPos.of(0, 0, 0),
        Targets.block()
            .where(PredicateDescriptors.id("cumulus:factory_controller"))
            .capture("controller"))
    .component(Components.substructure("left_arm",
        ResourceLocation.parse("cumulus:arm"),
        RelativeBlockPos.of(-3, 0, 0)))
    .component(Components.substructure("right_arm",
        ResourceLocation.parse("cumulus:arm"),
        RelativeBlockPos.of(3, 0, 0)))
    .component(Components.substructure("base",
        ResourceLocation.parse("cumulus:base"),
        RelativeBlockPos.of(0, -1, 0)))
    .context(ContextRuleDescriptor.derive(
        "facing", "controller.facing"))
    .build();`;

const ARM_CODE = `var arm = Structures.define(
        ResourceLocation.parse("cumulus:arm"))
    .point("shoulder",
        RelativeBlockPos.of(0, 0, 0),
        Targets.block())
    .point("elbow",
        RelativeBlockPos.of(0, 1, 0),
        Targets.capability("cumulus:energy"))
    .component(Components.substructure("tool",
        ResourceLocation.parse("cumulus:tool"),
        RelativeBlockPos.of(0, 1, 0)))
    .build();`;

export function Substructure() {
  return (
    <Section
      id="substructure"
      index="06"
      alt
      title={
        <>
          子结构嵌套：
          <span className="text-white/50"> 把工厂拆成"主厂 + 双臂 + 工具"</span>
        </>
      }
      lede={
        <>
          一个大型多方块机器强行写成一坨 point 会非常难维护。
          ArchitectCumulus 允许把子定义当作 Component 嵌入父定义，运行时按{" "}
          <strong className="text-brand-300">累加 offset</strong> 复合 child point / region / capture，
          并通过 <em className="text-cyan2">SubstructureLifecycleEvent</em> 把 child 状态/上下文镜像回父上下文。
          每一个 child occurrence 拥有独立 <em className="text-magenta">InstanceDelta</em>，并由{" "}
          <em className="text-lime2">SubstructureClaimState</em> 仲裁多父所有权冲突。
        </>
      }
    >
      <div className="grid lg:grid-cols-2 gap-5 mb-6">
        {/* 3D viewport */}
        <div className="relative rounded-2xl border border-white/8 bg-ink-950/80 overflow-hidden h-[400px]">
          <SubstructureViewport />
          <div className="absolute top-4 left-4 chip bg-ink-950/70 backdrop-blur-md">
            <Layers className="w-3 h-3 text-brand-300" />
            <span className="font-mono text-[11px] text-white/60">
              3-level nesting · auto-rotate
            </span>
          </div>
        </div>

        {/* tree */}
        <div className="rounded-2xl border border-white/8 bg-ink-900/70 p-4">
          <div className="flex items-center gap-2 mb-3">
            <Badge tone="brand">SubstructureGraphResolver</Badge>
            <span className="text-[11px] text-white/40">树形结构</span>
          </div>
          <TreeView node={TREE} />
        </div>
      </div>

      <div className="grid lg:grid-cols-2 gap-4 mb-6">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Badge tone="brand">parent</Badge>
            <span className="text-[12px] text-white/70">cumulus:factory</span>
          </div>
          <CodeBlock code={FACTORY_CODE} title="factory.java" />
        </div>
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Badge tone="cyan">child</Badge>
            <span className="text-[12px] text-white/70">cumulus:arm (内部展开)</span>
          </div>
          <CodeBlock code={ARM_CODE} title="arm.java" />
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="grid md:grid-cols-3 gap-3"
      >
        <div className="rounded-2xl border border-brand-400/20 bg-brand-500/5 p-4">
          <div className="text-[11px] uppercase tracking-wider text-brand-300/70 font-mono mb-2">
            累加 Offset
          </div>
          <div className="text-[12.5px] text-white/70 leading-relaxed">
            tool_a 在 factory 中的最终位置 ={" "}
            <code className="text-brand-200">factory(0,0,0) + arm(-3,0,0) + tool(0,1,0) = (-3,1,0)</code>
          </div>
        </div>
        <div className="rounded-2xl border border-cyan-400/20 bg-cyan-400/5 p-4">
          <div className="text-[11px] uppercase tracking-wider text-cyan2 font-mono mb-2">
            上下文镜像
          </div>
          <div className="text-[12.5px] text-white/70 leading-relaxed">
            STATUS_AND_CONTEXT 模式下，每个 child occurrence 把 status / context 派生回父上下文：
            <code className="block mt-1 text-cyan2 text-[11px]">substructure.&lt;id&gt;.status<br />substructure.&lt;id&gt;.context.&lt;key&gt;</code>
          </div>
        </div>
        <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4">
          <div className="text-[11px] uppercase tracking-wider text-rose2 font-mono mb-2">
            所有权冲突
          </div>
          <div className="text-[12.5px] text-white/70 leading-relaxed">
            两个父结构同时 claim 同一个 child occurrence → SubstructureClaimState 抛出{" "}
            <code className="text-rose2">SHARED_PART_CONFLICT</code>，
            child delta 被作废，第二次父结构会被拒绝。
          </div>
        </div>
      </motion.div>
    </Section>
  );
}
