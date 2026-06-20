import { cn } from "@/lib/utils";
import { Check, Copy } from "lucide-react";
import { useState } from "react";

interface CodeBlockProps {
  code: string;
  language?: string;
  className?: string;
  title?: string;
  /** 显式最大高度，例如 "220px" / "480px" / "60vh"。设了才会内部滚动；不设就自然撑开 */
  maxHeight?: string;
}

const keywords = new Set([
  "var", "val", "let", "const", "function", "return", "if", "else",
  "for", "while", "import", "from", "export", "default", "class",
  "new", "public", "private", "static", "void", "this", "extends",
  "implements", "interface", "enum", "record", "final", "throw",
  "try", "catch", "null", "true", "false",
]);

const types = new Set([
  "StructureDefinition", "StructurePointDefinition", "TargetDescriptor",
  "PredicateDescriptor", "TransformDescriptor", "ContextRuleDescriptor",
  "StructureVersionDescriptor", "RuntimeCheckEngine", "SectionKey",
  "DataRequest", "InstanceDelta", "RuntimeStructureContext",
  "ResourceLocation", "RelativeBlockPos", "Components", "Targets",
  "Transforms", "Structures", "PredicateDescriptors", "MinecraftServer",
  "ServerTickEvent", "LevelAccessor", "BlockPos", "Direction", "String",
  "List", "Map", "Set", "Optional", "var", "int", "boolean", "void",
]);

function highlight(line: string): JSX.Element[] {
  const tokens: JSX.Element[] = [];
  const regex = /(\/\/[^\n]*|"[^"]*"|'[^']*'|\b\d+(?:\.\d+)?\b|\b\w+\b|[{}()\[\];,.<>=:+\-*/&|!?]|\s+)/g;
  let m: RegExpExecArray | null;
  let i = 0;
  while ((m = regex.exec(line))) {
    const tk = m[0];
    if (tk.startsWith("//")) {
      tokens.push(<span key={i++} className="text-white/30 italic">{tk}</span>);
    } else if (tk.startsWith('"') || tk.startsWith("'")) {
      tokens.push(<span key={i++} className="text-lime2">{tk}</span>);
    } else if (/^\d/.test(tk)) {
      tokens.push(<span key={i++} className="text-amber2">{tk}</span>);
    } else if (keywords.has(tk)) {
      tokens.push(<span key={i++} className="text-magenta font-medium">{tk}</span>);
    } else if (types.has(tk)) {
      tokens.push(<span key={i++} className="text-cyan2">{tk}</span>);
    } else if (/^[A-Z]\w*$/.test(tk)) {
      tokens.push(<span key={i++} className="text-cyan2/80">{tk}</span>);
    } else if (/^[a-z_]\w*$/.test(tk)) {
      tokens.push(<span key={i++} className="text-brand-100">{tk}</span>);
    } else if (/^\s+$/.test(tk)) {
      tokens.push(<span key={i++}>{tk}</span>);
    } else {
      tokens.push(<span key={i++} className="text-white/50">{tk}</span>);
    }
  }
  return tokens;
}

export function CodeBlock({
  code,
  language = "java",
  className,
  title,
  maxHeight,
}: CodeBlockProps) {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  // 关键修复：
  // - 外层 flex flex-col 让 max-h 限制整个盒子
  // - header 用 shrink-0 不参与剩余空间分配
  // - 内容区 flex-1 min-h-0 overflow-auto，剩余空间内双向滚动
  // - 内容里再嵌 inline-block min-w-full，长行才能触发横向滚动
  const outerStyle = maxHeight
    ? { maxHeight }
    : undefined;

  return (
    <div
      style={outerStyle}
      className={cn(
        "rounded-xl border border-white/8 bg-ink-950/90 flex flex-col overflow-hidden",
        className
      )}
    >
      <div className="flex items-center justify-between px-3 py-2 border-b border-white/5 bg-white/[0.02] shrink-0">
        <div className="flex items-center gap-2 min-w-0">
          <div className="flex gap-1.5 shrink-0">
            <span className="w-2.5 h-2.5 rounded-full bg-rose2/60" />
            <span className="w-2.5 h-2.5 rounded-full bg-amber2/60" />
            <span className="w-2.5 h-2.5 rounded-full bg-lime2/60" />
          </div>
          <span className="text-[11px] font-mono text-white/40 ml-2 truncate">
            {title ?? language}
          </span>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <span className="text-[10px] font-mono text-white/30">
            {code.split("\n").length} 行
          </span>
          <button
            onClick={copy}
            className="text-white/30 hover:text-white/70 transition-colors"
            aria-label="copy"
          >
            {copied ? (
              <Check className="w-3.5 h-3.5 text-lime2" />
            ) : (
              <Copy className="w-3.5 h-3.5" />
            )}
          </button>
        </div>
      </div>
      <div className="flex-1 min-h-0 overflow-auto scrollbar-thin">
        <pre className="p-4 text-[12.5px] leading-[1.65] font-mono m-0">
          <code className="inline-block min-w-full">
            {code.split("\n").map((line, idx) => (
              <div key={idx} className="whitespace-pre hover:bg-white/[0.03] -mx-4 px-4">
                <span className="select-none text-white/20 mr-3 inline-block w-7 text-right align-top">
                  {idx + 1}
                </span>
                <span className="whitespace-pre">
                  {highlight(line)}
                  {" "}
                </span>
              </div>
            ))}
          </code>
        </pre>
      </div>
    </div>
  );
}
