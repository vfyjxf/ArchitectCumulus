package dev.vfyjxf.cumulus.impl.design3.minecraft;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.impl.design3.build.BuildAction;
import dev.vfyjxf.cumulus.impl.design3.build.BuildActionKind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftBuildTargetTest {
    @Test
    void appliesPointBlockActionAtAnchorOffsetAndIsIdempotent() {
        var writer = new FakeBlockBuildWriter()
                .known("minecraft:furnace");
        var target = new MinecraftBuildTarget(new BlockPos(10, 64, 10), writer);
        var action = blockAction("controller", "minecraft:furnace", RelativeBlockPos.of(1, 0, -1));

        assertFalse(target.contains(action));
        target.apply(action);
        assertTrue(target.contains(action));
        target.apply(action);

        assertEquals(List.of(new BlockPos(11, 64, 9)), writer.writes());
        assertEquals("minecraft:furnace", writer.blocks().get(new BlockPos(11, 64, 9)));
    }

    @Test
    void expandsRegionActionIntoDeterministicBlockWrites() {
        var writer = new FakeBlockBuildWriter()
                .known("minecraft:stone");
        var target = new MinecraftBuildTarget(BlockPos.ZERO, writer);
        var action = new BuildAction(
                "frame",
                BuildActionKind.REGION,
                ResourceLocation.parse("cumulus:region"),
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(1, 1, 0),
                Map.of("block", "minecraft:stone"));

        target.apply(action);

        assertEquals(List.of(
                        new BlockPos(0, 0, 0),
                        new BlockPos(0, 1, 0),
                        new BlockPos(1, 0, 0),
                        new BlockPos(1, 1, 0)),
                writer.writes());
        assertTrue(target.contains(action));
    }

    @Test
    void appliesFluidActionThroughFluidWriterAndIsIdempotent() {
        var blockWriter = new FakeBlockBuildWriter();
        var fluidWriter = new FakeFluidBuildWriter()
                .known("minecraft:water");
        var target = new MinecraftBuildTarget(BlockPos.ZERO, blockWriter, fluidWriter);
        var action = new BuildAction(
                "water",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:fluid"),
                RelativeBlockPos.of(0, 1, 0),
                RelativeBlockPos.of(0, 1, 0),
                Map.of("fluid", "minecraft:water"));

        assertFalse(target.contains(action));
        target.apply(action);
        assertTrue(target.contains(action));
        target.apply(action);

        assertEquals(List.of(new BlockPos(0, 1, 0)), fluidWriter.writes());
        assertEquals("minecraft:water", fluidWriter.fluids().get(new BlockPos(0, 1, 0)));
        assertTrue(blockWriter.writes().isEmpty());
    }

    @Test
    void appliesCapabilityBackingBlockActionThroughBlockWriterAndIsIdempotent() {
        var blockWriter = new FakeBlockBuildWriter()
                .known("minecraft:furnace");
        var target = new MinecraftBuildTarget(BlockPos.ZERO, blockWriter);
        var action = new BuildAction(
                "energy",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:capability"),
                RelativeBlockPos.of(0, 1, 0),
                RelativeBlockPos.of(0, 1, 0),
                Map.of(
                        "name", "energy",
                        "block", "minecraft:furnace",
                        "side", "north"));

        assertFalse(target.contains(action));
        target.apply(action);
        assertTrue(target.contains(action));
        target.apply(action);

        assertEquals(List.of(new BlockPos(0, 1, 0)), blockWriter.writes());
        assertEquals("minecraft:furnace", blockWriter.blocks().get(new BlockPos(0, 1, 0)));
    }

    @Test
    void capabilityActionWithoutBackingBlockDoesNotPlaceDefaultBlock() {
        var blockWriter = new FakeBlockBuildWriter()
                .known("minecraft:air");
        var target = new MinecraftBuildTarget(BlockPos.ZERO, blockWriter);
        var action = new BuildAction(
                "energy",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:capability"),
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(0, 0, 0),
                Map.of("name", "energy"));

        assertFalse(target.contains(action));
        target.apply(action);

        assertTrue(blockWriter.writes().isEmpty());
    }

    @Test
    void capabilityMutationActionIsUnsupportedWithoutCapabilityWriter() {
        var blockWriter = new FakeBlockBuildWriter()
                .known("minecraft:furnace");
        var target = new MinecraftBuildTarget(BlockPos.ZERO, blockWriter);
        var action = new BuildAction(
                "energy",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:capability"),
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(0, 0, 0),
                Map.of(
                        "name", "energy",
                        "operation", "insert",
                        "resource", "minecraft:coal",
                        "amount", "8"));

        assertFalse(target.supports(action));
        assertFalse(target.contains(action));
        target.apply(action);

        assertTrue(blockWriter.writes().isEmpty());
    }

    @Test
    void appliesCapabilityMutationActionThroughCapabilityWriterAndIsIdempotent() {
        var blockWriter = new FakeBlockBuildWriter();
        var capabilityWriter = new FakeCapabilityBuildWriter()
                .known("energy");
        var target = new MinecraftBuildTarget(
                BlockPos.ZERO,
                blockWriter,
                new FakeFluidBuildWriter(),
                new FakeBlockEntityBuildWriter(),
                new FakeEntityBuildWriter(),
                capabilityWriter);
        var action = new BuildAction(
                "energy",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:capability"),
                RelativeBlockPos.of(0, 1, 0),
                RelativeBlockPos.of(0, 1, 0),
                Map.of(
                        "name", "energy",
                        "operation", "insert",
                        "resource", "minecraft:coal",
                        "amount", "8"));

        assertTrue(target.supports(action));
        assertFalse(target.contains(action));
        target.apply(action);
        assertTrue(target.contains(action));
        target.apply(action);

        assertEquals(List.of(new BlockPos(0, 1, 0)), capabilityWriter.writes());
        assertEquals(Map.of(
                        "operation", "insert",
                        "resource", "minecraft:coal",
                        "amount", "8"),
                capabilityWriter.mutations().get(new BlockPos(0, 1, 0) + "|energy"));
        assertTrue(blockWriter.writes().isEmpty());
    }

    @Test
    void appliesCapabilityBackingBlockAndMutationInOneAction() {
        var blockWriter = new FakeBlockBuildWriter()
                .known("minecraft:furnace");
        var capabilityWriter = new FakeCapabilityBuildWriter()
                .known("energy");
        var target = new MinecraftBuildTarget(
                BlockPos.ZERO,
                blockWriter,
                new FakeFluidBuildWriter(),
                new FakeBlockEntityBuildWriter(),
                new FakeEntityBuildWriter(),
                capabilityWriter);
        var action = new BuildAction(
                "energy",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:capability"),
                RelativeBlockPos.of(0, 1, 0),
                RelativeBlockPos.of(0, 1, 0),
                Map.of(
                        "name", "energy",
                        "block", "minecraft:furnace",
                        "operation", "insert",
                        "resource", "minecraft:coal",
                        "amount", "8"));

        assertFalse(target.contains(action));
        target.apply(action);
        assertTrue(target.contains(action));
        target.apply(action);

        assertEquals(List.of(new BlockPos(0, 1, 0)), blockWriter.writes());
        assertEquals(List.of(new BlockPos(0, 1, 0)), capabilityWriter.writes());
    }

    @Test
    void appliesBlockEntityActionWithNbtAndIsIdempotent() {
        var blockWriter = new FakeBlockBuildWriter();
        var fluidWriter = new FakeFluidBuildWriter();
        var blockEntityWriter = new FakeBlockEntityBuildWriter()
                .known("minecraft:furnace", "minecraft:furnace");
        var target = new MinecraftBuildTarget(BlockPos.ZERO, blockWriter, fluidWriter, blockEntityWriter);
        var action = new BuildAction(
                "controller",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:block_entity"),
                RelativeBlockPos.of(2, 0, 0),
                RelativeBlockPos.of(2, 0, 0),
                Map.of(
                        "block", "minecraft:furnace",
                        "block_entity", "minecraft:furnace",
                        "nbt", "{Items:[]}"));

        assertFalse(target.contains(action));
        target.apply(action);
        assertTrue(target.contains(action));
        target.apply(action);

        assertEquals(List.of(new BlockPos(2, 0, 0)), blockEntityWriter.writes());
        assertEquals("{Items:[]}", blockEntityWriter.nbt().get(new BlockPos(2, 0, 0)));
        assertTrue(blockWriter.writes().isEmpty());
        assertTrue(fluidWriter.writes().isEmpty());
    }

    @Test
    void appliesEntityActionWithNbtAndIsIdempotent() {
        var blockWriter = new FakeBlockBuildWriter();
        var fluidWriter = new FakeFluidBuildWriter();
        var blockEntityWriter = new FakeBlockEntityBuildWriter();
        var entityWriter = new FakeEntityBuildWriter()
                .known("minecraft:villager");
        var target = new MinecraftBuildTarget(
                new BlockPos(10, 64, 10),
                blockWriter,
                fluidWriter,
                blockEntityWriter,
                entityWriter);
        var action = new BuildAction(
                "worker",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:entity"),
                RelativeBlockPos.of(0, 1, -1),
                RelativeBlockPos.of(0, 1, -1),
                Map.of(
                        "entity", "minecraft:villager",
                        "nbt", "{NoAI:1b}"));

        assertFalse(target.contains(action));
        target.apply(action);
        assertTrue(target.contains(action));
        target.apply(action);

        assertEquals(List.of(new BlockPos(10, 65, 9)), entityWriter.writes());
        assertEquals("{NoAI:1b}", entityWriter.nbt().get(new BlockPos(10, 65, 9)));
        assertTrue(blockWriter.writes().isEmpty());
        assertTrue(fluidWriter.writes().isEmpty());
        assertTrue(blockEntityWriter.writes().isEmpty());
    }

    @Test
    void entityNbtPatchComparisonIgnoresDefaultSavedFields() {
        assertTrue(EntityBuildWriter.containsAuthoredNbt(
                "{Air:300s,NoAI:1b,Pos:[0.5d,65.0d,9.5d],Rotation:[0.0f,0.0f],UUID:[I;1,2,3,4]}",
                "{NoAI:1b}"));
        assertFalse(EntityBuildWriter.containsAuthoredNbt(
                "{Air:300s,NoAI:0b}",
                "{NoAI:1b}"));
    }

    @Test
    void entityActionWithoutEntityIdDoesNotUseDefaultSpawnType() {
        var entityWriter = new FakeEntityBuildWriter()
                .known("minecraft:item");
        var target = new MinecraftBuildTarget(
                BlockPos.ZERO,
                new FakeBlockBuildWriter(),
                new FakeFluidBuildWriter(),
                new FakeBlockEntityBuildWriter(),
                entityWriter);
        var action = new BuildAction(
                "worker",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:entity"),
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(0, 0, 0));

        assertFalse(target.contains(action));
        target.apply(action);

        assertTrue(entityWriter.writes().isEmpty());
    }

    @Test
    void refusesUnsupportedTargetsAndUnknownBlockIds() {
        var writer = new FakeBlockBuildWriter()
                .known("minecraft:stone");
        var target = new MinecraftBuildTarget(BlockPos.ZERO, writer);
        var unsupported = new BuildAction(
                "fluid",
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:fluid"),
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(0, 0, 0));
        var unknown = blockAction("missing", "cumulus:missing_block", RelativeBlockPos.of(0, 0, 0));

        assertFalse(target.contains(unsupported));
        assertFalse(target.contains(unknown));
        target.apply(unsupported);
        target.apply(unknown);

        assertTrue(writer.writes().isEmpty());
    }

    private static BuildAction blockAction(String id, String blockId, RelativeBlockPos pos) {
        return new BuildAction(
                id,
                BuildActionKind.POINT,
                ResourceLocation.parse("cumulus:block"),
                pos,
                pos,
                Map.of("block", blockId));
    }

    private static final class FakeBlockBuildWriter implements BlockBuildWriter {
        private final java.util.Set<ResourceLocation> knownBlocks = new java.util.HashSet<>();
        private final Map<BlockPos, ResourceLocation> blocks = new LinkedHashMap<>();
        private final List<BlockPos> writes = new java.util.ArrayList<>();

        FakeBlockBuildWriter known(String id) {
            knownBlocks.add(ResourceLocation.parse(id));
            return this;
        }

        List<BlockPos> writes() {
            return List.copyOf(writes);
        }

        Map<BlockPos, String> blocks() {
            var values = new LinkedHashMap<BlockPos, String>();
            blocks.forEach((pos, id) -> values.put(pos, id.toString()));
            return values;
        }

        @Override
        public boolean canWrite(ResourceLocation blockId) {
            return knownBlocks.contains(blockId);
        }

        @Override
        public boolean hasBlock(BlockPos pos, ResourceLocation blockId) {
            return blockId.equals(blocks.get(pos));
        }

        @Override
        public boolean setBlock(BlockPos pos, ResourceLocation blockId) {
            if (!canWrite(blockId) || hasBlock(pos, blockId)) {
                return false;
            }
            blocks.put(pos, blockId);
            writes.add(pos);
            return true;
        }
    }

    private static final class FakeFluidBuildWriter implements FluidBuildWriter {
        private final java.util.Set<ResourceLocation> knownFluids = new java.util.HashSet<>();
        private final Map<BlockPos, ResourceLocation> fluids = new LinkedHashMap<>();
        private final List<BlockPos> writes = new java.util.ArrayList<>();

        FakeFluidBuildWriter known(String id) {
            knownFluids.add(ResourceLocation.parse(id));
            return this;
        }

        List<BlockPos> writes() {
            return List.copyOf(writes);
        }

        Map<BlockPos, String> fluids() {
            var values = new LinkedHashMap<BlockPos, String>();
            fluids.forEach((pos, id) -> values.put(pos, id.toString()));
            return values;
        }

        @Override
        public boolean canWrite(ResourceLocation fluidId) {
            return knownFluids.contains(fluidId);
        }

        @Override
        public boolean hasFluid(BlockPos pos, ResourceLocation fluidId) {
            return fluidId.equals(fluids.get(pos));
        }

        @Override
        public boolean setFluid(BlockPos pos, ResourceLocation fluidId) {
            if (!canWrite(fluidId) || hasFluid(pos, fluidId)) {
                return false;
            }
            fluids.put(pos, fluidId);
            writes.add(pos);
            return true;
        }
    }

    private static final class FakeBlockEntityBuildWriter implements BlockEntityBuildWriter {
        private final java.util.Set<String> known = new java.util.HashSet<>();
        private final Map<BlockPos, ResourceLocation> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, ResourceLocation> blockEntities = new LinkedHashMap<>();
        private final Map<BlockPos, String> nbt = new LinkedHashMap<>();
        private final List<BlockPos> writes = new java.util.ArrayList<>();

        FakeBlockEntityBuildWriter known(String blockId, String blockEntityId) {
            known.add(blockId + "|" + blockEntityId);
            return this;
        }

        List<BlockPos> writes() {
            return List.copyOf(writes);
        }

        Map<BlockPos, String> nbt() {
            return Map.copyOf(nbt);
        }

        @Override
        public boolean canWrite(ResourceLocation blockId, ResourceLocation blockEntityId) {
            return known.contains(blockId + "|" + blockEntityId);
        }

        @Override
        public boolean hasBlockEntity(
                BlockPos pos,
                ResourceLocation blockId,
                ResourceLocation blockEntityId,
                String nbt) {
            return blockId.equals(blocks.get(pos))
                    && blockEntityId.equals(blockEntities.get(pos))
                    && nbt.equals(this.nbt.get(pos));
        }

        @Override
        public boolean setBlockEntity(
                BlockPos pos,
                ResourceLocation blockId,
                ResourceLocation blockEntityId,
                String nbt) {
            if (!canWrite(blockId, blockEntityId) || hasBlockEntity(pos, blockId, blockEntityId, nbt)) {
                return false;
            }
            blocks.put(pos, blockId);
            blockEntities.put(pos, blockEntityId);
            this.nbt.put(pos, nbt);
            writes.add(pos);
            return true;
        }
    }

    private static final class FakeEntityBuildWriter implements EntityBuildWriter {
        private final java.util.Set<ResourceLocation> knownEntities = new java.util.HashSet<>();
        private final Map<BlockPos, ResourceLocation> entities = new LinkedHashMap<>();
        private final Map<BlockPos, String> nbt = new LinkedHashMap<>();
        private final List<BlockPos> writes = new java.util.ArrayList<>();

        FakeEntityBuildWriter known(String id) {
            knownEntities.add(ResourceLocation.parse(id));
            return this;
        }

        List<BlockPos> writes() {
            return List.copyOf(writes);
        }

        Map<BlockPos, String> nbt() {
            return Map.copyOf(nbt);
        }

        @Override
        public boolean canWrite(ResourceLocation entityId) {
            return knownEntities.contains(entityId);
        }

        @Override
        public boolean hasEntity(BlockPos pos, ResourceLocation entityId, String nbt) {
            return entityId.equals(entities.get(pos)) && nbt.equals(this.nbt.get(pos));
        }

        @Override
        public boolean spawnEntity(BlockPos pos, ResourceLocation entityId, String nbt) {
            if (!canWrite(entityId) || hasEntity(pos, entityId, nbt)) {
                return false;
            }
            entities.put(pos, entityId);
            this.nbt.put(pos, nbt);
            writes.add(pos);
            return true;
        }
    }

    private static final class FakeCapabilityBuildWriter implements CapabilityBuildWriter {
        private final java.util.Set<String> knownCapabilities = new java.util.HashSet<>();
        private final Map<String, Map<String, String>> mutations = new LinkedHashMap<>();
        private final List<BlockPos> writes = new java.util.ArrayList<>();

        FakeCapabilityBuildWriter known(String name) {
            knownCapabilities.add(name);
            return this;
        }

        List<BlockPos> writes() {
            return List.copyOf(writes);
        }

        Map<String, Map<String, String>> mutations() {
            var values = new LinkedHashMap<String, Map<String, String>>();
            mutations.forEach((key, mutation) -> values.put(key, Map.copyOf(mutation)));
            return values;
        }

        @Override
        public boolean canWrite(String capabilityName, String operation) {
            return knownCapabilities.contains(capabilityName) && !operation.isBlank();
        }

        @Override
        public boolean hasMutation(BlockPos pos, String capabilityName, Map<String, String> mutation) {
            return mutation.equals(mutations.get(key(pos, capabilityName)));
        }

        @Override
        public boolean mutate(BlockPos pos, String capabilityName, Map<String, String> mutation) {
            if (!canWrite(capabilityName, mutation.getOrDefault("operation", ""))
                    || hasMutation(pos, capabilityName, mutation)) {
                return false;
            }
            mutations.put(key(pos, capabilityName), Map.copyOf(mutation));
            writes.add(pos);
            return true;
        }

        private static String key(BlockPos pos, String capabilityName) {
            return pos + "|" + capabilityName;
        }
    }
}
