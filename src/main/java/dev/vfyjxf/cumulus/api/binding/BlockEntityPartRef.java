package dev.vfyjxf.cumulus.api.binding;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record BlockEntityPartRef(BlockPos pos) implements PartRef {
    public static final ResourceLocation KIND = ResourceLocation.fromNamespaceAndPath("cumulus", "block_entity");

    public BlockEntityPartRef {
        Objects.requireNonNull(pos, "pos");
    }

    @Override
    public ResourceLocation kind() {
        return KIND;
    }
}
