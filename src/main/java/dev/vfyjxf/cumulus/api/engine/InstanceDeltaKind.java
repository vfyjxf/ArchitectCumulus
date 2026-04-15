package dev.vfyjxf.cumulus.api.engine;

/**
 * delta 类型。
 */
public enum InstanceDeltaKind {
    /** 新发现并创建的实例。 */
    ADDED,
    /** 状态/诊断/上下文等发生变化的实例。 */
    UPDATED,
    /** 实例被移除（例如 anchor 卸载或 probe 失败）。 */
    REMOVED
}
