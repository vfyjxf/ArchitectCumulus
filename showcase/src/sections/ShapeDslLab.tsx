import { Section } from "@/components/ui/Section";
import { Badge } from "@/components/ui/Badge";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { CodeTabs } from "@/components/ui/CodeTabs";
import { motion } from "framer-motion";
import { cn } from "@/lib/utils";
import { useState } from "react";

/** 特性卡片 */
function FeatureCard({
  index, title, desc, accent, children,
}: {
  index: string; title: string; desc: string;
  accent: string; children: React.ReactNode;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px" }}
      transition={{ duration: 0.5 }}
      className="rounded-2xl border border-white/8 bg-white/[0.02] overflow-hidden"
    >
      <div className={cn("p-4 border-b border-white/5", `bg-${accent}-500/5`)}>
        <div className="flex items-baseline gap-3">
          <span className="font-mono text-[10px] text-white/30 tracking-wider">{index}</span>
          <h3 className="text-[15px] font-bold text-white/90">{title}</h3>
        </div>
        <p className="text-[12px] text-white/50 mt-1 leading-relaxed">{desc}</p>
      </div>
      <div className="p-4">{children}</div>
    </motion.div>
  );
}

export function ShapeDslLab() {
  const [activeDemo, setActiveDemo] = useState<string>("shape");

  const demos: Record<string, { title: string; code: string; maxHeight: string }> = {
    shape: {
      title: "PartPattern.java",
      maxHeight: "420px",
      code: `import static dev.vfyjxf.cumulus.api.definition.Structures.*;
import static dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors.*;

// 整个结构在一个 static final 表达式里
public static final StructureDefinition FORGE = Structures.define(
    "cumulus:forge",
    def -> {
        // 1. 声明 PartPattern（强类型，预绑定具体值）
        var casing = PartPattern.block("casing")
            .where(tag("cumulus:heat_casing"))
            .group("casing");

        var ctrl = PartPattern.block("controller")
            .where(id("cumulus:controller"))
            .capture("controller");

        var energy = PartPattern.capability("energy",
            "cumulus:energy").group("port");

        var air = PartPattern.air();

        // 2. 多维数组 [y][z][x] 画出 3D 结构
        return def.shape(new PartPattern[][][]{ {
            // Y=0 底层
            { casing, casing,  casing },
            { casing, ctrl,    casing },
            { casing, casing,  casing },
        }, {
            // Y=1 顶层
            { casing, casing, casing },
            { casing, casing, casing },
            { casing, casing, casing },
        }, {
            // Y=2 前层
            { casing, energy, casing },
            { casing, air,    casing },
            { casing, casing, casing },
        } })
        .transforms(Transforms.horizontalFacing())
        .build();
    });`,
    },
    emitter: {
      title: "MirrorEmitter.java",
      maxHeight: "420px",
      code: `// 自定义 ShapeEmitter：沿 X 轴镜像展开
public final class MirrorEmitter implements ShapeEmitter {
    @Override
    public void emit(PartPattern[][][] shape, ShapeEmitterContext ctx) {
        for (int y = 0; y < shape.length; y++)
            for (int z = 0; z < shape[y].length; z++)
                for (int x = 0; x < shape[y][z].length; x++) {
                    var part = shape[y][z][x];
                    if (part == null || part.isAir()) continue;
                    // 原始位置
                    ctx.point(x, y, z, part);
                    // 镜像位置
                    int mx = shape[y][z].length - 1 - x;
                    if (mx != x) ctx.point(mx, y, z, part);
                }
    }
}

// 使用：
.shape(halfShape, new MirrorEmitter())`,
    },
    checkPhase: {
      title: "AdaptiveTower.java",
      maxHeight: "420px",
      code: `import static dev.vfyjxf.cumulus.api.context.plan.stdlib.CheckPhases.*;

public static final StructureDefinition TOWER = Structures.define(
    "cumulus:tower",
    def -> {
        var ctrl = PartPattern.block("controller")
            .where(id("cumulus:controller"))
            .capture("controller");

        return def.shape(new PartPattern[][][]{ {
            { ctrl },
        } })
        // CheckPhase: 静态匹配后动态扩展
        .checkPhase(repeatRange(
            "ring",
            Components.hollowBox("ring",
                RelativeBlockPos.at(-1, 0, -1),
                RelativeBlockPos.at(1, 0, 2)),
            Axis.y, 2, 5, TOWER_HEIGHT))
        .build();
    });

// 执行流程：
// 1. 匹配 controller (静态 point)
// 2. CheckPhase.evaluate(ctx) → repeatRange 生成 2~5 层 point
// 3. executor 逐层匹配（L0,L1 required; L2~L4 optional）
// 4. 匹配的层数写入 TOWER_HEIGHT context key
// 5. toRuntime() → 机器读取 TOWER_HEIGHT`,
    },
    countConstraint: {
      title: "CoilMachine.java",
      maxHeight: "420px",
      code: `import static dev.vfyjxf.cumulus.api.predicate.StructurePredicates.*;

public static final StructureDefinition COIL = Structures.define(
    "cumulus:coil_machine",
    def -> {
        var coil = PartPattern.block("coil")
            .where(tag("cumulus:coil")).group("coil");
        var port = PartPattern.capability("port",
            "cumulus:energy").group("port");

        return def.shape(new PartPattern[][][]{ {
            { coil, coil, coil },
            { coil, port, coil },
            { coil, coil, coil },
        } })
        // Count 约束：在 CheckPhase 里声明
        .checkPhase(ctx -> PhaseResult.of(
            List.of(),
            List.of(
                CountConstraint.atLeast(group("coil"), 8),
                CountConstraint.atMost(group("port"), 4),
                CountConstraint.exactly(tag("cumulus:coil"), 4)
            )))
        .build();
    });

// executor 验证：
// count(group("coil")) >= 8 ? ✓ : invalid
// count(group("port")) <= 4 ? ✓ : invalid`,
    },
    predicate: {
      title: "PredicateComposition.java",
      maxHeight: "420px",
      code: `import static dev.vfyjxf.cumulus.api.predicate.StructurePredicates.*;

// StructurePredicate extends Predicate<MatchedPoint>
// 完整支持 AND / OR / NOT

// "是 casing 且耐热，但不是 controller"
StructurePredicate heatedCasing = id("cumulus:casing")
    .and(property("heat_resistant", "true"))
    .and(id("cumulus:controller").negate());

// "属于 casing 组或 port 组"
StructurePredicate structurePart = group("casing")
    .or(group("port"));

// "不是空气"
StructurePredicate notAir = id("minecraft:air").negate();

// 给 MatchResults 用：
long heatedCount = matches.count(heatedCasing);
boolean hasPort = matches.first(structurePart).isPresent();

// 给 CountConstraint 用：
CountConstraint c = CountConstraint.atLeast(heatedCasing, 8);

// 给 CheckPhase 的 PhaseContext.matches() 用：
ctx.matches().matching(notAir).count();`,
    },
  };

  return (
    <Section
      id="shape-dsl"
      index="04"
      title={
        <>
          Shape DSL Lab
          <span className="text-white/50"> · PartPattern 矩阵 + CheckPhase + 谓词组合</span>
        </>
      }
      lede={
        <>
          集中展示最近新增的核心 API 能力。用
          <strong className="text-white/80"> PartPattern 多维数组</strong>直观画出 3D 结构，
          <strong className="text-white/80"> CheckPhase</strong> 动态判定结构形态，
          <strong className="text-white/80"> CountConstraint</strong> 限制数量，
          <strong className="text-white/80"> StructurePredicate</strong> 组合过滤。全部强类型，编译期检查。
        </>
      }
    >
      {/* 特性概览 */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        <FeatureCard index="01" title="PartPattern 矩阵" desc="用多维数组 [y][z][x] 画出结构，每个格子是预绑定的 PartPattern。类似 GT aisle 但强类型。" accent="brand">
          <div className="font-mono text-[11px] text-white/50 space-y-1">
            <div><span className="text-brand-300">PartPattern.block("casing")</span></div>
            <div className="text-white/30">  .where(tag("...")).group("casing")</div>
            <div className="text-white/30 mt-2">// 数组里直接用</div>
            <div><span className="text-brand-200">{`{ casing, casing, casing }`}</span></div>
          </div>
        </FeatureCard>

        <FeatureCard index="02" title="ShapeEmitter" desc="底层展开引擎。默认逐格展开，自定义可实现镜像/旋转/条件布局。" accent="cyan">
          <div className="font-mono text-[11px] text-white/50 space-y-1">
            <div><span className="text-cyan2">.shape(matrix)</span> <span className="text-white/30">// 默认</span></div>
            <div><span className="text-cyan2">.shape(matrix, emitter)</span> <span className="text-white/30">// 自定义</span></div>
            <div className="text-white/30 mt-2">emit → ShapeEmitterContext.point(x,y,z,part)</div>
          </div>
        </FeatureCard>

        <FeatureCard index="03" title="CheckPhase" desc="静态匹配后动态扩展 point + 验证合规性。repeatRange 是便利用法。" accent="magenta">
          <div className="font-mono text-[11px] text-white/50 space-y-1">
            <div><span className="text-magenta">.checkPhase(ctx {"->"} PhaseResult.of(...))</span></div>
            <div className="text-white/30 mt-2">repeatRange(template, axis, min, max)</div>
            <div className="text-white/30">PhaseResult.of(points, constraints)</div>
          </div>
        </FeatureCard>

        <FeatureCard index="04" title="CountConstraint" desc="全局/分组数量约束。在 PhaseResult 里声明，executor 自动验证。" accent="amber">
          <div className="font-mono text-[11px] text-white/50 space-y-1">
            <div><span className="text-amber2">CountConstraint.atLeast(filter, 8)</span></div>
            <div><span className="text-amber2">CountConstraint.atMost(filter, 4)</span></div>
            <div><span className="text-amber2">CountConstraint.exactly(filter, 4)</span></div>
          </div>
        </FeatureCard>

        <FeatureCard index="05" title="StructurePredicate" desc="完整 AND/OR/NOT 谓词组合，测试 MatchedPoint。extends Predicate。" accent="lime">
          <div className="font-mono text-[11px] text-white/50 space-y-1">
            <div><span className="text-lime2">id("...").and(tag("..."))</span></div>
            <div><span className="text-lime2">group("...").or(group("..."))</span></div>
            <div><span className="text-lime2">id("...").negate()</span></div>
          </div>
        </FeatureCard>

        <FeatureCard index="06" title="Lambda 定义" desc="整个结构在一个 static final 表达式里完成，不泄漏局部变量。" accent="rose">
          <div className="font-mono text-[11px] text-white/50 space-y-1">
            <div><span className="text-rose2">Structures.define("id", def {"->"} {`{ ... return def.build(); })`}</span></div>
            <div className="text-white/30 mt-2">+ id() / at() 简写</div>
          </div>
        </FeatureCard>
      </div>

      {/* 代码演示 tab */}
      <div className="flex items-center gap-2 mb-3 flex-wrap">
        {Object.entries(demos).map(([key, d]) => (
          <button
            key={key}
            onClick={() => setActiveDemo(key)}
            className={cn(
              "px-3 py-1.5 rounded-lg font-mono text-[11px] border transition-colors",
              activeDemo === key
                ? "border-brand-400/60 bg-brand-500/15 text-brand-200"
                : "border-white/10 text-white/50 hover:border-white/20 hover:text-white/70"
            )}
          >
            {key === "shape" ? "PartPattern 矩阵" :
             key === "emitter" ? "自定义 Emitter" :
             key === "checkPhase" ? "CheckPhase" :
             key === "countConstraint" ? "Count 约束" :
             "谓词组合"}
          </button>
        ))}
      </div>

      <CodeBlock
        title={demos[activeDemo].title}
        maxHeight={demos[activeDemo].maxHeight}
        code={demos[activeDemo].code}
      />

      {/* 执行管线图 */}
      <div className="mt-6 rounded-2xl border border-white/8 bg-white/[0.02] p-5">
        <div className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-3">
          完整执行管线 · Shape DSL → IR → Runtime
        </div>
        <div className="flex flex-wrap items-center gap-2 text-[11px] font-mono">
          {[
            { label: "PartPattern[][][]", color: "text-brand-200", bg: "bg-brand-500/10" },
            { label: "ShapeEmitter.emit()", color: "text-cyan2", bg: "bg-cyan-500/10" },
            { label: "StructurePointDefinition × N", color: "text-magenta", bg: "bg-fuchsia-500/10" },
            { label: "Compiler → IR", color: "text-amber2", bg: "bg-amber-500/10" },
            { label: "matchPoint()", color: "text-lime2", bg: "bg-lime-500/10" },
            { label: "CheckPhase", color: "text-rose2", bg: "bg-rose-500/10" },
            { label: "constraints 验证", color: "text-amber2", bg: "bg-amber-500/10" },
            { label: "toRuntime()", color: "text-brand-200", bg: "bg-brand-500/10" },
          ].map((step, i, arr) => (
            <div key={step.label} className="flex items-center gap-2">
              <span className={cn("px-2.5 py-1 rounded-md border border-white/10", step.bg, step.color)}>
                {step.label}
              </span>
              {i < arr.length - 1 && <span className="text-white/20">→</span>}
            </div>
          ))}
        </div>
      </div>
    </Section>
  );
}
