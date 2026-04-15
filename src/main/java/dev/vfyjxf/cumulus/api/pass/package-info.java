/**
 * 结构实例流水线（passes）。
 *
 * <p>典型执行顺序（由引擎调度）：
 * <ol>
 *     <li>{@link dev.vfyjxf.cumulus.api.pass.Binder}：扫描并生成 bindings</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.pass.Checker}*：任意数量的合法性检查</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.pass.Compiler}：将结构编译为运行时上下文</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.pass.DependencyDeclarer}：声明跨实例只读依赖</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.pass.Linker}：读取依赖上下文并链接</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.pass.Exporter}：导出调试/可视化/数据生产视图</li>
 * </ol>
 *
 * <p>每个 pass 都可以产生诊断、缺数据请求与兴趣集更新。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
