package dev.vfyjxf.cumulus.impl.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public interface BlockEntityBuildWriter {
    boolean canWrite(ResourceLocation blockId, ResourceLocation blockEntityId);

    boolean hasBlockEntity(BlockPos pos, ResourceLocation blockId, ResourceLocation blockEntityId, String nbt);

    boolean setBlockEntity(BlockPos pos, ResourceLocation blockId, ResourceLocation blockEntityId, String nbt);
}
