package dev.vfyjxf.cumulus.impl.minecraft;

import net.minecraft.core.BlockPos;

import java.util.Map;

public interface TypedCapabilityBuildWriter {
    boolean canWrite(String operation);

    boolean hasMutation(BlockPos pos, Map<String, String> mutation);

    boolean mutate(BlockPos pos, Map<String, String> mutation);
}
