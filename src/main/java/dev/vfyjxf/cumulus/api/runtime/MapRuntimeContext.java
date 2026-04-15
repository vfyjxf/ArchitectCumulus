package dev.vfyjxf.cumulus.api.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 基于 HashMap 的默认上下文实现（用于 common/dummy 与简单场景）。
 */
public final class MapRuntimeContext implements RuntimeContext {
    private final Map<ContextKey<?>, Object> values;

    public MapRuntimeContext() {
        this.values = new HashMap<>();
    }

    public MapRuntimeContext(Map<ContextKey<?>, Object> values) {
        this.values = new HashMap<>(Objects.requireNonNull(values, "values"));
    }

    @Override
    public <T> Optional<T> get(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(key.valueType().cast(value));
    }

    @Override
    public Set<ContextKey<?>> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    @Override
    public <T> void put(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        values.put(key, key.valueType().cast(Objects.requireNonNull(value, "value")));
    }
}
