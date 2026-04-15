package dev.vfyjxf.cumulus.api.binding.plan;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

public sealed interface ScanSpec permits ScanSpec.Anchor, ScanSpec.Box, ScanSpec.EntityRadius {
    Codec<ScanSpec> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ScanSpec, T>> decode(DynamicOps<T> ops, T input) {
            var dynamic = new com.mojang.serialization.Dynamic<>(ops, input);
            return dynamic.get("type").asString().flatMap(type -> switch (type) {
                case "anchor" -> Anchor.CODEC.decode(ops, input).map(pair -> pair.mapFirst(v -> (ScanSpec) v));
                case "box" -> Box.CODEC.decode(ops, input).map(pair -> pair.mapFirst(v -> (ScanSpec) v));
                case "entity_radius" -> EntityRadius.CODEC.decode(ops, input).map(pair -> pair.mapFirst(v -> (ScanSpec) v));
                default -> DataResult.error(() -> "Unknown scan type: " + type);
            });
        }

        @Override
        public <T> DataResult<T> encode(ScanSpec input, DynamicOps<T> ops, T prefix) {
            Objects.requireNonNull(input, "input");
            String type;
            DataResult<T> base;
            if (input instanceof Anchor anchor) {
                type = "anchor";
                base = Anchor.CODEC.encodeStart(ops, anchor);
            } else if (input instanceof Box box) {
                type = "box";
                base = Box.CODEC.encodeStart(ops, box);
            } else if (input instanceof EntityRadius entityRadius) {
                type = "entity_radius";
                base = EntityRadius.CODEC.encodeStart(ops, entityRadius);
            } else {
                return DataResult.error(() -> "Unknown scan spec: " + input.getClass().getName());
            }
            return base.flatMap(encoded -> ops.mergeToMap(encoded, ops.createString("type"), ops.createString(type)));
        }
    };

    record Anchor(int offsetX, int offsetY, int offsetZ) implements ScanSpec {
        public static final Codec<Anchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("offset_x", 0).forGetter(Anchor::offsetX),
                Codec.INT.optionalFieldOf("offset_y", 0).forGetter(Anchor::offsetY),
                Codec.INT.optionalFieldOf("offset_z", 0).forGetter(Anchor::offsetZ)
        ).apply(instance, Anchor::new));

        public static Anchor atAnchor() {
            return new Anchor(0, 0, 0);
        }
    }

    record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements ScanSpec {
        public static final Codec<Box> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("min_x").forGetter(Box::minX),
                Codec.INT.fieldOf("min_y").forGetter(Box::minY),
                Codec.INT.fieldOf("min_z").forGetter(Box::minZ),
                Codec.INT.fieldOf("max_x").forGetter(Box::maxX),
                Codec.INT.fieldOf("max_y").forGetter(Box::maxY),
                Codec.INT.fieldOf("max_z").forGetter(Box::maxZ)
        ).apply(instance, Box::new));
    }

    record EntityRadius(int radius) implements ScanSpec {
        public static final Codec<EntityRadius> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("radius").forGetter(EntityRadius::radius)
        ).apply(instance, EntityRadius::new));

        public EntityRadius {
            if (radius < 0) {
                throw new IllegalArgumentException("radius must be >= 0");
            }
        }
    }
}
