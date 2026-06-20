package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record InMemoryTargetSnapshot(
                Map<TargetKey, SnapshotTarget> targets,
                Map<String, String> contextValues) {

    private static final ResourceLocation block = ResourceLocation.parse("cumulus:block");
    private static final ResourceLocation blockEntity = ResourceLocation.parse("cumulus:block_entity");
    private static final ResourceLocation entity = ResourceLocation.parse("cumulus:entity");
    private static final ResourceLocation fluid = ResourceLocation.parse("cumulus:fluid");
    private static final ResourceLocation capability = ResourceLocation.parse("cumulus:capability");
    private static final ResourceLocation region = ResourceLocation.parse("cumulus:region");

    public InMemoryTargetSnapshot {
        targets = Map.copyOf(targets);
        contextValues = Map.copyOf(contextValues);
    }

    public static InMemoryTargetSnapshot empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<SnapshotTarget> target(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        if (min == null || max == null) {
            throw new IllegalArgumentException("Target bounds must not be null");
        }
        return Optional.ofNullable(targets.get(new TargetKey(targetType, min, max)));
    }

    public Optional<SnapshotTarget> context(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Context key must not be blank");
        }
        return Optional.ofNullable(contextValues.get(key)).map(value -> new SnapshotTarget(value, Map.of("key", key)));
    }

    public static final class Builder {
        private final Map<TargetKey, SnapshotTarget> targets = new LinkedHashMap<>();
        private final Map<String, String> contextValues = new LinkedHashMap<>();

        public Builder block(RelativeBlockPos pos, String value) {
            return block(pos, value, Map.of());
        }

        public Builder block(RelativeBlockPos pos, String value, Map<String, String> properties) {
            put(block, pos, pos, value, properties);
            return this;
        }

        public Builder blockEntity(RelativeBlockPos pos, String value) {
            return blockEntity(pos, value, Map.of());
        }

        public Builder blockEntity(RelativeBlockPos pos, String value, Map<String, String> properties) {
            put(blockEntity, pos, pos, value, properties);
            return this;
        }

        public Builder entity(RelativeBlockPos pos, String value) {
            return entity(pos, value, Map.of());
        }

        public Builder entity(RelativeBlockPos pos, String value, Map<String, String> properties) {
            put(entity, pos, pos, value, properties);
            return this;
        }

        public Builder fluid(RelativeBlockPos pos, String value) {
            return fluid(pos, value, Map.of());
        }

        public Builder fluid(RelativeBlockPos pos, String value, Map<String, String> properties) {
            put(fluid, pos, pos, value, properties);
            return this;
        }

        public Builder capability(RelativeBlockPos pos, String value) {
            return capability(pos, value, Map.of());
        }

        public Builder capability(RelativeBlockPos pos, String value, Map<String, String> properties) {
            put(capability, pos, pos, value, properties);
            return this;
        }

        public Builder context(String key, String value) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Context key must not be blank");
            }
            if (value == null) {
                throw new IllegalArgumentException("Context value must not be null");
            }
            contextValues.put(key, value);
            return this;
        }

        public Builder region(RelativeBlockPos min, RelativeBlockPos max, String value) {
            return region(min, max, value, Map.of());
        }

        public Builder region(RelativeBlockPos min, RelativeBlockPos max, String value, Map<String, String> properties) {
            put(region, min, max, value, properties);
            return this;
        }

        public InMemoryTargetSnapshot build() {
            return new InMemoryTargetSnapshot(targets, contextValues);
        }

        private void put(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max, String value, Map<String, String> properties) {
            if (min == null || max == null) {
                throw new IllegalArgumentException("Snapshot target bounds must not be null");
            }
            targets.put(new TargetKey(targetType, min, max), new SnapshotTarget(value, properties));
        }
    }

    private record TargetKey(
                    ResourceLocation targetType,
                    RelativeBlockPos min,
                    RelativeBlockPos max) {
    }
}
