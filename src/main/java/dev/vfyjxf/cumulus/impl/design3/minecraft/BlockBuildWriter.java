package dev.vfyjxf.cumulus.impl.design3.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public interface BlockBuildWriter {
    boolean canWrite(ResourceLocation blockId);

    boolean hasBlock(BlockPos pos, ResourceLocation blockId);

    boolean setBlock(BlockPos pos, ResourceLocation blockId);
}
