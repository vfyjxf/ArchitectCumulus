package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.export.ExportView;
import dev.vfyjxf.cumulus.api.interest.InterestView;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

import java.util.List;
import java.util.Optional;

/**
 * 结构实例的只读视图（快照）。
 *
 * <p>该接口通常由引擎在 tick 结束时输出（见 {@link EngineFrame}），用于：
 * <ul>
 *     <li>外部系统读取结构合法性（status/diagnostics）</li>
 *     <li>读取运行时上下文（runtimeContext）</li>
 *     <li>读取导出视图（exportView，用于可视化/数据生产的数据访问）</li>
 *     <li>驱动层读取兴趣集（interest），用于后续增量轮询</li>
 * </ul>
 */
public interface InstanceView {
    /** 实例键。 */
    InstanceKey key();

    /** 实例状态（三态：OK/ERROR/INCOMPLETE）。 */
    InstanceStatus status();

    /** 结构化诊断列表。 */
    List<Diagnostic> diagnostics();

    /** 运行时上下文（结构合法且数据完整时可用）。 */
    Optional<RuntimeContextView> runtimeContext();

    /** 导出视图（可选）。 */
    Optional<ExportView> exportView();

    /** 兴趣集（用于增量轮询）。 */
    InterestView interest();
}
