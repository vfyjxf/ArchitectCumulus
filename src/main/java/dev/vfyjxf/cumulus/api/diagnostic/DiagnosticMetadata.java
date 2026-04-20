package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.Map;

public record DiagnosticMetadata(Map<String, String> properties) {
    public DiagnosticMetadata {
        properties = Map.copyOf(properties);
    }

    public static DiagnosticMetadata empty() {
        return new DiagnosticMetadata(Map.of());
    }
}
