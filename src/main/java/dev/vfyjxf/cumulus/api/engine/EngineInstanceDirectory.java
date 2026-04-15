package dev.vfyjxf.cumulus.api.engine;

/**
 * 引擎侧实例目录：在 {@link InstanceDirectory} 基础上提供遍历能力。
 *
 * <p>主要供 driver 使用（例如：遍历所有实例的 interest，或对 INCOMPLETE 实例发请求）。
 */
public interface EngineInstanceDirectory extends InstanceDirectory {
    /**
     * 遍历所有实例视图。
     *
     * <p>返回的 view 可能是快照或轻量代理，具体由实现决定；调用方不应假设可变性。
     */
    Iterable<InstanceView> views();
}
