package dev.vfyjxf.cumulus.api.binding;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * 实体部件引用：以 UUID 表示。
 */
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
