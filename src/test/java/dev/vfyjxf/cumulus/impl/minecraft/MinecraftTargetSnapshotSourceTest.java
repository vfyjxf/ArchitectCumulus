package dev.vfyjxf.cumulus.impl.minecraft;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;
import dev.vfyjxf.cumulus.impl.check.StructureCheckExecutor;
import dev.vfyjxf.cumulus.impl.check.TargetSnapshotReader;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftTargetSnapshotSourceTest {
    private static final ResourceLocation block = ResourceLocation.parse("cumulus:block");
    private static final ResourceLocation blockEntity = ResourceLocation.parse("cumulus:block_entity");
    private static final ResourceLocation entity = ResourceLocation.parse("cumulus:entity");
    private static final ResourceLocation fluid = ResourceLocation.parse("cumulus:fluid");
    private static final ResourceLocation capability = ResourceLocation.parse("cumulus:capability");
    private static final ResourceLocation region = ResourceLocation.parse("cumulus:region");

    @Test
    void readsLoadedBlockBlockEntityFluidEntityCapabilityAndContextTargetsAtAnchorOffset() {
        var reader = new FakeLevelTargetReader().block(new BlockPos(10, 64, 9), "minecraft:furnace", Map.of("facing", "north")).blockEntity(new BlockPos(10, 64, 9), "minecraft:furnace", Map.of("inventory", "3")).fluid(new BlockPos(11, 64, 10), "minecraft:water", Map.of("source", "true")).entity(new BlockPos(10, 65,
                        10), "minecraft:villager", Map.of("profession", "armorer")).capabilityTarget(new BlockPos(11, 65, 10), "energy", Map.of("side", "north"));
        var source = new MinecraftTargetSnapshotSource(
                        new BlockPos(10, 64, 10),
                        reader,
                        Map.of("facing", "north"));

        assertEquals("minecraft:furnace", source.target(block, RelativeBlockPos.of(0, 0, -1), RelativeBlockPos.of(0, 0, -1)).orElseThrow().value());
        assertEquals("3", source.target(blockEntity, RelativeBlockPos.of(0, 0, -1), RelativeBlockPos.of(0, 0, -1)).orElseThrow().properties().get("inventory"));
        assertEquals("minecraft:water", source.target(fluid, RelativeBlockPos.of(1, 0, 0), RelativeBlockPos.of(1, 0, 0)).orElseThrow().value());
        assertEquals("armorer", source.target(entity, RelativeBlockPos.of(0, 1, 0), RelativeBlockPos.of(0, 1, 0)).orElseThrow().properties().get("profession"));
        assertEquals("north", source.target(capability, RelativeBlockPos.of(1, 1, 0), RelativeBlockPos.of(1, 1, 0), Map.of("name", "energy")).orElseThrow().properties().get("side"));
        assertEquals("north", source.context("facing").orElseThrow());
    }

    @Test
    void leavesTargetsMissingWhenAnyCoveredChunkIsUnavailable() {
        var reader = new FakeLevelTargetReader().unloaded(new BlockPos(48, 64, 0)).block(new BlockPos(48, 64, 0), "minecraft:stone", Map.of());
        var source = new MinecraftTargetSnapshotSource(BlockPos.ZERO, reader, Map.of());

        assertTrue(source.target(block, RelativeBlockPos.of(48, 64, 0), RelativeBlockPos.of(48, 64, 0)).isEmpty());
    }

    @Test
    void foldsRegionBlocksIntoDeterministicAggregateTarget() {
        var reader = new FakeLevelTargetReader().block(new BlockPos(0, 0, 0), "minecraft:stone", Map.of()).block(new BlockPos(1, 0, 0), "minecraft:stone", Map.of()).block(new BlockPos(0, 1, 0), "minecraft:glass", Map.of()).block(new BlockPos(1, 1, 0), "minecraft:stone", Map.of());
        var source = new MinecraftTargetSnapshotSource(BlockPos.ZERO, reader, Map.of());

        var target = source.target(region, RelativeBlockPos.of(0, 0, 0), RelativeBlockPos.of(1, 1, 0)).orElseThrow();

        assertEquals("minecraft:stone", target.value());
        assertEquals("4", target.properties().get("volume"));
        assertEquals("3", target.properties().get("minecraft:stone"));
        assertEquals("1", target.properties().get("minecraft:glass"));
    }

    @Test
    void feedsRealWorldSourceThroughSnapshotReaderAndCheckExecutor() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:level_adapter_machine")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("minecraft:furnace")).where(PredicateDescriptors.property("facing", "north"))).point("tank", RelativeBlockPos.of(1, 0, 0), Targets.fluid().where(PredicateDescriptors.id("minecraft:water"))).point("energy", RelativeBlockPos.of(0, 1, 0), Targets.capability("energy").where(PredicateDescriptors.property("side", "north"))).build();
        var reader = new FakeLevelTargetReader().block(new BlockPos(5, 70, 5), "minecraft:furnace", Map.of("facing", "north")).fluid(new BlockPos(6, 70, 5), "minecraft:water", Map.of("source", "true")).capabilityTarget(new BlockPos(5, 71, 5), "energy", Map.of("side", "north"));
        var source = new MinecraftTargetSnapshotSource(new BlockPos(5, 70, 5), reader, Map.of());

        var snapshot = TargetSnapshotReader.fromDefinition(definition, source);
        var result = StructureCheckExecutor.check(definition, new BlockPos(5, 70, 5), snapshot);

        assertEquals(InstanceStatus.valid, result.status());
    }

    @Test
    void passesCapabilityTargetPropertiesToLevelReader() {
        var reader = new FakeLevelTargetReader().capabilityTarget(new BlockPos(0, 0, 0), "energy", Map.of("side", "east"));
        var source = new MinecraftTargetSnapshotSource(BlockPos.ZERO, reader, Map.of());

        var target = source.target(capability, RelativeBlockPos.of(0, 0, 0), RelativeBlockPos.of(0, 0, 0), Map.of("name", "energy", "side", "east")).orElseThrow();

        assertEquals("east", target.properties().get("side"));
        assertEquals("east", reader.lastCapabilityProperties.get("side"));
    }

    private static final class FakeLevelTargetReader implements LevelTargetReader {
        private final Map<BlockPos, SnapshotTarget> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, SnapshotTarget> blockEntities = new LinkedHashMap<>();
        private final Map<BlockPos, SnapshotTarget> fluids = new LinkedHashMap<>();
        private final Map<BlockPos, SnapshotTarget> entities = new LinkedHashMap<>();
        private final Map<BlockPos, SnapshotTarget> capabilities = new LinkedHashMap<>();
        private Map<String, String> lastCapabilityProperties = Map.of();
        private final Set<BlockPos> unloaded = new HashSet<>();

        FakeLevelTargetReader block(BlockPos pos, String value, Map<String, String> properties) {
            blocks.put(pos, new SnapshotTarget(value, properties));
            return this;
        }

        FakeLevelTargetReader blockEntity(BlockPos pos, String value, Map<String, String> properties) {
            blockEntities.put(pos, new SnapshotTarget(value, properties));
            return this;
        }

        FakeLevelTargetReader fluid(BlockPos pos, String value, Map<String, String> properties) {
            fluids.put(pos, new SnapshotTarget(value, properties));
            return this;
        }

        FakeLevelTargetReader entity(BlockPos pos, String value, Map<String, String> properties) {
            entities.put(pos, new SnapshotTarget(value, properties));
            return this;
        }

        FakeLevelTargetReader capabilityTarget(BlockPos pos, String value, Map<String, String> properties) {
            capabilities.put(pos, new SnapshotTarget(value, properties));
            return this;
        }

        FakeLevelTargetReader unloaded(BlockPos pos) {
            unloaded.add(pos);
            return this;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return !unloaded.contains(pos);
        }

        @Override
        public SnapshotTarget block(BlockPos pos) {
            return blocks.get(pos);
        }

        @Override
        public SnapshotTarget blockEntity(BlockPos pos) {
            return blockEntities.get(pos);
        }

        @Override
        public SnapshotTarget fluid(BlockPos pos) {
            return fluids.get(pos);
        }

        @Override
        public List<SnapshotTarget> entities(BlockPos min, BlockPos max) {
            return entities.entrySet().stream().filter(entry -> contains(min, max, entry.getKey())).map(Map.Entry::getValue).toList();
        }

        @Override
        public SnapshotTarget capability(BlockPos pos, String capabilityName) {
            return capability(pos, capabilityName, Map.of());
        }

        @Override
        public SnapshotTarget capability(BlockPos pos, String capabilityName, Map<String, String> targetProperties) {
            lastCapabilityProperties = Map.copyOf(targetProperties);
            var target = capabilities.get(pos);
            return target != null && capabilityName.equals(target.value()) ? target : null;
        }

        private static boolean contains(BlockPos min, BlockPos max, BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY() && pos.getY() <= max.getY() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
    }
}
