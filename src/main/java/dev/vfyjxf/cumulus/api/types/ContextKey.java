package dev.vfyjxf.cumulus.api.types;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Strongly-typed identifier for a single entry inside {@code RuntimeStructureContext}.
 *
 * <p>Replaces the previous {@code Map<String, String>} shape, where keys were bare
 * strings and values required per-call parsing. With {@code ContextKey<T>} the value
 * type is fixed at the key definition site and enforced by {@code RuntimeStructureContext}
 * on every {@code put}/{@code get}.
 *
 * <h2>Definition (mod-side, once)</h2>
 * <pre>{@code
 * public final class ModContextKeys {
 *     public static final ContextKey<BlockPos>     controller = ContextKey.blockPos("controller");
 *     public static final ContextKey<Direction>    facing     = ContextKey.direction("facing");
 *     public static final ContextKey<Integer>      tier       = ContextKey.integer("tier");
 *     public static final ContextKey<ResourceLocation> controllerId =
 *         ContextKey.resourceLocation("controller_id");
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // authoring — derive "facing" from controller.facing block-state property
 * .context(ContextRuleDescriptor.derive(
 *     ModContextKeys.facing,
 *     ContextSources.capture("controller").property("facing")))
 *
 * // machine logic — typed read, no casting
 * Direction facing = ctx.get(ModContextKeys.facing).orElse(Direction.NORTH);
 * }</pre>
 *
 * <p>Keys are value-equal when both {@link #name()} and {@link #type()} match.
 *
 * @param name human-readable key name; non-blank, treated as an opaque identifier
 *            (no dotted paths — use {@code ContextSource} for that)
 * @param type the runtime class of the value, used for instanceof-style guards when
 *            reading from a heterogeneous map; supplied explicitly because Java
 *            generics are erased
 */
public record ContextKey<T>(String name, Class<T> type) {

    public ContextKey {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ContextKey name must not be blank");
        }
        Objects.requireNonNull(type, "ContextKey type must not be null");
    }

    // ───────────────────────────────────────────────────────────────────
    // Primitive value type factories
    // ───────────────────────────────────────────────────────────────────

    public static ContextKey<String> string(String name) {
        return new ContextKey<>(name, String.class);
    }

    public static ContextKey<Integer> integer(String name) {
        return new ContextKey<>(name, Integer.class);
    }

    public static ContextKey<Long> longKey(String name) {
        return new ContextKey<>(name, Long.class);
    }

    public static ContextKey<Boolean> booleanKey(String name) {
        return new ContextKey<>(name, Boolean.class);
    }

    public static ContextKey<Double> doubleKey(String name) {
        return new ContextKey<>(name, Double.class);
    }

    // ───────────────────────────────────────────────────────────────────
    // Minecraft value type factories
    // ───────────────────────────────────────────────────────────────────

    public static ContextKey<BlockPos> blockPos(String name) {
        return new ContextKey<>(name, BlockPos.class);
    }

    public static ContextKey<Direction> direction(String name) {
        return new ContextKey<>(name, Direction.class);
    }

    public static ContextKey<ResourceLocation> resourceLocation(String name) {
        return new ContextKey<>(name, ResourceLocation.class);
    }

    // ───────────────────────────────────────────────────────────────────
    // Custom / arbitrary type
    // ───────────────────────────────────────────────────────────────────

    public static <T> ContextKey<T> of(String name, Class<T> type) {
        return new ContextKey<>(name, type);
    }

    /**
     * Cast an untyped value to this key's value type, throwing a clear
     * {@link ClassCastException} if the value is non-null and incompatible.
     * Used by {@code RuntimeStructureContext.get} after a raw map lookup.
     */
    @SuppressWarnings("unchecked")
    public T cast(Object value) {
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException(
                            "ContextKey '" + name + "' expects " + type.getName() + " but got " + value.getClass().getName());
        }
        return (T) value;
    }
}
