package dev.vfyjxf.cumulus.api.engine;

/**
 * 变化来源（用于调试/指标/诊断归因）。
 *
 * <p>该枚举不会改变引擎行为，只用于说明 dirty 的来源。
 */
public enum ChangeCause {
    /** 外部事件流产生的变化。 */
    EVENT,
    /** 主动扫描（driver 或引擎内部轮询）产生的变化。 */
    SCAN,
    /** 管理/命令触发（例如手动重算）。 */
    ADMIN,
    /** 未知来源。 */
    UNKNOWN
}
