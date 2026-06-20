package dev.vfyjxf.cumulus.impl.minecraft;

import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

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
