/**
 * 绑定结果（bindings）与部件引用（part refs）。
 *
 * <p>{@link dev.vfyjxf.cumulus.api.pass.Binder} 的输出会把“结构实例由哪些部件组成”写成
 * {@link dev.vfyjxf.cumulus.api.binding.BindingsView}，后续 checker/compiler 等 pass 只需要读取 bindings，
 * 避免反复扫描世界。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.binding;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
