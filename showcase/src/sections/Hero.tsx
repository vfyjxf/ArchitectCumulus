import { motion } from "framer-motion";
import { ArrowDown, Boxes, Sparkles, Zap } from "lucide-react";
import { HeroCube } from "@/three/HeroCube";
import { Badge } from "@/components/ui/Badge";

const stats = [
  {
    num: "7",
    label: "Target 类型",
    detail: "block · block_entity · entity · fluid · capability · context · region",
    color: "text-brand-300",
  },
  {
    num: "5",
    label: "Component 原语",
    detail: "point · box · hollowBox · region · substructure",
    color: "text-cyan2",
  },
  {
    num: "11",
    label: "诊断 Code",
    detail: "missingTarget / wrongTargetType / predicateFailed ...",
    color: "text-magenta",
  },
  {
    num: "∞",
    label: "嵌套子结构",
    detail: "父→子→孙 offset 累加 + 所有权仲裁",
    color: "text-lime2",
  },
];

export function Hero() {
  return (
    <section
      id="overview"
      className="relative min-h-screen pt-24 pb-16 overflow-hidden"
    >
      <div className="absolute inset-0 grid-bg opacity-30" />
      <div className="absolute top-0 right-0 w-[60%] h-[120%] bg-gradient-radial from-brand-500/15 via-transparent to-transparent blur-3xl" />

      <div className="relative max-w-[1400px] mx-auto px-6 md:px-12 grid lg:grid-cols-[1.3fr,1fr] gap-10 items-center">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.33, 1, 0.68, 1] }}
        >
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/5 border border-white/10 mb-6">
            <span className="relative flex w-2 h-2">
              <span className="absolute inline-flex w-full h-full rounded-full bg-lime2 opacity-75 animate-ping" />
              <span className="relative inline-flex w-2 h-2 rounded-full bg-lime2" />
            </span>
            <span className="text-[12px] text-white/60 font-mono">
              NeoForge · Java 21 · multiblock design runtime
            </span>
          </div>

          <h1 className="text-4xl md:text-6xl lg:text-7xl font-bold tracking-tight leading-[1.05]">
            把<span className="grad-text">多方块</span>当作
            <br />
            <span className="text-shimmer">数据结构</span>来设计
          </h1>

          <p className="mt-6 text-[15px] md:text-[17px] text-white/55 leading-relaxed max-w-2xl">
            ArchitectCumulus 把"多方块机器"从硬编码 Java 检查脚本，抽象成{" "}
            <span className="text-white/90 font-medium">
              Java DSL → 不可变 IR → 编译检查计划 → 运行时引擎 → 诊断图
            </span>
            的完整管线。这条管线支持方块、方块实体、实体、流体、能力、上下文、矩形区域七种 Target，并能在原版 Minecraft 服务器里以 Section 粒度做预算受限的脏块调度。
          </p>

          <div className="flex flex-wrap gap-2 mt-8 mb-10">
            <Badge tone="brand"><Boxes className="w-3 h-3" /> 不可变 IR</Badge>
            <Badge tone="cyan"><Zap className="w-3 h-3" /> Fast Tick</Badge>
            <Badge tone="magenta"><Sparkles className="w-3 h-3" /> Ghost Preview</Badge>
            <Badge tone="lime">Dirty-Section</Badge>
            <Badge tone="amber">Datagen</Badge>
            <Badge tone="rose">Substructure Nesting</Badge>
          </div>

          <div className="flex flex-wrap gap-3">
            <a
              href="#pipeline"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-b from-brand-400 to-brand-600 text-white font-medium text-[13px] border border-brand-300/30 shadow-[0_8px_28px_-6px_rgba(124,92,255,0.6)] hover:from-brand-300 hover:to-brand-500 transition-all"
            >
              查看完整管线
              <ArrowDown className="w-3.5 h-3.5" />
            </a>
            <a
              href="#builder"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-white/80 hover:text-white font-medium text-[13px] border border-white/10 transition-all"
            >
              打开 3D 构造器
            </a>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-12">
            {stats.map((s, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 + i * 0.1, duration: 0.5 }}
                className="glass rounded-xl p-3"
              >
                <div className={`text-3xl font-bold ${s.color}`}>{s.num}</div>
                <div className="text-[12px] text-white/70 font-medium mt-1">
                  {s.label}
                </div>
                <div className="text-[10.5px] text-white/40 font-mono mt-1 leading-tight">
                  {s.detail}
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 1, delay: 0.4 }}
          className="h-[400px] lg:h-[560px] relative"
        >
          <div className="absolute inset-0 rounded-3xl bg-gradient-to-br from-brand-500/10 via-transparent to-cyan2/5 border border-white/5 overflow-hidden">
            <HeroCube />
            <div className="absolute top-4 left-4 chip bg-ink-950/70 backdrop-blur-md text-cyan2">
              <span className="w-1.5 h-1.5 rounded-full bg-lime2 animate-pulse" />
              live · three.js
            </div>
            <div className="absolute bottom-4 right-4 text-[10.5px] font-mono text-white/30">
              drag to rotate · scroll to zoom
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
