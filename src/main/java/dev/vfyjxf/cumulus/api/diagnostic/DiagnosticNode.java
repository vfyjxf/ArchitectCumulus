package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.List;

public record DiagnosticNode(
                String id,
                Severity severity,
                String message,
                String localizationKey,
                List<DiagnosticEvidence> evidence,
                String sourcePath,
                List<FixHint> fixHints) {
    public DiagnosticNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Diagnostic node id must not be blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Diagnostic severity must not be null");
        }
        if (message == null) {
            throw new IllegalArgumentException("Diagnostic message must not be null");
        }
        if (localizationKey == null || localizationKey.isBlank()) {
            throw new IllegalArgumentException("Diagnostic localization key must not be blank");
        }
        evidence = List.copyOf(evidence);
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("Diagnostic source path must not be blank");
        }
        fixHints = List.copyOf(fixHints);
    }
}
