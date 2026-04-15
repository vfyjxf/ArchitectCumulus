package dev.vfyjxf.cumulus.api.driver;

import dev.vfyjxf.cumulus.api.engine.Budget;
import dev.vfyjxf.cumulus.api.engine.EngineInstanceDirectory;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;

/**
 * driver 运行上下文。
 *
 * <p>driver 用于在“无事件流/事件不完整”的情况下主动产生 dirty sections/anchors 或缺数据请求。
 * driver 不应直接修改世界；其产物通过 {@link DriverOutput} 返回给引擎合并处理。
 */
public interface DriverContext {
    /** 当前 tick 计数。 */
    long tick();

    /** 世界只读视图。 */
    PartitionAccess world();

    /** 引擎实例目录（只读）。 */
    EngineInstanceDirectory instances();

    /** 本 tick 预算。 */
    Budget budget();
}
