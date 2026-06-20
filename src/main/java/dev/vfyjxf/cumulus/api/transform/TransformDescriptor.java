package dev.vfyjxf.cumulus.api.transform;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record TransformDescriptor(ResourceLocation id, Map<String, String> properties) {
    public TransformDescriptor {
        if (id == null) {
            throw new IllegalArgumentException("Transform id must not be null");
        }
        properties = Map.copyOf(properties);
    }
}
