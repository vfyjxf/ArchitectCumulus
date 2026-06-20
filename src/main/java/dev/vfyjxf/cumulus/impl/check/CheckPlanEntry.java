package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;

public record CheckPlanEntry(
                StructurePointDefinition point,
                RelativeBlockPos min,
                RelativeBlockPos max) {
    public CheckPlanEntry {
        if (point == null) {
            throw new IllegalArgumentException("Check plan point must not be null");
        }
        if (min == null) {
            throw new IllegalArgumentException("Check plan min must not be null");
        }
        if (max == null) {
            throw new IllegalArgumentException("Check plan max must not be null");
        }
    }
}
