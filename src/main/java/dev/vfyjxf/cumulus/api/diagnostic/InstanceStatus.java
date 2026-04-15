package dev.vfyjxf.cumulus.api.diagnostic;

/**
 * 结构实例状态（三态）。
 *
 * <ul>
 *     <li>{@link #OK}：结构合法且数据完整（通常意味着 runtimeContext 可用）</li>
 *     <li>{@link #ERROR}：结构非法（诊断中存在 ERROR）</li>
 *     <li>{@link #INCOMPLETE}：缺数据/缺依赖导致暂不可判定或无法完成（应附带 DataRequest）</li>
 * </ul>
 */
public enum InstanceStatus {
    /** 合法且数据完整。 */
    OK,
    /** 非法。 */
    ERROR,
    /** 缺数据/缺依赖，暂不可完成。 */
    INCOMPLETE
}
