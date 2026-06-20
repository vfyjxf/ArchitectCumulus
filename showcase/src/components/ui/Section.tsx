import { cn } from "@/lib/utils";
import { motion } from "framer-motion";
import type { ReactNode } from "react";

interface SectionProps {
  id?: string;
  index: string;
  title: ReactNode;
  lede?: ReactNode;
  children: ReactNode;
  alt?: boolean;
  className?: string;
}

export function Section({
  id,
  index,
  title,
  lede,
  children,
  alt,
  className,
}: SectionProps) {
  return (
    <section
      id={id}
      className={cn(
        "relative px-6 md:px-12 py-20 md:py-28 max-w-[1400px] mx-auto",
        alt && "before:absolute before:inset-0 before:bg-gradient-to-b before:from-white/[0.02] before:to-transparent before:-z-10",
        className
      )}
    >
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.7, ease: [0.33, 1, 0.68, 1] }}
        className="mb-10 md:mb-14"
      >
        <div className="flex items-baseline gap-3 mb-3">
          <span className="font-mono text-[12px] text-brand-300/60 tracking-widest">
            {index}
          </span>
          <span className="h-px w-12 bg-gradient-to-r from-brand-400/40 to-transparent" />
        </div>
        <h2 className="text-2xl md:text-4xl font-bold tracking-tight text-white/95 mb-3">
          {title}
        </h2>
        {lede && (
          <p className="text-[14px] md:text-[15px] text-white/55 leading-relaxed max-w-3xl">
            {lede}
          </p>
        )}
      </motion.div>
      {children}
    </section>
  );
}
