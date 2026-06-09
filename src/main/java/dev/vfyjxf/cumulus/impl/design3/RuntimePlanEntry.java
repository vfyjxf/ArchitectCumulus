package dev.vfyjxf.cumulus.impl.design3;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record RuntimePlanEntry(
        String id,
        ResourceLocation targetType,
        RelativeBlockPos min,
        RelativeBlockPos max,
        Map<String, String> properties) {
    public RuntimePlanEntry {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Runtime plan entry id must not be blank");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Runtime plan target type must not be null");
        }
        if (min == null) {
            throw new IllegalArgumentException("Runtime plan min must not be null");
        }
        if (max == null) {
            throw new IllegalArgumentException("Runtime plan max must not be null");
        }
        properties = Map.copyOf(properties);
    }

    public RuntimePlanEntry(
            String id,
            ResourceLocation targetType,
            RelativeBlockPos min,
            RelativeBlockPos max) {
        this(id, targetType, min, max, Map.of());
    }
}
