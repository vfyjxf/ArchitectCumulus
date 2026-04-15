package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.List;
import java.util.Objects;

public record DiagnosticTrace(String message, List<DiagnosticTrace> children) {
    public DiagnosticTrace {
        Objects.requireNonNull(message, "message");
        children = List.copyOf(Objects.requireNonNull(children, "children"));
    }

    public static DiagnosticTrace leaf(String message) {
        return new DiagnosticTrace(message, List.of());
    }
}
