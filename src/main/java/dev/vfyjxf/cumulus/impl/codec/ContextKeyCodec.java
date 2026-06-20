package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.types.ContextKey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;

/**
 * Codec helper for {@link ContextKey}. Round-trips the typed key through a
 * two-field wire shape: {@code {"name": "...", "type": "..."}} where the type
 * is the binary name of the value class.
 *
 * <p>Built-in Minecraft types (BlockPos, Direction, ResourceLocation, primitives)
 * get a short alias so the wire format stays compact and stable across mappings
 * refactors. Custom types fall back to {@link Class#getName()}.
 */
public final class ContextKeyCodec {

    /** Wire aliases for common value types. Stable across refactors. */
    private static final Map<String, Class<?>> primitiveAliases = Map.of("string", String.class, "int", Integer.class, "long", Long.class, "boolean", Boolean.class, "double", Double.class, "block_pos", BlockPos.class, "direction", Direction.class, "resource_location", ResourceLocation.class);

    private ContextKeyCodec() {}

    public static Map<String, String> serialize(ContextKey<?> key) {
        Objects.requireNonNull(key, "ContextKey must not be null");
        return Map.of("name", key.name(), "type", aliasFor(key.type()));
    }

    public static ContextKey<?> deserialize(Map<String, String> wire) {
        Objects.requireNonNull(wire, "Wire map must not be null");
        var name = wire.get("name");
        var typeAlias = wire.get("type");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ContextKey wire form missing 'name'");
        }
        if (typeAlias == null || typeAlias.isBlank()) {
            throw new IllegalArgumentException("ContextKey wire form missing 'type'");
        }
        var type = primitiveAliases.get(typeAlias);
        if (type == null) {
            // Fall back to binary name lookup
            try {
                type = Class.forName(typeAlias);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(
                                "Unknown ContextKey value type: " + typeAlias, e);
            }
        }
        return ContextKey.of(name, type);
    }

    private static String aliasFor(Class<?> type) {
        for (var entry : primitiveAliases.entrySet()) {
            if (entry.getValue().equals(type)) {
                return entry.getKey();
            }
        }
        return type.getName();
    }
}
