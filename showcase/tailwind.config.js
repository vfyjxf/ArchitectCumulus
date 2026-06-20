/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          950: "#070710",
          900: "#0c0c1a",
          800: "#13132a",
          700: "#1c1c3b",
          600: "#272750",
          500: "#3a3a72",
        },
        brand: {
          50: "#eef0ff",
          100: "#dfe3ff",
          200: "#bcbcff",
          300: "#9a93ff",
          400: "#7c5cff",
          500: "#5d3df0",
          600: "#4828c4",
          700: "#371d96",
        },
        cyan2: "#22d3ee",
        magenta: "#f0abfc",
        lime2: "#a3e635",
        amber2: "#fbbf24",
        rose2: "#fb7185",
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui"],
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
      },
      keyframes: {
        pulse: {
          "0%,100%": { opacity: 1 },
          "50%": { opacity: 0.4 },
        },
        floaty: {
          "0%,100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-6px)" },
        },
        gridmove: {
          "0%": { backgroundPosition: "0 0" },
          "100%": { backgroundPosition: "60px 60px" },
        },
        shimmer: {
          "0%": { backgroundPosition: "-200% 0" },
          "100%": { backgroundPosition: "200% 0" },
        },
      },
      animation: {
        pulse: "pulse 2.4s ease-in-out infinite",
        floaty: "floaty 4s ease-in-out infinite",
        gridmove: "gridmove 8s linear infinite",
        shimmer: "shimmer 6s linear infinite",
      },
    },
  },
  plugins: [],
};
