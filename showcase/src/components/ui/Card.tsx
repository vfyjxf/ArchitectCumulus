import { cn } from "@/lib/utils";
import { motion, type HTMLMotionProps } from "framer-motion";
import { forwardRef } from "react";

interface CardProps extends HTMLMotionProps<"div"> {
  glow?: boolean;
  hover?: boolean;
}

export const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ className, glow, hover, children, ...props }, ref) => {
    return (
      <motion.div
        ref={ref}
        className={cn(
          "relative rounded-2xl border border-white/8 bg-ink-900/70 backdrop-blur-xl p-5",
          glow && "shadow-[0_0_0_1px_rgba(124,92,255,0.15),0_18px_60px_-15px_rgba(124,92,255,0.25)]",
          hover &&
            "transition-colors hover:border-brand-400/30 hover:bg-ink-800/80",
          className
        )}
        {...props}
      >
        {children}
      </motion.div>
    );
  }
);
Card.displayName = "Card";

export function CardTitle({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <h3
      className={cn(
        "text-base font-semibold text-white/90 mb-1 flex items-center gap-2",
        className
      )}
    >
      {children}
    </h3>
  );
}

export function CardDescription({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <p className={cn("text-[13px] text-white/50 leading-relaxed", className)}>
      {children}
    </p>
  );
}
