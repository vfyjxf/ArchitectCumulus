package dev.vfyjxf.cumulus.api.binding;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record BlockPartRef(BlockPos pos) implements PartRef {
    public static final ResourceLocation KIND = ResourceLocation.fromNamespaceAndPath("cumulus", "block");

    public BlockPartRef {
        Objects.requireNonNull(pos, "pos");
    }

    @Override
    public ResourceLocation kind() {
        return KIND;
    }
}
