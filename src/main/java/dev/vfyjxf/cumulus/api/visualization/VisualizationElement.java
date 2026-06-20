package dev.vfyjxf.cumulus.api.visualization;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;

public record VisualizationElement(
                String id,
                String type,
                VisualizationRegion region,
                Severity severity,
                String group,
                String labelKey,
                int priority) {
    public VisualizationElement {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Visualization element id must not be blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Visualization element type must not be blank");
        }
        if (region == null) {
            throw new IllegalArgumentException("Visualization region must not be null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Visualization severity must not be null");
        }
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("Visualization group must not be blank");
        }
        if (labelKey == null || labelKey.isBlank()) {
            throw new IllegalArgumentException("Visualization label key must not be blank");
        }
    }

    public static VisualizationElement region(String id, String type, RelativeBlockPos min, RelativeBlockPos max, Severity severity, String group, String labelKey, int priority) {
        return new VisualizationElement(id, type, new VisualizationRegion(min, max), severity, group, labelKey, priority);
    }
}
