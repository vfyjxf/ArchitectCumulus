package dev.vfyjxf.cumulus.api.visualization;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.target.Targets;

public record VisualizationRegion(RelativeBlockPos min, RelativeBlockPos max) {
    public VisualizationRegion {
        Targets.validateRegionBounds(min, max);
    }
}
