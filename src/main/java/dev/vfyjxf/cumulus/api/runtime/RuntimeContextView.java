package dev.vfyjxf.cumulus.api.runtime;

import java.util.Optional;
import java.util.Set;

public interface RuntimeContextView {
    <T> Optional<T> get(ContextKey<T> key);

    Set<ContextKey<?>> keys();
}
