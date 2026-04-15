package dev.vfyjxf.cumulus.api.runtime;

import java.util.Optional;
import java.util.Set;

/**
 * 运行时上下文只读视图。
 *
 * <p>上下文由 compiler/linker 产生，供机器逻辑读取。
 */
public interface RuntimeContextView {
    /**
     * 按 key 获取一个值（类型安全）。
     */
    <T> Optional<T> get(ContextKey<T> key);

    /**
     * 当前包含的所有 key。
     */
    Set<ContextKey<?>> keys();
}
