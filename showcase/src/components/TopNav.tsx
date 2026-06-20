import { cn } from "@/lib/utils";
import { AnimatePresence, motion } from "framer-motion";
import { Menu, X } from "lucide-react";
import { useEffect, useState } from "react";

const sections = [
  { id: "overview", label: "概览" },
  { id: "pipeline", label: "管线" },
  { id: "rationale", label: "设计决策" },
  { id: "builder", label: "3D 构造器" },
  { id: "transform-lab", label: "Transform Lab" },
  { id: "composer", label: "结构组装" },
  { id: "targets", label: "Target 图鉴" },
  { id: "predicate-composer", label: "谓词链" },
  { id: "components", label: "Component" },
  { id: "runtime", label: "Runtime 调度" },
  { id: "substructure", label: "子结构" },
  { id: "lifecycle-lab", label: "生命周期" },
  { id: "scenarios", label: "游戏场景" },
  { id: "cases", label: "vanilla 对比" },
  { id: "datadriven", label: "数据驱动" },
  { id: "cookbook", label: "API Cookbook" },
  { id: "diagnostics", label: "诊断图" },
  { id: "playground", label: "Playground" },
];

export function TopNav({ progress }: { progress: number }) {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState<string>("overview");

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    const obs = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio);
        if (visible[0]) setActive(visible[0].target.id);
      },
      { rootMargin: "-40% 0px -40% 0px" }
    );
    sections.forEach((s) => {
      const el = document.getElementById(s.id);
      if (el) obs.observe(el);
    });
    return () => obs.disconnect();
  }, []);

  const go = (id: string) => {
    setOpen(false);
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <>
      {/* progress bar */}
      <div className="fixed top-0 left-0 right-0 h-0.5 z-50 pointer-events-none">
        <div
          className="h-full bg-gradient-to-r from-brand-400 via-cyan2 to-magenta transition-[width] duration-150"
          style={{ width: `${progress * 100}%` }}
        />
      </div>

      <motion.nav
        initial={{ y: -80, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6 }}
        className={cn(
          "fixed top-0 left-0 right-0 z-40 transition-all duration-300",
          scrolled
            ? "bg-ink-950/80 backdrop-blur-xl border-b border-white/5"
            : "bg-transparent"
        )}
      >
        <div className="max-w-[1400px] mx-auto px-6 md:px-12 h-16 flex items-center justify-between">
          <button
            onClick={() => go("overview")}
            className="flex items-center gap-3 group"
          >
            <div className="grid grid-cols-2 gap-0.5 w-8 h-8 rounded-lg overflow-hidden ring-1 ring-white/10 group-hover:ring-brand-400/40 transition">
              <span className="bg-brand-400" />
              <span className="bg-cyan2" />
              <span className="bg-magenta" />
              <span className="bg-lime2" />
            </div>
            <div className="hidden sm:flex flex-col leading-tight text-left">
              <span className="text-[13px] font-semibold text-white/90">
                ArchitectCumulus
              </span>
              <span className="text-[10px] text-white/40 font-mono">
                multiblock · design · showcase
              </span>
            </div>
          </button>

          <ul className="hidden lg:flex items-center gap-1">
            {sections.map((s) => (
              <li key={s.id}>
                <button
                  onClick={() => go(s.id)}
                  className={cn(
                    "px-3 py-1.5 rounded-lg text-[12.5px] font-medium transition-all",
                    active === s.id
                      ? "text-white bg-white/10"
                      : "text-white/50 hover:text-white/80"
                  )}
                >
                  {s.label}
                </button>
              </li>
            ))}
          </ul>

          <button
            onClick={() => setOpen(true)}
            className="lg:hidden text-white/70 hover:text-white p-1"
            aria-label="menu"
          >
            <Menu className="w-5 h-5" />
          </button>
        </div>
      </motion.nav>

      {/* mobile menu */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-ink-950/95 backdrop-blur-xl p-6"
          >
            <div className="flex justify-end mb-8">
              <button
                onClick={() => setOpen(false)}
                className="text-white/60 hover:text-white"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
            <ul className="space-y-2">
              {sections.map((s) => (
                <li key={s.id}>
                  <button
                    onClick={() => go(s.id)}
                    className="w-full text-left text-2xl font-semibold text-white/80 hover:text-white py-2"
                  >
                    {s.label}
                  </button>
                </li>
              ))}
            </ul>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
