package dev.vfyjxf.cumulus.api.transform;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record TransformDescriptor(ResourceLocation id, Map<String, String> properties) {
    public TransformDescriptor {
        if (id == null) {
            throw new IllegalArgumentException("Transform id must not be null");
        }
        properties = Map.copyOf(properties);
    }
}
