package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.types.ContextKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Unified typed key→value map — the single primitive underlying
 * {@link dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext},
 * {@link CheckContext}, and
 * {@link dev.vfyjxf.cumulus.api.migration.MutableRuntimeContext}.
 *
 * <p>Replaces the duplicated {@code Map<ContextKey<?>, Object>} + copy-pasted
 * type-check logic that previously existed in three separate classes.
 *
 * <h2>Immutable view</h2>
 * <pre>{@code
 * ContextMap ctx = ContextMap.builder()
 *     .put(FACING, Direction.NORTH)
 *     .put(TIER, 3)
 *     .build();
 * Direction facing = ctx.get(FACING).orElse(Direction.NORTH);
 * }</pre>
 *
 * <h2>Mutable view (for computation phases and migration)</h2>
 * <pre>{@code
 * ContextMap.Mutable mut = ContextMap.mutable();
 * mut.put(CASING_COUNT, 26);
 * mut.remove(LEGACY_KEY);
 * ContextMap frozen = mut.freeze();
 * }</pre>
 */
public final class ContextMap {

    private final Map<ContextKey<?>, Object> entries;

    private ContextMap(Map<ContextKey<?>, Object> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    /** Create an empty immutable map. */
    public static ContextMap empty() {
        return new ContextMap(Map.of());
    }

    /** Create an immutable map from a raw entry map (defensive copy). */
    public static ContextMap of(Map<ContextKey<?>, Object> entries) {
        return new ContextMap(entries);
    }

    /** Start a fluent immutable builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Create a mutable map (starts empty). */
    public static Mutable mutable() {
        return new Mutable(new LinkedHashMap<>());
    }

    /** Create a mutable map seeded from this immutable map. */
    public Mutable mutableCopy() {
        return new Mutable(new LinkedHashMap<>(entries));
    }

    /** Typed read: returns the value cast to the key's type, or empty. */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(ContextKey<T> key) {
        Objects.requireNonNull(key, "key must not be null");
        var raw = entries.get(key);
        return raw == null ? Optional.empty() : Optional.of((T) raw);
    }

    /** True if a value exists under this key. */
    public boolean contains(ContextKey<?> key) {
        return entries.containsKey(key);
    }

    /** Unmodifiable entry set view. */
    public Map<ContextKey<?>, Object> entries() {
        return entries;
    }

    /** Number of entries. */
    public int size() {
        return entries.size();
    }

    // ── Mutable companion ─────────────────────────────────────────────

    /**
     * Mutable view of a {@link ContextMap}. Used during check computation
     * (CheckContext) and migration (MutableRuntimeContext). Call {@link #freeze}
     * to produce an immutable snapshot.
     */
    public static final class Mutable {

        private final Map<ContextKey<?>, Object> entries;

        Mutable(Map<ContextKey<?>, Object> entries) {
            this.entries = entries;
        }

        /** Typed put — type-checked at runtime via {@code key.type().isInstance(value)}. */
        public <T> Mutable put(ContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            if (!key.type().isInstance(value)) {
                throw new ClassCastException(
                        "ContextKey '" + key.name() + "' expects " + key.type().getName()
                                + " but got " + value.getClass().getName());
            }
            entries.put(key, value);
            return this;
        }

        /** Escape hatch for heterogeneous puts (runtime type check still fires). */
        public Mutable putRaw(ContextKey<?> key, Object value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            if (!key.type().isInstance(value)) {
                throw new ClassCastException(
                        "ContextKey '" + key.name() + "' expects " + key.type().getName()
                                + " but got " + value.getClass().getName());
            }
            entries.put(key, value);
            return this;
        }

        /** Bulk put from a raw map. Each entry is type-checked. */
        public Mutable putAll(Map<ContextKey<?>, Object> source) {
            for (var entry : source.entrySet()) {
                putRaw(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /** Remove the value under this key. */
        public Mutable remove(ContextKey<?> key) {
            entries.remove(key);
            return this;
        }

        /** Typed read (same as immutable). */
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(ContextKey<T> key) {
            Objects.requireNonNull(key, "key must not be null");
            var raw = entries.get(key);
            return raw == null ? Optional.empty() : Optional.of((T) raw);
        }

        /** True if a value exists under this key. */
        public boolean contains(ContextKey<?> key) {
            return entries.containsKey(key);
        }

        /** Mutable entry set view. */
        public Map<ContextKey<?>, Object> entries() {
            return entries;
        }

        /** Freeze into an immutable {@link ContextMap}. */
        public ContextMap freeze() {
            return new ContextMap(entries);
        }
    }

    // ── Fluent immutable builder ──────────────────────────────────────

    /**
     * Fluent builder for immutable {@link ContextMap}. Each put is type-checked.
     */
    public static final class Builder {

        private final Mutable mutable = mutable();

        public <T> Builder put(ContextKey<T> key, T value) {
            mutable.put(key, value);
            return this;
        }

        public Builder putAll(Map<ContextKey<?>, Object> source) {
            mutable.putAll(source);
            return this;
        }

        public ContextMap build() {
            return mutable.freeze();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextMap that)) return false;
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return "ContextMap" + entries;
    }
}
