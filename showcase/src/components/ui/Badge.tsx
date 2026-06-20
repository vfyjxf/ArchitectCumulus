import { cn } from "@/lib/utils";
import type { HTMLAttributes } from "react";

type Tone =
  | "brand"
  | "cyan"
  | "magenta"
  | "lime"
  | "amber"
  | "rose"
  | "slate"
  | "neutral";

const tones: Record<Tone, string> = {
  brand: "bg-brand-500/15 text-brand-200 border-brand-400/30",
  cyan: "bg-cyan-400/10 text-cyan2 border-cyan-400/30",
  magenta: "bg-pink-400/10 text-magenta border-pink-400/30",
  lime: "bg-lime-400/10 text-lime2 border-lime-400/30",
  amber: "bg-amber-400/10 text-amber2 border-amber-400/30",
  rose: "bg-rose-500/15 text-rose-300 border-rose-500/30",
  slate: "bg-slate-500/10 text-slate-300 border-slate-400/30",
  neutral: "bg-white/5 text-white/60 border-white/10",
};

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: Tone;
}

export function Badge({
  className,
  tone = "neutral",
  children,
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md text-[11px] font-semibold border tracking-wide",
        tones[tone],
        className
      )}
      {...props}
    >
      {children}
    </span>
  );
}
