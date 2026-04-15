/**
 * 数据驱动 schema（v2 骨架）。
 *
 * <p>本包提供 {@link dev.vfyjxf.cumulus.api.schema.ArchitectSchemaSpec} 与 loader，
 * 用 {@link com.mojang.serialization.Codec} 解析 JSON，借助 {@link dev.vfyjxf.cumulus.api.schema.SchemaTypeRegistry}
 * 构建运行时组件（anchorFinder/binder/checker/compiler...）。
 *
 * <p>v1 仅提供可落地的接口与最小 common 实现；datapack 接入与 /reload 热更由后续实现。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.schema;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
