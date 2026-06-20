package dev.vfyjxf.cumulus.api.visualization;

import java.util.List;

public record VisualizationModel(
                String id,
                List<VisualizationElement> elements) {
    public VisualizationModel {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Visualization model id must not be blank");
        }
        elements = List.copyOf(elements);
    }
}
