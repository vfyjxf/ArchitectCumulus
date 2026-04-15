package dev.vfyjxf.cumulus.api.runtime;

import net.minecraft.resources.ResourceLocation;

/**
 * 上下文字段的键。
 *
 * <p>通过 {@link #valueType()} 提供类型信息，配合 {@link RuntimeContextView#get(ContextKey)} 做类型安全访问。
 */
public interface ContextKey<T> {
    /** 稳定 id。 */
    ResourceLocation id();

    /** 值类型。 */
    Class<T> valueType();
}
