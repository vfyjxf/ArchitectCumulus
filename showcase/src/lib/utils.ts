import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const cubicIn = [0.32, 0, 0.67, 0] as const;
export const cubicOut = [0.33, 1, 0.68, 1] as const;
export const cubicInOut = [0.65, 0, 0.35, 1] as const;
