package dev.vfyjxf.cumulus.api.binding;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record CustomPartRef(ResourceLocation kind, Object handle) implements PartRef {
    public CustomPartRef {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(handle, "handle");
    }
}
