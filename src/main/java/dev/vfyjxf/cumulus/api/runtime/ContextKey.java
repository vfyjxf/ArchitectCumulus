package dev.vfyjxf.cumulus.api.runtime;

import net.minecraft.resources.ResourceLocation;

public interface ContextKey<T> {
    ResourceLocation id();

    Class<T> valueType();
}
