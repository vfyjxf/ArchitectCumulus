package dev.vfyjxf.cumulus.api.runtime;

public interface RuntimeContext extends RuntimeContextView {
    <T> void put(ContextKey<T> key, T value);
}
