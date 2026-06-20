import { motion } from "framer-motion";
import { Github, Terminal } from "lucide-react";

export function Footer() {
  return (
    <footer className="relative border-t border-white/8 bg-ink-950/60 px-6 md:px-12 py-12 mt-10">
      <div className="absolute inset-0 grid-bg opacity-20" />
      <div className="relative max-w-[1400px] mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="grid md:grid-cols-4 gap-8 mb-10"
        >
          <div className="md:col-span-1">
            <div className="flex items-center gap-2 mb-3">
              <div className="grid grid-cols-2 gap-0.5 w-8 h-8 rounded-lg overflow-hidden ring-1 ring-white/10">
                <span className="bg-brand-400" />
                <span className="bg-cyan2" />
                <span className="bg-magenta" />
                <span className="bg-lime2" />
              </div>
              <strong className="text-[14px]">ArchitectCumulus</strong>
            </div>
            <p className="text-[12px] text-white/50 leading-relaxed">
              NeoForge library · Java 21 · 多方块结构定义 / 校验 / 运行时一体化
            </p>
            <div className="flex gap-2 mt-3">
              <span className="chip">NeoForge 21.1</span>
              <span className="chip">JDK 21</span>
            </div>
          </div>

          <div>
            <h4 className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-3">
              顶层入口
            </h4>
            <ul className="space-y-1.5 text-[12.5px] text-white/60">
              <li><code className="font-mono">README.md</code></li>
              <li><code className="font-mono">ARCHITECTURE.md</code></li>
              <li><code className="font-mono">USAGE.md</code></li>
              <li><code className="font-mono">IMPLEMENTATION.md</code></li>
            </ul>
          </div>

          <div>
            <h4 className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-3">
              设计文档
            </h4>
            <ul className="space-y-1.5 text-[12.5px] text-white/60">
              <li>2026-06-08 merged architecture</li>
              <li>2026-04-20 structure checking</li>
              <li>2026-04-20 core IR + DSL plan</li>
            </ul>
          </div>

          <div>
            <h4 className="text-[11px] uppercase tracking-wider text-white/40 font-mono mb-3">
              核心入口类
            </h4>
            <ul className="space-y-1.5 text-[12.5px] text-white/60 font-mono">
              <li>Structures · Components</li>
              <li>Targets · PredicateDescriptors</li>
              <li>RuntimeCheckEngine</li>
              <li>StructureEditorController</li>
            </ul>
          </div>
        </motion.div>

        <div className="pt-6 border-t border-white/5 flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-[11.5px] text-white/40">
            本页面是一个静态可视化展示，所有 3D 场景均由 <span className="text-cyan2">three.js</span> +{" "}
            <span className="text-brand-300">@react-three/fiber</span> 在浏览器中渲染。
          </p>
          <div className="flex items-center gap-2">
            <span className="chip">
              <Terminal className="w-3 h-3 text-lime2" />
              <code className="font-mono text-[10.5px]">pnpm dev</code>
            </span>
            <span className="chip">
              <Github className="w-3 h-3 text-white/60" />
              <code className="font-mono text-[10.5px]">vite + react + tailwind</code>
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
}
