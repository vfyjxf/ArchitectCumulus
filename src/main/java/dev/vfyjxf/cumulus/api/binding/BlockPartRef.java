package dev.vfyjxf.cumulus.api.binding;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 方块部件引用：以世界坐标表示。
 */
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
