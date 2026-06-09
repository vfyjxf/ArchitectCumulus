package dev.vfyjxf.cumulus.impl.build;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import net.minecraft.resources.ResourceLocation;

public record BuiltTarget(
        ResourceLocation targetType,
        RelativeBlockPos min,
        RelativeBlockPos max,
        String actionId) {
    public BuiltTarget {
        if (targetType == null) {
            throw new IllegalArgumentException("Built target type must not be null");
        }
        if (min == null || max == null) {
            throw new IllegalArgumentException("Built target bounds must not be null");
        }
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("Built target action id must not be blank");
        }
    }
}
