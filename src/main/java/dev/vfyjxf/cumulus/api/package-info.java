/**
 * ArchitectCumulus 的公共 API。
 *
 * <p>核心概念：
 * <ul>
 *     <li>{@link dev.vfyjxf.cumulus.api.engine.ArchitectEngine}：每 tick 收敛的结构实例引擎</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.ArchitectSchema}：单一结构类型（“一种机器”）的流水线定义</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.partition.PartitionAccess}：按分块（MC section）读取世界数据的只读视图</li>
 *     <li>{@link dev.vfyjxf.cumulus.api.request.DataRequest}：缺数据时的“建议加载”请求（引擎不强制加载）</li>
 * </ul>
 *
 * <p>本库默认针对 Minecraft：以 {@link dev.vfyjxf.cumulus.api.partition.SectionKey}（16×16×16）
 * 作为增量与请求的最小粒度；当世界数据未加载时，pass 返回 {@code INCOMPLETE} 并附带请求，
 * 由宿主（模组/服务器系统）决定是否以及如何加载。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
