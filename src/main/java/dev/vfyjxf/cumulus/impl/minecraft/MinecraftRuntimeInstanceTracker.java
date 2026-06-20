package dev.vfyjxf.cumulus.impl.minecraft;

import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.impl.runtime.RuntimeCheckEngine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;

import java.util.Map;

public final class MinecraftRuntimeInstanceTracker {
    private final RuntimeCheckEngine engine;

    public MinecraftRuntimeInstanceTracker(RuntimeCheckEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Runtime check engine must not be null");
        }
        this.engine = engine;
    }

    public void track(ResourceLocation definitionId, BlockPos anchor, LevelAccessor level, Map<String, String> context) {
        engine.track(definitionId, anchor, MinecraftTargetSnapshotSource.fromLevel(anchor, level, context));
    }

    public void track(ResourceLocation definitionId, BlockPos anchor, LevelTargetReader reader, Map<String, String> context) {
        engine.track(definitionId, anchor, new MinecraftTargetSnapshotSource(anchor, reader, context));
    }

    public void untrack(ResourceLocation definitionId, BlockPos anchor) {
        engine.untrack(definitionId, anchor);
    }

    public void markChanged(BlockPos changedBlock) {
        engine.markDirty(SectionKey.fromBlock(changedBlock));
    }
}
