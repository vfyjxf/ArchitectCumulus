package dev.vfyjxf.cumulus.api.runtime;

/**
 * 可写运行时上下文。
 *
 * <p>常用于 compiler/linker 组装上下文；对外通常以 {@link RuntimeContextView} 暴露。
 */
public interface RuntimeContext extends RuntimeContextView {
    <T> void put(ContextKey<T> key, T value);
}
