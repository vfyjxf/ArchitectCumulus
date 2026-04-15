package dev.vfyjxf.cumulus.api.partition;

import net.minecraft.resources.ResourceLocation;

public interface BlockEntityView {
    ResourceLocation typeId();

    Object raw();

    default boolean isInTag(ResourceLocation tagId) {
        return false;
    }
}
