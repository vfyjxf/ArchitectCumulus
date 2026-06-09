package dev.vfyjxf.cumulus.api.diagnostic;

public record DiagnosticEdge(
        String fromNodeId,
        String toNodeId,
        String relation) {
    public DiagnosticEdge {
        if (fromNodeId == null || fromNodeId.isBlank()) {
            throw new IllegalArgumentException("Diagnostic edge source must not be blank");
        }
        if (toNodeId == null || toNodeId.isBlank()) {
            throw new IllegalArgumentException("Diagnostic edge target must not be blank");
        }
        if (relation == null || relation.isBlank()) {
            throw new IllegalArgumentException("Diagnostic edge relation must not be blank");
        }
    }
}
