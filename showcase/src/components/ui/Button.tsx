import { cn } from "@/lib/utils";
import { forwardRef, type ButtonHTMLAttributes } from "react";

type Variant = "primary" | "ghost" | "outline" | "danger" | "subtle";
type Size = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
}

const variantClasses: Record<Variant, string> = {
  primary:
    "bg-gradient-to-b from-brand-400 to-brand-600 text-white border border-brand-300/30 shadow-[0_4px_20px_-4px_rgba(124,92,255,0.6)] hover:from-brand-300 hover:to-brand-500",
  ghost:
    "bg-transparent text-white/70 hover:text-white hover:bg-white/5 border border-transparent",
  outline:
    "bg-ink-900/40 text-white/80 border border-white/10 hover:border-brand-400/40 hover:bg-ink-800/60",
  danger:
    "bg-rose-500/15 text-rose-300 border border-rose-500/30 hover:bg-rose-500/25",
  subtle:
    "bg-white/5 text-white/70 border border-white/5 hover:bg-white/10",
};

const sizeClasses: Record<Size, string> = {
  sm: "text-[12px] px-2.5 py-1.5 rounded-lg gap-1.5",
  md: "text-[13px] px-3.5 py-2 rounded-xl gap-2",
  lg: "text-[14px] px-5 py-2.5 rounded-xl gap-2",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "outline", size = "md", children, ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={cn(
          "inline-flex items-center justify-center font-medium transition-all duration-200 disabled:opacity-50 disabled:pointer-events-none select-none",
          variantClasses[variant],
          sizeClasses[size],
          className
        )}
        {...props}
      >
        {children}
      </button>
    );
  }
);
Button.displayName = "Button";
