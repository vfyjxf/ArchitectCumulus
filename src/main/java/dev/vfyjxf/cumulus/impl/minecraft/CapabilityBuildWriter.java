package dev.vfyjxf.cumulus.impl.minecraft;

import net.minecraft.core.BlockPos;

import java.util.Map;

public interface CapabilityBuildWriter {
    boolean canWrite(String capabilityName, String operation);

    boolean hasMutation(BlockPos pos, String capabilityName, Map<String, String> mutation);

    boolean mutate(BlockPos pos, String capabilityName, Map<String, String> mutation);
}
