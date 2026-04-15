package dev.vfyjxf.cumulus.api.partition;

/**
 * section 加载状态。
 */
public enum LoadState {
    /** 已加载，可读取。 */
    LOADED,
    /** 未加载/缺失，不应读取；应通过 DataRequest 请求宿主加载。 */
    MISSING
}
