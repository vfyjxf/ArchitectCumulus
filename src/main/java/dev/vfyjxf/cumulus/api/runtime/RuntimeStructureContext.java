package dev.vfyjxf.cumulus.api.runtime;

import dev.vfyjxf.cumulus.api.types.ContextKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable, machine-facing snapshot of a structure instance's computed context.
 *
 * <p>Replaces the legacy {@code Map<String, String>} shape: values are keyed by
 * typed {@link ContextKey} and read back with compile-time-checked types via
 * {@link #get(ContextKey)}. Entries are produced by {@code CheckContext} derived
 * and computed tiers (see {@code toRuntime}) and consumed by machines, the
 * substructure lifecycle state, and migration ops.
 */
public record RuntimeStructureContext(
                StructureInstanceKey key,
                int revision,
                Map<ContextKey<?>, Object> values) {

    public RuntimeStructureContext {
        if (key == null) {
            throw new IllegalArgumentException("Context key must not be null");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Context revision must not be negative");
        }
        values = Map.copyOf(values);
    }

    /**
     * Typed read: returns the value under {@code key} cast to {@code T}, or empty
     * if absent. Callers should store the result via {@code Optional} rather than
     * unboxing blindly.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(ContextKey<T> key) {
        var raw = values.get(key);
        return raw == null ? Optional.empty() : Optional.of((T) raw);
    }

    public static Builder builder(StructureInstanceKey key) {
        return new Builder(key);
    }

    public static final class Builder {
        private final StructureInstanceKey key;
        private int revision;
        private final Map<ContextKey<?>, Object> values = new LinkedHashMap<>();

        private Builder(StructureInstanceKey key) {
            if (key == null) {
                throw new IllegalArgumentException("Context key must not be null");
            }
            this.key = key;
        }

        public Builder revision(int revision) {
            this.revision = revision;
            return this;
        }

        public <T> Builder put(ContextKey<T> key, T value) {
            if (key == null) {
                throw new IllegalArgumentException("Context key must not be null");
            }
            if (value == null) {
                throw new IllegalArgumentException("Context value must not be null");
            }
            values.put(key, value);
            return this;
        }

        public Builder putAll(Map<ContextKey<?>, Object> source) {
            values.putAll(source);
            return this;
        }

        public RuntimeStructureContext build() {
            return new RuntimeStructureContext(key, revision, values);
        }
    }
}
