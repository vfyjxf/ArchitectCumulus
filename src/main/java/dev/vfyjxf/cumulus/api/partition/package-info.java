/**
 * 世界读取抽象（按分块访问）。
 *
 * <p>默认粒度是 Minecraft section（16×16×16），对应 {@link dev.vfyjxf.cumulus.api.partition.SectionKey}。
 * 引擎与 pass 通过 {@link dev.vfyjxf.cumulus.api.partition.PartitionAccess} 获取只读 {@link dev.vfyjxf.cumulus.api.partition.SectionView}。
 *
 * <p>当 section 未加载时，访问层应能表达 {@link dev.vfyjxf.cumulus.api.partition.LoadState#MISSING}，
 * 上层据此返回 {@code INCOMPLETE} 并产出 {@link dev.vfyjxf.cumulus.api.request.DataRequest}。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.partition;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
