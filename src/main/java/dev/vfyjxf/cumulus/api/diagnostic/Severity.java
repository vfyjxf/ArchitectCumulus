package dev.vfyjxf.cumulus.api.diagnostic;

/**
 * 诊断严重级别。
 */
public enum Severity {
    /** 信息。 */
    INFO,
    /** 警告（不一定导致实例 ERROR）。 */
    WARN,
    /** 错误（通常导致实例 ERROR）。 */
    ERROR
}
