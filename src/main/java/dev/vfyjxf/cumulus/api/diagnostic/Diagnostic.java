package dev.vfyjxf.cumulus.api.diagnostic;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 结构化诊断条目。
 *
 * <p>一个 diagnostic 应尽量稳定：
 * <ul>
 *     <li>{@link #code()}：稳定错误码（namespace:path），便于国际化、统计与规则匹配</li>
 *     <li>{@link #severity()}：严重级别</li>
 *     <li>{@link #summary()}：一行可读摘要</li>
 *     <li>{@link #evidence()}：定位证据（坐标/section/role/实体）</li>
 *     <li>{@link #trace()}：可选的因果树（解释复杂规则）</li>
 *     <li>{@link #fixHints()}：可选的修复提示（纯数据）</li>
 * </ul>
 */
public record Diagnostic(
        ResourceLocation code,
        Severity severity,
        String summary,
        DiagnosticEvidence evidence,
        Optional<DiagnosticTrace> trace,
        List<FixHint> fixHints
) {
    public Diagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(evidence, "evidence");
        trace = Objects.requireNonNull(trace, "trace");
        fixHints = List.copyOf(Objects.requireNonNull(fixHints, "fixHints"));
    }

    public static Diagnostic of(ResourceLocation code, Severity severity, String summary, DiagnosticEvidence evidence) {
        return new Diagnostic(code, severity, summary, evidence, Optional.empty(), List.of());
    }
}
