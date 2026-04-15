package dev.vfyjxf.cumulus.api.partition;

import net.minecraft.resources.ResourceLocation;

public interface BlockStateView {
    ResourceLocation id();

    Object raw();

    default boolean isInTag(ResourceLocation tagId) {
        return false;
    }
}
