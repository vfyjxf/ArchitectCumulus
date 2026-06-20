package dev.vfyjxf.cumulus.api.target;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Targets {
    private Targets() {}

    public static TargetPattern block() {
        return simple("cumulus:block");
    }

    public static TargetPattern blockEntity() {
        return simple("cumulus:block_entity");
    }

    public static TargetPattern blockEntity(ResourceLocation blockId, ResourceLocation blockEntityId, String nbt) {
        if (blockId == null) {
            throw new IllegalArgumentException("Block id must not be null");
        }
        if (blockEntityId == null) {
            throw new IllegalArgumentException("Block entity id must not be null");
        }
        if (nbt == null) {
            throw new IllegalArgumentException("Block entity NBT must not be null");
        }
        return new TargetPattern(
                        new TargetDescriptor(ResourceLocation.parse("cumulus:block_entity"), Map.of("block", blockId.toString(), "block_entity", blockEntityId.toString(), "nbt", nbt)),
                        List.of(),
                        Set.of());
    }

    public static TargetPattern entity() {
        return simple("cumulus:entity");
    }

    public static TargetPattern entity(ResourceLocation entityId, String nbt) {
        if (entityId == null) {
            throw new IllegalArgumentException("Entity id must not be null");
        }
        if (nbt == null) {
            throw new IllegalArgumentException("Entity NBT must not be null");
        }
        return new TargetPattern(
                        new TargetDescriptor(ResourceLocation.parse("cumulus:entity"), Map.of("entity", entityId.toString(), "nbt", nbt)),
                        List.of(),
                        Set.of());
    }

    public static TargetPattern fluid() {
        return simple("cumulus:fluid");
    }

    public static TargetPattern capability(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Capability target name must not be blank");
        }
        return new TargetPattern(
                        new TargetDescriptor(ResourceLocation.parse("cumulus:capability"), Map.of("name", name)),
                        List.of(),
                        Set.of());
    }

    public static TargetPattern context(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Context target key must not be blank");
        }
        return new TargetPattern(
                        new TargetDescriptor(ResourceLocation.parse("cumulus:context"), Map.of("key", key)),
                        List.of(),
                        Set.of());
    }

    public static TargetPattern region(RelativeBlockPos from, RelativeBlockPos to) {
        validateRegionBounds(from, to);
        return new TargetPattern(
                        new TargetDescriptor(ResourceLocation.parse("cumulus:region"), Map.of("from", from.asString(), "to", to.asString())),
                        List.of(),
                        Set.of());
    }

    public static void validateRegionBounds(RelativeBlockPos from, RelativeBlockPos to) {
        if (from == null) {
            throw new IllegalArgumentException("Region from position must not be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Region to position must not be null");
        }
        if (from.x() > to.x() || from.y() > to.y() || from.z() > to.z()) {
            throw new IllegalArgumentException("Region bounds must be inclusive and non-inverted");
        }
    }

    private static TargetPattern simple(String id) {
        return new TargetPattern(
                        new TargetDescriptor(ResourceLocation.parse(id), Map.of()),
                        List.of(),
                        Set.of());
    }
}
