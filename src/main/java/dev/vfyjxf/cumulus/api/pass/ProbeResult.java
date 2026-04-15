package dev.vfyjxf.cumulus.api.pass;

/**
 * probe 结果。
 */
public enum ProbeResult {
    /** 数据完整且确认存在。 */
    PRESENT,
    /** 数据完整且确认不存在。 */
    ABSENT,
    /** 缺数据，无法判断。 */
    UNLOADED
}
