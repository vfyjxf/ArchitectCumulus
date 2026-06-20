package dev.vfyjxf.cumulus.api.component;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record ComponentDescriptor(
                String name,
                ResourceLocation typeId,
                Map<String, Object> properties) {
    public ComponentDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Component descriptor name must not be blank");
        }
        if (typeId == null) {
            throw new IllegalArgumentException("Component descriptor type id must not be null");
        }
        properties = Map.copyOf(properties);
    }
}
