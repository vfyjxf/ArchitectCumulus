package dev.vfyjxf.cumulus.api.binding;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public record EntityPartRef(UUID entityId) implements PartRef {
    public static final ResourceLocation KIND = ResourceLocation.fromNamespaceAndPath("cumulus", "entity");

    public EntityPartRef {
        Objects.requireNonNull(entityId, "entityId");
    }

    @Override
    public ResourceLocation kind() {
        return KIND;
    }
}
