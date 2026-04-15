package dev.vfyjxf.cumulus.api.diagnostic;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
