package dev.vfyjxf.cumulus.api.migration;

import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable view over a {@link RuntimeStructureContext} passed to {@link MigrationOp#apply}.
 * Allows ops to add, rename, transform, and remove entries by typed {@link ContextKey}.
 *
 * <p>After all ops run, the executor calls {@link #toContext()} to produce an
 * immutable snapshot.
 */
public final class MutableRuntimeContext {

    private final StructureInstanceKey key;
    private final int revision;
    private final Map<ContextKey<?>, Object> values = new LinkedHashMap<>();

    public MutableRuntimeContext(RuntimeStructureContext source) {
        Objects.requireNonNull(source, "Source context must not be null");
        this.key = source.key();
        this.revision = source.revision();
        this.values.putAll(source.values());
    }

    public StructureInstanceKey key() {
        return key;
    }

    public int revision() {
        return revision;
    }

    /** Read a typed value. */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(ContextKey<T> key) {
        Objects.requireNonNull(key, "Context key must not be null");
        var raw = values.get(key);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(key.cast(raw));
    }

    /** Backwards-compat string lookup; matches any key with that name. */
    public Optional<String> getStringValue(String name) {
        return values.entrySet().stream()
                .filter(e -> e.getKey().name().equals(name))
                .map(e -> Optional.of(String.valueOf(e.getValue())))
                .findFirst()
                .orElse(Optional.empty());
    }

    /** Typed put. */
    public <T> void put(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "Context key must not be null");
        Objects.requireNonNull(value, "Context value must not be null");
        if (!key.type().isInstance(value)) {
            throw new ClassCastException(
                    "ContextKey '" + key.name() + "' expects " + key.type().getName()
                            + " but got " + value.getClass().getName());
        }
        values.put(key, value);
    }

    /** Untyped put for code that copies values from another context. */
    public void putRaw(ContextKey<?> key, Object value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (!key.type().isInstance(value)) {
            throw new ClassCastException(
                    "ContextKey '" + key.name() + "' expects " + key.type().getName()
                            + " but got " + value.getClass().getName());
        }
        values.put(key, value);
    }

    /** Remove a value by key. Returns the removed value, if any. */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> remove(ContextKey<T> key) {
        Objects.requireNonNull(key);
        var raw = values.remove(key);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(key.cast(raw));
    }

    /** True if any value is stored under {@code key}. */
    public boolean contains(ContextKey<?> key) {
        return values.containsKey(key);
    }

    /** Snapshot view as an immutable {@link RuntimeStructureContext}. */
    public RuntimeStructureContext toContext() {
        return RuntimeStructureContext.builder(key)
                .revision(revision)
                .putAll(values)
                .build();
    }

    /** Read-only view of the entries (for iteration in custom ops). */
    public Iterable<Map.Entry<ContextKey<?>, Object>> entries() {
        return values.entrySet();
    }
}
