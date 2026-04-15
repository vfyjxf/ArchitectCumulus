package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.List;
import java.util.Objects;

/**
 * 诊断 trace（可选的因果/规则树）。
 *
 * <p>用于表达复杂规则（组合谓语）为何失败：上层 message 描述规则节点，
 * children 进一步细分子规则或更具体的证据。
 */
public record DiagnosticTrace(String message, List<DiagnosticTrace> children) {
    public DiagnosticTrace {
        Objects.requireNonNull(message, "message");
        children = List.copyOf(Objects.requireNonNull(children, "children"));
    }

    public static DiagnosticTrace leaf(String message) {
        return new DiagnosticTrace(message, List.of());
    }
}
