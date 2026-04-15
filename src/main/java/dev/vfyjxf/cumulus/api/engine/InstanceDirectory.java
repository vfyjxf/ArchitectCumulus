package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

import java.util.Optional;

/**
 * 引擎内部/外部对“其他实例”的只读访问。
 *
 * <p>主要用于跨实例依赖（link 阶段）与 driver 的扫描决策。
 */
public interface InstanceDirectory {
    /**
     * 获取目标实例的运行时上下文（如果存在）。
     */
    Optional<RuntimeContextView> context(InstanceKey key);

    /**
     * 获取目标实例的状态（如果存在）。
     */
    Optional<InstanceStatus> status(InstanceKey key);
}
