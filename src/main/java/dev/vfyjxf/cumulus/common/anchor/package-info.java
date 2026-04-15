/**
 * common anchor 实现集合。
 *
 * <p>AnchorFinder 负责“在已加载范围内发现/探测结构实例锚点”。
 * v1 设计中不强制加载未加载区块，因此 discover/probe 应在未加载时优雅返回（empty/UNLOADED）。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.common.anchor;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
