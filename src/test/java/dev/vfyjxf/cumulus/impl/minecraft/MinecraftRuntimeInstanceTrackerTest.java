package dev.vfyjxf.cumulus.impl.minecraft;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;
import dev.vfyjxf.cumulus.impl.runtime.RuntimeCheckEngine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftRuntimeInstanceTrackerTest {
    @Test
    void tracksMinecraftBackedInstancesAndMarksChangedSectionsDirty() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:minecraft_runtime_machine")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("minecraft:furnace")).capture("controller")).build();
        var engine = new RuntimeCheckEngine().register(definition);
        var reader = new FakeLevelTargetReader().block(new BlockPos(5, 70, 5), "minecraft:furnace", Map.of());
        var tracker = new MinecraftRuntimeInstanceTracker(engine);

        tracker.track(definition.id(), new BlockPos(5, 70, 5), reader, Map.of());
        tracker.markChanged(new BlockPos(5, 70, 5));

        var frame = engine.tick(1, 4);
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
        assertEquals("5,70,5", frame.deltas().getFirst().context().get(ContextKey.string("controller")).orElse(null));
    }

    @Test
    void untracksMinecraftBackedInstancesAndStopsFutureDirtyChecks() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:minecraft_runtime_removed")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("minecraft:furnace"))).build();
        var engine = new RuntimeCheckEngine().register(definition);
        var reader = new FakeLevelTargetReader().block(new BlockPos(0, 64, 0), "minecraft:furnace", Map.of());
        var tracker = new MinecraftRuntimeInstanceTracker(engine);

        tracker.track(definition.id(), new BlockPos(0, 64, 0), reader, Map.of());
        tracker.untrack(definition.id(), new BlockPos(0, 64, 0));
        tracker.markChanged(new BlockPos(0, 64, 0));

        var frame = engine.tick(1, 4);
        assertEquals(List.of(), frame.deltas());
    }

    private static final class FakeLevelTargetReader implements LevelTargetReader {
        private final Map<BlockPos, SnapshotTarget> blocks = new LinkedHashMap<>();

        FakeLevelTargetReader block(BlockPos pos, String value, Map<String, String> properties) {
            blocks.put(pos, new SnapshotTarget(value, properties));
            return this;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return blocks.containsKey(pos);
        }

        @Override
        public SnapshotTarget block(BlockPos pos) {
            return blocks.get(pos);
        }

        @Override
        public SnapshotTarget blockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public SnapshotTarget fluid(BlockPos pos) {
            return null;
        }

        @Override
        public List<SnapshotTarget> entities(BlockPos min, BlockPos max) {
            return List.of();
        }
    }
}
