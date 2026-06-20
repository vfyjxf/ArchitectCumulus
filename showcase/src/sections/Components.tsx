import { motion } from "framer-motion";
import { Boxes } from "lucide-react";
import { Section } from "@/components/ui/Section";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Badge } from "@/components/ui/Badge";
import { ComponentPreview } from "@/three/ComponentPreview";
import { COMPONENTS } from "@/data/components";
import { cn } from "@/lib/utils";

function ComponentCard({
  c,
  index,
}: {
  c: (typeof COMPONENTS)[number];
  index: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px" }}
      transition={{ delay: index * 0.1, duration: 0.5 }}
      className="rounded-2xl border border-white/8 bg-ink-900/70 backdrop-blur-xl overflow-hidden"
    >
      {/* 3D preview */}
      <div className="relative h-[200px] bg-gradient-to-br from-ink-950 to-ink-800/50 border-b border-white/5">
        <ComponentPreview demo={c.demo} color={c.color} />
        <div className="absolute top-3 left-3">
          <code
            className="font-mono text-[12px] font-semibold px-2 py-1 rounded-md bg-ink-950/70 border"
            style={{ color: c.color, borderColor: c.color + "40" }}
          >
            {c.signature}
          </code>
        </div>
        <div className="absolute bottom-3 right-3 chip bg-ink-950/70 text-white/60">
          <span
            className="w-1.5 h-1.5 rounded-full"
            style={{ background: c.color }}
          />
          {c.emits}
        </div>
      </div>

      <div className="p-5">
        <div className="flex items-baseline justify-between mb-2">
          <h3
            className="text-[16px] font-semibold"
            style={{ color: c.color }}
          >
            {c.title}
          </h3>
          <span className="font-mono text-[10.5px] text-white/40">
            emits: <span className="text-white/70">{c.count}</span>
          </span>
        </div>

        <p className="text-[12.5px] text-white/60 leading-relaxed mb-3">
          {c.description}
        </p>

        <CodeBlock code={c.api} title={`${c.id}.java`} />
      </div>
    </motion.div>
  );
}

export function Components() {
  return (
    <Section
      id="components"
      index="04"
      alt
      title={
        <>
          Component 原语：
          <span className="text-white/50"> 从 1 个方块到一整条流水线</span>
        </>
      }
      lede={
        <>
          Component 是 IR 的发射器。一个{" "}
          <em className="text-brand-300">point</em> 发射 1 个 StructurePoint；
          一个 <em className="text-cyan2">box</em> 发射 N×M×K 个；
          <em className="text-magenta">hollowBox</em> 只发射外壳；
          <em className="text-rose2">region</em> 只发射 1 个区域 Target（不展开成方块）；
          <em className="text-lime2">substructure</em> 不展开，但在运行时按 offset 拼接子定义。
        </>
      }
    >
      <div className="grid lg:grid-cols-2 gap-4">
        {COMPONENTS.map((c, i) => (
          <ComponentCard key={c.id} c={c} index={i} />
        ))}
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="mt-6 p-5 rounded-2xl border border-white/8 bg-gradient-to-br from-brand-500/5 to-cyan-400/5"
      >
        <div className="flex items-start gap-3">
          <div className="w-9 h-9 rounded-lg bg-brand-500/15 border border-brand-400/30 flex items-center justify-center shrink-0">
            <Boxes className="w-4 h-4 text-brand-300" />
          </div>
          <div>
            <div className="text-[13px] font-semibold text-white/90 mb-1">
              何时用哪种 Component？
            </div>
            <div className="text-[12.5px] text-white/60 leading-relaxed grid sm:grid-cols-2 gap-x-6 gap-y-1">
              <div><Badge tone="brand">point</Badge> 唯一的 controller / port / 关键方块</div>
              <div><Badge tone="cyan">box</Badge> 实心小立方体 (≤2³)，外壳=内部</div>
              <div><Badge tone="magenta">hollowBox</Badge> 任何机壳 / 房间 / 反应堆壁</div>
              <div><Badge tone="rose">region</Badge> 门、通道、净空区（不展开方块）</div>
              <div><Badge tone="lime">substructure</Badge> 把工厂拆成 base + arm + tool</div>
              <div className="text-white/40 text-[11px] mt-1">
                注：box 在 (dx+1)(dy+1)(dz+1) &gt; 8 时考虑切换 hollowBox
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </Section>
  );
}
