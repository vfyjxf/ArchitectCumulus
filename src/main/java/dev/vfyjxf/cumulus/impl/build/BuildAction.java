package dev.vfyjxf.cumulus.impl.build;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record BuildAction(
        String id,
        BuildActionKind kind,
        ResourceLocation targetType,
        RelativeBlockPos min,
        RelativeBlockPos max,
        Map<String, String> properties) {
    public BuildAction {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Build action id must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Build action kind must not be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Build action target type must not be null");
        }
        if (min == null || max == null) {
            throw new IllegalArgumentException("Build action bounds must not be null");
        }
        properties = Map.copyOf(properties);
    }

    public BuildAction(
            String id,
            BuildActionKind kind,
            ResourceLocation targetType,
            RelativeBlockPos min,
            RelativeBlockPos max) {
        this(id, kind, targetType, min, max, Map.of());
    }
}
