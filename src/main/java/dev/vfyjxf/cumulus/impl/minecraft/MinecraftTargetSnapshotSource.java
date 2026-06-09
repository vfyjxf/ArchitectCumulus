package dev.vfyjxf.cumulus.impl.minecraft;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;
import dev.vfyjxf.cumulus.impl.check.TargetSnapshotSource;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

public final class MinecraftTargetSnapshotSource implements TargetSnapshotSource {
    private static final ResourceLocation BLOCK = ResourceLocation.parse("cumulus:block");
    private static final ResourceLocation BLOCK_ENTITY = ResourceLocation.parse("cumulus:block_entity");
    private static final ResourceLocation ENTITY = ResourceLocation.parse("cumulus:entity");
    private static final ResourceLocation FLUID = ResourceLocation.parse("cumulus:fluid");
    private static final ResourceLocation CAPABILITY = ResourceLocation.parse("cumulus:capability");
    private static final ResourceLocation REGION = ResourceLocation.parse("cumulus:region");

    private final BlockPos anchor;
    private final LevelTargetReader reader;
    private final Map<String, String> context;

    public MinecraftTargetSnapshotSource(BlockPos anchor, LevelTargetReader reader, Map<String, String> context) {
        if (anchor == null) {
            throw new IllegalArgumentException("Anchor must not be null");
        }
        if (reader == null) {
            throw new IllegalArgumentException("Level target reader must not be null");
        }
        this.anchor = anchor;
        this.reader = reader;
        this.context = Map.copyOf(context);
    }

    public static MinecraftTargetSnapshotSource fromLevel(
            BlockPos anchor,
            LevelAccessor level,
            Map<String, String> context) {
        if (level == null) {
            throw new IllegalArgumentException("Level must not be null");
        }
        return new MinecraftTargetSnapshotSource(anchor, new LevelAccessorTargetReader(level), context);
    }

    @Override
    public Optional<SnapshotTarget> target(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max) {
        return target(targetType, min, max, Map.of());
    }

    @Override
    public Optional<SnapshotTarget> target(
            ResourceLocation targetType,
            RelativeBlockPos min,
            RelativeBlockPos max,
            Map<String, String> targetProperties) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        if (min == null || max == null) {
            throw new IllegalArgumentException("Target bounds must not be null");
        }
        if (targetProperties == null) {
            throw new IllegalArgumentException("Target properties must not be null");
        }
        var absoluteMin = absolute(min);
        var absoluteMax = absolute(max);
        if (!boundsLoaded(absoluteMin, absoluteMax)) {
            return Optional.empty();
        }
        if (BLOCK.equals(targetType)) {
            return Optional.ofNullable(reader.block(absoluteMin));
        }
        if (BLOCK_ENTITY.equals(targetType)) {
            return Optional.ofNullable(reader.blockEntity(absoluteMin));
        }
        if (ENTITY.equals(targetType)) {
            return reader.entities(absoluteMin, absoluteMax).stream()
                    .min(Comparator.comparing(SnapshotTarget::value))
                    .map(target -> new SnapshotTarget(target.value(), target.properties()));
        }
        if (FLUID.equals(targetType)) {
            return Optional.ofNullable(reader.fluid(absoluteMin));
        }
        if (CAPABILITY.equals(targetType)) {
            return Optional.ofNullable(reader.capability(
                    absoluteMin,
                    targetProperties.getOrDefault("name", ""),
                    targetProperties));
        }
        if (REGION.equals(targetType)) {
            return region(absoluteMin, absoluteMax);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> context(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Context key must not be blank");
        }
        return Optional.ofNullable(context.get(key));
    }

    private Optional<SnapshotTarget> region(BlockPos min, BlockPos max) {
        var counts = new TreeMap<String, Integer>();
        var volume = 0;
        for (var x = min.getX(); x <= max.getX(); x++) {
            for (var y = min.getY(); y <= max.getY(); y++) {
                for (var z = min.getZ(); z <= max.getZ(); z++) {
                    volume++;
                    var block = reader.block(new BlockPos(x, y, z));
                    if (block == null) {
                        return Optional.empty();
                    }
                    counts.merge(block.value(), 1, Integer::sum);
                }
            }
        }
        var dominant = counts.entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .orElseThrow()
                .getKey();
        var properties = new LinkedHashMap<String, String>();
        properties.put("volume", Integer.toString(volume));
        for (var entry : counts.entrySet()) {
            properties.put(entry.getKey(), Integer.toString(entry.getValue()));
        }
        return Optional.of(new SnapshotTarget(dominant, properties));
    }

