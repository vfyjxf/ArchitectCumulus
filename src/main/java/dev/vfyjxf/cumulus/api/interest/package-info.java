/**
 * 兴趣集（Interest Set）。
 *
 * <p>兴趣集用于在“无事件流/事件不完整”的情况下，让引擎只轮询与实例相关的世界分块/坐标/实体，
 * 从而把全量扫描成本收敛到与活跃实例规模相关的数量级。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.interest;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
