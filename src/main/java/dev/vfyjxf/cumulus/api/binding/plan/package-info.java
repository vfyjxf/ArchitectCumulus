/**
 * 数据化的绑定计划（BindingPlan）。
 *
 * <p>该包提供一套“扫描 + 匹配”的声明式 schema，用于描述 binder 的行为：
 * <ul>
 *     <li>{@link dev.vfyjxf.cumulus.api.binding.plan.ScanSpec}：如何扫描（anchor/box/entity radius）</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.binding.plan.MatchSpec}：如何匹配（id/tag/逻辑组合）</li>
 * </ul>
 *
 * <p>common 中的 {@code PlanBinder} 是一个默认实现，可直接执行这些计划；同时 {@link com.mojang.serialization.Codec}
 * 允许将计划序列化为 JSON（便于后续 datapack/datagen 接入）。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.binding.plan;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
