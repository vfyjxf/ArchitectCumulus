package dev.vfyjxf.cumulus.api.partition;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public interface EntityView {
    UUID uuid();

    ResourceLocation typeId();

    long packedBlockPos();

    Object raw();

    default boolean isInTag(ResourceLocation tagId) {
        return false;
    }
}
