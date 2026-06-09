package dev.vfyjxf.cumulus.impl.design3.minecraft;

import dev.vfyjxf.cumulus.impl.design3.check.SnapshotTarget;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;

public interface LevelTargetReader {
    boolean isLoaded(BlockPos pos);

    SnapshotTarget block(BlockPos pos);

    SnapshotTarget blockEntity(BlockPos pos);

    SnapshotTarget fluid(BlockPos pos);

    List<SnapshotTarget> entities(BlockPos min, BlockPos max);

    default SnapshotTarget capability(BlockPos pos, String capabilityName) {
        return null;
    }

    default SnapshotTarget capability(BlockPos pos, String capabilityName, Map<String, String> targetProperties) {
        return capability(pos, capabilityName);
    }
}
