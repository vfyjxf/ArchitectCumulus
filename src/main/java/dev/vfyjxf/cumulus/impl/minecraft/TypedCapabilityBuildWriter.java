package dev.vfyjxf.cumulus.impl.minecraft;

import java.util.Map;
import net.minecraft.core.BlockPos;

public interface TypedCapabilityBuildWriter {
    boolean canWrite(String operation);

    boolean hasMutation(BlockPos pos, Map<String, String> mutation);

    boolean mutate(BlockPos pos, Map<String, String> mutation);
}
