package dev.vfyjxf.cumulus.api.target;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record TargetDescriptor(ResourceLocation typeId, Map<String, String> properties) {
    public TargetDescriptor {
        if (typeId == null) {
            throw new IllegalArgumentException("Target type id must not be null");
        }
        properties = Map.copyOf(properties);
    }
}
