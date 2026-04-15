package dev.vfyjxf.cumulus.api.schema;

import com.mojang.serialization.Dynamic;
import net.minecraft.resources.ResourceLocation;

/**
 * 数据驱动组件类型：{@code typeId + config -> runtime component}。
 *
 * <p>例如：一个 binder type 可以把 JSON config 解析成 {@link dev.vfyjxf.cumulus.api.pass.Binder} 实例。
 * type 本身应保持无状态/可复用；需要状态时可在 {@link SchemaBuildContext} 中提供。
 */
public interface SchemaComponentType<T> {
    ResourceLocation id();

    T create(Dynamic<?> config, SchemaBuildContext ctx);
}
