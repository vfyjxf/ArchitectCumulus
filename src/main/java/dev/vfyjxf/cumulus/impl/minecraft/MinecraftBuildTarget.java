package dev.vfyjxf.cumulus.impl.minecraft;

import dev.vfyjxf.cumulus.impl.build.BuildAction;
import dev.vfyjxf.cumulus.impl.build.BuildTarget;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class MinecraftBuildTarget implements BuildTarget {
    private static final ResourceLocation block = ResourceLocation.parse("cumulus:block");
    private static final ResourceLocation blockEntity = ResourceLocation.parse("cumulus:block_entity");
    private static final ResourceLocation capability = ResourceLocation.parse("cumulus:capability");
    private static final ResourceLocation entity = ResourceLocation.parse("cumulus:entity");
    private static final ResourceLocation fluid = ResourceLocation.parse("cumulus:fluid");
    private static final ResourceLocation region = ResourceLocation.parse("cumulus:region");
    private static final ResourceLocation defaultBlock = ResourceLocation.parse("minecraft:air");
    private static final ResourceLocation defaultFluid = ResourceLocation.parse("minecraft:water");

    private final BlockPos anchor;
    private final BlockBuildWriter writer;
    private final FluidBuildWriter fluidWriter;
    private final BlockEntityBuildWriter blockEntityWriter;
    private final EntityBuildWriter entityWriter;
    private final CapabilityBuildWriter capabilityWriter;

    public MinecraftBuildTarget(BlockPos anchor, BlockBuildWriter writer) {
        this(anchor, writer, NoopFluidBuildWriter.instance);
    }

    public MinecraftBuildTarget(BlockPos anchor, BlockBuildWriter writer, FluidBuildWriter fluidWriter) {
        this(anchor, writer, fluidWriter, NoopBlockEntityBuildWriter.instance);
    }

    public MinecraftBuildTarget(
                    BlockPos anchor,
                    BlockBuildWriter writer,
                    FluidBuildWriter fluidWriter,
                    BlockEntityBuildWriter blockEntityWriter) {
        this(anchor, writer, fluidWriter, blockEntityWriter, NoopEntityBuildWriter.instance);
    }

    public MinecraftBuildTarget(
                    BlockPos anchor,
                    BlockBuildWriter writer,
                    FluidBuildWriter fluidWriter,
                    BlockEntityBuildWriter blockEntityWriter,
                    EntityBuildWriter entityWriter) {
        this(anchor, writer, fluidWriter, blockEntityWriter, entityWriter, NoopCapabilityBuildWriter.instance);
    }

    public MinecraftBuildTarget(
                    BlockPos anchor,
                    BlockBuildWriter writer,
                    FluidBuildWriter fluidWriter,
                    BlockEntityBuildWriter blockEntityWriter,
                    EntityBuildWriter entityWriter,
                    CapabilityBuildWriter capabilityWriter) {
        if (anchor == null) {
            throw new IllegalArgumentException("Build anchor must not be null");
        }
        if (writer == null) {
            throw new IllegalArgumentException("Block build writer must not be null");
        }
        if (fluidWriter == null) {
            throw new IllegalArgumentException("Fluid build writer must not be null");
        }
        if (blockEntityWriter == null) {
            throw new IllegalArgumentException("Block entity build writer must not be null");
        }
        if (entityWriter == null) {
            throw new IllegalArgumentException("Entity build writer must not be null");
        }
        if (capabilityWriter == null) {
            throw new IllegalArgumentException("Capability build writer must not be null");
        }
        this.anchor = anchor;
        this.writer = writer;
        this.fluidWriter = fluidWriter;
        this.blockEntityWriter = blockEntityWriter;
        this.entityWriter = entityWriter;
        this.capabilityWriter = capabilityWriter;
    }

    public static MinecraftBuildTarget fromLevel(BlockPos anchor, LevelAccessor level) {
        if (level == null) {
            throw new IllegalArgumentException("Level must not be null");
        }
        var writer = new LevelBlockBuildWriter(level);
        return new MinecraftBuildTarget(
                        anchor,
                        writer,
                        new LevelFluidBuildWriter(level),
                        new LevelBlockEntityBuildWriter(level),
                        new LevelEntityBuildWriter(level));
    }

    @Override
    public boolean supports(BuildAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Build action must not be null");
        }
        if (capability.equals(action.targetType()) && !action.properties().getOrDefault("operation", "").isBlank() && action.properties().getOrDefault("block", "").isBlank()) {
            return capabilityWriter.canWrite(capabilityName(action), capabilityOperation(action));
        }
        return true;
    }

    @Override
    public boolean contains(BuildAction action) {
        if (fluid.equals(action.targetType())) {
            return containsFluid(action);
        }
        if (blockEntity.equals(action.targetType())) {
            return containsBlockEntity(action);
        }
        if (entity.equals(action.targetType())) {
            return containsEntity(action);
        }
        if (capability.equals(action.targetType())) {
            return containsCapability(action);
        }
        var blockId = blockId(action);
        if (blockId == null || !writer.canWrite(blockId)) {
            return false;
        }
        return containsBlockRange(action, blockId);
    }

    @Override
    public void apply(BuildAction action) {
        if (!supports(action)) {
            return;
        }
        if (fluid.equals(action.targetType())) {
            applyFluid(action);
            return;
        }
        if (blockEntity.equals(action.targetType())) {
            applyBlockEntity(action);
            return;
        }
        if (entity.equals(action.targetType())) {
            applyEntity(action);
            return;
        }
        if (capability.equals(action.targetType())) {
            applyCapability(action);
            return;
        }
        var blockId = blockId(action);
        if (blockId == null || !writer.canWrite(blockId)) {
            return;
        }
        applyBlockRange(action, blockId);
    }

    private boolean containsBlockRange(BuildAction action, ResourceLocation blockId) {
        for (var x = action.min().x(); x <= action.max().x(); x++) {
            for (var y = action.min().y(); y <= action.max().y(); y++) {
                for (var z = action.min().z(); z <= action.max().z(); z++) {
                    if (!writer.hasBlock(anchor.offset(x, y, z), blockId)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void applyBlockRange(BuildAction action, ResourceLocation blockId) {
        for (var x = action.min().x(); x <= action.max().x(); x++) {
            for (var y = action.min().y(); y <= action.max().y(); y++) {
                for (var z = action.min().z(); z <= action.max().z(); z++) {
                    var pos = anchor.offset(x, y, z);
                    if (!writer.hasBlock(pos, blockId)) {
                        writer.setBlock(pos, blockId);
                    }
                }
            }
        }
    }

    private static ResourceLocation blockId(BuildAction action) {
        if (!block.equals(action.targetType()) && !region.equals(action.targetType()) && !capability.equals(action.targetType())) {
            return null;
        }
        if (capability.equals(action.targetType()) && action.properties().getOrDefault("block", "").isBlank()) {
            return null;
        }
        return ResourceLocation.parse(action.properties().getOrDefault("block", defaultBlock.toString()));
    }

    private boolean containsCapabilityMutation(BuildAction action) {
        var capabilityName = capabilityName(action);
        var operation = capabilityOperation(action);
        if (!capabilityWriter.canWrite(capabilityName, operation)) {
            return false;
        }
        return capabilityWriter.hasMutation(absolute(action.min()), capabilityName, capabilityMutation(action));
    }

    private boolean containsCapability(BuildAction action) {
        var blockId = blockId(action);
        var hasBlockRequirement = blockId != null;
        var hasMutationRequirement = hasCapabilityMutation(action);
        if (!hasBlockRequirement && !hasMutationRequirement) {
            return false;
        }
        if (hasBlockRequirement && !writer.canWrite(blockId)) {
            return false;
        }
        if (hasBlockRequirement && !containsBlockRange(action, blockId)) {
            return false;
        }
        return !hasMutationRequirement || containsCapabilityMutation(action);
    }

    private void applyCapability(BuildAction action) {
        var blockId = blockId(action);
        if (blockId != null && writer.canWrite(blockId)) {
            applyBlockRange(action, blockId);
        }
        if (hasCapabilityMutation(action)) {
            applyCapabilityMutation(action);
        }
    }

    private void applyCapabilityMutation(BuildAction action) {
        var capabilityName = capabilityName(action);
        var operation = capabilityOperation(action);
        if (!capabilityWriter.canWrite(capabilityName, operation)) {
            return;
        }
        var pos = absolute(action.min());
        var mutation = capabilityMutation(action);
        if (!capabilityWriter.hasMutation(pos, capabilityName, mutation)) {
            capabilityWriter.mutate(pos, capabilityName, mutation);
        }
    }

    private static boolean hasCapabilityMutation(BuildAction action) {
        return capability.equals(action.targetType()) && !action.properties().getOrDefault("operation", "").isBlank();
    }

    private static String capabilityName(BuildAction action) {
        return action.properties().getOrDefault("name", action.id());
    }

    private static String capabilityOperation(BuildAction action) {
        return action.properties().getOrDefault("operation", "");
    }

    private static Map<String, String> capabilityMutation(BuildAction action) {
        var mutation = new java.util.LinkedHashMap<String, String>();
        for (var entry : action.properties().entrySet()) {
            if (!entry.getKey().equals("name") && !entry.getKey().equals("block")) {
                mutation.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(mutation);
    }

    private boolean containsFluid(BuildAction action) {
        var fluidId = fluidId(action);
        if (!fluidWriter.canWrite(fluidId)) {
            return false;
        }
        for (var x = action.min().x(); x <= action.max().x(); x++) {
            for (var y = action.min().y(); y <= action.max().y(); y++) {
                for (var z = action.min().z(); z <= action.max().z(); z++) {
                    if (!fluidWriter.hasFluid(anchor.offset(x, y, z), fluidId)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void applyFluid(BuildAction action) {
        var fluidId = fluidId(action);
        if (!fluidWriter.canWrite(fluidId)) {
            return;
        }
        for (var x = action.min().x(); x <= action.max().x(); x++) {
            for (var y = action.min().y(); y <= action.max().y(); y++) {
                for (var z = action.min().z(); z <= action.max().z(); z++) {
                    var pos = anchor.offset(x, y, z);
                    if (!fluidWriter.hasFluid(pos, fluidId)) {
                        fluidWriter.setFluid(pos, fluidId);
                    }
                }
            }
        }
    }

    private static ResourceLocation fluidId(BuildAction action) {
        return ResourceLocation.parse(action.properties().getOrDefault("fluid", defaultFluid.toString()));
    }

    private boolean containsBlockEntity(BuildAction action) {
        var blockId = blockEntityBlockId(action);
        var blockEntityId = blockEntityId(action);
        var nbt = blockEntityNbt(action);
        if (!blockEntityWriter.canWrite(blockId, blockEntityId)) {
            return false;
        }
        return blockEntityWriter.hasBlockEntity(absolute(action.min()), blockId, blockEntityId, nbt);
    }

    private void applyBlockEntity(BuildAction action) {
        var blockId = blockEntityBlockId(action);
        var blockEntityId = blockEntityId(action);
        var nbt = blockEntityNbt(action);
        if (!blockEntityWriter.canWrite(blockId, blockEntityId)) {
            return;
        }
        var pos = absolute(action.min());
        if (!blockEntityWriter.hasBlockEntity(pos, blockId, blockEntityId, nbt)) {
            blockEntityWriter.setBlockEntity(pos, blockId, blockEntityId, nbt);
        }
    }

    private BlockPos absolute(dev.vfyjxf.cumulus.api.definition.RelativeBlockPos pos) {
        return anchor.offset(pos.x(), pos.y(), pos.z());
    }

    private static ResourceLocation blockEntityBlockId(BuildAction action) {
        return ResourceLocation.parse(action.properties().getOrDefault("block", defaultBlock.toString()));
    }

    private static ResourceLocation blockEntityId(BuildAction action) {
        return ResourceLocation.parse(action.properties().getOrDefault("block_entity", action.properties().getOrDefault("type", "minecraft:furnace")));
    }

    private static String blockEntityNbt(BuildAction action) {
        return action.properties().getOrDefault("nbt", "{}");
    }

    private boolean containsEntity(BuildAction action) {
        var entityId = entityId(action);
        var nbt = entityNbt(action);
        if (entityId == null || !entityWriter.canWrite(entityId)) {
            return false;
        }
        return entityWriter.hasEntity(absolute(action.min()), entityId, nbt);
    }

    private void applyEntity(BuildAction action) {
        var entityId = entityId(action);
        var nbt = entityNbt(action);
        if (entityId == null || !entityWriter.canWrite(entityId)) {
            return;
        }
        var pos = absolute(action.min());
        if (!entityWriter.hasEntity(pos, entityId, nbt)) {
            entityWriter.spawnEntity(pos, entityId, nbt);
        }
    }

    private static ResourceLocation entityId(BuildAction action) {
        var value = action.properties().getOrDefault("entity", action.properties().get("type"));
        return value == null || value.isBlank() ? null : ResourceLocation.parse(value);
    }

    private static String entityNbt(BuildAction action) {
        return action.properties().getOrDefault("nbt", "{}");
    }

    private static final class LevelBlockBuildWriter implements BlockBuildWriter {
        private static final int updateClients = Block.UPDATE_CLIENTS;
        private final LevelAccessor level;

        private LevelBlockBuildWriter(LevelAccessor level) {
            this.level = level;
        }

        @Override
        public boolean canWrite(ResourceLocation blockId) {
            return BuiltInRegistries.BLOCK.containsKey(blockId);
        }

        @Override
        public boolean hasBlock(BlockPos pos, ResourceLocation blockId) {
            var block = BuiltInRegistries.BLOCK.get(blockId);
            return level.getBlockState(pos).is(block);
        }

        @Override
        public boolean setBlock(BlockPos pos, ResourceLocation blockId) {
            var block = BuiltInRegistries.BLOCK.get(blockId);
            if (block == null) {
                return false;
            }
            return level.setBlock(pos, block.defaultBlockState(), updateClients);
        }
    }

    private static final class LevelFluidBuildWriter implements FluidBuildWriter {
        private static final int updateClients = Block.UPDATE_CLIENTS;
        private final LevelAccessor level;

        private LevelFluidBuildWriter(LevelAccessor level) {
            this.level = level;
        }

        @Override
        public boolean canWrite(ResourceLocation fluidId) {
            return BuiltInRegistries.FLUID.containsKey(fluidId);
        }

        @Override
        public boolean hasFluid(BlockPos pos, ResourceLocation fluidId) {
            var fluid = BuiltInRegistries.FLUID.get(fluidId);
            return level.getFluidState(pos).is(fluid);
        }

        @Override
        public boolean setFluid(BlockPos pos, ResourceLocation fluidId) {
            var fluid = BuiltInRegistries.FLUID.get(fluidId);
            if (fluid == null) {
                return false;
            }
            return level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), updateClients);
        }
    }

    private enum NoopFluidBuildWriter implements FluidBuildWriter {
        instance;

        @Override
        public boolean canWrite(ResourceLocation fluidId) {
            return false;
        }

        @Override
        public boolean hasFluid(BlockPos pos, ResourceLocation fluidId) {
            return false;
        }

        @Override
        public boolean setFluid(BlockPos pos, ResourceLocation fluidId) {
            return false;
        }
    }

    private static final class LevelBlockEntityBuildWriter implements BlockEntityBuildWriter {
        private static final int updateClients = Block.UPDATE_CLIENTS;
        private final LevelAccessor level;

        private LevelBlockEntityBuildWriter(LevelAccessor level) {
            this.level = level;
        }

        @Override
        public boolean canWrite(ResourceLocation blockId, ResourceLocation blockEntityId) {
            return BuiltInRegistries.BLOCK.containsKey(blockId) && BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(blockEntityId);
        }

        @Override
        public boolean hasBlockEntity(BlockPos pos, ResourceLocation blockId, ResourceLocation blockEntityId, String nbt) {
            var block = BuiltInRegistries.BLOCK.get(blockId);
            var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(blockEntityId);
            var blockEntity = level.getBlockEntity(pos);
            return level.getBlockState(pos).is(block) && blockEntity != null && blockEntity.getType().equals(type) && normalizedNbt(blockEntity).equals(parseNbt(nbt));
        }

        @Override
        public boolean setBlockEntity(BlockPos pos, ResourceLocation blockId, ResourceLocation blockEntityId, String nbt) {
            var block = BuiltInRegistries.BLOCK.get(blockId);
            if (block == null || !canWrite(blockId, blockEntityId)) {
                return false;
            }
            level.setBlock(pos, block.defaultBlockState(), updateClients);
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                return false;
            }
            blockEntity.loadWithComponents(mergedNbt(blockEntity, parseNbt(nbt)), HolderLookup.Provider.create(java.util.stream.Stream.of()));
            blockEntity.setChanged();
            return true;
        }

        private static CompoundTag normalizedNbt(BlockEntity blockEntity) {
            var values = blockEntity.saveWithoutMetadata(HolderLookup.Provider.create(java.util.stream.Stream.of()));
            values.remove("x");
            values.remove("y");
            values.remove("z");
            return values;
        }

        private static CompoundTag mergedNbt(BlockEntity blockEntity, CompoundTag patch) {
            var values = blockEntity.saveWithoutMetadata(HolderLookup.Provider.create(java.util.stream.Stream.of()));
            values.merge(patch);
            return values;
        }

        private static CompoundTag parseNbt(String value) {
            try {
                return TagParser.parseTag(value);
            } catch (Exception ex) {
                return new CompoundTag();
            }
        }
    }

    private enum NoopBlockEntityBuildWriter implements BlockEntityBuildWriter {
        instance;

        @Override
        public boolean canWrite(ResourceLocation blockId, ResourceLocation blockEntityId) {
            return false;
        }

        @Override
        public boolean hasBlockEntity(BlockPos pos, ResourceLocation blockId, ResourceLocation blockEntityId, String nbt) {
            return false;
        }

        @Override
        public boolean setBlockEntity(BlockPos pos, ResourceLocation blockId, ResourceLocation blockEntityId, String nbt) {
            return false;
        }
    }

    private static final class LevelEntityBuildWriter implements EntityBuildWriter {
        private final LevelAccessor level;

        private LevelEntityBuildWriter(LevelAccessor level) {
            this.level = level;
        }

        @Override
        public boolean canWrite(ResourceLocation entityId) {
            return level instanceof ServerLevel && BuiltInRegistries.ENTITY_TYPE.containsKey(entityId);
        }

        @Override
        public boolean hasEntity(BlockPos pos, ResourceLocation entityId, String nbt) {
            var type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            var expected = parseNbt(nbt);
            return level.getEntities((Entity) null, entityBox(pos), entity -> entity.getType().equals(type)).stream().anyMatch(entity -> EntityBuildWriter.containsAuthoredNbt(normalizedNbt(entity), expected));
        }

        @Override
        public boolean spawnEntity(BlockPos pos, ResourceLocation entityId, String nbt) {
            if (!(level instanceof ServerLevel serverLevel) || !canWrite(entityId)) {
                return false;
            }
            var values = mergedNbt(pos, entityId, parseNbt(nbt));
            var entity = EntityType.create(values, serverLevel).orElse(null);
            if (entity == null) {
                return false;
            }
            entity.moveTo(Vec3.atBottomCenterOf(pos));
            return serverLevel.addFreshEntity(entity);
        }

        private static AABB entityBox(BlockPos pos) {
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        }

        private static CompoundTag normalizedNbt(Entity entity) {
            var values = entity.saveWithoutId(new CompoundTag());
            values.remove("Pos");
            values.remove("Motion");
            values.remove("Rotation");
            values.remove("UUID");
            return values;
        }

        private static CompoundTag mergedNbt(BlockPos pos, ResourceLocation entityId, CompoundTag patch) {
            var values = new CompoundTag();
            values.putString("id", entityId.toString());
            values.put("Pos", newDoubleList(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
            values.merge(patch);
            return values;
        }

        private static net.minecraft.nbt.ListTag newDoubleList(double... values) {
            var list = new net.minecraft.nbt.ListTag();
            for (var value : values) {
                list.add(net.minecraft.nbt.DoubleTag.valueOf(value));
            }
            return list;
        }

        private static CompoundTag parseNbt(String value) {
            try {
                return TagParser.parseTag(value);
            } catch (Exception ex) {
                return new CompoundTag();
            }
        }
    }

    private enum NoopEntityBuildWriter implements EntityBuildWriter {
        instance;

        @Override
        public boolean canWrite(ResourceLocation entityId) {
            return false;
        }

        @Override
        public boolean hasEntity(BlockPos pos, ResourceLocation entityId, String nbt) {
            return false;
        }

        @Override
        public boolean spawnEntity(BlockPos pos, ResourceLocation entityId, String nbt) {
            return false;
        }
    }

    private enum NoopCapabilityBuildWriter implements CapabilityBuildWriter {
        instance;

        @Override
        public boolean canWrite(String capabilityName, String operation) {
            return false;
        }

        @Override
        public boolean hasMutation(BlockPos pos, String capabilityName, Map<String, String> mutation) {
            return false;
        }

        @Override
        public boolean mutate(BlockPos pos, String capabilityName, Map<String, String> mutation) {
            return false;
        }
    }
}
