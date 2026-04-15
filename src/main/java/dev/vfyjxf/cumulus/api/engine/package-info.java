/**
 * 引擎核心：实例生命周期、tick 调度、delta 输出。
 *
 * <p>引擎通常按每个 {@code Level/维度} 各实例化一份（API 本身不携带维度信息）：
 * 通过 {@link dev.vfyjxf.cumulus.api.engine.InstanceKey}（schemaId + anchorPos）索引结构实例。
 *
 * <p>缺数据与不确定性通过 {@code InstanceStatus.INCOMPLETE + DataRequest} 表达；
 * 引擎不会在这里“强制加载区块”。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
