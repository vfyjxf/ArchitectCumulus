package dev.vfyjxf.cumulus.api.diagnostic;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import java.util.Map;

public record DiagnosticEvidence(
        RelativeBlockPos relativePosition,
        String targetId,
        String predicateId,
        String componentPath,
        String detail,
        Map<String, String> descriptors) {
    public DiagnosticEvidence {
        if (relativePosition == null) {
            throw new IllegalArgumentException("Diagnostic relative position must not be null");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Diagnostic target id must not be blank");
        }
        if (predicateId == null || predicateId.isBlank()) {
            throw new IllegalArgumentException("Diagnostic predicate id must not be blank");
        }
        if (componentPath == null || componentPath.isBlank()) {
            throw new IllegalArgumentException("Diagnostic component path must not be blank");
        }
        if (detail == null) {
            throw new IllegalArgumentException("Diagnostic detail must not be null");
        }
        descriptors = Map.copyOf(descriptors);
    }

    public static DiagnosticEvidence relative(
            RelativeBlockPos relativePosition,
            String targetId,
            String predicateId,
            String componentPath,
            String detail,
            Map<String, String> descriptors) {
        return new DiagnosticEvidence(relativePosition, targetId, predicateId, componentPath, detail, descriptors);
    }
}
