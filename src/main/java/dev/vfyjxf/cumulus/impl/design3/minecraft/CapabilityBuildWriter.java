package dev.vfyjxf.cumulus.impl.design3.minecraft;

import java.util.Map;
import net.minecraft.core.BlockPos;

public interface CapabilityBuildWriter {
    boolean canWrite(String capabilityName, String operation);

    boolean hasMutation(BlockPos pos, String capabilityName, Map<String, String> mutation);

    boolean mutate(BlockPos pos, String capabilityName, Map<String, String> mutation);
}
