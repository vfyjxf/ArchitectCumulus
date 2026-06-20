package dev.vfyjxf.cumulus.api.types;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Open, type-safe registry mapping a {@link ResourceLocation} id to a value of type {@code V}.
 *
 * <p>Used to register {@code TargetType}, {@code PredicateType}, {@code TransformType},
 * {@code DiagnosticCode}, {@code MigrationOp}, etc. without coupling callers to the
 * concrete implementations.
 *
 * <p>The registry is immutable: {@link #register} returns a new {@link Registry} rather
 * than mutating in place. This keeps registry snapshots safe to publish across threads
 * (important during datapack reload — see {@code RuntimeAssetRepository}).
 *
 * <p>Registration order is preserved via a backing {@link LinkedHashMap}, so iteration is
 * deterministic for diagnostics and datagen.
 *
 * @param <V> the registered value type (typically an interface like {@code TargetType})
 */
public record Registry<V>(Map<ResourceLocation, V> entries) {

    public Registry {
        Objects.requireNonNull(entries, "Registry entries must not be null");
        entries = Map.copyOf(entries);
    }

    public static <V> Registry<V> empty() {
        return new Registry<>(Map.of());
    }

    @SafeVarargs
    public static <V extends RegistryValue> Registry<V> of(V... values) {
        var map = new LinkedHashMap<ResourceLocation, V>();
        for (var v : values) {
            map.put(v.id(), v);
        }
        return new Registry<>(map);
    }

    public Optional<V> get(ResourceLocation id) {
        Objects.requireNonNull(id, "Registry lookup id must not be null");
        return Optional.ofNullable(entries.get(id));
    }

    /**
     * Return a new registry with the additional entry. If {@code id} is already present
     * it is replaced; existing entries keep their iteration order.
     */
    public Registry<V> register(ResourceLocation id, V value) {
        Objects.requireNonNull(id, "Registry id must not be null");
        Objects.requireNonNull(value, "Registry value must not be null");
        var next = new LinkedHashMap<>(entries);
        next.put(id, value);
        return new Registry<>(next);
    }

    public Registry<V> register(V value) {
        if (!(value instanceof RegistryValue rv)) {
            throw new IllegalArgumentException(
                "Registry value must implement RegistryValue when using the single-arg register(); got "
                    + value.getClass());
        }
        return register(rv.id(), value);
    }

    public int size() {
        return entries.size();
    }

    public boolean contains(ResourceLocation id) {
        return entries.containsKey(id);
    }

    /**
     * Marker interface implemented by values that carry their own id, so
     * {@link #register(Object)} can resolve it without an explicit id parameter.
     */
    public interface RegistryValue {
        ResourceLocation id();
    }
}
