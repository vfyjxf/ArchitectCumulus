package dev.vfyjxf.cumulus.impl.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public interface FluidBuildWriter {
    boolean canWrite(ResourceLocation fluidId);

    boolean hasFluid(BlockPos pos, ResourceLocation fluidId);

    boolean setFluid(BlockPos pos, ResourceLocation fluidId);
}