    private boolean boundsLoaded(BlockPos min, BlockPos max) {
        return reader.isLoaded(new BlockPos(min.getX(), min.getY(), min.getZ()))
                && reader.isLoaded(new BlockPos(min.getX(), min.getY(), max.getZ()))
                && reader.isLoaded(new BlockPos(min.getX(), max.getY(), min.getZ()))
                && reader.isLoaded(new BlockPos(min.getX(), max.getY(), max.getZ()))
                && reader.isLoaded(new BlockPos(max.getX(), min.getY(), min.getZ()))
                && reader.isLoaded(new BlockPos(max.getX(), min.getY(), max.getZ()))
                && reader.isLoaded(new BlockPos(max.getX(), max.getY(), min.getZ()))
                && reader.isLoaded(new BlockPos(max.getX(), max.getY(), max.getZ()));
    }

    private BlockPos absolute(RelativeBlockPos relative) {
        return anchor.offset(relative.x(), relative.y(), relative.z());
    }

    static final class LevelAccessorTargetReader implements LevelTargetReader {
        private final LevelAccessor level;
        private final MinecraftCapabilityResolvers capabilityResolvers;

        LevelAccessorTargetReader(LevelAccessor level) {
            this(level, MinecraftCapabilityResolvers.defaults());
        }

        LevelAccessorTargetReader(LevelAccessor level, MinecraftCapabilityResolvers capabilityResolvers) {
            this.level = level;
            this.capabilityResolvers = capabilityResolvers;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return level.hasChunkAt(pos);
        }

        @Override
        public SnapshotTarget block(BlockPos pos) {
            var state = level.getBlockState(pos);
            var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            return new SnapshotTarget(id.toString(), Map.of());
        }

        @Override
        public SnapshotTarget blockEntity(BlockPos pos) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                return null;
            }
            var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            var properties = new LinkedHashMap<String, String>();
            properties.put("block", blockId(blockEntity));
            return new SnapshotTarget(id.toString(), properties);
        }

        @Override
        public SnapshotTarget fluid(BlockPos pos) {
            var state = level.getFluidState(pos);
            if (state.isEmpty()) {
                return null;
            }
            var id = BuiltInRegistries.FLUID.getKey(state.getType());
            return new SnapshotTarget(id.toString(), Map.of(
                    "amount", Integer.toString(state.getAmount()),
                    "source", Boolean.toString(state.isSource())));
        }

        @Override
        public java.util.List<SnapshotTarget> entities(BlockPos min, BlockPos max) {
            var box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
            return level.getEntities((Entity) null, box, entity -> true).stream()
                    .map(entity -> {
                        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                        return new SnapshotTarget(id.toString(), Map.of(
                                "uuid", entity.getStringUUID()));
                    })
                    .sorted(Comparator.comparing(SnapshotTarget::value)
                            .thenComparing(target -> target.properties().getOrDefault("uuid", "")))
                    .toList();
        }

        @Override
        public SnapshotTarget capability(BlockPos pos, String capabilityName, Map<String, String> targetProperties) {
            if (!(level instanceof Level concreteLevel)) {
                return null;
            }
            return capabilityResolvers.resolve(
                    new MinecraftCapabilityResolvers.CapabilityLookup() {
                        @Override
                        public <T, C extends @Nullable Object> T get(
                                BlockCapability<T, C> capability,
                                BlockPos capabilityPos,
                                C context) {
                            return concreteLevel.getCapability(capability, capabilityPos, context);
                        }
                    },
                    pos,
                    capabilityName,
                    targetProperties);
        }

        private static String blockId(BlockEntity blockEntity) {
            var id = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
            return id.toString();
        }
    }
}
