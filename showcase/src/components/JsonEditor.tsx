import { useMemo, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";

interface JsonEditorProps {
  value: string;
  onChange: (s: string) => void;
  errorLine?: number;
  errorColumn?: number;
  className?: string;
}

// 极简 JSON 语法高亮：用 dangerouslySetInnerHTML 渲染高亮后的 HTML
function highlightJson(text: string): string {
  // 简单 tokenizer
  const tokens: string[] = [];
  let i = 0;
  while (i < text.length) {
    const ch = text[i];
    // whitespace
    if (ch === " " || ch === "\t") {
      let j = i;
      while (j < text.length && (text[j] === " " || text[j] === "\t")) j++;
      tokens.push(escape(text.slice(i, j)));
      i = j;
      continue;
    }
    // newline
    if (ch === "\n") {
      tokens.push("\n");
      i++;
      continue;
    }
    // string
    if (ch === '"') {
      let j = i + 1;
      let escaped = false;
      while (j < text.length) {
        if (!escaped && text[j] === "\\") escaped = true;
        else if (!escaped && text[j] === '"') {
          j++;
          break;
        } else escaped = false;
        j++;
      }
      const str = text.slice(i, j);
      // 检测是不是 key（后面跟冒号）
      let k = j;
      while (k < text.length && (text[k] === " " || text[k] === "\t")) k++;
      const isKey = text[k] === ":";
      const cls = isKey ? "text-cyan2" : "text-lime2";
      tokens.push(`<span class="${cls}">${escape(str)}</span>`);
      i = j;
      continue;
    }
    // number
    if (ch === "-" || /\d/.test(ch)) {
      let j = i;
      while (j < text.length && /[\d.eE+-]/.test(text[j])) j++;
      tokens.push(`<span class="text-amber2">${escape(text.slice(i, j))}</span>`);
      i = j;
      continue;
    }
    // keyword
    if (/[a-z]/.test(ch)) {
      let j = i;
      while (j < text.length && /[a-z]/.test(text[j])) j++;
      const word = text.slice(i, j);
      if (word === "true" || word === "false" || word === "null") {
        tokens.push(`<span class="text-magenta">${escape(word)}</span>`);
      } else {
        tokens.push(escape(word));
      }
      i = j;
      continue;
    }
    // punctuation
    if ("{}[]:,".includes(ch)) {
      tokens.push(`<span class="text-white/50">${escape(ch)}</span>`);
      i++;
      continue;
    }
    tokens.push(escape(ch));
    i++;
  }
  return tokens.join("");
}

function escape(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

export function JsonEditor({
  value,
  onChange,
  errorLine,
  errorColumn,
  className,
}: JsonEditorProps) {
  const taRef = useRef<HTMLTextAreaElement>(null);
  const preRef = useRef<HTMLPreElement>(null);

  const lines = useMemo(() => value.split("\n"), [value]);
  const highlighted = useMemo(() => highlightJson(value), [value]);

  // sync scroll
  useEffect(() => {
    const ta = taRef.current;
    const pre = preRef.current;
    if (!ta || !pre) return;
    const onScroll = () => {
      pre.scrollTop = ta.scrollTop;
      pre.scrollLeft = ta.scrollLeft;
    };
    ta.addEventListener("scroll", onScroll);
    return () => ta.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <div
      className={cn(
        "relative rounded-xl border border-white/8 bg-ink-950/90 overflow-hidden",
        className
      )}
    >
      <div className="flex items-center justify-between px-3 py-2 border-b border-white/5 bg-white/[0.02]">
        <div className="flex items-center gap-2">
          <div className="flex gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-rose2/60" />
            <span className="w-2.5 h-2.5 rounded-full bg-amber2/60" />
            <span className="w-2.5 h-2.5 rounded-full bg-lime2/60" />
          </div>
          <span className="text-[11px] font-mono text-white/40 ml-2">
            data/cumulus/structures/*.json
          </span>
        </div>
        <span className="text-[10px] font-mono text-white/30">
          {lines.length} lines · {value.length} bytes
        </span>
      </div>

      <div className="relative flex font-mono text-[12px] leading-[1.6]">
        {/* line numbers */}
        <div className="select-none text-right pr-2 pl-3 py-3 text-white/25 bg-white/[0.01] border-r border-white/5">
          {lines.map((_, i) => (
            <div
              key={i}
              className={cn(
                "h-[1.6em]",
                errorLine === i + 1 && "text-rose2 font-bold bg-rose-500/10"
              )}
            >
              {i + 1}
            </div>
          ))}
        </div>

        {/* editor area */}
        <div className="relative flex-1 overflow-hidden">
          <pre
            ref={preRef}
            aria-hidden
            className="absolute inset-0 py-3 px-3 pointer-events-none whitespace-pre overflow-auto scrollbar-thin text-white/80"
            dangerouslySetInnerHTML={{ __html: highlighted + "\n" }}
          />
          <textarea
            ref={taRef}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            spellCheck={false}
            className="absolute inset-0 py-3 px-3 bg-transparent text-transparent caret-white/90 resize-none outline-none whitespace-pre overflow-auto scrollbar-thin"
            style={{ width: "100%", height: "100%" }}
          />
        </div>
      </div>

      {/* error indicator */}
      {errorLine !== undefined && (
        <div className="px-3 py-2 border-t border-rose-500/30 bg-rose-500/8 text-[11px] font-mono text-rose2">
          ✗ line {errorLine}
          {errorColumn ? `:${errorColumn}` : ""} · parse error
        </div>
      )}
    </div>
  );
}
